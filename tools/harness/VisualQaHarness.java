import com.tomatosystem.figma.qa.ClxCompiler;
import com.tomatosystem.figma.qa.HeadlessBrowser;
import com.tomatosystem.figma.qa.PixelMatch;
import com.tomatosystem.figma.qa.VisualQaService;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Not part of the Eclipse build. Visual QA pipeline without Figma access.
 *
 *   VisualQaHarness <project root> <generated.clx> <generated.js> <out dir>
 *
 * 1) pixelmatch port on synthetic images (identical → 0, shifted box → > 0, size mismatch padded);
 * 2) e6-compiler headless compile of the CLX + runtime page;
 * 3) headless browser screenshot of it (skipped, exit 0, when no Chrome/Edge/Chromium/Playwright is installed);
 * 4) VisualQaService end to end with the CLX render as its own reference (report written, mismatch 0%).
 * Exit code 1 on failure.
 */
public class VisualQaHarness {
	public static void main(String[] args) throws Exception {
		File projectRoot = new File(args[0]);
		File clx = new File(args[1]);
		File js = new File(args[2]);
		File out = new File(args[3]);
		out.mkdirs();
		int failures = 0;

		// 1) pixel comparison
		BufferedImage a = box(400, 300, 50, 50, 120, 60, Color.BLUE);
		BufferedImage b = box(400, 300, 50, 50, 120, 60, Color.BLUE);
		PixelMatch.Result same = PixelMatch.compare(a, b, 0.1, false);
		BufferedImage c = box(400, 300, 70, 50, 120, 60, Color.BLUE);
		PixelMatch.Result shifted = PixelMatch.compare(a, c, 0.1, false);
		BufferedImage d = box(420, 300, 50, 50, 120, 60, Color.BLUE);
		PixelMatch.Result padded = PixelMatch.compare(a, d, 0.1, false);
		ImageIO.write(shifted.diff, "png", new File(out, "pixelmatch-shifted.png"));
		double[][] heat = PixelMatch.heatmap(shifted, 8, 12);
		failures += check(String.format("1 pixelmatch: 동일 %d, 20px 이동 %d (%.2f%%), 크기 다름 %dx%d 불일치 %d", same.mismatched, shifted.mismatched, shifted.ratio() * 100, padded.width, padded.height, padded.mismatched),
			same.mismatched == 0 && shifted.mismatched == 2 * 20 * 60 && padded.width == 420 && padded.mismatched == 0 && heat[1][1] > 0);

		// 2) compile
		ClxCompiler.Compiled compiled = ClxCompiler.compile(projectRoot, clx, js, new File(out, "work"));
		failures += check("2 e6-compiler 컴파일 " + compiled.appUri + " " + compiled.errors, compiled.ok() && compiled.indexHtml().isFile() && new File(compiled.outDir, "runtime/cleopatra.js").isFile());

		// 3) screenshot
		String renderer = HeadlessBrowser.describe();
		if (renderer.isEmpty()) {
			System.out.println("SKIP 3/4 헤드리스 브라우저 없음 (Chrome/Edge/Chromium 또는 npx playwright). figma.qa.browser 로 지정 가능.");
			System.out.println((failures == 0 ? "ALL OK" : failures + " FAILED") + " (렌더 검사 생략)");
			if (failures > 0) System.exit(1);
			return;
		}
		File png = new File(out, "clx.png");
		HeadlessBrowser.screenshot(compiled.outDir, "index.html", 1654, 940, png, 6000);
		BufferedImage rendered = ImageIO.read(png);
		double blank = blankRatio(rendered);
		failures += check(String.format("3 스크린샷 %s %dx%d, 빈 화면 비율 %.3f (%s)", png.getName(), rendered.getWidth(), rendered.getHeight(), blank, renderer),
			rendered.getWidth() == 1654 && rendered.getHeight() == 940 && blank < 0.99);

		// 4) service end to end, the render as its own reference (no Figma token): report files + 0% mismatch against itself
		// Reports go under the harness out dir, not into the project's clx-src/result.
		VisualQaService.Report report = VisualQaService.run(projectRoot, clx, js, "", "", 1654, 940, "", com.tomatosystem.figma.FigmaApiClient.Auth.PERSONAL_TOKEN, "harness-" + clx.getName().replaceAll("\\.clx$", ""), new File(out, "reports"));
		System.out.println("   " + report.summary());
		failures += check("4 VisualQaService: 리포트 " + (report.html == null ? "-" : report.html.getName()) + ", clx.png, Figma 없음 → 비교 생략",
			report.error == null && report.html.isFile() && report.clxPng.isFile() && report.mismatch < 0 && report.json.isFile());
		BufferedImage self = ImageIO.read(report.clxPng);
		PixelMatch.Result selfDiff = PixelMatch.compare(self, self, 0.1, false);
		failures += check("5 자기 자신과 비교 0%", selfDiff.mismatched == 0);

		System.out.println((failures == 0 ? "ALL OK" : failures + " FAILED") + " → " + out.getAbsolutePath());
		if (failures > 0) System.exit(1);
	}

	private static BufferedImage box(int w, int h, int x, int y, int bw, int bh, Color color) {
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, w, h);
		g.setColor(color);
		g.fillRect(x, y, bw, bh);
		g.dispose();
		return image;
	}

	private static double blankRatio(BufferedImage image) {
		long blank = 0, total = 0;
		for (int y = 0; y < image.getHeight(); y += 3) {
			for (int x = 0; x < image.getWidth(); x += 3) {
				int rgb = image.getRGB(x, y);
				total++;
				if (((rgb >> 16) & 0xFF) > 245 && ((rgb >> 8) & 0xFF) > 245 && (rgb & 0xFF) > 245) blank++;
			}
		}
		return blank / (double) total;
	}

	private static int check(String name, boolean ok) {
		System.out.println((ok ? "OK   " : "FAIL ") + name);
		return ok ? 0 : 1;
	}
}
