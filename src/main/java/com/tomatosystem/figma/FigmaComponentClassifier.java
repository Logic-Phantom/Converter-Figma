package com.tomatosystem.figma;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.json.JSONObject;

/**
 * Decides what a Figma node is in eXBuilder6 terms. For an INSTANCE the component set name is tried first, then the
 * main component name, then the layer name: "Base-input" / "Button" / "Radio-button" identify the control even when
 * the layer is called "Frame 1000004302". Keywords live in figma/component-keywords.properties (overridable).
 */
final class FigmaComponentClassifier {
	static final String IGNORE = "ignore";
	static final String PAGINATION = "pagination";
	static final String BUTTON = "button";
	static final String CHECKBOX = "checkbox";
	static final String RADIO = "radiobutton";
	static final String INPUT = "inputbox";
	static final String TAB = "tab";

	/** Order matters: the first kind whose keyword occurs in the normalized name wins (radio before button). */
	private static final String[][] DEFAULTS = {
		{ IGNORE, "icon,breadcrumb,logo,divider,scrollbar,tooltip,cursor,avatar,badge,spinner-loading,chevron,arrow" },
		{ PAGINATION, "pagination,paging,pageindexer,pager,페이지네이션" },
		{ "daterange", "daterange,period,기간" },
		{ "dateinput", "dateinput,datepicker,date,calendar,달력,날짜,일자" },
		{ "combobox", "combobox,combo,select,dropdown,콤보,셀렉트,드롭다운" },
		{ "searchinput", "searchinput,searchfield,검색입력" },
		{ CHECKBOX, "checkbox,check,체크" },
		{ RADIO, "radio,라디오" },
		{ "textarea", "textarea,multiline,텍스트영역" },
		{ "numbereditor", "numbereditor,numberinput,stepper" },
		{ "maskeditor", "maskeditor,mask" },
		{ INPUT, "inputbox,input,textfield,textbox,입력" },
		{ BUTTON, "button,btn,버튼" } };

	/** Icons inside an input that turn it into another control (visible icons only). */
	private static final String[][] INPUT_ICONS = {
		{ "combobox", "arrowdown,chevrondown,dropdown,caretdown,selectarrow,expandmore" },
		{ "dateinput", "calendar,date,달력" },
		{ "searchinput", "search,magnifier,돋보기" } };

	private static final String GRID_NAMES = "table,grid,그리드,테이블";
	/** "table_register" / "등록 테이블" in Korean design systems is a label|control form laid out as a table. */
	private static final String FORM_TABLE_NAMES = "register,form,detail,등록,상세,입력폼";
	private static final String TITLE_NAMES = "title,pagetitle,appheader,타이틀,화면제목";

	private final Map<String, List<String>> keywords = new LinkedHashMap<String, List<String>>();
	private final FigmaDocument document;

	FigmaComponentClassifier(FigmaDocument document) {
		this.document = document;
		Properties overrides = loadOverrides();
		for (String[] kind : DEFAULTS) keywords.put(kind[0], split(overrides.getProperty("component." + kind[0], kind[1])));
	}

	/**
	 * Control kind of an INSTANCE/COMPONENT (or of a plain frame named like a control that has no nested controls),
	 * IGNORE for decoration, or null for a container to descend into.
	 */
	String classify(JSONObject node) {
		String type = FigmaNodes.type(node);
		if ("INSTANCE".equals(type) || "COMPONENT".equals(type)) {
			for (String name : new String[] { document.componentSetName(node), variantFree(document.componentName(node)), FigmaNodes.name(node) }) {
				if (isTabName(name)) return TAB;
				String kind = match(name);
				// A Radio-button whose circle is hidden is used as a read-only value label: descend to its text.
				if ((RADIO.equals(kind) || CHECKBOX.equals(kind)) && !hasShape(node) && !FigmaNodes.visibleTexts(node).isEmpty()) return null;
				if (kind != null) return INPUT.equals(kind) ? refineInput(node) : kind;
			}
			return null;
		}
		if ("FRAME".equals(type) || "GROUP".equals(type)) {
			String kind = match(FigmaNodes.name(node));
			if (kind == null || IGNORE.equals(kind)) return kind;
			// "Selectbox" frames that wrap a label and a Base-input are containers, not controls.
			int texts = FigmaNodes.visibleTexts(node).size();
			if (hasNestedControl(node) || texts > 1) return null;
			// A frame named "radio" that only holds a text (read-only value cell) is a text, not a choice.
			if ((RADIO.equals(kind) || CHECKBOX.equals(kind)) && texts == 1 && !hasShape(node)) return null;
			return INPUT.equals(kind) ? refineInput(node) : kind;
		}
		if ("RECTANGLE".equals(type)) {
			// Wireframes: a box named "InputBox-8" / "Button-3" stands for that control.
			String kind = match(FigmaNodes.name(node));
			return kind == null || PAGINATION.equals(kind) ? IGNORE : kind;
		}
		if ("VECTOR".equals(type) || "BOOLEAN_OPERATION".equals(type) || "STAR".equals(type) || "LINE".equals(type)
			|| "ELLIPSE".equals(type) || "REGULAR_POLYGON".equals(type)) return IGNORE;
		return null;
	}

