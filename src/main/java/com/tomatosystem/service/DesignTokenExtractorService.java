package com.tomatosystem.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class DesignTokenExtractorService {
    
    private static final Set<String> COLOR_PROPERTIES = Set.of("fills", "strokes", "backgroundColor", "color");
    private static final Set<String> TYPOGRAPHY_PROPERTIES = Set.of("fontFamily", "fontSize", "fontWeight", "lineHeight", "letterSpacing");
    private static final Set<String> SPACING_PROPERTIES = Set.of("paddingTop", "paddingRight", "paddingBottom", "paddingLeft", "marginTop", "marginRight", "marginBottom", "marginLeft", "itemSpacing", "verticalPadding", "horizontalPadding");
    private static final Set<String> BORDER_RADIUS_PROPERTIES = Set.of("cornerRadius", "borderRadius", "topLeftRadius", "topRightRadius", "bottomLeftRadius", "bottomRightRadius");

    public void extractDesignTokens(JsonNode figmaJson, String outputPath) {
        try {
            Map<String, String> colorTokens = new HashMap<>();
            Map<String, String> typographyTokens = new HashMap<>();
            Map<String, String> spacingTokens = new HashMap<>();
            Map<String, String> borderRadiusTokens = new HashMap<>();

            // JSON 순회하며 토큰 추출
            extractTokensFromNode(figmaJson, colorTokens, typographyTokens, spacingTokens, borderRadiusTokens);

            // 결과를 다양한 형식으로 저장
            saveTokensAsJson(colorTokens, typographyTokens, spacingTokens, borderRadiusTokens, outputPath);
            saveTokensAsScss(colorTokens, typographyTokens, spacingTokens, borderRadiusTokens, outputPath);
            saveTokensAsCss(colorTokens, typographyTokens, spacingTokens, borderRadiusTokens, outputPath);
            saveTokensAsTailwind(colorTokens, typographyTokens, spacingTokens, borderRadiusTokens, outputPath);
            saveTokensAsStyledComponents(colorTokens, typographyTokens, spacingTokens, borderRadiusTokens, outputPath);

            System.out.println("디자인 토큰 추출 완료");
        } catch (Exception e) {
            System.err.println("디자인 토큰 추출 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void extractTokensFromNode(JsonNode node, 
                                     Map<String, String> colorTokens,
                                     Map<String, String> typographyTokens,
                                     Map<String, String> spacingTokens,
                                     Map<String, String> borderRadiusTokens) {
        if (node.isObject()) {
            // 컴포넌트 이름 가져오기
            String componentName = node.path("name").asText("").toLowerCase()
                    .replaceAll("[^a-z0-9-]", "-")
                    .replaceAll("-+", "-");

            // 색상 토큰 추출
            for (String prop : COLOR_PROPERTIES) {
                if (node.has(prop)) {
                    JsonNode colorNode = node.path(prop);
                    if (colorNode.isArray() && colorNode.size() > 0) {
                        colorNode = colorNode.get(0).path("color");
                    }
                    String colorValue = convertToHexColor(colorNode);
                    if (!colorValue.isEmpty()) {
                        String tokenName = "color-" + componentName + "-" + prop;
                        colorTokens.put(tokenName, colorValue);
                    }
                }
            }

            // 타이포그래피 토큰 추출
            JsonNode style = node.path("style");
            if (!style.isMissingNode()) {
                StringBuilder typographyValue = new StringBuilder();
                for (String prop : TYPOGRAPHY_PROPERTIES) {
                    JsonNode value = style.path(prop);
                    if (!value.isMissingNode()) {
                        if (typographyValue.length() > 0) typographyValue.append(", ");
                        typographyValue.append(value.asText());
                    }
                }
                if (typographyValue.length() > 0) {
                    String tokenName = "typography-" + componentName;
                    typographyTokens.put(tokenName, typographyValue.toString());
                }
            }

            // 간격 토큰 추출
            for (String prop : SPACING_PROPERTIES) {
                if (node.has(prop)) {
                    String value = node.path(prop).asText();
                    if (!value.isEmpty()) {
                        String tokenName = "spacing-" + componentName + "-" + prop;
                        spacingTokens.put(tokenName, value + "px");
                    }
                }
            }

            // 테두리 반경 토큰 추출
            for (String prop : BORDER_RADIUS_PROPERTIES) {
                if (node.has(prop)) {
                    String value = node.path(prop).asText();
                    if (!value.isEmpty()) {
                        String tokenName = "radius-" + componentName + "-" + prop;
                        borderRadiusTokens.put(tokenName, value + "px");
                    }
                }
            }

            // 자식 노드 순회
            node.fields().forEachRemaining(entry -> 
                extractTokensFromNode(entry.getValue(), colorTokens, typographyTokens, spacingTokens, borderRadiusTokens)
            );
        } else if (node.isArray()) {
            node.forEach(item -> 
                extractTokensFromNode(item, colorTokens, typographyTokens, spacingTokens, borderRadiusTokens)
            );
        }
    }

    private String convertToHexColor(JsonNode colorNode) {
        if (colorNode == null || colorNode.isMissingNode()) {
            return "";
        }

        try {
            double r = colorNode.path("r").asDouble();
            double g = colorNode.path("g").asDouble();
            double b = colorNode.path("b").asDouble();
            double a = colorNode.has("a") ? colorNode.path("a").asDouble() : 1.0;

            int rInt = Math.min(255, Math.max(0, (int)(r * 255 + 0.5)));
            int gInt = Math.min(255, Math.max(0, (int)(g * 255 + 0.5)));
            int bInt = Math.min(255, Math.max(0, (int)(b * 255 + 0.5)));

            if (a < 1.0) {
                return String.format("rgba(%d, %d, %d, %.2f)", rInt, gInt, bInt, a);
            }

            return String.format("#%02X%02X%02X", rInt, gInt, bInt);
        } catch (Exception e) {
            return "";
        }
    }

    private void saveTokensAsJson(Map<String, String> colorTokens,
                                Map<String, String> typographyTokens,
                                Map<String, String> spacingTokens,
                                Map<String, String> borderRadiusTokens,
                                String outputPath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        // 각 토큰 타입별로 객체 생성
        ObjectNode colors = mapper.createObjectNode();
        colorTokens.forEach(colors::put);
        root.set("colors", colors);

        ObjectNode typography = mapper.createObjectNode();
        typographyTokens.forEach(typography::put);
        root.set("typography", typography);

        ObjectNode spacing = mapper.createObjectNode();
        spacingTokens.forEach(spacing::put);
        root.set("spacing", spacing);

        ObjectNode borderRadius = mapper.createObjectNode();
        borderRadiusTokens.forEach(borderRadius::put);
        root.set("borderRadius", borderRadius);

        // JSON 파일로 저장
        String fileName = String.format("%s/design-tokens.json", outputPath);
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(fileName), root);
    }

    private void saveTokensAsScss(Map<String, String> colorTokens,
                                Map<String, String> typographyTokens,
                                Map<String, String> spacingTokens,
                                Map<String, String> borderRadiusTokens,
                                String outputPath) throws Exception {
        StringBuilder scss = new StringBuilder();
        
        // 색상 변수
        scss.append("// Colors\n");
        colorTokens.forEach((key, value) -> 
            scss.append(String.format("$%s: %s;\n", key, value))
        );

        // 타이포그래피 변수
        scss.append("\n// Typography\n");
        typographyTokens.forEach((key, value) -> 
            scss.append(String.format("$%s: %s;\n", key, value))
        );

        // 간격 변수
        scss.append("\n// Spacing\n");
        spacingTokens.forEach((key, value) -> 
            scss.append(String.format("$%s: %s;\n", key, value))
        );

        // 테두리 반경 변수
        scss.append("\n// Border Radius\n");
        borderRadiusTokens.forEach((key, value) -> 
            scss.append(String.format("$%s: %s;\n", key, value))
        );

        // SCSS 파일로 저장
        String fileName = String.format("%s/design-tokens.scss", outputPath);
        java.nio.file.Files.write(new File(fileName).toPath(), scss.toString().getBytes());
    }

    private void saveTokensAsCss(Map<String, String> colorTokens,
                               Map<String, String> typographyTokens,
                               Map<String, String> spacingTokens,
                               Map<String, String> borderRadiusTokens,
                               String outputPath) throws Exception {
        StringBuilder css = new StringBuilder();
        css.append(":root {\n");

        // 색상 변수
        css.append("  /* Colors */\n");
        colorTokens.forEach((key, value) -> 
            css.append(String.format("  --%s: %s;\n", key, value))
        );

        // 타이포그래피 변수
        css.append("\n  /* Typography */\n");
        typographyTokens.forEach((key, value) -> 
            css.append(String.format("  --%s: %s;\n", key, value))
        );

        // 간격 변수
        css.append("\n  /* Spacing */\n");
        spacingTokens.forEach((key, value) -> 
            css.append(String.format("  --%s: %s;\n", key, value))
        );

        // 테두리 반경 변수
        css.append("\n  /* Border Radius */\n");
        borderRadiusTokens.forEach((key, value) -> 
            css.append(String.format("  --%s: %s;\n", key, value))
        );

        css.append("}\n");

        // CSS 파일로 저장
        String fileName = String.format("%s/design-tokens.css", outputPath);
        java.nio.file.Files.write(new File(fileName).toPath(), css.toString().getBytes());
    }

    private void saveTokensAsTailwind(Map<String, String> colorTokens,
                                    Map<String, String> typographyTokens,
                                    Map<String, String> spacingTokens,
                                    Map<String, String> borderRadiusTokens,
                                    String outputPath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode config = mapper.createObjectNode();

        // theme 설정
        ObjectNode theme = mapper.createObjectNode();
        
        // 색상 설정
        ObjectNode colors = mapper.createObjectNode();
        colorTokens.forEach(colors::put);
        theme.set("colors", colors);

        // 타이포그래피 설정
        ObjectNode fontSize = mapper.createObjectNode();
        typographyTokens.forEach(fontSize::put);
        theme.set("fontSize", fontSize);

        // 간격 설정
        ObjectNode spacing = mapper.createObjectNode();
        spacingTokens.forEach(spacing::put);
        theme.set("spacing", spacing);

        // 테두리 반경 설정
        ObjectNode borderRadius = mapper.createObjectNode();
        borderRadiusTokens.forEach(borderRadius::put);
        theme.set("borderRadius", borderRadius);

        config.set("theme", theme);

        // Tailwind 설정 파일로 저장
        String fileName = String.format("%s/tailwind.config.js", outputPath);
        String configContent = "module.exports = " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
        java.nio.file.Files.write(new File(fileName).toPath(), configContent.getBytes());
    }

    private void saveTokensAsStyledComponents(Map<String, String> colorTokens,
                                           Map<String, String> typographyTokens,
                                           Map<String, String> spacingTokens,
                                           Map<String, String> borderRadiusTokens,
                                           String outputPath) throws Exception {
        StringBuilder styled = new StringBuilder();
        styled.append("export const theme = {\n");

        // 색상
        styled.append("  colors: {\n");
        colorTokens.forEach((key, value) -> 
            styled.append(String.format("    '%s': '%s',\n", key, value))
        );
        styled.append("  },\n\n");

        // 타이포그래피
        styled.append("  typography: {\n");
        typographyTokens.forEach((key, value) -> 
            styled.append(String.format("    '%s': '%s',\n", key, value))
        );
        styled.append("  },\n\n");

        // 간격
        styled.append("  spacing: {\n");
        spacingTokens.forEach((key, value) -> 
            styled.append(String.format("    '%s': '%s',\n", key, value))
        );
        styled.append("  },\n\n");

        // 테두리 반경
        styled.append("  borderRadius: {\n");
        borderRadiusTokens.forEach((key, value) -> 
            styled.append(String.format("    '%s': '%s',\n", key, value))
        );
        styled.append("  },\n");

        styled.append("};\n");

        // Styled Components 테마 파일로 저장
        String fileName = String.format("%s/theme.js", outputPath);
        java.nio.file.Files.write(new File(fileName).toPath(), styled.toString().getBytes());
    }
} 