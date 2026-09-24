package com.tomatosystem.exconverter.service;

import com.tomatosystem.exconverter.model.UiIr;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * eXBuilder6 pairs every .clx with a same-name .js file: the template's companion JS (comment header) with the
 * placeholders filled, followed by the event-handler skeleton for the listeners the generator wired (v2.2, only when
 * the UI-IR carries a backend binding; see {@link EventScriptGenerator}).
 */
public final class CompanionJsGenerator {
	private CompanionJsGenerator() {}

	public static byte[] generate(File templateClx, String outputFileName) { return generate(templateClx, outputFileName, null); }

	/** @param ir the compiled UI-IR whose handlers become functions; null for the plain template JS */
	public static byte[] generate(File templateClx, String outputFileName, UiIr ir) {
		LocalDateTime now = LocalDateTime.now();
		String date = now.format(DateTimeFormatter.ofPattern("yyyy. M. d.", Locale.KOREAN));
		String time = now.format(DateTimeFormatter.ofPattern("a h:mm:ss", Locale.KOREAN));
		String user = firstNonBlank(System.getProperty("user.name"), "exconverter");
		String body = readTemplateJs(templateClx);
		if (body.trim().isEmpty()) body = stub(outputFileName, date, time, user);
		else body = body.replace("${filename}", outputFileName).replace("${date}", date).replace("${time}", time).replace("${user}", user);
		String events = EventScriptGenerator.render(ir);
		if (!events.isEmpty()) body = body.replaceAll("\\s+$", "") + "\n" + events;
		return body.getBytes(StandardCharsets.UTF_8);
	}

	private static String readTemplateJs(File templateClx) {
		if (templateClx == null) return "";
		String name = templateClx.getName();
		int dot = name.lastIndexOf('.');
		File sibling = new File(templateClx.getParentFile(), (dot > 0 ? name.substring(0, dot) : name) + ".js");
		if (!sibling.isFile()) return "";
		try { return new String(Files.readAllBytes(sibling.toPath()), StandardCharsets.UTF_8); }
		catch (Exception e) { return ""; }
	}

	private static String stub(String fileName, String date, String time, String user) {
		return "/************************************************\n * " + fileName + "\n * Created at " + date + " " + time + ".\n *\n * @author " + user + "\n ************************************************/\n";
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) { if (value != null && !value.trim().isEmpty()) return value.trim(); }
		return "";
	}
}
