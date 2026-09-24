package com.tomatosystem.figma.theme;

import com.tomatosystem.exconverter.service.ProgressLog;
import com.tomatosystem.figma.FigmaSettings;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Figma design tokens → eXBuilder6 theme LESS.
 *
 * <p>Sources, in order of preference: (1) Variables ({@code GET /v1/files/:key/variables/local}, Enterprise plans
 * with the file_variables:read scope) — colours, numbers, strings per mode; (2) the file's local/published styles
 * ({@code styles} of the file JSON, resolved through the first node that uses each style) — fill colours, text
 * styles, drop shadows. Every token becomes a {@code @figma-…} LESS variable in
 * {@code clx-src/theme/figma/figma-tokens.part.less}; a second file {@code figma-theme.part.less} maps the tokens
 * onto the global variables of settings.part.less ({@code @default-text-color}, {@code @focus-border-color} …) and
 * is imported by cleopatra-theme.less so the whole theme follows the Figma tokens (LESS: last definition wins).
 */
public final class FigmaThemeSync {
	private FigmaThemeSync() { }

	/** One design token. */
	public static final class Token {
		public final String name;
		public final String lessName;
		/** color | number | string | font | shadow */
		public final String kind;
		public final String value;
		public final String source;
		public final String mode;
		Token(String name, String lessName, String kind, String value, String source, String mode) {
			this.name = name; this.lessName = lessName; this.kind = kind; this.value = value; this.source = source; this.mode = mode == null ? "" : mode;
		}
		public JSONObject toJson() { return new JSONObject().put("name", name).put("less", lessName).put("kind", kind).put("value", value).put("source", source).put("mode", mode); }
	}

	public static final class Result {
		public final List<Token> tokens = new ArrayList<Token>();
		/** settings variable → token that now defines it. */
		public final Map<String, Token> mapped = new LinkedHashMap<String, Token>();
		public final List<String> notes = new ArrayList<String>();
		public File tokensLess;
		public File themeLess;
		public File tokensJson;
		public boolean importAdded;
		public int fromVariables;
		public int fromStyles;

		public String summary() {
			StringBuilder s = new StringBuilder("테마 동기화: 토큰 " + tokens.size() + "개 (variables " + fromVariables + " · styles " + fromStyles + ")");
			if (!mapped.isEmpty()) {
				s.append(" · 매핑 ").append(mapped.size()).append("개 [");
				boolean first = true;
				for (Map.Entry<String, Token> e : mapped.entrySet()) { s.append(first ? "" : ", ").append('@').append(e.getKey()).append(" ← ").append(e.getValue().name); first = false; }
				s.append(']');
			} else s.append(" · 전역 변수 매핑 없음");
			if (tokensLess != null) s.append("\n    파일: ").append(tokensLess.getAbsolutePath()).append(", ").append(themeLess.getName());
			s.append(importAdded ? "\n    cleopatra-theme.less 에 import 추가" : "");
			for (String note : notes) s.append("\n    · ").append(note);
			return s.toString();
		}
	}

	// ------------------------------------------------------------------ token extraction

