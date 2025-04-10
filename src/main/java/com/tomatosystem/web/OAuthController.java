package com.tomatosystem.web;

import com.cleopatra.XBConfig;
import com.cleopatra.protocol.data.DataRequest;
import com.cleopatra.spring.UIView;
import com.cleopatra.ui.PageConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.http.*;
import org.springframework.mobile.device.Device;
import org.springframework.mobile.device.DeviceUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.View;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
public class OAuthController {

    private String clientId;
    private String clientSecret;
    private String redirectUri;

    // 🔧 properties 직접 읽기
    @PostConstruct
    public void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new FileNotFoundException("application.properties 파일을 찾을 수 없습니다.");
            }

            Properties prop = new Properties();
            prop.load(input);

            clientId = prop.getProperty("figma.client.id");
            clientSecret = prop.getProperty("figma.client.secret");
            redirectUri = prop.getProperty("figma.redirect.uri");

            if (clientId == null || clientSecret == null || redirectUri == null) {
                throw new IllegalArgumentException("필수 Figma OAuth 설정이 누락되었습니다.");
            }

        } catch (IOException e) {
            throw new RuntimeException("application.properties 읽기 실패: " + e.getMessage(), e);
        }
    }

    // 🔐 1. 사용자 브라우저를 Figma 로그인 창으로 리디렉트
    @GetMapping("/oauth/login.do")
    public void redirectToFigma(HttpServletResponse response) throws IOException {
        String encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String url = "https://www.figma.com/oauth?client_id=" + clientId
                + "&redirect_uri=" + encodedRedirect
                + "&scope=file_read&state=test-state&response_type=code";
        response.sendRedirect(url);
    }

//    @GetMapping("/oauth/callback.do")
//    public ResponseEntity<String> figmaCallback(@RequestParam String code) {
//        try {
//            // ✅ URL 수정
//            HttpPost post = new HttpPost("https://api.figma.com/v1/oauth/token");
//
//            List<BasicNameValuePair> params = List.of(
//                new BasicNameValuePair("client_id", clientId),
//                new BasicNameValuePair("client_secret", clientSecret),
//                new BasicNameValuePair("redirect_uri", redirectUri),
//                new BasicNameValuePair("code", code),
//                new BasicNameValuePair("grant_type", "authorization_code")
//            );
//
//            post.setEntity(new UrlEncodedFormEntity(params));
//
//            try (CloseableHttpClient client = HttpClients.createDefault();
//                 CloseableHttpResponse response = client.execute(post)) {
//
//                String json = EntityUtils.toString(response.getEntity());
//                System.out.println("🔍 Figma 응답 JSON: " + json);
//
//                ObjectMapper mapper = new ObjectMapper();
//                Map<String, Object> tokenData = mapper.readValue(json, Map.class);
//
//                if (tokenData.containsKey("error")) {
//                    String error = tokenData.get("message").toString();
//                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                            .body("❌ Figma 오류: " + error);
//                }
//
//                String accessToken = (String) tokenData.get("access_token");
//
//                return ResponseEntity.ok("✅ Access Token: " + accessToken);
//            }
//
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("❌ 예외 발생: " + e.getMessage());
//        }
//    }
    @GetMapping("/oauth/callback.do")
    public View figmaCallback(@RequestParam String code, DataRequest reqData) throws Exception {
        Map<String, String> initParam = new HashMap<>();

        try {
            HttpPost post = new HttpPost("https://api.figma.com/v1/oauth/token");

            List<BasicNameValuePair> params = List.of(
                new BasicNameValuePair("client_id", clientId),
                new BasicNameValuePair("client_secret", clientSecret),
                new BasicNameValuePair("redirect_uri", redirectUri),
                new BasicNameValuePair("code", code),
                new BasicNameValuePair("grant_type", "authorization_code")
            );

            post.setEntity(new UrlEncodedFormEntity(params));

            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(post)) {

                String json = EntityUtils.toString(response.getEntity());
                System.out.println("🔍 Figma 응답 JSON: " + json);

                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> tokenData = mapper.readValue(json, Map.class);

                if (tokenData.containsKey("error")) {
                    String error = (String) tokenData.get("message");
                    throw new RuntimeException("Figma 인증 실패: " + error);
                }

                // ✅ access_token 추출
                String accessToken = (String) tokenData.get("access_token");

                // ✅ initParam에 실제 토큰 저장
                initParam.put("token", accessToken);
            }

        } catch (Exception e) {
            e.printStackTrace();
            // 에러 발생 시 기본 에러 메시지 반환
            initParam.put("error", "Figma OAuth 처리 중 오류가 발생했습니다.");
        }

        // ✅ .clx 뷰 페이지로 이동하면서 token 전달
        return new UIView("/ui/design/converterStart.clx", initParam);
    }
}