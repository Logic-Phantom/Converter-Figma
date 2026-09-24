package com.tomatosystem.web;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cleopatra.protocol.data.DataRequest;
import com.tomatosystem.figma.ConversionOptions;
import com.tomatosystem.figma.FigmaApiClient;
import com.tomatosystem.figma.FigmaConversionService;
import com.tomatosystem.figma.FigmaSettings;
import com.tomatosystem.service.FigmaToClxService;

/**
 * Figma → eXBuilder6 CLX. Every endpoint converts through the template pipeline
 * (Figma JSON → UI-IR → most similar template under templates/ → CLX), see {@link FigmaConversionService}.
 * v2.2 options on every endpoint: {@code swagger} (OpenAPI/Swagger JSON URL → DataSet/DataMap/submission binding
 * + JS skeleton) and {@code qa=true} (render the result and diff it against Figma's PNG).
 * The coordinate-only converter of v1.x is kept at /design/jsonConvertLegacy.do for comparison.
 */
@Controller
@RequestMapping("/design")
public class DesignController {
	@Autowired
	private FigmaConversionService figmaConversionService;

	@Autowired
	private FigmaToClxService figmaToClxService;

	/**
	 * Personal access token conversion (directFileID.clx). Parameters (all optional):
	 * token, url (Figma URL, may carry node-id) or fileKey, nodeId, swagger, qa. Missing values come from
	 * FIGMA_DIRECT_TOKEN / FIGMA_DIRECT_FILEKEY (or figma.direct.* in application.properties).
	 */
	@GetMapping("/convertDirect.do")
	public ResponseEntity<String> convertDirect(HttpServletRequest request) {
		try {
			ConversionRequests.Input input = ConversionRequests.fromRequest(request);
			return convertRef(input.ref, input.token, input.auth, input.options());
		} catch (RuntimeException e) {
			return ConversionRequests.error(e);
		}
	}

	/** Same as convertDirect.do for an eXBuilder screen (convertApi.clx): dmParam.url, swagger, token, nodeId, qa. */
	@RequestMapping("/convertApi.do")
	public ResponseEntity<String> convertApi(DataRequest dataRequest) {
		try {
			ConversionRequests.Input input = ConversionRequests.fromDataRequest(dataRequest, true);
			if (input.ref == null) return ConversionRequests.text(HttpStatus.BAD_REQUEST, "Figma URL 또는 fileKey 가 필요합니다.");
			return convertRef(input.ref, input.token, input.auth, input.options());
		} catch (RuntimeException e) {
			return ConversionRequests.error(e);
		}
	}

	/**
	 * OAuth token conversion (converterStart.clx, dmParam.token). With dmParam.url / fileKey (and nodeId) that file;
	 * otherwise the first file of the first project of the configured team (figma.team.id).
	 */
	@RequestMapping("/convert.do")
	public ResponseEntity<String> convert(DataRequest dataRequest) {
		try {
			ConversionRequests.Input input = ConversionRequests.fromDataRequest(dataRequest, false);
			FigmaApiClient.FileRef ref = input.ref;
			if (ref == null) {
				List<JSONObject> projects = projects(input.token);
				if (projects.isEmpty()) return ConversionRequests.text(HttpStatus.NOT_FOUND, "No project found in team.");
				List<JSONObject> files = files(projects.get(0).optString("id"), input.token);
				if (files.isEmpty()) return ConversionRequests.text(HttpStatus.NOT_FOUND, "No files found in project.");
				ref = FigmaApiClient.parse(files.get(0).optString("key"), ConversionRequests.param(dataRequest, "nodeId"));
			}
			return convertRef(ref, input.token, FigmaApiClient.Auth.OAUTH, ConversionOptions.of(input.swagger, input.qa, ref.fileKey, input.token, FigmaApiClient.Auth.OAUTH));
		} catch (RuntimeException e) {
			return ConversionRequests.error(e);
		}
	}

	/** Every file of every project of the team (OAuth token). */
	@RequestMapping("/convertAll.do")
	public ResponseEntity<String> convertAll(DataRequest dataRequest) {
		String token = ConversionRequests.param(dataRequest, "token");
		StringBuilder log = new StringBuilder();
		try {
			List<JSONObject> projects = projects(token);
			if (projects.isEmpty()) return ConversionRequests.text(HttpStatus.NOT_FOUND, "No projects found in team.");
			for (JSONObject project : projects) {
				log.append("Project ").append(project.optString("name")).append(" (").append(project.optString("id")).append(")\n");
				for (JSONObject file : files(project.optString("id"), token)) {
					log.append("  File ").append(file.optString("name")).append(" (").append(file.optString("key")).append(")\n");
					try {
						JSONObject json = FigmaApiClient.fetch(FigmaApiClient.parse(file.optString("key"), null), token, FigmaApiClient.Auth.OAUTH);
						FigmaConversionService.saveRawJson(json, file.optString("name"));
						for (FigmaConversionService.Result r : figmaConversionService.convert(json, Collections.<String>emptyList())) log.append("    ").append(r.describe().replace("\n", "\n    ")).append('\n');
					} catch (RuntimeException e) {
						log.append("    ❌ ").append(e.getMessage()).append('\n');
					}
				}
			}
			return ConversionRequests.text(HttpStatus.OK, log.toString());
		} catch (RuntimeException e) {
			return ConversionRequests.error(e);
		}
	}

