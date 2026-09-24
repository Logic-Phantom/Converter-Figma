package com.tomatosystem.figma.api;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A backend API description: OpenAPI 3.x or Swagger 2.0 JSON, reduced to what the binder needs — every operation
 * with its query/path parameters, request body fields and the fields of the response (the row DTO of a list, or the
 * object DTO of a detail call). {@code $ref}s are resolved, {@code allOf} merged, the first {@code oneOf}/{@code anyOf}
 * branch taken. REST envelopes ({@code {data:{list:[…]}}}) are unwrapped and the path remembered.
 */
public final class OpenApiSpec {
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();
	private static final String[] ENVELOPE_KEYS = { "data", "result", "results", "body", "content", "payload", "item", "items", "list", "rows", "records", "value", "response" };
	private static final List<String> METHODS = Arrays.asList("get", "post", "put", "delete", "patch");

	public final String title;
	public final String version;
	public final String source;
	/** Prefix of every path (Swagger basePath / first OpenAPI server path), "" when none. */
	public final String basePath;
	public final List<Operation> operations;

	private OpenApiSpec(String title, String version, String source, String basePath, List<Operation> operations) {
		this.title = title; this.version = version; this.source = source; this.basePath = basePath; this.operations = operations;
	}

	/** http(s) URL (GET, Accept: application/json) or a local file path. */
	public static OpenApiSpec load(String urlOrPath) {
		String input = urlOrPath == null ? "" : urlOrPath.trim();
		if (input.isEmpty()) throw new IllegalArgumentException("OpenAPI/Swagger URL or file is required");
		try {
			if (input.matches("(?i)https?://.*")) {
				HttpRequest request = HttpRequest.newBuilder(URI.create(input)).timeout(Duration.ofSeconds(60)).header("Accept", "application/json").GET().build();
				HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
				if (response.statusCode() != 200) throw new IllegalStateException("OpenAPI " + response.statusCode() + " for " + input);
				return parse(response.body(), input);
			}
			File file = new File(input);
			if (!file.isFile()) throw new IllegalArgumentException("OpenAPI file not found: " + input);
			return parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8), file.getName());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("OpenAPI download interrupted", e);
		} catch (java.io.IOException e) {
			throw new IllegalStateException("OpenAPI download failed: " + e.getMessage(), e);
		}
	}

	public static OpenApiSpec parse(String json, String source) {
		JSONObject root;
		try { root = new JSONObject(json.trim()); }
		catch (RuntimeException e) { throw new IllegalArgumentException("OpenAPI/Swagger JSON 이 아닙니다 (YAML 은 JSON 으로 변환해 주세요): " + e.getMessage()); }
		boolean swagger2 = root.has("swagger");
		if (!swagger2 && !root.has("openapi")) throw new IllegalArgumentException("openapi 3.x 또는 swagger 2.0 문서가 아닙니다 (openapi/swagger 키 없음)");
		JSONObject info = root.optJSONObject("info");
		String title = info == null ? "" : info.optString("title", "");
		String version = info == null ? "" : info.optString("version", "");
		Resolver resolver = new Resolver(root);
		String basePath = swagger2 ? root.optString("basePath", "") : serverPath(root.optJSONArray("servers"));
		List<Operation> operations = new ArrayList<Operation>();
		JSONObject paths = root.optJSONObject("paths");
		if (paths != null) {
			for (String path : sortedKeys(paths)) {
				JSONObject item = paths.optJSONObject(path);
				if (item == null) continue;
				List<JSONObject> shared = paramList(item.optJSONArray("parameters"), resolver);
				for (String method : METHODS) {
					JSONObject op = item.optJSONObject(method);
					if (op == null) continue;
					operations.add(operation(method, path, op, shared, resolver, swagger2));
				}
			}
		}
		return new OpenApiSpec(title.isEmpty() ? source : title, version, source, basePath, operations);
	}

	public String describe() { return title + (version.isEmpty() ? "" : " " + version) + " (" + operations.size() + " operations)"; }

	// ------------------------------------------------------------------ model

	/** A query/path parameter, a request body field or a response field. */
	public static final class Property {
		public final String name;
		public final String type;
		public final String format;
		public final String description;
		public final boolean required;
		public final List<String> enums;
		Property(String name, String type, String format, String description, boolean required, List<String> enums) {
			this.name = name; this.type = type == null ? "" : type; this.format = format == null ? "" : format;
			this.description = description == null ? "" : description; this.required = required; this.enums = enums;
		}
		/** eXBuilder datacolumn datatype for the JSON type, "" for string-like values. */
		public String dataType() {
			if ("integer".equals(type)) return "number";
			if ("number".equals(type)) return "float".equals(format) || "double".equals(format) ? "decimal" : "number";
			return "";
		}
		@Override public String toString() { return name + (description.isEmpty() ? "" : "(" + description + ")"); }
	}

	public static final class Operation {
		public final String method;
		public final String path;
		public final String operationId;
		public final String summary;
		public final String description;
		public final List<String> tags;
		/** Query and path parameters. */
		public final List<Property> parameters;
		/** Fields of the request body object (of its items when the body is an array). */
		public final List<Property> bodyFields;
		public final boolean bodyIsArray;
		/** Fields of the response row DTO (list) or object DTO (detail). */
		public final List<Property> responseFields;
		public final boolean responseIsList;
		/** Dotted path to the rows/object inside the response envelope ("data.list"); "" when the body itself. */
		public final String responsePath;

		Operation(String method, String path, String operationId, String summary, String description, List<String> tags, List<Property> parameters,
				List<Property> bodyFields, boolean bodyIsArray, List<Property> responseFields, boolean responseIsList, String responsePath) {
			this.method = method; this.path = path; this.operationId = operationId; this.summary = summary; this.description = description; this.tags = tags;
			this.parameters = parameters; this.bodyFields = bodyFields; this.bodyIsArray = bodyIsArray; this.responseFields = responseFields;
			this.responseIsList = responseIsList; this.responsePath = responsePath;
		}

		/** Everything a client can send: parameters plus body fields. */
		public List<Property> inputs() {
			List<Property> all = new ArrayList<Property>(parameters);
			all.addAll(bodyFields);
			return all;
		}

		/** Words of the path, operationId, summary and tags, lower case, for keyword matching. */
		public String keywords() {
			return (path + " " + operationId + " " + summary + " " + description + " " + String.join(" ", tags)).toLowerCase(Locale.ROOT);
		}

		public List<String> pathParams() {
			List<String> names = new ArrayList<String>();
			java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{([^}]+)}").matcher(path);
			while (m.find()) names.add(m.group(1));
			return names;
		}

		public String describe() { return method.toUpperCase(Locale.ROOT) + " " + path + (summary.isEmpty() ? "" : " (" + summary + ")"); }
		@Override public String toString() { return describe(); }
	}

	// ------------------------------------------------------------------ parsing

	private static Operation operation(String method, String path, JSONObject op, List<JSONObject> shared, Resolver resolver, boolean swagger2) {
		List<JSONObject> params = new ArrayList<JSONObject>(shared);
		params.addAll(paramList(op.optJSONArray("parameters"), resolver));
		List<Property> parameters = new ArrayList<Property>();
		List<Property> bodyFields = new ArrayList<Property>();
		boolean bodyIsArray = false;
		Set<String> seen = new LinkedHashSet<String>();
		JSONObject bodySchema = null;
		for (JSONObject p : params) {
			String in = p.optString("in", "query");
			if ("body".equals(in)) { bodySchema = resolver.resolve(p.optJSONObject("schema")); continue; }
			if (!("query".equals(in) || "path".equals(in) || "formData".equals(in))) continue;
			String name = p.optString("name", "");
			if (name.isEmpty() || !seen.add(name)) continue;
			JSONObject schema = resolver.resolve(p.optJSONObject("schema"));
			String type = schema != null ? schema.optString("type", "") : p.optString("type", "");
			String format = schema != null ? schema.optString("format", "") : p.optString("format", "");
			parameters.add(new Property(name, type, format, text(p, schema), p.optBoolean("required", false), enums(schema != null ? schema : p)));
		}
		if (!swagger2) {
			JSONObject requestBody = resolver.resolve(op.optJSONObject("requestBody"));
			if (requestBody != null) bodySchema = firstContentSchema(requestBody.optJSONObject("content"), resolver);
		}
		if (bodySchema != null) {
			JSONObject object = bodySchema;
			if ("array".equals(bodySchema.optString("type")) || bodySchema.has("items")) { bodyIsArray = true; object = resolver.resolve(bodySchema.optJSONObject("items")); }
			bodyFields.addAll(resolver.properties(object));
		}
		Unwrapped response = response(op, resolver, swagger2);
		List<String> tags = new ArrayList<String>();
		JSONArray tagArray = op.optJSONArray("tags");
		if (tagArray != null) { for (int i = 0; i < tagArray.length(); i++) tags.add(String.valueOf(tagArray.opt(i))); }
		return new Operation(method, path, op.optString("operationId", ""), op.optString("summary", ""), op.optString("description", ""), tags,
			parameters, bodyFields, bodyIsArray, response.fields, response.list, response.path);
	}

	private static final class Unwrapped {
		final List<Property> fields; final boolean list; final String path;
		Unwrapped(List<Property> fields, boolean list, String path) { this.fields = fields; this.list = list; this.path = path; }
	}

	private static Unwrapped response(JSONObject op, Resolver resolver, boolean swagger2) {
		JSONObject responses = op.optJSONObject("responses");
		JSONObject schema = null;
		if (responses != null) {
			for (String code : new String[] { "200", "201", "2XX", "default" }) {
				JSONObject r = resolver.resolve(responses.optJSONObject(code));
				if (r == null) continue;
				schema = swagger2 ? resolver.resolve(r.optJSONObject("schema")) : firstContentSchema(r.optJSONObject("content"), resolver);
				if (schema != null) break;
			}
		}
		if (schema == null) return new Unwrapped(new ArrayList<Property>(), false, "");
		return unwrap(schema, resolver);
	}

	/** Array → its items; object → the first array-of-objects inside (depth ≤ 3), else a single-object envelope, else itself. */
	private static Unwrapped unwrap(JSONObject schema, Resolver resolver) {
		if (isArray(schema)) return new Unwrapped(resolver.properties(resolver.resolve(schema.optJSONObject("items"))), true, "");
		String listPath = findList(schema, resolver, "", 0);
		if (listPath != null) {
			JSONObject array = at(schema, resolver, listPath);
			return new Unwrapped(resolver.properties(resolver.resolve(array.optJSONObject("items"))), true, listPath);
		}
		List<Property> own = resolver.properties(schema);
		// {"data": {DTO}} — a lone object property that is bigger than the rest of the envelope.
		JSONObject props = schema.optJSONObject("properties");
		if (props != null) {
			for (String key : ENVELOPE_KEYS) {
				JSONObject inner = resolver.resolve(props.optJSONObject(key));
				if (inner != null && !isArray(inner) && resolver.properties(inner).size() >= Math.max(2, own.size() - 1)) {
					return new Unwrapped(resolver.properties(inner), false, key);
				}
			}
		}
		return new Unwrapped(own, false, "");
	}

	private static String findList(JSONObject schema, Resolver resolver, String path, int depth) {
		JSONObject props = schema == null ? null : schema.optJSONObject("properties");
		if (props == null || depth > 3) return null;
		List<String> keys = sortedKeys(props);
		// Envelope keys first so {"data":{"list":[...]}, "page": {...}} picks the row array.
		List<String> ordered = new ArrayList<String>();
		for (String key : ENVELOPE_KEYS) { if (props.has(key)) ordered.add(key); }
		for (String key : keys) { if (!ordered.contains(key)) ordered.add(key); }
		for (String key : ordered) {
			JSONObject child = resolver.resolve(props.optJSONObject(key));
			if (child == null) continue;
			String childPath = path.isEmpty() ? key : path + "." + key;
			if (isArray(child)) {
				JSONObject items = resolver.resolve(child.optJSONObject("items"));
				if (items != null && !resolver.properties(items).isEmpty()) return childPath;
			}
		}
		for (String key : ordered) {
			JSONObject child = resolver.resolve(props.optJSONObject(key));
			if (child == null || isArray(child)) continue;
			String found = findList(child, resolver, path.isEmpty() ? key : path + "." + key, depth + 1);
			if (found != null) return found;
		}
		return null;
	}

	private static JSONObject at(JSONObject schema, Resolver resolver, String path) {
		JSONObject current = schema;
		for (String key : path.split("\\.")) {
			JSONObject props = current.optJSONObject("properties");
			current = props == null ? null : resolver.resolve(props.optJSONObject(key));
			if (current == null) return new JSONObject();
		}
		return current;
	}

	private static boolean isArray(JSONObject schema) { return schema != null && ("array".equals(schema.optString("type")) || (schema.has("items") && !schema.has("properties"))); }

	private static JSONObject firstContentSchema(JSONObject content, Resolver resolver) {
		if (content == null) return null;
		List<String> types = sortedKeys(content);
		for (String preferred : new String[] { "application/json", "*/*" }) { if (content.has(preferred)) { types.remove(preferred); types.add(0, preferred); } }
		for (String type : types) {
			JSONObject media = content.optJSONObject(type);
			JSONObject schema = media == null ? null : resolver.resolve(media.optJSONObject("schema"));
			if (schema != null) return schema;
		}
		return null;
	}

	private static List<JSONObject> paramList(JSONArray array, Resolver resolver) {
		List<JSONObject> result = new ArrayList<JSONObject>();
		if (array == null) return result;
		for (int i = 0; i < array.length(); i++) {
			JSONObject p = resolver.resolve(array.optJSONObject(i));
			if (p != null) result.add(p);
		}
		return result;
	}

	private static String serverPath(JSONArray servers) {
		if (servers == null || servers.length() == 0) return "";
		JSONObject server = servers.optJSONObject(0);
		String url = server == null ? "" : server.optString("url", "").trim();
		if (url.isEmpty() || "/".equals(url)) return "";
		if (url.matches("(?i)https?://.*")) {
			try { String p = URI.create(url).getPath(); return p == null || "/".equals(p) ? "" : p.replaceAll("/+$", ""); } catch (RuntimeException e) { return ""; }
		}
		return url.replaceAll("/+$", "");
	}

	static String text(JSONObject param, JSONObject schema) {
		StringBuilder s = new StringBuilder();
		for (JSONObject o : new JSONObject[] { param, schema }) {
			if (o == null) continue;
			for (String key : new String[] { "description", "title", "x-label", "x-comment", "x-name", "x-korean", "x-display-name", "example" }) {
				String v = o.opt(key) == null ? "" : String.valueOf(o.opt(key)).trim();
				if (!v.isEmpty() && s.indexOf(v) < 0) { if (s.length() > 0) s.append(' '); s.append(v); }
			}
		}
		return s.toString();
	}

	static List<String> enums(JSONObject schema) {
		List<String> result = new ArrayList<String>();
		JSONArray array = schema == null ? null : schema.optJSONArray("enum");
		if (array != null) { for (int i = 0; i < array.length(); i++) result.add(String.valueOf(array.opt(i))); }
		return result;
	}

	private static List<String> sortedKeys(JSONObject o) {
		List<String> keys = new ArrayList<String>();
		for (Iterator<String> it = o.keys(); it.hasNext();) keys.add(it.next());
		java.util.Collections.sort(keys);
		return keys;
	}

	/** Resolves local $ref pointers and flattens allOf/oneOf/anyOf. */
	static final class Resolver {
		private final JSONObject root;
		private final Map<String, JSONObject> cache = new LinkedHashMap<String, JSONObject>();
		Resolver(JSONObject root) { this.root = root; }

		JSONObject resolve(JSONObject schema) { return resolve(schema, 0); }

		private JSONObject resolve(JSONObject schema, int depth) {
			if (schema == null || depth > 12) return schema;
			String ref = schema.optString("$ref", "");
			if (!ref.isEmpty()) {
				JSONObject target = lookup(ref);
				return target == null ? new JSONObject() : resolve(target, depth + 1);
			}
			JSONArray allOf = schema.optJSONArray("allOf");
			if (allOf != null) {
				JSONObject merged = new JSONObject();
				JSONObject props = new JSONObject();
				JSONArray required = new JSONArray();
				for (int i = 0; i < allOf.length(); i++) {
					JSONObject part = resolve(allOf.optJSONObject(i), depth + 1);
					if (part == null) continue;
					for (String key : sortedKeys(part)) { if (!"properties".equals(key) && !"required".equals(key)) merged.put(key, part.get(key)); }
					JSONObject pp = part.optJSONObject("properties");
					if (pp != null) { for (String key : sortedKeys(pp)) props.put(key, pp.get(key)); }
					JSONArray r = part.optJSONArray("required");
					if (r != null) { for (int j = 0; j < r.length(); j++) required.put(r.get(j)); }
				}
				merged.put("properties", props);
				merged.put("required", required);
				if (!merged.has("type")) merged.put("type", "object");
				return merged;
			}
			for (String alt : new String[] { "oneOf", "anyOf" }) {
				JSONArray options = schema.optJSONArray(alt);
				if (options != null && options.length() > 0) return resolve(options.optJSONObject(0), depth + 1);
			}
			return schema;
		}

		private JSONObject lookup(String ref) {
			if (cache.containsKey(ref)) return cache.get(ref);
			JSONObject found = null;
			if (ref.startsWith("#/")) {
				Object current = root;
				for (String part : ref.substring(2).split("/")) {
					String key = part.replace("~1", "/").replace("~0", "~");
					if (current instanceof JSONObject) current = ((JSONObject) current).opt(key);
					else if (current instanceof JSONArray) { try { current = ((JSONArray) current).opt(Integer.parseInt(key)); } catch (NumberFormatException e) { current = null; } }
					else current = null;
					if (current == null) break;
				}
				if (current instanceof JSONObject) found = (JSONObject) current;
			}
			cache.put(ref, found);
			return found;
		}

		/** Direct properties of an object schema (resolved), in document order (sorted for determinism). */
		List<Property> properties(JSONObject schema) {
			List<Property> result = new ArrayList<Property>();
			JSONObject resolved = resolve(schema);
			JSONObject props = resolved == null ? null : resolved.optJSONObject("properties");
			if (props == null) return result;
			Set<String> required = new LinkedHashSet<String>();
			JSONArray r = resolved.optJSONArray("required");
			if (r != null) { for (int i = 0; i < r.length(); i++) required.add(String.valueOf(r.opt(i))); }
			for (String name : sortedKeys(props)) {
				JSONObject p = resolve(props.optJSONObject(name));
				if (p == null) p = new JSONObject();
				String type = p.optString("type", p.has("properties") ? "object" : "");
				result.add(new Property(name, type, p.optString("format", ""), text(p, null), required.contains(name), enums(p)));
			}
			return result;
		}
	}
}
