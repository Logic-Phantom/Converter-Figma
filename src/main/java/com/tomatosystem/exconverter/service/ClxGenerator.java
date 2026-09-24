package com.tomatosystem.exconverter.service;

import com.tomatosystem.exconverter.model.UiIr;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Deterministic UI-IR → CLX compiler.
 *
 * The selected template supplies the skeleton: responsive screens, udcComAppHeader,
 * content-header/search-box, content-body/content, content-footer and their style classes
 * and spacing. Everything inside those containers is rebuilt from the UI-IR regions, so the
 * number of search fields, grids, grid columns and buttons follows the design image
 * instead of the template's placeholder controls.
 */
public class ClxGenerator {
	static final String CL = "http://tomatosystem.co.kr/cleopatra";
	static final String STD = "http://tomatosystem.co.kr/cleopatra/studio";

	public byte[] generate(File template, UiIr ir) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(Files.readAllBytes(template.toPath())));
			new Compiler(doc, ir).compile();
			return serialize(doc);
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("CLX generation failed: " + e.getMessage(), e);
		}
	}

	private static byte[] serialize(Document doc) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		output.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		transformer.transform(new DOMSource(doc), new StreamResult(output));
		return output.toByteArray();
	}

	/** One compilation; holds id/sid generators so every generated node is unique. */
	private static final class Compiler {
		private static final int CONTENT_WIDTH = 1408;
		private static final int FIELDS_PER_ROW = 3;
		private static final Map<String, String> SID_PREFIX = new HashMap<String, String>();
		private static final Map<String, String> ID_PREFIX = new HashMap<String, String>();
		static {
			String[][] sids = { { "output", "output" }, { "inputbox", "i-box" }, { "dateinput", "d-input" }, { "combobox", "c-box" },
				{ "searchinput", "s-input" }, { "button", "button" }, { "group", "group" }, { "grid", "grid" }, { "gridcolumn", "g-column" },
				{ "gridheader", "gh-band" }, { "griddetail", "gd-band" }, { "gridrow", "g-row" }, { "formdata", "f-data" },
				{ "flowlayoutdata", "f-data" }, { "verticaldata", "v-data" }, { "formlayout", "f-layout" }, { "flowlayout", "f-layout" },
				{ "verticallayout", "v-layout" }, { "udc", "ud-control" }, { "dataset", "d-set" }, { "datacolumn", "d-column" },
				{ "textarea", "t-area" }, { "checkbox", "c-box" }, { "radiobutton", "r-button" }, { "numbereditor", "n-editor" },
				{ "maskeditor", "m-editor" }, { "tabfolder", "t-folder" }, { "tabitem", "t-item" }, { "tree", "tree" }, { "pageindexer", "p-indexer" }, { "checkboxgroup", "cb-group" }, { "item", "item" },
				{ "listener", "listener" }, { "datamap", "d-map" }, { "submission", "submission" } };
			for (String[] s : sids) SID_PREFIX.put(s[0], s[1]);
			String[][] ids = { { "output", "opt" }, { "inputbox", "ipb" }, { "dateinput", "dti" }, { "combobox", "cmb" }, { "searchinput", "sipb" },
				{ "button", "btn" }, { "grid", "grd" }, { "textarea", "txa" }, { "checkbox", "cbx" }, { "radiobutton", "rdb" },
				{ "numbereditor", "nbe" }, { "maskeditor", "mse" }, { "tree", "tre" }, { "tabfolder", "tab" }, { "pageindexer", "pix" }, { "checkboxgroup", "cbg" } };
			for (String[] s : ids) ID_PREFIX.put(s[0], s[1]);
		}

		private final Document doc;
		private final UiIr ir;
		private final SecureRandom random = new SecureRandom();
		private final Set<String> sids = new HashSet<String>();
		private final Set<String> ids = new HashSet<String>();
		private Element model;
		/** Grids and forms built so far, across panes, for the fallback titles (screen name, then "목록 N" / "상세 N"). */
		private int gridIndex;
		private int formIndex;
		private final List<Element> gridPrototypes = new ArrayList<Element>();
		private final List<Element> formPrototypes = new ArrayList<Element>();
		private final List<Element> divisionPrototypes = new ArrayList<Element>();
		private Element treePrototype;
		private Element bodySearchPrototype;
		/** Backend binding (v2.2): DataMaps created for bound search/form regions and the handler names handed out. */
		private final Map<UiIr.Region, String> dataMapIds = new HashMap<UiIr.Region, String>();
		private final Map<String, Element> dataMapColumnLists = new LinkedHashMap<String, Element>();
		private final Set<String> functionNames = new HashSet<String>();
		private String searchDataMapId;

		Compiler(Document doc, UiIr ir) { this.doc = doc; this.ir = ir; }

		void compile() {
			ir.getHandlers().clear();
			stripWhitespace(doc.getDocumentElement());
			Element body = firstByLocalName(doc.getDocumentElement(), "body");
			if (body == null) throw new IllegalStateException("Template has no body");
			model = firstByLocalName(doc.getDocumentElement(), "model");
			Element appHeader = findUdc(body, "udcComAppHeader");
			Element header = findGroup(body, "grpHeader", "content-header", "pop-content-header");
			Element search = findGroup(body, "grpSearch", "search-box", null);
			Element data = findGroup(body, "grpData", "content-body", "pop-content-body");
			Element footer = findGroup(body, "grpFooter", "content-footer", "pop-content-footer");
			if (data != null) collectPrototypes(data);

			// Split regions by where the eXBuilder6 skeleton expects them.
			List<UiIr.Region> headerRegions = new ArrayList<UiIr.Region>();
			List<UiIr.Region> bodyRegions = new ArrayList<UiIr.Region>();
			UiIr.Region searchRegion = null;
			UiIr.Region footerRegion = null;
			String title = ir.getScreenName();
			List<UiIr.Region> regions = new ArrayList<UiIr.Region>(ir.getRegions());
			if (!regions.isEmpty() && UiIr.BUTTONS.equals(regions.get(regions.size() - 1).getType()) && footer != null) footerRegion = regions.remove(regions.size() - 1);
			for (java.util.Iterator<UiIr.Region> it = regions.iterator(); it.hasNext();) {
				UiIr.Region region = it.next();
				if (UiIr.TITLE.equals(region.getType())) { title = firstNonBlank(region.getText(), region.getTitle(), title); it.remove(); }
			}
			int searchIndex = -1;
			// The header search bar is the first full-width search; one inside a pane stays in the body.
			if (search != null) { for (int i = 0; i < regions.size() && searchIndex < 0; i++) { if (UiIr.SEARCH.equals(regions.get(i).getType()) && regions.get(i).getSide().isEmpty()) searchIndex = i; } }
			if (searchIndex >= 0) searchRegion = regions.get(searchIndex);
			// Explanatory text directly above the search box belongs to content-header; anything else keeps visual order in the body.
			boolean headerPrefix = searchIndex > 0 && header != null;
			for (int i = 0; i < searchIndex && headerPrefix; i++) headerPrefix = isHeaderFriendly(regions.get(i));
			for (int i = 0; i < regions.size(); i++) {
				if (i == searchIndex) continue;
				if (headerPrefix && i < searchIndex) headerRegions.add(regions.get(i));
				else bodyRegions.add(regions.get(i));
			}

			pruneGenerated(body, search, data, footer);
			collectExisting(doc.getDocumentElement());

			if (appHeader != null) setUdcProperty(appHeader, "title", title);
			if (searchRegion != null) buildSearch(search, searchRegion, true);
			else if (search != null) search.getParentNode().removeChild(search);
			if (header != null) buildHeaderExtras(header, search, headerRegions);
			if (data != null) buildBody(data, bodyRegions);
			else if (!bodyRegions.isEmpty()) ir.getWarnings().add("Template has no content-body; " + bodyRegions.size() + " regions skipped");
			if (footer != null) {
				if (footerRegion != null) buildFooter(footer, footerRegion);
				else footer.getParentNode().removeChild(footer);
			}
			if (header != null && childElements(header, true).isEmpty()) header.getParentNode().removeChild(header);
			buildSubmissions(body);
		}

		// ------------------------------------------------------------------ skeleton handling

		private boolean isHeaderFriendly(UiIr.Region region) {
			String type = region.getType();
			return UiIr.DESCRIPTION.equals(type) || UiIr.SECTION_TITLE.equals(type);
		}

		/** Removes the template's placeholder controls; containers and their layout rules stay. */
		private void pruneGenerated(Element body, Element search, Element data, Element footer) {
			if (search != null) removeControls(search);
			if (data != null) removeControls(data);
			if (footer != null) {
				for (Element buttons : descendantsByClass(footer, "footer-button-group")) {
					for (Element side : childElements(buttons, true)) removeControls(side);
				}
			}
		}

		/**
		 * Copies of the template's body building blocks, taken before the placeholders are pruned: every
		 * content group holding a grid, a form-base or a tree (with its title box, title-row buttons and page
		 * indexer), the body search bar (P2-3) and the division-groups (pane ratio, shuttle column).
		 */
		private void collectPrototypes(Element data) {
			for (Element group : descendantsByLocalName(data, "group")) {
				if (hasClass(group, "division-group")) { divisionPrototypes.add((Element) group.cloneNode(true)); continue; }
				if (hasClass(group, "search-box")) { if (bodySearchPrototype == null) bodySearchPrototype = (Element) group.cloneNode(true); continue; }
				if (!hasClass(group, "content")) continue;
				if (firstChildByLocalName(group, "grid") != null) gridPrototypes.add((Element) group.cloneNode(true));
				else if (firstChildByLocalName(group, "tree") != null) { if (treePrototype == null) treePrototype = (Element) group.cloneNode(true); }
				else { for (Element child : childElements(group, true)) { if (hasClass(child, "form-base")) { formPrototypes.add((Element) group.cloneNode(true)); break; } } }
			}
		}

		/** The prototype whose title-row buttons (and, for grids, page indexer) match the region; the first on a tie. */
		private Element choose(List<Element> prototypes, UiIr.Region region) {
			Element best = null;
			int bestScore = -1;
			for (Element prototype : prototypes) {
				int score = (LayoutShape.hasTitleButtons(prototype) == !region.getButtons().isEmpty() ? 2 : 0)
					+ (descendantsByLocalName(prototype, "pageindexer").isEmpty() != region.isPaging() ? 1 : 0);
				if (score > bestScore) { best = prototype; bestScore = score; }
			}
			return best;
		}

		/** Fresh copy of a prototype for one use: own layout data removed, new ids and sids. */
		private Element instantiate(Element prototype) {
			Element copy = (Element) prototype.cloneNode(true);
			stripLayoutData(copy);
			reassign(copy);
			return copy;
		}

		private void buildHeaderExtras(Element header, Element search, List<UiIr.Region> regions) {
			Element anchor = search != null && search.getParentNode() == header ? search : layoutOf(header);
			for (UiIr.Region region : regions) {
				Element control = buildRegion(region);
				if (control == null) continue;
				control.insertBefore(verticalData(CONTENT_WIDTH, heightOf(region), false), control.getFirstChild());
				header.insertBefore(control, anchor);
			}
			int total = 0;
			List<Element> children = childElements(header, true);
			for (Element child : children) {
				Element vd = firstChildByLocalName(child, "verticaldata");
				total += vd == null ? 24 : parsePx(vd.getAttribute("height"), 24);
			}
			Element vd = firstChildByLocalName(header, "verticaldata");
			if (vd != null) vd.setAttribute("height", (total + Math.max(0, children.size() - 1) * 12) + "px");
		}

		/**
		 * Stacks the body regions top to bottom. The run from the first to the last region that has a side
		 * becomes one division-group row with a left and a right pane; regions before and after it span the width.
		 */
		private void buildBody(Element data, List<UiIr.Region> regions) {
			Element layout = layoutOf(data);
			int first = -1;
			int last = -1;
			for (int i = 0; i < regions.size(); i++) { if (!regions.get(i).getSide().isEmpty()) { if (first < 0) first = i; last = i; } }
			List<Placed> placed = new ArrayList<Placed>();
			if (first < 0) placed.addAll(placeAll(regions));
			else {
				placed.addAll(placeAll(regions.subList(0, first)));
				placed.add(buildDivision(regions.subList(first, last + 1)));
				placed.addAll(placeAll(regions.subList(last + 1, regions.size())));
			}
			Stack stack = new Stack();
			for (Placed p : placed) {
				p.control.insertBefore(formData(stack.rows.size(), 0), p.control.getFirstChild());
				data.insertBefore(p.control, layout);
				stack.add(p);
			}
			Element newLayout = formLayout(stack.rows(), cols(new String[] { "1", "FRACTION" }), 12, 12);
			if (layout != null) data.replaceChild(newLayout, layout); else data.appendChild(newLayout);
			Element vd = firstChildByLocalName(data, "verticaldata");
			if (vd != null) vd.setAttribute("height", Math.max(parsePx(vd.getAttribute("height"), 650), stack.needed) + "px");
		}

		/** A built region and the formlayout row it needs: 1fr when flexible (grid/tabs/tree), else its pixel height. */
		private static final class Placed {
			final Element control; final boolean flexible; final int height;
			Placed(Element control, boolean flexible, int height) { this.control = control; this.flexible = flexible; this.height = height; }
		}

		/** Rows of a vertical formlayout (12px gaps) and the height they need, counting 260px per flexible row. */
		private static final class Stack {
			private final List<String[]> rows = new ArrayList<String[]>();
			int needed;
			boolean flexible;
			void add(Placed placed) {
				rows.add(placed.flexible ? new String[] { "1", "FRACTION" } : new String[] { String.valueOf(placed.height), "PIXEL" });
				needed += placed.height + (rows.size() > 1 ? 12 : 0);
				flexible |= placed.flexible;
			}
			List<String[]> rows() { return rows.isEmpty() ? rowsOf(new String[] { "1", "FRACTION" }) : rows; }
		}

		/** Places regions in order; a tabs region takes the following inTab regions into its selected tab. */
		private List<Placed> placeAll(List<UiIr.Region> regions) {
			List<Placed> placed = new ArrayList<Placed>();
			for (int i = 0; i < regions.size(); i++) {
				UiIr.Region region = regions.get(i);
				if (UiIr.TABS.equals(region.getType())) {
					int j = i + 1;
					while (j < regions.size() && regions.get(j).isInTab()) j++;
					placed.add(buildTabs(region, regions.subList(i + 1, j)));
					i = j - 1;
					continue;
				}
				Placed p = place(region);
				if (p != null) placed.add(p);
			}
			return placed;
		}

		private Placed place(UiIr.Region region) {
			Element control = UiIr.GRID.equals(region.getType()) ? buildGridContent(region, ++gridIndex) : buildRegion(region);
			return control == null ? null : new Placed(control, isFlexible(region), heightOf(region));
		}

		/** Stacks placed regions in a container with a vertical formlayout; returns the stack for its height. */
		private Stack stackInto(Element container, List<Placed> placed) {
			Stack stack = new Stack();
			for (Placed p : placed) {
				p.control.insertBefore(formData(stack.rows.size(), 0), p.control.getFirstChild());
				container.appendChild(p.control);
				stack.add(p);
			}
			container.appendChild(formLayout(stack.rows(), cols(new String[] { "1", "FRACTION" }), 12, 12));
			return stack;
		}

		/**
		 * Left and right panes in a division-group, with the ◀▶ shuttle column between them when the image has one.
		 * The template's division is reused when it has the same number of columns, so its ratio (1:1, 2:5,
		 * 250px:1, 1fr/24px/1fr) is kept; its rows are reduced to one because each pane stacks its own regions.
		 */
		private Placed buildDivision(List<UiIr.Region> split) {
			LayoutShape.Split panes = LayoutShape.Split.of(split);
			int columns = panes.center == null ? 2 : 3;
			Element division = null;
			for (Element prototype : divisionPrototypes) {
				Element layout = layoutOf(prototype);
				if (layout == null || tracks(layout, "columns") != columns) continue;
				division = instantiate(prototype);
				removeControls(division);
				Element copyLayout = layoutOf(division);
				for (Element row : childElements(copyLayout, false)) { if ("rows".equals(row.getLocalName())) copyLayout.removeChild(row); }
				copyLayout.insertBefore(track("rows", new String[] { "1", "FRACTION" }), copyLayout.getFirstChild());
				break;
			}
			if (division == null) {
				division = element("group");
				division.setAttribute("class", "division-group");
				List<String[]> tracks = columns == 3
					? cols(new String[] { "1", "FRACTION" }, new String[] { "24", "PIXEL" }, new String[] { "1", "FRACTION" })
					: cols(new String[] { "1", "FRACTION" }, new String[] { "1", "FRACTION" });
				division.appendChild(formLayout(rowsOf(new String[] { "1", "FRACTION" }), tracks, 16, 12));
			}
			Element layout = layoutOf(division);
			List<Placed> cells = new ArrayList<Placed>();
			cells.add(pane(panes.left));
			if (panes.center != null) cells.add(shuttleColumn(panes.center));
			cells.add(pane(panes.right));
			boolean flexible = false;
			int height = 0;
			for (int col = 0; col < cells.size(); col++) {
				Placed cell = cells.get(col);
				cell.control.insertBefore(formData(0, col), cell.control.getFirstChild());
				division.insertBefore(cell.control, layout);
				flexible |= cell.flexible;
				height = Math.max(height, cell.height);
			}
			return new Placed(division, flexible, height);
		}

		/** One pane: a lone region goes in directly (as the templates do), several are stacked in a plain group. */
		private Placed pane(List<UiIr.Region> regions) {
			List<Placed> placed = placeAll(regions);
			if (placed.size() == 1) return placed.get(0);
			Element group = element("group");
			group.setAttribute("id", uniqueId("grp"));
			Stack stack = stackInto(group, placed);
			return new Placed(group, stack.flexible, stack.needed);
		}

		/** ◀▶ buttons stacked in the 24px middle column, vertically centered like P7-1/P7-3. */
		private Placed shuttleColumn(UiIr.Region region) {
			Element group = element("group");
			group.setAttribute("id", uniqueId("grp"));
			group.setAttribute("class", "shuttle-button-group");
			List<String[]> rows = new ArrayList<String[]>();
			for (int i = 0; i < region.getButtons().size(); i++) {
				Element button = button(region.getButtons().get(i), 24, false);
				stripLayoutData(button);
				group.appendChild(withFormData(button, i, 0));
				rows.add(new String[] { "24", "PIXEL" });
			}
			Element layout = formLayout(rows, cols(new String[] { "1", "FRACTION" }), 6, 6);
			layout.setAttribute("top-margin", "1fr");
			layout.setAttribute("bottom-margin", "1fr");
			group.appendChild(layout);
			return new Placed(group, false, rows.size() * 30);
		}

		private static int tracks(Element layout, String name) {
			int n = 0;
			for (Element child : childElements(layout, false)) { if (name.equals(child.getLocalName())) n++; }
			return n;
		}

		private boolean isFlexible(UiIr.Region region) {
			String type = region.getType();
			return UiIr.GRID.equals(type) || UiIr.TABS.equals(type) || UiIr.TREE.equals(type);
		}

		private int heightOf(UiIr.Region region) {
			String type = region.getType();
			if (UiIr.DESCRIPTION.equals(type)) return Math.max(1, lines(region.getText()).size()) * 18 + 8;
			if (UiIr.SECTION_TITLE.equals(type)) return 24;
			if (UiIr.BUTTONS.equals(type)) return 28;
			if (UiIr.TEXTAREA.equals(type)) return 160;
			if (UiIr.SEARCH.equals(type)) {
				int rows = Math.max(1, (int) Math.ceil(region.getFields().size() / (double) perRow(region)));
				return rows * 24 + (rows - 1) * 6 + 20;
			}
			if (UiIr.FORM.equals(type)) {
				int rows = (int) Math.ceil(region.getFields().size() / (double) formPerRow(region));
				return (hasTitleRow(region) ? 36 : 0) + Math.max(1, rows) * 29 + 4;
			}
			return 260;
		}

		private Element buildRegion(UiIr.Region region) {
			String type = region.getType();
			if (UiIr.DESCRIPTION.equals(type)) return buildDescription(region);
			if (UiIr.SECTION_TITLE.equals(type)) return buildSectionTitle(firstNonBlank(region.getText(), region.getTitle()));
			if (UiIr.FORM.equals(type)) return buildFormContent(region);
			if (UiIr.BUTTONS.equals(type)) {
				if ("center".equalsIgnoreCase(region.getAlign()) || LayoutShape.isArrows(region)) return centeredButtonGroup(region.getButtons(), 28);
				return buttonGroup(region.getButtons(), "left".equalsIgnoreCase(region.getAlign()) ? "left" : "right", 28, false);
			}
			if (UiIr.TEXTAREA.equals(type)) return buildTextArea(region);
			if (UiIr.TABS.equals(type)) return buildTabs(region, new ArrayList<UiIr.Region>()).control;
			if (UiIr.TREE.equals(type)) return buildTreeContent(region);
			if (UiIr.SEARCH.equals(type)) return buildBodySearch(region);
			ir.getWarnings().add("Unsupported region skipped: " + type);
			return null;
		}

		// ------------------------------------------------------------------ search

		/** A second search bar inside the body (P2-3): the template's own body search-box when it has one. */
		private Element buildBodySearch(UiIr.Region region) {
			Element search;
			if (bodySearchPrototype != null) {
				search = instantiate(bodySearchPrototype);
				removeControls(search);
			} else {
				search = element("group");
				search.setAttribute("id", uniqueId("grpSearch"));
				search.setAttribute("class", "search-box");
			}
			buildSearch(search, region, false);
			return search;
		}

		/** @param defaultButtons the header search bar gets [초기화, 조회] when the image shows none; a body one does not */
		private void buildSearch(Element search, UiIr.Region region, boolean defaultButtons) {
			Element layout = layoutOf(search);
			List<UiIr.Field> fields = region.getFields();
			int perRow = perRow(region);
			int rowCount = Math.max(1, (int) Math.ceil(fields.size() / (double) perRow));
			for (int i = 0; i < fields.size(); i++) {
				UiIr.Field field = fields.get(i);
				int row = i / perRow;
				int col = (i % perRow) * 2;
				search.insertBefore(label(field, row, col, false), layout);
				Element control = fieldControl(field);
				bindField(control, region, field);
				search.insertBefore(withFormData(control, row, col + 1), layout);
			}
			List<String> buttons = region.getButtons().isEmpty() && defaultButtons ? java.util.Arrays.asList("초기화", "조회") : region.getButtons();
			if (!buttons.isEmpty()) {
				Element buttonGroup = buttonGroup(buttons, "right", 24, true);
				buttonGroup.setAttribute("id", uniqueId("grpBtnSearch"));
				buttonGroup.setAttribute("class", "search-button-group");
				search.insertBefore(withFormData(buttonGroup, rowCount - 1, perRow * 2), layout);
			}
			List<String[]> rows = new ArrayList<String[]>();
			for (int r = 0; r < rowCount; r++) rows.add(new String[] { "24", "PIXEL" });
			List<String[]> columns = new ArrayList<String[]>();
			for (int p = 0; p < perRow; p++) { columns.add(new String[] { "80", "PIXEL", "auto" }); columns.add(new String[] { "1", "FRACTION" }); }
			if (!buttons.isEmpty()) columns.add(new String[] { "97", "PIXEL", "auto" });
			Element newLayout = formLayout(rows, columns, 6, 6);
			if (layout != null) search.replaceChild(newLayout, layout); else search.appendChild(newLayout);
			Element vd = firstChildByLocalName(search, "verticaldata");
			int height = rowCount * 24 + (rowCount - 1) * 6 + 20;
			if (vd != null) vd.setAttribute("height", height + "px");
		}

		private int perRow(UiIr.Region region) {
			int n = region.getColumnsPerRow();
			return n > 0 && n <= 6 ? n : FIELDS_PER_ROW;
		}

		private Element label(UiIr.Field field, int row, int col, boolean ignoreSpacing) {
			Element output = control("output");
			output.setAttribute("class", field.isRequired() ? "label required" : "label");
			output.setAttribute("value", field.getLabel());
			Element fd = formData(row, col);
			if (ignoreSpacing) fd.setAttribute("ignore-layout-spacing", "true");
			output.appendChild(fd);
			return output;
		}

		private Element fieldControl(UiIr.Field field) {
			String component = field.getComponent();
			if ("daterange".equals(component)) {
				Element group = element("group");
				group.setAttribute("id", uniqueId("grp"));
				group.setAttribute("class", "form-control");
				group.appendChild(withFormData(control("dateinput"), 0, 0));
				Element tilde = control("output");
				tilde.setAttribute("value", "~");
				group.appendChild(withFormData(tilde, 0, 1));
				group.appendChild(withFormData(control("dateinput"), 0, 2));
				group.appendChild(formLayout(rowsOf(new String[] { "1", "FRACTION" }), cols(new String[] { "1", "FRACTION" }, new String[] { "10", "PIXEL", "auto" }, new String[] { "1", "FRACTION" }), 6, 6));
				return group;
			}
			Element control = control(ID_PREFIX.containsKey(component) ? component : "inputbox");
			if ("checkbox".equals(component)) control.setAttribute("text", "");
			if ("output".equals(component)) control.setAttribute("value", field.getValue());
			// Choices as in the templates (P3-1): <cl:item label=".." value="value1"/> per visible option.
			if ("radiobutton".equals(component) || "checkboxgroup".equals(component)) {
				for (int i = 0; i < field.getOptions().size(); i++) {
					Element item = element("item");
					item.setAttribute("label", field.getOptions().get(i));
					item.setAttribute("value", "value" + (i + 1));
					control.appendChild(item);
				}
			}
			return control;
		}

		// ------------------------------------------------------------------ grid

		private Element buildGridContent(UiIr.Region region, int index) {
			Element prototype = choose(gridPrototypes, region);
			Element content;
			Element grid;
			if (prototype != null) {
				content = instantiate(prototype);
				grid = descendantsByLocalName(content, "grid").get(0);
			} else {
				content = element("group");
				content.setAttribute("id", uniqueId("grp"));
				content.setAttribute("class", "content");
				Element udc = element("udc");
				udc.setAttribute("id", uniqueId("udccomgridtitle"));
				udc.setAttribute("type", "udc.com.udcComGridTitle");
				content.appendChild(withFormData(udc, 0, 0));
				grid = control("grid");
				content.appendChild(withFormData(grid, 1, 0));
				content.appendChild(formLayout(rowsOf(new String[] { "24", "PIXEL", "auto" }, new String[] { "1", "FRACTION" }), cols(new String[] { "1", "FRACTION" }), 4, 4));
			}
			// Keep only one grid in the cloned content and rebuild its bands.
			List<Element> grids = descendantsByLocalName(content, "grid");
			for (int i = 1; i < grids.size(); i++) grids.get(i).getParentNode().removeChild(grids.get(i));
			grid.setAttribute("id", grid.getAttribute("id").isEmpty() ? uniqueId("grd") : grid.getAttribute("id"));
			region.setControlId(grid.getAttribute("id"));
			for (Element udc : descendantsByLocalName(content, "udc")) {
				if (!udc.getAttribute("type").endsWith("udcComGridTitle")) continue;
				setUdcProperty(udc, "title", firstNonBlank(region.getTitle(), index == 1 ? ir.getScreenName() : "목록 " + index));
				// ctrl binds the row count / Excel export to this grid (compiles to udc.ctrl = linker.grid_N).
				setUdcProperty(udc, "ctrl", grid.getAttribute("id"), "control");
				if (region.getExcel() != null) setUdcProperty(udc, "showExportExcel", String.valueOf(region.getExcel()), "boolean");
			}
			fillTitleButtons(content, region, grid.getAttribute("id"));
			// Page indexer (P1-4): kept or added when the image shows page numbers under the table, removed otherwise.
			List<Element> indexers = descendantsByLocalName(content, "pageindexer");
			if (!region.isPaging()) { for (Element indexer : indexers) removeRow(content, indexer); }
			else if (indexers.isEmpty() && grid.getParentNode() == content) addPageIndexer(content, grid);
			for (Element child : childElements(grid, false)) { if (!isLayoutData(child)) grid.removeChild(child); }
			fillGrid(grid, region);
			return content;
		}

		private void fillGrid(Element grid, UiIr.Region region) {
			List<UiIr.Column> columns = region.getColumns();
			String datasetId = hasText(region.getDataId()) ? exactId(region.getDataId()) : uniqueId("dsList");
			Element dataset = element("dataset");
			dataset.setAttribute("id", datasetId);
			Element columnList = doc.createElementNS(CL, "cl:datacolumnlist");
			dataset.appendChild(columnList);
			Set<String> usedNames = new HashSet<String>();
			double scale = ir.getSourceWidth() > 0 ? Math.min(2.5, CONTENT_WIDTH / (double) ir.getSourceWidth()) : 1.0;

			for (UiIr.Column column : columns) {
				Element gc = element("gridcolumn");
				gc.setAttribute("width", columnWidth(column, scale) + "px");
				grid.appendChild(gc);
			}
			Element headerBand = element("gridheader");
			Element detailBand = element("griddetail");
			headerBand.appendChild(gridRow());
			detailBand.appendChild(gridRow());
			grid.appendChild(headerBand);
			grid.appendChild(detailBand);

			for (int i = 0; i < columns.size(); i++) {
				UiIr.Column column = columns.get(i);
				String editor = column.getEditor();
				Element headerCell = gridCell("gh-cell", i);
				Element detailCell = gridCell("gd-cell", i);
				headerBand.appendChild(headerCell);
				detailBand.appendChild(detailCell);
				if ("checkbox".equals(editor) && column.getHeader().isEmpty()) headerCell.setAttribute("columntype", "checkbox");
				else headerCell.setAttribute("text", column.getHeader());
				if ("checkbox".equals(editor) && isRowSelector(column, i)) { detailCell.setAttribute("columntype", "checkbox"); continue; }
				if ("rowindex".equals(editor)) { detailCell.setAttribute("columntype", "rowindex"); continue; }
				if ("button".equals(editor)) {
					Element button = control("button");
					button.setAttribute("class", "btn-secondary-03");
					button.setAttribute("value", firstNonBlank(column.getCellText(), column.getHeader(), "버튼"));
					detailCell.appendChild(button);
					continue;
				}
				String name = columnName(column, i, usedNames);
				Element dc = element("datacolumn");
				dc.setAttribute("name", name);
				if (hasText(column.getDataType())) dc.setAttribute("datatype", column.getDataType());
				else if ("numbereditor".equals(editor)) dc.setAttribute("datatype", "number");
				columnList.appendChild(dc);
				headerCell.setAttribute("targetcolumnname", name);
				detailCell.setAttribute("columnname", name);
				if ("checkbox".equals(editor)) { Element cbx = control("checkbox"); cbx.setAttribute("text", ""); detailCell.appendChild(cbx); }
				else if (!"output".equals(editor) && ID_PREFIX.containsKey(editor)) detailCell.appendChild(control(editor));
			}
			if (columnList.getChildNodes().getLength() > 0) {
				grid.setAttribute("datasetid", datasetId);
				ensureModel().appendChild(dataset);
			}
		}

		/** A leading checkbox column without data meaning is eXBuilder6's row-check column. */
		private boolean isRowSelector(UiIr.Column column, int index) {
			return index <= 1 && !column.getCellText().matches("[A-Z][A-Z0-9_]*");
		}

		private int columnWidth(UiIr.Column column, double scale) {
			if (column.getWidth() > 0) return Math.max(30, Math.min(800, (int) Math.round(column.getWidth() * scale)));
			String editor = column.getEditor();
			if ("checkbox".equals(editor)) return 40;
			if ("rowindex".equals(editor)) return 50;
			// Unknown width: estimate from the header and the first line of the sample cell (Korean ≈ 13px, Latin ≈ 7px).
			int text = Math.max(textWidth(column.getHeader()) + 24, textWidth(column.getCellText().split("\\r?\\n")[0]) + 20);
			return Math.max(80, Math.min(420, text));
		}

		private int textWidth(String text) {
			int width = 0;
			for (char ch : text.toCharArray()) width += ch >= 0x1100 ? 13 : 7;
			return width;
		}

		private String columnName(UiIr.Column column, int index, Set<String> used) {
			// An explicit binding name (OpenAPI DTO property, v2.2) wins over everything derived from the design.
			String candidate = column.getName().matches("[A-Za-z_$][A-Za-z0-9_$]{0,63}") ? column.getName() : "";
			if (candidate.isEmpty()) {
				// Design-mode grids show the bound column name (e.g. "FNM", "ACNO_____"); sample data like "onBodyLoad" is not one.
				candidate = ColumnNames.codeOf(column.getCellText());
				if (!candidate.matches("[A-Z][A-Z0-9_]{1,40}")) {
					String header = column.getHeader().replace(" ", "");
					candidate = ColumnNames.nameFor(header);
					if (candidate.isEmpty() && header.matches("[A-Za-z][A-Za-z0-9_]*")) candidate = header.toUpperCase(Locale.ROOT);
					if (candidate.isEmpty()) candidate = "COL" + (index + 1);
				}
			}
			String name = candidate;
			for (int n = 2; !used.add(name); n++) name = candidate + n;
			return name;
		}

		private Element gridRow() {
			Element row = element("gridrow");
			row.setAttribute("height", "25px");
			return row;
		}

		private Element gridCell(String sidPrefix, int col) {
			Element cell = doc.createElementNS(CL, "cl:gridcell");
			cell.setAttributeNS(STD, "std:sid", uniqueSid(sidPrefix));
			cell.setAttribute("rowindex", "0");
			cell.setAttribute("colindex", String.valueOf(col));
			return cell;
		}

		// ------------------------------------------------------------------ other regions

		private Element buildDescription(UiIr.Region region) {
			Element group = element("group");
			group.setAttribute("id", uniqueId("grp"));
			List<String> lines = lines(firstNonBlank(region.getText(), region.getTitle()));
			List<String[]> rows = new ArrayList<String[]>();
			for (int i = 0; i < lines.size(); i++) {
				Element output = control("output");
				output.setAttribute("value", lines.get(i));
				group.appendChild(withFormData(output, i, 0));
				rows.add(new String[] { "18", "PIXEL" });
			}
			group.appendChild(formLayout(rows, cols(new String[] { "1", "FRACTION" }), 0, 0));
			return group;
		}

		private Element buildSectionTitle(String text) {
			Element box = element("group");
			box.setAttribute("id", uniqueId("grp"));
			box.setAttribute("class", "content-title-box");
			Element output = control("output");
			output.setAttribute("class", "form-tit");
			output.setAttribute("value", text);
			box.appendChild(withFormData(output, 0, 0));
			box.appendChild(formLayout(rowsOf(new String[] { "1", "FRACTION" }), cols(new String[] { "1", "FRACTION", "auto" }), 32, 0));
			return box;
		}

		private Element buildForm(UiIr.Region region) {
			Element content = element("group");
			content.setAttribute("id", uniqueId("grp"));
			content.setAttribute("class", "content");
			int row = 0;
			List<String[]> contentRows = new ArrayList<String[]>();
			if (hasText(region.getTitle())) {
				content.appendChild(withFormData(buildSectionTitle(region.getTitle()), row++, 0));
				contentRows.add(new String[] { "24", "PIXEL", "auto" });
			}
			Element form = element("group");
			form.setAttribute("id", uniqueId("grp"));
			form.setAttribute("class", "form-base");
			fillFormBase(form, region, perRow(region));
			content.appendChild(withFormData(form, row, 0));
			contentRows.add(new String[] { "1", "FRACTION" });
			content.appendChild(formLayout(contentRows, cols(new String[] { "1", "FRACTION" }), 4, 4));
			return content;
		}

		/** Label/control pairs, perRow per row, the last control spanning the rest of its row. */
		private void fillFormBase(Element form, UiIr.Region region, int perRow) {
			List<UiIr.Field> fields = region.getFields();
			int rowCount = Math.max(1, (int) Math.ceil(fields.size() / (double) perRow));
			for (int i = 0; i < fields.size(); i++) {
				int r = i / perRow;
				int c = (i % perRow) * 2;
				form.appendChild(label(fields.get(i), r, c, true));
				Element control = withFormData(fieldControl(fields.get(i)), r, c + 1);
				bindField(control, region, fields.get(i));
				if (i == fields.size() - 1 && c + 1 < perRow * 2 - 1) firstChildByLocalName(control, "formdata").setAttribute("colspan", String.valueOf(perRow * 2 - c - 1));
				form.appendChild(control);
			}
			List<String[]> rows = new ArrayList<String[]>();
			for (int r = 0; r < rowCount; r++) rows.add(new String[] { "24", "PIXEL" });
			List<String[]> columns = new ArrayList<String[]>();
			for (int p = 0; p < perRow; p++) { columns.add(new String[] { "80", "PIXEL", "auto" }); columns.add(new String[] { "1", "FRACTION" }); }
			Element layout = formLayout(rows, columns, 9, 5);
			layout.setAttribute("top-margin", "2px"); layout.setAttribute("right-margin", "4px"); layout.setAttribute("bottom-margin", "2px"); layout.setAttribute("left-margin", "4px");
			layout.setAttribute("hseparatorwidth", "1"); layout.setAttribute("hseparatortype", "BY_CLASS"); layout.setAttribute("vseparatorwidth", "1"); layout.setAttribute("vseparatortype", "BY_CLASS");
			form.appendChild(layout);
		}

		/**
		 * A form in the template's own form content (udcComFormTitle or form-tit title row, title-row buttons,
		 * form-base styling). Pairs per row follow the image, else the template's form-base (2 in a narrow pane).
		 */
		private Element buildFormContent(UiIr.Region region) {
			Element prototype = choose(formPrototypes, region);
			formIndex++;
			if (prototype == null) return buildForm(region);
			Element content = instantiate(prototype);
			Element form = null;
			for (Element child : childElements(content, true)) { if (hasClass(child, "form-base")) form = child; }
			removeControls(form);
			Element oldLayout = layoutOf(form);
			if (oldLayout != null) form.removeChild(oldLayout);
			fillFormBase(form, region, formPerRow(region));
			titleRow(content, region, firstNonBlank(region.getTitle(), formIndex == 1 ? ir.getScreenName() : "상세 " + formIndex));
			return content;
		}

		private int formPerRow(UiIr.Region region) {
			int n = region.getColumnsPerRow();
			if (n > 0 && n <= 6) return n;
			Element prototype = choose(formPrototypes, region);
			if (prototype != null) {
				for (Element child : childElements(prototype, true)) {
					Element layout = hasClass(child, "form-base") ? layoutOf(child) : null;
					if (layout != null && tracks(layout, "columns") >= 2) return Math.min(6, tracks(layout, "columns") / 2);
				}
			}
			return FIELDS_PER_ROW;
		}

		/** A tree in the template's tree content (P6: udcComFormTitle title row, title-row buttons). */
		private Element buildTreeContent(UiIr.Region region) {
			if (treePrototype == null) return control("tree");
			Element content = instantiate(treePrototype);
			Element tree = firstChildByLocalName(content, "tree");
			for (Element child : childElements(tree, false)) { if (!isLayoutData(child)) tree.removeChild(child); }
			if (!tree.hasAttribute("id")) tree.setAttribute("id", uniqueId("tre"));
			titleRow(content, region, firstNonBlank(region.getTitle(), ir.getScreenName()));
			return content;
		}

		private boolean hasTitleRow(UiIr.Region region) { return hasText(region.getTitle()) || !region.getButtons().isEmpty(); }

		/**
		 * Sets the heading of a cloned content (udcComFormTitle / udcComGridTitle title, or output.form-tit) and its
		 * title-row buttons. Without a heading and buttons in the image, the title row is removed instead.
		 */
		private void titleRow(Element content, UiIr.Region region, String fallbackTitle) {
			List<Element> headings = new ArrayList<Element>();
			for (Element child : childElements(content, true)) {
				if (hasClass(child, "content-title-box") || "udc".equals(child.getLocalName()) || hasClass(child, "form-tit")) headings.add(child);
			}
			if (!hasTitleRow(region)) { for (Element heading : headings) removeRow(content, heading); return; }
			String title = firstNonBlank(region.getTitle(), fallbackTitle);
			for (Element udc : descendantsByLocalName(content, "udc")) {
				String type = udc.getAttribute("type");
				if (type.endsWith("udcComFormTitle") || type.endsWith("udcComGridTitle")) setUdcProperty(udc, "title", title);
			}
			for (Element output : descendantsByLocalName(content, "output")) { if (hasClass(output, "form-tit")) output.setAttribute("value", title); }
			fillTitleButtons(content, region, null);
		}

		/**
		 * The template's title-row buttons (행추가/행삭제 ...) are placeholders; the region's own buttons replace them.
		 * On a grid, row-edit buttons (행추가/행삭제/취소/저장) become one udcComGridCudBtns bound to the grid.
		 */
		private void fillTitleButtons(Element content, UiIr.Region region, String gridId) {
			if (!region.getButtons().isEmpty() && descendantsByClass(content, "title-button-group").isEmpty() && !addTitleButtonGroup(content)) {
				ir.getWarnings().add("제목행 버튼을 둘 자리가 없어 생략: " + region.getButtons());
			}
			for (Element titleButtons : descendantsByClass(content, "title-button-group")) {
				removeControls(titleButtons);
				Element flow = layoutOf(titleButtons);
				Map<String, String> cud = gridId == null ? new HashMap<String, String>() : cudRoles(region.getButtons());
				boolean cudPlaced = false;
				for (String text : region.getButtons()) {
					if (cud.containsValue(text)) {
						if (!cudPlaced) titleButtons.insertBefore(gridCudButtons(cud, gridId), flow);
						cudPlaced = true;
					} else titleButtons.insertBefore(button(text, 24, true), flow);
				}
			}
		}

		/**
		 * The chosen prototype has a bare title UDC (P1-4) but the design has title-row buttons: wrap the UDC in a
		 * content-title-box with a right-aligned title-button-group, the structure of the templates that have one (P1-2_P).
		 */
		private boolean addTitleButtonGroup(Element content) {
			Element udc = null;
			for (Element candidate : descendantsByLocalName(content, "udc")) {
				String type = candidate.getAttribute("type");
				if (type.endsWith("udcComGridTitle") || type.endsWith("udcComFormTitle")) { udc = candidate; break; }
			}
			Element fd = udc == null ? null : firstChildByLocalName(udc, "formdata");
			if (fd == null) return false;
			Element parent = (Element) udc.getParentNode();
			Element parentLayout = layoutOf(parent);
			if (parent != content && parentLayout != null && "formlayout".equals(parentLayout.getLocalName())) {
				// P1-4: the UDC already has its own title group; add a right-hand column for the buttons.
				int col = tracks(parentLayout, "columns");
				parentLayout.appendChild(track("columns", new String[] { "300", "PIXEL", "auto" }));
				parent.insertBefore(titleButtonGroup(parsePx(fd.getAttribute("row"), 0), col), parentLayout);
				return true;
			}
			Element box = element("group");
			box.setAttribute("class", "content-title-box");
			content.insertBefore(box, udc);
			udc.removeChild(fd);
			box.appendChild(fd);
			content.removeChild(udc);
			box.appendChild(withFormData(udc, 0, 0));
			box.appendChild(titleButtonGroup(0, 1));
			Element layout = formLayout(rowsOf(new String[] { "1", "FRACTION" }), cols(new String[] { "1", "FRACTION", "auto" }, new String[] { "300", "PIXEL", "auto" }), 0, 0);
			box.appendChild(layout);
			return true;
		}

		private Element titleButtonGroup(int row, int col) {
			Element buttons = element("group");
			buttons.setAttribute("class", "title-button-group");
			buttons.appendChild(formData(row, col));
			Element flow = element("flowlayout");
			flow.setAttribute("scrollable", "false");
			flow.setAttribute("halign", "right");
			buttons.appendChild(flow);
			return buttons;
		}

		private static final String[][] CUD_ROLES = {
			{ "New", "(행\\s*)?(추가|신규|등록)|add|new" },
			{ "Delete", "(행\\s*)?삭제|delete|remove" },
			{ "Restore", "(행\\s*)?(취소|복원)|되돌리기|restore|cancel" },
			{ "Save", "저장|save" } };

		/**
		 * Row-edit role → caption. The UDC is used only when an insert or delete button is present; a lone 저장 stays a
		 * plain button. Each role takes the first matching caption.
		 */
		private static Map<String, String> cudRoles(List<String> buttons) {
			Map<String, String> roles = new java.util.LinkedHashMap<String, String>();
			for (String[] role : CUD_ROLES) {
				for (String text : buttons) {
					if (!roles.containsValue(text) && text.replace(" ", "").toLowerCase(java.util.Locale.ROOT).matches(role[1])) { roles.put(role[0], text); break; }
				}
			}
			if (!roles.containsKey("New") && !roles.containsKey("Delete")) roles.clear();
			return roles;
		}

		/** udcComGridCudBtns (clx-src/udc/com): labels and visibility from the design, grid bound as a control property. */
		private Element gridCudButtons(Map<String, String> roles, String gridId) {
			Element udc = element("udc");
			udc.setAttribute("id", uniqueId("udccomgridcudbtns"));
			udc.setAttribute("type", "udc.com.udcComGridCudBtns");
			int width = 0;
			for (String text : roles.values()) width += buttonWidth(text) + 4;
			Element data = element("flowlayoutdata");
			data.setAttribute("width", Math.max(60, width) + "px");
			data.setAttribute("height", "24px");
			udc.appendChild(data);
			setUdcProperty(udc, "grid", gridId, "control");
			String[][] props = { { "New", "buttonNewLabel", "visibleNewButton" }, { "Delete", "buttonDelLabel", "visibleDeleteButton" },
				{ "Restore", "buttonRestoreLable", "visibleRestoreButton" }, { "Save", "buttonSaveLabel", "visibleSaveButton" } };
			for (String[] p : props) {
				String text = roles.get(p[0]);
				if (text != null) setUdcProperty(udc, p[1], text);
				else setUdcProperty(udc, p[2], "false", "boolean");
			}
			wireCudButtons(udc, roles);
			return udc;
		}

		/**
		 * Backend binding for the UDC's own buttons (v2.2): the UDC dispatches "save" / "delete" events. A bound 저장
		 * sends the save submission on "save"; a bound 삭제 turns off the UDC's default row removal
		 * (ignoreDefaultDeleteAction) and sends the DELETE submission for the selected row on "delete".
		 */
		private void wireCudButtons(Element udc, Map<String, String> roles) {
			String[][] events = { { "Save", "save" }, { "Delete", "delete" } };
			for (String[] e : events) {
				String caption = roles.get(e[0]);
				UiIr.Submission submission = caption == null ? null : ir.submissionFor(caption);
				if (submission == null) continue;
				if ("delete".equals(e[1])) setUdcProperty(udc, "ignoreDefaultDeleteAction", "true", "boolean");
				String function = uniqueFunction("on" + capitalize(udc.getAttribute("id")) + capitalize(e[1]));
				addListener(udc, e[1], function);
				ir.getHandlers().add(new UiIr.Handler(function, e[1], udc.getAttribute("id"), caption, submission.getId(), submission.getRole()));
			}
		}

		/** Removes a child and its formlayout row: later rows move up and the row track goes away. */
		private void removeRow(Element container, Element child) {
			Element fd = firstChildByLocalName(child, "formdata");
			container.removeChild(child);
			Element layout = layoutOf(container);
			if (fd == null || layout == null || !"formlayout".equals(layout.getLocalName())) return;
			int row = parsePx(fd.getAttribute("row"), -1);
			if (row < 0) return;
			for (Element other : childElements(container, true)) {
				Element ofd = firstChildByLocalName(other, "formdata");
				if (ofd != null && parsePx(ofd.getAttribute("row"), -1) == row) return; // the row is still in use
			}
			for (Element other : childElements(container, true)) {
				Element ofd = firstChildByLocalName(other, "formdata");
				int r = ofd == null ? -1 : parsePx(ofd.getAttribute("row"), -1);
				if (r > row) ofd.setAttribute("row", String.valueOf(r - 1));
			}
			int index = 0;
			for (Element track : childElements(layout, false)) {
				if (!"rows".equals(track.getLocalName())) continue;
				if (index++ == row) { layout.removeChild(track); break; }
			}
		}

		/** A pageindexer row under the grid, as in P1-4. */
		private void addPageIndexer(Element content, Element grid) {
			Element fd = firstChildByLocalName(grid, "formdata");
			int row = (fd == null ? 1 : parsePx(fd.getAttribute("row"), 1)) + 1;
			Element indexer = control("pageindexer");
			indexer.appendChild(formData(row, 0));
			Element layout = layoutOf(content);
			content.insertBefore(indexer, layout);
			if (layout != null && tracks(layout, "rows") <= row) {
				Element firstColumn = null;
				for (Element track : childElements(layout, false)) { if ("columns".equals(track.getLocalName())) { firstColumn = track; break; } }
				layout.insertBefore(track("rows", new String[] { "24", "PIXEL" }), firstColumn);
			}
		}

		private Element buildTextArea(UiIr.Region region) {
			Element content = element("group");
			content.setAttribute("id", uniqueId("grp"));
			content.setAttribute("class", "content");
			boolean hasHeader = hasText(region.getTitle()) || !region.getButtons().isEmpty();
			int row = 0;
			if (hasHeader) {
				if (hasText(region.getTitle())) {
					Element output = control("output");
					output.setAttribute("class", "form-tit");
					output.setAttribute("value", region.getTitle());
					content.appendChild(withFormData(output, 0, 0));
				}
				if (!region.getButtons().isEmpty()) content.appendChild(withFormData(buttonGroup(region.getButtons(), "right", 24, true), 0, 1));
				row = 1;
			}
			Element textarea = control("textarea");
			Element fd = formData(row, 0);
			fd.setAttribute("colspan", "2");
			textarea.appendChild(fd);
			content.appendChild(textarea);
			List<String[]> rows = hasHeader ? rowsOf(new String[] { "24", "PIXEL" }, new String[] { "1", "FRACTION" }) : rowsOf(new String[] { "1", "FRACTION" });
			content.appendChild(formLayout(rows, cols(new String[] { "1", "FRACTION" }, new String[] { "97", "PIXEL", "auto" }), 4, 4));
			return content;
		}

		/** Tab folder; the regions drawn inside the selected tab's panel (inTab) are stacked into the first tab (P5). */
		private Placed buildTabs(UiIr.Region region, List<UiIr.Region> content) {
			Element folder = element("tabfolder");
			folder.setAttribute("class", "tab-filled");
			List<String> tabs = region.getTabs().isEmpty() ? java.util.Arrays.asList("탭1") : region.getTabs();
			int needed = 0;
			for (int i = 0; i < tabs.size(); i++) {
				Element item = element("tabitem");
				if (i == 0) item.setAttribute("selected", "true");
				item.setAttribute("text", tabs.get(i));
				Element group = element("group");
				if (i == 0 && !content.isEmpty()) needed = stackInto(group, placeAll(content)).needed;
				else group.appendChild(formLayout(rowsOf(new String[] { "1", "FRACTION" }), cols(new String[] { "1", "FRACTION" }), 12, 12));
				item.appendChild(group);
				folder.appendChild(item);
			}
			return new Placed(folder, true, Math.max(260, needed + 40));
		}

		private void buildFooter(Element footer, UiIr.Region region) {
			List<Element> buttonGroups = descendantsByClass(footer, "footer-button-group");
			if (buttonGroups.isEmpty()) { footer.getParentNode().removeChild(footer); return; }
			List<Element> sides = childElements(buttonGroups.get(0), true);
			boolean left = "left".equalsIgnoreCase(region.getAlign());
			Element target = sides.isEmpty() ? buttonGroups.get(0) : sides.get(left || sides.size() == 1 ? 0 : sides.size() - 1);
			Element layout = layoutOf(target);
			for (String text : region.getButtons()) target.insertBefore(button(text, 28, false), layout);
		}

		private Element buttonGroup(List<String> buttons, String align, int height, boolean compact) {
			Element group = element("group");
			group.setAttribute("id", uniqueId("grp"));
			for (String text : buttons) group.appendChild(button(text, height, compact));
			Element flow = element("flowlayout");
			flow.setAttribute("hspacing", "6");
			flow.setAttribute("vspacing", "0");
			flow.setAttribute("halign", align);
			flow.setAttribute("linewrap", "false");
			group.appendChild(flow);
			return group;
		}

		/**
		 * Centered buttons, done the way the shuttle templates (P7-3) center their arrow buttons: a formlayout
		 * with 1fr side margins. flowlayout halign="center" appears in no template, so it is not used.
		 */
		private Element centeredButtonGroup(List<String> buttons, int height) {
			Element group = element("group");
			group.setAttribute("id", uniqueId("grp"));
			List<String[]> columns = new ArrayList<String[]>();
			for (int i = 0; i < buttons.size(); i++) {
				String text = buttons.get(i);
				Element button = button(text, height, false);
				stripLayoutData(button);
				group.appendChild(withFormData(button, 0, i));
				columns.add(iconClass(text) != null ? new String[] { "24", "PIXEL" } : new String[] { String.valueOf(buttonWidth(text)), "PIXEL", "auto" });
			}
			if (columns.isEmpty()) columns.add(new String[] { "1", "FRACTION" });
			Element layout = formLayout(rowsOf(new String[] { String.valueOf(height), "PIXEL" }), columns, 6, 0);
			layout.setAttribute("left-margin", "1fr");
			layout.setAttribute("right-margin", "1fr");
			group.appendChild(layout);
			// Arrow-only rows are shuttles between two lists (P7-2, P7-3's ▼▲), styled like the templates' ones.
			boolean arrows = !buttons.isEmpty();
			for (String text : buttons) arrows &= iconClass(text) != null;
			if (arrows) group.setAttribute("class", "shuttle-button-group");
			return group;
		}

		/** Arrow-only buttons map to the theme's icon buttons (P7 shuttle templates); the model writes them as ▲ ▼ ◀ ▶. */
		private static String iconClass(String text) {
			String t = text.trim();
			if (t.matches("[▲△↑⬆⏶]")) return "btn-up";
			if (t.matches("[▼▽↓⬇⏷]")) return "btn-down";
			if (t.matches("[◀◁←⬅]")) return "btn-left";
			if (t.matches("[▶▷→➡]")) return "btn-right";
			return null;
		}

		private static int buttonWidth(String text) { return Math.max(40, text.length() * 14 + 16); }

		/** Button style classes follow the template repository conventions. */
		private Element button(String text, int height, boolean compact) {
			Element button = control("button");
			String icon = iconClass(text);
			if (icon != null) {
				button.setAttribute("class", icon);
				Element data = element("flowlayoutdata");
				data.setAttribute("width", "20px");
				data.setAttribute("height", "24px");
				data.setAttribute("autosize", "none");
				button.appendChild(data);
				return button;
			}
			String t = text.replace(" ", "");
			String cls;
			if (compact) cls = t.matches(".*(조회|검색|찾기|Search|search).*") ? "btn-primary-02" : "btn-secondary-03 btn-md";
			else if (t.matches(".*(저장|등록|확인|적용|승인|신청).*")) cls = "btn-primary-01";
			else if (t.matches(".*(삭제|취소|반려).*")) cls = "btn-secondary-02";
			else cls = "btn-secondary-01";
			button.setAttribute("class", cls);
			button.setAttribute("value", text);
			wireButton(button, text);
			Element data = element("flowlayoutdata");
			data.setAttribute("width", buttonWidth(text) + "px");
			data.setAttribute("height", height + "px");
			data.setAttribute("autosize", "width");
			button.appendChild(data);
			return button;
		}

		// ------------------------------------------------------------------ backend binding (v2.2)

		/** cl:datamapbind on the field's control when the binder named it; the DataMap is created in the model on first use. */
		private void bindField(Element control, UiIr.Region region, UiIr.Field field) {
			if (!hasText(region.getDataId()) || !hasText(field.getName())) return;
			String dataId = dataMapId(region);
			String[] names = field.getName().split(",");
			if ("daterange".equals(field.getComponent())) {
				List<Element> inputs = descendantsByLocalName(control, "dateinput");
				for (int i = 0; i < inputs.size() && i < names.length; i++) bindControl(inputs.get(i), dataId, names[i].trim());
				return;
			}
			bindControl(control, dataId, names[0].trim());
		}

		/** Compiles to control.bind("value").toDataMap(app.lookup(dataId), column) (checked with e6-compiler). */
		private void bindControl(Element control, String dataId, String column) {
			if (!hasText(column)) return;
			Element bind = doc.createElementNS(CL, "cl:datamapbind");
			bind.setAttribute("property", "value");
			bind.setAttribute("datacontrolid", dataId);
			bind.setAttribute("columnname", column);
			control.insertBefore(bind, control.getFirstChild());
			addDataMapColumn(dataId, column);
		}

		private String dataMapId(UiIr.Region region) {
			String id = dataMapIds.get(region);
			if (id != null) return id;
			id = exactId(region.getDataId());
			dataMapIds.put(region, id);
			Element dataMap = element("datamap");
			dataMap.setAttribute("id", id);
			Element columnList = doc.createElementNS(CL, "cl:datacolumnlist");
			dataMap.appendChild(columnList);
			ensureModel().appendChild(dataMap);
			dataMapColumnLists.put(id, columnList);
			if (UiIr.SEARCH.equals(region.getType()) && searchDataMapId == null) searchDataMapId = id;
			return id;
		}

		private void addDataMapColumn(String dataId, String column) {
			Element columnList = dataMapColumnLists.get(dataId);
			if (columnList == null) return;
			for (Element existing : childElements(columnList, false)) { if (column.equals(existing.getAttribute("name"))) return; }
			Element dc = element("datacolumn");
			dc.setAttribute("name", column);
			columnList.appendChild(dc);
		}

		/** The binder's id when it is still free (dsList, dmSearch …), else a numbered variant. */
		private String exactId(String wanted) {
			if (ids.add(wanted)) return wanted;
			return uniqueId(wanted.replaceAll("\\d+$", ""));
		}

		/** Wires a design button to the submission its caption triggers, or to a reset of the search DataMap. */
		private void wireButton(Element button, String caption) {
			UiIr.Submission submission = ir.submissionFor(caption);
			String role;
			String submissionId = "";
			if (submission != null) { role = submission.getRole(); submissionId = submission.getId(); }
			else if (searchDataMapId != null && caption.replace(" ", "").toLowerCase(Locale.ROOT).matches("(초기화|리셋|reset|clear|지우기)")) role = "reset";
			else return;
			String base = role.isEmpty() ? submissionId.replaceFirst("^sub", "") : role;
			String function = uniqueFunction("onBtn" + capitalize(base) + "Click");
			addListener(button, "click", function);
			ir.getHandlers().add(new UiIr.Handler(function, "click", button.getAttribute("id"), caption, submissionId, role));
		}

		private void addListener(Element control, String event, String function) {
			Element listener = element("listener");
			listener.setAttribute("name", event);
			listener.setAttribute("handler", function);
			control.insertBefore(listener, control.getFirstChild());
		}

		private String uniqueFunction(String base) {
			if (functionNames.add(base)) return base;
			for (int n = 2; ; n++) { if (functionNames.add(base + n)) return base + n; }
		}

		/**
		 * cl:submission per bound backend call (request/response data only when that DataMap/DataSet exists in the
		 * model), a submit-done listener each, a body load listener that runs the first list query, and a grid
		 * selection-change listener that feeds the detail query. Nothing is added without submissions (v2.0 result).
		 */
		private void buildSubmissions(Element body) {
			if (ir.getSubmissions().isEmpty()) return;
			Element model = ensureModel();
			Set<String> dataIds = new HashSet<String>();
			for (Element child : childElements(model, false)) { if ("dataset".equals(child.getLocalName()) || "datamap".equals(child.getLocalName())) dataIds.add(child.getAttribute("id")); }
			String listSubmission = null;
			String detailSubmission = null;
			for (UiIr.Submission submission : ir.getSubmissions()) {
				Element s = element("submission");
				s.setAttribute("id", exactId(submission.getId()));
				s.setAttribute("action", submission.getAction());
				s.setAttribute("method", submission.getMethod());
				if (!"get".equals(submission.getMethod())) s.setAttribute("mediatype", "application/json");
				String function = uniqueFunction("onSub" + capitalize(s.getAttribute("id").replaceFirst("^sub", "")) + "SubmitDone");
				addListener(s, "submit-done", function);
				if (dataIds.contains(submission.getRequestDataId())) { Element r = doc.createElementNS(CL, "cl:requestdata"); r.setAttribute("dataid", submission.getRequestDataId()); s.appendChild(r); }
				if (dataIds.contains(submission.getResponseDataId())) { Element r = doc.createElementNS(CL, "cl:responsedata"); r.setAttribute("dataid", submission.getResponseDataId()); s.appendChild(r); }
				model.appendChild(s);
				ir.getHandlers().add(new UiIr.Handler(function, "submit-done", s.getAttribute("id"), "", submission.getId(), submission.getRole()));
				if (listSubmission == null && UiIr.Submission.ROLE_SEARCH.equals(submission.getRole())) listSubmission = submission.getId();
				if (detailSubmission == null && UiIr.Submission.ROLE_DETAIL.equals(submission.getRole())) detailSubmission = submission.getId();
			}
			if (listSubmission != null) {
				String function = uniqueFunction("onBodyLoad");
				addListener(body, "load", function);
				ir.getHandlers().add(new UiIr.Handler(function, "load", "", "", listSubmission, UiIr.Submission.ROLE_SEARCH));
			}
			if (detailSubmission != null) {
				List<Element> grids = descendantsByLocalName(body, "grid");
				if (!grids.isEmpty()) {
					Element grid = grids.get(0);
					String function = uniqueFunction("on" + capitalize(grid.getAttribute("id")) + "SelectionChange");
					addListener(grid, "selection-change", function);
					ir.getHandlers().add(new UiIr.Handler(function, "selection-change", grid.getAttribute("id"), "", detailSubmission, UiIr.Submission.ROLE_DETAIL));
				}
			}
		}

		private static String capitalize(String s) { return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

		// ------------------------------------------------------------------ node factories

		private Element element(String localName) {
			Element e = doc.createElementNS(CL, "cl:" + localName);
			String prefix = SID_PREFIX.get(localName);
			if (prefix != null) e.setAttributeNS(STD, "std:sid", uniqueSid(prefix));
			return e;
		}

		private Element control(String localName) {
			Element e = element(localName);
			String prefix = ID_PREFIX.get(localName);
			if (prefix != null) e.setAttribute("id", uniqueId(prefix));
			return e;
		}

		private Element withFormData(Element control, int row, int col) {
			control.insertBefore(formData(row, col), control.getFirstChild());
			return control;
		}

		private Element formData(int row, int col) {
			Element fd = element("formdata");
			fd.setAttribute("row", String.valueOf(row));
			fd.setAttribute("col", String.valueOf(col));
			return fd;
		}

		private Element verticalData(int width, int height, boolean autoHeight) {
			Element vd = element("verticaldata");
			vd.setAttribute("width", width + "px");
			vd.setAttribute("height", height + "px");
			if (autoHeight) vd.setAttribute("autosize", "height");
			return vd;
		}

		/** Track spec: {length, unit, optional "auto"}. */
		private Element formLayout(List<String[]> rows, List<String[]> columns, int hspace, int vspace) {
			Element layout = element("formlayout");
			layout.setAttribute("scrollable", "false");
			layout.setAttribute("hspace", hspace + "px");
			layout.setAttribute("vspace", vspace + "px");
			layout.setAttribute("top-margin", "0px");
			layout.setAttribute("right-margin", "0px");
			layout.setAttribute("bottom-margin", "0px");
			layout.setAttribute("left-margin", "0px");
			for (String[] r : rows) layout.appendChild(track("rows", r));
			for (String[] c : columns) layout.appendChild(track("columns", c));
			return layout;
		}

		private Element track(String name, String[] spec) {
			Element track = doc.createElementNS(CL, "cl:" + name);
			track.setAttribute("length", spec[0]);
			track.setAttribute("unit", spec[1]);
			if (spec.length > 2 && "auto".equals(spec[2])) { track.setAttribute("autoSizing", "true"); track.setAttribute("syncminlength", "false"); }
			return track;
		}

		private static List<String[]> rowsOf(String[]... specs) { return java.util.Arrays.asList(specs); }
		private static List<String[]> cols(String[]... specs) { return java.util.Arrays.asList(specs); }

		/** eXBuilder6 UDC property assignment inside the cl:udc element. */
		private void setUdcProperty(Element udc, String name, String value) { setUdcProperty(udc, name, value, "string"); }

		/** @param type string | boolean | control (control value = target control id, checked with e6-compiler) */
		private void setUdcProperty(Element udc, String name, String value, String type) {
			if (!hasText(value)) return;
			for (Element property : childElements(udc, false)) {
				if ("property".equals(property.getLocalName()) && name.equals(property.getAttribute("name"))) { property.setAttribute("value", value); property.setAttribute("type", type); return; }
			}
			Element property = doc.createElementNS(CL, "cl:property");
			property.setAttribute("name", name);
			property.setAttribute("value", value);
			property.setAttribute("type", type);
			udc.appendChild(property);
		}

		private Element ensureModel() {
			if (model != null) return model;
			Element head = firstByLocalName(doc.getDocumentElement(), "head");
			model = element("model");
			head.insertBefore(model, firstChildByLocalName(head, "appspec"));
			return model;
		}

		// ------------------------------------------------------------------ id / sid

		private void collectExisting(Element root) {
			String sid = root.getAttributeNS(STD, "sid");
			if (!sid.isEmpty()) sids.add(sid);
			if (!root.getAttribute("id").isEmpty()) ids.add(root.getAttribute("id"));
			for (Element child : childElements(root, false)) collectExisting(child);
		}

		private String uniqueSid(String prefix) {
			while (true) {
				String sid = prefix + "-" + String.format("%08x", random.nextInt());
				if (sids.add(sid)) return sid;
			}
		}

		private String uniqueId(String prefix) {
			if (!prefix.matches(".*\\d$") && !prefix.startsWith("grpBtn")) {
				for (int n = 1; ; n++) { if (ids.add(prefix + n)) return prefix + n; }
			}
			if (ids.add(prefix)) return prefix;
			for (int n = 2; ; n++) { if (ids.add(prefix + "_" + n)) return prefix + "_" + n; }
		}

		/** Gives a cloned subtree fresh sids and ids so it can be reused several times. */
		private void reassign(Element element) {
			if (element.hasAttributeNS(STD, "sid")) {
				String old = element.getAttributeNS(STD, "sid");
				int dash = old.lastIndexOf('-');
				element.setAttributeNS(STD, "std:sid", uniqueSid(dash > 0 ? old.substring(0, dash) : element.getLocalName()));
			}
			if (element.hasAttribute("id") && !"property".equals(element.getLocalName())) {
				element.setAttribute("id", uniqueId(element.getAttribute("id").replaceAll("\\d+$", "")));
			}
			for (Element child : childElements(element, false)) reassign(child);
		}

		// ------------------------------------------------------------------ DOM helpers

		private void removeControls(Element container) {
			for (Element child : childElements(container, true)) container.removeChild(child);
		}

		private void stripLayoutData(Element control) {
			for (Element child : childElements(control, false)) { if (isLayoutData(child)) control.removeChild(child); }
		}

		private static boolean isLayoutData(Element e) {
			String n = e.getLocalName();
			return "formdata".equals(n) || "verticaldata".equals(n) || "flowlayoutdata".equals(n) || "xylayoutdata".equals(n);
		}

		private static boolean isLayout(Element e) {
			String n = e.getLocalName();
			return "formlayout".equals(n) || "verticallayout".equals(n) || "flowlayout".equals(n) || "xylayout".equals(n) || "responsivexylayout".equals(n);
		}

		/** Child elements; when controlsOnly, layout, layout-data, listener and metadata nodes are excluded. */
		private static List<Element> childElements(Element parent, boolean controlsOnly) {
			List<Element> result = new ArrayList<Element>();
			NodeList nodes = parent.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				if (!(nodes.item(i) instanceof Element)) continue;
				Element e = (Element) nodes.item(i);
				if (controlsOnly && (isLayout(e) || isLayoutData(e) || !CL.equals(e.getNamespaceURI()) || "listener".equals(e.getLocalName()) || "property".equals(e.getLocalName()))) continue;
				result.add(e);
			}
			return result;
		}

		private static Element layoutOf(Element container) {
			for (Element child : childElements(container, false)) { if (isLayout(child)) return child; }
			return null;
		}

		private static Element firstChildByLocalName(Element parent, String localName) {
			for (Element child : childElements(parent, false)) { if (localName.equals(child.getLocalName())) return child; }
			return null;
		}

		private static Element firstByLocalName(Element root, String localName) {
			List<Element> found = descendantsByLocalName(root, localName);
			return found.isEmpty() ? null : found.get(0);
		}

		private static List<Element> descendantsByLocalName(Element root, String localName) {
			List<Element> result = new ArrayList<Element>();
			NodeList nodes = root.getElementsByTagNameNS("*", localName);
			for (int i = 0; i < nodes.getLength(); i++) result.add((Element) nodes.item(i));
			return result;
		}

		private static List<Element> descendantsByClass(Element root, String cls) {
			List<Element> result = new ArrayList<Element>();
			for (Element group : descendantsByLocalName(root, "group")) { if (hasClass(group, cls)) result.add(group); }
			return result;
		}

		private static boolean hasClass(Element e, String cls) {
			for (String c : e.getAttribute("class").trim().split("\\s+")) { if (c.equals(cls)) return true; }
			return false;
		}

		private static Element findGroup(Element body, String id, String cls, String altCls) {
			for (Element group : descendantsByLocalName(body, "group")) { if (id.equals(group.getAttribute("id"))) return group; }
			for (Element group : descendantsByLocalName(body, "group")) { if (hasClass(group, cls) || (altCls != null && hasClass(group, altCls))) return group; }
			return null;
		}

		private static Element findUdc(Element body, String typeSuffix) {
			for (Element udc : descendantsByLocalName(body, "udc")) { if (udc.getAttribute("type").endsWith(typeSuffix)) return udc; }
			return null;
		}

		private static void stripWhitespace(Node node) {
			NodeList children = node.getChildNodes();
			for (int i = children.getLength() - 1; i >= 0; i--) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.TEXT_NODE && child.getTextContent().trim().isEmpty()) node.removeChild(child);
				else if (child.getNodeType() == Node.ELEMENT_NODE) stripWhitespace(child);
			}
		}

		private static int parsePx(String value, int defaultValue) {
			try { return Integer.parseInt(value.replace("px", "").trim()); } catch (Exception e) { return defaultValue; }
		}

		private static List<String> lines(String text) {
			List<String> result = new ArrayList<String>();
			for (String line : (text == null ? "" : text).split("\\r?\\n")) { if (!line.trim().isEmpty()) result.add(line.trim()); }
			if (result.isEmpty()) result.add("");
			return result;
		}

		private static boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }

		private static String firstNonBlank(String... values) {
			for (String value : values) { if (hasText(value)) return value.trim(); }
			return "";
		}
	}
}
