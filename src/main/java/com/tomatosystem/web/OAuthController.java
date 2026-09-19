package com.tomatosystem.web;

import com.cleopatra.spring.UIView;
import com.tomatosystem.figma.FigmaSettings;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.View;

/**
 * Figma OAuth 2 (https://developers.figma.com/docs/rest-api/oauth-apps/).
 * - Scopes: file_read / files:read were removed on 2025-11-17. Default is "file_content:read projects:read"
 *   (projects:read for the team → project → file listing of /design/convert.do).
 * - Token exchange: client id/secret in an HTTP Basic header, not in the body.
 * - state: random per login, checked on the callback (CSRF).
 * Settings: figma.client.id, figma.client.secret, figma.redirect.uri, figma.oauth.scope — prefer environment
 * variables (FIGMA_CLIENT_SECRET ...) over application.properties for the secret.
 */
@RestController
public class OAuthController {
	private static final String STATE = "figmaOAuthState";
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
	private static final SecureRandom RANDOM = new SecureRandom();

	/** 1. Redirects the browser to the Figma consent screen. */
	@GetMapping("/oauth/login.do")
	public void redirectToFigma(HttpServletResponse response, HttpSession session) throws IOException {
		byte[] bytes = new byte[24];
		RANDOM.nextBytes(bytes);
		String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		session.setAttribute(STATE, state);
		String url = "https://www.figma.com/oauth?client_id=" + enc(setting("figma.client.id"))
			+ "&redirect_uri=" + enc(setting("figma.redirect.uri"))
			+ "&scope=" + enc(FigmaSettings.get("figma.oauth.scope", "file_content:read projects:read"))
			+ "&state=" + enc(state) + "&response_type=code";
		response.sendRedirect(url);
	}

	/** 2. Exchanges the code for an access token and opens converterStart.clx with it. */
	@GetMapping("/oauth/callback.do")
	public View figmaCallback(@RequestParam(required = false) String code, @RequestParam(required = false) String state,
			@RequestParam(required = false) String error, HttpSession session) {
		Map<String, String> initParam = new HashMap<String, String>();
		Object expected = session.getAttribute(STATE);
		session.removeAttribute(STATE);
		try {
			if (error != null) throw new IllegalStateException("Figma 인증 거부: " + error);
			if (code == null || expected == null || !expected.equals(state)) throw new IllegalStateException("OAuth state 불일치 (다시 로그인하세요)");
			String basic = Base64.getEncoder().encodeToString((setting("figma.client.id") + ":" + setting("figma.client.secret")).getBytes(StandardCharsets.UTF_8));
			String form = "redirect_uri=" + enc(setting("figma.redirect.uri")) + "&code=" + enc(code) + "&grant_type=authorization_code";
			HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.figma.com/v1/oauth/token"))
				.timeout(Duration.ofSeconds(30))
				.header("Authorization", "Basic " + basic)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(form)).build();
			HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			JSONObject token = new JSONObject(response.body());
			if (response.statusCode() != 200 || !token.has("access_token")) {
				throw new IllegalStateException("Figma 토큰 발급 실패 (" + response.statusCode() + "): " + token.optString("message", token.optString("error")));
			}
			initParam.put("token", token.getString("access_token"));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			initParam.put("error", "Figma OAuth 처리 중 오류가 발생했습니다.");
		} catch (Exception e) {
			initParam.put("error", "Figma OAuth 처리 중 오류가 발생했습니다: " + e.getMessage());
		}
		return new UIView("/ui/design/converterStart.clx", initParam);
	}

	private static String setting(String key) {
		String value = FigmaSettings.get(key, "");
		if (value.isEmpty()) throw new IllegalStateException(key + " 가 설정되지 않았습니다.");
		return value;
	}

	private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
