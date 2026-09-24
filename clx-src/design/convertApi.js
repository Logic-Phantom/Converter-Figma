/************************************************
 * convertApi.js
 * Created at 2026. 9. 24.
 *
 * Figma 링크 + Swagger/OpenAPI URL → /design/convertApi.do
 * (DataSet/DataMap/Submission 바인딩 + JS 스켈레톤, 선택: 시각 QA)
 ************************************************/

/*
 * "변환" 버튼에서 click 이벤트 발생 시 호출.
 */
function onBtnConvertClick(e){
	var dm = app.lookup("dmParam");
	if (!dm.getValue("url")) {
		alert("Figma 링크를 입력하세요.");
		return;
	}
	app.lookup("txaResult").value = "변환 중...";
	app.lookup("subConvert").send();
}

/*
 * 변환 응답(text/plain) 을 결과 영역에 표시한다.
 */
function onSubConvertSubmitDone(e){
	var submission = e.control;
	var text = submission.xhr && submission.xhr.responseText ? submission.xhr.responseText : "(응답 없음)";
	app.lookup("txaResult").value = text;
}
