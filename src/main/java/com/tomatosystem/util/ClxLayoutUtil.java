package com.tomatosystem.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ClxLayoutUtil {
    public static String convertFigmaJsonToClxXml(Map<String, Object> figmaJson) {
        StringBuilder sb = new StringBuilder();
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
        String clxXml = sb.toString();
        System.out.println(clxXml); // 변환된 XML을 콘솔에 출력 (디버깅용)
        return clxXml;
    }

    // 중간 계층(CANVAS/FRAME/GROUP 등) 재귀 순회
    private static void traverseFigmaNode(StringBuilder sb, Map<String, Object> node, int depth) {
        String type = (String) node.get("type");
        String name = (String) node.getOrDefault("name", "");
        List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
        String indent = "    ".repeat(depth);
        boolean isRoot = depth == 2; // body 바로 아래면 루트

        // UDC 헤더 처리: FRAME이고 이름에 title이 포함된 경우
        if ("FRAME".equalsIgnoreCase(type) && name.toLowerCase().contains("title")) {
            String udcId = "ud-control-" + genId();
            sb.append(indent).append("<cl:udc std:sid=\"").append(udcId).append("\" id=\"udcComAppHeader\" type=\"udc.udcComAppHeader\">\n");
            Double height = getHeight(node);
            Double width = getWidth(node);
            sb.append(indent).append("  <cl:verticaldata std:sid=\"v-data-").append(genId()).append("\"");
            if (width != null) sb.append(" width=\"").append(width.intValue()).append("px\"");
            if (height != null) sb.append(" height=\"").append(height.intValue()).append("px\"");
            sb.append("/>\n");
            // UDC 내부 property: 첫 TEXT 추출
            String titleText = findFirstTextValue(node);
            if (titleText != null && !titleText.isEmpty()) {
                sb.append(indent).append("  <cl:property name=\"title\" value=\"").append(escapeXml(titleText)).append("\" type=\"string\"/>\n");
            }
            sb.append(indent).append("</cl:udc>\n");
            return;
        }
        // 그룹핑이 필요한 경우만 <cl:group>+<cl:formlayout> 생성
        if (("CANVAS".equalsIgnoreCase(type) || "FRAME".equalsIgnoreCase(type) || "GROUP".equalsIgnoreCase(type)) && children != null && !children.isEmpty()) {
            // <cl:group> 생성 (루트 또는 그룹핑 필요시)
            sb.append(indent).append("<cl:group std:sid=\"group-").append(genId()).append("\" id=\"grp-").append(genId()).append("\"");
            // class/id 매핑
            if (name != null && !name.isEmpty()) {
                sb.append(" class=\"").append(name.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase()).append("\"");
            }
            sb.append(">\n");
            // verticaldata
            Double height = getHeight(node);
            Double width = getWidth(node);
            sb.append(indent)
              .append("  <cl:verticaldata std:sid=\"v-data-")
              .append(genId()).append("\"")
              .append(width != null ? " width=\"" + width.intValue() + "px\"" : "")
              .append(height != null ? " height=\"" + height.intValue() + "px\"" : "")
              .append("/>\n");
            // attribute 예시: name에 'search' 포함시 search-box 등
            if (name.toLowerCase().contains("search")) {
                sb.append(indent).append("  <cl:attribute name=\"mobile-column-count\" value=\"2\"/>\n");
                sb.append(indent).append("  <cl:attribute name=\"tablet-column-count\" value=\"2\"/>\n");
            }
            // row/col 그룹핑 (formlayout 필요 여부 판단)
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
            // 행/열별 크기 추정
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
            // formlayout 필요 여부: leaf 컨트롤이 하나라도 있으면 생성
            boolean hasLeaf = false;
            List<Map<String, Object>> groupChildren = new ArrayList<>();
            for (List<Map<String, Object>> row : rows) {
                for (Map<String, Object> item : row) {
                    if (item.get("children") == null) hasLeaf = true;
                    else groupChildren.add(item);
                }
            }
            if (hasLeaf) {
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
                for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
                    List<Map<String, Object>> row = rows.get(rowIdx);
                    for (int colIdx = 0; colIdx < row.size(); colIdx++) {
                        Map<String, Object> item = row.get(colIdx);
                        if (item.get("children") == null) {
                            convertLeafWithFormdata(sb, item, depth + 2, rowIdx, colIdx);
                        }
                    }
                }
                sb.append(indent).append("  </cl:formlayout>\n");
            }
            // formlayout 바깥에 그룹들 생성
            for (Map<String, Object> groupChild : groupChildren) {
                traverseFigmaNode(sb, groupChild, depth + 1);
            }
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

    // leaf 노드 변환 (폼/버티컬/버튼 등) + formdata
    private static void convertLeafWithFormdata(StringBuilder sb, Map<String, Object> node, int depth, int row, int col) {
        String type = (String) node.get("type");
        String name = (String) node.getOrDefault("name", "");
        String indent = "    ".repeat(depth);
        String formdata = "<cl:formdata std:sid=\"f-data-" + genId() + "\" row=\"" + row + "\" col=\"" + col + "\"/>";
        Double height = getHeight(node);
        Double width = getWidth(node);
        // leaf 컨트롤만 <cl:formdata>와 함께 출력
        if ("TEXT".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:output std:sid=\"output-").append(genId()).append("\" id=\"opt-").append(genId()).append("\" value=\"")
              .append(escapeXml((String) node.getOrDefault("characters", ""))).append("\">\n");
            sb.append(indent).append("  ").append(formdata).append("\n");
            sb.append(indent).append("</cl:output>\n");
        } else if ("INPUT".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:inputbox std:sid=\"i-box-").append(genId()).append("\" id=\"ipb-").append(genId()).append("\">\n");
            sb.append(indent).append("  ").append(formdata).append("\n");
            sb.append(indent).append("</cl:inputbox>\n");
        } else if ("DATEINPUT".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:dateinput std:sid=\"d-input-").append(genId()).append("\" id=\"dti-").append(genId()).append("\">\n");
            sb.append(indent).append("  ").append(formdata).append("\n");
            sb.append(indent).append("</cl:dateinput>\n");
        } else if ("COMBOBOX".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:combobox std:sid=\"c-box-").append(genId()).append("\" id=\"cmb-").append(genId()).append("\">\n");
            sb.append(indent).append("  ").append(formdata).append("\n");
            sb.append(indent).append("</cl:combobox>\n");
        } else if ("BUTTON".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:button std:sid=\"button-").append(genId()).append("\" id=\"btn-").append(genId()).append("\" value=\"")
              .append(escapeXml(name)).append("\">\n");
            sb.append(indent).append("  ").append(formdata).append("\n");
            sb.append(indent).append("</cl:button>\n");
        } else {
            // 기타 컨트롤은 기존 방식대로 처리
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

    // leaf 노드 변환 (폼/버티컬/버튼 등)
    private static void convertLeaf(StringBuilder sb, Map<String, Object> node, int depth) {
        String type = (String) node.get("type");
        String name = (String) node.getOrDefault("name", "");
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
                    .replace("\"", "&quot;");
    }
    // 유틸: 랜덤 ID
    private static String genId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
    // 외부 경로로 저장
    public static void saveClxToFile(String clxXml, String filePath) throws IOException {
        java.nio.file.Files.createDirectories(java.nio.file.Paths.get(new java.io.File(filePath).getParent()));
        try (java.io.FileWriter writer = new java.io.FileWriter(filePath)) {
            writer.write(clxXml);
        }
    }
} 