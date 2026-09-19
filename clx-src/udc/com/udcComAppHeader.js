/************************************************
 * AppHeader.js
 * Created at 2022. 10. 19. 오전 10:52:17.
 *
 * @author ryu
 ************************************************/
var util = createCommonUtil();

/*
 * 루트 컨테이너에서 load 이벤트 발생 시 호출.
 * 앱이 최초 구성된후 최초 랜더링 직후에 발생하는 이벤트 입니다.
 */
function onBodyLoad(e) {
	
	var hostApp = app.getHostAppInstance();
	
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
	
	//그리드 초기화
	//그리드ID가 지정된 경우가 아니면... 화면 내의 모든 그리드를 대상으로 초기화 작업을 수행한다.
	var vaGridIds = [];
	if (!ValueUtil.isNull(app.getAppProperty("gridIds"))) {
		vaGridIds = ValueUtil.split(app.getAppProperty("gridIds"), ",");
	} else {
		vaGridIds = util.Group.getAllChildrenByType(hostApp, "grid");
	}
	
	util.Grid.init(hostApp, vaGridIds);
	
	//프리폼 초기화
	var vaFreeformIds = [];
	if (!ValueUtil.isNull(app.getAppProperty("freeformIds"))) {
		vaFreeformIds = ValueUtil.split(app.getAppProperty("freeformIds"), ",");
	} else {
		var tempForms = util.Group.getAllChildrenByType(hostApp, "container", null, true);		
		var vaSearchBoxIds = ValueUtil.split(vsSearchBoxId, ",");
		tempForms.forEach(function(each) {
			/**@type cpr.controls.Container*/
			var form = each;
			if (vaSearchBoxIds.indexOf(form.id) == -1 && form.getLayout() instanceof cpr.controls.layouts.FormLayout) {
				if (util.Group.getBindDataSet(app, form) != null) {
					vaFreeformIds.push(form.id);
				}
			}
		});
	}
	util.FreeForm.init(hostApp, vaFreeformIds);
	
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
	
	/*
	 * TODO 화면 내 트리 컨트롤 초기화 로직을 수행하기 위해서는 아래 주석을 해제하십시오.
	 */
	// 트리 초기화
	/*
	 * var tempTrees = util.Group.getAllChildrenByType(hostApp, "tree", null, true);		
	 * util.Tree.init(app, tempTrees);
	 */
}

/*
 * "타이틀을 입력하세요" 아웃풋(optTit)에서 dblclick 이벤트 발생 시 호출.
 * 사용자가 컨트롤을 더블 클릭할 때 발생하는 이벤트.
 */
function onOptTitDblclick(e) {
	util.procEb6Privew(app);
}

/*
 * 루트 컨테이너에서 screen-change 이벤트 발생 시 호출.
 * 스크린 크기 변경 시 호출되는 이벤트.
 */
function onBodyScreenChange(e) {
	cpr.core.DeferredUpdateManager.INSTANCE.asyncExec(function() {
		var hostApp = app.getHostAppInstance();
		var vaCtrls = hostApp.getContainer().getAllRecursiveChildren(true);
		var vsScreenNm = e.screen.name;
		vaCtrls.filter(function(each) {
			return each.style.hasClass("subpage") || each.style.hasClass("content-box") || each.style.hasClass("card");
		}).forEach(function( /*cpr.controls.Container*/ each) {
			var voLayout = each.getLayout();
			if (voLayout instanceof cpr.controls.layouts.VerticalLayout || voLayout instanceof cpr.controls.layouts.FormLayout) {
				if (vsScreenNm == "mobile") {
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