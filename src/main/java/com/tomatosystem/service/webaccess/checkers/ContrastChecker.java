package com.tomatosystem.service.webaccess.checkers;

import com.fasterxml.jackson.databind.JsonNode;
import com.tomatosystem.service.webaccess.AccessibilityChecker;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContrastChecker implements AccessibilityChecker {
    private static final double WCAG_AA_NORMAL_TEXT_RATIO = 4.5;
    private static final double WCAG_AA_LARGE_TEXT_RATIO = 3.0;
    private static final int MIN_LARGE_TEXT_SIZE = 24;

    @Override
    public void check(JsonNode node, List<Map<String, Object>> issues, String path) {
        if (!"TEXT".equals(node.path("type").asText())) {
            return;
        }

        JsonNode style = node.path("style");
        if (!style.isMissingNode()) {
            JsonNode fills = style.path("fills");
            JsonNode backgroundColor = node.path("backgroundColor");
            if (!fills.isMissingNode() && fills.isArray() && fills.size() > 0) {
                checkColorContrast(fills.get(0), backgroundColor, node, issues, path);
            }
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
            
            boolean isLargeText = fontSize >= MIN_LARGE_TEXT_SIZE || (fontSize >= 18.5 && isBold);
            double requiredRatio = isLargeText ? WCAG_AA_LARGE_TEXT_RATIO : WCAG_AA_NORMAL_TEXT_RATIO;
            
            if (ratio < requiredRatio) {
                Map<String, Object> issue = new HashMap<>();
                issue.put("type", getCheckerId());
                issue.put("path", path);
                issue.put("contrastRatio", String.format("%.2f:1", ratio));
                issue.put("requiredRatio", String.format("%.1f:1", requiredRatio));
                issue.put("text", node.path("characters").asText());
                issue.put("fontSize", fontSize + "px");
                issue.put("isBold", isBold);
                issue.put("foregroundColor", String.format("#%02x%02x%02x", 
                    foreground.getRed(), foreground.getGreen(), foreground.getBlue()));
                issue.put("backgroundColor", String.format("#%02x%02x%02x", 
                    background.getRed(), background.getGreen(), background.getBlue()));
                issue.put("recommendation", String.format(
                    "%s 텍스트의 경우 최소 대비율 %.1f:1이 필요합니다. (현재: %.2f:1)",
                    isLargeText ? "큰" : "일반",
                    requiredRatio,
                    ratio
                ));
                issue.put("wcagCriteria", getWcagCriteria());
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

    @Override
    public String getCheckerId() {
        return "ColorContrast";
    }

    @Override
    public String getCheckerName() {
        return "색상 대비 검사";
    }

    @Override
    public String getWcagCriteria() {
        return "WCAG 1.4.3 Contrast (Minimum)";
    }
} 