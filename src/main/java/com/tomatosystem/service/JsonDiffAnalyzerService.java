package com.tomatosystem.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class JsonDiffAnalyzerService {

    public void analyzeJsonData(Map<String, Object> rawData, Map<String, Object> uploadedJsonData) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode oldJson = objectMapper.convertValue(uploadedJsonData, JsonNode.class); // 업로드된 JSON을 기준
        JsonNode newJson = objectMapper.convertValue(rawData, JsonNode.class); // 최신 Figma JSON을 비교 대상으로 처리

        performJsonDiffAnalysis(oldJson, newJson);
    }

    private void performJsonDiffAnalysis(JsonNode oldJson, JsonNode newJson) {
        Map<String, JsonNode> oldMap = new HashMap<>();
        Map<String, JsonNode> newMap = new HashMap<>();

        flattenJsonById(oldJson, oldMap); // 업로드된 JSON을 기준으로 처리
        flattenJsonById(newJson, newMap); // 최신 JSON을 비교 대상으로 처리

        Set<String> allIds = new HashSet<>();
        allIds.addAll(oldMap.keySet());
        allIds.addAll(newMap.keySet());

        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        List<String> modified = new ArrayList<>();

        for (String id : allIds) {
            JsonNode oldNode = oldMap.get(id);
            JsonNode newNode = newMap.get(id);

            if (oldNode == null) {
                added.add(id); // 새로 추가된 항목
            } else if (newNode == null) {
                removed.add(id); // 삭제된 항목
            } else if (!oldNode.equals(newNode)) {
                modified.add(id); // 수정된 항목
            }
        }

        printDiffSummary(added, removed, modified);
        printDetailedDiff("추가된 항목", added, newMap);
        printDetailedDiff("삭제된 항목", removed, oldMap);
        printModifiedDiff(modified, oldMap, newMap);
    }

    private void flattenJsonById(JsonNode node, Map<String, JsonNode> result) {
        if (node.isObject()) {
            if (node.has("id")) {
                result.put(node.get("id").asText(), node); // id를 기준으로 Map에 저장
            }
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> field = it.next();
                flattenJsonById(field.getValue(), result); // 재귀 호출
            }
            // 'children' 필드가 있는 경우 이를 재귀적으로 처리
            if (node.has("children")) {
                for (JsonNode child : node.get("children")) {
                    flattenJsonById(child, result);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                flattenJsonById(item, result); // 배열 처리
            }
        }
    }

    private void printDiffSummary(List<String> added, List<String> removed, List<String> modified) {
        System.out.println("📌 비교 결과 요약:");
        System.out.println(" - 추가된 항목 수 = " + added.size());
        System.out.println(" - 삭제된 항목 수 = " + removed.size());
        System.out.println(" - 수정된 항목 수 = " + modified.size());
    }

    private void printDetailedDiff(String title, List<String> ids, Map<String, JsonNode> nodeMap) {
        System.out.println("\n📌 " + title + ":");
        for (String id : ids) {
            JsonNode node = nodeMap.get(id);
            printNodeSummary("+", node);
            printStyleInfo(node, node); // 스타일을 추가로 출력
        }
    }

    private void printModifiedDiff(List<String> modified, Map<String, JsonNode> oldMap, Map<String, JsonNode> newMap) {
        System.out.println("\n📌 수정된 항목:");
        for (String id : modified) {
            JsonNode oldNode = oldMap.get(id);
            JsonNode newNode = newMap.get(id);
            printNodeSummary("*", oldNode);
            System.out.println("  → 변경 후: " + newNode.path("type").asText() + " Name: " + newNode.path("name").asText());
            printStyleInfo(oldNode, newNode);
        }
    }

    private void printNodeSummary(String prefix, JsonNode node) {
        String type = node.path("type").asText();
        String name = node.path("name").asText();
        System.out.println(prefix + " Type: " + type + " Name: " + name);
    }

    private void printStyleInfo(JsonNode oldNode, JsonNode newNode) {
        JsonNode oldStyleNode = oldNode.path("style");
        JsonNode newStyleNode = newNode.path("style");

        // 스타일이 달라진 경우
        if (!oldStyleNode.equals(newStyleNode)) {
            System.out.println("  → 스타일 변경:");
            oldStyleNode.fieldNames().forEachRemaining(field -> {
                JsonNode oldVal = oldStyleNode.get(field);
                JsonNode newVal = newStyleNode.get(field);
                if (newVal != null && !oldVal.equals(newVal)) {
                    System.out.println("    - " + field + " 변경: " + oldVal.asText() + " → " + newVal.asText());
                }
            });
        }

        // 스타일 외부의 속성들 (배경색, 선 색 등) 비교
        printAdditionalStyleInfo(oldNode, newNode);

        // styleOverrideTable 비교
        JsonNode oldStyleOverrideTableNode = oldNode.path("styleOverrideTable");
        JsonNode newStyleOverrideTableNode = newNode.path("styleOverrideTable");

        if (!oldStyleOverrideTableNode.equals(newStyleOverrideTableNode)) {
            System.out.println("    - 스타일 테이블 변경:");
            Iterator<String> fieldNames = oldStyleOverrideTableNode.fieldNames();
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                JsonNode oldOverrideStyle = oldStyleOverrideTableNode.get(key);
                JsonNode newOverrideStyle = newStyleOverrideTableNode.get(key);

                if (!oldOverrideStyle.equals(newOverrideStyle)) {
                    System.out.println("      - " + key + " 변경:");
                    compareStyleProperties(oldOverrideStyle, newOverrideStyle);
                }
            }
        }

        // characterStyleOverrides 비교
        JsonNode oldCharStyleOverrides = oldNode.path("characterStyleOverrides");
        JsonNode newCharStyleOverrides = newNode.path("characterStyleOverrides");

        if (!oldCharStyleOverrides.equals(newCharStyleOverrides)) {
            System.out.println("    - 문자 스타일 변경:");
            for (int i = 0; i < oldCharStyleOverrides.size(); i++) {
                JsonNode oldCharStyle = oldCharStyleOverrides.get(i);
                JsonNode newCharStyle = newCharStyleOverrides.get(i);
                compareStyleProperties(oldCharStyle, newCharStyle);
            }
        }
    }

    private void printAdditionalStyleInfo(JsonNode oldNode, JsonNode newNode) {
        // 배경색 비교
        JsonNode oldFills = oldNode.path("fills");
        JsonNode newFills = newNode.path("fills");
        if (!oldFills.equals(newFills)) {
            System.out.println("    - 배경색(fills) 변경:");
            compareFills(oldFills, newFills);
        }

        // 테두리 색 비교 (strokes)
        JsonNode oldStrokes = oldNode.path("strokes");
        JsonNode newStrokes = newNode.path("strokes");
        if (!oldStrokes.equals(newStrokes)) {
            System.out.println("    - 테두리 색(strokes) 변경:");
            compareStrokes(oldStrokes, newStrokes);
        }

        // 배경 비교 (background)
        JsonNode oldBackground = oldNode.path("background");
        JsonNode newBackground = newNode.path("background");
        if (!oldBackground.equals(newBackground)) {
            System.out.println("    - 배경(background) 변경:");
            compareBackground(oldBackground, newBackground);
        }
    }

    private void compareFills(JsonNode oldFills, JsonNode newFills) {
        if (oldFills.size() == newFills.size()) {
            for (int i = 0; i < oldFills.size(); i++) {
                JsonNode oldFill = oldFills.get(i);
                JsonNode newFill = newFills.get(i);
                if (!oldFill.equals(newFill)) {
                    System.out.println("      - " + convertToHexColor(oldFill.path("color")) + " → " + convertToHexColor(newFill.path("color")));
                }
            }
        } else {
            System.out.println("      - 필드의 개수 차이로 비교가 필요합니다.");
        }
    }

    private void compareStrokes(JsonNode oldStrokes, JsonNode newStrokes) {
        for (int i = 0; i < oldStrokes.size(); i++) {
            JsonNode oldStroke = oldStrokes.get(i);
            JsonNode newStroke = newStrokes.get(i);
            if (!oldStroke.equals(newStroke)) {
                System.out.println("      - " + convertToHexColor(oldStroke.path("color")) + " → " + convertToHexColor(newStroke.path("color")));
            }
        }
    }

    private void compareBackground(JsonNode oldBackground, JsonNode newBackground) {
        for (int i = 0; i < oldBackground.size(); i++) {
            JsonNode oldBg = oldBackground.get(i);
            JsonNode newBg = newBackground.get(i);
            if (!oldBg.equals(newBg)) {
                System.out.println("      - " + convertToHexColor(oldBg.path("color")) + " → " + convertToHexColor(newBg.path("color")));
            }
        }
    }

    private void compareStyleProperties(JsonNode oldStyle, JsonNode newStyle) {
        // fontFamily 비교
        if (!oldStyle.path("fontFamily").equals(newStyle.path("fontFamily"))) {
            System.out.println("        - fontFamily 변경: " + oldStyle.path("fontFamily").asText() + " → " + newStyle.path("fontFamily").asText());
        }

        // fontSize 비교
        if (!oldStyle.path("fontSize").equals(newStyle.path("fontSize"))) {
            System.out.println("        - fontSize 변경: " + oldStyle.path("fontSize").asText() + " → " + newStyle.path("fontSize").asText());
        }

        // lineHeightPx 비교
        if (!oldStyle.path("lineHeightPx").equals(newStyle.path("lineHeightPx"))) {
            System.out.println("        - lineHeightPx 변경: " + oldStyle.path("lineHeightPx").asText() + " → " + newStyle.path("lineHeightPx").asText());
        }

        // fill 색상 비교
        JsonNode oldFills = oldStyle.path("fills");
        JsonNode newFills = newStyle.path("fills");
        if (!oldFills.equals(newFills)) {
            System.out.println("        - 색상 변경: " + convertToHexColor(oldFills.path("color")) + " → " + convertToHexColor(newFills.path("color")));
        }
    }

    private String convertToHexColor(JsonNode colorNode) {
        int r = colorNode.path("r").asInt();
        int g = colorNode.path("g").asInt();
        int b = colorNode.path("b").asInt();
        return String.format("#%02X%02X%02X", (int)(r * 255), (int)(g * 255), (int)(b * 255));
    }
}