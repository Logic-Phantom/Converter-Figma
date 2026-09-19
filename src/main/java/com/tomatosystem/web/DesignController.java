package com.tomatosystem.web;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.View;

import com.cleopatra.protocol.data.DataRequest;
import com.cleopatra.protocol.data.ParameterGroup;
import com.cleopatra.protocol.data.UploadFile;
import com.cleopatra.spring.JSONDataView;
import com.tomatosystem.figma.FigmaApiClient;
import com.tomatosystem.figma.FigmaConversionService;
import com.tomatosystem.figma.FigmaSettings;
import com.tomatosystem.service.FigmaToClxService;

/**
 * Figma → eXBuilder6 CLX. Every endpoint converts through the template pipeline
 * (Figma JSON → UI-IR → most similar template under templates/ → CLX), see {@link FigmaConversionService}.
 * The coordinate-only converter of v1.x is kept at /design/jsonConvertLegacy.do for comparison.
 */
@Controller
@RequestMapping("/design")
public class DesignController {
	private static final MediaType TEXT = MediaType.valueOf("text/plain;charset=UTF-8");

	@Autowired
	private FigmaConversionService figmaConversionService;

	@Autowired
	private FigmaToClxService figmaToClxService;

	/**
	 * Personal access token conversion (directFileID.clx). Parameters (all optional):
	 * token, url (Figma URL, may carry node-id) or fileKey, nodeId. Missing values come from
	 * FIGMA_DIRECT_TOKEN / FIGMA_DIRECT_FILEKEY (or figma.direct.* in application.properties).
	 */
	@GetMapping("/convertDirect.do")
	public ResponseEntity<String> convertDirect(HttpServletRequest request) {
		String token = firstNonBlank(request.getParameter("token"), FigmaSettings.get("figma.direct.token", ""));
		String file = firstNonBlank(request.getParameter("url"), request.getParameter("fileKey"), FigmaSettings.get("figma.direct.fileKey", ""));
		try {
			FigmaApiClient.FileRef ref = FigmaApiClient.parse(file, request.getParameter("nodeId"));
			return convertRef(ref, token, FigmaApiClient.Auth.PERSONAL_TOKEN);
		} catch (RuntimeException e) {
			return error(e);
		}
	}

	/**
	 * OAuth token conversion (converterStart.clx, dmParam.token). With dmParam.url / fileKey (and nodeId) that file;
	 * otherwise the first file of the first project of the configured team (figma.team.id).
	 */
	@RequestMapping("/convert.do")
	public ResponseEntity<String> convert(DataRequest dataRequest) {
		String token = param(dataRequest, "token");
		try {
			String file = firstNonBlank(param(dataRequest, "url"), param(dataRequest, "fileKey"));
			if (file.isEmpty()) {
				List<JSONObject> projects = projects(token);
				if (projects.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(TEXT).body("No project found in team.");
				List<JSONObject> files = files(projects.get(0).optString("id"), token);
				if (files.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(TEXT).body("No files found in project.");
				file = files.get(0).optString("key");
			}
			return convertRef(FigmaApiClient.parse(file, param(dataRequest, "nodeId")), token, FigmaApiClient.Auth.OAUTH);
		} catch (RuntimeException e) {
			return error(e);
		}
	}

	/** Every file of every project of the team (OAuth token). */
	@RequestMapping("/convertAll.do")
	public ResponseEntity<String> convertAll(DataRequest dataRequest) {
		String token = param(dataRequest, "token");
		StringBuilder log = new StringBuilder();
		try {
			List<JSONObject> projects = projects(token);
			if (projects.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(TEXT).body("No projects found in team.");
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
			return ResponseEntity.ok().contentType(TEXT).body(log.toString());
		} catch (RuntimeException e) {
			return error(e);
		}
	}

	/** Uploaded Figma JSON (a saved GET /v1/files/:key or /nodes response) → template CLX (convertJson.clx). */
	@RequestMapping("/jsonConvert.do")
	public ResponseEntity<String> jsonConvert(DataRequest dataRequest) {
		try {
			File upload = firstJsonUpload(dataRequest);
			if (upload == null) return ResponseEntity.badRequest().contentType(TEXT).body("JSON 형식의 업로드 파일이 없습니다.");
			JSONObject json = new JSONObject(new String(Files.readAllBytes(upload.toPath()), StandardCharsets.UTF_8));
			FigmaConversionService.saveRawJson(json, json.optString("name", "upload"));
			return results(figmaConversionService.convert(json, Collections.<String>emptyList()));
		} catch (Exception e) {
			return error(e);
		}
	}

	/** v1.x coordinate converter (xylayout, no template) kept for comparison. */
	@RequestMapping("/jsonConvertLegacy.do")
	public ResponseEntity<String> jsonConvertLegacy(DataRequest dataRequest) {
		try {
			File upload = firstJsonUpload(dataRequest);
			if (upload == null) return ResponseEntity.badRequest().contentType(TEXT).body("JSON 형식의 업로드 파일이 없습니다.");
			@SuppressWarnings("unchecked")
			Map<String, Object> json = new com.fasterxml.jackson.databind.ObjectMapper().readValue(upload, Map.class);
			File clx = figmaToClxService.convertToClx(json);
			return ResponseEntity.ok().contentType(TEXT).body("CLX file saved successfully at: " + clx.getAbsolutePath());
		} catch (Exception e) {
			return error(e);
		}
	}

	@RequestMapping("/test.do")
	public View saveDtl3(HttpServletRequest request, HttpServletResponse response, DataRequest dataRequest) throws Exception {
		BigDecimal bd = new BigDecimal("9999999999999999");
		List<Map<String, Object>> list = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			Map<String, Object> map = new HashMap<>();
			map.put("column1", "test" + i);
			map.put("column2", "test2" + i);
			map.put("column3", bd);
			list.add(i, map);
		}
		dataRequest.setResponse("ds1", list);
		return new JSONDataView();
	}

	// ------------------------------------------------------------------ helpers

	private ResponseEntity<String> convertRef(FigmaApiClient.FileRef ref, String token, FigmaApiClient.Auth auth) {
		JSONObject json = FigmaApiClient.fetch(ref, token, auth);
		FigmaConversionService.saveRawJson(json, json.optString("name", ref.fileKey));
		return results(figmaConversionService.convert(json, ref.nodeIds));
	}

	private ResponseEntity<String> results(List<FigmaConversionService.Result> results) {
		boolean anyOk = false;
		for (FigmaConversionService.Result r : results) anyOk |= r.error == null;
		return ResponseEntity.status(anyOk ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR).contentType(TEXT).body(FigmaConversionService.describe(results));
	}

	private ResponseEntity<String> error(Exception e) {
		HttpStatus status = e instanceof IllegalArgumentException ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
		return ResponseEntity.status(status).contentType(TEXT).body("변환 실패: " + e.getMessage());
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

	private static File firstJsonUpload(DataRequest dataRequest) {
		Map<String, UploadFile[]> uploads = dataRequest.getUploadFiles();
		if (uploads == null) return null;
		for (UploadFile[] files : uploads.values()) {
			for (UploadFile upload : files) {
				File file = upload.getFile();
				if (file != null && file.getName().toLowerCase().contains(".json")) return file;
			}
		}
		return null;
	}

	private static String param(DataRequest dataRequest, String name) {
		ParameterGroup dm = dataRequest.getParameterGroup("dmParam");
		String value = dm == null ? null : dm.getValue(name);
		return value == null ? "" : value.trim();
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) { if (value != null && !value.trim().isEmpty()) return value.trim(); }
		return "";
	}
}
