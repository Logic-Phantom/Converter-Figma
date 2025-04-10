/************************************************
 * responsiveGrid.module.js
 *
 * @author tomatosystem
 ************************************************/

/*************************************************************************
 * 확인사항
 * 1. RGrid.prototype.ATTR_NM에 구성된 속성중 필요한 기능을 확인하고
 *    프로젝트 표준으로 그리드 컨트롤에 해당 속성명과 동일하게 사용자 속성을 구성하여 적용합니다.
 *    (eXBuilder6 > 프로젝트 표준 > 사용자 속성 정의)
 * 
 * 2. 반응형 모듈 적용은 event.module.js내 init EventBus에서 처리되며
 *    사용자 속성 "transform-on-mobile" 값 존재여부에 따라 적용여부를 판단 합니다.
 * 
 * 3. 반응형 그리드 모듈은 모바일 환경에서 표시할 cell Index를 사용자 속성 "view-column-indicies" 값에 구성하며 
 *    해당 인덱스 값에 의한 셀은 화면에 표시하고 나머지 셀은 아래 두가지 타입으로 표현될 수 있도록 적용합니다.
 *    프로젝트에 적합한 형식의 타입을 활용하시기 바랍니다.
 *    1) accordionType : 그리드를 폼 레이아웃으로 구성하여 아코디언과 유사하게 로우 클릭시 상세 영역 데이터를 접었다 펼치는 방식으로 표현
 *    2) dialogType : 상세 버튼을 추가하여 해당 버튼 클릭시 다이얼로그 팝업으로 상세 영역 데이터 표현
 *                    - app/com/comPGridCellView 팝업 구성
 * 
  *       ※ theme > custom > responsive.part.less 스타일 구성
  *       ※ 예제 제공 목적으로 두가지 타입의 샘플을 구성하기 위해 "transform-on-mobile" 값에 의해 타입이 정해지도록 구성되어 있습니다.
  *    참고 - RGrid.prototype.start
 *************************************************************************/

/************************************************
 * 반응형 그리드 모듈 옵션
 ************************************************/

/* 공통 옵션 */
/**
 * 반응형 그리드 타입
 * @type {"accordionType" | "dialogType" | "list"}
 */
var msRGridType = "accordionType";

/* expandType 옵션 */
/**
 * 행 높이 자동조절
 * @type {Boolean}
 */
var mbAutoRowHeight = false;

/**
 * 독점적 아코디언 사용 여부
 * (다른 행을 클릭하면 이미 열렸던 행을 접는 기능)
 * @type {Boolean}
 */
var mbExclusive = false;

/**
 * 아코디언 영역 내 여백
 * @type {Number}
 */
var mnInnerMargin = 10;

/**
 * 디테일 영역을 접거나 펼칠 수 있는 영역을 추가합니다.
 * (해당 값이 false 면 행을 클릭했을 때 접거나 펼쳐집니다.)
 * @type {Boolean}
 */
var mbAddExpander = false;

/**
 * 디테일행 익스펜더 버튼 클래스명
 * @type {String}
 */
var msExpanderClassName = "btn-expander";

/**
 * 그리드 형태 폼 클래스명
 * @type {String}
 */
var msGridClassName = "table";

/**
 * 그리드 형태 폼 내 헤더 클래스명
 * @type {String}
 */
var msGridHeaderClassName = "table-header";

/**
 * 그리드 형태 폼 내 헤더 셀 클래스명
 * @type {String}
 */
var msGridHeaderCellClassName = "table-header-cell";

/**
 * 그리드 형태 폼 내 디테일 클래스명
 * @type {String}
 */
var msGridRowClassName = "table-row";

/**
 * 그리드 형태 폼 내 디테일 셀 클래스명
 * @type {String}
 */
var msGridRowCellClassName = "table-cell";

/**
 * 그리드 형태 폼 내 아코디언 디테일 셀 클래스명
 * @type {String}
 */
var msGridAccordionRowClassName = "table-accordion-row";

/* dialogType 옵션 */
/**
 * 상세 버튼 헤더 컬럼 텍스트
 * @type {String}
 */
var msDetailHdrText = "상세";

/**
 * 상세 버튼 클래스명
 * @type {String}
 */
var msDetailBtnClassName = "btn-modile-detail";

/**
 * 반응형 그리드 유틸리티.
 * @param {cpr.controls.Grid} grid
 */
