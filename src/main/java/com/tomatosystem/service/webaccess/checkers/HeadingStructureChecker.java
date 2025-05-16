package com.tomatosystem.service.webaccess.checkers;

import com.fasterxml.jackson.databind.JsonNode;
import com.tomatosystem.service.webaccess.AccessibilityChecker;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class HeadingStructureChecker implements AccessibilityChecker {
    
    private static class HeadingLevel {
        String path;
        int level;
        String text;
        
        HeadingLevel(String path, int level, String text) {
            this.path = path;
            this.level = level;
            this.text = text;
        }
    }

    @Override
    public void check(JsonNode node, List<Map<String, Object>> issues, String path) {
        if ("TEXT".equals(node.path("type").asText())) {
            checkHeadingStructure(node, issues, path);
        }
        
        // 자식 노드들의 헤딩 레벨 순서도 확인
        JsonNode children = node.path("children");
        if (children.isArray()) {
            Stack<HeadingLevel> headingStack = new Stack<>();
            for (JsonNode child : children) {
                String childPath = path + " > " + child.path("name").asText();
                if ("TEXT".equals(child.path("type").asText())) {
                    checkHeadingHierarchy(child, childPath, headingStack, issues);
                }
            }
        }
    }

    private void checkHeadingStructure(JsonNode node, List<Map<String, Object>> issues, String path) {
        JsonNode style = node.path("style");
        if (!style.isMissingNode()) {
            double fontSize = style.path("fontSize").asDouble(0);
            boolean isBold = style.path("fontWeight").asDouble(400) >= 700;
            String text = node.path("characters").asText();
            
            // 헤딩으로 보이는 텍스트 확인
            if (fontSize > 20 && isBold) {
                String nodeName = node.path("name").asText().toLowerCase();
                if (!nodeName.contains("heading") && !nodeName.contains("title") && 
                    !nodeName.contains("h1") && !nodeName.contains("h2") && 
                    !nodeName.contains("h3") && !nodeName.contains("h4")) {
                    
                    Map<String, Object> issue = new HashMap<>();
                    issue.put("type", getCheckerId());
                    issue.put("path", path);
                    issue.put("text", text);
                    issue.put("fontSize", fontSize + "px");
                    issue.put("isBold", isBold);
                    issue.put("recommendation", "이 텍스트는 시각적으로 헤딩처럼 보이지만 적절한 헤딩 구조가 없습니다. " +
                        "의미적 구조를 위해 적절한 헤딩 레벨(h1-h6)을 지정해야 합니다.");
                    issue.put("wcagCriteria", getWcagCriteria());
                    issues.add(issue);
                }
            }
        }
    }

    private void checkHeadingHierarchy(JsonNode node, String path, Stack<HeadingLevel> headingStack, List<Map<String, Object>> issues) {
        JsonNode style = node.path("style");
        if (!style.isMissingNode()) {
            double fontSize = style.path("fontSize").asDouble(0);
            String text = node.path("characters").asText();
            
            // 헤딩 레벨 추정 (폰트 크기 기반)
            int estimatedLevel = estimateHeadingLevel(fontSize);
            if (estimatedLevel > 0) {
                if (!headingStack.isEmpty()) {
                    HeadingLevel previousHeading = headingStack.peek();
                    // 헤딩 레벨이 한 번에 두 단계 이상 낮아지면 경고
                    if (estimatedLevel > previousHeading.level + 1) {
                        Map<String, Object> issue = new HashMap<>();
                        issue.put("type", getCheckerId());
                        issue.put("path", path);
                        issue.put("text", text);
                        issue.put("currentLevel", estimatedLevel);
                        issue.put("previousLevel", previousHeading.level);
                        issue.put("recommendation", String.format(
                            "헤딩 레벨이 %d에서 %d로 건너뛰었습니다. 헤딩 구조는 순차적이어야 합니다.",
                            previousHeading.level, estimatedLevel));
                        issue.put("wcagCriteria", getWcagCriteria());
                        issues.add(issue);
                    }
                }
                headingStack.push(new HeadingLevel(path, estimatedLevel, text));
            }
        }
    }

    private int estimateHeadingLevel(double fontSize) {
        if (fontSize >= 32) return 1;
        if (fontSize >= 28) return 2;
        if (fontSize >= 24) return 3;
        if (fontSize >= 20) return 4;
        if (fontSize >= 16) return 5;
        return 0; // 헤딩이 아님
    }

    @Override
    public String getCheckerId() {
        return "HeadingStructure";
    }

    @Override
    public String getCheckerName() {
        return "헤딩 구조 검사";
    }

    @Override
    public String getWcagCriteria() {
        return "WCAG 1.3.1 Info and Relationships";
    }
} 