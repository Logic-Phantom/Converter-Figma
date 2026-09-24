package com.tomatosystem.web;

import com.tomatosystem.exconverter.service.ProjectRootResolver;
import com.tomatosystem.figma.FigmaApiClient;
import com.tomatosystem.figma.qa.HeadlessBrowser;
import com.tomatosystem.figma.qa.VisualQaService;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Visual QA of an already generated screen (v2.2 feature 3). The conversion endpoints run the same check with
 * {@code qa=true}; this controller re-runs it for an existing CLX and serves the reports.
 *
 * <ul>
 *   <li>{@code GET /figma/qa/compare.do?clx=convertTest/2026-09-24/화면.clx&url=<Figma frame link>&token=&width=&height=}
 *       — clx is relative to clx-src; url/fileKey + nodeId name the frame; width/height default to 1440×860.</li>
 *   <li>{@code GET /figma/qa/status.do} — which renderer/compiler would be used.</li>
 *   <li>{@code GET /figma/qa/report.do?path=2026-09-24/화면/report.html} — files under clx-src/result/visual-qa.</li>
 * </ul>
 */
@Controller
@RequestMapping("/figma/qa")
public class VisualQaController {
	@Autowired(required = false) private ServletContext servletContext;

	@GetMapping("/compare.do")
	public ResponseEntity<String> compare(HttpServletRequest request) {
		try {
			File root = ProjectRootResolver.resolve(servletContext);
			String clxParam = ConversionRequests.firstNonBlank(request.getParameter("clx"));
			if (clxParam.isEmpty()) return ConversionRequests.text(HttpStatus.BAD_REQUEST, "clx 파라미터(clx-src 기준 상대 경로)가 필요합니다.");
			File clx = safeChild(new File(root, "clx-src"), clxParam);
			if (clx == null || !clx.isFile()) return ConversionRequests.text(HttpStatus.NOT_FOUND, "CLX 를 찾지 못했습니다: " + clxParam);
			File js = new File(clx.getParentFile(), clx.getName().replaceAll("\\.clx$", ".js"));
			String fileKey = "";
			String nodeId = "";
			String token = ConversionRequests.firstNonBlank(request.getParameter("token"), com.tomatosystem.figma.FigmaSettings.get("figma.direct.token", ""));
			String file = ConversionRequests.firstNonBlank(request.getParameter("url"), request.getParameter("fileKey"));
			if (!file.isEmpty()) {
				FigmaApiClient.FileRef ref = FigmaApiClient.parse(file, request.getParameter("nodeId"));
				fileKey = ref.fileKey;
				nodeId = ref.nodeIds.isEmpty() ? "" : ref.nodeIds.get(0);
			}
			int width = parse(request.getParameter("width"), 1440);
			int height = parse(request.getParameter("height"), 860);
			VisualQaService.Report report = VisualQaService.run(root, clx, js, fileKey, nodeId, width, height, token, FigmaApiClient.Auth.PERSONAL_TOKEN, clx.getName().replaceAll("\\.clx$", ""));
			return ConversionRequests.text(report.error == null ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR, report.summary()
				+ (report.html == null ? "" : "\n    리포트 URL: /figma/qa/report.do?path=" + relative(new File(root, "clx-src/result/visual-qa"), report.html)));
		} catch (RuntimeException e) {
			return ConversionRequests.error(e);
		}
	}

	@GetMapping("/status.do")
	public ResponseEntity<String> status() {
		JSONObject status = new JSONObject().put("renderer", HeadlessBrowser.describe());
		try { status.put("compiler", com.tomatosystem.figma.qa.ClxCompiler.compilerJar(ProjectRootResolver.resolve(servletContext)).getAbsolutePath()); }
		catch (RuntimeException e) { status.put("compiler", "").put("compilerError", e.getMessage()); }
		try { status.put("runtime", com.tomatosystem.figma.qa.ClxCompiler.runtimeDir(ProjectRootResolver.resolve(servletContext)).getAbsolutePath()); }
		catch (RuntimeException e) { status.put("runtime", "").put("runtimeError", e.getMessage()); }
		return ResponseEntity.ok().contentType(ConversionRequests.JSON).body(status.toString(2));
	}

	/** Serves report.html / png / json from clx-src/result/visual-qa (no path traversal). */
	@GetMapping("/report.do")
	public ResponseEntity<byte[]> report(HttpServletRequest request) {
		try {
			File base = new File(ProjectRootResolver.resolve(servletContext), "clx-src/result/visual-qa");
			File file = safeChild(base, request.getParameter("path"));
			if (file == null || !file.isFile()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("not found".getBytes(StandardCharsets.UTF_8));
			String name = file.getName().toLowerCase();
			MediaType type = name.endsWith(".html") ? MediaType.valueOf("text/html;charset=UTF-8") : name.endsWith(".png") ? MediaType.IMAGE_PNG
				: name.endsWith(".json") ? ConversionRequests.JSON : MediaType.TEXT_PLAIN;
			return ResponseEntity.ok().contentType(type).body(Files.readAllBytes(file.toPath()));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(String.valueOf(e.getMessage()).getBytes(StandardCharsets.UTF_8));
		}
	}

	private static File safeChild(File base, String relative) {
		if (relative == null || relative.trim().isEmpty()) return null;
		try {
			File file = new File(base, relative.replace('\\', '/')).getCanonicalFile();
			return file.getPath().startsWith(base.getCanonicalFile().getPath()) ? file : null;
		} catch (java.io.IOException e) { return null; }
	}

	private static String relative(File base, File file) {
		return base.toPath().toAbsolutePath().relativize(file.toPath().toAbsolutePath()).toString().replace(File.separatorChar, '/');
	}

	private static int parse(String value, int defaultValue) {
		try { return Math.max(320, Integer.parseInt(value.trim())); } catch (RuntimeException e) { return defaultValue; }
	}
}
