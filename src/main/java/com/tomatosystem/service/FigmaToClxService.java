package com.tomatosystem.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.tomatosystem.type.GroupFrameNodeConverter;
import com.tomatosystem.type.InstanceNodeConverter;
import com.tomatosystem.type.TextNodeConverter;
import com.tomatosystem.type.VectorNodeConverter;

@Service
public class FigmaToClxService {
	 //제일 베스트
	  public File convertToClx(Map<String, Object> figmaJson) throws IOException {
	        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
	        
	        //String outputDir = "C:\\eb6-work\\workspace\\convertTestXml\\clx-src\\" + today;
	        String outputDir = "C:\\Users\\LCM\\git\\Converter-Figma\\clx-src\\" + today;
	        Files.createDirectories(Paths.get(outputDir));

	        int randomNumber = 10000 + new Random().nextInt(90000);
	        String baseFileName = "design" + randomNumber;
	        String clxFilePath = outputDir + "\\" + baseFileName + ".clx";
	        String jsFilePath = outputDir + "\\" + baseFileName + ".js";

	        File clxFile = new File(clxFilePath);
	        File jsFile = new File(jsFilePath);

	        // XML 파일 생성
	        try (FileWriter writer = new FileWriter(clxFile, StandardCharsets.UTF_8)) {
	            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
	            writer.write("<html xmlns=\"http://www.w3.org/1999/xhtml\" " +
	                    "xmlns:cl=\"http://tomatosystem.co.kr/cleopatra\" " +
	                    "xmlns:std=\"http://tomatosystem.co.kr/cleopatra/studio\" " +
	                    "std:sid=\"html-" + generateId() + "\" version=\"1.0.5538\">\n");

	            writer.write("  <head std:sid=\"head-" + generateId() + "\">\n");
	            writer.write("    <screen std:sid=\"screen-" + generateId() + "\" id=\"default\" name=\"default\" width=\"1924px\" height=\"768px\"/>\n");
	            writer.write("    <cl:model std:sid=\"model-" + generateId() + "\"/>\n");
	            writer.write("    <cl:appspec/>\n");
	            writer.write("  </head>\n");
	            writer.write("  <body std:sid=\"body-" + generateId() + "\">\n");

	            // 🔹 Figma JSON에서 children 가져오기
	            Map<String, Object> document = (Map<String, Object>) figmaJson.get("document");
	            if (document != null) {
	                List<Map<String, Object>> children = (List<Map<String, Object>>) document.get("children");
	                if (children != null) {
	                    for (Map<String, Object> element : children) {
	                        convertElement(writer, element, 2, 0, 0);
	                    }
	                }
	            }

	            writer.write("    <cl:xylayout std:sid=\"xylayout-" + generateId() + "\"/>\n");
	            writer.write("  </body>\n");
	            writer.write("  <std:studiosetting>\n");
	            writer.write("    <std:hruler/>\n");
	            writer.write("    <std:vruler/>\n");
	            writer.write("  </std:studiosetting>\n");
	            writer.write("</html>\n");
	        }

	        // JavaScript 파일 생성
	        try (FileWriter jsWriter = new FileWriter(jsFile, StandardCharsets.UTF_8)) {
	            jsWriter.write("// Generated JavaScript File for " + baseFileName + ".clx\n");
	            jsWriter.write("console.log('JavaScript for " + baseFileName + " loaded.');\n");
	        }

	        return clxFile;
	    }

