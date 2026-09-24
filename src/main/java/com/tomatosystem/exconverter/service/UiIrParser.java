package com.tomatosystem.exconverter.service;

import com.tomatosystem.exconverter.model.UiIr;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Parses the agreed UI-IR JSON contract. It intentionally has no CLX knowledge.
 * Local vision models are loose with naming, so types, components and editors
 * are normalized here instead of in the compiler.
 */
public final class UiIrParser {
	private static final int DEFAULT_WIDTH = 1440;
	private static final int DEFAULT_HEIGHT = 860;

	private UiIrParser() { }

	public static UiIr parse(String json) {
		JSONObject root = new JSONObject(stripFence(json));
		JSONObject screen = root.optJSONObject("screen");
		if (screen == null) screen = new JSONObject();
		UiIr result = new UiIr();
		result.setScreenName(firstNonBlank(screen.optString("name", ""), screen.optString("title", ""), "생성 화면"));
		result.setScreenType(screen.optString("type", root.optString("screenType", "")));
		result.setWidth(screen.optInt("width", DEFAULT_WIDTH));
		result.setHeight(screen.optInt("height", DEFAULT_HEIGHT));
		result.setSourceWidth(screen.optInt("sourceWidth", 0));
		if (result.getWidth() < 320 || result.getHeight() < 320) throw new IllegalArgumentException("screen width and height must be at least 320");
		JSONArray regions = root.optJSONArray("regions");
		// gemini-3.5-flash-lite nested regions inside screen (2026-09-18, docs/samples/user-role.gemini-3.5-flash-lite.ui-ir.json).
		if (regions == null) regions = screen.optJSONArray("regions");
		if (regions == null) throw new IllegalArgumentException("regions is required");
		for (int i = 0; i < regions.length(); i++) {
			JSONObject region = regions.optJSONObject(i);
			if (region == null) continue;
			UiIr.Region parsed = parseRegion(region);
			if (parsed != null) result.getRegions().add(parsed);
		}
		if (result.getRegions().isEmpty()) throw new IllegalArgumentException("UI-IR requires at least one region");
		parseApi(root.optJSONObject("api"), result);
		return UiIrNormalizer.normalize(result);
	}

	/** Backend binding written by the OpenAPI binder (v2.2): {"spec": "...", "submissions": [{id, action, method, role, caption, request, response, responsePath}]}. */
	private static void parseApi(JSONObject api, UiIr result) {
		if (api == null) return;
		result.setApiSource(api.optString("spec", ""));
		JSONArray submissions = api.optJSONArray("submissions");
		if (submissions == null) return;
		java.util.Set<String> ids = new java.util.HashSet<String>();
		for (int i = 0; i < submissions.length(); i++) {
			JSONObject s = submissions.optJSONObject(i);
			if (s == null) continue;
			String id = identifier(s.optString("id", ""));
			if (id.isEmpty() || !ids.add(id)) continue;
			UiIr.Submission submission = new UiIr.Submission(id);
			submission.setAction(s.optString("action", ""));
			submission.setMethod(s.optString("method", "get"));
			submission.setRole(s.optString("role", ""));
			submission.setCaption(s.optString("caption", ""));
			submission.setRequestDataId(identifier(s.optString("request", s.optString("requestDataId", ""))));
			submission.setResponseDataId(identifier(s.optString("response", s.optString("responseDataId", ""))));
			submission.setResponsePath(s.optString("responsePath", ""));
			submission.setOperationId(s.optString("operationId", ""));
			submission.setSummary(s.optString("summary", ""));
			if (submission.getAction().isEmpty()) continue;
			result.getSubmissions().add(submission);
		}
	}

	/** A CLX data control id / column name / DTO property; anything else is dropped rather than written into XML. */
	static String identifier(String raw) {
		String s = raw == null ? "" : raw.trim();
		return s.matches("[A-Za-z_$][A-Za-z0-9_$]{0,63}") ? s : "";
	}

	/** "from,to" for a daterange, otherwise one identifier; "" when invalid. */
	static String identifiers(String raw) {
		StringBuilder out = new StringBuilder();
		for (String part : (raw == null ? "" : raw).split(",")) {
			String id = identifier(part);
			if (id.isEmpty()) continue;
			if (out.length() > 0) out.append(',');
			out.append(id);
		}
		return out.toString();
	}

