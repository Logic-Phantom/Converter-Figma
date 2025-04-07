/************************************************
 * TST20250307.js
 * Created at 2025. 3. 7. 오후 1:23:48.
 *
 * @author LCM
 ************************************************/

/*
 * "Button" 버튼(btn1)에서 click 이벤트 발생 시 호출.
 * 사용자가 컨트롤을 클릭할 때 발생하는 이벤트.
 */
function onBtn1Click(e){
	var btn1 = e.control;
	app.lookup("sms1").send();
}
