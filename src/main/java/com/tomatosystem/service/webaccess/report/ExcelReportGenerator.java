package com.tomatosystem.service.webaccess.report;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.util.*;

public class ExcelReportGenerator {
    
    public void generateReport(List<Map<String, Object>> issues, String outputPath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            // 요약 시트 생성
            Sheet summarySheet = workbook.createSheet("접근성 분석 요약");
            createSummarySheet(summarySheet, issues);

            // 상세 분석 시트 생성
            Sheet detailSheet = workbook.createSheet("상세 분석 결과");
            createDetailSheet(detailSheet, issues);

            // WCAG 기준별 분석 시트
            Sheet wcagSheet = workbook.createSheet("WCAG 기준별 분석");
            createWCAGAnalysisSheet(wcagSheet, issues);

            // 컴포넌트별 분석 시트
            Sheet componentSheet = workbook.createSheet("컴포넌트별 분석");
            createComponentAnalysisSheet(componentSheet, issues);

            // Excel 파일 저장
            String excelFile = outputPath + "/accessibility-report.xlsx";
            try (FileOutputStream fileOut = new FileOutputStream(excelFile)) {
                workbook.write(fileOut);
            }
        } catch (Exception e) {
            throw new RuntimeException("Excel 리포트 생성 중 오류 발생: " + e.getMessage(), e);
        }
    }

    private void createSummarySheet(Sheet sheet, List<Map<String, Object>> issues) {
        // 헤더 스타일 설정
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        
        // 요약 정보 헤더
        Row headerRow = sheet.createRow(0);
        createHeaderCell(headerRow, 0, "분석 항목", headerStyle);
        createHeaderCell(headerRow, 1, "문제 수", headerStyle);
        createHeaderCell(headerRow, 2, "심각도", headerStyle);
        
        // 카테고리별 이슈 수 집계
        Map<String, Integer> issueCounts = new HashMap<>();
        Map<String, String> severityLevels = new HashMap<>();
        
        for (Map<String, Object> issue : issues) {
            String type = (String) issue.get("type");
            issueCounts.merge(type, 1, Integer::sum);
            
            // 심각도 결정 (예: 대비율이 최소 기준의 50% 미만이면 "높음")
            if ("ColorContrast".equals(type)) {
                String ratio = (String) issue.get("contrastRatio");
                double contrastRatio = Double.parseDouble(ratio.split(":")[0]);
                severityLevels.put(type, contrastRatio < 2.25 ? "높음" : "중간");
            } else if ("KeyboardAccessibility".equals(type)) {
                severityLevels.put(type, "높음");
            } else {
                severityLevels.put(type, "중간");
            }
        }
        
        // 데이터 행 추가
        int rowNum = 1;
        for (Map.Entry<String, Integer> entry : issueCounts.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(getKoreanTypeName(entry.getKey()));
            row.createCell(1).setCellValue(entry.getValue());
            row.createCell(2).setCellValue(severityLevels.get(entry.getKey()));
        }
        
        // 열 너비 자동 조정
        for (int i = 0; i < 3; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createDetailSheet(Sheet sheet, List<Map<String, Object>> issues) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        
        // 상세 분석 헤더
        Row headerRow = sheet.createRow(0);
        String[] headers = {"유형", "경로", "요소", "현재 값", "권장사항", "WCAG 기준"};
        for (int i = 0; i < headers.length; i++) {
            createHeaderCell(headerRow, i, headers[i], headerStyle);
        }
        
        // 상세 데이터 추가
        int rowNum = 1;
        for (Map<String, Object> issue : issues) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(getKoreanTypeName((String) issue.get("type")));
            row.createCell(1).setCellValue((String) issue.get("path"));
            row.createCell(2).setCellValue((String) issue.get("elementName"));
            
            // 현재 값 설정 (유형별로 다르게 처리)
            String currentValue = "";
            if ("ColorContrast".equals(issue.get("type"))) {
                currentValue = "대비율: " + issue.get("contrastRatio");
            } else if ("FontSize".equals(issue.get("type"))) {
                currentValue = "크기: " + issue.get("value");
            }
            row.createCell(3).setCellValue(currentValue);
            
            row.createCell(4).setCellValue((String) issue.get("recommendation"));
            row.createCell(5).setCellValue((String) issue.get("wcagCriteria"));
        }
        
        // 열 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createWCAGAnalysisSheet(Sheet sheet, List<Map<String, Object>> issues) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        
        // WCAG 기준별 분석 헤더
        Row headerRow = sheet.createRow(0);
        createHeaderCell(headerRow, 0, "WCAG 기준", headerStyle);
        createHeaderCell(headerRow, 1, "위반 수", headerStyle);
        createHeaderCell(headerRow, 2, "주요 문제점", headerStyle);
        
        // WCAG 기준별 이슈 집계
        Map<String, List<Map<String, Object>>> wcagIssues = new HashMap<>();
        for (Map<String, Object> issue : issues) {
            String wcagCriteria = (String) issue.get("wcagCriteria");
            wcagIssues.computeIfAbsent(wcagCriteria, k -> new ArrayList<>()).add(issue);
        }
        
        // 데이터 행 추가
        int rowNum = 1;
        for (Map.Entry<String, List<Map<String, Object>>> entry : wcagIssues.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue().size());
            
            // 주요 문제점 요약
            StringBuilder issues_summary = new StringBuilder();
            entry.getValue().stream()
                .map(issue -> (String) issue.get("recommendation"))
                .distinct()
                .limit(3)
                .forEach(rec -> issues_summary.append("- ").append(rec).append("\n"));
            
            row.createCell(2).setCellValue(issues_summary.toString());
        }
        
        // 열 너비 자동 조정
        for (int i = 0; i < 3; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createComponentAnalysisSheet(Sheet sheet, List<Map<String, Object>> issues) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        
        // 컴포넌트별 분석 헤더
        Row headerRow = sheet.createRow(0);
        createHeaderCell(headerRow, 0, "컴포넌트", headerStyle);
        createHeaderCell(headerRow, 1, "총 문제 수", headerStyle);
        createHeaderCell(headerRow, 2, "대비율 문제", headerStyle);
        createHeaderCell(headerRow, 3, "키보드 접근성", headerStyle);
        createHeaderCell(headerRow, 4, "대체 텍스트", headerStyle);
        createHeaderCell(headerRow, 5, "헤딩 구조", headerStyle);
        
        // 컴포넌트별 이슈 집계
        Map<String, Map<String, Integer>> componentIssues = new HashMap<>();
        for (Map<String, Object> issue : issues) {
            String path = (String) issue.get("path");
            String component = path.split(" > ")[0];
            String type = (String) issue.get("type");
            
            componentIssues.computeIfAbsent(component, k -> new HashMap<>())
                .merge(type, 1, Integer::sum);
        }
        
        // 데이터 행 추가
        int rowNum = 1;
        for (Map.Entry<String, Map<String, Integer>> entry : componentIssues.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            Map<String, Integer> typeCounts = entry.getValue();
            
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(typeCounts.values().stream().mapToInt(Integer::intValue).sum());
            row.createCell(2).setCellValue(typeCounts.getOrDefault("ColorContrast", 0));
            row.createCell(3).setCellValue(typeCounts.getOrDefault("KeyboardAccessibility", 0));
            row.createCell(4).setCellValue(typeCounts.getOrDefault("AlternativeText", 0));
            row.createCell(5).setCellValue(typeCounts.getOrDefault("HeadingStructure", 0));
        }
        
        // 열 너비 자동 조정
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        
        return style;
    }

    private void createHeaderCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private String getKoreanTypeName(String type) {
        switch (type) {
            case "ColorContrast": return "색상 대비";
            case "KeyboardAccessibility": return "키보드 접근성";
            case "AlternativeText": return "대체 텍스트";
            case "HeadingStructure": return "헤딩 구조";
            default: return type;
        }
    }
} 