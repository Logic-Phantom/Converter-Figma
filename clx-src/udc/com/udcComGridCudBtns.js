//공통 유틸(Util) 클래스
var util = createCommonUtil();

/**
 * 신규/삭제/취소/저장 버튼 활성/비활성화
 * @param {Boolean} pbEnable
 * @param {Array} paStatus [I:신규, D:삭제, R:취소,  D:저장]
 */
exports.setEnableCtrls = function(pbEnable, paStatus){
	var vaBtnIds = null;
	if(ValueUtil.isNull(paStatus)){
		vaBtnIds = ["btnNew", "btnDelete", "btnRestore", "btnSave"];
	}else{
		vaBtnIds = new Array();
		for(var i = 0; i < paStatus.length; i++){
			if("I" == paStatus[i]){
				vaBtnIds.push("btnNew");
			}else if("D" == paStatus[i]){
				vaBtnIds.push("btnDelete");
			}else if("S" == paStatus[i]){
				vaBtnIds.push("btnSave");
			}else if("R" == paStatus[i]){
				vaBtnIds.push("btnRestore");
			}
		}
	}
	util.Control.setEnable(app, pbEnable, vaBtnIds);
}

/*
 * "신규" 버튼에서 click 이벤트 발생 시 호출. 
 * 사용자가 컨트롤을 클릭할 때 발생하는 이벤트.
 */
function onBtnInsertClick(/* cpr.events.CMouseEvent */e) {
	var hostApp = app.getHostAppInstance();
	var vcCtrl = app.getAppProperty("grid"); //타겟 컨트롤
	var vsFocusColumn = app.getAppProperty("focusColumnName"); //포커스 컬럼
	
//	var event = new cpr.events.CUIEvent("insert");

	var vbStatus = false;
	if(vcCtrl == null || app.getAppProperty("ignoreDefaultNewAction") === true){
		//디폴트 행추가 Action 무시하는 경우는 이벤트만 발생시킴
		vbStatus = app.dispatchEvent(new cpr.events.CUIEvent("insert"));
	}else{
		var voRow = null;
		//신규 행추가 전에 체크할 로직이 있는지 체크
		var vbSuccess = app.dispatchEvent(new cpr.events.CUIEvent("beforeInsert"));
		
		if(!vbSuccess) return;
		
		//그리드인 경우
		if(vcCtrl instanceof cpr.controls.Grid){
			//1-1.신규 행 추가
			voRow = util.Grid.insertRow(hostApp, vcCtrl.id, vsFocusColumn);
		//프리폼인 경우
		}else{
			//2-1.변경사항 체크
			if(util.FreeForm.isModified(hostApp, vcCtrl.id, "CRM")) return false;
			
			freeFormInsertTask(hostApp, vcCtrl, voRow, vsFocusColumn)
		}
		var event = new cpr.events.CUIEvent("insert", {
			userData: {
				"row": voRow ? voRow : null,
				"rowIndex": voRow ? voRow.getIndex() : -1
			}
		});
		
		vbStatus = app.dispatchEvent(event);
	}
	
	if(vbStatus){
		var commonEvent = new cpr.events.CUIEvent("commonEvent", {
			userData: {
				"status": "insert"
			}
		});
		app.dispatchEvent(commonEvent);	
	}
}

function freeFormInsertTask(hostApp, vcCtrl, voRow, vsFocusColumn){
	
	var voBindContext = util.Group.getBindContext(hostApp, vcCtrl);
	//var vcGrid = voBindContext.grid;
	var voBindCtl = util.Group.getBindControl(hostApp, vcCtrl, voBindContext);
	/**@type cpr.data.DataSet */
	//var voDs = voBindContext.grid ? voBindContext.grid.dataSet : voBindContext.dataSet;
	var voDs = util.Group.getBindDataSet(hostApp, vcCtrl, voBindContext);
	var vnRowIndex = util.Group.getBindCtlRowIndex(hostApp, voBindContext);
	//데이터 Revert
	if(voDs.getRowState(vnRowIndex) == cpr.data.tabledata.RowState.INSERTED){
						
		util.FreeForm.revertRow(hostApp, vcCtrl.id, vnRowIndex, vsFocusColumn);
						
		if(vnRowIndex > -1){
			if(voBindCtl instanceof cpr.controls.Grid){
				voBindCtl.clearSelection(false);
				voBindCtl.selectRows(vnRowIndex)	
			};
		}
		if(voBindCtl instanceof cpr.controls.Grid){
			voRow = voBindCtl.getRow(vnRowIndex);
		}				
						
	}else{
		//2-2.프리폼 Reset처리
		util.FreeForm.revertAllData(hostApp, vcCtrl.id);
		//2-3.신규 행 추가
		voRow = util.FreeForm.insertRow(hostApp, vcCtrl.id, vsFocusColumn);
	}
}

/*
 * "삭제" 버튼에서 click 이벤트 발생 시 호출. 
 * 사용자가 컨트롤을 클릭할 때 발생하는 이벤트.
 */
