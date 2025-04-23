package com.tomatosystem.type;

import java.io.FileWriter;

import static com.tomatosystem.utill.NodeConverterUtils.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GroupFrameNodeConverter {

	public boolean convert(FileWriter writer, Map<String, Object> element, String name,
            double x, double y, double width, double height,
            double parentX, double parentY, String style, int depth) throws IOException {

			String indent = "    ".repeat(depth);
			String type = (String) element.get("type");
			
			// 정확히 이름이 "table"인 경우만 그리드로 처리
			boolean isTable = "table".equalsIgnoreCase(name);
			// type이 FRAME이고 이름에 title이 포함된 경우만 udc 처리
			boolean isTitleFrame = "FRAME".equalsIgnoreCase(type) && name.toLowerCase().contains("title");
			
			// ✅ 그리드 처리
			if (isTable) {
			String gridId = "grd" + generateId();
			writer.write(indent + "<cl:grid std:sid=\"grid-" + generateId() + "\" id=\"" + gridId + "\">\n");
			writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
			
			for (int i = 0; i < 5; i++) {
			 writer.write(indent + "  <cl:gridcolumn std:sid=\"g-column-" + generateId() + "\"/>\n");
			}
			
			writer.write(indent + "  <cl:gridheader std:sid=\"gh-band-" + generateId() + "\">\n");
			writer.write(indent + "    <cl:gridrow std:sid=\"g-row-" + generateId() + "\"/>\n");
			for (int i = 0; i < 5; i++) {
			 writer.write(indent + "    <cl:gridcell std:sid=\"gh-cell-" + generateId() + "\" rowindex=\"0\" colindex=\"" + i + "\"/>\n");
			}
			writer.write(indent + "  </cl:gridheader>\n");
			
			writer.write(indent + "  <cl:griddetail std:sid=\"gd-band-" + generateId() + "\">\n");
			writer.write(indent + "    <cl:gridrow std:sid=\"g-row-" + generateId() + "\"/>\n");
			for (int i = 0; i < 5; i++) {
			 writer.write(indent + "    <cl:gridcell std:sid=\"gd-cell-" + generateId() + "\" rowindex=\"0\" colindex=\"" + i + "\"/>\n");
			}
			writer.write(indent + "  </cl:griddetail>\n");
			
			writer.write(indent + "</cl:grid>\n");
			return false; // 자식 처리 안함
			}
			
			// ✅ UDC 처리
			if (isTitleFrame) {
			    String udcId = "ud-control-" + generateId();
			    String layoutId = "xyl-data-" + generateId();

			    writer.write(indent + "<cl:udc std:sid=\"" + udcId + "\" type=\"udc.udcComAppHeader\">\n");
			    writer.write(indent + "  <cl:xylayoutdata std:sid=\"" + layoutId + "\" top=\"" + (int) (y - parentY) + "px\" left=\"" + (int) (x - parentX) + "px\" width=\"" + (int) width + "px\" height=\"" + (int) height + "px\" horizontalAnchor=\"LEFT\" verticalAnchor=\"TOP\"/>\n");

			    // ✅ 재귀적으로 TEXT 타입의 characters 찾기
			    String titleText = findFirstTextValue(element);
			    if (titleText != null && !titleText.isEmpty()) {
			        writer.write(indent + "  <cl:property name=\"title\" value=\"" + escapeXml(titleText) + "\" type=\"string\"/>\n");
			    }

			    writer.write(indent + "</cl:udc>\n");
			    return false; // 자식 처리 안함
			}
			//라디오 한번에처리로직
			//라디오 요소의 갯수만큼 아이템으로 생성해야하여, 그룹에서 미리 자식을 훑고 판단할 수밖에없음
			if ("GROUP".equalsIgnoreCase(type) || "FRAME".equalsIgnoreCase(type)) {
			    List<Map<String, Object>> children = (List<Map<String, Object>>) element.get("children");

			    if (children != null && hasMultipleRadioButtons(children)) {
			        // GROUP 시작
			        String groupSid = "group-" + generateId();
			        String groupId = "group_" + generateId();
			        writer.write(indent + "<cl:group std:sid=\"" + groupSid + "\" id=\"" + groupId + "\" style=\"\">\n");

			        // 그룹 레이아웃
			        writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);

			        // <cl:radiobutton> 시작
			        String radioSid = "r-button-" + generateId();
			        String radioId = "rdb" + generateId();
			        writer.write(indent + "  <cl:radiobutton std:sid=\"" + radioSid + "\" id=\"" + radioId + "\">\n");

			        // 라디오 자체 레이아웃 (내부 위치는 추후 계산 가능)
			        //writer.write(indent + "    <cl:xylayoutdata std:sid=\"xyl-data-" + generateId() + "\" top=\"3px\" left=\"4px\" width=\"100px\" height=\"15px\" horizontalAnchor=\"LEFT\" verticalAnchor=\"TOP\"/>\n");
	     		     // 라디오 자체 레이아웃
			        writer.write(indent + "    <cl:xylayoutdata std:sid=\"xyl-data-" + generateId() + "\" top=\"3px\" left=\"4px\" width=\"" + width + "px\" height=\"" + height +  "px\" horizontalAnchor=\"LEFT\" verticalAnchor=\"TOP\"/>\n");


			        for (Map<String, Object> radioChild : children) {
			            String childType = (String) radioChild.get("type");
			            String childName = (String) radioChild.get("name");

			            if ("INSTANCE".equals(childType) && childName.toLowerCase().contains("radio")) {
			                String label = childName;
			                String value = findFirstTextValue(radioChild);
			                String itemSid = "item-" + generateId();

			                writer.write(indent + "    <cl:item std:sid=\"" + itemSid + "\" label=\"" + escapeXml(value) + "\" value=\"" + escapeXml(value) + "\"/>\n");
			            }
			        }

			        writer.write(indent + "  </cl:radiobutton>\n");
			        writer.write(indent + "  <cl:xylayout std:sid=\"xylayout-" + generateId() + "\"/>\n");
			        writer.write(indent + "</cl:group>\n");

			        return false; // 자식은 따로 처리 안 함
			    }
			}
			
			// ✅ 일반 그룹 처리
			String groupId = "group_" + generateId();
			writer.write(indent + "<cl:group std:sid=\"group-" + generateId() + "\" id=\"" + groupId + "\" style=\"" + escapeXml(style) + "\">\n");
			writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
			
			return true; // 자식 계속 처리
			}
    private boolean hasMultipleRadioButtons(List<Map<String, Object>> children) {
        // 자식 요소 중 "radio" 인스턴스가 2개 이상인지 체크
        int radioCount = 0;
        for (Map<String, Object> child : children) {
            String type = (String) child.get("type");
            if ("INSTANCE".equals(type) && child.get("name").toString().toLowerCase().contains("radio")) {
                radioCount++;
            }
        }
        return radioCount > 1; // 라디오 버튼이 2개 이상일 경우
    }
    

	private String findFirstTextValue(Map<String, Object> node) {
	    String type = (String) node.get("type");
	    if ("TEXT".equalsIgnoreCase(type)) {
	        Object characters = node.get("characters");
	        return characters != null ? characters.toString() : null;
	    }

	    List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
	    if (children != null) {
	        for (Map<String, Object> child : children) {
	            String result = findFirstTextValue(child);
	            if (result != null && !result.isEmpty()) {
	                return result;
	            }
	        }
	    }
	    return null;
	}
	
}
