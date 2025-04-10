/************************************************
 * responsiveForm.module.js
 *
 * @author tomatosystem
 ************************************************/

/*************************************************************************
 * 확인사항
 * 1. RForm.prototype.ATTR_NM에 구성된 속성중 필요한 기능을 확인하고
 *    프로젝트 표준으로 그룹(폼 레이아웃) 및 일반 컨트롤에 해당 속성명과 동일하게 사용자 속성을 구성하여 적용합니다.
 *    (eXBuilder6 > 프로젝트 표준 > 사용자 속성 정의)
 * 
 * 2. 반응형 모듈 적용은 event.module.js내 init EventBus에서 처리되며
 * 	    사용자 속성 "mobile-column-count" 또는  "tablet-column-count" 값 존재여부에 따라 적용여부를 판단 합니다.
 *    적용된 반응형 객체는 그룹의 "_RForm"(ex. grpForm["_RForm"]) 속성으로 반환받을 수 있습니다.  
 * 
 * 3. 반응형 모듈 사용시 적용된 그룹(폼 레이아웃)에서
 *    app.lookup("grpForm").getLayout().setColumnVisible(), setRowVisible() API를 사용하는 경우
 *    반응형이 적용된 태블릿, 모바일 화면에서는 폼 레이아웃의 Constraints가 변경되기 때문에
 *    util.FreeForm.setColumnVisible(), setRowVisible() 공통 API를 사용하시기 바랍니다.
 *************************************************************************/

/************************************************
 * 반응형 모듈 옵션
 ************************************************/

// 폼 레이아웃에서 라벨 영역에 대한 아웃풋 클래스 명
var msLabelClass = "label";

/**
 * 반응형 폼 레이아웃 유틸리티.
 * @param {cpr.controls.Container} container 폼 레이아웃을 가진 컨테이너.
 */
function RForm(container) {
	this._container = container;
	this._columnSettings = {};
	this._onScreenChange = this._onScreenChange.bind(this);
	/** @type {
	* 	 default: String[], 
	* 	 tablet: String[], 
	* 	 mobile: String[]
	* 	} 
	**/
	this._screenNms = this._setScreenNm();
	this._started = false;
}

/**
 * 반응형 폼 레이아웃 옵션 속성명<br>
 * (사용자 속성값의 기본타입은 String이며 valueType은 데이터 입력형식 확인을 위해 작성되었습니다.)
 */
