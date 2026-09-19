package com.tomatosystem.exconverter.service;

import com.tomatosystem.exconverter.model.UiIr;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The body layout of a screen as a sequence of blocks, so a design image and a template can be compared by order
 * and arrangement rather than by control counts alone.
 *
 * <pre>
 *   G  grid        (+b title-row buttons, +p page indexer)      F  form (+b)      T  tree (+b)
 *   A  textarea    S  search bar inside the body                X  arrow (shuttle) buttons
 *   TAB{ ... }                 tab folder with the selected tab's content
 *   DIV{ left | right }        side-by-side panes;  DIV{ left | X | right } with a shuttle column
 *   ACC{ ... } / EXT           accordion and third-party controls, which UI-IR cannot express
 * </pre>
 *
 * Examples: P4-4 = F G, P4-5 = G F, P3-2 = DIV{G | Fb}, P7-1 = DIV{G | X | G}, P5-1 = TAB{G}.
 * DIV and TAB are nested blocks: pane contents are only ever compared with pane contents, so the right pane's
 * "G X S G" is not matched against grids that sit below a template's division.
 * The header search bar, the title and the footer are not part of the body and are scored separately.
 */
public final class LayoutShape {
	static final String CL = "http://tomatosystem.co.kr/cleopatra";
	private static final int COST = 20;
	private static final int FLAG_COST = 5;
	/** Extra cost of a DIV/TAB/ACC wrapper on top of its contents. */
	private static final int WRAPPER_COST = 40;

	private LayoutShape() { }

	/** One block: a control token, or a DIV/TAB/ACC wrapper with its parts (panes, or the one tab/accordion body). */
	public static final class Block {
		final String token;
		final List<List<Block>> parts;
		Block(String token) { this.token = token; this.parts = new ArrayList<List<Block>>(); }
		boolean composite() { return "DIV".equals(token) || "TAB".equals(token) || "ACC".equals(token); }
		@Override public String toString() {
			if (!composite()) return token;
			StringBuilder sb = new StringBuilder(token).append('{');
			for (int i = 0; i < parts.size(); i++) { if (i > 0) sb.append(" | "); sb.append(join(parts.get(i))); }
			return sb.append('}').toString();
		}
	}

	public static String join(List<Block> blocks) {
		StringBuilder sb = new StringBuilder();
		for (Block b : blocks) { if (sb.length() > 0) sb.append(' '); sb.append(b); }
		return sb.toString();
	}

	// ------------------------------------------------------------------ UI-IR side

	public static List<Block> of(UiIr ir) {
		List<UiIr.Region> body = bodyRegions(ir);
		List<Block> blocks = new ArrayList<Block>();
		int first = -1;
		int last = -1;
		for (int i = 0; i < body.size(); i++) { if (!body.get(i).getSide().isEmpty()) { if (first < 0) first = i; last = i; } }
		if (first < 0) { sequence(body, blocks); return blocks; }
		sequence(body.subList(0, first), blocks);
		Split split = Split.of(body.subList(first, last + 1));
		Block division = new Block("DIV");
		division.parts.add(sequence(split.left, new ArrayList<Block>()));
		if (split.center != null) { List<Block> center = new ArrayList<Block>(); center.add(new Block("X")); division.parts.add(center); }
		division.parts.add(sequence(split.right, new ArrayList<Block>()));
		blocks.add(division);
		sequence(body.subList(last + 1, body.size()), blocks);
		return blocks;
	}

	/** Regions below the header search bar and above the footer buttons, in visual order. */
	static List<UiIr.Region> bodyRegions(UiIr ir) {
		List<UiIr.Region> regions = new ArrayList<UiIr.Region>(ir.getRegions());
		if (!regions.isEmpty() && UiIr.BUTTONS.equals(regions.get(regions.size() - 1).getType()) && !isArrows(regions.get(regions.size() - 1))) regions.remove(regions.size() - 1);
		for (int i = 0; i < regions.size(); i++) {
			if (UiIr.SEARCH.equals(regions.get(i).getType()) && regions.get(i).getSide().isEmpty()) { regions.remove(i); break; }
		}
		return regions;
	}

