package com.tomatosystem.type;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static com.tomatosystem.utill.NodeConverterUtils.*;

public class TextNodeConverter {

//    public void convert(FileWriter writer, Map<String, Object> element,
//                        double x, double y, double width, double height,
//                        double parentX, double parentY, String style, int depth) throws IOException {
//
//        String indent = "    ".repeat(depth);
//        String textId = "output_" + generateId();
//        String textValue = getTextValue(element);
//
//        writer.write(indent + "<cl:output std:sid=\"output-" + generateId() + "\" id=\"" + textId + "\" value=\"" +
//                escapeXml(textValue) + "\" style=\"" + escapeXml(style) + "\">\n");
//        writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
//        writer.write(indent + "</cl:output>\n");
//    }

    public void convert(FileWriter writer, Map<String, Object> element,
            double x, double y, double width, double height,
            double parentX, double parentY, String style, int depth) throws IOException {

		String indent = "    ".repeat(depth);
		String textId = "output_" + generateId();
		String textValue = getTextValue(element);
		
		writer.write(indent + "<cl:output std:sid=\"output-" + generateId() + "\" id=\"" + textId +
		         "\" value=\"" + escapeXml(textValue) + "\" style=\"" + escapeXml(style) + "\">\n");
		
		writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
		writer.write(indent + "</cl:output>\n");
	}
    
    private String getTextValue(Map<String, Object> element) {
        // characters 필드에서 텍스트 추출
        Object charactersObj = element.get("characters");
        return charactersObj != null ? charactersObj.toString() : "";
    }

//    private void writeLayoutData(FileWriter writer, double elementX, double elementY, double width, double height,
//                                 double parentX, double parentY, int depth) throws IOException {
//        double relativeX = elementX - parentX;
//        double relativeY = elementY - parentY;
//
//        String indent = "    ".repeat(depth);
//        writer.write(indent + "<cl:xylayoutdata " +
//                "top=\"" + relativeY + "px\" " +
//                "left=\"" + relativeX + "px\" " +
//                "width=\"" + width + "px\" " +
//                "height=\"" + height + "px\" " +
//                "horizontalAnchor=\"LEFT\" " +
//                "verticalAnchor=\"TOP\"/>\n");
//    }
//
//    private String escapeXml(String input) {
//        return input == null ? "" : input.replace("&", "&amp;")
//                                         .replace("<", "&lt;")
//                                         .replace(">", "&gt;")
//                                         .replace("\"", "&quot;");
//    }
//
//    private String generateId() {
//        return UUID.randomUUID().toString().substring(0, 8);
//    }
}
