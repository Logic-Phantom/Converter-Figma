package com.tomatosystem.type;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class VectorNodeConverter {

    private final String token;
    private final String fileKey;

    public VectorNodeConverter(String token, String fileKey) {
        this.token = token;
        this.fileKey = fileKey;
    }

    public boolean convert(FileWriter writer, Map<String, Object> element, String name,
            double x, double y, double width, double height,
            double parentX, double parentY, String style, int depth) throws IOException {

			String indent = "    ".repeat(depth);
			String type = (String) element.get("type");
			
			if ("VECTOR".equalsIgnoreCase(type) || "IMAGE".equalsIgnoreCase(type)) {
			String stdSid = "image-" + generateId();
			String nodeId = (String) element.get("id");
			String imageUrl = fetchImageUrl(fileKey, nodeId); // 파일 키는 생성자에서 받음
			System.out.println("url 확인" + imageUrl);
			if (imageUrl == null || imageUrl.isEmpty()) {
			 System.err.println("⚠️ 이미지 URL 없음: " + nodeId);
			 return false;
			}
			
			writer.write(indent + "<cl:img std:sid=\"" + stdSid + "\" src=\"" + escapeXml(imageUrl) + "\">\n");
			writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
			writer.write(indent + "</cl:img>\n");
			return false; // 처리 완료, 더 이상 children 내려갈 필요 없음
			}
			
			return true;
			}

    private String fetchImageUrl(String fileKey, String nodeId) {
        try {
            URL url = new URL("https://api.figma.com/v1/images/" + fileKey + "?ids=" + nodeId + "&format=png");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("X-Figma-Token", token); // 여기에 token 사용
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                responseBuilder.append(line);
            }
            in.close();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBuilder.toString());
            JsonNode images = root.path("images");
            return images.path(nodeId).asText();
        } catch (Exception e) {
            System.err.println("이미지 URL 가져오기 실패: " + e.getMessage());
            return null;
        }
    }

    // 유틸리티 메서드들 (예시로 구현)
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void writeLayoutData(FileWriter writer, double x, double y, double width, double height,
                                 double parentX, double parentY, int depth) throws IOException {
        // Layout Data 처리 로직
        String indent = "    ".repeat(depth);
        writer.write(indent + "  <cl:xylayoutdata top=\"" + (int) (y - parentY) + "px\" " +
                "left=\"" + (int) (x - parentX) + "px\" " +
                "width=\"" + (int) width + "px\" height=\"" + (int) height + "px\" " +
                "horizontalAnchor=\"LEFT\" verticalAnchor=\"TOP\"/>\n");
    }

    private String escapeXml(String input) {
        // XML 특수문자 처리
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