	private static List<Block> sequence(List<UiIr.Region> regions, List<Block> blocks) {
		for (int i = 0; i < regions.size(); i++) {
			UiIr.Region region = regions.get(i);
			if (UiIr.TABS.equals(region.getType())) {
				int j = i + 1;
				List<UiIr.Region> content = new ArrayList<UiIr.Region>();
				while (j < regions.size() && regions.get(j).isInTab()) content.add(regions.get(j++));
				Block tab = new Block("TAB");
				tab.parts.add(sequence(content, new ArrayList<Block>()));
				blocks.add(tab);
				i = j - 1;
				continue;
			}
			String token = token(region);
			if (token != null) blocks.add(new Block(token));
		}
		return blocks;
	}

	static String token(UiIr.Region region) {
		String type = region.getType();
		String buttons = region.getButtons().isEmpty() ? "" : "b";
		if (UiIr.GRID.equals(type)) return "G" + buttons + (region.isPaging() ? "p" : "");
		if (UiIr.FORM.equals(type)) return "F" + buttons;
		if (UiIr.TREE.equals(type)) return "T" + buttons;
		if (UiIr.TEXTAREA.equals(type)) return "A";
		if (UiIr.SEARCH.equals(type)) return "S";
		if (UiIr.BUTTONS.equals(type) && isArrows(region)) return "X";
		return null; // title, description, sectionTitle and plain button rows have no template counterpart
	}

	public static boolean isArrows(UiIr.Region region) {
		if (!UiIr.BUTTONS.equals(region.getType()) || region.getButtons().isEmpty()) return false;
		for (String caption : region.getButtons()) { if (!isArrow(caption)) return false; }
		return true;
	}

	public static boolean isArrow(String caption) { return caption.trim().matches("[▲△↑⬆▼▽↓⬇◀◁←⬅▶▷→➡]"); }

	static boolean isHorizontalArrows(UiIr.Region region) {
		if (!isArrows(region)) return false;
		for (String caption : region.getButtons()) { if (caption.trim().matches("[◀◁←⬅▶▷→➡]")) return true; }
		return false;
	}

	/**
	 * The regions of a side-by-side run split into panes. Arrow-only ◀▶ buttons without a side, listed after
	 * the left pane and before the right one, are the shuttle column between the panes (P7-1, P7-3).
	 */
	public static final class Split {
		public final List<UiIr.Region> left = new ArrayList<UiIr.Region>();
		public final List<UiIr.Region> right = new ArrayList<UiIr.Region>();
		public UiIr.Region center;

		public static Split of(List<UiIr.Region> run) {
			Split split = new Split();
			String side = UiIr.LEFT;
			for (UiIr.Region region : run) {
				if (split.center == null && region.getSide().isEmpty() && UiIr.LEFT.equals(side) && !split.left.isEmpty() && isHorizontalArrows(region)) { split.center = region; continue; }
				// A full-width region between two paned ones is read as part of the pane above it.
				if (!region.getSide().isEmpty()) side = region.getSide();
				(UiIr.RIGHT.equals(side) ? split.right : split.left).add(region);
			}
			return split;
		}
	}

	// ------------------------------------------------------------------ template side

	/** Blocks of a template's (or a generated CLX's) content-body. */
	public static List<Block> of(Element body) {
		List<Block> blocks = new ArrayList<Block>();
		if (body != null) walk(body, blocks);
		return blocks;
	}

