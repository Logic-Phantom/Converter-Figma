import com.sun.net.httpserver.HttpServer;
import com.tomatosystem.exconverter.model.UiIr;
import com.tomatosystem.exconverter.service.ClxGenerator;
import com.tomatosystem.exconverter.service.ClxValidator;
import com.tomatosystem.exconverter.service.TemplateCatalog;
import com.tomatosystem.exconverter.service.UiIrParser;
import com.tomatosystem.figma.FigmaDocument;
import com.tomatosystem.figma.FigmaUiIrExtractor;
import com.tomatosystem.figma.ai.AiClient;
import com.tomatosystem.figma.ai.AiClients;
import com.tomatosystem.figma.ai.LabelCodeNamer;
import com.tomatosystem.figma.ai.UiIrCritic;
import com.tomatosystem.figma.ai.UiIrPatch;
import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Not part of the Eclipse build. Checks the AI-assisted path WITHOUT an API key by starting a fake Gemini server
 * on localhost that returns a canned patch answer, so request shape, patch whitelist, fallback and the generated
 * CLX can all be verified offline.
 *
 *   FigmaAiHarness <templates dir> <out dir> <figma.json>
 *
 * Scenarios: 1 no AI (must equal the deterministic result) · 2 valid patch applied · 3 forbidden ops rejected
 * · 4 broken answer → fallback · 5 column naming via fake AI + cache.
 */
public class FigmaAiHarness {
	private static String cannedAnswer = "{\"patches\":[]}";
	private static int calls;