function RGrid(grid) {
	/** @type cpr.controls.Grid */
	this._grid = grid;
	/** @type cpr.core.AppInstance */
	this._app = grid.getAppInstance();
	/** @type cpr.controls.Container */
	this._parentContainer = this._grid.getParent();
	
	this._container = null;
	this._innerContainer = null;
	this._columnSettings = [];
	this._onScreenChange = this._onScreenChange.bind(this);
	this._started = false;
	this._hideCellIdxs = [];	
	/** @type {
	* 	  default : String[],
	* 	  tablet : String[],
	* 	  mobile : String[]
	* 	} 
	**/
	this._screenNms = this._setScreenNm();
	
	/** @type {"accordionType" | "dialogType" | "list"} */
	this._gridType = msRGridType;
	this._ctrlCopy = createCtrlCopyModule();
	
	this._app.addEventListener("screen-change", this._onScreenChange);
	grid.addEventListener("dispose", this._handleDispose.bind(this));
	
	// 모바일 화면에서 데이터셋 값이 변경됨을 적용하기 위한 이벤트 핸들러(load, filter)
	function _datasetEventHandler(e) {
		this._started = false;
		this.restore();
		this._revert();
		
		this._app.dispatchEvent(new cpr.events.CScreenChangeEvent({
			name: this._app.targetScreen.name
		}))
	}
	var vcBindDataset = grid.dataSet;
	vcBindDataset.addEventListener("load", _datasetEventHandler.bind(this));
	vcBindDataset.addEventListener("filter", _datasetEventHandler.bind(this));
	vcBindDataset.addEventListener("update", (function (e){
		// container 가 dispose 되면서 그리드 내 컨트롤 이벤트가 발생하지 않는 문제 수정
		if(this._container) {
			this._container.redraw();
		}
	}).bind(this));
	
	/*
	 * TODO 데이터셋의 insert, delete 이벤트 발생 시, 반응형 폼에 변경사항을 적용하기 위해서 아래 주석을 해제하십시오.
	 */
//	vcBindDataset.addEventListener("insert", _datasetEventHandler.bind(this));
//	vcBindDataset.addEventListener("delete", _datasetEventHandler.bind(this));
}

/**
 * 반응형 그리드 옵션 속성명<br>
 * (사용자 속성값의 기본타입은 String이며 valueType은 데이터 입력형식 확인을 위해 작성되었습니다.)
 */
RGrid.prototype.ATTR_NM = {
	/** <필수>모바일 : 모바일 환경에서 반응형 그리드 적용 여부<br>(valueType : String, ex. accordionType)*/
	ATTR_TRANSFORM_ON_MOBILE : "transform-on-mobile",
	/** 모바일 : 반응형 그리드 적용시 표시할 컬럼 인덱스(colIndex) <br>(valueType : Number[], ex. 1,3,4)*/
	ATTR_VIEW_COLUMN_INDICES : "view-column-indicies",
	/** 선택행 컨텍스트 연결된 그룹 ID<br>(valueType : String, ex. grpForm)*/
	ATTR_BIND_FORM_ID : "bindDataFormId",
	
	/** 모바일 : 각 행 별 spacing<br>transform-on-mobile: list 일 경우에만 적용<br>(valueType : Number, ex.10)*/
	ATTR_LIST_SPACING : "grid-list-spacing",
	/** 모바일 : 각 행 별 top margin<br>transform-on-mobile: list 일 경우에만 적용<br>(valueType : Number, ex.10)*/
	ATTR_LIST_TOP_MARGIN : "grid-list-top-margin",
	/** 모바일 : 각 행 별 left margin<br>transform-on-mobile: list 일 경우에만 적용<br>(valueType : Number, ex.10)*/
	ATTR_LIST_LEFT_MARGIN : "grid-list-left-margin",
	/** 모바일 : 각 행 별 right margin<br>transform-on-mobile: list 일 경우에만 적용<br>(valueType : Number, ex.10)*/
	ATTR_LIST_RIGHT_MARGIN : "grid-list-right-margin",
	/** 모바일 : 각 행 별 bottom margin<br>transform-on-mobile: list 일 경우에만 적용<br>(valueType : Number, ex.10)*/
	ATTR_LIST_BOTTOM_MARGIN : "grid-list-bottom-margin",
	/** 모바일 : 폼 내 각 Row의 높이<br>transform-on-mobile: list 일 경우에만 적용<br>(valueType : Number, ex.30)*/
	ATTR_LIST_ROW_HEIGHT : "grid-list-row-height"
}

/**
 * 타입에 따른 스크린 명칭 구성
 */
RGrid.prototype._setScreenNm = function(){
	var voScreenNms = {
		"default" : ["default"],
		"tablet" : ["tablet"],
		"mobile" : ["mobile"]
	};
	
	if (typeof AppProperties !== 'undefined') {
		if(!ValueUtil.isNull(AppProperties.SCREEN_DEFAULT_NM)) {
			voScreenNms["default"] = AppProperties.SCREEN_DEFAULT_NM;
		}
		if(!ValueUtil.isNull(AppProperties.SCREEN_TABLET_NM)) {
			voScreenNms["tablet"] = AppProperties.SCREEN_TABLET_NM;
		}
		if(!ValueUtil.isNull(AppProperties.SCREEN_MOBILE_NM)) {
			voScreenNms["mobile"] = AppProperties.SCREEN_MOBILE_NM;
		}
	}
				
	return voScreenNms;
}

