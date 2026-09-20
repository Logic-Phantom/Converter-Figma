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
 * Local Ollama (/api/generate, stream=false, format=json). Free and nothing leaves the network, so this is the
 * option for confidential designs. Reviewing a compact UI-IR is a text task, so a small model is enough; an image
 * is only sent when the configured model is a vision model.
 */
final class OllamaAiClient implements AiClient {
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

	private final String baseUrl = FigmaSettings.get("figma.ai.ollama.url", "http://127.0.0.1:11434").replaceAll("/+$", "");
	private final String model = FigmaSettings.get("figma.ai.ollama.model", "qwen3:4b");
	private final int timeout = intOf("figma.ai.timeoutSeconds", 60);

	@Override public boolean isEnabled() { return !model.isEmpty(); }

	@Override public String describe() { return "ollama:" + model + " @ " + baseUrl; }

	@Override
	public String completeJson(String systemPrompt, String userPrompt, byte[] pngImage) {
		if (!isEnabled()) return null;
		JSONObject request = new JSONObject()
			.put("model", model)
			.put("system", systemPrompt)
			.put("prompt", userPrompt)
			.put("stream", false)
			.put("format", "json")
			.put("think", false)
			.put("options", new JSONObject().put("temperature", 0));
		if (pngImage != null && pngImage.length > 0) request.put("images", new JSONArray().put(Base64.getEncoder().encodeToString(pngImage)));
		try {
			HttpRequest http = HttpRequest.newBuilder(URI.create(baseUrl + "/api/generate"))
				.timeout(Duration.ofSeconds(timeout))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(request.toString(), StandardCharsets.UTF_8)).build();
			HttpResponse<String> response = HTTP.send(http, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() != 200) {
				ProgressLog.step("AI 호출 실패 {} (ollama {}): {}", response.statusCode(), model, response.body());
				return null;
			}
			JSONObject body = new JSONObject(response.body());
			String text = body.optString("response", "");
			// qwen3 sometimes answers in "thinking" when think=false.
			return text.trim().isEmpty() ? body.optString("thinking", "") : text;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (Exception e) {
			ProgressLog.step("AI 호출 예외(ollama): {}", e.getMessage());
			return null;
		}
	}

	private static int intOf(String key, int defaultValue) {
		try { return Integer.parseInt(FigmaSettings.get(key, String.valueOf(defaultValue))); } catch (NumberFormatException e) { return defaultValue; }
	}
}
