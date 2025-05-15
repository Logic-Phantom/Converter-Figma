package com.tomatosystem.type;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static com.tomatosystem.utill.NodeConverterUtils.*;

public class TextNodeConverter {

    public void convert(FileWriter writer, Map<String, Object> element,
                        double x, double y, double width, double height,
                        double parentX, double parentY, String style, int depth) throws IOException {

        String indent = "    ".repeat(depth);
        String textId = "output_" + generateId();
        String textValue = getTextValue(element);

        // 텍스트 크기 계산
        double[] minSize = calculateTextMinSize(textValue, element);
        width = Math.max(width, minSize[0]);
        height = Math.max(height, minSize[1]);

        // 부모 컨테이너를 벗어나지 않도록 조정
        if (parentX > 0 && parentY > 0) {
            width = Math.min(width, parentX);
            height = Math.min(height, parentY);
        }

        writer.write(indent + "<cl:output std:sid=\"output-" + generateId() + "\" id=\"" + textId + "\" value=\"" +
                escapeXml(textValue) + "\" style=\"" + escapeXml(style) + "\">\n");
        writeLayoutData(writer, x, y, width, height, parentX, parentY, depth + 1);
        writer.write(indent + "</cl:output>\n");
    }
  
    private String getTextValue(Map<String, Object> element) {
        // characters 필드에서 텍스트 추출
        Object charactersObj = element.get("characters");
        return charactersObj != null ? charactersObj.toString() : "";
    }

}
