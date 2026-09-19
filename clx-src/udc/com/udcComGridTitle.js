//공통 유틸(Util) 클래스
var util = createCommonUtil();

// 클라이언트 엑셀 다운로드
var excelUtil = ExcelClientUtil();

/**
 * 그리드가 팝 아웃 되어있는지 여부.
 */
var mbPopOuted = false;
/**
 * 원래 부모에 속해 있었을 때의 레이아웃 컨스트레인트.
 */
var moOriginalConstraint = null;
/** 
 * 그리드의 원래 부모.
 * @type cpr.controls.Container 
 */
var moOriginalParent = null;
/** 
 * 그리드가 팝아웃 되었을 때 부모역할을 할 래퍼.
 * @type cpr.controls.Container 
 */
var moWrapper = null;

var moHostApp = null;

function onBodyLoad(/* cpr.events.CEvent */ e){
	var vcCtrl = app.getAppProperty("ctrl");
	if(vcCtrl){
		app.lookup("optTilte").value = vcCtrl.fieldLabel;
		if(vcCtrl.type == "grid"){
			if(vcCtrl.dataSet){
				app.setAppProperty("rowCount", vcCtrl.dataSet.getRowCount());
			}else{
				app.setAppProperty("rowCount", "0");
			}
		}
	}
	
	var voResourceLoader = new cpr.core.ResourceLoader();
	voResourceLoader.addScript("./thirdparty/excel/xlsx.min.js");
	voResourceLoader.addScript("./thirdparty/excel/FileSaver.min.js").load();
	//다운로드 권한이 없으면... 엑셀버튼 숨김
	if(util.Main.getMenuInfo(app, "DOWNLD_YN") !== "Y"){
		//app.setAppProperty("showExportExcel", false);
	}
	
	//모바일 화면이면... 엑셀버튼 숨김
//	var mainApp = util.getMainApp(app);
//	if(mainApp){
//		if(mainApp.getContainer().userAttr("adaptive-screen") === "true"){
//			app.setAppProperty("showExportExcel", false);
//			
//			var layout = app.lookup("grp1").getLayout();
//			layout.setColumnVisible(1, false);
//			layout.setColumnVisible(2, false);
//			layout.setColumnVisible(3, false);
//			layout.setColumnVisible(4, false);
//		}
//	}
}

/*
 * "엑셀출력" 버튼에서 click 이벤트 발생 시 호출.
 * 사용자가 컨트롤을 클릭할 때 발생하는 이벤트.
 */
function onBtnExcelExportClick(/* cpr.events.CMouseEvent */ e){
	var vcCtrl = app.getAppProperty("ctrl");
	var exportTitle = !ValueUtil.isNull(app.getAppProperty("exportExcelTitle")) ? app.getAppProperty("exportExcelTitle") : app.lookup("optTilte").value;
	var _app = vcCtrl.getAppInstance();
	util.Grid.exportData(_app, vcCtrl.id, exportTitle, null, {
		isExcludeHideColumn: !app.getAppProperty("exportHiddenColumns"),
		excludePart: app.getAppProperty("exportExcludePart"),
		applySuppress: true
	});
}

/*
 * Body에서 property-change 이벤트 발생 시 호출.
 * 앱의 속성이 변경될 때 발생하는 이벤트 입니다.
 */
function onBodyPropertyChange(/* cpr.events.CPropertyChangeEvent */ e){
	
	if(e.property == "rowCount"){
		app.lookup("optRowCount").redraw();
				
	}else if(e.property == "showExportExcel"){
		app.getContainer().getLayout().setColumnVisible(3, e.newValue);
		
	}else if(e.property == "title"){
		app.lookup("optTilte").value = e.newValue;
	}
}

/**
 * 선택된 View-Port를 화면 최상위로 올려, 확대보기 처리한다.
 */