/**
 * 반응형 그리드 적용
 */
RGrid.prototype.start = function() {
	if (this._started) return;
	
	this._gridType = this._grid.userAttr(this.ATTR_NM.ATTR_TRANSFORM_ON_MOBILE);
	var viewColIndexs = this._grid.userAttr(this.ATTR_NM.ATTR_VIEW_COLUMN_INDICES);
	
	if (viewColIndexs != null && viewColIndexs != '') {
		this._hide();
	}
	
	if (this._gridType == "dialogType") {
		this._setViewBtnColumn();
	} else if(this._gridType == "accordionType"){
		this._setColumnSettings();
		this._collapse();
	} else if(this._gridType == "list") {
		this._setColumnSettings();
		this._setListForm();
	}
	
	this._parentContainer.updateConstraint(this._grid, {
		"autoSize": "height"
	});
	
	var grpBindForm = this._app.lookup(this._grid.userAttr(this.ATTR_NM.ATTR_BIND_FORM_ID));
	if (grpBindForm){
		grpBindForm.visible = false;
	}
	
	this._started = true;
}

/**
 * 반응형 그리드 해제
 */
RGrid.prototype.stop = function() {
	if (!this._started) {
		return;
	}
		
	var viewColIndexs = this._grid.userAttr(this.ATTR_NM.ATTR_VIEW_COLUMN_INDICES);
		
	if (viewColIndexs != null && viewColIndexs != '') {
		this.restore();
		this._revert();
	} else if (this._gridType == "dialogType") {
		this.restore();
	} else {
		this.restore();
		this._revert();
	}
	
	this._parentContainer.updateConstraint(this._grid, {
		"autoSize": "none"
	});
	
	var grpBindForm = this._app.lookup(this._grid.userAttr(this.ATTR_NM.ATTR_BIND_FORM_ID));
	if (grpBindForm){
		grpBindForm.visible = true;
	}
	
	this._started = false;
}

/**
 * 스크린 사이즈가 변경됐을 때 발생하는 이벤트
 * @param {cpr.events.CScreenChangeEvent} e
 */
RGrid.prototype._onScreenChange = function(e) {
	var vsScrnName = e.screen.name;
	
	if(this._screenNms["mobile"].indexOf(vsScrnName) != -1) {
		this.start();
	} else if(this._screenNms["tablet"].indexOf(vsScrnName) != -1) {
		this.stop();
	} else {
		this.stop();
	} 
}

/**
 * 그리드가 dispose될 때 발생하는 이벤트
 * @param {cpr.events.CEvent} e
 */
RGrid.prototype._handleDispose = function(e) {
	this._app.removeEventListener("screen-change", this._onScreenChange);
}

/**
 * 그리드 컬럼 상태를 재정의 합니다.
 */
RGrid.prototype._hide = function() {
	var indicies = this._grid.userAttr(this.ATTR_NM.ATTR_VIEW_COLUMN_INDICES).split(/[\s,]+/g).map(function( /* String */ each) {
		return parseInt(each);
	});
	
	var grid = this._grid;
	var header = grid.header;
	var detail = grid.detail;
	var dCellIndicies = detail.getCellIndices();
	var hideCellIndicies = [];
	
	var colIndicesByVisible = grid.getColIndicesByVisible(true);
	for(var idx = 0; idx < grid.columnCount; idx++){
		if(indicies.indexOf(idx) == -1) {
			if(colIndicesByVisible.indexOf(idx) != -1) {
				hideCellIndicies.push(idx);
				grid.columnVisible(idx, false, "column-index");
			}
		}
	}

	this._hideCellIdxs = hideCellIndicies;
	
}


/**
 * 그리드 컬럼 숨김 상태를 원복합니다.
 */
RGrid.prototype._revert = function() {
	var grid = this._grid;
	var hideCellIdxs = this._hideCellIdxs;
	if(hideCellIdxs){
		hideCellIdxs.forEach(function( /* Number */ each) {
			grid.columnVisible(each, true, "column-index");
		});
	}
}

/**
 * 그리드 상태를 원복합니다.
 */
RGrid.prototype.restore = function() {
	this._columnSettings = [];
	var vcGrd = this._grid;
	var vaHedaerCol = vcGrd.header.getColumnByColIndex(0, 1);
	
	if (vaHedaerCol.length > 0 && vaHedaerCol[0].text == msDetailHdrText) {
		var cellIdx = vaHedaerCol[0].cellIndex;
		vcGrd.deleteColumn(cellIdx);
	} else {
		this._columnSettings = [];
		this._grid.visible = true;
		if (this._container) {
			this._container.dispose();
		}
	}
}


RGrid.prototype._columnSettings = [];

/**
 * 그리드를 폼 형태로 그리기 위한 데이터를 정의합니다.
 */
