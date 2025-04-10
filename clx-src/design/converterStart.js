/************************************************
 * 20250403.js
 * Created at 2025. 4. 3. 오전 10:57:02.
 *
 * @author LCM
 ************************************************/

/*
 * "Button" 버튼(btn1)에서 click 이벤트 발생 시 호출.
 * 사용자가 컨트롤을 클릭할 때 발생하는 이벤트.
 */
function onBtn1Click(e){
	var btn1 = e.control;
	app.lookup("sms4").send();
}

/*
 * "Button" 버튼에서 click 이벤트 발생 시 호출.
 * 사용자가 컨트롤을 클릭할 때 발생하는 이벤트.
 */
function onButtonClick(e){
	var button = e.control;
	//app.lookup("sms3").send();
	window.location.href = "/oauth/login.do";
}
