package com.tomatosystem.web;

import com.tomatosystem.service.AdvancedFigmaToClxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cleopatra.protocol.data.DataRequest;
import com.cleopatra.protocol.data.ParameterGroup;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/design")
public class AdvancedDesignController {
    @Autowired
    private AdvancedFigmaToClxService advancedFigmaToClxService;

    @RequestMapping("/convertAdvanced.do")
    public ResponseEntity<String> convertAdvancedClx(DataRequest dataRequest) {
        ParameterGroup dm = dataRequest.getParameterGroup("dmParam");
        String token = dm.getValue("token");
        String fileKey = dm.getValue("fileKey");
        try {
            String resultPath = advancedFigmaToClxService.convertFigmaJsonToClx(token, fileKey);
            return ResponseEntity.ok("Advanced CLX file saved at: " + resultPath);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @RequestMapping("/convertJsonToFormClx.do")
    public ResponseEntity<String> convertJsonToFormClx(String jsonFilePath, String outputPath) {
        try {
            // JSON 파일 읽기
            byte[] jsonData = Files.readAllBytes(Paths.get(jsonFilePath));
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> figmaJson = objectMapper.readValue(jsonData, Map.class);
            // 변환
            String clxXml = com.tomatosystem.util.ClxLayoutUtil.convertFigmaJsonToClxXml(figmaJson);
            com.tomatosystem.util.ClxLayoutUtil.saveClxToFile(clxXml, outputPath);
            return ResponseEntity.ok("변환 완료: " + outputPath);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("오류: " + e.getMessage());
        }
    }

    @RequestMapping("/convertFigmaToFormClx.do")
    public ResponseEntity<String> convertFigmaToFormClx(String token, String fileKey, String outputDir) {
        try {
            String today = java.time.LocalDate.now().toString();
            String fileName = today + "_form_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String clxPath = outputDir + fileName + ".clx";
            String jsPath = outputDir + fileName + ".js";

            // Figma API에서 JSON fetch
            String url = "https://api.figma.com/v1/files/" + fileKey;
            Map<String, Object> figmaJson = fetchFigmaData(url, token);
            // 변환
            String clxXml = com.tomatosystem.util.ClxLayoutUtil.convertFigmaJsonToClxXml(figmaJson);
            com.tomatosystem.util.ClxLayoutUtil.saveClxToFile(clxXml, clxPath);
            // JS 파일(빈 내용) 생성
            try (java.io.FileWriter jsWriter = new java.io.FileWriter(jsPath)) {
                jsWriter.write("");
            }
            return ResponseEntity.ok("변환 완료: " + clxPath + ", " + jsPath);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("오류: " + e.getMessage());
        }
    }

    // fetchFigmaData 재사용
    private Map<String, Object> fetchFigmaData(String url, String token) {
        org.apache.http.client.methods.HttpGet getRequest = new org.apache.http.client.methods.HttpGet(url);
        getRequest.addHeader("X-Figma-Token", token);
        try (org.apache.http.impl.client.CloseableHttpClient client = org.apache.http.impl.client.HttpClients.createDefault();
             org.apache.http.client.methods.CloseableHttpResponse response = client.execute(getRequest)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                String body = org.apache.http.util.EntityUtils.toString(response.getEntity());
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return objectMapper.readValue(body, Map.class);
            } else {
                throw new RuntimeException("Figma API 호출 실패: " + response.getStatusLine());
            }
        } catch (Exception e) {
            throw new RuntimeException("Figma 데이터 가져오기 실패", e);
        }
    }
} 