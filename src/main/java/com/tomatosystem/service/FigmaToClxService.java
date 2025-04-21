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
import com.tomatosystem.type.InputNodeConverter;
import com.tomatosystem.type.InstanceNodeConverter;
import com.tomatosystem.type.RectangleNodeConverter;
import com.tomatosystem.type.TextNodeConverter;
import com.tomatosystem.type.VectorNodeConverter;

@Service
public class FigmaToClxService {
	 //제일 베스트
	  public File convertToClx(Map<String, Object> figmaJson) throws IOException {
	        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
	        
	        //String outputDir = "C:\\eb6-work\\workspace\\convertTestXml\\clx-src\\" + today;
	        String outputDir = "C:\\Users\\LCM\\git\\Converter-Figma\\clx-src\\convertTest\\" + today;
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

		    // 🔹 그룹 및 프레임 처리
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
		    
		    // Rectangle 타입 처리
		    if ("RECTANGLE".equalsIgnoreCase(type)) {
		        RectangleNodeConverter rectangleConverter = new RectangleNodeConverter();
		        rectangleConverter.convert(writer, element, name, x, y, width, height, parentX, parentY, style, depth);
		        return;
		    }
		    
		    // 🔹 INSTANCE 타입 처리 - InstanceNodeConverter를 사용
		    if ("INSTANCE".equalsIgnoreCase(type)) {
		        InstanceNodeConverter instanceConverter = new InstanceNodeConverter(); // InstanceNodeConverter 클래스의 인스턴스 생성
		        instanceConverter.convert(writer, element, name, x, y, width, height, parentX, parentY, style, depth); // convert 메서드에 name 넘기기
		        return;
		    }
		    
		    // 🔹 일반 TEXT 요소는 <cl:output> 태그로 변환
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
		        InputNodeConverter inputConverter = new InputNodeConverter();
		        inputConverter.convert(writer, element, name, x, y, width, height, parentX, parentY, style, depth);
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
		
	  private String generateId() {
	        return UUID.randomUUID().toString().substring(0, 8);
	    }
}