RGrid.prototype._setColumnSettings = function() {
	var grid = this._grid;
	
	var header = grid.header;
	var detail = grid.detail;
	var hdrColLot = grid.getColumnLayout().header;
	var autoFit = grid.autoFit.replace(/\s/g, "");
	var autoFitCols = autoFit == "all" ? [] : autoFit.split(",");
	
	var mergeHdrCols = hdrColLot.filter(function(each) {
		return each.colSpan > 1;
	});
	
	var displayCols = [];
	var isBeforeDpCol = false;
	var beforeDpColInfo = {};
	
	for (var idx = 0; idx < detail.cellCount; idx++) {
		var colSpan = 1;
		var hdrColIdx = idx;
		var displayText = "";
		var displayLoca = "";
		
		if (displayCols.indexOf(idx) != -1) continue;
		
		if (mergeHdrCols.length > 0) {
			mergeHdrCols.forEach(function(each) {
				if (each.colIndex == idx) {
					colSpan = each.colSpan;
					for (var j = idx; j < idx + colSpan; j++) {
						var dtrlCol = detail.getColumnByColIndex(j, 1)[0];
						if (!dtrlCol.columnName) {
							displayCols.push(j);
							if (dtrlCol.control instanceof cpr.controls.Output) {
								displayText = dtrlCol.control.value;
								if (idx < j) {
									displayLoca = "after";
								} else {
									beforeDpColInfo["text"] = displayText;
									beforeDpColInfo["loca"] = "before";
									beforeDpColInfo["colIdx"] = j + 1;
									beforeDpColInfo["colSpan"] = colSpan;
									isBeforeDpCol = true;
								}
							}
						}
					}
				}
			});
		}
		
		if (isBeforeDpCol) {
			isBeforeDpCol = false;
			continue;
		}
		
		if (beforeDpColInfo["colIdx"] == idx) {
			hdrColIdx = idx - 1;
			colSpan = beforeDpColInfo["colSpan"];
			displayText = beforeDpColInfo["text"];
			displayLoca = beforeDpColInfo["loca"];
			beforeDpColInfo = {};
		}
		
		var headerColumn = header.getColumnByColIndex(hdrColIdx, colSpan)[0];
		var detailColumn = detail.getColumnByColIndex(idx, 1)[0];
		
		if (!detailColumn || detailColumn.columnType == "checkbox" || detailColumn.columnType == "radio") continue;
		var subText = null;
		if (headerColumn.rowIndex > 0) {
			var headerCellIndcs = grid.getHeaderCellIndices(detailColumn.cellIndex);
			var headerTxts = [];
			for (var cell = 0; cell < headerCellIndcs.length - 1; cell++) {
				var text = grid.header.getColumn(headerCellIndcs[cell]).text;
				headerTxts.push(text);
			}
			subText = headerTxts.join("-");
		}
		
		// AutoFit이 해제된 컬럼인 경우 고정 값으로 생성
		var width = "1fr";
		if (autoFitCols.length && autoFitCols.indexOf(headerColumn.colIndex.toString()) == -1) {
			width = grid.getColumnWidths()[detailColumn.colIndex];
		}
		
		var column = {
			text: headerColumn.text,
			visible: headerColumn.visible,
			columnName: detailColumn.columnName,
			columnType: detailColumn.columnType,
			control: detailColumn.control,
			subText: subText,
			displayText: displayText,
			displayLoca: displayLoca,
			width: width
		}
		
		this._columnSettings.push(column);
	}
}

/**
 * 그리드 일부 컬럼을 숨기고 그리드를 폼 형태로 동적 생성합니다.
 */
