package com.tomatosystem.service.webaccess.checkers;

import com.fasterxml.jackson.databind.JsonNode;
import com.tomatosystem.service.webaccess.AccessibilityChecker;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class KeyboardAccessibilityChecker implements AccessibilityChecker {
    
    private static final Set<String> INTERACTIVE_ELEMENTS = new HashSet<>(Arrays.asList(
        "button", "link", "input", "select", "textarea", "checkbox", "radio",
        "slider", "dropdown", "menu", "tab", "dialog"
    ));

    @Override
    public void check(JsonNode node, List<Map<String, Object>> issues, String path) {
        String nodeName = node.path("name").asText().toLowerCase();
        String nodeType = node.path("type").asText();
        
        // 상호작용 가능한 요소인지 확인
        boolean isInteractive = INTERACTIVE_ELEMENTS.stream()
            .anyMatch(nodeName::contains) ||
            "INSTANCE".equals(nodeType) ||
            "COMPONENT".equals(nodeType);

        if (isInteractive) {
            checkKeyboardAccess(node, issues, path);
        }
    }

    private void checkKeyboardAccess(JsonNode node, List<Map<String, Object>> issues, String path) {
        // tabIndex 확인
        JsonNode tabIndex = node.path("tabIndex");
        JsonNode role = node.path("role");
        JsonNode actions = node.path("actions");
        
        boolean hasKeyboardSupport = !tabIndex.isMissingNode() && tabIndex.asInt(-1) >= 0;
        boolean hasRole = !role.isMissingNode() && !role.asText().isEmpty();
        boolean hasActions = !actions.isMissingNode() && actions.size() > 0;

        if (!hasKeyboardSupport || !hasRole) {
            Map<String, Object> issue = new HashMap<>();
            issue.put("type", getCheckerId());
            issue.put("path", path);
            issue.put("elementName", node.path("name").asText());
            issue.put("elementType", node.path("type").asText());
            
            StringBuilder recommendation = new StringBuilder("키보드 접근성 개선이 필요합니다:");
            if (!hasKeyboardSupport) {
                recommendation.append("\n- tabIndex를 설정하여 키보드 포커스가 가능하도록 해야 합니다.");
            }
            if (!hasRole) {
                recommendation.append("\n- 적절한 ARIA role을 지정하여 요소의 역할을 명확히 해야 합니다.");
            }
            if (!hasActions) {
                recommendation.append("\n- 키보드 이벤트 처리(Enter, Space 등)를 추가해야 합니다.");
            }

            issue.put("recommendation", recommendation.toString());
            issue.put("wcagCriteria", getWcagCriteria());
            
            // 추가 정보
            issue.put("hasTabIndex", hasKeyboardSupport);
            issue.put("hasRole", hasRole);
            issue.put("hasActions", hasActions);
            
            issues.add(issue);
        }
    }

    @Override
    public String getCheckerId() {
        return "KeyboardAccessibility";
    }

    @Override
    public String getCheckerName() {
        return "키보드 접근성 검사";
    }

    @Override
    public String getWcagCriteria() {
        return "WCAG 2.1.1 Keyboard";
    }
} 