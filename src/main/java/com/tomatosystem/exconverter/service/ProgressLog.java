package com.tomatosystem.exconverter.service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pipeline progress for the Eclipse console. Written to System.out so it is visible
 * regardless of the logging backend configuration (log4j's console appender would print it twice).
 */
public final class ProgressLog {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProgressLog.class);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

	private ProgressLog() { }

	public static void step(String message, Object... args) {
		String text = format(message, args);
		System.out.println("[eXConverter " + LocalTime.now().format(TIME) + "] " + text);
		System.out.flush();
		if (LOGGER.isDebugEnabled()) LOGGER.debug(text);
	}

	private static String format(String message, Object... args) {
		StringBuilder out = new StringBuilder();
		int argIndex = 0;
		int from = 0;
		int at;
		while ((at = message.indexOf("{}", from)) >= 0) {
			out.append(message, from, at).append(argIndex < args.length ? String.valueOf(args[argIndex++]) : "{}");
			from = at + 2;
		}
		return out.append(message.substring(from)).toString();
	}
}
