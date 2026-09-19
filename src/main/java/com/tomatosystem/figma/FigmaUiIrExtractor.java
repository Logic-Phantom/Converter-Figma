package com.tomatosystem.figma;

import com.tomatosystem.figma.FigmaNodes.Box;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Figma screen (frame) → UI-IR JSON (the contract of src/main/resources/exconverter/ui-ir.schema.json).
 *
 * <p>Unlike the image pipeline of eXConverter-AI there is nothing to guess: texts, component identities and absolute
 * positions are exact, so this is deterministic. The designer's layer hierarchy is not trusted ("Group 815906",
 * "Frame 1000004302"); controls, texts and tables are collected as atoms with absolute boxes and re-segmented
 * top-to-bottom into regions (title, search, grid, form, buttons ...). Template selection and CLX generation then
 * run unchanged on the UI-IR.
 */
public class FigmaUiIrExtractor {
	private static final String COUNT_TEXT = "^\\[?\\s*(총\\s*(건수)?\\s*)?[\\d,]+\\s*(건|회|개|명|row|rows)\\s*]?$|^총\\s*[\\d,]+.*$";
	private static final String EXCEL_TEXT = ".*(엑셀|excel|xls|다운로드|download).*";
	/** Only a bare search caption marks a search bar; "평가의뢰 조회" is a business action. */
	private static final String SEARCH_BUTTON = "(조회|검색|찾기|조회하기|검색하기|search|find)";

	private final FigmaDocument document;
	private final FigmaComponentClassifier classifier;
	private final List<String> warnings = new ArrayList<String>();

	public FigmaUiIrExtractor(FigmaDocument document) {
		this.document = document;
		this.classifier = new FigmaComponentClassifier(document);
	}

	enum Kind { TEXT, CONTROL, GRID, PAGINATION, TITLE }

	/** One meaningful piece of the screen with its absolute box. */
	static final class Atom {
		final Kind kind;
		final Box box;
		String text = "";
		String component = "";
		String caption = "";
		double fontSize;
		int fontWeight = 400;
		boolean required;
		GridInfo grid;
		boolean used;
		Atom(Kind kind, Box box) { this.kind = kind; this.box = box; }
		boolean isInput() { return kind == Kind.CONTROL && !FigmaComponentClassifier.BUTTON.equals(component) && !isTab(); }
		boolean isTab() { return kind == Kind.CONTROL && FigmaComponentClassifier.TAB.equals(component); }
		boolean isButton() { return kind == Kind.CONTROL && FigmaComponentClassifier.BUTTON.equals(component); }
		boolean isChoice() { return kind == Kind.CONTROL && (FigmaComponentClassifier.RADIO.equals(component) || FigmaComponentClassifier.CHECKBOX.equals(component)); }
		@Override public String toString() { return kind + ":" + (text.isEmpty() ? component + "/" + caption : text) + box; }
	}

	static final class GridInfo {
		String title = "";
		final List<String> buttons = new ArrayList<String>();
		Boolean excel;
		boolean paging;
		final JSONArray columns = new JSONArray();
	}

	/** Result of one screen. */
	public static final class Screen {
		public final String name;
		public final JSONObject uiIr;
		public final List<String> warnings;
		Screen(String name, JSONObject uiIr, List<String> warnings) { this.name = name; this.uiIr = uiIr; this.warnings = warnings; }
	}

	public Screen extract(JSONObject screenNode) {
		warnings.clear();
		Box frame = FigmaNodes.boxOrUnion(screenNode);
		if (frame == null) throw new IllegalArgumentException("Screen has no visible layers: " + FigmaNodes.name(screenNode));
		List<Atom> atoms = new ArrayList<Atom>();
		for (JSONObject child : FigmaDocument.children(screenNode)) collect(child, atoms);

		JSONArray regions = new JSONArray();
		String title = takeTitle(atoms, frame);
		if (!title.isEmpty()) regions.put(new JSONObject().put("type", "title").put("text", title));
		dropPlaceholders(atoms);
		attachToGrids(atoms);
		attachChoiceLabels(atoms);
		List<Atom> rest = new ArrayList<Atom>();
		for (Atom a : atoms) { if (!a.used && a.kind != Kind.PAGINATION && a.kind != Kind.TITLE) rest.add(a); }
		buildRegions(rest, frame, "", regions);
		decideSearch(regions);
		markTabContent(regions);
		if (regions.length() == 0) throw new IllegalArgumentException("화면에서 변환할 요소(제목/입력/표/버튼)를 찾지 못했습니다: " + FigmaNodes.name(screenNode));

		String layerName = FigmaNodes.name(screenNode).replaceAll("\\s*\\(텍스트 입력\\)", "").trim();
		String screenName = title.isEmpty() ? layerName : title;
		JSONObject screen = new JSONObject()
			.put("name", screenName)
			.put("type", isPopup(layerName, frame) ? "POPUP" : "")
			.put("width", Math.max(320, (int) Math.round(frame.w)))
			.put("height", Math.max(320, (int) Math.round(frame.h)))
			.put("sourceWidth", (int) Math.round(frame.w));
		JSONObject uiIr = new JSONObject().put("screen", screen).put("regions", regions)
			.put("source", new JSONObject().put("figmaNodeId", screenNode.optString("id")).put("figmaName", FigmaNodes.name(screenNode)));
		return new Screen(layerName.isEmpty() ? screenName : layerName, uiIr, new ArrayList<String>(warnings));
	}

