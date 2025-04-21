package com.tomatosystem.utill;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ComponentKeywordProperties {

    private static final Properties props = new Properties();

    static {
        try (InputStream input = ComponentKeywordProperties.class.getClassLoader()
                .getResourceAsStream("ComponentType.properties")) {

            if (input == null) {
                throw new RuntimeException("ComponentType.properties 파일을 찾을 수 없습니다.");
            }

            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("ComponentType.properties 읽기 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 주어진 키에 해당하는 값을 반환
     * @param key 프로퍼티 키
     * @return 프로퍼티 값
     */
    public static String get(String key) {
        return props.getProperty(key);
    }

    /**
     * 이름과 비교하여 특정 키워드가 포함된 경우 true 반환
     * @param name 요소 이름
     * @param key 프로퍼티 키
     * @return 이름에 키워드가 포함되었으면 true
     */
    public static boolean nameMatches(String name, String key) {
        String keyword = get(key);
        return name != null && keyword != null && name.toLowerCase().contains(keyword.toLowerCase());
    }
}