	private static void walk(Element container, List<Block> blocks) {
		for (Element child : ordered(container)) {
			String name = child.getLocalName();
			if ("grid".equals(name)) { blocks.add(new Block("G" + (hasTitleButtons((Element) child.getParentNode()) ? "b" : "") + (children(child.getParentNode(), "pageindexer").isEmpty() ? "" : "p"))); continue; }
			if ("tree".equals(name)) { blocks.add(new Block("T" + (hasTitleButtons((Element) child.getParentNode()) ? "b" : ""))); continue; }
			if ("textarea".equals(name)) { blocks.add(new Block("A")); continue; }
			if ("uicontrolshell".equals(name) || "embeddedpage".equals(name)) { blocks.add(new Block("EXT")); continue; }
			if ("tabfolder".equals(name)) {
				Block tab = new Block("TAB");
				List<Block> content = new ArrayList<Block>();
				List<Element> items = children(child, "tabitem");
				if (!items.isEmpty()) walk(items.get(0), content);
				tab.parts.add(content);
				blocks.add(tab);
				continue;
			}
			if ("accordion".equals(name)) { Block acc = new Block("ACC"); List<Block> content = new ArrayList<Block>(); walk(child, content); acc.parts.add(content); blocks.add(acc); continue; }
			if ("sectionitem".equals(name)) { walk(child, blocks); continue; }
			if (!"group".equals(name)) continue;
			if (hasClass(child, "content-title-box") || hasClass(child, "title-button-group")) continue;
			if (hasClass(child, "form-base")) { blocks.add(new Block("F" + (hasTitleButtons((Element) child.getParentNode()) ? "b" : ""))); continue; }
			if (hasClass(child, "search-box")) { blocks.add(new Block("S")); continue; }
			if (hasClass(child, "shuttle-button-group")) { blocks.add(new Block("X")); continue; }
			if (hasClass(child, "division-group")) { blocks.add(division(child)); continue; }
			walk(child, blocks);
		}
	}

	private static Block division(Element division) {
		Element layout = child(division, "formlayout");
		int columns = Math.max(1, layout == null ? 2 : children(layout, "columns").size());
		Map<Integer, List<Element>> byCol = new TreeMap<Integer, List<Element>>();
		for (Element child : ordered(division)) byCol.computeIfAbsent(Math.min(columns - 1, formData(child, "col")), k -> new ArrayList<Element>()).add(child);
		Block block = new Block("DIV");
		for (int c = 0; c < columns; c++) {
			List<Block> pane = new ArrayList<Block>();
			for (Element e : byCol.getOrDefault(c, new ArrayList<Element>())) {
				Element holder = e.getOwnerDocument().createElementNS(CL, "cl:group");
				holder.appendChild(e.cloneNode(true));
				walk(holder, pane);
			}
			block.parts.add(pane);
		}
		return block;
	}

	/** True when a division has a middle column holding only the shuttle buttons (P7-1, P7-3). */
	public static boolean hasShuttleColumn(List<Block> blocks) {
		for (Block b : blocks) {
			if ("DIV".equals(b.token) && b.parts.size() == 3 && b.parts.get(1).size() == 1 && "X".equals(b.parts.get(1).get(0).token)) return true;
			for (List<Block> part : b.parts) { if (hasShuttleColumn(part)) return true; }
		}
		return false;
	}

	/** Any block, at any depth, whose token starts with the prefix ("TAB", "T" for tree ...). */
	public static boolean contains(List<Block> blocks, String token) {
		for (Block b : blocks) {
			if ("T".equals(token) ? b.token.matches("T[bp]*") : b.token.equals(token)) return true;
			for (List<Block> part : b.parts) { if (contains(part, token)) return true; }
		}
		return false;
	}

	/** A content group's title row carries real (placeholder) buttons. */
	static boolean hasTitleButtons(Element content) {
		if (content == null) return false;
		for (Element box : children(content, "group")) {
			if (!hasClass(box, "content-title-box")) continue;
			for (Element group : children(box, "group")) { if (hasClass(group, "title-button-group") && !children(group, "button").isEmpty()) return true; }
		}
		return false;
	}

	// ------------------------------------------------------------------ distance

