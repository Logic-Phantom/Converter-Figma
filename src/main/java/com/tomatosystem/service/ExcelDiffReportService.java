package com.tomatosystem.service;

import org.apache.poi.ss.usermodel.*;
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
                                  String pageName, int randomNum) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String today = dateFormat.format(new Date());
            
            // Excel 파일 저장 디렉토리 생성
            File directory = new File("C:\\Users\\LCM\\git\\Converter-Figma\\clx-src\\result\\excel\\" + today);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fileName = String.format("changes_%s_%d.xlsx", today, randomNum);
            File file = new File(directory, fileName);

            // 워크북 생성
            XSSFWorkbook workbook = new XSSFWorkbook();

            // 스타일 생성
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle summaryStyle = createSummaryStyle(workbook);

            // 요약 시트 생성
            createSummarySheet(workbook, added.size(), removed.size(), modified.size(), headerStyle, dataStyle);

            // 상세 시트 생성 (페이지별로)
            createDetailSheet(workbook, pageName, added, removed, modified, oldMap, newMap, headerStyle, dataStyle);

            // 파일 저장
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }
            workbook.close();

            System.out.println("\nExcel 보고서가 생성되었습니다: " + file.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Excel 파일 생성 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
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
        
        return style;
    }

    private CellStyle createSummaryStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // 배경색 설정 (연한 노란색)
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
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
        
        return style;
    }

    private void createSummarySheet(XSSFWorkbook workbook, int addedCount, int removedCount, 
                                  int modifiedCount, CellStyle headerStyle, CellStyle dataStyle) {
        XSSFSheet sheet = workbook.createSheet("변경 요약");
        
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

    private void createDetailSheet(XSSFWorkbook workbook, String pageName, 
                                 List<String> added, List<String> removed, List<String> modified,
                                 Map<String, JsonNode> oldMap, Map<String, JsonNode> newMap,
                                 CellStyle headerStyle, CellStyle dataStyle) {
        XSSFSheet sheet = workbook.createSheet(pageName != null ? pageName : "상세 변경 내역");
        
        // 컬럼 너비 설정
        sheet.setColumnWidth(0, 15 * COLUMN_WIDTH_UNIT);  // 컴포넌트 타입
        sheet.setColumnWidth(1, 20 * COLUMN_WIDTH_UNIT);  // 이름
        sheet.setColumnWidth(2, 20 * COLUMN_WIDTH_UNIT);  // 값
        sheet.setColumnWidth(3, 15 * COLUMN_WIDTH_UNIT);  // 변경 유형
        sheet.setColumnWidth(4, 25 * COLUMN_WIDTH_UNIT);  // 이전 값
        sheet.setColumnWidth(5, 25 * COLUMN_WIDTH_UNIT);  // 변경 값

        // 헤더 행 생성
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < DETAIL_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(DETAIL_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;

        // 추가된 항목
        for (String id : added) {
            JsonNode node = newMap.get(id);
            rowNum = addDetailRow(sheet, rowNum, node, null, "추가", dataStyle);
        }

        // 삭제된 항목
        for (String id : removed) {
            JsonNode node = oldMap.get(id);
            rowNum = addDetailRow(sheet, rowNum, node, null, "삭제", dataStyle);
        }

        // 수정된 항목
        for (String id : modified) {
            JsonNode oldNode = oldMap.get(id);
            JsonNode newNode = newMap.get(id);
            Map<String, String> changes = findActualChanges(oldNode, newNode);
            if (!changes.isEmpty()) {
                rowNum = addDetailRow(sheet, rowNum, oldNode, newNode, "수정", dataStyle);
            }
        }
    }

    private int addDetailRow(XSSFSheet sheet, int rowNum, JsonNode node, JsonNode newNode, 
                           String changeType, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        
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
        changeTypeCell.setCellValue(changeType);
        changeTypeCell.setCellStyle(style);

        // 변경 전/후 값
        if ("수정".equals(changeType) && newNode != null) {
            Map<String, String> changes = findActualChanges(node, newNode);
            
            Cell oldValueCell = row.createCell(4);
            Cell newValueCell = row.createCell(5);
            
            StringBuilder oldValue = new StringBuilder();
            StringBuilder newValue = new StringBuilder();
            
            for (Map.Entry<String, String> change : changes.entrySet()) {
                String[] parts = change.getValue().split(" → ");
                if (parts.length == 2) {
                    oldValue.append(change.getKey()).append(": ").append(parts[0]).append("\n");
                    newValue.append(change.getKey()).append(": ").append(parts[1]).append("\n");
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