	// ------------------------------------------------------------------ atom collection

	private void collect(JSONObject node, List<Atom> out) {
		if (!FigmaNodes.isVisible(node)) return;
		String type = FigmaNodes.type(node);
		Box box = FigmaNodes.box(node);
		if ("TEXT".equals(type)) {
			String text = FigmaNodes.characters(node);
			if (text.isEmpty() || box == null) return;
			Atom a = new Atom(Kind.TEXT, box);
			a.text = text;
			a.fontSize = FigmaNodes.fontSize(node);
			a.fontWeight = FigmaNodes.fontWeight(node);
			out.add(a);
			return;
		}
		if (box != null && box.w <= 0 && box.h <= 0) return;
		if (box != null && classifier.isTitleName(node) && !"INSTANCE".equals(type)) {
			JSONObject largest = largestText(node);
			if (largest != null) {
				Atom a = new Atom(Kind.TITLE, FigmaNodes.box(largest));
				a.text = FigmaNodes.characters(largest);
				a.fontSize = FigmaNodes.fontSize(largest);
				out.add(a);
				return;
			}
		}
		String kind = classifier.classify(node);
		if (FigmaComponentClassifier.IGNORE.equals(kind)) return;
		if (box != null && (classifier.isGridName(node) && kind == null || kind == null && isContainer(type))) {
			GridInfo grid = extractGrid(node, !classifier.isGridName(node));
			if (grid != null) {
				Atom a = new Atom(Kind.GRID, box);
				a.grid = grid;
				out.add(a);
				return;
			}
		}
		if (FigmaComponentClassifier.PAGINATION.equals(kind) && box != null) { out.add(new Atom(Kind.PAGINATION, box)); return; }
		if (kind != null && box != null) { out.add(control(node, kind, box)); return; }
		for (JSONObject child : FigmaDocument.children(node)) collect(child, out);
	}

	private static boolean isContainer(String type) {
		return "FRAME".equals(type) || "GROUP".equals(type) || "SECTION".equals(type) || "INSTANCE".equals(type) || "COMPONENT".equals(type);
	}

	private Atom control(JSONObject node, String kind, Box box) {
		Atom a = new Atom(Kind.CONTROL, box);
		a.component = kind;
		String property = FigmaNodes.textProperty(node);
		List<JSONObject> texts = FigmaNodes.visibleTexts(node);
		String inner = texts.isEmpty() ? "" : FigmaNodes.characters(texts.get(0));
		if (FigmaComponentClassifier.BUTTON.equals(kind)) {
			a.caption = !property.isEmpty() ? property : !inner.isEmpty() ? inner : genericName(FigmaNodes.name(node));
			a.text = FigmaNodes.normalize(FigmaNodes.name(node) + " " + document.componentName(node)); // for Excel detection
		} else {
			a.caption = !inner.isEmpty() ? inner : property;
		}
		return a;
	}

	/** "Button-6" / "Button" say nothing about the caption; a free layer name like "저장" does. */
	private static String genericName(String layerName) {
		String n = layerName.trim();
		return n.matches("(?i)(button|btn|버튼)[\\s_\\-]*\\d*") || n.isEmpty() ? "버튼" : n;
	}

	private static JSONObject largestText(JSONObject node) {
		JSONObject best = null;
		for (JSONObject text : FigmaNodes.visibleTexts(node)) {
			if (best == null || FigmaNodes.fontSize(text) > FigmaNodes.fontSize(best)) best = text;
		}
		return best;
	}

	// ------------------------------------------------------------------ grid

