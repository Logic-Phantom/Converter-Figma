package com.tomatosystem.figma;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.json.JSONObject;

/**
 * Frame renders through GET /v1/images/:key (Tier 1, scope file_content:read). Figma answers with a temporary URL
 * of the PNG, which is downloaded here. Shared by the AI critic (small, optional) and the visual QA (full size).
 */
public final class FigmaImages {
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();

	private FigmaImages() { }

	/**
	 * @param scale 0.01 … 4 (Figma's limits)
	 * @return PNG bytes
	 * @throws IllegalStateException when Figma refuses (rate limit, scope, node without render) or the download fails
	 */
	public static byte[] render(String fileKey, String nodeId, double scale, String token, FigmaApiClient.Auth auth) {
		if (fileKey == null || fileKey.isEmpty() || nodeId == null || nodeId.isEmpty()) throw new IllegalArgumentException("fileKey 와 nodeId 가 필요합니다");
		String id = nodeId.replace('-', ':');
		String path = "/v1/images/" + fileKey + "?ids=" + URLEncoder.encode(id, StandardCharsets.UTF_8)
			+ "&format=png&scale=" + String.format(java.util.Locale.ROOT, "%.2f", Math.max(0.01, Math.min(4, scale)));
		JSONObject response = FigmaApiClient.get(path, token, auth);
		JSONObject images = response.optJSONObject("images");
		String url = images == null ? "" : images.optString(id, "");
		if (url.isEmpty() || "null".equals(url)) throw new IllegalStateException("Figma 가 노드 " + id + " 의 렌더 이미지를 주지 않았습니다: " + response.optString("err", ""));
		try {
			HttpResponse<byte[]> image = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(120)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
			if (image.statusCode() != 200) throw new IllegalStateException("렌더 이미지 다운로드 " + image.statusCode());
			return image.body();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("렌더 이미지 다운로드 중단", e);
		} catch (java.io.IOException e) {
			throw new IllegalStateException("렌더 이미지 다운로드 실패: " + e.getMessage(), e);
		}
	}
}
