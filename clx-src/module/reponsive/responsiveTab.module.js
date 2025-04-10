/************************************************
 * responsiveTab.module.js
 *
 * @author tomatosystem
 ************************************************/

/*************************************************************************
 * 확인사항
 * 1. RTab.prototype.ATTR_NM에 구성된  
 *    프로젝트 표준으로  탭 폴더 컨트롤에 "transform-on-mobile" 사용자 속성을 구성하여 적용합니다.
 *    (eXBuilder6 > 프로젝트 표준 > 사용자 속성 정의)
 * 
 * 2. 반응형 모듈 적용은 event.module.js내 init EventBus에서 처리되며
 *    사용자 속성 "transform-on-mobile" 값 존재여부에 따라 적용여부를 판단 합니다.
 * 
 * 3. 탭 폴더 컨트롤을 모바일 환경에서 아코디언 컨트롤로 표시합니다. 
 **************************************************************************/

/**
 * 반응형 폼 유틸리티.
 * @param {cpr.controls.TabFolder} tabFolder
 */
function RTab(tabFolder) {
	this._tabFolder = tabFolder;
	
	this._onScreenChange = this._onScreenChange.bind(this);
	this._appInstance = this._tabFolder.getAppInstance();
	/** @type {
	* 	  default : String[],
	* 	  tablet : String[],
	* 	  mobile : String[]
	* 	} 
	**/
	this._screenNms = this._setScreenNm();
	
	this._appInstance.addEventListener("screen-change", this._onScreenChange);
	this._tabFolder.addEventListenerOnce("dispose", this.stop.bind(this));
}

/**
 * 반응형 탭 옵션 속성명<br>
 * (사용자 속성값의 기본타입은 String이며 valueType은 데이터 입력형식 확인을 위해 작성되었습니다.)
 */
RTab.prototype.ATTR_NM = {
	/** <필수>모바일 : 모바일 환경에서 반응형 탭폴더 적용 여부<br>(valueType : Boolean, ex. true)*/
	ATTR_TRANSFORM_ON_MOBILE : "transform-on-mobile"	
}

/**
 * 타입에 따른 스크린 명칭 구성
 */
RTab.prototype._setScreenNm = function(){
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

/** @type cpr.controls.Accordion */
RTab.prototype._accordion = null;

/** @type cpr.core.AppInstance */
RTab.prototype._appInstance = null;

RTab.prototype.stop = function() {
	if (this._appInstance) {
		this._appInstance.removeEventListener("screen-change", this._onScreenChange);
		this._appInstance = null;
	}
}

/**
 * 스크린 사이즈가 변경됐을 때 발생하는 이벤트
 * @param {cpr.events.CScreenChangeEvent} e
 */
RTab.prototype._onScreenChange = function(e) {
	var vsScrnName = e.screen.name;
	
	if(this._screenNms["mobile"].indexOf(vsScrnName) != -1) {
		this.switchToAccordion();
	} else {
		this.switchToTabFolder();
	} 
};

/**
 * 탭 폴더 컨트롤을 아코디언 컨트롤로 전환합니다.
 */
RTab.prototype.switchToAccordion = function() {
	if (this._accordion) {
		return;
	}

	var accordion = new cpr.controls.Accordion();
	accordion.addEventListener("selection-change", function(e){
		var item = e.newSelection;		
		if(item.length > 0){
			var itemIndex = accordion.getSectionItems().indexOf(item);
			var parent = accordion.getParent();
			if(parent){				
				item[0].content.style.animateFrom({
					"opacity" : "0"
				});
				var offsetRect = accordion.getOffsetRect();
				var top = offsetRect.top + itemIndex * 47;
				if(itemIndex > 0){
					top += 2;
				}
				parent.scrollTo(0, top, 0.7, cpr.animation.TimingFunction.EASE_OUT_CUBIC);
			}
		}
	});
	
	var tabItems = this._tabFolder.getTabItems();
	var selectedTabItem = this._tabFolder.getSelectedTabItem();

	for (var idx = 0; idx < tabItems.length; idx++) {
		var eachItem = tabItems[idx];
		this._tabFolder.removeTabItem(eachItem);
		var section = new cpr.controls.SectionItem();
		section.content = eachItem.content;
		section.title = eachItem.text;
		accordion.addSection(section);

		if (eachItem == selectedTabItem) {			
			accordion.setSelectedSections([section], false);			
		}
	}

	var parent = this._tabFolder.getParent();
	var constraint = JSON.parse(JSON.stringify(parent.getConstraint(this._tabFolder)));
	if (constraint["height"]) {
		constraint["height"] = parseInt(constraint["height"]) + (tabItems.length - 1) * 76 + "px";
	}

	var tabFolderIndex = parent.getChildren().indexOf(this._tabFolder);
	parent.insertChild(tabFolderIndex, accordion, constraint);

	this._tabFolder.visible = false;
	this._accordion = accordion;
};

/**
 * 아코디언 컨트롤을 탭 폴더 컨트롤로 전환합니다.
 */
RTab.prototype.switchToTabFolder = function() {
	if (!this._accordion) {
		return;
	}

	var sectionItems = this._accordion.getSectionItems();
	var originalSelections = this._accordion.getSelectedSections();
	while (this._accordion.getSectionCount() > 0) {
		var eachSectionItem = this._accordion.getSection(0);
		var selected = eachSectionItem == originalSelections[0];

		this._accordion.deleteSection(0);
		var tabItem = new cpr.controls.TabItem();
		tabItem.text = eachSectionItem.title;
		tabItem.content = eachSectionItem.content;
		tabItem.content["visible"] = true;
		this._tabFolder.addTabItem(tabItem);
		if (selected) {
			this._tabFolder.setSelectedTabItem(tabItem, false);
		}
	}

	this._tabFolder.visible = true;
	this._accordion.dispose();
	this._accordion = null;
};

/**
 * 반응형 탭 폴더 적용
 * @param {cpr.controls.TabFolder} tab
 */
globals.makeResponsiveTab = function(tab) {	
	return new RTab(tab);
}