package com.tomatosystem.type;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;


	public class InstanceNodeConverter {
		
		public void convert(FileWriter writer, Map<String, Object> element, String name, 
		                double x, double y, double width, double height, 
		                double parentX, double parentY, String style, int depth) 
		                throws IOException {
		        //System.out.println("인스턴스 타입 클래스화 확인용");
				// 'children' 배열 가져오기
				List<Map<String, Object>> children = (List<Map<String, Object>>) element.get("children");
				
				// 부모 요소 이름 가져오기
				Object parentNameObj = element.get("parentName");
				String parentName = (parentNameObj instanceof String) ? ((String) parentNameObj).toLowerCase() : "";
				
				// 🔍 'right' 내부에 'vector' 포함 여부 체크
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
				
				// ✅ 라디오 버튼인지 확인
				boolean isRadioButton = name.toLowerCase().contains("radio") || checkIfRadioButton(element);
				
				// 고유 ID 및 텍스트 값 생성
				String instanceId = "instance_" + generateId();
				String instanceValue = getButtonValue(element);
				
				// 디버깅 출력
				// System.out.println("Element Name: " + name + ", Parent Name: " + parentName + ", Has Vector in Right: " + hasVectorInRight + ", Is Radio Button: " + isRadioButton);
				
				// ✅ InputBox 또는 ComboBox 변환 (right 내부 vector 포함 여부에 따라 결정)
				if (name.toLowerCase().contains("base-input") || parentName.contains("input")) {
				    String tag = hasVectorInRight ? "cl:combobox" : "cl:inputbox";
				    String tagNameWithoutCl = tag.replace("cl:", ""); // "cl:"을 제거
				    if ("combobox".equals(tagNameWithoutCl)) {
				        tagNameWithoutCl = "c-box";
				    }
				
				    writer.write("    ".repeat(depth) + "<" + tag + " std:sid=\"" + tagNameWithoutCl + "-" + generateId() + "\" id=\"" + instanceId + "\" style=\"" + escapeXml(style) + "\">\n");
				    writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
				    writer.write("    ".repeat(depth) + "</" + tag + ">\n");
				    return;
				}
				
				// ✅ SelectBox (ComboBox) 변환
				if (name.toLowerCase().contains("selectbox") || parentName.contains("selectbox")) {
				    writer.write("    ".repeat(depth) + "<cl:combobox std:sid=\"c-box-" + generateId() + "\" id=\"" + instanceId + "\" style=\"" + escapeXml(style) + "\">\n");
				    writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
				    writer.write("    ".repeat(depth) + "</cl:combobox>\n");
				    return;
				}
				
				// ✅ Pagination (PageIndexer) 변환
				if (name.toLowerCase().contains("pagination")) {
				    writer.write("    ".repeat(depth) + "<cl:pageindexer std:sid=\"pageindexer-" + generateId() + "\" id=\"" + instanceId + "\" style=\"" + escapeXml(style) + "\">\n");
				    writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
				    writer.write("    ".repeat(depth) + "</cl:pageindexer>\n");
				    return;
				}
		
			// ✅ 기존 버튼 & 라디오 버튼 처리 (GROUP 내부 중복 생성 방지)
			if (!parentName.contains("group")) { // 그룹 내부에서는 중복 생성 방지
			    String tag = isRadioButton ? "cl:radiobutton" : "cl:button";
			    String tagNameWithoutCl = tag.replace("cl:", ""); // "cl:"을 제거
			    if ("radiobutton".equals(tagNameWithoutCl)) {
			        tagNameWithoutCl = "r-button";
			    }
			
			    writer.write("    ".repeat(depth) + "<" + tag + " std:sid=\"" + tagNameWithoutCl + "-" + generateId() + "\" id=\"" + instanceId + "\" value=\"" + escapeXml(instanceValue) + "\" style=\"" + escapeXml(style) + "\">\n");
			    writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
			    writer.write("    ".repeat(depth) + "</" + tag + ">\n");
			}
		}
	    // 🔹 스타일 추출 메서드 (INSTANCE 타입 버튼도 처리)
	    public String extractStyle(Map<String, Object> element) {
	        StringBuilder style = new StringBuilder();

	        boolean hasBackgroundColor = false;

	        String type = (String) element.get("type");

	        // 배경색
	        List<Map<String, Object>> fills = (List<Map<String, Object>>) element.get("fills");

	        if (fills != null && !fills.isEmpty()) {
	            Map<String, Object> fill = fills.get(0);
	            if (fill.containsKey("color")) {
	                if (!"TEXT".equals(type)) {
	                    Map<String, Object> color = (Map<String, Object>) fill.get("color");
	                    String hexColor = convertToHex(color);
	                    style.append("background-color: ").append(hexColor).append("; ");
	                    hasBackgroundColor = true;
	                }
	            }
	        }

	        // 테두리 색상
	        List<Map<String, Object>> strokes = (List<Map<String, Object>>) element.get("strokes");
	        if (strokes != null && !strokes.isEmpty()) {
	            Map<String, Object> stroke = strokes.get(0);
	            if (stroke.containsKey("color")) {
	                Map<String, Object> color = (Map<String, Object>) stroke.get("color");
	                String hexColor = convertToHex(color);
	                style.append("border-color: ").append(hexColor).append("; ");
	            }
	        }

	        // 투명도
	        if (element.containsKey("opacity")) {
	            double opacity = (double) element.get("opacity");
	            style.append("opacity: ").append(opacity).append("; ");
	        }

	        // 버튼인 경우 (BUTTON 또는 INSTANCE) && 배경색이 있는 경우 -> background-image: none; 추가
	        if (("BUTTON".equals(type) || "INSTANCE".equals(type)) && hasBackgroundColor) {
	            style.append("background-image: none; ");
	        }

	        return style.toString();
	    }

	    // 🔹 RGB → HEX 변환 함수
	    private String convertToHex(Map<String, Object> color) {
	        int r = (int) ((double) color.get("r") * 255);
	        int g = (int) ((double) color.get("g") * 255);
	        int b = (int) ((double) color.get("b") * 255);
	        return String.format("#%02X%02X%02X", r, g, b);
	    }

	    // 🔹 텍스트 값 추출 (버튼의 텍스트 등)
	    public String getTextValue(Map<String, Object> element) {
	        Map<String, Object> textProperties = (Map<String, Object>) element.get("componentProperties");
	        if (textProperties != null) {
	            for (String key : textProperties.keySet()) {
	                Map<String, Object> prop = (Map<String, Object>) textProperties.get(key);
	                if ("TEXT".equalsIgnoreCase((String) prop.get("type"))) {
	                    return (String) prop.getOrDefault("value", "");
	                }
	            }
	        }
	        return (String) element.getOrDefault("characters", "");
	    }

	    // 🔹 라디오 버튼 여부를 체크하는 함수
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

	    // 🔹 재귀적으로 'vector' 포함 여부 확인 함수
	    private boolean hasVectorDeepInRight(Map<String, Object> element) {
	        List<Map<String, Object>> children = (List<Map<String, Object>>) element.get("children");

	        if (children != null) {
	            for (Map<String, Object> child : children) {
	                String childName = (String) child.getOrDefault("name", "");
	                if (childName.toLowerCase().contains("vector")) {
	                    return true;
	                }
	                // 재귀적으로 자식 요소들 확인
	                if (hasVectorDeepInRight(child)) {
	                    return true;
	                }
	            }
	        }
	        return false;
	    }

	    // 🔹 레이아웃 데이터 작성 함수
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

	    // 🔹 버튼 값 추출 함수
	    public String getButtonValue(Map<String, Object> element) {
	        // 1️⃣ `characters` 속성을 우선적으로 가져옴
	        String textValue = (String) element.get("characters");
	        if (textValue != null && !textValue.trim().isEmpty()) {
	            return textValue.trim();
	        }

	        // 2️⃣ `componentProperties`에서도 값 검색
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

	        // 3️⃣ `mainComponent`에서 `characters` 값 찾기 (INSTANCE 타입일 경우)
	        Map<String, Object> mainComponent = (Map<String, Object>) element.get("mainComponent");
	        if (mainComponent != null) {
	            textValue = (String) mainComponent.get("characters");
	            if (textValue != null && !textValue.trim().isEmpty()) {
	                return textValue.trim();
	            }
	        }

	        // 4️⃣ `children` 배열에서 `TEXT` 타입을 찾기
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

	        // 5️⃣ 기본값 설정
	        return "Button";
	    }

	    // 🔹 XML 특수문자 처리 함수
	    public String escapeXml(String input) {
	        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	    }

	    // 🔹 고유 ID 생성 함수
	    public String generateId() {
	        return UUID.randomUUID().toString().substring(0, 8);
	    }
	}