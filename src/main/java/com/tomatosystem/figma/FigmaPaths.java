package com.tomatosystem.figma;

import com.tomatosystem.exconverter.service.ProjectRootResolver;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Folders under the project's clx-src, found by {@link ProjectRootResolver} (Eclipse workspace aware) instead of a
 * developer's hard-coded home directory.
 */
public final class FigmaPaths {
	private FigmaPaths() { }

	/** clx-src/{parts...}, created when missing. */
	public static File clxSrc(String... parts) {
		File dir;
		try { dir = new File(ProjectRootResolver.resolve(null), "clx-src"); }
		catch (RuntimeException e) { dir = new File("clx-src").getAbsoluteFile(); }
		for (String part : parts) dir = new File(dir, part);
		if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Could not create " + dir.getAbsolutePath());
		return dir;
	}

	public static String today() { return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE); }
}
