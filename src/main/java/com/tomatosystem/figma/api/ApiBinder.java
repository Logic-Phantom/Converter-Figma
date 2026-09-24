package com.tomatosystem.figma.api;

import com.tomatosystem.exconverter.service.ProgressLog;
import com.tomatosystem.figma.FigmaSettings;
import com.tomatosystem.figma.ai.AiClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Joins a UI-IR with an {@link OpenApiSpec}: every grid gets the list operation whose row DTO matches its headers
 * best, the search bar gets that operation's parameters, forms get detail/save/delete operations, and the buttons of
 * the design ("조회", "저장", "삭제") become the triggers. The result is written back into the UI-IR JSON
 * ({@code region.dataId}, {@code column.name}, {@code field.name}, root {@code api.submissions}); the CLX generator
 * turns it into DataSets, DataMaps, datamapbinds, submissions and listeners.
 *
 * <p>Rules only, unless an AI provider is configured — then unmatched labels are offered to it with the remaining
 * property names (see {@link ApiLabelMatcher}). Nothing here touches CLX.
 */
public final class ApiBinder {
	private static final String SEARCH_CAPTION = "(조회|검색|찾기|조회하기|검색하기|search|find|inquiry)";
	private static final String SAVE_CAPTION = "(저장|등록|확인|적용|신청|save|submit|register|apply)";
	private static final String DELETE_CAPTION = "(삭제|행삭제|delete|remove)";
	private static final String PAGING_PARAM = "(?i)(page|pageno|pagenum|pagenumber|pageindex|pagesize|size|rows|limit|offset|perpage|rowsperpage|start)";

	private final OpenApiSpec spec;
	private final ApiLabelMatcher matcher;
	private final String basePath;

	public ApiBinder(OpenApiSpec spec, AiClient ai) {
		this.spec = spec;
		this.matcher = new ApiLabelMatcher(ai);
		String configured = FigmaSettings.get("figma.api.baseUrl", "");
		this.basePath = (configured.isEmpty() ? spec.basePath : configured).replaceAll("/+$", "");
	}

	/** What was bound, for the response and the audit JSON. */
	public static final class Result {
		public final List<String> notes = new ArrayList<String>();
		public final List<String> submissions = new ArrayList<String>();
		public int boundColumns;
		public int totalColumns;
		public int boundFields;
		public int totalFields;
		public boolean any() { return !submissions.isEmpty(); }
		public String summary() {
			if (!any()) return "API 바인딩 없음" + (notes.isEmpty() ? "" : " (" + String.join("; ", notes) + ")");
			return "API 바인딩: " + String.join(", ", submissions) + " · 컬럼 " + boundColumns + "/" + totalColumns + " · 필드 " + boundFields + "/" + totalFields;
		}
	}

