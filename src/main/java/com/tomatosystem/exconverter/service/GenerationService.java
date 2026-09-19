package com.tomatosystem.exconverter.service;

import com.tomatosystem.exconverter.model.UiIr;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GenerationService {
	private static final Logger LOGGER = LoggerFactory.getLogger(GenerationService.class);
	private final TemplateCatalog catalog = new TemplateCatalog();
	private final ClxGenerator generator = new ClxGenerator();
	@Autowired(required = false) private ServletContext servletContext;

	public GenerationResult generate(UiIr ir) {
		return generate(ir, "generated", "");
	}

	public GenerationResult generate(UiIr ir, String originalFileName) {
		return generate(ir, originalFileName, "");
	}

	/** @param rawUiIrJson analyzer output kept next to the upload for debugging; may be empty */
	public GenerationResult generate(UiIr ir, String originalFileName, String rawUiIrJson) {
		for (UiIr.Region grid : ir.regionsOf(UiIr.GRID)) {
			if (grid.getColumns().size() > 60) throw new IllegalArgumentException("A grid supports up to 60 columns");
		}
		StringBuilder summary = new StringBuilder();
		for (UiIr.Region region : ir.getRegions()) {
			if (summary.length() > 0) summary.append(" → ");
			summary.append(region.getType());
			if (!region.getFields().isEmpty()) summary.append("(필드 ").append(region.getFields().size()).append(")");
			if (!region.getColumns().isEmpty()) summary.append("(컬럼 ").append(region.getColumns().size()).append(")");
		}
		ProgressLog.step("UI-IR 영역: {}", summary);
		TemplateCatalog.TemplateMatch template = catalog.selectFor(ir);
		ProgressLog.step("템플릿 선택: {} (점수 {}, {})", template.getId(), template.getScore(), template.getProfile());
		byte[] clx = generator.generate(template.getFile(), ir);
		List<String> errors = ClxValidator.validate(clx);
		if (!errors.isEmpty()) throw new IllegalStateException("Generated CLX is invalid: " + errors);
		ProgressLog.step("CLX 생성/검증 완료 ({} bytes){}", clx.length, ir.getWarnings().isEmpty() ? "" : " 경고: " + ir.getWarnings());
		try {
			String id = UUID.randomUUID().toString();
			String dateFolder = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
			File directory = ProjectRootResolver.resultDirectory(servletContext, dateFolder);
			String baseName = sanitizeBaseName(originalFileName);
			File clxFile = new File(directory, baseName + ".clx");
			File jsFile = new File(directory, baseName + ".js");
			Files.write(clxFile.toPath(), clx, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			Files.write(jsFile.toPath(), CompanionJsGenerator.generate(template.getFile(), baseName + ".js"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			writeUiIr(baseName, rawUiIrJson);
			ProgressLog.step("저장 완료: {} / {}", clxFile.getAbsolutePath(), jsFile.getName());
			return new GenerationResult(id, clxFile, jsFile, template.getId(), new ArrayList<String>(ir.getWarnings()));
		} catch (Exception e) { throw new IllegalStateException("Could not save generated CLX/JS", e); }
	}

	/** The UI-IR is not an eXBuilder6 source, so it goes to the generated root rather than clx-src. */
	private void writeUiIr(String baseName, String rawUiIrJson) {
		if (rawUiIrJson == null || rawUiIrJson.trim().isEmpty()) return;
		try {
			File dir = new File(ExConverterConfig.get("exconverter.generated.root", "generated"), "ui-ir");
			if (!dir.exists() && !dir.mkdirs()) return;
			File file = new File(dir, baseName + ".ui-ir.json");
			Files.write(file.toPath(), rawUiIrJson.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			LOGGER.info("Wrote UI-IR: {}", file.getAbsolutePath());
		} catch (Exception e) { LOGGER.warn("Could not save UI-IR: {}", e.getMessage()); }
	}

	public File find(String id) {
		throw new IllegalArgumentException("Download is no longer required; files are written to clx-src/result/{yyyy-MM-dd}");
	}

	static String sanitizeBaseName(String originalFileName) {
		String name = originalFileName == null ? "" : originalFileName.replace('\\', '/');
		int slash = name.lastIndexOf('/');
		if (slash >= 0) name = name.substring(slash + 1);
		int dot = name.lastIndexOf('.');
		if (dot > 0) name = name.substring(0, dot);
		name = name.replaceAll("[\\p{Cntrl}<>:\"/\\\\|?*]", "_").trim();
		if (name.isEmpty() || name.equals(".") || name.equals("..")) return "generated";
		return name;
	}

	public static class GenerationResult {
		private final String id;
		private final File file;
		private final File jsFile;
		private final String templateId;
		private final List<String> warnings;
		GenerationResult(String id, File file, File jsFile, String templateId, List<String> warnings) {
			this.id = id; this.file = file; this.jsFile = jsFile; this.templateId = templateId; this.warnings = warnings;
		}
		public String getId() { return id; }
		public File getFile() { return file; }
		public File getJsFile() { return jsFile; }
		public String getTemplateId() { return templateId; }
		public List<String> getWarnings() { return warnings; }
	}
}
