package com.tomatosystem.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ClxLayoutUtil {
    // 중복 방지용 id 저장
    private static final Set<String> usedIds = new HashSet<>();

    public static String convertFigmaJsonToClxXml(Map<String, Object> figmaJson) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:cl=\"http://tomatosystem.co.kr/cleopatra\" xmlns:std=\"http://tomatosystem.co.kr/cleopatra/studio\" std:sid=\"html-20d521fc\" version=\"1.0.5306\">\n");
            sb.append("  <head std:sid=\"head-009ed883\">\n");
            sb.append("    <std:metadata>\n");
            sb.append("      <std:property key=\"template-file\" value=\"templates/일반화면/1.그리드/V_그리드_수정X.clx\"/>\n");
            sb.append("    </std:metadata>\n");
            sb.append("    <screen std:sid=\"screen-a2592236\" id=\"PC\" name=\"PC\" width=\"1654px\" height=\"940px\" useCustomWidth=\"false\" useCustomHeight=\"false\" customHeight=\"600\" customWidth=\"800\" active=\"true\"/>\n");
            sb.append("    <screen std:sid=\"screen-eec030c4\" id=\"TABLET\" name=\"TABLET\" width=\"800px\" height=\"768px\" useCustomWidth=\"false\" useCustomHeight=\"false\" customHeight=\"768\" customWidth=\"390\"/>\n");
            sb.append("    <cl:model std:sid=\"model-de3b6abf\">\n");
            sb.append("      <cl:dataset std:sid=\"d-set-f5adb997\" id=\"dsAddFile\">\n");
            sb.append("        <cl:datacolumnlist>\n");
            sb.append("          <cl:datacolumn comment=\"파일명\" std:sid=\"d-column-96a12e43\" name=\"column1\"/>\n");
            sb.append("          <cl:datacolumn comment=\"파일크기\" std:sid=\"d-column-76c32eca\" name=\"column2\"/>\n");
            sb.append("          <cl:datacolumn comment=\"파일찾기\" std:sid=\"d-column-e00f56dd\" name=\"column3\"/>\n");
            sb.append("        </cl:datacolumnlist>\n");
            sb.append("        <cl:datarowlist/>\n");
            sb.append("      </cl:dataset>\n");
            sb.append("    </cl:model>\n");
            sb.append("    <cl:appspec/>\n");
            sb.append("  </head>\n");
            sb.append("  <body std:sid=\"body-64c95367\" class=\"content-wrapper\">\n");
            sb.append("  <cl:verticallayout std:sid=\"v-layout-78e61a9f\"/>\n");

            // Figma JSON 계층 순회 (CANVAS/FRAME/GROUP 등 중간 계층 모두 재귀)
            Map<String, Object> document = (Map<String, Object>) figmaJson.get("document");
            if (document != null) {
                List<Map<String, Object>> children = (List<Map<String, Object>>) document.get("children");
                if (children != null) {
                    for (Map<String, Object> child : children) {
                        traverseFigmaNode(sb, child, 2);
                    }
                }
            }

            sb.append("  </body>\n");
            sb.append("  <std:studiosetting>\n");
            sb.append("    <std:hruler/>\n");
            sb.append("    <std:vruler/>\n");
            sb.append("  </std:studiosetting>\n");
            sb.append("</html>\n");
        } catch (Exception e) {
            sb.append("\n<!-- XML GENERATION ERROR: " + e.getMessage() + " -->\n");
            e.printStackTrace();
        }
        String clxXml = sb.toString();
        // 태그 닫힘 검증
        if (!clxXml.trim().endsWith("</html>")) {
            System.err.println("[ClxLayoutUtil] WARNING: XML does not end with </html>!\n");
        }
        saveClxToTxtFile(clxXml, "C:/Users/LCM/git/Converter-Figma/clx-src/convertTest/변환결과.txt");
        return clxXml;
    }

    // txt 파일로 저장하는 메서드
    public static void saveClxToTxtFile(String clxXml, String filePath) {
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(new java.io.File(filePath).getParent()));
            try (java.io.FileWriter writer = new java.io.FileWriter(filePath)) {
                writer.write(clxXml);
            }
            // 디버깅용: 생성된 XML 길이와 앞/뒤 일부 출력
            System.out.println("[ClxLayoutUtil] XML length: " + (clxXml != null ? clxXml.length() : 0));
            if (clxXml != null && clxXml.length() > 200) {
                System.out.println("[ClxLayoutUtil] XML head: " + clxXml.substring(0, 200));
                System.out.println("[ClxLayoutUtil] XML tail: " + clxXml.substring(clxXml.length() - 200));
            } else if (clxXml != null) {
                System.out.println("[ClxLayoutUtil] XML: " + clxXml);
            }
        } catch (Exception e) {
            System.err.println("[ClxLayoutUtil] Failed to save file: " + filePath);
            e.printStackTrace();
        }
    }

    // 그룹 타입 판별
    private static boolean isGroupType(String type) {
        return "CANVAS".equalsIgnoreCase(type) || "FRAME".equalsIgnoreCase(type) || "GROUP".equalsIgnoreCase(type);
    }

    // 중간 계층(CANVAS/FRAME/GROUP 등) 재귀 순회
    private static void traverseFigmaNode(StringBuilder sb, Map<String, Object> node, int depth) {
        String type = (String) node.get("type");
        String name = (String) node.getOrDefault("name", "");
        List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
        String indent = "    ".repeat(depth);

        // UDC 헤더 처리
        if ("FRAME".equalsIgnoreCase(type) && name.toLowerCase().contains("title")) {
            String udcId = "ud-control-" + genId();
            sb.append(indent).append("<cl:udc std:sid=\"").append(udcId).append("\" id=\"udcComAppHeader\" type=\"udc.udcComAppHeader\">\n");
            Double height = getHeight(node);
            Double width = getWidth(node);
            sb.append(indent).append("  <cl:verticaldata std:sid=\"v-data-").append(genId()).append("\"");
            if (width != null) sb.append(" width=\"").append(width.intValue()).append("px\"");
            if (height != null) sb.append(" height=\"").append(height.intValue()).append("px\"");
            sb.append("/>\n");
            String titleText = findFirstTextValue(node);
            if (titleText != null && !titleText.isEmpty()) {
                sb.append(indent).append("  <cl:property name=\"title\" value=\"").append(escapeXml(titleText)).append("\" type=\"string\"/>\n");
            }
            sb.append(indent).append("</cl:udc>\n");
            return;
        }

        // 빈 그룹/레이아웃 생성 방지
        if (children == null || children.isEmpty()) return;

        // formlayout: leaf가 2개 이상, 실제 행/열 배치가 필요한 경우만
        if (children != null && children.size() > 1 && children.stream().allMatch(child -> child.get("children") == null)) {
            int tolerance = 20; // px
            List<List<Map<String, Object>>> rows = new ArrayList<>();
            List<Double> rowYs = new ArrayList<>();
            List<Map<String, Object>> all = new ArrayList<>(children);
            all.sort(Comparator.comparingDouble(ClxLayoutUtil::getY));
            for (Map<String, Object> item : all) {
                Double y = getY(item);
                boolean placed = false;
                for (int i = 0; i < rowYs.size(); i++) {
                    if (Math.abs(rowYs.get(i) - y) <= tolerance) {
                        rows.get(i).add(item);
                        placed = true;
                        break;
                    }
                }
                if (!placed) {
                    rowYs.add(y);
                    List<Map<String, Object>> newRow = new ArrayList<>();
                    newRow.add(item);
                    rows.add(newRow);
                }
            }
            for (List<Map<String, Object>> row : rows) {
                row.sort(Comparator.comparingDouble(ClxLayoutUtil::getX));
            }
            List<Double> rowHeightsList = new ArrayList<>();
            List<Double> colWidthsList = new ArrayList<>();
            for (List<Map<String, Object>> row : rows) {
                double maxH = 0;
                for (Map<String, Object> item : row) {
                    Double h = getHeight(item);
                    if (h != null && h > maxH) maxH = h;
                }
                rowHeightsList.add(maxH);
            }
            int colCount = rows.stream().mapToInt(List::size).max().orElse(1);
            for (int c = 0; c < colCount; c++) {
                double maxW = 0;
                for (List<Map<String, Object>> row : rows) {
                    if (c < row.size()) {
                        Double w = getWidth(row.get(c));
                        if (w != null && w > maxW) maxW = w;
                    }
                }
                colWidthsList.add(maxW);
            }
            // group + 컨트롤 + formlayout 구조
            String groupId = "grp-" + genId();
            sb.append(indent).append("<cl:group std:sid=\"group-").append(genId()).append("\" id=\"").append(groupId).append("\"");
            if (name != null && !name.isEmpty()) {
                sb.append(" class=\"").append(escapeXml(name.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase())).append("\"");
            }
            sb.append(">\n");
            Double height = getHeight(node);
            Double width = getWidth(node);
            sb.append(indent)
              .append("  <cl:verticaldata std:sid=\"v-data-")
              .append(genId()).append("\"")
              .append(width != null ? " width=\"" + width.intValue() + "px\"" : "")
              .append(height != null ? " height=\"" + height.intValue() + "px\"" : "")
              .append("/>\n");
            for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
                List<Map<String, Object>> row = rows.get(rowIdx);
                for (int colIdx = 0; colIdx < row.size(); colIdx++) {
                    Map<String, Object> item = row.get(colIdx);
                    convertLeafWithFormdata(sb, item, depth + 1, rowIdx, colIdx);
                }
            }
            sb.append(indent).append("  <cl:formlayout std:sid=\"f-layout-").append(genId()).append("\" scrollable=\"false\" hspace=\"6px\" vspace=\"6px\" top-margin=\"0px\" right-margin=\"0px\" bottom-margin=\"0px\" left-margin=\"0px\">\n");
            for (Double h : rowHeightsList) {
                if (h != null && h >= 40) {
                    sb.append(indent).append("    <cl:rows length=\"").append(h.intValue()).append("\" unit=\"PIXEL\"/>\n");
                } else {
                    sb.append(indent).append("    <cl:rows length=\"1\" unit=\"FRACTION\"/>\n");
                }
            }
            for (Double w : colWidthsList) {
                if (w != null && w >= 80) {
                    sb.append(indent).append("    <cl:columns length=\"").append(w.intValue()).append("\" unit=\"PIXEL\"/>\n");
                } else {
                    sb.append(indent).append("    <cl:columns length=\"1\" unit=\"FRACTION\"/>\n");
                }
            }
            sb.append(indent).append("  </cl:formlayout>\n");
            sb.append(indent).append("</cl:group>\n");
            return;
        }

        // group(verticallayout) 구조: 컨트롤 직접 나열
        if (isGroupType(type) && children != null && !children.isEmpty()) {
            String groupId = "grp-" + genId();
            sb.append(indent).append("<cl:group std:sid=\"group-").append(genId()).append("\" id=\"").append(groupId).append("\"");
            if (name != null && !name.isEmpty()) {
                sb.append(" class=\"").append(escapeXml(name.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase())).append("\"");
            }
            sb.append(">\n");
            Double height = getHeight(node);
            Double width = getWidth(node);
            sb.append(indent)
              .append("  <cl:verticaldata std:sid=\"v-data-")
              .append(genId()).append("\"")
              .append(width != null ? " width=\"" + width.intValue() + "px\"" : "")
              .append(height != null ? " height=\"" + height.intValue() + "px\"" : "")
              .append("/>\n");
            for (Map<String, Object> child : children) {
                traverseFigmaNode(sb, child, depth + 1);
            }
            sb.append(indent).append("</cl:group>\n");
            return;
        }

        // 컨트롤이 1개면 group으로 감싸기
        if (children != null && children.size() == 1) {
            String groupId = "grp-" + genId();
            sb.append(indent).append("<cl:group std:sid=\"group-").append(genId()).append("\" id=\"").append(groupId).append("\"");
            if (name != null && !name.isEmpty()) {
                sb.append(" class=\"").append(escapeXml(name.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase())).append("\"");
            }
            sb.append(">\n");
            Double height = getHeight(node);
            Double width = getWidth(node);
            sb.append(indent)
              .append("  <cl:verticaldata std:sid=\"v-data-")
              .append(genId()).append("\"")
              .append(width != null ? " width=\"" + width.intValue() + "px\"" : "")
              .append(height != null ? " height=\"" + height.intValue() + "px\"" : "")
              .append("/>\n");
            traverseFigmaNode(sb, children.get(0), depth + 1);
            sb.append(indent).append("</cl:group>\n");
            return;
        }
        // leaf 컨트롤 처리
        convertLeaf(sb, node, depth);
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

    // 높이/폭 추출 유틸
    private static Double getHeight(Map<String, Object> node) {
        Map<String, Object> box = (Map<String, Object>) node.get("absoluteBoundingBox");
        if (box != null && box.get("height") != null) return ((Number) box.get("height")).doubleValue();
        return null;
    }
    private static Double getWidth(Map<String, Object> node) {
        Map<String, Object> box = (Map<String, Object>) node.get("absoluteBoundingBox");
        if (box != null && box.get("width") != null) return ((Number) box.get("width")).doubleValue();
        return null;
    }

    // UDC 내부 property용: 첫 TEXT 찾기
    private static String findFirstTextValue(Map<String, Object> node) {
        String type = (String) node.get("type");
        if ("TEXT".equalsIgnoreCase(type)) {
            Object characters = node.get("characters");
            return characters != null ? characters.toString() : null;
        }
        List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
        if (children != null) {
            for (Map<String, Object> child : children) {
                String result = findFirstTextValue(child);
                if (result != null && !result.isEmpty()) {
                    return result;
                }
            }
        }
        return null;
    }

    // leaf 노드 변환 (formlayout 내부)
    private static void convertLeafWithFormdata(StringBuilder sb, Map<String, Object> node, int depth, int row, int col) {
        String type = (String) node.get("type");
        String name = (String) node.getOrDefault("name", "");
        String indent = "    ".repeat(depth);
        String formdata = "<cl:formdata std:sid=\"f-data-" + genId() + "\" row=\"" + row + "\" col=\"" + col + "\"/>";
        if ("TEXT".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:output std:sid=\"output-").append(genId()).append("\" value=\"")
              .append(escapeXml((String) node.getOrDefault("characters", ""))).append("\">\n");
            sb.append(indent).append("  ").append(formdata).append("\n");
            sb.append(indent).append("</cl:output>\n");
        } else if ("INPUT".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:inputbox std:sid=\"i-box-").append(genId()).append("\">\n");
            sb.append(indent).append("  ").append(formdata).append("\n");
            sb.append(indent).append("</cl:inputbox>\n");
        } else if ("DATEINPUT".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:dateinput std:sid=\"d-input-").append(genId()).append("\">\n");
            sb.append(indent).append("  ").append(formdata).append("\n");
            sb.append(indent).append("</cl:dateinput>\n");
        } else if ("COMBOBOX".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:combobox std:sid=\"c-box-").append(genId()).append("\">\n");
            sb.append(indent).append("  ").append(formdata).append("\n");
            sb.append(indent).append("</cl:combobox>\n");
        } else if ("SEARCHINPUT".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:searchinput std:sid=\"s-input-").append(genId()).append("\">\n");
            sb.append(indent).append("  ").append(formdata).append("\n");
            sb.append(indent).append("</cl:searchinput>\n");
        } else if ("RADIOBUTTON".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:radiobutton std:sid=\"r-button-").append(genId()).append("\">\n");
            sb.append(indent).append("  ").append(formdata).append("\n");
            sb.append(indent).append("</cl:radiobutton>\n");
        } else if ("CHECKBOXGROUP".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:checkboxgroup std:sid=\"cb-group-").append(genId()).append("\">\n");
            sb.append(indent).append("  ").append(formdata).append("\n");
            sb.append(indent).append("</cl:checkboxgroup>\n");
        } else if ("GRID".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:grid std:sid=\"grid-").append(genId()).append("\">\n");
            sb.append(indent).append("  ").append(formdata).append("\n");
            sb.append(indent).append("</cl:grid>\n");
        } else {
            writeControlXml(sb, node, depth);
        }
    }

    // leaf 노드 변환 (formlayout 외부)
    private static void convertLeaf(StringBuilder sb, Map<String, Object> node, int depth) {
        String type = (String) node.get("type");
        String name = (String) node.getOrDefault("name", "");
        String indent = "    ".repeat(depth);
        if ("TEXT".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:output std:sid=\"output-").append(genId()).append("\" value=\"")
              .append(escapeXml((String) node.getOrDefault("characters", ""))).append("\"/>\n");
        } else if ("INPUT".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:inputbox std:sid=\"i-box-").append(genId()).append("\"/>\n");
        } else if ("DATEINPUT".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:dateinput std:sid=\"d-input-").append(genId()).append("\"/>\n");
        } else if ("COMBOBOX".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:combobox std:sid=\"c-box-").append(genId()).append("\"/>\n");
        } else if ("SEARCHINPUT".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:searchinput std:sid=\"s-input-").append(genId()).append("\"/>\n");
        } else if ("RADIOBUTTON".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:radiobutton std:sid=\"r-button-").append(genId()).append("\"/>\n");
        } else if ("CHECKBOXGROUP".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:checkboxgroup std:sid=\"cb-group-").append(genId()).append("\"/>\n");
        } else if ("GRID".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:grid std:sid=\"grid-").append(genId()).append("\"/>\n");
        } else {
            writeControlXml(sb, node, depth);
        }
    }

    // 실제 컨트롤 XML만 출력 (formdata/verticaldata 없이)
    private static void writeControlXml(StringBuilder sb, Map<String, Object> node, int depth) {
        writeControlXml(sb, node, depth, (String) node.get("type"), (String) node.getOrDefault("name", ""));
    }
    private static void writeControlXml(StringBuilder sb, Map<String, Object> node, int depth, String type, String name) {
        String indent = "    ".repeat(depth);
        // INSTANCE 타입 매핑
        if ("INSTANCE".equalsIgnoreCase(type)) {
            String lowerName = name.toLowerCase();
            if (lowerName.contains("button")) type = "BUTTON";
            else if (lowerName.contains("input")) type = "INPUT";
            else if (lowerName.contains("combobox")) type = "COMBOBOX";
            else if (lowerName.contains("date")) type = "DATEINPUT";
            else if (lowerName.contains("radio")) return;
        }
        if ("TEXT".equalsIgnoreCase(type)) {
            sb.append(indent)
              .append("<cl:output std:sid=\"output-").append(genId())
              .append("\" id=\"opt-").append(genId())
              .append("\" value=\"")
              .append(escapeXml((String) node.getOrDefault("characters", "")))
              .append("\"/>\n");
        } else if ("INPUT".equalsIgnoreCase(type)) {
            sb.append(indent)
              .append("<cl:inputbox std:sid=\"i-box-").append(genId())
              .append("\" id=\"ipb-").append(genId())
              .append("\"/>\n");
        } else if ("DATEINPUT".equalsIgnoreCase(type)) {
            sb.append(indent)
              .append("<cl:dateinput std:sid=\"d-input-").append(genId())
              .append("\" id=\"dti-").append(genId())
              .append("\"/>\n");
        } else if ("COMBOBOX".equalsIgnoreCase(type)) {
            sb.append(indent)
              .append("<cl:combobox std:sid=\"c-box-").append(genId())
              .append("\" id=\"cmb-").append(genId())
              .append("\"/>\n");
        } else if ("BUTTON".equalsIgnoreCase(type)) {
            sb.append(indent)
              .append("<cl:button std:sid=\"button-").append(genId())
              .append("\" id=\"btn-").append(genId())
              .append("\" value=\"")
              .append(escapeXml(name))
              .append("\"/>\n");
        }
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
    // 외부 경로로 저장
    public static void saveClxToFile(String clxXml, String filePath) throws IOException {
        java.nio.file.Files.createDirectories(java.nio.file.Paths.get(new java.io.File(filePath).getParent()));
        try (java.io.FileWriter writer = new java.io.FileWriter(filePath)) {
            writer.write(clxXml);
        }
    }
} 