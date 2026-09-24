package com.tomatosystem.web;

import com.tomatosystem.exconverter.service.ProjectRootResolver;
import com.tomatosystem.figma.FigmaApiClient;
import com.tomatosystem.figma.theme.FigmaThemeSync;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;
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
 * Figma design tokens → clx-src/theme/figma/*.less (v2.2 feature 2).
 *
 * <ul>
 *   <li>{@code GET /figma/theme/sync.do?url|fileKey&token&apply=true|false&variables=true|false} — Variables
 *       (Enterprise) when allowed, always the file's styles; writes the LESS files and, with apply (default true),
 *       imports them from cleopatra-theme.less.</li>
 *   <li>{@code /figma/theme/jsonSync.do} — the same from an uploaded file JSON (and optionally a variables JSON).</li>
 * </ul>
 */
@Controller
@RequestMapping("/figma/theme")
public class FigmaThemeController {
	@Autowired(required = false) private ServletContext servletContext;

	@GetMapping("/sync.do")
	public ResponseEntity<String> sync(HttpServletRequest request) {
		try {
			ConversionRequests.Input input = ConversionRequests.fromRequest(request);
			boolean apply = !"false".equalsIgnoreCase(request.getParameter("apply"));
			boolean tryVariables = !"false".equalsIgnoreCase(request.getParameter("variables"));
			List<String> notes = new ArrayList<String>();
			JSONObject file = FigmaApiClient.get("/v1/files/" + input.ref.fileKey, input.token, input.auth);
			List<FigmaThemeSync.Token> variables = new ArrayList<FigmaThemeSync.Token>();
			if (tryVariables) {
				try {
					variables = FigmaThemeSync.fromVariables(FigmaApiClient.get("/v1/files/" + input.ref.fileKey + "/variables/local", input.token, input.auth));
				} catch (RuntimeException e) {
					notes.add("Variables API 사용 불가(Enterprise 플랜 + file_variables:read 필요) → 스타일만 사용: " + e.getMessage());
				}
			}
			FigmaThemeSync.Result result = FigmaThemeSync.sync(root(), variables, FigmaThemeSync.fromStyles(file), apply, "figma:" + input.ref.fileKey + " " + file.optString("name", ""));
			result.notes.addAll(0, notes);
			return ConversionRequests.text(HttpStatus.OK, result.summary());
		} catch (RuntimeException e) {
			return ConversionRequests.error(e);
		}
	}

	/** Uploaded file JSON (+ optional variables JSON whose top level has "meta.variables"). dmParam.apply=false skips the import. */
	@RequestMapping("/jsonSync.do")
	public ResponseEntity<String> jsonSync(DataRequest dataRequest) {
		try {
			JSONObject file = null;
			JSONObject variablesJson = null;
			for (File upload : ConversionRequests.uploads(dataRequest, ".json")) {
				JSONObject json = new JSONObject(new String(Files.readAllBytes(upload.toPath()), StandardCharsets.UTF_8));
				if (json.optJSONObject("meta") != null && json.getJSONObject("meta").optJSONObject("variables") != null) variablesJson = json;
				else if (file == null) file = json;
			}
			if (file == null && variablesJson == null) return ConversionRequests.text(HttpStatus.BAD_REQUEST, "Figma 파일 JSON 또는 variables JSON 업로드가 필요합니다.");
			boolean apply = !"false".equalsIgnoreCase(ConversionRequests.param(dataRequest, "apply"));
			FigmaThemeSync.Result result = FigmaThemeSync.sync(root(), variablesJson == null ? null : FigmaThemeSync.fromVariables(variablesJson),
				file == null ? null : FigmaThemeSync.fromStyles(file), apply, "upload " + (file == null ? "" : file.optString("name", "")));
			return ConversionRequests.text(HttpStatus.OK, result.summary());
		} catch (Exception e) {
			return ConversionRequests.error(e);
		}
	}

	private File root() { return ProjectRootResolver.resolve(servletContext); }
}
