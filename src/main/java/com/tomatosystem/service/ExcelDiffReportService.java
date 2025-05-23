package com.tomatosystem.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ExcelDiffReportService {
    private static final String[] SUMMARY_HEADERS = {"구분", "개수", "상세 내용"};
    private static final String[] DETAIL_HEADERS = {"컴포넌트 타입", "이름", "값", "변경 유형", "이전 값", "변경 값"};
    private static final int COLUMN_WIDTH_UNIT = 256; // POI의 컬럼 너비 단위

    public void generateExcelReport(List<String> added, List<String> removed, List<String> modified,
                                  Map<String, JsonNode> oldMap, Map<String, JsonNode> newMap,
                                  String pageName, int randomNum) throws Exception {
        try {
            System.out.println("Excel 리포트 생성 시작...");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String today = dateFormat.format(new Date());
            
            // Excel 파일 저장 디렉토리 생성
            File directory = new File("C:\\Users\\LCM\\git\\Converter-Figma\\clx-src\\result\\excel\\" + today);
            if (!directory.exists()) {
                System.out.println("디렉토리 생성: " + directory.getAbsolutePath());
                directory.mkdirs();
            }

            String fileName = String.format("changes_%s_%d.xlsx", today, randomNum);
            File file = new File(directory, fileName);
            System.out.println("생성할 파일 경로: " + file.getAbsolutePath());

            // 워크북 생성
            System.out.println("워크북 생성 시작...");
            XSSFWorkbook workbook = new XSSFWorkbook();

            // 스타일 생성
            System.out.println("스타일 생성...");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle summaryStyle = createSummaryStyle(workbook);
            CellStyle pageHeaderStyle = createPageHeaderStyle(workbook);

            // 전체 요약 시트 생성
            System.out.println("전체 요약 시트 생성...");
            createSummarySheet(workbook, added.size(), removed.size(), modified.size(), headerStyle, summaryStyle);

            // 페이지별 통계 및 상세 정보 시트 생성
            System.out.println("페이지별 변경사항 그룹화...");
            Map<String, List<ComponentChange>> pageChanges = groupChangesByPage(added, removed, modified, oldMap, newMap);
            
            System.out.println("페이지별 시트 생성...");
            for (Map.Entry<String, List<ComponentChange>> entry : pageChanges.entrySet()) {
                String currentPage = entry.getKey();
                List<ComponentChange> changes = entry.getValue();
                System.out.println("페이지 처리 중: " + currentPage);
                createPageDetailSheet(workbook, currentPage, changes, headerStyle, dataStyle, pageHeaderStyle);
            }

            // 파일 저장
            System.out.println("워크북 파일 저장...");
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }
            workbook.close();

            System.out.println("Excel 보고서가 생성되었습니다: " + file.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Excel 파일 생성 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw e; // 상위로 예외를 전파하여 호출자가 처리할 수 있도록 함
        }
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // 배경색 설정 (연한 파란색)
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // 테두리 설정
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        // 폰트 설정
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        
        // 정렬 설정
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        return style;
    }

    private CellStyle createDataStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // 테두리 설정
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        // 정렬 설정
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        // 자동 줄바꿈 설정
        style.setWrapText(true);
        
        // 배경색 없음 (흰색 배경)
        style.setFillPattern(FillPatternType.NO_FILL);
        
        return style;
    }

    private CellStyle createSummaryStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // 테두리 설정
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        // 폰트 설정
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        
        // 정렬 설정
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        // 배경색 없음 (흰색 배경)
        style.setFillPattern(FillPatternType.NO_FILL);
        
        return style;
    }

    private CellStyle createPageHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // 배경색 설정 (연한 녹색)
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // 테두리 설정
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        // 폰트 설정
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        
        // 정렬 설정
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        return style;
    }

    private void createSummarySheet(XSSFWorkbook workbook, int addedCount, int removedCount, 
                                  int modifiedCount, CellStyle headerStyle, CellStyle dataStyle) {
        XSSFSheet sheet = workbook.createSheet("전체 변경 요약");
        
        // 컬럼 너비 설정
        sheet.setColumnWidth(0, 15 * COLUMN_WIDTH_UNIT);  // 구분
        sheet.setColumnWidth(1, 10 * COLUMN_WIDTH_UNIT);  // 개수
        sheet.setColumnWidth(2, 40 * COLUMN_WIDTH_UNIT);  // 상세 내용

        // 헤더 행 생성
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < SUMMARY_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(SUMMARY_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        // 데이터 행 생성
        createSummaryRow(sheet, 1, "추가", addedCount, "새로 추가된 컴포넌트", dataStyle);
        createSummaryRow(sheet, 2, "삭제", removedCount, "삭제된 컴포넌트", dataStyle);
        createSummaryRow(sheet, 3, "수정", modifiedCount, "속성이 변경된 컴포넌트", dataStyle);
    }

    private void createSummaryRow(XSSFSheet sheet, int rowNum, String category, 
                                int count, String description, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        
        Cell categoryCell = row.createCell(0);
        categoryCell.setCellValue(category);
        categoryCell.setCellStyle(style);

        Cell countCell = row.createCell(1);
        countCell.setCellValue(count);
        countCell.setCellStyle(style);

        Cell descCell = row.createCell(2);
        descCell.setCellValue(description);
        descCell.setCellStyle(style);
    }

    private static class ComponentChange {
        String id;
        String type;
        JsonNode oldNode;
        JsonNode newNode;
        String changeType;
        String pageName;

        ComponentChange(String id, String type, JsonNode oldNode, JsonNode newNode, String changeType, String pageName) {
            this.id = id;
            this.type = type;
            this.oldNode = oldNode;
            this.newNode = newNode;
            this.changeType = changeType;
            this.pageName = pageName;
        }
    }

    private Map<String, List<ComponentChange>> groupChangesByPage(
            List<String> added, List<String> removed, List<String> modified,
            Map<String, JsonNode> oldMap, Map<String, JsonNode> newMap) {
        
        Map<String, List<ComponentChange>> pageChanges = new HashMap<>();
        int totalItems = added.size() + removed.size() + modified.size();
        int processedItems = 0;

        try {
            // 추가된 항목 처리
            System.out.println("추가된 항목 처리 중... (" + added.size() + "개)");
            for (String id : added) {
                JsonNode node = newMap.get(id);
                String pageName = getPageName(node);
                pageChanges.computeIfAbsent(pageName, k -> new ArrayList<>())
                          .add(new ComponentChange(id, "추가", null, node, "추가", pageName));
                processedItems++;
                if (processedItems % 100 == 0) {
                    System.out.println("처리 진행률: " + (processedItems * 100 / totalItems) + "%");
                }
            }

            // 삭제된 항목 처리
            System.out.println("삭제된 항목 처리 중... (" + removed.size() + "개)");
            for (String id : removed) {
                JsonNode node = oldMap.get(id);
                String pageName = getPageName(node);
                pageChanges.computeIfAbsent(pageName, k -> new ArrayList<>())
                          .add(new ComponentChange(id, "삭제", node, null, "삭제", pageName));
                processedItems++;
                if (processedItems % 100 == 0) {
                    System.out.println("처리 진행률: " + (processedItems * 100 / totalItems) + "%");
                }
            }

            // 수정된 항목 처리
            System.out.println("수정된 항목 처리 중... (" + modified.size() + "개)");
            for (String id : modified) {
                JsonNode oldNode = oldMap.get(id);
                JsonNode newNode = newMap.get(id);
                String pageName = getPageName(oldNode);
                if (!findActualChanges(oldNode, newNode).isEmpty()) {
                    pageChanges.computeIfAbsent(pageName, k -> new ArrayList<>())
                              .add(new ComponentChange(id, "수정", oldNode, newNode, "수정", pageName));
                }
                processedItems++;
                if (processedItems % 100 == 0) {
                    System.out.println("처리 진행률: " + (processedItems * 100 / totalItems) + "%");
                }
            }

            System.out.println("페이지별 그룹화 완료. 총 " + pageChanges.size() + "개 페이지 처리됨");
            return pageChanges;

        } catch (Exception e) {
            System.err.println("페이지별 그룹화 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    private String getPageName(JsonNode node) {
        try {
            // 노드의 상위 계층을 탐색하여 페이지 이름을 찾음
            if (node.has("type") && "PAGE".equals(node.path("type").asText())) {
                return node.path("name").asText("기본 페이지");
            }

            // document 노드 찾기
            if (node.has("document")) {
                JsonNode document = node.path("document");
                if (!document.isMissingNode()) {
                    JsonNode children = document.path("children");
                    if (children.isArray() && children.size() > 0) {
                        JsonNode firstPage = children.get(0);
                        if (firstPage.has("name")) {
                            return firstPage.path("name").asText("기본 페이지");
                        }
                    }
                }
            }

            return "기본 페이지";
        } catch (Exception e) {
            System.err.println("페이지 이름 추출 중 오류 발생: " + e.getMessage());
            return "기본 페이지";
        }
    }

    private void createPageDetailSheet(XSSFWorkbook workbook, String pageName, 
                                     List<ComponentChange> changes,
                                     CellStyle headerStyle, CellStyle dataStyle,
                                     CellStyle pageHeaderStyle) {
        XSSFSheet sheet = workbook.createSheet(pageName);
        
        // 컬럼 너비 설정
        sheet.setColumnWidth(0, 15 * COLUMN_WIDTH_UNIT);  // 컴포넌트 타입
        sheet.setColumnWidth(1, 20 * COLUMN_WIDTH_UNIT);  // 이름
        sheet.setColumnWidth(2, 20 * COLUMN_WIDTH_UNIT);  // 값
        sheet.setColumnWidth(3, 15 * COLUMN_WIDTH_UNIT);  // 변경 유형
        sheet.setColumnWidth(4, 25 * COLUMN_WIDTH_UNIT);  // 이전 값
        sheet.setColumnWidth(5, 25 * COLUMN_WIDTH_UNIT);  // 변경 값

        int currentRow = 0;

        // 페이지 통계 추가
        currentRow = addPageStatistics(sheet, changes, currentRow, pageHeaderStyle);
        
        // 빈 행 추가
        currentRow += 2;

        // 헤더 행 생성
        Row headerRow = sheet.createRow(currentRow++);
        for (int i = 0; i < DETAIL_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(DETAIL_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        // 상세 정보 추가
        for (ComponentChange change : changes) {
            currentRow = addDetailRow(sheet, currentRow, change, dataStyle);
            // 간격 제거
        }
    }

    private int addPageStatistics(XSSFSheet sheet, List<ComponentChange> changes, int startRow, CellStyle headerStyle) {
        int currentRow = startRow;
        
        // 통계 헤더
        Row headerRow = sheet.createRow(currentRow++);
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue("페이지 변경 통계");
        headerCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 0, 5));

        // 각 유형별 개수 계산
        long addedCount = changes.stream().filter(c -> "추가".equals(c.changeType)).count();
        long removedCount = changes.stream().filter(c -> "삭제".equals(c.changeType)).count();
        long modifiedCount = changes.stream().filter(c -> "수정".equals(c.changeType)).count();

        // 통계 정보 추가
        String[] stats = {"추가된 항목", "삭제된 항목", "수정된 항목"};
        long[] counts = {addedCount, removedCount, modifiedCount};
        
        // 통계 데이터용 스타일 생성
        CellStyle statsDataStyle = sheet.getWorkbook().createCellStyle();
        statsDataStyle.cloneStyleFrom(headerStyle);
        statsDataStyle.setFillPattern(FillPatternType.NO_FILL); // 배경색 없음
        
        for (int i = 0; i < stats.length; i++) {
            Row row = sheet.createRow(currentRow++);
            Cell typeCell = row.createCell(0);
            Cell countCell = row.createCell(1);
            
            typeCell.setCellValue(stats[i]);
            countCell.setCellValue(counts[i]);
            
            typeCell.setCellStyle(statsDataStyle);
            countCell.setCellStyle(statsDataStyle);
        }

        return currentRow;
    }

    private int addDetailRow(XSSFSheet sheet, int rowNum, ComponentChange change, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        JsonNode node = change.oldNode != null ? change.oldNode : change.newNode;
        
        // 컴포넌트 타입
        Cell typeCell = row.createCell(0);
        typeCell.setCellValue(getComponentType(node));
        typeCell.setCellStyle(style);

        // 이름
        Cell nameCell = row.createCell(1);
        nameCell.setCellValue(node.path("name").asText());
        nameCell.setCellStyle(style);

        // 값
        Cell valueCell = row.createCell(2);
        String value = getComponentValue(node);
        valueCell.setCellValue(value != null ? value : "");
        valueCell.setCellStyle(style);

        // 변경 유형
        Cell changeTypeCell = row.createCell(3);
        changeTypeCell.setCellValue(change.changeType);
        changeTypeCell.setCellStyle(style);

        // 변경 전/후 값
        if ("수정".equals(change.changeType) && change.newNode != null) {
            Map<String, String> changes = findActualChanges(change.oldNode, change.newNode);
            
            Cell oldValueCell = row.createCell(4);
            Cell newValueCell = row.createCell(5);
            
            StringBuilder oldValue = new StringBuilder();
            StringBuilder newValue = new StringBuilder();
            
            for (Map.Entry<String, String> entry : changes.entrySet()) {
                String[] parts = entry.getValue().split(" → ");
                if (parts.length == 2) {
                    oldValue.append(entry.getKey()).append(": ").append(parts[0]).append("\n");
                    newValue.append(entry.getKey()).append(": ").append(parts[1]).append("\n");
                }
            }
            
            oldValueCell.setCellValue(oldValue.toString().trim());
            newValueCell.setCellValue(newValue.toString().trim());
            
            oldValueCell.setCellStyle(style);
            newValueCell.setCellStyle(style);
        }

        return rowNum + 1;
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

    private String getComponentValue(JsonNode element) {
        // characters 속성 확인
        String textValue = element.path("characters").asText(null);
        if (textValue != null && !textValue.trim().isEmpty()) {
            return textValue.trim();
        }

        // componentProperties 확인
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

        // mainComponent 확인
        JsonNode mainComponent = element.path("mainComponent");
        if (!mainComponent.isMissingNode()) {
            textValue = mainComponent.path("characters").asText(null);
            if (textValue != null && !textValue.trim().isEmpty()) {
                return textValue.trim();
            }
        }

        // children에서 TEXT 타입 확인
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

    private Map<String, String> findActualChanges(JsonNode oldNode, JsonNode newNode) {
        Map<String, String> changes = new HashMap<>();
        
        // 위치 변경 확인
        JsonNode oldBox = oldNode.path("absoluteBoundingBox");
        JsonNode newBox = newNode.path("absoluteBoundingBox");
        
        if (!oldBox.isMissingNode() && !newBox.isMissingNode()) {
            double oldX = oldBox.path("x").asDouble();
            double oldY = oldBox.path("y").asDouble();
            double newX = newBox.path("x").asDouble();
            double newY = newBox.path("y").asDouble();
            
            if (oldX != newX || oldY != newY) {
                changes.put("위치", String.format("(%.1f, %.1f) → (%.1f, %.1f)", oldX, oldY, newX, newY));
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

        // 스타일 변경 확인
        compareStyleProperties(changes, oldNode.path("style"), newNode.path("style"));

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

    private void compareStyleProperties(Map<String, String> changes, JsonNode oldStyle, JsonNode newStyle) {
        if (!oldStyle.isMissingNode() && !newStyle.isMissingNode()) {
            String[] styleProps = {"fontFamily", "fontSize", "fontWeight", "textAlignHorizontal", "textAlignVertical"};
            for (String prop : styleProps) {
                JsonNode oldVal = oldStyle.path(prop);
                JsonNode newVal = newStyle.path(prop);
                if (!oldVal.isMissingNode() && !newVal.isMissingNode() && !oldVal.equals(newVal)) {
                    changes.put(prop, oldVal.asText() + " → " + newVal.asText());
                }
            }
        }
    }

    private String convertToHexColor(JsonNode colorNode) {
        if (colorNode == null || colorNode.isMissingNode()) {
            return "";
        }

        try {
            double r = colorNode.path("r").asDouble();
            double g = colorNode.path("g").asDouble();
            double b = colorNode.path("b").asDouble();
            double a = colorNode.has("a") ? colorNode.path("a").asDouble() : 1.0;

            int rInt = Math.min(255, Math.max(0, (int)(r * 255 + 0.5)));
            int gInt = Math.min(255, Math.max(0, (int)(g * 255 + 0.5)));
            int bInt = Math.min(255, Math.max(0, (int)(b * 255 + 0.5)));

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
} 