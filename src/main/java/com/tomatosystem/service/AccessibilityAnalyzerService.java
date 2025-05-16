package com.tomatosystem.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tomatosystem.service.webaccess.report.ExcelReportGenerator;
import org.springframework.stereotype.Service;

import java.io.File;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class AccessibilityAnalyzerService {
    
    private static final double WCAG_AA_NORMAL_TEXT_RATIO = 4.5;
    private static final double WCAG_AA_LARGE_TEXT_RATIO = 3.0;
    private static final double WCAG_AAA_NORMAL_TEXT_RATIO = 7.0;
    private static final double WCAG_AAA_LARGE_TEXT_RATIO = 4.5;
    
    private static final int MIN_NORMAL_TEXT_SIZE = 16; // 16px
    private static final int MIN_LARGE_TEXT_SIZE = 24; // 24px or 18.5px if bold
    
    private final ExcelReportGenerator excelReportGenerator;

    public AccessibilityAnalyzerService() {
        this.excelReportGenerator = new ExcelReportGenerator();
    }

    public void analyzeAccessibility(JsonNode figmaJson, String outputPath) {
        try {
            List<Map<String, Object>> accessibilityIssues = new ArrayList<>();
            analyzeNodeAccessibility(figmaJson, accessibilityIssues, new ArrayList<>());

            // 결과를 다양한 형식으로 저장
            saveAnalysisResults(accessibilityIssues, outputPath);
            
            // Excel 리포트 생성
            excelReportGenerator.generateReport(accessibilityIssues, outputPath);
            
            System.out.println("접근성 분석 완료");
        } catch (Exception e) {
            System.err.println("접근성 분석 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void analyzeNodeAccessibility(JsonNode node, List<Map<String, Object>> issues, List<String> parentPath) {
        if (node == null || !node.isObject()) {
            return;
        }

        // 현재 노드의 이름 가져오기
        String nodeName = node.path("name").asText("unnamed");
        List<String> currentPath = new ArrayList<>(parentPath);
        currentPath.add(nodeName);
        String fullPath = String.join(" > ", currentPath);

        // TEXT 타입 노드 분석
        if ("TEXT".equals(node.path("type").asText())) {
            analyzeTextNode(node, issues, fullPath);
        }

        // 자식 노드 분석
        JsonNode children = node.path("children");
        if (children.isArray()) {
            for (JsonNode child : children) {
                analyzeNodeAccessibility(child, issues, currentPath);
            }
        }
    }

    private void analyzeTextNode(JsonNode node, List<Map<String, Object>> issues, String path) {
        // 텍스트 스타일 분석
        JsonNode style = node.path("style");
        if (!style.isMissingNode()) {
            // 폰트 크기 검사
            double fontSize = style.path("fontSize").asDouble(0);
            boolean isBold = style.path("fontWeight").asDouble(400) >= 700;
            checkFontSize(fontSize, isBold, node, issues, path);

            // 색상 대비 검사
            JsonNode fills = style.path("fills");
            JsonNode backgroundColor = node.path("backgroundColor");
            if (!fills.isMissingNode() && fills.isArray() && fills.size() > 0) {
                checkColorContrast(fills.get(0), backgroundColor, node, issues, path);
            }
        }
    }

    private void checkFontSize(double fontSize, boolean isBold, JsonNode node, List<Map<String, Object>> issues, String path) {
        Map<String, Object> issue = new HashMap<>();
        boolean isAccessible = true;
        String recommendation = "";

        if (isBold) {
            if (fontSize < 14) { // 14px for bold text
                isAccessible = false;
                recommendation = "볼드 텍스트의 경우 최소 14px 이상이어야 합니다.";
            }
        } else {
            if (fontSize < MIN_NORMAL_TEXT_SIZE) {
                isAccessible = false;
                recommendation = "일반 텍스트의 경우 최소 16px 이상이어야 합니다.";
            }
        }

        if (!isAccessible) {
            issue.put("type", "FontSize");
            issue.put("path", path);
            issue.put("value", fontSize + "px");
            issue.put("recommendation", recommendation);
            issue.put("text", node.path("characters").asText());
            issue.put("wcagCriteria", "WCAG 2.1 Success Criterion 1.4.4 Resize text");
            issues.add(issue);
        }
    }

    private void checkColorContrast(JsonNode textColor, JsonNode backgroundColor, JsonNode node, List<Map<String, Object>> issues, String path) {
        try {
            Color foreground = getColorFromNode(textColor.path("color"));
            Color background = getColorFromNode(backgroundColor.path("color"));
            
            if (foreground == null || background == null) {
                return;
            }

            double ratio = calculateContrastRatio(foreground, background);
            double fontSize = node.path("style").path("fontSize").asDouble(0);
            boolean isBold = node.path("style").path("fontWeight").asDouble(400) >= 700;
            
            Map<String, Object> issue = new HashMap<>();
            boolean isAccessible = true;
            String recommendation = "";

            // WCAG 기준 검사
            if (fontSize >= MIN_LARGE_TEXT_SIZE || (fontSize >= 18.5 && isBold)) {
                // 큰 텍스트 기준
                if (ratio < WCAG_AA_LARGE_TEXT_RATIO) {
                    isAccessible = false;
                    recommendation = "큰 텍스트의 경우 최소 대비율 3:1이 필요합니다.";
                }
            } else {
                // 일반 텍스트 기준
                if (ratio < WCAG_AA_NORMAL_TEXT_RATIO) {
                    isAccessible = false;
                    recommendation = "일반 텍스트의 경우 최소 대비율 4.5:1이 필요합니다.";
                }
            }

            if (!isAccessible) {
                issue.put("type", "ColorContrast");
                issue.put("path", path);
                issue.put("contrastRatio", String.format("%.2f:1", ratio));
                issue.put("recommendation", recommendation);
                issue.put("text", node.path("characters").asText());
                issue.put("foregroundColor", String.format("#%02x%02x%02x", 
                    foreground.getRed(), foreground.getGreen(), foreground.getBlue()));
                issue.put("backgroundColor", String.format("#%02x%02x%02x", 
                    background.getRed(), background.getGreen(), background.getBlue()));
                issue.put("wcagCriteria", "WCAG 2.1 Success Criterion 1.4.3 Contrast (Minimum)");
                issues.add(issue);
            }
        } catch (Exception e) {
            System.err.println("색상 대비 검사 중 오류 발생: " + e.getMessage());
        }
    }

    private Color getColorFromNode(JsonNode colorNode) {
        if (colorNode == null || colorNode.isMissingNode()) {
            return null;
        }

        try {
            int r = (int)(colorNode.path("r").asDouble() * 255);
            int g = (int)(colorNode.path("g").asDouble() * 255);
            int b = (int)(colorNode.path("b").asDouble() * 255);
            return new Color(r, g, b);
        } catch (Exception e) {
            return null;
        }
    }

    private double calculateContrastRatio(Color foreground, Color background) {
        double l1 = calculateRelativeLuminance(foreground);
        double l2 = calculateRelativeLuminance(background);
        
        double lighter = Math.max(l1, l2);
        double darker = Math.min(l1, l2);
        
        return (lighter + 0.05) / (darker + 0.05);
    }

    private double calculateRelativeLuminance(Color color) {
        double[] rgb = new double[] {
            color.getRed() / 255.0,
            color.getGreen() / 255.0,
            color.getBlue() / 255.0
        };

        for (int i = 0; i < 3; i++) {
            if (rgb[i] <= 0.03928) {
                rgb[i] = rgb[i] / 12.92;
            } else {
                rgb[i] = Math.pow((rgb[i] + 0.055) / 1.055, 2.4);
            }
        }

        return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
    }

    private void saveAnalysisResults(List<Map<String, Object>> issues, String outputPath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        
        // JSON 형식으로 저장
        ObjectNode root = mapper.createObjectNode();
        root.put("totalIssues", issues.size());
        root.put("analyzedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        root.putPOJO("issues", issues);
        
        // JSON 파일로 저장
        String jsonFileName = outputPath + "/accessibility-report.json";
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(jsonFileName), root);

        // HTML 리포트 생성
        generateHtmlReport(issues, outputPath);
    }

    private void generateHtmlReport(List<Map<String, Object>> issues, String outputPath) throws Exception {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
            .append("<html lang=\"ko\">\n")
            .append("<head>\n")
            .append("    <meta charset=\"UTF-8\">\n")
            .append("    <title>접근성 분석 리포트</title>\n")
            .append("    <style>\n")
            .append("        body { font-family: Arial, sans-serif; margin: 20px; }\n")
            .append("        .issue { border: 1px solid #ddd; margin: 10px 0; padding: 15px; border-radius: 5px; }\n")
            .append("        .issue-type { font-weight: bold; color: #d32f2f; }\n")
            .append("        .path { color: #666; font-size: 0.9em; }\n")
            .append("        .recommendation { background: #fff3e0; padding: 10px; margin: 10px 0; }\n")
            .append("        .color-sample { display: inline-block; width: 20px; height: 20px; margin: 0 5px; vertical-align: middle; }\n")
            .append("    </style>\n")
            .append("</head>\n")
            .append("<body>\n")
            .append("    <h1>접근성 분석 리포트</h1>\n")
            .append("    <p>분석 시간: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("</p>\n")
            .append("    <p>총 발견된 문제: ").append(issues.size()).append("개</p>\n");

        for (Map<String, Object> issue : issues) {
            html.append("    <div class=\"issue\">\n")
                .append("        <div class=\"issue-type\">").append(issue.get("type")).append("</div>\n")
                .append("        <div class=\"path\">경로: ").append(issue.get("path")).append("</div>\n")
                .append("        <p>텍스트: ").append(issue.get("text")).append("</p>\n");

            if (issue.get("type").equals("ColorContrast")) {
                html.append("        <p>대비율: ").append(issue.get("contrastRatio")).append("</p>\n")
                    .append("        <p>전경색: <span class=\"color-sample\" style=\"background: ")
                    .append(issue.get("foregroundColor")).append("\"></span>").append(issue.get("foregroundColor")).append("</p>\n")
                    .append("        <p>배경색: <span class=\"color-sample\" style=\"background: ")
                    .append(issue.get("backgroundColor")).append("\"></span>").append(issue.get("backgroundColor")).append("</p>\n");
            } else if (issue.get("type").equals("FontSize")) {
                html.append("        <p>폰트 크기: ").append(issue.get("value")).append("</p>\n");
            }

            html.append("        <div class=\"recommendation\">권장사항: ").append(issue.get("recommendation")).append("</div>\n")
                .append("        <p>WCAG 기준: ").append(issue.get("wcagCriteria")).append("</p>\n")
                .append("    </div>\n");
        }

        html.append("</body>\n</html>");

        // HTML 파일로 저장
        String htmlFileName = outputPath + "/accessibility-report.html";
        java.nio.file.Files.write(new File(htmlFileName).toPath(), html.toString().getBytes());
    }
} 