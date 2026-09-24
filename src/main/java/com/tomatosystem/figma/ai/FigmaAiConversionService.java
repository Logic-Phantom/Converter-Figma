package com.tomatosystem.figma.ai;

import com.tomatosystem.exconverter.model.UiIr;
import com.tomatosystem.exconverter.service.GenerationService;
import com.tomatosystem.exconverter.service.ProgressLog;
import com.tomatosystem.exconverter.service.TemplateCatalog;
import com.tomatosystem.exconverter.service.UiIrParser;
import com.tomatosystem.figma.ConversionOptions;
import com.tomatosystem.figma.FigmaApiClient;
import com.tomatosystem.figma.FigmaConversionService;
import com.tomatosystem.figma.FigmaDocument;
import com.tomatosystem.figma.FigmaSettings;
import com.tomatosystem.figma.FigmaUiIrExtractor;
import com.tomatosystem.figma.api.ApiBinder;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AI-assisted conversion (v2.1). The deterministic v2.0 path ({@code FigmaConversionService}) is untouched and stays
 * the default; this service adds three optional steps around it, each of which falls back to the deterministic
 * result when the AI is off, over quota or wrong:
 *
 * <ol>
 *   <li>column codes: dictionary → cache → AI ({@link LabelCodeNamer})</li>
 *   <li>structure review: whitelisted patches only ({@link UiIrCritic})</li>
 *   <li>repair: on a ClxValidator failure, ask for a minimal fix, then fall back to the pre-AI UI-IR</li>
 * </ol>
 *
 * See docs/ai-architecture.md.
 */
@Service
public class FigmaAiConversionService {
	@Autowired private GenerationService generationService;

	private final TemplateCatalog catalog = new TemplateCatalog();

	/** Converts and saves; ai steps are skipped silently when figma.ai.provider=none. */
	public List<Result> convert(JSONObject figmaJson, List<String> nodeIds, String fileKey, String token, FigmaApiClient.Auth auth) {
		return convert(figmaJson, nodeIds, fileKey, token, auth, ConversionOptions.NONE);
	}

	/** @param options OpenAPI binding and/or visual QA (v2.2); {@link ConversionOptions#NONE} for the plain AI path */
	public List<Result> convert(JSONObject figmaJson, List<String> nodeIds, String fileKey, String token, FigmaApiClient.Auth auth, ConversionOptions options) {
		if (options == null) options = ConversionOptions.NONE;
		List<Result> results = new ArrayList<Result>();
		AiClient client = AiClients.create();
		UiIrCritic critic = new UiIrCritic(client);
		LabelCodeNamer namer = new LabelCodeNamer(client);
		ApiBinder binder = options.spec == null ? null : new ApiBinder(options.spec, client);
		FigmaDocument document = new FigmaDocument(figmaJson);
		FigmaUiIrExtractor extractor = new FigmaUiIrExtractor(document);
		Map<String, Integer> usedNames = new HashMap<String, Integer>();
		List<JSONObject> screens = document.screens(nodeIds);
		ProgressLog.step("===== Figma → CLX (AI 보조: {}) 화면 {}개{}{} =====", client.describe(), screens.size(),
			binder == null ? "" : ", OpenAPI " + options.spec.describe(), options.visualQa ? ", 시각 QA" : "");
		for (JSONObject screen : screens) {
			String label = screen.optString("name", "screen");
			try {
				Prepared prepared = prepare(extractor, screen, critic, namer, fileKey, token, auth);
				if (binder != null) {
					// Bind both copies so the fallback to the pre-AI UI-IR keeps the backend binding.
					prepared.api = binder.bind(prepared.after).summary();
					binder.bind(prepared.before);
				}
				String baseName = uniqueName(prepared.screenName, usedNames);
				Result result = generate(prepared, baseName);
				if (result.error == null && options.visualQa) result.qa = FigmaConversionService.visualQa(prepared.extracted, result.generated, options.fileKey.isEmpty() ? options.withKey(fileKey, token, auth) : options, baseName);
				results.add(result);
			} catch (RuntimeException e) {
				ProgressLog.step("변환 실패 [{}]: {}", label, e.getMessage());
				results.add(Result.failed(label, e.getMessage()));
			}
		}
		return results;
	}

