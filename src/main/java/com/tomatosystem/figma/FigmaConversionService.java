package com.tomatosystem.figma;

import com.tomatosystem.exconverter.model.UiIr;
import com.tomatosystem.exconverter.service.GenerationService;
import com.tomatosystem.exconverter.service.ProgressLog;
import com.tomatosystem.exconverter.service.ProjectRootResolver;
import com.tomatosystem.exconverter.service.UiIrParser;
import com.tomatosystem.figma.ai.AiClients;
import com.tomatosystem.figma.api.ApiBinder;
import com.tomatosystem.figma.qa.VisualQaService;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Figma file JSON → one CLX/JS pair per screen, built on the most similar template of templates/.
 * Pipeline: FigmaUiIrExtractor (Figma → UI-IR) → [ApiBinder (OpenAPI → DataSet/DataMap/submission names, v2.2)]
 * → UiIrParser (normalization) → TemplateCatalog (score-based template choice) → ClxGenerator (template skeleton +
 * Figma content) → ClxValidator → clx-src/convertTest/{date}/ → [VisualQaService (Figma PNG vs rendered CLX, v2.2)].
 */
@Service
public class FigmaConversionService {
	@Autowired private GenerationService generationService;

	/** Converts every screen of the file, or only the given node ids (v2.0 behaviour, no binding, no QA). */
	public List<Result> convert(JSONObject figmaJson, List<String> nodeIds) { return convert(figmaJson, nodeIds, ConversionOptions.NONE); }

	public List<Result> convert(JSONObject figmaJson, List<String> nodeIds, ConversionOptions options) {
		if (options == null) options = ConversionOptions.NONE;
		FigmaDocument document = new FigmaDocument(figmaJson);
		FigmaUiIrExtractor extractor = new FigmaUiIrExtractor(document);
		ApiBinder binder = options.spec == null ? null : new ApiBinder(options.spec, AiClients.create());
		List<Result> results = new ArrayList<Result>();
		Map<String, Integer> usedNames = new HashMap<String, Integer>();
		List<JSONObject> screens = document.screens(nodeIds);
		ProgressLog.step("===== Figma → CLX 변환 시작: {} (화면 {}개{}{}) =====", document.getName(), screens.size(),
			binder == null ? "" : ", OpenAPI " + options.spec.describe(), options.visualQa ? ", 시각 QA" : "");
		for (JSONObject screen : screens) {
			String label = screen.optString("name", "screen");
			try {
				FigmaUiIrExtractor.Screen extracted = extractor.extract(screen);
				String baseName = uniqueName(extracted.name, usedNames);
				String api = "";
				if (binder != null) api = binder.bind(extracted.uiIr).summary();
				String uiIrJson = extracted.uiIr.toString(2);
				UiIr ir = UiIrParser.parse(uiIrJson);
				ir.getWarnings().addAll(extracted.warnings);
				GenerationService.GenerationResult generated = generationService.generate(ir, baseName, uiIrJson);
				String qa = options.visualQa ? visualQa(extracted, generated, options, baseName) : "";
				results.add(new Result(label, baseName, generated.getTemplateId(), generated.getFile(), generated.getJsFile(), summary(ir), generated.getWarnings(), api, qa, null));
			} catch (RuntimeException e) {
				ProgressLog.step("변환 실패 [{}]: {}", label, e.getMessage());
				results.add(new Result(label, label, "", null, null, "", new ArrayList<String>(), "", "", e.getMessage()));
			}
		}
		return results;
	}

	/** Figma → UI-IR only (debugging / tests); no file is written. */
	public List<JSONObject> extractUiIr(JSONObject figmaJson, List<String> nodeIds) {
		FigmaDocument document = new FigmaDocument(figmaJson);
		FigmaUiIrExtractor extractor = new FigmaUiIrExtractor(document);
		List<JSONObject> result = new ArrayList<JSONObject>();
		for (JSONObject screen : document.screens(nodeIds)) result.add(extractor.extract(screen).uiIr);
		return result;
	}

