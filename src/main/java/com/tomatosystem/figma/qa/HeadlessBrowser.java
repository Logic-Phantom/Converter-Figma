package com.tomatosystem.figma.qa;

import com.sun.net.httpserver.HttpServer;
import com.tomatosystem.exconverter.service.ProgressLog;
import com.tomatosystem.figma.FigmaSettings;
import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Screenshots a static site folder with whatever headless browser the machine has, without Node or Playwright:
 * the folder is served on 127.0.0.1 (file:// blocks the runtime's XHRs) and Chrome/Edge/Chromium is run with
 * {@code --headless --screenshot}. {@code figma.qa.browser} names the executable; otherwise the usual install
 * locations are tried. {@code figma.qa.renderer=playwright} uses {@code npx playwright screenshot} instead.
 */
public final class HeadlessBrowser {
	private HeadlessBrowser() { }

	/** The browser executable, or null when none is installed. */
	public static File locate() {
		String configured = FigmaSettings.get("figma.qa.browser", "");
		if (!configured.isEmpty()) { File f = new File(configured); return f.canExecute() ? f : null; }
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		List<String> candidates = new ArrayList<String>();
		if (os.contains("mac")) {
			candidates.add("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
			candidates.add("/Applications/Chromium.app/Contents/MacOS/Chromium");
			candidates.add("/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge");
			candidates.add("/Applications/Brave Browser.app/Contents/MacOS/Brave Browser");
		} else if (os.contains("win")) {
			for (String base : new String[] { System.getenv("ProgramFiles"), System.getenv("ProgramFiles(x86)"), System.getenv("LocalAppData") }) {
				if (base == null || base.isEmpty()) continue;
				candidates.add(base + "\\Google\\Chrome\\Application\\chrome.exe");
				candidates.add(base + "\\Microsoft\\Edge\\Application\\msedge.exe");
				candidates.add(base + "\\Chromium\\Application\\chrome.exe");
			}
		} else {
			for (String name : new String[] { "google-chrome", "google-chrome-stable", "chromium", "chromium-browser", "microsoft-edge" }) {
				File onPath = onPath(name);
				if (onPath != null) candidates.add(onPath.getAbsolutePath());
			}
		}
		for (String c : candidates) { File f = new File(c); if (f.canExecute()) return f; }
		return null;
	}

	public static boolean playwrightRequested() { return "playwright".equalsIgnoreCase(FigmaSettings.get("figma.qa.renderer", "auto")); }

	/** "chrome:/path", "playwright" or "" when nothing can render. */
	public static String describe() {
		if (playwrightRequested()) return onPath("npx") != null ? "playwright" : "";
		File browser = locate();
		if (browser != null) return "chrome:" + browser.getAbsolutePath();
		return onPath("npx") != null ? "playwright" : "";
	}

	/**
	 * Serves {@code root} locally and screenshots {@code relativePath} at the given viewport.
	 *
	 * @param settleMillis virtual time given to the page before the capture (runtime boot + layout)
	 */
	public static void screenshot(File root, String relativePath, int width, int height, File png, int settleMillis) {
		HttpServer server = null;
		try {
			server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			File base = root.getCanonicalFile();
			server.createContext("/", exchange -> {
				String path = exchange.getRequestURI().getPath();
				if (path.endsWith("/")) path += "index.html";
				File file = new File(base, path).getCanonicalFile();
				if (!file.getPath().startsWith(base.getPath()) || !file.isFile()) { exchange.sendResponseHeaders(404, -1); exchange.close(); return; }
				byte[] bytes = Files.readAllBytes(file.toPath());
				exchange.getResponseHeaders().add("Content-Type", contentType(file.getName()));
				exchange.getResponseHeaders().add("Cache-Control", "no-store");
				exchange.sendResponseHeaders(200, bytes.length);
				try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
			});
			server.start();
			String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/" + relativePath.replace('\\', '/');
			if (png.getParentFile() != null) png.getParentFile().mkdirs();
			Files.deleteIfExists(png.toPath());
			List<String> command = command(url, width, height, png, settleMillis);
			ProgressLog.step("헤드리스 렌더: {} ({}x{})", url, width, height);
			ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
			Process process = builder.start();
			// Chrome's helper processes inherit the pipe and may outlive the capture, so the output is drained on the side
			// and the screenshot file itself is the completion signal; whatever is still alive afterwards is killed.
			java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
			Thread drain = new Thread(() -> { try { process.getInputStream().transferTo(output); } catch (java.io.IOException ignored) { /* pipe closed */ } }, "figma-qa-browser-output");
			drain.setDaemon(true);
			drain.start();
			int timeoutSeconds = Math.max(15, parseInt(FigmaSettings.get("figma.qa.browserTimeoutSeconds", "90"), 90));
			long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
			long stableSince = -1;
			long lastSize = -1;
			while (System.currentTimeMillis() < deadline) {
				if (!process.isAlive()) break;
				if (png.isFile() && png.length() > 0) {
					if (png.length() == lastSize) { if (stableSince < 0) stableSince = System.currentTimeMillis(); else if (System.currentTimeMillis() - stableSince > 1500) break; }
					else { lastSize = png.length(); stableSince = -1; }
				}
				Thread.sleep(200);
			}
			boolean timedOut = process.isAlive() && !(png.isFile() && png.length() > 0);
			killTree(process);
			drain.join(2000);
			if (timedOut) throw new IllegalStateException("브라우저가 " + timeoutSeconds + "초 안에 스크린샷을 만들지 않았습니다 (figma.qa.browserTimeoutSeconds)");
			if (!png.isFile() || png.length() == 0) {
				String log = new String(output.toByteArray(), StandardCharsets.UTF_8);
				throw new IllegalStateException("스크린샷 파일이 만들어지지 않았습니다: " + command.get(0) + "\n" + (log.length() > 800 ? log.substring(log.length() - 800) : log));
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("스크린샷 중단", e);
		} catch (java.io.IOException e) {
			throw new IllegalStateException("스크린샷 실패: " + e.getMessage(), e);
		} finally {
			if (server != null) server.stop(0);
		}
	}

	private static List<String> command(String url, int width, int height, File png, int settleMillis) throws java.io.IOException {
		List<String> command = new ArrayList<String>();
		File browser = playwrightRequested() ? null : locate();
		if (browser != null) {
			File profile = Files.createTempDirectory("figma-qa-profile").toFile();
			command.add(browser.getAbsolutePath());
			command.add("--headless=new");
			command.add("--disable-gpu");
			command.add("--hide-scrollbars");
			command.add("--no-first-run");
			command.add("--no-default-browser-check");
			command.add("--disable-extensions");
			command.add("--force-device-scale-factor=1");
			command.add("--user-data-dir=" + profile.getAbsolutePath());
			command.add("--window-size=" + width + "," + height);
			command.add("--virtual-time-budget=" + Math.max(1000, settleMillis));
			command.add("--screenshot=" + png.getAbsolutePath());
			command.add(url);
			return command;
		}
		File npx = onPath(System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win") ? "npx.cmd" : "npx");
		if (npx == null) throw new IllegalStateException("헤드리스 브라우저를 찾지 못했습니다. figma.qa.browser 에 Chrome/Edge 실행 파일 경로를 지정하거나 Node + Playwright(npx playwright install chromium)를 설치하세요.");
		command.add(npx.getAbsolutePath());
		command.add("playwright");
		command.add("screenshot");
		command.add("--browser=chromium");
		command.add("--viewport-size=" + width + "," + height);
		command.add("--wait-for-timeout=" + Math.max(1000, settleMillis));
		command.add(url);
		command.add(png.getAbsolutePath());
		return command;
	}

	/** Chrome leaves GPU/renderer helpers behind when killed; take the whole tree down. */
	private static void killTree(Process process) {
		try { process.descendants().forEach(ProcessHandle::destroyForcibly); } catch (RuntimeException ignored) { /* best effort */ }
		if (process.isAlive()) process.destroyForcibly();
		try { process.waitFor(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
	}

	private static int parseInt(String value, int defaultValue) {
		try { return Integer.parseInt(value.trim()); } catch (RuntimeException e) { return defaultValue; }
	}

	private static File onPath(String name) {
		String path = System.getenv("PATH");
		if (path == null) return null;
		for (String dir : path.split(File.pathSeparator)) {
			File f = new File(dir, name);
			if (f.canExecute()) return f;
		}
		return null;
	}

	private static String contentType(String name) {
		String n = name.toLowerCase(Locale.ROOT);
		if (n.endsWith(".html")) return "text/html; charset=UTF-8";
		if (n.endsWith(".js")) return "application/javascript; charset=UTF-8";
		if (n.endsWith(".css")) return "text/css; charset=UTF-8";
		if (n.endsWith(".json")) return "application/json; charset=UTF-8";
		if (n.endsWith(".png")) return "image/png";
		if (n.endsWith(".svg")) return "image/svg+xml";
		if (n.endsWith(".gif")) return "image/gif";
		if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
		if (n.endsWith(".woff")) return "font/woff";
		if (n.endsWith(".woff2")) return "font/woff2";
		if (n.endsWith(".ttf")) return "font/ttf";
		return "application/octet-stream";
	}
}
