import com.tomatosystem.exconverter.model.UiIr;
import com.tomatosystem.exconverter.service.ClxGenerator;
import com.tomatosystem.exconverter.service.ClxValidator;
import com.tomatosystem.exconverter.service.TemplateCatalog;
import com.tomatosystem.exconverter.service.UiIrParser;
import com.tomatosystem.figma.FigmaDocument;
import com.tomatosystem.figma.FigmaUiIrExtractor;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONObject;

/**
 * Not part of the Eclipse build. Figma JSON (saved API responses) → UI-IR → template → CLX, without a server.
 *
 *   FigmaHarness <templates dir> <out dir> <figma.json | dir>...
 *
 * Writes out/<json name>__<screen>.clx and .ui-ir.json, prints the chosen template and ClxValidator errors.
 * Exit code 1 when any screen failed.
 */
public class FigmaHarness {
	public static void main(String[] args) throws Exception {
		File templates = new File(args[0]);
		File out = new File(args[1]);
		out.mkdirs();
		List<File> inputs = new ArrayList<File>();
		for (int i = 2; i < args.length; i++) collect(new File(args[i]), inputs);
		TemplateCatalog catalog = new TemplateCatalog();
		ClxGenerator generator = new ClxGenerator();
		int ok = 0, failed = 0;
		for (File input : inputs) {
			JSONObject json = new JSONObject(new String(Files.readAllBytes(input.toPath()), StandardCharsets.UTF_8));
			FigmaDocument document = new FigmaDocument(json);
			FigmaUiIrExtractor extractor = new FigmaUiIrExtractor(document);
			int index = 0;
			for (JSONObject screen : document.screens(Collections.<String>emptyList())) {
				String base = input.getName().replaceAll("\\.json$", "") + "__" + (index++);
				try {
					FigmaUiIrExtractor.Screen s = extractor.extract(screen);
					Files.write(new File(out, base + ".ui-ir.json").toPath(), s.uiIr.toString(2).getBytes(StandardCharsets.UTF_8));
					UiIr ir = UiIrParser.parse(s.uiIr.toString());
					TemplateCatalog.TemplateMatch match = catalog.selectFor(ir, templates);
					byte[] clx = generator.generate(match.getFile(), ir);
					List<String> errors = ClxValidator.validate(clx);
					Files.write(new File(out, base + ".clx").toPath(), clx);
					Files.write(new File(out, base + ".js").toPath(), ("/* " + base + ".js */\n").getBytes(StandardCharsets.UTF_8));
					StringBuilder regions = new StringBuilder();
					for (UiIr.Region r : ir.getRegions()) regions.append(regions.length() > 0 ? " → " : "").append(r.getType()).append(r.getSide().isEmpty() ? "" : "[" + r.getSide() + "]")
						.append(r.getFields().isEmpty() ? "" : "(f" + r.getFields().size() + ")").append(r.getColumns().isEmpty() ? "" : "(c" + r.getColumns().size() + ")");
					System.out.println((errors.isEmpty() ? "OK   " : "FAIL ") + base + " [" + s.name + "] → " + match.getId() + " (" + match.getScore() + ")\n       " + regions + (errors.isEmpty() ? "" : "\n       " + errors) + (ir.getWarnings().isEmpty() ? "" : "\n       warn " + ir.getWarnings()));
					if (errors.isEmpty()) ok++; else failed++;
				} catch (Exception e) {
					failed++;
					System.out.println("ERR  " + base + ": " + e);
				}
			}
		}
		System.out.println("screens ok=" + ok + " failures=" + failed);
		if (failed > 0) System.exit(1);
	}

	private static void collect(File f, List<File> out) {
		if (f.isDirectory()) { File[] children = f.listFiles(); if (children != null) { java.util.Arrays.sort(children); for (File c : children) collect(c, out); } }
		else if (f.getName().endsWith(".json")) out.add(f);
	}
}