	/** Renders the generated screen at the Figma frame size and diffs it against Figma's PNG; never throws. */
	public static String visualQa(FigmaUiIrExtractor.Screen extracted, GenerationService.GenerationResult generated, ConversionOptions options, String baseName) {
		try {
			JSONObject screen = extracted.uiIr.getJSONObject("screen");
			JSONObject source = extracted.uiIr.optJSONObject("source");
			String nodeId = source == null ? "" : source.optString("figmaNodeId", "");
			File root = ProjectRootResolver.resolve(null);
			return VisualQaService.run(root, generated.getFile(), generated.getJsFile(), options.fileKey, nodeId, screen.optInt("width", 1440), screen.optInt("height", 860),
				options.token, options.auth, baseName).summary();
		} catch (RuntimeException e) {
			return "시각 QA 실패: " + e.getMessage();
		}
	}

	/** Keeps the raw Figma response next to the results (clx-src/json/{date}/), as before. */
	public static File saveRawJson(JSONObject figmaJson, String baseName) {
		try {
			File dir = FigmaPaths.clxSrc("json", FigmaPaths.today());
			String name = (baseName == null || baseName.isEmpty() ? "figma" : baseName.replaceAll("[\\p{Cntrl}<>:\"/\\\\|?*]", "_")) + "_" + Long.toString(System.currentTimeMillis(), 36) + ".json";
			File file = new File(dir, name);
			Files.write(file.toPath(), figmaJson.toString(2).getBytes(StandardCharsets.UTF_8));
			return file;
		} catch (Exception e) {
			ProgressLog.step("Figma JSON 저장 실패: {}", e.getMessage());
			return null;
		}
	}

	private static String uniqueName(String name, Map<String, Integer> used) {
		String base = name == null || name.trim().isEmpty() ? "figma" : name.trim();
		int n = used.merge(base, 1, Integer::sum);
		return n == 1 ? base : base + "_" + n;
	}

	private static String summary(UiIr ir) {
		StringBuilder s = new StringBuilder();
		for (UiIr.Region region : ir.getRegions()) {
			if (s.length() > 0) s.append(" → ");
			s.append(region.getType());
			if (!region.getSide().isEmpty()) s.append('[').append(region.getSide()).append(']');
			if (!region.getFields().isEmpty()) s.append("(필드 ").append(region.getFields().size()).append(')');
			if (!region.getColumns().isEmpty()) s.append("(컬럼 ").append(region.getColumns().size()).append(')');
		}
		return s.toString();
	}

	/** Outcome of one screen; error != null when it failed. */
	public static final class Result {
		public final String screen;
		public final String baseName;
		public final String templateId;
		public final File clx;
		public final File js;
		public final String regions;
		public final List<String> warnings;
		/** ApiBinder summary ("" without a spec). */
		public final String api;
		/** VisualQaService summary ("" when QA was not requested). */
		public final String qa;
		public final String error;
		Result(String screen, String baseName, String templateId, File clx, File js, String regions, List<String> warnings, String api, String qa, String error) {
			this.screen = screen; this.baseName = baseName; this.templateId = templateId; this.clx = clx; this.js = js;
			this.regions = regions; this.warnings = warnings; this.api = api == null ? "" : api; this.qa = qa == null ? "" : qa; this.error = error;
		}

		public String describe() {
			if (error != null) return "❌ " + screen + ": " + error;
			StringBuilder s = new StringBuilder("✅ " + screen + " → 템플릿 " + templateId + "\n    영역: " + regions);
			if (!api.isEmpty()) s.append("\n    ").append(api);
			if (!qa.isEmpty()) s.append("\n    ").append(qa);
			s.append("\n    저장: ").append(clx.getAbsolutePath());
			if (!warnings.isEmpty()) s.append("\n    경고: ").append(warnings);
			return s.toString();
		}
	}

	public static String describe(List<Result> results) {
		StringBuilder s = new StringBuilder();
		for (Result r : results) s.append(r.describe()).append('\n');
		return s.toString();
	}
}