	/**
	 * Uploaded Figma JSON (a saved GET /v1/files/:key or /nodes response) → template CLX (convertJson.clx).
	 * A second uploaded *.json that is an OpenAPI/Swagger document (or dmParam.swagger URL) binds the backend.
	 */
	@RequestMapping("/jsonConvert.do")
	public ResponseEntity<String> jsonConvert(DataRequest dataRequest) {
		try {
			File upload = ConversionRequests.figmaUpload(dataRequest);
			if (upload == null) return ConversionRequests.text(HttpStatus.BAD_REQUEST, "JSON 형식의 업로드 파일이 없습니다.");
			JSONObject json = new JSONObject(new String(Files.readAllBytes(upload.toPath()), StandardCharsets.UTF_8));
			FigmaConversionService.saveRawJson(json, json.optString("name", "upload"));
			return results(figmaConversionService.convert(json, Collections.<String>emptyList(), ConversionRequests.uploadOptions(dataRequest, upload)));
		} catch (Exception e) {
			return ConversionRequests.error(e);
		}
	}

	/** v1.x coordinate converter (xylayout, no template) kept for comparison. */
	@RequestMapping("/jsonConvertLegacy.do")
	public ResponseEntity<String> jsonConvertLegacy(DataRequest dataRequest) {
		try {
			File upload = ConversionRequests.figmaUpload(dataRequest);
			if (upload == null) return ConversionRequests.text(HttpStatus.BAD_REQUEST, "JSON 형식의 업로드 파일이 없습니다.");
			@SuppressWarnings("unchecked")
			Map<String, Object> json = new com.fasterxml.jackson.databind.ObjectMapper().readValue(upload, Map.class);
			File clx = figmaToClxService.convertToClx(json);
			return ConversionRequests.text(HttpStatus.OK, "CLX file saved successfully at: " + clx.getAbsolutePath());
		} catch (Exception e) {
			return ConversionRequests.error(e);
		}
	}

	// ------------------------------------------------------------------ helpers

	private ResponseEntity<String> convertRef(FigmaApiClient.FileRef ref, String token, FigmaApiClient.Auth auth, ConversionOptions options) {
		JSONObject json = FigmaApiClient.fetch(ref, token, auth);
		FigmaConversionService.saveRawJson(json, json.optString("name", ref.fileKey));
		return results(figmaConversionService.convert(json, ref.nodeIds, options));
	}

	private ResponseEntity<String> results(List<FigmaConversionService.Result> results) {
		boolean anyOk = false;
		for (FigmaConversionService.Result r : results) anyOk |= r.error == null;
		return ConversionRequests.text(anyOk ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR, FigmaConversionService.describe(results));
	}

	/**
	 * Figma has no "teams of this user" endpoint, so the team id is configured (figma.team.id).
	 * NOTE: GET /v1/teams/:id/projects and /v1/projects/:id/files are deprecated (2026-08-10) in favour of the
	 * v2 folders endpoints for teams with nested folders, and need the projects:read scope.
	 */
	private static List<JSONObject> projects(String token) {
		String teamId = FigmaSettings.get("figma.team.id", "");
		if (teamId.isEmpty()) throw new IllegalArgumentException("figma.team.id (FIGMA_TEAM_ID) 가 설정되지 않았습니다.");
		return list(FigmaApiClient.get("/v1/teams/" + teamId + "/projects", token, FigmaApiClient.Auth.OAUTH).optJSONArray("projects"));
	}

	private static List<JSONObject> files(String projectId, String token) {
		return list(FigmaApiClient.get("/v1/projects/" + projectId + "/files", token, FigmaApiClient.Auth.OAUTH).optJSONArray("files"));
	}

	private static List<JSONObject> list(JSONArray array) {
		List<JSONObject> result = new ArrayList<JSONObject>();
		if (array != null) for (int i = 0; i < array.length(); i++) { JSONObject o = array.optJSONObject(i); if (o != null) result.add(o); }
		return result;
	}
}