	/**
	 * Table → columns. Rows are texts/controls clustered by vertical centre; the header row is the first row with at
	 * least half of the widest row's texts. Texts and buttons above the header are the grid's title row.
	 *
	 * @param strict structural detection of an unnamed container: at least 3 header columns and 2 aligned data rows,
	 *               no input controls, nothing below the table
	 */
	private GridInfo extractGrid(JSONObject node, boolean strict) {
		List<Atom> items = new ArrayList<Atom>();
		boolean[] paging = { false };
		collectGridItems(node, items, paging);
		if (items.isEmpty()) return null;
		List<List<Atom>> rows = rows(items);
		int widest = 0;
		for (List<Atom> row : rows) widest = Math.max(widest, count(row, Kind.TEXT));
		int header = -1;
		for (int i = 0; i < rows.size() && header < 0; i++) {
			int texts = count(rows.get(i), Kind.TEXT);
			if (texts >= 2 && texts * 2 >= widest) header = i;
		}
		if (header < 0) return null;
		List<Atom> headerTexts = new ArrayList<Atom>();
		for (Atom a : rows.get(header)) { if (a.kind == Kind.TEXT) headerTexts.add(a); }
		headerTexts.sort(Comparator.comparingDouble(a -> a.box.x));
		List<List<Atom>> data = rows.subList(header + 1, rows.size());
		if (data.isEmpty()) return null;
		Box area = FigmaNodes.box(node);
		double left = area.x;
		double[] starts = new double[headerTexts.size()];
		double[] ends = new double[headerTexts.size()];
		for (int c = 0; c < headerTexts.size(); c++) {
			starts[c] = c == 0 ? Math.min(headerTexts.get(0).box.x, left + 1) : (headerTexts.get(c - 1).box.right() + headerTexts.get(c).box.x) / 2;
			ends[c] = c + 1 < headerTexts.size() ? (headerTexts.get(c).box.right() + headerTexts.get(c + 1).box.x) / 2 : area.right();
		}
		// A checkbox left of the first header text is the row-selection column.
		boolean selector = false;
		for (List<Atom> row : rows.subList(header, rows.size())) {
			for (Atom a : row) { if (FigmaComponentClassifier.CHECKBOX.equals(a.component) && a.box.right() <= headerTexts.get(0).box.x + 2) selector = true; }
		}
		if (selector) starts[0] = headerTexts.get(0).box.x;
		// A header row never holds inputs; label|control|label|control rows are a form drawn as a table (등록 테이블).
		for (Atom a : rows.get(header)) {
			if (a.kind == Kind.CONTROL && !a.isButton() && !(FigmaComponentClassifier.CHECKBOX.equals(a.component) && a.box.right() <= headerTexts.get(0).box.x + 2)) return null;
		}
		// Grid headers share one text style; "label | value | label | value" rows alternate label and value styles.
		java.util.Set<String> styles = new java.util.HashSet<String>();
		for (Atom t : headerTexts) styles.add(Math.round(t.fontSize) + "/" + t.fontWeight);
		if (styles.size() > 1) return null;

		if (strict) {
			if (headerTexts.size() < 3 || data.size() < 2 || header > 1) return null;
			for (Atom a : items) { if (a.isInput() && !a.isChoice()) return null; }
			for (List<Atom> row : data) {
				int aligned = 0;
				for (Atom a : row) { if (columnOf(a, starts, ends) >= 0) aligned++; }
				if (aligned * 10 < headerTexts.size() * 6) return null;
			}
		}

		GridInfo info = new GridInfo();
		info.paging = paging[0];
		for (int r = 0; r < header; r++) {
			for (Atom a : rows.get(r)) addTitleRowItem(info, a);
		}
		if (selector) info.columns.put(column("", "checkbox", (int) Math.round(headerTexts.get(0).box.x - left), ""));
		for (int c = 0; c < headerTexts.size(); c++) {
			Map<String, Integer> editors = new HashMap<String, Integer>();
			String cellText = "";
			for (int r = 0; r < Math.min(5, data.size()); r++) {
				for (Atom a : data.get(r)) {
					if (columnOf(a, starts, ends) != c || FigmaComponentClassifier.CHECKBOX.equals(a.component) && selector && c == 0 && a.box.right() <= headerTexts.get(0).box.x + 2) continue;
					String editor = a.kind == Kind.TEXT ? "output" : a.component;
					editors.merge(editor, 1, Integer::sum);
					// Design-mode grids show the bound column name (FNM, EMAIL, USER_ID); "C34" is sample data.
					if (r == 0 && a.kind == Kind.TEXT && a.text.matches("[A-Z]{2,}[A-Z0-9_]{0,28}")) cellText = a.text;
					if (r == 0 && a.isButton()) cellText = a.caption;
				}
			}
			String editor = "output";
			int best = 0;
			for (Map.Entry<String, Integer> e : editors.entrySet()) { if (e.getValue() > best) { best = e.getValue(); editor = e.getKey(); } }
			int width = (int) Math.round(Math.max(headerTexts.get(c).box.w, (c + 1 < headerTexts.size() ? headerTexts.get(c + 1).box.x : ends[c]) - headerTexts.get(c).box.x));
			info.columns.put(column(headerTexts.get(c).text.replace('\n', ' '), editor, width, cellText));
		}
		return info;
	}

	private void collectGridItems(JSONObject node, List<Atom> out, boolean[] paging) {
		for (JSONObject child : FigmaDocument.children(node)) {
			if (!FigmaNodes.isVisible(child)) continue;
			Box box = FigmaNodes.box(child);
			if ("TEXT".equals(FigmaNodes.type(child))) {
				String text = FigmaNodes.characters(child);
				if (text.isEmpty() || box == null) continue;
				Atom a = new Atom(Kind.TEXT, box);
				a.text = text;
				a.fontSize = FigmaNodes.fontSize(child);
				a.fontWeight = FigmaNodes.fontWeight(child);
				out.add(a);
				continue;
			}
			String kind = classifier.classify(child);
			if (FigmaComponentClassifier.IGNORE.equals(kind)) continue;
			if (FigmaComponentClassifier.PAGINATION.equals(kind)) { paging[0] = true; continue; }
			if (kind != null && box != null) { out.add(control(child, kind, box)); continue; }
			collectGridItems(child, out, paging);
		}
	}

