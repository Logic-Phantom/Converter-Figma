package com.tomatosystem.type;

import javax.json.JsonObject;

public interface FigmaNodeConverter {
    boolean canHandle(JsonObject node);
    String convert(JsonObject node);
}