	public Result bind(JSONObject uiIr) {
		Result result = new Result();
		JSONArray regions = uiIr.optJSONArray("regions");
		JSONObject screen = uiIr.optJSONObject("screen");
		if (regions == null || spec.operations.isEmpty()) { result.notes.add("OpenAPI 에 operation 이 없습니다"); return result; }
		String screenName = screen == null ? "" : screen.optString("name", "");
		List<JSONObject> grids = new ArrayList<JSONObject>();
		List<JSONObject> forms = new ArrayList<JSONObject>();
		JSONObject search = null;
		List<String> captions = new ArrayList<String>();
		for (int i = 0; i < regions.length(); i++) {
			JSONObject region = regions.optJSONObject(i);
			if (region == null) continue;
			String type = region.optString("type", "");
			if ("grid".equals(type)) grids.add(region);
			else if ("form".equals(type)) forms.add(region);
			else if ("search".equals(type) && (search == null || region.optString("side", "").isEmpty() && !search.optString("side", "").isEmpty())) search = region;
			JSONArray buttons = region.optJSONArray("buttons");
			if (buttons != null) { for (int b = 0; b < buttons.length(); b++) captions.add(buttons.optString(b, "")); }
		}
		JSONArray submissions = new JSONArray();
		Set<OpenApiSpec.Operation> used = new LinkedHashSet<OpenApiSpec.Operation>();
		boolean searchBound = false;
		String listSubmissionId = "";
		int gridIndex = 0;
		for (JSONObject grid : grids) {
			List<String> labels = columnLabels(grid);
			result.totalColumns += labels.size();
			OpenApiSpec.Operation op = bestOperation(labels, keywords(screenName, grid.optString("title", "")), used, Kind.LIST);
			if (op == null) { result.notes.add("그리드 '" + title(grid, screenName) + "': 헤더와 맞는 목록(배열 응답) API 를 찾지 못함"); continue; }
			used.add(op);
			String dsId = gridIndex == 0 ? "dsList" : "dsList" + (gridIndex + 1);
			gridIndex++;
			grid.put("dataId", dsId);
			Map<String, ApiLabelMatcher.Match> matches = matcher.match(labels, op.responseFields, scope(op));
			List<String> pairs = new ArrayList<String>();
			List<String> unmatched = new ArrayList<String>();
			JSONArray columns = grid.getJSONArray("columns");
			for (int c = 0; c < columns.length(); c++) {
				JSONObject column = columns.optJSONObject(c);
				if (column == null) continue;
				String header = column.optString("header", "").trim();
				if (!labels.contains(header)) continue;
				ApiLabelMatcher.Match m = matches.get(header);
				if (m == null) { unmatched.add(header); continue; }
				column.put("name", m.property.name);
				if (!m.property.dataType().isEmpty()) column.put("dataType", m.property.dataType());
				pairs.add(header + "→" + m.property.name + ("rule".equals(m.reason) ? "" : "(" + m.reason + ")"));
				result.boundColumns++;
			}
			JSONObject sub = submission(gridIndex == 1 ? "subList" : "subList" + gridIndex, op, "search");
			sub.put("response", dsId);
			result.notes.add(dsId + " ← " + op.describe() + " 컬럼 " + pairs.size() + "/" + labels.size() + (pairs.isEmpty() ? "" : ": " + String.join(", ", pairs)) + (unmatched.isEmpty() ? "" : " · 미매칭: " + String.join(", ", unmatched)));
			// The search bar sends this operation's parameters (query params, or the body of a POST search).
			if (search != null && !searchBound) {
				List<OpenApiSpec.Property> inputs = op.inputs();
				int bound = bindFields(search, inputs, scope(op), "dmSearch", result);
				if (bound > 0) { searchBound = true; sub.put("request", "dmSearch"); }
				List<String> paging = new ArrayList<String>();
				for (OpenApiSpec.Property p : inputs) { if (p.name.matches(PAGING_PARAM)) paging.add(p.name); }
				if (!paging.isEmpty()) result.notes.add("페이징 파라미터 " + paging + " 는 JS 에서 pageindexer 와 연결하세요");
			}
			String trigger = firstCaption(search == null ? new JSONArray() : search.optJSONArray("buttons"), SEARCH_CAPTION);
			if (trigger.isEmpty()) trigger = firstCaption(grid.optJSONArray("buttons"), SEARCH_CAPTION);
			if (trigger.isEmpty()) trigger = captionMatching(captions, SEARCH_CAPTION);
			sub.put("caption", trigger);
			if (listSubmissionId.isEmpty()) listSubmissionId = sub.getString("id");
			submissions.put(sub);
			result.submissions.add(sub.getString("id") + "(" + op.method.toUpperCase(Locale.ROOT) + " " + op.path + ")");
			// A 삭제 button on the grid's title row deletes the selected row: DELETE on the same resource, keyed by the row.
			String deleteCaption = exactOrShortCaption(captionsOf(grid), DELETE_CAPTION);
			if (forms.isEmpty() && !deleteCaption.isEmpty()) {
				OpenApiSpec.Operation delete = deleteOperation(op, used);
				if (delete != null) {
					used.add(delete);
					JSONObject del = submission(gridIndex == 1 ? "subDelete" : "subDelete" + gridIndex, delete, "delete");
					del.put("request", dsId);
					del.put("caption", deleteCaption);
					submissions.put(del);
					result.submissions.add(del.getString("id") + "(DELETE " + delete.path + ")");
					if (!delete.pathParams().isEmpty()) result.notes.add(del.getString("id") + " 경로 파라미터 " + delete.pathParams() + " 는 생성된 JS 에서 선택 행 값으로 치환");
				}
			}
		}
		if (search != null) result.totalFields += fieldLabels(search).size();

		int formIndex = 0;
		for (JSONObject form : forms) {
			List<String> labels = fieldLabels(form);
			result.totalFields += labels.size();
			OpenApiSpec.Operation detail = bestOperation(labels, keywords(screenName, form.optString("title", "")), used, Kind.DETAIL);
			OpenApiSpec.Operation save = bestOperation(labels, keywords(screenName, form.optString("title", "")), used, Kind.SAVE);
			if (detail == null && save == null) { result.notes.add("폼 '" + title(form, screenName) + "': 필드와 맞는 상세/저장 API 를 찾지 못함"); continue; }
			String dmId = formIndex == 0 ? "dmDetail" : "dmDetail" + (formIndex + 1);
			formIndex++;
			List<OpenApiSpec.Property> candidates = new ArrayList<OpenApiSpec.Property>();
			if (save != null) candidates.addAll(save.bodyFields);
			if (detail != null) { for (OpenApiSpec.Property p : detail.responseFields) { if (!containsName(candidates, p.name)) candidates.add(p); } }
			int bound = bindFields(form, candidates, scope(save != null ? save : detail), dmId, result);
			if (bound == 0) { result.notes.add("폼 '" + title(form, screenName) + "': 필드 이름을 하나도 짝짓지 못해 바인딩 생략"); form.remove("dataId"); continue; }
			if (detail != null) {
				used.add(detail);
				JSONObject sub = submission(formIndex == 1 ? "subDetail" : "subDetail" + formIndex, detail, "detail");
				sub.put("response", dmId);
				submissions.put(sub);
				result.submissions.add(sub.getString("id") + "(" + detail.method.toUpperCase(Locale.ROOT) + " " + detail.path + ")");
				if (!detail.pathParams().isEmpty()) result.notes.add(sub.getString("id") + " 경로 파라미터 " + detail.pathParams() + " 는 생성된 JS 에서 선택 행 값으로 치환");
			}
			if (save != null) {
				used.add(save);
				JSONObject sub = submission(formIndex == 1 ? "subSave" : "subSave" + formIndex, save, "save");
				sub.put("request", dmId);
				sub.put("caption", exactOrShortCaption(captions, SAVE_CAPTION));
				submissions.put(sub);
				result.submissions.add(sub.getString("id") + "(" + save.method.toUpperCase(Locale.ROOT) + " " + save.path + ")");
			}
			OpenApiSpec.Operation delete = deleteOperation(detail != null ? detail : save, used);
			if (delete != null) {
				used.add(delete);
				JSONObject sub = submission(formIndex == 1 ? "subDelete" : "subDelete" + formIndex, delete, "delete");
				sub.put("request", dmId);
				sub.put("caption", exactOrShortCaption(captions, DELETE_CAPTION));
				submissions.put(sub);
				result.submissions.add(sub.getString("id") + "(DELETE " + delete.path + ")");
			}
		}
		// A grid edited in place (행추가/저장 on its title row) saves its rows as an array body.
		if (forms.isEmpty() && !grids.isEmpty() && !exactOrShortCaption(captions, SAVE_CAPTION).isEmpty()) {
			JSONObject grid = grids.get(0);
			if (grid.has("dataId")) {
				OpenApiSpec.Operation save = bestOperation(columnLabels(grid), keywords(screenName, grid.optString("title", "")), used, Kind.SAVE);
				if (save != null) {
					used.add(save);
					JSONObject sub = submission("subSave", save, "save");
					sub.put("request", grid.getString("dataId"));
					sub.put("caption", exactOrShortCaption(captions, SAVE_CAPTION));
					submissions.put(sub);
					result.submissions.add("subSave(" + save.method.toUpperCase(Locale.ROOT) + " " + save.path + (save.bodyIsArray ? " 배열" : "") + ")");
				}
			}
		}
		if (submissions.length() > 0) uiIr.put("api", new JSONObject().put("spec", spec.title + " · " + spec.source).put("submissions", submissions));
		ProgressLog.step("{}", result.summary());
		return result;
	}

