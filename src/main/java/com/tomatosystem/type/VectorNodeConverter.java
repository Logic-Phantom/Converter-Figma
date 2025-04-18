package com.tomatosystem.type;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class VectorNodeConverter {

    public boolean convert(FileWriter writer, Map<String, Object> element, String name,
                           double x, double y, double width, double height,
                           double parentX, double parentY, String style, int depth) throws IOException {

        String indent = "    ".repeat(depth);
        String type = (String) element.get("type");

        if ("VECTOR".equalsIgnoreCase(type) || "IMAGE".equalsIgnoreCase(type)) {
            String imgId = "img_" + generateId();
            writer.write(indent + "<cl:img std:sid=\"img-" + generateId() + "\" id=\"" + imgId + "\" style=\"" + escapeXml(style) + "\">\n");
            writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);

            // VECTOR 타입 처리
            if ("VECTOR".equalsIgnoreCase(type)) {
                writer.write(indent + "  <cl:vector type=\"vector\" />\n");
            }
            // IMAGE 타입 처리
            else if ("IMAGE".equalsIgnoreCase(type)) {
                writer.write(indent + "  <cl:image src=\"" + escapeXml((String) element.get("imageUrl")) + "\" />\n");
            }

            writer.write(indent + "</cl:img>\n");
            return false; // 닫는 태그가 자동으로 처리됨
        }

        return true; // 다른 타입은 기본적으로 처리되지 않음
    }

    // 유틸리티 메서드들 (예시로 구현)
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void writeLayoutData(FileWriter writer, double x, double y, double width, double height,
                                 double parentX, double parentY, int depth) throws IOException {
        // Layout Data 처리 로직
        String indent = "    ".repeat(depth);
        writer.write(indent + "  <cl:xylayoutdata top=\"" + (int) (y - parentY) + "px\" " +
                "left=\"" + (int) (x - parentX) + "px\" " +
                "width=\"" + (int) width + "px\" height=\"" + (int) height + "px\" " +
                "horizontalAnchor=\"LEFT\" verticalAnchor=\"TOP\"/>\n");
    }

    private String escapeXml(String input) {
        // XML 특수문자 처리
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
