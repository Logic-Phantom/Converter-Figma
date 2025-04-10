/************************************************
 * Control Wrapping Utils
 * 각 사이트별 커스터마이징하여 사용
 * version 2.0
 ************************************************/

// 의존 모듈 선언.
module.depends("module/standard/common");

/**
 * ComUdcBtnKit(공통 UDC) 유틸
 * @constructor
 * @param {common.AppKit} appKit
 */
function ComUdcBtnKit(appKit) {
	this._appKit = appKit;
};

/**
 * UDC내 컨트롤의 Enable 속성을 설정한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {String} psType UDC Type(path) 명
 * @param {Boolean} pbEnable 컨트롤 활성화 여부
 * @param {String | Array} paCtlId 컨트롤 ID 또는 ID 배열
 * @return void
 */
ComUdcBtnKit.prototype.setEnable = function(app, psType, pbEnable, paCtlId) {
	if (!(paCtlId instanceof Array)) {
		paCtlId = [paCtlId];
	}
	if (typeof(pbEnable) != "boolean") {
		pbEnable = ValueUtil.fixBoolean(pbEnable);
	}
	
	var comButton = this._appKit.Group.getAllChildrenByType(app, psType);
	for (var i = 0, len = comButton.length; i < len; i++) {
		var ctrl = comButton[i];
		ctrl.setEnableCtrls(pbEnable, paCtlId);
	}
};

/**
 * UDC내 컨트롤의 이벤트를 발생시킨다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {String} psType UDC Type(path) 명
 * @param {#uicontrol} psCtlId 컨트롤 ID
 * @param {String} psEventType 이벤트명(ex-click)
 */
ComUdcBtnKit.prototype.dispatchEvent = function(app, psType, psCtlId, psEventType) {
	
	var comUdc = this._appKit.Group.getAllChildrenByType(app, psType);
	
	if (comUdc != null && comUdc.length > 0) {
		var vcCtrl = comUdc[0].getEmbeddedAppInstance().lookup(psCtlId);
		if (vcCtrl) {
			vcCtrl.dispatchEvent(new cpr.events.CEvent(psEventType));
		}
	}
};

/**
 * 일반 컨트롤 유틸
 * @constructor
 * @param {common.AppKit} appKit
 */
function ControlKit(appKit) {
	this._appKit = appKit;
};

/**
 * 컨트롤에 스타일 클래스를 추가합니다. <br/>
 * 기존에 적용되어 있는 클래스는 그대로 존재합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} paCtrlId 컨트롤 ID
 * @param {#css-class} psClassName 스타일 클래스명
 */
ControlKit.prototype.addClass = function (app, paCtrlId, psClassName) {
	if (!(paCtrlId instanceof Array)) {
		paCtrlId = [paCtrlId];
	}
	
	for (var i = 0, len = paCtrlId.length; i < len; i++) {
		/** @type cpr.controls.UIControl */
		var vcCtrl = app.lookup(paCtrlId[i]);
		if(!vcCtrl.style.hasClass(psClassName)) {
			vcCtrl.style.addClass(psClassName);
		}
	}
}


/**
 * 컨트롤에 특정 스타일 클래스를 제거합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} paCtrlId 컨트롤 ID
 * @param {#css-class} psClassName 스타일 클래스명
 */
ControlKit.prototype.removeClass = function (app, paCtrlId, psClassName) {
	if (!(paCtrlId instanceof Array)) {
		paCtrlId = [paCtrlId];
	}
	
	for (var i = 0, len = paCtrlId.length; i < len; i++) {
		/** @type cpr.controls.UIControl */
		var vcCtrl = app.lookup(paCtrlId[i]);
		if(vcCtrl.style.hasClass(psClassName)) {
			vcCtrl.style.removeClass(psClassName);
		}
	} 
}


/**
 * 컨트롤에 스타일 클래스를 적용합니다.<br/>
 * 기존에 적용되어 있던 클래스는 모두 제거되며, 여러 개의 클래스를 배열의 형태로 한 번에 적용 할 수 있습니다.
 * <pre><code>
 * util.Control.setClasses(app, "ipb", ["class1", "class2"]);
 *  </code></pre>
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} paCtrlId 컨트롤 ID
 * @param {#css-class[]} paClassNames 스타일 클래스 배열
 */
ControlKit.prototype.setClasses = function (app, paCtrlId, paClassNames) {
	if (!(paCtrlId instanceof Array)) {
		paCtrlId = [paCtrlId];
	}
	
	if (!(paClassNames instanceof Array)) {
		paClassNames = [paClassNames];
	}
	
	for (var i = 0, len = paCtrlId.length; i < len; i++) {
		/** @type cpr.controls.UIControl */
		var vcCtrl = app.lookup(paCtrlId[i]);
		vcCtrl.style.setClasses(paClassNames);
	}
}

/**
 * 지정한 컨트롤의 Visible 속성을 설정한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {Boolean} pbVisible 컨트롤 숨김 여부
 * @param {#uicontrol | Array} paCtlId 컨트롤 ID 또는 ID 배열
 * @return void
 */
ControlKit.prototype.setVisible = function(app, pbVisible, paCtlId) {
	if (!(paCtlId instanceof Array)) {
		paCtlId = [paCtlId];
	}
	if (typeof(pbVisible) != "boolean") {
		pbVisible = ValueUtil.fixBoolean(pbVisible);
	}
	for (var i = 0, len = paCtlId.length; i < len; i++) {
		app.lookup(paCtlId[i]).visible = pbVisible;
	}
};

/**
 * 지정한 컨트롤의 Enable 속성을 설정한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {Boolean} pbEnable 컨트롤 활성화 여부
 * @param {#uicontrol | Array} paCtlId 컨트롤 ID 또는 ID 배열
 * @return void
 */
ControlKit.prototype.setEnable = function(app, pbEnable, paCtlId) {
	if (!(paCtlId instanceof Array)) {
		paCtlId = [paCtlId];
	}
	if (typeof(pbEnable) != "boolean") {
		pbEnable = ValueUtil.fixBoolean(pbEnable);
	}
	var ctrl;
	for (var i = 0, len = paCtlId.length; i < len; i++) {
		ctrl = app.lookup(paCtlId[i]);
		if (ctrl) {
			if(ctrl instanceof cpr.controls.Container){
				if (pbEnable === false) {
					ctrl.unbind("enabled");
					ctrl.enabled = pbEnable;
				} else {
					if (ctrl.userData("_expressEnabled")) ctrl.bind("enabled").toExpression(ctrl.userData("_expressEnabled"));
					else ctrl.enabled = pbEnable;
				}
			} else {
				ctrl.enabled = pbEnable;
			}
		} else {
			if (paCtlId[i] instanceof cpr.controls.UIControl) {
				paCtlId[i].enabled = pbEnable;
			}
		}
	}
};

/**
 * 지정한 컨트롤의 ReadOnly 속성을 설정한다.<br>
 * 만약, 해당 컨트롤에 readonly이 없을경우 enable 속성으로 제어된다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {Boolean} pbReadOnly 컨트롤 readOnly 여부
 * @param {#uicontrol | Array} paCtlId 컨트롤 ID 또는 ID 배열
 * @return void
 */
ControlKit.prototype.setReadOnly = function(app, pbReadOnly, paCtlId) {
	if (!(paCtlId instanceof Array)) {
		paCtlId = [paCtlId];
	}
	
	for (var i = 0, len = paCtlId.length; i < len; i++) {
		var voCtl = app.lookup(paCtlId[i]);
		if (voCtl == null || "undefined" == voCtl) continue;
		
		var vsCtlType = voCtl.type;
		if (voCtl.readOnly !== undefined) {
			voCtl.readOnly = pbReadOnly;
		} else {
			this.setEnable(app, !pbReadOnly, paCtlId[i]);
		}
	}
};

/**
 * 컨트롤의 지정된 사용자 정의 속성(userattr) 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID
 * @param {String} psAttrName 속성
 * @return {String} 속성값
 */
ControlKit.prototype.getUserAttr = function(app, psCtlId, psAttrName) {
	return app.lookup(psCtlId).userAttr(psAttrName);
};

/**
 * 컨트롤의 지정된 사용자 정의 속성(userattr)의 값을 설정한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID
 * @param {String} psAttrName 속성
 * @param {String} psAttrValue 속성값
 * @return void
 */
ControlKit.prototype.setUserAttr = function(app, psCtlId, psAttrName, psAttrValue) {
	var ctrl = app.lookup(psCtlId);
	var userAttr = ctrl.userAttr();
	userAttr[psAttrName] = psAttrValue;
};

/**
 * 컨트롤를 포커스(focus) 한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID
 */
ControlKit.prototype.setFocus = function(app, psCtlId) {
	var ctrl = app.lookup(psCtlId);
	if (ctrl instanceof cpr.controls.UDCBase) {
		var focused = false;
		var embApp = ctrl.getEmbeddedAppInstance();
		embApp.getContainer().getChildren().some(function(embCtrl) {
			if (embCtrl.getBindInfo("value") && embCtrl.getBindInfo("value").property == "value") {
				embCtrl.focus();
				focused = true;
				return true;
			}
		});
		if (focused !== true) {
			app.focus(ctrl);
		}
	} else {
		app.focus(ctrl);
	}
}

/**
 * 컨트롤의 값을 초기화한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol | Array} paCtlId 일반 컨트롤 및 그리드 컨트롤 ID		
 * @return void
 */
ControlKit.prototype.reset = function(app, paCtlId) {
	if (!(paCtlId instanceof Array)) {
		paCtlId = [paCtlId];
	}
	var vcCtrl;
	for (var i = 0, len = paCtlId.length; i < len; i++) {
		vcCtrl = app.lookup(paCtlId[i]);
		if (vcCtrl == null) continue;
		if (vcCtrl.type == "grid") {
			vcCtrl.dataSet.clear();
			/* 커스텀 옵션 : 그리드 타이틀 영역의 데이터 건수 업데이트를 위한 그리드 타이틀 UDC 타입 구성*/
			var titles = this._appKit.Group.getAllChildrenByType(app, "udc.com.udcComGridTitle");
			for (var j = 0, jlen = titles.length; j < jlen; j++) {
				if (titles[j] == null || titles[j].getAppProperty("ctrl") == null) continue;
				if (titles[j].getAppProperty("ctrl").id == vcCtrl.id) {
					titles[j].setAppProperty("rowCount", vcCtrl.dataSet.getRowCount());
				}
			}
		} else if (vcCtrl.type == "container") {
			var voDs = this._appKit.Group.getBindDataSet(app, vcCtrl);
			if (voDs) voDs.clear();
			vcCtrl.redraw();
		} else {
			vcCtrl.value = "";
		}
	}
};

/**
 * 특정 컨트롤의 자료를 갱신하고 다시 그린다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol | Array} paCtlId 일반 컨트롤 및 그리드 컨트롤 ID
 * @return void
 */
ControlKit.prototype.redraw = function(app, paCtlId) {
	if (!(paCtlId instanceof Array)) {
		paCtlId = [paCtlId];
	}
	for (var i = 0, len = paCtlId.length; i < len; i++) {
		var vcCtrl = app.lookup(paCtlId[i]);
		if (vcCtrl) vcCtrl.redraw();
	}
};

/**
 * 컨트롤의 지정된 style 속성 값을 가져옵니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID
 * @param {String} psAttrName style 속성명
 * @return {String} style 속성값
 */
ControlKit.prototype.getStyleAttr = function(app, psCtlId, psAttrName) {
	/**@type cpr.controls.UIControl*/
	var vcCtrl = app.lookup(psCtlId);
	return vcCtrl.style.css(psAttrName);
};

/**
 * 컨트롤의 지정된 style 속성값을 설정한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID
 * @param {String} psAttrName 속성
 * @param {String} psAttrValue 속성값
 * @return void
 */
ControlKit.prototype.setStyleAttr = function(app, psCtlId, psAttrName, psAttrValue) {
	/**@type cpr.controls.UIControl*/
	var vcCtrl = app.lookup(psCtlId);
	return vcCtrl.style.css(psAttrName, psAttrValue);
};

/**
 * 컨트롤이 실제 그려진 사이즈를 리턴합니다.<br>
 * 컨트롤이 화면에 그려지지 않은 상태인 경우는 모든 값이 0인 객체가 리턴됩니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId  컨트롤 ID
 * @param {String} psPosition 구하고자하는 위치 및 크기 정보<br>(width, height, left, top, bottom, right)
 * @return {Interface{width: Number, height: Number, left: Number, top: Number, bottom: Number, right: Number}} HTML DOM에서의 컨트롤의 위치 및 크기 정보
 */
ControlKit.prototype.getActualRectPosition = function(app, psCtlId, psPosition) {
	/** @type cpr.controls.UIControl */
	var vcCtrl = app.lookup(psCtlId);
	var voActRec = vcCtrl.getActualRect();
	return voActRec[psPosition];
};

/**
 * 해당 컨트롤의 제약 조건을 반환합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 반환하고자 하는 컨트롤 ID
 * @param {String} psParentGrp? 상위 컨트롤 그룹내 컨트롤의 제약 조건을 구할시 사용
 * @return {cpr.controls.layouts.Constraint} 해당하는 제약조건
 */
ControlKit.prototype.getConstraint = function(app, psCtlId, psParentGrp) {
	var ctrl = app.lookup(psCtlId);
	var container;
	if (!ValueUtil.isNull(psParentGrp)) {
		container = app.lookup(psParentGrp);
	} else {
		container = app.getContainer();
	}
	return container.getConstraint(ctrl);
};

/**
 * 컨트롤의 지정된 제약 조건(constraint)을 변경합니다.<br>
 * 타겟 컨트롤에서 부모 컨트롤과의 연계된 위치를 변경합니다.<br>
 * parameter의 constraints가 포함한 항목만 변경합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID
 * @param {#container} psParentGrpId 상위 컨트롤의 ID (app의 container일 경우 null)
 * @param {Object} poConstraint 제약조건 : <br>
 * 상위 컨트롤의 레이아웃이 formlayout일 경우 rowIndex, colIndex 를 반드시 포함한 조건을 설정하여야합니다.
 * @return {Boolean} 성공여부
 */
ControlKit.prototype.updateConstraint = function(app, psCtlId, psParentGrpId, poConstraint) {
	/** @type cpr.controls.UIControl */
	var vcChild = app.lookup(psCtlId);
	if (vcChild == null) return false;
	/** @type cpr.controls.Container */
	var voContainer = null;
	if (!ValueUtil.isNull(psParentGrpId)) {
		voContainer = app.lookup(psParentGrpId);
	} else {
		voContainer = app.getContainer();
	}
	
	var voLayout = voContainer.getLayout();
	var voConstraint = null;
	if (voLayout instanceof cpr.controls.layouts.ResponsiveXYLayout) {
		var voSrcConstraint = voContainer.getConstraint(vcChild)["positions"][0];
		voConstraint = {
			positions: [Object.assign(voSrcConstraint, poConstraint)]
		}
	} else {
		voConstraint = poConstraint;
	}
	
	return voContainer.updateConstraint(vcChild, voConstraint);
};

/**
 * 해당 컨트롤의 이벤트를 발생시킨다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID
 * @param {String} psEventType 이벤트명(ex-click)
 */
ControlKit.prototype.dispatchEvent = function(app, psCtlId, psEventType) {
	var vcCtrl = app.lookup(psCtlId);
	if (vcCtrl) {
		vcCtrl.dispatchEvent(new cpr.events.CEvent(psEventType));
	}
};

/**
 * 지정한 컨트롤의 value를 지정한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID
 * @param {String} psValue 값
 * @param {Boolean} pbEmitEvent? 값 변경후의 before-value-change, value-change 이벤트 발생시킬지 여부<br>
 *                  만약 값만 바꾸고, 이벤트 발생은 일어나지 않도록 하는 경우에만 false로 지정
 * @return void
 */
ControlKit.prototype.setValue = function(app, psCtlId, psValue, pbEmitEvent) {
	var ctrl = app.lookup(psCtlId);
	if (pbEmitEvent === false && ctrl.putValue != undefined) {
		ctrl.putValue(psValue);
	} else {
		ctrl.value = psValue;
	}
};

/**
 * @desc 지정한 컨트롤의 value를 취득한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID
 * @return void
 */
ControlKit.prototype.getValue = function(app, psCtlId) {
	return app.lookup(psCtlId).value;
};

/**
 * @desc 지정한 컨트롤의 value를 취득한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID
 * @param {String} psProperty 컨트롤 속성명
 * @return void
 */
ControlKit.prototype.getProperty = function(app, psCtlId, psProperty) {
	return app.lookup(psCtlId)[psProperty];
};

/**
 * 파라미터로받은 parent 하위의 모든 uiComponent들을 찾아 리턴합니다.<br>
 * (그룹, 임베디드앱, UDC일떄 사용)
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {cpr.controls.UIControl[]} paParent 부모 컨트롤 또는 부모 컨트롤 배열
 * @return {cpr.controls.UIControl[]} 
 */
ControlKit.prototype.getAllUiControl = function(app, paParent, result) {
	
	if (!(paParent instanceof Array)) {
		paParent = [paParent];
	}
	result = result || []
	var childs = []
	
	paParent.forEach(function(each) {
		if (each.type === "embeddedapp" || each instanceof cpr.controls.UDCBase) {
			childs = childs.concat(each.getEmbeddedAppInstance().getContainer().getAllRecursiveChildren());
		} else if (each.type === "container") {
			childs = childs.concat(each.getAllRecursiveChildren())
		}
	});
	result = result.concat(childs);
	var containers = childs.filter(function(each) {
		if (each.type === "embeddedapp" || each instanceof cpr.controls.UDCBase) {
			return true;
		}
	})
	
	return containers.length === 0 ? result : this.getAllUiControl(app, containers, result)
}

/**
 * app에 있는 모든 데이터 컨트롤을 리턴합니다.
 * @param {cpr.core.AppInstance[]} apps 앱인스턴스 또는 앱인스턴스 배열
 */
ControlKit.prototype.getAllDataControl = function(apps) {
	var getRecursiveApp = function(startApp, resArray) {
		resArray = resArray || startApp;
		
		var appInstances = [];
		
		startApp.forEach(function(each) {
			appInstances = appInstances.concat(
				each.getContainer().getAllRecursiveChildren().filter(function(ctrl) {
					return ctrl.type === "embeddedapp" || ctrl instanceof cpr.controls.UDCBase
				}).map(function(emb) {
					return emb.getEmbeddedAppInstance();
				})
			)
		});
		
		return appInstances.length === 0 ? resArray : getRecursiveApp(appInstances, resArray.concat(appInstances))
	}
	
	if (!(apps instanceof Array)) {
		apps = [apps];
	}
	var res = [];
	getRecursiveApp(apps).forEach(function( /* cpr.core.AppInstance */ each) {
		res = res.concat(each.getAllDataControls().filter(function(dataControl) {
			return !(dataControl instanceof cpr.protocols.Submission);
		}))
	})
	return res;
}

/**
 * 컨트롤에 지정된 다국어 언어키를 변경한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 아이디
 * @param {String} psLangKey 언어키
 */
ControlKit.prototype.setLanguage = function(app, psCtlId, psLangKey) {
	var vcCtrl = app.lookup(psCtlId);
	vcCtrl.bind("value").toLanguage(psLangKey);
};


/**
 * 데이터맵(DataMap) 데이터 컴포넌트 유틸
 * @param {common.AppKit} appKit
 */
function DataMapKit(appKit) {
	this._appKit = appKit;
}

/**
 * 입력 받은 columnName에 해당되는 데이터를 반환
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#datamap} psDataMapId 데이터맵 ID
 * @param {#column} psColumnName 값을 가져오고자 하는 컬럼명
 * @return {Object} 해당 데이터<br>
 * 					header dataType에 따라 반환타입이 정해짐<br>
					해당 columnName의 column이 존재 할 경우 해당 값 반환<br>
					해당 columnName의 값이 없을 경우 ""(공백) 반환<br>
					해당 columnName이 존재하지 않을 경우 null 반환
 */
DataMapKit.prototype.getValue = function(app, psDataMapId, psColumnName) {
	/** @type cpr.data.DataMap */
	var vcDataMap = app.lookup(psDataMapId);
	return vcDataMap.getValue(psColumnName);
};

/**
 * 입력 받은 columnName에 해당되는 데이터를 수정
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#datamap} psDataMapId 데이터맵 ID
 * @param {#column} psColumnName 값을 가져오고자 하는 컬럼명
 * @param {String} psValue 수정할 value 값
 * @return {Boolean} 값 수정 성공 여부
 */
DataMapKit.prototype.setValue = function(app, psDataMapId, psColumnName, psValue) {
	/** @type cpr.data.DataMap */
	var vcDataMap = app.lookup(psDataMapId);
	return vcDataMap.setValue(psColumnName, psValue);
};

/**
 * 데이터를 모두 제거합니다.<br>
 * (data가 모두 공백으로 설정됩니다.)
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#datamap} psDataMapId 데이터맵 ID
 * @param {Boolean} pbKeep? 데이터 유지여부(컬럼정보는 유지됨) (default: false)
 */
DataMapKit.prototype.clear = function(app, psDataMapId, pbKeep) {
	/** @type cpr.data.DataMap */
	var vcDataMap = app.lookup(psDataMapId);
	pbKeep = !ValueUtil.isNull(pbKeep) ? pbKeep : false;
	
	if (pbKeep) {
		vcDataMap.clear(false);
	} else {
		vcDataMap.clear();
	}
};

/**
 * 데이터를 모두 초기화합니다.<br>
 * (data 모두 초기 설정값으로 설정됩니다.)
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param  {#datamap} psDataMapId 데이터맵 ID
 */
DataMapKit.prototype.reset = function(app, psDataMapId) {
	/** @type cpr.data.DataMap */
	var vcDataMap = app.lookup(psDataMapId);
	vcDataMap.reset();
};

/**
 * 현재 데이터맵의 데이터를 타겟 데이터맵으로 복사합니다. <br>
 * 복사시 타겟 데이터맵의 alterColumnLayout 속성에 따라 복사방법의 설정됩니다. 
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#datamap} psSourceDataMapId 데이터맵 ID
 * @param {#datamap} psTargetDataMapId 복사 데이터가 들어갈 타겟 데이터맵 ID
 * @param {Boolean} pbCopyToColumn? 복사시 컬럼도 복사할지에 대한 여부. (default:false)
 * @return {Boolean}
 */
DataMapKit.prototype.copyToDataMap = function(app, psSourceDataMapId, psTargetDataMapId, pbCopyToColumn) {
	
	/** @type cpr.data.DataMap */
	var vcSourceDataMap = app.lookup(psSourceDataMapId);
	/** @type cpr.data.DataMap */
	var vcTargetDataMap = app.lookup(psTargetDataMapId);
	
	if (!ValueUtil.isNull(vcSourceDataMap) && !ValueUtil.isNull(vcTargetDataMap)) {
		if (pbCopyToColumn) {
			this._appKit.DataSet.copyToColumn(vcSourceDataMap, vcTargetDataMap);
		}
		
		return vcSourceDataMap.copyToDataMap(vcTargetDataMap);
	}
};


/**
 * Column을 추가합니다.<br>
 * Header정보에 추가되며, data가 있는 경우 row data에도 해당 column data가 추가됩니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#datamap} psDataMapId 데이터맵 ID
 * @param {String} psColumnNm 추가하려는 Header 명
 * @param {String} psValue? 초기값 설정
 * @return {Boolean} 컬럼 추가 성공 여부
 */
DataMapKit.prototype.addColumn = function(app, psDataMapId, psColumnNm, psValue) {
	var vcDataMap = app.lookup(psDataMapId);
	return vcDataMap.addColumn(new cpr.data.header.DataHeader(psColumnNm, "string"), psValue);
};

/**
 * Column을 삭제합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#datamap} psDataMapId 데이터맵 ID
 * @param {#column} psColumnName 삭제할 컬럼 명
 * @return {Boolean} 컬럼 삭제 성공 여부
 */
DataMapKit.prototype.deleteColumn = function(app, psDataMapId, psColumnName) {
	var vcDataMap = app.lookup(psDataMapId);
	return vcDataMap.deleteColumn(psColumnName);
};

/**
 * 데이터맵이 비어있는지 체크합니다.
 * @param {cpr.core.AppInstance} app
 * @param {#datamap} psDataMapId
 */
DataMapKit.prototype.isEmpty = function(app, psDataMapId) {
	/** @type cpr.data.DataMap **/
	var dm = app.lookup(psDataMapId)
	
	return dm && dm.getColumnNames().every(function(col) {
		return !dm.getString(col);
	})
}

/**
 * 데이터맵을 동적으로 생성합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {String} psDataMapId 생성할 데이터셋 아이디
 * @param {Object} poColumnInfo 컬럼정보 ex) {column1 : "string"} : 컬럼명 : 타입
 * @return {cpr.data.DataMap} 생성된 데이터맵
 */
DataMapKit.prototype.makeDataMap = function(app, psDataMapId, poColumnInfo) {
	
	if (!psDataMapId || !poColumnInfo) return;
	var tmpDm = new cpr.data.DataMap(psDataMapId);
	var that = this;
	app.register(tmpDm);
	
	Object.keys(poColumnInfo).forEach(function(each) {
		var columnNm = each;
		var type = poColumnInfo.each;
		
		that.addColumn(app, psDataMapId, columnNm, null, type)
	});
	
	return tmpDm;
}

/**
 * 데이터 셋의 특정 row의 데이터를 데이터 맵으로 복사합니다 <br>
 * 복사시 타겟 데이터맵의 alterColumnLayout 속성에 따라 복사방법의 설정됩니다. 
 * (server : 컬럼이 모두 source와 같이 변경 , client : 같은컬럼명만 복사, 이외는 null , merge : 머지)
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#datamap} psTargetDataMapId 복사 데이터가 들어갈 타겟 데이터맵 ID
 * @param {#dataset} psSourceDataSetId 데이터셋 ID
 * @param {Number} pnIndex DataSet의 index 
 * @return {Number} 추가된 컬럼수  ( -1일 경우 장애 )
 */
DataMapKit.prototype.copyFromDataSet = function(app, psTargetDataMapId, psSourceDataSetId, pnIndex) {
	/** @type cpr.data.DataSet */
	var vcDataSet = app.lookup(psSourceDataSetId);
	
	if (ValueUtil.isNull(vcDataSet)) {
		return -1;
	}
	
	var voRow = vcDataSet.getRow(pnIndex);
	if (!voRow) return -1;
	
	return this.copyFromRow(app, psTargetDataMapId, voRow);
};

/**
 * 현재 데이터맵에 Row의 값을 넣는다.<br>
 * 복사시 타겟 데이터맵의 alterColumnLayout 속성에 따라 복사방법의 설정됩니다. 
 * (server : 컬럼이 모두 soruce와 같이 변경 , client : 같은컬럼명만 복사 , 이외는 null , merge : 머지)
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#datamap} psTargetDataMapId 데이터맵 ID
 * @param {cpr.data.Row} poRow
 * @return {Number} 추가된 컬럼수 ( -1일 경우 장애 )
 */
DataMapKit.prototype.copyFromRow = function(app, psTargetDataMapId, poRow) {
	/** @type cpr.data.DataMap */
	var vcDataMap = app.lookup(psTargetDataMapId);
	if (ValueUtil.isNull(vcDataMap)) return -1;
	
	return vcDataMap.build(poRow.getRowData());
};  

/**
 * 전달받은 데이터맵을 targetDataMapId로 clone한다. (alterColumnLayout 정보 포함) <br>
 * 전달받은 데이터맵(pcSourceDataMap)이 없으면 null값으로 반환한다.<br>
 * targetDataMap이 기 생성되어있으면 null값으로 반환한다.<br>
 * 반복적으로 지정해야할 경우 copyToDataMap을 이용하거나 targetDataMap을 삭제[datamap.dispose()] 후 다시 호출하는 것을 권장한다. 
 * @param {cpr.core.AppInstance} app 앱인스턴스 
 * @param {cpr.data.DataMap} pcSourceDataMap 복제할 데이터맵
 * @param {String} psTargetDataMapId clone으로 생성될 데이터맵 ID (현재 앱인스턴스에 중복된 ID 가 있으면 복제 불가)
 * @return {cpr.data.DataMap}
 */
DataMapKit.prototype.cloneDataMap = function(app, pcSourceDataMap, psTargetDataMapId) {
	if (!pcSourceDataMap || !(pcSourceDataMap instanceof cpr.data.DataMap) ) return null;
	
	/** @type cpr.data.DataMap	 */
	var vcTargetDataMap = app.lookup(psTargetDataMapId);
	// 타겟 데이터셋이 있는경우 null 값을 반환한다.
	if(vcTargetDataMap) return null;
	
	vcTargetDataMap = new cpr.data.DataMap(psTargetDataMapId);
	vcTargetDataMap.alterColumnLayout = pcSourceDataMap.alterColumnLayout;
	app.register(vcTargetDataMap);
	
	// datamap의 컬럼을 복사하여 targetDatamap에 추가한다.
	this.copyToDataMap(app, pcSourceDataMap.id, psTargetDataMapId, true);
	
	return vcTargetDataMap;
};


/**
 * 데이터셋 컨트롤 유틸
 * @constructor
 * @param {common.AppKit} appKit
 */
function DataSetKit(appKit) {
	this._appKit = appKit;
};

/**
 * 데이터셋 또는 데이터맵에 컬럼(Column)을 추가합니다.<br>
 * Header정보 추가되며, data가 있는 경우 row data에도 해당 column data가 추가됩니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @param {String} psColumnNm 추가하려는 컬럼명
 * @param {Object} psValue? 초기값 설정
 * @param {"string" | "number" | "decimal" | "expression"} psColumnType? 컬럼유형
 * @return {Boolean} 컬럼 추가 성공 여부
 */
DataSetKit.prototype.addColumn = function(app, psDataSetId, psColumnNm, psValue, psColumnType) {
	/** @type cpr.data.DataSet */
	var dataset = app.lookup(psDataSetId);
	
	var columnType = !ValueUtil.isNull(psColumnType) ? psColumnType.toLowerCase() : "string";
	return dataset.addColumn(new cpr.data.header.DataHeader(psColumnNm, columnType), psValue);
};

/**
 * 조건에 맞는 데이터셋의 특정 값을 반환합니다.
 * <pre><code>
 * util.DataSet.getCondValue(app, "dsLttmRcd", "CD == '" + vsNewVal + "'", "CD_USG_01");
 * </code></pre>
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @param {String} psCondition 특정 값을 가져올 조건
 * @param {#column} psColumnName 가져오려는 값의 컬럼명
 */
DataSetKit.prototype.getCondValue = function(app, psDataSetId, psCondition, psColumnName) {
	/** @type cpr.data.DataSet */
	var dataset = app.lookup(psDataSetId);
	
	var voRow = dataset.findFirstRow(psCondition);
	return voRow != null ? voRow.getValue(psColumnName) : "";
};

/**
 * 데이터셋의 특정 값을 반환합니다.
 * <pre><code>
 * util.DataSet.getValue(app, "dsLttmRcd", 1, "CD_USG_01");
 * </code></pre>
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @param {Number} pnRowIndex 값을 가져올 row의 row index
 * @param {#column} psColumnName 가져오려는 값의 컬럼명
 */
DataSetKit.prototype.getValue = function(app, psDataSetId, pnRowIndex, psColumnName) {
	/** @type cpr.data.DataSet */
	var dataset = app.lookup(psDataSetId);
	
	return dataset.getValue(pnRowIndex, psColumnName)
};

/**
 * 입력 받은 rowIndex와 columnName에 해당되는 데이터를 수정합니다.<br>
 * <br>
 * 1. 상태변경<br>
 * 해당 columnName에 해당되는 Column이 DisplayColumn이 아니고 Row상태가 UNCHANGED 상태인 경우<br>
 * Row 상태가 UPDATED로 바뀝니다.(UNCHANGED -> UPDATED)<br>
 * DELEDED상태이거나 INSERTED상태인 row는 수정할 수 없습니다.<br>
 * 2. 이벤트<br>
 * 수정이 된 경우 <b>UPDATED 이벤트가 발생합니다.</b>
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @param {Number} pnRowIndex 수정할 row의 row index
 * @param {#column} psColumnName 수정할 column의 columnName
 * @param {Object} psValue 수정할 value 값
 * @return {Boolean} 값 수정 성공 여부
 */
DataSetKit.prototype.setValue = function(app, psDataSetId, pnRowIndex, psColumnName, psValue) {
	/** @type cpr.data.DataSet */
	var vcDataSet = app.lookup(psDataSetId);
	return vcDataSet.setValue(pnRowIndex, psColumnName, psValue);
};

/**
 * 모든 데이터셋 정보를 제거합니다.<br>
 * data, sort, filter가 제거됩니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#dataset} psDataSetId 데이터셋 ID
 */
DataSetKit.prototype.clear = function(app, psDataSetId) {
	/** @type cpr.data.DataSet */
	var vcDataSet = app.lookup(psDataSetId);
	vcDataSet.clear();
}

/**
 * 지정한 범위 내의 row들 중 조건에 맞는 모든 Row 객체의 배열을 반환<br>
 * 또는 지정한 범위 내의 row들 중 조건에 맞는 첫번째 Row 객체를 반환
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @param {String} psCondition 조건식<br>
 *                 ex)"STUD_DIV_RCD == 'CT101REGU' && SA_NM == '컴퓨터정보과'"<br>
 * 					사용가능수식 :<br>!=", "!==", "$=", "%", "&&", "(", "*", "*=", "+", ",", "-", ".", "/", "/*", "//", "<", "<=", "==", "===", ">", ">=", "?", "[", "^=", "||"
 * @param {Boolean} pbAllStatus?
 *                             true : 조건에 맞는 모든 row 리턴<br>
 *                             default : 조건에 맞는 첫번째 row 리턴
 * @param {Number} pnStartIdx? Number  범위지정 시작 row index
 * @param {Number} pnEndIdx? Number  범위지정 끝 row index
 * @retrun 데이터 로우
 */
DataSetKit.prototype.findRow = function(app, psDataSetId, psCondition, pbAllStatus, pnStartIdx, pnEndIdx) {
	/** @type cpr.data.DataSet */
	var vcDataSet = app.lookup(psDataSetId);
	
	if (pbAllStatus) {
		return vcDataSet.findAllRow(psCondition, pnStartIdx, pnEndIdx);
	} else {
		return vcDataSet.findFirstRow(psCondition, pnStartIdx, pnEndIdx);
	}
};


/**
 * 지정한 범위 내의 row들 중 조건에 맞는 모든 Row 객체의 배열을 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @param {String} psCondition 조건식
 *                 ex)"STUD_DIV_RCD == 'CT101REGU' && SA_NM == '컴퓨터정보과'"<br>
 * 					사용가능수식 :<br>!=", "!==", "$=", "%", "&&", "(", "*", "*=", "+", ",", "-", ".", "/", "/*", "//", "<", "<=", "==", "===", ">", ">=", "?", "[", "^=", "||"
 * @param {Number} pnStartIdx? 범위지정 시작 row index.
 * @param {Number} pnEndIdx? 범위지정 끝 row index.
 * @retrun {cpr.data.Row[]} 검색 조건에 맞는 Row 객체 배열
 */
DataSetKit.prototype.findAllRow = function(app, psDataSetId, psCondition, pnStartIdx, pnEndIdx) {
	/** @type cpr.data.DataSet */
	var dataset = app.lookup(psDataSetId);
	return dataset.findAllRow(psCondition, pnStartIdx, pnEndIdx);
};

/**
 * 지정한 범위 내의 row들 중 조건에 맞는 첫번째 Row 객체에 해당하는 컬럼의 value를 취득
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @param {String} psCondition 조건식<br>
 *                 ex)"STUD_DIV_RCD == 'CT101REGU' && SA_NM == '컴퓨터정보과'"<br>
 * 					사용가능수식 :<br>!=", "!==", "$=", "%", "&&", "(", "*", "*=", "+", ",", "-", ".", "/", "/*", "//", "<", "<=", "==", "===", ">", ">=", "?", "[", "^=", "||"
 * @param {#column} psColumnName 컬럼명
 * @param {Number} pnStartIdx?  범위지정 시작 row index
 * @param {Number} pnEndIdx?   범위지정 끝 row index
 * @retrun 데이터 로우
 */
DataSetKit.prototype.getFindRowValue = function(app, psDataSetId, psCondition, psColumnName, pnStartIdx, pnEndIdx) {
	/** @type cpr.data.IDataRow */
	var voRow = this.findRow(app, psDataSetId, psCondition, false, pnStartIdx, pnEndIdx);
	if (voRow != null) {
		return voRow.getValue(psColumnName);
	} else {
		return null;
	}
};

/**
 * 현재 Row 수를 반환
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @retrun {Number} 로우 갯수
 */
DataSetKit.prototype.getRowCount = function(app, psDataSetId) {
	/** @type cpr.data.DataSet */
	var vcDataSet = app.lookup(psDataSetId);
	return vcDataSet.getRowCount();
};

/**
 * 현재 데이터셋의 데이터를 타겟 데이터셋으로 복사합니다.<br>
 * 타겟 데이터셋의 존재하는 컬럼의 데이터만 복사됩니다.<br>
 * 복사시 추가되는 데이터는 INSERT 상태입니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스 
 * @param {#dataset} psSourceDataSetId 데이터셋 ID
 * @param {#dataset} psTargetDataSetId 복사 데이터가 들어갈 타겟 DataSet ID
 * @param {String} psFilterCondition? 복사시 필터링할 조건 (생략시 전체 복사, target의 기존 데이터는 삭제됨)<br>
 * 				   "STUD_DIV_RCD == 'CT101REGU' && SA_NM == '컴퓨터정보과'" (동일한 로우가 있을경우 복사안함)
 * @param {Boolean} pbCopyToColumn? 복사시 컬럼도 복사할지에 대한 여부. (default:false)
 * @return {Boolean}
 */
DataSetKit.prototype.copyToDataSet = function(app, psSourceDataSetId, psTargetDataSetId, psFilterCondition, pbCopyToColumn) {
	/** @type cpr.data.DataSet */
	var vcSourceDataSet = app.lookup(psSourceDataSetId);
	/** @type cpr.data.DataSet */
	var vcTargetDataSet = app.lookup(psTargetDataSetId);
	
	if (!psFilterCondition) vcTargetDataSet.clear();
	else {
		var vaFindRow = vcTargetDataSet.findAllRow(psFilterCondition);
		if (vaFindRow != null && vaFindRow.length > 0) {
			return;
		}
	}
	
	// pbCopyToColumn가 true 일 경우 column을 복사한다.
	if(pbCopyToColumn){
		this.copyToColumn(vcSourceDataSet, vcTargetDataSet);
	}
	
	return vcSourceDataSet.copyToDataSet(vcTargetDataSet, psFilterCondition);
};

/**
 * rowData를 입력받아 원하는 특정 row index의 앞이나 뒤에 신규 row를 추가합니다.<br>
 * <b>INSERTED 이벤트가 발생합니다.</b>
 * @param {cpr.core.AppInstance} app 앱인스턴스 
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @param {Number} pnIndex index 삽입하고자 하는 row index
 * @param {Boolean} pbAfter 해당 row index의 뒤에 삽입할지 여부 (true:뒤 / false:앞)
 * @param {cpr.data.RowConfigInfo} poRowData 추가할 row data (key: header명, value: value 를 갖는 json data)<br>
{[columnName: string]: string | number | boolean}
*  @return {cpr.data.Row} 추가한 신규 Row 객체
 */
DataSetKit.prototype.insertRow = function(app, psDataSetId, pnIndex, pbAfter, poRowData) {
	/** @type cpr.data.DataSet */
	var vcDataSet = app.lookup(psDataSetId);
	if (poRowData == null) {
		return vcDataSet.insertRow(pnIndex, pbAfter);
	} else {
		return vcDataSet.insertRowData(pnIndex, pbAfter, poRowData);
	}
	
};

/**
 * 데이터셋의 수정유무를 반환합니다.
 * @param {cpr.core.AppInstance} app
 * @param {#dataset | #dataset[]} paDataSetId
 * @param {"I" | "U" | "D"} psState?
 * @return {Boolean} 수정유무
 */
DataSetKit.prototype.isModify = function(app, paDataSetId, psState) {
	if (!(paDataSetId instanceof Array)) {
		paDataSetId = [paDataSetId];
	}
	
	return paDataSetId.some(function(each) {
		/**
		 * @type cpr.data.DataSet
		 */
		var ds = app.lookup(each);
		if (!psState && ds.isModified()) {
			return true;
		} else if (psState === "I" && ds.getRowStatedIndices(cpr.data.tabledata.RowState.INSERTED).length > 0) {
			return true;
		} else if (psState === "U" && ds.getRowStatedIndices(cpr.data.tabledata.RowState.UPDATED).length > 0) {
			return true;
		} else if (psState === "D" && ds.getRowStatedIndices(cpr.data.tabledata.RowState.DELETED).length > 0) {
			return true;
		}
	})
}

/**
 * 특정 row index의 데이터만 원복시킵니다<br>
 * @param {cpr.core.AppInstance} app 앱인스턴스 
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @param {Number} pnIndex 원복할 row index
 */
DataSetKit.prototype.revertRow = function(app, psDataSetId, pnIndex) {
	/** @type cpr.data.DataSet */
	var vcDataSet = app.lookup(psDataSetId);
	vcDataSet.revertRow(pnIndex);
};

/**
 * 특정 Row의 상태 값을 string 타입으로 반환합니다. <br>
 * @param {cpr.core.AppInstance} app 앱인스턴스 
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @param {Number} pnIndex row index
*  @return {String} Row을 찾을 수 없을 경우 null을 반환하고 그렇지 않은 경우 아래 값 중 하나를 반환합니다.<br>
				        변경되지 않은 상태 : "UC", "UNCHANGED"<br>
				        신규 상태 : "I", "INSERTED"<br>
				        수정 상태 : "U", "UPDATED"<br>
				        삭제 상태 : "D" , "DELETED"<br>
				        추가되었다가 삭제된 상태 : "ID", "INSERTDELETED"
 */
DataSetKit.prototype.getRowStateString = function(app, psDataSetId, pnIndex) {
	/** @type cpr.data.DataSet */
	var vcDataSet = app.lookup(psDataSetId);
	return vcDataSet.getRowStateString(pnIndex);
};

/**
 * row index를 입력받아 해당 row를 제거합니다.
 *  DELETED 이벤트가 발생합니다.  <br>
 * @param {cpr.core.AppInstance} app 앱인스턴스 
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @param {Number} pnIndex index 삭제하고자하는 row index
 *  @return {Boolean} 삭제 성공 여부
 */
DataSetKit.prototype.deleteRow = function(app, psDataSetId, pnIndex) {
	/** @type cpr.data.DataSet */
	var vcDataSet = app.lookup(psDataSetId);
	return vcDataSet.deleteRow(pnIndex);
};

/**
 * 데이터셋에서 삭제 상태인 행을찾아 리스트에서 삭제합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @return {cpr.data.DataSet} 
 */
DataSetKit.prototype.removeRows = function(app, psDataSetId) {
	
	if (!app || !psDataSetId || !(app.lookup(psDataSetId) instanceof cpr.data.DataSet)) return;
	
	/** @type cpr.data.DataSet */
	var targetDs = app.lookup(psDataSetId);
	
	for (var i = 0; i < targetDs.getRowCount(); i++) {
		var row = targetDs.getRow(i);
		if (row.getState() === cpr.data.tabledata.RowState.DELETED) {
			targetDs.realDeleteRow(row.getIndex());
			i--;
		}
	}
	
	return targetDs;
}

/**
 * 데이터셋에 중복된 데이터가 있는지 체크합니다.<br>
 * (pk로 사용할 컬럼을 DataSet의 info에 ","로 구분지어 기술해야한다.)
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @param {Boolean} pbFilterCheck 필터를 체크할지에 대한 여부 (false:필터 미적용 / true:필터적용 )
 * @return {Boolean} 중복 데이터 존재 유무
 */
DataSetKit.prototype.dupExistCheck = function(app, psDataSetId, pbFilterCheck) {
	if (!app || !psDataSetId || !(app.lookup(psDataSetId) instanceof cpr.data.DataSet)) return null;
	
	/** @type cpr.data.DataSet*/
	var targetDs = app.lookup(psDataSetId);
	
	if (ValueUtil.isNull(targetDs.info)) return false;
	
	var pkCols = ValueUtil.split(targetDs.info, ",");
	var pkValues = [];
	var rows = [];
	
	if(pbFilterCheck){ 
		// 필터가 적용중일 경우
		// 필터된 데이터의 전체 데이터를 가져온다.
		rows = targetDs.getRowDataRanged();
	} else {
		// 필터가 미적용일 경우 (default)
		// forEachOfUnfilterRows를 활용하여 필터를 무시하고 모든 데이터를 가져온다.
		targetDs.forEachOfUnfilteredRows(function(/* cpr.data.IDataRow */row) {
			rows.push(row.getRowData());
		});
	}
	
	// 중복 데이터 체크.
	rows.forEach(function(row){
		var pkValue = ""
		pkCols.forEach(function(col) {
			pkValue += row[col] + ","; //키 사이 구분자
		});
		pkValues.push(pkValue.slice(0, -1));
	});
	
	return pkValues.some(function(each) {
		return pkValues.indexOf(each) !== pkValues.lastIndexOf(each);
	});
}

/**
 * 데이터셋이 비어있는지 유무를 리턴합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @return {Boolean} 비어있는지 유무
 */
DataSetKit.prototype.isEmpty = function(app, psDataSetId) {
	/** @type cpr.data.DataSet*/
	var ds = app.lookup(psDataSetId);
	
	if (!ds || !(ds instanceof cpr.data.DataSet)) return;
	return ds.getRowCount() == 0
}

/**
 * 조건에 맞는 행들을 찾아 특정컬럼의 합계를 반환합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @param {String} psCondition 검색조건
 * @param {#column} psColumn 합계를 구할 컬럼명(Number타입이어야함)
 * @param {Number} pnStartIndex? 검색시작 row Index
 * @param {Number} pnLastIndex? 검색마지막 row Index 
 * @return {Number} 합계
 */
DataSetKit.prototype.getCaseSum = function(app, psDataSetId, psCondition, psColumn, pnStartIndex, pnLastIndex) {
	/** @type cpr.data.DataSet*/
	var targetDataSet = app.lookup(psDataSetId);
	if (!targetDataSet || !(targetDataSet instanceof cpr.data.DataSet) || !psColumn) return 0;
	
	var rows = targetDataSet.findAllRow(psCondition, pnStartIndex, pnLastIndex);
	
	return rows.reduce(function(acc, cur) {
		if (!ValueUtil.isNumber(acc)) {
			acc = acc.getNumber(psColumn)
		}
		return acc + cur.getNumber(psColumn) || 0;
	}, 0);
}

/**
 * 조건에 맞는 행들을 찾아 특정컬럼의 평균을 반환합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @param {String} psCondition 검색조건
 * @param {#column} psColumn 평균을 구할 컬럼명(Number타입이어야함)
 * @param {Number} pnStartIndex? 검색시작 row Index
 * @param {Number} pnLastIndex? 검색마지막 row Index 
 * @return {Number} 평균값
 */
DataSetKit.prototype.getCaseAvg = function(app, psDataSetId, psCondition, psColumn, pnStartIndex, pnLastIndex) {
	/** @type cpr.data.DataSet*/
	var targetDataSet = app.lookup(psDataSetId);
	if (!targetDataSet || !(targetDataSet instanceof cpr.data.DataSet) || !psColumn) return 0;
	
	var rows = targetDataSet.findAllRow(psCondition, pnStartIndex, pnLastIndex);
	
	return Number(rows.reduce(function(acc, cur) {
		if (!ValueUtil.isNumber(acc)) {
			acc = acc.getNumber(psColumn)
		}
		return acc + cur.getNumber(psColumn) || 0;
	}, 0) / rows.length);
}

/**
 * 조건에 맞는 행들을 찾아 특정컬럼의 최대값을 반환합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @param {String} psCondition 검색조건
 * @param {#column} psColumn 최대값을 구할 컬럼명(Number타입이어야함)
 * @param {Number} pnStartIndex 검색시작 row Index
 * @param {Number} pnLastIndex 검색마지막 row Index 
 * @return {Number} 평균값
 */
DataSetKit.prototype.getCaseMax = function(app, psDataSetId, psCondition, psColumn, pnStartIndex, pnLastIndex) {
	/** @type cpr.data.DataSet*/
	var targetDataSet = app.lookup(psDataSetId);
	if (!targetDataSet || !(targetDataSet instanceof cpr.data.DataSet) || !psColumn) return 0;
	
	var rows = targetDataSet.findAllRow(psCondition, pnStartIndex, pnLastIndex);
	
	return rows.reduce(function(prev, cur) {
		if (!ValueUtil.isNumber(prev)) {
			prev = prev.getValue(psColumn)
		}		
		return prev > cur.getValue(psColumn) ? prev : cur.getValue(psColumn)
	});
}

/**
 * 조건에 맞는 행들을 찾아 특정컬럼의 최소값을 반환합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @param {String} psCondition 검색조건
 * @param {#column} psColumn 최소값을 구할 컬럼명(Number타입이어야함)
 * @param {Number} pnStartIndex 검색시작 row Index
 * @param {Number} pnLastIndex 검색마지막 row Index 
 * @return {Number} 평균값
 */
DataSetKit.prototype.getCaseMin = function(app, psDataSetId, psCondition, psColumn, pnStartIndex, pnLastIndex) {
	/** @type cpr.data.DataSet*/
	var targetDataSet = app.lookup(psDataSetId);
	if (!targetDataSet || !(targetDataSet instanceof cpr.data.DataSet) || !psColumn) return 0;
	
	var rows = targetDataSet.findAllRow(psCondition, pnStartIndex, pnLastIndex);
	
	return rows.reduce(function(prev, cur) {
		if (!ValueUtil.isNumber(prev)) {
			prev = prev.getValue(psColumn)
		}
		
		return prev > cur.getValue(psColumn) ? cur.getValue(psColumn) : prev
	}, 0);
}

/**
 * 데이터셋을 동적으로 생성합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {String} psDataSetId 생성할 데이터셋 ID
 * @param {Object} poColumnInfo 컬럼정보 ex) {column1 : "string"} : 컬럼명 : 타입
 * @return {cpr.data.DataSet} 생성된 데이터셋
 */
DataSetKit.prototype.makeDataList = function(app, psDataSetId, poColumnInfo) {
	
	if (!psDataSetId || !poColumnInfo) return;
	
	var tmpDs = new cpr.data.DataSet(psDataSetId);
	var that = this;
	app.register(tmpDs);
	
	Object.keys(poColumnInfo).forEach(function(each) {
		var columnNm = each;
		var type = poColumnInfo.each;
		
		that.addColumn(app, psDataSetId, columnNm, null, type)
	});
	
	return tmpDs;
}

/**
 * 전체 데이터에서 특정 컬럼의 값 중 중복되지 않은 로우 배열을 반환 한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#dataset} psDataSetId 데이터셋 Id
 * @param {#column} psColumnNm 기준 컬럼명
 * @return {cpr.data.Row[]} 중복되지 않은 로우 배열
 */
DataSetKit.prototype.getDistinctRows = function(app, psDataSetId, psColumnNm) {
	/** @type cpr.data.DataSet */
	var vcDataSet = app.lookup(psDataSetId);
	if (ValueUtil.isNull(vcDataSet) || ValueUtil.isNull(psColumnNm)) {
		return;
	}
	
	/** @type cpr.data.DataSet */
	var vcTempDataSet = new cpr.data.DataSet();
	vcTempDataSet.alterColumnLayout = "server";
	vcTempDataSet.build(vcDataSet.getRowDataRanged());
	vcTempDataSet.addDisplayColumn(new cpr.data.header.ExpressionHeader("index", "getIndex()"));
	
	var vaDistinctVals = vcTempDataSet.getUnfilteredDistinctValues(psColumnNm);
	var vaFindRowIndex = [];
	vaDistinctVals.forEach(function(psDistinctVal) {
		var voFindRow = vcTempDataSet.findFirstRow(psColumnNm + " == '" + psDistinctVal + "'");
		vaFindRowIndex.push(voFindRow.getIndex());
	});
	
	var vsCondition = "";
	vaFindRowIndex.forEach(function(pnFindRowIndex, idx) {
		if (idx != (vaFindRowIndex.length - 1)) {
			vsCondition = vsCondition.concat("getIndex() == ", pnFindRowIndex, " || ");
		} else {
			vsCondition = vsCondition.concat("getIndex() == ", pnFindRowIndex);
		}
	});
	
	return vcDataSet.findAllRow(vsCondition);
};

/**
 * 전체 데이터에서 특정 컬럼의 값 중 중복되지 않은 로우로 필터 한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @param {#column} psColumnNm 기준 컬럼명
 * @return void
 */
DataSetKit.prototype.setDistinctRows = function(app, psDataSetId, psColumnNm) {
	/** @type cpr.data.DataSet */
	var vcDataSet = app.lookup(psDataSetId);
	if (ValueUtil.isNull(vcDataSet) || ValueUtil.isNull(psColumnNm)) {
		return;
	}
	
	vcDataSet.clearFilter();
	
	/** @type cpr.data.DataSet */
	var vcTempDataSet = new cpr.data.DataSet();
	vcTempDataSet.alterColumnLayout = "server";
	vcTempDataSet.build(vcDataSet.getRowDataRanged());
	vcTempDataSet.addDisplayColumn(new cpr.data.header.ExpressionHeader("index", "getIndex()"));
	
	var vaDistinctVals = vcTempDataSet.getUnfilteredDistinctValues(psColumnNm);
	var vaFindRowIndex = [];
	vaDistinctVals.forEach(function(psDistinctVal) {
		var voFindRow = vcTempDataSet.findFirstRow(psColumnNm + " == '" + psDistinctVal + "'");
		vaFindRowIndex.push(voFindRow.getIndex());
	});
	
	var vsCondition = "";
	vaFindRowIndex.forEach(function(pnFindRowIndex, i) {
		if (i != (vaFindRowIndex.length - 1)) {
			vsCondition = vsCondition.concat("getIndex() == ", pnFindRowIndex, " || ");
		} else {
			vsCondition = vsCondition.concat("getIndex() == ", pnFindRowIndex);
		}
	});
	
	vcDataSet.setFilter(vsCondition);
};

/**
 * 원본 데이터셋에서 중복 데이터가 제거된 데이터셋을 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#dataset} psDataSetId 데이터셋 Id
 * @param {#column} psColumnNm 기준 컬럼명
 * @return {cpr.data.DataSet} 중복이 제거된 dataSet
 */
DataSetKit.prototype.getDistinctDataSet = function(app, psDataSetId, psColumnNm) {
	/** @type cpr.data.DataSet */
	var vcDataSet = app.lookup(psDataSetId);
	if (ValueUtil.isNull(vcDataSet) || ValueUtil.isNull(psColumnNm)) {
		return;
	}
	
	/** @type cpr.data.DataSet */
	var vcTempDataSet = new cpr.data.DataSet();
	vcTempDataSet.alterColumnLayout = "server";
	
	var voRowData = vcDataSet.getRowDataRanged();
	
	// JSON 중복 제거 
	var voNowRowData = voRowData.filter(function(item1, idx) {
		return voRowData.findIndex(function(item2) {
			return item1[psColumnNm] == item2[psColumnNm]
		}) == idx;
	});
	vcTempDataSet.build(voNowRowData);
	
	return vcTempDataSet;
};

/**
 * 마지막행에 행을 추가한다. 
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#dataset} psDataSetId 데이터셋 Id
 * @param {cpr.data.RowConfigInfo} poRowData 추가할 row data (key: header명, value: value 를 갖는 json data) <br>
 * {[columnName: string]: string | number} 
 * @return {cpr.data.IDataRow} 추가한 신규 Row 객체
 */
DataSetKit.prototype.addRow = function(app, psDataSetId, poRowData) {
	/** @type cpr.data.DataSet */
	var vcDataSet = app.lookup(psDataSetId);
	if (ValueUtil.isNull(vcDataSet)) {
		return;
	}

	if (poRowData == null || poRowData == undefined) {
		return vcDataSet.addRow();
	} 	else {
		return vcDataSet.addRowData(poRowData);
	}
};

/**
 * 데이터셋의 Row 에 일치하는 컬럼명의 데이터를 copy한다.
 * 해당 로우에 같은 컬럼명일경우 copy한다.
 * @param {cpr.core.AppInstance} app 앱 인스턴스
 * @param {#dataset} psDataSetId 데이터셋 ID
 * @param {Number} pnIndex 타겟 rowIndex
 * @param {#datamap} psDataMapId  데이터맵 ID
 * @return {cpr.data.IDataRow}
 */
DataSetKit.prototype.copyFromDataMap = function(app, psDataSetId, pnIndex, psDataMapId) {
	/** @type cpr.data.DataSet */
	var vcDataSet = app.lookup(psDataSetId);
	/** @type cpr.data.DataMap */
	var vcDataMap = app.lookup(psDataMapId);
	
	if (ValueUtil.isNull(vcDataSet) || ValueUtil.isNull(vcDataMap)) {
		return null;
	}
	
	if (ValueUtil.isNull(pnIndex) || pnIndex < 0) return null;
	if (vcDataSet.getRowCount() < (pnIndex + 1)) return null;
	
	var vcRow = vcDataSet.getRow(pnIndex);
	vcRow.setRowData(vcDataMap.getDatas());
	return vcRow;
};

/**
 * 전달받은 데이터셋을 targetDataSet ID로 clone한다.<br>
 * stateRestore , alterColumnLayout 의 정보도 가져와 설정한다.<br>
 * 전달받은 데이터셋(pcSourceDataSet, targetDataSet)이 없으면 null값으로 반환한다.<br>
 * @param {cpr.core.AppInstance} app 앱인스턴스 
 * @param {cpr.data.DataSet} pcSourceDataSet source DataSet 
 * @param {cpr.data.DataSet} pcTargetDataSet clone으로 생성될 DataSet
 * @param {String} psFilterCondition? 생성 시 필터링할 조건 (생략시 전체 복사, target으로 데이터셋이 있을 경우 기존 데이터는 삭제됨)<br>
 * @param {Boolean} pbRowState? 생성 시 rowState 정보를 반영할지 여부. (default: true);
 * @return {cpr.data.DataSet}  
 */
DataSetKit.prototype._cloneDataSet = function(app, pcSourceDataSet, pcTargetDataSet, psFilterCondition, pbRowState) {
	// source 데이터셋이 없을 경우 실행하지 않는다.
	if (!pcSourceDataSet || !(pcSourceDataSet instanceof cpr.data.DataSet)) return null;
	
	// target 데이터셋이 없을 경우 실행하지 않는다.
	if (!pcTargetDataSet || !(pcTargetDataSet instanceof cpr.data.DataSet)) return null;
	
	// dataset의 컬럼을 복사하여 targetDataset에 추가한다.
	this.copyToDataSet(app, pcSourceDataSet.id, pcTargetDataSet.id, psFilterCondition, true);
	pcTargetDataSet.commit();
	
	// rowSatate 반환여부가 true이거나 설정하지 않았을 경우 rowStatus를 모두 검사하여 target DataSet에도 반영한다.
	if (pbRowState != false) {
		// insert Row
		pcSourceDataSet.getRowStatedIndices(cpr.data.tabledata.RowState.INSERTED).forEach(function(pnInsertRowIndex) {
			pcTargetDataSet.setRowState(pnInsertRowIndex, cpr.data.tabledata.RowState.INSERTED);
		});
		// update Row
		pcSourceDataSet.getRowStatedIndices(cpr.data.tabledata.RowState.UPDATED).forEach(function(pnUpdateRowIndex) {
			pcTargetDataSet.setRowState(pnUpdateRowIndex, cpr.data.tabledata.RowState.UPDATED);
		});
		// delete Row
		pcSourceDataSet.getRowStatedIndices(cpr.data.tabledata.RowState.DELETED).forEach(function(pnDeleteRowIndex) {
			pcTargetDataSet.setRowState(pnDeleteRowIndex, cpr.data.tabledata.RowState.DELETED);
		});
	}
	return pcTargetDataSet;
};

/**
 * 전달받은 데이터셋을 targetDataSetId로 clone한다.<br>
 * stateRestore , alterColumnLayout 의 정보도 가져온다.<br>
 * source 데이터셋이 없거나, target 데이터셋이 존재할 경우 null값으로 반환한다.<br>
 * 기존 존재하는 타겟 데이터셋에 clone 해야 할 경우 cloneOrReplaceDataSet을 이용하는 것을 권장한다. 
 * @param {cpr.core.AppInstance} app 앱인스턴스 
 * @param {#dataset} psSourceDataSetId source 데이터셋 ID
 * @param {String} psTargetDataSetId clone으로 생성될 데이터셋 ID (중복되는 ID가 존재할 경우 clone 하지 않는다)
 * @param {String} psFilterCondition? 생성 시 필터링할 조건 (생략시 전체 복사, target으로 데이터셋이 있을 경우 기존 데이터는 삭제됨)<br>
 * @param {boolean} pbRowState? 생성 시 rowState 정보를 반영할지 여부. (default: true);
 * @return {cpr.data.DataSet}  
 */
DataSetKit.prototype.cloneDataSet = function(app, psSourceDataSetId, psTargetDataSetId, psFilterCondition, pbRowState) {
	/** @type cpr.data.DataSet */
	var vcSourceDataSet = app.lookup(psSourceDataSetId);
	if(ValueUtil.isNull(vcSourceDataSet)) return null;
	
	/** @type cpr.data.DataSet	 */
	var vcTargetDataSet = app.lookup(psTargetDataSetId);
	if(vcTargetDataSet) return null;
	
	
	vcTargetDataSet = new cpr.data.DataSet(psTargetDataSetId);
	vcTargetDataSet.alterColumnLayout = vcSourceDataSet.alterColumnLayout;
	vcTargetDataSet.stateRestore = vcSourceDataSet.stateRestore;
	app.register(vcTargetDataSet);
	
	return this._cloneDataSet(app, vcSourceDataSet, vcTargetDataSet, psFilterCondition, pbRowState);
};

/**
 * 전달받은 데이터셋을 targetDataSetId로 clone or replace한다.<br>
 * targetDataSetId로 데이터셋이 존재할 경우 모든 정보의 초기화 후 재생성한다.<br>
 * stateRestore , alterColumnLayout 의 정보도 가져온다.<br>
 * 전달받은 데이터셋(pcSourceDataSet)이 없으면 null값으로 반환한다.<br>
 * @param {cpr.core.AppInstance} app 앱인스턴스 
 * @param {#dataset} psSourceDataSetId source DataSet 
 * @param {String | #dataset} psTargetDataSetId clone으로 생성될 DataSet ID
 * @param {String} psFilterCondition? 생성 시 필터링할 조건 (생략시 전체 복사, target으로 데이터셋이 있을 경우 기존 데이터는 삭제됨)<br>
 * @param {Boolean} pbRowState? 생성 시 rowState 정보를 반영할지 여부. (default: true);
 * @return {cpr.data.DataSet}  
 */
DataSetKit.prototype.cloneOrReplaceDataSet = function(app, psSourceDataSetId, psTargetDataSetId, psFilterCondition, pbRowState) {
	
	/** @type cpr.data.DataSet */
	var vcSourceDataSet = app.lookup(psSourceDataSetId);
	/** @type cpr.data.DataSet */
	var vcTargetDataSet = app.lookup(psTargetDataSetId);
	
	if(ValueUtil.isNull(vcTargetDataSet)){
		// 데이터셋이 없는경우 신규 생성한다.
		vcTargetDataSet = new cpr.data.DataSet(psTargetDataSetId);	
		vcTargetDataSet.alterColumnLayout = vcSourceDataSet.alterColumnLayout;
		vcTargetDataSet.stateRestore = vcSourceDataSet.stateRestore;
		app.register(vcTargetDataSet);
	} else {
		// 데이터셋이 있는경우 모두 초기화한다.
		vcTargetDataSet.clear(true);
		vcTargetDataSet.alterColumnLayout = vcSourceDataSet.alterColumnLayout;
		vcTargetDataSet.stateRestore = vcSourceDataSet.stateRestore;
	}
	
	return this._cloneDataSet(app, vcSourceDataSet, vcTargetDataSet, psFilterCondition, pbRowState);
};

/**
 * pcDataObject(DataSet) 의 column을  복사하여 pcTargetDataObject(DataSet) 에 column을 추가한다. 
 * @param {cpr.data.DataSet} pcDataObject : source DataSet
 * @param {cpr.data.DataSet} pcTargetDataObject : target DataSet
 * 
 * @alt
 *   pcDataObject(DataMap) 의 column을  복사하여 pcTargetDataObject(DataMap) 에 column을 추가한다. 
 * @param {cpr.data.DataMap} pcDataObject : source DataMap
 * @param {cpr.data.DataMap} pcTargetDataObject : target DataMap
 */
DataSetKit.prototype.copyToColumn = function(pcDataObject, pcTargetDataObject) {
	
	var vaHeaders = pcDataObject.getHeaders();
	
	//column copy 
	vaHeaders.forEach(function(eachHeader) {
		var vsName = eachHeader.getName();
		var voDataType = eachHeader.getDataType();
		var voColumnType = eachHeader.getColumnType();
		var vsInfo = eachHeader.getInfo();
		
		var createHeader = null;
		if (voDataType == cpr.data.tabledata.DataType.EXPRESSION) {
			createHeader = new cpr.data.header.ExpressionHeader(vsName, eachHeader.getExpression());
		} else {
			createHeader = new cpr.data.header.DataHeader(vsName, voDataType);
		}
		createHeader.setInfo(vsInfo);
		
		if (pcDataObject instanceof cpr.data.DataSet && pcTargetDataObject instanceof cpr.data.DataSet && voColumnType == cpr.data.header.HeaderType.DISPLAY_COLUMN) {
			pcTargetDataObject.addDisplayColumn(createHeader);
		} else {
			pcTargetDataObject.addColumn(createHeader);
		}
	});
};


/**
 * Dialog 유틸
 * @constructor
 * @param {common.AppKit} appKit
 */
function DialogKit(appKit) {
	this._appKit = appKit;
};

/**
 * 모달(Modal) 팝업을 호출한다.
 * <pre><code>
 * Dialog.open(app, "app/cmn/CMN001", 700, 500, function(dialog){...});<br>
 * <p>또는</p>
 * Dialog.open(app, "app/cmn/CMN001", 700, 500, function(dialog){...}, {key1:"value1", key2:"value2"},{left : 400, top : 200});
 * </code></pre>
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#app} appid 팝업 화면 주소
 * @param {Number} width 팝업 창의 가로 사이즈
 * @param {Number} height 팝업 창의 세로 사이즈(루트 레이아웃이 버티컬 레이아웃일 경우 -1적용시 자동크기를 적용)
 * @param {Function} handler 팝업이 닫힐 때 콜백함수(callback function)
 * @param {Object} initValue? 초기 파라메터 key/value쌍으로 팝업창에 넘길 파라메터 JSON 데이터 [예시)-{key1:"value1", key2:"value2"}]
 * @param {
 *   left? : Number <!--다이얼로그의 x좌표 (default : 가운데 위치) -->,
 *   top? : Number <!-- 다이얼로그의 y좌표 (default : 가운데 위치) -->,
 *   modal? : Boolean <!-- modal 여부 (default : true) -->,
 *   headerTitle? : String <!-- 다이얼로그 헤더 타이틀 (default : 앱 타이틀) -->,
 *   headerVisible? : Boolean <!-- 다이얼로그 헤더 보이기 여부 (default : true) -->,
 *   headerMovable? : Boolean <!--다이얼로그 헤더를 통해 이동 가능 여부 (default : true)  -->,
 *   headerClose? : Boolean <!--다이얼로그 헤더 close 버튼 보이기 여부 (default : true)  -->, 
 *   resizable? :Boolean <!-- 다이얼로그 Rect 부분에 크기 조정 가능 여부 (default : true) -->,
 *   headerMin? :Boolean <!--다이얼로그 헤더 최소화 버튼 보이기 여부 (default : false)  -->, 
 *   headerMax? :Boolean <!--  다이얼로그 헤더 최대화 버튼 보이기 여부 (default : false) -->,
 *   autoFocusedTarget? :cpr.controls.AutoFocusedTargetType <!--다이얼로그 열때 자동 포커스 설정 (default : auto) -->, 
 *   restoreFocus? :Boolean <!--다이얼로그가 닫힐 때 이전 포커스를 복원 여부(default: true) -->,
 *   escKeySupport? :Boolean <!--esc 키를 통해 닫기 기능 지원여부 설정 (default : false) -->
 *   } prop? 팝업 설정 속성
 */
DialogKit.prototype.open = function(app, appid, width, height, handler, initValue, prop) {
	if (initValue == null) {
		initValue = {};
	}
	
	//윈도우 최소 창크기보다 작은 경우... 윈도우 사이즈에 맞게 사이즈 조정
	var windowWidth = (window.innerWidth | document.body.clientWidth) - 10;
	var windowHeight = (window.innerHeight | document.body.clientHeight) - 45;
	if (windowWidth < width) width = windowWidth;
	if (windowHeight < height) height = windowHeight;
	
	var dialogProp = {
		width: Number(width) + 10,
		height: Number(height) + 45,
		modal: (prop && prop.modal != undefined) ? prop.modal : true,
		headerTitle: (prop && prop.headerTitle != "") ? prop.headerTitle : null,
		headerVisible: (prop && prop.headerVisible != undefined) ? prop.headerVisible : true,
		headerMovable: (prop && prop.headerMovable != undefined) ? prop.headerMovable : true,
		headerClose: (prop && prop.headerClose != undefined) ? prop.headerClose : true,
		resizable: (prop && prop.resizable != undefined) ? prop.resizable : false,
		headerMin: (prop && prop.headerMin != undefined) ? prop.headerMin : false,
		headerMax: (prop && prop.headerMax != undefined) ? prop.headerMax : false,
		autoFocusedTarget: (prop && prop.autoFocusedTarget != undefined) ? prop.autoFocusedTarget : "auto",
		restoreFocus: (prop && prop.restoreFocus != undefined) ? prop.restoreFocus : true,
		escKeySupport: (prop && prop.escKeySupport != undefined) ? prop.escKeySupport : false,
		openApp: app
	};

	if (height == -1) { // 자동 크기 적용시 최초 로드 시점 숨김
		dialogProp.left = -(dialogProp.width);
	} else if (prop != null && prop.left) {
		dialogProp.left = prop.left;
	}
	
	if (prop != null && prop.top) {
		dialogProp.top = prop.top;
	}
	var vsDialogId = app.id + "_" + appid;
	
	// App에서 Dialog
	var _this = this;
	var voDialogMngr = app.getRootAppInstance().dialogManager;
	voDialogMngr.openDialog(appid, vsDialogId, dialogProp, function( /* cpr.controls.Dialog */ dialog) {
		
		dialog.userAttr({
			"_originWidth" : dialogProp["width"],
			"_originHeight" : dialogProp["height"]
		});
		
		dialog.headerVisible = dialogProp.headerVisible;
		dialog.headerMovable = dialogProp.headerMovable;
		dialog.headerClose = dialogProp.headerClose;
		dialog.resizable = dialogProp.resizable;
		dialog.headerMin = dialogProp.headerMin;
		dialog.headerMax = dialogProp.headerMax;
		dialog.autoFocusedTarget = dialogProp.autoFocusedTarget;
		dialog.restoreFocus = dialogProp.restoreFocus;
		dialog.escKeySupport = dialogProp.escKeySupport;
		
		initValue._dialogRef = dialog;
		
		if(dialogProp.headerTitle){
			dialog.headerTitle = dialogProp.headerTitle;
		} else if (dialog.app.title) {
			dialog.headerTitle = dialog.app.title;
		}
		
		if (handler) {
			dialog.addEventListenerOnce("close", handler);
		}
		
		if (initValue) {
			dialog.initValue = initValue;
		}
		
		dialog.ready(function(dialogApp) {
			dialogApp.getEmbeddedAppInstance().setAppProperty("openApp", dialogProp["openApp"]);
			
			_this.setPopupInfo(dialog.getEmbeddedAppInstance(), "isPopup", true);
			_this.setPopupInfo(dialog.getEmbeddedAppInstance(), "modal", dialogProp.modal);
		})
		
		// height가 -1(자동)이고  루트레이아웃이 버티컬 레이아웃이면 autoHeight 적용
		if (height == -1) {
			dialog.addEventListenerOnce("load", function(e) {
				cpr.core.DeferredUpdateManager.INSTANCE.update();
				
				/** @type cpr.controls.Dialog */
				var voDialog = e.control;
				var voAppIns = voDialog.getEmbeddedAppInstance();
				var vcContainer = voAppIns.getContainer();
				var voLayout = vcContainer.getLayout();
				
				// 최상위 레이아웃이 버티컬 레이아웃일 경우 적용
				if (!(voLayout instanceof cpr.controls.layouts.VerticalLayout)) return;
				
				var vnAfterTop = 0;
				var vnAfterLeft = 0;
				var vnAfterHeight = 0;
				var vnDigMaxHeight = 900;
				
				if(typeof AppProperties !== 'undefined' && AppProperties.DIALOG_MAX_HEIGHT){
					vnDigMaxHeight = AppProperties.DIALOG_MAX_HEIGHT;
				}
	
				if (prop != null && prop.left) {
					vnAfterLeft = prop.left;
				} else {
					var vnWidth = dialogProp.width || vcContainer.getActualRect().width;
					var tempWid = (vnWidth != windowWidth) ? 5 : 0; // vnWidth=windowWidth 일 경우, 중간 위치를 맞추기 위함 
					vnAfterLeft = Math.abs((windowWidth/2) - (vnWidth/2)) + tempWid;
				}
				
				cpr.core.DeferredUpdateManager.INSTANCE.asyncExec(function() {
					// 내부 컨트롤 autoSize 후 처리
					_.delay(function(){
						if(voDialog.disposed) return;
						
						vnAfterHeight = vcContainer.getContentPaneRect().height;
						
						// 다이얼로그 header 높이 추가
						var vnHeaderHeight = voDialog.getActualRect().height - vcContainer.getActualRect().height;
						vnAfterHeight += vnHeaderHeight;
						
						// 다이얼로그 최대 height
						var vnRootHeight =  voDialog.getAppInstance().getActualRect().height;
						if (vnAfterHeight > vnDigMaxHeight) vnAfterHeight = vnDigMaxHeight;
						else if (vnAfterHeight > vnRootHeight) vnAfterHeight = vnRootHeight - 45; // 전체 화면 높이보다 다이얼로그 높이가 클 경우 높이조정
						
						// 다이얼로그 top -> 전체 height 의 중앙
						var vsDialogNm = voDialogMngr.getDialogName(voDialog);
						vnAfterTop = (vnRootHeight/2) - (vnAfterHeight/2);
						
						voDialogMngr.updateConstraintByName(vsDialogNm, {
							"top": vnAfterTop,
							"left": vnAfterLeft,
							"height": vnAfterHeight
						});
					}, 100);
				});				
			});
		}
	});
};

/**
 * 현재 앱이 팝업인지 여부를 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @return {Boolean} 
 */
DialogKit.prototype.isDialogPopup = function(app) {		
	return (!ValueUtil.isNull(app.getHost()) && app.getHost().modal != undefined && app.getHost().type == "dialog") ? true : false;
};

/**
 * window 팝업을 호출한다.<br>
 * <pre><code>
 * var voMap = new cpr.utils.ObjectMap();
 * voMap.put("CLX_PATH", "app/tst/tstPTaskRsltImg");
 * voMap.put("imgData", "imgData");
 * voMap.put("popUpCls", "rsltImg");
 * voMap.put("testImagePath", util.DataMap.getValue(app, "dmParamImg", "testImagePath"));<br>
 * util.Dialog.windowOpen(app, "/TstTaskMng/index.do", "_popup", voMap);
 * util.Dialog.windowOpen(app, "/TstTaskMng/index.do"", "_self", voMap)
 * </code></pre>
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {String} psActionUrl 팝업 aution URL
 * @param {#app} psPopId 팝업 ID
 * @param {cpr.utils.ObjectMap} pmParameter Parameter map
 * @param {Number} width?
 * @param {Number} height?
 * @param {Number} top?
 * @param {Number} left?
 * @param {Boolean} isModal?
 * @return {Boolean} 
 */
DialogKit.prototype.windowOpen = function(app, psActionUrl, psPopId, pmParameter, width, height, top, left, isModal) {
	var vnWidth = width == null ? window.screen.availWidth : width;
	var vnHeight = height == null ? window.screen.availHeight : height;
	var vnTop = top == null ? (window.screen.availHeight - height) / 2 : top;
	var vnLeft = left == null ? (window.screen.availWidth - width) / 2 : left;
	var initValue = {}
	if (vnTop < 0) vnTop = 0;
	if (vnLeft < 0) vnLeft = 0;
	var vbIsModal = isModal == null ? false : isModal;
	var vsProp = "menubar=0,resizable=yes,scrollbars=yes,status=0,top=" + vnTop + ",left=" + vnLeft + ",width=" + vnWidth + ",height=" + vnHeight;
	
	var openWindow = window.open("about:blank", psPopId, vsProp);
	var voPostMethod = new cpr.protocols.HttpPostMethod(psActionUrl, psPopId);
	
	if (pmParameter != null) {
		Object.keys(pmParameter).forEach(function(key, value){
	   		voPostMethod.addParameter(key, ValueUtil.fixNull(value));
		});
	}
	
	voPostMethod.submit();
	voPostMethod.dispose();
	
	window["_app"] = app;
	return openWindow;
};


/**
 * window 팝업을 오픈 한다.<br>
 * - jsp 서블릿을 제공하는 웹프로젝트 환경에서만 가능(cmnPopupModaless.jsp는 별도 요청 필요)<br>
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#app} psAppId 팝업으로 띄울 앱 ID
 * @param {String} windowName? 윈도우 이름<br>isOnlyOne이 true인 경우, 동일한 윈도우 이름에 대해서는 1개의 window만 생성합니다.
 * @param {top : Number <!-- 윈도우팝업의 y좌표 (default:가운데 위치) -->,
 *               left: Number <!-- 윈도우팝업의 x좌표 (default:가운데 위치) -->,
 *               width : Number <!-- 팝업 창의 가로 사이즈  -->,
 *               height : Number<!-- 팝업 창의 세로 사이즈  -->} prop 팝업 설정 속성
 * @param {Function} poCallBackFunc? 팝업이 닫힐 때 콜백함수
 * @param {Object} initValue? 초기 파라메터 key/value쌍으로 팝업창에 넘길 파라미터<br> 
 * 											예시) {key1:"value1", key2:"value2"}
 * @param {Boolean} pbOnlyOne? 동일한 windowName 에 대해 한개의 window 만 생성할지 여부(default false)
 * @return {Window} 
 */
DialogKit.prototype.openWindow = function(app, psAppId, windowName, prop, poCallBackFunc, initValue, pbOnlyOne) {

	var vbOlnyOne = (pbOnlyOne == null || pbOnlyOne == undefined) ? false : pbOnlyOne; //1개의 tltle에 1개의 widnow생성 여부
	
	var vnWidth = prop.width == null ? window.screen.availWidth : prop.width;
	var vnHeight = prop.height == null ? window.screen.availHeight : prop.height;
	var vnTop = prop.top == null ? ((window.screen.availHeight - prop.height) / 2) : prop.top;
	var vnLeft = prop.left == null ? ((window.screen.availWidth - prop.width) / 2) : prop.left;

	if (vnTop < 0) vnTop = 0;
	if (vnLeft < 0) vnLeft = 0;
	
	var vsProp = "menubar=0,resizable=yes,scrollbars=yes,status=0,top=" + vnTop + ",left=" + vnLeft + ",width=" + vnWidth + ",height=" + vnHeight;
	var vsWindowName = null;
	if (!ValueUtil.isNull(windowName)) {
		vsWindowName = String(windowName);
	} else {
		vsWindowName = "_popup";
	}
	
	if(pbOnlyOne == false) {
		vsWindowName += moment().format("YYYYMMDDHHmmssSSS");
	}
	
	var vsWinSeqValue = "window-" + moment().format("YYYYMMDDHHmmssSSS"); 
	var voWindow = window.open("about:blank", windowName, vsProp);
	
	// window popup 호출
	var vsPath = location.pathname.substring(0, location.pathname.lastIndexOf("ui/"));
	var vsJspPath = vsPath + "/cmnPopupModaless.jsp";
	var vsActionUrl= location.protocol + "//" + location.host + vsJspPath;
	
	var voPostMethod = new cpr.protocols.HttpPostMethod(vsActionUrl, vsWindowName);
	voPostMethod.addParameter("appId", psAppId);
	voPostMethod.addParameter("initValue", JSON.stringify(initValue));
	voPostMethod.addParameter("_openWindowSeq", vsWinSeqValue); // window 팝업창의 Sequence 부여 (하단의 addWinLoadEvent 에서 동일한 window 객체 호출되었는 체크용도)

	voPostMethod.submit();
	voPostMethod.dispose();
	
	var voApp = app;
	var checkWinLoad = null;
	voWindow.addEventListener("load", addWinLoadEvent(voApp, voWindow, 0));
	
	function addWinLoadEvent(poApp, poWindow, pnNum) {
		if (poWindow != null && poWindow.document != null &&
			poWindow.document.getElementById("openWindowSeq") != null &&
			poWindow.document.getElementById("openWindowSeq").value == vsWinSeqValue) {
			
			if(checkWinLoad) {
				clearTimeout(checkWinLoad);
				checkWinLoad = null;
			}
			
			poWindow.addEventListener("beforeunload", function(e) {
				if (!ValueUtil.isNull(poCallBackFunc) && TypeUtil.isFunc(poCallBackFunc)) {
					var returnValue = poWindow.returnValue;
					poCallBackFunc(returnValue);
				}
			});
			
			poWindow.openApp = poApp;
		} else {
			// poWindow 의 document 가 아직 생성되지 않은 경우, 재귀호출 30회 시도 (이 후는 오류로 간주함)
			if (pnNum < 30) {
				checkWinLoad = setTimeout(function() {
					addWinLoadEvent(poApp, poWindow, (pnNum + 1));
				}, 100);
			}
		}
	};
	
	return voWindow;
};

/**
 * 팝업이 appId를 기준으로 호출되어 있는지에 대한 여부를 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#app} appId 팝업 화면 주소
 * @param {Boolean} pbCurApp true: 현재 앱인스턴스에 팝업 띄움<br>
 *						 					false: 루트앱인스턴스에 팝업 띄움 (default:false)
 * @param {Boolean} pbFocus true: 해당하는 화면으로 포커스한다.<br>
 * 											false: 포커스하지 않는다. (default:false)
 * @return {Boolean} 
 */
DialogKit.prototype.isOpend = function(app, appId, pbCurApp, pbFocus) {

	var voAppInstance = app;
	if (!pbCurApp) {
		// 루트 앱인스턴스 설정
		voAppInstance = this._appKit.getMainApp(app);
	}
	
	var isOpend = false;
	var voDialogManager = voAppInstance.dialogManager;
	voDialogManager.getDialogNames().forEach(function(dialogName) {
		// 모든 다이알로그의 app.id를 비교하여 현재 넘겨받은 app id 와 비교.
		var dialog = voDialogManager.getDialogByName(dialogName);
		if (dialog.app.id == appId) {
			// 있을경우 true 로 반환;
			isOpend = true;
			if (pbFocus) {
				voDialogManager.activateDialogByName(dialogName);
			}
		}
	});
	
	return isOpend;
};

/**
 * 현재 앱을 다이얼로그로 호출한 앱인스턴스를 리턴한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @return {cpr.core.AppInstance} 
 */
DialogKit.prototype.getOpenerApp = function(app) {
	
	var voApp = null;
	
	if (!ValueUtil.isNull(app.getHost()) && this.getPopupInfo(app, "isPopup")) {
		voApp = app.getAppProperty("openApp");
	}
	
	if (voApp == null || voApp == undefined) {
		voApp = window["openApp"];
	}
	
	return voApp;
};

/**
 * 현재 앱을 다이얼로그로 호출한 앱 ID를 리턴한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @return {String} 
 */
DialogKit.prototype.getOpenerAppId = function(app) {
	
	if (!ValueUtil.isNull(app.getHost()) && this.getPopupInfo(app, "isPopup")) {
		var voApp = app.getAppProperty("openApp");
		if (!ValueUtil.isNull(voApp) && (voApp instanceof cpr.core.AppInstance)) {
			return ValueUtil.fixNull(voApp.app.id);
		} else {
			return null;
		}
	}
	return null;
};

/**
 * 특정 컨트롤을 화면의 상단에 플로팅 하여 띄운다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtrlId 플로팅으로 띄울 컨트롤ID
 * @param {#uicontrol} psTargetCtlId? 특정 컨트롤 위에 플로팅 할시 컨트롤ID
 * @param {Number} x x축 포지션 위치
 * @param {Number} y y축 포지션 위치
 * @param {Number} width 컨트롤 너비
 * @param {Number} height 컨트롤 높이
 */
DialogKit.prototype.floating = function(app, psCtrlId, psTargetCtlId, x, y, width, height) {
	
	/**@type cpr.controls.UIControl */
	var vcCtrl = app.lookup(psCtrlId);
	if (vcCtrl == null) return false;
	
	if (vcCtrl.visible === false) {
		vcCtrl.visible = true;
	}
	
	var showConstraint = {};
	var container = app.getContainer();
	if (psTargetCtlId != null) {
		var vcTargetCtrl = app.lookup(psTargetCtlId);
		if(vcTargetCtrl) showConstraint = vcTargetCtrl.getActualRect();
	} else {
		showConstraint = {
			"position": "absolute",
			"width": width + "px",
			"height": height + "px"
		};
		
		if (this.isDialogPopup(app)) {
			// 다이얼로그인 경우
			if (((y + height) - container.getActualRect().top) > container.getActualRect().height) {
				showConstraint.top = (y - (container.getActualRect().top + height)) + "px";
			} else {
				showConstraint.top = (y - container.getActualRect().top) + "px";
			}
			showConstraint.left = (x - (container.getActualRect().left + width)) + "px";
		} else {
			showConstraint.top = (y - 90) + "px";
			
			// 띄우려는 container의 width를 구함
			var containerWidth = container.getActualRect().width;
			// 만약 띄우려는 container 보다 띄우려는 포지션의 길이가 길 경우 ( x축 + width )
			if (x + width > containerWidth) {
				// 띄우려는 컨트롤이 오른쪽에서 10px 떨어진 만큼의 크기로 맞춘다.
				var controlLeft = controlLeft - width - 10;
				// 만약 떨어진 만큼의 크기가 왼쪽으로 넘어갈경우 0px로 맞춘다.
				if (controlLeft < 0) {
					controlLeft = 0;
				}
				// width 또한 container 사이즈에 맞춘다.
				showConstraint.left = controlLeft + "px";
				showConstraint.width = containerWidth + "px";
			} else {
				showConstraint.left = x + "px";
			}
		}
	}
	
	// floating하기 전에 해당 컨트롤의 부모객체 정보를 저장함
	var voFloatingMap = app.getAppProperty("__floatingMap");
	var map = voFloatingMap ? voFloatingMap : new cpr.utils.ObjectMap();
	map.put(vcCtrl.id, vcCtrl.getParent());
	if (voFloatingMap == null || voFloatingMap == undefined) {
		app.setAppProperty("__floatingMap", map);
	}
	
	var layout = container.getLayout();
	if (layout instanceof cpr.controls.layouts.FormLayout ||
		layout instanceof cpr.controls.layouts.VerticalLayout) {
		app.floatControl(vcCtrl, showConstraint);
	} else {
		container.addChild(vcCtrl, showConstraint);
	}
};

/**
 * 앱화면의 상단에 플로팅으로 띄워진 컨트롤/컨트롤그룹을 제거한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtrlId 플로팅으로 띄워진 컨트롤ID
 */
DialogKit.prototype.removefloating = function(app, psCtrlId) {
	var ctrl = app.lookup(psCtrlId);
	if (ctrl == null) return false;
	
	var container = app.getContainer();
	var layout = container.getLayout();
	if (layout instanceof cpr.controls.layouts.FormLayout ||
		layout instanceof cpr.controls.layouts.VerticalLayout) {
		app.removeFloatingControl(ctrl);
	} else {
		container.removeChild(ctrl);
	}
	
	var voFloatingMap = app.getAppProperty("__floatingMap");
	if (voFloatingMap) {
		var parent = voFloatingMap.get(ctrl.id);
		if (ctrl.visible != false) {
			ctrl.visible = false;
		}
		parent.addChild(ctrl);
		var tempFloatginMap = voFloatingMap.remove(ctrl.id)
		app.setAppProperty("__floatingMap", tempFloatginMap);
	}
};

/**
 * 윈도우 팝업으로 오픈된 화면의 initValue를 반환한다.<br/>
 * initValue는 util.Dialog.openWindow 를 통해서 전달한 경우에 한해서 확인이 가능하다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @return {any} initValue
 */
DialogKit.prototype.getWinPopInitValue = function(app) {
	if (this.getPopupInfo(app, "popupType") == "WIN") {
		return app.getAppProperty("_winPopInitValue");
	}
	return null;
};

/**
 * 윈도우 팝업을 close 한다.<br/>
 * util.Dialog.openWindow 를 통해 오픈된 윈도우 팝업만을 대상으로 하며, close시, returnValue 로 함께 전달한다.
 * @param {cpr.core.AppInstance} app
 * @param {any} returnValue
 */
DialogKit.prototype.closeWinPopup = function(app, returnValue) {
	if (this.getPopupInfo(app, "popupType") == "WIN") {
		window["returnValue"] = returnValue;
		window.close();
	}
}

/**
 * 팝업 정보를 반환 한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {"isPopup" | "popupType" | "modal"} psDiv? 반환 받고자 하는 팝업 정보 구분 ["isPopup" | "popupType" | "modal"]
 * @return {"isPopup"?:Boolean <!-- 팝업 여부 -->,
 *             "popupType"?:"WIN"|"MOD"|"MOL" <!-- MOD : 모달 <br> MOL : 모달리스<br> WIN : 윈도우-->,
 *             "modal"?:Boolean <!-- 다이얼로그 modal 여부(true:모달 / false:모달리스) -->}
 */
DialogKit.prototype.getPopupInfo = function(app, psDiv) {
	/**
	 * 재귀적으로 부모를 찾아  현재 App이 팝업 내부에 있는 App인지 여부를 반환
	 * @param {cpr.core.AppInstance} poApp
	 * @return {Boolean}
	 */
	function getIsPopup(poApp) {
		/** @type cpr.core.AppInstance */
		var _app = poApp;
		var vcHost = _app.getHost();
		var voHostApp = _app.getHostAppInstance();
		
		var vbResult;
		if (_app.isRootAppInstance()) {
			vbResult = false;
		} else if (vcHost instanceof cpr.controls.Dialog) {
			vbResult = true;
		} else {
			vbResult = getIsPopup(voHostApp);
		}
		
		return vbResult;
	}
	
	var voPoupInfo = app.getAppProperty("_popupInfo");
	if (ValueUtil.isNull(voPoupInfo)) {
		/* 
		 * MDI 구조가 아닌 화면에서 팝업 내부에 임베디드앱이 있고, 해당 임베디드앱 내부의 UDC 에서 서브미션 통신을 하였을 경우
		 * app 객체에 팝업 정보가 없어 잘못된 값을 반환 할 수 있으므로 재귀적으로 Dialog 여부를 반환 한다.
		 */
		if (app.state == "initializing" || app.getHost() instanceof cpr.controls.EmbeddedApp) {
			return {
				/*
				 * 현재 앱이 초기화 되고 경우 app객체에 팝업 정보가 없기 때문에 재귀적으로 Dialog 여부를 반환 한다.
				 * UDC내부에서 load이벤트에서 서브미션이 호출 될  경우 부모앱이 초기화 중인 시점임
				 */
				isPopup: getIsPopup(app)
			}
		} 
		return {};
	} else {
		if (ValueUtil.isNull(psDiv)) {
			return voPoupInfo;
		} else {
			return voPoupInfo[psDiv];
		}
	}
};

/**
 * 앱 인스턴스에 팝업 정보를 저장한다.
 * 저장된 정보는 util.Dialog.getPopupInfo 로 확인할 수 있다.
 * @param {cpr.core.AppInstance} app 앱 인스턴스
 * @param {"isPopup" | "popupType" | "modal"} psDiv 저장하고자 하는 팝업 정보 구분
 * @param {any} psValue
 */
DialogKit.prototype.setPopupInfo = function(app, psDiv, psValue) {
	
	var voPoupInfo = app.getAppProperty("_popupInfo");
	if (ValueUtil.isNull(voPoupInfo)) {
		var voData = {};
		voData[psDiv] = psValue;
		app.setAppProperty("_popupInfo", voData);
	} else {
		var voTempPopupInfo = _.clone(voPoupInfo);
		voTempPopupInfo[psDiv] = psValue;
		app.setAppProperty("_popupInfo", voTempPopupInfo);
	}
}


/**
 * Embeded앱 컨트롤 유틸
 * @constructor
 * @param {common.AppKit} appKit
 */
function EmbeddedAppKit(appKit) {
	this._appKit = appKit;
};

/**
 * Embeded 앱내의 함수를 호출한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#embeddedapp} psEmbeddedappId 임베디드 앱 ID
 * @param {String} psFuncName 호출 함수명
 * @param {String | Array} paArgs 함수에 전달할 arguments. spread 형태로 해당 인자값 부터 모두 arguments로 넘어간다 
 * @return {any} method 내 파라미터
 */
EmbeddedAppKit.prototype.callAppMethod = function(app, psEmbeddedappId, psFuncName, paArgs) {
	/** @type cpr.controls.EmbeddedApp */
	var emb = app.lookup(psEmbeddedappId);
	var value = null;
	
	// paArgs 함수부터 입력받은 모든 arguments를 받아온다.
	var vaArgs = Array.apply(this, arguments).splice(3);
	
	if (emb) {
		emb.ready(function(e) {
			if (!e.hasAppMethod(psFuncName)) {
				this._appKit.Msg.alert("The embeded page not have " + psFuncName + " function! (script error)");
				return null;
			}
			value = e.getEmbeddedAppInstance().applyAppMethod(psFuncName, vaArgs);
		});
	}
	return value;
};

/**
 * 임베디드 앱을 포함하고 있는 Host앱의 특정 함수를 호출한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {String} psFuncName 호출 함수명
 * @param {String | Array} paArgs? 함수에 전달할 arguments. spread 형태로 해당 인자값 부터 모두 arguments로 넘어간다 
 * @return {any} 반환값
 */
EmbeddedAppKit.prototype.callHostAppMethod = function(app, psFuncName, paArgs) {
	/** @type cpr.core.AppInstance */
	var hostApp = app.getHostAppInstance();
	
	// paArgs 함수부터 입력받은 모든 arguments를 받아온다.
	var vaArgs = Array.apply(this, arguments).splice(2);
	
	if (hostApp && hostApp.hasAppMethod(psFuncName)) {
		return hostApp.applyAppMethod(psFuncName, vaArgs);
	}
	return null;
};

/**
 * 해당 임베디드 앱에 연결된 페이지의 앱 APP가 존재하는지 여부를 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#embeddedapp} psEmbeddedappId 임베디드 앱 ID
 * @return {Boolean} 임베디드 앱 유/무 반환
 */
EmbeddedAppKit.prototype.hasPage = function(app, psEmbeddedappId) {
	/** @type cpr.controls.EmbeddedApp */
	var emb = app.lookup(psEmbeddedappId);
	return (emb && emb.app) ? true : false;
};

/**
 * Embeded 앱에 호출할 화면을 설정한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#embeddedapp} psEmbeddedappId 임베디드 앱 ID
 * @param {#app} psAppId 호출할 화면 앱ID
 * @param {any} poInitValue? 초기 파라메터
 */
EmbeddedAppKit.prototype.setPage = function(app, psEmbeddedappId, psAppId, poInitValue) {
	/** @type cpr.controls.EmbeddedApp */
	var emb = app.lookup(psEmbeddedappId);
	return new Promise(function(resolve, reject) {
		if (emb) {
			cpr.core.App.load(psAppId, function(loadedApp) {
				if (loadedApp) {
					/* 로드된 앱을 임베디드 앱에 설정*/
					emb.app = loadedApp;
					emb.ready(function(e) {
						/* 커스텀 옵션 : 로드시 초기 파라미터 전달*/
						//emb.initValue = poInitValue;
						if (!ValueUtil.isNull(poInitValue)) {
							emb.setAppProperty("initValue", poInitValue);
						}
						resolve(emb);
					});
				}
			});
		} else {
			throw new cpr.exceptions.IllegalArgumentException("not found embedded app");
		}
	});
};

/**
 * 임베디드 컨트롤에 포함되어있는 앱객체들을 제거합니다. 
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#embeddedapp} psEmbeddedappId 임베디드 앱 ID
 */
EmbeddedAppKit.prototype.dispose = function(app, psEmbeddedappId) {
	/** @type cpr.controls.EmbeddedApp */
	var emb = app.lookup(psEmbeddedappId);
	if (emb && emb.getEmbeddedAppInstance()) {
		emb.getEmbeddedAppInstance().dispose();
	}
};

/**
 * FreeForm컨트롤 유틸<br>
 * - 일반적으로 그리드가 바인딩되었거나 데이터셋을 사용하는 폼레이아웃 컨트롤에 적용<br>
 * - 그리드 + 상세(폼레이아웃) 화면에서 주로 사용 <br>
 * - 바인드컨텍스트가 지정된 그리드 및 트리컨트롤의 데이터셋 제어 
 * @constructor
 * @param {common.AppKit} appKit
 */
function FreeFormKit(appKit) {
	this._appKit = appKit;
};

/**
 * 입력용 폼레이아웃 컨트롤들에 대해 초기화 로직을 수행한다.<br><br>
 * 
 * <b><제약사항></b><br>
 * - 폼 레이아웃의 문맥 바인딩이 "선택 행 컨텍스트" 또는 "데이터 로우 컨텍스트" 로 설정되어 있는 경우로 제한한다.<br><br>
 * 
 * <b><수행 로직></b><br>
 * 1. 데이터 여부에 따른 비활성화 처리 (load, filter 이벤트)<br>
 *    (데이터가 없으면 입력 안됨 처리)<br>
 * 2. udcComAppHeader 의 앱속성(freeformIds)에 지정한 폼 레이아웃 ID 에 포함될 경우 초기화 지정<br>
 * 	  앱속성(freeformIds) 미작성 시, 문맥 바인딩이 설정된 모든 폼 레이아웃을 대상으로 초기화 지정(조회조건 그룹 제외)<br><br>
 * 
 *  - 사이트별 Customizing 필요
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#container | Array} paFreeFormId 프리폼 ID 또는 ID배열
 */
FreeFormKit.prototype.init = function(app, paFreeFormId) {
	if (!(paFreeFormId instanceof Array)) {
		paFreeFormId = [paFreeFormId];
	}
	/**@type cpr.controls.Container */
	var vcForm = null
	/** @type cpr.bind.BindContext */
	var voBindContext = null;
	/** @type cpr.data.DataSet */
	var voDs = null;
	
	var voMap = new cpr.utils.ObjectMap();
	var voBindMap = new cpr.utils.ObjectMap();
	
	for (var i = 0, len = paFreeFormId.length; i < len; i++) {
		vcForm = app.lookup(paFreeFormId[i]);
		if (vcForm == null) continue;
		
		/* 프리폼 초기화 제외 */ 
		if(vcForm.userAttr("ignoreInit") === "Y") continue;
			
		voBindContext = this._appKit.Group.getBindContext(app, vcForm);
		
		if (voBindContext) {
			voDs = this._appKit.Group.getBindDataSet(app, vcForm, voBindContext);
			
			vcForm.userData("_originEnabled", vcForm.enabled);
			if (vcForm.getBindInfo("enabled") != null) {
				vcForm.userData("_expressEnabled", vcForm.getBindInfo("enabled").expression);
			}
			
			if (voDs.userData("_freeforms")) {
				var tempFreeForms = voDs.userData("_freeforms");
				tempFreeForms.push(vcForm.id);
				voDs.userData("_freeforms", tempFreeForms);
			} else {
				voDs.userData("_freeforms", [vcForm.id])
			}
		}
		
		var childCtrls = vcForm.getAllRecursiveChildren();
		childCtrls.forEach(function(ctrl) {
			if (ctrl.type == "numbereditor") {
				if (ctrl.spinButton != false && ctrl.style.css("text-align") == "") {
					ctrl.style.css({
						"text-align": "center"
					});
				}
				if ((ctrl.format === "0000" || ctrl.format === "9999") && ctrl.max == 0) {
					ctrl.max = 1.7976931348623157E308;
				}
			}
		});
		
		if (voDs) {
			if (voMap.get(voDs.id) == null) {
				voMap.put(voDs.id, voDs);
			}
			
			if (voBindContext.grid == null && voBindMap.get(voDs.id) == null) {
				voBindMap.put(voDs.id, voDs);
				voDs.stateRestore = true; //현재값과 Origin이 같으면... 변경없도록 처리
			}
		}
	}
	var _app = app,
		_appKit = this._appKit;
	
	voMap.keys().forEach(function(key) {
		voMap.get(key).addEventListener("load", function( /* cpr.events.CDataEvent */ e) {
			var dataset = e.control;
			var freeforms = dataset.userData("_freeforms");
			freeforms.forEach(function( /* eachType */ formId) {
				/**@type cpr.controls.Container */
				var form = _app.lookup(formId);
				if(form) {
					//데이터가 없으면... 프리폼 비활성화
					if (dataset.getRowCount() < 1) {
						if (form.userData("_expressEnabled")) {
							form.unbind("enabled");
						}
						form.enabled = false;
					} else {
						//데이터가 있으면  프리폼 활성화
						if (form.userData("_originEnabled") !== false) {
							if (form.userData("_expressEnabled")) {
								form.bind("enabled").toExpression(form.userData("_expressEnabled"));
							} else {
								form.enabled = true;
							}
						}
					}
				}
			});
		});
		
		voMap.get(key).addEventListener("filter", function( /* cpr.events.CDataEvent */ e) {
			var dataset = e.control;
			var freeforms = dataset.userData("_freeforms");
			freeforms.forEach(function( /* eachType */ formId) {
				/**@type cpr.controls.Container */
				var form = _app.lookup(formId);
				//데이터가 없으면... 프리폼 비활성화
				if (dataset.getRowCount() < 1) {
					if (form.userData("_expressEnabled")) {
						form.unbind("enabled");
					}
					form.enabled = false;
				} else {
					//데이터가 있으면 있고, 조회권한이 아니고... 프리폼 활성화
					if (form.userData("_originEnabled") !== false) {
						if (form.userData("_expressEnabled")) {
							form.bind("enabled").toExpression(form.userData("_expressEnabled"));
						} else {
							form.enabled = true;
						}
					}
				}
			});
		});
	});
};

/**
 * 프리폼(폼레이아웃)에 신규 행(Row)을 추가한다.<br>
 * 바인드컨텍스트로 연결된 트리 및 그리드 컨트롤의 insertrow 수행<br>
 *  - 사이트별 Customizing 필요
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#container} psFreeFormId 프리폼 ID
 * @param {#column} psEditCol 신규 이후 포커스 COLUMN명
 * @param {Number} pnRowIdx? 추가하고자 하는 Row index<br>
 *                 (defalut : 현재 선택된 로우 이후)
 * @param {Object} poRowData? 추가할 row data (key: header명, value: value 를 갖는 json data)
 * @return {cpr.controls.provider.GridRow} 추가한 Row의 GridRow 객체
 */
FreeFormKit.prototype.insertRow = function(app, psFreeFormId, psEditCol, pnRowIdx, poRowData) {
	/**@type cpr.controls.Container */
	var vcForm = app.lookup(psFreeFormId);
	var voBindContext = this._appKit.Group.getBindContext(app, vcForm);
		
	var vcBindCtl = this._appKit.Group.getBindControl(app, vcForm, voBindContext);
	/** @type cpr.data.DataSet */
	var voDs = this._appKit.Group.getBindDataSet(app, vcForm, voBindContext);
	var rowIndex = -1;
	if (!ValueUtil.isNull(pnRowIdx)) {
		rowIndex = pnRowIdx;
	} else {
		rowIndex = this._appKit.Group.getBindCtlRowIndex(app, voBindContext);
	}
	
	// InsertRow
	var insertedRow = null;
	if (poRowData != null) {
		insertedRow = voDs.insertRowData(rowIndex, true, poRowData);
	} else {
		// 트리
		if (vcBindCtl instanceof cpr.controls.Tree) {
			var vsSelVal = ValueUtil.fixNull(vcBindCtl.value);
			
			var voRow = {};
			voRow[vcBindCtl.itemSetConfig.label] = "";
			voRow[vcBindCtl.itemSetConfig.value] = "";
			voRow[vcBindCtl.itemSetConfig.parentValue] = vsSelVal;
			
			insertedRow = voDs.insertRowData(rowIndex, true, voRow);
			if (vsSelVal != "") {
				var voItem = vcBindCtl.getItemByValue(vsSelVal);
				vcBindCtl.expandItem(voItem);
			}
		} else {
			insertedRow = voDs.insertRow(rowIndex, true);
		}
	}
	
	// SelectRow
	if (vcBindCtl instanceof cpr.controls.Tree) {
		// 트리
		vcBindCtl.selectItemByValue("DEFAULT", true);
		// 1.0.3795(2022-05-13) 버전에서 트리의 focusItem API가 revealItem으로 변경
		vcBindCtl.revealItem(vcBindCtl.getItem(insertedRow.getIndex()));
	} else if (vcBindCtl instanceof cpr.controls.Grid) {
		// 그리드		
		vcBindCtl.selectRows(insertedRow.getIndex(), true);
	} else {
		// 프리폼
		vcForm.redraw();
	}
	
	if(voDs.userData("_freeforms")) {
		var _app = app;
		voDs.userData("_freeforms").forEach(function(formId){
			var vcFormCtrl = app.lookup(formId);
			if(vcFormCtrl){
				if(vcFormCtrl.userData("_expressEnabled")){
					vcFormCtrl.bind("enabled").toExpression(vcFormCtrl.userData("_expressEnabled"));
				}else{
					vcFormCtrl.enabled = true;
				}
			}
		});
	} else {
		if (vcForm.userData("_expressEnabled")) {
			vcForm.bind("enabled").toExpression(vcForm.userData("_expressEnabled"));
		} else {
			vcForm.enabled = true;
		}
	}
	
	// Focus
	if (psEditCol) {
		var vcCtrl = this._appKit.Group.getDataBindedControl(app, vcForm.id, psEditCol);
		if (vcCtrl) vcCtrl.focus();
	}
	
	return insertedRow;
};

/**
 * 프리폼(폼레이아웃)에 행(Row)을 삭제한다.<br>
 * 바인드컨텍스트로 연결된 트리 및 그리드 컨트롤의 delete로직 수행<br>
 * - 사이트별 Customizing 필요
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#container} psFreeFormId 프리폼 ID
 * @param {String} psAftMsg? 메시지 유형(CRM)
 * @param {"confirmCallback" : Function <!-- "CRM" confirm창 사용시 확인에 대한 콜백 -->
 *         ,"cancelCallback" : Function <!-- "CRM" confirm창 사용시 취소(닫기)에 대한 콜백 -->} poOption? "CRM" confirm 창에 대한 콜백 함수 
 * @return void
 */
FreeFormKit.prototype.deleteRow = function(app, psFreeFormId, psAftMsg, poOption) {
	var _this = this;
	/** @type cpr.controls.Container */
	var vcForm = app.lookup(psFreeFormId);
	/** @type cpr.bind.BindContext */
	var voBindContext = this._appKit.Group.getBindContext(app, vcForm);
		
	var voDs = this._appKit.Group.getBindDataSet(app, vcForm, voBindContext);
	var vcBindCtl = this._appKit.Group.getBindControl(app, vcForm, voBindContext);
	
	var rowIndex = this._appKit.Group.getBindCtlRowIndex(app, voBindContext);
	if (voDs == null || voDs.getRowCount() < 1) {
		//삭제할 데이터가 없습니다.
		this._appKit.Msg.alertDlg(app, "INF-M007");
	} else {
		if (!ValueUtil.isNull(psAftMsg)) {
			//삭제하시겠습니까?
            var poConfrimOption = {
                "confirmCallback" : function() {
                    if(voDs.getRowState(rowIndex) == cpr.data.tabledata.RowState.INSERTED){
                        voDs.revertRow(rowIndex);
                        vcForm.redraw();
                        if(vcBindCtl instanceof cpr.controls.Grid){
                            vcBindCtl.redraw();
                            //가장 마지막 행에서 신규 행 추가 후, 삭제할 경우에 가장 마지막 행을 선택해줌
                            if(voDs.getRowCount() -1 < rowIndex){
                                vcBindCtl.selectRows([rowIndex-1]);
                            }
                        } 
                        if(vcBindCtl instanceof cpr.controls.Tree) vcBindCtl.redraw();
                        //데이터 건수가 없으면... 프리폼 비활성화
                        if(voDs.getRowCount() < 1) {
                        	if (voDs.userData("_freeforms")) {
                        		var _app = app;
                        		voDs.userData("_freeforms").forEach(function(formId) {
                        			var vcFormCtrl = app.lookup(formId);
                        			if (vcFormCtrl) {
                        				if (vcFormCtrl.userData("_expressEnabled")) {
                        					vcFormCtrl.bind("enabled").toExpression(vcFormCtrl.userData("_expressEnabled"));
                        				} else {
                        					vcFormCtrl.enabled = false;
                        				}
                        			}
                        		});
                        	} else {
                        		if (vcForm.userData("_expressEnabled")) {
                        			vcForm.bind("enabled").toExpression(vcForm.userData("_expressEnabled"));
                        		} else {
                        			vcForm.enabled = false;
                        		}
                        	}
                        }
                        return false;
                    }else{
                        voDs.setRowState(rowIndex, cpr.data.tabledata.RowState.DELETED);
                        if(typeof(poOption.confirmCallback) == "function") {
                            poOption.confirmCallback(); // 화면에서 전달받은 close 콜백 
                        }
                    }
                }
            }
            this._appKit.Msg.confirmDlg(app, "CRM-M002", null, poConfrimOption);
		} else {
			if (voDs.getRowState(rowIndex) == cpr.data.tabledata.RowState.INSERTED) {
				voDs.revertRow(rowIndex);
			} else {
				voDs.setRowState(rowIndex, cpr.data.tabledata.RowState.DELETED);
			}
		}
	}
	
	return false;
};

/**
 * 프리폼(폼레이아웃)에 바인딩된 값을 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#container} psFreeFormId 프리폼 ID
 * @param {#column} psColumnName 컬럼명
 * @return {String} 프리폼의 컬럼값
 */
FreeFormKit.prototype.getValue = function(app, psFreeFormId, psColumnName) {
	/** @type cpr.controls.Container */
	var vcForm = app.lookup(psFreeFormId);
	var voBindContext = this._appKit.Group.getBindContext(app, vcForm);
	
	/** @type cpr.data.DataSet */
	var voDs = this._appKit.Group.getBindDataSet(app, vcForm, voBindContext);
	var rowIndex = this._appKit.Group.getBindCtlRowIndex(app, voBindContext);
	
	return voDs.getValue(rowIndex, psColumnName);
};

/**
 * 프리폼(폼레이아웃)에 바인딩된 값을 변경한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#container} psFreeFormId 프리폼 ID
 * @param {#column} psColumnName 컬럼명
 * @param {String} psValue 변경하고자 하는 값
 * @return void
 */
FreeFormKit.prototype.setValue = function(app, psFreeFormId, psColumnName, psValue) {
	/** @type cpr.controls.Container */
	var vcForm = app.lookup(psFreeFormId);
	var voBindContext = this._appKit.Group.getBindContext(app, vcForm);
	
	/** @type cpr.data.DataSet */
	var voDs = this._appKit.Group.getBindDataSet(app, vcForm, voBindContext);
	var rowIndex = this._appKit.Group.getBindCtlRowIndex(app, voBindContext);
	
	voDs.setValue(rowIndex, psColumnName, psValue);
	vcForm.redraw();
};

/**
 * 프리폼(폼레이아웃) 내의 특정 컬럼을 포커싱한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#container} psFreeFormId 프리폼 ID
 * @param {#column} psColumnName 포커싱할 컬럼명
 */
FreeFormKit.prototype.setFocus = function(app, psFreeFormId, psColumnName) {
	/** @type cpr.controls.Container */
	var vcForm = app.lookup(psFreeFormId);
	
	var vcCtrl = this._appKit.Group.getDataBindedControl(app, vcForm.id, psColumnName);
	if (vcCtrl) this._appKit.Control.setFocus(app, vcCtrl.id);
};

/**
 * 프리폼(폼레이아웃)의 변경사항을 되돌린다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#container} psFreeFormId 프리폼 ID
 * @param {Number} pnRowIndex? 되돌릴 행의 index
 * @param {#column} psEditCol? 포커싱할 컬럼명
 * @return void
 */
FreeFormKit.prototype.revertRow = function(app, psFreeFormId, pnRowIndex, psEditCol) {
	/**@type cpr.controls.Container */
	var vcForm = app.lookup(psFreeFormId);
	var voBindContext = this._appKit.Group.getBindContext(app, vcForm);		
	var voBindCtl = this._appKit.Group.getBindControl(app, vcForm, voBindContext);
	
	/**@type cpr.data.DataSet */
	var voDs = this._appKit.Group.getBindDataSet(app, vcForm, voBindContext);
	
	var vnRowIndex = 0;
	if (!ValueUtil.isNull(pnRowIndex)) {
		vnRowIndex = pnRowIndex;
	} else {
		vnRowIndex = this._appKit.Group.getBindCtlRowIndex(app, voBindContext);
	}
	
	//데이터 Revert
	var rowData = voDs.getRow(vnRowIndex).getRowData();
	
	var vsGridRowState = "";
	if (voBindCtl instanceof cpr.controls.Grid) {
		vsGridRowState = voBindCtl.getRowState(vnRowIndex);
	}
	
	for (var column in rowData) {
		voDs.setValue(vnRowIndex, column, voDs.getOriginalValue(vnRowIndex, column));
	}
	
	voDs.getRow(vnRowIndex).setState(cpr.data.tabledata.RowState.UNCHANGED);
	
	if (vsGridRowState == cpr.data.tabledata.RowState.INSERTED) {
		if (voBindCtl instanceof cpr.controls.Grid) {
			voBindCtl.setRowState(vnRowIndex, vsGridRowState);
		}		
	}
	
	if (voBindCtl) voBindCtl.redraw();
	
	vcForm.redraw();
	
	if (!ValueUtil.isNull(psEditCol)) {
		var vcCtrl = this._appKit.Group.getDataBindedControl(app, vcForm.id, psEditCol);
		if (vcCtrl) vcCtrl.focus();
	}
};

/**
 * 프리폼(폼레이아웃)의 변경사항을 되돌린다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#container} psFreeFormId 프리폼 ID
 * @return void
 */
FreeFormKit.prototype.revertAllData = function(app, psFreeFormId) {
	/**@type cpr.controls.Container */
	var vcForm = app.lookup(psFreeFormId);
	var voBindContext = this._appKit.Group.getBindContext(app, vcForm);		
	var voBindCtl = this._appKit.Group.getBindControl(app, vcForm, voBindContext);	
	/**@type cpr.data.DataSet */
	var voDs = this._appKit.Group.getBindDataSet(app, vcForm, voBindContext);
	
	//데이터 Revert
	voDs.revert();
	if (voBindCtl) {
		voBindCtl.redraw();
	}
	
	vcForm.redraw();
};

/**
 * 프리폼(폼레이아웃)의 변경사항 유/무를 반환를 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#container | Array} paFreeFormId 프리폼 ID
 * @param {"MSG" | "CRM"} psAftMsg? 
 *						MSG : 변경사항 내역이 없을 경우 '변경된 내역이 없습니다.' 메세지 출력<br>
 *  					CRM : 변경내역이 존재할경우 '변경사항이 반영되지 않았습니다. 계속 하시겠습니까?' confirm 메시지출력
 * @param {"confirmCallback" : Function <!-- "CRM" confirm창 사용시 확인에 대한 콜백 -->
 *         ,"cancelCallback" : Function <!-- "CRM" confirm창 사용시 취소(닫기)에 대한 콜백 -->} poOption? "CRM" confirm 창에 대한 콜백 함수 
 * @return {Boolean} 데이터 변경 여부
 */
FreeFormKit.prototype.isModified = function(app, paFreeFormId, psAftMsg, poOption) {
	if (!(paFreeFormId instanceof Array)) {
		paFreeFormId = [paFreeFormId];
	}
	
	psAftMsg = psAftMsg == null ? "" : psAftMsg;
	
	var modify = false;
	var vcGroup = null;
	for (var i = 0, len = paFreeFormId.length; i < len; i++) {
		if (paFreeFormId[i] instanceof cpr.controls.Container) {
			vcGroup = paFreeFormId[i];
		} else {
			vcGroup = app.lookup(paFreeFormId[i]);
		}
		
		var voDataSet = this._appKit.Group.getBindDataSet(app, vcGroup);
		if (voDataSet != null && voDataSet.isModified()) {
			modify = true;
			break;
		}
	}
	
	if (modify) {
        if(psAftMsg.toUpperCase() == "CRM"){//변경사항이 반영되지 않았습니다. 계속 하시겠습니까? confirm
            this._appKit.Msg.confirmDlg(app, "CRM-M003", [vcGroup.fieldLabel], poOption);
            return true;
        } else {
            return modify;
        }
	} else {
		if (psAftMsg.toUpperCase() == "MSG") { //변경된 내역이 없습니다.
			this._appKit.Msg.notify(app, "INF-M006");
		}
	}
	
	return modify;
};

/**
 * 프리폼(폼레이아웃) 특정 행의 가시성 여부를 지정합니다.<br>
 * (반응형 모듈 사용시 스크린 별로 레이아웃의 행/열 정보가 변경되므로
 * 해당 api를 사용하여 가시성 여부 지정)
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#container | Array} paFreeFormId 프리폼 ID
 * @param {Number | Number[]} paIndex 행 인덱스 또는 행 인덱스 배열
 * @param {Boolean} pbVisible visible 여부(default : true)
 */
FreeFormKit.prototype.setRowVisible = function(app, paFreeFormId, paIndex, pbVisible) {
	if (!(paFreeFormId instanceof Array)) {
		paFreeFormId = [paFreeFormId];
	}
	
	if (!(paIndex instanceof Array)) {
		paIndex = [paIndex];
	}
	
	pbVisible = pbVisible == null || pbVisible == undefined ? true : pbVisible;
	
	/** @type cpr.controls.Container */
	var vcFreeForm = null;
	var screenNm = app.targetScreen.name;
	
	for (var i = 0, len = paFreeFormId.length; i < len; i++) {
		if (paFreeFormId[i] instanceof cpr.controls.Container) {
			vcFreeForm = paFreeFormId[i];
		} else {
			vcFreeForm = app.lookup(paFreeFormId[i]);
		}
		
		if(!vcFreeForm || !(vcFreeForm instanceof cpr.controls.Container) 
			|| !(vcFreeForm.getLayout() instanceof cpr.controls.layouts.FormLayout)) continue;
		
		/** @type cpr.controls.layouts.FormLayout */			
		var voLayout = vcFreeForm.getLayout();
		/** @type RForm */
		var voRForm = vcFreeForm["_RForm"]; // 반응형 폼 레이아웃 생성자	
				
		for (var j = 0, len = paIndex.length; j < len; j++){	
			if(voRForm){
				if(screenNm == voRForm._screenNms["tablet"]){
					var vsTabletColCnt = vcFreeForm.userAttr(voRForm.ATTR_NM.ATTR_TABLET_COLUMN_COUNT);
					if(vsTabletColCnt && Number(vsTabletColCnt) > 0){
						// 반응형 폼 레이아웃 api로 처리
						voRForm.setRowVisible(screenNm, paIndex, pbVisible);
						break;	
					}
				} else if(screenNm == voRForm._screenNms["mobile"]){
					var vsMobileColCnt = vcFreeForm.userAttr(voRForm.ATTR_NM.ATTR_MOBILE_COLUMN_COUNT);
					if(vsMobileColCnt && Number(vsMobileColCnt) > 0){
						// 반응형 폼 레이아웃 api로 처리
						voRForm.setRowVisible(screenNm, paIndex, pbVisible);
						break;
					}
				}
			}
			
			voLayout.setRowVisible(paIndex[j], pbVisible);						
		}
	}	
};

/**
 * 프리폼(폼레이아웃) 특정 열의 가시성 여부를 지정합니다.<br>
 * (반응형 모듈 사용시 스크린 별로 레이아웃의 행/열 정보가 변경되므로
 * 해당 api를 사용하여 가시성 여부 지정)
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#container | Array} paFreeFormId 프리폼 ID
 * @param {Number | Number[]} paIndex 열 인덱스 또는 열 인덱스 배열
 * @param {Boolean} pbVisible visible 여부(default : true)
 */
FreeFormKit.prototype.setColumnVisible = function(app, paFreeFormId, paIndex, pbVisible) {
	
	if (!(paFreeFormId instanceof Array)){
		paFreeFormId = [paFreeFormId];	
	}
	if (!(paIndex instanceof Array)){
		paIndex = [paIndex];	
	}
	
	pbVisible = pbVisible == null || pbVisible == undefined ? true : pbVisible;
	
	/** @type cpr.controls.Container */
	var vcFreeForm = null;
	var screenNm = app.targetScreen.name;
		
	for (var i = 0, len = paFreeFormId.length; i < len; i++) {
		if (paFreeFormId[i] instanceof cpr.controls.Container) {
			vcFreeForm = paFreeFormId[i];
		} else {
			vcFreeForm = app.lookup(paFreeFormId[i]);
		}
		
		if(!vcFreeForm || !(vcFreeForm instanceof cpr.controls.Container)
			|| !(vcFreeForm.getLayout() instanceof cpr.controls.layouts.FormLayout)) continue;
		
		/** @type cpr.controls.layouts.FormLayout */			
		var voLayout = vcFreeForm.getLayout();
		
		/** @type RForm */
		var voRForm = vcFreeForm["_RForm"]; // 반응형 폼 레이아웃 생성자
		
		for (var j = 0, len = paIndex.length; j < len; j++){						
			if(voRForm){
				if(screenNm == voRForm._screenNms["tablet"]){
					var vsTabletColCnt = vcFreeForm.userAttr(voRForm.ATTR_NM.ATTR_TABLET_COLUMN_COUNT);
					if(vsTabletColCnt && Number(vsTabletColCnt) > 0){
						// 반응형 폼 레이아웃 api로 처리
						voRForm.setColumnVisible(screenNm, paIndex, pbVisible);
						break;	
					}
				} else if(screenNm == voRForm._screenNms["mobile"]){
					var vsMobileColCnt = vcFreeForm.userAttr(voRForm.ATTR_NM.ATTR_MOBILE_COLUMN_COUNT);
					if(vsMobileColCnt && Number(vsMobileColCnt) > 0){
						// 반응형 폼 레이아웃 api로 처리
						voRForm.setColumnVisible(screenNm, paIndex, pbVisible);
						break;
					}
				}
			}
			
			voLayout.setColumnVisible(paIndex[j], pbVisible);						
		}
	}	
};

/**
 * 현재 바인딩된 프리폼 데이터 상태를 변경한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#container} psFreeFormId 프리폼 ID
 * @param {cpr.data.tabledata.RowState} state 변경할 상태값. <br>
		<state 종류><br>
		cpr.data.tabledata.RowState.UNCHANGED : 변경되지 않은 상태.<br>
		cpr.data.tabledata.RowState.INSERTED : 행이 신규로 추가된 상태.<br>
		cpr.data.tabledata.RowState.UPDATED : 행이 수정된 상태.<br>
		cpr.data.tabledata.RowState.DELETED : 행이 삭제된 상태.<br>
 */
FreeFormKit.prototype.setRowState = function(app, psFreeFormId, state) {
	/**@type cpr.controls.Container */
	var vcForm = app.lookup(psFreeFormId);

	/** @type cpr.bind.BindContext */
	var voBindContext = this._appKit.Group.getBindContext(app, vcForm);		
	/**@type cpr.data.DataSet */
	var dataset = this._appKit.Group.getBindDataSet(app, vcForm, voBindContext);
	var rowIndex = this._appKit.Group.getBindCtlRowIndex(app, voBindContext);

	dataset.setRowState(rowIndex, state);
};

/**
 * 현재 바인딩된 프리폼 데이터 상태를 가져온다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#container} psFreeFormId 프리폼 ID
 * @return {cpr.data.tabledata.RowState} 프리폼의 데이터 상태
 */
FreeFormKit.prototype.getRowState = function(app, psFreeFormId) {
	/**@type cpr.controls.Container */
	var vcForm = app.lookup(psFreeFormId);
	
	/** @type cpr.bind.BindContext */
	var voBindContext = this._appKit.Group.getBindContext(app, vcForm);		
	/**@type cpr.data.DataSet */
	var dataset = this._appKit.Group.getBindDataSet(app, vcForm, voBindContext);
	var rowIndex = this._appKit.Group.getBindCtlRowIndex(app, voBindContext);
	
	return dataset.getRowState(rowIndex);
};

/**
 * 그리드(Grid) 컨트롤 유틸
 * @constructor
 * @param {common.AppKit} appKit
 */
function GridKit(appKit) {
	this._appKit = appKit;	
};

/**
 * 그리드를 초기화한다.<br>
 * udcComAppHeader 의 앱속성(gridIds)에 지정한 그리드 ID 에 포함될 경우 초기화 지정<br>
 * 앱속성(gridIds) 미작성 시, 모든 그리드를 대상으로 초기화 지정<br><br>
 * 
 * <b><수행 로직></b><br>
 * 1. 상태 컬럼 바인드 지정  (I, U, D 및 class 부여)<br>
 * 2. 인덱스컬럼 text및 css지정<br>
 * 3. 소트/필터 컬럼 자동지정 <br>
 * 4. 그리드, 프리폼 PK컬럼 enable 설정<br>
 *   - 그리드의 선택행 컨텍스트 사용 그룹은 그리드의 사용자속성 bindDataFormId 지정 필수<br>
 *   - 그리드 PK컬럼의 사용자 속성 editablePK = "Y"이면 enable 설정 X<br>
 * 5. update이벤트 추가 ( 저장후 그리드의 마지막 작업행을 찾기 위함)<br>
 * 6. 그리드 매핑 데이터셋에 load 이벤트 추가 (그리드의 마지막행 찾기, 조회 건수 업데이트)<br>
 * 7. 그리드 filter 이벤트 추가(그리드 타이틀 영역의 데이터 건수 업데이트)<br>
 * 8. 그리드 selection-dispose 이벤트 추가(행추가 후 삭제로 인해 선택행이 없는 경우... 이전 행 자동 선택하도록(행 추가 -> 삭제시))<br><br>
 * 
 *  - 사이트별 Customizing 필요
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid | Array} paGridId 그리드 ID
 * @return void
 */
GridKit.prototype.init = function(app, paGridId) {
	if (!(paGridId instanceof Array)) {
		paGridId = [paGridId];
	}
	
	//Index 컬럼 반환
	function getIndexDetailColumn(poGrid) {
		var detail = poGrid.detail;
		var column;
		for (var i = 0, len = detail.cellCount; i < len; i++) {
			column = detail.getColumn(i);
			if (column.columnType == "rowindex") {
				return column;
			}
		}
		return null;
	}
	
	var _app = app;
	var _appKit = this._appKit;
	for (var i = 0, len = paGridId.length; i < len; i++) {
		/** @type cpr.controls.Grid*/
		var vcGrid = (paGridId[i] instanceof cpr.controls.Grid) ? paGridId[i] : _app.lookup(paGridId[i]);
		if (vcGrid == null || ValueUtil.isNull(vcGrid.id)) continue;
		
		/* 그리드 초기화 제외 */ 
		if(vcGrid.userAttr("ignoreInit") === "Y") continue;
		
		/** @type cpr.data.DataSet */
		var vcDataSet = vcGrid.dataSet;
		vcDataSet.userData("_gridId", vcGrid.id);
		
		//상태컬럼
		var statusColumn = this.getHeaderStatusColumn(app, vcGrid.id);
		if (statusColumn != null) {
			/* 커스텀 옵션 : 상태(Status) 컬럼 숨김시 구성*/
			//statusColumn.visible = false;
			var detailColumn = vcGrid.detail.getColumn(statusColumn.cellIndex);
			var statusColumnCtrl = detailColumn ? detailColumn.control : null;
			if (statusColumnCtrl) {			
				statusColumnCtrl.style.bindClass().toExpression("switch(getStateString()){ case 'I' : 'state insert'  case 'U' : 'state update'  case 'D' : 'state delete'  default : ''}");
			}
		}
		//인덱스컬럼
		var indexColumn = getIndexDetailColumn(vcGrid);
		if (indexColumn != null) {
			indexColumn.style.css({
				"text-align": "center"
			});
			var hIndexColumn = vcGrid.header.getColumn(indexColumn.cellIndex);
			
			var hIndexText = "No";
			if(typeof AppProperties !== 'undefined' && AppProperties.GRID_INDEX_COL_HEADER_TEXT){
				hIndexText = AppProperties.GRID_INDEX_COL_HEADER_TEXT;
			}
	
			if (hIndexColumn && hIndexColumn.text != hIndexText) hIndexColumn.text = hIndexText;
		}
		
		//소트/필터 컬럼 자동지정
		var dColumn, hColumn, vaHColumns;
		for (var j = 0, jlen = vcGrid.detail.cellCount; j < jlen; j++) {
			dColumn = vcGrid.detail.getColumn(j);
			if (dColumn.columnType == "checkbox" || dColumn.columnType == "rowindex" || dColumn.columnType == "radio") continue;
			if (dColumn.columnName == null || dColumn.columnName == "") continue;
			vaHColumns = vcGrid.header.getColumnByColIndex(dColumn.colIndex, dColumn.colSpan);
			if (vaHColumns) {
				vaHColumns.forEach(function( /* cpr.controls.gridpart.GridHeaderColumn */ column) {
					if(vcGrid.userAttr("applyGridSortable") !== "Y") {
						column.sortable = true;
					}
					if(vcGrid.userAttr("applyGridFilterable") !== "Y") {
						column.filterable = true;
					}
					if (column.targetColumnName == null || column.targetColumnName == "") {
						column.targetColumnName = dColumn.columnName;
					}
				});
			}
		}
		
		/* 커스텀 옵션 : 최초 로드된 시점의 헤더 컬럼 Visible 정보 저장*/
		//var vsHidenColumnIdxs = "";
		//for(var k=0, klen=vcGrid.header.cellCount; k<klen; k++){
		//	if(vcGrid.header.getColumn(k).visible === false){
		//		vsHidenColumnIdxs += k+",";
		//	}
		//}
		//vcGrid.userAttr("originHiddenColumns", vsHidenColumnIdxs);
		
		//그리드 PK컬럼 enable 설정
		var vaPkColumnNames = ValueUtil.split(vcDataSet.info, ",");
		var vsDataBindCtxId = vcGrid.userAttr("bindDataFormId");
		vaPkColumnNames.some(function(value, idx) {
			if (value == "") return false;
			//그리드 PK컬럼 설정(필수 스타일, 활성화/비활성화 바인딩 처리등)
			var columns = vcGrid.detail.getColumnByName(value);
			var vaHColumns = _appKit.Grid.getHeaderColumn(_app, vcGrid.id, value);
			if (columns != null && columns.length > 0) {
				columns.forEach(function(col) {
					if (col.control) {
						if (col.control.userAttr("editablePK") !== "Y") {
							col.control.bind("enabled").toExpression("getStateString() == 'I' ? true : false");
							col.control.userAttr("required", "Y");
							if (vaHColumns) {
								vaHColumns.forEach(function( /* cpr.controls.gridpart.GridColumn */ column) {
									if (column.cellIndex == col.cellIndex) {
										column.style.setClasses("required");
									}
								});
							}
						}
					}
				});
			}
		});
		
		//프리폼 PK 컬럼 설정 
		if (!ValueUtil.isNull(vsDataBindCtxId) && vaPkColumnNames.length > 0) {
			var freeformes = ValueUtil.split(vsDataBindCtxId, ",");
			freeformes.forEach(function( /* eachType */ formId) {
				/**@type cpr.controls.Container */
				var freeform = _app.lookup(formId);
				if (freeform != null) {
					var vaChildCtrls = freeform.getAllRecursiveChildren();
					vaPkColumnNames.some(function(value, idx) {
						if (value == "") return false;
						vaChildCtrls.some(function(ctrl, ix) {
							if (ctrl.type == "output") return false;
							if (ctrl.userAttr("ignorePk") == "Y") return false;
							if (ctrl.userAttr("editablePK") == "Y") return false;
							var bind = ctrl.getBindInfo("value");
							if (bind && bind.type == "datacolumn" && value == bind.columnName) {
								ctrl.bind("enabled").toExpression("getStateString() == 'I' ? true : false");
								ctrl.userAttr("required", "Y");
							}
						});
					});
				}
			});
		}
		var vsItemKey = app.id + vcGrid.id;
		var layout = localStorage.getItem(vsItemKey);
		if (!ValueUtil.isNull(layout)) {
			vcGrid.setColumnLayout(JSON.parse(layout));
		}
		
		//마지막 작업행을 찾기위해서...그리드 findRow 설정
		vcDataSet.addEventListener("update", function( /* cpr.events.CDataEvent */ e) {
			/** 
			 * @type cpr.data.DataSet
			 */
			var dataset = e.control;
			var rowIndex = e.row.getIndex();
			var row = e.row;
			var vaPkColumns = ValueUtil.split(dataset.info, ",");
			//if(vaPkColumns.length < 1){
			if (vaPkColumns.length < 1 || ValueUtil.isNull(dataset.info)) {
				dataset.userData("_findRowCondition", null);
			} else {
				var vaTempCond = [];
				vaPkColumns.forEach(function(column) {
					vaTempCond.push(column + "==" + "'" + dataset.getValue(rowIndex, column) + "'");
				});
				
				dataset.userData("_findRowCondition", (vaTempCond.length > 0 ? vaTempCond.join(" && ") : null));
			}
			
		});
		//그리드에 바인딩된 데이터셋(Dataset)이 로드될 때 처리
		//마지막행 찾기, 조회 건수 업데이트
		vcDataSet.addEventListener("load", function( /* cpr.events.CDataEvent */ e) {
			/** @type cpr.data.DataSet */
			var dataset = e.control;
			/** @type cpr.controls.Grid */
			var grd = dataset.getAppInstance().lookup(dataset.userData("_gridId"));
			if (grd == null) return;
			
			//대상 그리드가 정렬된 상태라면... 정렬을 푼다.
			if (dataset.getSort() != "") {
				dataset.clearSort();
			}
			
			//마지막 작업행 찾기
			if (dataset.getRowCount() > 0) {
				if (dataset.userData("_findRowCondition")) {
					var row = dataset.findFirstRow(dataset.userData("_findRowCondition"));
					if (row) {
						if (grd.selectionUnit == "cell") {
							grd.focusCell(row.getIndex(), 0);
							grd.moveToCell(row.getIndex(), 0);
						} else {
							setTimeout(function() {
								_appKit.Grid.selectRow(_app, grd.id, row.getIndex());
							}, 200);
						}
					} else {
						grd.selectionUnit == "cell" ? grd.focusCell(0, 0) : _appKit.Grid.selectRow(_app, grd.id, 0);
					}
				} else {
					if (grd.selectionUnit == "cell") grd.focusCell(0, 0);
					else _appKit.Grid.selectRow(_app, grd.id, 0);
				}
			} else {}
			
			//마지막 작업행 정보 Clear
			dataset.userData("_findRowCondition", null);
			
			//그리드 타이틀 영역의 데이터 건수 업데이트
			var titles = _appKit.Group.getAllChildrenByType(_app, "udc.com.udcComGridTitle");
			for (var i = 0, len = titles.length; i < len; i++) {
				if (titles[i] == null) continue;
				if (titles[i].getAppProperty("ctrl") == null) continue;
				if (titles[i].getAppProperty("ctrl").id == grd.id) {
					titles[i].setAppProperty("rowCount", dataset.getRowCount());
					break;
				}
			}
		});
		
		vcDataSet.addEventListener("filter", function(e) {
			/** @type cpr.data.DataSet */
			var dataset = e.control;
			/** @type cpr.controls.Grid */
			var grd = dataset.getAppInstance().lookup(dataset.userData("_gridId"));
			if (grd == null) return;
			
			//그리드 타이틀 영역의 데이터 건수 업데이트
			var titles = _appKit.Group.getAllChildrenByType(_app, "udc.com.udcComGridTitle");
			for (var i = 0, len = titles.length; i < len; i++) {
				if (titles[i] == null) continue;
				if (titles[i].getAppProperty("ctrl") == null) continue;
				if (titles[i].getAppProperty("ctrl").id == grd.id) {
					titles[i].setAppProperty("rowCount", dataset.getRowCount());
					break;
				}
			}
		});
		//행 삭제로 인한, 선택행이 없는 경우... 이전 행 자동 선택하도록(행 추가 -> 삭제시)
		vcGrid.addEventListener("selection-dispose", function(e) {
			var oldSelection = e.oldSelection;
			if (oldSelection != null && oldSelection.length > 0 && oldSelection[0] > -1 && oldSelection[0] < e.control.rowCount) {
				e.control.selectRows(oldSelection[0]);
			}
		});
	}
};

/**
 * 그리드 특정 cell의 값을 변경한다. (detail 영역)<br>
 * (주의) for문 등으로 대량의 데이터를 setCellValue 호출하는 경우에는 pbEmitEvent값을 false로 주어서, 스크립트 실행시간을 줄여줄 수 있다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {String | Number} psDataColmnId cellIndex 값을 변경하고자 하는 cell의 cell index <br>
 * 										  (또는 binding된 data column name)
 * @param {String} psValue 변경하고자 하는 값
 * @param {Number} pnRowIndex? 값을 변경하고자 하는 cell의 row index<br>
 *                 (defalut : 선택된 rowindex)
 * @param {Boolean} pbEmitEvent? 이벤트(before-update, update)를 발생시킬지 여부
 * @return void
 */
GridKit.prototype.setCellValue = function(app, psGridId, psDataColmnId, psValue, pnRowIndex, pbEmitEvent) {
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	var rowIndex = pnRowIndex == null ? this.getIndex(app, psGridId) : pnRowIndex;
	
	vcGrid.setCellValue(rowIndex, psDataColmnId, psValue, pbEmitEvent);
};

/**
 * 그리드 특정 cell의 값을 반환한다.(detail 영역) 
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId  그리드 ID
 * @param {String | Number} psDataColmnId cellIndex 값을 가져오고자 하는 cell의 cell index<br>
 * 										  (또는 binding된 data column name)
 * @param {Number} pnRowIndex? 값을 변경하고자 하는 cell의 row index<br>
 *                 (defalut : 선택된 rowindex)
 * @return {Object} 해당 cell의 값
 */
GridKit.prototype.getCellValue = function(app, psGridId, psDataColmnId, pnRowIndex) {
	/**@type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	var rowIndex = pnRowIndex == null ? this.getIndex(app, psGridId) : pnRowIndex;
	return vcGrid.getCellValue(rowIndex, psDataColmnId);
};

/**
 * 그리드 특정 row cell의 origin 값을 반환한다.(detail 영역) 
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {String | Number} psDataColmnId cellIndex 값을 가져오고자 하는 cell의 cell index<br>
 * 										  (또는 binding된 data column name)
 * @param {Number} pnRowIndex? 값을 가져오고자 하는  cell의  행인덱스<br>
 *                 defalut : 선택된 rowindex
 * @return {any} 해당 cell의 값
 */
GridKit.prototype.getOriginCellValue = function(app, psGridId, psDataColmnId, pnRowIndex) {
	/**@type cpr.controls.Grid */
	var grid = app.lookup(psGridId);
	var rowIndex = pnRowIndex == null ? this.getIndex(app, psGridId) : pnRowIndex;
	return grid.dataSet.getOriginalValue(rowIndex, psDataColmnId);
};

/**
 * 그리드 특정 row index의 GridRow객체를 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {Number} pnRowIndex? 값을 가져오고자 하는  cell의  행인덱스<br>
 *                 defalut : 선택된 rowindex
 * @return {cpr.controls.provider.GridRow} 해당 index의 GridRow 객체
 */
GridKit.prototype.getDataRow = function(app, psGridId, pnRowIndex) {
	/**@type cpr.controls.Grid */
	var grid = app.lookup(psGridId);
	var rowIndex = pnRowIndex == null ? this.getIndex(app, psGridId) : pnRowIndex;
	return grid.getDataRow(rowIndex);
};

/**
 * 현재 연결된 데이터 구조체에 sort 조건을 변경하고, sort 적용<br>
 * <pre><code>
 * Grid.sort(app, "grd1", "a, b DESC")
 * </code></pre>
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {String} psCondition sort 조건식
 * @return void
 */
GridKit.prototype.sort = function(app, psGridId, psCondition) {
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	vcGrid.clearSort();
	vcGrid.sort(psCondition);
	vcGrid.redraw();
};

/**
 * 그리드 초기화(데이터 clear)
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid | Array} paGridId 그리드 ID
 * @return void
 */
GridKit.prototype.reset = function(app, paGridId) {
	if (!(paGridId instanceof Array)) {
		paGridId = [paGridId];
	}
	
	for (var i = 0; i < paGridId.length; i++) {
		var vcGrid = app.lookup(paGridId[i]);
		vcGrid.dataSet.clear();
		vcGrid.redraw();
	}
};

/**
 * 현재 연결된 데이터 구조체에 filter 조건을 변경하고, filter합니다.<br>
 * <pre><code>
 * Grid.filter(app, "grd1", "age >= 20")
 * </code></pre>
 * 	=> "age"컬럼의 값이 20이상인 값만 필터링합니다.<br>
 * <pre><code>
 * Grid.filter(app, "grd1", "name ^= '김'")
 * </code></pre>
 * 	=> "name"컬럼의 값이 '김'으로 시작하는 값만 필터링합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {#expression} psCondition filter 조건식
 * @return void
 */
GridKit.prototype.filter = function(app, psGridId, psCondition) {
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	var vsFilter = vcGrid.getFilter();
	if (!ValueUtil.isNull(vsFilter)) {
		vcGrid.clearFilter();
	}
	vcGrid.filter(psCondition);
};

/**
 * 그리드의 변경사항 유/무를 반환를 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid | Array} paGridId 그리드 ID
 * @param {"MSG" | "CRM"} psAftMsg? 
 *						MSG : 변경사항 내역이 없을 경우 '변경된 내역이 없습니다.' 메세지 출력<br>
 *  					CRM : 변경내역이 존재할경우 '변경사항이 반영되지 않았습니다. 계속 하시겠습니까?' confirm 메시지출력 <br>
 * @param {cpr.events.CSelectionEvent} event? 이벤트 객체
 * @param {"confirmCallback" : Function <!-- "CRM" confirm창 사용시 확인에 대한 콜백 -->
 *         ,"cancelCallback" : Function <!-- "CRM" confirm창 사용시 취소(닫기)에 대한 콜백 -->
 *         ,"bWindowConfirm" : boolean  <!--  windowConfirm호출 여부 -->} poOption? "CRM" confirm 창에 대한 콜백 함수 및 windowConfirm호출 여부
 * @return {Boolean} 데이터 변경 여부
 */
GridKit.prototype.isModified = function(app, paGridId, psAftMsg, event, poOption) {
	//유효성 체크로 인해서 행선택 변경 발생으로 변경여부 체크가 되는 경우는 SKIP...
	if (event != null && event.control != null && event.control.userAttr("selectionChangeByValidation") === "true") {
		event.control.removeUserAttr("selectionChangeByValidation");
		return false;
	}
	
	if (!(paGridId instanceof Array)) {
		paGridId = [paGridId];
	}
	psAftMsg = psAftMsg == null ? "" : psAftMsg;
	
	var modify = false;
	var vcGrid = null;
	for (var i = 0, len = paGridId.length; i < len; i++) {
		if (paGridId[i] instanceof cpr.controls.Grid) {
			vcGrid = paGridId[i];
		} else {
			vcGrid = app.lookup(paGridId[i]);
		}
		
		if (vcGrid.dataSet == null) continue;
		
		if (vcGrid.dataSet.isModified()) {
			modify = true;
			break;
		}
	}
	
	if (modify) {
        if(psAftMsg.toUpperCase() == "CRM"){//변경사항이 반영되지 않았습니다. 계속 하시겠습니까? confirm
            if (ValueUtil.fixNull(poOption) != "" && poOption["bWindowConfirm"]){
    			if (!this._appKit.Msg.confirm("CRM-M003", [vcGrid.fieldLabel])) return true;
    			else return false;
            } else {
                this._appKit.Msg.confirmDlg(app, "CRM-M003", [vcGrid.fieldLabel], poOption);
                return true;
            }
        } else {
            return true;
        }
	} else {
		if (psAftMsg.toUpperCase() == "MSG") { //변경된 내역이 없습니다.
			this._appKit.Msg.notify(app, "INF-M006");
		}
	}
	
	return modify;
};

/**
 * 해당 그리드의 체크된 행(Row)이나 선택된 행의 인덱스를 반환한다.(check된 행이 있는 경우, 체크된 행이 우선적으로 반환된다.)
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @return {Number[]} 선택된 row index 배열
 */
GridKit.prototype.getCheckOrSelectedRowIndex = function(app, psGridId) {
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	if (vcGrid.rowCount < 1) return [];
	
	var vaRdoIndexs = [];
	var vnRdoIndex = vcGrid.getRadioSelection();
	var vaChkIndexs = vcGrid.getCheckRowIndices();
	if (vaChkIndexs != null && vaChkIndexs.length > 0) {
		return vaChkIndexs;
	} else if (vnRdoIndex != -1) {
		vaRdoIndexs.push(vnRdoIndex);
		return vaRdoIndexs;
	} else {
		if (vcGrid.selectionUnit == "cell") {
			var vaSelIndices = vcGrid.getSelectedCellIndices();
			var rowIndices = [];
			for (var i = 0, len = vaSelIndices.length; i < len; i++) {
				rowIndices.push(vaSelIndices[i].rowIndex);
			}
			return rowIndices;
		} else {
			return vcGrid.getSelectedRowIndices();
		}
	}
};

/**
 * 해당 그리드의 체크된 행(Row)의 인덱스를 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @return {Number[]} 선택된 row index 배열
 */
GridKit.prototype.getCheckedRowIndex = function(app, psGridId) {
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	if (vcGrid.rowCount < 1) return [];
	
	var vaRdoIndexs = [];
	var vnRdoIndex = vcGrid.getRadioSelection();
	var vaChkIndexs = vcGrid.getCheckRowIndices();
	if (vaChkIndexs != null && vaChkIndexs.length > 0) {
		return vaChkIndexs;
	} else if (vnRdoIndex != -1) {
		vaRdoIndexs.push(vnRdoIndex);
		return vaRdoIndexs;
	} else {
		return [];
	}
};

/**
 * 그리드 내 변경된 특정 행(Row)의 데이터를 원상태로 복구한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {Number} pnRowIndex? 원복하고 싶은 row index<br>
 *                 (default : 현재 체크 및 선택된 로우)
 * @return void
 */
GridKit.prototype.revertRowData = function(app, psGridId, pnRowIndex) {
	/**@type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	
	if (pnRowIndex == null) {
		var vaSelectIdx = this.getCheckOrSelectedRowIndex(app, psGridId);
		if (vaSelectIdx.length < 1) {
			return false;
		}
		var vcDataSet = vcGrid.dataSet;
		var rowIndex;
		for (var i = vaSelectIdx.length - 1; i >= 0; i--) {
			rowIndex = vaSelectIdx[i];
			if (vcGrid.isCheckedRow(rowIndex)) {
				vcGrid.setCheckRowIndex(rowIndex, false); //체크 해제
			}
			var vsStatus = "";
			if (vcDataSet != null) {
				vsStatus = vcDataSet.getRowStateString(rowIndex);
			}
			vcGrid.revertRowData(rowIndex); //데이터 원복
			//신규 행이면...
			if (vsStatus == "I") {
				if (rowIndex == vcGrid.getRowCount()) {
					if (rowIndex == 0) {
						vcGrid.clearSelection();
					} else {
						this.selectRow(app, vcGrid.id, rowIndex - 1);
					}
				} else {
					this.selectRow(app, vcGrid.id, rowIndex);
				}
			}
		}
	} else {
		vcGrid.revertRowData(pnRowIndex);
	}
};

/**
 * 그리드 내에서 변경된 모든 데이터를 원상태로 복구한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 */
GridKit.prototype.revertAllData = function(app, psGridId) {
	var vcGrid = app.lookup(psGridId);
	vcGrid.revertData();
};

/**
 * 그리드의 특정 행 데이터를 그룹 폼의 데이터셋에 복사한다.<br>
 * (사용처) 그리드의 데이터셋을 바인딩하여 사용하지 않는 경우에... 그리드의 선택된 행 데이터를 프리폼/그룹에 매핑하기 위한 용도임
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드ID
 * @param {#container} psTargetForm 복사할 Group Form ID
 * @param {Number} pnRowIdx 복사할 그리드 로우 인덱스 
 * @return void
 */
GridKit.prototype.copyRowToGroupForm = function(app, psGridId, psTargetForm, pnRowIdx) {
	var vcGrid = app.lookup(psGridId);
	var rowIndex = pnRowIdx == null ? this.getIndex(app, psGridId) : pnRowIdx;
	
	var vcGrpFrm = app.lookup(psTargetForm);
	vcGrpFrm.getBindContext().rowIndex = rowIndex;
	vcGrpFrm.redraw();
};

/**
 * 소스(Source) 그리드의 선택된 행(Row)의 데이터를 타겟(Target) 그리드로 복사한다.<br>
 * 단, 복사할려는 데이터가 타겟 그리드에 이미 존재하는 경우에는 복사하지 않는다.(중복 복사 방지)
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psSrcGridId 그리드 ID
 * @param {#grid} psDesGridId 복사할 그리드 ID
 * @param {Number} pnSrcRowIdx? 그리드 로우 인덱스<br>
 *                 default : 체크된 row 나 선택된 row 인덱스를 취득 (check우선)
 * @return void
 */
GridKit.prototype.copyToGridData = function(app, psSrcGridId, psDesGridId, pnSrcRowIdx) {
	var vcSrcGrid = app.lookup(psSrcGridId);
	var vcDesGrid = app.lookup(psDesGridId);
	
	var rowIndexs = pnSrcRowIdx == null ? this.getCheckOrSelectedRowIndex(app, psSrcGridId) : pnSrcRowIdx;
	if (!(rowIndexs instanceof Array)) {
		rowIndexs = [rowIndexs];
	}
	//복사할 ROW가 없으면...SKIP
	if (rowIndexs.length < 1) return;
	
	var srcDataSet = vcSrcGrid.dataSet;
	var tarDataSet = vcDesGrid.dataSet;
	for (var i = 0, len = rowIndexs.length; i < len; i++) {
		//신규 후 삭제된 행은 제외
		if (srcDataSet.getRowState(rowIndexs[i]) == cpr.data.tabledata.RowState.INSERTDELETED) continue;
		
		var data = srcDataSet.getRowData(rowIndexs[i]);
		// json 형식의 row의 데이터
		/** @type Array */
		var str = [];
		// 이미 존재하는 row를 찾기 위해 row의 모든 column을 비교하는 조건 제작
		// str = "column1 == 'value1' && column2 == 'value2'..."
		for (var key in data) {
			str.push(key + " == '" + data[key] + "'");
		}
		str = str.join(" && ");
		// 조건에 맞는 row 탐색
		var findRow = tarDataSet.findAllRow(str);
		// 조건에 해당하는 row가 없다면 target 그리드에 선택된 row를 추가
		if (findRow == null || findRow.length < 1) {
			tarDataSet.addRowData(data);
		}
	}
	
	vcDesGrid.redraw();
};

/**
 * 소스(Source) 그리드의 모든 행(Row)의 데이터를 타겟(Target) 그리드로 복사한다.<br>
 * 단, 복사할려는 데이터가 타겟 그리드에 이미 존재하는 경우에는 복사하지 않는다.(중복 복사 방지)
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psSrcGridId 그리드 ID
 * @param {#grid} psDesGridId 복사할 그리드 ID
 * @return void
 */
GridKit.prototype.copyToAllGridData = function(app, psSrcGridId, psDesGridId) {
	var vcSrcGrid = app.lookup(psSrcGridId);
	
	var indices = [];
	for (var i = 0, len = vcSrcGrid.rowCount; i < len; i++) {
		indices.push(i);
	}
	
	this.copyToGridData(app, psSrcGridId, psDesGridId, indices);
};

/**
 * 그리드 작업행을 찾기 위한 조건을 설정한다. 데이터셋에 설정된 PK정보를 기준으로 자동 지정된다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {Number} pnRowIndex? 그리드 로우(Row) 인덱스
 * @param {Boolean} pbForce? 기존에 로우에 대한 정보가 있으면 SKIP 여부
 * @return void
 */
GridKit.prototype.markFindRowCondition = function(app, psGridId, pnRowIndex, pbForce) {
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	/** @type cpr.data.DataSet */
	var voDataSet = vcGrid.dataSet;
	
	if (pbForce != undefined && ValueUtil.fixBoolean(pbForce) === true) {
		if (!ValueUtil.isNull(voDataSet.userData("_findRowCondition"))) return;
	}
	
	var rowIndex = !ValueUtil.isNull(pnRowIndex) ? pnRowIndex : this.getIndex(app, psGridId);
	
	var vaTempCond = [];
	var vaPkColumns = ValueUtil.split(voDataSet.info, ",");
	vaPkColumns.forEach(function(column) {
		vaTempCond.push(column + "==" + "'" + voDataSet.getValue(rowIndex, column) + "'");
	});
	
	if (vaTempCond.length > 0) {
		voDataSet.userData("_findRowCondition", vaTempCond.join(" && "));
	} else {
		voDataSet.userData("_findRowCondition", null);
	}
};

/**
 * 소스(Source) 그리드의 선택된 행(Row)의 데이터를 타겟(Target) 그리드로 이동한다.<br>
 * 데이터 이동 후, 소스(Source) 그리드의 이동된 행(Row)의 상태는 delete모드로 상태값만 변경된다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psSrcGridId 그리드 ID
 * @param {#grid} psDesGridId 이동할 그리드 ID
 * @param {Number | Number[]} pnSrcRowIdx? 그리드 로우 인덱스<br>
 *                            default : 체크된 row 나 선택된 row 인덱스를 취득 (check우선)
 * @return void
 */
GridKit.prototype.moveToGridData = function(app, psSrcGridId, psDesGridId, pnSrcRowIdx) {
	var vcSrcGrid = app.lookup(psSrcGridId);
	var vcDesGrid = app.lookup(psDesGridId);
	
	var rowIndexs = pnSrcRowIdx == null ? this.getCheckOrSelectedRowIndex(app, psSrcGridId) : pnSrcRowIdx;
	if (!(rowIndexs instanceof Array)) {
		rowIndexs = [rowIndexs];
	}
	//이동할 ROW가 없으면...SKIP
	if (rowIndexs.length < 1) return;
	
	var srcDataSet = vcSrcGrid.dataSet;
	var tarDataSet = vcDesGrid.dataSet;
	for (var i = 0, len = rowIndexs.length; i < len; i++) {
		//신규 후 삭제된 행은 제외
		if (srcDataSet.getRowState(rowIndexs[i]) == cpr.data.tabledata.RowState.INSERTDELETED) continue;
		
		tarDataSet.addRowData(srcDataSet.getRowData(rowIndexs[i]));
	}
	vcDesGrid.redraw();
	vcSrcGrid.deleteRow(pnSrcRowIdx);
};

/**
 * 소스(Source) 그리드의 모든 데이터행(Row)을 타겟(Target) 그리드로 이동한다.<br>
 * 데이터 이동 후, 소스(Source) 그리드의 이동된 행(Row)의 상태는 delete모드로 상태값만 변경된다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psSrcGridId 그리드 ID
 * @param {#grid} psDesGridId 이동할 그리드 ID
 */
GridKit.prototype.moveToAllGridData = function(app, psSrcGridId, psDesGridId) {
	var vcSrcGrid = app.lookup(psSrcGridId);
	
	var indices = [];
	for (var i = 0, len = vcSrcGrid.rowCount; i < len; i++) {
		indices.push(i);
	}
	
	this.moveToGridData(app, psSrcGridId, psDesGridId, indices);
};

/**
 * 그리드에서 로우(Row)를 선택해준다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드ID
 * @param {Number | Number[]} pnRowIndex? 포커스를 부여할 Row의 인덱스(default : 현재 행 인덱스)
 * @param {Boolean} pbEmitEvent? 이벤트(before-selection-change, selection-change)를 발생시킬지 여부
 * @return void
 */
GridKit.prototype.selectRow = function(app, psGridId, pnRowIndex, pbEmitEvent) {
	/** @type cpr.controls.Grid */
	var grid = app.lookup(psGridId);
	if (pnRowIndex == null || pnRowIndex == undefined) {
		pnRowIndex = this.getIndex(app, psGridId);
	}
	
	grid.selectRows(pnRowIndex, pbEmitEvent);
	if (!(pnRowIndex instanceof Array)) {
		grid.focusCell(pnRowIndex, 0);
		grid.moveToCell(pnRowIndex, 0);
	}
};

/**
 * 그리드에서 조건을 만족하는 로우(Row)를 선택해준다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드ID
 * @param {String} psCondition 조건식 :<br>
 *                 ex)"STUD_DIV_RCD == 'CT101REGU' && SA_NM == '컴퓨터정보과'"<br>
 * 					사용가능수식 :<br>!=", "!==", "$=", "%", "&&", "(", "*", "*=", "+", ",", "-", ".", "/", "/*", "//", "<", "<=", "==", "===", ">", ">=", "?", "[", "^=", "||"
 * @param {Number} pnCellIdx? 포커스를 부여할 Cell의 인덱스<br>
 *                 (default : 조건에 만족하는 행 select)
 * @return void
 */
GridKit.prototype.selectRowByCondition = function(app, psGridId, psCondition, pnCellIdx) {
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	var voRow = vcGrid.findFirstRow(psCondition);
	
	if (voRow) {
		if (pnCellIdx) vcGrid.focusCell(voRow.getIndex(), pnCellIdx);
		else vcGrid.selectRows(voRow.getIndex());
	}
};

/**
 * 그리드 행선택 변경 이벤트 발생시, 변경 이전에 선택된 행을 선택해준다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {cpr.events.CSelectionEvent} event 그리드 선택행 변경 이벤트
 * @param {Boolean} emitEvent? 이벤트(before-selection-change, selection-change)를 발생시킬지 여부
 * @return void
 */
GridKit.prototype.selectBeforeRow = function(app, event, emitEvent) {
	/** @type cpr.controls.Grid */
	var vcGrid = event.control;
	var emit = emitEvent === true ? true : false;
	
	var voOldSelection = event.oldSelection[0];
	var vsPkValues = this.getRowPKColumnValues(app, vcGrid.id, voOldSelection);
	var voFindRow = vcGrid.findFirstRow(vsPkValues);
	if (voFindRow) {
		vcGrid.clearSelection(false);
		if (vcGrid.selectionUnit == "cell") {
			vcGrid.selectCells([{
				rowIndex: voFindRow["rowIndex"],
				cellIndex: voFindRow["cellIndex"]
			}], emit);
		} else {
			vcGrid.selectRows(voFindRow.getIndex(), emit);
		}
	}
};

/**
 * 그리드에 신규 행(Row)을 추가한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {String | Number} pnEditCellIdx 시작 cellIndex cell index 또는 column name
 * @param {Number} pnRowIdx? 추가하고자 하는 Row index<br>
 *                 (defalut : 현재 선택된 로우 이후)
 * @param {Object} poRowData? 추가할 row data<br> (key: header명, value: value 를 갖는 json data)
 * @return {cpr.controls.provider.GridRow} 추가한 Row의 GridRow 객체.
 */
GridKit.prototype.insertRow = function(app, psGridId, pnEditCellIdx, pnRowIdx, poRowData) {
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	var rowIndex = pnRowIdx == null ? this.getIndex(app, psGridId) : pnRowIdx;
	
	var insertedRow = null;
	if (poRowData != null)
		insertedRow = vcGrid.insertRowData(rowIndex, true, poRowData);
	else
		insertedRow = vcGrid.insertRow(rowIndex, true);
	
	var vnInsIdx = insertedRow.getIndex();
	
	// SELECT 된 로우가 없을 경우 다음로직(setEditRowIndex , focusCell)을 진행하지 않는다.
	if (!vcGrid.selectRows([vnInsIdx])) return insertedRow;
	
	if (!vcGrid.readOnly) {
		vcGrid.setEditRowIndex(vnInsIdx, true);
	}
	
	if (pnEditCellIdx) {
		vcGrid.focusCell(vnInsIdx, pnEditCellIdx);
		//포커싱할 컬럼이 UDC인 경우에...
		if (!ValueUtil.isNumber(pnEditCellIdx)) {
			for (var i = 0, len = vcGrid.detail.cellCount; i < len; i++) {
				if (vcGrid.detail.getColumn(i).columnName == pnEditCellIdx) {
					var ctrl = vcGrid.detail.getColumn(i).control;
					if (ctrl instanceof cpr.controls.UDCBase) {
						var empApp = ctrl.getEmbeddedAppInstance();
						ctrl = AppUtil.getUDCBindValueControl(ctrl);
						if (ctrl) empApp.focus(ctrl.id);
					}
					break;
				}
			}
		}
	} else {
		vcGrid.focusCell(vnInsIdx, 0);
	}
	//그리드에 바인딩된 프리폼이 있는 경우... 프리폼 활성화
	if (!ValueUtil.isNull(vcGrid.userAttr("bindDataFormId"))) {
		var freeformes = ValueUtil.split(vcGrid.userAttr("bindDataFormId"), ",");
		freeformes.forEach(function( /* eachType */ formId) {
			/**@type cpr.controls.Container */
			var freeform = vcGrid.getAppInstance().lookup(formId);
			if (freeform != null) {
				if (freeform.userData("_expressEnabled")) freeform.bind("enabled").toExpression(freeform.userData("_expressEnabled"));
				else freeform.enabled = true;
			}
		});
	}
	
	return insertedRow;
};

/**
 * 그리드에 단 한건의 신규 행(Row)을 추가한다. (단하나의 신규 행만 추가하고자 하는 경우에 사용)
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {String | Number} pnEditCellIdx 시작 cell index 또는 column name
 * @param {#grid | Array} paModifiedGrid
 * @param {Number} pnRowIdx? 추가하고자 하는 Row index<br>
 *                 (defalut : 현재 선택된 로우 이후)
 * @param {Object} poRowData? 추가할 row data<br> (key: header명, value: value 를 갖는 json data)
 * @return {cpr.controls.provider.GridRow} 추가한 Row의 GridRow 객체
 */
GridKit.prototype.insertRowOnlyOne = function(app, psGridId, pnEditCellIdx, paModifiedGrid, pnRowIdx, poRowData) {
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	if (vcGrid == null) return null;
	var rowIndex = pnRowIdx == null ? this.getIndex(app, psGridId) : pnRowIdx;
	
	var insertedRow = null;
	if (vcGrid.dataSet.getRowStatedCount(cpr.data.tabledata.RowState.INSERTED) > 0) {
		for (var i = 0, len = vcGrid.rowCount; i < len; i++) {
			if (vcGrid.getRowState(i) == cpr.data.tabledata.RowState.INSERTED) {
				insertedRow = vcGrid.getRow(i);
				break;
			}
		}
	}
	
	if (insertedRow) {
		//신규 추가된 행이 존재합니다.변경사항이 반영되지 않았습니다. 계속 하시겠습니까?
		if (!this._appKit.Msg.confirm("CRM-M206", [vcGrid.fieldLabel])) return null;
		this._appKit.Control.reset(app, paModifiedGrid);
		var vaColumns = vcGrid.dataSet.getColumnNames();
		vaColumns.forEach(function(column) {
			vcGrid.setCellValue(insertedRow.getIndex(), column, "", false);
		});
	} else {
		if (this.isModified(app, paModifiedGrid, "CRM")) return null;
		this._appKit.Control.reset(app, paModifiedGrid);
		insertedRow = vcGrid.insertRow(rowIndex, true);
	}
	
	var vnInsIdx = insertedRow.getIndex();
	if (vcGrid.readOnly) {
		vcGrid.selectRows([vnInsIdx]);
	} else {
		vcGrid.selectRows([vnInsIdx]);
		vcGrid.setEditRowIndex(vnInsIdx, true);
	}
	
	if (pnEditCellIdx) {
		vcGrid.focusCell(vnInsIdx, pnEditCellIdx);
		//포커싱할 컬럼이 UDC인 경우에...
		if (!ValueUtil.isNumber(pnEditCellIdx)) {
			for (var i = 0, len = vcGrid.detail.cellCount; i < len; i++) {
				if (vcGrid.detail.getColumn(i).columnName == pnEditCellIdx) {
					var ctrl = vcGrid.detail.getColumn(i).control;
					if (ctrl instanceof cpr.controls.UDCBase) {
						var empApp = ctrl.getEmbeddedAppInstance();
						ctrl = AppUtil.getUDCBindValueControl(ctrl);
						if (ctrl) empApp.focus(ctrl.id);
					}
					break;
				}
			}
		}
	}
	
	return insertedRow;
};

/**
 * 그리드의 선택된 행(Row)를 삭제한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {Number | Number[]} pnRowIdx? 삭제하고자 하는 Row index<br>
 *                 default : 체크된 row 나 선택된 row 인덱스를 취득 (check우선)
 * @return {Number[]} 삭제된 행 (배열)                
 */
GridKit.prototype.deleteRow = function(app, psGridId, pnRowIdx) {
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	var _this = this;
	
	var rowIndexs = pnRowIdx == null ? this.getCheckOrSelectedRowIndex(app, psGridId) : pnRowIdx;
	
	if (!(rowIndexs instanceof Array)) {
		rowIndexs = [pnRowIdx];
	}
	//삭제할 행이 없는 경우... 메시지 박스를 보여줌
	if (rowIndexs.length < 1) {
		//삭제할 데이터가 없습니다.
		this._appKit.Msg.alertDlg(app, "INF-M007");
		return false;
	}
	
	//신규 후 삭제시... 디테일 데이터에 대한 Reference 삭제(삭제 플래그로 업데이트)
	var vaDetailCtrls = null;
	
	var vcDataSet = vcGrid.dataSet;
	for (var i = rowIndexs.length - 1; i >= 0; i--) {
		var rowIdx = rowIndexs[i];
		vcGrid.deleteRow(rowIdx);
		
		if (vcDataSet != null) {
			if (vcDataSet.getRowState(rowIdx) == cpr.data.tabledata.RowState.INSERTDELETED) {
				
				vcGrid.revertRowData(rowIdx);
				if (rowIdx == vcGrid.getRowCount()) {
					if (rowIdx == 0) {
						vcGrid.clearSelection();
					} else {
						vcGrid.selectRows(rowIdx - 1);
					}
				} else {
					vcGrid.selectRows(rowIdx);
				}
			}
		}
	}
	
	//그리드에 바인딩된 프리폼이 있는 경우... 프리폼 활성화
	if (!ValueUtil.isNull(vcGrid.userAttr("bindDataFormId"))) {
		var freeformes = ValueUtil.split(vcGrid.userAttr("bindDataFormId"), ",");
		freeformes.forEach(function( /* eachType */ formId) {
			/**@type cpr.controls.Container */
			var freeform = vcGrid.getAppInstance().lookup(formId);
			if (freeform != null) {
				var voDs = _this._appKit.Group.getBindDataSet(freeform.getAppInstance(), freeform);
				//데이터 건수가 없으면... 프리폼 비활성화
				if (voDs.getRowCount() < 1) {
					freeform.enabled = false;
				}
			}
		});
	}
	
	return rowIndexs;
};

/**
 * 특정 row의 상태값을 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {Number} pnRowIdx 상태를 알고자 하는 row index
 * @return {cpr.data.tabledata.RowState} 해당 row index의 상태값<br>
			<b>state 종류</b><br>
			cpr.data.tabledata.RowState.EMPTIED : 삭제된 로우를 커밋 시 삭제된 배열을에서 제거하기 위한 임시 상태<br>
			cpr.data.tabledata.RowState.UNCHANGED : 변경되지 않은 상태<br>
			cpr.data.tabledata.RowState.INSERTED : 행이 신규로 추가된 상태<br>
			cpr.data.tabledata.RowState.UPDATED : 행이 수정된 상태<br>
			cpr.data.tabledata.RowState.DELETED : 행이 삭제된 상태<br>
			cpr.data.tabledata.RowState.INSERTDELETED : 행이 추가되었다가 삭제된 상태
 */
GridKit.prototype.getRowState = function(app, psGridId, pnRowIdx) {
	var vcGrid = app.lookup(psGridId);
	var rowIndex = pnRowIdx == null ? this.getIndex(app, psGridId) : pnRowIdx;
	return vcGrid.getRowState(rowIndex);
};

/**
 * 특정 row의 상태를 변경한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {cpr.data.tabledata.RowState} state 변경할 상태값 <br>
		<b>state 종류</b><br>
		cpr.data.tabledata.RowState.UNCHANGED : 변경되지 않은 상태<br>
		cpr.data.tabledata.RowState.INSERTED : 행이 신규로 추가된 상태<br>
		cpr.data.tabledata.RowState.UPDATED : 행이 수정된 상태<br>
		cpr.data.tabledata.RowState.DELETED : 행이 삭제된 상태<br>
 * @param {Number} pnRowIdx 변경하고자 하는 row index
 */
GridKit.prototype.setRowState = function(app, psGridId, state, pnRowIdx) {
	var vcGrid = app.lookup(psGridId);
	var rowIndex = pnRowIdx == null ? this.getIndex(app, psGridId) : pnRowIdx;
	vcGrid.setRowState(rowIndex, state);
};

/**
 * 전체 row의 상태값을 특정 상태(state)로 일괄 업데이트 한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {cpr.data.tabledata.RowState} state 변경할 상태값<br>
		<b>state 종류</b><br>
		cpr.data.tabledata.RowState.UNCHANGED : 변경되지 않은 상태<br>
		cpr.data.tabledata.RowState.INSERTED : 행이 신규로 추가된 상태<br>
		cpr.data.tabledata.RowState.UPDATED : 행이 수정된 상태<br>
		cpr.data.tabledata.RowState.DELETED : 행이 삭제된 상태<br>
 */
GridKit.prototype.setRowStateAll = function(app, psGridId, state) {
	var vcGrid = app.lookup(psGridId);
	var vcDataSet = vcGrid.dataSet;
	vcDataSet.setRowStateAll(state);
	vcGrid.redraw();
};

/**
 * 해당 상태 값을 갖는 row를 검색하여 row index 배열을 반환합니다.
 * <pre><code>
 * Grid.getRowStatedIndices(app,"grd1",cpr.data.tabledata.RowState.UPDATED);
 * </code></pre>
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {cpr.data.tabledata.RowState} state 검색할 상태값<br>
			<b>state 종류</b><br>
			cpr.data.tabledata.RowState.UNCHANGED : 변경되지 않은 상태<br>
			cpr.data.tabledata.RowState.INSERTED : 행이 신규로 추가된 상태<br>
			cpr.data.tabledata.RowState.UPDATED : 행이 수정된 상태<br>
			cpr.data.tabledata.RowState.DELETED : 행이 삭제된 상태<br>
 * @return {Array} row index 배열
 */
GridKit.prototype.getRowStatedIndices = function(app, psGridId, state) {
	var vcGrid = app.lookup(psGridId);
	return vcGrid.dataSet.getRowStatedIndices(state);
};

/**
 * 그리드의 로우 갯수 반환
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @return {Number}  로우 카운트 
 */
GridKit.prototype.getRowCount = function(app, psGridId) {
	var vcGrid = app.lookup(psGridId);
	return vcGrid.rowCount;
};

/**
 * 그리드의 현재 선택된 행의 인덱스(Index)를 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @return {Number} 로우(Row) 인덱스 
 */
GridKit.prototype.getIndex = function(app, psGridId) {
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	
	return vcGrid.selectionUnit != "cell" ? vcGrid.getSelectedRowIndex() : vcGrid.getSelectedIndices()[0] == null ? -1 : vcGrid.getSelectedIndices()[0]["rowIndex"];
};

/**
 * 그리드의 타이틀명을 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @return {String} 타이틀 문자열 
 */
GridKit.prototype.getTitle = function(app, psGridId) {
	var titleCtlrs = this._appKit.Group.getAllChildrenByType(app, "udc.com.udcComGridTitle");
	if (titleCtlrs != null) {
		for (var i = 0, len = titleCtlrs.length; i < len; i++) {
			if (titleCtlrs[i].ctrl && titleCtlrs[i].ctrl.id == psGridId) {
				return titleCtlrs[i].title;
			}
		}
	}
	return "";
};

/**
 * 그리드의 특정 컬럼에 포커싱을 처리한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {String} psDataColumnId 데이터 컬럼 cell index 또는 column name
 * @param {Number} pnRowIndex? 행 인덱스
 * @return void
 */
GridKit.prototype.setFocusColumn = function(app, psGridId, psDataColumnId, pnRowIndex) {
	/** @type cpr.controls.Grid */
	var grid = app.lookup(psGridId);
	var rowIndex = pnRowIndex == null ? this.getIndex(app, psGridId) : pnRowIndex;
	
	if (grid.readOnly) {
		grid.selectRows([rowIndex]);
	} else {
		grid.selectRows([rowIndex]);
		grid.setEditRowIndex(rowIndex, true);
	}
	grid.focusCell(rowIndex, psDataColumnId);
};

/**
 * 그리드 디테일 columnname로 헤더 컬럼 취득
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {#column} psColumnName 컬럼명
 * @return {Array} 헤더 컬럼 배열Array (cpr.controls.gridpart.GridColumn)
 */
GridKit.prototype.getHeaderColumn = function(app, psGridId, psColumnName) {
	/** @type cpr.controls.Grid*/
	var vcGrid = app.lookup(psGridId);
	var vaDetailColumn = vcGrid.detail.getColumnByName(psColumnName);
	
	var vaHeaderColumns = new Array();
	vaDetailColumn.forEach(function(dColumn) {		
		var vaHeaderCellIndex = vcGrid.getHeaderCellIndices(dColumn.cellIndex);
		vaHeaderCellIndex.forEach(function(each) {
			vaHeaderColumns.push(vcGrid.header.getColumn(each));
		});
	});
	
	return vaHeaderColumns;
};

/**
 * 그리드 디테일 컬럼의 ColIndex로 헤더 컬럼 취득
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드ID
 * @param {Number} pnColIndex 컬럼 ColIndex
 * @return {Array} 헤더 컬럼 배열Array (cpr.controls.gridpart.GridColumn)
 */
GridKit.prototype.getHeaderColumnByColIdex = function(app, psGridId, pnColIndex) {
	/** @type cpr.controls.Grid*/
	var vcGrid = app.lookup(psGridId);
	var voHeader = vcGrid.header;
	
	var vaHeaderColumns = new Array();
	var hColumn;
	for (var i = 0, len = voHeader.cellCount; i < len; i++) {
		hColumn = voHeader.getColumn(i);
		if (hColumn != null && hColumn.colIndex == pnColIndex) {
			vaHeaderColumns.push(hColumn);
		}
	}
	
	return vaHeaderColumns;
};

/**
 * 그리드 헤더 컬럼의 텍스트(text) 문자열을 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {#column} psColumnName 컬럼명
 * @return {String} 헤더 컬럼 text
 */
GridKit.prototype.getHeaderColumnText = function(app, psGridId, psColumnName) {
	var vaColumns = this.getHeaderColumn(app, psGridId, psColumnName);
	return vaColumns.length > 0 ? vaColumns[0].getText() : "";
};

/**
 * 그리드 헤더 중에 STATUS 컬럼 객체를 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @return {cpr.controls.gridpart.GridHeaderColumn} 헤더 컬럼
 */
GridKit.prototype.getHeaderStatusColumn = function(app, psGridId) {
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	if(!vcGrid){
		return null;
	}
	
	var header = vcGrid.header;
	var column = null;
	var stateColText = "F";
	if(typeof AppProperties !== 'undefined' && AppProperties.GRID_STATE_COL_HEADER_TEXT){
		stateColText = AppProperties.GRID_STATE_COL_HEADER_TEXT;
	}
	
	var columnLayout = vcGrid.getColumnLayout();	
	for (var i = 0, len = columnLayout.header.length; i < len; i++) {
		var headerCellIndex = columnLayout.header[i].cellIndex;
		column = header.getColumn(headerCellIndex);
		if(column == null) continue;
		
		if (column.getText() == stateColText) {
			return column;
		}
	}
	return null;
};

/**
 * 그리드 내 컬럼을 숨긴다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {#column} psColumnName 컬럼 명
 * @param {Boolean} pbHideAllColumn? 병합된 컬럼 전부 가져올지 여부 (default:true)
 * @return void
 */
GridKit.prototype.hideColumn = function(app, psGridId, psColumnName, pbHideAllColumn) {
	/** @type cpr.controls.Grid*/
	var vcGrid = app.lookup(psGridId);
	var vbHideAllColumn = ValueUtil.isNull(pbHideAllColumn) ? true : pbHideAllColumn;
	
	if (!vbHideAllColumn) {
		var vaDetailColumn = vcGrid.detail.getColumnByName(psColumnName);
		vaDetailColumn.forEach(function(dColumn) {
			vcGrid.columnVisible(dColumn.colIndex, false, "column-index");
		})
	} else {
		var vaColumns = this.getHeaderColumn(app, psGridId, psColumnName);
		if (vaColumns) {
			vaColumns.forEach(function( /* cpr.controls.gridpart.GridHeaderColumn */ each) {
				each.visible = false;
			});
		}
	}
};

/**
 * 그리드 컬럼을 보이도록 변경한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {#column} psColumnName 컬럼명
 * @param {Boolean} pbShowAllColumn? 병합된 컬럼 전부 가져올지 여부 (default:true)
 * @return void
 */
GridKit.prototype.showColumn = function(app, psGridId, psColumnName, pbShowAllColumn) {
	/** @type cpr.controls.Grid*/
	var vcGrid = app.lookup(psGridId);

	var vbShowAllColumn = ValueUtil.isNull(pbShowAllColumn) ? true : pbShowAllColumn;
	if (!vbShowAllColumn) {
		var vaDetailColumn = vcGrid.detail.getColumnByName(psColumnName);
		vaDetailColumn.forEach(function(dColumn) {
			vcGrid.columnVisible(dColumn.colIndex, true, "column-index");
		})
	} else {
		var vaColumns = this.getHeaderColumn(app, psGridId, psColumnName);
		if (vaColumns) {
			vaColumns.forEach(function( /* cpr.controls.gridpart.GridHeaderColumn */ each) {
				each.visible = true;
			});
		}
	}
};

/**
 * 그리드 데이터를 조건에 맞게 필터링한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {#expression} psCondition 데이터 필터링 조건
 * @return void
 */
GridKit.prototype.setFilter = function(app, psGridId, psCondition) {
	/** @type cpr.controls.Grid*/
	var grid = app.lookup(psGridId);
	grid.setFilter(psCondition);
};

/**
 * 그리드 데이터 필터링을 취소한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @return void
 */
GridKit.prototype.clearFilter = function(app, psGridId) {
	/** @type cpr.controls.Grid*/
	var grid = app.lookup(psGridId);
	grid.clearFilter();
};

/**
 * 그리드의 데이터셋의 FindRow를 지정한다.<br>
 * 해당 함수 사용시 그리드 조회시 psFindRowCond로 지정된 행이 자동 선택된다. 
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {String} psCondition 조건식 <br>
 *                 ex)"STUD_DIV_RCD == 'CT101REGU' && SA_NM == '컴퓨터정보과'" <br>
 * 					사용가능수식 !=", "!==", "$=", "%", "&&", "(", "*", "*=", "+", ",", "-", ".", "/", "/*", "//", "<", "<=", "==", "===", ">", ">=", "?", "[", "^=", "||" 
 * @return void
 */
GridKit.prototype.setFindRowCondition = function(app, psGridId, psCondition) {
	var vcGrid = app.lookup(psGridId);
	vcGrid.dataSet.userData("_findRowCondition", psCondition);
};

/**
 * 현재 로우의 key(pk) value를 반환한다. 
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {Number} pnRowIndex? 취득하고자하는 row index. <br>
 *                 defalut : 선택된 rowindex
 * @return {String}
 */
GridKit.prototype.getRowPKColumnValues = function(app, psGridId, pnRowIndex) {
	/** @type cpr.controls.Grid*/
	var vcGrid = app.lookup(psGridId);
	/** @type cpr.data.DataSet */
	var vcDataSet = vcGrid.dataSet;
	
	var rowIndex = pnRowIndex == null ? this.getIndex(app, psGridId) : pnRowIndex;
	
	var vaPkColmns = ValueUtil.split(vcDataSet.info, ",");
	var vaTempCond = [];
	vaPkColmns.forEach(function(column) {
		var vsPkValue = vcDataSet.getValue(rowIndex, column);
		vaTempCond.push(column + "==" + "'" + vcDataSet.getValue(rowIndex, column) + "'");
	});
	
	return vaTempCond.length > 0 ? vaTempCond.join(" && ") : "";
};

/**
 * 그리드의 선택 유/무 체크 및 PK값이 입력되어 있는지를 체크한다. 
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {String | Array} paIgnoreCol PK값 입력 체크 예외 컬럼명
 * @return {Boolean}
 */
GridKit.prototype.checkSelectionWithPkValues = function(app, psGridId, paIgnoreCol) {
	/** @type cpr.controls.Grid*/
	var vcGrid = app.lookup(psGridId);
	var rowIndex = this.getIndex(app, vcGrid.id);
	//Row 선택여부 확인
	if (rowIndex < 0) {
		this._appKit.Msg.alertDlg(app, "INF-M129", [vcGrid.fieldLabel]); //선택된 데이터가 없습니다.
		return false;
	}
	//ROW의 PK값 입력여부 체크
	var vaPKColumns = ValueUtil.split(ValueUtil.fixNull(vcGrid.dataSet.info), ",");
	var valid = true,
		text, focusColumn, vbChk = false;
	for (var i = 0, len = vaPKColumns.length; i < len; i++) {
		if (ValueUtil.isNull(vcGrid.getCellValue(rowIndex, vaPKColumns[i]))) {
			if (!(paIgnoreCol instanceof Array)) {
				paIgnoreCol = [paIgnoreCol];
			}
			
			vbChk = false;
			paIgnoreCol.some(function(colName) {
				if (colName == vaPKColumns[i]) {
					vbChk = true;
					return false;
				}
			});
			
			if (vbChk) continue;
			
			valid = false;
			focusColumn = vaPKColumns[i];
			text = this.getHeaderColumnText(app, vcGrid.id, focusColumn);
			break;
		}
	}
	
	if (!valid) {
		//항목은 필수입력 항목입니다.
		this._appKit.Msg.alertDlg(app, "WRN-M001", [vcGrid.fieldLabel + "의 " + text]);
		vcGrid.setEditRowIndex(rowIndex, true);
		vcGrid.focusCell(rowIndex, focusColumn);
		//포커싱할 컬럼이 UDC인 경우에...
		var vaDetailColumns = vcGrid.detail.getColumnByName(focusColumn);
		var dctrl = vaDetailColumns != null && vaDetailColumns.length > 0 ? vaDetailColumns[0].control : null;
		if (dctrl != null && dctrl instanceof cpr.controls.UDCBase) {
			var empApp = dctrl.getEmbeddedAppInstance();
			dctrl = AppUtil.getUDCBindValueControl(dctrl);
			if (dctrl) empApp.focus(dctrl.id);
		}
		return false;
	}
	
	return true;
};

/**
 * 그리드에 보여지는 데이터를 엑셀로 export 한다. <br>
 * - 사이트별 Customizing 필요
 * @param {cpr.core.AppInstance} app 앱 인스턴스
 * @param {#grid | #grid[]} psGridId 그리드 ID <br>(Array 로 작성 할 경우 그리드 개수만큼 시트가 생성되며, 각 시트별 한개씩 그리드가 순차적으로 export 된다)
 * @param {String} psFileName export 파일명
 * @param {Array} metadata 엑셀 익스포트시 설정 정보 (password(암호), printPageOrientation(출력용지방향)) 
 * @param {
 *   fileType? : "xlsx" | "xls" | "csv" <!-- 파일유형 (default: xlsx) -->,
 *   applyFormat? : Boolean <!-- 익스포트시 포맷을 적용시킬 지 여부 (default: true) --> ,
 *   freezeHeader? : Boolean <!--엑셀 익스포트시 헤더 부분을 틀고정할지 여부를 설정 (default; false)  -->,
 *   useFormat? : Boolean <!--엑셀로 익스포트시 엑셀의 format 기능을 사용할지 여부 (default; true)  -->,
 *   applySuppress? : Boolean <!-- 엑셀 익스포트시 suppress, mergedToIndexExpr에 의한 셀 병합을 반영할지 여부(default: true) -->,
 *   reduce? : Boolean <!-- 엑셀 익스포트 데이터에서 불필요한 데이터를 제거할지 여부 (default: true) -->,
 *   excludeColumns? : String <!-- 출력시 제외할 컬럼명 (여러개인 경우 콤마로 구분)<br> ex) COL1,COL2,COL3 -->,
 *   isExcludeHideColumn? : Boolean <!-- 숨김컬럼 제외 여부 (default: true) -->,
 *   excludePart?: cpr.controls.gridpart.VLOC_REGION <!-- 출력시 제외영역 (header,detail,footer,gheader,gfooter)<br>ex) footer,gfooter -->,
 *   rows?: Number[] <!-- 익스포트시킬 특정 row index 배열 -->,
 *   exportForm? : Boolean <!-- export 시 빈양식으로 다운로드 할지 여부 (default: false) -->,
 *   handler? : (datas : Number, rowIndex : Number)=>Void <!-- 엑셀 익스포트시 행별 스타일 또는 데이터를 변경할 수 있는 핸들러  -->,
 *   sheetName? : String[] <!-- 시트명 배열<br>배열 순서대로 시트명으로 적용되며, 시트명이 없는 경우 Sheet@(@는 넘버링)으로 적용된다. -->,
 *   showLoadmask? : Boolean <!-- 익스포트 시 로드마스크를 띄울지 여부 (default: false) -->,
 *   showRowIndex? : Boolean <!-- rowindex 특수컬럼을 export 할지 여부 (default: false) -->,
 *   headerDelimiter? : String <!-- csv로 export시, 헤더가 다중행일 경우  헤더 텍스트를 합칠 때 사용되는 구분자(default: /) -->
 * } options? 그리드 엑셀 익스포트 옵션
 * @param {
 *   (pbSuccess: Boolean, 
 *   poData: {fileName: String<!-- export 파일명 -->}
 *   ) => Void } poCallback? 익스포트 후 콜백함수<br>(첫번째 파라미터: 서브미션 성공여부 / 두번째 파라미터: 엑셀다운로드 관련 데이터)
 * @return void
 */
GridKit.prototype.exportData = function(app, psGridId, psFileName, metadata, options, poCallback) {
	var _this = this;
	var _app = app;
	
	/* options의 기본값 세팅 */
	if(ValueUtil.isNull(options)) options = {};
	options = _.defaults(options, {
		fileType : "xlsx",
		applyFormat : true,
		freezeHeader : false,
		useFormat : true,
		applySuppress: true,
		reduce : true,
		isExcludeHideColumn : true,
		exportForm : false,
		sheetName : [],
		showLoadmask : false,
		showRowIndex : false,
		headerDelimiter : "/"
	});
	
	if (!(psGridId instanceof Array)) {
		psGridId = [psGridId];
	}
	
	if(options.showLoadmask) {
		_this._appKit.showLoadMask(app);
	}
	
	var subExport = new cpr.protocols.Submission();
	subExport.action = "../export/" + psFileName.replace("\/", "") + "." + options.fileType;
	subExport.mediaType = "application/json";
	subExport.responseType = "blob";
	subExport.addParameter("filename", psFileName.replace("\/", ""));
	subExport.addEventListenerOnce("submit-error", function(e) {		
		//엑셀 내보내기시 장애가 발생하였습니다. 관리자에게 문의 하시기 바랍니다.
		_this._appKit.Msg.alertDlg(_app, "ERR-M011");
	});
	subExport.addEventListenerOnce("submit-done", function(e) {
		if(options.showLoadmask) {
			var vcFloatLoadmask = cpr.core.NotificationCenter.INSTANCE.loadmask;
			if (vcFloatLoadmask == null || vcFloatLoadmask._activeSubmission == null || vcFloatLoadmask._activeSubmission.length == 0) {
				_this._appKit.hideLoadMask(_app);
			}
		}
		
		if (!ValueUtil.isNull(poCallback) && TypeUtil.isFunc(poCallback)) {
			poCallback(e.control.isSuccess(), {
				fileName : psFileName.replace("\/", "") + "." + options.fileType
			});
		}
	})
	
	var voExportOptions = [];
		
	// 각 그리드 별 exportOption 생성
	psGridId.forEach(function(gridId, pnIndex) {
		/** @type cpr.controls.Grid */
		var vcGrid = app.lookup(gridId);
		
		var columnLayout = _.clone(vcGrid.getColumnLayout());
		
		//기본 출력 제외 컬럼(인덱스 컬럼, 선택용 체크 컬럼)
		var voColumn, voHColumn;
		var vaExcludeCollIndexs = [];
		var vaExportRowIndex = [];
		
		for (var i = 0, len = columnLayout.detail.length; i < len; i++) {
			var detailCellIndex = columnLayout.detail[i].cellIndex;
			voColumn = vcGrid.detail.getColumn(detailCellIndex);
			if (voColumn == null) continue;
			
			var detailColIndex =  columnLayout.detail[i].colIndex;
//			if(voColumn.columnType == "rowindex" && !options.showRowIndex) {
//				vaExcludeCollIndexs.push(detailColIndex);
//			} else 
			if (voColumn.columnType == "checkbox" || voColumn.columnType == "radio") {
				vaExcludeCollIndexs.push(detailColIndex);
			} else if (voColumn.control instanceof cpr.controls.UDCBase) {
				if (voColumn.control == null || voColumn.control.getBindInfo("value") == null) {
					vaExcludeCollIndexs.push(detailColIndex);
				}
			}
			/* 커스텀 옵션 : 컬럼명이 바인딩되지 않은 컬럼 출력 예외처리*/
			//		else if(voColumn.columnName == null || voColumn.columnName == ""){
			//			vaExcludeCellIndexs.push(i);
			//		}
			else {
				//숨김컬럼 제외
				if (options.isExcludeHideColumn) {
					// 기존 그리드에서 visible=false 된 colindex는 export에서 제외
					vcGrid.getColIndicesByVisible(false).forEach(function(colidx) {
						if(vaExcludeCollIndexs.indexOf(colidx) == -1) vaExcludeCollIndexs.push(colidx);
					})
				}
			}
		}
		
		//상태컬럼 제외
		var statusColumn = _this.getHeaderStatusColumn(app, vcGrid.id);
		if (statusColumn != null) {
			vaExcludeCollIndexs.push(statusColumn.colIndex);
		}
		
		//그외 추가적인 출력 제외 컬럼이 존재하는 경우
		if (!ValueUtil.isNull(options.excludeColumns)) {
			var vaExclColumns = ValueUtil.split(options.excludeColumns, ",");
			var vaDColumns;
			for (var j = 0, jlen = vaExclColumns.length; j < jlen; j++) {
				vaDColumns = vcGrid.detail.getColumnByName(vaExclColumns[j].trim());
				if (vaDColumns) {
					vaDColumns.forEach(function( /* cpr.controls.gridpart.GridDetailColumn */ each) {
						vaExcludeCollIndexs.push(each.colIndex);
					});
				}
			}
		}
		
		// 그리드 출력 스타일지정
		// band(region)별로 원하는 스타일 추가 (header, detail, footer, gheader, gfooter)
		var styleHandler = function(cellStyle, region, cellIndex, columnName) {
			var voStyleInfo = {
				"text-align": "center",
				"border-bottom-color": "black",
				"border-bottom-style": "solid",
				"border-bottom-width": "1px",
				"border-left-color": "black",
				"border-left-style": "solid",
				"border-left-width": "1px",
				"border-right-color": "black",
				"border-right-style": "solid",
				"border-right-width": "1px",
				"border-top-color": "black",
				"border-top-style": "solid",
				"border-top-width": "1px"
			}				
			voStyleInfo = Object.assign({}, cellStyle["style"], voStyleInfo);
			
			if (region == "header") {
				voStyleInfo["background-color"] = "#dddddd";
				cellStyle["style"] = voStyleInfo;
			} else if (region == "detail") {
				cellStyle["style"] = voStyleInfo;
			}		
		}
		
		var exportOption = {
			exceptStyle: false,
			applyFormat: options.applyFormat,
			freezeHeader: options.freezeHeader,
			applySuppress: options.applySuppress,
			useFormat: options.useFormat,
			reduce: options.reduce,
			excludeColIndex: vaExcludeCollIndexs,
			rowDataHandler: options.handler,
			rowgroupStyleHandler: styleHandler
		};
		
		if(options.exportForm) {
			exportOption.rows = -1;
		}
		
		// 출력시킬 특정 로우 인덱스
		if (!ValueUtil.isNull(options.rows)) {
			exportOption.rows = options.rows;
		} 
		
		var exportData = vcGrid.getExportData(exportOption);
		
		if (options.exportForm && exportOption.rows == -1) {
			//양식을 만든다. (빈 행 1Row 추가)
			var voRowGroups = exportData["rowgroups"];
			for (var idx = 0; idx < voRowGroups.length; idx++) {
				var voRowGrp = voRowGroups[idx];
				if (voRowGrp["region"] == "detail") {
					var tempRows = [];
					for (var idx2 = 0; idx2 < voRowGrp["style"].length; idx2++) {
						tempRows.push({
							value: "",
							style: {}
						});
					}
					voRowGrp["data"].push(tempRows);
				}
			}
			
			//풋터 그룹픗터 그룹해더는 제외한다. 
			var tempRegion = ["footer", "gheader", "gfooter"];
			for (var k = (voRowGroups.length - 1); k >= 0; k--) {
				if (tempRegion.indexOf(voRowGroups[k].region) > -1) {
					exportData.rowgroups.splice(k, 1);
				}
			}
		}
		
		//풋터 또는 그룹풋터 제외하는 경우
		if (!ValueUtil.isNull(options.excludePart)) {
			if (!(options.excludePart instanceof Array)) {
				options.excludePart = [options.excludePart];
			}
			
			options.excludePart.forEach(function(part) {
				var len = exportData.rowgroups.length;
				for (var i = (len - 1); i >= 0; i--) {
					if (exportData.rowgroups[i].region == part) {
						exportData.rowgroups.splice(i, 1);
					}
				}
			})
		}
		
		// csv export 시, 멀티헤더일 경우 헤더 텍스트 머징
		if(options.fileType.toLowerCase() == "csv" && vcGrid.header.getRowHeights().length > 1) {
			for (var i = 0, len = exportData.rowgroups.length; i < len; i++) {
				var rowGroup = exportData.rowgroups[i];
				if(rowGroup.region == "header") {
					rowGroup.data[0] = []; // 기존 헤더 데이터 초기화
					
					vcGrid.getColIndicesByVisible(true).forEach(function(index){
						if(vaExcludeCollIndexs.indexOf(index) == -1) {
							rowGroup.data[0].push({
								value: _getColumnText(vcGrid, index, options.headerDelimiter),
								style: {}
							})
						}
					});
				}
			}
		}
		
		// 시트명 설정
		exportData.name =  ValueUtil.fixNull(options.sheetName[pnIndex], ("Sheet"+pnIndex), false);
		
		// exportOption 저장
		voExportOptions.push(exportData);
	}); // end of grid forEach
	
	
	// export 하기 위한 옵션 merge
	var reqExportObj = cpr.utils.ExportUtil.merge(voExportOptions);
	
	// metatdata  설정
	reqExportObj["metadata"] = {};
	reqExportObj["metadata"]["fileName"] = psFileName;
	
	if (metadata != null) {
		if (metadata["password"] != null) {
			reqExportObj["metadata"]["password"] = metadata["password"];
		}
		if (metadata["printPageOrientation"] != null) {
			reqExportObj["metadata"]["printPageOrientation"] = metadata["printPageOrientation"];
		}
	}
	reqExportObj["metadata"]["ext"] = options.fileType;
		
	subExport.setRequestObject(reqExportObj);
	
	if (options.applySuppress) {
		subExport.setHeader("applySuppress", options.applySuppress.toString());
	}
	subExport.setHeader("ext", options.fileType);
	
	return subExport.send();
	
	/**
	 * 그리드 헤더 텍스트 반환
	 * 멀티헤더의 경우 "-" 로 연결되어 반환
	 * @param {cpr.controls.Grid} pcGrid
	 * @param {Number} pnIndex
	 * @param {String} delimiter
	 */
	function _getColumnText(pcGrid, pnIndex, delimiter) {
		
		var headerCell = pcGrid.getHeaderCellIndices(pnIndex);
		
		var vsResult = "";
		if (headerCell.length > 0) {
			headerCell.forEach(function(each) {
				var colNm = pcGrid.header.getColumn(each);
				vsResult += (colNm.text || "") + delimiter;
			});
			
			if (vsResult != "") {
				vsResult = vsResult.substr(0, vsResult.length - delimiter.length);
			}
		}
		
		return vsResult;
	}
};

/**
 * 그리드의 행 레이아웃을 정의합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {Number} pnRowCnt 그리드에 보여질 Header와 Detail영역의 Row수 
 * @param {Number} pnDefaultMergedColIndex 자동으로 머지될 컬럼 인덱스(ex 2: 2열 이전의 컬럼은 자동으로 머지됩니다.)
 */
GridKit.prototype.addHeaderRow = function(app, psGridId, pnRowCnt, pnDefaultMergedColIndex) {
	/**@type cpr.controls.Grid*/
	var grid = app.lookup(psGridId);	
	var originConfig = grid.getInitConfig();
	var originRowCnt = originConfig.header.rows.length;
	
	originConfig.header.rows = []
	for (var i = 0; i < pnRowCnt; i++) {
		originConfig.header.rows.push({
			height: '24px'
		});
	}
	
	originConfig.header.cells.forEach(function(cell) {		
		if (cell.constraint.colIndex <= pnDefaultMergedColIndex) {
			cell.constraint.rowSpan = originConfig.header.rows.length;
		}
	});
	
	grid.init(_.clone(originConfig));
}

/**
 * 그리드의 행 레이아웃을 정의합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {Number} pnRowCnt 그리드에 보여질 Header와 Detail영역의 Row수 
 * @param {Number} pnDefaultMergedColIndex 자동으로 머지될 컬럼 인덱스(ex 2: 2열 이전의 컬럼은 자동으로 머지됩니다.)
 */
GridKit.prototype.addDetailRow = function(app, psGridId, pnRowCnt, pnDefaultMergedColIndex) {
	/** @type cpr.controls.Grid*/
	var grid = app.lookup(psGridId);	
	var originConfig = grid.getInitConfig();
	var originRowCnt = originConfig.detail.rows.length;
	
	originConfig.detail.rows = [];
	for (var i = 0; i < pnRowCnt; i++) {
		originConfig.detail.rows.push({
			height: '24px'
		});
	}
	
	originConfig.detail.cells.forEach(function(cell) {		
		if (cell.constraint.colIndex <= pnDefaultMergedColIndex) {
			cell.constraint.rowSpan = originConfig.detail.rows.length;
		}
	});
	originConfig.autoFit = "all";
	
	grid.init(_.clone(originConfig));
}

/**
 * 그리드의 행 레이아웃을 정의합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {Number} pnRowCnt 그리드에 보여질 Header와 Detail영역의 Row수 
 * @param {Number} pnDefaultMergedColIndex 자동으로 머지될 컬럼 인덱스(ex 2: 2열 이전의 컬럼은 자동으로 머지됩니다.)
 */
GridKit.prototype.defineRowLayout = function(app, psGridId, pnRowCnt, pnDefaultMergedColIndex) {
	/**
	 * @type cpr.controls.Grid
	 */
	var grid = app.lookup(psGridId);
	var initConfig = grid.getInitConfig();
	
	if (!grid.userData("_originConfigInfo")) {
		var dataSet = initConfig.dataSet
		initConfig.dataSet = null;
		grid.userData("_originConfigInfo", deepCopy(initConfig));
		
		initConfig.dataSet = dataSet;
		grid.userData("_originConfigInfo").dataSet = dataSet;
	}
	
	/* STEP1 - header, detail의 영역을 늘려 셀이 들어갈 공간 확보*/
	//헤더영역 row수 조정
	this.addHeaderRow(app, psGridId, pnRowCnt, pnDefaultMergedColIndex);
	//디테일영역 row수 조정
	this.addDetailRow(app, psGridId, pnRowCnt, pnDefaultMergedColIndex);
	
	var headerCells = [];
	var nextColIdx = pnDefaultMergedColIndex + 1;
	var layout = grid.getColumnLayout();
	
	var rowCursor = 0;
	var cellCursor = 0;
	
	var cellCntInRow = Math.ceil(((layout.detail.length > layout.header.length ? layout.detail.length : layout.header.length - pnDefaultMergedColIndex - 1) / pnRowCnt));
	
	if (grid.userData("_originConfigInfo")) {
		initConfig.columns = deepCopy(grid.userData("_originConfigInfo").columns.slice(0, cellCntInRow + pnDefaultMergedColIndex + 1));
	} else {
		initConfig.columns = initConfig.columns.slice(0, cellCntInRow + pnDefaultMergedColIndex + 1);
	}
	
	/* STEP2 - header영역에 셀을 재배치*/
	for (var i = pnDefaultMergedColIndex + 1; i < initConfig.header.cells.length; i++) {
		
		var headerCell = initConfig.header.cells[i];
		headerCell.constraint.colIndex = nextColIdx;
		headerCell.constraint.rowIndex = rowCursor;
		nextColIdx += headerCell.constraint.colSpan || 1;
		cellCursor += headerCell.constraint.colSpan || 1;
		
		if (cellCursor >= cellCntInRow) {
			cellCursor = 0;
			nextColIdx = pnDefaultMergedColIndex + 1;
			rowCursor++;
		}
		
	}
	
	nextColIdx = pnDefaultMergedColIndex + 1;
	cellCursor = 0;
	rowCursor = 0;
	
	/* STEP3 - detail영역에 셀을 재배치*/
	for (var i = pnDefaultMergedColIndex + 1; i < initConfig.detail.cells.length; i++) {
		
		var detailCell = initConfig.detail.cells[i];
		detailCell.constraint.colIndex = nextColIdx;
		detailCell.constraint.rowIndex = rowCursor;
		nextColIdx += detailCell.constraint.colSpan || 1
		cellCursor += detailCell.constraint.colSpan || 1
		
		if (cellCursor >= cellCntInRow) {
			cellCursor = 0;
			nextColIdx = pnDefaultMergedColIndex + 1;
			rowCursor++;
		}
	}
	
	/* STEP4 - 변경한 config정보 반영*/
	grid.init(initConfig);
	
	/**
	 * 깊은복사
	 * @param {any} obj
	 */
	function deepCopy(obj) {
		if (obj === null || typeof(obj) !== 'object' || 'isActiveClone' in obj)
			return obj;
		
		if (obj instanceof Date)
			var temp = new obj.constructor(); //or new Date(obj);
		else
			var temp = obj.constructor();
		
		for (var key in obj) {
			if (Object.prototype.hasOwnProperty.call(obj, key)) {
				obj['isActiveClone'] = null;
				temp[key] = deepCopy(obj[key]);
				delete obj['isActiveClone'];
			}
		}
		return temp;
	}
}

/**
 * 그리드를 로드시점으로 초기화 합니다.
 * @param {any} app 앱인스턴스
 * @param {String} psGridId 그리드 ID
 */
GridKit.prototype.revertRowLayout = function(app, psGridId) {
	/** @type cpr.controls.Grid*/
	var grid = app.lookup(psGridId);
	grid.init(grid.userData("_originConfigInfo"));
	grid.userData("_originConfigInfo", null);
}

/**
 * 신규 행을 추가 한 후 현재 선택 되어 있는 행의 데이터를 추가 한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드ID
 * @param {Boolean} pbIsPkColCopy? pk 컬럼을 복사 할지 여부(pk컬럼 : DataSet에 입력 한 info 값) <br/><b> default: true </b>
 * @return {cpr.controls.provider.GridRow} 복사 한 신규 행
 */
GridKit.prototype.rowCopy = function(app, psGridId, pbIsPkColCopy) {
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	if (ValueUtil.isNull(vcGrid)) {
		return;
	}
	
	var voSelectedRow = vcGrid.getSelectedRow();
	if (ValueUtil.isNull(voSelectedRow)) {
		return;
	}
	var vbIsPkColCopy = ValueUtil.isNull(pbIsPkColCopy) ? true : pbIsPkColCopy;
	
	var voRowData = voSelectedRow.getRowData();
	
	var voInsRow = null;
	if (vbIsPkColCopy) {
		voInsRow = this.insertRow(app, vcGrid.id, null, null, voRowData);
	} else {
		/** @type cpr.data.DataSet */
		var vcDataSet = vcGrid.dataSet;
		var vaPkColumnNames = ValueUtil.split(vcDataSet.info, ",");
		vaPkColumnNames.forEach(function(psPkColumnName) {
			voRowData[psPkColumnName] = "";
		});
		voInsRow = this.insertRow(app, vcGrid.id, null, null, voRowData);
	}
	
	return voInsRow;
}

/**
 * 그리드의 트리셀을 접거나 펼친다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드ID
 * @param {#treecell} psTreeCellId 트리셀ID
 * @param {Boolean} pbExpanded? 펼침 여부(true: 펼침, false: 접힘) <br/><b> default: true </b>
 */
GridKit.prototype.treeCellExpanded = function(app, psGridId, psTreeCellId, pbExpanded) {
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	
	/** @type cpr.controls.gridpart.renderer.TreeCell */
	var vcTreeCell = app.lookup(psTreeCellId);
	
	if (ValueUtil.isNull(vcGrid) || ValueUtil.isNull(vcTreeCell)) {
		return false;
	}
	
	var vbExpanded = ValueUtil.isNull(pbExpanded) ? true : pbExpanded;
	if(vbExpanded) vcGrid.expandAll();
	else vcGrid.collapseAll();
}

/**
 * 그리드의 컬럼을 동적으로 구성 한다.<br/>
 * 그리드 헤더 텍스트는 데이터셋 컬럼의 info 를 적용하며, info가 없을 경우 columnName을 사용한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드ID
 * @param {#dataset} psDataSetId 데이터셋ID
 * @param {Boolean} pbUseRowIdx? 컬럼 타입 rowindex 사용 여부<br/><b> default: false </b>
 */
GridKit.prototype.dynamicColumnSet = function(app, psGridId, psDataSetId, pbUseRowIdx) {
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	/** @type cpr.data.DataSet */
	var vcDataSet = app.lookup(psDataSetId);
	
	if (ValueUtil.isNull(vcGrid) || ValueUtil.isNull(vcDataSet)) {
		return false;
	}
	
	/**
	 * Grid의 header cell 정보
	 * {label: String, target: "columnName"}[]
	 */
	var voHeaderCells = [];
	var vaColumnNames = vcDataSet.getColumnNames();
	var vbUseRowIdx = !ValueUtil.isNull(pbUseRowIdx) ? pbUseRowIdx : false;
	if (vbUseRowIdx) {
		vaColumnNames.unshift("_rowindex"); // 배열의 맨 앞에 값을 추가한다.
	}
	
	var vsHdrIndexText = "No";
	if(typeof AppProperties !== 'undefined' && AppProperties.GRID_INDEX_COL_HEADER_TEXT){
		vsHdrIndexText = AppProperties.GRID_INDEX_COL_HEADER_TEXT;
	}
	
	for (var i = 0; i < vaColumnNames.length; i++) {
		var vsColumnNm = vaColumnNames[i];
		var voHeaderCell = {};
		if (vsColumnNm == "_rowindex") {
			voHeaderCell = {
				label: vsHdrIndexText,
				target: ""
			}
			voHeaderCells[voHeaderCells.length] = voHeaderCell;
			continue;
		}
		
		var voColumn = vcDataSet.getColumn(vsColumnNm);
		var voHeader = voColumn.getHeader();
		voHeaderCell = {
			label: ValueUtil.isNull(voHeader.getInfo()) ? voHeader.getName() : voHeader.getInfo(),
			target: voHeader.getName()
		};
		
		voHeaderCells[voHeaderCells.length] = voHeaderCell;
	}
	
	/** @type cpr.controls.gridpart.GridConfig */
	var voGridInfo = {};
	
	/**
	 * Detail Cell에 매핑할 dataset의 columnName 및 셀의 속성 배열<br/>
	 * <pre>
	 * {
	 *  column:string,
	 *  suppressible?:boolean,
	 *  suppressRef?:number,
	 *  control?:string // detail cell의 컨트롤 타입
	 * }
	 * </pre>
	 */
	var voDetailCells = [];
	
	// grid columns 설정
	voGridInfo.columns = [];
	
	//컬럼 autoFit 설정 할 컬럼 인덱스
	var vaAutoFitIdx = [];
	for (var i = 0; i < vaColumnNames.length; i++) {
		if (vaColumnNames[i] == "_rowindex") {
			voGridInfo.columns[voGridInfo.columns.length] = {
				width: "50px"
			};
		} else {
			voGridInfo.columns[voGridInfo.columns.length] = {
				width: "100px"
			};
			//rowindex는 autoFit 설정 하지 않는다.
			vaAutoFitIdx.push(i);
		}
	}
	
	// grid autoFit 설정
	voGridInfo.autoFit = vaAutoFitIdx.join(",");
	
	// grid header 생성
	voGridInfo.header = function() {
		/** @type Object */
		var voHeaderInfo = {
			rows: [],
			cells: []
		};
		
		// grid header의 행 설정
		voHeaderInfo.rows.push({
			height: "36px"
		});
		
		var vnStartColIdx = 0;
		var voCellCreator = function(poHeaderCell, idx) {
			var voCellInfo = {
				constraint: {},
				configurator: function(cell) {
					if (!ValueUtil.isNull(poHeaderCell.target)) {
						cell.targetColumnName = poHeaderCell.target;
					}
					cell.text = poHeaderCell.label;
				}
			};
			
			voCellInfo.constraint.rowIndex = 0;
			voCellInfo.constraint.colIndex = vnStartColIdx;
			
			var voDetailCell = {
				target: poHeaderCell.target
			};
			
			voDetailCells[voCellInfo.constraint.colIndex] = voDetailCell;
			
			voHeaderInfo.cells[voHeaderInfo.cells.length] = voCellInfo;
			
			// 다음셀 연산을 위해 인덱스 값 초기화
			vnStartColIdx = voCellInfo.constraint.colIndex + 1;
		}
		
		voHeaderCells.forEach(voCellCreator);
		
		return voHeaderInfo;
	}();
	
	// grid detail 생성
	voGridInfo.detail = function() {
		var voDetailInfo = {
			rows: [{
				height: "37px"
			}], // detail은 한 행
			cells: []
		};
		
		voDetailCells.forEach(function(poDetailCell, idx) {
			var voCellInfo = {
				constraint: {
					rowIndex: 0,
					colIndex: idx
				},
				configurator: function(cell) {
					if (!ValueUtil.isNull(poDetailCell.target)) {
						cell.columnName = poDetailCell.target;
					} else {
						cell.columnType = "rowindex";
					}
				}
			};
			voDetailInfo.cells[voDetailInfo.cells.length] = voCellInfo;
		});
		return voDetailInfo;
	}();
	
	//데이터셋 설정
	voGridInfo.dataSet = vcDataSet;
	
	//그리드 초기화
	vcGrid.init(voGridInfo);
	
	//그리드 갱신
	vcGrid.redraw();
}

/**
 * sourceIndex의 row를 targetIndex로 변환한다.<br/>
 * 만약 targetIndex가 sourceIndex 보다 클 경우 실제 변환된 인덱스는 targetIndex는 -1 로 변환된다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드ID
 * @param {Number} psSourceIndex row 위치를 바꿀 대상 row index.
 * @param {Number} psTargetIndex row 위치가 바뀔 row index.
 * @param {Boolean} pbAfter? 해당 row index의 뒤로 이동할지 여부. (true:뒤 / false:앞) <br/><b>default: true </b>
 */
GridKit.prototype.moveRowIndex = function(app, psGridId, psSourceIndex, psTargetIndex, pbAfter) {
	
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	if (ValueUtil.isNull(vcGrid)) {
		return;
	}
	
	/** @type cpr.data.DataSet */
	var vcDataSet = vcGrid.dataSet;
	var vbAfter = ValueUtil.isNull(pbAfter) ? true : pbAfter;
	
	vcDataSet.moveRowIndex(psSourceIndex, psTargetIndex, vbAfter);
	
	this.selectRow(app, vcGrid.id, psTargetIndex);
	vcGrid.redraw();
}

/**
 * 그리드의 체크된 Row에 한해서 해당 서브미션을 태운다.<br>
 * <pre><code>   
 * var rvMsg = util.Grid.extGridCheckdSave(app, "grdCrud", "subSave", "IUD", "CRM-M001", function(pbSuccess){ });
 * if( rvMsg != "OK" && rvMsg != "REE10" ){
 * 	util.Msg.alertDlg(app, rvMsg );
 * }
 *  </code></pre>   
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#grid} psGridId 그리드ID
 * @param {#submission} psSubmissionId 저장 시 호출되는 서브미션 ID
 * @param {"IUD" | "IU" | "ID" | "UD" | "I" | "U" | "D"} psSaveType 저장할 type을 정의한다. (row Status기준) <br/>
 * 																		- IUD : 신규,변경,삭제<br/>
 * 																		- IU : 신규,변경<br/>
 * 																		- ID : 신규,삭제<br/>
 * 																		- UD : 변경,삭제<br/>
 * 																		- I : 신규<br/>
 * 																		- U : 변경<br/>
 * 																		- D : 삭제 (주의) 삭제만 있을 경우 선택행을 무조건 삭제로 처리합니다.
 * @param {String} psSaveMsgCd 저장 확인 메시지 코드<br/>저장 전 확인 메시지를 띄우며, 확인 시 체크된 행에 대해 서브미션을 태운다.
 * @param {Function} poCallFunction? 서브미션 callback function 
 * @return {String} Error Code <br>
 * 							- OK  : 정상 <br>
 * 							- ERR00 : 파라미터 장애 <br>
 * 							- REE01 : 선택한 행이 없습니다.<br> 
 * 							- REE03 : 선택된 내역중 변경 내역이 없습니다.<br>
 * 							- REE04 : submission이 이상이 있습니다.( request에 해당 그리드의 dataSet이 없습니다. )<br>
 * 							- REE10 : validate error 
 */
GridKit.prototype.extGridCheckdSave = function(app, psGridId, psSubmissionId, psSaveType, psSaveMsgCd, poCallFunction) {
	
	if (ValueUtil.isNull(psGridId) || ValueUtil.isNull(psSubmissionId) || ValueUtil.isNull(psSaveType)) {
		return "ERR00"; //파라미터장애 
	}
	
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	/** @type cpr.protocols.Submission */
	var vcSubmission = app.lookup(psSubmissionId);
	/** @type cpr.data.DataSet */
	var vcDataSet = vcGrid.dataSet;
	
	if (ValueUtil.isNull(vcGrid) || ValueUtil.isNull(vcSubmission)) {
		return "ERR00"; //파라미터장애 
	}
	
	// 1.선택행이 있는지를 확인 한다. 
	var vaCheckRowIndices = vcGrid.getCheckRowIndices();
	if (vaCheckRowIndices.length < 1) return "REE01" // 선택한 행이 없습니다.
	
	var vaRowDatas = vcDataSet.getRowDatasByState(cpr.data.tabledata.RowState.MODIFIED);
	
	var vnRowIndices = vcDataSet.getRowIndicesByState(cpr.data.tabledata.RowState.MODIFIED);
	
	if (psSaveType != "D") {
		if (vaRowDatas.length < 1) return "REE02"; //변경된 내역이 없습니다.  
	}
	
	// 임시 dataSet을 생성한다. 
	var voTempDataSet = app.lookup(vcDataSet.id + "_tempCrud");
	if (!voTempDataSet) {
		voTempDataSet = new cpr.data.DataSet(vcDataSet.id + "_tempCrud");
		voTempDataSet.alterColumnLayout = voTempDataSet.alterColumnLayout;
		
		var vaDsHeaders = vcDataSet.getHeaders();
		vaDsHeaders.forEach(function(eachHeader) {
			voTempDataSet.addColumn(eachHeader);
		});
		//user attribute를 얻는다. 
		voTempDataSet.userAttr(vcDataSet.userAttr());
		app.register(voTempDataSet);
	} else {
		voTempDataSet.clear();
	}
	
	var vnActionItemCnt = 0;
	var vaSelectIndex = [];
	
	// 삭제만 있을 경우 선택행을 처리 루틴이 다름 
	if (psSaveType == "D") {
		
		vaCheckRowIndices.forEach(function(eachIndex) {
			var voGridRow = vcGrid.getRow(eachIndex);
			if (voGridRow.rowChecked) {
				var voRow = voTempDataSet.addRowData(voGridRow.getRowData());
				voRow.setState(cpr.data.tabledata.RowState.DELETED);
				vnActionItemCnt = vnActionItemCnt + 1;
			}
		});
		
	} else {
		
		// Data를 Copy한다. 
		if (psSaveType.indexOf("I") > -1) {
			var vaInsertRow = vnRowIndices[cpr.data.tabledata.RowState.INSERTED];
			if (vaInsertRow) {
				vaInsertRow.forEach(function(eachIndex) {
					var voGridRow = vcGrid.getRow(eachIndex);
					if (voGridRow.rowChecked && voGridRow.getState() == cpr.data.tabledata.RowState.INSERTED) {
						vaSelectIndex.push(eachIndex); // validate
						
						var voRow = voTempDataSet.addRowData(voGridRow.getRowData());
						voRow.setState(cpr.data.tabledata.RowState.INSERTED);
						vnActionItemCnt = vnActionItemCnt + 1;
					}
				});
			}
		}
		
		if (psSaveType.indexOf("U") > -1) {
			var vaUpdateRow = vnRowIndices[cpr.data.tabledata.RowState.UPDATED];
			if (vaUpdateRow) {
				vaUpdateRow.forEach(function(eachIndex) {
					var voGridRow = vcGrid.getRow(eachIndex);
					if (voGridRow.rowChecked && voGridRow.getState() == cpr.data.tabledata.RowState.UPDATED) {
						vaSelectIndex.push(eachIndex); // validate
						
						var voRow = voTempDataSet.addRowData(voGridRow.getRowData());
						voRow.setState(cpr.data.tabledata.RowState.UPDATED);
						vnActionItemCnt = vnActionItemCnt + 1;
					}
				});
			}
		}
		
		if (psSaveType.indexOf("D") > -1) {
			var voDeleteRow = vnRowIndices[cpr.data.tabledata.RowState.DELETED];
			if (voDeleteRow) {
				voDeleteRow.forEach(function(eachIndex) {
					var voGridRow = vcGrid.getRow(eachIndex);
					if (voGridRow.rowChecked && voGridRow.getState() == cpr.data.tabledata.RowState.DELETED) {
						var voRow = voTempDataSet.addRowData(voGridRow.getRowData());
						voRow.setState(cpr.data.tabledata.RowState.DELETED);
						vnActionItemCnt = vnActionItemCnt + 1;
					}
				});
			}
		}
	}
	
	if (vnActionItemCnt == 0) return "REE03"; //선택된 내역중 변경 내역이 없습니다.
	
	// 저장 전 validate 확인 (I, U 상태 Row를 대상으로 유효성 검사 진행)
	if (this._appKit._validateGridFromRowIdex(vcGrid, vaSelectIndex, true) == false) {
		return "REE10"; // validate error 
	}
	
	// submission 의 요청 데이터 확인
	var voReqDataset = vcSubmission.getRequestData(vcDataSet.id);
	if (!voReqDataset) return "REE04"; // submission이 이상이 있습니다.( request에 해당 그리드의 dataSet이 없습니다. )
	
	var _SubmitKit = this._appKit.Submit;
	
	this._appKit.Msg.confirmDlg(app, psSaveMsgCd, vcGrid.fieldLabel, {
		confirmCallback: function() {
			// [확인] 콜백 함수
			
			// submission 의 requestData 를 tempDataset 으로 바인딩한다.
			var vsAlias = ValueUtil.fixNull(voReqDataset.alias, vcDataSet.id);
			
			vcSubmission.addRequestData(voTempDataSet, vsAlias, voReqDataset.payload);
			vcSubmission.removeRequestData(vcDataSet.id);
			
			vcSubmission.addEventListenerOnce("receive", function(e) {
				
				// submission 의 requestData 를 원복한다.
				var voTempReqDataset = vcSubmission.getRequestData(voTempDataSet.id);
				var vsTempAlias = vcDataSet.id;
				var vbPayload = cpr.protocols.PayloadType.modified;
				if (!ValueUtil.isNull(voTempReqDataset)) {
					vsTempAlias = voTempReqDataset.alias;
					vbPayload = voTempReqDataset.payload
				}
				
				vcSubmission.addRequestData(vcDataSet, vsTempAlias, vbPayload);
				vcSubmission.removeRequestData(voTempDataSet.id);
			});
			
			// submission send
			_SubmitKit.send(app, psSubmissionId, poCallFunction);
		},
		cancelCallback: function() {
			// [취소] 콜백 함수
			
			// Dataset을 refresh 한다. 
			voTempDataSet.clear();
			
			//서브미션을 Reset한다.  
			var voTempDataset = vcSubmission.getRequestData(voTempDataSet.id);
			var vsTempAlias = vcDataSet.id;
			var vbPayload = cpr.protocols.PayloadType.modified;
			if (!ValueUtil.isNull(voTempDataset)) {
				vsTempAlias = voTempDataset.alias;
				vbPayload = voTempDataset.payload
			}
			vcSubmission.addRequestData(vcDataSet, vsTempAlias, vbPayload);
			vcSubmission.removeRequestData(voTempDataSet.id);
		}
	});
	
	return "OK";
}

/**
 * 그리드 내 디테일 셀에 배치된 컨트롤의 value-change 이벤트에서 그리드 cell 관련 정보를 return 한다.
 * @param {cpr.core.AppInstance} app 앱 인스턴스
 * @param {cpr.events.CValueChangeEvent} peValueChangeEvent value-change 이벤트 객체
 * @return {
 *   rowIndex : Number  <!-- rowIndex -->,
 *   cellIndex : Number <!-- cellIndex -->,
 *   columnNm : String  <!-- columnName -->,
 *   colIndex : Number  <!-- xolumnIndex -->,
 *   row : cpr.controls.provider.GridRow  <!-- Row --> 
 *  } 그리드에 배치된 컨트롤이 아닐 경우 null 리턴
 */
GridKit.prototype.getCellInfo = function(app, peValueChangeEvent) {

	if (peValueChangeEvent == null || peValueChangeEvent == undefined) return null;
	if(!(peValueChangeEvent instanceof cpr.events.CValueChangeEvent)) return null;
	
	/** @type cpr.controls.UIControl */
	var vcCtrl = peValueChangeEvent.control;
	if (vcCtrl == null || vcCtrl == undefined) return null;
	
	/** @type cpr.controls.Grid */
	var vcGrid = vcCtrl.getParent();
	if (!(vcGrid instanceof cpr.controls.Grid)) return null;
	
	var vsColumnName = null;
	var vnRowIndex = null;
	var vnCellIndex = null;
	var voRow = null;
	
	var voRelatedTgtObj = peValueChangeEvent.relatedTargetObject;
	if (voRelatedTgtObj) {
		vsColumnName = voRelatedTgtObj.columnName;
		vnRowIndex = voRelatedTgtObj.rowIndex;
		vnCellIndex = voRelatedTgtObj.cellIndex;
		voRow = voRelatedTgtObj.row;
	} else {
		vsColumnName = vcCtrl.getBindInfo("value").columnName;
		vnRowIndex = vcCtrl.getBindContext().rowIndex;
		vnCellIndex = vcGrid.getCellIndex(vsColumnName);
		voRow = vcGrid.getRow(vnRowIndex);
	}
	var vnColIndex = vcGrid.detail.getColumn(vnCellIndex).colIndex;
	
	return {
		rowIndex: vnRowIndex,
		cellIndex: vnCellIndex,
		columnNm: vsColumnName,
		colIndex: vnColIndex,
		row: voRow
	};
}

/**
 * 그리드에 배치된 트리셀 중 체크된 행에대한 rowIndex, row 정보를 return 한다.
 * @param {cpr.core.AppInstance} app 앱 인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {Boolean} pbAll? 트리셀 접기 상태와 무관한 전체 행의 체크된 인덱스를 반환받을 지 유무. (default:false)
 * @return {
 *   rowIndex : Number[] <!-- 체크된 트리셀의 행 인덱스 배열 -->,
 *   row : cpr.data.Row[] <!-- 체크된 트리셀의 row 객체 배열 -->
 * }
 */
GridKit.prototype.getCheckedTreeCellInfo = function (app, psGridId, pbAll) {
	
	if (ValueUtil.isNull(app) || ValueUtil.isNull(psGridId)) {
		return;
	}
	
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	if(!(vcGrid instanceof cpr.controls.Grid)) return;
	
	var vbIsAll = ValueUtil.isNull(pbAll) ? false : pbAll;
	var vaTreeCheckIndices = vcGrid.getTreeCheckIndices(vbIsAll);
	
	var rows = [];
	vaTreeCheckIndices.forEach(function(rowIndex){
		var voRow = vcGrid.getRow(rowIndex);
		rows.push(voRow);
	});
	
	return {
		rowIndex : vaTreeCheckIndices,
		row : rows
	}
}

/**
 * 그리드 내에 배치된 트리셀을 parent data 를 통해 트리 그리드를 구성한다. <br/>
 * 해당 트리셀의 사용자 속성(dataCol, parentCol) 에 저장된 컬럼명으로 구성된다.<br/>(dataCol - value column name, parentCol - up value column name)
 * @param {cpr.core.AppInstance} app 앱 인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {#column} psDataColNm 데이터 컬럼명
 * @param {#column} psParentColNm 부모 데이터 컬럼명
 */
GridKit.prototype.buildGridToTree = function(app, psGridId, psDataColNm, psParentColNm) {  
   
    var vsLevel = "temp_l_" ;
    var vsSort = "temp_n_";  
    
   /** @type cpr.controls.Grid */
   var vcGrid = app.lookup(psGridId);
   /** @type cpr.data.DataSet */
   var vcDataSet = vcGrid.dataSet;
   
   var vaTeeCell = this.getTreeCell(app, psGridId);
   if(vaTeeCell.length == 0) return;
   
   var vcTeeCell = vaTeeCell[0];
    var voTempLvlHeader = vcDataSet.getHeader(vsLevel);
    if (voTempLvlHeader == null || voTempLvlHeader == undefined) {
    	vcDataSet.addColumn(new cpr.data.header.DataHeader(vsLevel, "number"));
    }
                        
    var voTempSortHeader = vcDataSet.getHeader(vsSort);
    if (voTempSortHeader == null || voTempSortHeader == undefined) {
    	vcDataSet.addColumn(new cpr.data.header.DataHeader(vsSort, "number"));
    }
    
    vcTeeCell.bind("level").toExpression(vsLevel); 
    
   /** @type cpr.controls.Tree */
   var vcTmpTree = vcGrid.userData("_tree"); 
    if (!vcTmpTree) {
    	var voTreeInfo = {
    		"label": psDataColNm,
    		"value": psDataColNm,
    		"parentValue": psParentColNm,
    		"checked": "checked"
    	};
    	vcTmpTree = new cpr.controls.Tree();
    	(function(tree_1) {
    		tree_1.setItemSet(vcDataSet, voTreeInfo);
    	})(vcTmpTree);
    }
    vcTmpTree.userAttr("_level", vsLevel);
    vcTmpTree.userAttr("_sort", vsSort);

    this._setTreeInfo(vcTmpTree, null, 0);
    vcGrid.userData("_tree", vcTmpTree);
    vcTeeCell.redraw();
    
    vcGrid.sort(vsSort);
    vcGrid.redraw(); 
     
    vcDataSet.deleteColumn(vsSort);
};

/**
 * TreeItem의 children 을 반환한다.
 * @param {cpr.controls.Tree} pcTree 트리 컨트롤
 * @param {cpr.controls.TreeItem} poTreeItem 현재 트리 아이템
 * @return {cpr.controls.TreeItem[]}  
 */
GridKit.prototype._getChilder = function(pcTree, poTreeItem) {
	if (poTreeItem == null) {
		return pcTree.getChildren(poTreeItem);
	} else {
		if (poTreeItem.children == undefined) {
			return [];
		} else {
			return poTreeItem.children;
		}
	}
}

/**
 * 현재 아이템을 기준으로 자식아이템의 row에 level, sort 정보를 사용자속성으로 추가한다.
 * @param {cpr.controls.Tree} pcTree 트리 컨트롤
 * @param {cpr.controls.TreeItem} poTreeItem 현재 트리 아이템
 * @param {Number} pnSortNum
 */
GridKit.prototype._setTreeInfo = function(pcTree, poTreeItem, pnSortNum) {
	var vnSort = pnSortNum;
	
	var voTreeItems = this._getChilder(pcTree, poTreeItem);

	for (var idx = 0; idx < voTreeItems.length; idx++) {
		var voItem = voTreeItems[idx];
		var vnDepth = voItem.depth;
		var voRow = voItem.row;
		
		vnSort = vnSort + 1;
		
		voRow.putValue(pcTree.userAttr("_level"), (vnDepth+1));
		voRow.putValue(pcTree.userAttr("_sort"), vnSort);
		vnSort = this._setTreeInfo(pcTree, voItem, vnSort);
	}
	
	return vnSort;
};

/**
 * 그리드의 Tree Cell 을 parent data 를 통해 트리그리드를 만든다.  
 * @param {cpr.core.AppInstance} app 앱 인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @return {cpr.controls.gridpart.renderer.TreeCell[]}
 */
GridKit.prototype.getTreeCell = function(app, psGridId) {
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	if(ValueUtil.isNull(vcGrid)) return;
	
	/** @type cpr.controls.gridpart.renderer.TreeCell[] */
	var vaTreeCell = [];
	var vnDetailCellIndices = vcGrid.detail.getCellIndices();
	vnDetailCellIndices.forEach(function(eachCellIdx) {
		var vcDetailCtrl = vcGrid.detail.getControl(eachCellIdx);
		if(vcDetailCtrl instanceof cpr.controls.gridpart.renderer.TreeCell) {
			vaTreeCell.push(vcDetailCtrl);
		}
	});
	
	return vaTreeCell;
};

/**
 * 그리드 디테일 컬럼에 적용된 스타일 클래스를 반환합니다.<br/>
 * 익스프레션 바인딩일 경우, 행 별 바인딩이 적용된 결과를 확인 할 수 있습니다.<br/>
 * 디테일 컬럼에 적용된 클래스와 컨트롤이 배치되어 있는 경우 컨트롤에 적용된 클래스를 리턴합니다.
 * @param {cpr.core.AppInstance} app 앱 인스턴스
 * @param {#grid} psGridId 그리드ID
 * @param {Number} pnRowIndex rowindex
 * @param {#column} psColumnName 컬럼명
 * @return {
 	column: String <!-- 컬럼에 적용된 스타일 클래스명 -->,
 	control: String <!-- 컬럼에 배치된 컨트롤에 적용된 스타일 클래스명 -->
 } 
 */
GridKit.prototype.getDetailCellClasses = function(app, psGridId, pnRowIndex, psColumnName) {
	
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	if (!vcGrid || !(vcGrid instanceof cpr.controls.Grid)) return;
	
	var vaClasses = {
		column: []
	};
	
	var vaColumns = vcGrid.detail.getColumnByName(psColumnName);
	vaColumns.forEach(function(column) {
		var row = vcGrid.getRow(Number(pnRowIndex));
		
		// STEP1) 컨트롤 스타일 클래스
		var vcDetailCtrl = column.control;
		if (vcDetailCtrl) {
			var voClassBindInfo = vcDetailCtrl.style.getClassBindInfo();
			if (voClassBindInfo && voClassBindInfo.type == "expression") {
				var expr = new cpr.expression.Expression(voClassBindInfo.expression);
				var result = expr.evaluate(row);
				vaClasses["control"] = result;
			} else {
				vaClasses["control"] = vcDetailCtrl.style.getClasses();
			}
		}
		
		// STEP2) 컬럼 스타일 클래스
		var voColClassBindInfo = column.style.getClassBindInfo();
		if (voColClassBindInfo && voColClassBindInfo.type == "expression") {
			var expr2 = new cpr.expression.Expression(voColClassBindInfo.expression);
			var result2 = expr2.evaluate(row);
			vaClasses["column"] = result2;
		} else {
			vaClasses["column"] = column.style.getClasses();
		}
	});
	
	return vaClasses;
}

/**
 * 그리드의 colIndex에 의한  headerCell을 반환한다. 
 * @param {cpr.core.AppInstance} app 앱 인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {Number} pnColIndex colIndex (그리드 헤더 셀 중, 해당 colIndex 가 포함된 headerCell 리턴)
 * @return {cpr.controls.gridpart.GridHeaderColumn[]}
 */
 GridKit.prototype.getHeaderCellByColumnIndexIn = function(app, psGridId, pnColIndex) {
 	
 	/** @type cpr.controls.Grid */
 	var vcGrid = app.lookup(psGridId);
 	if (!vcGrid || !(vcGrid instanceof cpr.controls.Grid)) return;
 	
 	var vaCells = this.getGridHeaderCells(vcGrid);

 	var vaHeaderCells = vaCells.filter(function(eachCell) {
 		var vnColIndex = eachCell.colIndex;
 		// eachCell이 포함된 영역 중 마지막 colIndex (colSpan 이 넓은 경우,,)
 		var vnLastColIndex = vnColIndex + (eachCell.colSpan > 0 ? (eachCell.colSpan - 1) : 0);
 		
 		// 헤더 셀 중, pnColIndex 가 포함된 eachCell 리턴
 		if (pnColIndex >= vnColIndex && pnColIndex <= vnLastColIndex) return true;
 		return false;
 	});
 	
 	return vaHeaderCells;
 }

/**
 * 그리드의 colIndex에 의한  detailCell을 반환한다. 
 * @param {cpr.core.AppInstance} app 앱 인스턴스
 * @param {#grid} psGridId 그리드 ID
 * @param {Number} pnColIndex colIndex
 * @return {cpr.controls.gridpart.GridDetailColumn[]}
 */
 GridKit.prototype.getDetailCellByColumnIndexIn = function(app, psGridId, pnColIndex) {
 	
 	/** @type cpr.controls.Grid */
 	var vcGrid = app.lookup(psGridId);
 	if (!vcGrid || !(vcGrid instanceof cpr.controls.Grid)) return;
 	
 	var vaCells = this.getGridDetailCells(vcGrid);
 	
 	var vaDetailCells = vaCells.filter(function(eachCell) {
 		var vnColIndex = eachCell.colIndex;
 		// eachCell이 포함된 영역 중 마지막 colIndex (colSpan 이 넓은 경우,,)
 		var vnLastColIndex = vnColIndex + (eachCell.colSpan > 0 ? (eachCell.colSpan - 1) : 0);
 		
 		// 디테일 셀 중, pnColIndex 가 포함된 eachCell 리턴
 		if (pnColIndex >= vnColIndex && pnColIndex <= vnLastColIndex) return true;
 		return false;
 	});
 	
 	return vaDetailCells;
 }
 
 /**
  * 그리드의 확장 레이아웃 정보를 반환한다. (getColumnLayout 의 확장버전)<br/>
  * 확장 레이아웃 정보는 cellIndex에 의한 셀의 순서, 셀의 사이즈, 셀의 visible 정보(columnLayout,header,detail) 를 포함한다.
  * @param {cpr.core.AppInstance} app 앱 인스턴스 
  * @param {#grid} psGridId 그리드 ID
  * @param {String[]} vaInvisibleColumnNm? 디테일 셀 중 visible=false 로 설정한 컬럼명 배열(실제 cell 의 visible 여부와는 관계없이 우선 적용)
  * @return {cpr.controls.gridpart.GridColumnLayout}
  */
 GridKit.prototype.getExtColunmLayout = function(app, psGridId, vaInvisibleColumnNm) {
 	
 	/** @type cpr.controls.Grid */
 	var vcGrid = app.lookup(psGridId);
 	if(!vcGrid || !(vcGrid instanceof cpr.controls.Grid)) return;
 	
 	var vaDetailCells = this.getGridDetailCells(vcGrid);
 	
 	/* 그리드 레이아웃 정보 */
 	var voGridLayout = vcGrid.getColumnLayout(true);
 	var voColumnLayout = voGridLayout.columnLayout;
 	var voHeaderInfo = voGridLayout.header;
 	var voDetailInfo = voGridLayout.detail;
 	
 	// TODO voGridLayout.columnLayout 에 visible 속성이 없는 경우 아래 주석 해제 필요
// 	var vaInvisibleCells = vcGrid.getCellIndicesByVisible(false);
// 	voColumnLayout.forEach(function(eachLyt, idx){
// 		if(ValueUtil.isNull(eachLyt["visible"])) {
// 			eachLyt["visible"] = (vaInvisibleCells.indexOf(idx) != -1 ? true : false)
// 		}
// 	});
 	
 	// detail 에 visible 정보 추가
 	vaDetailCells.forEach(function(eachDCell) {
 		var vnCellIndex = eachDCell.cellIndex;
 		var vnColIndex = eachDCell.colIndex;
 		var vnColSpan = eachDCell.colSpan;
 		var vbCellVisible = vcGrid.getColIndicesByVisible(true).indexOf(eachDCell.colIndex);
 		var vsColumnName = eachDCell.columnName;
 		
 		var vbVisibleTemp = vbCellVisible;
 		var vnTempColIndex = vnColIndex;
 		
 		if (vaInvisibleColumnNm != null && vaInvisibleColumnNm != undefined && vaInvisibleColumnNm.length > 0) {
 			if (vaInvisibleColumnNm.indexOf(vsColumnName) >= 0) {
 				vbVisibleTemp = false;
 			}
 		}
 		
 		if (vbVisibleTemp == false) {
 			for (var idx = 0; idx < vnColSpan; idx++) {
 				vnTempColIndex = vnTempColIndex + idx;
 				if (vbVisibleTemp == false) {
					// vaInvisibleColumnNm에 포함된 컬럼이 바인됭 셀인 경우
 					// columnLayout 의 visible 도 false 로 설정
 					voGridLayout.columnLayout[vnTempColIndex]["visible"] = false;
 				}
 			}
 			voDetailInfo.forEach(function(eachDetailCell) {
 				if (eachDetailCell.cellIndex == vnCellIndex) {
 					eachDetailCell["visible"] = false;
 				}
 			});
 		}
 		
 		voDetailInfo.forEach(function(eachDetailCell) {
 			if (eachDetailCell["visible"] != false) {
 				eachDetailCell["visible"] = true;
 			}
 		});
 		
 	});
 	
 	return voGridLayout;
 }
 
 /**
  * 그리드의 header cell을 전부 가지고 온다. 
  * @param {cpr.controls.Grid} pcGrid 
  * @return {cpr.controls.gridpart.GridHeaderColumn[]}
  */
 GridKit.prototype.getGridHeaderCells = function(pcGrid) {
 	var vnCellIndices = pcGrid.header.getCellIndices();
 	var vaHeaderCells = [];
 	
 	vnCellIndices.forEach(function(eachCellIndex) {
 		var voHColumn = pcGrid.header.getColumn(eachCellIndex);
 		vaHeaderCells.push(voHColumn);
 	});
 	
 	return vaHeaderCells;
 	
 }
 
 /**
  * 그리드의 detail cell을 전부 가지고 온다. 
  * @param {cpr.controls.Grid} pcGrid 
  * @return {cpr.controls.gridpart.GridDetailColumn[]}
  */
 GridKit.prototype.getGridDetailCells = function(pcGrid) {
 	var vnCellIndices = pcGrid.detail.getCellIndices();
 	var vaDetailCells = [];
 	
 	vnCellIndices.forEach(function(eachCellIndex) {
 		var voDColumn = pcGrid.detail.getColumn(eachCellIndex);
 		vaDetailCells.push(voDColumn);
 	});
 	
 	return vaDetailCells;
 }
 
 /**
  * 스크롤 뷰포트 바깥의 보이지 않는 영역까지 모두 그릴지 여부를 설정한다.<br/>
  * @param {cpr.core.AppInstance} app 앱 인스턴스
  * @param {#grid} psGridId  그리드 ID
  * @param {"true" | "false" | "column"} mode? 렌더링 모드 설정 (default : true)<br/>
  * 															[true] : row, column 모두 스크롤에 의해 보이지 않는 영역까지 그린다.<br/>
  * 															[false] : row, column가 스크롤에 의해 보이지 않는 영역은 그리지 않는다.<br/>
  * 															["column"] : column만 스크롤에 의해 보이지 않는 영역까지 그리고 row는 보이지 않는 영역은 그리지 않는다.
  */
 GridKit.prototype.setWholeRenderingMode = function(app, psGridId, mode) {
 	/** @type cpr.controls.Grid */
 	var vcGrid = app.lookup(psGridId);
 	if (vcGrid == null) return;
 	
 	var vsMode = ValueUtil.isNull(mode) ? true : mode;
 	if(vsMode == "true") vsMode = true;
 	if(vsMode == "false") vsMode = false;
 	
 	vcGrid.setWholeRenderingMode(vsMode);
 }
 
 /**
  * 동적(스크립트)으로 그리드를 구성 한다. <br/>
  * @param {cpr.core.AppInstance} app 앱 인스턴스
  * @param {#grid} psGridId 그리드ID
  * @param {
  *   columns : String[]<!-- 그리드 컬럼 별 너비 배열(컬럼 갯수 만큼 필요) <br/> <b> &nbsp * 입력 예시: [{"width":"100px"}, {"width":"100px"}] </b> -->,
  *   dataSetId	: #dataset	<!-- 그리드와 바인딩 되는 데이터셋 ID 또는 데이터셋 객체(app.lookup("dataSetId")) -->,
  *   isSimpleGrid : Boolean <!-- 단순 그리드 구성 여부(데이터셋의 컬럼 순서로 구성 한 단순 출력용 그리드 <br/> <b>* 헤더영역 컬럼명은 데이터셋 컬럼의 info 속성이 존재 할 경우 info 속성 우선</b> <br/> <b>* 디테일영역에 입력 컨트롤 배치X</b> -->,
  *   autoFit : String <!-- 지정된 영역의 너비에 맞게 셀의 크기를 자동조절 <br/> 
  *                         none : 해당 기능을 사용하지 않고, 지정된 넓이로 설정됩니다.<br/>
  *                         all : 모든 컬럼이 균등 계산되어 셀의 크기가 자동 조절됩니다.<br/>
  *                         columnindex : 특정 컬럼 인덱스를 지정시, 해당 컬럼 크기만 조절하여 지정된 영역의 너비에 맞도록 합니다. 콤마(,) 구분자를 통해 여러 컬럼을 설정할 수 있습니다.<br/>
  *                         <b>* default : all</b>  
  *                     -->,
  *   useSimpleGirdColumnType : {
  *     checkbox: Boolean <!-- checkbox 배치 여부(rowindex와 같이 배치 할 경우 checkbox가 첫번째 컬럼에 배치) <b>default : true</b> -->,
  *     rowindex:Boolean <!-- rowindex 배치 여부(checkbox와 같이 배치 할 경우 rowindex가 두번째 컬럼에 배치 함) <b>default : true</b> -->
  *   }<!-- 단순 그리드(isSimpleGrid 속성이 true인 경우)에서 columnType control 배치 여부 <br/> -->,
  *   header : {
  *     rows: cpr.controls.gridpart.GridRowConfig[] <!-- 헤더 로우 높이 배열 (로우 갯수 만큼 필요) <br/> <b>* 입력 예시: [{"height":"100px"}]</b> -->,
  *     cells: cpr.controls.gridpart.GridHeaderCellConfig[] <!-- <b>헤더 영역 내부의 cell 구성 정보(constraint, configurator)</b> -->
  *   }<!-- 헤더 영역 정보 -->,
  *   detail : {
  *     rows: cpr.controls.gridpart.GridRowConfig[] <!-- 디테일 로우 높이 배열 <br/> <b>* 입력 예시: [{"height":"100px"}]</b> -->,
  *     cells: cpr.controls.gridpart.GridHeaderCellConfig[] <!-- <b>디테일 영역 내부의 cell 구성 정보(constraint, configurator)</b> -->
  *   }<!-- 디테일 영역 정보 -->,
  *   footer : {
  *     rows: cpr.controls.gridpart.GridRowConfig[] <!-- 푸터 로우 높이 배열 <br/> <b>* 입력 예시: [{"height":"100px"}]</b> -->,
  *     cells: cpr.controls.gridpart.GridHeaderCellConfig[] <!-- <b>푸터 영역 내부의 cell 구성 정보(constraint, configurator)</b> -->
  *   }<!-- 푸터 영역 정보 -->
  * } poDynamicInfo 동적으로 그리드를 구성 할 정보 <br/>&nbsp <b>* 임시 화면(clx)에서 구성 하고자 하는 그리드 형태로 그리드를 구성 한 후 컴파일 된 결과 파일 보기에서 그리드 구성 정보 소스를 복사하여 파라미터로 전달 시 데이터 구성을 쉽게 할 수 있습니다.</b><br/>
  * 				   <b style="color:red;">&nbsp * 디자인탭 Context Menu(마우스 우클릭) > 보기 > 컴파일된 결과 파일 보기 > // UI Configuration 주석 하위에서 grid.init() 함수 파라미터 부분 참조</b>
  * 
  */
 GridKit.prototype.dynamicGrid = function(app, psGridId, poDynamicInfo) {
 	var _app = app;
 	/** @type cpr.controls.Grid */
 	var vcGrid = _app.lookup(psGridId);
 	if (!vcGrid) {
 		return false;
 	}
 	
 	/**
 	 * 그리드 구성 정보
 	 * @type cpr.controls.gridpart.GridConfig
 	 */
 	var voGridInfo = {
 		columns: [],
 		dataSet: "",
 		header: {},
 		detail: {}
 	};
 	
 	//dataSet 설정
 	if (typeof(poDynamicInfo.dataSetId) == "string") {
 		voGridInfo.dataSet = _app.lookup(poDynamicInfo.dataSetId);
 	} else if (poDynamicInfo.dataSetId instanceof cpr.data.DataSet) {
 		voGridInfo.dataSet = poDynamicInfo.dataSetId;
 	}
 	
 	if (ValueUtil.fixBoolean(poDynamicInfo["isSimpleGrid"])) {
 		//단순 출력용 그리드 구성 이면
 		
 		// TODO 업무시스템의 표준에 따라 그리드 행 별 높이 변경 필요
 		voGridInfo.header.rows = [{
 			height: "36px"
 		}];
 		voGridInfo.detail.rows = [{
 			height: "37px"
 		}];
 		
 		var vcDataSet = voGridInfo.dataSet;
 		var vaColumnNames = vcDataSet.getColumnNames();
 		
 		//컬럼 너비 배열
 		var vaColumns = [];
 		
 		//헤더 영역 셀 구성 정보
 		var vaHeaderCells = [];
 		//디테일 영역 셀 구성 정보
 		var vaDetailCells = [];
 		
 		/**
 		 * checkbox, rowindex 배치 시 동적으로 생성 할 컬럼의 colindex 증가 시키기 위한 변수
 		 */
 		var vnAddIndex = 0;
 		var voUseSimpleGirdColumnType = poDynamicInfo["useSimpleGirdColumnType"];
 		if (!voUseSimpleGirdColumnType) {
 			voUseSimpleGirdColumnType = {
 				checkbox: true,
 				rowindex: true
 			}
 		} else {
 			//checkbox, rowindex 둘 중 하나가 비어 있는 경우 기본값 설정을 위하여
 			voUseSimpleGirdColumnType.checkbox = ValueUtil.fixBoolean(voUseSimpleGirdColumnType.checkbox);
 			voUseSimpleGirdColumnType.rowindex = ValueUtil.fixBoolean(voUseSimpleGirdColumnType.rowindex);
 		}
 		
 		if (voUseSimpleGirdColumnType.checkbox) {
 			vaColumns.push({"width": "50px"}); // TODO 업무시스템의 표준에 따라 checkbox 컬럼 너비 수정 필요
 			
 			//헤더 영역 셀 구성 정보 입력
 			vaHeaderCells.push({
 				"constraint": {"rowIndex": 0, "colIndex": vnAddIndex},
 				"configurator": function(cell) {cell.columnType = "checkbox";}
 			});
 			//디테일 영역 셀 구성 정보 입력
 			vaDetailCells.push({
 				"constraint": {
 					"rowIndex": 0,
 					"colIndex": vnAddIndex
 				},
 				"configurator": function(cell) {
 					cell.columnType = "checkbox";
 				}
 			});
 			vnAddIndex++;
 		}
 		
 		if (voUseSimpleGirdColumnType.rowindex) {
 			var vsHdrIndexText = "No";
			if(typeof AppProperties !== 'undefined' && AppProperties.GRID_INDEX_COL_HEADER_TEXT){
				vsHdrIndexText = AppProperties.GRID_INDEX_COL_HEADER_TEXT;
			}
	
 			vaColumns.push({"width": "50px"}); // TODO 업무시스템의 표준에 따라 rowindex 컬럼 너비 수정 필요
 			
 			//헤더 영역 셀 구성 정보 입력
 			vaHeaderCells.push({
 				"constraint": {"rowIndex": 0, "colIndex": vnAddIndex}, 
 				"configurator": function(cell) {cell.text = vsHdrIndexText;}
 			});
 			//디테일 영역 셀 구성 정보 입력
 			vaDetailCells.push({
 				"constraint": {"rowIndex": 0, "colIndex": vnAddIndex},
 				"configurator": function(cell) {cell.columnType = "rowindex";}
 			});
 			vnAddIndex++;
 		}
 		
 		vaColumnNames.forEach(function(psColumnNm, pnIndex) {
 			vaColumns.push({
 				"width": "100px"
 			});
 			
 			var vsColumnNm = psColumnNm;
 			//헤더 영역 셀 구성 정보 입력
 			vaHeaderCells.push({
 				"constraint": {"rowIndex": 0, "colIndex": pnIndex + vnAddIndex}, //checkbox, rowindex 배치 시 colindex 증가
 				"configurator": function(cell) {
 					var voHeader = vcDataSet.getHeader(vsColumnNm);
 					var vsInfo = voHeader.getInfo();
 					cell.text = ValueUtil.isNull(vsInfo) ? vsColumnNm : vsInfo;
 					
 					//업무시스템에서 아래 기능 사용을 표준으로 지정 시 주석 해제
//					cell.sortable = true;
//					cell.filterable = true;
//					cell.targetColumnName = vsColumnNm;
 				}
 			});
 			
 			//디테일 영역 셀 구성 정보 입력
 			vaDetailCells.push({
 				"constraint": {"rowIndex": 0, "colIndex": pnIndex + vnAddIndex }, //checkbox, rowindex 배치 시 colindex 증가
 				"configurator": function(cell) {
 					cell.columnName = vsColumnNm;
 				}
 			});
 		});
 		
 		// 컬럼 너비 설정
 		voGridInfo.columns = vaColumns;
 		voGridInfo.header.cells = vaHeaderCells;
 		voGridInfo.detail.cells = vaDetailCells;
 	} else {
 		// 컬럼 너비 설정
 		voGridInfo.columns = poDynamicInfo["columns"];
 		
 		//헤더 정보 설정
 		voGridInfo.header = poDynamicInfo["header"];
 		
 		//디테일 정보 설정
 		voGridInfo.detail = poDynamicInfo["detail"];
 		
 		//푸터 정보 설정
 		if (poDynamicInfo["footer"]) {
 			voGridInfo.footer = poDynamicInfo["footer"];
 		}
 	}
 	
 	voGridInfo.autoFit = ValueUtil.isNull(poDynamicInfo["autoFit"]) ? "all" : poDynamicInfo["autoFit"];
 	vcGrid.init(voGridInfo);
 };
 
 /**
  * 동적 그리드 구성 합니다. <br/>
  * <b>* 세번째 파라미터 데이터셋에 표준 형식으로 데이터가 구성 되어 있어야 함 </b> <br/><br/>
  * @param {cpr.core.AppInstance} app 앱인스턴스
  * @param {#grid} psGridId 동적으로 구성 할 그리드ID
  * @param {#dataset} psConfDsId 그리드 구성 정보를 담고 있는 데이터셋ID <br/>
  * 								<table border="1"style="width: 550px;"><tbody><tr><td colspan="3" align="center">데이터셋 컬럼 구성</td></tr><tr><td>컬럼명</td><td>데이터 타입</td><td>설명</td></tr><tr><td>div</td><td>String</td><td>구분(header/detail/footer)</td></tr><tr><td>rowIdx</td><td>Number</td><td>구분 영역의 행 인덱스(첫번째 행 : 0)</td></tr><tr>	<td>mergedYn</td><td>String</td><td>셀 병합 여부(Y/N)</td></tr><tr><td>rowSpan</td><td>Number</td><td>행 병합 사이즈</td></tr>
  * 								<tr><td>colSpan</td><td>Number</td><td>열 병합 사이즈</td></tr>	<tr><td>parentUniqueKey</td><td>String</td><td>최상위 데이터의 유니크한 키 값</td></tr><tr><td>uniqueKey</td><td>String</td><td>유니크한 키 값</td></tr><tr><td>columnValue</td><td>String</td><td><p>구분 영역에 따른 설정 값</p><p>header : cell의 text</p><p>detail : cell에 바인딩 할 column명</p><p>footer : cell에 출력 할 값(문자열 또는 익스프레션 함수)</p></td></tr><tr><td>columnTypeCtrl</td><td>String</td>
  * 								<td><p>nomal/checkbox/rowindex/radio</p><p>* rowindex와 radio는 구분 영역이 detail만 가능</p></td></tr><tr><td>ctrlType</td><td>String</td><td>셀에 배치 할 컨트롤 타입</td></tr></tbody></table>
  *								<br/>
  * 								<table border="1"style="width: 550px;"><tbody><tr><td colspan="10" align="center">데이터셋 데이터 구성 예시</td><tr><tr><td>div</td><td>rowIdx</td><td>mergedYn</td><td>rowSpan</td><td>colSpan</td><td>parentUniqueKey</td><td>uniqueKey</td><td>columnValue</td><td>columnTypeCtrl</td><td>ctrlType</td></tr><tr><td>header</td><td>0</td><td>Y</td><td>2</td><td>1</td><td>&nbsp;</td><td>0001</td><td>&nbsp;</td><td>checkbox</td><td>&nbsp;</td></tr>
  * 								<tr><td>header</td><td>0</td><td>Y</td><td>2</td><td>1</td><td>&nbsp;</td><td>0002</td><td>No</td><td>nomal</td><td>&nbsp;</td></tr><tr><td>header</td><td>0</td><td>Y</td><td>2</td><td>1</td><td>&nbsp;</td><td>0003</td><td>선택</td><td>nomal</td><td>&nbsp;</td></tr><tr><td>header</td><td>0</td><td>Y</td><td>1</td><td>3</td><td>&nbsp;</td><td>0004</td><td>헤더 1</td><td>nomal</td><td>&nbsp;</td></tr><tr><td>header</td><td>1</td><td>N</td>
  * 								<td>&nbsp;</td><td>&nbsp;</td><td>0004</td><td>0005</td><td>헤더 1-1</td><td>nomal</td><td>&nbsp;</td></tr><tr><td>header</td><td>1</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>0004</td><td>0006</td><td>헤더 1-2</td><td>nomal</td><td>&nbsp;</td></tr><tr><td>header</td><td>1</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>0004</td><td>0007</td><td>헤더 1-3</td><td>nomal</td><td>&nbsp;</td></tr><tr><td>header</td><td>0</td><td>Y</td><td>2</td>
  * 								<td>2</td><td>&nbsp;</td><td>0008</td><td>헤더 2</td><td>nomal</td><td>&nbsp;</td></tr><tr><td>header</td><td>0</td><td>Y</td><td>1</td><td>3</td><td>&nbsp;</td><td>0009</td><td>헤더 3</td><td>nomal</td><td>&nbsp;</td></tr><tr><td>header</td><td>1</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>0009</td><td>0010</td><td>헤더 3-1</td><td>nomal</td><td>&nbsp;</td></tr><tr><td>header</td><td>1</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>0009</td>
  * 								<td>0011</td><td>헤더 3-2</td><td>nomal</td><td>&nbsp;</td></tr><tr><td>header</td><td>1</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>0009</td><td>0012</td><td>헤더 3-3</td><td>nomal</td><td>&nbsp;</td></tr><tr><td>detail</td><td>0</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>0013</td><td>&nbsp;</td><td>checkbox</td><td>&nbsp;</td></tr><tr><td>detail</td><td>0</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>0014</td>
  * 								<td>&nbsp;</td><td>rowindex</td><td>&nbsp;</td></tr><tr><td>detail</td><td>0</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>0015</td><td>&nbsp;</td><td>radio</td><td>&nbsp;</td></tr><tr><td>detail</td><td>0</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>0016</td><td>column1</td><td>nomal</td><td>inputbox</td></tr><tr><td>detail</td><td>0</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>0017</td><td>column2</td>
  * 								<td>nomal</td><td>inputbox</td></tr><tr><td>detail</td><td>0</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>0018</td><td>column3</td><td>nomal</td><td>numbereditor</td></tr><tr><td>detail</td><td>0</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>0019</td><td>column4</td><td>nomal</td><td>&nbsp;</td></tr><tr><td>detail</td><td>0</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>0020</td><td>column5</td><td>nomal</td>
  * 								<td>&nbsp;</td></tr><tr><td>detail</td><td>0</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>0021</td><td>column6</td><td>nomal</td><td>inputbox</td></tr><tr><td>detail</td><td>0</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>0022</td><td>column7</td><td>nomal</td><td>inputbox</td></tr><tr><td>detail</td><td>0</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>0023</td><td>column8</td><td>nomal</td><td>numbereditor</td></tr>
  * 								<tr><td>footer</td><td>0</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>0024</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr><tr><td>footer</td><td>0</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>0025</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr><tr><td>footer</td><td>0</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>0026</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr><tr><td>footer</td><td>0</td>
  * 								<td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>0027</td><td>'합계'</td><td>&nbsp;</td><td>&nbsp;</td></tr><tr><td>footer</td><td>0</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>0028</td><td>getSum('column3')</td><td>&nbsp;</td><td>&nbsp;</td></tr><tr><td>footer</td><td>0</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>0029</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr><tr><td>footer</td><td>0</td><td>N</td><td>&nbsp;</td>
  * 								<td>&nbsp;</td><td>&nbsp;</td><td>0030</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td></tr><tr><td>footer</td><td>0</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>0031</td><td>'평균'</td><td>&nbsp;</td><td>&nbsp;</td></tr><tr><td>footer</td><td>0</td><td>N</td><td>&nbsp;</td><td>&nbsp;</td><td>&nbsp;</td><td>0032</td><td>getAvg('column8')</td><td>&nbsp;</td><td>&nbsp;</td></tr></tbody></table>			
  * @param {#dataset} psBindDsId 그리드와 바인딩 할 데이터셋ID <br/>
  * 								<b>* 3번째 파라미터 데이터셋 psConfDsId의 구분이 detail이고 columnValue 값에 해당 하는 컬럼명을 맵핑</b>
  */
 GridKit.prototype.dynamicGridStd = function(app, psGridId, psConfDsId, psBindDsId) {
 	var _this = this;
 	var _app = app;

 	/**
 	 *  동적으로 구성 할 그리드
 	 *  @type cpr.controls.Grid 
 	 */
 	var vcGrid = _app.lookup(psGridId);
 	/**
 	 * 그리드 구성 정보를 담고 있는 데이터셋
 	 * @type cpr.data.DataSet
 	 */
 	var vcGridConfigDataSet = app.lookup(psConfDsId);
 	/**
 	 * 그리드와 바인드 할 데이터셋
 	 * @type cpr.data.DataSet
 	 */
 	var vcGridBindDataSet = app.lookup(psBindDsId);
 	
 	//그리드, 그리드 구성 정보를 담고 있는 데이터셋, 그리드와 바인딩 할 데이터셋이 존재 하지 않을 경우 리턴
 	if (!(vcGrid || vcGridConfigDataSet || vcGridBindDataSet)) {
 		return;
 	}
 	
 	/**
 	 * 그리드 구성 정보
 	 * @type cpr.controls.gridpart.GridConfig
 	 */
 	var voGridInfo = {
 		dataSet: vcGridBindDataSet,
 		autoFit: [],
 		columns: []
 	};
 	
 	//데이터셋 로우 객체의 사용자 정의 속성을 설정하여 사용 하기 위하여 임시 데이터셋에 원본 데이터셋의 데이터를 temp에 복사하여 사용
 	var vcDynamicGridTemp = app.lookup("_dsDynamicGridTemp");
 	if (!vcDynamicGridTemp) { //임시 데이터셋이 App에 등록 되어 있지 않으면
 		vcDynamicGridTemp = new cpr.data.DataSet("_dsDynamicGridTemp");
 		var vaHeaders = vcGridConfigDataSet.getHeaders();
 		vaHeaders.forEach(function(poHeader) {
 			// dataType도 동일하게 복사하기 위하여 addCoulmn으로 구성
 			vcDynamicGridTemp.addColumn(new cpr.data.header.DataHeader(poHeader.getName(), poHeader.getDataType()));
 		});
 		app.register(vcDynamicGridTemp);
 	} else {
 		//임시 데이터셋이 app에 등록 되어 있는 경우 데이터셋 정보만 클리어
 		vcDynamicGridTemp.clear();
 	}
 	
 	//원본 데이터셋의 데이터를 복사
 	vcDynamicGridTemp.build(vcGridConfigDataSet.getRowDataRanged());
 	
 	var vsHdrIndexText = "No";
	if(typeof AppProperties !== 'undefined' && AppProperties.GRID_INDEX_COL_HEADER_TEXT){
		vsHdrIndexText = AppProperties.GRID_INDEX_COL_HEADER_TEXT;
	}
 	
 	/**
 	 * @param {"header" | "detail" | "footer"} psGridpart gridpart(header/detail/footer)
 	 * @param {cpr.data.Row} poRow
 	 * @param {Number} pnColIndex
 	 * @return {
 	 *   colIndex : Number,
 	 *   constraint : {rowindex:Number, colindex:Number, colSpan:Number, rowSpan:Number},
 	 *   columnValue : String,
 	 *   columnTypeCtrl : String,
 	 *   ctrlType : String,
 	 *   autoFit : String
 	 * }
 	 */
 	function _configInfo(psGridpart, poRow, pnColIndex) {
 		var vsPart = psGridpart["part"];
 		var vnRowIdx = poRow.getValue("rowIdx");
 		var vsMergedYn = poRow.getValue("mergedYn");
 		var vsColumnValue = poRow.getValue("columnValue");
 		/** @type {"nomal" | "checkbox" | "radio"} */
 		var vsColumnTypeCtrl = poRow.getValue("columnTypeCtrl");
 		var vsCtrlType = poRow.getValue("ctrlType");
 		if (vnRowIdx > 0) { //헤더 영역 두번째 행인 경우 상위(첫번째)컬럼 정보를 통하여 colIndex 설정
 			var vsParentUniqueKey = poRow.getValue("parentUniqueKey");
 			var voParentRow = vcDynamicGridTemp.findFirstRow("uniqueKey == '" + vsParentUniqueKey + "'");
 			var vnParentColIndex = voParentRow.getAttr("_colIndex");
 			var vnParentColSapn = voParentRow.getValue("colSpan");
 			var vnChildColIndexRange = vnParentColIndex + vnParentColSapn;
 			if (ValueUtil.isNull(poRow.getAttr("_colIndex"))) {
 				//같은 로우 인덱스를 가지는 자식 로우들 반환 
 				var voRowToChilds = vcDynamicGridTemp.findAllRow("rowIdx == '" + vnRowIdx + "' && parentUniqueKey == '" + vsParentUniqueKey + "'");
 				for (var i = 0; i < voRowToChilds.length; i++) {
 					var voChildRow = voRowToChilds[i];
 					var voBefChildRow = voRowToChilds[(i - 1)];
 					var vnChildColIndex = vnParentColIndex + i;
 					if (i > 0) {
 						var vnBefChildColSpan = ValueUtil.fixNumber(voBefChildRow.getValue("colSpan"));
 						var vnAdd = vnBefChildColSpan == 0 ? 1 : vnBefChildColSpan;
 						vnChildColIndex = ValueUtil.fixNumber(voBefChildRow.getAttr("_colIndex")) + vnAdd;
 					}
 					
 					voChildRow.setAttr("_colIndex", vnChildColIndex);
 					if (vnChildColIndexRange < (vnChildColIndex + ValueUtil.fixNumber(voChildRow.getValue("colSpan")))) {
 						break;
 					}
 				}
 			}
 			pnColIndex = poRow.getAttr("_colIndex");
 		} else {
 			poRow.setAttr("_colIndex", pnColIndex);
 		}
 		
 		if (vsPart == "header" && !(vsColumnValue == vsHdrIndexText || vsColumnValue == "선택" || vsColumnTypeCtrl == "checkbox")) {
 			vaAutoFit.push(pnColIndex);
 		}
 		var voConstraint = {
 			"rowIndex": vnRowIdx,
 			"colIndex": pnColIndex
 		};
 		if (vsMergedYn == "Y") { //셀 병합인 경우
 			//colSpan, rowSpan의 데이터가 비어 있는 경우 1로 설정
 			var vnColSpan = ValueUtil.fixNumber(poRow.getValue("colSpan")) == 0 ? 1 : poRow.getValue("colSpan");
 			var vnRowSpan = ValueUtil.fixNumber(poRow.getValue("rowSpan")) == 0 ? 1 : poRow.getValue("rowSpan");
 			
 			voConstraint["colSpan"] = vnColSpan;
 			voConstraint["rowSpan"] = vnRowSpan;
 			
 			//행 병합 사이즈와 행의 갯수가 동일 한 경우 colSpan 만큼 colIndex를 증가 시킨다.
 			if (vnColSpan > 1 && vnRowSpan == psGridpart.rows.length) {
 				pnColIndex += (vnColSpan - 1);
 				if (vsPart == "header" && !(vsColumnValue == vsHdrIndexText || vsColumnValue == "선택" || vsColumnTypeCtrl == "checkbox")) {
 					vaAutoFit.push(pnColIndex);
 				}
 			}
 		}
 		
 		return {
 			colIndex: pnColIndex,
 			constraint: voConstraint,
 			columnValue: vsColumnValue,
 			columnTypeCtrl: vsColumnTypeCtrl,
 			ctrlType: vsCtrlType
 		};
 	}
 	
 	/**
 	 * 컬럼 너비 배열 정보
 	 * @type {{width:String}[]}
 	 */
 	var vaColumns = [];
 	//헤더 컬럼의 첫번째 로우를 모두 반환 받아 최종 컬럼의 갯수 만큼 vaColumns에 너비 정보를 담는다.
 	var vaHeaderFirstRows = vcDynamicGridTemp.findAllRow("div == \"header\" && rowIdx == 0");
 	for (var i = 0; i < vaHeaderFirstRows.length; i++) {
 		var voHeaderFirstRow = vaHeaderFirstRows[i];
 		var vnColSpan = ValueUtil.fixNumber(voHeaderFirstRow.getValue("colSpan")) == 0 ? 1 : voHeaderFirstRow.getValue("colSpan");
 		
 		var vsColumnValue = voHeaderFirstRow.getValue("columnValue");
 		var vsColumnTypeCtrl = voHeaderFirstRow.getValue("columnTypeCtrl");
 		if ((vsColumnValue == vsHdrIndexText || vsColumnValue == "선택" || vsColumnTypeCtrl == "checkbox")) {
 			vaColumns.push({
 				"width": "50px"
 			});
 			continue;
 		}
 		
 		for (var j = i; j < (i + vnColSpan); j++) {
 			vaColumns.push({
 				"width": "100px"
 			});
 		}
 	}
 	//그리드 초기 정보에 컬럼 너비 배열 정보 추가
 	voGridInfo.columns = vaColumns;
 	
 	/**
 	 * 지정된 영역의 너비에 맞게 셀의 크기를 자동조절 할 컬럼 인덱스 배열
 	 * @type Array
 	 */
 	var vaAutoFit = [];
 	
 	//Header 영역 구성 정보를 모두 반환
 	var vaHeaderAllRows = vcDynamicGridTemp.findAllRow("div == \"header\"");
 	if (vaHeaderAllRows.length > 0) {
 		/**
 		 * @type {
 		 *   <!-- 헤더 영역 로우 정보 -->
 		 *   rows: cpr.controls.gridpart.GridRowConfig[],
 		 *   <!-- 헤더 영역 내부의 cell 구성 정보 -->
 		 *   cells: cpr.controls.gridpart.GridHeaderCellConfig[] 
 		 * }
 		 */
 		var voHeader = {
 			part: "header",
 			rows: [],
 			cells: []
 		};
 		var vnHeaderRowMaxCnt = vcDynamicGridTemp.getConditionalMax("div == \"header\"", "rowIdx").toNumber();
 		//rowIdex의 최대값 만큼 height 헤더 영역 로우 정보 설정
 		for (var i = 0; i <= vnHeaderRowMaxCnt; i++) {
 			voHeader.rows.push({
 				"height": "36px"
 			});
 		}
 		
 		//헤더 컬럼 인덱스 계산을 위한 변수
 		var vnHeaderColIndex = 0;
 		for (var i = 0; i < vaHeaderAllRows.length; i++) {
 			var voHeaderRow = vaHeaderAllRows[i];
 			var voRetVal = _configInfo(voHeader, voHeaderRow, vnHeaderColIndex);
 			
 			//헤더 구성 정보 설정(반복문 내부에서 헤더 컬럼 설정 정보를 구성 하므로 익명 함수로 감싸 전달 하는 파라미터를 보장) 
 			(function(poConstraint, psColumnValue, psColumnTypeCtrl) {
 				voHeader.cells.push({
 					constraint: poConstraint,
 					configurator: function(cell) {
 						cell.text = psColumnValue;
 						if (psColumnTypeCtrl == "checkbox") {
 							cell.columnType = "checkbox";
 						}
 					}
 				});
 			})(voRetVal.constraint, voRetVal.columnValue, voRetVal.columnTypeCtrl);
 			
 			//헤더 컬럼 인덱스 증가
 			vnHeaderColIndex = voRetVal.colIndex + 1;
 		}
 		//그리드 초기 정보에 헤더 영역 정보 추가
 		voGridInfo.header = voHeader;
 	}
 	
 	//디테일 영역 구성 정보를 모두 반환
 	var vaDetailAllRows = vcDynamicGridTemp.findAllRow("div == \"detail\"");
 	if (vaDetailAllRows.length > 0) {
 		/**
 		 * @type {
 		 *   <!-- 디테일 영역 로우 정보 -->
 		 *   rows: cpr.controls.gridpart.GridRowConfig[],
 		 *   <!-- 디테일 영역 내부의 cell 구성 정보 -->
 		 *   cells: cpr.controls.gridpart.GridDetailCellConfig[] 
 		 * }
 		 */
 		var voDetail = {
 			part: "detail",
 			rows: [],
 			cells: []
 		};
 		var vnDetailRowMaxCnt = vcDynamicGridTemp.getConditionalMax("div == \"detail\"", "rowIdx").toNumber();
 		//rowIdex의 최대값 만큼 height 헤더 영역 로우 정보 설정
 		for (var i = 0; i <= vnDetailRowMaxCnt; i++) {
 			voDetail.rows.push({
 				"height": "37px"
 			});
 		}
 		
 		//디테일 컬럼 인덱스 계산을 위한 변수
 		var vnDetailColIndex = 0;
 		for (var i = 0; i < vaDetailAllRows.length; i++) {
 			var voDetailRow = vaDetailAllRows[i];
 			var voRetVal = _configInfo(voDetail, voDetailRow, vnDetailColIndex);
 			
 			//디테일 구성 정보 설정(반복문 내부에서 디테일 컬럼 설정 정보를 구성 하므로 익명 함수로 감싸 전달 하는 파라미터를 보장) 
 			(function(poConstraint, psColumnValue, psColumnTypeCtrl, psCtrlType) {
 				voDetail.cells.push({
 					constraint: poConstraint,
 					configurator: function(cell) {
 						cell.columnName = psColumnValue;
 						//vsColumnTypeCtrl가 비어있거나 nomal이 아닌 경우 columnType 설정(default nomal)
 						if (!(ValueUtil.isNull(psColumnTypeCtrl) || psColumnTypeCtrl == "nomal")) {
 							cell.columnType = psColumnTypeCtrl;
 						}
 						
 						//컨트롤 타입값이 존재 하는 경우
 						if (!ValueUtil.isNull(psCtrlType)) {
 							var vsColmnNm = psColumnValue;
 							var vsCtrlTp = psCtrlType;
 							cell.control = (function(psColmnNm, psCtrlTp) {
 								var vcCtrl = null;
 								if (psCtrlTp == "inputbox") {
 									vcCtrl = new cpr.controls.InputBox();
 									//인풋박스 속성 추가가 필요 한 경우 설정
 									
 								} else if (psCtrlTp == "numbereditor") {
 									vcCtrl = new cpr.controls.NumberEditor();
 									//넘버에디터 속성 추가가 필요 한 경우 설정
 									
 								}
 								//추가 컨트롤 배치가 필요 한 경우 추가 로직 필요
 								
 								vcCtrl.bind("value").toDataColumn(psColmnNm);
 								return vcCtrl;
 							})(vsColmnNm, vsCtrlTp);
 						}
 					}
 				});
 			})(voRetVal.constraint, voRetVal.columnValue, voRetVal.columnTypeCtrl, voRetVal.ctrlType);
 			
 			//디테일 컬럼 인덱스 증가
 			vnDetailColIndex = voRetVal.colIndex + 1;
 		}
 		//그리드 초기 정보에 디테일 영역 정보 추가
 		voGridInfo.detail = voDetail;
 	}
 	
 	//푸터 영역 구성 정보를 모두 반환
 	var vaFooterAllRows = vcDynamicGridTemp.findAllRow("div == \"footer\"");
 	if (vaFooterAllRows.length > 0) {
 		/**
 		 * @type {
 		 *   <!-- 푸터 영역 로우 정보 -->
 		 *   rows: cpr.controls.gridpart.GridRowConfig[],
 		 *   <!-- 푸터 영역 내부의 cell 구성 정보 -->
 		 *   cells: cpr.controls.gridpart.GridFooterCellConfig[] 
 		 * }
 		 */
 		var voFooter = {
 			part: "footer",
 			rows: [],
 			cells: []
 		};
 		var vnFooterRowMaxCnt = vcDynamicGridTemp.getConditionalMax("div == \"footer\"", "rowIdx").toNumber();
 		//rowIdex의 최대값 만큼 height 헤더 영역 로우 정보 설정
 		for (var i = 0; i <= vnFooterRowMaxCnt; i++) {
 			voFooter.rows.push({
 				"height": "30px"
 			});
 		}
 		
 		//푸터 컬럼 인덱스 계산을 위한 변수
 		var vnFooterColIndex = 0;
 		for (var i = 0; i < vaFooterAllRows.length; i++) {
 			var voFooterRow = vaFooterAllRows[i];
 			var voRetVal = _configInfo(voFooter, voFooterRow, vnFooterColIndex);
 			
 			//디테일 구성 정보 설정(반복문 내부에서 디테일 컬럼 설정 정보를 구성 하므로 익명 함수로 감싸 전달 하는 파라미터를 보장) 
 			(function(poConstraint, psColumnValue, psCtrlType) {
 				voFooter.cells.push({
 					constraint: poConstraint,
 					configurator: function(cell) {
 						if (!ValueUtil.isNull(psColumnValue)) {
 							cell.expr = psColumnValue;
 						}
 						
 						//컨트롤 타입값이 존재 하는 경우
 						if (!ValueUtil.isNull(psCtrlType)) {
 							var vsColmnVal = psColumnValue;
 							var vsCtrlTp = psCtrlType;
 							//푸터 영역에 아웃풋 컨트롤 배치가 필요 한 경우 설정(그리드 구성 데이터셋에 format에 대한 컬럼과 데이터가 필요)
//							cell.control = (function(psColmnNm, psCtrlTp){
//								if(psCtrlTp == "output"){
//									var vcOutput = new cpr.controls.Output();
//									vcOutput.format = "";
//									return vcOutput;
//								}
//							})(vsColmnVal, vsCtrlTp);
 						}
 					}
 				});
 			})(voRetVal.constraint, voRetVal.columnValue, voRetVal.ctrlType);
 			
 			//푸터 컬럼 인덱스 증가
 			vnFooterColIndex = voRetVal.colIndex + 1;
 		}
 		//그리드 초기 정보에 푸터 영역 정보 추가
 		voGridInfo.footer = voFooter;
 	}
 	
 	//colIdex에 대한 AutoFit 값에 중복이 존재 하는 경우 중복 값 제외
 	var vaResult = vaAutoFit.filter(function(pnValue, idx) {
 		return vaAutoFit.indexOf(pnValue) === idx;
 	});
 	//그리드 초기 정보에 컬럼 자동 너비 정보 추가
 	voGridInfo.autoFit = vaResult.join(",");
 	
 	//그리드 초기화
 	vcGrid.init(voGridInfo);
 }
 
 
 /**
  * 해당 그리드의 행을 체크 또는 체크 해제한다.
  * @param {cpr.core.AppInstance} app 앱인스턴스
  * @param {#grid} psGridId  그리드 ID
  * @param {Number | Number[]} pnRowIndex 체크 또는 체크 해제하고자 하는 행 인덱스 또는 인덱스 배열
  * @param {Boolean} pbChecked? 체크 또는 체크 해제여부(default: true)
  * @return void
  */
 GridKit.prototype.setCheckRowIndex = function(app, psGridId, pnRowIndex, pbChecked) {
 	/** @type cpr.controls.Grid */
 	var vcGrid = app.lookup(psGridId);
 	
 	if (!(pnRowIndex instanceof Array)) {
 		pnRowIndex = [pnRowIndex];
 	}
 	
 	pbChecked = !ValueUtil.isNull(pbChecked) ? pbChecked : true;
 	for (var i = 0, len = pnRowIndex.length; i < len; i++) {
 		vcGrid.setCheckRowIndex(pnRowIndex[i], pbChecked);
 	}
 };


/**
 * Group컨트롤 유틸
 * @constructor
 * @param {common.AppKit} appKit
 */
function GroupKit(appKit) {
	this._appKit = appKit;
};

/**
 * 그룹 초기화시 공통 적용
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param  * @param {Array} paGrpId 그룹ID 배열
 */
GroupKit.prototype.init = function(app, paGrpId) {}

/**
 * 조회조건 및 작업영역 그룹 컨트롤 초기화<br><br>
 * 
 * <b><수행 로직></b><br>
 * 1. AppProperties.SEARCH_BTN_ID 설정에 따라 조회조건 변경시 작업영역 데이터 Clear (선택)<br>
 *   - 조회영역내 조회조건 컨트롤의 selection-change, value-change시 작업영역(paDisableCtl) disable 및 그리드, 프리폼 초기화<br>
 * 2. 조회조건 변경시 작업영역 데이터 변경시 알림(확인) 메시지 출력 <br>
 *  - udcComAppHeader 에서 공통 적용됨<br>
 * 3. 조회버튼의 ID는 'btnSearch'' 또는 value를 '조회''로 지정해야함<br>
 * 4. input 컨트롤(인풋박스,넘버에디터) 의 사용자 속성에 autoKeydownSearch = "Y" 지정시 keydown 이벤트 등록 (조회버튼 클릭)<br><br>
 * 
 * - 사이트별 Customizing 필요
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#container} psSearchBoxId 조회조건 영역 그룹컨트롤ID
 * @param {#container | Array} paDisableCtl 조회조건 변경시 비활성화 처리되는 영역의 그룹 컨트롤ID
 * @param {#uicontrol | Array} paExceptCtl? 적용 제외 컨트롤 ID
 * @param {Boolean} pbGrpDataDisable 앱로드시(초기) 작업 영역 비활성화 여부
 * @return void
 */
GroupKit.prototype.initSearchBox = function(app, psSearchBoxId, paDisableCtl, paExceptCtl, pbGrpDataDisable) {
	
	//비활성화 영역 컨트롤
	paDisableCtl = paDisableCtl != null ? paDisableCtl : new Array();
	if (!(paDisableCtl instanceof Array)) {
		paDisableCtl = [paDisableCtl];
	}
	
	//적용 제외 컨트롤 목록
	paExceptCtl = paExceptCtl != null ? paExceptCtl : new Array();
	if (!(paExceptCtl instanceof Array)) {
		paExceptCtl = [paExceptCtl];
	}
	
	var _app = app;
	var _appKit = this._appKit;
	
	var vsSchBtnId = "btnSearch";
	if(typeof AppProperties !== 'undefined' && AppProperties.SEARCH_BTN_ID){
		vsSchBtnId = AppProperties.SEARCH_BTN_ID;
	}	
	
	function doAddSearchBoxEvent(ctrl, psSearchBoxId, pbGrpDataDisable) {
		var vcSearchBox = app.lookup(psSearchBoxId);
		
		//조회버튼인 경우
		if (ctrl.type == "button" && (ctrl.id && ctrl.id.match(vsSchBtnId) || ctrl.value == "조회")) {
			paExceptCtl.push(ctrl.id);
			vcSearchBox.userAttr("__search_button__", ctrl.id); // 한 화면에 조회영역이 여러 개인 경우, 각 조회영역 그룹에 조회버튼id 설정 (autoKeydownSearch 기능을 위함)
			ctrl.addEventListener("click", function( /* cpr.events.CEvent */ e) {
				
				if (e.defaultPrevented === false && pbGrpDataDisable) {					
					doShadowView(_app, true, psSearchBoxId);
				}
			});
		} else {
			if (ctrl.type == "button" || ctrl.type == "output" || ctrl.type == "img" || ctrl.visible === false || ctrl.readOnly === true) return;
			
			/**
			 * 변경사항이 있는 경우
			 * 계속진행을 하시겠습니까? 에서 취소 선택시 업무단 value-change 이벤트가 호출되지 않게 하기 위해
			 * before 이벤트를 추가함.
			 */
			var bfEventType = (ctrl.type == "combobox" || ctrl.type == "radiobutton") ? "before-selection-change" : "before-value-change";
			ctrl.addEventListener(bfEventType, function(e) {
				if (_appKit.isAppModified(_app, "CRM", _app.getContainer())) {
					return false;
				} else {
					return true;
				}
				return true;
			});
			
			//인풋박스 컨트롤 Keydown 이벤트 추가
			if ((ctrl.type == "inputbox" || ctrl.type == "numbereditor") && ctrl.userAttr("autoKeydownSearch") == "Y") {
				ctrl.addEventListener("keydown", function( /* cpr.events.CKeyboardEvent */ e) {
					if (e.keyCode == cpr.events.KeyCode.ENTER) {
						var vsTargetSchButton = vcSearchBox.userAttr("__search_button__") || vsSchBtnId;
						//Enter키 입력시, 조회 버튼 클릭 이벤트 발생
						_appKit.Control.dispatchEvent(app, vsTargetSchButton, "click");
					}
				});
			}
		}
	}
	
	function doShadowView(app, pbEnable, psSearchBoxId) {
		if (paDisableCtl.length > 0) {
			if (vaSearchBoxIds.length > 1) {
				var vnIdx = vaSearchBoxIds.indexOf(psSearchBoxId);
				_appKit.Control.setEnable(app, pbEnable, paDisableCtl[vnIdx]);
			} else {
				_appKit.Control.setEnable(app, pbEnable, paDisableCtl);
			}
		}
	}
	
	var initFocus = false;
	
	function doFocusCtrl(poCtrl) {
		if (poCtrl.type == "button" || poCtrl.type == "output" || poCtrl.type == "img" || poCtrl.type == "container") return;
		
		if (!initFocus) {
			poCtrl.focus();
			initFocus = true;
		}
	}
	var vaSearchBoxIds = ValueUtil.split(psSearchBoxId, ",");
	for (var z = 0, zlen = vaSearchBoxIds.length; z < zlen; z++) {
		/** @type cpr.controls.Container */
		var vcSearchBox = app.lookup(vaSearchBoxIds[z]);
		if (vcSearchBox) {
			
			/* 조회조건 영역 초기화 제외 */ 
			if(vcSearchBox.userAttr("ignoreInit") === "Y") continue;
		
			var childCtrls = _appKit._getChildren(vcSearchBox);
			for (var i = 0, len = childCtrls.length; i < len; i++) {
				//udc컨트롤일 경우.
				if (childCtrls[i] instanceof cpr.controls.UDCBase) {
					var embApp = childCtrls[i].getEmbeddedAppInstance();
					embApp.getContainer().getChildren().some(function(ctrl) {
						if (ctrl instanceof cpr.controls.Container) {
							ctrl.getChildren().some(function(subCtrl) {
								doAddSearchBoxEvent(subCtrl, vaSearchBoxIds[z], pbGrpDataDisable);
								doFocusCtrl(subCtrl);
							});
						} else {
							doAddSearchBoxEvent(ctrl, vaSearchBoxIds[z], pbGrpDataDisable);
							doFocusCtrl(ctrl);
						}
					});
				} else {
					//이벤트 추가
					doAddSearchBoxEvent(childCtrls[i], vaSearchBoxIds[z], pbGrpDataDisable);
					//포커싱
					doFocusCtrl(childCtrls[i]);
				}
			}
			
			if (pbGrpDataDisable) {
				doShadowView(_app, false, vaSearchBoxIds[z]);
			}			
		}
	}
};

/**
 * 해당 그룹 컴포넌트 내의 DataColumn에 바인딩된 컨트롤 객체를 반환한다.<br>
 * 이는 프리폼 내의 DataColumn의 값을 갖는(바인딩) 컨트롤을 찾기 위해 사용된다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#container} psGrpId 그룹 ID
 * @param {#column} psDataColumnNm datacolumn 명
 * @return {Object} 컨트롤 객체
 */
GroupKit.prototype.getDataBindedControl = function(app, psGrpId, psDataColumnNm) {
	var vcFrf = app.lookup(psGrpId);
	var vaChild = vcFrf.getChildren();
	var vcBindCtrl = null;
	
	vaChild.some(function(ctrl, idx) {
		if (vcBindCtrl) return true;
		
		if (ctrl.type == "container") vcBindCtrl = this.getDataBindedControl(app, ctrl.id, psDataColumnNm);
		if (ctrl.type == "output") return false;
		var bind = ctrl.getBindInfo("value");
		if (bind && bind.type == "datacolumn" && psDataColumnNm === bind.columnName) {
			if (ctrl instanceof cpr.controls.UDCBase) {
				vcBindCtrl = AppUtil.getUDCBindValueControl(ctrl);
			} else {
				vcBindCtrl = ctrl;
			}
			
			return true;
		}
	});
	
	return vcBindCtrl;
};

/**
 * 그룹 또는 컨테이너 내의 특정 타입에 해당하는 자식 컨트롤을 취득한다.<br>
 * (사용처) 해당 화면내의 특정 유형의 컨트롤 목록을 얻고자 하는 경우에 사용
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {String} psCtlType 컨트롤 타입(ex: grid)
 * @param {cpr.controls.Container} poContainer 자식 컨트롤을 취득하고자 하는 부모 컨테이너 객체  	
 * @param {Boolean} pbRecursive? 자식 컨테이너를 Recusive하게 찾을건지 여부
 * @return {Array} 자식 컨트롤 객체 배열
 */
GroupKit.prototype.getAllChildrenByType = function(app, psCtlType, poContainer, pbRecursive) {
	var vaTypesChild = new Array();
	
	var container = app.getContainer();
	
	function getChildRecursive(psCtlType, poContainer) {
		var vaChildCtrls = poContainer ? (pbRecursive === true ? poContainer.getAllRecursiveChildren() : poContainer.getChildren()) : (pbRecursive === true ? container.getAllRecursiveChildren() : container.getChildren());
		for (var i = 0, len = vaChildCtrls.length; i < len; i++) {
			if (vaChildCtrls[i].type == psCtlType) {
				vaTypesChild.push(vaChildCtrls[i]);
			} else if (vaChildCtrls[i] instanceof cpr.controls.Container) {
				getChildRecursive(psCtlType, vaChildCtrls[i]);
			} else if (vaChildCtrls[i] instanceof cpr.controls.UDCBase) {
				var voUdcApp = vaChildCtrls[i].getEmbeddedAppInstance();
				if (voUdcApp) getChildRecursive(psCtlType, voUdcApp.getContainer());
			} else if (vaChildCtrls[i] instanceof cpr.controls.EmbeddedApp) {
				var voEmbApp = vaChildCtrls[i].getEmbeddedAppInstance();
				if (voEmbApp) getChildRecursive(psCtlType, voEmbApp.getContainer());
			}
		}
		vaChildCtrls = null;
	}
	
	getChildRecursive(psCtlType, poContainer);
	
	return vaTypesChild;
};

/**
 * 그룹 또는 컨테이너 내의 특정 ID를 갖는 자식 컨트롤을 취득한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {Array} paCtrlIds 컨트롤 ID 배열
 * @param {cpr.controls.Container} poContainer? 자식 컨트롤을 취득하고자 하는 부모 컨테이너 객체  	
 * @return {Array} 자식 컨트롤 객체 배열
 */
GroupKit.prototype.getControlByID = function(app, paCtrlIds, poContainer) {
	if (!(paCtrlIds instanceof Array)) {
		paCtrlIds = [paCtrlIds];
	}
	var vaChildCtrls = new Array();
	var container = poContainer ? poContainer : this._appKit.getMainApp(app).getContainer();
	
	function getChildRecursive(paCtrlIds, poContainer) {
		var childCtrls = poContainer.getAllRecursiveChildren();
		for (var i = 0, len = childCtrls.length; i < len; i++) {
			if (paCtrlIds.indexOf(childCtrls[i].id) != -1) {
				vaChildCtrls.push(childCtrls[i]);
			} else if (childCtrls[i] instanceof cpr.controls.UDCBase) {
				var voUdcApp = childCtrls[i].getEmbeddedAppInstance();
				if (voUdcApp) getChildRecursive(paCtrlIds, voUdcApp.getContainer());
			} else if (childCtrls[i] instanceof cpr.controls.EmbeddedApp) {
				var voEmbApp = childCtrls[i].getEmbeddedAppInstance();
				if (voEmbApp) getChildRecursive(paCtrlIds, voEmbApp.getContainer());
			}
		}
	}
	
	getChildRecursive(paCtrlIds, container);
	
	return vaChildCtrls;
};

/**
 * 그룹 컨트롤에 바인딩된 데이터셋을 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {cpr.controls.Container} poContainer 자식 컨트롤을 취득하고자 하는 부모 컨테이너 객체
 * @param {cpr.bind.BindContext} poBindContext? (Optional) 바인드 컨텍스트 
 * @return {cpr.data.DataSet} 바인딩된 데이터셋 객체
 */
GroupKit.prototype.getBindDataSet = function(app, poContainer, poBindContext) {
	/**@type cpr.data.DataSet */
	var voDataSet = null;
	/** @type cpr.bind.BindContext */
	var voBindContext = ValueUtil.isNull(poBindContext) ? this.getBindContext(app, poContainer) : poBindContext;
	if (voBindContext instanceof cpr.bind.GridSelectionContext) {
		voDataSet = voBindContext.grid.dataSet;
	} else if (voBindContext instanceof cpr.bind.DataRowContext) {
		voDataSet = voBindContext.dataSet;
	} else if (voBindContext instanceof cpr.bind.ItemSelectionContext) {
		voDataSet = voBindContext.control.dataSet;
	}
	
	return voDataSet;
};

/**
 * 그룹 컨트롤의 바인딩 문맥(Context) 객체를 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {cpr.controls.Container} poContainer 자식 컨트롤을 취득하고자 하는 부모 컨테이너 객체 
 * @return {cpr.bind.BindContext} 바인딩 Context 객체
 */
GroupKit.prototype.getBindContext = function(app, poContainer) {
	/** @type cpr.bind.BindContext */
	var voBindContext = poContainer.getBindContext();
	if (voBindContext == null || voBindContext == undefined) {
		var vaChildCtrls = this.getAllChildrenByType(app, "container", poContainer);
		vaChildCtrls.forEach(function( /* Object */ ctrl) {
			if (ctrl.getBindContext()) {
				voBindContext = ctrl.getBindContext();
				return true;
			}
		});
	}
	
	return voBindContext;
};

/**
 * 그룹 컨트롤에 바인딩된  컨트롤을 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {cpr.controls.Container} poContainer 자식 컨트롤을 취득하고자 하는 부모 컨테이너 객체 
 * @param {cpr.bind.BindContext} poBindContext? (Optional) 바인드 컨텍스트 
 * @return {cpr.controls.UIControl} 바인딩된 컨트롤 객체
 */
GroupKit.prototype.getBindControl = function(app, poContainer, poBindContext) {
	/**@type cpr.controls.UIControl */
	var voCtl = null;
	
	/** @type cpr.bind.BindContext */
	var voBindContext = ValueUtil.isNull(poBindContext) ? this.getBindContext(app, poContainer) : poBindContext;
	if (voBindContext instanceof cpr.bind.GridSelectionContext) {
		voCtl = voBindContext.grid;
	} else if (voBindContext instanceof cpr.bind.DataRowContext) {
		voCtl = voBindContext.dataSet;
	} else if (voBindContext instanceof cpr.bind.ItemSelectionContext) {
		voCtl = voBindContext.control;
	}
	
	return voCtl;
};

/**
 * 그룹 컨트롤에 바인딩된  컨트롤으 로우 인덱스를 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {cpr.bind.BindContext} poBindContex 바인드 컨텍스트 
 * @return {Number} 로우 인덱스
 */
GroupKit.prototype.getBindCtlRowIndex = function(app, poBindContext) {
	
	var vnRowIndex = 0;
	
	if (poBindContext == null) {
		return 0;
	}
	
	if (poBindContext instanceof cpr.bind.GridSelectionContext) {
		vnRowIndex = poBindContext.rowIndex;
	} else if (poBindContext instanceof cpr.bind.DataRowContext) {
		vnRowIndex = poBindContext.rowIndex;
	} else if (poBindContext instanceof cpr.bind.ItemSelectionContext) {
		vnRowIndex = poBindContext.control.getSelectedIndices()[0];
	}
	
	return vnRowIndex;
};
/**
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {cpr.controls.Container} poContainer 자식 컨트롤을 취득하고자 하는 부모 컨테이너 객체 
 */
GroupKit.prototype.setFloatGrp = function(app, poGroup) {
	var _app = app;
	
	for (var i = 0; i < poGroup.getChildrenCount(); i++) {
		var pogrpChid = poGroup.getChildren()[i];
		
		if (pogrpChid instanceof cpr.controls.Container) {
			pogrpChid.getLayout().topMargin = 10;
			pogrpChid.getLayout().bottomMargin = 10;
			pogrpChid.getLayout().spacing = 3;
		}
	}
	
	poGroup.style.css({
		"border-top": "1px solid #DFDFDF",
		"background-color": "#FFFFFF"
	})
	
	poGroup.visible = true;
	
	var rightSpacing, leftSpacing;
	var vsAppId = "app/com/Main";
	if(typeof AppProperties !== 'undefined' && AppProperties.MAIN_APP_ID){
		vsAppId = AppProperties.MAIN_APP_ID;
	}
	
	if (app.getHostAppInstance().app.id != vsAppId) {
		rightSpacing = "0px";
		leftSpacing = "0px";
	} else {
		rightSpacing = "11px";
		leftSpacing = "11px";
	}
	
	poGroup.style.css({
		position: "absolute",
		right: rightSpacing,
		left: leftSpacing,
		bottom: "0px",
		height: "54px"
	});
	
	_app.floatControl(poGroup);
};

/**
 * Layout의 Type명을 정의한다.
 * <pre><code>
 * util.Group.layoutClass["XYLayout"];
 * </code></pre>
 */
GroupKit.prototype.layoutClass = {
	"XYLayout": cpr.controls.layouts.XYLayout,
	"ResponsiveXYLayout": cpr.controls.layouts.ResponsiveXYLayout,
	"VerticalLayout": cpr.controls.layouts.VerticalLayout,
	"FlowLayout": cpr.controls.layouts.FlowLayout
};

/**
 * contaner 의 layout type을 반환한다.<br>
 * <pre><code>
 * util.Group.getLayoutType("grpId") == "XYLayout"
 * </code></pre> 
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#container} psContainer 그룹 ID
 * @return {String}  : [ XYLayout , ResponsiveXYLayout, VerticalLayout, FlowLayout, FlowLayout ] 
 */
GroupKit.prototype.getLayoutTypeString = function(app, psContainer) {
	var poContainer = app.lookup(psContainer);
	
	if (poContainer == null || poContainer == undefined) return "";
	
	var voLayout = poContainer.getLayout();
	
	var vLayoutNm = "XYLayout";
	
	if (voLayout instanceof cpr.controls.layouts.XYLayout) {
		vLayoutNm = "XYLayout";
	} else if (voLayout instanceof cpr.controls.layouts.ResponsiveXYLayout) {
		vLayoutNm = "ResponsiveXYLayout";
	} else if (voLayout instanceof cpr.controls.layouts.VerticalLayout) {
		vLayoutNm = "VerticalLayout";
	} else if (voLayout instanceof cpr.controls.layouts.FlowLayout) {
		vLayoutNm = "FlowLayout";
	}
	return vLayoutNm;
	
};

/**
 * contaner 의 layout type을 반환한다.
 * <pre><code>
 * util.Group.getLayoutType("grpId") instanceof cpr.controls.layouts.XYLayout
 * </code></pre> 
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#container} psContainer 그룹 ID
 * @return {cpr.controls.layouts}  : cpr.controls.layouts.XYLayout , cpr.controls.layouts.ResponsiveXYLayout, cpr.controls.layouts.VerticalLayout, cpr.controls.layouts.FlowLayout, cpr.controls.layouts.FlowLayout
 */
GroupKit.prototype.getLayoutType = function(app, psContainer) {
	
	var vLayoutNm = this.getLayoutTypeString(app, psContainer);
	
	return this.layoutClass[vLayoutNm];
	
};

/**
 * 그룹에 속해있는 컨트롤의 데이터를 초기화 한다.
 * @param {any} app 앱인스턴스
 * @param {#Container} psGrpId 그룹 ID
 */
GroupKit.prototype.clear = function(app, psGrpId) {
	var _app = app;
	var targetGrp = _app.lookup(psGrpId);
	var _appkit_ = this._appKit;
	var bindContext = _appkit_.Group.getBindContext(_app, targetGrp);
	
	if (bindContext) {
		if (bindContext.grid) {
			this._appKit.FreeForm.revertRow(_app, psGrpId);
		} else if (bindContext.dataSet) {
			var voDs = bindContext.dataSet;
			
			var vnRowIndex = bindContext.rowIndex;
			//데이터 Revert
			var rowData = voDs.getRow(vnRowIndex).getRowData();
			var vsGridRowState = voDs.getRowState(vnRowIndex);
			for (var column in rowData) {
				voDs.setValue(vnRowIndex, column, voDs.getOriginalValue(vnRowIndex, column));
			}

			if (vsGridRowState == cpr.data.tabledata.RowState.INSERTED) {
				voDs.setRowState(vnRowIndex, vsGridRowState);
			}
			
			_appkit_.Control.redraw(_app, targetGrp.id);
		} else if (bindContext.dataMap) {
			
			var voDm = bindContext.dataMap;
			
			var colNms = voDm.getColumnNames();
			
			colNms.forEach(function(each) {
				voDm.setValue(each, voDm.getOriginalValue(each));
			});
			
			_appkit_.Control.redraw(_app, targetGrp.id);
		} else {
			var targetGrp = _app.lookup(psGrpId);
			
			targetGrp.getAllRecursiveChildren().forEach(function(each) {
				if (each.type == "output" || each.type == "button") return false;
				
				each.value = "";
			});
		}
	} else {
		var targetGrp = _app.lookup(psGrpId);
		
		targetGrp.getAllRecursiveChildren().forEach(function(each) {
			if (each instanceof cpr.controls.Container) {
				each.getAllRecursiveChildren().forEach(function(child) {
					if (child.type == "output" || child.type == "button") return false;
					
					child.value = "";
				});
			} else if (each instanceof cpr.controls.UDCBase) {
				var embApp = each.getEmbeddedAppInstance();
				var children = embApp.getContainer().getAllRecursiveChildren();
				
				children.forEach(function(udcChild) {
					if (udcChild instanceof cpr.controls.Container) {
						_appkit_.Group.clear(embApp, udcChild.id);
					} else {
						if (udcChild.type == "output" || udcChild.type == "button") return false;
						
						udcChild.value = "";
					}
				});
			} else {
				if (each.type == "output" || each.type == "button") return false;
				
				each.value = "";
			}
			
		});
	}
}

/**
 * pc/mobile 화면보기 전환
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {"pc" | "mobile"} screenName 스크린명
 */
GroupKit.prototype.changeScreen = function(app, screenName) {
	
	var that = this;
	var vsMobileScreenNm = "mobile";
	if(typeof AppProperties !== 'undefined' && AppProperties.SCREEN_MOBILE_NM){		
		vsMobileScreenNm = AppProperties.SCREEN_MOBILE_NM;
	}
	
	var actions = {
		pc: function() {
			app.getContainer().style.css("min-width", "1340px");
			that.getAllChildrenByType(app, "container", app.getContainer(), true).forEach(function(each) {
				if (each._RForm) each._RForm._restore();
			});			
		},
		mobile: function() {
			app.getContainer().style.css("min-width", "none");
			that.getAllChildrenByType(app, "container", app.getContainer(), true).forEach(function(each) {
				if (each._RForm) {
					each._RForm._transform(each._RForm._columnSettings[vsMobileScreenNm]);
				}
			})			
		}
	}
	actions[screenName]();
}

/**
 * 그리드에 바인딩된 프리폼내의 컨트롤에 포커싱 한다.
 * @private
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#container} psGrpId 그룹ID
 * @param {#column} psFocusColumnNm datacolumn명
 */
GroupKit.prototype.focusColumnForGridToForm = function(app, psGrpId, psFocusColumnNm) {
	var vaBindFormCtrls = app.getContainer().getAllRecursiveChildren(false).filter(function(child) {
		return child.type == "container" && child.getBindContext().grid != null;
	});
	
	var _this = this;
	if (vaBindFormCtrls != null && vaBindFormCtrls.length > 0) {
		for (var i = 0, len = vaBindFormCtrls.length; i < len; i++) {
			if (vaBindFormCtrls[i].getBindContext().grid.id == psGrpId) {
				var vcFocusCtrl = _this.getDataBindedControl(app, vaBindFormCtrls[0].id, psFocusColumnNm);
				if (vcFocusCtrl) app.focus(vcFocusCtrl);
				break;
			}
		}
	}
};

/**
 * 메인 MDI 컨트롤 유틸
 * @constructor
 * @param {common.AppKit} appKit
 */
function MDIKit(appKit) {
	this._appKit = appKit;
};

/**
 * close 메인 MDI의 탭으로 화면을 오픈한다.<br>
 * - Root App에 해당 함수 필요(doOpenMenuToMdi)<br>
 * - 사이트별 Customizing 필요
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#mdifolder} psMenuId 메뉴 ID
 */
MDIKit.prototype.open = function(app, psMenuId, poParam) {
	if (app.getRootAppInstance().hasAppMethod("doOpenMenuToMdi")) {
		app.getRootAppInstance().callAppMethod("doOpenMenuToMdi", psMenuId, poParam);
	}
};

/**
 * Msg(메시지) 유틸<br>
 * language.json 참조
 * @constructor
 * @param {common.module} appKit
 */
function MsgKit(appKit) {
	this._appKit = appKit;
};

/**
 * 메시지 ID에 해당되는 메시지를 반환한다.
 * @param {String} psMsgId  메시지 ID
 * @param {String | Array} paArgs 메시지 내용 중 @로 표시된 부분 넣어줄 데이터 배열
 * @return {String} 메시지 문자열
 */
MsgKit.prototype.getMsg = function(psMsgId, paArgs) {
	if (psMsgId == null || psMsgId == "") return "";
	var psMsg = cpr.I18N.INSTANCE.message("NLS-" + psMsgId);
	if (psMsg == null || psMsg.indexOf("NLS-") >= 0) {
		return psMsgId.replace(/\\n/gi, "\n");
	}
	if (!ValueUtil.isNull(paArgs)) {
		if (!(paArgs instanceof Array)) {
			paArgs = [paArgs];
		}
		//정규 표현식 사용하여 동적 메시지 치환
		var regExp = psMsg.match(/\{[0-9]+\}/ig);
		regExp.forEach(function( /* String */ exp) {
			var idx = ValueUtil.fixNumber(exp.replace("{", "").replace("}", "").trim());
			psMsg = psMsg.replace(exp, new String(paArgs[idx]).replace(/\r\n/ig, ""));
		});
	}
	
	return psMsg.replace(/\\n/ig, "\n");
};

/**
 * 메시지 구독대상에게 알림처리 한다.<br>
 * (main.clx의 cpr.core.NotificationCenter.INSTANCE.subscribe()
 * <pre><code>
 * Msg.notify(app, "CMN-M001");
 * </code></pre>
 * @param {cpr.core.AppInstance} app 앱인스턴스 객체
 * @param {String} psMsgId 메시지 ID
 * @param {String | Array} paArgs? 메시지 내용 중 @로 표시된 부분 넣어줄 데이터 배열
 * @param {"INFO" | "WARNING" | "DANGER"} psMsgType? 출력타입 (default : INFO)
 * @param {Number} pnDelay? notify 사용시 알림창이 표시되는 시간 설정
 * @param {Boolean} pbKeep? 이전 메시지를 유지시킬지 여부 
 * @return void
 */
MsgKit.prototype.notify = function(app, psMsgId, paArgs, psMsgType, pnDelay) {
	
	var voMsgInfo = {};
	voMsgInfo.TYPE = psMsgType;
	voMsgInfo.MSG = this.getMsg(psMsgId, paArgs);
	if (pnDelay != null) {
		voMsgInfo.DELAY = pnDelay;
	}
	
	var vsMsgTopicId = "app-msg";
	if(typeof AppProperties !== 'undefined' && AppProperties.MSG_TOPIC_ID){
		vsMsgTopicId = AppProperties.MSG_TOPIC_ID;
	}
	
	cpr.core.NotificationCenter.INSTANCE.post(vsMsgTopicId, voMsgInfo);
};

/**
 * 브라우저의 시스템 confirm 알림창으로 메시지 출력.
 * <pre><code>
 * Msg.confirm("CRM-M001");<br>
 * <p>또는</p>
 * Msg.confirm("CRM-M016", ["선택된 파일"]);
 * </code></pre>
 * @param {String} psMsgId 메시지 ID
 * @param {String | Array} paArgs? 메시지 내용 중 @로 표시된 부분 넣어줄 데이터 배열
 * @return {Boolean} Confirm 창의 확인 결과
 */
MsgKit.prototype.confirm = function(psMsgId, paArgs) {
	return confirm(this.getMsg(psMsgId, paArgs));
};

/**
 * 다이얼로그 confirm 알림창으로 메시지 출력.<br>
 * <pre><code>
 * util.Msg.confirmDlg(app, "메시지 또는 메시지 코드", "@파라미터", { 
 *	confirmCallback: function(){
 *	 // 확인 버튼 클릭 후 동작
 *	},
 *	cancelCallback: function(){
 *	 // 취소(닫기) 버튼 클릭 후 동작
 *	},
 *  confirmBtnText : "확인 버튼명",
 *  cancelBtnText : "취소 버튼명"
 * });
 * </code></pre>
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {String} psMsgId 메세지 ID
 * @param {Array} paArgs? 메세지 내용 중 @로 표시된 부분에 넣을 데이터 배열
 * @param {{confirmCallback? : Function <!-- 확인 Callback -->,
 * cancelCallback? : Function <!-- 취소(닫기) Callback -->,
 * confirmBtnText : String <!-- 확인 버튼 텍스트 -->,
 * cancelBtnText : String<!-- 취소(닫기) 버튼 텍스트  -->,
 * msgStateType : "INFO" | "SUCCESS" | "WARNING" | "DANGER"<!-- 메세지  타입(메세지 구분시 활용) -->
 * }} poOptions  callback 처리, 버튼명 등 옵션 구성
 */
MsgKit.prototype.confirmDlg = function(app, psMsgId, paArgs, poOptions) {
	return this._openMsgDialog(app, "confirm", psMsgId, paArgs, poOptions);
};

/**
 * 브라우저의 시스템 alert 알림창으로 메시지 출력.
 * <pre><code>
 * Msg.alert("CMN-M001");<br>
 * <p>또는</p>
 * Msg.alert("CRM-M016", ["선택된 파일"]);
 * </code></pre>
 * @param {String} psMsgId 메시지 ID
 * @param {String | Array} paArgs? 메시지 내용 중 @로 표시된 부분 넣어줄 데이터 배열
 */
MsgKit.prototype.alert = function(psMsgId, paArgs) {
	alert(this.getMsg(psMsgId, paArgs));
};

/**
 * 다이얼로그 alert 알림창으로 메시지 출력.
 * <pre><code>
 * util.Msg.alertDlg(app, "메시지 또는 메시지 코드", "@파라미터", { 
 *	confirmCallback: function(){
 *	 // 확인 버튼 클릭 후 동작
 *	}, 
 *  confirmBtnText : "확인 버튼명" 
 * });
 * </code></pre>
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {String} psMsgId 메세지 ID
 * @param {Array} paArgs? 메세지 내용 중 @로 표시된 부분에 넣을 데이터 배열
 * @param {{confirmCallback? : Function <!-- 확인(닫기) Callback -->, 
 * confirmBtnText : String<!-- 확인(닫기) 버튼 텍스트  -->,
 * msgStateType : "INFO" | "SUCCESS" | "WARNING" | "DANGER"<!-- 메세지  타입(메세지 구분시 활용) -->
 * }} poOptions callback 처리, 버튼명 등 옵션 구성
 */
MsgKit.prototype.alertDlg = function(app, psMsgId, paArgs, poOptions) {
	return this._openMsgDialog(app, "alert", psMsgId, paArgs, poOptions);
};

/**
 * 공통 메세지 다이얼로그를 호출하는 함수입니다.
 * @private
 * @param {cpr.core.AppInstance} app
 * @param {"confirm"|"alert"} psMsgType
 * @param {String} psMsgId
 * @param {Array} paArgs
 * @param {{confirmCallback? : Function <!-- 확인 Callback -->,
 * cancelCallback? : Function <!-- 취소(닫기) Callback -->,
 * confirmBtnText : String <!-- 확인 버튼 텍스트 -->,
 * cancelBtnText : String <!-- 취소(닫기) 버튼 텍스트  -->,
 * msgStateType : "INFO" | "SUCCESS" | "WARNING" | "DANGER" <!-- 메세지  타입(메세지 구분시 활용) -->
 *   }} poOptions callback 처리, 버튼명 등 옵션 구성
 */
MsgKit.prototype._openMsgDialog = function(app, psMsgType, psMsgId, paArgs, poOptions) {
	var voCloneOption = {};
	
	var hasOption = function(psParamName) {
		if (ValueUtil.fixNull(poOptions) != "" && poOptions[psParamName]) {
			return true;
		} else {
			return false;
		}
	}
	if (ValueUtil.fixNull(poOptions) != "") {
		voCloneOption = _.clone(poOptions);
	}
	delete voCloneOption["confirmCallback"];
	delete voCloneOption["cancelCallback"];
	voCloneOption["msg"] = this.getMsg(psMsgId, paArgs);
	voCloneOption["msgType"] = psMsgType;
	/* 커스텀 옵션 : 메세지 다이얼로그를 컨텐츠 크기를 고정크기로 사용할 경우 활용, 자동크기(-1)를 사용할 경우 메세지 다이얼로그의 루트 레이아웃을 버티컬 레이아웃으로 구성해야 합니다.*/
	// return this._appKit.Dialog.open(app, "app/com/comPMsgDialog", 400, 200, function(evt) {
	return this._appKit.Dialog.open(app, "app/com/comPMsgDialog", 400, -1, function(evt) {
		/** @type cpr.controls.Dialog */
		var vcDialog = evt.control;
		var voReturnValue = vcDialog.returnValue;
		
		if (ValueUtil.fixNull(voReturnValue) != "") {
			if (hasOption("confirmCallback") && _.isFunction(poOptions["confirmCallback"])) {
				poOptions["confirmCallback"].call(null, voReturnValue);
			}
		} else {
			if (hasOption("cancelCallback") && _.isFunction(poOptions["cancelCallback"])) {
				poOptions["cancelCallback"].call(null);
			}
		}
	}, voCloneOption);
}

/**
 * 리스트 형태 컨트롤 유틸
 * @constructor
 * @param {common.AppKit} appKit
 */
function SelectKit(appKit) {
	this._appKit = appKit;
};

/**
 * 입력한 index의 위치에 새로운 item을 추가한다.
 * <pre><code>
 * SelectCtl.addItem(app, "cmb1", "라벨1", "값1");<br>
 * <p>또는</p>
 * SelectCtl.addItem(app, "cmb1", "라벨1", "값1", 0);
 * </code></pre>
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID<br>(only Combo, List, Radio, CheckBox Group)
 * @param {String} psLabel 추가할 item의 label
 * @param {String} psValue 추가할 item의 value
 * @param {Number} pnIndex? 추가할 item의 index (default는 마지막 행 뒤에 추가됨)
 * @return void 
 */
SelectKit.prototype.addItem = function(app, psCtlId, psLabel, psValue, pnIndex) {
	/** @type cpr.controls.ComboBox */
	var vcCtl = app.lookup(psCtlId);
	var item;
	
	if (ValueUtil.isNull(pnIndex)) {
		vcCtl.addItem(new cpr.controls.Item(psLabel, psValue));
	} else {
		if (pnIndex >= 0 && pnIndex <= vcCtl.getItemCount()) {
			if (pnIndex == 0) {
				item = vcCtl.getItem(pnIndex);
				vcCtl.insertItemBefore(new cpr.controls.Item(psLabel, psValue), item);
			} else {
				item = vcCtl.getItem(pnIndex - 1);
				vcCtl.insertItemAfter(new cpr.controls.Item(psLabel, psValue), item);
			}
		}
	}
};

/**
 * 지정한 인덱스(Index)의 아이템 라벨(label)을 반환한다.<br>
 * multiple "true"의 경우 index에 해당하는 여러 라벨값을 알고자 할 때, pnIndex는 구분자를 기준으로 조인된 String 형태를 가진다.
 * <pre><code>
 * SelectCtl.getItemLabel(app, "cmb1");<br>
 * <p>또는</p>
 * SelectCtl.getItemLabel(app, "cmb1", "1,2,3");
 * </code></pre>
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID
 * @return {String | Array}	multiple : true 일 경우 Array(String)<br>
 * 							multiple : false 일 경우 String
 * @param {Number} pnIndex? 인덱스 번호
 * @return {String | Array}	multiple : true 일 경우 Array(String)<br>
 * 							multiple : false 일 경우 String
 */
SelectKit.prototype.getItemLabel = function(app, psCtlId, pnIndex) {
	var vcCtl = app.lookup(psCtlId);
	if (ValueUtil.isNull(pnIndex)) {
		var item = vcCtl.getSelectionFirst();
		return item ? item.label : "";
	} else {
		if (vcCtl.multiple) { //다중 선택 가능한 경우 라벨 배열 반환
			var vaIdx = ValueUtil.split(pnIndex, ",");
			for (var i = 0, len = vaIdx.length; i < len; i++) {
				vaIdx[i] = vcCtl.getItem(vaIdx[i]).label;
			}
			return vaIdx;
		} else {
			return vcCtl.getItem(pnIndex).label;
		}
	}
};

/**
 * 지정한 인덱스(Index)의 아이템 값(value)을 반환한다.<br>
 * multiple "true"의 경우 index에 해당하는 여러 value 값을 알고자 할 때,<br>
 * pnIndex는 구분자를 기준으로 조인된 String 형태를 가진다.
 * <pre><code>
 * SelectCtl.getItemValue(app, "cmb1");<br>
 * <p>또는</p>
 * SelectCtl.getItemValue(app, "cmb1", "1,2,3");
 * </code></pre>
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID
 * @param {Number} pnIndex? 인덱스 번호
 * @return {String | Array}	multiple : true 일 경우 Array(String)<br>
 * 							multiple : false 일 경우 String
 */
SelectKit.prototype.getItemValue = function(app, psCtlId, pnIndex) {
	/**@type cpr.controls.ComboBox */
	var vcCtl = app.lookup(psCtlId);
	if (ValueUtil.isNull(pnIndex)) {
		var item = vcCtl.getSelectionFirst();
		return item ? item.value : "";
	} else {
		if (vcCtl.multiple) { //다중 선택 가능한 경우 값 배열 반환
			var vaIdx = ValueUtil.split(pnIndex, ",");
			for (var i = 0, len = vaIdx.length; i < len; i++) {
				vaIdx[i] = vcCtl.getItem(vaIdx[i]).value;
			}
			return vaIdx;
		} else {
			return vcCtl.getItem(pnIndex).value;
		}
	}
};

/**
 * 지정한 인덱스(Index)의 아이템 값(value)을 반환한다.<br>
 * multiple "true"의 경우 index에 해당하는 여러 value 값을 알고자 할 때,<br>
 * pnIndex는 구분자를 기준으로 조인된 String 형태를 가진다.
 * <pre><code>
 * SelectCtl.getItemValue(app, "cmb1", "1,2,3");
 * </code></pre>
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID
 * @param {Number} pnIndex? 인덱스 번호
 * @return {String | Array}	multiple : true 일 경우 Array(String)<br>
 * 							multiple : false 일 경우 String
 */
SelectKit.prototype.getValue = function(app, psCtlId, pnIndex) {
	/**@type cpr.controls.ComboBox */
	var ctrl = app.lookup(psCtlId);
	if (ValueUtil.isNull(pnIndex)) {
		var item = ctrl.getSelectionFirst();
		return item ? item.value : "";
	} else {
		if (ctrl.multiple) { //다중 선택 가능한 경우 값 배열 반환
			var vaIdx = ValueUtil.split(pnIndex, ",");
			for (var i = 0, len = vaIdx.length; i < len; i++) {
				vaIdx[i] = ctrl.getItem(vaIdx[i]).value;
			}
			return vaIdx;
		} else {
			return ctrl.getItem(pnIndex).value;
		}
	}
};

/**
 * 컨트롤의 값을 셋팅한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤ID
 * @param {String} psValue  컨트롤값
 * @param {boolean} pbEmitEvent? value-changed 이벤트 발생시킬지 여부
 */
SelectKit.prototype.setValue = function(app, psCtlId, psValue, pbEmitEvent) {
	/**@type cpr.controls.ComboBox */
	var ctrl = app.lookup(psCtlId);
	if (pbEmitEvent != undefined && pbEmitEvent === false) {
		ctrl.putValue(psValue);
	} else {
		ctrl.value = psValue;
	}
};

/**
 * 현재 선택 중인 아이템의 index를 반환한다.<br>
 * multiple "true"의 경우, index는 배열의 형태로 반환된다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID
 * @return {Number | Array}	multiple : true 일 경우 Array(Number)<br>
 * 							multiple : false 일 경우 Number 		
 */
SelectKit.prototype.getSelectedIndex = function(app, psCtlId) {
	/** @type cpr.controls.ComboBox */
	var vcCtl = app.lookup(psCtlId);
	var vaItems = vcCtl.getSelection();
	if (vcCtl.multiple) {
		var vaIndices = new Array();
		for (var i = 0, len = vaItems.length; i < len; i++) {
			vaIndices.push(vcCtl.getIndex(vaItems[i]));
		}
		return vaIndices;
	} else {
		return vcCtl.getIndex(vaItems[0]);
	}
};

/**
 * 인덱스(Index) 또는 value에 해당하는 아이템(Item)을 선택한다.<br>
 * multiple "true"의 경우 여러 개의 아이템을 선택하고자 할 때,<br>
 * puRowIdx는 구분자를 기준으로 조인된 String 형태를 가진다.
 * <pre><code>
 * SelectCtl.selectItem(app, "cmb1", "0");<br>
 * <p>또는<p>
 * SelectCtl.selectItem(app, "cmb1", "값1,값2,값3");
 * </code></pre>
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID
 * @param {String | Array} puRowIdx 인덱스 또는 value 값
 * @param {Boolean} emitEvent? 이벤트(before-selection-change, selection-change)를 발생시킬지 여부
 * @return {Boolean} 
 */
SelectKit.prototype.selectItem = function(app, psCtlId, puRowIdx, emitEvent) {
	/**@type cpr.controls.CheckBoxGroup */
	var vcCtl = app.lookup(psCtlId);
	
	if (vcCtl == null || vcCtl == undefined) return false;
	
	puRowIdx = ValueUtil.split(puRowIdx, ",");
	if (vcCtl.multiple) { //다중 선택 가능한 경우
		if (puRowIdx.length > 0) {
			if (!ValueUtil.isNumber(puRowIdx[0])) {
				for (var i = 0, len = puRowIdx.length; i < len; i++) {
					puRowIdx[i] = vcCtl.getIndexByValue(puRowIdx[i]);
				}
			}
			vcCtl.selectItems(puRowIdx, emitEvent);
		}
	} else {
		if (puRowIdx.length > 0) {
			if (!ValueUtil.isNumber(puRowIdx[0])) {
				var item = vcCtl.getItemByValue(puRowIdx[0]);
				if (item) vcCtl.selectItemByValue(puRowIdx[0], emitEvent);
				else vcCtl.selectItem(0, emitEvent);
			} else {
				if (Number(puRowIdx[0]) >= vcCtl.getItemCount()) {
					vcCtl.selectItem(0, emitEvent);
				} else {
					vcCtl.selectItem(puRowIdx[0], emitEvent);
				}
			}
		}
	}
	
	return true;
};

/**
 * 모든 아이템을 선택한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤ID
 * @return void
 */
SelectKit.prototype.selectAllItem = function(app, psCtlId) {
	/** @type cpr.controls.ComboBox */
	var vcCtl = app.lookup(psCtlId);
	var indices = new Array();
	for (var i = 0, len = vcCtl.getItemCount(); i < len; i++) {
		indices.push(i);
	}
	vcCtl.selectItems(indices);
};

/**
 * 해당 컨트롤 아이템을 필터링 한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID
 * @param {String} psCondition 필터 조건
 * @return void
 */
SelectKit.prototype.setFilter = function(app, psCtlId, psCondition) {
	/** @type cpr.controls.ComboBox */
	var ctrl = app.lookup(psCtlId);
	ctrl.setFilter("value == null || value == '' || (" + psCondition + ")");
};

/**
 * 컨트롤의 필터링을 해제한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID
 * @return void
 */
SelectKit.prototype.clearFilter = function(app, psCtlId) {
	/** @type cpr.controls.ComboBox */
	var ctrl = app.lookup(psCtlId);
	ctrl.clearFilter();
};

/**
 * 콤보박스의 값을 Reset한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID
 */
SelectKit.prototype.reset = function(app, psCtlId) {
	/** @type cpr.controls.ComboBox */
	var vcCtl = app.lookup(psCtlId);
	if (vcCtl.dataSet) {
		vcCtl.dataSet.clear();
	}
	vcCtl.value = "";
};

/**
 * 필터링 할 컬럼명(psFilterColumnName)은 데이터셋의 컬럼명을 작성한다.<br>
 * 그리드에서 사용 금지
 * @desc 두 개의 List형 컨트롤이 종속 관계를 가질 때, 종속되는 컨트롤의 데이터를 필터링하기 위한 메소드
 * @param {#uicontrol} psMainId 메인 컨트롤 ID
 * @param {#uicontrol} psSubId 적용될 컨트롤 ID
 * @param {#column} psFilterColumnName 적용될 컨트롤의 필터링 할 컬럼명
 * @param {Boolean} pbFirstItemSelect? 첫번째 아이템 선택 여부  default : true
 * @return void
 */
SelectKit.prototype.cascadeList = function(app, psMainId, psSubId, psFilterColumnName, pbFirstItemSelect) {
	var voMainCtl = app.lookup(psMainId);
	var voSubCtl = app.lookup(psSubId);
	
	if (voMainCtl == null || voSubCtl == null) {
		return;
	}
	pbFirstItemSelect = pbFirstItemSelect == null ? true : pbFirstItemSelect;
	
	var vaItems = voMainCtl.getSelection();
	var vsValue = "";
	if (vaItems.length > 0) {
		vsValue = vaItems[0].value;
	}
	
	voSubCtl.clearFilter();
	
	var voFirstItem = voSubCtl.getItem(0);
	var vsFirstItemValue = voFirstItem.value;
	var vsFirstItemLable = voFirstItem.label;
	
	//'전체' 아이템 여부
	var vbAllStatus = false;
	/* 커스텀 옵션 : 셀렉트 계열 컨트롤의 선행 데이터에 구성된 '전체' 아이템의 메세지 코드*/
	//var vsGlsAll = cpr.I18N.INSTANCE.message("UI-GLS-ALL");
	var vsGlsAll = "전체";
	
	if (vsGlsAll == vsFirstItemLable && (ValueUtil.isNull(vsFirstItemValue) || vsFirstItemValue.indexOf("%") != -1)) {
		vbAllStatus = true;
	}
	
	//전체아이템이 포함됐을 경우
	if (vbAllStatus) {
		var vsFilter = psFilterColumnName + "== '" + vsValue + "' || ( label == '" + vsGlsAll + "' && (value == '' || value == '%'))";
		voSubCtl.setFilter(vsFilter);
		if (pbFirstItemSelect)
			this.selectItem(app, psSubId, 0);
	} else {
		voSubCtl.setFilter(psFilterColumnName + "== '" + vsValue + "'");
		if (pbFirstItemSelect) {
			var vaSubCtlItems = voSubCtl.getItems();
			if (vaSubCtlItems.length > 0) {
				this.selectItem(app, psSubId, vaSubCtlItems[0].value);
			} else {
				this.selectItem(app, psSubId, 0);
			}
		}
		
	}
	
	voSubCtl.redraw();
};

/**
 * 콤보박스에 매핑된 데이터셋의 컴럼값을 입력조건에 따라 가져온다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID
 * @param {String} psColName 찾을 컬럼명
 * @param {String} condition 조건 (예: "CD == 'test'")
 * @return {String} 컬럼값
 */
SelectKit.prototype.findValue = function(app, psCtlId, psColName, condition) {
	/** @type cpr.controls.ComboBox */
	var ctrl = app.lookup(psCtlId);
	if (ctrl.dataSet) {
		var findRow = ctrl.dataSet.findFirstRow(condition);
		if (findRow) {
			return findRow.getValue(psColName);
		}
	}
};

/**
 * 지정한 인덱스(Index)의 아이템 기존 값(value)을 반환한다.<br/>
 * multiple=true 의 경우 index에 해당하는 여러 value 값을 알고자 할 때, pnIndex는 구분자를 기준으로 조인된 String 형태를 가진다.
 * <pre><code>
 * util.SelectCtl.getOtherItemValue(app, "cmb1", "columnNm", "1,2,3");
 * </code></pre>
 * @param {cpr.core.AppInstance} 	app 앱 인스턴스
 * @param {#uicontrol} psCtlId 컨트롤 ID
 * @param {#column} psColumnNm 기존 값을 확인 할 컬럼명
 * @param {Number} pnIndex? 인덱스 번호
 * @return {String | Array} multiple : true 일 경우 Array(String) <br/>
 * 									multiple : false 일 경우 String
 */
SelectKit.prototype.getOtherItemValue = function(app, psCtlId, psColumnNm, pnIndex) {
	/**@type cpr.controls.ComboBox */
	var vcCtrl = app.lookup(psCtlId);
	/**@type cpr.data.DataSet */
	var vcDataset = vcCtrl.dataSet;
	var originVal;
	
	if (ValueUtil.isNull(pnIndex)) {
		var voItem = vcCtrl.getSelectionFirst();
		
		if (voItem) {
			var vnIndex = vcCtrl.getDataSetIndexByValue(voItem.value);
			originVal = vcDataset.getValue(vnIndex, psColumnNm);
		} else {
			originVal = "";
		}
		
		return originVal;
	} else {
		if (vcCtrl.multiple) { //다중 선택 가능한 경우 값 배열 반환
			var vaIndex = ValueUtil.split(pnIndex, ",");
			for (var i = 0, len = vaIndex.length; i < len; i++) {
				vaIndex[i] = vcDataset.getValue(vaIndex[i], psColumnNm);
			}
			return vaIndex;
		} else {
			originVal = vcDataset.getValue(pnIndex, psColumnNm);
			return originVal;
		}
	}
};

/**
 * 컨트롤의 선행데이터를 리턴한다.
 * @param {cpr.core.AppInstance} app 앱 인스턴스
 * @param {#uicontrol} psCtlId  컨트롤ID 
 * @return {cpr.controls.Item[]}  선행 데이터 
 */
SelectKit.prototype.getStaticItems = function(app, psCtlId) {
	
	/** @type cpr.controls.ComboBox */
	var vcCtrl = app.lookup(psCtlId);
	
	var voItems = vcCtrl.getItems();
	
	if (voItems) {
		var voStaticItems = voItems.filter(function(item) {
			if (item.row == null) return true;
			else return false;
		});
		return voStaticItems;
	} else {
		return [];
	}
};

/**
 * 지정한 값에 해당하는 아이템 라벨(label)을 반환한다.
 * @param {cpr.core.AppInstance} 	app 앱인스턴스
 * @param {#uicontrol} psCtlId	컨트롤ID
 * @param {any} psValue	컨트롤값
 * @return {String}	라벨명
 */
SelectKit.prototype.getLabelByValue = function(app, psCtlId, psValue) {
	/**@type cpr.controls.ComboBox*/
	var vcCtrl = app.lookup(psCtlId);
	var voItem = vcCtrl.getItemByValue(psValue);
	if (voItem) {
		return voItem.label;
	}
	return "";
};


/**
 * 서브미션 유틸
 * @constructor
 * @param {common.AppKit} appKit
 */
function SubmissionKit(appKit) {
	this._appKit = appKit;
};

/**
 * Submission Before Handler<br>
 * 사이트별 Customizing 필요<br>
 *  - 시스템 컬럼 수정 필요 (CRT_USER_ID, CRT_PGM_ID, CRT_IP_MAC, UPD_USER_ID, UPD_PGM_ID, UPD_IP_MAC)
 * @param {cpr.events.CSubmissionEvent} e
 * @private
 */
SubmissionKit.prototype._onBeforeSubmit = function(e) {
	/** @type cpr.protocols.Submission*/
	var submit = e.control;
	var _app = submit.getAppInstance();
	
	/* PK키 original값 추가*/
	submit.setDataRowHandler(function( /** @type cpr.data.Row */ rowdata) {
		var additionalValue = {};
		var dsInfo = rowdata.getDataSetInfo();
		if (dsInfo && (rowdata.getState() == cpr.data.tabledata.RowState.UPDATED || cpr.data.tabledata.RowState.DELETED)) {
			var vaPks = dsInfo.split(",");
			vaPks.some(function(value, idx) {
				value = value.replace(/(^\s*)|(\s*$)/g, "")
				if (value == "") return false;
				
				additionalValue[value + "__origin"] = rowdata.getOriginalValue(value);
			});
		} else if (dsInfo && (rowdata.getState() == cpr.data.tabledata.RowState.INSERTED)) {
			var vaPks = dsInfo.split(",");
			vaPks.some(function(value, idx) {
				value = value.replace(/(^\s*)|(\s*$)/g, "")
				if (value == "") return false;
				
				additionalValue[value + "__origin"] = rowdata.getValue(value);
			});
		}
		
		return additionalValue;
	});
};

/**
 * Submission Receive Handler<br>
 * 사이트별 Customizing 필요<br>
 *  - 1. 에러메시지 키 변경 필요
 * @param {cpr.events.CSubmissionEvent} e
 * @param {Boolean} pbSuccess
 * @private
 */
SubmissionKit.prototype._onSubmitReceive = function(e, pbSuccess) {
	/** 
	 * @type cpr.protocols.Submission
	 */
	var submission = e.control;
	var xhr = submission.xhr;
	var contentType = xhr.getResponseHeader("Content-Type");
	
	if (contentType == null) return true;
	
	contentType = contentType.toLowerCase();
	if (contentType.indexOf(";") > -1) {
		contentType = contentType.substring(0, contentType.indexOf(";"));
	}
	contentType = ValueUtil.trim(contentType);
	
	/* 커스텀 옵션 : 서블릿(서버)에서 UIView를 리턴하거나 response.redirect 할 경우 사용*/
	//	if("text/html" == contentType && xhr.status == 200){
	//		var resText = xhr.responseText;
	//		if(!ValueUtil.isNull(xhr.responseURL) && !ValueUtil.isNull(resText)){
	//			if(resText.indexOf("<html") > -1){
	//				window.location.href = xhr.responseURL ;	
	//			}
	//		/*
	//		비동기방식 및 일부브라우저에서 작동되지 않아 권장방식 아님 		 
	//		document.open("text/html");
	//		document.write(xhr.responseText);
	//		document.close();
	//		 e.preventDefault();
	//		*/
	//		return false;	
	//		}
	//	}
	
	// 404 에러발생시 json 파싱 스크립트 에러가 나타나지 않도록 에러 처리 
	if ("text/html" == contentType && xhr.status == 404) {
		e.preventDefault();
		var errorEvent = new cpr.events.CSubmissionEvent("submit-error");
		submission.dispatchEvent(errorEvent);
		
		return false;
	}
	
	if ("application/json" != contentType || "text/tab-separated-values" == contentType) {
		return true;
	}
	
	var response = xhr.responseText;
	var jsonRes = JSON.parse(response);
	var errMsgInfo = jsonRes["ERRMSGINFO"];
	if (errMsgInfo) {
		var vsErrMsg = "";
		try {
			var vsErrMsg = "\"" + errMsgInfo.ERRMSG + "\"";
			vsErrMsg = Function('"use strict";return (' + vsErrMsg + ')')();
		} catch (e) {
			vsErrMsg = errMsgInfo.ERRMSG;
		}
		this._appKit.Msg.alert(vsErrMsg.replace(/\r\n/ig, "\n").replace(/\\n/gi, "\n"));
		var urlContext = top.location.pathname.substring(0, top.location.pathname.indexOf("/", 2));
		if (urlContext == "/") urlContext = "";
		
		/* 커스텀 옵션 : 에러메세지 키에 따른 화면 전환 필요시 사용*/
		//		//사용자 세션없는 오류인 경우
		//		if("CMN003.CMN@CMN003" == errMsgInfo.ERRCODE){
		//			top.location.href = urlContext+"/logout.jsp";
		//		//중복로그인 오류인 경우
		//		}else if("CMN003.CMN@CMN062" == errMsgInfo.ERRCODE){
		//			top.location.href = urlContext+"/logout.jsp";
		//		}
		return false;
	}
	
	return true;
};

/**
 * @param {cpr.events.CSubmissionEvent} e
 * @private
 */
SubmissionKit.prototype._onSubmitLoadProgress = function(e) {
	/** @type cpr.protocols.Submission*/
	var submission = e.control;
	var loadmask = this._getLoadMask(submission);
	if (loadmask) {
		try {
			if (submission.responseType === "blob") {
				loadmask.module.progress(e.total, e.loaded);
			}
			
			/* 커스텀 옵션 : CSV등 대용량 데이터 조회시 사용 */
			//	else if ( submission.responseType === "text" ){
			//		if(submission.getResponseDataCount() > 0){
			//			var rowCnt = submission.getResponseData(0).data.getRowCount();
			//			loadmask.module.count(rowCnt);
			//		}
			//	}
			if (submission.getResponseDataCount() > 0) {
				var rowCnt = submission.getResponseData(0).data.getRowCount();
				loadmask.module.count(rowCnt);
			}
		} catch (ex) {
			console.log(ex.toString());
			
		}
	}
};

/**
 * @param {cpr.events.CSubmissionEvent} e
 * @private
 */
SubmissionKit.prototype._onSubmitUploadProgress = function(e) {
	/** @type cpr.protocols.Submission*/
	var submission = e.control;
	var loadmask = this._getLoadMask(submission);
	if (loadmask) {
		loadmask.module.showProgress();
		loadmask.module.progress(e.loaded, e.total);
	}
};

/**
 * @param {cpr.events.CSubmissionEvent} e
 * @private
 */
SubmissionKit.prototype._onSubmitProgress = function(e) {
	/** @type cpr.protocols.Submission*/
	var submission = e.control;
	var loadmask = this._getLoadMask(submission);
	if (loadmask) {
		loadmask.module.showProgress();
		loadmask.module.progress(e.loaded, e.total);
	}
};

/**
 * Submission Success Handler
 * @param {cpr.events.CSubmissionEvent} e
 * @param {Boolean} pbSuccess
 * @private
 */
SubmissionKit.prototype._onSubmitSuccess = function(e, pbSuccess) {
	return pbSuccess;
};

/**
 * Submission Error Handler
 * @param {cpr.events.CSubmissionEvent} e
 * @private
 */
SubmissionKit.prototype._onSubmitError = function(e) {
	/** @type cpr.protocols.Submission*/
	var submission = e.control;
	var _app = submission.getAppInstance();
	
	var msg = "";
	var code = "";
	
	//메타 데이터가 미존재하는 경우에는 적용되지 않도록 
	if (submission.getMetadataKeys().length > 0) {
		msg = submission.getMetadata("ERRMSG");
		code = submission.getMetadata("STATUSCODE");
	}
	var xhr = e.control.xhr;
	var statusMsg = this._appKit.Msg.getMsg("ERR-" + xhr.status);
	
	msg = ValueUtil.isNull(msg) ? statusMsg : msg;
	
	if (e.nativeEvent) {
		msg = "network : " + e.nativeEvent.type;
	} else {
		if (ValueUtil.isNull(msg)) {
			//시스템 내부 장애가 발생하였습니다.\n 관리자에게 문의 하시기 바랍니다.
			msg = "ERR-SRV"
		}
	}
	this._appKit.Msg.notify(_app, msg, null, "DANGER");
	this._appKit.coverPage(_app);
	return false;
};

/**
 * Submission Error Status Handler<br>
 * 서브미션이 전송된 후 수신받은 서버의 응답상태코드가 200이 아닐 때 발생합니다.<br>
 * error-status 이벤트 핸들러에서 이벤트의 preventDefault함수를 호출하면 서버의 응답메세지를 모두 수신한 후 submit-error이벤트를 발생시킵니다.<br>
 * preventDefault함수를 호출하지 않으면 서버의 응답메세지를 수신하지 않고 즉시 submit-error이벤트를 발생시킵니다.<br>
 * 비동기로 동작할 때만 사용할 수 있습니다. 
 * @param {cpr.events.CSubmissionEvent} e
 * @private
 */
SubmissionKit.prototype._onSubmitErrorStatus = function(e) {
	/** @type cpr.protocols.Submission*/
	var submission = e.control;
	
	/* 커스텀 옵션 : 에러 코드에 따른 동작 추가시 활용*/
	//	var xhr = submission.xhr;
	//	xhr.status; //에러코드
	e.preventDefault();
	return false;
};

/**
 * Submission Done Handler<br>
 * 1. 서버에서 생성된 최신 로우 찾기<br>
 * 2. 어플리케이션 비즈니스 콜백 메소드 실행<br>
 * 3. 로딩 마스크 제거<br>
 * @param {cpr.events.CSubmissionEvent} e
 * @param {Function} poCallbackFunc
 * @param {Boolean} pbSuccess
 * @param {Boolean} pbAppDisable
 * @private
 */
SubmissionKit.prototype._onSubmitDone = function(e, poCallbackFunc, pbSuccess, pbAppDisable) {
	/** @type cpr.protocols.Submission*/
	var submission = e.control;
	var _app = submission.getAppInstance();
	
	//메타 데이터가 미존재하는 경우에는 적용되지 않도록 
	if (submission.getMetadataKeys().length > 0) {
		//마지막 행찾기
		var vsFindRowKey = submission.getMetadata("strFindRowKey");
		if (!ValueUtil.isNull(vsFindRowKey)) {
			var vnDsCnt = submission.getRequestDataCount();
			var voDs, vaFindKey;
			var vaFindRowKeys = ValueUtil.split(vsFindRowKey, "|");
			var findKey = null;
			for (var i = 0, len = vaFindRowKeys.length; i < len; i++) {
				findKey = ValueUtil.trim(vaFindRowKeys[i]);
				if (findKey == "") continue;
				vaFindKey = ValueUtil.split(findKey, ":");
				if (vaFindKey.length == 2) {
					for (var j = 0; j < vnDsCnt; j++) {
						voDs = submission.getRequestData(j).data;
						if (voDs.type != "dataset") continue;
						if (voDs.id == vaFindKey[0]) {
							voDs.userData("_findRowCondition", vaFindKey[1]);
							break;
						}
					}
				} else {
					for (var j = 0; j < vnDsCnt; j++) {
						voDs = submission.getRequestData(j).data;
						if (voDs.type != "dataset") continue;
						voDs.userData("_findRowCondition", vaFindKey[0]);
					}
				}
			}
		}
	}
	
	var loadmask = this._getLoadMask(submission);
	if (loadmask && loadmask.module.isVisibleProgress && loadmask.module.isVisibleProgress()) {
		loadmask.module.progress(100, 100);
	}
	
	var idx = loadmask._activeSubmission.indexOf(submission);
	if (idx != -1) {
		loadmask._activeSubmission.splice(idx, 1);
	}
	
	//실패한 경우.. 커버를 씌움
	if (pbAppDisable === true && pbSuccess != true) {
		this._appKit.coverPage(_app);
	}
	
	// submission success에서 다른 submission을 실행했을 경우 loadmask를 내리지 않는다.
	if (loadmask._activeSubmission.length == 0) {
		// hide loadmask
		try {
			if (ValueUtil.isNull(_app) || loadmask.getAppInstance() != _app) {
				// 앱이 disposing 된 경우, 루트앱인스턴스를 _app 으로 설정
				_app = loadmask.getAppInstance();
			}
			this._appKit.hideLoadMask(_app);
		} catch (ex) {
			console.log(ex.toString());
		}
	}
	
	
	if (!(submission instanceof cpr.protocols.MatrixSubmission)) {
		submission.removeAllFileParameters();
		submission.removeAllParameters();
//		submission.removeAllEventListeners();
	}
	
	//콜백이 존재하는 경우... 콜백함수 호출	
	//콜백을 제일 뒤로 옮김
	if (poCallbackFunc != null && (typeof poCallbackFunc == "function")) {
		poCallbackFunc(pbSuccess, e.control);
	}
};

/**
 * @param {cpr.protocols.Submission} poSubmission
 * @private
 */
SubmissionKit.prototype._getLoadMask = function(poSubmission) {
	var loadmask = cpr.core.NotificationCenter.INSTANCE.loadmask;
	return loadmask;
};

/**
 * 해당 서브미션 요청 데이터를 가지고 있는지 체크
 * @param {cpr.protocols.Submission} poSubmission 서브미션 객체
 * @param {String} psDataId  데이터셋/맵 ID
 * @private
 */
SubmissionKit.prototype._hasRequestData = function(poSubmission, psDataId) {
	for (var i = 0, len = poSubmission.getRequestDataCount(); i < len; i++) {
		if (poSubmission.getRequestData(i).data.id == psDataId) {
			return true;
		}
	}
	return false;
}

/**
 * 해당 서브미션 요청 데이터를 가지고 있는지 체크
 * @param {cpr.protocols.Submission} poSubmission  서브미션 객체
 * @param {String} psDataId 데이터셋/맵 ID
 * @private
 */
SubmissionKit.prototype._hasResponseData = function(poSubmission, psDataId) {
	for (var i = 0, len = poSubmission.getResponseDataCount(); i < len; i++) {
		if (poSubmission.getResponseData(i).data.id == psDataId) {
			return true;
		}
	}
	return false;
}

/**
 * 전송시 추가로 전달되는 파라미터를 추가합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#submission} psSubmissionId 서브미션 ID
 * @param {String} psParamName 파라미터의 이름
 * @param {String} psValue 파라미터의 값
 * @return void
 */
SubmissionKit.prototype.addParameter = function(app, psSubmissionId, psParamName, psValue) {
	/** @type cpr.protocols.Submission */
	var vcSubmission = app.lookup(psSubmissionId);
	vcSubmission.addParameter(psParamName, psValue);
};

/**
 * 전송시 추가로 전달되는 파라미터를 추가합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#submission} psSubmissionId 서브미션 ID
 * @param {Array} paFiles 파일의 객체
 * @return void
 */
SubmissionKit.prototype.addFileParameter = function(app, psSubmissionId, paFiles) {
	/** @type cpr.protocols.Submission */
	var vcSubmission = app.lookup(psSubmissionId);
	if (paFiles == null) return;
	if (paFiles instanceof Array) {
		paFiles.forEach(function(voFile) {
			vcSubmission.addFileParameter("exb.fileupload.filelist", voFile);
		});
	} else {
		vcSubmission.addFileParameter("exb.fileupload.filelist", paFiles);
	}
};

/**
 * 엑셀 임포트 시 추가로 전달되는 파라미터를 추가합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#submission} psSubmissionId 서브미션ID
 * @param {#grid} psGridId 그리드 ID
 * @param {File} poFile 파일객체
 * @param {
 *   headerLine : Number <!-- 헤더 라인 -->,
 *   excludeCellIndex : Number[] <!-- 임포트 시 제외할 detail cellindex -->,
 *   datasetId : #dataset <!-- 임포트 할 데이터셋 id -->
 * } options? 추가옵션
 */
SubmissionKit.prototype.addImportGridFileParameter = function(app, psSubmissionId, psGridId, poFile, options) {
	/** @type cpr.protocols.Submission */
	var vcSubmission = app.lookup(psSubmissionId);
	if (poFile == null) return;
	
	/** @type cpr.controls.Grid */
	var vcGrid = app.lookup(psGridId);
	var vcDataset = vcGrid.dataSet;
	var voColInfo = {};
	
	vcGrid.getColumnLayout().detail.map(function(each){
		var cellIndex = each.cellIndex;
		if(options && !ValueUtil.isNull(options.excludeCellIndex)) {
			if (!(options.excludeCellIndex instanceof Array)) {
				options.excludeCellIndex = [options.excludeCellIndex];
			}
			if(options.excludeCellIndex.indexOf(cellIndex) != -1) {
				return;
			}
		}
		
		var voDColumn = vcGrid.detail.getColumn(cellIndex);
		var vnHdrCellIndices = vcGrid.getHeaderCellIndices(cellIndex);
		var voHColumn = vcGrid.header.getColumn(vnHdrCellIndices[vnHdrCellIndices.length-1]);
		if(voDColumn.columnName) {
			voColInfo[voHColumn.text] = voDColumn.columnName;
		}
	});

	var vnHeaderLine = (options && !ValueUtil.isNull(options.headerLine)) ? options.headerLine : vcGrid.header.getRowHeights().length;
	if(vnHeaderLine < 0) vnHeaderLine = 0;
	
	vcSubmission.setParameters("headerLine", vnHeaderLine);
	vcSubmission.setParameters("columnInfo", JSON.stringify(voColInfo));
	vcSubmission.setParameters("datasetId", vcDataset.id);
	vcSubmission.addFileParameter("exb.import.grid.file", poFile);
};


/**
 * 전송시 추가로 전달되는 파라미터를 추가합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#submission} psSubmissionId 서브미션 ID
 * @param {#datamap | #dataset} psDataId 데이터셋 또는 데이터맵 ID
 * @param {String} psAlias? 요청 데이터의 Alias명(요청데이터 명칭이 다른 경우에만 지정)
 * @param {String} psPayloadType? 요청 데이터의 payloadType (all, modified)
 * @return void
 */
SubmissionKit.prototype.addRequestData = function(app, psSubmissionId, psDataId, psAlias, psPayloadType) {
	/** @type cpr.protocols.Submission */
	var submission = app.lookup(psSubmissionId);
	submission.addRequestData(app.lookup(psDataId), psAlias, psPayloadType);
};

/**
 * 전송시 추가로 응답데이터를 추가합니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#submission} psSubmissionId 서브미션 ID
 * @param {#datamap | #dataset} psDataId 데이터셋 또는 데이터맵 ID
 * @param {Boolean} pbAdd 데이터셋 옵션 설정된 데이터셋에 데이터를 모두 지우고 추가할지 기존 데이터를 남기고 추가 할지 여부
 * @param {String} psAlias? 응답 데이터의 Alias명(응답데이터 명칭이 다른 경우에만 지정)
 * @return void
 */
SubmissionKit.prototype.addResponseData = function(app, psSubmissionId, psDataId, pbAdd, psAlias) {
	/** @type cpr.protocols.Submission */
	var submission = app.lookup(psSubmissionId);
	submission.addResponseData(app.lookup(psDataId), pbAdd, psAlias);
};

/**
 * 서브미션 호출<br>
 * - 사이트별 Customizing 필요<br>
 * - 서브미션에 before-submit,  receive, submit-error, submit-success, submit-done 이벤트 부여
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#submission} psSvcId 서브미션 ID
 * @param {Function} successCallback 서브미션 후 콜백 메소드
 * @param {Boolean} pbAppEnable? 서브미션 오류 및 exception 발생시 커버페이지를 씌움
 * @param {String} maskType 로드 마스크 타입
 */
SubmissionKit.prototype.send = function(app, psSvcId, successCallback, pbAppEnable, maskType) {
	var _app = app;
	var submission = _app.lookup(psSvcId);
	// 통신 중 동일한 서브미션 요청 발생시 예외처리
	if (!submission || submission.status == "SENDING") return;
	
	//context-path를 고려하여, action URL이 ../로 시작하도록 변경
	if (submission.action.indexOf("/") == 0) {
		submission.action = ".." + submission.action;
	}
	
	/* 커스텀 옵션 : multipart/form-data인 경우 maskType 변경시 설정*/
	//	if(submission.mediaType === "multipart/form-data") {
	//		maskType="pro";
	//	}
	
	//어플리케이션 전체에 마스크(Mask)를 씌운다.
	this._appKit.showLoadMask(app, maskType);
	
	var loadmask = cpr.core.NotificationCenter.INSTANCE.loadmask;
	
	if (submission.userAttr("responseType") === "TSV" || submission.fallbackContentType === "text/tab-separated-values") {
		loadmask = this._getLoadMask(submission);
		loadmask.module.count(0);
		loadmask.module.show();
		
		submission.addEventListener("submit-load-progress", function(e) {
			_this._onSubmitLoadProgress(e);
		});
	}
	
	var vbSuccess = true;
	var _this = this;
	submission.addEventListenerOnce("before-submit", function(e) {
		_this._onBeforeSubmit(e);
	});
	
	if (submission.mediaType === "multipart/form-data") {
		submission.addEventListener("submit-upload-progress", function(e) {
			_this._onSubmitUploadProgress(e);
		});
	}
	
	if (submission.responseType === "blob") {
		submission.addEventListenerOnce("submit-progress", function(e) {
			_this._onSubmitProgress(e);
		});
	}
	
	submission.addEventListenerOnce("receive", function(e) {
		vbSuccess = _this._onSubmitReceive(e);
	});
	
	submission.addEventListenerOnce("submit-error", function(e) {
		vbSuccess = _this._onSubmitError(e);
	});
	
	submission.addEventListenerOnce("error-status", function(e) {
		vbSuccess = _this._onSubmitErrorStatus(e);
	});
	
	submission.addEventListenerOnce("submit-success", function(e) {
		vbSuccess = _this._onSubmitSuccess(e, vbSuccess);
	});
	
	submission.addEventListenerOnce("submit-done", function(e) {
		_this._onSubmitDone(e, successCallback, vbSuccess, pbAppEnable);
	});
	
	// hostAppInstance 와 udc 내부 다른 util 객체 생성으로 배열에 적재되지 않는 문제 해결 (2022-07-27 dyseo update)
	if (!ValueUtil.isNull(loadmask)) {
		if (ValueUtil.isNull(loadmask._activeSubmission)) {
			loadmask._activeSubmission = [];
		}
		loadmask._activeSubmission[loadmask._activeSubmission.length] = submission;
	}
    	
	return submission.send();
};

/**
 * 탭(TabFolder) 컨트롤 유틸
 * @constructor
 * @param {common.AppKit} appKit
 */
function TabKit(appKit) {
	this._appKit = appKit;
};

/**
 * 현재 선택된 탭아이템 id를 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#tabfolder} psTabId 탭 ID
 * @return {Number} 탭아이템 id (탭아이템id는 인덱스와 유사 탭아이템 순서대로 id 부여됨)
 */
TabKit.prototype.getSelectedId = function(app, psTabId) {
	/** @type cpr.controls.TabFolder */
	var vcTab = app.lookup(psTabId);
	var vcTabItem = vcTab.getSelectedTabItem();
	
	return vcTabItem ? vcTabItem.id : "";
};

/**
 * 현재 선택된 탭아이템 Text를 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#TabFolder} psTabId 탭 ID
 * @return {Number} 탭아이템 Text (탭아이템id는 인덱스와 유사 탭아이템 순서대로 id 부여됨)
 */
TabKit.prototype.getSelectedNm = function(app, psTabId) {
	/** @type cpr.controls.TabFolder */
	var vcTab = app.lookup(psTabId);
	var vcTabItem = vcTab.getSelectedTabItem();
	
	return vcTabItem ? vcTabItem.text : "";	
};

/**
 * 입력한 id에 해당하는 탭 아이템을 선택한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#tabfolder} psTabId 탭 ID
 * @param {Number} pnId 탭아이템 ID
 * @param {Boolean} emitEvent? 이벤트(before-selection-change, selection-change)를 발생시킬지 여부
 */
TabKit.prototype.setSelectedTabItemById = function(app, psTabId, pnId, emitEvent) {
	/** @type cpr.controls.TabFolder */
	var vcTab = app.lookup(psTabId);
	
	var vaTabItem = vcTab.getTabItems();
	var vcTabItem = vaTabItem.filter(function(item) {
		return item.id == pnId;
	});
	
	var emit = emitEvent != undefined ? emitEvent : true;
	vcTab.setSelectedTabItem(vcTabItem[0], emit);
};

/**
 * 탭 페이지를 숨기거나/보여준다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#tabfolder} psTabId 탭 ID
 * @param {Number} pnIndex 탭 아이템 Index
 * @param {Boolean} pbVisible 숨김여부
 */
TabKit.prototype.setVisibleTabItem = function(app, psTabId, pnIndex, pbVisible) {
	/** @type cpr.controls.TabFolder */
	var vcTab = app.lookup(psTabId);
	
	var vaTabItem = vcTab.getTabItems();
	var vcTabItem = vaTabItem.filter(function(item) {
		return item.id == pnIndex;
	});
	
	if (vcTabItem) {
		vcTabItem[0].visible = pbVisible;
	}
};

/**
 * 탭 페이지 버튼을 활성화시키거나 비활성화 시킨다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#tabfolder} psTabId 탭 ID
 * @param {String | Array} paIndex  활성화/비활성화 할 탭 Index 또는 Index 배열 (탭 index 시작 = 1) 
 * @param {Boolean} psEnable  활성화여부
 */
TabKit.prototype.setEnableTabItem = function(app, psTabId, paIndex, psEnable) {
	/** @type cpr.controls.TabFolder */
	var vcTab = app.lookup(psTabId);
	
	if (!(paIndex instanceof Array)) {
		paIndex = [paIndex];
	}
	var vaTabItem = vcTab.getTabItems();
	
	for (var i = 0, len = paIndex.length; i < len; i++) {
		var vnTabIdx = paIndex[i] - 1;
		vaTabItem[vnTabIdx].enabled = psEnable;
	}
};

/**
 * 현재 선택된 탭아이템 name을 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#tabfolder} psTabId 탭 Id
 * @return {String} 탭아이템 name
 */
TabKit.prototype.getSelectedTabName = function(app, psTabId) {
	/** @type cpr.controls.TabFolder */
	var vcTab = app.lookup(psTabId);
	var tabItem = vcTab.getSelectedTabItem();
	
	return tabItem ? tabItem.name : "";
};

/**
 * 입력한 name에 해당하는 탭 아이템을 선택한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#tabfolder} psTabId 탭Id
 * @param {String} psName 탭아이템 name
 * @param {Boolean} pbEmitEvent? 이벤트(before-selection-change, selection-change)를 발생시킬지 여부 (default: true)
 */
TabKit.prototype.setSelectedTabItemByName = function(app, psTabId, psName, pbEmitEvent) {
	/** @type cpr.controls.TabFolder */
	var vcTab = app.lookup(psTabId);
	
	var vaTabItem = vcTab.getTabItems().filter(function(item) {
		return item.name == psName;
	});
	
	var emitEvent = !ValueUtil.isNull(pbEmitEvent) ? pbEmitEvent : true;
	vcTab.setSelectedTabItem(vaTabItem[0], emitEvent);
};


/**
 * 트리(Tree) 컨트롤 유틸
 * @constructor
 * @param {common.AppKit} appKit
 */
function TreeKit(appKit) {
	this._appKit = appKit;
};

/**
 * 현재 선택된 아이템의 value를 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#tree} psTreeId 트리 ID
 * @param {String} psDiv? 얻어올 값 영역(label 또는 value)
 * @return {String | Array}  multiple : true 일 경우 Array(String)<br>
 *                           multiple : false 일 경우 String  
 */
TreeKit.prototype.getSelectedValue = function(app, psTreeId, psDiv) {
	/** @type cpr.controls.Tree */
	var vcTree = app.lookup(psTreeId);
	var vaItem = vcTree.getSelection();
	//아이템이 없으면... 공백 반환
	if (vaItem.length < 1) return "";
	
	psDiv = (psDiv != null ? psDiv.toUpperCase() : "VALUE");
	if (vcTree.multiple) {
		var vaValues = new Array();
		vaItem.forEach(function(vcItem) {
			if (psDiv == "LABEL")
				vaValues.push(vcItem.label);
			else
				vaValues.push(vcItem.value);
		});
		return vaValues;
	} else {
		return psDiv == "LABEL" ? vaItem[0].label : vaItem[0].value;
	}
};

/**
 * 입력한 value에 해당하는 아이템의 label 또는 parentValue를 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#tree} psTreeId 트리 ID
 * @param {String} psValue search value
 * @param {String} psDiv 가지고 오는 구분자 값(LABEL(디폴트), PVALUE)
 * @return {String}
 */
TreeKit.prototype.getItem = function(app, psTreeId, psValue, psDiv) {
	/** @type cpr.controls.Tree */
	var vcTree = app.lookup(psTreeId);
	psDiv = !ValueUtil.isNull(psDiv) ? psDiv.toUpperCase() : "LABEL";
	
	try {
		var vaItem = vcTree.getSelection();
		if (!psValue && vaItem.length > 0) psValue = vaItem[0].value;
	} catch (e) {
		return null;
	}
	
	var voItem = vcTree.getItemByValue(psValue);	
	if (!voItem) return null;
	
	if (psDiv == "LABEL") {
		return voItem.label;
	} else {
		return voItem.parentValue;
	}
};

/**
 * 해당 아이템의 상위 아이템을 펼친다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#tree} psTreeId 트리 ID
 * @param {Object} poItem item
 * @param {Boolean} pbHierarchy? 계층적으로 모든 상위까지 펼칠지 여부 (false인 경우, 바로 상위의 부모 아이템만 펼친다.)
 * @return void
 */
TreeKit.prototype.expandParentItem = function(app, psTreeId, poItem, pbHierarchy) {
	/** @type cpr.controls.Tree */
	var vcTree = app.lookup(psTreeId);
	var vaParentItem = new Array();
	pbHierarchy == !ValueUtil.isNull(pbHierarchy) ? pbHierarchy : true;
	
	function checkExpandItem(poPItem) {
		var item = vcTree.getItemByValue(poPItem.parentValue);
		if (item != null && item.value != "" && !vcTree.isExpanded(item)) {
			vaParentItem.push(item);
			checkExpandItem(item);
		}
	}
	
	if (pbHierarchy) {
		checkExpandItem(poItem);
	} else {
		vaParentItem.push(vcTree.getItemByValue(poItem.parentValue));
	}
	
	for (var i = 0, len = vaParentItem.length; i < len; i++) {
		if (vaParentItem[i]) {
			vcTree.expandItem(vaParentItem[i]);
		}
	}
};

/**
 * 트리 선택 아이템 변경 이벤트 발생시, 변경 이전에 선택된 아이템을 선택해준다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {cpr.events.CSelectionEvent} event 트리 선택 아이템 변경 이벤트
 * @param {Boolean} emitEvent? 이벤트(before-selection-change, selection-change)를 발생시킬지 여부
 * @return void
 */
TreeKit.prototype.selectBeforeRow = function(app, event, emitEvent) {
	/** @type cpr.controls.Tree */
	var vcTree = event.control;
	var emit = emitEvent === true ? true : false;
	
	var voOldSelection = event.oldSelection[0];
	var vsOldVal = voOldSelection.value;
	vcTree.selectItemByValue(vsOldVal, emit);
	// 1.0.3795(2022-05-13) 버전에서 트리의 focusItem API가 revealItem으로 변경
	vcTree.revealItem(voOldSelection);
};

/**
 * 입력한 label 또는 value에 해당하는 트리 아이템을 선택한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#tree} psTreeId 트리 ID
 * @param {String} psValue search value
 * @param {String} psDiv 가지고 오는 구분자 값(VALUE(디폴트), LABEL)
 * @return void
 */
TreeKit.prototype.selectItem = function(app, psTreeId, psValue, psDiv) {
	/** @type cpr.controls.Tree */
	var vcTree = app.lookup(psTreeId);
	
	psDiv = !ValueUtil.isNull(psDiv) ? psDiv.toUpperCase() : "VALUE";
	
	if (psDiv == "VALUE") {
		vcTree.selectItemByValue(psValue);
	} else {
		vcTree.selectItemByLabel(psValue);
	}
};

/**
 * 아이템에 해당하는 모든 child item을 펼치거나 닫습니다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#tree} psTreeId 트리 ID
 * @param {Boolean} pbExpand 펼치기 : true, 닫기 : false
 * @param {Object} poItem? item 생략가능 (default 최상위 item)
 * @param {Boolean} pbIncludeRoot? 루트 아이템을 포함하여 펼칠지 여부 생략가능 <br>(default false)
 * @return void
 */
TreeKit.prototype.expandAllItems = function(app, psTreeId, pbExpand, poItem, pbIncludeRoot) {
	/** @type cpr.controls.Tree */
	var vcTree = app.lookup(psTreeId);
	
	var vbIncludeRoot = ValueUtil.isNull(pbIncludeRoot) ? false : pbIncludeRoot;
	
	if (!ValueUtil.isNull(poItem)) {
		if (pbExpand) {
			vcTree.expandItem(poItem);
			vcTree.expandAllItems(poItem, {
				includeRoot: vbIncludeRoot
			});
		} else {
			vcTree.collapseItem(poItem);
			vcTree.collapseAllItems(poItem, {
				includeRoot: vbIncludeRoot
			});
		}
		
	} else {
		pbExpand ? vcTree.expandAllItems() : vcTree.collapseAllItems();
	}
	
};

/**
 * 현재 선택된 트리 인덱스
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {#tree} psTreeId 트리 ID
 * @return {Number} 현재 선택된 트리 인덱스
 */
TreeKit.prototype.getIndex = function(app, psTreeId) {	
	/**@type cpr.controls.Tree */
	var vcTree = app.lookup(psTreeId);
	var selectItem = vcTree.getSelectionFirst();
	if (selectItem) {
		return selectItem.row.getIndex();
	}
	return null;
};

/**
 * 트리 컨트롤 초기화<br>
 * udcComAppHeader 에서 모든 트리를 대상으로 초기화 지정<br><br>
 * 
 * <b><수행 로직></b><br>
 * 1. 프리폼 PK컬럼 enable 설정<br>
 *   - 트리의 선택행 컨텍스트 사용 그룹은 트리의 사용자속성 bindDataFormId 지정 필수<br>
 *   - 신규 item 일 경우에만 PK컬럼 활성화 설정<br>
 * 2. update이벤트 추가 ( 저장후 트리의 마지막 작업행을 찾기 위함)<br>
 * 3. 트리 매핑 데이터셋에 load 이벤트 추가 (트리의 마지막 작업행 찾기)<br><br>
 * 
 *  - 사이트별 Customizing 필요
 * @param {cpr.core.AppInstance} app 앱 인스턴스
 * @param {#tree} paTreeId 트리 ID
 */
TreeKit.prototype.init = function(app, paTreeId) {
	if (!(paTreeId instanceof Array)) {
		paTreeId = [paTreeId];
	}
	
	var _this = this;
	var _app = app;
	
	for (var i = 0, len = paTreeId.length; i < len; i++) {
		/** @type cpr.controls.Tree */
		var vcTree = (paTreeId[i] instanceof cpr.controls.Tree) ? paTreeId[i] : _app.lookup(paTreeId[i]);
		if (vcTree == null) continue;
		
		/* 트리 초기화 제외 */ 
		if(vcTree.userAttr("ignoreInit") === "Y") continue;
		
		/** @type cpr.data.DataSet */
		var vcDataSet = vcTree.dataSet;
		vcDataSet.userAttr("_treeId", vcTree.id);
		
		//프리폼 PK 컬럼 설정  
		var vaPkColumnNames = ValueUtil.split(vcDataSet.info, ",");
		var vsDataBindCtxId = vcTree.userAttr("bindDataFormId");
		if (!ValueUtil.isNull(vsDataBindCtxId) && vaPkColumnNames.length > 0) {
			var freeformes = ValueUtil.split(vsDataBindCtxId, ",");
			freeformes.forEach(function( /* eachType */ formId) {
				/**@type cpr.controls.Container */
				var freeform = _app.lookup(formId);
				if (freeform != null) {
					var vaChildCtrls = freeform.getAllRecursiveChildren();
					vaPkColumnNames.some(function(value, idx) {
						if (value == "") return false;
						vaChildCtrls.some(function(ctrl, idx) {
							if (ctrl.type == "output") return false;
							if (ctrl.userAttr("ignorePk") == "Y") return false;
							if (ctrl.userAttr("editablePK") == "Y") return false;
							var bind = ctrl.getBindInfo("value");
							if (bind && bind.type == "datacolumn" && value == bind.columnName) {
								ctrl.bind("enabled").toExpression("getStateString() == 'I' ? true : false"); // 신규 item 일 경우에만 PK컬럼 활성화 설정
								ctrl.userAttr("required", "Y");
							}
						});
					});
				}
			});
		}
		
		// 마지막 작업행 findRow
		vcDataSet.addEventListener("update", function( /* cpr.events.CDataEvent */ e) {
			var dataset = e.control;
			var rowIndex = e.row.getIndex();
			var vaPkColumns = ValueUtil.split(dataset.info, ",");
			
			if (vaPkColumns.length < 1 || ValueUtil.isNull(dataset.info)) {
				dataset.userAttr("_findTreeCondition", "");
			} else {
				var vaTempCond = [];
				vaPkColumns.forEach(function(column) {
					vaTempCond.push(dataset.getValue(rowIndex, column));
				});
				
				if (vaTempCond.length > 0) {
					dataset.userAttr("_findTreeCondition", vaTempCond.join(""));
				} else {
					dataset.userAttr("_findTreeCondition", "");
				}
			}
		});
		
		// 트리에 바인딩된 데이터셋(Dataset)이 로드될 때 처리
		// 마지막행 찾기, 조회 건수 업데이트
		vcDataSet.addEventListener("load", function( /* cpr.events.CDataEvent */ e) {
			
			/** @type cpr.data.DataSet */
			var dataset = e.control;
			/** @type cpr.controls.Tree */
			var tree = _app.lookup(dataset.userAttr("_treeId"));
			if (tree == null) return;
			
			//대상 트리가 정렬된 상태라면... 정렬을 푼다.
			if (dataset.getSort() != "") {
				dataset.clearSort();
			}
			
			//마지막 작업행 찾기
			if (dataset.getRowCount() > 0) {
				if (dataset.userAttr("_findTreeCondition")) {
					cpr.core.DeferredUpdateManager.INSTANCE.asyncExec(function(e) {
						var voRow = dataset.findFirstRow(tree.itemSetConfig.value + "=='" + dataset.userAttr("_findTreeCondition") + "'");
						var vnIdx = voRow.getIndex();
						tree.selectItem(vnIdx);
						tree.revealItem(tree.getItem(vnIdx));
					});
				} else {
					cpr.core.DeferredUpdateManager.INSTANCE.asyncExec(function(e) {
						var item = tree.getItem(0);
						_this.expandParentItem(_app, tree.id, item);
						tree.selectItem(0, true);
					});
				}
			}
			//마지막 작업행 정보 Clear
			dataset.userAttr("_findTreeCondition", "");
		});
	}
};

/**
 * 트리에 표시된 체크박스의 체크된 아이템배열을 반환한다.<br>
 * multiple=true or showItemCheckbox=true 일 경우 체크 된 아이템을 확인한다.<br>
 * 둘 다 false 일 경우, null을 반환한다.
 * @param {cpr.core.AppInstance} app 앱 인스턴스
 * @param {#tree} psTreeId 트리 ID
 * @return {cpr.controls.TreeItem[]}  체크박스에 체크되어 있는 트리 아이템 배열 (multiple, showItemCheckbox 둘다 true 일 경우 multiple 에 선택된 아이템 반환 )
 */
TreeKit.prototype.getCheckedItems = function(app, psTreeId) {
	
	/** @type cpr.controls.Tree */
	var vcTree = app.lookup(psTreeId);
	var voSelection = null;
	
	var vbMultiple = vcTree.multiple;
	var vbItemCheckbox = vcTree.showItemCheckbox;
	if (vbMultiple) {
		voSelection = vcTree.getSelection();
	} else if (vbItemCheckbox) {
		voSelection = vcTree.getCheckedItems();
	}
	
	return voSelection;
};

/**
 * 트리 아이템의 모든 자식들을 반환한다.
 * @param {cpr.controls.TreeItem} poItem 트리 아이템
 * @param {boolean} pbIncludeSelf? 자신을 포함할지 여부. (defualt:true)
 * @return {cpr.controls.TreeItem[]}
 */
TreeKit.prototype.getTreeItemAllChildren = function(poItem, pbIncludeSelf) {
	var _this = this;
	
	var vaChildren = [];
	
	//pbIncludeSelf 가 false일 경우 자신을 포함하지 않는다.
	if (pbIncludeSelf != false) {
		vaChildren.push(poItem);
	}
	
	// 전달받은 Item의 자식값.
	var voChildren = poItem.children;
	
	// 자식이 있을 경우.
	if (voChildren) {
		voChildren.forEach(function(eachItem) {
			// 자식의 자식이 있을 경우 체크한다.
			_this.getTreeItemAllChildren(eachItem).forEach(function(treeItem) {
				vaChildren.push(treeItem);
			});
		});
	}
	return vaChildren;
}

/**
 * 트리 아이템의 모든 상위 아이템들을 반환한다.
 * @param {cpr.controls.TreeItem} poItem 트리 아이템
 * @param {boolean} pbIncludeSelf? 자신을 포함할지 여부. (defualt:true)
 * @return {cpr.controls.TreeItem[]}
 */
TreeKit.prototype.getTreeItemAllParents = function(poItem, pbIncludeSelf) {
	var _this = this;
	
	var vaParents = [];
	
	// pbIncludeSelf 가 false일 경우 자신을 포함하지 않는다.
	if (pbIncludeSelf != false) {
		vaParents.push(poItem);
	}
	
	// 전달받은 Item의 상위 아이템.
	var poParent = poItem.parentItem;
	
	// 상위 아이템이 있을 경우.
	if (poParent) {
		_this.getTreeItemAllParents(poParent).forEach(function(treeItem) {
			// vaParents 객체에 담는다.
			vaParents.push(treeItem);
		});
		
	}
	return vaParents;
}


/**
 * 파일 유틸
 * @constructor
 * @param {common.AppKit} appKit
 */
function FileKit(appKit) {
	this._appKit = appKit;
};

/**
 * 파일 최대 업로드 사이즈
 */
FileKit.prototype.getMaxUploadSize = function() {
	return 100;
};

/**
 * 업로드 가능한 파일 확장자 목록반환
 */
FileKit.prototype.getPemitedFileExts = function() {
	var vaFileExt = ['JPG', 'PNG', 'GIF', 'TIF', 'TIFF', 'JFIF', 'BMP', 'TXT', 'HWP', 'DOCX', 'DOC'
					, 'DOCM', 'PPT', 'PPTX', 'PPTM', 'PPS', 'PPSX', 'XLS', 'XLSX', 'XLSM', 'XLAM'
					, 'XLA', 'PSD', 'PDF', 'ODS', 'OGG', 'MP4', 'AVI', 'WMV', 'ZIP', 'RAR', 'TAR'
					, '7Z', 'TBZ', 'TGZ', 'LZH', 'GZ', 'AI'
				   ];
	return vaFileExt;
};

/**
 * 업로드 불가한 파일 확장자 목록반환
 */
FileKit.prototype.getLimitedFileExts = function() {
	// 파일 선택 제한 확장자.
	var vaFileExt = ['A6P','AC','AS','ACR','ACTION','AIR','APP','ASP','ASPX','AWK'
					,'BAT'
					,'CGI','CMD','COM','CSH'
					,'DEK','DLD','DS'
					,'EBM','ESH','EXE','EZS'
					,'FKY','FRS','FXP'
					,'GADGET'
					,'HMS','HTA'
					,'ICD','INX','IPF','ISU'
					,'JAR','JS','JSE','JSP','JSX'
					,'KIX'
					,'LUA'
					,'MCR','MEM','MPX','MS','MST'
					,'OBS'
					,'PAF','PEX','PHP','PIF','PL','PRC','PRG','PVD','PWC','PY','PYC','PYO'
					,'QPX'
					,'RBX','RGS','ROX','RPJ'
					,'SCAR','SCR','SCRIPT','SCT','SH','SHB','SHS','SPR'
					,'TLB','TMS'
					,'U3P','UDF'
					,'VB','VBE','VBS','VBSCRIPT'
					,'WCM','WPK','WS','WSF'
					,'XQT'
				  ];
	
	return vaFileExt;
};	

/**
 * 파일 확장자 체크
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {String} psFileNm 파일명
 * @param {String[]} paFileFilter 가능 파일 확장자 명
 */
FileKit.prototype.checkFileExt = function(app, psFileNm, paFileFilter) {
	if (ValueUtil.isNull(paFileFilter)) paFileFilter = this.getPemitedFileExts();
		
	var vbCheck = false;
	//허용 파일 확장자 체크
	var arrStr = ValueUtil.split(psFileNm, ".");
	var extStr = arrStr [arrStr.length - 1].toUpperCase();
	for (var i=0, len=paFileFilter.length; i<len; i++) {
		if (extStr == paFileFilter[i].toUpperCase()) {
			vbCheck = true;
			break;
		}
	}
	
	if (!vbCheck) {
		this._appKit.Msg.alertDlg(app, "WRN-M024", [extStr]);
		return false;
	}
	
	//제한 파일 확장자 체크
	var vaLimitedFileExts = this.getLimitedFileExts();
	for (var i=0, len=vaLimitedFileExts.length; i<len; i++) {
		if (extStr == vaLimitedFileExts[i]) {
			this._appKit.Msg.alertDlg(app, "WRN-M024", [extStr]);
			return false;
		}
	}
	
	return true;
};	

/**
 * 파일 업로드 용량 체크
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {File} poFile 검사할 파일
 * @param {Number} pnLimitFileSize 파일 제한 사이즈
 */
FileKit.prototype.checkFileSize = function(app, poFile, pnLimitFileSize) {
	if(!ValueUtil.isNull(poFile)){
		if(poFile.size > (pnLimitFileSize * 1024 * 1024)){
			//파일의 크기가 @mb를 초과하는 경우 첨부할 수 없습니다.
			this._appKit.Msg.alertDlg(app, "WRN-M016", [pnLimitFileSize * 1024 * 1024]);
			return false;
		}
	}
	
	return true;	
};	


/**
 * File Dialog를 띄우고 선택한 File을 반환한다.
 * @param {cpr.core.AppInstance} app 앱인스턴스
 * @param {Any} paFileFilter 확장자를 선택한다. null일 경우 default로 적용되어 있는 확장자만 업로드하도록 한다. (예: ["xls", "xlsx"])
 * @param {Function} poCallBackFunc 후처리 콜백함수
 * @param {Boolean} pbMultiple? 단건 선택일지 멀티 선택일지 여부 (default : 멀티) false 일경우 단건
 * @param {Number} pnLimitFileSize? 파일업로드 제한 용량사이즈(mb 단위)
 */
FileKit.prototype.getFileName = function(app, paFileFilter, poCallBackFunc, pbMultiple, pnLimitFileSize) {
	app.getContainer().getChildren().forEach(function(each){
		if(each instanceof cpr.controls.FileInput && each.id == "com_fileinput") {
			each.dispose();
		}
	});
	
	var fileInput = new cpr.controls.FileInput("com_fileinput");
	fileInput.visible = false;
	//파일 확장자
	if(!ValueUtil.isNull(paFileFilter)) {
		var tempFilter = "";
		if(paFileFilter instanceof Array) {
			for(var i = 0; i < paFileFilter.length; i++) {
				if(i == 0) {
					tempFilter += "." + paFileFilter[i];
				} else {
					tempFilter = tempFilter + ",." + paFileFilter[i];
				}
			}
			fileInput.acceptFile = tempFilter;
		} else {
			fileInput.acceptFile = paFileFilter;
		}
		
	}
	//multi 선택가능여부
	if(!ValueUtil.isNull(pbMultiple)) {
		fileInput.multiple = pbMultiple;
	}
	//파일 업로드 용량
	if(!ValueUtil.isNull(pnLimitFileSize)) {
		fileInput.limitFileSize = pnLimitFileSize;
	}else{
		fileInput.limitFileSize = this.getMaxUploadSize();
	}
	fileInput.limitFileSizeUnit = "mb";
	
	fileInput.addEventListenerOnce("value-change", function(e) {
		if(typeof (poCallBackFunc) == "function"){
			var files = fileInput.files;
			var vsFileNm = "";
			
			//파일 선택 유/무 체크
			if(files.length < 1){
				//파일을 선택해 주세요.
				this._appKit.Msg.alertDlg(app, "CRM-M012");
				return false;
			}
			for(var i=0, len=files.length; i<len; i++) {
				var voFile = files[i];
				//업로드 확장자 체크
				var fileFilter = new Array();
				
				if(paFileFilter) {
					if(typeof(paFileFilter) == "string") {
						var tempFileFilter = paFileFilter.split(",");
						
						tempFileFilter.forEach(function(each){
							fileFilter.push(each.replace(".", ""));
						});
					}
					
					if(paFileFilter instanceof Array) {
						fileFilter = paFileFilter;
					}
				}
				
	   			if(!this.checkFileExt(app, voFile.name, fileFilter)) return false;
				
				//업로드 용량 체크 로직
		   		if(!this.checkFileSize(app, voFile, fileInput.limitFileSize)) return false;
			}
			
		   	poCallBackFunc(files);
		}
	});
	
	app.getContainer().addChild(fileInput, {
		"width": "0px",
		"height": "0px"
	});
	
	fileInput.redraw();
	
	setTimeout(function() {
		fileInput.openFileChooser();
	}, 500);
};	

exports.ComUdcBtnKit = ComUdcBtnKit;
exports.ControlKit = ControlKit;
exports.DataMapKit = DataMapKit;
exports.DataSetKit = DataSetKit;
exports.DialogKit = DialogKit;
exports.EmbeddedAppKit = EmbeddedAppKit;
exports.FreeFormKit = FreeFormKit;
exports.GridKit = GridKit;
exports.GroupKit = GroupKit;
exports.MDIKit = MDIKit;
exports.MsgKit = MsgKit;
exports.SelectKit = SelectKit;
exports.SubmissionKit = SubmissionKit;
exports.TabKit = TabKit;
exports.TreeKit = TreeKit;
exports.FileKit = FileKit;