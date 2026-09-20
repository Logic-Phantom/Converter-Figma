package com.tomatosystem.figma.ai;

import com.tomatosystem.exconverter.service.ProgressLog;
import com.tomatosystem.figma.FigmaSettings;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Google Gemini (generateContent, non-streaming, JSON response). Free tier works; quotas are per model and per day,
 * and on the free tier Google may use the input to improve its products — do not point it at confidential designs.
 * Requests here are small: a compact UI-IR (1~4KB), optionally one downscaled PNG.
 */
final class GeminiAiClient implements AiClient {
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

	private final String apiKey = FigmaSettings.get("figma.ai.gemini.apiKey", "");
	private final String model = FigmaSettings.get("figma.ai.gemini.model", "gemini-3.5-flash-lite");
	private final String baseUrl = FigmaSettings.get("figma.ai.gemini.url", "https://generativelanguage.googleapis.com").replaceAll("/+$", "");
	private final int timeout = intOf("figma.ai.timeoutSeconds", 60);
	private final int retries = intOf("figma.ai.maxRetries", 1);
	private final int maxOutputTokens = intOf("figma.ai.maxOutputTokens", 4096);

	@Override public boolean isEnabled() { return !apiKey.isEmpty(); }

	@Override public String describe() { return "gemini:" + model + (apiKey.isEmpty() ? " (키 없음)" : ""); }

	@Override
	public String completeJson(String systemPrompt, String userPrompt, byte[] pngImage) {
		if (!isEnabled()) return null;
		JSONArray parts = new JSONArray();
		if (pngImage != null && pngImage.length > 0) {
			parts.put(new JSONObject().put("inline_data", new JSONObject().put("mime_type", "image/png").put("data", Base64.getEncoder().encodeToString(pngImage))));
		}
		parts.put(new JSONObject().put("text", userPrompt));
		JSONObject request = new JSONObject()
			.put("systemInstruction", new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", systemPrompt))))
			.put("contents", new JSONArray().put(new JSONObject().put("role", "user").put("parts", parts)))
			.put("generationConfig", new JSONObject()
				.put("temperature", 0)
				.put("maxOutputTokens", maxOutputTokens)
				.put("responseMimeType", "application/json")
				// Reading a UI-IR is extraction, not hard reasoning; keep the budget for the answer.
				.put("thinkingConfig", new JSONObject().put("thinkingLevel", "MINIMAL")));

		for (int attempt = 0; attempt <= retries; attempt++) {
			try {
				HttpRequest http = HttpRequest.newBuilder(URI.create(baseUrl + "/v1beta/models/" + model + ":generateContent"))
					.timeout(Duration.ofSeconds(timeout))
					.header("Content-Type", "application/json")
					.header("x-goog-api-key", apiKey)
					.POST(HttpRequest.BodyPublishers.ofString(request.toString(), StandardCharsets.UTF_8)).build();
				HttpResponse<String> response = HTTP.send(http, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
				if (response.statusCode() == 200) {
					String text = firstText(new JSONObject(response.body()));
					if (text != null && !text.trim().isEmpty()) return text;
					ProgressLog.step("AI 응답이 비어 있음 (Gemini)");
					return null;
				}
				// 429 = free-tier quota for today; retrying immediately will not help.
				ProgressLog.step("AI 호출 실패 {} ({}): {}", response.statusCode(), model, excerpt(response.body()));
				if (response.statusCode() == 429 || response.statusCode() == 403 || response.statusCode() == 400) return null;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			} catch (Exception e) {
				ProgressLog.step("AI 호출 예외: {}", e.getMessage());
			}
		}
		return null;
	}

	/** candidates[0].content.parts[].text, skipping thinking parts. */
	private static String firstText(JSONObject body) {
		JSONArray candidates = body.optJSONArray("candidates");
		if (candidates == null || candidates.length() == 0) return null;
		JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
		JSONArray parts = content == null ? null : content.optJSONArray("parts");
		if (parts == null) return null;
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < parts.length(); i++) {
			JSONObject part = parts.optJSONObject(i);
			if (part == null || part.optBoolean("thought", false)) continue;
			text.append(part.optString("text", ""));
		}
		return text.toString();
	}

	private static String excerpt(String body) {
		if (body == null) return "";
		return body.length() > 200 ? body.substring(0, 200) : body;
	}

	private static int intOf(String key, int defaultValue) {
		try { return Integer.parseInt(FigmaSettings.get(key, String.valueOf(defaultValue))); } catch (NumberFormatException e) { return defaultValue; }
	}
}