	// ------------------------------------------------------------------ operations

	private enum Kind { LIST, DETAIL, SAVE }

	/** The operation whose DTO matches the most labels; ties go to the one whose path/summary mentions the screen. */
	private OpenApiSpec.Operation bestOperation(List<String> labels, List<String> keywords, Set<OpenApiSpec.Operation> used, Kind kind) {
		OpenApiSpec.Operation best = null;
		int bestScore = 0;
		for (OpenApiSpec.Operation op : spec.operations) {
			if (used.contains(op)) continue;
			List<OpenApiSpec.Property> fields;
			if (kind == Kind.LIST) { if (!op.responseIsList || !(op.method.equals("get") || op.method.equals("post"))) continue; fields = op.responseFields; }
			else if (kind == Kind.DETAIL) { if (op.responseIsList || !op.method.equals("get")) continue; fields = op.responseFields; }
			else { if (!(op.method.equals("post") || op.method.equals("put") || op.method.equals("patch"))) continue; fields = op.bodyFields; }
			if (fields.size() < 1) continue;
			int matched = 0;
			for (String label : labels) {
				int max = 0;
				for (OpenApiSpec.Property p : fields) max = Math.max(max, ApiLabelMatcher.score(label, p));
				if (max >= ApiLabelMatcher.MIN_SCORE) matched++;
			}
			int score = matched * 10;
			String words = op.keywords();
			for (String keyword : keywords) { if (keyword.length() >= 2 && words.contains(keyword)) score += 3; }
			if (kind == Kind.LIST && op.method.equals("get")) score += 1;
			if (kind == Kind.SAVE && op.method.equals("post")) score += 1;
			if (matched == 0 && (labels.isEmpty() ? score == 0 : true)) continue;
			if (score > bestScore) { best = op; bestScore = score; }
		}
		return best;
	}