RForm.prototype.ATTR_NM = {
	/** <필수>모바일 : 폼 레이아웃의 colCount<br>(valueType : Number, ex. 2)*/
	ATTR_MOBILE_COLUMN_COUNT	: "mobile-column-count",	
	/** 모바일 : 폼 레이아웃 left, right margin<br>(valueType : Number, ex. 5)*/
	ATTR_MOBILE_H_MARGIN		: "mobile-horizontal-margin",
	/** 모바일 : 폼 레이아웃 top, bottom margin<br>(valueType : Number, ex. 5)*/
	ATTR_MOBILE_V_MARGIN		: "mobile-vertical-margin",
	/** 모바일 : 폼 레이아웃 자식 컨트롤 중 모바일 환경에서 숨겨질 컨트롤<br>(valueType : Boolean, ex. true, 해당 속성은 폼 레이아웃 자식 컨트롤의 사용자 속성에 지정)*/	
	ATTR_HIDE_ON_MOBILE			: "hide-on-mobile",
	/** 모바일 : 자동높이 사용시 최소 높이<br>(valueType : Number, ex. 150, 해당 속성은 폼 레이아웃 자식 컨트롤의 사용자 속성에 지정)*/		
	ATTR_MOBILE_MIN_HEIGHT		: "mobile-min-height",
	/** 모바일 : 폼 레이아웃 내 컨트롤 배치 순서 정의<br>(valueType : Number, 해당 속성은 폼 레이아웃 자식 컨트롤의 사용자 속성에 지정해야 하며 모든 자식 컨트롤에 순서를 지정해야 합니다)*/	
	ATTR_MOBILE_INDEX			: "mobile-index",
	/** 모바일 : 폼레이아웃 내 horizontalSpacing 적용<br>(valueType : Number, ex. 10)*/
	ATTR_MOBILE_H_SPACING : "mobile-h-spacing",
	/** 모바일 : 폼레이아웃 내 verticalSpacing 적용<br>(valueType : Number, ex. 10)*/
	ATTR_MOBILE_V_SPACING : "mobile-v-spacing",
	
	/** <필수>태블릿 : 폼 레이아웃의 colCount<br>(valueType : Number, ex. 4)*/
	ATTR_TABLET_COLUMN_COUNT	: "tablet-column-count",
	/** 태블릿 : 폼 레이아웃 left, right margin<br>(valueType : Number, ex. 10)*/
	ATTR_TABLET_H_MARGIN		: "tablet-horizontal-margin",
	/** 태블릿 : 폼 레이아웃 top, bottom margin<br>(valueType : Number, ex. 15)*/
	ATTR_TABLET_V_MARGIN		: "tablet-vertical-margin",
	/** 태블릿 : 폼 레이아웃 자식 컨트롤 중 태블릿 환경에서 숨겨질 컨트롤<br>(valueType : Boolean, ex. true, 해당 속성은 폼 레이아웃 자식 컨트롤의 사용자 속성에 지정)*/	
	ATTR_HIDE_ON_TABLET			: "hide-on-tablet",
	/** 태블릿 : 자동높이 사용시 최소 높이<br>(valueType : Number, ex. 150, 해당 속성은 폼 레이아웃 자식 컨트롤의 사용자 속성에 지정)*/
	ATTR_TABLET_MIN_HEIGHT		: "tablet-min-height",
	/** 태블릿 : 폼 레이아웃 내 컨트롤 배치 순서 정의<br>(valueType : Number, 해당 속성은 폼 레이아웃 자식 컨트롤의 사용자 속성에 지정해야 하며 모든 자식 컨트롤에 순서를 지정해야 합니다)*/	
	ATTR_TABLET_INDEX			: "tablet-index",
	/** 태블릿 : 폼레이아웃 내 horizontalSpacing 적용<br>(valueType : Number, ex. 10)*/
	ATTR_TABLET_H_SPACING : "tablet-h-spacing",
	/** 태블릿 : 폼레이아웃 내 verticalSpacing 적용<br>(valueType : Number, ex. 10)*/
	ATTR_TABLET_V_SPACING : "tablet-v-spacing",
	
	/** 공통 : 자동높이 사용 여부<br>(valueType : Boolean)*/
	ATTR_NEEDS_AUTO_HEIGHT : "needs-auto-height",	
	/** 공통 : 테이블형 폼 레이아웃 피벗형태 전환 (valueType : Enumeration, default : horizontal(or vertical))*/	
	ATTR_COLLAPSE_DIRECTION : "collapse-direction",	
	/** 공통 : 모바일, 태블릿에서 적용될 스타일 클래스<br>(valueType : String)*/	
	ATTR_CUSTOM_CLASS : "custom-class-name",
	
	/** 제외 : 폼레이아웃 내 자식 컨트롤 확인 ⇒ 반응형 폼레이아웃 내부 SlidePaginition 이 포함되어 있는 경우, 페이지니션 컨트롤은 제외 */
	ATTR_SLIDE_PAGE_ITEM : "slide-page-item"
}

/**
 * 타입에 따른 스크린 명칭 구성
 */
