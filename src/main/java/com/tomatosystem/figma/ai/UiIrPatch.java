package com.tomatosystem.figma.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The only edits an AI may make to a UI-IR. Everything outside this whitelist is dropped, so a wrong or hostile
 * answer can never invent regions, reorder the screen or reach the CLX: the worst case is "no change".
 *
 * <p>Answer format expected from the model:
 * <pre>{"patches":[{"op":"field.component","region":1,"field":2,"value":"combobox","reason":"…"}]}</pre>
 */
public final class UiIrPatch {
	private static final int MAX_OPS = 30;
	private static final int MAX_TEXT = 60;
	private static final List<String> COMPONENTS = Arrays.asList("inputbox", "dateinput", "daterange", "combobox", "searchinput",
		"checkbox", "checkboxgroup", "radiobutton", "numbereditor", "maskeditor", "textarea", "output");
	private static final List<String> EDITORS = Arrays.asList("output", "checkbox", "rowindex", "inputbox", "maskeditor",
		"numbereditor", "combobox", "dateinput", "button");
	/** A type may only move inside its own group: no "grid → form" (the fields do not exist). */
	private static final List<List<String>> TYPE_GROUPS = Arrays.asList(Arrays.asList("search", "form"), Arrays.asList("description", "sectionTitle"));
	private static final List<String> DROPPABLE = Arrays.asList("description", "sectionTitle", "buttons");
	/** Every op the model may use; anything else is refused before it is even looked at. */
	private static final List<String> OPS = Arrays.asList("screen.name", "region.type", "region.title", "region.drop",
		"field.label", "field.component", "field.required", "column.header", "column.editor", "column.name");

	private UiIrPatch() { }

	/** Outcome of applying a model answer: the new UI-IR plus what was applied and what was refused. */
	public static final class Result {
		public final JSONObject uiIr;
		public final List<String> applied = new ArrayList<String>();
		public final List<String> rejected = new ArrayList<String>();
		Result(JSONObject uiIr) { this.uiIr = uiIr; }
		public boolean changed() { return !applied.isEmpty(); }
	}

