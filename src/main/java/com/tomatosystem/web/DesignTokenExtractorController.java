package com.tomatosystem.web;

import com.cleopatra.protocol.data.DataRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomatosystem.figma.FigmaApiClient;
import com.tomatosystem.figma.FigmaSettings;
import com.tomatosystem.service.DesignTokenExtractorService;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * v1.x design-token export (CSS/SCSS/Tailwind/JSON of every colour/font/spacing seen in the file). The eXBuilder
 * theme sync (v2.2) lives at /figma/theme/sync.do. Figma access goes through {@link FigmaApiClient} like every
 * other endpoint (token from the request, else FIGMA_DIRECT_TOKEN).
 */
@RestController
@RequestMapping("/figma/design-tokens")
public class DesignTokenExtractorController {

    private final DesignTokenExtractorService designTokenExtractorService;
    private final ObjectMapper objectMapper;
    private static final String BASE_PATH = com.tomatosystem.figma.FigmaPaths.clxSrc("result", "design-tokens").getAbsolutePath();

    public DesignTokenExtractorController(DesignTokenExtractorService designTokenExtractorService) {
        this.designTokenExtractorService = designTokenExtractorService;
        this.objectMapper = new ObjectMapper();
    }

    private String getOutputPath() {
        LocalDate today = LocalDate.now();
        String datePath = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return BASE_PATH + "/" + datePath;
    }

    /** Parameters (optional): token, url | fileKey; defaults figma.direct.token / figma.analysis.fileKey. */
    @RequestMapping("/extract.do")
    public ResponseEntity<String> extractDesignTokens(HttpServletRequest request, HttpServletResponse response, DataRequest dataRequest) {
        try {
            String token = ConversionRequests.firstNonBlank(request.getParameter("token"), FigmaSettings.get("figma.direct.token", ""));
            String file = ConversionRequests.firstNonBlank(request.getParameter("url"), request.getParameter("fileKey"), FigmaSettings.get("figma.analysis.fileKey", "rXU0zhKF2HjzFsND9njYbq"));
            FigmaApiClient.FileRef ref = FigmaApiClient.parse(file, null);
            JsonNode figmaJson = objectMapper.readTree(FigmaApiClient.get("/v1/files/" + ref.fileKey, token, FigmaApiClient.Auth.PERSONAL_TOKEN).toString());

            String outputPath = getOutputPath();
            Files.createDirectories(Paths.get(outputPath));
            designTokenExtractorService.extractDesignTokens(figmaJson, outputPath);
            return ResponseEntity.ok("Design tokens extracted successfully. Files saved in: " + outputPath);
        } catch (Exception e) {
            return ResponseEntity.status(e instanceof IllegalArgumentException ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR)
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
            Path base = Paths.get(BASE_PATH).toAbsolutePath().normalize();
            Path filePath = base.resolve(date).resolve(fileName).normalize();
            if (!filePath.startsWith(base) || !Files.exists(filePath)) {
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
}
