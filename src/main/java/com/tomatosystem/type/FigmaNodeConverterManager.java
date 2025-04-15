//package com.tomatosystem.type;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import javax.json.JsonObject;
//
//public class FigmaNodeConverterManager {
//
//    private List<FigmaNodeConverter> converters;
//
//    public FigmaNodeConverterManager() {
//        converters = new ArrayList<>();
//        converters.add(new InstanceNodeConverter());
//        // 나중에 TextNodeConverter, GroupNodeConverter 등 추가 가능
//    }
//
//    public String convert(JsonObject node) {
//        for (FigmaNodeConverter converter : converters) {
//            if (converter.canHandle(node)) {
//                return converter.convert(node);
//            }
//        }
//        return ""; // 혹은 기본 처리
//    }
//}