	private static int columnOf(Atom a, double[] starts, double[] ends) {
		double cx = a.box.cx();
		for (int c = 0; c < starts.length; c++) { if (cx >= starts[c] - 2 && cx < ends[c] + 2) return c; }
		return -1;
	}

	private static JSONObject column(String header, String editor, int width, String cellText) {
		return new JSONObject().put("header", header).put("editor", editor).put("width", Math.max(0, width)).put("cellText", cellText);
	}

	/** Title row of a grid: a heading, the count ("총 123건", drawn by udcComGridTitle), buttons, Excel download. */
	private static void addTitleRowItem(GridInfo info, Atom a) {
		if (a.kind == Kind.TEXT) {
			if (a.text.replace(" ", "").matches(COUNT_TEXT.replace(" ", "")) || a.text.matches(COUNT_TEXT)) return;
			if (info.title.isEmpty() && a.text.length() <= 40) info.title = a.text.replace('\n', ' ');
		} else if (a.isButton()) {
			if ((a.caption + " " + a.text).toLowerCase(Locale.ROOT).matches(EXCEL_TEXT)) info.excel = Boolean.TRUE;
			else info.buttons.add(a.caption);
		}
	}

	// ------------------------------------------------------------------ association

	/**
	 * The biggest bold text near the top (or a "title" frame) is the screen title (udcComAppHeader). Breadcrumbs and
	 * favourites icons are ignored by the classifier.
	 */
	private String takeTitle(List<Atom> atoms, Box frame) {
		Atom best = null;
		for (Atom a : atoms) {
			if (a.kind == Kind.TITLE && (best == null || a.box.y < best.box.y)) best = a;
		}
		if (best == null) {
			double firstContent = frame.bottom();
			for (Atom a : atoms) { if (a.kind != Kind.TEXT) firstContent = Math.min(firstContent, a.box.y); }
			for (Atom a : atoms) {
				if (a.kind != Kind.TEXT || a.fontSize < 18 || a.box.bottom() > firstContent + 1 || a.box.y > frame.y + Math.max(160, frame.h * 0.2)) continue;
				if (best == null || a.fontSize > best.fontSize || a.fontSize == best.fontSize && a.box.y < best.box.y) best = a;
			}
		}
		if (best == null) return "";
		best.used = true;
		for (Atom a : atoms) { if (a.kind == Kind.TITLE) a.used = true; }
		return best.text.replace('\n', ' ').trim();
	}

	/** Buttons / heading / count text right above a grid form its title row; a pagination right below sets paging. */
	private void attachToGrids(List<Atom> atoms) {
		for (Atom grid : atoms) {
			if (grid.kind != Kind.GRID) continue;
			for (Atom a : atoms) {
				if (a.used || a == grid) continue;
				if (a.kind == Kind.PAGINATION && a.box.y >= grid.box.bottom() - 4 && a.box.y <= grid.box.bottom() + 80 && a.box.overlapsX(grid.box)) {
					grid.grid.paging = true;
					a.used = true;
				}
			}
			List<Atom> above = new ArrayList<Atom>();
			for (Atom a : atoms) {
				if (a.used || a == grid || !(a.kind == Kind.TEXT || a.isButton())) continue;
				if (a.box.bottom() <= grid.box.y + 4 && a.box.bottom() >= grid.box.y - 64 && a.box.overlapsX(grid.box)) above.add(a);
			}
			// Not when that line holds input fields: then it is a search/form row, not the grid's title row.
			boolean fieldLine = false;
			for (Atom a : above) {
				for (Atom other : atoms) { if (other.isInput() && !other.used && overlapY(a.box, other.box)) fieldLine = true; }
			}
			if (fieldLine) continue;
			above.sort(Comparator.comparingDouble(a -> a.box.x));
			for (Atom a : above) { addTitleRowItem(grid.grid, a); a.used = true; }
		}
	}

	/** Placeholder/value texts drawn on top of an input ("아이디를 입력해주세요") belong to that input, not the layout. */
	private static void dropPlaceholders(List<Atom> atoms) {
		for (Atom text : atoms) {
			if (text.kind != Kind.TEXT || text.used) continue;
			for (Atom input : atoms) {
				if (!input.isInput() || !input.box.contains(text.box)) continue;
				if (input.caption.isEmpty()) input.caption = text.text;
				text.used = true;
				break;
			}
		}
	}

	/** Regions below a tab bar are drawn inside the selected tab (UI-IR inTab), except a closing footer button row. */
	private static void markTabContent(JSONArray regions) {
		int tabs = -1;
		for (int i = 0; i < regions.length(); i++) {
			JSONObject region = regions.getJSONObject(i);
			if ("tabs".equals(region.getString("type"))) { tabs = i; continue; }
			if (tabs < 0) continue;
			boolean footer = i == regions.length() - 1 && "buttons".equals(region.getString("type"));
			if (!footer && region.optString("side").equals(regions.getJSONObject(tabs).optString("side"))) region.put("inTab", true);
		}
	}