RGrid.prototype._collapse = function() {
	var grid = this._grid;
	grid.visible = false;
	
	this._container = new cpr.controls.Container();
	var layout = new cpr.controls.layouts.FormLayout();
	this._container.setLayout(layout);
	
	var height = grid.header.getRowHeight(0);
	layout.setRows([height + "px", "1fr"]);
	
	/** @type Object[] */
	var visibleColumns = this._columnSettings.filter(function(each) {
		return each.visible == true;
	});
	
	var colCount = visibleColumns.map(function(each) {
		return each.width;
	});
	
	if (mbAddExpander) {
		colCount.splice(0, 0, "30px");
	}
	layout.setColumns(colCount);
	
	if (mbAutoRowHeight) {
		layout.setRowAutoSizing(0, true);
	}
	
	layout.horizontalSpacing = "0px";
	layout.verticalSpacing = "0px";
	layout.horizontalSeparatorWidth = 1;
	layout.verticalSeparatorWidth = 1;
	layout.setUseRowShade(0, true);
	layout.columnShadeClass = msGridHeaderClassName;
	this._container.style.addClass(msGridClassName);
	
	// 헤더셀 추가
	for (var idx = 0; idx < visibleColumns.length; idx++) {
		var headerColumn = visibleColumns[idx];
		var headerText = headerColumn.text;
		
		// 인덱스 컬럼의 경우 헤더컬럼명 치환동작 추가
		if(headerColumn.columnType == "rowindex"){
			if(AppProperties && headerText != AppProperties.GRID_INDEX_COL_HEADER_TEXT){
				headerText = AppProperties.GRID_INDEX_COL_HEADER_TEXT;
			}
		}
		
		var headerCell = new cpr.controls.Output();
		headerCell.value = headerText;
		headerCell.style.addClass(msGridHeaderCellClassName);
		
		var colIndex = idx;
		if (mbAddExpander) {
			colIndex += 1;
		}
		this._container.addChild(headerCell, {
			colIndex: colIndex,
			rowIndex: 0
		});
	}
	
	var colSpan = !mbAddExpander ? visibleColumns.length : visibleColumns.length + 1;
	
	// 디테일 영역 추가
	this._innerContainer = new cpr.controls.Container();
	this._setDetailCells(); // 디테일 내부 동적 생성
	this._container.addChild(this._innerContainer, {
		colIndex: 0,
		rowIndex: 1,
		colSpan: colSpan
	});
	
	var that = this;
	// grid cell-click 이벤트 전파
	this._container.addEventListener("click", function(e){
		
		var vcTargetCtrl = e.targetControl;
		if(vcTargetCtrl.style.hasClass("label") 
		|| (vcTargetCtrl instanceof cpr.controls.Container)
		|| (vcTargetCtrl instanceof cpr.controls.Button && vcTargetCtrl.style.hasClass(msExpanderClassName))) return; // 헤더 셀 리턴

		var vnTargetRowIndex = vcTargetCtrl.getBindContext().rowIndex;
		var vsTargetColumnName = vcTargetCtrl.getBindInfo("value").columnName;
		var vsTargetCellIndex = grid.detail.getColumnByName(vsTargetColumnName)[0].cellIndex;
		
		var option = {
			relativeTargetName: "detail",
			row: grid.getRow(vnTargetRowIndex),
			rowIndex: vnTargetRowIndex,
			cellIndex: vsTargetCellIndex,
			columnName: vsTargetColumnName,
			cellBoundingRect: grid.getCellBounds("detail", vnTargetRowIndex, vsTargetCellIndex)
		}
		var cellClickEvt = new cpr.events.CGridMouseEvent(cpr.events.GridEventType.CELL_CLICK, option);
		grid.dispatchEvent(cellClickEvt);
	});
	
	var targetIndex = this._parentContainer.getChildren().indexOf(grid) + 1;
	this._parentContainer.insertChild(targetIndex, this._container, {
		autoSize: "height"
	});
}

/**
 * 그리드 디테일 영역을 생성합니다.
 */
