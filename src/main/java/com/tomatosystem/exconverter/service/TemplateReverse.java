package com.tomatosystem.exconverter.service;

import com.tomatosystem.exconverter.model.UiIr;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Reads a template CLX (or a generated one) back into the UI-IR that a perfect analysis of that screen's image
 * would return: regions in visual order, side for left/right panes, inTab for tab content, paging, title-row
 * buttons and shuttle arrows. Titles, captions and labels are placeholders.
 *
 * This is how the template repository checks itself: a template is healthy when that ideal UI-IR selects a
 * template of the same structure and the generator rebuilds the same structure from it
 * ({@link TemplateInspector}). It also reports controls the generator does not know, which it would drop.
 */
public final class TemplateReverse {
	static final String CL = "http://tomatosystem.co.kr/cleopatra";
	/** Controls the generator understands or rebuilds; anything else inside the body is reported. */
	private static final Set<String> KNOWN = new HashSet<String>(Arrays.asList(
		"group", "grid", "tree", "textarea", "tabfolder", "tabitem", "output", "button", "inputbox", "combobox", "dateinput",
		"searchinput", "checkbox", "checkboxgroup", "radiobutton", "numbereditor", "maskeditor", "pageindexer", "udc", "img", "image", "accordion", "sectionitem"));
	/** Region kinds UI-IR cannot describe, so an image of them can never be matched (P3-4, P8). */
	private static final Set<String> INEXPRESSIBLE = new HashSet<String>(Arrays.asList("uicontrolshell", "embeddedpage", "accordion"));

	private TemplateReverse() { }

	public static final class Result {
		public final UiIr ir = new UiIr();
		/** Why the screen cannot be expressed as UI-IR, or null. */
		public String unsupported;
		/** Unknown cl: elements (and UDC types) inside the body; the generator drops them. */
		public final Set<String> unknownControls = new TreeSet<String>();
		public boolean usable;
	}

	public static Result analyze(byte[] clx, String fileName) throws Exception {
		Document doc = parse(clx);
		Element body = (Element) doc.getElementsByTagNameNS("*", "body").item(0);
		Result result = new Result();
		UiIr ir = result.ir;
		ir.setScreenName("화면");
		ir.setWidth(1440); ir.setHeight(860); ir.setSourceWidth(1408);
		if (body == null) { result.unsupported = "body 없음"; return result; }
		boolean popup = fileName.matches(".*_P\\.clx$") || !groupsByClass(body, "pop-content-body").isEmpty();
		ir.setScreenType(popup ? "POPUP" : "");
		UiIr.Region title = new UiIr.Region(UiIr.TITLE); title.setText("화면"); ir.getRegions().add(title);

		Element data = firstGroupByClass(body, "content-body", "pop-content-body");
		for (Element search : groupsByClass(body, "search-box")) {
			if (data == null || !isAncestor(data, search)) { ir.getRegions().add(searchRegion(search)); break; }
		}
		if (data == null) { result.unsupported = "content-body/pop-content-body 없음 (생성기가 본문을 넣을 곳이 없음)"; return result; }
		result.usable = true;
		collectUnknown(data, result.unknownControls);
		for (String name : INEXPRESSIBLE) {
			if (data.getElementsByTagNameNS("*", name).getLength() > 0) result.unsupported = "UI-IR 로 표현할 수 없는 컨트롤 " + name;
		}
		walk(data, "", false, ir.getRegions());
		Element footer = firstGroupByClass(body, "content-footer", "pop-content-footer");
		if (footer != null && !groupsByClass(footer, "footer-button-group").isEmpty()) {
			UiIr.Region buttons = new UiIr.Region(UiIr.BUTTONS);
			buttons.getButtons().add("저장");
			ir.getRegions().add(buttons);
		}
		return result;
	}

