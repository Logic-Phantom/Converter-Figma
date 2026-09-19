package com.tomatosystem.web;

import com.cleopatra.protocol.data.DataRequest;
import com.cleopatra.protocol.data.ParameterGroup;
import com.tomatosystem.figma.FigmaApiClient;
import com.tomatosystem.figma.FigmaConversionService;
import com.tomatosystem.figma.FigmaSettings;
import java.util.List;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Former form-layout experiment (ClxLayoutUtil). The template pipeline replaces it: the "form" look now comes from
 * the most similar template (search-box / form-base formlayouts) instead of hand-written formlayout XML.
 * The old /convertJsonToFormClx.do (arbitrary server file path in, arbitrary path out) was removed.
 */
@RestController
@RequestMapping("/designForm")
public class AdvancedDesignController {
	private static final MediaType TEXT = MediaType.valueOf("text/plain;charset=UTF-8");

	@Autowired
	private FigmaConversionService figmaConversionService;

	/** dmParam: token (personal access token), fileKey or url, nodeId. */
	@RequestMapping("/convertAdvanced.do")
	public ResponseEntity<String> convertAdvancedClx(DataRequest dataRequest) {
		ParameterGroup dm = dataRequest.getParameterGroup("dmParam");
		String token = dm == null ? "" : nvl(dm.getValue("token"));
		String file = dm == null ? "" : nvl(dm.getValue("url")).isEmpty() ? nvl(dm.getValue("fileKey")) : nvl(dm.getValue("url"));
		return convert(token, file, dm == null ? null : dm.getValue("nodeId"));
	}

	/** convertForm.clx "폼변환": the configured direct file (FIGMA_DIRECT_TOKEN / FIGMA_DIRECT_FILEKEY). */
	@RequestMapping("/convertFigmaToFormClx.do")
	public ResponseEntity<String> convertFigmaToFormClx() {
		return convert(FigmaSettings.get("figma.direct.token", ""), FigmaSettings.get("figma.direct.fileKey", ""), null);
	}

	private ResponseEntity<String> convert(String token, String file, String nodeId) {
		try {
			FigmaApiClient.FileRef ref = FigmaApiClient.parse(file, nodeId);
			JSONObject json = FigmaApiClient.fetch(ref, token, FigmaApiClient.Auth.PERSONAL_TOKEN);
			FigmaConversionService.saveRawJson(json, json.optString("name", ref.fileKey));
			List<FigmaConversionService.Result> results = figmaConversionService.convert(json, ref.nodeIds);
			return ResponseEntity.ok().contentType(TEXT).body(FigmaConversionService.describe(results));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(TEXT).body("변환 실패: " + e.getMessage());
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(TEXT).body("변환 실패: " + e.getMessage());
		}
	}

	private static String nvl(String value) { return value == null ? "" : value.trim(); }
}
