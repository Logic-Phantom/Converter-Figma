
/**
 * @class AppUtil AppInstance에 대한 유틸
 */
AppUtil = {
    /**
     * 해당 앱의 속성(Property)값을 할당한다.
     * @param {cpr.core.AppInstance} app 앱인스턴스 객체
	 * @param {String | Object} propertyName App 속성
	 * @param {String | Object} value App 속성값
	 * @param {boolean} pbEvent value-change 이벤트 발생여부  (default : true)
	 * @return void
     */
    setAppProperty : function (app, propertyName, value, pbEvent) {
    	pbEvent = pbEvent == null ? true : pbEvent;
    	
        /** @type cpr.core.AppInstance */
        var _app = app;
        var hostApp = _app.getHostAppInstance();
        var property = _app.getAppProperty(propertyName);
        if(hostApp && hostApp.lookup(property) && hostApp.lookup(property) instanceof cpr.controls.UIControl){
        	if(pbEvent){
        		hostApp.lookup(property).value = value;
        	}else{
        		hostApp.lookup(property).putValue(value);
        	}
        }else{
        	_app.setAppProperty(propertyName, value);
        	
        	//그룹에 임베디드된 경우라면... 해당 그룹을 redraw()해준다.
        	if (app.getHost().getParent() && app.getHost().getParent().type == "container") {
        		app.getHost().getParent().redraw();
        	}
        }
    },
    
    /**
     * UDC 컨트롤에 대해 value 앱 속성에 바인딩된 컨트롤 객체를 반환한다.
     * @param {cpr.controls.UIControl} poCtrl
     */
    getUDCBindValueControl : function(poCtrl){
    	var vcBindCtrl = poCtrl;
    	var embApp = poCtrl.getEmbeddedAppInstance();
		embApp.getContainer().getChildren().some(function(embCtrl){
			if(embCtrl.type == "container"){
				embCtrl.getChildren().some(function(subembCtrl){
					if(subembCtrl.getBindInfo("value") && subembCtrl.getBindInfo("value").property == "value"){
						vcBindCtrl = subembCtrl;
						return true;
					}
				});
			}else{
				if(embCtrl.getBindInfo("value") && embCtrl.getBindInfo("value").property == "value"){
					vcBindCtrl = embCtrl;
					return true;
				}
			}
		});
		
		return vcBindCtrl;
    }
 };

/**
 * @class ValueUtil Value 체크 및 형 변환
 */
