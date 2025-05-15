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
import java.util.Random;
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
                added.add(id);
            } else if (oldNode != null && newNode == null) {
                removed.add(id);
            } else if (oldNode != null && newNode != null) {
                if (isNodeActuallyModified(oldNode, newNode)) {
                    modified.add(id);
                }
            }
        }

        // 콘솔에 출력
        printDiffSummary(added, removed, modified);
        printDetailedDiff("추가된 항목", added, newMap);
        printDetailedDiff("삭제된 항목", removed, oldMap);
        printModifiedDiff(modified, oldMap, newMap);

        // 파일로 저장
        saveDiffResultToFile(added, removed, modified, oldMap, newMap);
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
        if (colorNode == null || colorNode.isMissingNode()) {
            return "";
        }

        try {
            // Figma의 색상값은 0~1 사이의 실수로 표현됨
            double r = colorNode.path("r").asDouble();
            double g = colorNode.path("g").asDouble();
            double b = colorNode.path("b").asDouble();

            // 알파값이 있는 경우 처리
            double a = colorNode.has("a") ? colorNode.path("a").asDouble() : 1.0;

            // 0~1 사이의 값을 0~255 사이의 정수로 변환
            int rInt = Math.min(255, Math.max(0, (int)(r * 255 + 0.5)));
            int gInt = Math.min(255, Math.max(0, (int)(g * 255 + 0.5)));
            int bInt = Math.min(255, Math.max(0, (int)(b * 255 + 0.5)));

            // 알파값이 1이 아닌 경우 RGBA 형식으로 반환
            if (a < 1.0) {
                int aInt = Math.min(255, Math.max(0, (int)(a * 255 + 0.5)));
                return String.format("#%02X%02X%02X%02X", rInt, gInt, bInt, aInt);
            }

            return String.format("#%02X%02X%02X", rInt, gInt, bInt);
        } catch (Exception e) {
            System.err.println("색상 변환 중 오류 발생: " + e.getMessage());
            return "";
        }
    }

    private void saveDiffResultToFile(List<String> added, List<String> removed, List<String> modified,
                                    Map<String, JsonNode> oldMap, Map<String, JsonNode> newMap) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String today = dateFormat.format(new Date());
            
            // 디렉토리 생성
            File directory = new File("C:\\Users\\LCM\\git\\Converter-Figma\\clx-src\\result\\" + today);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 난수 생성 (100~999)
            Random random = new Random();
            int randomNum = random.nextInt(900) + 100;

            // 파일 생성
            String fileName = String.format("result_%s_%d.txt", today, randomNum);
            File file = new File(directory, fileName);
            StringBuilder content = new StringBuilder();

            // 요약 정보 작성
            content.append("📌 비교 결과 요약:\n");
            content.append(" - 추가된 항목 수 = ").append(added.size()).append("\n");
            content.append(" - 삭제된 항목 수 = ").append(removed.size()).append("\n");
            content.append(" - 수정된 항목 수 = ").append(getActualModifiedCount(modified, oldMap, newMap)).append("\n\n");

            // 추가된 항목
            if (!added.isEmpty()) {
                content.append("📌 추가된 항목:\n");
                for (String id : added) {
                    JsonNode node = newMap.get(id);
                    String type = node.path("type").asText();
                    String name = node.path("name").asText();
                    if (!type.isEmpty()) {
                        content.append("+ Type: ").append(type);
                        if (!name.isEmpty()) {
                            content.append(" Name: ").append(name);
                        }
                        // 좌표 정보 추가
                        appendPositionInfo(content, node);
                        content.append("\n");
                    }
                }
                content.append("\n");
            }

            // 삭제된 항목
            if (!removed.isEmpty()) {
                content.append("📌 삭제된 항목:\n");
                for (String id : removed) {
                    JsonNode node = oldMap.get(id);
                    String type = node.path("type").asText();
                    String name = node.path("name").asText();
                    if (!type.isEmpty()) {
                        content.append("- Type: ").append(type);
                        if (!name.isEmpty()) {
                            content.append(" Name: ").append(name);
                        }
                        // 좌표 정보 추가
                        appendPositionInfo(content, node);
                        content.append("\n");
                    }
                }
                content.append("\n");
            }

            // 수정된 항목
            if (!modified.isEmpty()) {
                content.append("📌 수정된 항목:\n");
                for (String id : modified) {
                    JsonNode oldNode = oldMap.get(id);
                    JsonNode newNode = newMap.get(id);
                    
                    Map<String, String> changes = findActualChanges(oldNode, newNode);
                    if (!changes.isEmpty()) {
                        content.append("* Type: ").append(oldNode.path("type").asText());
                        String name = oldNode.path("name").asText();
                        if (!name.isEmpty()) {
                            content.append(" Name: ").append(name);
                        }
                        content.append("\n");
                        
                        // 변경된 속성들 표시
                        for (Map.Entry<String, String> change : changes.entrySet()) {
                            String key = change.getKey();
                            String value = change.getValue();
                            
                            // 좌표/크기 관련 변경사항은 특별히 처리
                            if (key.equals("position")) {
                                content.append("  - 위치 변경: ").append(value).append("\n");
                            } else if (key.equals("size")) {
                                content.append("  - 크기 변경: ").append(value).append("\n");
                            } else if (key.equals("rotation")) {
                                content.append("  - 회전 변경: ").append(value).append("°\n");
                            } else {
                                content.append("  - ").append(key).append(": ").append(value).append("\n");
                            }
                        }
                        
                        // 스타일 변경 정보 추가
                        appendStyleChanges(content, oldNode, newNode);
                        content.append("\n");
                    }
                }
            }

            // 파일 쓰기
            java.nio.file.Files.write(file.toPath(), content.toString().getBytes());
            System.out.println("\n결과가 다음 파일에 저장되었습니다: " + file.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("파일 저장 중 오류 발생: " + e.getMessage());
        }
    }

    private void appendPositionInfo(StringBuilder content, JsonNode node) {
        double x = node.path("x").asDouble();
        double y = node.path("y").asDouble();
        if (!node.path("x").isMissingNode() && !node.path("y").isMissingNode()) {
            content.append(" (x: ").append(String.format("%.1f", x))
                  .append(", y: ").append(String.format("%.1f", y)).append(")");
        }
    }

    private Map<String, String> findActualChanges(JsonNode oldNode, JsonNode newNode) {
        Map<String, String> changes = new HashMap<>();
        Set<String> skipFields = Set.of("children", "id", "key", "VARIABLE_ALIAS");
        
        // 위치 변경 확인
        double oldX = oldNode.path("x").asDouble();
        double oldY = oldNode.path("y").asDouble();
        double newX = newNode.path("x").asDouble();
        double newY = newNode.path("y").asDouble();
        
        if (!oldNode.path("x").isMissingNode() && !oldNode.path("y").isMissingNode() &&
            !newNode.path("x").isMissingNode() && !newNode.path("y").isMissingNode() &&
            (oldX != newX || oldY != newY)) {
            changes.put("position", String.format("(%.1f, %.1f) → (%.1f, %.1f)", oldX, oldY, newX, newY));
        }

        // 크기 변경 확인
        double oldWidth = oldNode.path("width").asDouble();
        double oldHeight = oldNode.path("height").asDouble();
        double newWidth = newNode.path("width").asDouble();
        double newHeight = newNode.path("height").asDouble();
        
        if (!oldNode.path("width").isMissingNode() && !oldNode.path("height").isMissingNode() &&
            !newNode.path("width").isMissingNode() && !newNode.path("height").isMissingNode() &&
            (oldWidth != newWidth || oldHeight != newHeight)) {
            changes.put("size", String.format("%.1f x %.1f → %.1f x %.1f", oldWidth, oldHeight, newWidth, newHeight));
        }

        // 회전 변경 확인
        if (!oldNode.path("rotation").isMissingNode() && !newNode.path("rotation").isMissingNode() &&
            oldNode.path("rotation").asDouble() != newNode.path("rotation").asDouble()) {
            changes.put("rotation", String.format("%.1f → %.1f", 
                oldNode.path("rotation").asDouble(),
                newNode.path("rotation").asDouble()));
        }

        // fills 변경 확인
        if (!oldNode.path("fills").equals(newNode.path("fills"))) {
            String colorChange = getColorChangeSummary(oldNode.path("fills"), newNode.path("fills"));
            if (!colorChange.isEmpty()) {
                changes.put("배경색", colorChange);
            }
        }

        // background 변경 확인
        if (!oldNode.path("background").equals(newNode.path("background"))) {
            String colorChange = getColorChangeSummary(oldNode.path("background"), newNode.path("background"));
            if (!colorChange.isEmpty()) {
                changes.put("배경", colorChange);
            }
        }

        return changes;
    }

    private String getColorChangeSummary(JsonNode oldColors, JsonNode newColors) {
        if (oldColors.size() > 0 && newColors.size() > 0) {
            JsonNode oldColor = oldColors.get(0).path("color");
            JsonNode newColor = newColors.get(0).path("color");
            if (!oldColor.isMissingNode() && !newColor.isMissingNode()) {
                return convertToHexColor(oldColor) + " → " + convertToHexColor(newColor);
            }
        }
        return "";
    }

    private void appendStyleChanges(StringBuilder content, JsonNode oldNode, JsonNode newNode) {
        // fills 변경 확인
        JsonNode oldFills = oldNode.path("fills");
        JsonNode newFills = newNode.path("fills");
        if (!oldFills.equals(newFills) && oldFills.size() > 0 && newFills.size() > 0) {
            JsonNode oldColor = oldFills.get(0).path("color");
            JsonNode newColor = newFills.get(0).path("color");
            if (!oldColor.isMissingNode() && !newColor.isMissingNode()) {
                content.append("    - 배경색 변경: ")
                      .append(convertToHexColor(oldColor))
                      .append(" → ")
                      .append(convertToHexColor(newColor))
                      .append("\n");
            }
        }

        // background 변경 확인
        JsonNode oldBackground = oldNode.path("background");
        JsonNode newBackground = newNode.path("background");
        if (!oldBackground.equals(newBackground) && oldBackground.size() > 0 && newBackground.size() > 0) {
            JsonNode oldColor = oldBackground.get(0).path("color");
            JsonNode newColor = newBackground.get(0).path("color");
            if (!oldColor.isMissingNode() && !newColor.isMissingNode()) {
                content.append("    - 배경 변경: ")
                      .append(convertToHexColor(oldColor))
                      .append(" → ")
                      .append(convertToHexColor(newColor))
                      .append("\n");
            }
        }

        // 추가 스타일 속성 비교
        compareStyleProperties(content, oldNode.path("style"), newNode.path("style"));
    }

    private void compareStyleProperties(StringBuilder content, JsonNode oldStyle, JsonNode newStyle) {
        if (!oldStyle.isMissingNode() && !newStyle.isMissingNode()) {
            String[] styleProps = {"fontFamily", "fontSize", "fontWeight", "textAlignHorizontal", "textAlignVertical"};
            for (String prop : styleProps) {
                JsonNode oldVal = oldStyle.path(prop);
                JsonNode newVal = newStyle.path(prop);
                if (!oldVal.isMissingNode() && !newVal.isMissingNode() && !oldVal.equals(newVal)) {
                    content.append("    - ").append(prop).append(" 변경: ")
                          .append(oldVal.asText())
                          .append(" → ")
                          .append(newVal.asText())
                          .append("\n");
                }
            }
        }
    }

    private int getActualModifiedCount(List<String> modified, Map<String, JsonNode> oldMap, Map<String, JsonNode> newMap) {
        int count = 0;
        for (String id : modified) {
            JsonNode oldNode = oldMap.get(id);
            JsonNode newNode = newMap.get(id);
            if (!findActualChanges(oldNode, newNode).isEmpty()) {
                count++;
            }
        }
        return count;
    }
}