/************************************************
 * CtrlCopy.module.js
 * Created at 2021. 4. 27. 오후 1:07:57.
 *
 * Version 1.0
 * Updated Date : 2021-09-23
 * 
 * @author kjh
 ************************************************/

/*
 * 컨트롤을 복사하는 기능을 제공하는 모듈입니다.
 * 각 컨트롤과 UDC, 그룹 을 카피 할 수 있으며, 해당 컨트롤의 속성,스타일,사용자속성,바인딩,이벤트 모두 동일하게 복사합니다.
 * 모듈을 통해 복사한 컨트롤 개체를 리턴하며, 복사된 컨트롤을 스크립트를 통해 직접 추가하여 사용합니다.
 * 
 * 복사방식은 총 2가지 입니다.
 * 1. 일반 컨트롤 복사 / 그룹 컨트롤 복사
 * 		copy API 를 통해 컨트롤 복사합니다. 
 * 		컨트롤 타입이 container 여부를 따라 내부적으로 그룹복사 또는 일반컨트롤 복사로 나뉩니다
 * 		복사 가능한 컨트롤 : 인풋박스, 데이트인풋, 넘버에디터, 버튼, 마스크에디터, 아웃풋, 체크박스, 콤보박스, 라디오버튼, 체크박스그룹, 텍스트에리어, 이미지, HTML스니펫, 그룹, UDC, 서치인풋
 * 
 * 2. 그리드 컨트롤 복사
 * 		그리드는 폼레이아웃 그룹을 통해 동일한 형태로 확인 할 수 있습니다. (3가지 타입)
 * 		단, 행/열 병합은 지원하지 않습니다.
 * 
 * 		2-1) origin
 * 		런타임에 보여지는 그리드와 똑같은 형태로 복사합니다.
 * 		단, 그리드 형태와 동일하지만 폼레이아웃으로 이루어져 있습니다.
 * 
 * 		2-2) detail
 * 		그리드에서 선택된 행의 디테일 정보를 프리폼 형태로 복사합니다.
 * 		선택된 행이 없을 경우 첫번째 행을 선택하여 보여줍니다.
 * 
 * 		2-3) page
 * 		detail 에서 제공하는 동일한 형태에서 타이틀 및 페이지 컨트롤을 함께 제공합니다.
 * 		옵션을 통해 타이틀과 페이징에 대한 보임 여부, 스타일 등을 설정 할 수 있습니다.
 */

/************************************************
 * 전역변수
 ************************************************/
/**
 * 멀티헤더 유지 사용자 속성명(그리드를 폼레이아웃 형태로 카피할 때 사용)
 * true 일 경우 멀티헤더개수 만큼 셀을 생성합니다.
 * false 인 경우 한 셀에 msDelimiter 에 설정한 값을 구분자로 한 셀에 헤더 텍스트를 표현합니다.
 */
var ATTR_MAINTAIN_MULTI_HEADER = "maintain-multi-header"

/**
 * 멀티 헤더 텍스트 구분자
 */
var msDelimiter = " ";

/**
 * 복사 컨트롤 아이디 정책
 */
var CopyIdConfig = {
	ctrl: "_tmp_copy",
	grid: "_tmp_copy_grid",
}

/************************************************
 * 이벤트리스너 캐치
 ************************************************/
cpr.controls.Control.prototype._addEventListener = cpr.controls.Control.prototype.addEventListener
cpr.controls.Control.prototype.addEventListener = function(type, listener) {
	
	this._addEventListener(type, listener)
	
	if (!this._eventListenerList) this._eventListenerList = {};
	if (!this._eventListenerList[type]) this._eventListenerList[type] = [];
	
	this._eventListenerList[type].push(listener);
}

/************************************************
 * ctrlCopy
 ************************************************/
/**
 * @return {CtrlCopyModule}
 */
globals.createCtrlCopyModule = function() {
	return new CtrlCopyModule();
}

var CtrlCopyModule = function() {
	var ctrlCopier = new SingleCtrlCopier();
	var containerCopier = new ContainerCopier(ctrlCopier);
	var gridConverter = new GridConverter(ctrlCopier);
	/**
	 * 파라미터로 받은 컨트롤을 복사해 새로운 컨트롤을 리턴합니다.
	 * @param {cpr.controls.UIControl} ctrl
	 * @return {cpr.controls.UIControl}
	 */
	this.copy = function(ctrl) {
		if (ctrl.type === "container") {
			return containerCopier.copy(ctrl, gridConverter);
		} else {
			return ctrlCopier.copy(ctrl)
		}
	}
	
	/**
	 * 파라미터로 받은 그리드와 동일한 UI를가진 폼레이아웃을 생성하여 리턴합니다.
	 * @param {cpr.controls.Grid} targetGrid 
	 * @param {String} convertType? 변환 타입 (origin | detail | page), default origin
	 * @param {Number} pnRowHeight? 폼레이아웃의 행높이( convertType 이 detail 일 경우 사용)
	 * @param {
	 *   showTitle : Boolean <!-- 그리드 타이틀 및 총 행 개수를 표시합니다. ( 타이틀은 그리드의 fieldLabel 속성 값을 표시합니다, default true) -->,
	 *   titleClass : #css-class <!-- 타이틀 스타일 클래스를 설정합니다. -->,
	 *   showCount : Boolean <!-- 총 건수를 표시합니다. (default true)-->,
	 *   countClass :#css-class <!-- 총 건수 표시 스타일 클래스를 설정합니다. -->,
	 *   showPageIndex : Boolean <!-- 페이지인덱서를 추가합니다. (default true) -->,
	 *   pageIndexClass : #css-class <!-- 페이지인덱서 스타일 클래스를 설정합니다.-->,
	 *   groupClass : #css-class <!-- 타이틀, 폼레이아웃, 페이지인덱서를 포함하는 그룹의 스타일 클래스를 설정합니다. -->,
	 *   titleUDC : cpr.controls.UDCBase <!-- 타이틀에 추가 할 UDC -->,
	 *   rowIndex : Number <!-- converType=detail일때, 그리드 행 인덱스 -->
	 * } poOption? converType 이 detail / page 일 때 페이징 옵션, 
	 */
	this.convertGridToForm = function( /* cpr.controls.Grid */ targetGrid, convertType, pnRowHeight, poOption) {
		gridConverter.setGrid(targetGrid);
		
		if (!convertType || convertType === "origin") {
			return gridConverter.fillCtrl(gridConverter.buildOriginFormArea());
		} else if (convertType === "page") {
			var vcDetailForm = gridConverter.buildDetailFormArea(pnRowHeight);
			return gridConverter.buildGroupArea(vcDetailForm, poOption);
		} else {
			return gridConverter.buildDetailFormArea(pnRowHeight, poOption);
		}
		
	}
}

