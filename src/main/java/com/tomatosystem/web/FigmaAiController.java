package com.tomatosystem.web;

import com.tomatosystem.figma.FigmaApiClient;
import com.tomatosystem.figma.FigmaConversionService;
import com.tomatosystem.figma.FigmaSettings;
import com.tomatosystem.figma.ai.FigmaAiConversionService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cleopatra.protocol.data.DataRequest;
import com.cleopatra.protocol.data.UploadFile;

/**
 * AI-assisted conversion endpoints (v2.1). Separate from /design/* so the deterministic pipeline keeps working
 * exactly as before; with figma.ai.provider=none these produce the same CLX as /design/convertDirect.do.
 * See docs/ai-architecture.md.
 */
@Controller
@RequestMapping("/designAi")
public class FigmaAiController {
	private static final MediaType TEXT = MediaType.valueOf("text/plain;charset=UTF-8");
	private static final MediaType JSON = MediaType.valueOf("application/json;charset=UTF-8");

	@Autowired
	private FigmaAiConversionService aiConversionService;

	/** Parameters as /design/convertDirect.do: token, url|fileKey, nodeId. */
	@GetMapping("/convert.do")
	public ResponseEntity<String> convert(HttpServletRequest request) {
		try {
			Input input = Input.of(request);
			JSONObject json = FigmaApiClient.fetch(input.ref, input.token, FigmaApiClient.Auth.PERSONAL_TOKEN);
			FigmaConversionService.saveRawJson(json, json.optString("name", input.ref.fileKey));
			List<FigmaAiConversionService.Result> results = aiConversionService.convert(json, input.ref.nodeIds, input.ref.fileKey, input.token, FigmaApiClient.Auth.PERSONAL_TOKEN);
			boolean anyOk = false;
			for (FigmaAiConversionService.Result r : results) anyOk |= r.error == null;
			return ResponseEntity.status(anyOk ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR).contentType(TEXT)
				.body(FigmaAiConversionService.describe(results));
		} catch (RuntimeException e) {
			return error(e);
		}
	}

	/** Dry run: UI-IR before/after, applied patches and the template that would be chosen. Nothing is written. */
	@GetMapping("/preview.do")
	public ResponseEntity<String> preview(HttpServletRequest request) {
		try {
			Input input = Input.of(request);
			JSONObject json = FigmaApiClient.fetch(input.ref, input.token, FigmaApiClient.Auth.PERSONAL_TOKEN);
			return ResponseEntity.ok().contentType(JSON)
				.body(aiConversionService.preview(json, input.ref.nodeIds, input.ref.fileKey, input.token, FigmaApiClient.Auth.PERSONAL_TOKEN).toString(2));
		} catch (RuntimeException e) {
			return error(e);
		}
	}

	/** Uploaded Figma JSON (no API call, so no screenshot). */
	@RequestMapping("/jsonConvert.do")
	public ResponseEntity<String> jsonConvert(DataRequest dataRequest) {
		try {
			java.io.File upload = null;
			if (dataRequest.getUploadFiles() != null) {
				for (UploadFile[] files : dataRequest.getUploadFiles().values()) {
					for (UploadFile file : files) {
						if (file.getFile() != null && file.getFile().getName().toLowerCase().contains(".json")) { upload = file.getFile(); break; }
					}
				}
			}
			if (upload == null) return ResponseEntity.badRequest().contentType(TEXT).body("JSON 형식의 업로드 파일이 없습니다.");
			JSONObject json = new JSONObject(new String(Files.readAllBytes(upload.toPath()), StandardCharsets.UTF_8));
			FigmaConversionService.saveRawJson(json, json.optString("name", "upload"));
			return ResponseEntity.ok().contentType(TEXT)
				.body(FigmaAiConversionService.describe(aiConversionService.convert(json, java.util.Collections.<String>emptyList(), "", "", FigmaApiClient.Auth.PERSONAL_TOKEN)));
		} catch (Exception e) {
			return error(e);
		}
	}

	/** Which provider is configured, and how many label→column codes are cached. */
	@GetMapping("/status.do")
	public ResponseEntity<String> status() {
		return ResponseEntity.ok().contentType(JSON).body(aiConversionService.status().toString(2));
	}

	private ResponseEntity<String> error(Exception e) {
		HttpStatus status = e instanceof IllegalArgumentException ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
		return ResponseEntity.status(status).contentType(TEXT).body("변환 실패: " + e.getMessage());
	}

	private static final class Input {
		final FigmaApiClient.FileRef ref;
		final String token;
		private Input(FigmaApiClient.FileRef ref, String token) { this.ref = ref; this.token = token; }

		static Input of(HttpServletRequest request) {
			String token = firstNonBlank(request.getParameter("token"), FigmaSettings.get("figma.direct.token", ""));
			String file = firstNonBlank(request.getParameter("url"), request.getParameter("fileKey"), FigmaSettings.get("figma.direct.fileKey", ""));
			return new Input(FigmaApiClient.parse(file, request.getParameter("nodeId")), token);
		}

		private static String firstNonBlank(String... values) {
			for (String value : values) { if (value != null && !value.trim().isEmpty()) return value.trim(); }
			return "";
		}
	}
}