	/**
	 * Weighted edit distance over blocks. A control block costs 20 to insert or delete, a wrapper 40 plus its
	 * contents. The same control differing only in flags costs 5 per flag. DIV against DIV compares pane with
	 * pane (a missing shuttle column costs its weight); TAB against TAB compares the tab contents.
	 */
	public static int distance(List<Block> a, List<Block> b) {
		int[][] d = new int[a.size() + 1][b.size() + 1];
		for (int i = 1; i <= a.size(); i++) d[i][0] = d[i - 1][0] + weight(a.get(i - 1));
		for (int j = 1; j <= b.size(); j++) d[0][j] = d[0][j - 1] + weight(b.get(j - 1));
		for (int i = 1; i <= a.size(); i++) {
			for (int j = 1; j <= b.size(); j++) {
				d[i][j] = Math.min(Math.min(d[i - 1][j] + weight(a.get(i - 1)), d[i][j - 1] + weight(b.get(j - 1))), d[i - 1][j - 1] + substitution(a.get(i - 1), b.get(j - 1)));
			}
		}
		return d[a.size()][b.size()];
	}

	private static int weight(Block block) {
		if (!block.composite()) return COST;
		int w = WRAPPER_COST;
		for (List<Block> part : block.parts) { for (Block b : part) w += weight(b); }
		return w;
	}

	private static int weight(List<Block> blocks) { int w = 0; for (Block b : blocks) w += weight(b); return w; }

	private static int substitution(Block x, Block y) {
		if (x.composite() || y.composite()) {
			if (!x.token.equals(y.token)) return weight(x) + weight(y);
			if (!"DIV".equals(x.token)) return distance(x.parts.get(0), y.parts.get(0));
			List<List<Block>> p = x.parts;
			List<List<Block>> q = y.parts;
			// Outer panes align with outer panes; a middle (shuttle) column present on one side only is inserted or deleted.
			int cost = distance(p.get(0), q.get(0)) + distance(p.get(p.size() - 1), q.get(q.size() - 1));
			if (p.size() == 3 && q.size() == 3) cost += distance(p.get(1), q.get(1));
			else if (p.size() == 3) cost += weight(p.get(1));
			else if (q.size() == 3) cost += weight(q.get(1));
			return cost;
		}
		String a = x.token;
		String b = y.token;
		if (a.equals(b)) return 0;
		boolean control = a.matches("[GFT][bp]*") && b.matches("[GFT][bp]*");
		if (!control || a.charAt(0) != b.charAt(0)) return COST;
		int cost = 0;
		for (char flag : new char[] { 'b', 'p' }) { if ((a.indexOf(flag) >= 0) != (b.indexOf(flag) >= 0)) cost += FLAG_COST; }
		return cost;
	}

	// ------------------------------------------------------------------ DOM helpers

	private static List<Element> ordered(Element parent) {
		List<Element> list = new ArrayList<Element>();
		for (Element e : children(parent, null)) { if (CL.equals(e.getNamespaceURI()) && !isLayoutOrData(e)) list.add(e); }
		list.sort(Comparator.comparingInt((Element e) -> formData(e, "row")).thenComparingInt(e -> formData(e, "col")));
		return list;
	}

	private static int formData(Element e, String name) {
		Element fd = child(e, "formdata");
		if (fd == null) return 0;
		try { return Integer.parseInt(fd.getAttribute(name)); } catch (NumberFormatException ex) { return 0; }
	}

	private static boolean isLayoutOrData(Element e) {
		String n = e.getLocalName();
		return n.endsWith("layout") || n.endsWith("data") || "property".equals(n) || "listener".equals(n);
	}

	private static List<Element> children(Node parent, String localName) {
		List<Element> list = new ArrayList<Element>();
		if (parent == null) return list;
		NodeList nodes = parent.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node n = nodes.item(i);
			if (n instanceof Element && (localName == null || localName.equals(n.getLocalName()))) list.add((Element) n);
		}
		return list;
	}

	private static Element child(Element parent, String localName) { List<Element> l = children(parent, localName); return l.isEmpty() ? null : l.get(0); }

	private static boolean hasClass(Element e, String cls) {
		for (String c : e.getAttribute("class").trim().split("\\s+")) { if (c.equals(cls)) return true; }
		return false;
	}
}