RGrid.prototype._setDetailCells = function() {
	var grid = this._grid;
	var layout = new cpr.controls.layouts.VerticalLayout();
	this._innerContainer.setLayout(layout);
	
	layout.spacing = 0;
	var visibleColumn = this._columnSettings.filter(function(each) {
		return each.visible == true;
	});
	
	// 디테일 행 추가
	var height = this._container.getLayout().getRows()[0];
	var rowCount = this._grid.getRowCount();
	for (var row = 0; row < rowCount; row++) {
		var tableRow = new cpr.controls.Container();
		var tableRowLayout = new cpr.controls.layouts.FormLayout();
		tableRow.setLayout(tableRowLayout);
		
		tableRowLayout.setColumns(this._container.getLayout().getColumns());
		tableRowLayout.setRows([height]);
		tableRowLayout.verticalSeparatorWidth = 1;
		tableRowLayout.horizontalSpacing = "0px";
		tableRowLayout.verticalSpacing = "0px";
		
		tableRow.style.addClass(msGridRowClassName);
		tableRow.setBindContext(new cpr.bind.DataRowContext(this._grid.dataSet, row));
		
		var detailCellCnt = tableRowLayout.getColumns().length;
		
		if (!mbAddExpander) {
			tableRow.addEventListener("click", this._onClick);
		} else {
			detailCellCnt -= 1;
			
			var expander = new cpr.controls.Button();
			expander.style.setClasses(msExpanderClassName);
			expander.addEventListener("click", this._onClick);
			tableRow.addChild(expander, {
				colIndex: 0,
				rowIndex: 0
			});
		}
		
		if (mbAutoRowHeight) {
			tableRowLayout.setRowAutoSizing(row, true);
		}
		
		// 디테일 셀 컨트롤 추가
		for (var col = 0; col < detailCellCnt; col++) {
			var colIndex = mbAddExpander ? col + 1 : col;
			var column = visibleColumn[col];
			
			var child = null;
			if(!ValueUtil.isNull(column.control)) {
				child = this._ctrlCopy.copy(column.control);
			} else {
				child =  this._copyControl(column);
			}
			tableRow.addChild(child, {
				colIndex: colIndex,
				rowIndex: 0
			});
		}
		
		this._innerContainer.addChild(tableRow, {
			autoSize: "height"
		});
	}
	
	var vaHideCellIdxs = this._hideCellIdxs;
	// 디테일 아코디언 행 추가
	var hideColumns = this._columnSettings.filter(function(each) {
		// 반응형 동작을 위해 구성된 표시항목 외 visible = false 인컬럼 숨김
		var vnCellIdx = grid.getCellIndex(each["columnName"]);
		return !each.visible && vaHideCellIdxs.indexOf(vnCellIdx) != -1;
	});
	
	var accordionCols = [];
	for (var idx = 0; idx < hideColumns.length; idx++) {
		accordionCols.push(height);
	}
	var childCount = this._innerContainer.getChildrenCount();
	
	for (var aRow = 0; aRow < childCount; aRow++) {
		var accordion = new cpr.controls.Container();
		var accordionLayout = new cpr.controls.layouts.FormLayout();
		accordion.setLayout(accordionLayout);
		
		accordionLayout.topMargin = mnInnerMargin;
		accordionLayout.rightMargin = mnInnerMargin;
		accordionLayout.bottomMargin = mnInnerMargin;
		accordionLayout.leftMargin = mnInnerMargin;
		
		accordionLayout.setRows(accordionCols);
		accordionLayout.setColumns(["1px", "1fr"]);
		accordionLayout.setColumnAutoSizing(0, true);
		accordion.style.addClass(msGridAccordionRowClassName);
		accordion.visible = false;
		
		accordion.setBindContext(new cpr.bind.DataRowContext(this._grid.dataSet, aRow));
		
		if (mbAutoRowHeight) {
			for (var idx = 0; idx < accordionLayout.getRows().length; idx++) {
				accordionLayout.setRowAutoSizing(idx, true);
			}
		}
		
		for (var innrRow = 0; innrRow < hideColumns.length; innrRow++) {
			var innrColumn = hideColumns[innrRow];
			var innerText = innrColumn.text;
			
			// 인덱스 컬럼의 경우 헤더컬럼명 치환동작 추가
			if(innrColumn.columnType == "rowindex"){
				if(AppProperties && innerText != AppProperties.GRID_INDEX_COL_HEADER_TEXT){
					innerText = AppProperties.GRID_INDEX_COL_HEADER_TEXT;
				}
			}
		
			var label = new cpr.controls.Output();
			label.value = innerText;
			label.style.addClass("label");
			label.style.css("text-align", "right");
			accordion.addChild(label, {
				colIndex: 0,
				rowIndex: innrRow
			});
			
			var accordionChild = null;
			if(!ValueUtil.isNull(innrColumn.control)) {
				accordionChild = this._ctrlCopy.copy(innrColumn.control);
			} else {
				accordionChild =  this._copyControl(innrColumn);
				accordionChild.style.css("text-align", "right");
			}
			accordion.addChild(accordionChild, {
				colIndex: 1,
				rowIndex: innrRow
			});
		}
		
		this._innerContainer.insertChild((aRow * 2) + 1, accordion, {
			autoSize: "height"
		});
	}
}

/**
 * 그리드 내 컨트롤을 복사합니다.
 * @param {control:cpr.controls.UIControl,columnName:String,visible:Boolean} column
 */
RGrid.prototype._copyControl = function(column) {
	
	var control = column["control"];
	var copyControl = null;
	if (control instanceof cpr.controls.Output) {
		copyControl = new cpr.controls.Output();
		copyControl.dataType = control.dataType;
		copyControl.format = control.format;
		copyControl.displayExp = control.displayExp;
	} else if (control instanceof cpr.controls.CheckBox) {
		copyControl = new cpr.controls.CheckBox();
		copyControl.trueValue = control.trueValue;
		copyControl.falseValue = control.falseValue;
	} else if (control instanceof cpr.controls.ComboBox) {
		copyControl = new cpr.controls.ComboBox();
		copyControl.setItemSet(control.dataSet, control.itemSetConfig);
		copyControl.hideButton = true;
	} else {
		control = new cpr.controls.Output();
		copyControl = new cpr.controls.Output();
	}
	
	/* 추가적인 컨트롤이 있을 시 컨트롤에 대하여 복사할 속성을 작성하십시오.*/
	
	if(column.columnType == "rowindex"){
		copyControl.bind("value").toExpression("getIndex() + 1");
	} else {
		copyControl.bind("value").toDataColumn(column["columnName"]);
	}
	
	// 디테일 셀 공통
	copyControl.fieldLabel = control.fieldLabel;
	
	copyControl.style.setClasses(control.style.getClasses());
	
	if (column.displayText) {
		if (column.displayLoca == "after") {
			copyControl.displayExp = "text +' " + column.displayText + "'";
		} else {
			copyControl.displayExp = "'" + column.displayText + " ' + text";
		}
	}
	
	return copyControl;
}

