package com.tomatosystem.figma.ai;

import com.tomatosystem.exconverter.model.UiIr;
import com.tomatosystem.exconverter.service.ProgressLog;
import com.tomatosystem.exconverter.service.UiIrParser;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

/**
 * Second opinion on a deterministic UI-IR. The model sees the UI-IR (and optionally the rendered frame) and may
 * answer only with whitelisted {@link UiIrPatch} ops. A patched UI-IR is kept only when it still parses; otherwise
 * the deterministic one is used, so the AI can improve the result but never break it.
 */
public final class UiIrCritic {
	private static final String SYSTEM = String.join("\n",
		"당신은 업무 화면 설계 검토자입니다. 입력은 Figma 디자인에서 규칙으로 추출한 UI-IR(JSON)입니다.",
		"UI-IR 은 화면을 위에서 아래로 나열한 영역 목록입니다.",
		"  type: title(화면제목) | search(조회조건) | form(입력/상세) | grid(표) | tabs | textarea | buttons | description | sectionTitle",
		"  form/search.fields[]: {label, component, required, options?}  component: inputbox, combobox, dateinput, daterange,",
		"    searchinput, checkbox, checkboxgroup, radiobutton, numbereditor, maskeditor, textarea, output(읽기전용)",
		"  grid.columns[]: {header, editor, width, cellText}  editor: output, rowindex, checkbox, button, inputbox, combobox, dateinput, maskeditor, numbereditor",
		"",
		"당신의 일은 '명백히 잘못된 것'만 고치는 것입니다. 애매하면 고치지 마세요. 추측 금지.",
		"자주 나오는 오류: 조회조건인데 form 으로, 상세입력인데 search 로 분류 / 콤보박스를 입력칸으로 /",
		"날짜 입력을 일반 입력으로 / 표의 편집기 오판 / 컬럼 바인딩명(cellText) 누락 / 장식용 텍스트가 description 으로 남음.",
		"",
		"반드시 아래 JSON 형식만 출력하세요. 고칠 것이 없으면 {\"patches\":[]} 를 출력하세요.",
		"{\"patches\":[{\"op\":\"...\",\"region\":0,\"field\":0,\"column\":0,\"value\":\"...\",\"reason\":\"한 줄 근거\"}]}",
		"허용 op: region.type(search↔form, description↔sectionTitle 만), region.title, region.drop(description/sectionTitle/buttons 만),",
		"field.label, field.component, field.required, column.header, column.editor, column.name(대문자 컬럼코드), screen.name",
		"region/field/column 은 0부터 시작하는 배열 인덱스입니다. 영역을 추가하거나 순서를 바꿀 수는 없습니다. 최대 30개.");

	private final AiClient client;

	public UiIrCritic(AiClient client) { this.client = client; }

	/** Result of one review; uiIr is the deterministic input when nothing was applied. */
	public static final class Review {
		public final JSONObject uiIr;
		public final List<String> applied;
		public final List<String> rejected;
		public final boolean called;
		Review(JSONObject uiIr, List<String> applied, List<String> rejected, boolean called) {
			this.uiIr = uiIr; this.applied = applied; this.rejected = rejected; this.called = called;
		}
	}

	/** @param png rendered frame, or null */
	public Review review(JSONObject uiIr, byte[] png) {
		if (!client.isEnabled()) return new Review(uiIr, new ArrayList<String>(), new ArrayList<String>(), false);
		String user = "다음 UI-IR 을 검토하세요."
			+ (png != null ? " 첨부 이미지는 같은 화면의 실제 렌더링입니다. 이미지와 UI-IR 이 다르면 이미지를 기준으로 판단하세요." : "")
			+ "\n\n" + strip(uiIr).toString(2);
		String answer = client.completeJson(SYSTEM, user, png);
		if (answer == null) return new Review(uiIr, new ArrayList<String>(), list("AI 응답 없음(설정/한도/타임아웃) → 규칙 결과 사용"), true);
		UiIrPatch.Result patched = UiIrPatch.apply(uiIr, answer);
		if (!patched.changed()) return new Review(uiIr, patched.applied, patched.rejected, true);
		try {
			UiIrParser.parse(patched.uiIr.toString()); // the patched UI-IR must still be a valid screen
		} catch (RuntimeException e) {
			ProgressLog.step("AI 보정 결과가 UI-IR 규격을 벗어나 폐기: {}", e.getMessage());
			patched.rejected.add("보정 결과 파싱 실패 → 규칙 결과 사용: " + e.getMessage());
			return new Review(uiIr, new ArrayList<String>(), patched.rejected, true);
		}
		ProgressLog.step("AI 보정 {}건 적용: {}", patched.applied.size(), patched.applied);
		return new Review(patched.uiIr, patched.applied, patched.rejected, true);
	}

	/** Asks for a fix after ClxValidator refused the generated CLX. */
	public Review repair(JSONObject uiIr, String errors) {
		if (!client.isEnabled()) return new Review(uiIr, new ArrayList<String>(), new ArrayList<String>(), false);
		String user = "아래 UI-IR 로 CLX 를 만들었더니 검증 오류가 났습니다. 오류를 없앨 최소한의 수정만 patch 로 제안하세요.\n"
			+ "검증 오류: " + errors + "\n\n" + strip(uiIr).toString(2);
		String answer = client.completeJson(SYSTEM, user, null);
		if (answer == null) return new Review(uiIr, new ArrayList<String>(), list("수리용 AI 응답 없음"), true);
		UiIrPatch.Result patched = UiIrPatch.apply(uiIr, answer);
		if (!patched.changed()) return new Review(uiIr, patched.applied, patched.rejected, true);
		try { UiIrParser.parse(patched.uiIr.toString()); } catch (RuntimeException e) {
			return new Review(uiIr, new ArrayList<String>(), list("수리 결과 파싱 실패: " + e.getMessage()), true);
		}
		return new Review(patched.uiIr, patched.applied, patched.rejected, true);
	}

	/** The model does not need Figma node ids or our debug fields. */
	private static JSONObject strip(JSONObject uiIr) {
		JSONObject copy = new JSONObject(uiIr.toString());
		copy.remove("source");
		copy.remove("ai");
		return copy;
	}

	private static List<String> list(String value) {
		List<String> result = new ArrayList<String>();
		result.add(value);
		return result;
	}

	/** Region summary for logs: "title → search(f6) → grid(c15)". */
	public static String summary(UiIr ir) {
		StringBuilder s = new StringBuilder();
		for (UiIr.Region region : ir.getRegions()) {
			if (s.length() > 0) s.append(" → ");
			s.append(region.getType());
			if (!region.getFields().isEmpty()) s.append("(f").append(region.getFields().size()).append(')');
			if (!region.getColumns().isEmpty()) s.append("(c").append(region.getColumns().size()).append(')');
		}
		return s.toString();
	}
}
