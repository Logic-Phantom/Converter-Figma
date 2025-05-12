package com.tomatosystem.service;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

//    public void analyzeJsonData(Map<String, Object> rawData, Map<String, Object> uploadedJsonData) throws IOException {
//        ObjectMapper objectMapper = new ObjectMapper();
//        JsonNode oldJson = objectMapper.convertValue(uploadedJsonData, JsonNode.class); // 업로드된 JSON을 기준
//        JsonNode newJson = objectMapper.convertValue(rawData, JsonNode.class); // 최신 Figma JSON을 비교 대상으로 처리
//
//        performJsonDiffAnalysis(oldJson, newJson);
//    }
//
//    private void performJsonDiffAnalysis(JsonNode oldJson, JsonNode newJson) {
//        Map<String, JsonNode> oldMap = new HashMap<>();
//        Map<String, JsonNode> newMap = new HashMap<>();
//
//        flattenJsonById(oldJson, oldMap); // 업로드된 JSON을 기준으로 처리
//        flattenJsonById(newJson, newMap); // 최신 JSON을 비교 대상으로 처리
//
//        Set<String> allIds = new HashSet<>();
//        allIds.addAll(oldMap.keySet());
//        allIds.addAll(newMap.keySet());
//
//        List<String> added = new ArrayList<>();
//        List<String> removed = new ArrayList<>();
//        List<String> modified = new ArrayList<>();
//
////        for (String id : allIds) {
////            JsonNode oldNode = oldMap.get(id);
////            JsonNode newNode = newMap.get(id);
////
////            if (oldNode == null) {
////                added.add(id); // 새로 추가된 항목
////            } else if (newNode == null) {
////                removed.add(id); // 삭제된 항목
////            } else if (!oldNode.equals(newNode)) {
////                modified.add(id); // 수정된 항목
////            }
////        }
//        for (String id : allIds) {
//            JsonNode oldNode = oldMap.get(id);
//            JsonNode newNode = newMap.get(id);
//
//            if (oldNode == null) {
//                added.add(id);
//            } else if (newNode == null) {
//                removed.add(id);
//            } else if (isNodeActuallyModified(oldNode, newNode)) {
//                modified.add(id);
//            }
//        }
//
//        printDiffSummary(added, removed, modified);
//        printDetailedDiff("추가된 항목", added, newMap);
//        printDetailedDiff("삭제된 항목", removed, oldMap);
//        printModifiedDiff(modified, oldMap, newMap);
//    }
//
//    private boolean isNodeActuallyModified(JsonNode oldNode, JsonNode newNode) {
//        // 자식 요소는 무시하고 자기 자신만 비교
//        Set<String> skipFields = Set.of("children"); // 하위 노드는 무시
//
//        Iterator<String> fieldNames = oldNode.fieldNames();
//        while (fieldNames.hasNext()) {
//            String field = fieldNames.next();
//            if (skipFields.contains(field)) continue;
//
//            JsonNode oldField = oldNode.get(field);
//            JsonNode newField = newNode.get(field);
//            if (newField == null || !oldField.equals(newField)) {
//                return true;
//            }
//        }
//
//        // newNode에만 있는 필드도 체크
//        Iterator<String> newFieldNames = newNode.fieldNames();
//        while (newFieldNames.hasNext()) {
//            String field = newFieldNames.next();
//            if (skipFields.contains(field)) continue;
//
//            if (!oldNode.has(field)) {
//                return true;
//            }
//        }
//
//        return false;
//    }
//    
//    private void flattenJsonById(JsonNode node, Map<String, JsonNode> result) {
//        if (node.isObject()) {
//            if (node.has("id")) {
//                result.put(node.get("id").asText(), node); // id를 기준으로 Map에 저장
//            }
//            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
//                Map.Entry<String, JsonNode> field = it.next();
//                flattenJsonById(field.getValue(), result); // 재귀 호출
//            }
//            // 'children' 필드가 있는 경우 이를 재귀적으로 처리
//            if (node.has("children")) {
//                for (JsonNode child : node.get("children")) {
//                    flattenJsonById(child, result);
//                }
//            }
//        } else if (node.isArray()) {
//            for (JsonNode item : node) {
//                flattenJsonById(item, result); // 배열 처리
//            }
//        }
//    }
//
//    private void printDiffSummary(List<String> added, List<String> removed, List<String> modified) {
//        System.out.println("📌 비교 결과 요약:");
//        System.out.println(" - 추가된 항목 수 = " + added.size());
//        System.out.println(" - 삭제된 항목 수 = " + removed.size());
//        System.out.println(" - 수정된 항목 수 = " + modified.size());
//    }
//
//    private void printDetailedDiff(String title, List<String> ids, Map<String, JsonNode> nodeMap) {
//        System.out.println("\n📌 " + title + ":");
//        for (String id : ids) {
//            JsonNode node = nodeMap.get(id);
//            printNodeSummary("+", node);
//            printStyleInfo(node, node); // 스타일을 추가로 출력
//        }
//    }
//
//    private void printModifiedDiff(List<String> modified, Map<String, JsonNode> oldMap, Map<String, JsonNode> newMap) {
//        System.out.println("\n📌 수정된 항목:");
//        for (String id : modified) {
//            JsonNode oldNode = oldMap.get(id);
//            JsonNode newNode = newMap.get(id);
//            printNodeSummary("*", oldNode);
//            System.out.println("  → 변경 후: " + newNode.path("type").asText() + " Name: " + newNode.path("name").asText());
//            printStyleInfo(oldNode, newNode);
//        }
//    }
//
//    private void printNodeSummary(String prefix, JsonNode node) {
//        String type = node.path("type").asText();
//        String name = node.path("name").asText();
//        System.out.println(prefix + " Type: " + type + " Name: " + name);
//    }

	//2025-05-12(최신버전과 직전버전 차이)
    public void analyzeJsonData(Map<String, Object> rawData, Map<String, Object> uploadedJsonData) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode oldJson = objectMapper.convertValue(uploadedJsonData, JsonNode.class); // 업로드된 JSON
        JsonNode newJson = objectMapper.convertValue(rawData, JsonNode.class);         // Figma 최신 JSON

        performJsonDiffAnalysis(oldJson, newJson);
    }

    //json 데이터 비교 로직 추가(메타 데이터 lastModified 기준으로) 개발필요
    private FigmaApiService figmaApiService;

    public JsonDiffAnalyzerService(FigmaApiService figmaApiService) {
        this.figmaApiService = figmaApiService;
    }

    public void analyzeJsonDataDiff(Map<String, Object> rawData, Map<String, Object> uploadedJsonData, String fileId) throws IOException, ParseException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode oldJson = objectMapper.convertValue(uploadedJsonData, JsonNode.class); // 업로드된 JSON
        JsonNode newJson = objectMapper.convertValue(rawData, JsonNode.class);         // Figma 최신 JSON

        // Figma에서 마지막 수정 시간을 확인
        long lastModifiedTimeFromFigma = figmaApiService.getLastModifiedTime(fileId);
        
        // 업로드된 데이터에서 lastModified 값을 가져오고, 이를 long으로 변환
        long lastModifiedTimeFromUploadedData = getLastModifiedTimeFromUploadedData(uploadedJsonData);

        if (lastModifiedTimeFromFigma > lastModifiedTimeFromUploadedData) {
            // Figma에서 마지막 수정 시간이 업로드된 데이터보다 최신이면 비교
            performJsonDiffAnalysis(oldJson, newJson);
        } else {
            System.out.println("변경 사항이 없습니다. 최신 데이터와 동일합니다.");
        }
    }


    private long getLastModifiedTimeFromUploadedData(Map<String, Object> uploadedJsonData) throws ParseException {
        // 업로드된 JSON 데이터에서 lastModified 값을 String으로 가져옴
        String lastModifiedStr = (String) uploadedJsonData.get("lastModified");
        
        // 날짜 형식에 맞는 SimpleDateFormat을 사용하여 문자열을 Date로 변환
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Date date = sdf.parse(lastModifiedStr);
        
        // Date 객체에서 타임스탬프를 밀리초 단위로 반환
        return date.getTime();
    }
    
    private void performJsonDiffAnalysis(JsonNode oldJson, JsonNode newJson) {
        Map<String, JsonNode> oldMap = new HashMap<>();
        Map<String, JsonNode> newMap = new HashMap<>();

        Set<String> visitedOldIds = new HashSet<>();
        Set<String> visitedNewIds = new HashSet<>();

        flattenJsonById(oldJson, oldMap, visitedOldIds);
        flattenJsonById(newJson, newMap, visitedNewIds);

        Set<String> allIds = new HashSet<>();
        allIds.addAll(oldMap.keySet());
        allIds.addAll(newMap.keySet());

        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        List<String> modified = new ArrayList<>();

        for (String id : allIds) {
            JsonNode oldNode = oldMap.get(id);
            JsonNode newNode = newMap.get(id);

            if (oldNode == null && newNode != null) {
                // 새로 추가된 항목
                added.add(id);
            } else if (oldNode != null && newNode == null) {
                // 삭제된 항목
                removed.add(id);
            } else if (oldNode != null && newNode != null) {
                // 기존 항목이 수정된 경우만 체크
                if (isNodeActuallyModified(oldNode, newNode)) {
                    modified.add(id);
                }
            }
        }

        printDiffSummary(added, removed, modified);
        printDetailedDiff("추가된 항목", added, newMap);
        printDetailedDiff("삭제된 항목", removed, oldMap);
        printModifiedDiff(modified, oldMap, newMap);
    }

    private void flattenJsonById(JsonNode node, Map<String, JsonNode> result, Set<String> visitedIds) {
        if (node.isObject()) {
            if (node.has("id")) {
                String id = node.get("id").asText();
                // VARIABLE_ALIAS를 가진 노드는 처리하지 않음
                if (node.has("type") && node.get("type").asText().equals("VARIABLE_ALIAS")) {
                    return;  // VARIABLE_ALIAS 항목은 무시
                }
                if (visitedIds.contains(id)) return;
                visitedIds.add(id);
                result.put(id, node);
            }

            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> field = it.next();
                if (!"children".equals(field.getKey())) {
                    flattenJsonById(field.getValue(), result, visitedIds);
                }
            }

            // children 별도 처리
            if (node.has("children")) {
                for (JsonNode child : node.get("children")) {
                    flattenJsonById(child, result, visitedIds);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                flattenJsonById(item, result, visitedIds);
            }
        }
    }

    private boolean isNodeActuallyModified(JsonNode oldNode, JsonNode newNode) {
        Set<String> skipFields = Set.of("children", "VARIABLE_ALIAS"); // "VARIABLE_ALIAS" 필드를 무시
        Iterator<String> fields = oldNode.fieldNames();

        // oldNode와 newNode의 실제 차이점을 체크합니다.
        while (fields.hasNext()) {
            String field = fields.next();
            if (skipFields.contains(field)) continue;

            JsonNode oldVal = oldNode.get(field);
            JsonNode newVal = newNode.get(field);

            // 값이 다르면 수정된 항목으로 판단
            if (newVal == null || !oldVal.equals(newVal)) {
                return true; // 차이가 있으면 수정된 항목으로 판단
            }
        }

        // newNode에만 있는 필드 확인
        fields = newNode.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            if (skipFields.contains(field)) continue;
            if (!oldNode.has(field)) return true; // 새로운 필드가 있으면 수정된 항목으로 판단
        }

        return false;
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
            printStyleInfo(node, node);
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

