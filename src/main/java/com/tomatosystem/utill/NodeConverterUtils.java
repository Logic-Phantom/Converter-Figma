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
        writer.write(indent + "<cl:xylayoutdata top=\"" + (int) (y - parentY) + "px\" " +
                     "left=\"" + (int) (x - parentX) + "px\" " +
                     "width=\"" + (int) width + "px\" height=\"" + (int) height + "px\" " +
                     "horizontalAnchor=\"LEFT\" verticalAnchor=\"TOP\"/>\n");
    }
}