function onBtnDelClick(/* cpr.events.CMouseEvent */e) {
	var vaCtrlArr = [];
	var vcCtrl = app.getAppProperty("grid"); //타겟 컨트롤
	
	var hostApp = app.getHostAppInstance();
	var event = new cpr.events.CUIEvent("delete");
	
	var vbStatus = false; 
	
	if(vcCtrl == null || app.getAppProperty("ignoreDefaultDeleteAction") === true){
		//디폴트 행삭제 Action 무시하는 경우는 이벤트만 발생시킴
		vbStatus = app.dispatchEvent(event);
	}else{
		//행삭제 전에 체크할 로직이 있는지 체크
		var vbSuccess = app.dispatchEvent(new cpr.events.CUIEvent("beforeDelete"));
		if(!vbSuccess) return;
		
		// 그리드인 경우
		if(vcCtrl instanceof cpr.controls.Grid){
			//선택행 삭제
			util.Grid.deleteRow(hostApp, vcCtrl.id);
			
			vbStatus = app.dispatchEvent(event);
			
		// 프리폼인 경우
		}else{
			if(util.FreeForm.deleteRow(hostApp, vcCtrl.id, "CRM")){
				vbStatus = app.dispatchEvent(event);
			}
		}
	}
	if(vbStatus){
		var commonEvent = new cpr.events.CUIEvent("commonEvent", {
			userData: {
				"status": "delete"
			}
		});
		app.dispatchEvent(commonEvent);	
	}
}

/*
 * "저장" 버튼에서 click 이벤트 발생 시 호출. 
 * 사용자가 컨트롤을 클릭할 때 발생하는 이벤트.
 */
function onBtnSaveClick(/* cpr.events.CMouseEvent */e) {
	var event = new cpr.events.CUIEvent("save");
	app.dispatchEvent(event);
}

/*
 * Body에서 property-change 이벤트 발생 시 호출. 
 * 앱의 속성이 변경될 때 발생하는 이벤트 입니다.
 */
function onBodyPropertyChange(/* cpr.events.CPropertyChangeEvent */e) {
	if(e.property == "visibleNewButton"){
		if(e.newValue === false){
			app.getContainer().getLayout().setColumnVisible(0, false);
		} else if(e.newValue === true){
            app.getContainer().getLayout().setColumnVisible(0, true);
        }
	}else if(e.property == "visibleDeleteButton"){
		if(e.newValue === false){
			app.getContainer().getLayout().setColumnVisible(1, false);
		} else if(e.newValue === true){
            app.getContainer().getLayout().setColumnVisible(1, true);
        }
	}else if(e.property == "visibleRestoreButton"){
		if(e.newValue === false){
			app.getContainer().getLayout().setColumnVisible(2, false);
		} else if(e.newValue === true){
			app.getContainer().getLayout().setColumnVisible(2, true);
		}
	}else if(e.property == "visibleSaveButton"){
		if(e.newValue === false){
			app.getContainer().getLayout().setColumnVisible(3, false);
		} else if(e.newValue === true){
			app.getContainer().getLayout().setColumnVisible(3, true);
		}	
	}else if (e.property == "forceDelete") {
		if(e.newValue === true){
			var vcBtnDel = app.lookup("btnDelete");
			vcBtnDel.style.setClasses(["btn-primary", "btn-i-remove"]);
			vcBtnDel.redraw();
		}
	}
}


/*
 * 버튼(btnRestore)에서 click 이벤트 발생 시 호출.
 * 사용자가 컨트롤을 클릭할 때 발생하는 이벤트.
 */
function onBtnRestoreClick(/* cpr.events.CMouseEvent */ e){
	/** 
	 * @type cpr.controls.Button
	 */
	var btnRestore = e.control;
	
	var vaCtrlArr = [];
	var vcCtrl = app.getAppProperty("grid"); //타겟 컨트롤
	
	var hostApp = app.getHostAppInstance();
	var event = new cpr.events.CUIEvent("restore");
	var vbStatus = false;
	if(vcCtrl == null || app.getAppProperty("ignoreDefaultRestoreAction") === true){
		//디폴트 행삭제 Action 무시하는 경우는 이벤트만 발생시킴
		vbStatus = app.dispatchEvent(event);
	}else{
		//행삭제 전에 체크할 로직이 있는지 체크
		var vbSuccess = app.dispatchEvent(new cpr.events.CUIEvent("beforeRestore"));
		if(!vbSuccess) return;
		// 그리드인 경우
		if(vcCtrl instanceof cpr.controls.Grid){
			//선택행 삭제
			util.Grid.revertRowData(hostApp, vcCtrl.id);
			
			var vsDataBindCtxId = vcCtrl.userAttr("bindDataFormId");
			
			if(vsDataBindCtxId){
				hostApp.lookup(vsDataBindCtxId).redraw();	
			}
			
			vbStatus = app.dispatchEvent(event);
		// 프리폼인 경우
		}else{
			var vcForm = hostApp.lookup(vcCtrl.id);
			/** @type cpr.bind.BindContext */
			var voBindContext = util.Group.getBindContext(hostApp, vcForm);
			//var voDs = voBindContext.grid ? voBindContext.grid.dataSet : voBindContext.dataSet;
			//var rowIndex = voBindContext.grid ? util.Grid.getIndex(hostApp, voBindContext.grid.id) : voBindContext.rowIndex;
			var voDs = util.Group.getBindDataSet(hostApp, vcForm, voBindContext);
			var rowIndex = util.Group.getBindCtlRowIndex(hostApp, voBindContext);

			if(voDs.getRowState(rowIndex) == cpr.data.tabledata.RowState.INSERTED){
				voDs.revertRow(rowIndex);
			}else{
				util.FreeForm.revertRow(hostApp, vcCtrl.id);
			}
				
			vbStatus = app.dispatchEvent(event);
		}
		//util.Control.removeInvalidClassAll(hostApp, vcCtrl.id);
	}
	 if(vbStatus){
		var commonEvent = new cpr.events.CUIEvent("commonEvent", {
			userData: {
				"status": "restore"
			}
		});
		app.dispatchEvent(commonEvent);	
	}
	
}