	/** UI-IR before/after, the patches and the template that would be chosen — without writing any file. */
	public JSONObject preview(JSONObject figmaJson, List<String> nodeIds, String fileKey, String token, FigmaApiClient.Auth auth) {
		AiClient client = AiClients.create();
		UiIrCritic critic = new UiIrCritic(client);
		LabelCodeNamer namer = new LabelCodeNamer(client);
		FigmaDocument document = new FigmaDocument(figmaJson);
		FigmaUiIrExtractor extractor = new FigmaUiIrExtractor(document);
		JSONArray screens = new JSONArray();
		for (JSONObject screen : document.screens(nodeIds)) {
			JSONObject entry = new JSONObject().put("screen", screen.optString("name"));
			try {
				Prepared prepared = prepare(extractor, screen, critic, namer, fileKey, token, auth);
				UiIr ir = UiIrParser.parse(prepared.after.toString());
				entry.put("uiIrBefore", prepared.before)
					.put("uiIrAfter", prepared.after)
					.put("applied", prepared.applied)
					.put("rejected", prepared.rejected)
					.put("namedColumns", prepared.named)
					.put("regions", UiIrCritic.summary(ir))
					.put("template", catalog.selectFor(ir).getId());
			} catch (RuntimeException e) {
				entry.put("error", String.valueOf(e.getMessage()));
			}
			screens.put(entry);
		}
		return new JSONObject().put("provider", client.describe()).put("screens", screens);
	}

	public JSONObject status() {
		AiClient client = AiClients.create();
		return new JSONObject()
			.put("provider", client.describe())
			.put("enabled", client.isEnabled())
			.put("screenshot", FigmaScreenshots.isEnabled())
			.put("naming", namingEnabled())
			.put("repair", repairEnabled())
			.put("labelCacheSize", new LabelCodeNamer(AiClients.DISABLED).cacheSize());
	}

	// ------------------------------------------------------------------ steps

	private Prepared prepare(FigmaUiIrExtractor extractor, JSONObject screen, UiIrCritic critic, LabelCodeNamer namer,
			String fileKey, String token, FigmaApiClient.Auth auth) {
		FigmaUiIrExtractor.Screen extracted = extractor.extract(screen);
		Prepared prepared = new Prepared();
		prepared.extracted = extracted;
		prepared.screenName = extracted.name;
		prepared.before = new JSONObject(extracted.uiIr.toString());
		JSONObject working = extracted.uiIr;
		if (namingEnabled()) prepared.named = namer.nameColumns(working);
		byte[] png = FigmaScreenshots.render(fileKey, working.optJSONObject("source") == null ? "" : working.getJSONObject("source").optString("figmaNodeId"),
			working.getJSONObject("screen").optDouble("sourceWidth", 0), token, auth);
		prepared.screenshot = png != null;
		UiIrCritic.Review review = critic.review(working, png);
		prepared.applied = review.applied;
		prepared.rejected = review.rejected;
		prepared.after = review.uiIr;
		prepared.critic = critic;
		prepared.warnings = extracted.warnings;
		return prepared;
	}

	/** Generates with up to two AI repairs, then with the pre-AI UI-IR, so a bad patch never loses the screen. */
	private Result generate(Prepared prepared, String baseName) {
		JSONObject uiIr = prepared.after;
		RuntimeException last = null;
		for (int attempt = 0; attempt < 3; attempt++) {
			UiIr ir = UiIrParser.parse(uiIr.toString());
			ir.getWarnings().addAll(prepared.warnings);
			try {
				GenerationService.GenerationResult generated = generationService.generate(ir, baseName, audit(uiIr, prepared).toString(2));
				Result result = new Result(prepared.screenName, generated.getTemplateId(), generated.getFile(), generated.getJsFile(),
					UiIrCritic.summary(ir), prepared.applied, prepared.rejected, prepared.named, generated.getWarnings(), null);
				result.generated = generated;
				result.api = prepared.api;
				return result;
			} catch (IllegalStateException e) {
				last = e;
				String message = String.valueOf(e.getMessage());
				if (attempt < 2 && repairEnabled() && message.contains("invalid")) {
					ProgressLog.step("검증 실패 → AI 수리 시도 {}/2: {}", attempt + 1, message);
					UiIrCritic.Review repair = prepared.critic.repair(uiIr, message);
					prepared.applied.addAll(repair.applied);
					prepared.rejected.addAll(repair.rejected);
					if (!repair.applied.isEmpty()) { uiIr = repair.uiIr; continue; }
				}
				// Last resort: the untouched deterministic UI-IR.
				if (!uiIr.similar(prepared.before)) {
					ProgressLog.step("AI 보정을 버리고 규칙 결과로 재생성");
					prepared.rejected.add("생성 실패로 AI 보정 폐기: " + message);
					prepared.applied.clear();
					uiIr = prepared.before;
					continue;
				}
				break;
			}
		}
		return Result.failed(prepared.screenName, last == null ? "생성 실패" : last.getMessage());
	}

