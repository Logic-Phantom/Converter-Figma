/************************************************
 * udcComAppHeader.js
 * Created at 2024. 7. 19. 오후 5:31:50.
 *
 * @author ryu
 ************************************************/
var util = createCommonUtil();

/**
 * UDC 컨트롤이 그리드의 뷰 모드에서 표시할 텍스트를 반환합니다.
 */
exports.getText = function(){
	// TODO: 그리드의 뷰 모드에서 표시할 텍스트를 반환하는 하는 코드를 작성해야 합니다.
	return "";
};

/*
 * 루트 컨테이너에서 load 이벤트 발생 시 호출.
 * 앱이 최초 구성된후 최초 랜더링 직후에 발생하는 이벤트 입니다.
 */
function onBodyLoad(e){
	var hostApp = app.getHostAppInstance();
	if(!hostApp) return;
	
	if (!util.Dialog.isDialogPopup(hostApp) && app.getRootAppInstance().hasAppMethod("getMenuPath")) {
		
		//어플리케이션 메뉴 정보
		var voMenuInfo = util.Main.getMenuInfo(app);
		var vsCallPage = voMenuInfo.get("CALL_PAGE");
		var vsMenuId = voMenuInfo.get("MENU_ID");
		var vsMenuNm = voMenuInfo.get("MENU_NM");
		
		var vsAppPropTitle = app.getAppProperty("title");
		
		if (!ValueUtil.isNull(vsMenuNm) && vsAppPropTitle == "타이틀을 입력하세요") {
			app.lookup("optTit").value = vsMenuNm;
		}
		
		var vcGrpMenuPath = app.lookup("grpBc");
		vcGrpMenuPath.removeAllChildren();
		var voDmMenuNaviPath = app.getRootAppInstance().callAppMethod("getMenuPath", vsMenuId);
		if (voDmMenuNaviPath) {
			
			var vaMenuNaviPathId = voDmMenuNaviPath.get("MENU_PATH_ID");
			var vaMenuNaviPathNm = voDmMenuNaviPath.get("MENU_PATH_NM");			
			var vcGrpMenuPathLayout = vcGrpMenuPath.getLayout();
			vaMenuNaviPathId.some(function(cos, idx) {
				var vcOptMenuPath = new cpr.controls.Output(cos);
				vcOptMenuPath.style.addClass("breadcrumb-item");
				var vsPathNm = vaMenuNaviPathNm[idx];
				
				vcOptMenuPath.value = vsPathNm;
				
				if (idx == vaMenuNaviPathId.length - 1) {
					vcOptMenuPath.tooltip = vsCallPage;
				}
				
				vcGrpMenuPath.addChild(vcOptMenuPath, {
					width: "100px",
					height: "24px",
					autoSize: "width"
				});
			});
		}
		
		// 로컬스토리지에 저장된 즐겨찾기 메뉴일 경우
		var vsFavMenus = localStorage.getItem(AppProperties.PROJECT_NM + "favMenus");
		// 로컬스토리지에 즐겨찾기 메뉴ID 저장
		if (!ValueUtil.isNull(vsFavMenus)) {
			JSON.parse(vsFavMenus).filter(function(each){
				if (vsMenuId == each) app.lookup("cbxFav").checked = true;
				return;
			});
		}
	} else {
		
		util.Control.setVisible(app, false, ["grpBc"]);
		var vsAppPropTitle = app.getAppProperty("title");
		if (vsAppPropTitle == "타이틀을 입력하세요") {
			app.lookup("optTit").value = hostApp.app.title;
		}
	}
	
	// 조회조건 그룹 ID
	var vsSearchBoxId  = app.getAppProperty("searchBoxId") != null ? app.getAppProperty("searchBoxId") : "grpSearch";
	// 데이터 그룹 ID
	var vsDisableBoxId = app.getAppProperty("groupBoxIds");
	var vaDisableBoxIds = vsDisableBoxId != null ? ValueUtil.split(vsDisableBoxId, ",") : ["grpData"];
	
	//조회조건 그룹 		
	if (!ValueUtil.isNull(vsSearchBoxId)) {
		var pbExist = false;
		for (var i = 0, len = vaDisableBoxIds.length; i < len; i++) {
			if (hostApp.lookup(vaDisableBoxIds[i]) != null) {
				pbExist = true;
				break;
			}
		}
		
		if (pbExist) {
			var vsInitializeYn = app.getAppProperty("isGrpDataDisable");			
						
			// 화면 조회시 grpData 그룹 비활 예외 처리				
			util.Group.initSearchBox(hostApp, vsSearchBoxId, vaDisableBoxIds, null, vsInitializeYn);
		}
	}
	
	//그리드 초기화
	//그리드ID가 지정된 경우가 아니면... 화면 내의 모든 그리드를 대상으로 초기화 작업을 수행한다.
	if(app.getAppProperty("isGridInit")) {
		var vaIgnoreGridIds = ValueUtil.split(app.getAppProperty("ignoreGridIds"), ",");
		var vaGridIds = util.Group.getAllChildrenByType(hostApp, "grid").filter(function(/* cpr.controls.Grid */ grid){
			return (vaIgnoreGridIds.indexOf(grid.id) == -1) ;
		});
		util.Grid.init(hostApp, vaGridIds);
	}
	
	//프리폼 초기화
	if(app.getAppProperty("isFreeformInit")) {
		var vaSearchBoxIds = ValueUtil.split(vsSearchBoxId, ",");
		var vaIgnoreFreeformIds = ValueUtil.split(app.getAppProperty("ignoreFreeFormIds"), ",");
		var vaFreeformIds = [];
		util.Group.getAllChildrenByType(hostApp, "container", null, true).forEach(function(/* cpr.controls.Container */ form){
			if (vaIgnoreFreeformIds.indexOf(form.id) == -1 && vaSearchBoxIds.indexOf(form.id) == -1 && form.getLayout() instanceof cpr.controls.layouts.FormLayout) {
				if (util.Group.getBindDataSet(app, form) != null) {
					vaFreeformIds.push(form.id);
				}
			}
		});
		util.FreeForm.init(hostApp, vaFreeformIds);
	}
	
	// 트리 초기화
	if(app.getAppProperty("isTreeInit")) {
		var vaIgnoreTreeIds = ValueUtil.split(app.getAppProperty("ignoreTreeIds"), ",");
		var vaTrees = util.Group.getAllChildrenByType(hostApp, "tree", null, true).filter(function(/* cpr.controls.Tree */ tree){
			return (vaIgnoreTreeIds.indexOf(tree.id) == -1) ;
		});
		util.Tree.init(hostApp, vaTrees);
	}
}