	private static UiIr.Region parseRegion(JSONObject json) {
		String type = normalizeRegionType(json.optString("type", ""));
		if (type == null) return null;
		UiIr.Region region = new UiIr.Region(type);
		region.setText(firstNonBlank(json.optString("text", ""), json.optString("value", "")));
		region.setTitle(json.optString("title", json.optString("label", "")).trim());
		region.setAlign(json.optString("align", ""));
		region.setColumnsPerRow(json.optInt("columnsPerRow", 0));
		region.setSide(normalizeSide(json.optString("side", json.optString("pane", ""))));
		region.setInTab(json.optBoolean("inTab", false));
		region.setDataId(identifier(json.optString("dataId", "")));
		region.setPaging(json.optBoolean("paging", json.optBoolean("pagination", false)));
		if (json.has("excel")) region.setExcel(json.optBoolean("excel", false));
		JSONArray fields = json.optJSONArray("fields");
		if (fields != null) {
			for (int i = 0; i < fields.length(); i++) {
				JSONObject f = fields.optJSONObject(i);
				if (f == null) continue;
				String label = f.optString("label", "").trim();
				if (label.isEmpty()) continue;
				UiIr.Field field = new UiIr.Field(label, normalizeComponent(f.optString("component", f.optString("type", ""))), f.optBoolean("required", false), f.optString("value", ""));
				field.setName(identifiers(f.optString("name", "")));
				JSONArray options = f.optJSONArray("options");
				if (options != null) { for (int o = 0; o < options.length(); o++) { String option = String.valueOf(options.get(o)).trim(); if (!option.isEmpty()) field.getOptions().add(option); } }
				region.getFields().add(field);
			}
		}
		JSONArray columns = json.optJSONArray("columns");
		if (columns != null) {
			for (int i = 0; i < columns.length(); i++) {
				Object item = columns.get(i);
				if (item instanceof JSONObject) {
					JSONObject c = (JSONObject) item;
					String header = firstNonBlank(c.optString("header", ""), c.optString("label", ""), c.optString("text", ""));
					String cellText = c.optString("cellText", "");
					String editor = normalizeEditor(c.optString("editor", c.optString("component", "")));
					if ("output".equals(editor) && isSequenceHeader(header) && cellText.matches("[#0-9]*")) editor = "rowindex";
					UiIr.Column column = new UiIr.Column(header, editor, Math.max(0, c.optInt("width", 0)), cellText);
					column.setName(identifier(c.optString("name", "")));
					column.setDataType(c.optString("dataType", c.optString("datatype", "")).trim().toLowerCase(Locale.ROOT).matches("number|date|decimal") ? c.optString("dataType", c.optString("datatype", "")).trim().toLowerCase(Locale.ROOT) : "");
					region.getColumns().add(column);
				} else {
					String header = String.valueOf(item).trim();
					if (!header.isEmpty()) region.getColumns().add(new UiIr.Column(header, isSequenceHeader(header) ? "rowindex" : "output", 0, ""));
				}
			}
		}
		collapseRepeatedColumns(region.getColumns());
		addButtons(json.optJSONArray("buttons"), region);
		addButtons(json.optJSONArray("actions"), region);
		JSONArray tabs = json.optJSONArray("tabs");
		if (tabs != null) { for (int i = 0; i < tabs.length(); i++) { String tab = tabs.optString(i, "").trim(); if (!tab.isEmpty()) region.getTabs().add(tab); } }
		if (UiIr.GRID.equals(type) && region.getColumns().isEmpty()) return null;
		if ((UiIr.SEARCH.equals(type) || UiIr.FORM.equals(type)) && region.getFields().isEmpty() && region.getButtons().isEmpty()) return null;
		if (UiIr.BUTTONS.equals(type) && region.getButtons().isEmpty()) return null;
		if ((UiIr.DESCRIPTION.equals(type) || UiIr.SECTION_TITLE.equals(type) || UiIr.TITLE.equals(type)) && region.getText().isEmpty() && region.getTitle().isEmpty()) return null;
		return region;
	}

	private static void addButtons(JSONArray buttons, UiIr.Region region) {
		if (buttons == null) return;
		for (int i = 0; i < buttons.length(); i++) {
			Object item = buttons.get(i);
			String label = item instanceof JSONObject ? firstNonBlank(((JSONObject) item).optString("label", ""), ((JSONObject) item).optString("text", "")) : String.valueOf(item).trim();
			if ("reset".equalsIgnoreCase(label)) label = "초기화";
			if ("search".equalsIgnoreCase(label)) label = "조회";
			if (!label.isEmpty()) region.getButtons().add(label);
		}
	}

