package com.tomatosystem.service.webaccess.checkers;

import com.fasterxml.jackson.databind.JsonNode;
import com.tomatosystem.service.webaccess.AccessibilityChecker;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlternativeTextChecker implements AccessibilityChecker {
    
    @Override
    public void check(JsonNode node, List<Map<String, Object>> issues, String path) {
        if ("IMAGE".equals(node.path("type").asText()) || 
            node.path("name").asText().toLowerCase().contains("image") ||
            node.path("name").asText().toLowerCase().contains("img")) {
            
            JsonNode altText = node.path("altText");
            JsonNode description = node.path("description");
            
            if ((altText.isMissingNode() || altText.asText().isEmpty()) &&
                (description.isMissingNode() || description.asText().isEmpty())) {
                
                Map<String, Object> issue = new HashMap<>();
                issue.put("type", getCheckerId());
                issue.put("path", path);
                issue.put("elementName", node.path("name").asText());
                issue.put("elementType", node.path("type").asText());
                issue.put("recommendation", "이미지에 대체 텍스트를 제공해야 합니다. " +
                    "스크린 리더 사용자가 이미지의 내용을 이해할 수 있도록 적절한 설명을 추가하세요.");
                issue.put("wcagCriteria", getWcagCriteria());
                issues.add(issue);
            } else if (!altText.isMissingNode() && !altText.asText().isEmpty()) {
                // 대체 텍스트 품질 검사
                String alt = altText.asText();
                if (alt.toLowerCase().contains("image of") || 
                    alt.toLowerCase().contains("picture of") ||
                    alt.length() < 5) {
                    
                    Map<String, Object> issue = new HashMap<>();
                    issue.put("type", getCheckerId());
                    issue.put("path", path);
                    issue.put("elementName", node.path("name").asText());
                    issue.put("elementType", node.path("type").asText());
                    issue.put("currentAltText", alt);
                    issue.put("recommendation", "대체 텍스트가 불충분합니다. " +
                        "'image of', 'picture of'와 같은 불필요한 설명은 제거하고, " +
                        "이미지의 목적과 내용을 명확하게 설명하세요.");
                    issue.put("wcagCriteria", getWcagCriteria());
                    issues.add(issue);
                }
            }
        }
    }

    @Override
    public String getCheckerId() {
        return "AlternativeText";
    }

    @Override
    public String getCheckerName() {
        return "대체 텍스트 검사";
    }

    @Override
    public String getWcagCriteria() {
        return "WCAG 1.1.1 Non-text Content";
    }
} 