	/** GET /v1/files/:key/variables/local → tokens (default mode as the main token, other modes suffixed --mode). */
	public static List<Token> fromVariables(JSONObject response) {
		List<Token> tokens = new ArrayList<Token>();
		JSONObject meta = response == null ? null : response.optJSONObject("meta");
		if (meta == null) return tokens;
		JSONObject collections = meta.optJSONObject("variableCollections");
		JSONObject variables = meta.optJSONObject("variables");
		if (variables == null) return tokens;
		for (String id : sortedKeys(variables)) {
			JSONObject v = variables.optJSONObject(id);
			if (v == null || v.optBoolean("remote", false) && v.optBoolean("hiddenFromPublishing", false)) continue;
			JSONObject collection = collections == null ? null : collections.optJSONObject(v.optString("variableCollectionId", ""));
			String collectionName = collection == null ? "" : collection.optString("name", "");
			String defaultMode = collection == null ? "" : collection.optString("defaultModeId", "");
			Map<String, String> modeNames = new LinkedHashMap<String, String>();
			JSONArray modes = collection == null ? null : collection.optJSONArray("modes");
			if (modes != null) { for (int i = 0; i < modes.length(); i++) { JSONObject m = modes.optJSONObject(i); if (m != null) modeNames.put(m.optString("modeId"), m.optString("name")); } }
			JSONObject values = v.optJSONObject("valuesByMode");
			if (values == null) continue;
			String type = v.optString("resolvedType", "");
			List<String> scopes = new ArrayList<String>();
			JSONArray scopeArray = v.optJSONArray("scopes");
			if (scopeArray != null) { for (int i = 0; i < scopeArray.length(); i++) scopes.add(scopeArray.optString(i, "")); }
			String name = (collectionName.isEmpty() ? "" : collectionName + "/") + v.optString("name", id);
			for (String modeId : sortedKeys(values)) {
				String value = variableValue(values.get(modeId), type, scopes, variables, 0);
				if (value == null) continue;
				boolean main = modeId.equals(defaultMode) || defaultMode.isEmpty() && tokens.stream().noneMatch(t -> t.name.equals(name) && t.mode.isEmpty());
				String mode = main ? "" : modeNames.getOrDefault(modeId, modeId);
				String less = lessName(name) + (mode.isEmpty() ? "" : "--" + slug(mode));
				tokens.add(new Token(name, less, kindOf(type), value, "variable", mode));
			}
		}
		return tokens;
	}

