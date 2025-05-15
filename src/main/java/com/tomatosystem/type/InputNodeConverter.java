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
}