/**
 * Container 복사담당 class
 * @param {CtrlCopier} pSinglectrlCopier
 */
var ContainerCopier = function( /* CtrlCopier */ pSinglectrlCopier) {
	
	var singleCtrlCopier = pSinglectrlCopier;
	
	/**
	 * 그룹을 복사합니다.
	 * @param {cpr.controls.Container} originGrp
	 * @param {GridConverter} pGridCopier
	 */
	this.copy = function(originGrp, pGridCopier) {
		
		if (originGrp.type !== "container") return;
		
		/**
		 * @type {cpr.controls.Container}
		 */
		var tmpGrp = singleCtrlCopier.copy(originGrp);
		var childrens = originGrp.getChildren();
		var that = this;
		
		var gridConverter = pGridCopier;
		childrens.forEach(function(child) {
			
			var childConstraint = originGrp.getConstraint(child);
			
			if (child.type === "grid") {
				gridConverter.setGrid(child);
				var tmpGrid = gridConverter.fillCtrl(gridConverter.buildOriginFormArea());
				tmpGrp.addChild(tmpGrid, _.clone(childConstraint));
			} else {
				var tmpChild = that.buildRecursiveContainer(child);
				if (tmpChild) {
					tmpGrp.addChild(tmpChild, _.clone(childConstraint));
				}
			}
		});
		
		return tmpGrp;
	}
	/**
	 * 그룹의 모든 자식을 재귀적으로 추가시켜 리턴합니다.
	 * @param {cpr.controls.Container} ctrl
	 * @param {cpr.controls.Container} tmpCtrl
	 */
	this.buildRecursiveContainer = function(ctrl, tmpCtrl) {
		var tmpCtrl = tmpCtrl ? tmpCtrl : singleCtrlCopier.copy(ctrl, true);
		if (ctrl.type !== "container") return tmpCtrl;
		
		var children = ctrl.getChildren();
		var that = this;
		
		children.forEach(function(child) {
			var tmpChild = singleCtrlCopier.copy(child);
			
			if (tmpChild) {
				tmpCtrl.addChild(tmpChild, _.clone(ctrl.getConstraint(child)))
				if (child.type === "container") {
					that.buildRecursiveContainer(child, tmpChild);
				}
			}
			
		})
		
		return tmpCtrl;
	}
}

/**
 * @constructor
 */
var SingleCtrlCopier = function() {
	
	/**
	 * 파라미터로 받은 컨트롤과 동일한 속성을 가진 컨트롤을 리턴합니다.
	 * @param {cpr.controls.UIControl} ctrl
	 * @return {cpr.controls.UIControl}
	 */
	this.copy = function(ctrl, pbContainerCopy) {
		if (!(ctrl instanceof cpr.controls.UIControl)) return;
		
		//STEP1 - 파라미터로 받은 컨트롤의 생성자함수로 동일한 유형의 컨트롤 생성
		var tmpObj;
		var vsCopyId = _makeCopyCtrlId(ctrl);
		
		if (ctrl instanceof cpr.controls.UDCBase) {
			var udcTypeSplit = ctrl.type.split(".");
			var udcType = udcTypeSplit.splice(1, udcTypeSplit.length);
			
			udcType.forEach(function(each) {
				if (!tmpObj) tmpObj = udc[each];
				else if (tmpObj[each] instanceof Function) tmpObj = new tmpObj[each](vsCopyId);
				else tmpObj = tmpObj[each];
			});
			
		} else {
			/**
			 * @type cpr.controls.UIControl
			 */
			tmpObj = new ctrl.constructor(vsCopyId);
		}
		
		var userDataKeys = Object.keys(ctrl.userData());
		var userAttrKeys = Object.keys(ctrl.userAttr());
		
		//STEP2 - 기본속성 복사(현재 CtrlPropertyConfig객체에서 받아오고있는 property정보를 스크립트로 긁어올 수 있는지 확인 필요)
		//      - BasePropertyFactory에 프로퍼티 복사작업 위임 [2021-04-28 kjh]
		var copyProp = BasePropertyFactory.copyProperty(tmpObj.type, ctrl, tmpObj);
		if (!copyProp) {
			if (!pbContainerCopy || pbContainerCopy === false) {
				alert(ctrl.type + " 은(는) 복사를 지원하지 않습니다.");
			}
			tmpObj = null;
			return;
		}
		
		//STEP3 - userData복사(userData에 Object값도 들어갈 수 있으므로 얕은복사로 진행해도 되는지 확인필요)
		userDataKeys.forEach(function(key) {
			tmpObj.userData(key, _.clone(ctrl.userData(key)));
		});
		
		//STEP4 - userAttr복사
		userAttrKeys.forEach(function(key) {
			tmpObj.userAttr(key, _.clone(ctrl.userAttr(key)));
		});
		
		//STEP5 - style복사
		tmpObj.style.setClasses(ctrl.style.getClasses());
		tmpObj.style.css(ctrl.style.css());
		var classBindInfo = ctrl.style.getClassBindInfo();
		if(classBindInfo && classBindInfo.expression) {
			// class 가 익스프레션 바인딩 되어 있는 경우 복사
			tmpObj.style.bindClass().toExpression(classBindInfo.expression);	
		}
		
		//STEP6- 이벤트정보 복사 -> exbuilder API로 이벤트등록정보를 가져오는 방법을 찾지 못해 addEventListener를 hooking하여 임시구현
		if (ctrl._eventListenerList) {
			Object.keys(ctrl._eventListenerList).forEach(function(type) {
				var listeners = ctrl._eventListenerList[type];
				listeners.forEach(function(each) {
					tmpObj.addEventListener(type, each);
				})
			});
		}
		
		return tmpObj;
	}
	
}

