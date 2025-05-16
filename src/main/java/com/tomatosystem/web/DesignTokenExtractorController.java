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
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/figma/design-tokens")
public class DesignTokenExtractorController {

    private final DesignTokenExtractorService designTokenExtractorService;
    private final ObjectMapper objectMapper;
    private static final String BASE_PATH = "clx-src/result/design-tokens";

    public DesignTokenExtractorController(DesignTokenExtractorService designTokenExtractorService) {
        this.designTokenExtractorService = designTokenExtractorService;
        this.objectMapper = new ObjectMapper();
    }

    private String getOutputPath() {
        LocalDate today = LocalDate.now();
        String datePath = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return BASE_PATH + "/" + datePath;
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

            // Create output directory with today's date
            String outputPath = getOutputPath();
            Files.createDirectories(Paths.get(outputPath));

            // Extract design tokens
            designTokenExtractorService.extractDesignTokens(figmaJson, outputPath);

            return ResponseEntity.ok("Design tokens extracted successfully. Files saved in: " + outputPath);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during design token extraction: " + e.getMessage());
        }
    }

    @GetMapping("/list.do")
    public ResponseEntity<List<Map<String, String>>> listTokenFiles() {
        try {
            String outputPath = getOutputPath();
            File dir = new File(outputPath);
            if (!dir.exists() || !dir.isDirectory()) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<Map<String, String>> files = new ArrayList<>();
            File[] tokenFiles = dir.listFiles((d, name) -> 
                name.endsWith(".json") || 
                name.endsWith(".scss") || 
                name.endsWith(".css") || 
                name.endsWith(".js")
            );

            if (tokenFiles != null) {
                for (File file : tokenFiles) {
                    Map<String, String> fileInfo = new HashMap<>();
                    fileInfo.put("name", file.getName());
                    fileInfo.put("size", String.format("%.2f KB", file.length() / 1024.0));
                    fileInfo.put("lastModified", new java.util.Date(file.lastModified()).toString());
                    fileInfo.put("path", outputPath + "/" + file.getName());
                    files.add(fileInfo);
                }
            }

            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/view/{date}/{fileName:.+}")
    public ResponseEntity<String> viewFileContent(@PathVariable String date, @PathVariable String fileName) {
        try {
            Path filePath = Paths.get(BASE_PATH, date, fileName);
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            String content = Files.readString(filePath);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(getContentType(fileName)))
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading file: " + e.getMessage());
        }
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith(".json")) {
            return "application/json";
        } else if (fileName.endsWith(".scss") || fileName.endsWith(".css")) {
            return "text/css";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript";
        }
        return "text/plain";
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