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
					
					// 입력 필드의 최소 크기 설정
					double minWidth = 100;  // 최소 너비
					double minHeight = 30;  // 최소 높이
					
					// 크기 조정
					width = Math.max(width, minWidth);
					height = Math.max(height, minHeight);
					
					// 부모 컨테이너를 벗어나지 않도록 조정
					if (parentX > 0 && parentY > 0) {
						width = Math.min(width, parentX);
						height = Math.min(height, parentY);
					}
					
					if ("INPUT".equalsIgnoreCase(type)) {
					   String inputId = "input_" + generateId();
					   writer.write(indent + "<cl:inputbox std:sid=\"inputbox-" + generateId() + "\" id=\"" + inputId + "\" style=\"" + escapeXml(style) + "\">\n");
					   writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
					   writer.write(indent + "</cl:inputbox>\n");
					}
			}
					
}
