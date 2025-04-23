/************************************************
 * jsonDiff.js
 * Created at 2025. 4. 23. 오후 4:00:38.
 *
 * @author LCM
 ************************************************/

/*
 * 파일 인풋에서 value-change 이벤트 발생 시 호출.
 * FileInput의 value를 변경하여 변경된 값이 저장된 후에 발생하는 이벤트.
 */
function onFi1ValueChange(e){
	var fileinput1 = e.control;
	var vaFiles = fileinput1.files;
	
	if(vaFiles != null && vaFiles.length > 0){
		var submit = app.lookup("subFile");
		
		var voFile;
		for(var i = 0, len = vaFiles.length; i < len; i++){
			voFile = vaFiles[i];
			//허용 가능 파일 유형 체크
				
			submit.addFileParameter(voFile.name, voFile);
		}
		
        app.lookup("subFile").send();
	}
}
