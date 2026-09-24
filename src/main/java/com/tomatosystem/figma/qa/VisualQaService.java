package com.tomatosystem.figma.qa;

import com.tomatosystem.exconverter.service.ProgressLog;
import com.tomatosystem.figma.FigmaApiClient;
import com.tomatosystem.figma.FigmaImages;
import com.tomatosystem.figma.FigmaSettings;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Visual regression QA for one generated screen: Figma's own PNG of the frame (GET /v1/images) versus a headless
 * render of the generated CLX (e6-compiler → runtime page → Chrome), compared pixel by pixel ({@link PixelMatch}).
 * Writes clx-src/result/visual-qa/{date}/{name}/{figma.png, clx.png, diff.png, report.html, report.json}.
 * Without a Figma token the CLX render and report are still produced, without the diff.
 */
public final class VisualQaService {
	private VisualQaService() { }

	public static final class Report {
		public final String name;
		public final File dir;
		public File figmaPng;
		public File clxPng;
		public File diffPng;
		public File html;
		public File json;
		public int width;
		public int height;
		public double mismatch = -1;
		public final List<String> hotspots = new ArrayList<String>();
		public final List<String> notes = new ArrayList<String>();
		public String renderer = "";
		public String error;
		Report(String name, File dir) { this.name = name; this.dir = dir; }

		public String summary() {
			if (error != null) return "시각 QA 실패: " + error;
			StringBuilder s = new StringBuilder("시각 QA: ");
			if (mismatch < 0) s.append("Figma 렌더 없음(비교 생략)");
			else s.append(String.format(Locale.ROOT, "불일치 %.1f%%", mismatch * 100)).append(hotspots.isEmpty() ? "" : " · 주요 영역 " + hotspots);
			if (html != null) s.append(" · 리포트 ").append(html.getAbsolutePath());
			return s.toString();
		}
	}

	/**
	 * @param projectRoot project holding ci-lib/clx/e6-compiler.jar, exbuilder/runtime and clx-src (udc, theme)
	 * @param width       viewport = Figma frame size, so the responsive screen (EXB-FULL/DIV/PART) matches the design
	 * @param fileKey     with nodeId and token: the frame to fetch from Figma; any of them empty = no reference image
	 */
	public static Report run(File projectRoot, File clx, File js, String fileKey, String nodeId, int width, int height, String token, FigmaApiClient.Auth auth, String name) {
		return run(projectRoot, clx, js, fileKey, nodeId, width, height, token, auth, name, null);
	}

