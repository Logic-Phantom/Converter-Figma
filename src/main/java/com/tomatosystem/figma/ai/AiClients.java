package com.tomatosystem.figma.ai;

import com.tomatosystem.figma.FigmaSettings;
import java.util.Locale;

/** Picks the provider from figma.ai.provider. Default "none" = the deterministic v2.0 pipeline, no AI call. */
public final class AiClients {
	private AiClients() { }

	public static AiClient create() { return create(FigmaSettings.get("figma.ai.provider", "none")); }

	public static AiClient create(String provider) {
		String name = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
		if (name.equals("gemini")) return new GeminiAiClient();
		if (name.equals("ollama")) return new OllamaAiClient();
		return DISABLED;
	}

	/** Never calls anything; keeps every caller on the deterministic result. */
	public static final AiClient DISABLED = new AiClient() {
		@Override public String completeJson(String systemPrompt, String userPrompt, byte[] pngImage) { return null; }
		@Override public String describe() { return "none"; }
		@Override public boolean isEnabled() { return false; }
	};
}