ValueUtil = {
    /**
     * 해당 값이 Null인지 여부를 체크하여 반환한다.
	 * @param {String | Object} puValue 값
	 * @return {Boolean} Null 여부
     */
    isNull : function (puValue) {
        return (this.fixNull(puValue) == "");
    },

    /**
     * 해당 값이 숫자(Number) 타입인지 여부를 반환한다.
	 * @param {Number | String} puValue	값
	 * @example ValueUtil.isNumber("1234.56") == true
	 * @return {Boolean} Number인지 여부
     */
    isNumber : function (puValue) {
        var vnNum = Number(puValue);
        return isNaN(vnNum) == false;
    },
    
    /**
	 * 해당 값이 숫자(Number) 타입인지 여부를 반환한다.<br>
	 * 숫자일 경우, 기존 값과 비교 시 동일하지 않을 경우에는 false 를 반환한다.	
	 * <pre>
	 * <code>ValueUtil.isNumberStrict("1234.56") // true
	 * ValueUtil.isNumberStrict("00001") // false
	 * </code></pre>
	 * @param {Number | String} puValue		값
	 * @example ValueUtil.isNumberStrict("1234.56") == true / ValueUtil.isNumberStrict("00001") == false
	 * @return {Boolean} Number인지 여부
	 */
	isNumberStrict: function(puValue) {
		var vnNum = Number(puValue);
		return (isNaN(vnNum) == false && vnNum.toString() == puValue);
	},
	
	/**
     * 해당 값이 boolean 타입인지 여부를 반환한다.
	 * <pre>
	 * <code>ValueUtil.isBoolean(false) // true
	 * </code></pre>
     * @param {Boolean | Object} puValue		값
     * @example ValueUtil.isBoolean(false) == true
     * @return {Boolean} Boolean인지 여부
     */
    isBoolean: function(puValue) {
        if (typeof(puValue) == "boolean" || puValue instanceof Boolean) return true ;
        else return false ;
    },
    
    /**
     * 해당 값의 Array 여부를 반환한다.
     * @param {Object} puValue 값
     * @return {Boolean} Array인지 여부
     */
    isArray: function(puValue) {
	    return puValue instanceof Array;
	},
	
	/**
	 * 파일명(문자열)에 특수문자가 포함되었는지 여부를 반환한다.
	 * @param {String} psValue 파일명
	 * @return {Boolean} 특수문자가 포함된 경우 false 리턴
	 */
	isFileSpecChar: function(psValue) {
		if(this.isNull(psValue)) return false;
		var vsSpecialChar = /[\/?,;:|*~`!^+<>@#$%^\\\=\'\"]/gi;
		
		if (!vsSpecialChar.test(psValue)) {
			return true;
		} else {
			return false;
		}
	},
	
	/**
	 * 해당 값이 정수/실수 인지 여부를 반환한다.
	 * <pre><code>
	 *  ValueUtil.isInteger(1234) //true
	 * </code></pre>
	 * @param {Number} pnValue 값
	 * @example ValueUtil.isInteger(1234) == true
	 * @return {Boolean} 정수인지 여부
	 */
	isInteger: function(pnValue) {
		var vnNum = this.fixNumber(pnValue);
		return vnNum % 1 === 0;
	},
	
	/**
	 * 해당 문자로 시작하는지 여부 반환 <br>
	 * - 주의사항 : null 은 false 리턴
	 * <pre>
	 * <code>
	 *  ValueUtil.isStartWith("aaaabbbbb","aaa") // true
	 * </code>
	 * </pre>
	 * @param {String} psValue 값
	 * @param {String} psFindStr 찾고자하는 문자/문자열
	 * @param {Boolean} pbIgnoreCase? 대소문자 구분할지 여부 (deafult : false)<br/>true면 대소문자를 구분한다.
	 * @return {Boolean} psFindStr 로 시작하는 문자열인지 여부
	 */
	isStartWith: function(psValue, psFindStr, pbIgnoreCase) {
		var vsSourceTxt = this.fixNull(psValue, null, false);
		var vsFindStr = this.fixNull(psFindStr, " ", false);
		if(vsFindStr == "") vsFindStr = " ";
		
		var vbIgnoreCase = (pbIgnoreCase == null || pbIgnoreCase == undefined) ? false : pbIgnoreCase;
		if(!vbIgnoreCase) {
			vsSourceTxt = vsSourceTxt.toLowerCase();
			vsFindStr = vsFindStr.toLowerCase();
		}
		
		return vsSourceTxt.indexOf(vsFindStr) == 0;
	},
	
	/**
	 * 해당 문자로 끝나는지 확인한다.
	 * <pre><code>
	 *  ValueUtil.isEndWith("aaaabbbbb","bbbb") // true
	 * </code></pre>  
	 * @param {String} psValue 값
	 * @param {String} psFindStr 찾고자하는 문자/문자열
	 * @param {Boolean} pbIgnoreCase? 대소문자 구분할지 여부 (deafult : false)<br/>true면 대소문자를 구분한다.
	 * @return {Boolean} psFindStr 로 끝나는 문자열인지 여부
	 */
	isEndWith: function(psValue, psFindStr, pbIgnoreCase) {
		var vsSourceTxt = this.fixNull(psValue, null, false);
		var vsFindStr = this.fixNull(psFindStr, " ", false);
		if (vsFindStr == "") vsFindStr = " ";
		
		var vbIgnoreCase = (pbIgnoreCase == null || pbIgnoreCase == undefined) ? false : pbIgnoreCase;
		if (!vbIgnoreCase) {
			vsSourceTxt = vsSourceTxt.toLowerCase();
			vsFindStr = vsFindStr.toLowerCase();
		}
		
		var vnPos = vsSourceTxt.lastIndexOf(vsFindStr);
		if (vnPos < 0) return false;
		
		var vnTotLength = vsSourceTxt.length;
		if ((vnTotLength - vnPos) == vsFindStr.length) return true;
		
		return false;
	},
    
    /**
     * 해당 문자를 갖고있는지 확인한다. 
     * <pre><code>
     * ValueUtil.isHaveStr("aaaabbbbb","bbbb") // true  
     * </code></pre>
     * @param {String} psSource 값
     * @param {String} psFindStr 찾고자하는 문자/문자열
     * @param {Boolean} pbIgnoreCase? 대소문자 구분할지 여부 (deafult : false)<br/>true면 대소문자를 구분한다.
     * @return {Boolean} psFindStr 이 문자열에 포함되어 있는지 여부
     */
    isHaveStr: function(psSource, psFindStr, pbIgnoreCase) {
    	var vsSourceTxt = this.fixNull(psSource, null, false);
    	var vsFindStr = this.fixNull(psFindStr, " ", false);
    	if (vsFindStr == "") vsFindStr = " ";
    	
    	var vbIgnoreCase = (pbIgnoreCase == null || pbIgnoreCase == undefined) ? false : pbIgnoreCase;
		if (!vbIgnoreCase) {
			vsSourceTxt = vsSourceTxt.toLowerCase();
			vsFindStr = vsFindStr.toLowerCase();
		}
    	
    	var vnPos = vsSourceTxt.lastIndexOf(vsFindStr);
    	return vnPos > -1;
    },
    
    /**
     * 화면에서 보여주고 있는지 상태를 리턴한다.<br/>
     * 자신을 포함하는 모든 부모의 visible 상태가 false인 경우 false를 반환된다.<br/>
     * <pre><code>
     * ValueUtil.isHTMLVisible(app.lookup("btn1")) // true
     * </pre></code>
     * @param {cpr.controls.UIControl} pcCtrl UI컨트롤
     * @return {Boolean} 화면에 컨트롤이 보여지는지 여부
     */
    isHTMLVisible : function (pcCtrl){
    	return pcCtrl.isShowing();
    },
    
    /**
     * 해당 값에 대한 문자열을 반환한다. <br/>
     * 만약 해당값이 null이거나 정의되지 않은 경우, 대체문자 혹은 공백("") 문자열을 반환한다.
     * @param {String | Object} puValue	값
     * @param {String} puAlterValue? 대체문자
     * @param {Boolean} pbTrimValue? 입력 값(puValue) 의 값을 trim 할 지 여부 (default : true) <br/>true 일 경우에 trim 한다
     * @return {String} 문자열 String
     */
    fixNull: function(puValue, puAlterValue, pbTrimValue) {
    	
    	var vbTrimValue = (pbTrimValue == null || pbTrimValue == undefined) ? true : pbTrimValue;
    	if(vbTrimValue) {
	    	var vsType = typeof(puValue);
	    	if (vsType == "string" || (vsType == "object" && puValue instanceof String)) {
	    		puValue = this.trim(puValue);
	    	}
    	}
    	
    	var vsAlterStr = "";
    	if (puAlterValue == null || puAlterValue == "null" || puAlterValue == "undefined" || puAlterValue == undefined) {
    		vsAlterStr = "";
    	} else {
    		vsAlterStr = puAlterValue;
    	}
    	
    	return (puValue == null || puValue == "null" || puValue == "undefined" || puValue == undefined) ? vsAlterStr : String(puValue);
    },
    
    /**
     * 해당 값을 불리언(Boolean) 타입으로 변환한다.
	 * @param {Boolean | Object} puValue 값
	 * @return {Boolean} 불리언 유형으로 반환
     */
    fixBoolean : function (puValue) {
        if (typeof(puValue) == "boolean" || puValue instanceof Boolean) {
            return puValue;
        }
        if (typeof(puValue) == "number" || puValue instanceof Number) {
            return puValue != 0;
        }
        return (this.fixNull(puValue).toUpperCase() == "TRUE");
    },

    /**
     * 해당 값을 숫자(Number) 타입으로 변환한다.
	 * @param {Object} puValue 값
	 * @return {Number} 숫자 타입으로 반환
     */
    fixNumber : function (puValue) {
        if (typeof(puValue) == "number" || puValue instanceof Number) {
            return puValue;
        }
        var vnNum = Number(this.fixNull(puValue));
        return isNaN(vnNum) ? 0 : vnNum;
    },
    
    /**
     * 해당 값을 숫자(Float) 타입으로 변환한다.
	 * @param {Object} puValue 값
	 * @return {Float} 소수점이 있는 숫자 타입으로 반환
     */
    fixFloat : function (puValue) {
        if (typeof(puValue) == "number" || puValue instanceof Number) {
            return puValue;
        }
        var vnFloat = parseFloat(this.fixNull(puValue));
        return isNaN(vnFloat) ? 0 : vnFloat;
    },
    
    /**
	 * nvl(puValue, puDefalt)
	 * 입력값이 null 일때, Defalt value 를 return 한다.
	 * @param {any} puValue 체크대상 값
	 * @param {any} puDefalt 기본값
	 * @return {any} 처리된 값
	 */
	nvl: function(puValue, puDefalt) {
		return (this.isNull(puValue)) ? puDefalt : puValue;
	},
    /**
     * 해당 값의 앞/뒤 공백을 제거한 문자열을 반환한다.
	 * @param {String} psValue 값
	 * @return {String} 공백 제거된 문자열
     */
    trim : function (psValue) {
        return psValue == null ? psValue : psValue.replace(/^[\s\uFEFF\xA0]+|[\s\uFEFF\xA0]+$/g,'');
    },
    
    /** 
     * 문자열의 좌측 공백(psReplaceValue)을 제거 
     * @param {String} psValue 대상 
     * @param {String} psReplaceValue? 제거대상 문자 (default : " " )
     * @return {String} 공백(or 제거대상 문자) 제거된 문자열
     */
    lTrim: function(psValue, psReplaceValue) {
    	
    	psValue = this.fixNull(psValue, null, false);
    	if (psValue == "") return "";
    	
    	psReplaceValue = this.fixNull(psReplaceValue, " ", false);
    	if(psReplaceValue.length < 1) psReplaceValue = " ";
    	
    	var vnPos;
    	for (vnPos = 0; vnPos < psValue.length; vnPos += psReplaceValue.length) {
    		if (psValue.substr(vnPos, psReplaceValue.length) != psReplaceValue)
    			break;
    	}
    	
    	return psValue.substr(vnPos);
    },
	
	/** 
	 * 문자열의 우측 공백(psReplaceValue)을 제거 
	 * @param {String} psValue  대상 
	 * @param {String} psReplaceValue? 제거대상 문자 (default : " " )
	 * @return {String} 공백(or 제거대상 문자) 제거된 문자열
	 */
	rTrim: function(psValue, psReplaceValue) {
		
		psValue = this.fixNull(psValue, null, false);
		if (psValue == "") return "";
		
		psReplaceValue = this.fixNull(psReplaceValue, " ");
		if (psReplaceValue.length < 1) psReplaceValue = " ";
		
		var vnPos;
		for (vnPos = psValue.length - psReplaceValue.length; vnPos >= 0; vnPos -= psReplaceValue.length) {
			if (psValue.substr(vnPos, psReplaceValue.length) != psReplaceValue)
				break;
		}
		
		return psValue.substr(0, vnPos + psReplaceValue.length);
	},
	
	/** 
	 *  전체 문자길이가 되도록 좌측에 대체문자를 채운다.<br>
	 * <pre><code>
	 *  ValueUtil.lPad( "1" , "0" , 3) // "001"
	 *  ValueUtil.lPad( "1" , " " , 3) // "  1"	
	 * </code></pre>
	 * @param {String} psValue  값
	 * @param {String} psReplaceValue 대체문자
	 * @param {Number} pnTotLength  전체 문자길이  
	 * @return {String} pad 가 된 문자 
	 */
	lPad: function(psValue, psReplaceValue, pnTotLength) {
		
		psValue = this.fixNull(psValue, null, false);
		psReplaceValue = this.fixNull(psReplaceValue, " ", false);
		pnTotLength = this.fixNumber(pnTotLength);
		
		if (pnTotLength < 1) return psValue;
		
		for (var i = 0, size = pnTotLength - psValue.length; i < size; i++) {
			psValue = psReplaceValue + psValue;
		}
		
		if(psValue.length > pnTotLength) {
			psValue = psValue.substr(psValue.length - pnTotLength, psValue.length);	
		}
		
		return psValue;
	},
	
	/** 
	 *  전체 문자길이가 되도록 만큼 우측에  대체문자를 체운다.
	 * <pre><code>
	 *  ValueUtil.rPad( "1" , "0" , 3); // "100"
	 *  ValueUtil.rPad( "1" , " " , 3); // "1  "
	 * </code></pre>
	 * @param {String} psValue  값 
	 * @param {String} psReplaceValue 대체문자  
	 * @param {Number} pnTotLength  전체 문자길이  
	 * @return {String} pad가 된 문자 
	 */
	rPad: function(psValue, psReplaceValue, pnTotLength) {
		
		psValue = this.fixNull(psValue, null, false);
		psReplaceValue = this.fixNull(psReplaceValue, " ", false);
		pnTotLength = this.fixNumber(pnTotLength);
		
		if (pnTotLength < 1) return psValue;
		
		for (var i = 0, size = pnTotLength - psValue.length; i < size; i++) {
			psValue = psValue + psReplaceValue;
		}
		
		if(psValue.length > pnTotLength) {
			psValue = psValue.substr(0, pnTotLength);	
		}
		
		return psValue;
	},
    
    /**
     * 문자열을 split한 배열을 반환한다.
	 * @param {String} psValue split 대상 문자열
	 * @param {String} psDelemeter 구분문자 (ex: 콤마(,))
	 * @return {Array} 문자열 배열
     */
    split : function (psValue, psDelemeter) {
    	psValue = this.fixNull(psValue);
        var vaValues = new Array();
        var vaTemp = psValue.split(psDelemeter);
        var _this = this;
        vaTemp.forEach(function(/* eachType */ item){
        	vaValues.push(_this.trim(item));
        });
        
        return vaValues;
    },
    
    /**
     * 문자열에서 패턴(psPattern) 이 대체 문자열(psReplaceTxt)로 전부 변경된 새 문자열을 리턴한다.
     * <pre><code>
     * ValueUtil.replaceAll( "t-o-m-a-t-o" , "-" , "") //"tomato"
     * </pre></code>
     * @param {String} psSource 원본 문자열 
     * @param {String} psPattern 변경할 문자열
     * @param {String} psReplaceTxt 대체 문자열
     * @return {String} 변경된 문자열
     */
    replaceAll: function(psSource, psPattern, psReplaceTxt) {
    	
    	if (psSource == null || psSource == "") return psSource;
    	if (psPattern == null || psPattern == "") return psSource;
    	
    	var vaResult = psSource.split(psPattern);
    	return vaResult.join(psReplaceTxt);
    },
    
    /**
     * 문자열 데이터의 길이(length)를 반환한다.
	 * @param {String} value 값
	 * @param {"char" | "utf8" | "ascii"} unit? 단위<br/>
     * [char] : 문자의 길이.<br/>
 	 * [utf8] : utf8 기준의 문자 byte size.<br/>
 	 * [ascii] : ascii 기준의 문자 byte size.
	 * @return {Number} 문자열 길이
     */
    getLength : function(value, unit) {
    	if(!unit) unit = "char";
    	
		var length = 0;
		switch(unit) {
			case "utf8":{
				for(var i=0, len=value.length; i<len; i++) {
				    if(escape(value.charAt(i)).length >= 4)
				        length += 3;
				    else if(escape(value.charAt(i)) == "%A7")
				        length += 3;
				    else if(escape(value.charAt(i)) != "%0D")
				        length++;
				    else length++;
				}
				break;
			}
			case "ascii":{
				for(var i = 0, c; c = value.charAt(i++); length += c >> 7 ? 2 : 1);
				break;
			}
			default : {
				length = value.length;
			}
		}
		
		return length;
    },
    getByteLength: function(/*String*/_str){
    	var stringByteLength = 0;
    	stringByteLength = (function(s,b,i,c){
		    for(b=i=0;c=s.charCodeAt(i++);b+=c>>11?2:c>>7?2:1);
		    return b
		})(_str);

		return stringByteLength;
    },
    
    /** 전화번호를 3개의 Array로 나누어서 리턴
     * <pre>
     * <code>
     *  var vaReturnValue = ValueUtil.getPttTelNo("02-842-3333");
     *  //vaReturnValue[0]:"02"
     *  //vaReturnValue[1]:"842"
     *  //vaReturnValue[2]:"3333"
     * </code>
     * </pre>
     * @param {String | Object} psValue 전화번호
     * @return {String[]} 분할된 3개의 전화번호
     **/
    getPttTelNo: function(psValue) {
    	var vaPttTelno = ["", "", ""];

    	if (this.isNull(psValue)) return vaPttTelno;
    
    	psValue = psValue.trim().replace(/[\-]/g, "");
    	var vnTelLen = psValue.length;
    	var vbSeoulYn = false; // 국번이 서울인지 여부 (유일하게 자리수가 다른 국번)
    	
    	/**
    	 * 서울 02  경기 031 인천 032 강원 033
    	 * 충남 041 대전 042 충북 043 부산 051
    	 * 울산 052 대구 053 경북 054 경남 055
    	 * 전남 061 광주 062 전북 063 제주 064
    	 */
    	if (psValue.substr(0, 2) == "02") vbSeoulYn = true;
    	
    	if (vbSeoulYn) {
    		if (vnTelLen <= 2) {
    			vaPttTelno = [psValue, "", ""];
    		} else if (vnTelLen == 3) {
    			vaPttTelno = [psValue.substr(0, 2), psValue.substr(2, 1), ""];
    		} else if (vnTelLen == 4) {
    			vaPttTelno = [psValue.substr(0, 2), psValue.substr(2, 2), ""];
    		} else if (vnTelLen == 5) {
    			vaPttTelno = [psValue.substr(0, 2), psValue.substr(2, 3), ""];
    		} else if (vnTelLen == 6) {
    			vaPttTelno = [psValue.substr(0, 2), psValue.substr(2, 4), ""];
    		} else if (vnTelLen == 7) {
    			vaPttTelno = [psValue.substr(0, 2), psValue.substr(2, 4), psValue.substr(6, 1)];
    		} else if (vnTelLen == 8) {
    			vaPttTelno = [psValue.substr(0, 2), psValue.substr(2, 2), psValue.substr(4, 4)];
    			//------------------------------------------
    		} else if (vnTelLen == 9) {
    			vaPttTelno = [psValue.substr(0, 2), psValue.substr(2, 3), psValue.substr(5, 4)];
    		} else if (vnTelLen == 10) {
    			vaPttTelno = [psValue.substr(0, 2), psValue.substr(2, 4), psValue.substr(6, 4)];
    		} else if (vnTelLen == 11) {
    			vaPttTelno = [psValue.substr(0, 3), psValue.substr(3, 4), psValue.substr(7, 4)];
    		} else {
    			vaPttTelno = ["", "", ""];
    		}
    	} else {
    		if (vnTelLen <= 2) {
    			vaPttTelno = [psValue, "", ""];
    		} else if (vnTelLen == 3) {
    			vaPttTelno = [psValue.substr(0, 3), "", ""];
    		} else if (vnTelLen == 4) {
    			vaPttTelno = [psValue.substr(0, 3), psValue.substr(3, 1), ""];
    		} else if (vnTelLen == 5) {
    			vaPttTelno = [psValue.substr(0, 3), psValue.substr(3, 2), ""];
    		} else if (vnTelLen == 6) {
    			vaPttTelno = [psValue.substr(0, 3), psValue.substr(3, 3), ""];
    		} else if (vnTelLen == 7) {
    			vaPttTelno = [psValue.substr(0, 3), psValue.substr(3, 4), ""];
    		} else if (vnTelLen == 8) {
    			vaPttTelno = [psValue.substr(0, 3), psValue.substr(3, 1), psValue.substr(4, 4)];
    		} else if (vnTelLen == 9) { //없는전화번호
    			vaPttTelno = [psValue.substr(0, 3), psValue.substr(3, 2), psValue.substr(5, 4)];
    		} else if (vnTelLen == 10) {
    			vaPttTelno = [psValue.substr(0, 3), psValue.substr(3, 3), psValue.substr(6, 4)];
    		} else if (vnTelLen == 11) {
    			vaPttTelno = [psValue.substr(0, 3), psValue.substr(3, 4), psValue.substr(7, 4)];
    		} else {
    			vaPttTelno = ["", "", ""];
    		}
    	}
    	return vaPttTelno;
    },
    
    /**
	 * 숫자 마스킹 처리 함수
	 * (뒷 pnStart번자리 부터 pnCount 개 마스킹) </br>
	 * (pnStart : 1, pnCount : 4 입력시 뒤에 숫자 1번째 부터 4개의 숫자 마스킹) </br>
	 * @param {String} psData 데이터
	 * @param {Number} pnStart 뒤에 시작 인덱스
	 * @param {Number} pnCount 마스킹 카운트
	 * @param {Boolean} pbRvrs false 시 앞자리 pnStart 부터 마스킹 (default : true)
	 * @return {String} 마스킹된 숫자 반환
	 */
    setNumberMask : function(psData, pnStart, pnCount, pbRvrs){
    	
    	// default 셋팅
		pbRvrs = pbRvrs == null ? true : pbRvrs;
	
		var aData =  [];
		var aReturn  =  [];
	
		aData = psData.split("");
	
		var nDecCnt = 0; // 암호화된 개수
		var nNumber = 0; // 숫자 개수
	
		// true 시, 뒷자리부터 마스킹.
		if (pbRvrs) {
			aData.reverse();
		}
	
		aData.forEach(function(psChar){
			if (psChar != " " && !isNaN(psChar)) {
				nNumber++;
			}
	
			// 뒤에서 pnStart번 자리 부터, pnCount 개 마스킹
			if (nNumber > pnStart && nDecCnt < pnCount && !isNaN(psChar) && psChar != " ") {
				nDecCnt++;
				psChar = "*";
		    }
	
		    aReturn.push(psChar);
		});
	
		// reverse 하여 마스킹해서, 다시 reverse 후 return
		if (pbRvrs) {
			aReturn.reverse();
		}
	
		return aReturn.join("");
    },
    
    /**
     * 파일명의 length를 체크한다. 
     * @param {String} psValue 파일명
     * @param {Number} pnTotLength 최대 파일명 length
     * @return {Boolean} 파일명 길이가 최대 길이(pnTotLength) 이상이면 false 리턴
     */
    fileNameLengthChk: function(psValue, pnTotLength) {
    	if (this.isNull(psValue) || this.isNull(pnTotLength)) return false;
    	
    	pnTotLength = this.fixNumber(pnTotLength);
    	if (this.getLength(psValue) > pnTotLength) {
    		return false;
    	} else {
    		return true;
    	}
    },
    
    /************************************************************************************
	 * 입력값을 마스킹한다
     * @exmple  var sMaskValue  = ValueUtil.maskType("JUMIN","9901012000000"))
	 * @param {"POLY"|"JUMIN"| "ACCOUNT"|"LOAN"|"TEL" |"EMAIL"|"ADDRESS"|"HPID"|"PASSPORT" |"DRIVER" |"FOREIGNER"|"CARD" |"NAME"} psType String			-Data Type<br>
				'POLY' 	    :     <br>
				'JUMIN' 	: 주민등록번호<br>
				'ACCOUNT' 	: 은행계좌번호<br>
				'LOAN' 		: 융자대출번호<br>
				'TEL' 		: 전화번호<br>
				'EMAIL' 	: 이메일<br>
				'ADDRESS' 	: 주소   (,콤마 기준으로 이후값 마스킹)<br>
				'HPID' 		: 홈페이지 id<br>
				'PASSPORT'  : 여권번호<br>
				'DRIVER'    : 운전면허번호<br>
			    'FOREIGNER' : 외국인등록번호<br>
			    'CARD'      : 카드번호<br>
                'NAME'      : 이름<br>
	 * @param  sourceData String value
	 * @return String    마스킹된 결과값
	 ************************************************************************************/
	maskType: function(psType, sourceData) {
		// "개인정보 암호화(마스킹)" 페이지 기준.
		// 증권번호, 주민등록번호, 은행계좌번호, 융자대출번호, 전화번호, E-Mail, 주소, 홈페이지 id
		// 증권번호 : 앞 2~4자리. 예시 > 1****678
		// 주민등록번호 : 뒤 6자리. 예시 > 760421-1******
		// 은행계좌번호 : 뒤 6자리~2자리. 예시 > 12345****9
		// 융자대출번호 : 제한없음.
		// 전화번호 : 뒤 4자리. 예시 > 010-7890-****
		// E-Mail : 뒤 3자리. 예시> abc***@hanwha.com
		// 주소 : 세부주소. 예시 > 서울시 영등포구 여의도동 63 *****
		// 홈페이지 id : 뒤 3자리. 예시 > abc***
		// 여권번호: 발급 일련번호 뒤 4자리 or 영문 1자리 + 뒷 3자리 예시 > M1234A567 -> M1234****, M12A34567 -> M12*34***
		// 운전면허번호 : 숫자 첫 2번째 자리와 일련번호 뒷2자리, 체크 값 첫 번째 자리 예시 > 12-1*-1234**-*2, 서울-1*-1234**-*2
		// 외국인등록번호 : 뒤 6자리. 예시 > 760421-1******
		// 카드번호 : 14~16 자리에 따라 4~6개 마스킹 예시 > 1234-56**-****-3456, 1234-56****-*2345, 1234-56****-1234
		// 이름  : 한글은 첫자와 마지막자를 제외한 *

		var data = this.trim(String(sourceData));
		if( this.isNull(data) ){
			return this.fixNull(data);
		}

		var sReturnValue = "";
		var pattern      = "";
				
		switch (psType.toUpperCase()) {

			case 'POLY': //증권번호. (앞 2번째 부터 4개 숫자 마스킹)
				pattern = /^([0-9]{1})(.{4})([0-9]+)$/;
				if(pattern.test(data)){
					sReturnValue = data.replace(pattern,"$1****$3");
				}
				break;

			case 'JUMIN': //주민등록번호.(마지막 6자리 마스킹)
				pattern = /^([0-9]{6})(-*)([a-zA-Z0-9]{1})(.{6})$/;
				//주민번호로 13자리가 안된 값이 넘어오는 경우, 채우기  (#5786 암호화된 문자열의 주민번호도 포멧팅하도록 수정 )(2021.09.01)
                if(data.length < 13) {
	                // [#5828] (2021.09.06) minLength -> fixLength로 변경 처리 (fixLength만큼 글자수를 채운다)
                    data = this.revise(data, "rpad", { fixLength : 13 , lengthUnit :"char" , padStr  :"*" });
                }
				if(pattern.test(data)){
					sReturnValue = data.replace(pattern,"$1-$3******");
				}
				break;

			case 'ACCOUNT': //계좌번호 (뒤 6~2자리. 5개 숫자 마스킹)
				/* 2021-02-22 뒤 2~6, 5개 숫자 마스킹  */
				// 10~15 자리수내에서 마스킹
				var sDataAccount = data.replace(/-/g, "");
				if (sDataAccount.length > 9 && sDataAccount.length < 16) {
					sReturnValue = this.setNumberMask(data, 1, 5);
				}
				break;

			case 'LOAN': //융자대출번호 (마스킹 없음)
				sReturnValue = data;
				break;

			case 'TEL': //전화번호 (마지막 4개 숫자 마스킹)
				pattern =/(^02.{0}|^01.{1}|[0-9]{3})([0-9]+)(.{4})/;
				var sDataTel = data.replace(/-/g, "");
				if(pattern.test(sDataTel)){
					sReturnValue = sDataTel.replace(pattern,"$1-$2-****");
				}
				break;

			case 'EMAIL': //이메일 (이메일 아이디 뒤 3자리 마스킹)
				pattern =/^([a-zA-Z0-9._-]+)([a-zA-Z0-9._-]{3})@([a-zA-Z0-9._-]+)[.]([a-zA-Z0-9._-]+)$/;
				if(pattern.test(data)){
					sReturnValue = data.replace(pattern,"$1***@$3.$4");
				}
				break;

			case 'ADDRESS': //주소
				// 뒷자리 * 5개로 표현  (2020.7.1)
				var aData = data.split(",");
				sReturnValue = aData[0] + " *****";
				break;

			case 'HPID': //홈페이지 ID
				pattern =/^(.+)(.{3})$/;
				if(pattern.test(data)){
					sReturnValue = data.replace(pattern,"$1***");
				}
				break;

			case 'PASSPORT': //여권번호
				var oNewPattern1 =/^([a-zA-Z]{1})([0-9]+)([a-zA-Z]{1})([0-9]+)([0-9]{3})$/; // 신 여권번호 ex ) M12A34567
				var oNewPattern2 =/^([a-zA-Z]{1})([a-zA-Z]{1})([0-9]+)([0-9]{3})$/; // 신여권번호 ex) MA1234567
				var oOldPattern =/^(.+)(.{4})$/; // 구 여권번호

				if (oNewPattern1.test(data)) {
					sReturnValue = data.replace(oNewPattern1 , "$1$2*$4***");
				} else if (oNewPattern2.test(data)) {
					sReturnValue = data.replace(oNewPattern2 , "$1*$3***");
				} else if (oOldPattern.test(data)) {
					sReturnValue = data.replace(oOldPattern,"$1****");
				}

				break;

			case 'DRIVER': // 운전면허번호
				pattern = /^([0-9|ㄱ-ㅎ|ㅏ-ㅣ|가-힣]{2})(-*)([0-9]{1})([0-9]{1})(-*)([0-9]+)([0-9]{2})(-*)([0-9]{1})([0-9]{1})/;

				if(pattern.test(data)){
					sReturnValue = data.replace(pattern,"$1-$3*-$6**-*$10");
				}

				break;

			case 'FOREGINER': // TODO: 오타로 삭제예정 - 외국인등록번호 ()
				pattern = /^([0-9]{6})(-*)([0-9]{1})(.{6})$/;
				//주민번호로 13자리가 안된 값이 넘어오는 경우, 채우기  (#5786 암호화된 문자열의 주민번호도 포멧팅하도록 수정 )(2021.09.01)
                if(data.length < 13) {
                // [#5828] (2021.09.06) minLength -> fixLength로 변경 처리 (fixLength만큼 글자수를 채운다)
                    data = this.revise(data, "rpad", { fixLength : 13 , lengthUnit :"char" , padStr  :"*" });
                }
				if(pattern.test(data)){
					sReturnValue = data.replace(pattern,"$1-$3******");
				}
				break;

            case 'FOREIGNER': // 외국인등록번호
                pattern = /^([0-9]{6})(-*)([0-9]{1})(.{6})$/;
                //주민번호로 13자리가 안된 값이 넘어오는 경우, 채우기  (#5786 암호화된 문자열의 주민번호도 포멧팅하도록 수정 )(2021.09.01)
                if(data.length < 13) {
                // [#5828] (2021.09.06) minLength -> fixLength로 변경 처리 (fixLength만큼 글자수를 채운다)
                    data = this.revise(data, "rpad", { fixLength : 13 , lengthUnit :"char" , padStr  :"*" });
                }
                if(pattern.test(data)){
                    sReturnValue = data.replace(pattern,"$1-$3******");
                }
                break;

			case 'CARD': // 카드번호
				var pattern_14 = /^([0-9]{4})(-*)([0-9]{2})([0-9]{4})(-*)([0-9]{4})$/;
				var pattern_15 = /^([0-9]{4})(-*)([0-9]{2})([0-9]{4})(-*)([0-9]{1})([0-9]{4})$/;
				var pattern_16 = /^([0-9]{4})(-*)([0-9]{2})([0-9]{2})(-*)([0-9]{4})(-*)([0-9]{4})$/;

				if (pattern_14.test(data)){
					sReturnValue = data.replace(pattern_14,"$1-$3****-$6");
				} else if (pattern_15.test(data)){
					sReturnValue = data.replace(pattern_15,"$1-$3****-*$7");
				} else if (pattern_16.test(data)){
					sReturnValue = data.replace(pattern_16,"$1-$3**-****-$8");
				}

				break;
			case 'NAME' :
				if(data.match(/^[ㄱ-ㅎ|ㅏ-ㅣ|가-힣]+$/) ){  //한글인경우
					if(data.length > 2){
						var originName = data.split('');
						originName.forEach(function(name, i){
							if(i == 0 || i === originName.length-1){
								return;
							}
							originName[i] = '*';
						});

						var joinName = originName.join();
						return joinName.replace(/,/g, '');
					}
					else{
						var patternKor = /.$/;
						return data.replace(patternKor, '*')
					}
				}
				else{   //영어인경우
					var names = data.split(" ");
					var lastName = "";
					var maskedName = "";

                    if ( names.length > 1 ){
                        lastName = names.pop(); //성
                    }

					names = names.map(function(name){   //First Name, Middle Name

						//#5141 정규식"((?<="로 인한 ie오류로 인하여 수정처리(2021.06.18)
						var stReplaceIdx = name.length - Math.ceil(name.length / 2);  //replace 시작위치
						var sMaskName = name.substr(0, stReplaceIdx);
						var nMaskCnt  = name.length - stReplaceIdx;

                        for (var idx = 0; idx < nMaskCnt; idx++) {
                            sMaskName = sMaskName + "*";
                        }

						return sMaskName;
					});

					names.forEach(function(each, i){
						maskedName += each + " ";
					});
					maskedName +=  lastName

					return maskedName;
				}
				break;
			default :
				break;	// sReturnValue가 ""이기 때문
		}

		// sReturnValue 가 없을 경우 사용자가 입력한 data 리턴되게 변경.
		// 마스킹 형식과 일치하지 않은 경우, 마스킹 처리되지 않아서 sReturnValue 가 빈값.
		if (sReturnValue == "") {
			sReturnValue = data;
		}

		return sReturnValue;
	}
 };

/**
 * @class 날짜 유틸 클래스
 */
DateUtil = {

    /**
     * 날짜를 지정한 패턴의 문자열로 반환한다.
	 * @param {Date} poDate	날짜
	 * @param {String} psPattern 포맷 문자열(ex: YYYYMMDD)
	 * @return {String} 날짜 문자열
     */
    format : function (poDate, psPattern) { // dateValue As Date, strPattern As String
        var CAL_INITIAL = {
		    MONTH_IN_YEAR :         ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"],
		    SHORT_MONTH_IN_YEAR :   ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"],
		    DAY_IN_WEEK :           ["Sunday", "Monday", "Tuesday", "Wednesday","Thursday", "Friday", "Saturday"],
		    SHORT_DAY_IN_WEEK :     ["Sun", "Mon", "Tue", "Wed","Thu", "Fri", "Sat"]
		};
        
        var year      = poDate.getFullYear();
	    var month     = poDate.getMonth() + 1;
	    var day       = poDate.getDate();
	    var dayInWeek = poDate.getDay();
	    var hour24    = poDate.getHours();
	    var ampm      = (hour24 < 12) ? "AM" : "PM";
	    var hour12    = (hour24 > 12) ? (hour24 - 12) : hour24;
	    var min       = poDate.getMinutes();
	    var sec       = poDate.getSeconds();
	
	    var YYYY = "" + year;
	    var YY   = YYYY.substr(2);
	    var MM   = (("" + month).length == 1) ? "0" + month : "" + month;
	    var MON  = CAL_INITIAL.MONTH_IN_YEAR[month-1];
	    var mon  = CAL_INITIAL.SHORT_MONTH_IN_YEAR[month-1];
	    var DD   = (("" + day).length == 1) ? "0" + day : "" + day;
	    var DAY  = CAL_INITIAL.DAY_IN_WEEK[dayInWeek];
	    var day  = CAL_INITIAL.SHORT_DAY_IN_WEEK[dayInWeek];
	    var HH   = (("" + hour24).length == 1) ? "0" + hour24 : "" + hour24;
	    var hh   = (("" + hour12).length == 1) ? "0" + hour12 : "" + hour12;
	    var mm   = (("" + min).length == 1) ? "0" + min : "" + min;
	    var ss   = (("" + sec).length == 1) ? "0" + sec : "" + sec;
	    var SS   = "" + poDate.getMilliseconds();
		
	    var dateStr;
	    var index = -1;
	    if (typeof(psPattern) == "undefined") {
	        dateStr = "YYYYMMDD";
	    } else {
	        dateStr = psPattern;
	    }
	
	    dateStr = dateStr.replace(/YYYY/g, YYYY);
	    dateStr = dateStr.replace(/yyyy/g, YYYY);
	    dateStr = dateStr.replace(/YY/g,   YY);
	    dateStr = dateStr.replace(/MM/g,   MM);
	    dateStr = dateStr.replace(/MON/g,  MON);
	    dateStr = dateStr.replace(/mon/g,  mon);
	    dateStr = dateStr.replace(/DD/g,   DD);
	    dateStr = dateStr.replace(/dd/g,   DD);
	    dateStr = dateStr.replace(/day/g,  day);
	    dateStr = dateStr.replace(/DAY/g,  DAY);
	    dateStr = dateStr.replace(/hh/g,   hh);
	    dateStr = dateStr.replace(/HH/g,   HH);
	    dateStr = dateStr.replace(/mm/g,   mm);
	    dateStr = dateStr.replace(/ss/g,   ss);
	    dateStr = dateStr.replace(/(\s+)a/g, "$1" + ampm);
	
	    return dateStr;
    },

    /**
     * 올바른 날짜인지를 체크한다.
	 * @param {Number | String} puYear 년도
	 * @param {Number | String} puMonth	월
	 * @param {Number | String} puDay 일
	 * @return {Boolean} 유효한 날짜인지 여부
    */
    isValid : function (puYear, puMonth, puDay) {
    	var pnYear = Number(puYear);
    	var pnMonth = Number(puMonth);
    	var pnDay = Number(puDay);
        var vdDate = new Date(pnYear, pnMonth-1, pnDay);
        return vdDate.getFullYear() == pnYear      &&
               vdDate.getMonth   () == pnMonth - 1 &&
               vdDate.getDate    () == pnDay;
    },

    /**
     * 현재 날짜에 해당 날짜만큼 더한 날짜를 반환한다.
	 * @param {String} psDate 날짜 문자열(format : YYYYMMDD)
	 * @param {Number} pnDayTerm 추가 일수
	 * @return {String} 날짜 문자열
    */
    addDate : function (psDate, pnDayTerm) { 
    	var pnYear 	= Number(psDate.substring(0,4));
    	var pnMonth = Number(psDate.substring(4,6));
    	var pnDay 	= Number(psDate.substring(6,8));

    	if (this.isValid(pnYear, pnMonth, pnDay)) {
	    	var vdDate = new Date(pnYear, pnMonth-1, pnDay);
	    	var vnOneDay = 1*24*60*60*1000 ; /* 1day,24hour,60minute,60seconds,1000ms */
	    	
	    	var psTime = vdDate.getTime() + (Number(pnDayTerm)*Number(vnOneDay));
	    	vdDate.setTime(psTime);
	    	
	        return this.format(vdDate,"YYYYMMDD");
    	}else{
    		return psDate;
    	}
    },
    
    /**
     * 현재 날짜에 해당 월만큼 더한 날짜를 반환한다.
	 * @param {String} psDate 날짜 문자열(format : YYYYMMDD)
	 * @param {Number} pnAddMonth 추가 월
	 * @return {String} 날짜 문자열
    */
    addMonth : function (psDate, pnAddMonth) { 
    	
    	var pnYear 	= Number(psDate.substring(0,4));
    	var pnMonth = Number(psDate.substring(4,6));
    	var pnDay 	= Number(psDate.substring(6,8));

    	if (this.isValid(pnYear, pnMonth, pnDay)) {
    		
	    	var vdDate = new Date(pnYear, pnMonth-1, pnDay);
	    	
	    	vdDate = new Date(vdDate.setMonth(vdDate.getMonth() + Number(pnAddMonth)));	
	    	
	        return this.format(vdDate,"YYYYMMDD");
    	}else{
    		return psDate;
    	}
    },
    
    /**
     * 날짜 문자열을 Date형으로 변환하여 반환한다.
     * <pre><code>
     * DateUtil.toDate("2007-02-09","YYYY-MM-DD");
 	 * </code></pre>
	 * @param {Date} psDateTime	날짜
	 * @param {String} psPattern 포맷 문자열(ex: YYYY-MM-DD)
	 * @example DateUtil.toDate("2007-02-09","YYYY-MM-DD")
	 * @return {Date} 날짜(Date) 객체
     */ 
    toDate : function (psDateTime, psPattern) {
        var vdDate = new Date();
        var vnIdx, vnCnt;

        var vsaFmt = ["Y", "M", "D", "H", "m", "s", "S"];
        var vnFmtLen = vsaFmt.length;
        var vnPtnLen = psPattern.length;
        var vnaNums = [vdDate.getFullYear(), vdDate.getMonth()+1, vdDate.getDate(), vdDate.getHours(), vdDate.getMinutes(), vdDate.getSeconds(), vdDate.getMilliseconds()];

        for (var i = 0; i < vnFmtLen; i++) {
            vnIdx = psPattern.indexOf(vsaFmt[i]);
            if (vnIdx != -1) {
                vnCnt = 1;
                for (var j=vnIdx+1; j < vnPtnLen; j++) {
                    if (psPattern.charAt(j) != vsaFmt[i]) { break; }
                    vnCnt++;
                }
                vnaNums[i] = Number(psDateTime.substring(vnIdx, vnIdx+vnCnt));
            } else {
                if(i==0) vnaNums[0] = 1900;
                else if(i==2) vnaNums[2] = 01;
            }
        }

        if (vnaNums[0] < 1900) { // 년도는 검증
            if (vnaNums[0] <= vdDate.getFullYear() % 100) {
                vnaNums[0] += vdDate.getFullYear() - (vdDate.getFullYear() % 100);
            } else if (vnaNums[0] < 100) {
                vnaNums[0] += 1900;
            } else {
                vnaNums[0] = 1900;
            }
        }

        return new Date(vnaNums[0], vnaNums[1]-1, vnaNums[2], vnaNums[3], vnaNums[4], vnaNums[5], vnaNums[6]);
    },

    /**
     * 해당월의 마지막 일자를 반환한다.
     * <pre><code>
     * DateUtil.getMonthLastDay("20230201");<br>
     * 또는<br>
     * DateUtil.getMonthLastDay("20230301", -1);
 	 * </code></pre>
	 * @param {String} psDate 년월 문자열(format : YYYYMM, YYYYMMDD)
	 * @param {Number} pnAdd? +/- 월 수
	 * @return {Number} 일(Day)
     */ 
    getMonthLastDay : function (psDate, pnAdd) {
    	var pnYear 	= Number(psDate.substring(0,4));
    	var pnMonth = Number(psDate.substring(4,6));
        var vdDate = new Date(pnYear, pnMonth, 0, 1, 0, 0);
        if(pnAdd == null){
        	return vdDate.getDate();
        }else{
        	var vdDate2 = new Date(vdDate.getFullYear(), vdDate.getMonth()+1+pnAdd, 0, 1, 0, 0);
        	return vdDate2.getDate();
        }
    },

    /**
     * 두 날짜간의 일(Day)수를 반환한다.
	 * @param {String} psDate1st 날짜 문자열(format : YYYYMMDD)
	 * @param {String} psDate2nd 날짜 문자열(format : YYYYMMDD)
	 * @return {Number} 일수(Day)
     */
    getDiffDay : function (psDate1st, psDate2nd) {
    	var date1 = this.toDate(psDate1st, "YYYYMMDD");
    	var date2 = this.toDate(psDate2nd, "YYYYMMDD");
        
        return parseInt((date2 - date1)/(1000*60*60*24));
    },
    
    /**
     * 해당 날짜의 하루 전 날짜 반환한다.
     * @param {String} psDate 날짜 문자열(format : YYYYMMDD)
     */
    getBeforeDate : function(psDate){
    	var y = psDate.substring(0, 4);
		var m = psDate.substring(4, 6);
		var d = psDate.substring(6, 8);
		var befDt = new Date(y, m - 1, d - 1);
		var befDtYear = befDt.getFullYear().toString();
		var befDtMonth = new String(befDt.getMonth() + 1);
		var befDtDate = befDt.getDate().toString();
		
		if (befDtMonth.length == 1) befDtMonth = "0" + befDtMonth;
		if (befDtDate.length == 1) befDtDate = "0" + befDtDate;
		
		return befDtYear + befDtMonth + befDtDate + "000000";
    },
    
    /**
     * 입력받은 날짜에 시분초 문자열 000000을 붙여서 반환한다.
     * @param {String} psDate 날짜포맷 문자열
     */
    addZoreDate : function(psDate){
    	var dateString = psDate.substring(0, 8);
		dateString += "000000";
		return dateString;
    },
    
    /**
     * <pre><code>
     *  DateUtil.addMinutes("0900", 50);
     * </code></pre>
     * @param {String} psHHmm 특정분을 더할 시분 값
	 * @param {String} pnAddMinutes 더할 분
	 * @return {String} 시분(HHmm)
     */
    addMinutes : function (psHHmm, pnAddMinutes) {
    	var vdDate = DateUtil.toDate(psHHmm, "HHmm");
		vdDate.setMinutes(vdDate.getMinutes() + pnAddMinutes);
		
		var vnHours = vdDate.getHours();
		var vnMinutes = vdDate.getMinutes();
		
		var vsHours = "";
		var vsMinutes = "";
		
		if(vnHours < 10){
			vsHours = "0" + vnHours;
		}else{
			vsHours = vnHours + "";
		}
		
		if(vnMinutes < 10){
			vsMinutes = "0" + vnMinutes;
		}else{
			vsMinutes = vnMinutes + "";
		}
		
		return vsHours + vsMinutes;
    },
    
    getCurrentTime : function() {
    	return new Date().getTime();
    },
    
    /**
     * 입력한 일자에 해당되는 한글 요일을 반환한다.
     * <pre><code>
     * DateUti.getDayOfWeek("20191120");
 	 * </code></pre>
	 * @param {String} psDate 일자 문자열(ex:20191120)
	 * @return {String} 한글 요일
     */ 
    getDayOfWeek : function (psDate) {
    	
    	var vsYear 	= psDate.substring(0,4);
    	var vsMonth = psDate.substring(4,6);
    	var vsDay 	= psDate.substring(6,8);
    	var vaWeek  = ['일', '월', '화', '수', '목', '금', '토'];
    	
		return vaWeek[new Date(vsYear + "-" + vsMonth + "-" + vsDay).getDay()];
    },
    
    /**
     * yyyyMMdd 형태의 문자열 날짜를 반환한다.
     * <pre><code>
     * DateUtil.makeDate("2010", "05", "01"); // "20100501"
     * </code></pre>
     * @param {String | Number} pnYear	- Year : 년도
     * @param {String | Number} pnMonth	- Month : 월
     * @param {String | Number} pnDate	- Date : 일
     * @example : DateUtil.makeDate("2010", "05", "01");
     * @return {String} 날짜 문자열
     */
    makeDate: function(pnYear, pnMonth, pnDate) {
    	if (ValueUtil.isNull(pnYear) || ValueUtil.isNull(pnMonth) || ValueUtil.isNull(pnDate)) return "";
    	
    	var voNewDate = new Date(pnYear, pnMonth - 1, pnDate);
    	
    	var vsFullYear = voNewDate.getFullYear().toString();
    	var vsMonth = ValueUtil.lPad(voNewDate.getMonth() + 1, "0", 2);
    	var vsDate = ValueUtil.lPad(voNewDate.getDate(), "0", 2);
    	
    	return vsFullYear + vsMonth + vsDate;
    },
    
    /**
     * 현재일자를 입력한 format형식으로 반환한다.
     * <pre><code>
     * DateUtil.getCurrentDay("YYYY-MM-DD");
     * </code></pre>
     * @param {String} psFormat format형식(예:YYYY-MM-DD", "YYYYMMDD", "YY-MM-DD", "YYMMDD", "YYYY-MM-DD HH:mm:ss")
     * @return {String} format에 해당하는 현재일자
     */
    getCurrentDay: function(psFormat) {
    	var now = new Date(this.getCurrentTime());
    	return this.format(now, psFormat);
    }
};
	

/**
 * @class
 * @desc 변수 타입체크 유틸입니다
 */
TypeUtil = {
	/**
	 * 이메일 형식에 맞는지 체크합니다.
	 * @param {String} value
	 * @return {Boolean}
	 */
	isEmail : function(value){
		if(!value) return true;
	
		if(/^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*(\.\w{2,3})+$/.test(value)) {
			return true;
		}
		
		return false;
	},
	
	/**
	 * url 형식에 맞는지 체크합니다.
	 * @param {String} value
	 * @return {Boolean}
	 */
	isURL : function(value){
		if(!value) return true;
	
		// w3resource.com
		var regexp = /^(?:(?:https?|ftp):\/\/)?(?:(?!(?:10|127)(?:\.\d{1,3}){3})(?!(?:169\.254|192\.168)(?:\.\d{1,3}){2})(?!172\.(?:1[6-9]|2\d|3[0-1])(?:\.\d{1,3}){2})(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])(?:\.(?:1?\d{1,2}|2[0-4]\d|25[0-5])){2}(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))|(?:(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)(?:\.(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)*(?:\.(?:[a-z\u00a1-\uffff]{2,})))(?::\d{2,5})?(?:\/\S*)?$/;
		if(regexp.test(value)) {
			return true;
		}
		
		return false;
	},
	/**
	 * 사업자번호 형식에 맞는지 체크합니다.
	 * @param {String} value
	 * @return {Boolean}
	 */
	isBizCSN: function(value){
		if(!value) return true;
	
		// 넘어온 값의 정수만 추츨하여 문자열의 배열로 만들고 10자리 숫자인지 확인합니다.
		if ((value = (value + '').match(/\d{1}/g)).length != 10) {
			return false;
		}
	
		// 합 / 체크키
		var sum = 0, key = [1, 3, 7, 1, 3, 7, 1, 3, 5];
	
		// 0 ~ 8 까지 9개의 숫자를 체크키와 곱하여 합에 더합니다.
		for (var i = 0 ; i < 9 ; i++) { sum += (key[i] * Number(value[i])); }
	
		// 각 8번배열의 값을 곱한 후 10으로 나누고 내림하여 기존 합에 더합니다.
		// 다시 10의 나머지를 구한후 그 값을 10에서 빼면 이것이 검증번호 이며 기존 검증번호와 비교하면됩니다.
		// 나머지를 구한 값이 0인 경우가 있어 %10 로직 추가
		return ((10 - ((sum + Math.floor(key[8] * Number(value[8]) / 10)) % 10)) % 10) == Number(value[9]);
	},
	
	/**
	 * 주민번호 형식에 맞는지 체크합니다.
	 * @param {String} value
	 * @return {Boolean}
	 */
	isSSN : function(value){
		if(!value) return true;
		value = value.replace(/[\-]/g, "");
		
		
		var fmt = /^\d{6}[1234]\d{6}$/;
		if(!fmt.test(value)){
			return false;
		}
	
		var birthYear = (value.charAt(7) <= "2") ? "19" : "20";
		birthYear += value.substr(0, 2);
		var birthMonth = value.substr(2, 2) - 1;
		var birthDate = value.substr(4, 2);
		var birth = new Date(birthYear, birthMonth, birthDate);
	
		if( birth.getFullYear() % 100 != value.substr(0, 2) ||
		    birth.getMonth() != birthMonth ||
		    birth.getDate() != birthDate) {
		    return false;
		}
	
		var arrDivide = [2, 3, 4, 5, 6, 7, 8, 9, 2, 3, 4, 5];            	
		var checkdigit = 0;            	
		for(var i = 0; i < value.length - 1; i++) {
			checkdigit += parseInt(value.charAt(i)) * parseInt(arrDivide[i]);
		}
		checkdigit = (11 - (checkdigit % 11)) % 10;
		if(checkdigit != value.charAt(12)){
			return false;
		} else {
			return true;
		}
	},
	
	/**
	 * 주민번호 형식에 맞는지 체크합니다. (외국인 포함)
	 * @param {String} value
	 * @return {Boolean}
	 */
	isSSNorFrn: function(value) {
		if (!value) return true;
		value = value.replace(/[\-]/g, "");
		
		var fmt = /^\d{6}[12345678]\d{6}$/;
		if (!fmt.test(value)) {
			return false;
		}
		
		var vsChr = value.charAt(6);
		
		var birthYear = (vsChr == "1" || vsChr == "2" || vsChr == "5" || vsChr == "6") ? "19" : "20";
		birthYear += value.substr(0, 2);
		var birthMonth = value.substr(2, 2) - 1;
		var birthDate = value.substr(4, 2);
		var birth = new Date(birthYear, birthMonth, birthDate);
		
		if (birth.getFullYear() % 100 != value.substr(0, 2) ||
			birth.getMonth() != birthMonth ||
			birth.getDate() != birthDate) {
			return false;
		}
		
		var arrDivide = [2, 3, 4, 5, 6, 7, 8, 9, 2, 3, 4, 5];
		var checkdigit = 0;
		for (var i = 0; i < value.length - 1; i++) {
			checkdigit += parseInt(value.charAt(i)) * parseInt(arrDivide[i]);
		}
		
		if (vsChr == "5" ||
			vsChr == "6" ||
			vsChr == "7" ||
			vsChr == "8") { //외국인
			checkdigit = (13 - (checkdigit % 11)) % 10;
		} else {
			checkdigit = (11 - (checkdigit % 11)) % 10;
		}
		
		if (checkdigit != value.charAt(12)) {
			return false;
		} else {
			return true;
		}
	},
	
	/**
	 * 유선전화번호 형식에 맞는지 체크합니다.
	 * @param {String} value
	 * @return {Boolean}
	 */
	isTelNo: function(value) {
		if (!value) return true;
		
		if (/^((0[1678][16789])(\d{3,4})(\d{4}))|((\d{2,3})(\d{3,4})(\d{4}))$/.test(value.replace(/-/g, ""))) {
			return true;
		}
		
		return false;
	},
	
	/**
	 * 핸드폰번호 형식에 맞는지 체크합니다.
	 * @param {String} value
	 * @return {Boolean}
	 */
	isTelMobile: function(value) {
		if (!value) return true;
		
		if (/^(0[1678][016789])(\d{3,4})(\d{4})$/.test(value.replace(/-/g, ""))) {
			return true;
		}
		
		return false;
	},
	
	/**
	 * 해당 값이 'function' 유형인지 여부를 반환한다.
	 * @param {Function} poFunc
	 * @return {Boolean}
	 */
	isFunc: function(poFunc) {
		if (poFunc != null && (typeof poFunc == "function")) {
			return true;
		} else {
			return false;
		}
	},
	
	/**
	 * 해당 값이 '외국인등록번호' 형식에 맞는 문자열인지 여부를 반환한다.
	 * @param {String | Object} value 문자열값
	 * @return {Boolean}
	 */
	isFrno: function(value) {
		value = value.toString().replace(/[\-]/g, "");
		var vnSum = 0;
		if (value.length != 13) return false;
		
		if (value.substr(6, 1) != 5 && value.substr(6, 1) != 6 && value.substr(6, 1) != 7 && value.substr(6, 1) != 8) {
			return false;
		}
		
		if (Number(value.substr(7, 2)) % 2 != 0) return false;
		
		for (var i = 0; i < 12; i++) {
			vnSum += Number(value.substr(i, 1)) * ((i % 8) + 2);
		}
		
		if ((((11 - (vnSum % 11)) % 10 + 2) % 10) == Number(value.substr(12, 1))) {
			return true;
		}
		return false;
	},
	
	/**
	 * 해당 값이 '법인번호' 형식에 맞는 문자열인지 여부를 반환한다.
	 * @param {String | Object} value 문자열값
	 * @return {Boolean}
	 */
	isCorpno: function(value) {
		
		value = value.toString().replace(/[\-]/g, "");
		
		var arr_regno = value.split("");
		var arr_wt = new Array(1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2);
		var iSum_regno = 0;
		var iCheck_digit = 0;
		
		if (value.length != 13) {
			return false;
		}
		
		for (var i = 0; i < 12; i++) {
			iSum_regno += eval(arr_regno[i]) * eval(arr_wt[i]);
		}
		
		iCheck_digit = 10 - (iSum_regno % 10);
		iCheck_digit = iCheck_digit % 10;
		
		if (iCheck_digit != arr_regno[12]) {
			return false;
		}
		return true;
	},
	
	/**
	 * 해당 값이 '카드번호' 형식에 맞는 문자열인지 여부를 반환한다.
	 * @param {String | Object} value 문자열값 (카드번호16자리)
	 * @return {Boolean}
	 */
	isCreditno: function(value) {
		if (ValueUtil.isNull(value)) return false;
		
		value = value.toString().replace(/[\-]/g, "");
		
		if (value.length < 13 || value.length > 19) return false;
		
		var sum = 0;
		var buf = new Array();
		
		for (var i = 0; i < value.length; i++) {
			buf[i] = Number(value.charAt(i));
		}
		
		var temp;
		for (var i = buf.length - 1, j = 0; i >= 0; i--, j++) {
			temp = buf[i] * ((j % 2) + 1);
			if (temp >= 10) {
				temp = temp - 9;
			}
			sum += temp;
		}
		
		if ((sum % 10) == 0) {
			return true;
		} else {
			return false;
		}
	},
	
	/**
	 * 해당 값이 '우편번호' 형식에 맞는 문자열인지 여부를 반환한다.
	 * @param {String | Object} value 문자열값 (5자리)
	 * @return {Boolean}
	 */
	isZipcd: function(value) {
		if (value.length != 5) {
			return false;
		} else {
			return true;
		}
	}
}