	/** A DELETE whose path starts like the resource's path ("/api/users/{id}" for "/api/users"). */
	private OpenApiSpec.Operation deleteOperation(OpenApiSpec.Operation resource, Set<OpenApiSpec.Operation> used) {
		if (resource == null) return null;
		String prefix = resource.path.replaceAll("/\\{[^}]+}.*$", "");
		OpenApiSpec.Operation fallback = null;
		for (OpenApiSpec.Operation op : spec.operations) {
			if (used.contains(op) || !op.method.equals("delete")) continue;
			if (op.path.startsWith(prefix)) return op;
			if (fallback == null) fallback = op;
		}
		return null == fallback ? null : (spec.operations.size() <= 12 ? fallback : null);
	}

	private JSONObject submission(String id, OpenApiSpec.Operation op, String role) {
		return new JSONObject()
			.put("id", id)
			.put("action", basePath + op.path)
			.put("method", op.method)
			.put("role", role)
			.put("responsePath", op.responsePath)
			.put("operationId", op.operationId)
			.put("summary", op.summary);
	}

	// ------------------------------------------------------------------ fields

	/** Names the fields of a search/form region from the candidates; returns how many were bound. */
	private int bindFields(JSONObject region, List<OpenApiSpec.Property> candidates, String scope, String dataId, Result result) {
		JSONArray fields = region.optJSONArray("fields");
		if (fields == null || candidates.isEmpty()) return 0;
		List<String> labels = fieldLabels(region);
		Map<String, ApiLabelMatcher.Match> matches = matcher.match(labels, candidates, scope);
		Set<String> taken = new LinkedHashSet<String>();
		for (ApiLabelMatcher.Match m : matches.values()) taken.add(m.property.name);
		int bound = 0;
		List<String> pairs = new ArrayList<String>();
		for (int i = 0; i < fields.length(); i++) {
			JSONObject field = fields.optJSONObject(i);
			if (field == null) continue;
			String label = field.optString("label", "").trim();
			String name = "";
			if ("daterange".equals(field.optString("component"))) {
				String[] pair = dateRangePair(label, candidates, taken);
				if (pair != null) { name = pair[0] + "," + pair[1]; taken.add(pair[0]); taken.add(pair[1]); }
			}
			if (name.isEmpty() && matches.containsKey(label)) name = matches.get(label).property.name;
			if (name.isEmpty()) continue;
			field.put("name", name);
			pairs.add(label + "→" + name);
			bound++;
		}
		if (bound > 0) {
			region.put("dataId", dataId);
			result.boundFields += bound;
			result.notes.add(dataId + " 필드 " + bound + "/" + labels.size() + ": " + String.join(", ", pairs));
		}
		return bound;
	}

