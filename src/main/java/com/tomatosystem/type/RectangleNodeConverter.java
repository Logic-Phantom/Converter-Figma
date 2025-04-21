package com.tomatosystem.type;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RectangleNodeConverter {

    // Rectangle 타입을 <cl:group>으로 변환
    public boolean convert(FileWriter writer, Map<String, Object> element, String name, 
                            double x, double y, double width, double height, 
                            double parentX, double parentY, String style, int depth) throws IOException {
        
        String indent = "    ".repeat(depth);
        
        // ✅ 일반 그룹 처리
        String groupId = "group_" + generateId();
        writer.write(indent + "<cl:group std:sid=\"group-" + generateId() + "\" id=\"" + groupId + "\" style=\"" + escapeXml(style) + "\">\n");

        // 좌표 및 크기 정보 작성
        writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
        
        // 자식 요소는 없다고 가정
        // ✅ <cl:group> 닫는 태그
        writer.write(indent + "</cl:group>\n");
        
        return true; // 자식 요소가 없으므로 더 이상 처리할 내용은 없음
    }

    private String generateId() {
        return UUID.randomUUID().toString();
    }

    private void writeLayoutData(FileWriter writer, double x, double y, double width, double height,
                                 double parentX, double parentY, int depth) throws IOException {
        String indent = "    ".repeat(depth);
        writer.write(indent + "<cl:xylayoutdata std:sid=\"" + generateId() + "\" top=\"" + (int) (y - parentY) + "px\" left=\"" + (int) (x - parentX) + "px\" width=\"" + (int) width + "px\" height=\"" + (int) height + "px\" horizontalAnchor=\"LEFT\" verticalAnchor=\"TOP\"/>\n");
    }

    private String escapeXml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}