	public static void main(String[] args) throws Exception {
		File templates = new File(args[0]);
		File out = new File(args[1]);
		out.mkdirs();
		JSONObject figma = new JSONObject(new String(Files.readAllBytes(new File(args[2]).toPath()), StandardCharsets.UTF_8));

		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 18512), 0);
		server.createContext("/", exchange -> {
			calls++;
			byte[] request = exchange.getRequestBody().readAllBytes();
			// The request must carry a system instruction and one user part; log the size so image handling is visible.
			JSONObject body = new JSONObject(new String(request, StandardCharsets.UTF_8));
			System.out.println("   [fake ai] " + exchange.getRequestURI().getPath() + " systemInstruction="
				+ (body.optJSONObject("systemInstruction") != null) + " bytes=" + request.length);
			JSONObject response = new JSONObject().put("candidates", new JSONArray().put(new JSONObject()
				.put("content", new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", cannedAnswer))))));
			byte[] bytes = response.toString().getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, bytes.length);
			exchange.getResponseBody().write(bytes);
			exchange.close();
		});
		server.start();
		System.setProperty("figma.ai.gemini.url", "http://127.0.0.1:18512");
		System.setProperty("figma.ai.gemini.apiKey", "fake-key");
		System.setProperty("exconverter.generated.root", new File(out, "generated").getAbsolutePath());

		FigmaDocument document = new FigmaDocument(figma);
		FigmaUiIrExtractor extractor = new FigmaUiIrExtractor(document);
		JSONObject screen = document.screens(Collections.<String>emptyList()).get(0);
		JSONObject base = extractor.extract(screen).uiIr;
		int failures = 0;

		// 1) provider=none must not call anything and must leave the UI-IR untouched.
		UiIrCritic off = new UiIrCritic(AiClients.create("none"));
		UiIrCritic.Review offReview = off.review(new JSONObject(base.toString()), null);
		failures += check("1 AI 끔: 호출 없음/변경 없음", !offReview.called && offReview.applied.isEmpty() && calls == 0);

		AiClient gemini = AiClients.create("gemini");
		UiIrCritic critic = new UiIrCritic(gemini);
		int firstGridRegion = indexOf(base, "grid");
		int firstFieldRegion = indexOf(base, "search") >= 0 ? indexOf(base, "search") : indexOf(base, "form");

		// 2) a valid patch is applied.
		cannedAnswer = new JSONObject().put("patches", new JSONArray()
			.put(new JSONObject().put("op", "column.editor").put("region", firstGridRegion).put("column", 2).put("value", "combobox"))
			.put(new JSONObject().put("op", "column.name").put("region", firstGridRegion).put("column", 2).put("value", "budget_yr"))
			.put(new JSONObject().put("op", "field.component").put("region", firstFieldRegion).put("field", 0).put("value", "COMBOBOX"))).toString();
		UiIrCritic.Review applied = critic.review(new JSONObject(base.toString()), null);
		JSONObject column = applied.uiIr.getJSONArray("regions").getJSONObject(firstGridRegion).getJSONArray("columns").getJSONObject(2);
		failures += check("2 허용 patch 적용 (" + applied.applied + ")",
			applied.applied.size() == 3 && "combobox".equals(column.getString("editor")) && "BUDGET_YR".equals(column.getString("cellText")));

		// 3) forbidden ops are rejected, the UI-IR stays as it was.
		cannedAnswer = new JSONObject().put("patches", new JSONArray()
			.put(new JSONObject().put("op", "region.type").put("region", firstGridRegion).put("value", "form"))
			.put(new JSONObject().put("op", "region.add").put("value", "grid"))
			.put(new JSONObject().put("op", "column.editor").put("region", firstGridRegion).put("column", 999).put("value", "output"))
			.put(new JSONObject().put("op", "field.component").put("region", firstFieldRegion).put("field", 0).put("value", "richtexteditor"))).toString();
		UiIrCritic.Review rejected = critic.review(new JSONObject(base.toString()), null);
		failures += check("3 금지 op 거부 (" + rejected.rejected + ")", rejected.applied.isEmpty() && rejected.rejected.size() == 4);

		// 4) a broken answer falls back to the deterministic UI-IR.
		cannedAnswer = "이건 JSON이 아닙니다";
		UiIrCritic.Review broken = critic.review(new JSONObject(base.toString()), null);
		failures += check("4 깨진 응답 → 폴백", broken.applied.isEmpty() && broken.uiIr.similar(base));

		// 5) column naming: dictionary/cache first, AI for the rest, cached afterwards.
		cannedAnswer = namingAnswer(base);
		JSONObject named = new JSONObject(base.toString());
		List<String> names = new LabelCodeNamer(gemini).nameColumns(named);
		int callsAfterNaming = calls;
		new LabelCodeNamer(gemini).nameColumns(new JSONObject(base.toString()));
		failures += check("5 컬럼 코드 " + names.size() + "개, 2회차는 캐시로 API 0회", !names.isEmpty() && calls == callsAfterNaming);

        // 6) the patched UI-IR still generates a valid CLX.
		cannedAnswer = new JSONObject().put("patches", new JSONArray()
			.put(new JSONObject().put("op", "column.editor").put("region", firstGridRegion).put("column", 2).put("value", "combobox"))).toString();
		UiIrCritic.Review finalReview = critic.review(named, null);
		UiIr ir = UiIrParser.parse(finalReview.uiIr.toString());
		TemplateCatalog.TemplateMatch match = new TemplateCatalog().selectFor(ir, templates);
		byte[] clx = new ClxGenerator().generate(match.getFile(), ir);
		List<String> errors = ClxValidator.validate(clx);
		Files.write(new File(out, "ai.clx").toPath(), clx);
		Files.write(new File(out, "ai.js").toPath(), "/* ai.js */\n".getBytes(StandardCharsets.UTF_8));
		Files.write(new File(out, "ai.ui-ir.json").toPath(), finalReview.uiIr.toString(2).getBytes(StandardCharsets.UTF_8));
		failures += check("6 보정 후 CLX 생성/검증 (" + match.getId() + ") " + errors, errors.isEmpty());

		// 7) a patch aimed at the CLX itself is impossible: no op touches XML.
		cannedAnswer = new JSONObject().put("patches", new JSONArray()
			.put(new JSONObject().put("op", "clx.replace").put("value", "<cl:button/>"))).toString();
		UiIrPatch.Result hostile = UiIrPatch.apply(base, cannedAnswer);
		failures += check("7 CLX 직접 수정 시도 거부", hostile.applied.isEmpty());

		server.stop(0);
		System.out.println((failures == 0 ? "ALL OK" : failures + " FAILED") + " (fake AI 호출 " + calls + "회)");
		if (failures > 0) System.exit(1);
	}

	private static String namingAnswer(JSONObject uiIr) {
		JSONObject codes = new JSONObject();
		int grid = indexOf(uiIr, "grid");
		if (grid >= 0) {
			JSONArray columns = uiIr.getJSONArray("regions").getJSONObject(grid).getJSONArray("columns");
			for (int i = 0; i < columns.length(); i++) {
				String header = columns.getJSONObject(i).optString("header", "");
				if (!header.isEmpty()) codes.put(header, "COL_" + (i + 1) + "X");
			}
		}
		return new JSONObject().put("codes", codes).toString();
	}

	private static int indexOf(JSONObject uiIr, String type) {
		JSONArray regions = uiIr.getJSONArray("regions");
		for (int i = 0; i < regions.length(); i++) { if (type.equals(regions.getJSONObject(i).optString("type"))) return i; }
		return -1;
	}

	private static int check(String name, boolean ok) {
		System.out.println((ok ? "OK   " : "FAIL ") + name);
		return ok ? 0 : 1;
	}
}
