package com.tomatosystem.figma;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

/**
 * Figma settings: JVM -Dkey=value > environment variable (dots → underscores, upper case, e.g. FIGMA_CLIENT_SECRET)
 * > classpath application.properties. Keep secrets (client secret, personal access token) in the environment.
 */
public final class FigmaSettings {
	private static final Properties FILE = load();

	private FigmaSettings() { }

	public static String get(String key, String defaultValue) {
		String value = System.getProperty(key);
		if (isBlank(value)) value = System.getenv(key.replace('.', '_').toUpperCase(Locale.ROOT));
		if (isBlank(value)) value = FILE.getProperty(key);
		return isBlank(value) ? defaultValue : value.trim();
	}

	private static Properties load() {
		Properties properties = new Properties();
		try (InputStream input = FigmaSettings.class.getClassLoader().getResourceAsStream("application.properties")) {
			if (input != null) properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
		} catch (Exception ignored) { /* defaults apply */ }
		return properties;
	}

	private static boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
}
