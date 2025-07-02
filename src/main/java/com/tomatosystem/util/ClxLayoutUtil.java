package com.tomatosystem.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ClxLayoutUtil {
    public static String convertFigmaJsonToClxXml(Map<String, Object> figmaJson) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:cl=\"http://tomatosystem.co.kr/cleopatra\" xmlns:std=\"http://tomatosystem.co.kr/cleopatra/studio\" std:sid=\"html-advanced\" version=\"1.0.0\">\n");
        sb.append("  <head std:sid=\"head-advanced\">\n");
        sb.append("    <screen std:sid=\"screen-advanced\" id=\"PC\" name=\"PC\" width=\"1654px\" height=\"940px\" useCustomWidth=\"false\" useCustomHeight=\"false\" customHeight=\"600\" customWidth=\"800\" active=\"true\"/>\n");
        sb.append("    <cl:model std:sid=\"model-advanced\"/>\n");
        sb.append("    <cl:appspec/>\n");
        sb.append("  </head>\n");
        sb.append("  <body std:sid=\"body-advanced\">\n");

        // 최상위 document/children부터 시작
        Map<String, Object> document = (Map<String, Object>) figmaJson.get("document");
        if (document != null) {
            List<Map<String, Object>> children = (List<Map<String, Object>>) document.get("children");
            if (children != null) {
                for (Map<String, Object> child : children) {
                    convertNode(sb, child, 2, 0, 0);
                }
            }
        }

        sb.append("  </body>\n");
        sb.append("  <std:studiosetting>\n");
        sb.append("    <std:hruler/>\n");
        sb.append("    <std:vruler/>\n");
        sb.append("  </std:studiosetting>\n");
        sb.append("</html>");
        return sb.toString();
    }

    // 재귀적으로 노드 변환
    private static void convertNode(StringBuilder sb, Map<String, Object> node, int depth, int row, int col) {
        String type = (String) node.get("type");
        String name = (String) node.getOrDefault("name", "");
        List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
        String indent = "    ".repeat(depth);

        // 그룹/프레임이면 내부 children 구조 분석
        if ("FRAME".equalsIgnoreCase(type) || "GROUP".equalsIgnoreCase(type)) {
            if (children != null && !children.isEmpty()) {
                // 폼 구조(라벨+입력 등 반복)인지, 세로 나열인지, 자유 배치인지 판단
                boolean isForm = isFormLayout(children);
                boolean isVertical = isVerticalLayout(children);
                boolean hasButton = hasButton(children);
                if (isForm) {
                    // 폼 레이아웃
                    sb.append(indent).append("<cl:group std:sid=\"group-").append(genId()).append("\" id=\"grp-").append(genId()).append("\">\n");
                    sb.append(indent).append("  <cl:formlayout std:sid=\"f-layout-").append(genId()).append("\" scrollable=\"false\" hspace=\"6px\" vspace=\"6px\">\n");
                    int rowIdx = 0;
                    for (List<Map<String, Object>> rowGroup : groupByRow(children)) {
                        int colIdx = 0;
                        for (Map<String, Object> child : rowGroup) {
                            convertFormChild(sb, child, depth + 2, rowIdx, colIdx);
                            colIdx++;
                        }
                        rowIdx++;
                    }
                    sb.append(indent).append("  </cl:formlayout>\n");
                    // 버튼류는 하단에 별도 verticallayout으로 배치
                    if (hasButton) {
                        sb.append(indent).append("  <cl:verticallayout std:sid=\"v-layout-btn-").append(genId()).append("\" spacing=\"10\">\n");
                        for (Map<String, Object> child : children) {
                            if (isButton(child)) {
                                convertLeaf(sb, child, depth + 3, 0, 0);
                            }
                        }
                        sb.append(indent).append("  </cl:verticallayout>\n");
                    }
                    sb.append(indent).append("</cl:group>\n");
                } else if (isVertical) {
                    // 버티컬 레이아웃
                    sb.append(indent).append("<cl:group std:sid=\"group-").append(genId()).append("\" id=\"grp-").append(genId()).append("\">\n");
                    sb.append(indent).append("  <cl:verticallayout std:sid=\"v-layout-").append(genId()).append("\" spacing=\"12\"/>\n");
                    for (Map<String, Object> child : children) {
                        convertNode(sb, child, depth + 2, 0, 0);
                    }
                    sb.append(indent).append("</cl:group>\n");
                } else {
                    // 자유 배치 (XY)
                    sb.append(indent).append("<cl:group std:sid=\"group-").append(genId()).append("\" id=\"grp-").append(genId()).append("\">\n");
                    for (Map<String, Object> child : children) {
                        convertNode(sb, child, depth + 1, 0, 0);
                    }
                    sb.append(indent).append("</cl:group>\n");
                }
                return;
            }
        }
        // leaf 노드(텍스트, 인풋 등)
        convertLeaf(sb, node, depth, row, col);
    }

    // 폼 구조(라벨+입력 등 반복)인지 판별 (간단히: children이 2개씩 쌍으로 반복)
    private static boolean isFormLayout(List<Map<String, Object>> children) {
        if (children.size() < 2) return false;
        // 2개씩 쌍으로 반복되는지 확인
        for (int i = 0; i < children.size(); i += 2) {
            if (i + 1 >= children.size()) return false;
            String t1 = (String) children.get(i).get("type");
            String t2 = (String) children.get(i + 1).get("type");
            if (!"TEXT".equalsIgnoreCase(t1)) return false;
            if (!"INPUT".equalsIgnoreCase(t2) && !"DATEINPUT".equalsIgnoreCase(t2) && !"COMBOBOX".equalsIgnoreCase(t2)) return false;
        }
        return true;
    }
    // 세로 나열인지 판별 (모든 children이 TEXT/INPUT/DATEINPUT/COMBOBOX/BUTTON 등 단일 열)
    private static boolean isVerticalLayout(List<Map<String, Object>> children) {
        Set<String> allowed = new HashSet<>(Arrays.asList("TEXT", "INPUT", "DATEINPUT", "COMBOBOX", "BUTTON"));
        for (Map<String, Object> child : children) {
            String t = (String) child.get("type");
            if (!allowed.contains(t != null ? t.toUpperCase() : "")) return false;
        }
        return true;
    }
    // row별로 그룹핑 (폼 구조)
    private static List<List<Map<String, Object>>> groupByRow(List<Map<String, Object>> children) {
        List<List<Map<String, Object>>> rows = new ArrayList<>();
        for (int i = 0; i < children.size(); i += 2) {
            List<Map<String, Object>> row = new ArrayList<>();
            row.add(children.get(i));
            if (i + 1 < children.size()) row.add(children.get(i + 1));
            rows.add(row);
        }
        return rows;
    }
    // 폼 레이아웃의 자식 변환 (row/colindex)
    private static void convertFormChild(StringBuilder sb, Map<String, Object> node, int depth, int row, int col) {
        String type = (String) node.get("type");
        String indent = "    ".repeat(depth);
        if ("TEXT".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:output std:sid=\"output-").append(genId()).append("\" id=\"opt-").append(genId()).append("\" value=\"")
              .append(escapeXml((String) node.getOrDefault("characters", ""))).append("\">\n");
            sb.append(indent).append("  <cl:formdata std:sid=\"f-data-").append(genId()).append("\" row=\"").append(row).append("\" col=\"").append(col).append(""/>)\n");
            sb.append(indent).append("</cl:output>\n");
        } else if ("INPUT".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:inputbox std:sid=\"i-box-").append(genId()).append("\" id=\"ipb-").append(genId()).append("\">\n");
            sb.append(indent).append("  <cl:formdata std:sid=\"f-data-").append(genId()).append("\" row=\"").append(row).append("\" col=\"").append(col).append(""/>)\n");
            sb.append(indent).append("</cl:inputbox>\n");
        } else if ("DATEINPUT".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:dateinput std:sid=\"d-input-").append(genId()).append("\" id=\"dti-").append(genId()).append("\">\n");
            sb.append(indent).append("  <cl:formdata std:sid=\"f-data-").append(genId()).append("\" row=\"").append(row).append("\" col=\"").append(col).append(""/>)\n");
            sb.append(indent).append("</cl:dateinput>\n");
        } else if ("COMBOBOX".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:combobox std:sid=\"c-box-").append(genId()).append("\" id=\"cmb-").append(genId()).append("\">\n");
            sb.append(indent).append("  <cl:formdata std:sid=\"f-data-").append(genId()).append("\" row=\"").append(row).append("\" col=\"").append(col).append(""/>)\n");
            sb.append(indent).append("</cl:combobox>\n");
        }
    }
    // leaf 노드 변환
    private static void convertLeaf(StringBuilder sb, Map<String, Object> node, int depth, int row, int col) {
        String type = (String) node.get("type");
        String indent = "    ".repeat(depth);
        if ("TEXT".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:output std:sid=\"output-").append(genId()).append("\" id=\"opt-").append(genId()).append("\" value=\"")
              .append(escapeXml((String) node.getOrDefault("characters", ""))).append("\"/>)\n");
        } else if ("INPUT".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:inputbox std:sid=\"i-box-").append(genId()).append("\" id=\"ipb-").append(genId()).append("\"/>)\n");
        } else if ("DATEINPUT".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:dateinput std:sid=\"d-input-").append(genId()).append("\" id=\"dti-").append(genId()).append("\"/>)\n");
        } else if ("COMBOBOX".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:combobox std:sid=\"c-box-").append(genId()).append("\" id=\"cmb-").append(genId()).append("\"/>)\n");
        } else if ("BUTTON".equalsIgnoreCase(type)) {
            sb.append(indent).append("<cl:button std:sid=\"button-").append(genId()).append("\" id=\"btn-").append(genId()).append("\" value=\"")
              .append(escapeXml((String) node.getOrDefault("characters", "버튼"))).append("\"/>)\n");
        }
    }
    // 버튼 포함 여부
    private static boolean hasButton(List<Map<String, Object>> children) {
        for (Map<String, Object> child : children) {
            if (isButton(child)) return true;
        }
        return false;
    }
    private static boolean isButton(Map<String, Object> node) {
        String type = (String) node.get("type");
        return "BUTTON".equalsIgnoreCase(type);
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
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(clxXml);
        }
    }
} 