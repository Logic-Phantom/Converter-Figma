package com.tomatosystem.exconverter.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Framework-independent intermediate representation used between AI/OCR and CLX.
 * Regions are kept in visual (top-to-bottom) order; the CLX compiler decides
 * which eXBuilder6 container (content-header/body/footer) receives each one.
 */
public class UiIr {
	public static final String TITLE = "title";
	public static final String DESCRIPTION = "description";
	public static final String SECTION_TITLE = "sectionTitle";
	public static final String SEARCH = "search";
	public static final String FORM = "form";
	public static final String GRID = "grid";
	public static final String TABS = "tabs";
	public static final String TREE = "tree";
	public static final String BUTTONS = "buttons";
	public static final String TEXTAREA = "textarea";
	/** Region.side values for a screen split into a left and a right pane; "" means full width. */
	public static final String LEFT = "left";
	public static final String RIGHT = "right";

	private String screenName;
	private String screenType = "";
	private int width;
	private int height;
	/** Width in pixels of the image the analyzer saw; bbox/width values are relative to it. */
	private int sourceWidth;
	private final List<Region> regions = new ArrayList<Region>();
	private final List<String> warnings = new ArrayList<String>();

	public String getScreenName() { return screenName; }
	public void setScreenName(String screenName) { this.screenName = screenName; }
	public String getScreenType() { return screenType; }
	public void setScreenType(String screenType) { this.screenType = screenType == null ? "" : screenType; }
	public int getWidth() { return width; }
	public void setWidth(int width) { this.width = width; }
	public int getHeight() { return height; }
	public void setHeight(int height) { this.height = height; }
	public int getSourceWidth() { return sourceWidth; }
	public void setSourceWidth(int sourceWidth) { this.sourceWidth = sourceWidth; }
	public List<Region> getRegions() { return regions; }
	public List<String> getWarnings() { return warnings; }

	public List<Region> regionsOf(String type) {
		List<Region> result = new ArrayList<Region>();
		for (Region region : regions) { if (type.equals(region.getType())) result.add(region); }
		return result;
	}

	public Region firstRegion(String type) {
		for (Region region : regions) { if (type.equals(region.getType())) return region; }
		return null;
	}

	/** True when at least one region sits in a left or right pane rather than spanning the full width. */
	public boolean isSplit() {
		for (Region region : regions) { if (!region.getSide().isEmpty()) return true; }
		return false;
	}

	/** Legacy accessor: fields of the first search region. */
	public List<Field> getSearchFields() {
		Region search = firstRegion(SEARCH);
		return search == null ? new ArrayList<Field>() : search.getFields();
	}

	/** Legacy accessor: header texts of the first grid region. */
	public List<String> getGridColumns() {
		List<String> result = new ArrayList<String>();
		Region grid = firstRegion(GRID);
		if (grid != null) { for (Column column : grid.getColumns()) result.add(column.getHeader()); }
		return result;
	}

	public static class Region {
		private final String type;
		private String text = "";
		private String title = "";
		private String align = "";
		private int columnsPerRow;
		private String side = "";
		private boolean inTab;
		private boolean paging;
		private Boolean excel;
		private final List<Field> fields = new ArrayList<Field>();
		private final List<Column> columns = new ArrayList<Column>();
		private final List<String> buttons = new ArrayList<String>();
		private final List<String> tabs = new ArrayList<String>();

		public Region(String type) { this.type = type; }
		public String getType() { return type; }
		public String getText() { return text; }
		public void setText(String text) { this.text = text == null ? "" : text; }
		public String getTitle() { return title; }
		public void setTitle(String title) { this.title = title == null ? "" : title; }
		public String getAlign() { return align; }
		public void setAlign(String align) { this.align = align == null ? "" : align; }
		public int getColumnsPerRow() { return columnsPerRow; }
		public void setColumnsPerRow(int columnsPerRow) { this.columnsPerRow = columnsPerRow; }
		/** {@link UiIr#LEFT}, {@link UiIr#RIGHT} or "" for full width. */
		public String getSide() { return side; }
		public void setSide(String side) { this.side = side == null ? "" : side; }
		/** Drawn inside the panel of the selected tab of the closest preceding tabs region. */
		public boolean isInTab() { return inTab; }
		public void setInTab(boolean inTab) { this.inTab = inTab; }
		/** Grid only: a page-number bar (pageindexer) is drawn under the table. */
		public boolean isPaging() { return paging; }
		public void setPaging(boolean paging) { this.paging = paging; }
		/** Grid only: an Excel download button is drawn on the title row (udcComGridTitle showExportExcel); null = unknown. */
		public Boolean getExcel() { return excel; }
		public void setExcel(Boolean excel) { this.excel = excel; }
		public List<Field> getFields() { return fields; }
		public List<Column> getColumns() { return columns; }
		public List<String> getButtons() { return buttons; }
		public List<String> getTabs() { return tabs; }
	}

	public static class Field {
		private final String label;
		private final String component;
		private final boolean required;
		private final String value;
		/** Captions of the choices of a radiobutton / checkboxgroup, left to right. */
		private final List<String> options = new ArrayList<String>();
		public Field(String label, String component, boolean required) { this(label, component, required, ""); }
		public List<String> getOptions() { return options; }
		public Field(String label, String component, boolean required, String value) { this.label = label; this.component = component; this.required = required; this.value = value == null ? "" : value; }
		public String getLabel() { return label; }
		public String getComponent() { return component; }
		public boolean isRequired() { return required; }
		public String getValue() { return value; }
	}

	public static class Column {
		private final String header;
		private final String editor;
		private final int width;
		private final String cellText;
		public Column(String header, String editor, int width, String cellText) { this.header = header; this.editor = editor; this.width = width; this.cellText = cellText == null ? "" : cellText; }
		public String getHeader() { return header; }
		public String getEditor() { return editor; }
		/** Width in source-image pixels, or 0 when unknown. */
		public int getWidth() { return width; }
		public String getCellText() { return cellText; }
	}
}
