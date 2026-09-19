package com.tomatosystem.figma;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A Figma REST API response: GET /v1/files/:key (document + components + componentSets) or
 * GET /v1/files/:key/nodes?ids=.. (nodes.{id}.document, each with its own component maps).
 * Resolves an INSTANCE to the names of its main component and component set, which are stable
 * ("Base-input", "Button", "Radio-button") while layer names are free text ("Frame 1000004302").
 */
public class FigmaDocument {
	private final String name;
	private final List<JSONObject> roots = new ArrayList<JSONObject>();
	private final JSONObject components = new JSONObject();
	private final JSONObject componentSets = new JSONObject();

	public FigmaDocument(JSONObject json) {
		this.name = json.optString("name", "");
		JSONObject nodes = json.optJSONObject("nodes");
		if (nodes != null) {
			for (Iterator<String> it = nodes.keys(); it.hasNext();) {
				JSONObject entry = nodes.optJSONObject(it.next());
				if (entry == null || entry.optJSONObject("document") == null) continue;
				roots.add(entry.getJSONObject("document"));
				merge(components, entry.optJSONObject("components"));
				merge(componentSets, entry.optJSONObject("componentSets"));
			}
		} else if (json.optJSONObject("document") != null) {
			roots.add(json.getJSONObject("document"));
			merge(components, json.optJSONObject("components"));
			merge(componentSets, json.optJSONObject("componentSets"));
		}
		if (roots.isEmpty()) throw new IllegalArgumentException("Figma JSON has neither document nor nodes");
	}

	public String getName() { return name; }

	/** Main component name, e.g. "Size=Medium, Select=On" for a variant; "" when unknown. */
	public String componentName(JSONObject instance) {
		JSONObject component = components.optJSONObject(instance.optString("componentId", ""));
		return component == null ? "" : component.optString("name", "");
	}

	/** Component set name, e.g. "Radio-button"; "" for a component outside a set. */
	public String componentSetName(JSONObject instance) {
		JSONObject component = components.optJSONObject(instance.optString("componentId", ""));
		if (component == null) return "";
		JSONObject set = componentSets.optJSONObject(component.optString("componentSetId", ""));
		return set == null ? "" : set.optString("name", "");
	}

	/**
	 * The screens to convert. With node ids, those nodes; otherwise per page (CANVAS): each top-level frame when the
	 * page holds separate, non-overlapping frames, else the whole page as one screen (loose layers on the canvas).
	 */
	public List<JSONObject> screens(List<String> nodeIds) {
		List<JSONObject> result = new ArrayList<JSONObject>();
		if (nodeIds != null && !nodeIds.isEmpty()) {
			for (String id : nodeIds) {
				for (JSONObject root : roots) {
					JSONObject found = find(root, id.replace('-', ':'));
					if (found != null) { addScreens(found, result); break; }
				}
			}
			if (result.isEmpty()) throw new IllegalArgumentException("Node not found: " + nodeIds);
			return result;
		}
		for (JSONObject root : roots) addScreens(root, result);
		return result;
	}

	private void addScreens(JSONObject node, List<JSONObject> result) {
		String type = node.optString("type");
		if ("DOCUMENT".equals(type)) {
			for (JSONObject page : children(node)) addScreens(page, result);
		} else if ("CANVAS".equals(type) || "SECTION".equals(type)) {
			List<JSONObject> frames = new ArrayList<JSONObject>();
			boolean loose = false;
			for (JSONObject child : children(node)) {
				if (!FigmaNodes.isVisible(child)) continue;
				String t = child.optString("type");
				if ("SECTION".equals(t)) { addScreens(child, result); continue; }
				FigmaNodes.Box box = FigmaNodes.box(child);
				if (("FRAME".equals(t) || "COMPONENT".equals(t)) && box != null && box.w >= 320 && box.h >= 200) frames.add(child);
				else loose = true;
			}
			if (!loose && !frames.isEmpty() && !overlapping(frames)) result.addAll(frames);
			else if (!frames.isEmpty() || loose) result.add(node);
		} else {
			result.add(node);
		}
	}

	private static boolean overlapping(List<JSONObject> frames) {
		for (int i = 0; i < frames.size(); i++) {
			for (int j = i + 1; j < frames.size(); j++) {
				if (FigmaNodes.box(frames.get(i)).intersects(FigmaNodes.box(frames.get(j)))) return true;
			}
		}
		return false;
	}

	private static JSONObject find(JSONObject node, String id) {
		if (id.equals(node.optString("id"))) return node;
		for (JSONObject child : children(node)) {
			JSONObject found = find(child, id);
			if (found != null) return found;
		}
		return null;
	}

	static List<JSONObject> children(JSONObject node) {
		List<JSONObject> result = new ArrayList<JSONObject>();
		JSONArray array = node.optJSONArray("children");
		if (array == null) return result;
		for (int i = 0; i < array.length(); i++) { JSONObject child = array.optJSONObject(i); if (child != null) result.add(child); }
		return result;
	}

	private static void merge(JSONObject target, JSONObject source) {
		if (source == null) return;
		for (Iterator<String> it = source.keys(); it.hasNext();) { String key = it.next(); target.put(key, source.get(key)); }
	}
}