/**
 * 동적으로 컨트롤의 property 를 리스트형태로 반환합니다.
 * @param {cpr.controls.UIControl} ctrl
 * @return {Array} property 항목 리스트
 */
function getCtrlPropertyList(ctrl) {
	
	if (ctrl.value == null) ctrl.value = "";
	
	var objectToInspect;
	var result = [];
	
	for (objectToInspect = ctrl; objectToInspect !== null; objectToInspect = Object.getPrototypeOf(objectToInspect)) {
		result = result.concat(Object.getOwnPropertyNames(objectToInspect));
	}
	
	var vaDClnArr = ["dateValue"]; // 복사하지 않을 속성명 배열
	result = result.filter(function(each) {
		/*
		 * 1) _ 로 시작하는 내부 api 제거
		 * 2) Function 제거
		 * 3) vaDClnArr 에 해당되지 않은 속성
		 * 4) 난독화 된 속성 제거
		 */
		if (each.indexOf("_") === -1 && !(ctrl[each] instanceof Function) && vaDClnArr.indexOf(each) == -1 && each.indexOf("µ") == -1) return each;
	})
	
	result = _.uniq(result);
	
	return result;
}

var BindConfig = {
	context: {
		funcNm: [cpr.bind.GridSelectionContext, cpr.bind.ItemSelectionContext]
	},
	expression: {
		funcNm: "toExpression",
		propNm: ["expression"]
	},
	datacolumn: {
		funcNm: "toDataColumn",
		propNm: ["columnName"]
	},
	datamap: {
		funcNm: "toDataMap",
		propNm: ["srcId", "columnName"]
	}
}
/**
 * 컨트롤 프로퍼티 복사를 담당하는 변수입니다.
 */
var BasePropertyFactory = {
	
	propertyStrategy: {
		inputbox: new ControlPropertyStrategy("inputbox"),
		dateinput: new ControlPropertyStrategy("dateinput"),
		numbereditor: new ControlPropertyStrategy("numbereditor"),
		button: new ControlPropertyStrategy("button"),
		maskeditor: new ControlPropertyStrategy("maskeditor"),
		output: new ControlPropertyStrategy("output"),
		checkbox: new ControlPropertyStrategy("checkbox"),
		combobox: new ControlPropertyStrategy("combobox").setStrategy(DataSetBindStrategy),
		radiobutton: new ControlPropertyStrategy("radiobutton").setStrategy(DataSetBindStrategy),
		checkboxgroup: new ControlPropertyStrategy("checkboxgroup").setStrategy(DataSetBindStrategy),
		textarea: new ControlPropertyStrategy("textarea"),
		image: new ControlPropertyStrategy("image").setStrategy(DataSetBindStrategy),
		htmlsnippet: new ControlPropertyStrategy("htmlsnippet")
			.setStrategy(function(sourceCtrl, targetCtrl) {
				targetCtrl.value = sourceCtrl.value;
			}),
		container: new ControlPropertyStrategy("container").setStrategy(ContainerStrategy),
		"cpr.controls.UDCBase": new ControlPropertyStrategy("cpr.controls.UDCBase").setStrategy(UdcStrategy),
		searchinput: new ControlPropertyStrategy("searchinput")
		
	},
	
	/**
	 * 컨트롤별 속성을 복사합니다.
	 * @param {String} type
	 * @param {cpr.controls.UIControl} sourceCtrl
	 * @param {cpr.controls.UIControl} targetCtrl
	 */
	copyProperty: function(type, sourceCtrl, targetCtrl) {
		var that = this;
		/**
		 * @type {ControlPropertyStrategy}
		 */
		var propStrategy = sourceCtrl instanceof cpr.controls.UDCBase ? that.propertyStrategy["cpr.controls.UDCBase"] : that.propertyStrategy[type];
		if (!propStrategy) {
			return false;
		}
		
		propStrategy.setControls(sourceCtrl, targetCtrl);
		propStrategy.copyBaseProperty();
		propStrategy.copyControlProperty();
		propStrategy.copyBindInfo();
		
		return true;
	}
}