/**
 * 디테일행을 접거나 펼칩니다.
 * @param {cpr.events.CMouseEvent} e
 */
RGrid.prototype._onClick = function(e) {
	/** @type cpr.controls.UIControl */
	var control = e.control;
	
	var parent = control.getParent();
	if (mbAddExpander) {
		control = parent;
		parent = parent.getParent();
	}
	/** @type cpr.controls.Container */
	var nextControl = parent.getChildren()[parent.getChildren().indexOf(control) + 1];
	nextControl.visible = !nextControl.visible;
	
	if (mbExclusive) {
		parent.getChildren().filter(function(each) {
			return each.style.hasClass("selected");
		}).forEach(function(each) {
			var eachParent = each.getParent();
			var nextCtrl = eachParent.getChildren()[eachParent.getChildren().indexOf(each) + 1];
			nextCtrl.visible = false;
			each.style.removeClass("selected");
		});
	}
	
	if (nextControl.visible) {
		control.style.addClass("selected");
	} else {
		control.style.removeClass("selected");
	}
}
/**
 * 그리드 데이터를 팝업으로 출력하는 상세 버튼을 생성합니다.
 */
RGrid.prototype._setViewBtnColumn = function() {
	
	var vcGrd = this._grid;
	var vaColLot = vcGrd.getColumnLayout().header;
	var vnHdrRowCnt = vcGrd.header.getRowHeights().length;
	var vaHedaerCol = vcGrd.header.getColumnByColIndex(0, 1);
	
	if (vaHedaerCol.length > 0 && vaHedaerCol[0].text == msDetailHdrText) return;
	
	var vsAttrBindFormId = this.ATTR_NM.ATTR_BIND_FORM_ID;
	vcGrd.insertColumn({
		columnLayout: [{
			width: "30px"
		}],
		header: [{
			constraint: {
				rowIndex: 0,
				colIndex: 0,
				colSpan: 1,
				rowSpan: vnHdrRowCnt
			},
			configurator: function(configurator) {
				configurator.text = msDetailHdrText;
			}
		}],
		detail: [{
			constraint: {
				rowIndex: 0,
				colIndex: 0,
				colSpan: 1,
				rowSpan: vnHdrRowCnt
			},
			configurator: function(cell) {
				cell.control = (function() {
					var btnDetail = new cpr.controls.Button("btnDetail");
					btnDetail.tooltip = "상세 데이터 팝업 버튼";
					btnDetail.fieldLabel = "상세 데이터 팝업 버튼";
					btnDetail.icon = "theme/images/controls/button/ic_btn_pop.svg";
					btnDetail.style.css({
						"padding": "0px",
						"border" : "1px solid #E0E0E0",
						"border-radius" : "4px"
					});
					btnDetail.style.setClasses([msDetailBtnClassName]);
					btnDetail.userAttr("__responseButton__", "true");
					btnDetail.addEventListener("click", function(e) {
						/** @type cpr.controls.Grid */
						var grid = e.control.getParent();
						var rowIndex = grid.getSelectedRowIndex();
						var _app = grid.getAppInstance();
						var grpBindForm = _app.lookup(grid.userAttr(vsAttrBindFormId));
						
						_app.getRootAppInstance().openDialog("app/com/comPGridCellView", {
							top: 0,
							bottom: 0,
							left: 0,
							right: 0,
							headerMovable: false
						}, function(dialog) {
							dialog.userAttr({
								"_originWidth" : 500,
								"_originHeight" : 600
							});
							dialog.headerTitle = grid.fieldLabel;
							dialog.initValue = {
								"grid": grid,
								"grpBindForm": grpBindForm,
								"rowIdx": rowIndex
							};
							
							dialog.addEventListenerOnce("close", function(e){
								var voRowData = e.control.returnValue;
								if(voRowData) {
									grid.getRow(rowIndex).setRowData(voRowData);
									if (grpBindForm) grpBindForm.redraw();
								}
							})
						})
					});					
					return btnDetail;
				})();
				cell.controlConstraint = {};
			}
		}]
	}, 0, false);
	
	var vsAutoFit = vcGrd.autoFit.replace(/\s/g, "");
	if(vsAutoFit == "all"){
		var vaAutoFit = [];
		for(var i=1; i < vcGrd.columnCount; i++){
			vaAutoFit.push(i);
		}
		vsAutoFit = vaAutoFit.join(",");	
		vcGrd.autoFit = vsAutoFit;
	}	
}

/**
 * 
 */