function doPopoutView(){
	/** @type cpr.controls.Grid */
	var vcGrid = app.getAppProperty("ctrl");
	if(vcGrid == null) return;
	
	if (mbPopOuted === true) return;
	
	var vcViewCtrl = vcGrid.getParent();
	
	moHostApp = vcGrid.getAppInstance();
	
	// 팝 아웃 시키기전 필요한 상태들을 백업 함.
	moOriginalParent = vcViewCtrl.getParent();
	moOriginalConstraint = moOriginalParent.getConstraint(vcViewCtrl);

	// 원래 부모로 부터 그리드 제거.
	moOriginalParent.removeChild(vcViewCtrl);

	// 플로팅 시킬 그룹
	moWrapper = new cpr.controls.Container();
	moWrapper.style.css({
		position: "fixed",
		top: "50px",
		right: "50px",
		bottom: "50px",
		left: "50px",
		background: "white", // 배경을 가려 줘야 함.
	});
	
	// 닫기 버튼 생성 및 추가.
	var closeButton = new cpr.controls.Button();
	closeButton.value = "닫기";
	closeButton.addEventListenerOnce("click", (function() {
		doClosePopView();
	}));

	moWrapper.addChild(closeButton, {
		bottom: "10px",
		width: "80px",
		height: "25px",
		left: "calc(50% - 40px)"
	});

	// 플로팅 될 그룹에 그리드 추가.
	moWrapper.addChild(vcViewCtrl, {
		top: "5px",
		right: "5px",
		bottom: "45px",
		left: "5px"
	});

	// 그룹을 플로팅 시킴.
	moHostApp.getRootAppInstance().floatControl(moWrapper);

	// 플로팅 된 그룹에 애니메이션 재생.
	moWrapper.style.animateFrom({
		"transform": "translateZ(-300px)"
	}, 0.3, cpr.animation.TimingFunction.EASE_OUT_BACK);
	
	mbPopOuted = true;
}

/**
 * View-Port 확대보기 되돌리기
 * 확대보기 처리된 컨트롤을 다시 원래상태로 되돌린다.
 */
function doClosePopView(){
	/** @type cpr.controls.Grid */
	var vcGrid = app.getAppProperty("ctrl");
	if(vcGrid == null) return;
	
	if (mbPopOuted === false) return;
	
	var vcViewCtrl = vcGrid.getParent();
	
	// 래퍼로 부터 그리드 제거.
	moWrapper.removeChild(vcViewCtrl);
	
	// 원래 부모에 그리드를 다시 추가.
	moOriginalParent.addChild(vcViewCtrl, moOriginalConstraint);

	// 플롯된 래퍼를 제거.
	moWrapper.getParent().removeChild(moWrapper);
	moWrapper = null;
	
	// 그리드에 애니메이션 추가.
	vcViewCtrl.style.animateFrom({
		"transform" : "translateZ(100px)",
		"opacity" : "0"
	}, 0.1);
	
	mbPopOuted = false;
}



/*
 * 버튼(btnTools)에서 click 이벤트 발생 시 호출.
 * 사용자가 컨트롤을 클릭할 때 발생하는 이벤트.
 */
