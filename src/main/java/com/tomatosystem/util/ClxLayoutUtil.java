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
        return sb.toString();
    }

    // 중간 계층(CANVAS/FRAME/GROUP 등) 재귀 순회
    private static void traverseFigmaNode(StringBuilder sb, Map<String, Object> node, int depth) {
        String type = (String) node.get("type");
        List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
        // CANVAS/FRAME/GROUP 등은 내부 children 재귀
        if (("CANVAS".equalsIgnoreCase(type) || "FRAME".equalsIgnoreCase(type) || "GROUP".equalsIgnoreCase(type)) && children != null) {
            for (Map<String, Object> child : children) {
                traverseFigmaNode(sb, child, depth + 1);
            }
            return;
        }
        // leaf 노드(텍스트, 인풋 등)는 폼/버티컬/버튼 등으로 변환
        convertLeaf(sb, node, depth);
    }

    // leaf 노드 변환 (폼/버티컬/버튼 등)
    private static void convertLeaf(StringBuilder sb, Map<String, Object> node, int depth) {
        String type = (String) node.get("type");
        String name = (String) node.getOrDefault("name", "");
        String indent = "    ".repeat(depth);
        // 예시: TEXT → output, INPUT → inputbox, BUTTON → button 등
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
              .append(escapeXml(name)).append("\"/>)\n");
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