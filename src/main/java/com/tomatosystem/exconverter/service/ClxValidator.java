package com.tomatosystem.exconverter.service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Structural checks that catch CLX files eXBuilder6 would refuse or mis-render. */
public final class ClxValidator {
	private static final String CL_NAMESPACE = "http://tomatosystem.co.kr/cleopatra";
	private static final String STD_NAMESPACE = "http://tomatosystem.co.kr/cleopatra/studio";
	private ClxValidator() { }

	public static List<String> validate(byte[] contents) {
		List<String> errors = new ArrayList<String>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); factory.setNamespaceAware(true);
			Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(contents));
			if (document.getElementsByTagName("screen").getLength() == 0) errors.add("screen definition is missing");
			if (document.getElementsByTagName("body").getLength() == 0) errors.add("body is missing");
			Set<String> ids = new HashSet<String>();
			Set<String> sids = new HashSet<String>();
			Set<String> datasets = new HashSet<String>();
			NodeList all = document.getElementsByTagNameNS("*", "*");
			for (int i = 0; i < all.getLength(); i++) {
				Element e = (Element) all.item(i);
				String sid = e.getAttributeNS(STD_NAMESPACE, "sid");
				if (!sid.isEmpty() && !sids.add(sid)) errors.add("duplicate std:sid: " + sid);
				if (!CL_NAMESPACE.equals(e.getNamespaceURI()) || "property".equals(e.getLocalName()) || "datacolumn".equals(e.getLocalName())) continue;
				String id = e.getAttribute("id");
				if (!id.isEmpty() && !ids.add(id)) errors.add("duplicate id: " + id);
				if ("dataset".equals(e.getLocalName()) || "datamap".equals(e.getLocalName())) datasets.add(id);
			}
			NodeList grids = document.getElementsByTagNameNS(CL_NAMESPACE, "grid");
			for (int i = 0; i < grids.getLength(); i++) validateGrid((Element) grids.item(i), datasets, errors);
			NodeList layouts = document.getElementsByTagNameNS(CL_NAMESPACE, "formlayout");
			for (int i = 0; i < layouts.getLength(); i++) validateFormLayout((Element) layouts.item(i), errors);
		} catch (Exception e) { errors.add("XML is not well formed: " + e.getMessage()); }
		return errors;
	}

	private static void validateGrid(Element grid, Set<String> datasets, List<String> errors) {
		String name = grid.getAttribute("id").isEmpty() ? "grid" : grid.getAttribute("id");
		int columns = children(grid, "gridcolumn").size();
		if (columns == 0) errors.add(name + ": has no gridcolumn");
		String datasetId = grid.getAttribute("datasetid");
		if (!datasetId.isEmpty() && !datasets.contains(datasetId)) errors.add(name + ": dataset not found: " + datasetId);
		for (String band : new String[] { "gridheader", "griddetail" }) {
			for (Element b : children(grid, band)) {
				for (Element cell : children(b, "gridcell")) {
					int col = parse(cell.getAttribute("colindex"));
					if (col < 0 || col >= columns) errors.add(name + ": " + band + " cell colindex " + col + " outside " + columns + " columns");
				}
			}
		}
	}

	private static void validateFormLayout(Element layout, List<String> errors) {
		Node parent = layout.getParentNode();
		if (!(parent instanceof Element)) return;
		int rows = children(layout, "rows").size();
		int cols = children(layout, "columns").size();
		for (Element child : children((Element) parent, null)) {
			for (Element data : children(child, "formdata")) {
				int row = parse(data.getAttribute("row"));
				int col = parse(data.getAttribute("col"));
				int rowspan = Math.max(1, parse(data.getAttribute("rowspan")));
				int colspan = Math.max(1, parse(data.getAttribute("colspan")));
				String owner = child.getAttribute("id").isEmpty() ? child.getLocalName() : child.getAttribute("id");
				if (row < 0 || row + rowspan > rows || col < 0 || col + colspan > cols) errors.add(owner + ": formdata row=" + row + " col=" + col + " outside " + rows + "x" + cols + " formlayout");
			}
		}
	}

	private static List<Element> children(Element parent, String localName) {
		List<Element> result = new ArrayList<Element>();
		NodeList nodes = parent.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			if (nodes.item(i) instanceof Element && (localName == null || localName.equals(((Element) nodes.item(i)).getLocalName()))) result.add((Element) nodes.item(i));
		}
		return result;
	}

	private static int parse(String value) {
		try { return value == null || value.isEmpty() ? 0 : Integer.parseInt(value.trim()); } catch (NumberFormatException e) { return -1; }
	}
}
