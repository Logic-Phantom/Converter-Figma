package com.tomatosystem.web;

import com.cleopatra.protocol.data.DataRequest;
import com.cleopatra.protocol.data.ParameterGroup;
import com.cleopatra.protocol.data.UploadFile;
import com.tomatosystem.figma.ConversionOptions;
import com.tomatosystem.figma.FigmaApiClient;
import com.tomatosystem.figma.FigmaSettings;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Request plumbing shared by the conversion controllers: parameters from a plain GET or from an eXBuilder
 * DataRequest (dmParam), uploads, the Figma token/file defaults, the v2.2 options (swagger, qa) and the
 * text/JSON error responses. Keeps the controllers to routing.
 */
final class ConversionRequests {
	static final MediaType TEXT = MediaType.valueOf("text/plain;charset=UTF-8");
	static final MediaType JSON = MediaType.valueOf("application/json;charset=UTF-8");

	private ConversionRequests() { }

	/** Everything a conversion request may carry, from either request style. */
	static final class Input {
		final FigmaApiClient.FileRef ref;
		final String token;
		final FigmaApiClient.Auth auth;
		final String swagger;
		final boolean qa;
		private Input(FigmaApiClient.FileRef ref, String token, FigmaApiClient.Auth auth, String swagger, boolean qa) {
			this.ref = ref; this.token = token; this.auth = auth; this.swagger = swagger; this.qa = qa;
		}

		/** Loads the OpenAPI document (when given) and packs the QA credentials. */
		ConversionOptions options() { return ConversionOptions.of(swagger, qa, ref == null ? "" : ref.fileKey, token, auth); }
	}

	/** GET style: token, url | fileKey, nodeId, swagger, qa. Missing token/file fall back to figma.direct.* settings. */
	static Input fromRequest(HttpServletRequest request) {
		String token = firstNonBlank(request.getParameter("token"), FigmaSettings.get("figma.direct.token", ""));
		String file = firstNonBlank(request.getParameter("url"), request.getParameter("fileKey"), FigmaSettings.get("figma.direct.fileKey", ""));
		return new Input(FigmaApiClient.parse(file, request.getParameter("nodeId")), token, FigmaApiClient.Auth.PERSONAL_TOKEN,
			firstNonBlank(request.getParameter("swagger"), request.getParameter("openapi")), isTrue(request.getParameter("qa")));
	}

	/** eXBuilder style (dmParam): token, url | fileKey, nodeId, swagger, qa; the token is an OAuth token unless personal=true. */
	static Input fromDataRequest(DataRequest dataRequest, boolean personalToken) {
		String token = firstNonBlank(param(dataRequest, "token"), personalToken ? FigmaSettings.get("figma.direct.token", "") : "");
		String file = firstNonBlank(param(dataRequest, "url"), param(dataRequest, "fileKey"), personalToken ? FigmaSettings.get("figma.direct.fileKey", "") : "");
		FigmaApiClient.FileRef ref = file.isEmpty() ? null : FigmaApiClient.parse(file, param(dataRequest, "nodeId"));
		return new Input(ref, token, personalToken ? FigmaApiClient.Auth.PERSONAL_TOKEN : FigmaApiClient.Auth.OAUTH,
			firstNonBlank(param(dataRequest, "swagger"), param(dataRequest, "openapi")), isTrue(param(dataRequest, "qa")));
	}

	/** Options for an uploaded-JSON conversion: an uploaded *.json that is an OpenAPI document, or a swagger URL parameter. */
	static ConversionOptions uploadOptions(DataRequest dataRequest, File figmaJson) {
		String swagger = firstNonBlank(param(dataRequest, "swagger"), param(dataRequest, "openapi"));
		if (swagger.isEmpty()) {
			for (File upload : uploads(dataRequest, ".json")) {
				if (upload.equals(figmaJson)) continue;
				try {
					String head = new String(Files.readAllBytes(upload.toPath()), StandardCharsets.UTF_8);
					if (head.contains("\"openapi\"") || head.contains("\"swagger\"")) { swagger = upload.getAbsolutePath(); break; }
				} catch (Exception ignored) { /* not a spec */ }
			}
		}
		return ConversionOptions.of(swagger, false, "", "", FigmaApiClient.Auth.PERSONAL_TOKEN);
	}

	/** The uploaded Figma JSON: the first *.json upload that is not an OpenAPI document. */
	static File figmaUpload(DataRequest dataRequest) {
		File first = null;
		for (File upload : uploads(dataRequest, ".json")) {
			try {
				String text = new String(Files.readAllBytes(upload.toPath()), StandardCharsets.UTF_8);
				if (text.contains("\"document\"") || text.contains("\"nodes\"")) return upload;
			} catch (Exception ignored) { /* fall through */ }
			if (first == null) first = upload;
		}
		return first;
	}

	static java.util.List<File> uploads(DataRequest dataRequest, String suffix) {
		java.util.List<File> result = new java.util.ArrayList<File>();
		Map<String, UploadFile[]> uploads = dataRequest == null ? null : dataRequest.getUploadFiles();
		if (uploads == null) return result;
		for (UploadFile[] files : uploads.values()) {
			for (UploadFile upload : files) {
				File file = upload.getFile();
				if (file != null && file.getName().toLowerCase(Locale.ROOT).contains(suffix)) result.add(file);
			}
		}
		return result;
	}

	static String param(DataRequest dataRequest, String name) {
		ParameterGroup dm = dataRequest == null ? null : dataRequest.getParameterGroup("dmParam");
		String value = dm == null ? null : dm.getValue(name);
		return value == null ? "" : value.trim();
	}

	static ResponseEntity<String> text(HttpStatus status, String body) { return ResponseEntity.status(status).contentType(TEXT).body(body); }

	static ResponseEntity<String> error(Exception e) {
		HttpStatus status = e instanceof IllegalArgumentException ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
		return text(status, "변환 실패: " + e.getMessage());
	}

	static boolean isTrue(String value) { return value != null && value.trim().matches("(?i)true|1|y|yes|on"); }

	static String firstNonBlank(String... values) {
		for (String value : values) { if (value != null && !value.trim().isEmpty()) return value.trim(); }
		return "";
	}
}