	private static String variableValue(Object raw, String type, List<String> scopes, JSONObject variables, int depth) {
		if (raw == null || raw == JSONObject.NULL || depth > 8) return null;
		if (raw instanceof JSONObject) {
			JSONObject o = (JSONObject) raw;
			if ("VARIABLE_ALIAS".equals(o.optString("type"))) {
				JSONObject target = variables.optJSONObject(o.optString("id", ""));
				JSONObject values = target == null ? null : target.optJSONObject("valuesByMode");
				if (values == null || values.length() == 0) return null;
				return variableValue(values.get(sortedKeys(values).get(0)), target.optString("resolvedType", type), scopes, variables, depth + 1);
			}
			if (o.has("r") && o.has("g") && o.has("b")) return color(o, 1.0);
			return null;
		}
		if ("COLOR".equals(type)) return null;
		if ("BOOLEAN".equals(type)) return null;
		if ("FLOAT".equals(type) || raw instanceof Number) {
			double d = ((Number) raw).doubleValue();
			boolean unitless = false;
			for (String scope : scopes) { if (scope.matches("OPACITY|FONT_WEIGHT|LAYER_OPACITY")) unitless = true; }
			String number = d == Math.rint(d) ? String.valueOf((long) d) : String.format(Locale.ROOT, "%.2f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
			return unitless ? number : number + "px";
		}
		return "\"" + String.valueOf(raw).replace("\"", "\\\"") + "\"";
	}

	private static String kindOf(String type) {
		if ("COLOR".equals(type)) return "color";
		if ("FLOAT".equals(type)) return "number";
		return "string";
	}

	/** File JSON (GET /v1/files/:key, or a /nodes response) → tokens from its FILL/TEXT/EFFECT styles. */
	public static List<Token> fromStyles(JSONObject fileJson) {
		List<Token> tokens = new ArrayList<Token>();
		JSONObject styles = new JSONObject();
		List<JSONObject> roots = new ArrayList<JSONObject>();
		merge(styles, fileJson.optJSONObject("styles"));
		if (fileJson.optJSONObject("document") != null) roots.add(fileJson.getJSONObject("document"));
		JSONObject nodes = fileJson.optJSONObject("nodes");
		if (nodes != null) {
			for (Iterator<String> it = nodes.keys(); it.hasNext();) {
				JSONObject entry = nodes.optJSONObject(it.next());
				if (entry == null) continue;
				merge(styles, entry.optJSONObject("styles"));
				if (entry.optJSONObject("document") != null) roots.add(entry.getJSONObject("document"));
			}
		}
		if (styles.length() == 0) return tokens;
		// First node using each style id, per usage kind (fill / stroke / text / effect).
		Map<String, JSONObject> usage = new LinkedHashMap<String, JSONObject>();
		for (JSONObject root : roots) collectStyleUsage(root, usage, 0);
		for (String id : sortedKeys(styles)) {
			JSONObject style = styles.optJSONObject(id);
			if (style == null) continue;
			String type = style.optString("styleType", "");
			String name = style.optString("name", id);
			if ("FILL".equals(type)) {
				JSONObject node = usage.get("fill:" + id);
				String kind = "fill";
				if (node == null) { node = usage.get("stroke:" + id); kind = "stroke"; }
				if (node == null) continue;
				String value = firstSolid(node.optJSONArray("fill".equals(kind) ? "fills" : "strokes"));
				if (value != null) tokens.add(new Token(name, lessName(name), "color", value, "style", ""));
			} else if ("TEXT".equals(type)) {
				JSONObject node = usage.get("text:" + id);
				JSONObject ts = node == null ? null : node.optJSONObject("style");
				if (ts == null) continue;
				String family = ts.optString("fontFamily", "");
				double size = ts.optDouble("fontSize", 0);
				int weight = ts.optInt("fontWeight", 400);
				double lineHeight = ts.optDouble("lineHeightPx", 0);
				if (family.isEmpty() || size <= 0) continue;
				String font = weight + " " + px(size) + (lineHeight > 0 ? "/" + px(lineHeight) : "") + " \"" + family + "\", sans-serif";
				tokens.add(new Token(name, lessName(name), "font", font, "style", ""));
				tokens.add(new Token(name + "/size", lessName(name) + "-size", "number", px(size), "style", ""));
				tokens.add(new Token(name + "/weight", lessName(name) + "-weight", "number", String.valueOf(weight), "style", ""));
				tokens.add(new Token(name + "/family", lessName(name) + "-family", "string", "\"" + family + "\"", "style", ""));
				if (lineHeight > 0) tokens.add(new Token(name + "/line-height", lessName(name) + "-line-height", "number", px(lineHeight), "style", ""));
			} else if ("EFFECT".equals(type)) {
				JSONObject node = usage.get("effect:" + id);
				JSONArray effects = node == null ? null : node.optJSONArray("effects");
				String shadow = shadow(effects);
				if (shadow != null) tokens.add(new Token(name, lessName(name), "shadow", shadow, "style", ""));
			}
		}
		return tokens;
	}

	private static void collectStyleUsage(JSONObject node, Map<String, JSONObject> usage, int depth) {
		if (node == null || depth > 60) return;
		JSONObject styles = node.optJSONObject("styles");
		if (styles != null) {
			for (String key : sortedKeys(styles)) {
				String kind = key.replaceAll("s$", "");
				usage.putIfAbsent(kind + ":" + styles.optString(key, ""), node);
			}
		}
		JSONArray children = node.optJSONArray("children");
		if (children != null) { for (int i = 0; i < children.length(); i++) collectStyleUsage(children.optJSONObject(i), usage, depth + 1); }
	}

	private static String firstSolid(JSONArray paints) {
		if (paints == null) return null;
		for (int i = 0; i < paints.length(); i++) {
			JSONObject paint = paints.optJSONObject(i);
			if (paint == null || !paint.optBoolean("visible", true) || !"SOLID".equals(paint.optString("type"))) continue;
			JSONObject color = paint.optJSONObject("color");
			if (color != null) return color(color, paint.optDouble("opacity", 1.0));
		}
		return null;
	}

	private static String shadow(JSONArray effects) {
		if (effects == null) return null;
		for (int i = 0; i < effects.length(); i++) {
			JSONObject effect = effects.optJSONObject(i);
			if (effect == null || !effect.optBoolean("visible", true) || !effect.optString("type", "").endsWith("SHADOW")) continue;
			JSONObject offset = effect.optJSONObject("offset");
			JSONObject color = effect.optJSONObject("color");
			if (color == null) continue;
			return (effect.optString("type").startsWith("INNER") ? "inset " : "") + px(offset == null ? 0 : offset.optDouble("x", 0)) + " " + px(offset == null ? 0 : offset.optDouble("y", 0))
				+ " " + px(effect.optDouble("radius", 0)) + " " + px(effect.optDouble("spread", 0)) + " " + color(color, 1.0);
		}
		return null;
	}

	static String color(JSONObject c, double opacity) {
		int r = channel(c.optDouble("r", 0)), g = channel(c.optDouble("g", 0)), b = channel(c.optDouble("b", 0));
		double a = c.optDouble("a", 1.0) * opacity;
		if (a >= 0.999) return String.format(Locale.ROOT, "#%02x%02x%02x", r, g, b);
		return String.format(Locale.ROOT, "rgba(%d, %d, %d, %.2f)", r, g, b, a);
	}

	private static int channel(double v) { return Math.max(0, Math.min(255, (int) Math.round(v * 255))); }
	private static String px(double v) { return (v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "")) + "px"; }

