package com.tomatosystem.service.webaccess.report;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import java.io.File;
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
                
                // 모든 행과 열에 대해 테두리 스타일 재적용
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        CellStyle style = cell.getCellStyle();
                        if (style != null) {
                            CellStyle newStyle = workbook.createCellStyle();
                            newStyle.cloneStyleFrom(style);
                            // 모든 테두리를 명시적으로 설정
                            newStyle.setBorderTop(style.getBorderTopEnum());
                            newStyle.setBorderBottom(style.getBorderBottomEnum());
                            newStyle.setBorderLeft(style.getBorderLeftEnum());
                            newStyle.setBorderRight(style.getBorderRightEnum());
                            // 배경색 제거
                            newStyle.setFillPattern(FillPatternType.NO_FILL);
                            cell.setCellStyle(newStyle);
                        }
                    }
                }
            }

            // 출력 디렉토리 생성
            File outputDir = new File(outputPath);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // Excel 파일 저장
            String excelFile = outputPath + File.separator + "accessibility-report.xlsx";
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
        for (int i = 0; i < 5; i++) {
            Cell cell = titleRow.createCell(i);
            cell.setCellStyle(titleStyle);
            if (i == 0) {
                cell.setCellValue("웹 접근성 분석 요약 리포트");
            }
        }
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
        titleRow.setHeight((short) 900);

        // 분석 시간과 총 이슈 수
        Row infoRow = sheet.createRow(1);
        for (int i = 0; i < 5; i++) {
            Cell cell = infoRow.createCell(i);
            cell.setCellStyle(defaultStyle);
            if (i == 0) {
                cell.setCellValue("분석 시간: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            } else if (i == 2) {
                cell.setCellValue("총 발견된 문제: " + (issues.isEmpty() ? "0" : issues.size()) + "개");
            }
        }
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
        
        // 모든 테두리 설정
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        
        // 배경색 명시적으로 제거
        style.setFillPattern(FillPatternType.NO_FILL);
        
        Font font = workbook.createFont();
        font.setFontName("맑은 고딕");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // 모든 테두리 설정
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderRight(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.MEDIUM);
        
        // 배경색 명시적으로 제거
        style.setFillPattern(FillPatternType.NO_FILL);
        
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
        
        // 모든 테두리 설정
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderRight(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.MEDIUM);
        
        // 배경색 명시적으로 제거
        style.setFillPattern(FillPatternType.NO_FILL);
        
        Font font = workbook.createFont();
        font.setFontName("맑은 고딕");
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        
        return style;
    }

    private void createDetailSheet(Sheet sheet, List<Map<String, Object>> issues, CellStyle titleStyle, CellStyle headerStyle, CellStyle defaultStyle) {
        // 제목 행
        Row titleRow = sheet.createRow(0);
        for (int i = 0; i < 6; i++) {
            Cell cell = titleRow.createCell(i);
            cell.setCellStyle(titleStyle);
            if (i == 0) {
                cell.setCellValue("웹 접근성 상세 분석 결과");
            }
        }
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        titleRow.setHeight((short) 900);

        // 분석 시간
        Row infoRow = sheet.createRow(1);
        for (int i = 0; i < 6; i++) {
            Cell cell = infoRow.createCell(i);
            cell.setCellStyle(defaultStyle);
            if (i == 0) {
                cell.setCellValue("분석 시간: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            }
        }
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));
        
        // 상세 분석 헤더
        Row headerRow = sheet.createRow(3);
        String[] headers = {"유형", "경로", "요소", "현재 값", "권장사항", "WCAG 기준"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 상세 데이터 추가
        int rowNum = 4;
        if (!issues.isEmpty()) {
            for (Map<String, Object> issue : issues) {
                Row row = sheet.createRow(rowNum++);
                
                // 유형
                Cell typeCell = row.createCell(0);
                typeCell.setCellValue(getKoreanTypeName((String) issue.get("type")));
                typeCell.setCellStyle(defaultStyle);
                
                // 경로
                Cell pathCell = row.createCell(1);
                pathCell.setCellValue((String) issue.get("path"));
                pathCell.setCellStyle(defaultStyle);
                
                // 요소
                Cell elementCell = row.createCell(2);
                elementCell.setCellValue((String) issue.get("elementName"));
                elementCell.setCellStyle(defaultStyle);
                
                // 현재 값
                Cell valueCell = row.createCell(3);
                String currentValue = "";
                if ("ColorContrast".equals(issue.get("type"))) {
                    currentValue = "대비율: " + issue.get("contrastRatio");
                } else if ("FontSize".equals(issue.get("type"))) {
                    currentValue = "크기: " + issue.get("value");
                }
                valueCell.setCellValue(currentValue);
                valueCell.setCellStyle(defaultStyle);
                
                // 권장사항
                Cell recCell = row.createCell(4);
                recCell.setCellValue((String) issue.get("recommendation"));
                recCell.setCellStyle(defaultStyle);
                
                // WCAG 기준
                Cell wcagCell = row.createCell(5);
                wcagCell.setCellValue((String) issue.get("wcagCriteria"));
                wcagCell.setCellStyle(defaultStyle);
            }
        } else {
            Row row = sheet.createRow(rowNum);
            // 모든 셀 생성 및 스타일 적용
            for (int i = 0; i < 6; i++) {
                Cell cell = row.createCell(i);
                cell.setCellStyle(defaultStyle);
                if (i == 0) {
                    cell.setCellValue("발견된 접근성 문제가 없습니다.");
                }
            }
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 5));
            // 병합된 셀의 테두리 설정
            RegionUtil.setBorderTop(BorderStyle.THIN, new CellRangeAddress(rowNum, rowNum, 0, 5), sheet);
            RegionUtil.setBorderBottom(BorderStyle.THIN, new CellRangeAddress(rowNum, rowNum, 0, 5), sheet);
            RegionUtil.setBorderLeft(BorderStyle.THIN, new CellRangeAddress(rowNum, rowNum, 0, 5), sheet);
            RegionUtil.setBorderRight(BorderStyle.THIN, new CellRangeAddress(rowNum, rowNum, 0, 5), sheet);
        }
        
        // 열 너비 설정
        sheet.setColumnWidth(0, 20 * 256); // 유형
        sheet.setColumnWidth(1, 40 * 256); // 경로
        sheet.setColumnWidth(2, 30 * 256); // 요소
        sheet.setColumnWidth(3, 20 * 256); // 현재 값
        sheet.setColumnWidth(4, 50 * 256); // 권장사항
        sheet.setColumnWidth(5, 30 * 256); // WCAG 기준
    }

    private void createWCAGAnalysisSheet(Sheet sheet, List<Map<String, Object>> issues, CellStyle titleStyle, CellStyle headerStyle, CellStyle defaultStyle) {
        // 제목 행
        Row titleRow = sheet.createRow(0);
        for (int i = 0; i < 3; i++) {
            Cell cell = titleRow.createCell(i);
            cell.setCellStyle(titleStyle);
            if (i == 0) {
                cell.setCellValue("WCAG 기준별 분석");
            }
        }
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
        titleRow.setHeight((short) 900);
        
        // WCAG 기준별 분석 헤더
        Row headerRow = sheet.createRow(2);
        String[] headers = {"WCAG 기준", "위반 수", "주요 문제점"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // WCAG 기준별 이슈 집계
        Map<String, List<Map<String, Object>>> wcagIssues = new HashMap<>();
        if (!issues.isEmpty()) {
            for (Map<String, Object> issue : issues) {
                String wcagCriteria = (String) issue.get("wcagCriteria");
                wcagIssues.computeIfAbsent(wcagCriteria, k -> new ArrayList<>()).add(issue);
            }
        }
        
        // 데이터 행 추가
        int rowNum = 3;
        if (!wcagIssues.isEmpty()) {
            for (Map.Entry<String, List<Map<String, Object>>> entry : wcagIssues.entrySet()) {
                Row row = sheet.createRow(rowNum++);
                
                // WCAG 기준
                Cell criteriaCell = row.createCell(0);
                criteriaCell.setCellValue(entry.getKey());
                criteriaCell.setCellStyle(defaultStyle);
                
                // 위반 수
                Cell countCell = row.createCell(1);
                countCell.setCellValue(entry.getValue().size());
                countCell.setCellStyle(defaultStyle);
                
                // 주요 문제점
                Cell issuesCell = row.createCell(2);
                StringBuilder issues_summary = new StringBuilder();
                entry.getValue().stream()
                    .map(issue -> (String) issue.get("recommendation"))
                    .distinct()
                    .limit(3)
                    .forEach(rec -> issues_summary.append("- ").append(rec).append("\n"));
                issuesCell.setCellValue(issues_summary.toString());
                issuesCell.setCellStyle(defaultStyle);
            }
        } else {
            Row row = sheet.createRow(rowNum);
            // 모든 셀 생성 및 스타일 적용
            for (int i = 0; i < 3; i++) {
                Cell cell = row.createCell(i);
                cell.setCellStyle(defaultStyle);
                if (i == 0) {
                    cell.setCellValue("WCAG 기준 위반 사항이 없습니다.");
                }
            }
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 2));
            // 병합된 셀의 테두리 설정
            RegionUtil.setBorderTop(BorderStyle.THIN, new CellRangeAddress(rowNum, rowNum, 0, 2), sheet);
            RegionUtil.setBorderBottom(BorderStyle.THIN, new CellRangeAddress(rowNum, rowNum, 0, 2), sheet);
            RegionUtil.setBorderLeft(BorderStyle.THIN, new CellRangeAddress(rowNum, rowNum, 0, 2), sheet);
            RegionUtil.setBorderRight(BorderStyle.THIN, new CellRangeAddress(rowNum, rowNum, 0, 2), sheet);
        }
        
        // 열 너비 설정
        sheet.setColumnWidth(0, 40 * 256); // WCAG 기준
        sheet.setColumnWidth(1, 15 * 256); // 위반 수
        sheet.setColumnWidth(2, 60 * 256); // 주요 문제점
    }

    private void createComponentAnalysisSheet(Sheet sheet, List<Map<String, Object>> issues, CellStyle titleStyle, CellStyle headerStyle, CellStyle defaultStyle) {
        // 제목 행
        Row titleRow = sheet.createRow(0);
        for (int i = 0; i < 6; i++) {
            Cell cell = titleRow.createCell(i);
            cell.setCellStyle(titleStyle);
            if (i == 0) {
                cell.setCellValue("컴포넌트별 접근성 분석");
            }
        }
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        titleRow.setHeight((short) 900);
        
        // 컴포넌트별 분석 헤더
        Row headerRow = sheet.createRow(2);
        String[] headers = {"컴포넌트", "총 문제 수", "대비율 문제", "키보드 접근성", "대체 텍스트", "헤딩 구조"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 컴포넌트별 이슈 집계
        Map<String, Map<String, Integer>> componentIssues = new HashMap<>();
        if (!issues.isEmpty()) {
            for (Map<String, Object> issue : issues) {
                String path = (String) issue.get("path");
                String component = path.split(" > ")[0];
                String type = (String) issue.get("type");
                
                componentIssues.computeIfAbsent(component, k -> new HashMap<>())
                    .merge(type, 1, Integer::sum);
            }
        }
        
        // 데이터 행 추가
        int rowNum = 3;
        if (!componentIssues.isEmpty()) {
            for (Map.Entry<String, Map<String, Integer>> entry : componentIssues.entrySet()) {
                Row row = sheet.createRow(rowNum++);
                Map<String, Integer> typeCounts = entry.getValue();
                
                // 컴포넌트
                Cell componentCell = row.createCell(0);
                componentCell.setCellValue(entry.getKey());
                componentCell.setCellStyle(defaultStyle);
                
                // 총 문제 수
                Cell totalCell = row.createCell(1);
                totalCell.setCellValue(typeCounts.values().stream().mapToInt(Integer::intValue).sum());
                totalCell.setCellStyle(defaultStyle);
                
                // 유형별 문제 수
                Cell contrastCell = row.createCell(2);
                contrastCell.setCellValue(typeCounts.getOrDefault("ColorContrast", 0));
                contrastCell.setCellStyle(defaultStyle);
                
                Cell keyboardCell = row.createCell(3);
                keyboardCell.setCellValue(typeCounts.getOrDefault("KeyboardAccessibility", 0));
                keyboardCell.setCellStyle(defaultStyle);
                
                Cell altTextCell = row.createCell(4);
                altTextCell.setCellValue(typeCounts.getOrDefault("AlternativeText", 0));
                altTextCell.setCellStyle(defaultStyle);
                
                Cell headingCell = row.createCell(5);
                headingCell.setCellValue(typeCounts.getOrDefault("HeadingStructure", 0));
                headingCell.setCellStyle(defaultStyle);
            }
        } else {
            Row row = sheet.createRow(rowNum);
            // 모든 셀 생성 및 스타일 적용
            for (int i = 0; i < 6; i++) {
                Cell cell = row.createCell(i);
                cell.setCellStyle(defaultStyle);
                if (i == 0) {
                    cell.setCellValue("컴포넌트별 접근성 문제가 없습니다.");
                }
            }
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 5));
            // 병합된 셀의 테두리 설정
            RegionUtil.setBorderTop(BorderStyle.THIN, new CellRangeAddress(rowNum, rowNum, 0, 5), sheet);
            RegionUtil.setBorderBottom(BorderStyle.THIN, new CellRangeAddress(rowNum, rowNum, 0, 5), sheet);
            RegionUtil.setBorderLeft(BorderStyle.THIN, new CellRangeAddress(rowNum, rowNum, 0, 5), sheet);
            RegionUtil.setBorderRight(BorderStyle.THIN, new CellRangeAddress(rowNum, rowNum, 0, 5), sheet);
        }
        
        // 열 너비 설정
        sheet.setColumnWidth(0, 40 * 256); // 컴포넌트
        sheet.setColumnWidth(1, 15 * 256); // 총 문제 수
        sheet.setColumnWidth(2, 15 * 256); // 대비율 문제
        sheet.setColumnWidth(3, 15 * 256); // 키보드 접근성
        sheet.setColumnWidth(4, 15 * 256); // 대체 텍스트
        sheet.setColumnWidth(5, 15 * 256); // 헤딩 구조
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