/**
 * 각 컨트롤마다 수행되야하는 로직을 담은 클래스입니다.
 * @class
 * @param {String} type
 */
function ControlPropertyStrategy(type) {
	this.sourceCtrl = null;
	this.targetCtrl = null;
	var that = this;
	this.strategy = function() {}
	
	/**
	 * @type {Array}
	 */
	this.property;
	
	/**
	 * 컨트롤에 대한 참조를 설정합니다.
	 * @param {cpr.controls.UIControl} sourceCtrl 원본 컨트롤
	 * @param {cpr.controls.UIControl} targetCtrl 복사대상 컨트롤
	 */
	this.setControls = function(sourceCtrl, targetCtrl) {
		this.sourceCtrl = sourceCtrl
		this.targetCtrl = targetCtrl;
		this.property = getCtrlPropertyList(targetCtrl);
	}
	
	/**
	 * 컨트롤별 개별로 설정해야하는 작업이 있는경우 실행대상함수를 설정합니다.
	 * @param {Function} strategy
	 */
	this.setStrategy = function(strategy) {
		this.strategy = strategy;
		return that;
	}
	/**
	 * 컨트롤별 기본 프로퍼티를 복사합니다(편집기 > Properties > 일반의 속성들)
	 */
	this.copyBaseProperty = function() {
		
		var that = this;
		
		if (this.property.indexOf("displayText") != -1) {
			/*
			 * 인풋계열 컨트롤
			 * length 속성 복사 시 오류 방지
			 */
			if (that.sourceCtrl.value == null) that.sourceCtrl.putValue("");
		}
		
		var vbDisplayExp = this.property.indexOf("displayExp") != -1;
		
		this.property.forEach(function(prop) {
			//2-1. 기본 속성들 값 복사
			if(prop.slice(-4) === "Date") {
				// Date 타입의 속성을 복사하는 경우
				that.targetCtrl[prop] = new Date(that.sourceCtrl[prop].getTime());
			} else if (vbDisplayExp && prop == "text") {
				// displayExp 속성이 있는 컨트롤일 경우 text 속성을 복사 방지
			} else {
				that.targetCtrl[prop] = _.clone(that.sourceCtrl[prop]);
			}
			
		});
	}
	
	/**
	 * 컨트롤별 바인드정보를 복사합니다
	 */
	this.copyBindInfo = function() {
		
		var props = [];
		
		if (this.sourceCtrl instanceof cpr.controls.UDCBase) {
			Object.keys(this.sourceCtrl.getAllAppProperties()).forEach(function(key) {
				props.push(key);
			})
			props = props.concat(this.property)
		} else {
			props = this.property;
		}
		
		// 속성 바인딩
		props.forEach(function(prop) {
			var bindInfo = that.sourceCtrl.getBindInfo(prop);
			if (bindInfo) {
				var args = [];
				var bindConfig = BindConfig[bindInfo.type];
				bindConfig.propNm.forEach(function(each) {
					if (each === "srcId") args.push(that.sourceCtrl.getParent().getAppInstance().lookup(bindInfo[each]))
					else args.push(bindInfo[each]);
				})
				that.targetCtrl.bind(prop)[bindConfig.funcNm].apply(that.targetCtrl.bind(prop), args)
			}
		});
		
		// 문맥 바인딩 (선택행 컨텍스트)
		var voBindContext = that.sourceCtrl.getBindContext();
		var contextConfig = BindConfig.context;
		contextConfig.funcNm.forEach(function(each) {
			if (voBindContext instanceof each) {
				that.targetCtrl.setBindContext(new each(voBindContext.grid || voBindContext._control));
			}
		});
	}
	
	/**
	 * 각 컨트롤별 등록한 개별처리함수를 수행합니다.
	 */
	this.copyControlProperty = function() {
		this.strategy(this.sourceCtrl, this.targetCtrl);
	}
}

/**
 * 데이터셋이 바인딩 될 수 있는 컨트롤을 처리하기 위한 함수입니다.
 * @param {any} sourceCtrl
 * @param {any} targetCtrl
 */
function DataSetBindStrategy(sourceCtrl, targetCtrl) {
	//선행데이터 복사
	sourceCtrl.getItems().forEach(function(each) {
		if (!(each.row)) {
			targetCtrl.addItem(each);
		}
	})
	//데이터셋 연결
	if (sourceCtrl.itemSetConfig) {
		targetCtrl.setItemSet(sourceCtrl.dataSet, sourceCtrl.itemSetConfig)
	}
}

/**
 * 레이아웃을 복사 합니다.
 * @param {cpr.controls.Container} sourceCtrl
 * @param {cpr.controls.Container} targetCtrl
 */
