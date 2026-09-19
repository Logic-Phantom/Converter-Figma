package com.tomatosystem.figma;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.json.JSONObject;

/** Geometry, visibility and text helpers over raw Figma node JSON. */
final class FigmaNodes {
	private FigmaNodes() { }

	/** absoluteBoundingBox in canvas pixels. */
	static final class Box {
		final double x, y, w, h;
		Box(double x, double y, double w, double h) { this.x = x; this.y = y; this.w = w; this.h = h; }
		double right() { return x + w; }
		double bottom() { return y + h; }
		double cx() { return x + w / 2; }
		double cy() { return y + h / 2; }
		boolean intersects(Box o) { return x < o.right() && o.x < right() && y < o.bottom() && o.y < bottom(); }
		boolean overlapsX(Box o) { return x < o.right() && o.x < right(); }
		boolean contains(Box o) { return o.x >= x - 1 && o.y >= y - 1 && o.right() <= right() + 1 && o.bottom() <= bottom() + 1; }
		Box union(Box o) {
			if (o == null) return this;
			double nx = Math.min(x, o.x), ny = Math.min(y, o.y);
			return new Box(nx, ny, Math.max(right(), o.right()) - nx, Math.max(bottom(), o.bottom()) - ny);
		}
		@Override public String toString() { return "(" + (int) x + "," + (int) y + " " + (int) w + "x" + (int) h + ")"; }
	}

	static Box box(JSONObject node) {
		JSONObject b = node.optJSONObject("absoluteBoundingBox");
		if (b == null) return null;
		return new Box(b.optDouble("x", 0), b.optDouble("y", 0), b.optDouble("width", 0), b.optDouble("height", 0));
	}

	/** Canvas pages have no bounding box; their extent is the union of their visible children. */
	static Box boxOrUnion(JSONObject node) {
		Box own = box(node);
		if (own != null) return own;
		Box union = null;
		for (JSONObject child : FigmaDocument.children(node)) {
			if (!isVisible(child)) continue;
			Box b = boxOrUnion(child);
			if (b != null) union = union == null ? b : union.union(b);
		}
		return union;
	}

	/** Hidden layers (visible=false) and fully transparent ones are not part of the design. */
	static boolean isVisible(JSONObject node) {
		return node.optBoolean("visible", true) && node.optDouble("opacity", 1) > 0.01;
	}

	static String type(JSONObject node) { return node.optString("type", ""); }

	static String name(JSONObject node) { return node.optString("name", ""); }

	/** Text content; Figma's line/paragraph separators (U+2028/U+2029) become '\n'. */
	static String characters(JSONObject node) {
		return node.optString("characters", "").replace(' ', ' ').replace(' ', '\n').replace(' ', '\n').trim();
	}

	static double fontSize(JSONObject text) {
		JSONObject style = text.optJSONObject("style");
		return style == null ? 0 : style.optDouble("fontSize", 0);
	}

	static int fontWeight(JSONObject text) {
		JSONObject style = text.optJSONObject("style");
		return style == null ? 400 : style.optInt("fontWeight", 400);
	}

	/** Visible TEXT descendants in document order. */
	static List<JSONObject> visibleTexts(JSONObject node) {
		List<JSONObject> result = new ArrayList<JSONObject>();
		collectTexts(node, result);
		return result;
	}

	private static void collectTexts(JSONObject node, List<JSONObject> out) {
		if (!isVisible(node)) return;
		if ("TEXT".equals(type(node))) { if (!characters(node).isEmpty()) out.add(node); return; }
		for (JSONObject child : FigmaDocument.children(node)) collectTexts(child, out);
	}

	/** First non-empty TEXT component property (e.g. "Button name#67:81" = "조회"); "" when none. */
	static String textProperty(JSONObject instance) {
		JSONObject props = instance.optJSONObject("componentProperties");
		if (props == null) return "";
		String fallback = "";
		for (Iterator<String> it = props.keys(); it.hasNext();) {
			String key = it.next();
			JSONObject prop = props.optJSONObject(key);
			if (prop == null || !"TEXT".equals(prop.optString("type"))) continue;
			String value = String.valueOf(prop.opt("value")).trim();
			if (value.isEmpty() || "null".equals(value)) continue;
			String k = key.toLowerCase(Locale.ROOT);
			if (k.contains("name") || k.contains("label") || k.contains("text") || k.contains("텍스트")) return value;
			if (fallback.isEmpty()) fallback = value;
		}
		return fallback;
	}

	/** Lower case without spaces, '-', '_' and '/', so "Base-input" and "base_input" compare equal. */
	static String normalize(String text) {
		return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[\\s_\\-/]+", "");
	}
}
