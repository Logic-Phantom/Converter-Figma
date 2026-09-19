package com.tomatosystem.exconverter.service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

/**
 * Configuration lookup: JVM system property, then environment variable
 * (dots → underscores, upper case), then classpath exconverter/exconverter.properties.
 * The properties file lets Eclipse/Tomcat users configure Ollama without JVM arguments.
 */
public final class ExConverterConfig {
	private static final Properties FILE = load();

	private ExConverterConfig() { }

	public static String get(String key, String defaultValue) {
		String value = System.getProperty(key);
		if (isBlank(value)) value = System.getenv(key.replace('.', '_').toUpperCase(Locale.ROOT));
		if (isBlank(value)) value = FILE.getProperty(key);
		return isBlank(value) ? defaultValue : value.trim();
	}

	public static int getInt(String key, int defaultValue) {
		try { return Integer.parseInt(get(key, String.valueOf(defaultValue))); } catch (NumberFormatException e) { return defaultValue; }
	}

	private static Properties load() {
		Properties properties = new Properties();
		try (InputStream input = ExConverterConfig.class.getClassLoader().getResourceAsStream("exconverter/exconverter.properties")) {
			if (input != null) properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
		} catch (Exception ignored) { /* Defaults apply. */ }
		return properties;
	}

	private static boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
}