	static String lessName(String name) { return "figma-" + slug(name); }
	static String slug(String text) {
		String s = text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9가-힣]+", "-").replaceAll("^-+|-+$", "");
		return s.isEmpty() ? "token" : s;
	}

	// ------------------------------------------------------------------ mapping + files

	/** Default token-name patterns for the settings variables; figma.theme.map.&lt;variable&gt;=regex overrides one. */
	private static final String[][] DEFAULT_MAP = {
		{ "default-text-color", "color", "(?i).*(text|font|fg|foreground|typography).*(primary|default|main|base|body|900|90|black).*|(?i).*(gray|grey|neutral|black).*(90|900|950|800|80)$", "dark" },
		{ "disabled-text-color", "color", "(?i).*(text|font).*(disabled|tertiary|placeholder|muted|400|40).*|(?i).*(gray|grey|neutral).*(50|500|60|600)$", "mid" },
		{ "default-border-color", "color", "(?i).*(border|line|divider|outline|stroke).*(default|primary|main|base|300|30).*|(?i).*(gray|grey|neutral).*(30|300|40|400)$", "light" },
		{ "disabled-border-color", "color", "(?i).*(border|line|divider|outline).*(disabled|light|200|20).*|(?i).*(gray|grey|neutral).*(20|200|10|100)$", "light" },
		{ "focus-border-color", "color", "(?i).*focus.*|(?i).*(primary|brand|main|accent|blue).*(500|50|main|default|base|600|60)$|(?i)^(primary|brand|accent)(/|-)?(color)?$", "any" },
		{ "selection-background", "color", "(?i).*(selected|selection|active).*(bg|background|fill).*|(?i).*(primary|brand|main|accent|blue).*(500|50|main|default|base)$", "any" },
		{ "selection-foreground", "color", "(?i).*(selected|selection|active).*(text|fg|foreground).*|(?i).*(white|on-primary|onprimary|inverse).*", "light" },
		{ "hover-background", "color", "(?i).*(hover).*(bg|background|fill)?.*|(?i).*(primary|brand|main|accent|blue).*(50|100|10|light|lighter|subtle)$", "light" },
		{ "hover-foreground", "color", "(?i).*hover.*(text|fg|foreground).*", "dark" },
		{ "default-font", "font", "(?i).*(body|base|default|regular|text).*(m|md|medium|regular|400|default|base)?.*", "body-font" } };

	/**
	 * A name-pattern match is only trusted when the value looks right for the variable: "dark" text colours (luminance
	 * &lt; 0.45), "light" borders/hover backgrounds (&gt; 0.55), "mid" disabled text, and a body font of 12–18px. Design
	 * systems disagree on whether gray-100 is the lightest or the darkest step, so the value decides, not the name.
	 */
	static boolean valueFits(Token t, String expectation) {
		if ("any".equals(expectation)) return true;
		if ("body-font".equals(expectation)) {
			java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)px").matcher(t.value);
			if (!m.find()) return false;
			double size = Double.parseDouble(m.group(1));
			return size >= 12 && size <= 18;
		}
		double l = luminance(t.value);
		if (l < 0) return false;
		if ("dark".equals(expectation)) return l < 0.45;
		if ("light".equals(expectation)) return l > 0.55;
		if ("mid".equals(expectation)) return l >= 0.25 && l <= 0.75;
		return true;
	}

	/** Relative luminance 0..1 of "#rrggbb" or "rgba(r, g, b, a)" (alpha blended on white); -1 when not a colour. */
	static double luminance(String value) {
		int r, g, b;
		double a = 1;
		String v = value == null ? "" : value.trim();
		try {
			if (v.matches("#[0-9a-fA-F]{6}")) { r = Integer.parseInt(v.substring(1, 3), 16); g = Integer.parseInt(v.substring(3, 5), 16); b = Integer.parseInt(v.substring(5, 7), 16); }
			else if (v.startsWith("rgba(")) {
				String[] parts = v.substring(5, v.length() - 1).split(",");
				r = Integer.parseInt(parts[0].trim()); g = Integer.parseInt(parts[1].trim()); b = Integer.parseInt(parts[2].trim()); a = Double.parseDouble(parts[3].trim());
			} else return -1;
		} catch (RuntimeException e) { return -1; }
		double rr = 255 + (r - 255) * a, gg = 255 + (g - 255) * a, bb = 255 + (b - 255) * a;
		return (0.2126 * rr + 0.7152 * gg + 0.0722 * bb) / 255.0;
	}

	/**
	 * Writes the two LESS files (and figma-tokens.json), maps the settings variables and, when apply, imports the
	 * theme file from cleopatra-theme.less (idempotent).
	 */
	public static Result sync(File projectRoot, List<Token> variableTokens, List<Token> styleTokens, boolean apply, String source) {
		Result result = new Result();
		result.fromVariables = variableTokens == null ? 0 : variableTokens.size();
		result.fromStyles = styleTokens == null ? 0 : styleTokens.size();
		Map<String, Token> byLess = new LinkedHashMap<String, Token>();
		for (List<Token> list : new List[] { variableTokens, styleTokens }) {
			if (list == null) continue;
			for (Token t : list) {
				String less = t.lessName;
				for (int n = 2; byLess.containsKey(less) && !sameValue(byLess.get(less), t); n++) less = t.lessName + "-" + n;
				if (!byLess.containsKey(less)) byLess.put(less, less.equals(t.lessName) ? t : new Token(t.name, less, t.kind, t.value, t.source, t.mode));
			}
		}
		result.tokens.addAll(byLess.values());
		if (result.tokens.isEmpty()) { result.notes.add("추출된 토큰이 없습니다 (Variables 권한 없음 + 파일에 스타일 없음)"); return result; }
		for (String[] rule : DEFAULT_MAP) {
			String configured = FigmaSettings.get("figma.theme.map." + rule[0], "");
			String pattern = configured.isEmpty() ? rule[2] : configured;
			Token chosen = null;
			for (Token t : result.tokens) {
				if (!t.mode.isEmpty() || !t.kind.equals(rule[1])) continue;
				if (!(t.name.matches(pattern) || t.name.replace('/', '-').matches(pattern))) continue;
				// A configured pattern is the user's decision; the built-in one is checked against the value.
				if (configured.isEmpty() && !valueFits(t, rule[3])) continue;
				chosen = t;
				break;
			}
			if (chosen != null) result.mapped.put(rule[0], chosen);
		}
		try {
			File themeDir = new File(projectRoot, "clx-src/theme");
			if (!themeDir.isDirectory()) throw new IllegalStateException("clx-src/theme 이 없습니다: " + themeDir.getAbsolutePath());
			File dir = new File(themeDir, "figma");
			if (!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("폴더 생성 실패: " + dir.getAbsolutePath());
			String stamp = LocalDateTime.now().withNano(0).toString().replace('T', ' ');
			StringBuilder tokens = new StringBuilder();
			tokens.append("// Figma 디자인 토큰 (자동 생성 ").append(stamp).append(", 원본: ").append(source).append(")\n");
			tokens.append("// 이 파일은 /figma/theme/sync.do 가 다시 만듭니다. 직접 고치지 말고 Figma 의 Variables/Styles 를 바꾸세요.\n\n");
			String lastGroup = null;
			for (Token t : result.tokens) {
				String group = t.name.contains("/") ? t.name.substring(0, t.name.indexOf('/')) : "(root)";
				if (!group.equals(lastGroup)) { tokens.append(lastGroup == null ? "" : "\n").append("// ").append(group).append('\n'); lastGroup = group; }
				tokens.append('@').append(t.lessName).append(": ").append(t.value).append(";  // ").append(t.name).append(t.mode.isEmpty() ? "" : " [" + t.mode + "]").append(" (").append(t.source).append(")\n");
			}
			result.tokensLess = new File(dir, "figma-tokens.part.less");
			Files.write(result.tokensLess.toPath(), tokens.toString().getBytes(StandardCharsets.UTF_8));
			StringBuilder theme = new StringBuilder();
			theme.append("// Figma 토큰 → eXBuilder6 전역 테마 변수 (자동 생성 ").append(stamp).append(")\n");
			theme.append("// settings.part.less 의 변수를 Figma 토큰으로 덮어씁니다 (LESS 는 마지막 정의가 이깁니다).\n");
			theme.append("// 매칭 규칙은 application.properties 의 figma.theme.map.<변수>=<토큰 이름 정규식> 으로 바꿀 수 있습니다.\n");
			theme.append("@import \"figma-tokens.part.less\";\n\n");
			for (String[] rule : DEFAULT_MAP) {
				Token t = result.mapped.get(rule[0]);
				if (t != null) theme.append('@').append(rule[0]).append(": @").append(t.lessName).append(";  // ← ").append(t.name).append(" = ").append(t.value).append('\n');
				else theme.append("// @").append(rule[0]).append(": (매칭되는 ").append(rule[1]).append(" 토큰 없음)\n");
			}
			result.themeLess = new File(dir, "figma-theme.part.less");
			Files.write(result.themeLess.toPath(), theme.toString().getBytes(StandardCharsets.UTF_8));
			JSONArray json = new JSONArray();
			for (Token t : result.tokens) json.put(t.toJson());
			result.tokensJson = new File(dir, "figma-tokens.json");
			Files.write(result.tokensJson.toPath(), new JSONObject().put("generatedAt", stamp).put("source", source).put("tokens", json).toString(2).getBytes(StandardCharsets.UTF_8));
			if (apply) result.importAdded = addImport(new File(themeDir, "cleopatra-theme.less"), result.notes);
			else result.notes.add("apply=false: cleopatra-theme.less 는 바꾸지 않음 (@import \"figma/figma-theme.part.less\"; 를 직접 추가)");
		} catch (java.io.IOException e) {
			throw new IllegalStateException("테마 파일 쓰기 실패: " + e.getMessage(), e);
		}
		ProgressLog.step("{}", result.summary().replace('\n', ' '));
		return result;
	}

	/** Adds the import after the settings import (or at the top); returns true when the file changed. */
	static boolean addImport(File themeLess, List<String> notes) throws java.io.IOException {
		String line = "@import \"figma/figma-theme.part.less\";";
		if (!themeLess.isFile()) { notes.add("cleopatra-theme.less 가 없어 import 를 추가하지 못함"); return false; }
		String text = new String(Files.readAllBytes(themeLess.toPath()), StandardCharsets.UTF_8);
		if (text.contains("figma/figma-theme.part.less")) { notes.add("cleopatra-theme.less 에 import 가 이미 있음"); return false; }
		int at = text.indexOf("@import \"settings.part.less\";");
		String updated;
		if (at >= 0) {
			int end = text.indexOf('\n', at);
			end = end < 0 ? text.length() : end + 1;
			updated = text.substring(0, end) + line + "\n" + text.substring(end);
		} else updated = line + "\n" + text;
		Files.write(themeLess.toPath(), updated.getBytes(StandardCharsets.UTF_8));
		return true;
	}

	private static boolean sameValue(Token a, Token b) { return a.value.equals(b.value) && a.kind.equals(b.kind) && a.mode.equals(b.mode); }

	private static void merge(JSONObject target, JSONObject source) {
		if (source == null) return;
		for (Iterator<String> it = source.keys(); it.hasNext();) { String key = it.next(); target.put(key, source.get(key)); }
	}

	private static List<String> sortedKeys(JSONObject o) {
		List<String> keys = new ArrayList<String>();
		for (Iterator<String> it = o.keys(); it.hasNext();) keys.add(it.next());
		java.util.Collections.sort(keys);
		return keys;
	}
}
