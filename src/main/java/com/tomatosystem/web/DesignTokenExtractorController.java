package com.tomatosystem.web;

import com.cleopatra.protocol.data.DataRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomatosystem.service.DesignTokenExtractorService;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/figma/design-tokens")
public class DesignTokenExtractorController {

    private final DesignTokenExtractorService designTokenExtractorService;
    private final ObjectMapper objectMapper;

    public DesignTokenExtractorController(DesignTokenExtractorService designTokenExtractorService) {
        this.designTokenExtractorService = designTokenExtractorService;
        this.objectMapper = new ObjectMapper();
    }

    @RequestMapping("/extract.do")
    public ResponseEntity<String> extractDesignTokens(HttpServletRequest request, HttpServletResponse response, DataRequest dataRequest) {
        String token = "사용자 토큰"; // TODO: Implement proper token management
        String fileKey = "rXU0zhKF2HjzFsND9njYbq"; // TODO: Make this configurable
        String url = "https://api.figma.com/v1/files/" + fileKey;

        try {
            // Fetch Figma data
            Map<String, Object> figmaData = fetchFigmaData(url, token);
            if (figmaData == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch Figma data");
            }

            // Convert Map to JsonNode
            JsonNode figmaJson = objectMapper.valueToTree(figmaData);

            // Create output directory if it doesn't exist
            String outputPath = "design-tokens";
            new File(outputPath).mkdirs();

            // Extract design tokens
            designTokenExtractorService.extractDesignTokens(figmaJson, outputPath);

            return ResponseEntity.ok("Design tokens extracted successfully");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during design token extraction: " + e.getMessage());
        }
    }

    private Map<String, Object> fetchFigmaData(String url, String token) {
        HttpGet getRequest = new HttpGet(url);
        getRequest.addHeader("X-Figma-Token", token);

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(getRequest)) {

            if (response.getStatusLine().getStatusCode() == 200) {
                String body = EntityUtils.toString(response.getEntity());
                return objectMapper.readValue(body, Map.class);
            } else {
                throw new RuntimeException("Failed to call Figma API: " + response.getStatusLine());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Figma data", e);
        }
    }
} 