	private static JSONObject audit(JSONObject uiIr, Prepared prepared) {
		JSONObject copy = new JSONObject(uiIr.toString());
		copy.put("ai", new JSONObject()
			.put("provider", AiClients.create().describe())
			.put("applied", prepared.applied)
			.put("rejected", prepared.rejected)
			.put("namedColumns", prepared.named)
			.put("screenshot", prepared.screenshot));
		return copy;
	}

	private static boolean namingEnabled() { return !"false".equalsIgnoreCase(FigmaSettings.get("figma.ai.naming", "true")); }
	private static boolean repairEnabled() { return !"false".equalsIgnoreCase(FigmaSettings.get("figma.ai.repair", "true")); }

	private static String uniqueName(String name, Map<String, Integer> used) {
		String base = name == null || name.trim().isEmpty() ? "figma" : name.trim();
		int n = used.merge(base, 1, Integer::sum);
		return n == 1 ? base : base + "_" + n;
	}

	private static final class Prepared {
		FigmaUiIrExtractor.Screen extracted;
		String screenName;
		String api = "";
		JSONObject before;
		JSONObject after;
		UiIrCritic critic;
		List<String> applied = new ArrayList<String>();
		List<String> rejected = new ArrayList<String>();
		List<String> named = new ArrayList<String>();
		List<String> warnings = new ArrayList<String>();
		boolean screenshot;
	}

	/** One screen's outcome. */
	public static final class Result {
		public final String screen;
		public final String templateId;
		public final File clx;
		public final File js;
		public final String regions;
		public final List<String> applied;
		public final List<String> rejected;
		public final List<String> named;
		public final List<String> warnings;
		public final String error;
		/** ApiBinder summary ("" without a spec) and VisualQaService summary ("" without QA), v2.2. */
		public String api = "";
		public String qa = "";
		GenerationService.GenerationResult generated;

		Result(String screen, String templateId, File clx, File js, String regions, List<String> applied,
				List<String> rejected, List<String> named, List<String> warnings, String error) {
			this.screen = screen; this.templateId = templateId; this.clx = clx; this.js = js; this.regions = regions;
			this.applied = applied; this.rejected = rejected; this.named = named; this.warnings = warnings; this.error = error;
		}

		static Result failed(String screen, String error) {
			return new Result(screen, "", null, null, "", new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), error);
		}

		public String describe() {
			if (error != null) return "❌ " + screen + ": " + error;
			StringBuilder s = new StringBuilder("✅ " + screen + " → 템플릿 " + templateId + "\n    영역: " + regions);
			if (!named.isEmpty()) s.append("\n    컬럼코드: ").append(named);
			if (!applied.isEmpty()) s.append("\n    AI 보정: ").append(applied);
			if (!rejected.isEmpty()) s.append("\n    AI 무시/실패: ").append(rejected);
			if (!api.isEmpty()) s.append("\n    ").append(api);
			if (!qa.isEmpty()) s.append("\n    ").append(qa);
			if (!warnings.isEmpty()) s.append("\n    경고: ").append(warnings);
			return s.append("\n    저장: ").append(clx.getAbsolutePath()).toString();
		}
	}

	public static String describe(List<Result> results) {
		StringBuilder s = new StringBuilder();
		for (Result r : results) s.append(r.describe()).append('\n');
		return s.toString();
	}
}
