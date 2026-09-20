package com.tomatosystem.figma.ai;

import com.tomatosystem.exconverter.service.ColumnNames;
import com.tomatosystem.exconverter.service.ProgressLog;
import com.tomatosystem.figma.FigmaSettings;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Dataset column codes for grid headers. Without this the generator falls back to COL1, COL2 … because the built-in
 * dictionary only knows ~40 labels.
 *
 * <p>Order per header: the design's own binding text (cellText) → dictionary ({@link ColumnNames}) → file cache →
 * one AI call for whatever is still unknown. Answers are cached on disk, so repeated conversions cost no API calls
 * and stay stable (the same label always yields the same column code).
 */
public final class LabelCodeNamer {
	private static final String SYSTEM = String.join("\n",
		"당신은 한국 공공/금융 업무 시스템의 데이터 컬럼 명명 규칙 전문가입니다.",
		"한글 화면 라벨 목록을 받아 각 라벨에 어울리는 DB/데이터셋 컬럼 코드를 제안하세요.",
		"규칙: 영문 대문자와 숫자, 밑줄만 사용(최대 20자). 관례 약어 사용(성명 NM, 등록일 REG_DT, 사용여부 USE_YN,",
		"금액 AMT, 수량 QTY, 구분 SE, 코드 CD, 번호 NO, 일자 YMD, 여부 YN). 라벨마다 서로 다른 코드를 쓰세요.",
		"출력은 아래 JSON 만: {\"codes\":{\"라벨\":\"CODE\", ...}}");

	private final AiClient client;
	private final File cacheFile;
	private final Map<String, String> cache = new LinkedHashMap<String, String>();

	public LabelCodeNamer(AiClient client) {
		this.client = client;
		this.cacheFile = new File(new File(FigmaSettings.get("exconverter.generated.root", "generated"), "ai-cache"), "label-codes.json");
		load();
	}

	/**
	 * Fills in `cellText` (the generator's binding hook) for grid columns that have no code yet.
	 *
	 * @return the labels that were named, for the audit log
	 */
	public List<String> nameColumns(JSONObject uiIr) {
		List<String> named = new ArrayList<String>();
		JSONArray regions = uiIr.optJSONArray("regions");
		if (regions == null) return named;
		List<String> unknown = new ArrayList<String>();
		List<JSONObject> pending = new ArrayList<JSONObject>();
		for (int r = 0; r < regions.length(); r++) {
			JSONObject region = regions.optJSONObject(r);
			if (region == null || !"grid".equals(region.optString("type"))) continue;
			JSONArray columns = region.optJSONArray("columns");
			if (columns == null) continue;
			for (int c = 0; c < columns.length(); c++) {
				JSONObject column = columns.optJSONObject(c);
				if (column == null) continue;
				String header = column.optString("header", "").trim();
				String editor = column.optString("editor", "");
				if (header.isEmpty() || "rowindex".equals(editor) || "checkbox".equals(editor) && header.isEmpty()) continue;
				if (!column.optString("cellText", "").isEmpty()) continue;
				String code = fromDictionary(header);
				if (code.isEmpty()) code = cache.getOrDefault(key(header), "");
				if (code.isEmpty()) { unknown.add(header); pending.add(column); continue; }
				column.put("cellText", code);
				named.add(header + "→" + code);
			}
		}
		if (!unknown.isEmpty() && client.isEnabled()) {
			Map<String, String> codes = ask(unknown);
			for (int i = 0; i < pending.size(); i++) {
				String header = unknown.get(i);
				String code = codes.get(header);
				if (code == null) continue;
				pending.get(i).put("cellText", code);
				cache.put(key(header), code);
				named.add(header + "→" + code + "(AI)");
			}
			if (!codes.isEmpty()) save();
		}
		return named;
	}

	private Map<String, String> ask(List<String> labels) {
		Map<String, String> result = new LinkedHashMap<String, String>();
		JSONArray list = new JSONArray();
		for (String label : labels) list.put(label);
		String answer = client.completeJson(SYSTEM, "라벨 목록: " + list.toString(), null);
		if (answer == null) return result;
		try {
			int start = answer.indexOf('{');
			int end = answer.lastIndexOf('}');
			if (start < 0 || end <= start) return result;
			JSONObject codes = new JSONObject(answer.substring(start, end + 1)).optJSONObject("codes");
			if (codes == null) return result;
			java.util.Set<String> used = new java.util.HashSet<String>(cache.values());
			for (String label : labels) {
				String code = codes.optString(label, "").trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "");
				if (!code.matches("[A-Z][A-Z0-9_]{1,19}") || used.contains(code)) continue;
				used.add(code);
				result.put(label, code);
			}
		} catch (RuntimeException e) {
			ProgressLog.step("컬럼 코드 응답 파싱 실패: {}", e.getMessage());
		}
		return result;
	}

	private static String fromDictionary(String header) {
		String code = ColumnNames.nameFor(header);
		return code == null ? "" : code;
	}

	private static String key(String header) { return header.replace(" ", "").toLowerCase(Locale.ROOT); }

	private void load() {
		try {
			if (!cacheFile.isFile()) return;
			JSONObject json = new JSONObject(new String(Files.readAllBytes(cacheFile.toPath()), StandardCharsets.UTF_8));
			for (String key : json.keySet()) cache.put(key, json.getString(key));
		} catch (Exception e) {
			ProgressLog.step("컬럼 코드 캐시 읽기 실패: {}", e.getMessage());
		}
	}

	private void save() {
		try {
			File dir = cacheFile.getParentFile();
			if (!dir.isDirectory() && !dir.mkdirs()) return;
			JSONObject json = new JSONObject();
			for (Map.Entry<String, String> entry : cache.entrySet()) json.put(entry.getKey(), entry.getValue());
			Files.write(cacheFile.toPath(), json.toString(2).getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			ProgressLog.step("컬럼 코드 캐시 저장 실패: {}", e.getMessage());
		}
	}

	public int cacheSize() { return cache.size(); }
}