	/** A radio/checkbox without its own caption takes the text just to its right. */
	private void attachChoiceLabels(List<Atom> atoms) {
		for (Atom choice : atoms) {
			if (!choice.isChoice() || !choice.caption.isEmpty()) continue;
			Atom best = null;
			for (Atom t : atoms) {
				if (t.used || t.kind != Kind.TEXT || !overlapY(choice.box, t.box)) continue;
				double gap = t.box.x - choice.box.right();
				if (gap >= -2 && gap <= 40 && (best == null || t.box.x < best.box.x)) best = t;
			}
			if (best != null) { choice.caption = best.text; best.used = true; }
		}
	}

	// ------------------------------------------------------------------ segmentation

	/**
	 * Groups atoms into horizontal bands (atoms overlapping vertically, transitively) from top to bottom. A band that
	 * holds two tables/forms side by side is a left/right split; each side is segmented on its own.
	 */
	private void buildRegions(List<Atom> atoms, Box frame, String side, JSONArray regions) {
		List<List<Atom>> bands = bands(atoms);
		List<List<Atom>> lines = new ArrayList<List<Atom>>();
		for (List<Atom> band : bands) {
			List<Atom>[] split = side.isEmpty() ? splitSideBySide(band) : null;
			if (split != null) {
				flushLines(lines, frame, side, regions);
				buildRegions(split[0], frame, "left", regions);
				buildRegions(split[1], frame, "right", regions);
			} else {
				lines.add(band);
			}
		}
		flushLines(lines, frame, side, regions);
	}

	/** x of the texts that label an input (text directly left of a control), per flush; marks label columns of form tables. */
	private final List<Double> labelXs = new ArrayList<Double>();

	private void flushLines(List<List<Atom>> lines, Box frame, String side, JSONArray regions) {
		labelXs.clear();
		for (List<Atom> line : lines) {
			List<Atom> sorted = sortedX(line);
			for (int k = 1; k < sorted.size(); k++) {
				if (sorted.get(k).isInput() && sorted.get(k - 1).kind == Kind.TEXT) labelXs.add(sorted.get(k - 1).box.x);
			}
		}
		int i = 0;
		while (i < lines.size()) {
			List<Atom> line = lines.get(i);
			Atom grid = first(line, Kind.GRID);
			if (grid != null) {
				regions.put(gridRegion(grid, side));
				i++;
				continue;
			}
			if (hasTab(line)) {
				JSONArray tabs = new JSONArray();
				for (Atom a : sortedX(line)) { if (a.isTab()) tabs.put(a.caption.isEmpty() ? "탭 " + (tabs.length() + 1) : a.caption); }
				regions.put(withSide(new JSONObject().put("type", "tabs").put("tabs", tabs), side));
				i++;
				continue;
			}
			// A heading and/or buttons right above a field group is that form's title row (udcComFormTitle + buttons).
			List<Atom> titleRow = null;
			if (!isFieldLine(lines, i) && i + 1 < lines.size() && isFieldLine(lines, i + 1)
				&& count(line, Kind.TEXT) <= 2 && lines.get(i + 1).get(0).box.y - bottom(java.util.Collections.singletonList(line)) < 60) {
				titleRow = line;
				line = lines.get(++i);
			}
			if (isFieldLine(lines, i)) {
				// Consecutive field lines (and a caption line right above controls) form one search/form group.
				List<List<Atom>> group = new ArrayList<List<Atom>>();
				while (i < lines.size() && first(lines.get(i), Kind.GRID) == null && isFieldLine(lines, i)) group.add(lines.get(i++));
				JSONObject region = fieldRegion(group, side);
				if (titleRow != null) {
					JSONArray buttons = new JSONArray();
					for (Atom a : sortedX(titleRow)) {
						if (a.isButton()) buttons.put(a.caption);
						else if (a.kind == Kind.TEXT && !a.text.matches(COUNT_TEXT) && region.optString("title").isEmpty()) region.put("title", a.text.replace('\n', ' '));
					}
					for (int b = 0; b < region.getJSONArray("buttons").length(); b++) buttons.put(region.getJSONArray("buttons").get(b));
					region.put("buttons", buttons);
				}
				// A buttons-only line right under the fields (not the last line of the screen) belongs to them.
				if (i < lines.size() - 1 && onlyButtons(lines.get(i)) && lines.get(i).get(0).box.y - bottom(group) < 60) {
					for (Atom b : sortedX(lines.get(i))) region.getJSONArray("buttons").put(b.caption);
					i++;
				}
				regions.put(region);
				continue;
			}
			if (onlyButtons(line) || hasButton(line)) {
				JSONArray buttons = new JSONArray();
				List<Atom> texts = new ArrayList<Atom>();
				for (Atom a : sortedX(line)) { if (a.isButton()) buttons.put(a.caption); else if (a.kind == Kind.TEXT && !a.text.matches(COUNT_TEXT)) texts.add(a); }
				if (!texts.isEmpty()) regions.put(textRegion(texts, side));
				regions.put(withSide(new JSONObject().put("type", "buttons").put("align", align(line, frame)).put("buttons", buttons), side));
				i++;
				continue;
			}
			List<Atom> texts = new ArrayList<Atom>();
			for (Atom a : sortedX(line)) { if (a.kind == Kind.TEXT && !a.text.matches(COUNT_TEXT)) texts.add(a); }
			if (!texts.isEmpty()) regions.put(textRegion(texts, side));
			i++;
		}
		lines.clear();
	}

