/************************************************
 * ResponsiveVertical.module.js
 *
 * @author tomatosystem
 ************************************************/

/*************************************************************************
 * 확인사항
 * 1. RForm.prototype.ATTR_NM에 구성된 속성중 필요한 기능을 확인하고
 *    프로젝트 표준으로 그룹(버티컬 레이아웃)에 해당 속성명과 동일하게 사용자 속성을 구성하여 적용합니다.
 *    (eXBuilder6 > 프로젝트 표준 > 사용자 속성 정의)
 * 
 * 2. 반응형 모듈 적용은 event.module.js내 init EventBus에서 처리되며
 * 	    사용자 속성 "mobile-fit" or "tablet-fit" 값 존재여부에 따라 적용여부를 판단 합니다.
 *    적용된 반응형 객체는 그룹의 "_RVertical"(ex. grpForm["_RVertical"]) 속성으로 반환받을 수 있습니다.  
 * 
 * 3. 반응형 버티컬 모듈은  스크린에 따라 버티컬 레이아웃으로 구성된 그룹의 visible, margin, spacing 등을 적용된 값으로 변경합니다.
 *    (모바일 등 스크린 사이즈가 작은경우 여백을 줄이고 컨텐츠 영역을 확장시켜서 표시하기 위함)
 *************************************************************************/

/**
 * 반응형 버티컬 레이아웃 유틸리티
 * @param {cpr.controls.Container} container
 */
function RVertical(container) {
	this._container = container;
	this._appInstance = container.getAppInstance();
	if(!this._appInstance.isUDCInstance() && container.isAppContainer) {
		// 앱 컨테이너일 경우, uuid 저장 (_fillLayout 에서 내부 height (padding 제외 실제 사용 가능 높이) 를 확인)
		this._appInstance.getContainer().htmlAttr("uuid", this._appInstance.getContainer().uuid);
	}
	this._started = false;
	this._onScreenChange = this._onScreenChange.bind(this);
	/** @type {
	* 	  default : String[],
	* 	  tablet : String[],
	* 	  mobile : String[]
	* 	} 
	**/
	this._screenNms = this._setScreenNm();
}

/**
 * 반응형 버티컬 레이아웃 옵션 속성명<br>
 * (사용자 속성값의 기본타입은 String이며 valueType은 데이터 입력형식 확인을 위해 작성되었습니다.)
 */
RVertical.prototype.ATTR_NM = {
	/** <필수>모바일 : 반응형 버티컬 레이아웃 사용여부 <br>(valueType : Boolean, ex. true)*/
	ATTR_MOBILE_FIT : "mobile-fit",
	/** <필수>태블릿 : 반응형 버티컬 레이아웃 사용여부<br>(valueType : Boolean, ex. true)*/
	ATTR_TABLET_FIT : "tablet-fit",
	
	/** 모바일 : 버티컬 레이아웃 top margin<br>(valueType : Number, ex. 5)*/
	ATTR_MOBILE_T_MARGIN : "mobile-top-margin",
	/** 모바일 : 버티컬 레이아웃 right margin<br>(valueType : Number, ex. 5)*/
	ATTR_MOBILE_R_MARGIN : "mobile-right-margin",
	/** 태블릿 : 버티컬 레이아웃 bottom margin<br>(valueType : Number, ex. 5)*/
	ATTR_MOBILE_B_MARGIN : "mobile-bottom-margin",
	/** 태블릿 : 버티컬 레이아웃 left margin<br>(valueType : Number, ex. 5)*/
	ATTR_MOBILE_L_MARGIN : "mobile-left-margin",
	/** 태블릿 : 버티컬 레이아웃 spacing<br>(valueType : Number, ex. 10)*/
	ATTR_MOBILE_SPACING : "mobile-spacing",
	
	/** 태블릿 : 버티컬 레이아웃 top margin<br>(valueType : Number, ex. 5)*/
	ATTR_TABLET_T_MARGIN : "tablet-top-margin",
	/** 태블릿 : 버티컬 레이아웃 right margin<br>(valueType : Number, ex. 5)*/
	ATTR_TABLET_R_MARGIN : "tablet-right-margin",
	/** 태블릿 : 버티컬 레이아웃 bottom margin<br>(valueType : Number, ex. 5)*/
	ATTR_TABLET_B_MARGIN : "tablet-bottom-margin",
	/** 태블릿 : 버티컬 레이아웃 left margin<br>(valueType : Number, ex. 5)*/
	ATTR_TABLET_L_MARGIN : "tablet-left-margin",	
	/** 태블릿 : 버티컬 레이아웃 spacing<br>(valueType : Number, ex. 10)*/
	ATTR_TABLET_SPACING : "tablet-spacing",
	
	/** 모바일 : 버티컬 레이아웃 숨김여부<br>(valueType : Boolean, true or false)*/	
	ATTR_HIDE_ON_MOBILE : "hide-on-mobile",
	/** 태블릿 : 버티컬 레이아웃 숨김여부<br>(valueType : Boolean, true or false)*/	
	ATTR_HIDE_ON_TABLET : "hide-on-tablet",
	
	/** 제외 : 버티컬레이아웃 내 자식 컨트롤 확인 ⇒ 반응형 버티컬레이아웃 내부 SlidePaginition 이 포함되어 있는 경우, 페이지니션 컨트롤은 제외 */
	ATTR_SLIDE_PAGE_ITEM : "slide-page-item"	
}