function onBtnToolsClick(/* cpr.events.CMouseEvent */ e){
	/**@type cpr.controls.Grid */
	var grid = app.getAppProperty("ctrl");
	if(grid == null || grid.type !== "grid"){
		util.Msg.alertDlg(app, "Can not find Target Grid Control!");
		return false;
	}
	
	var vsGridKey = grid.getAppInstance().id + grid.id;
	
	var ctxMenu = new cpr.controls.Menu("ctx_menu");
	ctxMenu.addItem(new cpr.controls.MenuItem("정렬", "sort", "root")); 
	ctxMenu.addItem(new cpr.controls.MenuItem("정렬 표시", "sortable", "sort"));
	ctxMenu.addItem(new cpr.controls.MenuItem("정렬 초기화", "sortInit", "sort"));
	ctxMenu.addItem(new cpr.controls.MenuItem("정렬 취소", "sortCancel", "sort"));
	ctxMenu.addItem(new cpr.controls.MenuItem("필터", "filter", "root"));
	ctxMenu.addItem(new cpr.controls.MenuItem("필터 표시", "filterable", "filter"));
	ctxMenu.addItem(new cpr.controls.MenuItem("필터 초기화", "filterInit", "filter"));
	ctxMenu.addItem(new cpr.controls.MenuItem("필터 취소", "filterCancel", "filter"));
	ctxMenu.addItem(new cpr.controls.MenuItem("개인화저장", "personalized", "root"));
	ctxMenu.addItem(new cpr.controls.MenuItem("초기화", "initial", "root"));
	ctxMenu.addItem(new cpr.controls.MenuItem("컬럼 설정", "column", "root"));
	ctxMenu.addItem(new cpr.controls.MenuItem("틀고정", "split", "root"));
	ctxMenu.addItem(new cpr.controls.MenuItem("엑셀내보내기", "excelExport", "root"));
//	ctxMenu.addItem(new cpr.controls.MenuItem("인쇄", "print", "root"));
	
	// 인쇄시 그리드에 선택된 행이 존재할 경우 컨트롤 영역이 보여져 그리드 선택 clear 후 재 선택되도록 수정 (2021.07.29 추가)
	var vaGrdSelectIdx = grid.getSelectedIndices();
	ctxMenu.addEventListener("before-selection-change", function(e){
		vaGrdSelectIdx = grid.getSelectedIndices();
		grid.clearSelection();
	});
	
	// 인쇄시 그리드에 선택된 행이 존재할 경우 컨트롤 영역이 보여져 그리드 선택 clear 후 재 선택되도록 수정 (2021.07.29 추가)
	ctxMenu.addEventListener("dispose", function(e){
		grid.selectRows(vaGrdSelectIdx);
	});
	
	ctxMenu.addEventListener("item-click", function(/**@type cpr.events.CItemEvent */ e){
		var itemValue = e.item.value;
		
		if(itemValue == "sort" || itemValue == "filter") {
		//정렬, 필터 선택시 컨텍스트 메뉴가 닫히지 않음
			ctxMenu.selectItem(ctxMenu.selectItemByValue(itemValue));
			return;
		//정렬 표시
		} else if(itemValue == "sortable") {
			for(var i = 0, len = grid.header.cellCount; i < len; i++) {
				grid.header.getColumn(i).sortable = true;
			}
		//정렬 초기화
		}else if(itemValue == "sortInit"){
			if(grid) grid.header.clearSort();
		//정렬 취소
		}else if(itemValue == "sortCancel"){
			if(grid) grid.header.clearSort();
			for (var i = 0, len = grid.header.cellCount; i < len; i++) {
				grid.header.getColumn(i).sortable = false;
			}	
		//필터 보여주기
		}else if(itemValue == "filterable"){
			var isHFiltered = false;
			var column, filterStr;
			for(var i=0, len=grid.header.cellCount; i<len; i++){
				column = grid.header.getColumn(i);
				filterStr = column.getFilter();
				if(filterStr != null && filterStr[0] != "__all__"){
					isHFiltered = true;
					break;
				}
			}
			if(!isHFiltered){
				var hTRowIndex = app.getAppProperty("headerRowIndex");
				for(var i=0, len=grid.header.cellCount; i<len; i++){
					column = grid.header.getColumn(i);
					if(hTRowIndex > -1){
						if((column.rowIndex + column.rowSpan) == (hTRowIndex+1) && column.targetColumnName != ""){
							column.filterable = true;
						}
					}else{
						if(column.targetColumnName != ""){
							column.filterable = true;
						}
					}
				}
			}
		//필터 초기화
		}else if(itemValue == "filterInit"){
			if(grid) grid.header.clearFilter();
		//필터 해제 및 필터 숨김 (필터 취소)
		}else if(itemValue == "filterCancel"){
			if(grid){
				grid.header.clearFilter();
				for(var i=0, len=grid.header.cellCount; i<len; i++){
					grid.header.getColumn(i).filterable = false;
				}
			}
		//인쇄
		}else if(itemValue == "print"){
			/*
			var targetEle = document.getElementById("uuid-"+grid.uuid);
			// 3.DOM의 text를 얻습니다.
			var content = targetEle.innerHTML;
			// 오픈되는 윈도우에 프린트하려는 데이터를 공유합니다.
		    sessionStorage.setItem("print-content", content);
		    var windowWidth = (window.innerWidth | document.body.clientWidth)-500;
			var windowHeight = (window.innerHeight | document.body.clientHeight)-300;
			var width = windowWidth > 600 ? windowWidth : 600;
			var height = windowHeight > 400 ? windowHeight : 400;
			
			//print하는 페이지를 엽니다.
			var popWindow = window.open('app/com/inc/print.html',"print",'left=100,top=100, resizable=yes, height='+height+',width='+width);
			* */
			
			sessionStorage.setItem("print-content", getHTMLToGrid([grid], {
					title : grid.fieldLabel,
				})); 
			
			var windowWidth = (window.innerWidth | document.body.clientWidth)-500;
			var windowHeight = (window.innerHeight | document.body.clientHeight)-300;
			var width = windowWidth > 600 ? windowWidth : 600;
			var height = windowHeight > 400 ? windowHeight : 400;
			var popWindow = window.open('app/com/inc/print.html',"print",'left=100,top=100, resizable=yes, height='+height+',width='+width);
		}else if(itemValue == "personalized"){
			var voGridLayout = grid.getColumnLayout();
			var vsGridLayout = JSON.stringify(voGridLayout);
			localStorage.setItem( vsGridKey , vsGridLayout);
			util.Msg.notify(app, "그리드 상태가 저장되었습니다.");
		}else if(itemValue == "initial"){
			localStorage.setItem( vsGridKey , null);
			grid.resetGrid();
			util.Msg.notify(app, "그리드가 초기화 되었습니다.");
		
		// 그리드 컬럼 필터팝업 (2021.10.05 추가)
		} else if(itemValue == "column") {

			
			util.Dialog.open(app, "app/com/comPGridFilter", 400, 490, function(dialog){
				
			}, {targetGrid:grid});
			
//			if(rootContainer == null || showConstraint == null) return;
//			var grdFilter = new udc.com.udcComGridFilter();
//			grdFilter.userAttr("grid-filter", "true");
//			grdFilter.grid = grid;
//			grdFilter.addEventListenerOnce("cnclClick", function(e) {
//				rootContainer.removeChild(grdFilter);
//			})
//			grdFilter.addEventListenerOnce("okClick", function(e) {
//				rootContainer.removeChild(grdFilter);
//			})
//			
//			var vnWidth = 320;
//			var vnSetColLocX = e.clientX;
//			showConstraint.width = vnWidth + "px";
//			if(vnSetColLocX < 320) {
//				showConstraint.left = 0 + "px";
//			} else {
//				showConstraint.left = parseInt(_.clone(showConstraint.left)) - (vnWidth/2) + "px";
//			}
//			
//			rootContainer.addEventListenerOnce("click", function(){
////				rootContainer.removeChild(grdFilter);
//			})
//
//			rootContainer.floatControl(grdFilter, showConstraint);
			//그리드 컬럼 틀 고정 팝업 (2022.04.22 추가)
		} else if(itemValue == "split") {
			util.Dialog.open(app, "app/com/comPGridSplit", 330, 310, function(dialog){
				
			}, {targetGrid:grid});
//			if(rootContainer == null || showConstraint == null) return;
//			var gridSplit = new udc.com.udcComGridSplit();
//			gridSplit.grid = grid;
//			gridSplit.addEventListenerOnce("cnclClick", function(e) {
//				rootContainer.removeChild(gridSplit);
//			})
//			gridSplit.addEventListenerOnce("okClick", function(e) {
//				rootContainer.removeChild(gridSplit);
//			})
//			
//			var vnWidth = 320;
//			var vnSplitColLocX = e.clientX;
//			showConstraint.width = vnWidth + "px";
//			if(vnSplitColLocX < 320) {
//				showConstraint.left = 0 + "px";
//			} else {
//				showConstraint.left = parseInt(_.clone(showConstraint.left)) - (vnWidth/2) + "px";
//			}
//			
//			rootContainer.addEventListenerOnce("click", function(){
//					//rootContainer.removeChild(gridSplit);
//			})
//			
//			rootContainer.floatControl(gridSplit, showConstraint);
		}else if(itemValue == "excelExport"){
			
			var vcCtrl = app.getAppProperty("ctrl");
			var exportTitle = !ValueUtil.isNull(app.getAppProperty("exportExcelTitle")) ? app.getAppProperty("exportExcelTitle") : app.lookup("optTilte").value;
			var _app = vcCtrl.getAppInstance();
			var vsDomain = document.domain;
			if(window.eb6Preview) {
				// 미리보기 화면에서, USE_CLIENT_EXCEL_DOWLOAD=true 인 경우
				
				/**
				 * README 클라이언트 엑셀 다운로드 유의사항
				 * 
				 * 1) 헤더, 디테일 영역만 다운로드 가능 ( 그룹헤더, 그룹 푸터, 푸터는 불가능)
				 * 	   ㄴ 모듈 내 커스터마이징 가능
				 * 2) 다중 헤더, 다중 디테일 불가능 (cellindex 순서대로 각 영역별 한 행씩 할당됨)
				 * 3) 병합 불가능 (병합으로 인해 헤더셀이 디테일 셀 개수보다 적을 경우, 다른 컬럼에 데이터가 할당될 수도 있음)
				 * 4) 스타일 설정 없음
				 * 	   ㄴ 모듈 내 커스터마이징 가능
				 * 5) 헤더 컬럼과 디테일 컬럼은 헤더 text 를 기준으로 설정. 헤더 text 가 중복될 경우, 잘못된 데이터가 바인딩 될 수 있으므로 주의
				 * 	   ㄴ 모듈 내 커스터마이징 가능
				 * 6) 필터, 소트, 컬럼 순서변경이 될 경우, 적용된 결과로 엑셀 다운로드 가능
				 * 7) columnVisible=false 인 컬럼도 엑셀 다운로드 시 포함됨
				 *     ㄴ 모듈 내 커스터 마이징 가능
				 * 
				 * - 각 사이트 별 Customizing 필요
				 */
				if(AppProperties.USE_CLIENT_EXCEL_DOWLOAD === true) {
					excelUtil.exportExcelToJSON(exportTitle+".xlsx", "sheet1", vcCtrl, null);
				} else {
					util.Msg.alertDlg(app, "INF-M000", "엑셀 다운로드를 하기 위해서는\n서버 연동 후 실행하시기 바랍니다.");
				}
			} else {
				util.Grid.exportData(_app, vcCtrl.id, exportTitle, null, {
					applySuppress: true,
					isExcludeHideColumn: !app.getAppProperty("exportHiddenColumns"),
					excludePart: app.getAppProperty("exportExcludePart")
				});
			}
		}
		
		ctxMenu.blur();
//		ctxMenu.dispose();
	});
	
	ctxMenu.addEventListenerOnce("blur", function(/**@type cpr.events.CFocusEvent*/ e){
		// 인쇄시 그리드에 선택된 행이 존재할 경우, 컨트롤 영역이 보여져 그리드 선택 clear 후 재 선택되도록 수정 (2021.07.29 추가)
		grid.selectRows(vaGrdSelectIdx);
		e.control.dispose();
	});
		
	/**@type cpr.controls.Container */
	var rootContainer = null;
	var showConstraint = {
		"position" : "absolute",
		"width" : "auto",
		"height" : "auto"
	};
	if(util.Dialog.isDialogPopup(grid.getAppInstance())){
		rootContainer = grid.getAppInstance().getContainer();
		
		if((e.clientY - rootContainer.getActualRect().top + 130) > rootContainer.getActualRect().height )
			showConstraint.top = (e.clientY - rootContainer.getActualRect().top - 130) +"px";
		else
			showConstraint.top = (e.clientY - rootContainer.getActualRect().top) +"px";
		
		showConstraint.left = (e.clientX - (rootContainer.getActualRect().left + 130)) + "px";
	}else{
		rootContainer = grid.getAppInstance().getRootAppInstance().getContainer();
		// 스크롤 이동 후 컨텍스트 메뉴를 띄운 경우 클릭된 위치에 스크롤 이동량 추가
		showConstraint.top = (e.clientY + e.view.scrollY) + "px";
		if(e.clientX < 130){
			showConstraint.left = "0px";
		}else{
			showConstraint.left = (e.clientX) + "px";
		}
	}

	var layout = rootContainer.getLayout();
	if(layout instanceof cpr.controls.layouts.FormLayout
		|| layout instanceof cpr.controls.layouts.VerticalLayout){
		rootContainer.floatControl(ctxMenu, showConstraint);
	}else{
		rootContainer.addChild(ctxMenu, showConstraint);
	}
	ctxMenu.focus();
}



