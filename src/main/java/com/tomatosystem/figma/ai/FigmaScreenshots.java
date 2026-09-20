package com.tomatosystem.figma.ai;

import com.tomatosystem.exconverter.service.ProgressLog;
import com.tomatosystem.figma.FigmaApiClient;
import com.tomatosystem.figma.FigmaSettings;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.json.JSONObject;

/**
 * Renders one frame with GET /v1/images (Tier 1, same file_content:read scope) so the critic can compare the UI-IR
 * with what the screen actually looks like. Optional: only used when figma.ai.screenshot=true.
 */
public final class FigmaScreenshots {
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();

	private FigmaScreenshots() { }

	public static boolean isEnabled() { return "true".equalsIgnoreCase(FigmaSettings.get("figma.ai.screenshot", "false")); }

	/**
	 * @param frameWidth design width, used to keep the PNG near maxSide so image tokens stay small
	 * @return PNG bytes, or null when disabled or anything failed
	 */
	public static byte[] render(String fileKey, String nodeId, double frameWidth, String token, FigmaApiClient.Auth auth) {
		if (!isEnabled() || fileKey == null || fileKey.isEmpty() || nodeId == null || nodeId.isEmpty()) return null;
		try {
			int maxSide = Integer.parseInt(FigmaSettings.get("figma.ai.screenshot.maxSide", "1024"));
			double scale = frameWidth > 0 ? Math.min(1.0, Math.max(0.1, maxSide / frameWidth)) : 1.0;
			String path = "/v1/images/" + fileKey + "?ids=" + URLEncoder.encode(nodeId, StandardCharsets.UTF_8)
				+ "&format=png&scale=" + String.format(java.util.Locale.ROOT, "%.2f", scale);
			JSONObject response = FigmaApiClient.get(path, token, auth);
			JSONObject images = response.optJSONObject("images");
			String url = images == null ? "" : images.optString(nodeId, "");
			if (url.isEmpty() || "null".equals(url)) return null;
			HttpResponse<byte[]> image = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60)).GET().build(),
				HttpResponse.BodyHandlers.ofByteArray());
			if (image.statusCode() != 200) return null;
			byte[] bytes = image.body();
			// A few MB of base64 would dominate the request; skip the image rather than blow the quota.
			if (bytes.length > 4 * 1024 * 1024) {
				ProgressLog.step("렌더 이미지가 너무 커서 생략 ({} KB)", bytes.length / 1024);
				return null;
			}
			return bytes;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (Exception e) {
			ProgressLog.step("화면 렌더 실패(무시하고 계속): {}", e.getMessage());
			return null;
		}
	}
}
