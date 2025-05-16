package com.tomatosystem.service.webaccess.report;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import java.io.FileOutputStream;
import java.util.*;
import java.text.SimpleDateFormat;

public class ExcelReportGenerator {
    
    public void generateReport(List<Map<String, Object>> issues, String outputPath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            // 기본 셀 스타일 설정
            CellStyle defaultStyle = createDefaultStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            
            // 요약 시트 생성
            Sheet summarySheet = workbook.createSheet("접근성 분석 요약");
            createSummarySheet(summarySheet, issues, titleStyle, headerStyle, defaultStyle);

            // 상세 분석 시트 생성
            Sheet detailSheet = workbook.createSheet("상세 분석 결과");
            createDetailSheet(detailSheet, issues, titleStyle, headerStyle, defaultStyle);

            // WCAG 기준별 분석 시트
            Sheet wcagSheet = workbook.createSheet("WCAG 기준별 분석");
            createWCAGAnalysisSheet(wcagSheet, issues, titleStyle, headerStyle, defaultStyle);

            // 컴포넌트별 분석 시트
            Sheet componentSheet = workbook.createSheet("컴포넌트별 분석");
            createComponentAnalysisSheet(componentSheet, issues, titleStyle, headerStyle, defaultStyle);

            // 모든 시트의 기본 설정
            for (Sheet sheet : new Sheet[]{summarySheet, detailSheet, wcagSheet, componentSheet}) {
                sheet.setDefaultRowHeight((short) 400); // 기본 행 높이
                sheet.setDisplayGridlines(false); // 눈금선 숨기기
            }

            // Excel 파일 저장
            String excelFile = outputPath + "/accessibility-report.xlsx";
            try (FileOutputStream fileOut = new FileOutputStream(excelFile)) {
                workbook.write(fileOut);
            }
        } catch (Exception e) {
            throw new RuntimeException("Excel 리포트 생성 중 오류 발생: " + e.getMessage(), e);
        }
    }

    private void createSummarySheet(Sheet sheet, List<Map<String, Object>> issues, CellStyle titleStyle, CellStyle headerStyle, CellStyle defaultStyle) {
        // 제목 행
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("웹 접근성 분석 요약 리포트");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
        titleRow.setHeight((short) 900);

        // 분석 시간과 총 이슈 수
        Row infoRow = sheet.createRow(1);
        infoRow.createCell(0).setCellValue("분석 시간: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        infoRow.createCell(2).setCellValue("총 발견된 문제: " + (issues.isEmpty() ? "0" : issues.size()) + "개");
        infoRow.getCell(0).setCellStyle(defaultStyle);
        infoRow.getCell(2).setCellStyle(defaultStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 1));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 2, 4));
        
        // 헤더 행
        Row headerRow = sheet.createRow(3);
        String[] headers = {"분석 항목", "문제 수", "심각도", "주요 문제점", "개선 권고사항"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 데이터 행 추가
        Map<String, Integer> issueCounts = new HashMap<>();
        Map<String, String> severityLevels = new HashMap<>();
        Map<String, List<String>> recommendations = new HashMap<>();
        
        // 모든 가능한 타입에 대해 초기화
        String[] allTypes = {"ColorContrast", "KeyboardAccessibility", "AlternativeText", "HeadingStructure", "FontSize"};
        for (String type : allTypes) {
            issueCounts.put(type, 0);
            severityLevels.put(type, "낮음");
            recommendations.put(type, new ArrayList<>());
        }
        
        // 실제 이슈 집계
        if (!issues.isEmpty()) {
            for (Map<String, Object> issue : issues) {
                String type = (String) issue.get("type");
                issueCounts.merge(type, 1, Integer::sum);
                
                if ("ColorContrast".equals(type)) {
                    String ratio = (String) issue.get("contrastRatio");
                    double contrastRatio = Double.parseDouble(ratio.split(":")[0]);
                    severityLevels.put(type, contrastRatio < 2.25 ? "높음" : "중간");
                } else if ("KeyboardAccessibility".equals(type)) {
                    severityLevels.put(type, "높음");
                }
                
                String recommendation = (String) issue.get("recommendation");
                if (recommendation != null) {
                    recommendations.get(type).add(recommendation);
                }
            }
        }
        
        // 데이터 출력
        int rowNum = 4;
        for (String type : allTypes) {
            Row row = sheet.createRow(rowNum++);
            
            // 분석 항목
            Cell typeCell = row.createCell(0);
            typeCell.setCellValue(getKoreanTypeName(type));
            typeCell.setCellStyle(defaultStyle);
            
            // 문제 수
            Cell countCell = row.createCell(1);
            countCell.setCellValue(issueCounts.get(type));
            countCell.setCellStyle(defaultStyle);
            
            // 심각도
            Cell severityCell = row.createCell(2);
            severityCell.setCellValue(severityLevels.get(type));
            severityCell.setCellStyle(defaultStyle);
            
            // 주요 문제점
            Cell issueCell = row.createCell(3);
            issueCell.setCellValue(getMainIssueDescription(type));
            issueCell.setCellStyle(defaultStyle);
            
            // 개선 권고사항
            Cell recCell = row.createCell(4);
            recCell.setCellValue(getDefaultRecommendation(type));
            recCell.setCellStyle(defaultStyle);
        }
        
        // 열 너비 설정
        sheet.setColumnWidth(0, 30 * 256); // 분석 항목
        sheet.setColumnWidth(1, 15 * 256); // 문제 수
        sheet.setColumnWidth(2, 15 * 256); // 심각도
        sheet.setColumnWidth(3, 40 * 256); // 주요 문제점
        sheet.setColumnWidth(4, 50 * 256); // 개선 권고사항
    }

    private String getMainIssueDescription(String type) {
        switch (type) {
            case "ColorContrast":
                return "텍스트와 배경 간의 색상 대비가 WCAG 기준에 미달";
            case "KeyboardAccessibility":
                return "키보드로 접근 및 조작이 불가능한 요소 존재";
            case "AlternativeText":
                return "이미지에 대한 대체 텍스트 미제공";
            case "HeadingStructure":
                return "제목 구조의 계층성 오류";
            case "FontSize":
                return "가독성이 떨어지는 작은 글자 크기";
            default:
                return "";
        }
    }

    private String getDefaultRecommendation(String type) {
        switch (type) {
            case "ColorContrast":
                return "일반 텍스트는 4.5:1, 큰 텍스트는 3:1 이상의 명암비 확보";
            case "KeyboardAccessibility":
                return "모든 기능은 키보드로 접근 및 조작 가능하도록 구현";
            case "AlternativeText":
                return "의미 있는 이미지에 적절한 대체 텍스트 제공";
            case "HeadingStructure":
                return "논리적인 제목 구조를 위해 적절한 수준의 제목 태그 사용";
            case "FontSize":
                return "일반 텍스트 16px, 볼드체 14px 이상 사용 권장";
            default:
                return "";
        }
    }

    private CellStyle createDefaultStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        
        Font font = workbook.createFont();
        font.setFontName("맑은 고딕");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        Font font = workbook.createFont();
        font.setFontName("맑은 고딕");
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        Font font = workbook.createFont();
        font.setFontName("맑은 고딕");
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        
        return style;
    }

    private void createDetailSheet(Sheet sheet, List<Map<String, Object>> issues, CellStyle titleStyle, CellStyle headerStyle, CellStyle defaultStyle) {
        // 상세 분석 헤더
        Row headerRow = sheet.createRow(0);
        String[] headers = {"유형", "경로", "요소", "현재 값", "권장사항", "WCAG 기준"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
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

    private void createWCAGAnalysisSheet(Sheet sheet, List<Map<String, Object>> issues, CellStyle titleStyle, CellStyle headerStyle, CellStyle defaultStyle) {
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

    private void createComponentAnalysisSheet(Sheet sheet, List<Map<String, Object>> issues, CellStyle titleStyle, CellStyle headerStyle, CellStyle defaultStyle) {
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
            case "FontSize": return "글자 크기";
            default: return type;
        }
    }
} 