/**
 * 프린트할 html 반환
 */
function getHTMLToGrid(gridArr, pageInfo){
	var htmlTagArr= [];
	htmlTagArr[htmlTagArr.length] = "<div class='page'>";

	if (pageInfo != null) {
		// 제목
		if (pageInfo.hasOwnProperty("title")) {
			htmlTagArr[htmlTagArr.length] = "<h1>"+pageInfo["title"]+"</h1>";
		}
		// 결재선 테이블
		if (pageInfo.hasOwnProperty("approvalLine")) {
			var approvalLineTagArr = [];
			approvalLineTagArr[approvalLineTagArr.length] = "<table class='approval_table' >";
			approvalLineTagArr[approvalLineTagArr.length] = "<tr>";
			for (var i = 0; i < pageInfo["approvalLine"].length; i++) {
				approvalLineTagArr[approvalLineTagArr.length] = "<td>"+pageInfo["approvalLine"][i]+"</td>";
			}
			approvalLineTagArr[approvalLineTagArr.length] = "</tr><tr>";
			for (var i = 0; i < pageInfo["approvalLine"].length; i++) {
				approvalLineTagArr[approvalLineTagArr.length] = "<td></td>";
			}
			approvalLineTagArr[approvalLineTagArr.length] = "</tr>";
			approvalLineTagArr[approvalLineTagArr.length] = "</table>";
			htmlTagArr[htmlTagArr.length] = approvalLineTagArr.join("");
		}
	}
	
	
	// 그리드
	for (var i = 0; i < gridArr.length; i++) {
		htmlTagArr[htmlTagArr.length] = getGridTable(gridArr[i]).join("");
		
		if (i != (gridArr.length-1)) {
			htmlTagArr[htmlTagArr.length] = "<div style='page-break-before: always; '> </div>";
		}
	}
	htmlTagArr[htmlTagArr.length] = "</div>";
	
	return htmlTagArr.join("");
}

