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
	app.lookup("subFigma").send();
}

/*
 * 루트 컨테이너에서 load 이벤트 발생 시 호출.
 * 앱이 최초 구성된후 최초 랜더링 직후에 발생하는 이벤트 입니다.
 */
function onBodyLoad(e){
	var token = cpr.core.Platform.INSTANCE.getParameter("token");
	
	if(token){
		app.lookup("dmParam").setValue("token", token);
	}else{
		alert("토큰을 받지 못했습니다.");
	}
	
}