/*
 * 아웃풋에서 dblclick 이벤트 발생 시 호출.
 * 사용자가 컨트롤을 더블 클릭할 때 발생하는 이벤트.
 */
function onOutputDblclick(e){
	util.procEb6Privew(app);
}

/*
 * 루트 컨테이너에서 screen-change 이벤트 발생 시 호출.
 * 스크린 크기 변경 시 전파되는 이벤트.
 */
function onBodyScreenChange(e){
	cpr.core.DeferredUpdateManager.INSTANCE.asyncExec(function() {
		var hostApp = app.getHostAppInstance();
		if(!hostApp) return;
		
		var vaCtrls = hostApp.getContainer().getAllRecursiveChildren(true);
		var vsScreenNm = e.screen.name;
		vaCtrls.filter(function(each) {
			return each.style.hasClass("subpage") || each.style.hasClass("content-box") || each.style.hasClass("card");
		}).forEach(function( /*cpr.controls.Container*/ each) {
			var voLayout = each.getLayout();
			if (voLayout instanceof cpr.controls.layouts.VerticalLayout || voLayout instanceof cpr.controls.layouts.FormLayout) {
				if (vsScreenNm == "EXB-PART") {
					voLayout._org_topMargin = voLayout.topMargin;
					voLayout._org_rightMargin = voLayout.rightMargin;
					voLayout._org_bottomMargin = voLayout.bottomMargin;
					voLayout._org_leftMargin = voLayout.leftMargin;
					voLayout.topMargin = 5;
					voLayout.bottomMargin = 5;
					voLayout.leftMargin = 10;
					voLayout.rightMargin = 5;
				} else {
					var vnOrgTopMargin = voLayout._org_topMargin;
					if (vnOrgTopMargin != undefined) {
						voLayout.topMargin = voLayout._org_topMargin;
						voLayout.rightMargin = voLayout._org_rightMargin;
						voLayout.bottomMargin = voLayout._org_bottomMargin;
						voLayout.leftMargin = voLayout._org_leftMargin;
					}
				}				
			}
		});
	});
}

/*
 * 체크 박스에서 value-change 이벤트 발생 시 호출.
 */
function onCheckBoxValueChange(e){
	var checkBox = e.control;
	
	//어플리케이션 메뉴 정보
	var voMenuInfo = util.Main.getMenuInfo(app);
	var vsMenuId = voMenuInfo.get("MENU_ID");
	var favMenus = [];
	var vsFavMenus = localStorage.getItem(AppProperties.PROJECT_NM + "favMenus");
		
	if (checkBox.checked) {
		// 로컬스토리지에 즐겨찾기 메뉴ID 저장
		if (!ValueUtil.isNull(JSON.parse(vsFavMenus))) {
			JSON.parse(vsFavMenus).filter(function(each){
				if (vsMenuId != each) favMenus.push(each);
				return;
			});
		}
		
		favMenus.push(vsMenuId);
	} else {
		// 로컬스토리지에 즐겨찾기 메뉴ID 삭제
		if (!ValueUtil.isNull(JSON.parse(vsFavMenus))) {
			JSON.parse(vsFavMenus).forEach(function(each, idx){
				if (vsMenuId == each) favMenus.splice(idx, 1);
				else favMenus.push(each);
			});
		}
		
	}
	
	localStorage.setItem(AppProperties.PROJECT_NM + "favMenus", JSON.stringify(favMenus));
	
	var rootAppIns = app.getRootAppInstance();
	if(rootAppIns.hasAppMethod("favMnRefresh")) {
		rootAppIns.callAppMethod("favMnRefresh");
	}
}