	/** startDate/endDate, fromYmd/toYmd, bgngDt/endDt … whose remaining tokens fit the label. */
	private static String[] dateRangePair(String label, List<OpenApiSpec.Property> candidates, Set<String> taken) {
		String[][] pairs = { { "start", "end" }, { "from", "to" }, { "bgng", "end" }, { "begin", "end" }, { "st", "ed" }, { "s", "e" }, { "min", "max" } };
		for (String[] pair : pairs) {
			OpenApiSpec.Property from = null, to = null;
			for (OpenApiSpec.Property c : candidates) {
				if (taken.contains(c.name)) continue;
				List<String> tokens = ApiLabelMatcher.tokens(c.name);
				if (tokens.contains(pair[0]) && from == null) from = c;
				else if (tokens.contains(pair[1]) && to == null) to = c;
			}
			if (from == null || to == null) continue;
			// Both should share the rest of their tokens ("regDt") and one of them should fit the label when it says more than 기간.
			List<String> a = new ArrayList<String>(ApiLabelMatcher.tokens(from.name)); a.remove(pair[0]);
			List<String> b = new ArrayList<String>(ApiLabelMatcher.tokens(to.name)); b.remove(pair[1]);
			if (!a.equals(b)) continue;
			int score = Math.max(ApiLabelMatcher.score(label, from), ApiLabelMatcher.score(label, to));
			if (score >= ApiLabelMatcher.MIN_SCORE || label.matches(".*(기간|일자|날짜|일시|기간별).*") || a.isEmpty()) return new String[] { from.name, to.name };
		}
		return null;
	}

	// ------------------------------------------------------------------ helpers

	private static List<String> columnLabels(JSONObject grid) {
		List<String> labels = new ArrayList<String>();
		JSONArray columns = grid.optJSONArray("columns");
		if (columns == null) return labels;
		for (int c = 0; c < columns.length(); c++) {
			JSONObject column = columns.optJSONObject(c);
			if (column == null) continue;
			String editor = column.optString("editor", "output");
			String header = column.optString("header", "").trim();
			if (header.isEmpty() || "checkbox".equals(editor) || "rowindex".equals(editor) || "button".equals(editor)) continue;
			if (header.matches("(?i)(no\\.?|번호|순번|#)")) continue;
			if (!labels.contains(header)) labels.add(header);
		}
		return labels;
	}

	private static List<String> fieldLabels(JSONObject region) {
		List<String> labels = new ArrayList<String>();
		JSONArray fields = region.optJSONArray("fields");
		if (fields == null) return labels;
		for (int i = 0; i < fields.length(); i++) {
			JSONObject field = fields.optJSONObject(i);
			String label = field == null ? "" : field.optString("label", "").trim();
			if (!label.isEmpty() && !labels.contains(label)) labels.add(label);
		}
		return labels;
	}

	private static List<String> keywords(String screenName, String title) {
		List<String> words = new ArrayList<String>();
		for (String text : new String[] { screenName, title }) {
			for (String w : (text == null ? "" : text).toLowerCase(Locale.ROOT).split("[\\s/_\\-()]+")) { if (w.length() >= 2 && !words.contains(w)) words.add(w); }
		}
		return words;
	}

	private static String title(JSONObject region, String screenName) {
		String t = region.optString("title", "").trim();
		return t.isEmpty() ? screenName : t;
	}

	private static String scope(OpenApiSpec.Operation op) { return spec(op) + "|" + op.method + " " + op.path; }
	private static String spec(OpenApiSpec.Operation op) { return op.operationId.isEmpty() ? "" : op.operationId; }

	private static boolean containsName(List<OpenApiSpec.Property> list, String name) {
		for (OpenApiSpec.Property p : list) { if (p.name.equals(name)) return true; }
		return false;
	}

	private static List<String> captionsOf(JSONObject region) {
		List<String> captions = new ArrayList<String>();
		JSONArray buttons = region.optJSONArray("buttons");
		for (int i = 0; buttons != null && i < buttons.length(); i++) captions.add(buttons.optString(i, ""));
		return captions;
	}

	private static String firstCaption(JSONArray buttons, String pattern) {
		if (buttons == null) return "";
		for (int i = 0; i < buttons.length(); i++) {
			String caption = buttons.optString(i, "").trim();
			if (caption.replace(" ", "").toLowerCase(Locale.ROOT).matches(pattern)) return caption;
		}
		return "";
	}

	private static String captionMatching(List<String> captions, String pattern) {
		for (String caption : captions) { if (caption.replace(" ", "").toLowerCase(Locale.ROOT).matches(pattern)) return caption; }
		return "";
	}

	/** An exact "저장"/"삭제" first; a short caption containing the word ("행삭제") second; long business captions never. */
	private static String exactOrShortCaption(List<String> captions, String pattern) {
		String exact = captionMatching(captions, pattern);
		if (!exact.isEmpty()) return exact;
		for (String caption : captions) {
			String compact = caption.replace(" ", "").toLowerCase(Locale.ROOT);
			if (compact.length() <= 5 && compact.matches(".*" + pattern + ".*")) return caption;
		}
		return "";
	}
}