	private boolean isFieldLine(List<List<Atom>> lines, int i) {
		List<Atom> line = lines.get(i);
		return hasInput(line) || isLabelLineFor(lines, i) || isValueLine(line);
	}

	/**
	 * A text-only row of a form table: "프로젝트 번호 | CG-202407-001 | 프로젝트 명 | ...". It starts with a text in a
	 * label column (x of a text that labels an input elsewhere in the table) and also holds value texts.
	 */
	private boolean isValueLine(List<Atom> line) {
		if (labelXs.isEmpty()) return false;
		List<Atom> sorted = sortedX(line);
		if (sorted.size() < 2) return false;
		boolean value = false;
		for (Atom a : sorted) { if (a.kind != Kind.TEXT && !a.isButton()) return false; if (a.kind == Kind.TEXT && !isLabelColumn(a)) value = true; }
		return sorted.get(0).kind == Kind.TEXT && isLabelColumn(sorted.get(0)) && value;
	}

	/** Without known label columns every text after a label counts as a value. */
	private boolean isLabelColumn(Atom text) {
		for (double x : labelXs) { if (Math.abs(text.box.x - x) <= 8) return true; }
		return false;
	}

	/** A text-only line whose texts sit right above the unlabelled controls of the next line (labels on top). */
	private boolean isLabelLineFor(List<List<Atom>> lines, int i) {
		if (i + 1 >= lines.size() || hasInput(lines.get(i)) || !hasInput(lines.get(i + 1))) return false;
		for (Atom a : lines.get(i)) { if (a.kind != Kind.TEXT) return false; }
		List<Atom> next = sortedX(lines.get(i + 1));
		if (!next.isEmpty() && next.get(0).kind == Kind.TEXT) return false; // the next line has labels on the left
		return lines.get(i + 1).get(0).box.y - bottom(java.util.Collections.singletonList(lines.get(i))) < 24;
	}

	private JSONObject gridRegion(Atom grid, String side) {
		GridInfo g = grid.grid;
		JSONObject region = new JSONObject().put("type", "grid").put("title", g.title).put("columns", g.columns).put("paging", g.paging);
		JSONArray buttons = new JSONArray();
		for (String b : g.buttons) buttons.put(b);
		region.put("buttons", buttons);
		// Without an Excel button in the design, hide udcComGridTitle's default Excel export.
		region.put("excel", g.excel != null && g.excel);
		return withSide(region, side);
	}

	/** Fields of one group: label text followed by its control(s); radios/checkboxes after one label are its options. */
	private JSONObject fieldRegion(List<List<Atom>> lines, String side) {
		JSONArray fields = new JSONArray();
		JSONArray buttons = new JSONArray();
		int perRow = 1;
		List<Atom> labelsAbove = new ArrayList<Atom>();
		for (List<Atom> line : lines) {
			if (!hasInput(line) && !isValueLine(line)) { labelsAbove.addAll(line); continue; }
			List<FieldBuild> built = new ArrayList<FieldBuild>();
			FieldBuild current = null;
			String pendingLabel = null;
			boolean pendingRequired = false;
			for (Atom a : sortedX(line)) {
				if (a.kind == Kind.TEXT) {
					String t = a.text.trim();
					if (t.matches("[*＊]")) { if (pendingLabel != null) pendingRequired = true; else if (current != null && current.controls.isEmpty()) current.required = true; else pendingRequired = true; continue; }
					if (t.matches("[-~∼〜]") && current != null) { current.separator = t; continue; }
					if (current != null && !current.controls.isEmpty() && t.length() <= 3 && !t.matches(".*[가-힣]{2,}.*")) continue; // unit text such as "원", "%"
					if (pendingLabel != null && !isLabelColumn(a)) {
						// label → plain text in a form table: a read-only value (output).
						FieldBuild output = new FieldBuild(pendingLabel, pendingRequired);
						output.value = t;
						built.add(output);
						pendingLabel = null;
						pendingRequired = false;
						current = null;
						continue;
					}
					current = null;
					pendingLabel = t;
				} else if (a.isButton()) {
					buttons.put(a.caption);
				} else if (a.isInput()) {
					if (pendingLabel != null) {
						current = new FieldBuild(pendingLabel, pendingRequired);
						built.add(current);
						pendingLabel = null;
						pendingRequired = false;
					} else if (current == null) {
						Atom above = labelAbove(a, labelsAbove);
						current = new FieldBuild(above == null ? "" : above.text, false);
						built.add(current);
					}
					current.controls.add(a);
				}
			}
			labelsAbove.clear();
			perRow = Math.max(perRow, built.size());
			for (FieldBuild f : built) fields.put(f.toJson(fields.length() + 1));
		}
		return withSide(new JSONObject().put("type", "form").put("columnsPerRow", Math.min(6, perRow)).put("fields", fields).put("buttons", buttons), side);
	}

