<%@page import="org.json.simple.*"%>
<%@page import="java.util.*"%>
<%@page import="java.io.*"%>
<%@page contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>

<%
	request.setCharacterEncoding("utf-8");
	
	String strAppIdDefault = "app/main/Main" ;
    String strAppId = request.getParameter("appId");
    String strOpenWindwSeq = request.getParameter("_openWindowSeq");
	String initValue = request.getParameter("initValue");
	
	if(strAppId != null && strAppId != ""){
		strAppIdDefault = strAppId;
	}
%>

<!DOCTYPE html>
<html>
<base href="/ui/">
<meta charset="UTF-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta http-equiv="Expires" content="-1">
<meta http-equiv="Pragma" content="no-cache">
<meta http-equiv="Cache-Control" content="No-Cache">
<meta name="viewport" content="width=device-width, user-scalable=no">
<script type="text/javascript" src="/resource/cleopatra.js"></script>
<script type="text/javascript" src="/resource/conf/defaults.js"></script>
<script type="text/javascript" src="cpr-lib/language.js"></script>
<script type="text/javascript" src="cpr-lib/user-modules.js"></script>
<script type="text/javascript" src="cpr-lib/udc.js"></script>
<script type="text/javascript" src="<%=strAppIdDefault%>.clx.js"></script>

<link rel="stylesheet" type="text/css" href="/resource/css/cleopatra.css">
<link rel="stylesheet" type="text/css" href="theme/cleopatra-theme.css">
<link rel="stylesheet" type="text/css" href="theme/custom-theme.css">
<link rel="stylesheet" type="text/css" href="theme/pb/exbsp-theme.css">
	 
<style>
	/* for HTML5 */
	html, body {
		margin: 0px;
		padding: 0px;
		height: 100%;
	}
	body{
		box-sizing: content-box;
		min-height: 100%;
	}
</style>

<script>
	var vpOpenerAppInstance = null; // 부모 화면 앱 인스턴스
	var voPopAppInstance = null; // 팝업으로 띄워진 현재 앱 인스턴스

	function getOpenAppInstance() {
		return voOpenerAppInstance;
	}

	function getPopAppInstance() {
		return voPopAppInstance;
	}

	function setOnload() {
		setOpenAppInstance(window.openApp);
	}
	
	function setOpenAppInstance(poApp) {
		voOpenerAppInstance = poApp;
	}
</script>
</head>
<body onload="javascript:setOnload();">
	<input type="hidden" id="openWindowSeq" value="<%=strOpenWindwSeq%>" />
	<script type="text/javascript">
		var app = cpr.core.Platform.INSTANCE.lookup("<%=strAppIdDefault%>");
		var appInstance = app.createNewInstance();
	
		appInstance.addEventListenerOnce("unload", function(e) {
			window.close();
		});
	
		appInstance.run(null, function(poAppIns) {
			poAppIns.setAppProperty("_winPopInitValue", <%=initValue%>);
			poAppIns.setAppProperty("_popupInfo", {
				popupType : "WIN"
			});
		});
	
		voPopAppInstance = appInstance;
	</script>
</body>
</html>
