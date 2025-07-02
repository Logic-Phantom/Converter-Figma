package com.tomatosystem.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ClxLayoutUtil {
    // 중복 방지용 id 저장
    private static final Set<String> usedIds = new HashSet<>();

    /**
     * Figma JSON을 CLEOPATRA XML로 변환 (전체 구조)
     */
    public static String convertFigmaJsonToClxXml(Map<String, Object> figmaJson) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:cl=\"http://tomatosystem.co.kr/cleopatra\" xmlns:std=\"http://tomatosystem.co.kr/cleopatra/studio\" std:sid=\"html-").append(genId()).append("\" version=\"1.0.0000\">\n");
            sb.append("  <head std:sid=\"head-").append(genId()).append("\">\n");
            parseMetadata(sb, figmaJson);
            parseScreens(sb, figmaJson);
            parseModel(sb, figmaJson);
            sb.append("    <cl:appspec/>\n");
            sb.append("  </head>\n");
            sb.append("  <body std:sid=\"body-").append(genId()).append("\" class=\"content-wrapper\">\n");
            parseBody(sb, figmaJson);
            sb.append("  </body>\n");
            sb.append("  <std:studiosetting>\n    <std:hruler/>\n    <std:vruler/>\n  </std:studiosetting>\n");
            sb.append("</html>\n");
        } catch (Exception e) {
            sb.append("\n<!-- XML GENERATION ERROR: " + e.getMessage() + " -->\n");
            e.printStackTrace();
        }
        String clxXml = sb.toString();
        saveClxToTxtFile(clxXml, "C:/Users/LCM/git/Converter-Figma/clx-src/convertTest/변환결과.txt");
        return clxXml;
    }

    // <std:metadata> 등 헤더 메타데이터
    private static void parseMetadata(StringBuilder sb, Map<String, Object> figmaJson) {
        sb.append("    <std:metadata>\n");
        sb.append("      <std:property key=\"template-file\" value=\"templates/일반화면/1.그리드/V_그리드_수정X.clx\"/>\n");
        sb.append("    </std:metadata>\n");
    }

    // <screen> 여러 개 생성 (예시: PC, TABLET 등)
    private static void parseScreens(StringBuilder sb, Map<String, Object> figmaJson) {
        // 실제로는 figmaJson에서 화면별 정보 추출 필요
        sb.append("    <screen std:sid=\"screen-").append(genId()).append("\" id=\"PC\" name=\"PC\" width=\"1920px\" height=\"1080px\" useCustomWidth=\"false\" useCustomHeight=\"false\" customHeight=\"600\" customWidth=\"800\" active=\"true\"/>\n");
        sb.append("    <screen std:sid=\"screen-").append(genId()).append("\" id=\"EXB-DIV\" name=\"EXB-DIV\" width=\"1024px\" height=\"860px\"/>\n");
        sb.append("    <screen std:sid=\"screen-").append(genId()).append("\" id=\"EXB-PART\" name=\"EXB-PART\" width=\"768px\" height=\"860px\"/>\n");
        sb.append("    <screen std:sid=\"screen-").append(genId()).append("\" id=\"EXB-POP\" name=\"EXB-POP\" width=\"480px\" height=\"580px\"/>\n");
    }

    // <cl:model> 등 데이터셋/맵/서브미션 (예시)
    private static void parseModel(StringBuilder sb, Map<String, Object> figmaJson) {
        sb.append("    <cl:model std:sid=\"model-").append(genId()).append("\">\n");
        sb.append("      <cl:dataset std:sid=\"d-set-").append(genId()).append("\" id=\"dsBaseCardList\">\n");
        sb.append("        <cl:datacolumnlist>\n");
        sb.append("          <cl:datacolumn comment=\"예산연도\" std:sid=\"d-column-").append(genId()).append("\" info=\"예산연도\" name=\"yyyy\"/>\n");
        sb.append("          <cl:datacolumn comment=\"보건소장위임여부\" std:sid=\"d-column-").append(genId()).append("\" name=\"cardRegNo\" datatype=\"string\"/>\n");
        sb.append("        </cl:datacolumnlist>\n");
        sb.append("      </cl:dataset>\n");
        sb.append("      <cl:datamap comment=\"조회조건 dm\" std:sid=\"d-map-").append(genId()).append("\" id=\"dmBaseCardListForm\">\n");
        sb.append("        <cl:datacolumnlist>\n");
        sb.append("          <cl:datacolumn std:sid=\"d-column-").append(genId()).append("\" name=\"psnYyyy\"/>\n");
        sb.append("          <cl:datacolumn std:sid=\"d-column-").append(genId()).append("\" name=\"phcSym\"/>\n");
        sb.append("        </cl:datacolumnlist>\n");
        sb.append("      </cl:datamap>\n");
        sb.append("      <cl:submission std:sid=\"submission-").append(genId()).append("\" id=\"sub1\" action=\"/regist/selectPersonCardList.do\" mediatype=\"application/json\">\n");
        sb.append("        <cl:requestdata dataid=\"dmBaseCardListForm\"/>\n");
        sb.append("        <cl:responsedata dataid=\"dsBaseCardList\"/>\n");
        sb.append("      </cl:submission>\n");
        sb.append("    </cl:model>\n");
    }

    // <body> 내부 변환 (그룹/컨트롤/레이아웃)
    private static void parseBody(StringBuilder sb, Map<String, Object> figmaJson) {
        Map<String, Object> document = (Map<String, Object>) figmaJson.get("document");
        if (document != null) {
            List<Map<String, Object>> children = (List<Map<String, Object>>) document.get("children");
            if (children != null) {
                // 그룹핑 및 row/col 자동 배치
                List<Map<String, Object>> textNodes = children.stream()
                        .flatMap(node -> flattenTextNodes(node).stream())
                        .collect(Collectors.toList());
                List<List<Map<String, Object>>> rows = groupByRow(textNodes, 30.0); // tolerance 30px
                for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
                    List<Map<String, Object>> row = rows.get(rowIdx);
                    row.sort(Comparator.comparingDouble(ClxLayoutUtil::getX));
                    for (int colIdx = 0; colIdx < row.size(); colIdx++) {
                        Map<String, Object> node = row.get(colIdx);
                        parseTextNode(sb, node, rowIdx, colIdx);
                    }
                }
            }
        }
    }

    // TEXT 노드만 flatten
    private static List<Map<String, Object>> flattenTextNodes(Map<String, Object> node) {
        List<Map<String, Object>> result = new ArrayList<>();
        if ("TEXT".equalsIgnoreCase((String) node.get("type"))) {
            result.add(node);
        }
        List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
        if (children != null) {
            for (Map<String, Object> child : children) {
                result.addAll(flattenTextNodes(child));
            }
        }
        return result;
    }

    // y좌표로 행 그룹핑
    private static List<List<Map<String, Object>>> groupByRow(List<Map<String, Object>> nodes, double tolerance) {
        List<List<Map<String, Object>>> rows = new ArrayList<>();
        List<Double> rowYs = new ArrayList<>();
        nodes.sort(Comparator.comparingDouble(ClxLayoutUtil::getY));
        for (Map<String, Object> node : nodes) {
            Double y = getY(node);
            boolean placed = false;
            for (int i = 0; i < rowYs.size(); i++) {
                if (Math.abs(rowYs.get(i) - y) <= tolerance) {
                    rows.get(i).add(node);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                rowYs.add(y);
                List<Map<String, Object>> newRow = new ArrayList<>();
                newRow.add(node);
                rows.add(newRow);
            }
        }
        return rows;
    }

    // TEXT 노드 → <cl:output> 변환
    private static void parseTextNode(StringBuilder sb, Map<String, Object> node, int row, int col) {
        String nodeId = (String) node.get("id");
        String value = (String) node.getOrDefault("characters", "");
        String name = (String) node.getOrDefault("name", "");
        String className = "label"; // 필요시 스타일/이름에 따라 동적으로
        String indent = "    ";
        sb.append(indent)
          .append("<cl:output std:sid=\"output-").append(genId()).append("\"")
          .append(" id=\"opt-").append(nodeId).append("\"")
          .append(" class=\"").append(className).append("\"")
          .append(" value=\"").append(escapeXml(value)).append("\">\n");
        sb.append(indent)
          .append("  <cl:formdata std:sid=\"f-data-").append(genId()).append("\" row=\"")
          .append(row).append("\" col=\"").append(col).append("\"/>")
          .append("\n");
        sb.append(indent).append("</cl:output>\n");
    }

    // 좌표 추출 유틸
    private static Double getY(Map<String, Object> node) {
        Map<String, Object> box = (Map<String, Object>) node.get("absoluteBoundingBox");
        if (box != null && box.get("y") != null) return ((Number) box.get("y")).doubleValue();
        return 0.0;
    }
    private static Double getX(Map<String, Object> node) {
        Map<String, Object> box = (Map<String, Object>) node.get("absoluteBoundingBox");
        if (box != null && box.get("x") != null) return ((Number) box.get("x")).doubleValue();
        return 0.0;
    }

    // 유틸: XML 이스케이프
    private static String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }
    // 유틸: 랜덤 ID
    private static String genId() {
        String uuid;
        do {
            uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        } while (usedIds.contains(uuid));
        usedIds.add(uuid);
        return uuid;
    }
    // txt 파일로 저장하는 메서드
    public static void saveClxToTxtFile(String clxXml, String filePath) {
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(new java.io.File(filePath).getParent()));
            try (java.io.FileWriter writer = new java.io.FileWriter(filePath)) {
                writer.write(clxXml);
            }
            System.out.println("[ClxLayoutUtil] XML length: " + (clxXml != null ? clxXml.length() : 0));
        } catch (Exception e) {
            System.err.println("[ClxLayoutUtil] Failed to save file: " + filePath);
            e.printStackTrace();
        }
    }
} 