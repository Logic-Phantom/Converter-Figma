package com.tomatosystem.figma;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;

/**
 * Figma REST API (https://developers.figma.com/docs/rest-api/). Scope needed: file_content:read
 * (file_read / files:read were removed on 2025-11-17). Personal access tokens expire after at most 90 days.
 */
public final class FigmaApiClient {
	public static final String API = "https://api.figma.com";
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NORMAL).build();
	private static final Pattern FIGMA_URL = Pattern.compile("figma\\.com/(?:file|design|proto|board)/([A-Za-z0-9]+)(?:/[^?#]*)?(?:\\?([^#]*))?");

	private FigmaApiClient() { }

	/** Personal access token → X-Figma-Token; OAuth access token → Authorization: Bearer. */
	public enum Auth { PERSONAL_TOKEN, OAUTH }

	/** File key plus the node ids to convert (empty = every screen of the file). */
	public static final class FileRef {
		public final String fileKey;
		public final List<String> nodeIds;
		FileRef(String fileKey, List<String> nodeIds) { this.fileKey = fileKey; this.nodeIds = nodeIds; }
	}

	/**
	 * Accepts a file key or a Figma URL (https://www.figma.com/design/KEY/Name?node-id=12-34). An explicit nodeId
	 * ("12:34" or "12-34", comma separated) wins over the one in the URL.
	 */
	public static FileRef parse(String urlOrKey, String nodeIds) {
		String input = urlOrKey == null ? "" : urlOrKey.trim();
		if (input.isEmpty()) throw new IllegalArgumentException("Figma file key or URL is required");
		String key = input;
		String urlNode = "";
		Matcher m = FIGMA_URL.matcher(input);
		if (m.find()) {
			key = m.group(1);
			if (m.group(2) != null) {
				for (String pair : m.group(2).split("&")) {
					if (pair.startsWith("node-id=")) urlNode = URLDecoder.decode(pair.substring(8), StandardCharsets.UTF_8);
				}
			}
		}
		if (!key.matches("[A-Za-z0-9]+")) throw new IllegalArgumentException("Invalid Figma file key: " + key);
		List<String> ids = new ArrayList<String>();
		String wanted = nodeIds != null && !nodeIds.trim().isEmpty() ? nodeIds : urlNode;
		for (String id : wanted.split(",")) {
			String n = id.trim().replace('-', ':');
			if (!n.isEmpty() && !"0:1".equals(n)) ids.add(n);
		}
		return new FileRef(key, ids);
	}

	/** GET /v1/files/:key, or GET /v1/files/:key/nodes?ids=.. when nodes are given (much smaller response). */
	public static JSONObject fetch(FileRef ref, String token, Auth auth) {
		if (ref.nodeIds.isEmpty()) return get("/v1/files/" + ref.fileKey, token, auth);
		return get("/v1/files/" + ref.fileKey + "/nodes?ids=" + URLEncoder.encode(String.join(",", ref.nodeIds), StandardCharsets.UTF_8), token, auth);
	}

	public static JSONObject get(String path, String token, Auth auth) {
		if (token == null || token.trim().isEmpty()) throw new IllegalArgumentException("Figma token is required");
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(API + path)).timeout(Duration.ofSeconds(120)).GET();
		if (auth == Auth.OAUTH) request.header("Authorization", "Bearer " + token.trim());
		else request.header("X-Figma-Token", token.trim());
		try {
			HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			int status = response.statusCode();
			if (status == 200) return new JSONObject(response.body());
			String body = response.body() == null ? "" : response.body();
			if (body.length() > 300) body = body.substring(0, 300);
			if (status == 429) {
				String retry = response.headers().firstValue("Retry-After").orElse("?");
				throw new IllegalStateException("Figma API rate limit (429). Retry after " + retry + "s. " + body);
			}
			if (status == 403) throw new IllegalStateException("Figma API 403: token expired or missing scope file_content:read. " + body);
			throw new IllegalStateException("Figma API " + status + " for " + path + ": " + body);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Figma API call interrupted", e);
		} catch (java.io.IOException e) {
			throw new IllegalStateException("Figma API call failed: " + e.getMessage(), e);
		}
	}
}
