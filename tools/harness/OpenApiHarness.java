import com.tomatosystem.exconverter.model.UiIr;
import com.tomatosystem.exconverter.service.ClxGenerator;
import com.tomatosystem.exconverter.service.ClxValidator;
import com.tomatosystem.exconverter.service.CompanionJsGenerator;
import com.tomatosystem.exconverter.service.TemplateCatalog;
import com.tomatosystem.exconverter.service.UiIrParser;
import com.tomatosystem.figma.FigmaDocument;
import com.tomatosystem.figma.FigmaUiIrExtractor;
import com.tomatosystem.figma.ai.AiClients;
import com.tomatosystem.figma.api.ApiBinder;
import com.tomatosystem.figma.api.ApiLabelMatcher;
import com.tomatosystem.figma.api.OpenApiSpec;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import org.json.JSONObject;

/**
 * Not part of the Eclipse build. Figma JSON + OpenAPI/Swagger JSON → UI-IR with backend binding → CLX/JS, offline.
 *
 *   OpenApiHarness <templates dir> <out dir> <figma.json> <openapi.json> [swagger2.json]
 *
 * Checks (exit code 1 on any failure): spec parsing (3.x and 2.0), label ↔ property matching, dataset columns named
 * after the DTO, DataMap + datamapbind for the search bar, submissions with request/response data and listeners,
 * the companion JS defining every handler, ClxValidator, and that the same UI-IR without a spec still yields the
 * v2.0 CLX (no datamap/submission). Compile out/api.clx with e6-compiler afterwards (see README §7).
 */
