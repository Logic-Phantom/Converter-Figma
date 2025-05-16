/************************************************
 * designToken.js
 * Created at 2025. 5. 16. 오후 1:30:24.
 *
 * @author LCM
 ************************************************/

/*
 * "전송" 버튼에서 click 이벤트 발생 시 호출.
 * 사용자가 컨트롤을 클릭할 때 발생하는 이벤트.
 */
function onButtonClick(e){
	var button = e.control;
	app.lookup("subToken").send();
}