/**
 * 그리드 데이터를 통한 table 반환
 * @param {cpr.controls.Grid} grid
 */
function getGridTable(grid) {
	var exportData = grid.getExportData({
		exceptStyle : false,
		applyFormat: true,
		applySuppress: true
	});
	
	var tableTagArr = [];
	tableTagArr[tableTagArr.length] = "<table class='cl-control cl-grid'>";
	var cols = exportData.cols;
	tableTagArr[tableTagArr.length] = "<colgroup>";
	var colWidths = [];
	var totalColumnWidth = 0;
	for (var ci = 0; ci < cols.length; ci++) {
		var colWidth = cpr.utils.ParamUtil.parseSize(cols[ci]["width"]).size;
		totalColumnWidth += colWidth;
		colWidths[colWidths.length] = colWidth;
	}
	for (var ci = 0; ci < colWidths.length; ci++) {
		tableTagArr[tableTagArr.length] = "<col style='width : " + (colWidths[ci] / totalColumnWidth) + "%'>";
	}
	tableTagArr[tableTagArr.length] = "</colgroup>";
	
	var layouts = exportData.rowgroups;
	for (var li = 0; li < layouts.length; li++) {
		var layout = layouts[li];
		var datas = layout.data;
		var styles = layout.style;
		var trTagArr = [];
		
		// 셀별 데이터를 trTagArr에 배열로 추가
		for (var ri = 0; ri < datas.length; ri++) {
			var tdTagArr = [];
			for (var ci = 0; ci < datas[ri].length; ci++) {
				var rowIndex = styles[ci]["rowindex"];
				if (tdTagArr[rowIndex] == null) {
					tdTagArr[rowIndex] = [];
				}
				if (Object.keys(datas[ri][ci]).length != 0) {
					tdTagArr[rowIndex][tdTagArr[rowIndex].length] = getTd(datas[ri][ci], styles[ci]).join("");
				}
			}
			trTagArr = trTagArr.concat(tdTagArr);
		}
		
		if (layout["region"] == "header") {
			tableTagArr[tableTagArr.length] = "<thead class='cl-grid-header'>";
		}
		for (var tri=0; tri < trTagArr.length; tri++) {
			tableTagArr[tableTagArr.length] = "<tr class='cl-grid-row'>";
			tableTagArr[tableTagArr.length] = trTagArr[tri].join("");
			tableTagArr[tableTagArr.length] = "</tr>";
		}
		if (layout["region"] == "header") {
			tableTagArr[tableTagArr.length] = "</thead>";
		}
	}
	
	tableTagArr[tableTagArr.length] = "</table>";
	return tableTagArr;
	
}