RForm.prototype._setScreenNm = function(){
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
 * 화면 크기별 컬럼 설정
 * @type {{[key:string]:number}}
 */
RForm.prototype._columnSettings = {};

/** @type cpr.controls.layouts.FormLayout */
RForm.prototype._originalLayout = null;

/** @type cpr.utils.ObjectMap */
RForm.prototype._originalConstraints = null;

/** @type cpr.core.AppInstance */
RForm.prototype._appInstance = null;

/**
 * 스크린별 컬럼 정보를 구성합니다.
 * @param {String} screenName 스크린 명
 * @param {Number} colCount 컬럼 수
 */
RForm.prototype.setColumnSettings = function(screenName, colCount) {
	this._columnSettings[screenName] = colCount;
};

/**
 * 초기 상태 백업.
 */
RForm.prototype._backup = function(pbDraw) {
	var that = this;
	if(that._container.disposed) return;
	
	// 원본 Constraint
	this._originalConstraints = new cpr.utils.ObjectMap();
	
	var children = this._container.getChildren().filter(function(each){
		return (each.userAttr(that.ATTR_NM.ATTR_SLIDE_PAGE_ITEM) != "true");
	});
	for (var idx = 0; idx < children.length; idx++) {
		var eachChild = children[idx];
		var constraint = this._container.getConstraint(eachChild);
		this._originalConstraints.put(eachChild, constraint);
	}
	
	if(!pbDraw) {
		// 최초 한번만 originLayout 저장
		// before-draw 할때는 originLayout 이 업데이트 되지 않도록 설정함
		this._originalLayout = this._container.getLayout();
	}
};

RForm.prototype.start = function() {
	if (this._started) {
		return;
	}
	
	this._backup();
	this._appInstance = this._container.getAppInstance();
	this._appInstance.addEventListener("screen-change", this._onScreenChange);
	this._container.addEventListenerOnce("dispose", this.stop.bind(this));

	var that = this;
	/*
	 * 자식 컨트롤이 동적으로 폼 레이아웃 내에 추가된 경우, 변경된 자식컨트롤에 대해 반응형을 재적용합니다.
	 * addChild() 또는 insertChild() 후 this._container.redraw() 를 호출해야 아래 이벤트 리스너가 동작합니다.
	 */
	this._container.addEventListener("before-draw", function(e){
		cpr.core.DeferredUpdateManager.INSTANCE.asyncExec(function(){
			var children = that._container.getChildren().filter(function(each){
				return (each.userAttr(that.ATTR_NM.ATTR_SLIDE_PAGE_ITEM) != "true");
			});
			
			// 폼레이아웃 내 자식이 _originalConstraint 와 다를 경우에만 다시 _backup 수행
			var isEqual = children.every(function(each){
				if(that._originalConstraints.keys().indexOf(each) != -1) {
					return true;
				} else {
					return false;
				}
			});
			if(!isEqual) {
				that._backup(true);

				that._appInstance.dispatchEvent(new cpr.events.CScreenChangeEvent({
					name: that._appInstance.targetScreen.name
				}))
			}
		});
	});
	
	this._started = true;
};

RForm.prototype.stop = function() {
	if (!this._started) {
		return;
	}
	
	if (this._appInstance) {
		this._appInstance.removeEventListener("screen-change", this._onScreenChange);
		this._appInstance = null;
	}
	
	this._started = false;
};

RForm.prototype._restore = function() {
	var that = this;
	if(that._container.disposed) return;
	
	this._container.setLayout(this._originalLayout);
	
	/** @type cpr.controls.VisibleUIControl[] */
	var children = this._container.getChildren().filter(function(each){
		return (each.userAttr(that.ATTR_NM.ATTR_SLIDE_PAGE_ITEM) != "true");
	});

	for (var idx = 0; idx < children.length; idx++) {
		var eachChild = children[idx];
		
		// 사용자 속성 옵션에  의해 숨겨진 컨트롤은 표시 원복
		if(eachChild.userAttr(this.ATTR_NM.ATTR_HIDE_ON_MOBILE) == "true" 
			|| eachChild.userAttr(this.ATTR_NM.ATTR_HIDE_ON_TABLET) == "true"
			|| eachChild.userAttr("_layoutVisible") == "false"){
			eachChild.visible = true;
		}
		
		this._container.replaceConstraint(eachChild, this._originalConstraints.get(eachChild));
	}
	
	this._clearCollapseClasses();
}

/**
 * 
 * @param {cpr.events.CScreenChangeEvent} e
 */
RForm.prototype._onScreenChange = function(e) {
	var that = this;
	if(that._container.disposed) return;
	
	var colSettings = this._columnSettings[e.screen.name];
	if (colSettings == null || colSettings <= 0) {
		this._restore();
	} else {
		this._transform(colSettings);
	}
};

/**
 * @return {cpr.controls.VisibleUIControl[]}
 */
RForm.prototype._getSortedChildren = function(paChild) {
	var that = this;
	var children = paChild;
			
	var useVerticalFirstSort = this._container.userAttr(this.ATTR_NM.ATTR_COLLAPSE_DIRECTION) == "vertical";
	
	/** @type String */
	var explictIndexAttribute = null;
	var vsContainerTargetScrn = this._container.getAppInstance().targetScreen.name;
	if(this._screenNms["mobile"].indexOf(vsContainerTargetScrn) != -1) {
		explictIndexAttribute = this.ATTR_NM.ATTR_MOBILE_INDEX;
	} else if(this._screenNms["tablet"].indexOf(vsContainerTargetScrn) != -1) {
		explictIndexAttribute = this.ATTR_NM.ATTR_TABLET_INDEX;
	}
	
	return children.sort(function( /* cpr.controls.UIControl*/ a, /* cpr.controls.UIControl*/ b) {
		var indexA = parseInt(a.userAttr(explictIndexAttribute || "") || "9999");
		var indexB = parseInt(b.userAttr(explictIndexAttribute || "") || "9999");
		
		var explictIndexGap = indexA - indexB;
		if (explictIndexGap !== 0) {
			return explictIndexGap;
		}
		
		/** @type cpr.controls.layouts.FormConstraint */
		var constA = that._originalConstraints.get(a);
		
		/** @type cpr.controls.layouts.FormConstraint */
		var constB = that._originalConstraints.get(b);
		
		var rowDiff = constA.rowIndex - constB.rowIndex;
		var colDiff = constA.colIndex - constB.colIndex;
		
		if (useVerticalFirstSort) {
			return colDiff !== 0 ? colDiff : rowDiff;
		} else {
			return rowDiff !== 0 ? rowDiff : colDiff;
		}
	});
};

/**
 * 정의된 컬럼수에 따라 레이아웃 구조를 변경합니다.
 * @param {Number} colCount 컬럼수
 */
RForm.prototype._transform = function(colCount) {
	var that = this;
	if(that._container.disposed) return;
	
	var layout = new cpr.controls.layouts.FormLayout();
	that._container.setLayout(layout);
	
	layout.topMargin = that._originalLayout.topMargin;
	layout.rightMargin = that._originalLayout.rightMargin;
	layout.bottomMargin = that._originalLayout.bottomMargin;
	layout.leftMargin = that._originalLayout.leftMargin;
	layout.horizontalSpacing = that._originalLayout.horizontalSpacing;
	layout.verticalSpacing = that._originalLayout.verticalSpacing;
	layout.horizontalSeparatorWidth = that._originalLayout.horizontalSeparatorWidth;
	layout.horizontalSeparatorClass = that._originalLayout.horizontalSeparatorClass;
	layout.verticalSeparatorWidth = that._originalLayout.verticalSeparatorWidth;
	layout.verticalSeparatorClass = that._originalLayout.verticalSeparatorClass;
	
	/* 컨트롤별 visible관련 사용자 속성 초기화 및 행/열 visible 정보 처리*/
	var children = that._container.getChildren().filter(function(each){
		return (each.userAttr(that.ATTR_NM.ATTR_SLIDE_PAGE_ITEM) != "true");
	});
	children.forEach(function(each){
		var childConst = that._originalConstraints.get(each);
		
		// hide 속성값으로 인한 visible 처리 초기화
		if(each.userAttr("_hideOnTablet") == "true" || each.userAttr("_hideOnMobile") == "true"){
			each.userAttr("_hideOnTablet", "false");
			each.userAttr("_hideOnMobile", "false");
			each.visible = true;
		}
		
		// 원본 레이아웃에서 hidden 처리된 행, 열에 있는 컨트롤 예외처리		
		if(!that._originalLayout.isColumnVisible(childConst.colIndex) || !that._originalLayout.isRowVisible(childConst.rowIndex)){
			each.userAttr("_layoutVisible", "false");
		}
	});
	
	var sortedChildren  = that._getSortedChildren(children);
	var visibleChildren = [];
	var hiddenChildren  = [];
	
	var vsTargetScrnName = that._appInstance.targetScreen.name;
	if(this._screenNms["mobile"].indexOf(vsTargetScrnName) != -1) {
		
		if (that._container.userAttr(that.ATTR_NM.ATTR_MOBILE_H_MARGIN)) {
			layout.rightMargin = that._container.userAttr(that.ATTR_NM.ATTR_MOBILE_H_MARGIN);
			layout.leftMargin = that._container.userAttr(that.ATTR_NM.ATTR_MOBILE_H_MARGIN);
			layout.horizontalSpacing = parseInt(that._container.userAttr(that.ATTR_NM.ATTR_MOBILE_H_MARGIN)) * 2;
		}
		if (that._container.userAttr(that.ATTR_NM.ATTR_MOBILE_V_MARGIN)) {
			layout.topMargin = that._container.userAttr(that.ATTR_NM.ATTR_MOBILE_V_MARGIN);
			layout.bottomMargin = that._container.userAttr(that.ATTR_NM.ATTR_MOBILE_V_MARGIN);
			layout.verticalSpacing = parseInt(that._container.userAttr(that.ATTR_NM.ATTR_MOBILE_V_MARGIN)) * 2;
		}

		if (that._container.userAttr(that.ATTR_NM.ATTR_MOBILE_V_SPACING)) {
			layout.verticalSpacing = that._container.userAttr(that.ATTR_NM.ATTR_MOBILE_V_SPACING);
		}
		
		if (that._container.userAttr(that.ATTR_NM.ATTR_MOBILE_H_SPACING)) {
			layout.horizontalSpacing = that._container.userAttr(that.ATTR_NM.ATTR_MOBILE_H_SPACING);
		}
		
		visibleChildren = sortedChildren.filter(function( /* cpr.controls.VisibleUIControl */ each) {
			return each.visible && each.userAttr(that.ATTR_NM.ATTR_HIDE_ON_MOBILE) != "true" && each.userAttr("_layoutVisible") != "false";
		});
		
		hiddenChildren = sortedChildren.filter(function( /* cpr.controls.VisibleUIControl */ each) {
			var attrHideOnMobile = each.userAttr(that.ATTR_NM.ATTR_HIDE_ON_MOBILE);
			if (each.visible && attrHideOnMobile == "true") {
				each.userAttr("_hideOnMobile", "true");
			}
			return !each.visible || attrHideOnMobile == "true" || each.userAttr("_layoutVisible") == "false";
		}).forEach(function( /* cpr.controls.VisibleUIControl */ each) {
			each.visible = false;
		});
		
	} else if(this._screenNms["tablet"].indexOf(vsTargetScrnName) != -1) {
		
		if (that._container.userAttr(that.ATTR_NM.ATTR_TABLET_H_MARGIN)) {
			layout.rightMargin = that._container.userAttr(that.ATTR_NM.ATTR_TABLET_H_MARGIN);
			layout.leftMargin = that._container.userAttr(that.ATTR_NM.ATTR_TABLET_H_MARGIN);
			layout.horizontalSpacing = parseInt(that._container.userAttr(that.ATTR_NM.ATTR_TABLET_H_MARGIN)) * 2;
		}
		if (that._container.userAttr(that.ATTR_NM.ATTR_TABLET_V_MARGIN)) {
			layout.topMargin = that._container.userAttr(that.ATTR_NM.ATTR_TABLET_V_MARGIN);
			layout.bottomMargin = that._container.userAttr(that.ATTR_NM.ATTR_TABLET_V_MARGIN);
			layout.verticalSpacing = parseInt(that._container.userAttr(that.ATTR_NM.ATTR_TABLET_V_MARGIN)) * 2;
		}
		
		if (that._container.userAttr(that.ATTR_NM.ATTR_TABLET_V_SPACING)) {
			layout.verticalSpacing = that._container.userAttr(that.ATTR_NM.ATTR_TABLET_V_SPACING);
		}
		
		if (that._container.userAttr(that.ATTR_NM.ATTR_TABLET_H_SPACING)) {
			layout.horizontalSpacing = that._container.userAttr(that.ATTR_NM.ATTR_TABLET_H_SPACING);
		}
			
		visibleChildren = sortedChildren.filter(function( /* cpr.controls.VisibleUIControl */ each) {
			return each.visible && each.userAttr(that.ATTR_NM.ATTR_HIDE_ON_TABLET) != "true" && each.userAttr("_layoutVisible") != "false";
		});
		
		hiddenChildren = sortedChildren.filter(function( /* cpr.controls.VisibleUIControl */ each) {
			var attrHideOnTablet = each.userAttr(that.ATTR_NM.ATTR_HIDE_ON_TABLET);
			if (each.visible && attrHideOnTablet == "true") {
				each.userAttr("_hideOnTablet", "true");
			}
			return !each.visible || attrHideOnTablet == "true" || each.userAttr("_layoutVisible") == "false";
		}).forEach(function( /* cpr.controls.VisibleUIControl */ each) {
			each.visible = false;
		});
	}
	
	var columnDivisions = that._originalLayout.getColumnDivisions();
	var colSettings = columnDivisions.slice(0, colCount);
	
	// 모바일이나 태블릿이 컬럼 개수가 더 많은 경우 처리.
	while (colSettings.length < colCount) {
		colSettings.push(copyDiv(colSettings[colSettings.length - 1]));
	}
	
	var rowDiv = new cpr.controls.layouts.FormDivision("1fr");
	var vnRowMinLength = 10;
	
	var rowDivisions = that._originalLayout.getRowDivisions();
	colSettings.forEach(function( /* cpr.controls.layouts.FormDivision */ each, idx) {
		if (rowDivisions[idx]) {
			var vsLengthExp = rowDivisions[idx].lengthExpression;
			vsLengthExp = vsLengthExp.replace("px", "");
			
			var vnLengthExp = parseInt(vsLengthExp);
			if (vnRowMinLength < vnLengthExp) {
				vnRowMinLength = vnLengthExp;
			}
		}
	});
	
	if (vnRowMinLength > 10) {
		rowDiv.minLength = vnRowMinLength;
	}
	
	// 세로 컬랩스 처리.
	if (that._container.userAttr(that.ATTR_NM.ATTR_COLLAPSE_DIRECTION) == "vertical") {
		var rowDivisions = that._originalLayout.getRowDivisions();
		colSettings.forEach(function( /* cpr.controls.layouts.FormDivision */ each, idx) {
			each.shades = rowDivisions[idx].shades;
			each.customShadeColor = rowDivisions[idx].customShadeColor;
		});
		
		rowDiv.shades = false;
	}
	
	var lastColDiv = colSettings[colSettings.length - 1];
	lastColDiv.lengthExpression = "1fr";
	lastColDiv.shades = false;
	colSettings.forEach(function(each){
		if(!each.visible) each.visible = true;
	});
	layout.setColumnDivisions(colSettings);
	layout.scrollable = true;
	
	var numberOfColumns = colSettings.length;
		
	var rowIndex = 0;
	var columnIndex = 0;
	var rowNeedsAutoHeight = false;
	
	/** @type cpr.controls.layouts.FormDivision[] */
	var rows = [];
	
	for (var idx = 0; idx < visibleChildren.length; idx++) {
		if (rows[rowIndex] == null) {
			rows.push(copyDiv(rowDiv));
		}
		
		/**
		 * 현재 행에 남은 컬럼 수.
		 */
		var leftColumnsInCurrentLine = numberOfColumns - columnIndex;
		var each = visibleChildren[idx];
		each.visible = true;
		
		/**
		 * 원본 컨스트레인트.
		 * @type cpr.controls.layouts.FormConstraint
		 */
		var originalConstraint = that._originalConstraints.get(each);
		var originalColSpan = originalConstraint.colSpan || 1;
		
		var colSpan = Math.min(originalColSpan, leftColumnsInCurrentLine);
		var ignoreLayoutSpacing = originalConstraint.ignoreLayoutSpacing;
		
		/* 
		 * 커스텀 옵션 : 조회버튼 그룹을 최하단 우측 영역에 배치합니다.
		 *           		  조회버튼 식별 로직은 프로젝트 구조에 따라 구성해야 합니다.
		 */		
		if(ValueUtil.isStartWith(each.id, AppProperties.SEARCH_BTN_ID)) {
			// 조회버튼 식별 로직
			this._container.replaceConstraint(each, {
				rowIndex: rowIndex,
				colIndex: numberOfColumns - 1,
				colSpan: colSpan,
				ignoreLayoutSpacing: ignoreLayoutSpacing,
				/* 우측 정렬 옵션(horizontalAlign) : 일반적으로 width 를 함께 지정하여 우측 정렬 설정하는 것을 권장 */
				horizontalAlign : "right",
				width : originalConstraint.width,
			});
		} else {
			this._container.replaceConstraint(each, {
				rowIndex: rowIndex,
				colIndex: columnIndex,
				colSpan: colSpan,
				ignoreLayoutSpacing: ignoreLayoutSpacing
			});
		}
		
		// 커스텀 옵션 사용시 하위 스코프에 있는 that._container.replaceConstraint 로직은 제거 합니다.
//		that._container.replaceConstraint(each, {
//			rowIndex: rowIndex,
//			colIndex: columnIndex,
//			colSpan: colSpan,
//			ignoreLayoutSpacing: ignoreLayoutSpacing
//		});
		this._container.reorderChild(each, idx); // 표시순으로 컨트롤 순서 정렬
		
		if (each.userAttr(that.ATTR_NM.ATTR_NEEDS_AUTO_HEIGHT) == "true") {
			rows[rows.length - 1].autoSizing = true;
			rows[rows.length - 1].lengthExpression = "25px";
		}
		
		
		if (this._screenNms["mobile"].indexOf(vsTargetScrnName) != -1) {
			if (each.userAttr(that.ATTR_NM.ATTR_MOBILE_MIN_HEIGHT)) {
				var eachMinLength = parseInt(each.userAttr(that.ATTR_NM.ATTR_MOBILE_MIN_HEIGHT));
				var knownRow = rows[rows.length - 1];
				if (knownRow.lengthExpression.match(".*fr$")) {
					knownRow.minLength = Math.max(knownRow.minLength, eachMinLength);
				} else {
					knownRow.lengthExpression = Math.max(parseInt(knownRow.lengthExpression), eachMinLength) + "px";
				}
			}
		} else if (this._screenNms["tablet"].indexOf(vsTargetScrnName) != -1) {
			if (each.userAttr(that.ATTR_NM.ATTR_TABLET_MIN_HEIGHT)) {
				
				var eachMinLength = parseInt(each.userAttr(that.ATTR_NM.ATTR_TABLET_MIN_HEIGHT));
				var knownRow = rows[rows.length - 1];
				if (knownRow.lengthExpression.match(".*fr$")) {
					knownRow.minLength = Math.max(knownRow.minLength, eachMinLength);
				} else {
					knownRow.lengthExpression = Math.max(parseInt(knownRow.lengthExpression), eachMinLength) + "px";
				}
			}
		}
		
		/* 스크린이 mobile or tablet 일 때, 라벨 영역 음영 처리 */
		if(this._screenNms["mobile"].indexOf(that._container.getAppInstance().targetScreen.name) != -1 && that._container.userAttr(that.ATTR_NM.ATTR_MOBILE_COLUMN_COUNT) == 1
		|| this._screenNms["tablet"].indexOf(that._container.getAppInstance().targetScreen.name) != -1 && that._container.userAttr(that.ATTR_NM.ATTR_TABLET_COLUMN_COUNT) == 1) {
			if (visibleChildren[idx] instanceof cpr.controls.Output && _.contains(visibleChildren[idx].style.getClasses(), msLabelClass)) {
				rows[idx].shades = true;
			}
		}
		
		columnIndex = columnIndex + colSpan;
		
		if (columnIndex >= numberOfColumns) {
			rowIndex++;
			columnIndex = 0;
		}
	}
	
	layout.setRowDivisions(rows);
	
	that._clearCollapseClasses();
	that._container.style.addClass("collapsed");
	that._container.style.addClass("collapsed-" + colCount);
	
	if(this._container.userAttr(that.ATTR_NM.ATTR_CUSTOM_CLASS)){
		that._container.style.addClass(this._container.userAttr(that.ATTR_NM.ATTR_CUSTOM_CLASS));
	}
};

/**
 * 
 * @param {Number} colCount 컬럼 수
 */
RForm.prototype._clearCollapseClasses = function() {
	var that = this;
	if(that._container.disposed) return;
	
	var classes = this._container.style.getClasses();
	var collapseClasses = classes.filter(function( /* String */ each) {
		return each.indexOf("collapsed") === 0;
	});
	collapseClasses.forEach((function( /* String */ each) {
		this._container.style.removeClass(each);
		this._container.style.removeClass(this._container.userAttr(this.ATTR_NM.ATTR_CUSTOM_CLASS));
	}).bind(this));
}

/**
 * 반응형 폼레이아웃 특정 행의 가시성 여부를 지정합니다
 * @param {String} psScreenNm 스크린 명
 * @param {Number[] | Number} paRowIdx 로우 인덱스 또는 로우 인덱스 배열
 * @param {Boolean} pbVisible 표시 여부
 */
RForm.prototype.setRowVisible = function(psScreenNm, paRowIdx, pbVisible) {
	var colSettings = this._columnSettings[psScreenNm];
	
	if(!(paRowIdx instanceof Array)){
		paRowIdx = [paRowIdx];
	}
	var originalLayout = this._originalLayout;
	paRowIdx.forEach(function(rowIdx){
		originalLayout.setRowVisible(parseInt(rowIdx), pbVisible);
	});
	
	this._transform(colSettings);	
}

/**
 * 반응형 폼레이아웃 특정 열의 가시성 여부를 지정합니다
 * @param {String} psScreenNm 스크린 명
 * @param {Number[] | Number} paColIdx 컬럼 인덱스 또는 컬럼 인덱스 배열
 * @param {Boolean} pbVisible 표시 여부
 */
RForm.prototype.setColumnVisible = function(psScreenNm, paColIdx, pbVisible) {
	var colSettings = this._columnSettings[psScreenNm];
	
	if(!(paColIdx instanceof Array)){
		paColIdx = [paColIdx];
	}
	var originalLayout = this._originalLayout;
	paColIdx.forEach(function(colIdx){
		originalLayout.setColumnVisible(parseInt(colIdx), pbVisible);
	});
	
	this._transform(colSettings);
}

/**
 * 
 * @param {cpr.controls.layouts.FormDivision} div
 */
function copyDiv(div) {
	var result = new cpr.controls.layouts.FormDivision(div.lengthExpression);
	result.autoSizing = div.autoSizing;
	result.hidden = div.hidden;
	result.shades = div.shades;
	result.customShadeColor = div.customShadeColor;
	result.minLength = div.minLength;
	return result;
}

/**
 * 반응형 그룹(폼 레이아웃) 적용
 * @param {cpr.controls.Container} container
 */
globals.makeResponsive = function(container) {
	return new RForm(container);
};