	private static Atom labelAbove(Atom control, List<Atom> texts) {
		Atom best = null;
		for (Atom t : texts) {
			if (t.kind != Kind.TEXT || t.used || !t.box.overlapsX(control.box) || t.box.bottom() > control.box.y + 2) continue;
			if (best == null || t.box.bottom() > best.box.bottom()) best = t;
		}
		if (best != null) best.used = true;
		return best;
	}

	private static final class FieldBuild {
		final String label;
		boolean required;
		String separator = "";
		/** Read-only value of a label without a control. */
		String value;
		final List<Atom> controls = new ArrayList<Atom>();
		FieldBuild(String rawLabel, boolean required) {
			String label = rawLabel.replace('\n', ' ').trim();
			this.required = required || label.contains("*") || label.contains("＊");
			this.label = label.replaceAll("[*＊]", "").replaceAll("[:：]\\s*$", "").trim();
		}

		JSONObject toJson(int index) {
			if (controls.isEmpty()) {
				return new JSONObject().put("label", label.isEmpty() ? "항목 " + index : label).put("component", "output").put("required", required).put("value", value == null ? "" : value.replace('\n', ' '));
			}
			String component = controls.get(0).component;
			JSONArray options = new JSONArray();
			boolean allRadio = true, allCheck = true, allDate = true;
			for (Atom c : controls) {
				allRadio &= FigmaComponentClassifier.RADIO.equals(c.component);
				allCheck &= FigmaComponentClassifier.CHECKBOX.equals(c.component);
				allDate &= c.component.startsWith("date");
			}
			if (allRadio) { component = "radiobutton"; for (Atom c : controls) if (!c.caption.isEmpty()) options.put(c.caption); }
			else if (allCheck && controls.size() > 1) { component = "checkboxgroup"; for (Atom c : controls) if (!c.caption.isEmpty()) options.put(c.caption); }
			else if (allDate && controls.size() == 2) component = "daterange";
			String name = label;
			if (name.isEmpty()) name = allCheck && controls.size() == 1 && !controls.get(0).caption.isEmpty() ? controls.get(0).caption : "항목 " + index;
			JSONObject json = new JSONObject().put("label", name).put("component", component).put("required", required);
			if (options.length() > 0) json.put("options", options);
			if ("combobox".equals(component) && !controls.get(0).caption.isEmpty()) json.put("value", controls.get(0).caption);
			return json;
		}
	}

	private static JSONObject textRegion(List<Atom> texts, String side) {
		Atom first = texts.get(0);
		StringBuilder text = new StringBuilder();
		for (Atom t : texts) { if (text.length() > 0) text.append(' '); text.append(t.text); }
		boolean heading = texts.size() == 1 && (first.fontWeight >= 600 && first.fontSize >= 14 || first.fontSize >= 18) && first.text.length() <= 40;
		return withSide(new JSONObject().put("type", heading ? "sectionTitle" : "description").put("text", text.toString()), side);
	}

	/**
	 * The first full-width field group above any grid is the search bar when it has a search button (조회/검색), or
	 * when it sits right under the page title without a heading of its own and a grid follows. A titled group
	 * ("프로젝트상세") is a form.
	 */
	private static void decideSearch(JSONArray regions) {
		for (int i = 0; i < regions.length(); i++) {
			JSONObject region = regions.getJSONObject(i);
			String type = region.getString("type");
			if ("grid".equals(type) || "tabs".equals(type)) return;
			if (!"form".equals(type) || !region.optString("side").isEmpty()) continue;
			boolean searchButton = false;
			JSONArray buttons = region.getJSONArray("buttons");
			for (int b = 0; b < buttons.length(); b++) searchButton |= buttons.getString(b).replace(" ", "").toLowerCase(Locale.ROOT).matches(SEARCH_BUTTON);
			boolean gridFollows = false;
			for (int j = i + 1; j < regions.length(); j++) gridFollows |= "grid".equals(regions.getJSONObject(j).getString("type"));
			boolean underTitle = i == 0 || i == 1 && "title".equals(regions.getJSONObject(0).getString("type"));
			if (searchButton || gridFollows && underTitle && region.optString("title").isEmpty()) region.put("type", "search");
			return;
		}
	}

	// ------------------------------------------------------------------ geometry helpers

