package com.tomatosystem.figma.ai;

import com.tomatosystem.exconverter.service.ProgressLog;
import com.tomatosystem.figma.FigmaApiClient;
import com.tomatosystem.figma.FigmaImages;
import com.tomatosystem.figma.FigmaSettings;

/**
 * The critic's optional frame render (figma.ai.screenshot=true): one {@link FigmaImages#render} scaled so the long
 * side stays near figma.ai.screenshot.maxSide, and never an exception — the critic simply goes without the image.
 */
public final class FigmaScreenshots {
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
			byte[] bytes = FigmaImages.render(fileKey, nodeId, scale, token, auth);
			// A few MB of base64 would dominate the request; skip the image rather than blow the quota.
			if (bytes.length > 4 * 1024 * 1024) {
				ProgressLog.step("렌더 이미지가 너무 커서 생략 ({} KB)", bytes.length / 1024);
				return null;
			}
			return bytes;
		} catch (RuntimeException e) {
			ProgressLog.step("화면 렌더 실패(무시하고 계속): {}", e.getMessage());
			return null;
		}
	}
}
