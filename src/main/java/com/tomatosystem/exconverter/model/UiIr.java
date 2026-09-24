package com.tomatosystem.exconverter.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Framework-independent intermediate representation used between AI/OCR and CLX.
 * Regions are kept in visual (top-to-bottom) order; the CLX compiler decides
 * which eXBuilder6 container (content-header/body/footer) receives each one.
 *
 * <p>v2.2 (Converter-Figma): optional backend binding. A region may carry the id of the DataSet/DataMap it is bound
 * to ({@link Region#getDataId()}), fields/columns may carry the DTO property they map to ({@link Field#getName()},
 * {@link Column#getName()}), and the screen may list the submissions to generate ({@link #getSubmissions()}).
 * The generator records the event handlers it wired ({@link #getHandlers()}) so the companion JS can be written.
 * Everything is optional: without it the CLX is exactly the v2.0/v2.1 result.
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
	/** Backend calls to generate (cl:submission), filled by the OpenAPI binder; empty = no backend binding. */
	private final List<Submission> submissions = new ArrayList<Submission>();
	/** Event handlers the CLX generator wired (listeners), consumed by the companion JS generator. */
	private final List<Handler> handlers = new ArrayList<Handler>();
	/** Where the submissions came from (OpenAPI title / URL), for comments in the generated JS. */
	private String apiSource = "";

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
	public List<Submission> getSubmissions() { return submissions; }
	public List<Handler> getHandlers() { return handlers; }
	public String getApiSource() { return apiSource; }
	public void setApiSource(String apiSource) { this.apiSource = apiSource == null ? "" : apiSource; }

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

	/** The submission a button caption triggers, or null. */
	public Submission submissionFor(String caption) {
		String wanted = caption == null ? "" : caption.replace(" ", "");
		if (wanted.isEmpty()) return null;
		for (Submission submission : submissions) { if (wanted.equals(submission.getCaption().replace(" ", ""))) return submission; }
		return null;
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
		private String dataId = "";
		private String controlId = "";
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
		/** Grid: the DataSet id; search/form: the DataMap id the fields are bound to. "" = generator default (grid) / unbound (fields). */
		public String getDataId() { return dataId; }
		public void setDataId(String dataId) { this.dataId = dataId == null ? "" : dataId.trim(); }
		/** Id of the CLX control the generator built for this region (grid id); set during generation, not part of the JSON. */
		public String getControlId() { return controlId; }
		public void setControlId(String controlId) { this.controlId = controlId == null ? "" : controlId; }
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
		private String name = "";
		/** Captions of the choices of a radiobutton / checkboxgroup, left to right. */
		private final List<String> options = new ArrayList<String>();
		public Field(String label, String component, boolean required) { this(label, component, required, ""); }
		public List<String> getOptions() { return options; }
		public Field(String label, String component, boolean required, String value) { this.label = label; this.component = component; this.required = required; this.value = value == null ? "" : value; }
		public String getLabel() { return label; }
		public String getComponent() { return component; }
		public boolean isRequired() { return required; }
		public String getValue() { return value; }
		/** DataMap column (DTO property) bound to the control; a daterange carries "from,to". "" = not bound. */
		public String getName() { return name; }
		public void setName(String name) { this.name = name == null ? "" : name.trim(); }
	}

	public static class Column {
		private final String header;
		private final String editor;
		private final int width;
		private final String cellText;
		private String name = "";
		private String dataType = "";
		public Column(String header, String editor, int width, String cellText) { this.header = header; this.editor = editor; this.width = width; this.cellText = cellText == null ? "" : cellText; }
		public String getHeader() { return header; }
		public String getEditor() { return editor; }
		/** Width in source-image pixels, or 0 when unknown. */
		public int getWidth() { return width; }
		public String getCellText() { return cellText; }
		/** Explicit DataSet column name (DTO property); "" = derived from cellText / dictionary / COLn. */
		public String getName() { return name; }
		public void setName(String name) { this.name = name == null ? "" : name.trim(); }
		/** datacolumn datatype ("number", "date" …) when the API says so; "" = default. */
		public String getDataType() { return dataType; }
		public void setDataType(String dataType) { this.dataType = dataType == null ? "" : dataType.trim(); }
	}

	/** One backend call: cl:submission with its request/response data controls and the button that triggers it. */
	public static class Submission {
		public static final String ROLE_SEARCH = "search";
		public static final String ROLE_DETAIL = "detail";
		public static final String ROLE_SAVE = "save";
		public static final String ROLE_DELETE = "delete";

		private final String id;
		private String action = "";
		private String method = "get";
		private String role = "";
		private String caption = "";
		private String requestDataId = "";
		private String responseDataId = "";
		private String responsePath = "";
		private String operationId = "";
		private String summary = "";

		public Submission(String id) { this.id = id; }
		public String getId() { return id; }
		public String getAction() { return action; }
		public void setAction(String action) { this.action = action == null ? "" : action.trim(); }
		public String getMethod() { return method; }
		public void setMethod(String method) { this.method = method == null || method.trim().isEmpty() ? "get" : method.trim().toLowerCase(java.util.Locale.ROOT); }
		/** search | detail | save | delete | "" (custom). Decides the JS skeleton and the default handler names. */
		public String getRole() { return role; }
		public void setRole(String role) { this.role = role == null ? "" : role.trim(); }
		/** Caption of the design button that sends it ("조회", "저장"); "" when nothing on the screen triggers it. */
		public String getCaption() { return caption; }
		public void setCaption(String caption) { this.caption = caption == null ? "" : caption.trim(); }
		public String getRequestDataId() { return requestDataId; }
		public void setRequestDataId(String requestDataId) { this.requestDataId = requestDataId == null ? "" : requestDataId.trim(); }
		public String getResponseDataId() { return responseDataId; }
		public void setResponseDataId(String responseDataId) { this.responseDataId = responseDataId == null ? "" : responseDataId.trim(); }
		/** Dotted path of the row array inside a REST envelope ("data.list"); "" when the body is the array/object itself. */
		public String getResponsePath() { return responsePath; }
		public void setResponsePath(String responsePath) { this.responsePath = responsePath == null ? "" : responsePath.trim(); }
		public String getOperationId() { return operationId; }
		public void setOperationId(String operationId) { this.operationId = operationId == null ? "" : operationId.trim(); }
		public String getSummary() { return summary; }
		public void setSummary(String summary) { this.summary = summary == null ? "" : summary.trim(); }
	}

	/** A listener the generator put into the CLX; the companion JS must define the function. */
	public static class Handler {
		private final String function;
		private final String event;
		private final String controlId;
		private final String caption;
		private final String submissionId;
		private final String role;
		public Handler(String function, String event, String controlId, String caption, String submissionId, String role) {
			this.function = function; this.event = event; this.controlId = controlId == null ? "" : controlId;
			this.caption = caption == null ? "" : caption; this.submissionId = submissionId == null ? "" : submissionId; this.role = role == null ? "" : role;
		}
		public String getFunction() { return function; }
		/** click | load | submit-done | selection-change */
		public String getEvent() { return event; }
		public String getControlId() { return controlId; }
		public String getCaption() { return caption; }
		public String getSubmissionId() { return submissionId; }
		public String getRole() { return role; }
	}
}