	/**
	 * Namespace-aware parse that reports malformed XML as "XML 오류 (줄 N, 열 M): ..." instead of printing
	 * "[Fatal Error]" to stderr, which the JDK's default error handler does on every attempt.
	 */
	public static Document parse(byte[] xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
		builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler());
		try {
			return builder.parse(new ByteArrayInputStream(xml));
		} catch (org.xml.sax.SAXParseException e) {
			throw new IllegalArgumentException("XML 오류 (줄 " + e.getLineNumber() + ", 열 " + e.getColumnNumber() + "): " + e.getMessage(), e);
		}
	}

	/** Structure only: titles, captions and field labels are placeholders and are ignored. */
	public static String structureKey(UiIr ir) {
		StringBuilder sb = new StringBuilder(ir.getScreenType().contains("POPUP") ? "popup " : "page ");
		for (UiIr.Region r : ir.getRegions()) {
			if (UiIr.TITLE.equals(r.getType())) continue;
			sb.append(r.getType());
			if (!r.getSide().isEmpty()) sb.append('@').append(r.getSide().charAt(0));
			if (r.isInTab()) sb.append("^tab");
			if (r.isPaging()) sb.append("+pg");
			if (UiIr.BUTTONS.equals(r.getType())) {
				sb.append('(');
				for (String b : r.getButtons()) sb.append(LayoutShape.isArrow(b) ? b : "b");
				sb.append(')');
			} else if (!r.getButtons().isEmpty()) sb.append("+btn");
			sb.append(' ');
		}
		return sb.toString().trim();
	}

	// ------------------------------------------------------------------ body walk

	private static void walk(Element container, String side, boolean inTab, List<UiIr.Region> out) {
		for (Element child : ordered(container)) {
			String name = child.getLocalName();
			if ("grid".equals(name)) { out.add(gridRegion(child, side, inTab)); continue; }
			if ("tree".equals(name)) {
				UiIr.Region tree = tagged(new UiIr.Region(UiIr.TREE), side, inTab);
				tree.setTitle("트리");
				addTitleButtons((Element) child.getParentNode(), tree);
				out.add(tree);
				continue;
			}
			if ("textarea".equals(name)) { out.add(tagged(new UiIr.Region(UiIr.TEXTAREA), side, inTab)); continue; }
			if ("tabfolder".equals(name)) {
				UiIr.Region tabs = tagged(new UiIr.Region(UiIr.TABS), side, inTab);
				List<Element> items = children(child, "tabitem");
				for (Element item : items) tabs.getTabs().add(item.getAttribute("text"));
				out.add(tabs);
				if (!items.isEmpty()) walk(items.get(0), side, true, out);
				continue;
			}
			if (!"group".equals(name)) {
				if ("accordion".equals(name) || "sectionitem".equals(name)) walk(child, side, inTab, out);
				continue;
			}
			if (hasClass(child, "content-title-box") || hasClass(child, "title-button-group")) continue;
			if (hasClass(child, "form-base")) { out.add(formRegion(child, side, inTab)); continue; }
			if (hasClass(child, "search-box")) { out.add(tagged(searchRegion(child), side, inTab)); continue; }
			if (hasClass(child, "shuttle-button-group")) { out.add(tagged(arrowButtons(child), side, inTab)); continue; }
			if (hasClass(child, "division-group")) { walkDivision(child, inTab, out); continue; }
			walk(child, side, inTab, out);
		}
	}

	/** Left pane, then (shuttle column), then right pane, as the prompt asks the model to list them. */
	private static void walkDivision(Element division, boolean inTab, List<UiIr.Region> out) {
		Element layout = child(division, "formlayout");
		int columns = Math.max(1, layout == null ? 2 : children(layout, "columns").size());
		Map<Integer, List<Element>> byCol = new TreeMap<Integer, List<Element>>();
		for (Element child : ordered(division)) byCol.computeIfAbsent(Math.min(columns - 1, formData(child, "col")), k -> new ArrayList<Element>()).add(child);
		for (int c = 0; c < columns; c++) {
			boolean shuttle = columns == 3 && c == 1;
			String side = shuttle ? "" : (c == 0 ? UiIr.LEFT : UiIr.RIGHT);
			for (Element e : byCol.getOrDefault(c, new ArrayList<Element>())) {
				Element holder = e.getOwnerDocument().createElementNS(CL, "cl:group");
				holder.appendChild(e.cloneNode(true));
				walk(holder, side, inTab, out);
			}
		}
	}

	private static UiIr.Region gridRegion(Element grid, String side, boolean inTab) {
		UiIr.Region region = tagged(new UiIr.Region(UiIr.GRID), side, inTab);
		int columns = children(grid, "gridcolumn").size();
		for (int i = 0; i < Math.max(1, columns); i++) region.getColumns().add(new UiIr.Column("컬럼" + (i + 1), "output", 0, ""));
		Element content = (Element) grid.getParentNode();
		region.setTitle("목록");
		addTitleButtons(content, region);
		region.setPaging(!children(content, "pageindexer").isEmpty());
		return region;
	}

	private static UiIr.Region formRegion(Element form, String side, boolean inTab) {
		UiIr.Region region = tagged(new UiIr.Region(UiIr.FORM), side, inTab);
		int labels = countLabels(form);
		for (int i = 0; i < Math.max(1, labels); i++) region.getFields().add(new UiIr.Field("항목" + (i + 1), "inputbox", false));
		Element layout = child(form, "formlayout");
		int cols = layout == null ? 6 : children(layout, "columns").size();
		region.setColumnsPerRow(Math.max(1, Math.min(6, cols / 2)));
		region.setTitle("상세");
		addTitleButtons((Element) form.getParentNode(), region);
		return region;
	}

	private static UiIr.Region searchRegion(Element search) {
		UiIr.Region region = new UiIr.Region(UiIr.SEARCH);
		int labels = countLabels(search);
		for (int i = 0; i < Math.max(1, labels); i++) region.getFields().add(new UiIr.Field("조건" + (i + 1), "inputbox", false));
		region.getButtons().add("조회");
		return region;
	}

	private static UiIr.Region arrowButtons(Element group) {
		UiIr.Region region = new UiIr.Region(UiIr.BUTTONS);
		region.setAlign("center");
		for (Element button : children(group, "button")) {
			String cls = button.getAttribute("class");
			region.getButtons().add(cls.contains("btn-up") ? "▲" : cls.contains("btn-down") ? "▼" : cls.contains("btn-left") ? "◀" : cls.contains("btn-right") ? "▶" : firstNonBlank(button.getAttribute("value"), "버튼"));
		}
		return region;
	}

	/** Title-row buttons of the content that holds a grid/form/tree: template placeholders stand for real buttons. */
	private static void addTitleButtons(Element content, UiIr.Region region) {
		for (Element box : children(content, "group")) {
			if (!hasClass(box, "content-title-box")) continue;
			for (Element group : children(box, "group")) {
				if (!hasClass(group, "title-button-group")) continue;
				for (Element button : children(group, "button")) region.getButtons().add(firstNonBlank(button.getAttribute("value"), "버튼"));
			}
		}
	}

	private static UiIr.Region tagged(UiIr.Region region, String side, boolean inTab) { region.setSide(side); region.setInTab(inTab); return region; }

	private static void collectUnknown(Element root, Set<String> unknown) {
		NodeList all = root.getElementsByTagNameNS(CL, "*");
		for (int i = 0; i < all.getLength(); i++) {
			Element e = (Element) all.item(i);
			String name = e.getLocalName();
			if (isStructural(e)) continue;
			if ("udc".equals(name)) {
				String type = e.getAttribute("type");
				if (!type.endsWith("udcComGridTitle") && !type.endsWith("udcComFormTitle")) unknown.add("udc:" + type);
				continue;
			}
			if (!KNOWN.contains(name)) unknown.add(name);
		}
	}

	/** Layouts, layout data and the insides of grids and UDCs are not controls. */
	private static boolean isStructural(Element e) {
		String n = e.getLocalName();
		if (n.endsWith("layout") || n.endsWith("data") || "rows".equals(n) || "columns".equals(n) || "property".equals(n) || "listener".equals(n)) return true;
		if ((n.startsWith("grid") && !"grid".equals(n)) || "item".equals(n) || "itemset".equals(n) || n.endsWith("bind")) return true;
		for (Node p = e.getParentNode(); p instanceof Element; p = p.getParentNode()) {
			String pn = p.getLocalName();
			if ("grid".equals(pn) || "udc".equals(pn) || "combobox".equals(pn) || "radiobutton".equals(pn) || "checkboxgroup".equals(pn) || "tree".equals(pn)) return true;
		}
		return false;
	}

	// ------------------------------------------------------------------ DOM helpers

	private static List<Element> ordered(Element parent) {
		List<Element> list = new ArrayList<Element>();
		for (Element e : children(parent, null)) { if (CL.equals(e.getNamespaceURI()) && !isStructural(e)) list.add(e); }
		list.sort(Comparator.comparingInt((Element e) -> formData(e, "row")).thenComparingInt(e -> formData(e, "col")));
		return list;
	}

	private static int formData(Element e, String name) {
		Element fd = child(e, "formdata");
		if (fd == null) return 0;
		try { return Integer.parseInt(fd.getAttribute(name)); } catch (NumberFormatException ex) { return 0; }
	}

	private static List<Element> children(Element parent, String localName) {
		List<Element> list = new ArrayList<Element>();
		NodeList nodes = parent.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node n = nodes.item(i);
			if (n instanceof Element && (localName == null || localName.equals(n.getLocalName()))) list.add((Element) n);
		}
		return list;
	}

	private static Element child(Element parent, String localName) { List<Element> l = children(parent, localName); return l.isEmpty() ? null : l.get(0); }

	private static int countLabels(Element root) {
		int n = 0;
		NodeList outputs = root.getElementsByTagNameNS("*", "output");
		for (int i = 0; i < outputs.getLength(); i++) { if (((Element) outputs.item(i)).getAttribute("class").matches(".*\\blabel\\b.*")) n++; }
		return n;
	}

	private static List<Element> groupsByClass(Element root, String cls) {
		List<Element> list = new ArrayList<Element>();
		NodeList groups = root.getElementsByTagNameNS("*", "group");
		for (int i = 0; i < groups.getLength(); i++) { if (hasClass((Element) groups.item(i), cls)) list.add((Element) groups.item(i)); }
		return list;
	}

	private static Element firstGroupByClass(Element root, String... classes) {
		for (String cls : classes) { List<Element> l = groupsByClass(root, cls); if (!l.isEmpty()) return l.get(0); }
		return null;
	}

	private static boolean hasClass(Element e, String cls) {
		for (String c : e.getAttribute("class").trim().split("\\s+")) { if (c.equals(cls)) return true; }
		return false;
	}

	private static boolean isAncestor(Element ancestor, Node node) {
		for (Node n = node.getParentNode(); n != null; n = n.getParentNode()) { if (n == ancestor) return true; }
		return false;
	}

	private static String firstNonBlank(String... values) {
		for (String v : values) { if (v != null && !v.trim().isEmpty()) return v.trim(); }
		return "";
	}
}