	/**
	 * Applies the model answer to a copy of the UI-IR.
	 *
	 * @param uiIr  deterministic UI-IR (not modified)
	 * @param answer model answer; anything unparseable yields zero applied ops
	 */
	public static Result apply(JSONObject uiIr, String answer) {
		Result result = new Result(new JSONObject(uiIr.toString()));
		JSONArray patches = patchesOf(answer, result);
		if (patches == null) return result;
		JSONArray regions = result.uiIr.optJSONArray("regions");
		if (regions == null) return result;
		int dropped = 0;
		for (int i = 0; i < patches.length() && i < MAX_OPS; i++) {
			JSONObject patch = patches.optJSONObject(i);
			if (patch == null) continue;
			String op = patch.optString("op", "").trim();
			String value = clean(patch.optString("value", ""));
			if (!OPS.contains(op)) { reject(result, op, "허용되지 않은 op"); continue; }
			try {
				if ("screen.name".equals(op)) {
					if (value.isEmpty()) { reject(result, op, "빈 값"); continue; }
					result.uiIr.getJSONObject("screen").put("name", value);
					result.applied.add(op + " = " + value);
					continue;
				}
				JSONObject region = regionAt(regions, patch);
				if (region == null) { reject(result, op, "region 인덱스 오류"); continue; }
				if ("region.type".equals(op)) {
					String from = region.optString("type");
					value = value.toLowerCase(Locale.ROOT);
					if (!sameGroup(from, value)) { reject(result, op, from + " → " + value + " 는 허용되지 않음"); continue; }
					region.put("type", value);
					result.applied.add(op + "[" + patch.optInt("region") + "] " + from + " → " + value);
				} else if ("region.title".equals(op)) {
					if (value.isEmpty()) { reject(result, op, "빈 값"); continue; }
					region.put("title", value);
					result.applied.add(op + "[" + patch.optInt("region") + "] = " + value);
				} else if ("region.drop".equals(op)) {
					if (dropped >= 1) { reject(result, op, "한 번에 1개 영역만 삭제 가능"); continue; }
					if (!DROPPABLE.contains(region.optString("type"))) { reject(result, op, region.optString("type") + " 영역은 삭제 불가"); continue; }
					region.put("__drop", true);
					dropped++;
					result.applied.add(op + "[" + patch.optInt("region") + "] " + region.optString("type"));
				} else if (op.startsWith("field.")) {
					JSONArray fields = region.optJSONArray("fields");
					JSONObject field = itemAt(fields, patch.optInt("field", -1));
					if (field == null) { reject(result, op, "field 인덱스 오류"); continue; }
					if ("field.component".equals(op)) {
						value = value.toLowerCase(Locale.ROOT);
						if (!COMPONENTS.contains(value)) { reject(result, op, "알 수 없는 컨트롤 " + value); continue; }
						field.put("component", value);
					} else if ("field.label".equals(op)) {
						if (value.isEmpty()) { reject(result, op, "빈 값"); continue; }
						field.put("label", value);
					} else if ("field.required".equals(op)) {
						field.put("required", "true".equalsIgnoreCase(value) || patch.optBoolean("value", false));
					} else { reject(result, op, "알 수 없는 op"); continue; }
					result.applied.add(op + "[" + patch.optInt("region") + "." + patch.optInt("field") + "] = " + value);
				} else if (op.startsWith("column.")) {
					JSONArray columns = region.optJSONArray("columns");
					JSONObject column = itemAt(columns, patch.optInt("column", -1));
					if (column == null) { reject(result, op, "column 인덱스 오류"); continue; }
					if ("column.editor".equals(op)) {
						value = value.toLowerCase(Locale.ROOT);
						if (!EDITORS.contains(value)) { reject(result, op, "알 수 없는 편집기 " + value); continue; }
						column.put("editor", value);
					} else if ("column.header".equals(op)) {
						if (value.isEmpty()) { reject(result, op, "빈 값"); continue; }
						column.put("header", value);
					} else if ("column.name".equals(op)) {
						// The generator binds a column whose cell text is an upper-case identifier.
						value = value.toUpperCase(Locale.ROOT);
						if (!value.matches("[A-Z][A-Z0-9_]{1,29}")) { reject(result, op, "컬럼코드 형식 아님: " + value); continue; }
						column.put("cellText", value);
					} else { reject(result, op, "알 수 없는 op"); continue; }
					result.applied.add(op + "[" + patch.optInt("region") + "." + patch.optInt("column") + "] = " + value);
				} else {
					reject(result, op, "허용되지 않은 op");
				}
			} catch (RuntimeException e) {
				reject(result, op, e.getMessage());
			}
		}
		if (dropped > 0) {
			JSONArray kept = new JSONArray();
			for (int i = 0; i < regions.length(); i++) {
				JSONObject region = regions.optJSONObject(i);
				if (region != null && !region.optBoolean("__drop", false)) kept.put(region);
			}
			result.uiIr.put("regions", kept);
		}
		return result;
	}

	private static JSONArray patchesOf(String answer, Result result) {
		if (answer == null || answer.trim().isEmpty()) return null;
		String text = answer.trim();
		int start = text.indexOf('{');
		int end = text.lastIndexOf('}');
		if (start < 0 || end <= start) { result.rejected.add("AI 응답이 JSON 이 아님"); return null; }
		try {
			JSONObject json = new JSONObject(text.substring(start, end + 1));
			JSONArray patches = json.optJSONArray("patches");
			if (patches == null) patches = json.optJSONArray("ops");
			if (patches == null) result.rejected.add("patches 배열 없음");
			return patches;
		} catch (RuntimeException e) {
			result.rejected.add("AI 응답 파싱 실패: " + e.getMessage());
			return null;
		}
	}

	private static JSONObject regionAt(JSONArray regions, JSONObject patch) {
		return itemAt(regions, patch.optInt("region", -1));
	}

	private static JSONObject itemAt(JSONArray array, int index) {
		return array == null || index < 0 || index >= array.length() ? null : array.optJSONObject(index);
	}

	private static boolean sameGroup(String from, String to) {
		for (List<String> group : TYPE_GROUPS) { if (group.contains(from) && group.contains(to) && !from.equals(to)) return true; }
		return false;
	}

	private static String clean(String value) {
		String text = value == null ? "" : value.replaceAll("[\\p{Cntrl}]", " ").trim();
		if (text.length() > MAX_TEXT) text = text.substring(0, MAX_TEXT).trim();
		return text;
	}

	private static void reject(Result result, String op, String reason) { result.rejected.add(op + ": " + reason); }
}