	  private void convertElement(FileWriter writer, Map<String, Object> element, int depth, double parentX, double parentY) throws IOException {
		    String type = (String) element.get("type");
		    String name = (String) element.getOrDefault("name", "Unknown");
		    List<Map<String, Object>> children = (List<Map<String, Object>>) element.get("children");

		    // 위치 및 크기 계산
		    Map<String, Object> bbox = (Map<String, Object>) element.get("absoluteBoundingBox");
		    double x = bbox != null ? (double) bbox.getOrDefault("x", 0) : 0;
		    double y = bbox != null ? (double) bbox.getOrDefault("y", 0) : 0;
		    double width = bbox != null ? (double) bbox.getOrDefault("width", 100) : 100;
		    double height = bbox != null ? (double) bbox.getOrDefault("height", 50) : 50;
		    
		    // 스타일 가져오기
		    String style = extractStyle(element);

		    String indent = "    ".repeat(depth);
		    //System.out.println("🔹 변환 중: " + type + " - " + name);

		    if ("FRAME".equalsIgnoreCase(type) || "GROUP".equalsIgnoreCase(type)) {
		        GroupFrameNodeConverter groupFrameConverter = new GroupFrameNodeConverter();
		        boolean needsClosingTag = groupFrameConverter.convert(writer, element, name, x, y, width, height, parentX, parentY, style, depth);

		        // ✅ 닫는 태그가 필요한 경우만 자식 순회 (그룹만 해당)
		        if (needsClosingTag && children != null) {
		            for (Map<String, Object> child : children) {
		                String childName = (String) child.getOrDefault("name", "");
		                // table1, table2 등은 생략 (이미 table 처리됨)
		                if (!childName.matches("(?i)table\\d+")) {
		                    convertElement(writer, child, depth + 1, x, y);
		                }
		            }
		        }

		        if (needsClosingTag) {
		            writer.write(indent + "</cl:group>\n");
		        }
		        return;
		    }
		    
		 // 🔹 그룹 및 프레임 처리
//		    if ("FRAME".equalsIgnoreCase(type) || "GROUP".equalsIgnoreCase(type)) {
//		        boolean isTable = "table".equalsIgnoreCase(name);
//              //title
//		        boolean isTitleFrame = name.toLowerCase().contains("title"); // 🔹 title 포함 여부
//		        
//		        if (isTable) {
//		            // ✅ `table`을 `<cl:grid>`로 변환
//		            String gridId = "grd" + generateId();
//		            writer.write(indent + "<cl:grid std:sid=\"grid-" + generateId() + "\" id=\"" + gridId + "\">\n");
//		            writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
//
//		            // ✅ 컬럼 추가
//		            for (int i = 0; i < 5; i++) {
//		                writer.write(indent + "  <cl:gridcolumn std:sid=\"g-column-" + generateId() + "\"/>\n");
//		            }
//
//		            // ✅ 헤더 추가
//		            writer.write(indent + "  <cl:gridheader std:sid=\"gh-band-" + generateId() + "\">\n");
//		            writer.write(indent + "    <cl:gridrow std:sid=\"g-row-" + generateId() + "\"/>\n");
//		            for (int i = 0; i < 5; i++) {
//		                writer.write(indent + "    <cl:gridcell std:sid=\"gh-cell-" + generateId() + "\" rowindex=\"0\" colindex=\"" + i + "\"/>\n");
//		            }
//		            writer.write(indent + "  </cl:gridheader>\n");
//
//		            // ✅ 데이터 추가
//		            writer.write(indent + "  <cl:griddetail std:sid=\"gd-band-" + generateId() + "\">\n");
//		            writer.write(indent + "    <cl:gridrow std:sid=\"g-row-" + generateId() + "\"/>\n");
//		            for (int i = 0; i < 5; i++) {
//		                writer.write(indent + "    <cl:gridcell std:sid=\"gd-cell-" + generateId() + "\" rowindex=\"0\" colindex=\"" + i + "\"/>\n");
//		            }
//		            writer.write(indent + "  </cl:griddetail>\n");
//
//		            writer.write(indent + "</cl:grid>\n");
//		            return;
//		        }
//
//		        // ✅ title이 포함된 FRAME의 경우 UDC 생성
//		        if (isTitleFrame && "FRAME".equalsIgnoreCase(type)) {
//		            String udcId = "ud-control-" + generateId();
//		            String layoutId = "xyl-data-" + generateId();
//
//		            writer.write(indent + "<cl:udc std:sid=\"" + udcId + "\" type=\"udc.udcComAppHeader\">\n");
//		            writer.write(indent + "  <cl:xylayoutdata std:sid=\"" + layoutId + "\" top=\"" + (int)(y - parentY) + "px\" left=\"" + (int)(x - parentX) + "px\" width=\"" + (int)width + "px\" height=\"" + (int)height + "px\" horizontalAnchor=\"LEFT\" verticalAnchor=\"TOP\"/>\n");
//		            writer.write(indent + "</cl:udc>\n");
//		            return;
//		        }
//		        
//		        // ✅ 일반 <cl:group> 처리
//		        String groupId = "group_" + generateId();
//		        writer.write(indent + "<cl:group std:sid=\"group-" + generateId() + "\" id=\"" + groupId + "\" style=\"" + escapeXml(style) + "\">\n");
//		        writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
//
//		        if (children != null) {
//		            for (Map<String, Object> child : children) {
//		                String childName = (String) child.getOrDefault("name", "");
//		                if (!childName.matches("(?i)table\\d+")) { // "table1", "table2" 같은 요소는 무시
//		                    convertElement(writer, child, depth + 1, x, y);
//		                }
//		            }
//		        }
//
//		        writer.write(indent + "</cl:group>\n");
//		        return;
//		    }

		    // 그룹 및 프레임 처리
//		    if ("FRAME".equalsIgnoreCase(type) || "GROUP".equalsIgnoreCase(type)) {
//		        GroupFrameNodeConverter groupFrameConverter = new GroupFrameNodeConverter();
//		        groupFrameConverter.convert(writer, element, name, x, y, width, height, parentX, parentY, style, depth);
//
//		        // 자식 처리
//		        if (children != null) {
//		            for (Map<String, Object> child : children) {
//		                String childName = (String) child.getOrDefault("name", "");
//		                if (!childName.matches("(?i)table\\d+")) { // table1, table2 같은 요소는 무시
//		                    convertElement(writer, child, depth + 1, x, y); // 자식 요소 처리
//		                }
//		            }
//		            writer.write(indent + "</cl:group>\n");
//		        }
//
//		        // 그룹 태그 닫기 (이 부분은 이제 convertElement에서 처리)
//		        //writer.write(indent + "</cl:group>\n");
//		        return;
//		    }

		  
//		    if ("INSTANCE".equalsIgnoreCase(type)) {
//		        String instanceId = "instance_" + generateId();
//		        String instanceValue = getButtonValue(element);
//
//		        // 부모 요소 이름 가져오기
//		        Object parentNameObj = element.get("parentName");
//		        String parentName = (parentNameObj instanceof String) ? ((String) parentNameObj).toLowerCase() : "";
//
//		        // 🔍 'right' 내부에 'vector' 포함 여부 체크
//		        boolean hasVectorInRight = false;
//
//		        if (children != null) {
//		            for (Map<String, Object> child : children) {
//		                String childName = (String) child.getOrDefault("name", "");
//
//		                if (childName.toLowerCase().contains("right")) {
//		                    hasVectorInRight = hasVectorDeepInRight(child);
//		                    break;
//		                }
//		            }
//		        }
//
//		        // ✅ 라디오 버튼인지 확인
//		        boolean isRadioButton = name.toLowerCase().contains("radio") || checkIfRadioButton(element);
//
//		        // 디버깅 출력
//		       // System.out.println("Element Name: " + name + ", Parent Name: " + parentName + ", Has Vector in Right: " + hasVectorInRight + ", Is Radio Button: " + isRadioButton);
//
//		        // ✅ InputBox 또는 ComboBox 변환 (right 내부 vector 포함 여부에 따라 결정)
//		        if (name.toLowerCase().contains("base-input") || parentName.contains("input")) {
//		            String tag = hasVectorInRight ? "cl:combobox" : "cl:inputbox";
//		            String tagNameWithoutCl = tag.replace("cl:", ""); // "cl:"을 제거
//		            // tag가 "combobox"일 경우 "c-box"로 변경
//		            if ("combobox".equals(tagNameWithoutCl)) {
//		                tagNameWithoutCl = "c-box";
//		            }
//		            
//		            writer.write(indent + "<" + tag + " std:sid=\"" + tagNameWithoutCl + "-" + generateId() + "\" id=\"" + instanceId + "\" style=\"" + escapeXml(style) + "\">\n");
//		            //writer.write(indent + "<" + tag + " std:sid=\'"+tag + "-'" + generateId() + "\" id=\"" + instanceId + "\" style=\"" + escapeXml(style) + "\">\n");
//		            writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
//		            writer.write(indent + "</" + tag + ">\n");
//		            return;
//		        }
//
//		        // ✅ SelectBox (ComboBox) 변환
//		        if (name.toLowerCase().contains("selectbox") || parentName.contains("selectbox")) {
//		            writer.write(indent + "<cl:combobox std:sid=\"c-box-" + generateId() + "\" id=\"" + instanceId + "\" style=\"" + escapeXml(style) + "\">\n");
//		            writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
//		            writer.write(indent + "</cl:combobox>\n");
//		            return;
//		        }
//
//		        // ✅ Pagination (PageIndexer) 변환
//		        if (name.toLowerCase().contains("pagination")) {
//		            writer.write(indent + "<cl:pageindexer std:sid=\"pageindexer-" + generateId() + "\" id=\"" + instanceId + "\" style=\"" + escapeXml(style) + "\">\n");
//		            writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
//		            writer.write(indent + "</cl:pageindexer>\n");
//		            return;
//		        }
//
//		        // ✅ 기존 버튼 & 라디오 버튼 처리 (GROUP 내부 중복 생성 방지)
//		        if (!parentName.contains("group")) { // 그룹 내부에서는 중복 생성 방지
//		            String tag = isRadioButton ? "cl:radiobutton" : "cl:button";
//		            String tagNameWithoutCl = tag.replace("cl:", ""); // "cl:"을 제거
//		            //r-button
//		            if ("radiobutton".equals(tagNameWithoutCl)) {
//		                tagNameWithoutCl = "r-button";
//		            }
//		            writer.write(indent + "<" + tag + " std:sid=\"" + tagNameWithoutCl + "-" + generateId() + "\" id=\"" + instanceId + "\" value=\"" + escapeXml(instanceValue) +  "\" style=\"" + escapeXml(style) + "\">\n");
//		            //writer.write(indent + "<" + tag + " std:sid=\'"+tag + "-'" + generateId() + "\" id=\"" + instanceId + "\" value=\"" + escapeXml(instanceValue) + "\" style=\"" + escapeXml(style) + "\">\n");
//		            writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
//		            writer.write(indent + "</" + tag + ">\n");
//		        }
//
//		        return;
//		    }
		    // 🔹 INSTANCE 타입 처리 - InstanceNodeConverter를 사용
		    if ("INSTANCE".equalsIgnoreCase(type)) {
		        InstanceNodeConverter instanceConverter = new InstanceNodeConverter(); // InstanceNodeConverter 클래스의 인스턴스 생성
		        instanceConverter.convert(writer, element, name, x, y, width, height, parentX, parentY, style, depth); // convert 메서드에 name 넘기기
		        return;
		    }
		    
		    // 🔹 일반 TEXT 요소는 <cl:output> 태그로 변환
//		    if ("TEXT".equalsIgnoreCase(type)) {
//		        String textId = "output_" + generateId();
//		        String textValue = getTextValue(element);
//
//		        writer.write(indent + "<cl:output std:sid=\"output-" + generateId() + "\" id=\"" + textId + "\" value=\"" + escapeXml(textValue) + "\" style=\"" + escapeXml(style) + "\">\n");
//		        writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
//		        writer.write(indent + "</cl:output>\n");
//		        return;
//		    }
		    if ("TEXT".equalsIgnoreCase(type)) {
		        TextNodeConverter textConverter = new TextNodeConverter();
		        textConverter.convert(writer, element, x, y, width, height, parentX, parentY, style, depth);
		        return;
		    }
		    

		    // 🔹 이미지 요소 변환
//		    if ("VECTOR".equalsIgnoreCase(type) || "IMAGE".equalsIgnoreCase(type)) {
//		        VectorNodeConverter vectorConverter = new VectorNodeConverter();
//		        boolean needsClosingTag = vectorConverter.convert(writer, element, name, x, y, width, height, parentX, parentY, style, depth);
//		        
//		        // 이 부분에서 닫는 태그가 필요 없다면 false로 반환하고, 필요하면 true로 반환
//		        return;
//		    }

		    // 🔹 인풋 박스 변환
		    if ("INPUT".equalsIgnoreCase(type)) {
		        String inputId = "input_" + generateId();
		        writer.write(indent + "<cl:inputbox std:sid=\"inputbox-" + generateId() + "\" id=\"" + inputId + "\" style=\"" + escapeXml(style) + "\">\n");
		        writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
		        writer.write(indent + "</cl:inputbox>\n");
		        return;
		    }

		    // 🔹 자식 요소 변환 (그룹이 아닌 일반 요소)
		    if (children != null) {
		        for (Map<String, Object> child : children) {
		            convertElement(writer, child, depth + 1, x, y);
		        }
		    }
		}

