package com.tomatosystem.web;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cleopatra.protocol.data.DataRequest;
import com.cleopatra.protocol.data.UploadFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomatosystem.service.JsonDiffAnalyzerService;

@RestController
@RequestMapping("/figma")
public class JsonDiffAnalyzerController {

    private final JsonDiffAnalyzerService jsonDiffAnalyzerService;

    public JsonDiffAnalyzerController(JsonDiffAnalyzerService jsonDiffAnalyzerService) {
        this.jsonDiffAnalyzerService = jsonDiffAnalyzerService;
    }

    @RequestMapping("/fetchAndAnalyzeFigmaData.do")
    public ResponseEntity<String> fetchAndAnalyzeFigmaData(HttpServletRequest request, HttpServletResponse response, DataRequest dataRequest) {
        String token = "사용자 토큰";
        String fileKey = "rXU0zhKF2HjzFsND9njYbq";
        String url = "https://api.figma.com/v1/files/" + fileKey;

        try {
            // Figma 데이터 가져오기
            Map<String, Object> rawData = fetchFigmaDataDirect(url, token);

            if (rawData == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Figma 데이터 가져오기 실패");
            }

            // 업로드된 파일 처리 (JSON 파일)
            Map<String, UploadFile[]> uploadFiles = dataRequest.getUploadFiles();
            System.out.println("업로드 파일 == " + uploadFiles);
            boolean hasJsonFile = false;

            if (uploadFiles != null && !uploadFiles.isEmpty()) {
                for (UploadFile[] uFiles : uploadFiles.values()) {
                    for (UploadFile uFile : uFiles) {
                        File file = uFile.getFile();
                        System.out.println("파일 객체: " + file);
                        if (file != null) {
                            String fileName = file.getName();
                            System.out.println("파일 이름: " + fileName);
                            if (fileName.toLowerCase().contains(".json")) {
                                hasJsonFile = true;

                                Map<String, Object> uploadedJsonData = readJsonFromFile(file);
                                if (uploadedJsonData == null) {
                                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("업로드된 JSON 파일 읽기 실패");
                                }

                                System.out.println("업로드 json == " + uploadedJsonData);
                                jsonDiffAnalyzerService.analyzeJsonData(rawData, uploadedJsonData);
                                return ResponseEntity.ok("Figma 데이터와 업로드된 JSON 파일 분석 완료.");
                            }
                        }
                    }
                }
            }

            // JSON 파일이 아예 없었을 경우
            if (!hasJsonFile) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("JSON 형식의 업로드 파일이 없습니다.");
            }

            return ResponseEntity.ok("Figma 데이터 분석 완료."); // 이건 사실상 도달하지 않음

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("요청 처리 중 오류 발생: " + e.getMessage());
        }
    }

    private Map<String, Object> readJsonFromFile(File file) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(file, Map.class); // JSON을 Map 형식으로 변환
        } catch (IOException e) {
            return null;
        }
    }

    private Map<String, Object> fetchFigmaDataDirect(String url, String token) {
        HttpGet getRequest = new HttpGet(url);
        getRequest.addHeader("X-Figma-Token", token);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = client.execute(getRequest)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    String body = EntityUtils.toString(response.getEntity());
                    ObjectMapper objectMapper = new ObjectMapper();
                    return objectMapper.readValue(body, Map.class);
                } else {
                    throw new RuntimeException("Figma API 호출 실패: " + response.getStatusLine());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Figma 데이터 가져오기 실패", e);
        }
    }
}