package com.tomatosystem.type;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InstanceNodeConverter {

	public void convert(FileWriter writer, Map<String, Object> element, String name,
            double x, double y, double width, double height,
            double parentX, double parentY, String style, int depth)
			throws IOException {
			
			// 자식 요소가 있는지 확인
			List<Map<String, Object>> children = (List<Map<String, Object>>) element.get("children");
			
			// 부모 이름 처리
			Object parentNameObj = element.get("parentName");
			String parentName = (parentNameObj instanceof String) ? ((String) parentNameObj).toLowerCase() : "";
			
			// 오른쪽에 벡터가 있는지 확인
			boolean hasVectorInRight = false;
			if (children != null) {
			for (Map<String, Object> child : children) {
			   String childName = (String) child.getOrDefault("name", "");
			   if (childName.toLowerCase().contains("right")) {
			       hasVectorInRight = hasVectorDeepInRight(child);
			       break;
			   }
			}
			}
			
			// 이름 소문자로 변환
			String lowerName = name.toLowerCase();
			String instanceId = "instance_" + generateId();
			String instanceStyle = extractStyle(element);
			String instanceValue = getButtonValue(element);
			
//			// 디버깅: 네임과 부모 이름 출력
//			System.out.println("Name: " + name);
//			System.out.println("Parent Name: " + parentName);
//			System.out.println("Has Vector in Right: " + hasVectorInRight);
			
			// ComboBox 처리
			if (lowerName.contains("combobox") || parentName.contains("combobox") ||
			lowerName.contains("selectbox") || parentName.contains("selectbox") ||
			(lowerName.contains("base-input") && hasVectorInRight)) {
			
			writer.write("    ".repeat(depth) + "<cl:combobox std:sid=\"c-box-" + generateId() + "\" id=\"" + instanceId + "\" style=\"" + escapeXml(instanceStyle) + "\">\n");
			writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
			writer.write("    ".repeat(depth) + "</cl:combobox>\n");
			return;
			}
			
			// InputBox 처리
			if (lowerName.contains("base-input") || parentName.contains("input")) {
			writer.write("    ".repeat(depth) + "<cl:inputbox std:sid=\"inputbox-" + generateId() + "\" id=\"" + instanceId + "\" style=\"" + escapeXml(instanceStyle) + "\">\n");
			writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
			writer.write("    ".repeat(depth) + "</cl:inputbox>\n");
			return;
			}
			
			// Pagination 처리
			if (lowerName.contains("pagination")) {
			writer.write("    ".repeat(depth) + "<cl:pageindexer std:sid=\"pageindexer-" + generateId() + "\" id=\"" + instanceId + "\" style=\"" + escapeXml(instanceStyle) + "\">\n");
			writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
			writer.write("    ".repeat(depth) + "</cl:pageindexer>\n");
			return;
			}
			
			// RadioButton 처리
			if (lowerName.contains("radio") || checkIfRadioButton(element)) {
			writer.write("    ".repeat(depth) + "<cl:radiobutton std:sid=\"r-button-" + generateId() + "\" id=\"" + instanceId + "\" value=\"" + escapeXml(instanceValue) + "\" style=\"" + escapeXml(instanceStyle) + "\">\n");
			writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
			writer.write("    ".repeat(depth) + "</cl:radiobutton>\n");
			return;
			}
			
			// Button 처리
			writer.write("    ".repeat(depth) + "<cl:button std:sid=\"button-" + generateId() + "\" id=\"" + instanceId + "\" value=\"" + escapeXml(instanceValue) + "\" style=\"" + escapeXml(instanceStyle) + "\">\n");
			writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
			writer.write("    ".repeat(depth) + "</cl:button>\n");
			}

    public String extractStyle(Map<String, Object> element) {
        StringBuilder style = new StringBuilder();
        boolean hasBackgroundColor = false;

        String type = (String) element.get("type");
        List<Map<String, Object>> fills = (List<Map<String, Object>>) element.get("fills");

        if (fills != null && !fills.isEmpty()) {
            Map<String, Object> fill = fills.get(0);
            if (fill.containsKey("color") && !"TEXT".equals(type)) {
                Map<String, Object> color = (Map<String, Object>) fill.get("color");
                String hexColor = convertToHex(color);
                style.append("background-color: ").append(hexColor).append("; ");
                hasBackgroundColor = true;
            }
        }

        List<Map<String, Object>> strokes = (List<Map<String, Object>>) element.get("strokes");
        if (strokes != null && !strokes.isEmpty()) {
            Map<String, Object> stroke = strokes.get(0);
            if (stroke.containsKey("color")) {
                Map<String, Object> color = (Map<String, Object>) stroke.get("color");
                String hexColor = convertToHex(color);
                style.append("border-color: ").append(hexColor).append("; ");
            }
        }

        if (element.containsKey("opacity")) {
            double opacity = (double) element.get("opacity");
            style.append("opacity: ").append(opacity).append("; ");
        }

        if (("BUTTON".equals(type) || "INSTANCE".equals(type)) && hasBackgroundColor) {
            style.append("background-image: none; ");
        }

        return style.toString();
    }

    private String convertToHex(Map<String, Object> color) {
        int r = (int) ((double) color.get("r") * 255);
        int g = (int) ((double) color.get("g") * 255);
        int b = (int) ((double) color.get("b") * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    public String getButtonValue(Map<String, Object> element) {
        String textValue = (String) element.get("characters");
        if (textValue != null && !textValue.trim().isEmpty()) {
            return textValue.trim();
        }

        Map<String, Object> componentProperties = (Map<String, Object>) element.get("componentProperties");
        if (componentProperties != null) {
            for (String key : componentProperties.keySet()) {
                Map<String, Object> prop = (Map<String, Object>) componentProperties.get(key);
                if ("TEXT".equalsIgnoreCase((String) prop.get("type"))) {
                    textValue = (String) prop.get("value");
                    if (textValue != null && !textValue.trim().isEmpty()) {
                        return textValue.trim();
                    }
                }
            }
        }

        Map<String, Object> mainComponent = (Map<String, Object>) element.get("mainComponent");
        if (mainComponent != null) {
            textValue = (String) mainComponent.get("characters");
            if (textValue != null && !textValue.trim().isEmpty()) {
                return textValue.trim();
            }
        }

        List<Map<String, Object>> children = (List<Map<String, Object>>) element.get("children");
        if (children != null) {
            for (Map<String, Object> child : children) {
                if ("TEXT".equalsIgnoreCase((String) child.get("type"))) {
                    textValue = (String) child.get("characters");
                    if (textValue != null && !textValue.trim().isEmpty()) {
                        return textValue.trim();
                    }
                }
            }
        }

        return "Button";
    }

    public boolean checkIfRadioButton(Map<String, Object> element) {
        Map<String, Object> componentProperties = (Map<String, Object>) element.get("componentProperties");
        if (componentProperties != null) {
            for (String key : componentProperties.keySet()) {
                if (key.toLowerCase().contains("radio")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasVectorDeepInRight(Map<String, Object> element) {
        List<Map<String, Object>> children = (List<Map<String, Object>>) element.get("children");
        if (children != null) {
            for (Map<String, Object> child : children) {
                String childName = (String) child.getOrDefault("name", "");
                if (childName.toLowerCase().contains("vector")) {
                    return true;
                }
                if (hasVectorDeepInRight(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void writeLayoutData(FileWriter writer, double elementX, double elementY, double width, double height, double parentX, double parentY, int depth) throws IOException {
        double relativeX = elementX - parentX;
        double relativeY = elementY - parentY;

        String indent = "    ".repeat(depth);
        writer.write(indent + "<cl:xylayoutdata " +
                "top=\"" + relativeY + "px\" " +
                "left=\"" + relativeX + "px\" " +
                "width=\"" + width + "px\" " +
                "height=\"" + height + "px\" " +
                "horizontalAnchor=\"LEFT\" " +
                "verticalAnchor=\"TOP\"/>\n");
    }

    public String escapeXml(String input) {
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public String generateId() {
        return UUID.randomUUID().toString();  // UUID 전체 사용
    }
}