package com.tomatosystem.type;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import static com.tomatosystem.utill.NodeConverterUtils.*;
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
    //이미지 URL 사용이 아니고 다운받는 방식 
    private void downloadImage(String imageUrl, String outputFilePath) throws IOException {
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        
        // 이미지 파일을 읽어오기 위한 InputStream
        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(outputFilePath)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
//    이미지 URL 사용이 아니고 다운받는 방식 
//    public boolean convert(FileWriter writer, Map<String, Object> element, String name,
//            double x, double y, double width, double height,
//            double parentX, double parentY, String style, int depth) throws IOException {
//
//        String type = (String) element.get("type");
//
//        if ("VECTOR".equalsIgnoreCase(type) || "IMAGE".equalsIgnoreCase(type)) {
//            String nodeId = (String) element.get("id");
//            String imageUrl = fetchImageUrl(fileKey, nodeId);
//
//            if (imageUrl != null && !imageUrl.isEmpty()) {
//                String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
//
//                // 실제 저장 디렉토리 생성
//                String imageDir = "C:\\Users\\LCM\\git\\Converter-Figma\\clx-src\\image\\" + today;
//                Files.createDirectories(Paths.get(imageDir));
//
//                // 파일명 생성
//                String imageFileName = generateId() + ".png";
//                String outputFilePath = imageDir + "\\" + imageFileName;
//
//                // 다운로드
//                downloadImage(imageUrl, outputFilePath);
//
//                // CLEOPATRA에서 쓸 상대 경로
//                String relativePath = "image/" + today + "/" + imageFileName;
//
//                String indent = "    ".repeat(depth);
//                writer.write(indent + "<cl:image src=\"" + relativePath + "\" />\n");
//
//                writer.write(indent + "<cl:xylayoutdata top=\"" + (int) (y - parentY) + "px\" " +
//                    "left=\"" + (int) (x - parentX) + "px\" " +
//                    "width=\"" + (int) width + "px\" height=\"" + (int) height + "px\" " +
//                    "horizontalAnchor=\"LEFT\" verticalAnchor=\"TOP\"/>\n");
//            }
//
//            return false; // 하위 children은 처리하지 않음
//        }
//
//        return true;
//    }
    
    // 유틸리티 메서드들 (예시로 구현)
//    private String generateId() {
//        return UUID.randomUUID().toString().replace("-", "");
//    }
//
//    private void writeLayoutData(FileWriter writer, double x, double y, double width, double height,
//                                 double parentX, double parentY, int depth) throws IOException {
//        // Layout Data 처리 로직
//        String indent = "    ".repeat(depth);
//        writer.write(indent + "  <cl:xylayoutdata top=\"" + (int) (y - parentY) + "px\" " +
//                "left=\"" + (int) (x - parentX) + "px\" " +
//                "width=\"" + (int) width + "px\" height=\"" + (int) height + "px\" " +
//                "horizontalAnchor=\"LEFT\" verticalAnchor=\"TOP\"/>\n");
//    }
//
//    private String escapeXml(String input) {
//        // XML 특수문자 처리
//        if (input == null) return "";
//        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
//    }
}
