package com.tomatosystem.figma.ai;

/**
 * One text (optionally text + image) completion that must answer with JSON.
 * Implementations never throw for a service problem: they return null so the caller keeps the deterministic result.
 */
public interface AiClient {
	/**
	 * @param systemPrompt rules for the model
	 * @param userPrompt   the question (compact UI-IR, validator errors, labels …)
	 * @param pngImage     optional screenshot of the frame, PNG bytes; may be null
	 * @return the model's JSON answer as text, or null when the provider is off, misconfigured or failed
	 */
	String completeJson(String systemPrompt, String userPrompt, byte[] pngImage);

	/** Provider name for logs and /designAi/status.do ("none", "gemini:gemini-3.5-flash-lite", "ollama:qwen3:4b"). */
	String describe();

	/** False when no call will ever be made (no key, provider=none): callers skip prompt building entirely. */
	boolean isEnabled();
}
