package com.tomatosystem.type;

import java.io.FileWriter;
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
//			if (isTitleFrame) {
//			String udcId = "ud-control-" + generateId();
//			String layoutId = "xyl-data-" + generateId();
//			
//			writer.write(indent + "<cl:udc std:sid=\"" + udcId + "\" type=\"udc.udcComAppHeader\">\n");
//			writer.write(indent + "  <cl:xylayoutdata std:sid=\"" + layoutId + "\" top=\"" + (int) (y - parentY) + "px\" left=\"" + (int) (x - parentX) + "px\" width=\"" + (int) width + "px\" height=\"" + (int) height + "px\" horizontalAnchor=\"LEFT\" verticalAnchor=\"TOP\"/>\n");
//			writer.write(indent + "</cl:udc>\n");
//			
//			return false; // 자식 처리 안함
//			}
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
			
			// ✅ 일반 그룹 처리
			String groupId = "group_" + generateId();
			writer.write(indent + "<cl:group std:sid=\"group-" + generateId() + "\" id=\"" + groupId + "\" style=\"" + escapeXml(style) + "\">\n");
			writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
			
			return true; // 자식 계속 처리
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
	
    private String generateId() {
        return UUID.randomUUID().toString();
    }

    private void writeLayoutData(FileWriter writer, double x, double y, double width, double height,
                                 double parentX, double parentY, int depth) throws IOException {
        String indent = "    ".repeat(depth);
        writer.write(indent + "<cl:xylayoutdata std:sid=\"" + generateId() + "\" top=\"" + (int) (y - parentY) + "px\" left=\"" + (int) (x - parentX) + "px\" width=\"" + (int) width + "px\" height=\"" + (int) height + "px\" horizontalAnchor=\"LEFT\" verticalAnchor=\"TOP\"/>\n");
    }

    private String escapeXml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
