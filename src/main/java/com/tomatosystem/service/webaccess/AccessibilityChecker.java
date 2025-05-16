package com.tomatosystem.service.webaccess;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

public interface AccessibilityChecker {
    void check(JsonNode node, List<Map<String, Object>> issues, String path);
    String getCheckerId();
    String getCheckerName();
    String getWcagCriteria();
} 