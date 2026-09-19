package com.tomatosystem.exconverter.service;

import com.tomatosystem.exconverter.model.UiIr;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Metadata/rule-based template retrieval. Each CLX under the template root is profiled (header search bar,
 * popup, footer, and the body layout as a {@link LayoutShape} token sequence) and compared with the UI-IR.
 * Order and arrangement count: grid-over-form (P4-5) and form-over-grid (P4-4) are different templates, as are
 * grid|form panes (P3-2) and form|grid panes (P4-3).
 * Adding a CLX to the repository is enough to make it selectable; nothing is retrained.
 */
public class TemplateCatalog {
	private final Map<String, TemplateProfile> profiles = new ConcurrentHashMap<String, TemplateProfile>();

	public TemplateMatch selectFor(UiIr ir) { return selectFor(ir, templateRoot()); }

	/** Selection among the templates under a given root (the watcher and the harness check a specific folder). */
	public TemplateMatch selectFor(UiIr ir, File root) {
		List<File> candidates = templateFiles(root);
		if (candidates.isEmpty()) throw new IllegalStateException("No CLX template exists under " + root.getAbsolutePath());
		Features wanted = Features.of(ir);
		List<TemplateMatch> ranked = new ArrayList<TemplateMatch>();
		for (File file : candidates) {
			TemplateProfile profile = profile(file);
			// Without a content-body (P0 inner pattern) the generator has nowhere to put the regions.
			if (profile == null || !profile.usable) continue;
			ranked.add(new TemplateMatch(file, idOf(root, file), profile.score(wanted), profile.describe()));
		}
		if (ranked.isEmpty()) throw new IllegalStateException("No readable CLX template exists under " + root.getAbsolutePath());
		ranked.sort(Comparator.comparingInt(TemplateMatch::getScore).reversed().thenComparingLong((TemplateMatch m) -> m.getFile().length()).thenComparing(TemplateMatch::getId));
		return ranked.get(0);
	}

	/** Every *.clx under the root, sorted by path. Read on each call, so added files take part immediately. */
	public static List<File> templateFiles(File root) {
		if (!root.isDirectory()) throw new IllegalStateException("Template directory not found: " + root.getAbsolutePath());
		List<File> files = new ArrayList<File>();
		try (Stream<java.nio.file.Path> walk = Files.walk(root.toPath())) {
			walk.filter(path -> path.toString().toLowerCase().endsWith(".clx")).forEach(path -> files.add(path.toFile()));
		} catch (IOException e) { throw new IllegalStateException("Could not read template directory", e); }
		files.sort(Comparator.comparing(File::getPath));
		return files;
	}