	boolean isGridName(JSONObject node) {
		String name = FigmaNodes.normalize(FigmaNodes.name(node));
		return containsAny(name, GRID_NAMES) && !containsAny(name, FORM_TABLE_NAMES);
	}

	/** "Base-Tab", "TabItem", "탭" — but not "table". */
	private static boolean isTabName(String rawName) {
		String n = FigmaNodes.normalize(rawName);
		return n.matches(".*tab(?!le).*") && !n.contains("tabl") || n.contains("탭");
	}

	boolean isTitleName(JSONObject node) {
		String n = FigmaNodes.normalize(FigmaNodes.name(node));
		for (String k : TITLE_NAMES.split(",")) { if (n.startsWith(k)) return true; }
		return false;
	}

	/** A Base-input with a visible arrow-down icon is a combobox, with a calendar icon a dateinput. */
	private String refineInput(JSONObject node) {
		StringBuilder icons = new StringBuilder();
		collectIconNames(node, icons, true);
		String names = icons.toString();
		for (String[] rule : INPUT_ICONS) { if (containsAny(names, rule[1])) return rule[0]; }
		return INPUT;
	}

	private void collectIconNames(JSONObject node, StringBuilder out, boolean root) {
		if (!FigmaNodes.isVisible(node)) return;
		if (!root && ("INSTANCE".equals(FigmaNodes.type(node)) || "FRAME".equals(FigmaNodes.type(node)) || "GROUP".equals(FigmaNodes.type(node)))) {
			// Variant values carry the icon identity: "Type=arrow-down".
			out.append(FigmaNodes.normalize(FigmaNodes.name(node))).append('|')
				.append(FigmaNodes.normalize(document.componentName(node))).append('|');
		}
		for (JSONObject child : FigmaDocument.children(node)) collectIconNames(child, out, false);
	}

	private static boolean hasShape(JSONObject node) {
		for (JSONObject child : FigmaDocument.children(node)) {
			if (!FigmaNodes.isVisible(child)) continue;
			String t = FigmaNodes.type(child);
			if ("VECTOR".equals(t) || "ELLIPSE".equals(t) || "RECTANGLE".equals(t) || "BOOLEAN_OPERATION".equals(t)) return true;
			if (hasShape(child)) return true;
		}
		return false;
	}

	private boolean hasNestedControl(JSONObject node) {
		for (JSONObject child : FigmaDocument.children(node)) {
			if (!FigmaNodes.isVisible(child)) continue;
			String t = FigmaNodes.type(child);
			if ("INSTANCE".equals(t) || "COMPONENT".equals(t)) {
				String kind = classify(child);
				if (kind != null && !IGNORE.equals(kind)) return true;
			}
			if (hasNestedControl(child)) return true;
		}
		return false;
	}

	private String match(String rawName) {
		String name = FigmaNodes.normalize(rawName);
		if (name.isEmpty()) return null;
		for (Map.Entry<String, List<String>> entry : keywords.entrySet()) {
			for (String keyword : entry.getValue()) { if (name.contains(keyword)) return entry.getKey(); }
		}
		return null;
	}

	/** "Size=Medium, State=Default" names a variant, not a control; skip it for classification. */
	private static String variantFree(String componentName) { return componentName.contains("=") ? "" : componentName; }

	private static boolean containsAny(String normalized, String csv) {
		for (String k : csv.split(",")) { if (!k.isEmpty() && normalized.contains(FigmaNodes.normalize(k))) return true; }
		return false;
	}

	private static List<String> split(String csv) {
		List<String> result = new ArrayList<String>();
		for (String k : csv.split(",")) { String n = FigmaNodes.normalize(k); if (!n.isEmpty()) result.add(n); }
		return result;
	}

	private static Properties loadOverrides() {
		Properties p = new Properties();
		try (InputStream in = FigmaComponentClassifier.class.getClassLoader().getResourceAsStream("figma/component-keywords.properties")) {
			if (in != null) p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
		} catch (Exception ignored) { /* defaults apply */ }
		return p;
	}
}
