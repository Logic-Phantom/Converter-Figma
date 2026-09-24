package com.tomatosystem.web;

import com.tomatosystem.figma.FigmaApiClient;
import com.tomatosystem.figma.FigmaConversionService;
import com.tomatosystem.figma.ai.FigmaAiConversionService;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cleopatra.protocol.data.DataRequest;

/**
 * AI-assisted conversion endpoints (v2.1). Separate from /design/* so the deterministic pipeline keeps working
 * exactly as before; with figma.ai.provider=none these produce the same CLX as /design/convertDirect.do.
 * The v2.2 options {@code swagger} and {@code qa} work here too (the AI also helps the label ↔ DTO matching).
 * See docs/ai-architecture.md.
 */
@Controller
@RequestMapping("/designAi")
public class FigmaAiController {
	@Autowired
	private FigmaAiConversionService aiConversionService;

	/** Parameters as /design/convertDirect.do: token, url|fileKey, nodeId, swagger, qa. */
	@GetMapping("/convert.do")
	public ResponseEntity<String> convert(HttpServletRequest request) {
		try {
			ConversionRequests.Input input = ConversionRequests.fromRequest(request);
			JSONObject json = FigmaApiClient.fetch(input.ref, input.token, input.auth);
			FigmaConversionService.saveRawJson(json, json.optString("name", input.ref.fileKey));
			List<FigmaAiConversionService.Result> results = aiConversionService.convert(json, input.ref.nodeIds, input.ref.fileKey, input.token, input.auth, input.options());
			boolean anyOk = false;
			for (FigmaAiConversionService.Result r : results) anyOk |= r.error == null;
			return ConversionRequests.text(anyOk ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR, FigmaAiConversionService.describe(results));
		} catch (RuntimeException e) {
			return ConversionRequests.error(e);
		}
	}

	/** Dry run: UI-IR before/after, applied patches and the template that would be chosen. Nothing is written. */
	@GetMapping("/preview.do")
	public ResponseEntity<String> preview(HttpServletRequest request) {
		try {
			ConversionRequests.Input input = ConversionRequests.fromRequest(request);
			JSONObject json = FigmaApiClient.fetch(input.ref, input.token, input.auth);
			return ResponseEntity.ok().contentType(ConversionRequests.JSON)
				.body(aiConversionService.preview(json, input.ref.nodeIds, input.ref.fileKey, input.token, input.auth).toString(2));
		} catch (RuntimeException e) {
			return ConversionRequests.error(e);
		}
	}

	/** Uploaded Figma JSON (no API call, so no screenshot); a second uploaded OpenAPI *.json binds the backend. */
	@RequestMapping("/jsonConvert.do")
	public ResponseEntity<String> jsonConvert(DataRequest dataRequest) {
		try {
			File upload = ConversionRequests.figmaUpload(dataRequest);
			if (upload == null) return ConversionRequests.text(HttpStatus.BAD_REQUEST, "JSON 형식의 업로드 파일이 없습니다.");
			JSONObject json = new JSONObject(new String(Files.readAllBytes(upload.toPath()), StandardCharsets.UTF_8));
			FigmaConversionService.saveRawJson(json, json.optString("name", "upload"));
			return ConversionRequests.text(HttpStatus.OK, FigmaAiConversionService.describe(aiConversionService.convert(json, java.util.Collections.<String>emptyList(), "", "",
				FigmaApiClient.Auth.PERSONAL_TOKEN, ConversionRequests.uploadOptions(dataRequest, upload))));
		} catch (Exception e) {
			return ConversionRequests.error(e);
		}
	}

	/** Which provider is configured, and how many label→column codes are cached. */
	@GetMapping("/status.do")
	public ResponseEntity<String> status() {
		return ResponseEntity.ok().contentType(ConversionRequests.JSON).body(aiConversionService.status().toString(2));
	}
}