//    private void printStyleInfo(JsonNode oldNode, JsonNode newNode) {
//        JsonNode oldStyleNode = oldNode.path("style");
//        JsonNode newStyleNode = newNode.path("style");
//
//        // 스타일이 달라진 경우
//        if (!oldStyleNode.equals(newStyleNode)) {
//            System.out.println("  → 스타일 변경:");
//            oldStyleNode.fieldNames().forEachRemaining(field -> {
//                JsonNode oldVal = oldStyleNode.get(field);
//                JsonNode newVal = newStyleNode.get(field);
//                if (newVal != null && !oldVal.equals(newVal)) {
//                    System.out.println("    - " + field + " 변경: " + oldVal.asText() + " → " + newVal.asText());
//                }
//            });
//        }
//
//        // 스타일 외부의 속성들 (배경색, 선 색 등) 비교
//        printAdditionalStyleInfo(oldNode, newNode);
//    }

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
        for (int i = 0; i < oldFills.size(); i++) {
            JsonNode oldFill = oldFills.get(i);
            JsonNode newFill = newFills.get(i);
            if (!oldFill.equals(newFill)) {
                System.out.println("      - " + convertToHexColor(oldFill.path("color")) + " → " + convertToHexColor(newFill.path("color")));
            }
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

    private void printStyleInfo(JsonNode oldNode, JsonNode newNode) {
        JsonNode oldStyleNode = oldNode.path("style");
        JsonNode newStyleNode = newNode.path("style");

        // 스타일이 달라진 경우
        if (!oldStyleNode.equals(newStyleNode)) {
            System.out.println("  → 스타일 변경:");

            // 두 스타일 노드의 필드를 비교
            Iterator<String> fieldNames = oldStyleNode.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                JsonNode oldVal = oldStyleNode.get(field);
                JsonNode newVal = newStyleNode.get(field);

                if (newVal != null && !oldVal.equals(newVal)) {
                    System.out.println("    - " + field + " 변경: " + oldVal.asText() + " → " + newVal.asText());
                }
            }

            // newStyleNode에만 있는 새로운 필드도 체크
            Iterator<String> newFieldNames = newStyleNode.fieldNames();
            while (newFieldNames.hasNext()) {
                String field = newFieldNames.next();
                if (!oldStyleNode.has(field)) {
                    System.out.println("    - " + field + " 추가됨: " + newStyleNode.get(field).asText());
                }
            }
        }

        // 스타일 외부의 속성들 (배경색, 선 색 등) 비교
        printAdditionalStyleInfo(oldNode, newNode);
    }
    
    private String convertToHexColor(JsonNode colorNode) {
        int r = colorNode.path("r").asInt();
        int g = colorNode.path("g").asInt();
        int b = colorNode.path("b").asInt();
        return String.format("#%02X%02X%02X", (int)(r * 255), (int)(g * 255), (int)(b * 255));
    }
}