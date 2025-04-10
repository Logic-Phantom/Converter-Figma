// 의존 모듈 선언.
module.depends("module/standard/common");

/**
 * 공통 Validator Class
 */
Validator = function(appKit) {
	/** @type AppKit */
	this._appKit = appKit;
};

/**
 * 유효성 체크
 * @desc 사이트별 Customizing 필요
 *       각 Step에서 요구된 사용자 속성 별 프로젝트에서 활용될 사용자 속성 등록(eXBuilder6 프로젝트 표준 > 사용자 속성 정의)  
 * @param {cpr.controls.UIControl} ctrl UI 컨트롤
 * @param {String} ctrlValue 컨트롤 value
 * @param {cpr.controls.UIControl} poParentCtl 부모 컨트롤
 * @param {Number} pnIdx
 * @param {Number} pnCellIdx
 * @param {cpr.data.IDataRow} poRow
 * @param {Boolean} isMsgAlert validation 메시지 출력 여부
 */
Validator.prototype.validate = function(ctrl, ctrlValue, poParentCtl, pnIdx, pnCellIdx, poRow, isMsgAlert) {
	var _appKit = this._appKit;
	
	if(!ctrl) return true;
	if(ctrl.type == "output" || ctrl.type == "img" || ctrl.type == "button") return true;
	
	isMsgAlert = isMsgAlert == null ? true : isMsgAlert;
	
	// 바인딩 및 헤더컬럼으로 수정 필요
	var vsFieldLabel = ctrl.fieldLabel;
	if(ValueUtil.isNull(vsFieldLabel) && ctrl.getHost){
		vsFieldLabel = ctrl.getHost().fieldLabel;
	}
	
	/** 그리드 내부 컨트롤의 경우 헤더 텍스트 반환 */
	function getGridFieldLabel(poParentCtl, psFieldLabel, pnDetailCellIndex){
		if (poParentCtl instanceof cpr.controls.Grid) {
			var vnCellIdx = pnDetailCellIndex || pnCellIdx;
			var vcDetailCtl = poParentCtl.detail.getColumn(vnCellIdx);
			var vnHeaderCellIndex = poParentCtl.getHeaderCellIndices(vcDetailCtl.cellIndex);
			
			if (vnHeaderCellIndex.length > 0) {
				var result = [];
				var vsDelimiter = "/";
				vnHeaderCellIndex.forEach(function(each) {
					var voHCol = poParentCtl.header.getColumn(each);
					result.push(ValueUtil.fixNull(voHCol.text));
				});
				if(result.length > 0) {
					psFieldLabel = result.join(vsDelimiter);
				}
			}
		}
		return psFieldLabel;
	}	
	
	/** 메세지 다이얼로그 출력 */
	function parentValidMsg(psMsg, poParentCtl, pnIdx){
		//그리드 내 컨트롤
		if(poParentCtl instanceof cpr.controls.Grid){
			var vsMsg = _appKit.Msg.getMsg("WRN-M002", [poParentCtl.fieldLabel, Number(pnIdx)+1]);
			psMsg = vsMsg + " " + psMsg;
		}
		if(isMsgAlert){
			var poOptions = {
				"confirmCallback": function() {
					ctrl.focus();
				},
				"msgStateType": "WARNING"
			};
			_appKit.Msg.alertDlg(ctrl.getAppInstance(), psMsg, null, poOptions);
		}
	}
	
	/* STEP1 : 필수 입력 체크(일반 컨트롤 사용자 속성 : required or UDC 출판 속성 : required */
	{
		var notnull = "";
		if(poParentCtl instanceof cpr.controls.Grid && ctrl instanceof cpr.controls.UDCBase){
			notnull = ctrl.getAppProperty("required") === true || ctrl.getAppProperty("required") === "Y" ? "Y" : "";
		}else{
			/* 커스텀 옵션 : 컨트롤의 사용자 속성 "required"를 통한 필수 체크  */
			notnull = ctrl.userAttr("required");
			
			var vsRequiredClass = "required";
			if(typeof AppProperties !== 'undefined' && AppProperties.VALID_REQUIRED_CLASS){
				vsRequiredClass = AppProperties.VALID_REQUIRED_CLASS;
			}
			var vaRequiredClass = vsRequiredClass.split(",").map(function(each){
				return each.trim();
			});
			
			/* 커스텀 옵션 : 컨트롤의 Style Class "required"를 통한 필수 체크, 그리드에 포함된 컨트롤이 익스프레션 바인딩을 통해 필수 스타일이 적용되는 경우 활용   */
			if (poParentCtl && poParentCtl instanceof cpr.controls.Grid) {
				var voCtrlClassBindInfo = ctrl.style.getClassBindInfo();
				if (voCtrlClassBindInfo && voCtrlClassBindInfo.type == "expression") {
					
					var expr = new cpr.expression.Expression(voCtrlClassBindInfo.expression);
					var row = poParentCtl.getRow(pnIdx);
					var result = expr.evaluate(row);
					if(vaRequiredClass.indexOf(result) != -1) {
						notnull = "Y";
					}
				}
			} else {
				/* 커스텀 옵션 : 컨트롤의 Style Class "required"를 통한 필수 체크 , Label(ouput) 이외에 컨트롤에도 필수 스타일이 적용되는 경우 활용  */
				var vaStyleNm = ctrl.style.getClasses();
				for(var i=0; i < vaStyleNm.length; i++){
					var vsStyleNm = vaStyleNm[i];
					if(vaRequiredClass.indexOf(vsStyleNm) != -1){
						notnull = "Y";
						break;
					}
				}
			}
			
			/* 
			 * 커스텀 옵션 : 컨트롤의 fieldLabel에 적시된 label(output) ID 컨트롤의 Style Class : required를 통한 필수 체크
			 * 			 웹 접근성을 고려하는 경우 aria-label에 유의미한 값이 할당되지 않으므로 해당 방식을 권장하지 않습니다.
			 */
//			var vsLabelId = ctrl.fieldLabel;
//			var voCtrlLabelobj = ctrl.getAppInstance().lookup(vsLabelId);			
//			if( !!voCtrlLabelobj ){
//					var vsaLabelStyleNm = voCtrlLabelobj.style.getClasses();
//					vsaLabelStyleNm.forEach(function(vsStyleNm){
//						if(vaRequiredClass.indexOf(vsStyleNm) != -1){
//							notnull = "Y";
//							vsFieldLabel = voCtrlLabelobj.value;
//						}
//					});
//			}
		}
		
		if(notnull === "Y") {
			if(ctrlValue == null || new String(ctrlValue) == "") {
				vsFieldLabel = getGridFieldLabel(poParentCtl, vsFieldLabel);
				//{0}은(는) 필수 입력 항목입니다.
				var vsMsg = this._appKit.Msg.getMsg("WRN-M001", [vsFieldLabel]);
				parentValidMsg(vsMsg, poParentCtl, pnIdx);
				
				return false;
			}
		}
	}
	
	/* STEP2 : 지정된 컬럼중 하나 이상 필수 입력 체크(사용자 속성 : xorRequired)
	 * 				- 그리드일경우					 :  columnName
	 * 				- 그룹 및 일반컨트롤일 경우   : 컨트롤 ID
	 */
	{
		var xorNull = ctrl.userAttr("xorRequired");
		if(xorNull) {
			var vaXorNull = ValueUtil.split(xorNull.replace(/\[|\]/g,""), ",");
			var vsName = "";
						
			var vbStatus = false;
			if(poParentCtl instanceof cpr.controls.Grid){ //그리드 내 컨트롤
				for (var j = 0; j < vaXorNull.length; j++) {
					var vsValue = poRow != null ? poRow.getValue(vaXorNull[j]) : poParentCtl.getCellValue(pnIdx, vaXorNull[j]);
					if(!ValueUtil.isNull(vsValue)){
						vbStatus = true;
						break;
					}
					var vaDetailCell = poParentCtl.detail.getColumnByName(vaXorNull[j]);
					vaDetailCell.some(function(vcCell){
						var vsTargetFieldLabel = vcCell.control ? vcCell.control.fieldLabel : "";
						vsName += getGridFieldLabel(poParentCtl, vsTargetFieldLabel, vcCell.cellIndex) + ", ";
					});
				}
				if(!vbStatus){
					//{0}중 하나는 필수 입력 항목입니다.
					var vsMsg = this._appKit.Msg.getMsg("WRN-M003", [vsName.substring(0, vsName.length -1)]);
					parentValidMsg(vsMsg, poParentCtl, pnIdx);
					return false;
				}
			}else{
				for (var j = 0; j < vaXorNull.length; j++) {
					var vcCtl = ctrl.getAppInstance().lookup(vaXorNull[j]);
					var vsValue = vcCtl.value;
					if(!ValueUtil.isNull(vsValue)){
						vbStatus = true;
						break;
					}
					vsName += vcCtl.fieldLabel + " ,";
				}
				
				if(!vbStatus) {
					//{0}중 하나는 필수 입력 항목입니다.
					var vsMsg = this._appKit.Msg.getMsg("WRN-M003", [vsName.substring(0, vsName.length -1)]);
					parentValidMsg(vsMsg, poParentCtl, pnIdx);
					return false;
				}
			}
		}
	}
	
	// 나머지 항목은 값이 있을 때만 체크
	if(ctrlValue == null || ctrlValue == "") return true;
	
	/* STEP3 : Type check(사용자 속성 : columnType)
	 * 
	 * ※ 하나의 값이 여러개의 컨트롤로 분리되어 있는 경우 (사용자 속성 : mergeControlIds, 자기 자신은 제외한 컨트롤 id 순서대로 작성 필요)
	 *    ⇒ 여러 개의 컨트롤 value 를 합쳐 하나의 value 로 유효성 검사 진행 
	 */
	{
		var type = ctrl.userAttr("columnType");
		if(type) {
			
			// columnType 을 작성한 컨트롤이 분리되어 있을 경우, value를 합치는 사용자속성 추가
			var mergeControlIds = ctrl.userAttr("mergeControlIds");
			if(!ValueUtil.isNull(mergeControlIds)) {
				var vaMergeCtrlIds = ValueUtil.replaceAll(mergeControlIds, " ", "").split(",");
				vaMergeCtrlIds.forEach(function(each){
					var vcCtrl = ctrl.getAppInstance().lookup(each);
					if(!ValueUtil.isNull(vcCtrl)) {
						var vsSeparator = "";
						if(type == "email" && !ValueUtil.isNull(ctrlValue)) vsSeparator = "@";
						ctrlValue += vsSeparator + ValueUtil.fixNull(vcCtrl.value);
					}
				});
			}
			
			var isChk = true;
			if(type == "email"){
				isChk = TypeUtil.isEmail(ctrlValue);
			}else if(type == "ssn"){
				isChk = TypeUtil.isSSN(ctrlValue);
			}else if(type == "bizno"){
				isChk = TypeUtil.isBizCSN(ctrlValue);
			}else if(type == "phone"){
				isChk = TypeUtil.isTelMobile(ctrlValue);
			}else if(type == "tel"){
				isChk = TypeUtil.isTelNo(ctrlValue);
			}else if(type == "url"){
				isChk = TypeUtil.isURL(ctrlValue);
			}else if(type == "frno") {
				isChk = TypeUtil.isFrno(ctrlValue);
			}else if(type == "corp") {
				isChk = TypeUtil.isCorpno(ctrlValue);
			}else if(type == "credit") {
				isChk = TypeUtil.isCreditno(ctrlValue);
			}else if(type == "zipcd") {
				isChk = TypeUtil.isZipcd(ctrlValue);
			}
			
			if(isChk == false) {
				vsFieldLabel = getGridFieldLabel(poParentCtl, vsFieldLabel);
				//{0}은(는) 유효하지 않은 형식입니다.
				var vsMsg = this._appKit.Msg.getMsg("WRN-M004", [vsFieldLabel]);
				parentValidMsg(vsMsg, poParentCtl, pnIdx);
				return false;
			}
		}
	}
	
	/* STEP4 : 최소 길이 check(사용자 속성 : minlength, 문자열 길이 비교)*/
	{
		var minlength = ctrl.userAttr("minlength");
		if(minlength) {
			var minlengthNum = Number(minlength);
			var length = ValueUtil.getLength(ctrlValue, ctrl.lengthUnit);
			if(length < minlengthNum) {
				vsFieldLabel = getGridFieldLabel(poParentCtl, vsFieldLabel);
				//{0}은(는) {1}자 이상으로 입력하십시오.
				var vsMsg = this._appKit.Msg.getMsg("WRN-M005", [vsFieldLabel, minlength]);
				parentValidMsg(vsMsg, poParentCtl, pnIdx);
				return false;
			}
		}
	}
	
	/* STEP5 : 고정 길이 check(사용자 속성 : fixlength)*/
	{
		var fixlength = ctrl.userAttr("fixlength");
		if(fixlength) {
			var fixlength = Number(fixlength);
			var length = ValueUtil.getLength(ctrlValue, ctrl.lengthUnit);
			if(length != fixlength) {
				vsFieldLabel = getGridFieldLabel(poParentCtl, vsFieldLabel);
				//{0}은(는) {1} 자리수만큼 입력하십시오.
				var vsMsg = this._appKit.Msg.getMsg("WRN-M006", [vsFieldLabel, fixlength]);
				parentValidMsg(vsMsg, poParentCtl, pnIdx);
				return false;
			}
		}
	}
	
	/* 
	 * STEP6 : 두 값을 비교(사용자 속성 : compare)
	 * 			- 그리드 일 경우 : columnName +  비교연산자(<=, <, >=, >, ==, =)
	 * 			- 일반 컨트롤 일 경우 : 컨트롤 ID + 비교연산자(<=, <, >=, >, ==, =)
	 * ex) 시작일시 데이트 인풋 : dtiEnd,<=
	 * 		종료일시 데이트 인풋 : dtiStart,>=
	 */
	{
		var compare = ctrl.userAttr("compare");
		if(!ValueUtil.isNull(compare)) {
			var compareCol = compare.substring(0, compare.indexOf(",")).trim();
			var compareType = compare.substr(compare.indexOf(",") + 1).trim();
			//그리드 내 컨트롤
			var vbStatus = false;
			var vsCompareColValue;
        	var vsCompareColLable;
        	var value = ctrlValue;
			if(poParentCtl instanceof cpr.controls.Grid){
				vsCompareColValue = poRow != null ? poRow.getValue(compareCol) : poParentCtl.getCellValue(pnIdx, compareCol);
				var vcDetailColumn = poParentCtl.detail.getColumnByName(compareCol)[0];
				var vsTargetFieldLabel = vcDetailColumn.control ? vcDetailColumn.control.fieldLabel : "";
				vsCompareColLable = getGridFieldLabel(poParentCtl, vsTargetFieldLabel, vcDetailColumn.cellIndex);
			}else{
				// compareCol 이 존재하지 않는 경우 return
				var vcCompareCtrl = ctrl.getAppInstance().lookup(compareCol);
				if(!vcCompareCtrl) {
					var vsTempMsg = "컨트롤이 존재하지 않습니다. compare 속성을 확인하시기 바랍니다.";
					var vsMsg = this._appKit.Msg.getMsg("NF-M000", [vsTempMsg]);
					parentValidMsg(vsMsg, poParentCtl);
					return false;
				}
				
				vsCompareColValue = vcCompareCtrl.value;
				vsCompareColLable = vcCompareCtrl.fieldLabel;
			}
			
			if(!ValueUtil.isNull(value) && !ValueUtil.isNull(vsCompareColValue)){
				var vbReturn = false;
				var vsCompareVal = "'"+value+"'" + compareType + "'"+vsCompareColValue+"'";
				var vsCompareValNumber = value + compareType + vsCompareColValue;
				if (ValueUtil.isNumber(value) && ValueUtil.isNumber(vsCompareColValue)) {
					vbReturn = Function('"use strict";return (' + vsCompareValNumber + ')')();	
				}else{
					vbReturn = Function('"use strict";return (' + vsCompareVal + ')')();	
				}
		            
	            if (!vbReturn) {
	            	 vsFieldLabel = getGridFieldLabel(poParentCtl, vsFieldLabel);
	            	 var vsMsg = "";
	            	if(compareType == "<=" || compareType == "<" ){
	            		//{0}은(는) {1}보다 클 수 없습니다.
	            		vsMsg = this._appKit.Msg.getMsg("WRN-M009", [vsFieldLabel, vsCompareColLable]);
	            	}else if (compareType == ">=" || compareType == ">" ){
	            		//{0}은(는) {1}보다 작을수 없습니다.
	            		vsMsg = this._appKit.Msg.getMsg("WRN-M010", [vsFieldLabel, vsCompareColLable]);
	            	}else if (compareType == "==" || compareType == "="){
	            		//{0}은(는) {1}와 같아야 합니다.
	            		vsMsg = this._appKit.Msg.getMsg("WRN-M011", [vsFieldLabel, vsCompareColLable]);
	            	}else{
	            		
	            	}
	            	parentValidMsg(vsMsg, poParentCtl, pnIdx);
	                return false;
	            }
			}
		}
	}
	
	/* STEP7 : 최대 길이 check(사용자 속성 : maxlength, 문자열 길이 비교)*/
	{
		var maxlength = ctrl.userAttr("maxlength");
		if(maxlength) {
			var maxlengthNum = Number(maxlength);
			var length = ValueUtil.getLength(ctrlValue, ctrl.lengthUnit);
			if(length > maxlengthNum) {
				vsFieldLabel = getGridFieldLabel(poParentCtl, vsFieldLabel);
				//{0}은(는) {1}자 이하로 입력하십시오.
				var vsMsg = this._appKit.Msg.getMsg("WRN-M055", [vsFieldLabel, maxlength]);
				parentValidMsg(vsMsg, poParentCtl, pnIdx);
				return false;
			}
		}
	}
	
	/* STEP8 : 정수타입 별 유효한 값인지 check(사용자 속성 : integer)
	 * 				- small : short integer 체크
	 * 				- normal : integer 체크
	 * 				- big : long integer 체크
	 */
	{
		var integer = ctrl.userAttr("integer");
		if (integer) {
			var min, max;
			if (integer == "small") {
				// short integer
				min = parseInt("-32_768".replace(/_/g, "")); //-32768 
				max = parseInt("32_767".replace(/_/g, "")); //32767
			} else if (integer == "normal") {
				// integer
				min = parseInt("-2_147_483_648".replace(/_/g, "")); //-2147483648
				max = parseInt("2_147_483_647".replace(/_/g, "")); //2147483647
			} else if (integer == "big") {
				// long integer
				min = parseInt("-9_223_372_036_854_775_808".replace(/_/g, "")); //-9223372036854776000
				max = parseInt("9_223_372_036_854_775_807".replace(/_/g, "")); //9223372036854776000
			}
			
			var vsMsg = "";
			if (ctrlValue.search(/^(-)?([0-9]+)$/) == 0) {
				if (min <= Number(ctrlValue) && Number(ctrlValue) <= max) {
				} else {
					//입력가능한 숫자의 유효 범위를 벗어났습니다
					var vsMsg = this._appKit.Msg.getMsg("WRN-M056");
					parentValidMsg(vsMsg, poParentCtl, pnIdx);
					return false;
				}
			} else {
				if (ctrlValue.indexOf(".") != -1) {
					//입력가능한 숫자의 유효 범위를 벗어났습니다
					var vsMsg = this._appKit.Msg.getMsg("WRN-M056");
					parentValidMsg(vsMsg, poParentCtl, pnIdx);
					return false;
				} else {
					//입력가능한 숫자의 유효 범위를 벗어났습니다
					var vsMsg = this._appKit.Msg.getMsg("WRN-M056");
					parentValidMsg(vsMsg, poParentCtl, pnIdx);
					return false;
				}
			}
		}
	}
	
	/* STEP9 : 정수 값 사이에 있는지 check(사용자 속성 : integer-between)
	 * ex) 10,50 ⇒ (10 < value <50 의 값인지 체크) 
	 */
	{
		var integerBetween = ctrl.userAttr("integer-between");
		if (integerBetween) {
			var vaBetween = integerBetween.split(",");
			if (vaBetween.length > 1) {
				var vnBtwMin = vaBetween[0];
				var vnBtnMax = vaBetween[1];
				
				if (Number(ctrlValue) < Number(vnBtwMin) || Number(ctrlValue) > Number(vnBtnMax)) {
					 vsFieldLabel = getGridFieldLabel(poParentCtl, vsFieldLabel);
					//{0}은(는) {1}부터 {2}사이로 입력하십시오.
					var vsMsg = this._appKit.Msg.getMsg("WRN-M008", [vsFieldLabel, vnBtwMin, vnBtnMax]);
					parentValidMsg(vsMsg, poParentCtl, pnIdx);
					return false;
				}
			}
		}
	}
	
	/* STEP10 : 날짜 사이에 있는지 check(사용자 속성 : between)
	 * ex) 20230101,20231231 ⇒ (2023.01.01 ~ 2023.12.31 사이에 있는 날짜인지 확인)
	 */
	{
		var between = ctrl.userAttr("between");
		if (between) {
			var vaBetween = between.split(",");
			if (vaBetween.length > 1) {
				var vsFromDate = vaBetween[0];
				var vsToDate = vaBetween[1];
				
				var current = moment(ctrlValue);
				// 날짜비교
				var vbAfter = current.isAfter(vsFromDate) || current.isSame(vsFromDate);
				var vbBefore = current.isBefore(vsToDate) || current.isSame(vsToDate);
				if (!vbAfter || !vbBefore) {
					 vsFieldLabel = getGridFieldLabel(poParentCtl, vsFieldLabel);
					//{0}은(는) {1}부터 {2}사이로 입력하십시오.
					var vsMsg = this._appKit.Msg.getMsg("WRN-M008", [vsFieldLabel, vsFromDate, vsToDate]);
					parentValidMsg(vsMsg, poParentCtl, pnIdx);
					return false;
				}
			}
		}
	}
	
	/* STEP11 : 정수, 소수영역 각 최대 자리수 check(사용자 속성 : fixedDecimal)
	 * ex) 3,2 ⇒ (정수 최대 3자리, 소수 최대 2자리까지 유효, 0 ~ 999.99)
	 */
	{
		var fixedDecimal = ctrl.userAttr("fixedDecimal");
		if (fixedDecimal) {
			var vaFixedDecial = fixedDecimal.split(",");
			if (vaFixedDecial.length > 1) {
				var vnPrecision = vaFixedDecial[0]; // 정수 최대 자리수
				var vnScale = vaFixedDecial[1]; // 소수 최대 자리수
				
				if (ValueUtil.isNumber(ctrlValue)) {
					var vaCtrlValue = ctrlValue.toString().split(".");
					if (ValueUtil.getLength(vaCtrlValue[0]) > vnPrecision ||
						(vaCtrlValue.length > 1 && ValueUtil.getLength(vaCtrlValue[1]) > vnScale)) {
						vsFieldLabel = getGridFieldLabel(poParentCtl, vsFieldLabel);
						//{0}은(는) {1}자리와 소수점 {2}자리까지 입력할 수 있습니다.
						var vsMsg = this._appKit.Msg.getMsg("WRN-M057", [vsFieldLabel, vnPrecision, vnScale]);
						parentValidMsg(vsMsg, poParentCtl, pnIdx);
						return false;
					}
				}
			}
		}
	}
	
	/* STEP12 : 넘버형 데이터의 최대 길이 check(사용자 속성 : maxLengthDigits)
	 * 				반드시 넘버형 데이터일 때만 유효성 검증 진행 가능
	 */
	{
		var maxLengthDigits = ctrl.userAttr("maxLengthDigits");
		if (maxLengthDigits) {
			// 0~9 로 구성된 숫자만 체크
			if (ValueUtil.isNumber(ctrlValue)) {
				if (ValueUtil.getLength(ctrlValue.toString()) > maxLengthDigits) {
					vsFieldLabel = getGridFieldLabel(poParentCtl, vsFieldLabel);
					//{0}은(는) {1}자 이하로 입력하십시오.\n({0}은(는) 숫자만 입력할 수 있습니다.)
					var vsMsg = this._appKit.Msg.getMsg("WRN-M055", [vsFieldLabel, maxLengthDigits]) + "\n"
									+ this._appKit.Msg.getMsg("WRN-M007", [vsFieldLabel]);
					parentValidMsg(vsMsg, poParentCtl, pnIdx);
					return false;
				}
			}
		}
	}
	
	/* STEP13 : 숫자 최소 값 check(사용자 속성 : min)
	 * 				반드시 넘버형 데이터일 때만 유효성 검증 진행 가능
	 */
	{
		var minVal = ctrl.userAttr("min");
		if (minVal && ValueUtil.isNumber(ctrlValue)) {
			if (Number(ctrlValue) < Number(minVal)) {
				vsFieldLabel = getGridFieldLabel(poParentCtl, vsFieldLabel);
				//{0}은(는) {1}보다 작을수 없습니다.
				var vsMsg = this._appKit.Msg.getMsg("WRN-M010", [vsFieldLabel, minVal]);
				parentValidMsg(vsMsg, poParentCtl, pnIdx);
				return false;
			}
		}
	}
	
	/* STEP14 : 숫자 최대 값 check(사용자 속성 : max)
	 * 				반드시 넘버형 데이터일 때만 유효성 검증 진행 가능
	 */
	{
		var maxVal = ctrl.userAttr("max");
		if (maxVal && ValueUtil.isNumber(ctrlValue)) {
			if (Number(ctrlValue) > Number(maxVal)) {
				vsFieldLabel = getGridFieldLabel(poParentCtl, vsFieldLabel);
				// {0}은(는) {1}보다 클 수 없습니다.
				var vsMsg = this._appKit.Msg.getMsg("WRN-M009", [vsFieldLabel, maxVal]);
				parentValidMsg(vsMsg, poParentCtl, pnIdx);
				return false;
			}
		}
	}
	
	/* STEP15 : 소수점 영역 최대 자리수 check(사용자 속성 : maxDecimalPointLength)
	 * 	ex) 3 ⇒ (소수점 아래 최대 3자리까지만 가능, #.0 ~  #.999))
	 */
	{
		var maxDecimalPointLength = ctrl.userAttr("maxDecimalPointLength");
		if (maxDecimalPointLength) {
			var vaCtrlValue = ctrlValue.toString().split(".");
			var vnCtrlPointLength = 0; // 정수일 경우, 소수점 기본 0자리
			if(vaCtrlValue.length > 1) {
				vnCtrlPointLength = ValueUtil.getLength(vaCtrlValue[1]);
			}
			if (Number(vnCtrlPointLength) > Number(maxDecimalPointLength)) {
				//소수점은 최대 {0}자리입니다.
				var vsMsg = this._appKit.Msg.getMsg("WRN-M058", [maxDecimalPointLength]);
				parentValidMsg(vsMsg, poParentCtl, pnIdx);
				return false;
			}
		}
	}
	
	/* STEP16 : 소수점 영역 최소 자리수 check(사용자 속성 : minDecimalPointLength)
	 * ex) 3 ⇒ (소수점 아래 최소 3자리이상 가능, #.000 ~ )
	 */
	{
		var minDecimalPointLength = ctrl.userAttr("minDecimalPointLength");
		if (minDecimalPointLength) {
			var vaCtrlValue = ctrlValue.toString().split(".");
			var vnCtrlPointLength = 0; // 정수일 경우, 소수점 기본 0자리
			if(vaCtrlValue.length > 1) {
				vnCtrlPointLength = ValueUtil.getLength(vaCtrlValue[1]);
			}
			if (Number(vnCtrlPointLength) < Number(minDecimalPointLength)) {
				//소수점은 최소 {0}자리입니다.
				var vsMsg = this._appKit.Msg.getMsg("WRN-M059", [minDecimalPointLength]);
				parentValidMsg(vsMsg, poParentCtl, pnIdx);
				return false;
			}
		}
	}
	
	return true;
}
 