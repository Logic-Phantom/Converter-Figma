/************************************************
 * expression.module.js
 * Created at 2022. 9. 22. 오후 8:21:47.
 *
 * @author 
 ************************************************/

/**
 * 표현식에서 다국어 반환
 * @param {String} key 키
 * @param {String} defaultValue 기본값
 */
cpr.expression.ExpressionEngine.INSTANCE.registerFunction("getLanguage", function(key, defaultValue){
	var language = null;
	
	try {
	    if(key === null || key === ''){
	        language = defaultValue;
	    } else {
	    	language = cpr.I18N.INSTANCE.message(key, cpr.I18N.INSTANCE.currentLanguage); 
	    }
		
	} catch (err) {
		console.error(err);
	}
	
	return language;
});

//round 함수
cpr.expression.ExpressionEngine.INSTANCE.registerFunction("getRound", function(value, position) {
	if(isNaN(value)) return 0;
	else if(value == Infinity || value == -Infinity) return 0;
	if(position == undefined || position == null) return Math.round(value);
	else return Math.round(value * Math.pow(10, position))/Math.pow(10, position);
});
//floor 함수
cpr.expression.ExpressionEngine.INSTANCE.registerFunction("getFloor", function(value, position) {
	if(isNaN(value)) return 0;
	else if(value == Infinity || value == -Infinity) return 0;
	if(position == undefined || position == null) return Math.floor(value);
	else return Math.floor(value * Math.pow(10, position))/Math.pow(10, position);
});
//숫자 천단위 콤마 포맷
cpr.expression.ExpressionEngine.INSTANCE.registerFunction("formatToNumber", function(value) {
	if(isNaN(value)) return value;
	return new String(value).replace(/\B(?=(\d{3})+(?!\d))/g, ",");
});
//표현식 함수 등록 : 문자열 소문자로 변환
cpr.expression.ExpressionEngine.INSTANCE.registerFunction("doLowerCase", function(value) {
	return value.toLowerCase();
});