package com.tomatosystem.type;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import static com.tomatosystem.utill.NodeConverterUtils.*;
public class InputNodeConverter {
	   public void convert(FileWriter writer, Map<String, Object> element, String name,
               double x, double y, double width, double height,
               double parentX, double parentY, String style, int depth) throws IOException {

					String indent = "    ".repeat(depth);
					String type = (String) element.get("type");
					
					if ("INPUT".equalsIgnoreCase(type)) {
					   String inputId = "input_" + generateId();
					   writer.write(indent + "<cl:inputbox std:sid=\"inputbox-" + generateId() + "\" id=\"" + inputId + "\" style=\"" + escapeXml(style) + "\">\n");
					   writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
					   writer.write(indent + "</cl:inputbox>\n");
					}
					}
					
//					// 유틸리티 메서드들 (예시로 구현)
//					private String generateId() {
//					return UUID.randomUUID().toString().replace("-", "");
//					}
//					
//					private void writeLayoutData(FileWriter writer, double x, double y, double width, double height,
//					                        double parentX, double parentY, int depth) throws IOException {
//					// Layout Data 처리 로직
//					String indent = "    ".repeat(depth);
//					writer.write(indent + "  <cl:xylayoutdata top=\"" + (int) (y - parentY) + "px\" " +
//					       "left=\"" + (int) (x - parentX) + "px\" " +
//					       "width=\"" + (int) width + "px\" height=\"" + (int) height + "px\" " +
//					       "horizontalAnchor=\"LEFT\" verticalAnchor=\"TOP\"/>\n");
//					}
//					
//					private String escapeXml(String input) {
//					// XML 특수문자 처리
//					if (input == null) return "";
//					return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
//					}
}