RGrid.prototype._setListForm = function() {
	var that = this;
	
	var grid = this._grid;
	grid.visible = false;
	
	/*
	 * 헤더 행 개수만큼 헤더 셀을 생성할지 여부를 결정합니다. (vbMaintainMultiHeader)
	 * true : 헤더 행 개수만큼 헤더 셀 생성
	 * false : 하나의 셀에 헤더 텍스트 연결
	 */
	var vbMaintainMultiHeader = false;
	var ATTR_GRID_MAINTAIN_MULTI_HEADER = "maintain-multi-header";
	if(ValueUtil.isNull(this._grid.userAttr(ATTR_GRID_MAINTAIN_MULTI_HEADER))) {
		this._grid.userAttr(ATTR_GRID_MAINTAIN_MULTI_HEADER, vbMaintainMultiHeader.toString()); 
	} else {
		vbMaintainMultiHeader = ValueUtil.fixBoolean(this._grid.userAttr(ATTR_GRID_MAINTAIN_MULTI_HEADER));
	}
	
	this._container = new cpr.controls.Container();
	var layout = new cpr.controls.layouts.VerticalLayout();
	layout.spacing = parseInt(grid.userAttr(this.ATTR_NM.ATTR_LIST_SPACING)) || 0;
	this._container.setLayout(layout);
	this._container.style.addClass(msGridClassName);
	
	var rowCount = grid.getRowCount();
	for(var rdx = 0; rdx < rowCount; rdx++){
		var voRow = grid.getRow(rdx);
		var rowStatus = voRow.getStateString();
		if(rowStatus == "D" || rowStatus == "ID") continue;
		
		var vnRowHeight = parseInt(grid.userAttr(this.ATTR_NM.ATTR_LIST_ROW_HEIGHT) || null);
		that._innerContainer = this._ctrlCopy.convertGridToForm(grid, "detail", vnRowHeight, {
			rowIndex: rdx
		});
		
		/** @type cpr.controls.layouts.FormLayout */
		var innerLayout = that._innerContainer.getLayout();
		innerLayout.rowShadeClass = msGridHeaderClassName;
		if(vbMaintainMultiHeader) {
			this._grid.header.getRowHeights().forEach(function(each, idx){
				innerLayout.setUseColumnShade(idx, true);
			});			
		} else {
			innerLayout.setUseColumnShade(0, true);
		}
		
		innerLayout.topMargin = parseInt(grid.userAttr(this.ATTR_NM.ATTR_LIST_TOP_MARGIN)) || 0;
		innerLayout.leftMargin = parseInt(grid.userAttr(this.ATTR_NM.ATTR_LIST_LEFT_MARGIN)) || 0;
		innerLayout.rightMargin = parseInt(grid.userAttr(this.ATTR_NM.ATTR_LIST_RIGHT_MARGIN)) || 0;
		innerLayout.bottomMargin = parseInt(grid.userAttr(this.ATTR_NM.ATTR_LIST_BOTTOM_MARGIN)) || 0;
		
		that._innerContainer.getAllRecursiveChildren(false).forEach(function(each){
			if(!(each instanceof cpr.controls.Container)) {
				// cl-grid-cell 내부에 header, detail 영역
				
				var relativeTarget = "";
				if(each.style.hasClass("cl-grid-header")) {
					each.style.removeClass("cl-grid-header");
					
					relativeTarget = "header";

					// TODO 헤더셀 스타일 적용
					each.style.addClass(msGridHeaderCellClassName);
					
				} else {
					relativeTarget = "detail";
					
					// TODO 디테일셀 스타일 적용을 위해서 아래 주석을 해제하십시오.
//					each.style.addClass(className);
				}
				
				// cell-click 이벤트 전파
				var voConstraint = that._innerContainer.getConstraint(each.getParent());
				var option = {
					relativeTargetName: relativeTarget,
					row: voRow,
					rowIndex: rdx,
					cellIndex: voConstraint.rowIndex,
					columnName: grid.detail.getColumn(voConstraint.rowIndex).columnName,
					cellBoundingRect: grid.getCellBounds(relativeTarget, rdx, voConstraint.rowIndex),
//					_mblRowForm: that._innerContainer
				}
				each.addEventListener("click", function(e){
					var cellClickEvt = new cpr.events.CGridMouseEvent(cpr.events.GridEventType.CELL_CLICK, option);
					grid.dispatchEvent(cellClickEvt);
				});
			} else {
				// TODO cl-grid-cell 영역에 스타일 적용을 위해서 아래 주석을 해제하십시오.
//				each.style.addClass(className);
			}
		});
		
		var vnBorderTop = (rdx == 0) ? "" : "0px";
		var vnBorderBottom = (rdx != rowCount-1) ? "" : "0px";
		that._innerContainer.style.css({
			"min-height" : "0px",
			"border-top" : vnBorderTop,
			"border-bottom" : vnBorderBottom,
		})
		that._container.addChild(that._innerContainer, {
			autoSize: "height"
		})
	}
	
	var targetIndex = this._parentContainer.getChildren().indexOf(grid) + 1;
	this._parentContainer.insertChild(targetIndex, this._container, {
		autoSize: "height"
	});
}

/**
 * 반응형 그리드 적용
 * @param {cpr.controls.Grid} grid
 */
globals.makeResponsiveGrid = function(grid) {
	return new RGrid(grid);
}