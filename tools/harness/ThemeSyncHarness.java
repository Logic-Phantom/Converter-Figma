import com.tomatosystem.figma.theme.FigmaThemeSync;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.json.JSONObject;

/**
 * Not part of the Eclipse build. Figma design tokens → theme LESS, offline.
 *
 *   ThemeSyncHarness <project root> <out dir> <figma file.json> [variables.json]
 *
 * Copies clx-src/theme of the project into out/project/clx-src/theme (the real theme is never touched), syncs the
 * tokens of the saved file JSON (styles) and of the sample variables response into it twice (the import must be
 * added once), and checks the LESS files. Exit code 1 on failure. Compile out/project with e6-compiler afterwards
 * to prove the LESS still builds (run-all.sh does).
 */
public class ThemeSyncHarness {
	public static void main(String[] args) throws Exception {
		File projectRoot = new File(args[0]);
		File out = new File(args[1]);
		JSONObject file = new JSONObject(new String(Files.readAllBytes(new File(args[2]).toPath()), StandardCharsets.UTF_8));
		JSONObject variables = args.length > 3 ? new JSONObject(new String(Files.readAllBytes(new File(args[3]).toPath()), StandardCharsets.UTF_8)) : null;
		File project = new File(out, "project");
		copyTheme(new File(projectRoot, "clx-src/theme"), new File(project, "clx-src/theme"));
		for (String name : new String[] { ".project", ".settings" }) copyAny(new File(projectRoot, name), new File(project, name));
		for (String name : new String[] { "env.json", "language.json" }) copyAny(new File(projectRoot, "clx-src/" + name), new File(project, "clx-src/" + name));
		int failures = 0;

		List<FigmaThemeSync.Token> styleTokens = FigmaThemeSync.fromStyles(file);
		int colors = 0, fonts = 0;
		for (FigmaThemeSync.Token t : styleTokens) { if ("color".equals(t.kind)) colors++; if ("font".equals(t.kind)) fonts++; }
		failures += check("1 파일 styles → 토큰 " + styleTokens.size() + "개 (color " + colors + ", font " + fonts + ")", colors >= 5 && fonts >= 3);

		List<FigmaThemeSync.Token> variableTokens = variables == null ? null : FigmaThemeSync.fromVariables(variables);
		if (variableTokens != null) {
			FigmaThemeSync.Token focus = find(variableTokens, "Brand/color/focus", "");
			FigmaThemeSync.Token dark = find(variableTokens, "Brand/color/primary/500", "Dark");
			FigmaThemeSync.Token radius = find(variableTokens, "Brand/radius/md", "");
			FigmaThemeSync.Token family = find(variableTokens, "Brand/font/family", "");
			failures += check("2 variables → 토큰 " + variableTokens.size() + "개: alias 해석 " + (focus == null ? "-" : focus.value) + ", Dark 모드 " + (dark == null ? "-" : dark.lessName)
				+ ", radius " + (radius == null ? "-" : radius.value) + ", font " + (family == null ? "-" : family.value),
				variableTokens.size() == 16 && focus != null && "#1369c5".equals(focus.value) && dark != null && dark.lessName.endsWith("--dark")
				&& radius != null && "6px".equals(radius.value) && family != null && "\"Pretendard\"".equals(family.value));
		}

		FigmaThemeSync.Result first = FigmaThemeSync.sync(project, variableTokens, styleTokens, true, "harness");
		System.out.println("   " + first.summary().replace("\n", "\n   "));
		String tokensLess = new String(Files.readAllBytes(first.tokensLess.toPath()), StandardCharsets.UTF_8);
		String themeLess = new String(Files.readAllBytes(first.themeLess.toPath()), StandardCharsets.UTF_8);
		String main = new String(Files.readAllBytes(new File(project, "clx-src/theme/cleopatra-theme.less").toPath()), StandardCharsets.UTF_8);
		failures += check("3 figma-tokens.part.less 에 @figma- 변수", tokensLess.contains("@figma-") && tokensLess.split("\n@figma-").length - 1 == first.tokens.size());
		failures += check("4 전역 변수 매핑 (" + first.mapped.keySet() + ")", first.mapped.containsKey("focus-border-color") && first.mapped.containsKey("default-text-color")
			&& themeLess.contains("@focus-border-color: @figma-") && themeLess.contains("@import \"figma-tokens.part.less\";"));
		failures += check("5 cleopatra-theme.less 에 import 추가 (settings 다음 줄)", first.importAdded && main.indexOf("@import \"figma/figma-theme.part.less\";") > main.indexOf("@import \"settings.part.less\";"));

		FigmaThemeSync.Result second = FigmaThemeSync.sync(project, variableTokens, styleTokens, true, "harness");
		String mainAgain = new String(Files.readAllBytes(new File(project, "clx-src/theme/cleopatra-theme.less").toPath()), StandardCharsets.UTF_8);
		failures += check("6 두 번째 동기화는 import 를 다시 넣지 않음", !second.importAdded && mainAgain.equals(main));

		File appliedFalse = new File(out, "project-noapply");
		copyTheme(new File(projectRoot, "clx-src/theme"), new File(appliedFalse, "clx-src/theme"));
		FigmaThemeSync.Result third = FigmaThemeSync.sync(appliedFalse, variableTokens, styleTokens, false, "harness");
		String untouched = new String(Files.readAllBytes(new File(appliedFalse, "clx-src/theme/cleopatra-theme.less").toPath()), StandardCharsets.UTF_8);
		failures += check("7 apply=false 면 cleopatra-theme.less 불변, 파일만 생성", !third.importAdded && !untouched.contains("figma/") && third.tokensLess.isFile());

		System.out.println((failures == 0 ? "ALL OK" : failures + " FAILED") + " → " + project.getAbsolutePath());
		if (failures > 0) System.exit(1);
	}

	private static FigmaThemeSync.Token find(List<FigmaThemeSync.Token> tokens, String name, String mode) {
		for (FigmaThemeSync.Token t : tokens) { if (t.name.equals(name) && t.mode.equals(mode)) return t; }
		return null;
	}

	private static void copyTheme(File from, File to) throws Exception {
		if (to.exists()) { java.nio.file.Files.walk(to.toPath()).sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete()); }
		copyAny(from, to);
	}

	private static void copyAny(File from, File to) throws Exception {
		if (!from.exists()) return;
		if (from.isDirectory()) {
			to.mkdirs();
			File[] children = from.listFiles();
			if (children != null) for (File c : children) copyAny(c, new File(to, c.getName()));
		} else {
			to.getParentFile().mkdirs();
			Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static int check(String name, boolean ok) {
		System.out.println((ok ? "OK   " : "FAIL ") + name);
		return ok ? 0 : 1;
	}
}