public class OpenApiHarness {
	public static void main(String[] args) throws Exception {
		File templates = new File(args[0]);
		File out = new File(args[1]);
		out.mkdirs();
		JSONObject figma = new JSONObject(new String(Files.readAllBytes(new File(args[2]).toPath()), StandardCharsets.UTF_8));
		OpenApiSpec spec = OpenApiSpec.load(args[3]);
		int failures = 0;

		// 1) spec parsing
		OpenApiSpec.Operation list = find(spec, "get", "/health-cards");
		failures += check("1 OpenAPI 3.x 파싱: " + spec.describe() + " basePath=" + spec.basePath,
			spec.operations.size() == 5 && "/api/v1".equals(spec.basePath) && list != null && list.responseIsList && "data.list".equals(list.responsePath)
			&& list.responseFields.size() == 15 && list.parameters.size() == 8);
		OpenApiSpec.Operation create = find(spec, "post", "/health-cards");
		failures += check("2 allOf 병합 + requestBody: createHealthCard body " + (create == null ? "-" : create.bodyFields.size()) + "개",
			create != null && create.bodyFields.size() == 10 && !create.bodyIsArray);
		if (args.length > 4) {
			OpenApiSpec v2 = OpenApiSpec.load(args[4]);
			OpenApiSpec.Operation v2list = find(v2, "get", "/health-cards");
			OpenApiSpec.Operation v2save = find(v2, "post", "/health-cards");
			failures += check("3 Swagger 2.0 파싱: " + v2.describe(), v2list != null && v2list.responseIsList && v2list.responseFields.size() == 9
				&& v2list.parameters.size() == 5 && v2save != null && v2save.bodyIsArray && v2save.bodyFields.size() == 9);
		}

		// 4) label matching rules (no AI)
		int nameScore = ApiLabelMatcher.score("성명", find(list.responseFields, "name"));
		int rrnScore = ApiLabelMatcher.score("주민등록번호", find(list.responseFields, "rrn"));
		int yearScore = ApiLabelMatcher.score("예산연도", find(list.responseFields, "budgetYear"));
		int wrong = ApiLabelMatcher.score("성명", find(list.responseFields, "rrn"));
		failures += check("4 라벨 매칭 점수: 성명→name " + nameScore + ", 주민등록번호→rrn " + rrnScore + ", 예산연도→budgetYear " + yearScore + ", 성명→rrn " + wrong,
			nameScore >= 40 && rrnScore >= 90 && yearScore >= 90 && wrong < 40);

		// 5) binding on the extracted UI-IR
		FigmaDocument document = new FigmaDocument(figma);
		FigmaUiIrExtractor extractor = new FigmaUiIrExtractor(document);
		JSONObject screen = document.screens(Collections.<String>emptyList()).get(0);
		JSONObject plain = extractor.extract(screen).uiIr;
		JSONObject bound = new JSONObject(plain.toString());
		ApiBinder.Result binding = new ApiBinder(spec, AiClients.create("none")).bind(bound);
		for (String note : binding.notes) System.out.println("   · " + note);
		Files.write(new File(out, "api.ui-ir.json").toPath(), bound.toString(2).getBytes(StandardCharsets.UTF_8));
		JSONObject api = bound.optJSONObject("api");
		// The sample screen has a search bar, one grid with a 삭제 title-row button and no form: list + row delete, nothing else.
		failures += check("5 바인딩: " + binding.summary(), api != null && binding.boundColumns >= 10 && binding.boundFields >= 5
			&& has(api, "subList") && has(api, "subDelete") && !has(api, "subDetail") && !has(api, "subSave"));
		failures += check("5b 등록일→regDate (다른연도 등록 otherYearRegYn 보다 우선)", bound.toString().contains("\"name\":\"regDate\"") && !bound.toString().contains("등록일→otherYearRegYn"));

		// 6) CLX generation with the binding
		UiIr ir = UiIrParser.parse(bound.toString());
		TemplateCatalog.TemplateMatch match = new TemplateCatalog().selectFor(ir, templates);
		byte[] clx = new ClxGenerator().generate(match.getFile(), ir);
		String xml = new String(clx, StandardCharsets.UTF_8);
		List<String> errors = ClxValidator.validate(clx);
		Files.write(new File(out, "api.clx").toPath(), clx);
		byte[] js = CompanionJsGenerator.generate(match.getFile(), "api.js", ir);
		String script = new String(js, StandardCharsets.UTF_8);
		Files.write(new File(out, "api.js").toPath(), js);
		failures += check("6 CLX 검증 (" + match.getId() + ") " + errors, errors.isEmpty());
		failures += check("7 DataSet 컬럼이 DTO 이름: dsList/budgetYear/rrn/diseaseCode",
			xml.contains("id=\"dsList\"") && xml.contains("name=\"budgetYear\"") && xml.contains("name=\"rrn\"") && xml.contains("name=\"diseaseCode\""));
		failures += check("8 DataMap + datamapbind: dmSearch / datacontrolid / columnname=budgetYear",
			xml.contains("<cl:datamap") && xml.contains("id=\"dmSearch\"") && xml.contains("datacontrolid=\"dmSearch\"") && xml.contains("columnname=\"budgetYear\""));
		failures += check("9 submission: subList GET /api/v1/health-cards, requestdata dmSearch, responsedata dsList, submit-done",
			xml.contains("id=\"subList\"") && xml.contains("action=\"/api/v1/health-cards\"") && xml.contains("<cl:requestdata dataid=\"dmSearch\"")
			&& xml.contains("<cl:responsedata dataid=\"dsList\"") && xml.contains("name=\"submit-done\""));
		failures += check("10 listener: 조회 버튼 click → onBtnSearchClick, body load → onBodyLoad, 초기화 → onBtnResetClick",
			xml.contains("handler=\"onBtnSearchClick\"") && xml.contains("handler=\"onBodyLoad\"") && xml.contains("handler=\"onBtnResetClick\""));
		boolean allDefined = true;
		for (UiIr.Handler h : ir.getHandlers()) allDefined &= script.contains("function " + h.getFunction() + "(");
		failures += check("11 JS 스켈레톤: 핸들러 " + ir.getHandlers().size() + "개 모두 정의 + send()/build()/pickPath/replacePathParams",
			allDefined && ir.getHandlers().size() >= 6 && script.contains(".send()") && script.contains("dataSet.build(rows)") && script.contains("\"data.list\"")
			&& script.contains("replacePathParams(\"/api/v1/health-cards/{cardNo}\"") && script.contains("getSelectedRow()"));

		// 12) no spec → v2.0 result untouched
		UiIr plainIr = UiIrParser.parse(plain.toString());
		String plainXml = new String(new ClxGenerator().generate(match.getFile(), plainIr), StandardCharsets.UTF_8);
		String plainJs = new String(CompanionJsGenerator.generate(match.getFile(), "plain.js", plainIr), StandardCharsets.UTF_8);
		failures += check("12 스펙 없이 변환하면 datamap/submission/listener 없음 (v2.0 동일)",
			!plainXml.contains("<cl:datamap") && !plainXml.contains("<cl:submission") && !plainXml.contains("<cl:listener") && !plainJs.contains("function on"));

		System.out.println((failures == 0 ? "ALL OK" : failures + " FAILED") + " → " + out.getAbsolutePath());
		if (failures > 0) System.exit(1);
	}

	private static OpenApiSpec.Operation find(OpenApiSpec spec, String method, String path) {
		for (OpenApiSpec.Operation op : spec.operations) { if (op.method.equals(method) && op.path.equals(path)) return op; }
		return null;
	}

	private static OpenApiSpec.Property find(List<OpenApiSpec.Property> list, String name) {
		for (OpenApiSpec.Property p : list) { if (p.name.equals(name)) return p; }
		throw new IllegalStateException("property missing: " + name);
	}

	private static boolean has(JSONObject api, String submissionId) {
		org.json.JSONArray list = api.optJSONArray("submissions");
		for (int i = 0; list != null && i < list.length(); i++) { if (submissionId.equals(list.getJSONObject(i).optString("id"))) return true; }
		return false;
	}

	private static int check(String name, boolean ok) {
		System.out.println((ok ? "OK   " : "FAIL ") + name);
		return ok ? 0 : 1;
	}
}