	/** left/right pane of a split screen; anything else (full, center, "") means full width. */
	static String normalizeSide(String raw) {
		String s = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
		if (s.equals("left") || s.equals("l") || s.equals("왼쪽") || s.equals("좌") || s.equals("좌측")) return UiIr.LEFT;
		if (s.equals("right") || s.equals("r") || s.equals("오른쪽") || s.equals("우") || s.equals("우측")) return UiIr.RIGHT;
		return "";
	}

	static String normalizeRegionType(String raw) {
		String t = raw.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
		if (t.isEmpty()) return null;
		if (t.equals("title") || t.equals("pagetitle") || t.equals("header")) return UiIr.TITLE;
		if (t.contains("description") || t.equals("text") || t.equals("paragraph") || t.equals("notice") || t.equals("info")) return UiIr.DESCRIPTION;
		if (t.contains("sectiontitle") || t.equals("section") || t.equals("subtitle") || t.equals("heading")) return UiIr.SECTION_TITLE;
		if (t.contains("search") || t.contains("filter") || t.contains("condition")) return UiIr.SEARCH;
		if (t.contains("form") || t.contains("detail")) return UiIr.FORM;
		if (t.contains("grid") || t.contains("table") || t.equals("list")) return UiIr.GRID;
		if (t.contains("tab")) return UiIr.TABS;
		if (t.contains("tree")) return UiIr.TREE;
		if (t.contains("button") || t.contains("footer") || t.contains("action")) return UiIr.BUTTONS;
		if (t.contains("textarea") || t.contains("memo")) return UiIr.TEXTAREA;
		return null;
	}

	static String normalizeComponent(String raw) {
		String c = raw.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
		if (c.contains("range") || c.contains("period") || c.equals("fromto")) return "daterange";
		if (c.contains("date") || c.contains("calendar")) return "dateinput";
		if (c.contains("combo") || c.contains("select") || c.contains("dropdown")) return "combobox";
		if (c.contains("searchinput") || c.equals("search") || c.contains("popup")) return "searchinput";
		if (c.contains("checkboxgroup") || c.contains("checkgroup") || c.contains("multicheck")) return "checkboxgroup";
		if (c.contains("checkbox") || c.equals("check")) return "checkbox";
		if (c.contains("radio")) return "radiobutton";
		if (c.contains("number") || c.contains("spin")) return "numbereditor";
		if (c.contains("mask")) return "maskeditor";
		if (c.contains("textarea")) return "textarea";
		if (c.equals("output") || c.equals("label") || c.equals("text")) return "output";
		return "inputbox";
	}

	static String normalizeEditor(String raw) {
		String c = raw.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
		if (c.isEmpty() || c.equals("output") || c.equals("text") || c.equals("label") || c.equals("readonly")) return "output";
		if (c.contains("rowindex") || c.contains("rownum") || c.equals("index") || c.equals("no") || c.contains("sequence")) return "rowindex";
		if (c.contains("button")) return "button";
		String component = normalizeComponent(raw);
		return "daterange".equals(component) ? "dateinput" : component;
	}

	/**
	 * Small vision models sometimes list every data row as extra columns
	 * (A,B,C,A,B,C,...). When the header sequence repeats with a fixed period, keep one period.
	 */
	static void collapseRepeatedColumns(java.util.List<UiIr.Column> columns) {
		int n = columns.size();
		for (int period = 1; period <= n / 2; period++) {
			if (n % period != 0) continue;
			boolean repeats = true;
			for (int i = period; i < n && repeats; i++) repeats = columns.get(i).getHeader().equals(columns.get(i - period).getHeader());
			if (repeats) { columns.subList(period, n).clear(); return; }
		}
	}

	private static boolean isSequenceHeader(String header) {
		return header.trim().matches("(?i)(번호|순번|no\\.?|#)");
	}

	private static String stripFence(String json) {
		String text = json == null ? "" : json.trim();
		if (text.startsWith("```")) {
			int firstNewline = text.indexOf('\n');
			int lastFence = text.lastIndexOf("```");
			if (firstNewline >= 0 && lastFence > firstNewline) text = text.substring(firstNewline + 1, lastFence).trim();
		}
		int start = text.indexOf('{');
		int end = text.lastIndexOf('}');
		return start >= 0 && end > start ? text.substring(start, end + 1) : text;
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) { if (value != null && !value.trim().isEmpty()) return value.trim(); }
		return "";
	}
}