function ContainerStrategy(sourceCtrl, targetCtrl) {
	var layout = sourceCtrl.getLayout();
	var layoutType = layout.constructor;
	
	var newLayout = new layoutType();
	if(newLayout instanceof cpr.controls.layouts.FormLayout) {
		(function(/* cpr.controls.layouts.FormLayout */ origin, /* cpr.controls.layouts.FormLayout */ newLayout) {
			// 레이아웃 별 속성 정보 복사
			getCtrlPropertyList(origin).forEach(function(each){
				newLayout[each] = origin[each];
			});
			// 각 행/컬럼 구조 설정(너비, 자동크기여부 등)
			origin.getColumns().forEach(function(each, i){
				newLayout.setColumnAutoSizing(i, origin.isColumnAutoSizing(i));
			});
			origin.getRows().forEach(function(each, i){
				newLayout.setRowAutoSizing(i, origin.isRowAutoSizing(i));
			});
			newLayout.setColumns(origin.getColumns());
			newLayout.setRows(origin.getRows());
			newLayout.setColumnDivisions(origin.getColumnDivisions());
			newLayout.setRowDivisions(origin.getRowDivisions());
		})(layout, newLayout);
		
	} else if(newLayout instanceof cpr.controls.layouts.VerticalLayout){
		(function(/* cpr.controls.layouts.VerticalLayout */ origin, /* cpr.controls.layouts.VerticalLayout */ newLayout) {
			getCtrlPropertyList(origin).forEach(function(each){
				newLayout[each] = origin[each];
			});
		})(layout, newLayout);
		
	} else if(newLayout instanceof cpr.controls.layouts.FlowLayout){
		(function(/* cpr.controls.layouts.FlowLayout */ origin, /* cpr.controls.layouts.FlowLayout */ newLayout) {
			getCtrlPropertyList(origin).forEach(function(each){
				newLayout[each] = origin[each];
			});
		})(layout, newLayout);
	} else {
		return null;
	}
	
	targetCtrl.setLayout(newLayout);
}

/**
 * 
 * @param {cpr.controls.UDCBase} sourceCtrl
 * @param {cpr.controls.UDCBase} targetCtrl
 */
function UdcStrategy(sourceCtrl, targetCtrl) {
	
	var voAppProperties = sourceCtrl.getAllAppProperties();
	Object.keys(voAppProperties).forEach(function(key) {
		var value = voAppProperties[key];
		
		targetCtrl.setAppProperty(key, value);
	});
}

/**
 * 그리드를 변환합니다
 * @param {SingleCtrlCopier} ctrlCopier
 */
