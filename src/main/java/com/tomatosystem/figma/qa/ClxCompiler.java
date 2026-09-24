package com.tomatosystem.figma.qa;

import com.tomatosystem.exconverter.service.ProgressLog;
import com.tomatosystem.figma.FigmaSettings;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Headless eXBuilder6 compile of one CLX/JS pair with ci-lib/clx/e6-compiler.jar, producing a static folder that a
 * browser can open: {@code index.html} (the compiler's own stub for {@code --main}), the compiled {@code .clx.js},
 * the UDC bundle, the theme CSS and a copy of the runtime (exbuilder/runtime, else ci-lib/clx/runtime).
 */
public final class ClxCompiler {
	private ClxCompiler() { }

	public static final class Compiled {
		public final File outDir;
		public final String appUri;
		public final String log;
		public final List<String> errors;
		Compiled(File outDir, String appUri, String log, List<String> errors) { this.outDir = outDir; this.appUri = appUri; this.log = log; this.errors = errors; }
		public boolean ok() { return errors.isEmpty(); }
		public File indexHtml() { return new File(outDir, "index.html"); }
	}

	public static File compilerJar(File projectRoot) {
		String configured = FigmaSettings.get("figma.qa.compiler", "");
		File jar = configured.isEmpty() ? new File(projectRoot, "ci-lib/clx/e6-compiler.jar") : new File(configured);
		if (!jar.isFile()) throw new IllegalStateException("e6-compiler.jar 을 찾지 못했습니다: " + jar.getAbsolutePath() + " (figma.qa.compiler 로 지정 가능)");
		return jar;
	}

	public static File runtimeDir(File projectRoot) {
		for (String candidate : new String[] { "exbuilder/runtime", "ci-lib/clx/runtime" }) {
			File dir = new File(projectRoot, candidate);
			if (new File(dir, "cleopatra.js").isFile()) return dir;
		}
		throw new IllegalStateException("eXBuilder6 런타임(cleopatra.js)을 찾지 못했습니다: exbuilder/runtime 또는 ci-lib/clx/runtime");
	}

	/**
	 * @param projectRoot the eXBuilder project (holds .project/.settings/clx-src with env.json, udc, theme)
	 * @param workDir     scratch folder; {@code project/} and {@code out/} are (re)created inside
	 */
	public static Compiled compile(File projectRoot, File clx, File js, File workDir) {
		try {
			File jar = compilerJar(projectRoot);
			File project = new File(workDir, "project");
			File out = new File(workDir, "out");
			deleteTree(project.toPath());
			deleteTree(out.toPath());
			File clxSrc = new File(project, "clx-src");
			clxSrc.mkdirs();
			for (String name : new String[] { ".project", ".settings" }) copyIfExists(new File(projectRoot, name), new File(project, name));
			for (String name : new String[] { "env.json", "language.json", "udc", "theme", "module" }) copyIfExists(new File(projectRoot, "clx-src/" + name), new File(clxSrc, name));
			String base = clx.getName().replaceAll("\\.clx$", "");
			File qa = new File(clxSrc, "qa");
			qa.mkdirs();
			Files.copy(clx.toPath(), new File(qa, base + ".clx").toPath(), StandardCopyOption.REPLACE_EXISTING);
			if (js != null && js.isFile()) Files.copy(js.toPath(), new File(qa, base + ".js").toPath(), StandardCopyOption.REPLACE_EXISTING);
			else Files.write(new File(qa, base + ".js").toPath(), ("/* " + base + ".js */\n").getBytes(StandardCharsets.UTF_8));
			String appUri = "qa/" + base;
			List<String> command = new ArrayList<String>();
			command.add(new File(System.getProperty("java.home"), "bin" + File.separator + (System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java")).getAbsolutePath());
			command.add("-Dfile.encoding=UTF-8");
			command.add("-jar");
			command.add(jar.getAbsolutePath());
			command.add("-s"); command.add(project.getAbsolutePath());
			command.add("-o"); command.add(out.getAbsolutePath());
			command.add("--main"); command.add(appUri);
			ProgressLog.step("e6-compiler 실행: {}", appUri);
			Process process = new ProcessBuilder(command).redirectErrorStream(true).directory(workDir).start();
			String log = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			if (!process.waitFor(300, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new IllegalStateException("e6-compiler 가 300초 안에 끝나지 않았습니다"); }
			List<String> errors = new ArrayList<String>();
			for (String line : log.split("\\r?\\n")) {
				String t = line.trim();
				if (t.matches("\\d+\\. ERROR:.*") || t.startsWith("ERROR:")) errors.add(t.replaceAll("\\(file:[^)]*\\)", "").trim());
			}
			if (!log.contains("BUILD SUCCESS") && errors.isEmpty()) errors.add("BUILD SUCCESS 가 출력되지 않았습니다 (종료 코드 " + process.exitValue() + ")");
			if (!new File(out, appUri + ".clx.js").isFile() && errors.isEmpty()) errors.add("컴파일 결과 " + appUri + ".clx.js 가 없습니다");
			File runtime = new File(out, "runtime");
			copyTree(runtimeDir(projectRoot).toPath(), runtime.toPath());
			if (!new File(out, "index.html").isFile()) Files.write(new File(out, "index.html").toPath(), stub(appUri).getBytes(StandardCharsets.UTF_8));
			return new Compiled(out, appUri, log, errors);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("e6-compiler 중단", e);
		} catch (IOException e) {
			throw new IllegalStateException("e6-compiler 실행 실패: " + e.getMessage(), e);
		}
	}

	/** The same page the compiler writes for --main, in case an older compiler does not. */
	static String stub(String appUri) {
		return "<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width\">"
			+ "<script src=\"runtime/cleopatra.js\"></script><script src=\"./cpr-lib/udc.js\"></script><script src=\"" + appUri + ".clx.js\"></script>"
			+ "<link rel=\"stylesheet\" href=\"runtime/css/cleopatra.css\"><link rel=\"stylesheet\" href=\"./theme/cleopatra-theme.css\">"
			+ "<style>html,body{margin:0;padding:0;height:100%}body{box-sizing:content-box;min-height:100%}</style></head>"
			+ "<body><script>cpr.core.Platform.INSTANCE.lookup(\"" + appUri + "\").createNewInstance().run();</script></body></html>\n";
	}

	private static void copyIfExists(File from, File to) throws IOException {
		if (!from.exists()) return;
		if (from.isDirectory()) copyTree(from.toPath(), to.toPath());
		else { to.getParentFile().mkdirs(); Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING); }
	}

	static void copyTree(Path from, Path to) throws IOException {
		try (Stream<Path> walk = Files.walk(from)) {
			for (Path p : (Iterable<Path>) walk::iterator) {
				Path target = to.resolve(from.relativize(p).toString());
				if (Files.isDirectory(p)) Files.createDirectories(target);
				else { Files.createDirectories(target.getParent()); Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING); }
			}
		}
	}

	static void deleteTree(Path root) throws IOException {
		if (!Files.exists(root)) return;
		try (Stream<Path> walk = Files.walk(root)) {
			List<Path> all = new ArrayList<Path>();
			walk.forEach(all::add);
			java.util.Collections.reverse(all);
			for (Path p : all) Files.deleteIfExists(p);
		}
	}
}
