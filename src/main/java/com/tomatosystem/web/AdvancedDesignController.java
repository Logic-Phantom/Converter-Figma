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
} 