	/** @param outputRoot where the dated report folders go; null = {@code <projectRoot>/clx-src/result/visual-qa} */
	public static Report run(File projectRoot, File clx, File js, String fileKey, String nodeId, int width, int height, String token, FigmaApiClient.Auth auth, String name, File outputRoot) {
		String safe = (name == null || name.trim().isEmpty() ? clx.getName().replaceAll("\\.clx$", "") : name).replaceAll("[\\p{Cntrl}<>:\"/\\\\|?*]", "_").trim();
		File root = outputRoot != null ? outputRoot : new File(projectRoot, "clx-src/result/visual-qa");
		File dir = new File(root, LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "/" + safe);
		Report report = new Report(safe, dir);
		report.width = Math.max(320, width);
		report.height = Math.max(320, height);
		try {
			if (!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("결과 폴더를 만들 수 없습니다: " + dir.getAbsolutePath());
			BufferedImage reference = null;
			if (!isBlank(fileKey) && !isBlank(nodeId) && !isBlank(token)) {
				try {
					byte[] png = FigmaImages.render(fileKey, nodeId, 1.0, token, auth);
					report.figmaPng = new File(dir, "figma.png");
					Files.write(report.figmaPng.toPath(), png);
					reference = ImageIO.read(new ByteArrayInputStream(png));
					if (reference == null) throw new IllegalStateException("Figma PNG 를 읽을 수 없습니다");
					report.notes.add("Figma 렌더 " + reference.getWidth() + "x" + reference.getHeight() + " (scale 1)");
				} catch (RuntimeException e) {
					report.notes.add("Figma 렌더를 받지 못해 비교를 생략: " + e.getMessage());
				}
			} else {
				report.notes.add("Figma 파일키/노드/토큰이 없어 원본 렌더 없이 CLX 화면만 렌더링");
			}
			report.renderer = HeadlessBrowser.describe();
			if (report.renderer.isEmpty()) throw new IllegalStateException("헤드리스 브라우저(Chrome/Edge/Chromium) 또는 Playwright 를 찾지 못했습니다. figma.qa.browser 를 설정하세요.");
			File work = new File(dir, "work");
			ClxCompiler.Compiled compiled = ClxCompiler.compile(projectRoot, clx, js, work);
			Files.write(new File(dir, "compile.log").toPath(), compiled.log.getBytes(StandardCharsets.UTF_8));
			if (!compiled.ok()) throw new IllegalStateException("e6-compiler 오류: " + compiled.errors);
			report.clxPng = new File(dir, "clx.png");
			int settle = parseInt(FigmaSettings.get("figma.qa.settleMillis", "6000"), 6000);
			HeadlessBrowser.screenshot(compiled.outDir, "index.html", report.width, report.height, report.clxPng, settle);
			BufferedImage rendered = ImageIO.read(report.clxPng);
			if (rendered == null) throw new IllegalStateException("CLX 렌더 PNG 를 읽을 수 없습니다");
			report.notes.add("CLX 렌더 " + rendered.getWidth() + "x" + rendered.getHeight() + " (" + report.renderer + ")");
			if (blankRatio(rendered) > 0.999) report.notes.add("경고: CLX 렌더가 비어 있습니다 (런타임 로드 실패? work/out/index.html 를 브라우저로 열어 확인)");
			if (reference != null) {
				double threshold = parseDouble(FigmaSettings.get("figma.qa.threshold", "0.1"), 0.1);
				PixelMatch.Result diff = PixelMatch.compare(reference, rendered, threshold, false);
				report.diffPng = new File(dir, "diff.png");
				ImageIO.write(diff.diff, "png", report.diffPng);
				report.mismatch = diff.ratio();
				double[][] heat = PixelMatch.heatmap(diff, 8, 12);
				List<double[]> cells = new ArrayList<double[]>();
				for (int r = 0; r < 8; r++) { for (int c = 0; c < 12; c++) { if (heat[r][c] >= 0.05) cells.add(new double[] { heat[r][c], r, c }); } }
				cells.sort((x, y) -> Double.compare(y[0], x[0]));
				for (int i = 0; i < Math.min(5, cells.size()); i++) report.hotspots.add(String.format(Locale.ROOT, "%d행/%d열 %.0f%%", (int) cells.get(i)[1] + 1, (int) cells.get(i)[2] + 1, cells.get(i)[0] * 100));
				report.notes.add(String.format(Locale.ROOT, "픽셀 비교 %dx%d: 불일치 %d (%.2f%%), 안티앨리어싱 %d", diff.width, diff.height, diff.mismatched, diff.ratio() * 100, diff.antialiased));
				writeReport(report, heat);
			} else {
				writeReport(report, null);
			}
			if (!"true".equalsIgnoreCase(FigmaSettings.get("figma.qa.keepWork", "false"))) ClxCompiler.deleteTree(work.toPath());
			ProgressLog.step("{}", report.summary());
		} catch (Exception e) {
			report.error = e.getMessage();
			ProgressLog.step("시각 QA 실패 [{}]: {}", safe, e.getMessage());
			try { writeReport(report, null); } catch (Exception ignored) { /* the error is already in the report object */ }
		}
		return report;
	}

	private static void writeReport(Report report, double[][] heat) throws java.io.IOException {
		JSONObject json = new JSONObject()
			.put("name", report.name)
			.put("generatedAt", LocalDateTime.now().toString())
			.put("width", report.width).put("height", report.height)
			.put("mismatch", report.mismatch)
			.put("hotspots", new JSONArray(report.hotspots))
			.put("notes", new JSONArray(report.notes))
			.put("renderer", report.renderer)
			.put("error", report.error == null ? JSONObject.NULL : report.error)
			.put("figmaPng", report.figmaPng == null ? JSONObject.NULL : report.figmaPng.getName())
			.put("clxPng", report.clxPng == null ? JSONObject.NULL : report.clxPng.getName())
			.put("diffPng", report.diffPng == null ? JSONObject.NULL : report.diffPng.getName());
		report.json = new File(report.dir, "report.json");
		Files.write(report.json.toPath(), json.toString(2).getBytes(StandardCharsets.UTF_8));
		StringBuilder h = new StringBuilder();
		h.append("<!DOCTYPE html><html lang=\"ko\"><head><meta charset=\"UTF-8\"><title>Visual QA · ").append(esc(report.name)).append("</title>");
		h.append("<style>body{font-family:'Malgun Gothic',sans-serif;margin:16px;color:#222}h1{font-size:20px}table{border-collapse:collapse}td,th{border:1px solid #ccc;padding:3px 6px;font-size:12px;text-align:center}");
		h.append(".imgs{display:flex;gap:12px;flex-wrap:wrap}.imgs figure{margin:0;max-width:32%}.imgs img{width:100%;border:1px solid #ddd}figcaption{font-size:13px;color:#555}.bad{color:#c00;font-weight:bold}.ok{color:#080;font-weight:bold}.hot{background:#f99}.warm{background:#fdd}</style></head><body>");
		h.append("<h1>Figma vs CLX 시각 비교 · ").append(esc(report.name)).append("</h1>");
		if (report.error != null) h.append("<p class=\"bad\">실패: ").append(esc(report.error)).append("</p>");
		else if (report.mismatch >= 0) h.append("<p>불일치 <span class=\"").append(report.mismatch > 0.2 ? "bad" : "ok").append("\">").append(String.format(Locale.ROOT, "%.2f%%", report.mismatch * 100)).append("</span>")
			.append(report.hotspots.isEmpty() ? "" : " · 주요 영역: " + esc(String.join(", ", report.hotspots))).append("</p>");
		else h.append("<p>Figma 원본 렌더가 없어 비교하지 않았습니다.</p>");
		h.append("<div class=\"imgs\">");
		if (report.figmaPng != null) h.append("<figure><img src=\"figma.png\" alt=\"Figma\"><figcaption>Figma 렌더</figcaption></figure>");
		if (report.clxPng != null) h.append("<figure><img src=\"clx.png\" alt=\"CLX\"><figcaption>생성된 CLX 렌더</figcaption></figure>");
		if (report.diffPng != null) h.append("<figure><img src=\"diff.png\" alt=\"diff\"><figcaption>차이 (빨강 = 불일치, 노랑 = 안티앨리어싱)</figcaption></figure>");
		h.append("</div>");
		if (heat != null) {
			h.append("<h2 style=\"font-size:16px\">영역별 불일치 비율 (8행 × 12열)</h2><table>");
			for (double[] row : heat) {
				h.append("<tr>");
				for (double cell : row) h.append("<td class=\"").append(cell >= 0.2 ? "hot" : cell >= 0.05 ? "warm" : "").append("\">").append(String.format(Locale.ROOT, "%.0f", cell * 100)).append("</td>");
				h.append("</tr>");
			}
			h.append("</table>");
		}
		h.append("<h2 style=\"font-size:16px\">메모</h2><ul>");
		for (String note : report.notes) h.append("<li>").append(esc(note)).append("</li>");
		h.append("</ul><p style=\"color:#888;font-size:12px\">").append(LocalDateTime.now()).append(" · renderer ").append(esc(report.renderer)).append("</p></body></html>");
		report.html = new File(report.dir, "report.html");
		Files.write(report.html.toPath(), h.toString().getBytes(StandardCharsets.UTF_8));
	}

	static double blankRatio(BufferedImage image) {
		long blank = 0, total = 0;
		for (int y = 0; y < image.getHeight(); y += 3) {
			for (int x = 0; x < image.getWidth(); x += 3) {
				int rgb = image.getRGB(x, y);
				int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
				total++;
				if (r > 245 && g > 245 && b > 245) blank++;
			}
		}
		return total == 0 ? 1 : blank / (double) total;
	}

	private static String esc(String s) { return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
	private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
	private static int parseInt(String s, int d) { try { return Integer.parseInt(s.trim()); } catch (RuntimeException e) { return d; } }
	private static double parseDouble(String s, double d) { try { return Double.parseDouble(s.trim()); } catch (RuntimeException e) { return d; } }
}