	public static String idOf(File root, File file) { return root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/'); }

	private static volatile File projectTemplates;

	/**
	 * Template root, in order: exconverter.template.root; the project's own templates/ folder (so a CLX added
	 * there is used at once, without Publish; off with exconverter.template.useProjectFolder=false); the copy
	 * WTP packages into WEB-INF/classes/exconverter/templates; ./templates.
	 */
	public static File templateRoot() {
		String configuredRoot = ExConverterConfig.get("exconverter.template.root", "");
		if (!configuredRoot.isEmpty()) return new File(configuredRoot);
		if (!"false".equalsIgnoreCase(ExConverterConfig.get("exconverter.template.useProjectFolder", "true"))) {
			File source = projectTemplates;
			if (source == null) {
				try { source = new File(ProjectRootResolver.resolve(null), "templates"); } catch (Exception ignored) { source = new File(""); }
				projectTemplates = source;
			}
			if (source.isDirectory()) return source;
		}
		try {
			URL packagedTemplates = Thread.currentThread().getContextClassLoader().getResource("exconverter/templates");
			if (packagedTemplates != null && "file".equalsIgnoreCase(packagedTemplates.getProtocol())) return new File(packagedTemplates.toURI());
		} catch (Exception ignored) { /* The local development fallback below remains valid. */ }
		return new File("templates");
	}

	private TemplateProfile profile(File file) {
		String path = file.getAbsolutePath() + ":";
		String key = path + file.lastModified() + ":" + file.length();
		TemplateProfile cached = profiles.get(key);
		if (cached != null) return cached;
		TemplateProfile profile;
		try { profile = TemplateProfile.of(file.getName(), Files.readAllBytes(file.toPath())); }
		catch (Exception e) { profile = TemplateProfile.unreadable(e.getMessage()); } // unusable, never selected
		profiles.keySet().removeIf(k -> k.startsWith(path)); // an edited template replaces its old profile
		profiles.put(key, profile);
		return profile;
	}

	/** Body layout of one template as text (e.g. "DIV{Gb | Gb}"), for reports. */
	public String describe(File file) {
		TemplateProfile profile = profile(file);
		return profile.error != null ? "(읽을 수 없음: " + profile.error + ")" : profile.describe();
	}

	/** What the design image asks for. */
	static final class Features {
		boolean search; boolean tabs; boolean tree; boolean popup; boolean footer; boolean shuttleColumn;
		List<LayoutShape.Block> body;
		static Features of(UiIr ir) {
			Features f = new Features();
			for (UiIr.Region region : ir.getRegions()) { if (UiIr.SEARCH.equals(region.getType()) && region.getSide().isEmpty()) f.search = true; }
			f.tabs = ir.firstRegion(UiIr.TABS) != null;
			f.tree = ir.firstRegion(UiIr.TREE) != null;
			// shuttleColumn below: ◀▶ between the panes only; ▲▼ between two grids of one pane is not a P7 shuttle column.
			f.popup = ir.getScreenType().toUpperCase().contains("POPUP");
			f.footer = !ir.getRegions().isEmpty() && UiIr.BUTTONS.equals(ir.getRegions().get(ir.getRegions().size() - 1).getType());
			f.body = LayoutShape.of(ir);
			f.shuttleColumn = LayoutShape.hasShuttleColumn(f.body);
			return f;
		}
	}

	/** Structural fingerprint of a template CLX. */
	static final class TemplateProfile {
		boolean usable; boolean search; boolean tabs; boolean tree; boolean popup; boolean footer; boolean shuttleColumn;
		/** Body layout blocks, see {@link LayoutShape}. */
		List<LayoutShape.Block> body;

		/** A template that cannot be read; kept in the cache so it is not re-parsed on every request. */
		static TemplateProfile unreadable(String reason) {
			TemplateProfile p = new TemplateProfile();
			p.body = new ArrayList<LayoutShape.Block>();
			p.error = reason;
			return p;
		}

		String error;

		static TemplateProfile of(String fileName, byte[] xml) throws Exception {
			Document doc = TemplateReverse.parse(xml);
			TemplateProfile p = new TemplateProfile();
			Element data = firstGroupByClass(doc, "content-body", "pop-content-body");
			p.usable = data != null;
			// The header search bar only: P2-3 has a second search-box inside the body, which is a body token.
			for (Element search : groupsByClass(doc, "search-box")) { if (data == null || !isInside(search, data)) p.search = true; }
			p.popup = fileName.matches(".*_P\\.clx$") || data != null && data.getAttribute("class").contains("pop-content-body");
			p.footer = !groupsByClass(doc, "footer-button-group").isEmpty();
			p.body = LayoutShape.of(data);
			p.tabs = LayoutShape.contains(p.body, "TAB");
			p.tree = LayoutShape.contains(p.body, "T");
			p.shuttleColumn = LayoutShape.hasShuttleColumn(p.body);
			return p;
		}

		/**
		 * 100, then: header search bar ±40, popup mismatch −80, footer +5, body layout distance (order and
		 * arrangement, 20 per differing block, 5 per differing grid/form flag), tabs or tree presence mismatch −60,
		 * shuttle column mismatch −40 (a P7 shuttle is chosen for ◀▶ between panes, not for any two panes).
		 */
		int score(Features w) {
			int score = 100;
			score += w.search == search ? 40 : -40;
			score += w.popup == popup ? 0 : -80;
			score += w.footer == footer ? 5 : 0;
			score -= LayoutShape.distance(w.body, body);
			score += w.tabs == tabs ? 0 : -60;
			score += w.tree == tree ? 0 : -60;
			score += w.shuttleColumn == shuttleColumn ? 0 : -40;
			return score;
		}

		String describe() {
			return "search=" + search + ", popup=" + popup + ", body=" + LayoutShape.join(body);
		}

		private static List<Element> groupsByClass(Document doc, String cls) {
			List<Element> result = new ArrayList<Element>();
			NodeList groups = doc.getElementsByTagNameNS("*", "group");
			for (int i = 0; i < groups.getLength(); i++) {
				Element group = (Element) groups.item(i);
				for (String c : group.getAttribute("class").trim().split("\\s+")) { if (c.equals(cls)) { result.add(group); break; } }
			}
			return result;
		}

		private static Element firstGroupByClass(Document doc, String... classes) {
			for (String cls : classes) { List<Element> found = groupsByClass(doc, cls); if (!found.isEmpty()) return found.get(0); }
			return null;
		}

		private static boolean isInside(Node node, Element ancestor) {
			for (Node n = node.getParentNode(); n != null; n = n.getParentNode()) { if (n == ancestor) return true; }
			return false;
		}
	}

	public static class TemplateMatch {
		private final File file; private final String id; private final int score; private final String profile;
		TemplateMatch(File file, String id, int score, String profile) { this.file = file; this.id = id; this.score = score; this.profile = profile; }
		public File getFile() { return file; }
		public String getId() { return id; }
		public int getScore() { return score; }
		public String getProfile() { return profile; }
	}
}