	private static List<List<Atom>> bands(List<Atom> atoms) {
		List<Atom> sorted = new ArrayList<Atom>(atoms);
		sorted.sort(Comparator.comparingDouble((Atom a) -> a.box.y).thenComparingDouble(a -> a.box.x));
		List<List<Atom>> bands = new ArrayList<List<Atom>>();
		List<Atom> current = null;
		double bottom = 0;
		for (Atom a : sorted) {
			// Overlap must be real (2px) so that stacked rows with touching edges stay separate.
			if (current != null && a.box.y < bottom - 2) { current.add(a); bottom = Math.max(bottom, a.box.bottom()); continue; }
			current = new ArrayList<Atom>();
			current.add(a);
			bands.add(current);
			bottom = a.box.bottom();
		}
		return bands;
	}

	/** Rows of a table: items whose vertical centres are within half the smaller item height. */
	private static List<List<Atom>> rows(List<Atom> items) {
		List<Atom> sorted = new ArrayList<Atom>(items);
		sorted.sort(Comparator.comparingDouble(a -> a.box.cy()));
		List<List<Atom>> rows = new ArrayList<List<Atom>>();
		List<Atom> current = null;
		double center = 0;
		for (Atom a : sorted) {
			double tolerance = Math.max(4, Math.min(a.box.h, 24) / 2);
			if (current != null && Math.abs(a.box.cy() - center) <= tolerance) { current.add(a); continue; }
			current = new ArrayList<Atom>();
			current.add(a);
			rows.add(current);
			center = a.box.cy();
		}
		return rows;
	}

	/** Two tables/forms side by side: split at the widest horizontal gap that leaves a major block on each side. */
	@SuppressWarnings("unchecked")
	private static List<Atom>[] splitSideBySide(List<Atom> band) {
		int majors = 0;
		for (Atom a : band) { if (a.kind == Kind.GRID) majors++; }
		if (majors == 0 || band.size() < 2) return null;
		List<Atom> sorted = sortedX(band);
		double reach = sorted.get(0).box.right();
		double bestGap = 0;
		int bestIndex = -1;
		for (int i = 1; i < sorted.size(); i++) {
			double gap = sorted.get(i).box.x - reach;
			if (gap > bestGap) { bestGap = gap; bestIndex = i; }
			reach = Math.max(reach, sorted.get(i).box.right());
		}
		if (bestIndex < 0 || bestGap < 8) return null;
		List<Atom> left = new ArrayList<Atom>(sorted.subList(0, bestIndex));
		List<Atom> right = new ArrayList<Atom>(sorted.subList(bestIndex, sorted.size()));
		if (!isMajor(left) || !isMajor(right)) return null;
		return new List[] { left, right };
	}

	private static boolean isMajor(List<Atom> side) {
		int inputs = 0;
		for (Atom a : side) { if (a.kind == Kind.GRID) return true; if (a.isInput()) inputs++; }
		return inputs >= 2;
	}

	private static String align(List<Atom> line, Box frame) {
		double l = Double.MAX_VALUE, r = -Double.MAX_VALUE;
		for (Atom a : line) { l = Math.min(l, a.box.x); r = Math.max(r, a.box.right()); }
		double center = (l + r) / 2;
		if (Math.abs(center - frame.cx()) < frame.w * 0.1) return "center";
		return center > frame.cx() ? "right" : "left";
	}

	private static boolean overlapY(Box a, Box b) { return a.y < b.bottom() && b.y < a.bottom(); }

	private static List<Atom> sortedX(List<Atom> atoms) {
		List<Atom> sorted = new ArrayList<Atom>(atoms);
		sorted.sort(Comparator.comparingDouble(a -> a.box.x));
		return sorted;
	}

	private static double bottom(List<List<Atom>> lines) {
		double b = -Double.MAX_VALUE;
		for (List<Atom> line : lines) for (Atom a : line) b = Math.max(b, a.box.bottom());
		return b;
	}

	private static Atom first(List<Atom> atoms, Kind kind) { for (Atom a : atoms) if (a.kind == kind) return a; return null; }
	private static int count(List<Atom> atoms, Kind kind) { int n = 0; for (Atom a : atoms) if (a.kind == kind) n++; return n; }
	private static boolean hasInput(List<Atom> atoms) { for (Atom a : atoms) if (a.isInput()) return true; return false; }
	private static boolean hasButton(List<Atom> atoms) { for (Atom a : atoms) if (a.isButton()) return true; return false; }
	private static boolean hasTab(List<Atom> atoms) { for (Atom a : atoms) if (a.isTab()) return true; return false; }
	private static boolean onlyButtons(List<Atom> atoms) {
		for (Atom a : atoms) if (!a.isButton() && !(a.kind == Kind.TEXT && a.text.matches(COUNT_TEXT))) return false;
		return hasButton(atoms);
	}

	private static JSONObject withSide(JSONObject region, String side) { if (!side.isEmpty()) region.put("side", side); return region; }

	private static boolean isPopup(String name, Box frame) {
		return name.toLowerCase(Locale.ROOT).matches(".*(popup|팝업|dialog|modal|레이어).*") || frame.w < 1000;
	}
}