/**
 * 타입에 따른 스크린 명칭 구성
 */
RVertical.prototype._setScreenNm = function(){
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

RVertical.prototype._backup = function() {
	this._originalLayout = this._container.getLayout();
}

/** @type cpr.controls.layouts.VerticalLayout */
RVertical.prototype._originalLayout = null;

RVertical.prototype._transform = function() {
	var that = this;
	if(that._container.disposed) return;
	
	var originalLayout = this._originalLayout;
	var layout = new cpr.controls.layouts.VerticalLayout();
	this._container.setLayout(layout);
	
	layout.topMargin = originalLayout.topMargin;
	layout.rightMargin = originalLayout.rightMargin;
	layout.bottomMargin = originalLayout.bottomMargin;
	layout.leftMargin = originalLayout.leftMargin;
	layout.spacing = originalLayout.spacing;
	layout.scrollable = originalLayout.scrollable;
	
	/** @type String */
	var tMargin;
	
	/** @type String */
	var rMargin;
	
	/** @type String */
	var bMargin;
	
	/** @type String */
	var lMargin;
	
	/** @type String */
	var spacing;
	
	var vsTargetScrnName = this._container.getAppInstance().targetScreen.name;
	if(this._screenNms["mobile"].indexOf(vsTargetScrnName) != -1) {
		
		tMargin = parseInt(this._container.userAttr(this.ATTR_NM.ATTR_MOBILE_T_MARGIN) || layout.topMargin);
		rMargin = parseInt(this._container.userAttr(this.ATTR_NM.ATTR_MOBILE_R_MARGIN) || layout.rightMargin);
		bMargin = parseInt(this._container.userAttr(this.ATTR_NM.ATTR_MOBILE_B_MARGIN) || layout.bottomMargin);
		lMargin = parseInt(this._container.userAttr(this.ATTR_NM.ATTR_MOBILE_L_MARGIN) || layout.leftMargin);
		spacing = parseInt(this._container.userAttr(this.ATTR_NM.ATTR_MOBILE_SPACING) || layout.spacing);
		layout.distribution = "fill";
		
		/* 사용자 속성 "hide-on-mobile"의 값이 true면 visible false 처리 */
		if (this._container.userAttr(this.ATTR_NM.ATTR_HIDE_ON_MOBILE) == "true") {
			this._container.visible = false;
		} else {
			this._container.visible = true;
		}
		
	} else if(this._screenNms["tablet"].indexOf(vsTargetScrnName) != -1) {
		
		tMargin = parseInt(this._container.userAttr(this.ATTR_NM.ATTR_TABLET_T_MARGIN) || layout.topMargin);
		rMargin = parseInt(this._container.userAttr(this.ATTR_NM.ATTR_TABLET_R_MARGIN) || layout.rightMargin);
		bMargin = parseInt(this._container.userAttr(this.ATTR_NM.ATTR_TABLET_B_MARGIN) || layout.bottomMargin);
		lMargin = parseInt(this._container.userAttr(this.ATTR_NM.ATTR_TABLET_L_MARGIN) || layout.leftMargin);
		spacing = parseInt(this._container.userAttr(this.ATTR_NM.ATTR_TABLET_SPACING) || layout.spacing);
		layout.distribution = "fill";
		
		/* 사용자 속성 "hide-on-tablet"의 값이 true면 visible false 처리 */
		if (this._container.userAttr(this.ATTR_NM.ATTR_HIDE_ON_TABLET) == "true") {
			this._container.visible = false;
		} else {
			this._container.visible = true;
		}
	} 
	
	layout.topMargin = tMargin;
	layout.rightMargin = rMargin;
	layout.bottomMargin = bMargin;
	layout.leftMargin = lMargin;
	layout.spacing = spacing;
};

/**
 * 루트 레이아웃이 버티컬 레이아웃인 경우, 
 * 작업영역(grpData) 의 높이를 화면 가득 차도록 설정한다.
 */
RVertical.prototype._fillLayout = function () {
	
	var voAppIns = this._appInstance;
	var voAppContainer = voAppIns.getContainer();
	if(voAppIns.disposed || voAppContainer == null) return;
	var voContainerLayout = voAppContainer.getLayout();
	
	if(!(voContainerLayout instanceof cpr.controls.layouts.VerticalLayout)) return;
	
	// 현재 스크린 화면명
	var vsScreenNm = voAppIns.targetScreen.name;
			
	var vsFixLayout = voAppContainer.userData("fixLayout");
	if(vsFixLayout == "Y") return;
	
	/** 앱 컨테이너가 버티컬 레이아웃이고 작업영역 그룹의 ID가 grpData인 경우
	    높이를 화면에 꽉차도록 재정의함 */
	var vcFillLayout;
	var flExclusionHeight = 0;
	
	var vaChildren = voAppContainer.getChildren();

	/*
	 * TODO 프로젝트 별 커스터마이징 
	 * 
	 * 작업영역 그룹 선정
	 * 우선순위1 ) fillLayout=Y 인 컨트롤
	 * 우선순위2 ) udcComAppHeader 의 groupBoxIdx 에 작성된 첫번째 컨트롤(default grpData)
	 */
	
	// 우선순위1 ) fillLayout=Y 인 컨트롤
	var vaFillLayout = vaChildren.filter(function(each) {
		return each.userAttr("fillLayout") == "Y";
	});
	
	// 우선순위2 ) grpData
	var vaGroupBoxIds = null;
	vaChildren.filter(function(child){
		return child instanceof udc.com.udcComAppHeader;
	}).forEach(function(/* udc.com.udcComAppHeader */ appHeader){
		var vsGroupBoxIds =  appHeader.getAppProperty("groupBoxIds");
		if(!ValueUtil.isNull(vsGroupBoxIds)) {
			vaGroupBoxIds = vsGroupBoxIds.split(",");
		}
	});
	
	if (vaFillLayout.length > 0) {
		vcFillLayout = vaFillLayout[0];
	} else if (!ValueUtil.isNull(vaGroupBoxIds)) {
		vcFillLayout = voAppIns.lookup(vaGroupBoxIds[0]);
	} else {
		return;
	}
	
	if(!vcFillLayout) return;
	
	vaChildren.forEach(function(ctrl) {
		if (ctrl.id != vcFillLayout.id) {
			
			// visible false 제외
			if(!ctrl.isShowing()) return;
			
			// 플로팅 컨트롤 제외
			if (ctrl.isFloated()) return;
			
			var vnHeight = ctrl.getActualRect(true).height;
			
			if (ValueUtil.isNull(vnHeight) || vnHeight <= 0) {
				var voChildConst = voAppContainer.getConstraint(ctrl);
				vnHeight = voChildConst.height.replace("px", "");
			}
			
			flExclusionHeight += ValueUtil.fixNumber(vnHeight);
		} else {
			var voConstGrpData = voAppContainer.getConstraint(ctrl);
			if (!ctrl.userData("origin.autoSize")) {
				ctrl.userData("origin.autoSize", ValueUtil.fixNull(voConstGrpData.autoSize));
			}
			
			/** TODO 커스터 마이징
			    
		    스크린 사이즈에 따라 grpData영역 자동크기 적용
		    모바일이 아닌 경우에만 처리하며 모바일에서는 스크롤을 생성하여 UI에 정의된 크기만큼 구성함
		    디바이스 체크가 필요한경우 that.util.isMobile()조건 추가	*/
			if (AppProperties.SCREEN_MOBILE_NM.indexOf(vsScreenNm) > -1) {
				if (ctrl.userData("origin.autoSize") == "none") {
					voAppContainer.updateConstraint(ctrl, {
						autoSize: "height"
					})
				}
				
				return false;
			} else {
				if (ctrl.userData("origin.autoSize") == "none") {
					voAppContainer.updateConstraint(ctrl, {
						autoSize: "none"
					})
				}
			}
			
			/* 컨텐츠에 따라 추가 height변경 필요시 구성 */
		}
	});
	
	if(!voAppContainer.getConstraint(vcFillLayout)) return;
	var vsGrpDataHeight = voAppContainer.getConstraint(vcFillLayout).height;
	if (!vsGrpDataHeight) {
		return;
	}
	var vnGrpDataHeight = vsGrpDataHeight.toString().replace("px", "");
	vnGrpDataHeight = ValueUtil.fixNumber(vnGrpDataHeight);
	
	// 보수적으로 Body 레이아웃 높이가 APP_FILL_SIZE_MIN_HEGIHT 을 넘으면 기존 유지(불필요시 제거 가능)	
	if (vnGrpDataHeight > ValueUtil.fixNumber(AppProperties.APP_FILL_SIZE_MIN_HEGIHT)) {
		voAppContainer.userData("fixLayout", "Y");
		return;
	}
	
	/** TODO 커스터 마이징 : 사이트별 UI 표준에 따라 보수적인 레이아웃 높이 구성*/
	var poConstraint = {};
	var mainHeight = voAppIns.getRootAppInstance().getActualRect().height;
	if (mainHeight >= AppProperties.APP_FILL_SIZE_MIN_HEGIHT) {
		var embHeight = voAppContainer.getConstraint(vcFillLayout).height + "";
		vcFillLayout.userAttr("origin.height", embHeight);
		
		var vnMainLayoutSpacing = voContainerLayout.spacing;
		var topMargin = parseInt((voContainerLayout.topMargin) || "0");
		var bottomMargin = parseInt((voContainerLayout.bottomMargin) || "0");
		
		var vnContainerHeight = voAppContainer.getActualRect().height; // 실제 appContainer height (padding 제외)
		var voContainerLayoutEl = document.querySelector("div[data-usr-uuid='"+voAppContainer.htmlAttr("uuid")+"'] .cl-layout-content");
		if(voContainerLayoutEl) {
			var computedStyle = getComputedStyle(voContainerLayoutEl);
			vnContainerHeight -= parseInt(computedStyle.paddingTop || "0");
			vnContainerHeight -= parseInt(computedStyle.paddingBottom || "0");
		}
		
		// vsFixHeight = 앱컨테이너 실제 사용 가능한 hieght - (top/bottom margin - grpData 제외 높이 - 모든 spacing)
		var vsFixheight = vnContainerHeight - (topMargin + bottomMargin + flExclusionHeight + vnMainLayoutSpacing * (vaChildren.length - 1));
		poConstraint = {
			minHeight : vsFixheight,
			height : vsFixheight
		}
	} else {
		var vsHeightPx = AppProperties.APP_FILL_SIZE_MIN_HEGIHT + "px";
		var vsFillLayoutOrgH = vcFillLayout.userAttr("origin.height");
		
		if (!ValueUtil.isNull(vsFillLayoutOrgH)) {
			vsHeightPx = vsFillLayoutOrgH;
		}
		poConstraint = {
			height: vsHeightPx
		};
	}
	
	voAppContainer.updateConstraint(vcFillLayout, poConstraint);
	voAppContainer.redraw();
	
	// updateConstraint 하면서 깜빡이는 현상이 없도록 UI 즉시 그리기
	cpr.core.DeferredUpdateManager.INSTANCE.update();
}

RVertical.prototype._restore = function() {
	this._container.setLayout(this._originalLayout);
	
	if(!this._container.disposed) {
		this._container.visible = true;
	}
};

RVertical.prototype.start = function() {
	if (this._started) {
		return;
	}
	this._backup();
	
	this._appInstance.addEventListener("screen-change", this._onScreenChange);
};

/**
 * @param {cpr.events.CScreenChangeEvent} e
 */
RVertical.prototype._onScreenChange = function(e) {
	var that = this;
	if(that._container.disposed) return;
	
	var vsScrnName = e.screen.name;
	
	if(this._screenNms["mobile"].indexOf(vsScrnName) != -1) {
		if (this._container.userAttr(this.ATTR_NM.ATTR_MOBILE_FIT) == "true") {
			this._transform();
		} else {
			this._restore();
		}
	} else if(this._screenNms["tablet"].indexOf(vsScrnName) != -1) {
		if (this._container.userAttr(this.ATTR_NM.ATTR_TABLET_FIT) == "true") {
			this._transform();
		} else {
			this._restore();
		}
	} else if(this._screenNms["default"].indexOf(vsScrnName) != -1) {
		if (this._container.userAttr(this.ATTR_NM.ATTR_HIDE_ON_MOBILE) == "false" || this._container.userAttr(this.ATTR_NM.ATTR_HIDE_ON_TABLET) == "false") {
			this._container.visible = false;
		} else {
			this._restore();
		}
	}
	
	if (typeof AppProperties !== 'undefined') {
		if(!ValueUtil.isNull(AppProperties.IS_APP_FILL_SIZE) && AppProperties.IS_APP_FILL_SIZE === true) {
			// IS_APP_FILL_SIZE=true 일 경우,
			// 루트 레이아웃이 버티컬 레이아웃일 경우, grpData 의 높이를 컨테츠 영역에 가득 차도록 사이즈 조절 
			var that =this;
			cpr.core.DeferredUpdateManager.INSTANCE.asyncExec(function(){
				that._fillLayout();
			});
		}
	}	
};

/**
 * 반응형 버티컬 레이아웃 적용
 * @param {cpr.controls.Container} container
 */
globals.makeVResponsive = function(container) {
	return new RVertical(container);
};