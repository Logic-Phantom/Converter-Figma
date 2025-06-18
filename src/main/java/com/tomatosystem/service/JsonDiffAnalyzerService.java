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

    private final FigmaApiService figmaApiService;
    private final ExcelDiffReportService excelDiffReportService;

    public JsonDiffAnalyzerService(FigmaApiService figmaApiService, ExcelDiffReportService excelDiffReportService) {
        this.figmaApiService = figmaApiService;
        this.excelDiffReportService = excelDiffReportService;
    }

	//2025-05-12(최신버전과 직전버전 차이)
    public void analyzeJsonData(Map<String, Object> rawData, Map<String, Object> uploadedJsonData) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode oldJson = objectMapper.convertValue(uploadedJsonData, JsonNode.class); // 업로드된 JSON
        JsonNode newJson = objectMapper.convertValue(rawData, JsonNode.class);         // Figma 최신 JSON

        performJsonDiffAnalysis(oldJson, newJson);
    }

    //json 데이터 비교 로직 추가(메타 데이터 lastModified 기준으로) 개발필요

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
        try {
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

            // 난수 한 번만 생성 (100-999)
            Random random = new Random();
            int randomNum = random.nextInt(900) + 100;

            // 콘솔에 출력
            printDiffSummary(added, removed, modified);
            printDetailedDiff("추가된 항목", added, newMap);
            printDetailedDiff("삭제된 항목", removed, oldMap);
            printModifiedDiff(modified, oldMap, newMap);

            // 텍스트 파일로 저장
            saveDiffResultToFile(added, removed, modified, oldMap, newMap, randomNum);
            
            // Excel 파일로 저장
            try {
                String pageName = extractPageName(oldJson);
                System.out.println("\nExcel 파일 생성을 시작합니다...");
                excelDiffReportService.generateExcelReport(added, removed, modified, oldMap, newMap, pageName, randomNum);
                System.out.println("Excel 파일 생성이 완료되었습니다.");
            } catch (Exception e) {
                System.err.println("Excel 파일 생성 중 오류 발생: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("\n분석이 완료되었습니다.");
        } catch (Exception e) {
            System.err.println("분석 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String extractPageName(JsonNode json) {
        // document 노드 찾기
        JsonNode document = json.path("document");
        if (!document.isMissingNode()) {
            // children 배열에서 첫 번째 페이지 찾기
            JsonNode children = document.path("children");
            if (children.isArray() && children.size() > 0) {
                JsonNode firstPage = children.get(0);
                if (firstPage.has("name")) {
                    return firstPage.get("name").asText("상세 변경 내역");
                }
            }
        }
        
        // 페이지 이름을 찾지 못한 경우 기본값 반환
        return "상세 변경 내역";
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
            
            // Check for position changes
            double oldX = oldNode.path("x").asDouble();
            double oldY = oldNode.path("y").asDouble();
            double newX = newNode.path("x").asDouble();
            double newY = newNode.path("y").asDouble();
            
            if (!oldNode.path("x").isMissingNode() && !oldNode.path("y").isMissingNode() &&
                !newNode.path("x").isMissingNode() && !newNode.path("y").isMissingNode() &&
                (oldX != newX || oldY != newY)) {
                System.out.println("  - 위치 변경: (" + String.format("%.1f", oldX) + ", " + String.format("%.1f", oldY) + 
                                 ") → (" + String.format("%.1f", newX) + ", " + String.format("%.1f", newY) + ")");
            }
            
            // Check for size changes
            checkAndPrintSizeChanges(oldNode, newNode);
            
            printStyleInfo(oldNode, newNode);
        }
    }

    private void printNodeSummary(String prefix, JsonNode node) {
        String type = node.path("type").asText();
        String name = node.path("name").asText();
        System.out.println(prefix + " Type: " + type + " Name: " + name);
    }

    private void checkAndPrintSizeChanges(JsonNode oldNode, JsonNode newNode) {
        // absoluteBoundingBox를 사용한 크기 변경 확인
        JsonNode oldBox = oldNode.path("absoluteBoundingBox");
        JsonNode newBox = newNode.path("absoluteBoundingBox");
        
        if (!oldBox.isMissingNode() && !newBox.isMissingNode()) {
            double oldWidth = oldBox.path("width").asDouble();
            double oldHeight = oldBox.path("height").asDouble();
            double newWidth = newBox.path("width").asDouble();
            double newHeight = newBox.path("height").asDouble();
            
            boolean widthChanged = oldWidth != newWidth;
            boolean heightChanged = oldHeight != newHeight;
            
            if (widthChanged || heightChanged) {
                System.out.println("  - 크기 변경:");
                if (widthChanged) {
                    System.out.println("    너비: " + String.format("%.1f", oldWidth) + " → " + String.format("%.1f", newWidth));
                }
                if (heightChanged) {
                    System.out.println("    높이: " + String.format("%.1f", oldHeight) + " → " + String.format("%.1f", newHeight));
                }
            }
        }
        
        // size 속성을 사용한 크기 변경 확인 (일부 컴포넌트에서 사용)
        if (oldNode.has("size") && newNode.has("size")) {
            JsonNode oldSize = oldNode.path("size");
            JsonNode newSize = newNode.path("size");
            
            if (!oldSize.path("width").isMissingNode() && !newSize.path("width").isMissingNode() &&
                !oldSize.path("height").isMissingNode() && !newSize.path("height").isMissingNode()) {
                
                double oldWidth = oldSize.path("width").asDouble();
                double oldHeight = oldSize.path("height").asDouble();
                double newWidth = newSize.path("width").asDouble();
                double newHeight = newSize.path("height").asDouble();
                
                boolean widthChanged = oldWidth != newWidth;
                boolean heightChanged = oldHeight != newHeight;
                
                if (widthChanged || heightChanged) {
                    System.out.println("  - 크기 변경 (size 속성):");
                    if (widthChanged) {
                        System.out.println("    너비: " + String.format("%.1f", oldWidth) + " → " + String.format("%.1f", newWidth));
                    }
                    if (heightChanged) {
                        System.out.println("    높이: " + String.format("%.1f", oldHeight) + " → " + String.format("%.1f", newHeight));
                    }
                }
            }
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
                                    Map<String, JsonNode> oldMap, Map<String, JsonNode> newMap, int randomNum) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String today = dateFormat.format(new Date());
            
            File directory = new File("C:\\Users\\LCM\\git\\Converter-Figma\\clx-src\\result\\txt\\" + today);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fileName = String.format("result_%s_%d.txt", today, randomNum);
            File file = new File(directory, fileName);
            StringBuilder content = new StringBuilder();

            // 요약 정보
            content.append("📌 비교 결과 요약:\n");
            content.append(" - 추가된 항목 수 = ").append(added.size()).append("\n");
            content.append(" - 삭제된 항목 수 = ").append(removed.size()).append("\n");
            content.append(" - 수정된 항목 수 = ").append(getActualModifiedCount(modified, oldMap, newMap)).append("\n\n");

            // 수정된 항목
            if (!modified.isEmpty()) {
                content.append("📌 수정된 항목:\n");
                for (String id : modified) {
                    JsonNode oldNode = oldMap.get(id);
                    JsonNode newNode = newMap.get(id);
                    Map<String, String> changes = findActualChanges(oldNode, newNode);
                    
                    if (!changes.isEmpty()) {
                        // 기본 정보 출력
                        content.append("* Type: ").append(getComponentType(oldNode))
                              .append(" Name: ").append(oldNode.path("name").asText());
                        
                        String value = getComponentValue(oldNode);
                        if (value != null) {
                            content.append(" Value: ").append(value);
                        }
                        content.append("\n");

                        // 변경 사항 출력
                        if (changes.containsKey("position")) {
                            content.append("  - 위치 변경: ").append(changes.get("position")).append("\n");
                        }
                        if (changes.containsKey("크기")) {
                            content.append("  - 크기 변경: ").append(changes.get("크기")).append("\n");
                        }
                        if (changes.containsKey("배경색")) {
                            content.append("  - 배경색: ").append(changes.get("배경색")).append("\n");
                        }
                        if (changes.containsKey("배경")) {
                            content.append("  - 배경: ").append(changes.get("배경")).append("\n");
                        }
                        content.append("\n");
                    }
                }
            }

            java.nio.file.Files.write(file.toPath(), content.toString().getBytes());
            System.out.println("\n결과가 다음 파일에 저장되었습니다: " + file.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("파일 저장 중 오류 발생: " + e.getMessage());
        }
    }

    private void appendNodeInfo(StringBuilder content, String prefix, JsonNode node) {
        String type = getComponentType(node);
        String name = node.path("name").asText();
        
        content.append(prefix).append("Type: ").append(type);
        if (!name.isEmpty()) {
            content.append(" Name: ").append(name);
        }

        // 컴포넌트 값 추가
        if ("INSTANCE".equals(node.path("type").asText()) || "COMPONENT".equals(node.path("type").asText())) {
            String componentValue = getComponentValue(node);
            if (componentValue != null) {
                content.append(" Value: ").append(componentValue);
            }
        }

        // 좌표 정보 추가
        if (!node.path("x").isMissingNode() && !node.path("y").isMissingNode()) {
            double x = node.path("x").asDouble();
            double y = node.path("y").asDouble();
            content.append(" Position: (").append(String.format("%.1f", x))
                  .append(", ").append(String.format("%.1f", y)).append(")");
        }

        content.append("\n");
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
        
        // 위치 변경 확인 (absoluteBoundingBox 사용)
        JsonNode oldBox = oldNode.path("absoluteBoundingBox");
        JsonNode newBox = newNode.path("absoluteBoundingBox");
        
        if (!oldBox.isMissingNode() && !newBox.isMissingNode()) {
            double oldX = oldBox.path("x").asDouble();
            double oldY = oldBox.path("y").asDouble();
            double newX = newBox.path("x").asDouble();
            double newY = newBox.path("y").asDouble();
            
            if (oldX != newX || oldY != newY) {
                changes.put("position", String.format("(%.1f, %.1f) → (%.1f, %.1f)", oldX, oldY, newX, newY));
            }
            
            // 크기 변경 확인
            double oldWidth = oldBox.path("width").asDouble();
            double oldHeight = oldBox.path("height").asDouble();
            double newWidth = newBox.path("width").asDouble();
            double newHeight = newBox.path("height").asDouble();
            
            boolean widthChanged = oldWidth != newWidth;
            boolean heightChanged = oldHeight != newHeight;
            
            if (widthChanged || heightChanged) {
                StringBuilder sizeChange = new StringBuilder();
                if (widthChanged) {
                    sizeChange.append("너비: ").append(String.format("%.1f", oldWidth))
                             .append(" → ").append(String.format("%.1f", newWidth));
                }
                if (heightChanged) {
                    if (widthChanged) sizeChange.append(", ");
                    sizeChange.append("높이: ").append(String.format("%.1f", oldHeight))
                             .append(" → ").append(String.format("%.1f", newHeight));
                }
                changes.put("크기", sizeChange.toString());
            }
        }
        
        // size 속성을 사용한 크기 변경 확인 (일부 컴포넌트에서 사용)
        if (oldNode.has("size") && newNode.has("size")) {
            JsonNode oldSize = oldNode.path("size");
            JsonNode newSize = newNode.path("size");
            
            if (!oldSize.path("width").isMissingNode() && !newSize.path("width").isMissingNode() &&
                !oldSize.path("height").isMissingNode() && !newSize.path("height").isMissingNode()) {
                
                double oldWidth = oldSize.path("width").asDouble();
                double oldHeight = oldSize.path("height").asDouble();
                double newWidth = newSize.path("width").asDouble();
                double newHeight = newSize.path("height").asDouble();
                
                boolean widthChanged = oldWidth != newWidth;
                boolean heightChanged = oldHeight != newHeight;
                
                if (widthChanged || heightChanged) {
                    StringBuilder sizeChange = new StringBuilder();
                    if (widthChanged) {
                        sizeChange.append("너비: ").append(String.format("%.1f", oldWidth))
                                 .append(" → ").append(String.format("%.1f", newWidth));
                    }
                    if (heightChanged) {
                        if (widthChanged) sizeChange.append(", ");
                        sizeChange.append("높이: ").append(String.format("%.1f", oldHeight))
                                 .append(" → ").append(String.format("%.1f", newHeight));
                    }
                    changes.put("크기", sizeChange.toString());
                }
            }
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

    private String getComponentValue(JsonNode element) {
        String type = element.path("type").asText();
        String name = element.path("name").asText().toLowerCase();

        // 1. characters 속성 확인
        String textValue = element.path("characters").asText(null);
        if (textValue != null && !textValue.trim().isEmpty()) {
            return textValue.trim();
        }

        // 2. componentProperties 확인
        JsonNode componentProperties = element.path("componentProperties");
        if (!componentProperties.isMissingNode()) {
            Iterator<Map.Entry<String, JsonNode>> fields = componentProperties.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode prop = entry.getValue();
                if ("TEXT".equalsIgnoreCase(prop.path("type").asText()) && 
                    !prop.path("value").isMissingNode()) {
                    textValue = prop.path("value").asText();
                    if (!textValue.trim().isEmpty()) {
                        return textValue.trim();
                    }
                }
            }
        }

        // 3. mainComponent 확인
        JsonNode mainComponent = element.path("mainComponent");
        if (!mainComponent.isMissingNode()) {
            textValue = mainComponent.path("characters").asText(null);
            if (textValue != null && !textValue.trim().isEmpty()) {
                return textValue.trim();
            }
        }

        // 4. children에서 TEXT 타입 확인
        JsonNode children = element.path("children");
        if (!children.isMissingNode() && children.isArray()) {
            for (JsonNode child : children) {
                if ("TEXT".equalsIgnoreCase(child.path("type").asText())) {
                    textValue = child.path("characters").asText(null);
                    if (textValue != null && !textValue.trim().isEmpty()) {
                        return textValue.trim();
                    }
                }
            }
        }

        return null;
    }

    private String getComponentType(JsonNode node) {
        String type = node.path("type").asText();
        String name = node.path("name").asText().toLowerCase();

        if ("INSTANCE".equals(type) || "COMPONENT".equals(type)) {
            if (name.contains("button") || name.contains("btn")) {
                return "Button";
            } else if (name.contains("inputbox")) {
                return "Input";
            } else if (name.contains("output") || name.contains("display")) {
                return "Output";
            } else if (name.contains("label") || name.contains("text")) {
                return "Label";
            } else if (name.contains("checkbox")) {
                return "Checkbox";
            } else if (name.contains("radio")) {
                return "Radio";
            } else if (name.contains("combobox") || name.contains("combo")) {
                return "Select";
            } else if (name.contains("textarea")) {
                return "TextArea";
            }
        }
        return type;
    }
}