package com.tomatosystem.service;

import com.tomatosystem.util.ClxLayoutUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Service
public class AdvancedFigmaToClxService {
    public String convertFigmaJsonToClx(String token, String fileKey) throws Exception {
        // 1. Figma API에서 JSON 데이터 가져오기
        String url = "https://api.figma.com/v1/files/" + fileKey;
        Map<String, Object> figmaJson = fetchFigmaData(url, token);
        // 2. 계층 구조 분석 및 clx XML 변환
        String clxXml = ClxLayoutUtil.convertFigmaJsonToClxXml(figmaJson);
        // 3. 파일로 저장
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String outputDir = "C:\\Users\\LCM\\git\\Converter-Figma\\clx-src\\convertTest\\" + today + "\\form\\";
        Files.createDirectories(Paths.get(outputDir));
        String randomStr = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String fileName = today + "_advanced_" + randomStr + ".clx";
        String filePath = outputDir + File.separator + fileName;
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(clxXml);
        }
        return filePath;
    }

    private Map<String, Object> fetchFigmaData(String url, String token) throws Exception {
        HttpGet getRequest = new HttpGet(url);
        getRequest.addHeader("X-Figma-Token", token);
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(getRequest)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                String body = EntityUtils.toString(response.getEntity());
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(body, Map.class);
            } else {
                throw new RuntimeException("Figma API 호출 실패: " + response.getStatusLine());
            }
        }
    }
} 