var GridConverter = function(ctrlCopier) {
	
	var copier = ctrlCopier
	var gridParent;
	var gridColumnLayout;
	var gridHeaderBand;
	var gridDetailBand;
	var headerRowCount;
	
	var targetGrid;
	var vbMaintainHeader = true;
	
	var that = this;
	
	/**
	 * 
	 * @param {cpr.controls.Grid} poTargetGrid
	 */
	this.setGrid = function(poTargetGrid) {
		targetGrid = poTargetGrid;
		gridParent = poTargetGrid.getParent();
		gridColumnLayout = poTargetGrid.getColumnLayout(true);
		gridHeaderBand = poTargetGrid.header;
		gridDetailBand = poTargetGrid.detail;
		headerRowCount = gridHeaderBand.getRowHeights().length;
		
		vbMaintainHeader = targetGrid.userAttr(ATTR_MAINTAIN_MULTI_HEADER) == "false" ? false : true;
	}
	
	this.buildOriginFormArea = function() {
		var vsCopyId = _makeCopyCtrlId(targetGrid);
		var tmpFormGrp = new cpr.controls.Container(vsCopyId);
		var layout = new cpr.controls.layouts.FormLayout();
		var columnWidthSum = 0;
		var allColumnWidthSum = 0;
		var columnRatio = [];
		
		tmpFormGrp.setLayout(layout);
		tmpFormGrp.style.addClass("cl-grid");
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.bottomMargin = 0;
		layout.leftMargin = 0;
		layout.rightMargin = 0;
		layout.topMargin = 0;
		
		//STEP1. row, column빌드
		var columnWidths = [];
		var rowHeights = [];
		
		gridHeaderBand.getRowHeights().forEach(function(rowHeight) {
			rowHeights.push(rowHeight.height + "px");
		});
		for (var i = 0; i < targetGrid.getDataRowCount(); i++) {
			rowHeights = rowHeights.concat(gridDetailBand.getRowHeights().map(function(each) {
				return each.height + "px";
			}));
		}
		layout.setRows(rowHeights);
		
		gridColumnLayout.columnLayout.forEach(function(each) {
			if (each.autoFit) {
				columnWidthSum += each.width;
			}
			allColumnWidthSum += each.width
			columnWidths.push(each.width + "px");
		});
		
		columnWidths.forEach(function(each, i) {
			if (gridColumnLayout.columnLayout[i].autoFit) {
				columnRatio.push({
					index: i,
					ratio: (parseInt(each.replace("px", "")) / columnWidthSum)
				});
			}
		});
		
		layout.setColumns(columnWidths);
		
		if (allColumnWidthSum <= gridParent.getActualRect().width) {
			//1-1. autoFit컬럼 대상으로 width 리사이즈작업
			var columnSpace = gridParent.getActualRect().width - allColumnWidthSum;
			var autoFitColumn = [];
			gridColumnLayout.columnLayout.forEach(function(each, i) {
				if (each.autoFit) {
					autoFitColumn.push(i);
				}
			});
			var originColumns = layout.getColumns();
			var filledColumns = []
			originColumns.forEach(function(each, index) {
				if (_.contains(autoFitColumn, index)) {
					var ratioInfo = columnRatio.find(function(each) {
						if (each.index === index) return true
					});
					
					filledColumns.push(Math.floor(parseInt(each.replace("px", "")) + (Math.floor(ratioInfo.ratio * 10000) / 10000 * columnSpace)) + "px");
				} else {
					filledColumns.push(each);
				}
			});
			
			layout.setColumns(filledColumns);
		} else {
			tmpFormGrp.style.css("overflow", "hidden")
		}
		
		return tmpFormGrp;
	}
	
	/**
	 * 
	 * @param {cpr.controls.Container} tmpFormGrp
	 */
	this.fillCtrl = function(tmpFormGrp) {
		var cellContainer = new cpr.controls.Container();
		var cellLayout = new cpr.controls.layouts.XYLayout();
		cellContainer.setLayout(cellLayout);
		//STEP2. header, detail컨트롤 채워넣기
		targetGrid.forEachOfGridCells(function(cellInfo) {
			
			cellInfo.forEach(function(cell) {
				if (cell.region === "header") { //header
					cellContainer = new cpr.controls.Container();
					cellLayout = new cpr.controls.layouts.XYLayout();
					cellContainer.setLayout(cellLayout);
					
					var headerOpt = new cpr.controls.Output();
					headerOpt.value = cell.text;
					headerOpt.style.addClass("cl-grid-header");
					
					cellContainer.addChild(headerOpt, {
						top: 0,
						bottom: 0,
						left: 0,
						right: 0
					});
					cellContainer.style.addClass("cl-grid-cell");
					
					tmpFormGrp.addChild(cellContainer, {
						colIndex: cell.colIndex,
						colSpan: cell.colSpan,
						rowIndex: cell.rowIndex,
						rowSpan: cell.rowSpan
					});
				} else if (cell.region === "middle") {
					var originCtrl = cell.ctrl;
					var copiedCtrl;
					var cellContainer = new cpr.controls.Container();
					var cellLayout = new cpr.controls.layouts.XYLayout();
					var befRowCtrl
					
					if (originCtrl) {
						copiedCtrl = copier.copy(originCtrl)
					} else {
						copiedCtrl = new cpr.controls.Output();
						copiedCtrl.value = cell.text;
						copiedCtrl.style.css("text-align", "center");
					}
					copiedCtrl.style.css("border", "none");
					cellContainer.addChild(copiedCtrl, {
						top: 0,
						bottom: 0,
						left: 0,
						right: 0
					});
					cellContainer.style.addClass("cl-grid-cell");
					
					if (cell.dsRowIndex !== 0) {
						befRowCtrl = tmpFormGrp.getLayout().findControls({
							colIndex: cell.colIndex,
							colSpan: cell.colSpan,
							rowIndex: cell.dsRowIndex + headerRowCount - 1,
							rowSpan: cell.rowSpan
						})
					}
					
					if (gridDetailBand.getColumn(cell.cellIndex).suppressible && befRowCtrl && befRowCtrl[0].getFirstChild().value === copiedCtrl.value) {
						var originConstraint = befRowCtrl[0].getParent().getConstraint(befRowCtrl[0]);
						
						if (targetGrid.suppressedCellType === "split") {
							befRowCtrl[0].getFirstChild().style.css("vertical-align", "top")
						}
						tmpFormGrp.replaceConstraint(befRowCtrl[0], {
							colIndex: originConstraint.colIndex,
							colSpan: originConstraint.colSpan,
							rowIndex: originConstraint.rowIndex,
							rowSpan: originConstraint.rowSpan + 1
						});
					} else {
						tmpFormGrp.addChild(cellContainer, {
							colIndex: cell.colIndex,
							colSpan: cell.colSpan,
							rowIndex: cell.dsRowIndex + headerRowCount,
							rowSpan: cell.rowSpan
						});
					}
				}
			});
		});
		
		return tmpFormGrp
	}
	
	this.buildDetailFormArea = function(pnRowHeight, poOption) {
		var columnWidths = [];
		var grpColumnLayout = []
		var grpRowLayout = [];
		var rowsHeight = gridHeaderBand.getRowHeights();
		
		var vsCopyId = _makeCopyCtrlId(targetGrid);
		var tmpFormGrp = new cpr.controls.Container(vsCopyId);
		var layout = new cpr.controls.layouts.FormLayout();
		layout.horizontalSpacing = "0px";
		layout.verticalSpacing = "0px";
		tmpFormGrp.setLayout(layout);
		tmpFormGrp.style.addClass("cl-grid");
		
		// 선택행이 없을 경우 첫번째 로우 선택
		if (targetGrid.getSelectedRowIndex() === -1) {
			targetGrid.selectRows(0);
		}
		
		// setColumns
		var vaColVisible = [];
		gridColumnLayout.columnLayout.forEach(function(each) {
			columnWidths.push(each.width);
			vaColVisible.push(each.visible);
		}); 

		if (vbMaintainHeader) {
			rowsHeight.forEach(function(each) {
				grpColumnLayout.push(Math.max.apply(null, columnWidths) + "px");
			});
		} else {
			grpColumnLayout.push(Math.max.apply(null, columnWidths) + "px");
		}
		grpColumnLayout.push("1fr");
		layout.setColumns(grpColumnLayout);
		
		// setRows
		columnWidths.forEach(function(each, idx) {
			if (pnRowHeight == null) {
				var rowHeight = Math.max.apply(null, rowsHeight.map(function(each) {
					return each.height;
				}));
				grpRowLayout.push(rowHeight + "px");
			} else {
				grpRowLayout.push(pnRowHeight + "px");
			}
		});
		layout.setRows(grpRowLayout);
		
		// visible=false 컬럼 숨기기
		vaColVisible.forEach(function(each, idx) {
			layout.setRowVisible(idx, each);
		}); 
		
		// 그리드 선택행 바인딩
		if(poOption && !ValueUtil.isNull(poOption.rowIndex)) {
			tmpFormGrp.setBindContext(new cpr.bind.DataRowContext(targetGrid.dataSet, poOption.rowIndex));
		} else {
			tmpFormGrp.setBindContext(new cpr.bind.GridSelectionContext(targetGrid));
		}
		
		var cellContainer = new cpr.controls.Container();
		var cellLayout = new cpr.controls.layouts.XYLayout();
		cellContainer.setLayout(cellLayout);
		
		var columnCount = targetGrid.columnCount;
		for (var idx = 0; idx < columnCount; idx++) {
			if (!vbMaintainHeader) {
				// 한 셀에 멀티 헤더 텍스트 합침
				cellContainer = new cpr.controls.Container();
				cellLayout = new cpr.controls.layouts.FormLayout();
				cellLayout.setColumns(["1fr"]);
				cellLayout.setRows(["1fr"]);
				cellContainer.setLayout(cellLayout);
				
				var vsHeaderTxt = _getColumnText(targetGrid, idx);
				var headerOpt = new cpr.controls.Output();
				headerOpt.value = vsHeaderTxt;
				headerOpt.style.css("padding", "5px");
				headerOpt.style.addClass("cl-grid-header");
				
				cellContainer.addChild(headerOpt, {
					top: 0,
					bottom: 0,
					left: 0,
					right: 0
				});
				cellContainer.style.addClass("cl-grid-cell");
				// last-child border-right:none 일 경우 보더 추가
				cellContainer.style.addClass("border-right");
				
				tmpFormGrp.addChild(cellContainer, {
					colIndex: 0,
					rowIndex: idx,
				});
			}
		}
		
		var vnMergeCol = false;
		targetGrid.forEachOfGridCells(function(cellInfo) {
			cellInfo.forEach(function(cell) {
				if (cell.region === "header" && vbMaintainHeader) {
					// 멀티 헤더 구조대로 셀 유지(멀티헤더 개수만큼 셀 생성)
					cellContainer = new cpr.controls.Container();
					cellLayout = new cpr.controls.layouts.FormLayout();
					cellLayout.setColumns(["1fr"]);
					cellLayout.setRows(["1fr"]);
					cellContainer.setLayout(cellLayout);
					
					var headerOpt = new cpr.controls.Output();
					headerOpt.value = cell.text;
					headerOpt.style.css("padding", "5px");
					headerOpt.style.addClass("cl-grid-header");
					
					cellContainer.addChild(headerOpt, {
						"colIndex": 0,
						"rowIndex": 0
					});
					cellContainer.style.addClass("cl-grid-cell");
					// last-child border-right:none 일 경우 보더 추가
					cellContainer.style.addClass("border-right");
					
					tmpFormGrp.addChild(cellContainer, {
						colIndex: cell.rowIndex,
						colSpan: cell.rowSpan,
						rowIndex: cell.colIndex,
						rowSpan: cell.colSpan
					});
				} else if (cell.region === "middle") {
					if (cell.dsRowIndex === targetGrid.getSelectedRowIndex()) {
						cellContainer = new cpr.controls.Container();
						cellLayout = new cpr.controls.layouts.FormLayout();
						cellLayout.setColumns(["1fr"]);
						cellLayout.setRows(["1fr"]);
						cellContainer.setLayout(cellLayout);
						
						var originCtrl = cell.ctrl;
						var copiedCtrl;
						var befColCtrl;
						
						if (originCtrl) {
							copiedCtrl = copier.copy(originCtrl);
						} else {
							copiedCtrl = new cpr.controls.Output();
							copiedCtrl.style.css("text-align", "center");
							if (cell.columnName != "" && cell.columnName != null && cell.columnName != undefined) {
								copiedCtrl.bind("value").toDataColumn(cell.columnName);
							} else {
								if (cell.columnType == "rowindex") {
									copiedCtrl.bind("value").toExpression("rowIndex + 1");
								} else {
									copiedCtrl.value = cell.text;
								}
							}
						}
						
						copiedCtrl.style.css("border", "none");
						
						cellContainer.style.css("padding", "5px");
						cellContainer.style.addClass("cl-grid-cell");
						// last-child border-right:none 일 경우 보더 추가
						cellContainer.style.addClass("border-right");
						cellContainer.setLayout(cellLayout);
						cellContainer.addChild(copiedCtrl, {
							"colIndex": 0,
							"rowIndex": 0
						});
						
						var vnColIndex = vbMaintainHeader ? rowsHeight.length : 1;
						var vcBeforeCtrl = tmpFormGrp.getLayout().findControls({
							rowIndex: cell.colIndex,
							colIndex: 0
						})[0];
						var index = tmpFormGrp.getChildren().indexOf(vcBeforeCtrl)+1;
						tmpFormGrp.insertChild(index, cellContainer, {
							colIndex: vnColIndex,
							colSpan: cell.rowSpan,
							rowIndex: cell.colIndex,
							rowSpan: cell.colSpan
						});
					}
				}
			});
		});
		
		layout.getRows().forEach(function(each, idx) {
			layout.setRowAutoSizing(idx, true);
		});
		
		return tmpFormGrp;
	}
	
	this.buildGroupArea = function(pcForm, option) {
		
		var vbShowPageIndex = option.showPageIndex != null ? option.showPageIndex : true;
		var vbShowTitle = option.showTitle != null ? option.showTitle : true;
		var vbShowCount = option.showCount != null ? option.showCount : true;
		
		var vsGrpClass = option.groupClass;
		var vsTitleClass = option.titleClass;
		var vsCountClass = option.countClass;
		var vsPageClass = option.pageIndexClass;
		var vcTitleUDC = option.titleUDC;
		
		var vcDetailForm = pcForm;
		
		// STEP1) 전체 그룹생성
		var tempGrp = new cpr.controls.Container();
		var grpLayout = new cpr.controls.layouts.VerticalLayout();
		grpLayout.topMargin = 10;
		grpLayout.leftMargin = 10;
		grpLayout.rightMargin = 10;
		grpLayout.bottomMargin = 10;
		grpLayout.spacing = 10;
		tempGrp.setLayout(grpLayout);
		
		if (vsGrpClass) {
			tempGrp.style.addClass(vsGrpClass);
		}
		
		// STEP2) 타이틀 생성
		var tempTitle = new cpr.controls.Container();
		var titleLayout = new cpr.controls.layouts.FlowLayout();
		titleLayout.topMargin = 0;
		titleLayout.leftMargin = 0;
		titleLayout.rightMargin = 0;
		titleLayout.bottomMargin = 0;
		titleLayout.horizontalSpacing = 10;
		titleLayout.verticalSpacing = 10;
		titleLayout.verticalAlign = "middle";
		titleLayout.lineWrap = false;
		titleLayout.scrollable = false;
		tempTitle.setLayout(titleLayout);
		
		if (vbShowTitle) {
			
			// 2-1) 타이틀
			if (vcTitleUDC) {
				// TODO 앱속성을 설정하세요.
				//				vcTitleUDC.ctrl = targetGrid;
				//				vcTitleUDC.title = targetGrid.fieldLabel;
				//				vcTitleUDC.rowCount = targetGrid.getRowCount();
				
				tempGrp.addChild(vcTitleUDC, {
					autoSize: "height"
				});
			} else {
				
				var gridTItle = new cpr.controls.Output();
				gridTItle.value = targetGrid.fieldLabel;
				if (vsTitleClass) {
					gridTItle.style.addClass(vsTitleClass);
				}
				tempTitle.addChild(gridTItle, {
					autoSize: "width",
					height: "100%"
				});
			}
		}
		
		if (vbShowCount) {
			
			// 2-2) 건수
			var count = new cpr.controls.Output();
			count.value = targetGrid.getRowCount();
			count.displayExp = "'[' + text + ']'";
			if (vsCountClass) count.style.addClass(vsCountClass);
			
			tempTitle.addChild(count, {
				autoSize: "width",
				height: "100%"
			});
		}
		
		if (vbShowTitle || vbShowCount) {
			tempGrp.addChild(tempTitle, {
				autoSize: "height"
			});
		}
		
		// STEP3) 디테일 폼레이아웃 추가
		tempGrp.addChild(vcDetailForm, {
			autoSize: "height"
		});
		
		// STEP4) 페이지인덱서 생성
		if (vbShowPageIndex) {
			
			var tempPage = new cpr.controls.PageIndexer();
			tempPage.pageRowCount = 1;
			tempPage.startPageIndex = targetGrid.getSelectedRowIndex() + 1;
			tempPage.currentPageIndex = targetGrid.getSelectedRowIndex() + 1;
			tempPage.totalRowCount = targetGrid.getRowCount();
			tempPage.addEventListener("selection-change", function(e) {
				targetGrid.selectRows(e.newSelection - 1);
			});
			if (vsPageClass) {
				tempPage.style.addClass(vsPageClass);
			}
			
			tempGrp.addChild(tempPage, {
				autoSize: "height"
			});
		}
		
		return tempGrp;
	}
}