	    /**
	     * 🔍 재귀적으로 'vector' 포함 여부 확인 함수
	     */
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
	    
	// 🔹 스타일 추출 메서드 (INSTANCE 타입 버튼도 처리)
	  private String extractStyle(Map<String, Object> element) {
	      StringBuilder style = new StringBuilder();

	      boolean hasBackgroundColor = false;

	      String type = (String) element.get("type");
	      //System.out.println("Element Type: " + type); // 🔹 타입 확인

	      // 배경색
	      List<Map<String, Object>> fills = (List<Map<String, Object>>) element.get("fills");
	     // System.out.println("Fills: " + fills); // 🔹 fills 구조 확인

	      if (fills != null && !fills.isEmpty()) {
	          Map<String, Object> fill = fills.get(0);
	          if (fill.containsKey("color")) {
	        	  if(!"TEXT".equals(type)) {
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

	      //System.out.println("Generated Style: " + style.toString()); // 🔹 최종 스타일 확인

	      return style.toString();
	  }

	  
	  // 🔹 RGB → HEX 변환 함수
	  private String convertToHex(Map<String, Object> color) {
	      int r = (int) ((double) color.get("r") * 255);
	      int g = (int) ((double) color.get("g") * 255);
	      int b = (int) ((double) color.get("b") * 255);
	      return String.format("#%02X%02X%02X", r, g, b);
	  }
		
	  private String getTextValue(Map<String, Object> element) {
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
	  
	 // 🔹 라디오 버튼 여부를 체크하는 함수 추가
	    private boolean checkIfRadioButton(Map<String, Object> element) {
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
	    
	    private void writeLayoutData(FileWriter writer, double elementX, double elementY, double width, double height, double parentX, double parentY, int depth) throws IOException {
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

	    private String getButtonValue(Map<String, Object> element) {
	        // 1️⃣ `characters` 속성을 우선적으로 가져옴
	        String textValue = (String) element.get("characters");
	        if (textValue != null && !textValue.trim().isEmpty()) {
	            //System.out.println("Button Value (characters): " + textValue);
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
	                        //System.out.println("Button Value (componentProperties): " + textValue);
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
	                //System.out.println("Button Value (mainComponent): " + textValue);
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
	                        //System.out.println("Button Value (children TEXT): " + textValue);
	                        return textValue.trim();
	                    }
	                }
	            }
	        }

	        // 5️⃣ 기본값 설정
	       // System.out.println("Button Value Not Found, Using Default: Button");
	        return "Button";
	    }

	    private String escapeXml(String input) {
	        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	    }

	    private String generateId() {
	        return UUID.randomUUID().toString().substring(0, 8);
	    }
}
