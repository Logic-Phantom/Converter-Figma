package com.tomatosystem.exconverter.service;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Locates the eXBuilder6 project root so generated CLX/JS land under clx-src/result.
 * Eclipse WTP runs Tomcat from workspace/.metadata/.../tmpN, so ancestors never contain
 * clx-src; the resolver also scans workspace project folders one level down.
 */
public final class ProjectRootResolver {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProjectRootResolver.class);

	private ProjectRootResolver() {}

	public static File resolve(ServletContext servletContext) {
		String configured = firstNonBlank(ExConverterConfig.get("exconverter.project.root", ""));
		if (configured != null) {
			File root = new File(configured);
			if (isProjectRoot(root)) return root.getAbsoluteFile();
			throw new IllegalStateException("exconverter.project.root is set but clx-src was not found: " + root.getAbsolutePath());
		}
		for (File seed : seeds(servletContext)) {
			File found = walkForProjectRoot(seed);
			if (found != null) {
				LOGGER.info("Resolved eXConverter project root: {}", found.getAbsolutePath());
				return found;
			}
		}
		throw new IllegalStateException("Cannot locate clx-src under the Eclipse workspace. Set -Dexconverter.project.root to the Converter-Figma (eXCoverter-Figma) folder.");
	}

	public static File resultDirectory(ServletContext servletContext, String dateFolder) {
		String configuredResult = firstNonBlank(ExConverterConfig.get("exconverter.clx.result.root", ""));
		File resultRoot = configuredResult != null ? new File(configuredResult) : new File(resolve(servletContext), "clx-src" + File.separator + ExConverterConfig.get("exconverter.clx.result.folder", "result"));
		File dated = new File(resultRoot, dateFolder);
		if (!dated.exists() && !dated.mkdirs()) throw new IllegalStateException("Could not create result directory: " + dated.getAbsolutePath());
		LOGGER.info("CLX/JS result directory: {}", dated.getAbsolutePath());
		return dated;
	}

	private static List<File> seeds(ServletContext servletContext) {
		List<File> seeds = new ArrayList<File>();
		add(seeds, System.getProperty("user.dir"));
		add(seeds, System.getProperty("catalina.base"));
		add(seeds, System.getProperty("catalina.home"));
		try {
			URL location = ProjectRootResolver.class.getProtectionDomain().getCodeSource().getLocation();
			if (location != null && "file".equalsIgnoreCase(location.getProtocol())) add(seeds, new File(location.toURI()).getAbsolutePath());
		} catch (Exception ignored) { /* Other seeds remain valid. */ }
		if (servletContext != null) {
			add(seeds, servletContext.getRealPath("/"));
			add(seeds, servletContext.getRealPath("/WEB-INF/classes"));
		}
		add(seeds, "C:\\eclipse_AI\\Converter-Figma");
		add(seeds, "C:\\eclipse_AI\\eXCoverter-Figma");
		return seeds;
	}

	private static File walkForProjectRoot(File start) {
		File current = start == null ? null : start.getAbsoluteFile();
		for (int i = 0; i < 16 && current != null; i++) {
			if (isProjectRoot(current)) return current;
			File nested = findProjectAmongChildren(current);
			if (nested != null) return nested;
			if (".metadata".equals(current.getName()) && current.getParentFile() != null) {
				File inWorkspace = findProjectAmongChildren(current.getParentFile());
				if (inWorkspace != null) return inWorkspace;
			}
			current = current.getParentFile();
		}
		return null;
	}

	private static File findProjectAmongChildren(File parent) {
		if (parent == null || parent.getParentFile() == null || !parent.isDirectory()) return null;
		// Converter-Figma copy: prefer this project so results never land in the sibling eXConverter-AI project.
		String[] preferred = { "Converter-Figma", "eXCoverter-Figma" };
		for (String name : preferred) {
			File candidate = new File(parent, name);
			if (isProjectRoot(candidate)) return candidate;
		}
		File[] children = parent.listFiles();
		if (children == null || children.length > 80) return null;
		for (File child : children) {
			if (isProjectRoot(child)) return child;
		}
		return null;
	}

	private static boolean isProjectRoot(File candidate) {
		if (candidate == null || !candidate.isDirectory()) return false;
		File clxSrc = new File(candidate, "clx-src");
		return clxSrc.isDirectory();
	}

	private static void add(List<File> seeds, String path) {
		if (path == null || path.trim().isEmpty()) return;
		seeds.add(new File(path.trim()));
	}

	private static String firstNonBlank(String... values) {
		if (values == null) return null;
		for (String value : values) { if (value != null && !value.trim().isEmpty()) return value.trim(); }
		return null;
	}
}