/**
 * 그리드 헤더 텍스트 반환
 * 멀티헤더의 경우 "-" 로 연결되어 반환
 * @param {cpr.controls.Grid} pcGrid
 * @param {Number} pnIndex
 */
function _getColumnText(pcGrid, pnIndex) {
	
	var headerCell = pcGrid.getHeaderCellIndices(pnIndex);
	
	var vsResult = "";
	if (headerCell.length > 0) {
		headerCell.forEach(function(each) {
			var colNm = pcGrid.header.getColumn(each);
			vsResult += (colNm.text || "") + msDelimiter;
		});
		
		if (vsResult != "") {
			vsResult = vsResult.substr(0, vsResult.length - 1);
		}
	}
	
	return vsResult;
}

/**
 * 
 * @param {any} ctrl
 */
function _makeCopyCtrlId(ctrl) {
	
	if (!(ctrl instanceof cpr.controls.UIControl)) return;
	
	var copyString = "";
	
	// grid 일 때
	if (ctrl.type == "grid") {
		var vnRowIndex = ctrl.getSelectedRowIndex();
		copyString = CopyIdConfig[ctrl.type] + "_R" + vnRowIndex;
	} else {
		copyString = CopyIdConfig["ctrl"];
	}
	
	var tmpCtrlId = ctrl.id ? ctrl.id + copyString : undefined;
	
	return tmpCtrlId;
}