/**
 * 셀 데이터를 통한 td 반환
 * @param {any} cell
 * @param {any} columnInfo
 * @returns {string[]}
 */
function getTd(cell, columnInfo) {
	var value = cell["value"];
	var cellStyle = cell["style"];
	var columnStyle = columnInfo["style"];
	var style = _.extend({}, columnStyle, cellStyle);
	var styleKeys = Object.keys(style);
	
	var tdTagArr = [];
	tdTagArr[tdTagArr.length] = "<td class='cl-grid-cell'"; 
	if (cell.hasOwnProperty("rowspan")) {
		if (cell["rowspan"] > 1) {
			tdTagArr[tdTagArr.length] = " rowspan='"+cell["rowspan"]+"'";
		}
	} else if (columnInfo["rowspan"] > 1) {
		tdTagArr[tdTagArr.length] = " rowspan='"+columnInfo["rowspan"]+"'";
	}
	if (cell.hasOwnProperty("colspan")) {
		if (cell["colspan"] > 1) {
			tdTagArr[tdTagArr.length] = " colspan='"+cell["colspan"]+"'";
		}
	} else if (columnInfo["colspan"] > 1) {
		tdTagArr[tdTagArr.length] = " colspan='"+columnInfo["colspan"]+"'";
	}
	
	if (styleKeys.length > 0) {
		tdTagArr[tdTagArr.length] = " style='";
		for (var si = 0; si < styleKeys.length; si++) {
			tdTagArr[tdTagArr.length] = styleKeys[si] + " : " + style[styleKeys[si]] +"; ";
		}
		tdTagArr[tdTagArr.length] = "'"
	}
	tdTagArr[tdTagArr.length] = ">" + value + "</td>";
	
	return tdTagArr;
}


/*
 * 버튼(btnTools2)에서 click 이벤트 발생 시 호출.
 * 사용자가 컨트롤을 클릭할 때 발생하는 이벤트.
 */
function onBtnTools2Click(/* cpr.events.CMouseEvent */ e){
	/** 
	 * @type cpr.controls.Button
	 */
	var btnTools2 = e.control;
	doPopoutView();
}
