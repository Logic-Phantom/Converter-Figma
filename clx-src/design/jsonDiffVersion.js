/************************************************
 * jsonDiff.js
 * Created at 2025. 4. 23. 오후 4:00:38.
 *
 * @author LCM
 ************************************************/
/*
 * "Button" 버튼에서 click 이벤트 발생 시 호출.
 * 사용자가 컨트롤을 클릭할 때 발생하는 이벤트.
 */
function onButtonClick(e){
	var button = e.control;
	app.lookup("subFile").send();
}
