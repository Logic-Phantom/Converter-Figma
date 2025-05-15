package com.tomatosystem.utill;

import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

public class NodeConverterUtils {

    public static String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }

    public static void writeLayoutData(FileWriter writer, double x, double y, double width, double height,
                                       double parentX, double parentY, int depth) throws IOException {
        String indent = "    ".repeat(depth);
        writer.write(indent + "<cl:xylayoutdata top=\"" + (int) y + "px\" " +
                     "left=\"" + (int) x + "px\" " +
                     "width=\"" + (int) width + "px\" height=\"" + (int) height + "px\" " +
                     "horizontalAnchor=\"LEFT\" verticalAnchor=\"TOP\"/>\n");
    }

    // 텍스트 컴포넌트의 최소 크기를 계산
    public static double[] calculateTextMinSize(String text, Map<String, Object> style) {
        // 기본 최소 크기
        double minWidth = 50;  // 최소 너비
        double minHeight = 20; // 최소 높이
        
        if (text != null) {
            // 텍스트 길이에 따른 최소 너비 계산 (대략적인 계산)
            minWidth = Math.max(minWidth, text.length() * 8);  // 글자당 약 8px
            
            // 줄바꿈 문자에 따른 최소 높이 계산
            long lineCount = text.chars().filter(ch -> ch == '\n').count() + 1;
            minHeight = Math.max(minHeight, lineCount * 20);  // 줄당 20px
        }
        
        return new double[]{minWidth, minHeight};
    }
}