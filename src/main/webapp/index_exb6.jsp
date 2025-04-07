<%@ page language="java"
    contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"
    session="false"
%>

<%
	String strContextPath = request.getContextPath();
%>
   <!-- 
   	eXBuilder6 앱 welcome page 호출용 jsp
    -->
<!DOCTYPE html>
<html>
	<head>
		<base href="<%=strContextPath%>/ui/" />
		
		<meta charset="utf-8"/>
        <meta name="viewport" content="user-scalable=1, initial-scale=1.0, width=device-width"/>
        <meta name="og:image" content="http://edu.tomatosystem.co.kr/theme/images/com/exb6-logo-og-image.png"/>
        <meta name="og:title" content="eXCFrame"/>
        <meta name="description" content="eXBuilder6, 엑스빌더6, 각종 기능 예제, 유형별 화면 템플릿, 공통모듈 예제 데모를 확인 할 수 있습니다."/>
        <meta name="og:url" content="http://edu.tomatosystem.co.kr"/>
        <meta name="og:description" content="eXBuilder6 각종 기능 예제, 유형별 화면 템플릿, 공통모듈 예제 데모를 확인 할 수 있습니다."/>
        <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
        <title>eXCFrame-eXBuilder6 기능 예제</title>
        <link rel="stylesheet" media="all" href="../resource/css/cleopatra.css?v=1.0.5243"/>
        <link rel="stylesheet" media="all" href="theme/cleopatra-theme.css?v=1.0.5243"/>
        <link rel="stylesheet" media="all" href="theme/custom-theme.css?v=1.0.5243"/>
        <link rel="stylesheet" media="all" href="theme/pb/exbps.css"/>
        <script src="../resource/cleopatra.js?v=1.0.5243" defer></script>
        <script src="../resource/conf/defaults.js?v=1.0.5243" defer></script>
        <script src="cpr-lib/language.js?v=1.0.5243" defer></script>
        <script src="cpr-lib/user-modules.js?v=1.0.5243" defer></script>
        <script src="cpr-lib/udc.js?v=1.0.5243" defer></script>
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
	</head>
<script>
	function doload() {
		
		/*
		cpr.core.ResourceLoader.setQueryProvider(function(originURL,allowsCache) {
			var qryParam = {};
			if (!allowsCache) {
				qryParam = {
					"v" : "20240910"//moment().format("YYYYMMDD-HH")
				}
			}
			return qryParam;
		});
		*/
		pageLoadingImg();

		//root instance에 app/com/main/main앱 지정
		cpr.core.App.load("app/main/Main", function(loadedApp) {
			loadedApp.createNewInstance().run();
		});
	}

	function pageLoadingImg() {
		var pli = document.getElementById("pageLoadingImage");
		if (pli) {
			pli.parentNode.removeChild(pli);
		}
	}
</script>
</head>
<img id="pageLoadingImage"
	style="position: absolute; top: calc(50% - 75.0px); left: calc(50% - 125.0px); width: 250px; height: 150px"
	src="data:image/gif;base64,R0lGODlh+gCWAKIDAISEhMbGxv///wAAAAAAAAAAAAAAAAAAACH/C05FVFNDQVBFMi4wAwEAAAAh+QQFCgADACwAAAAA+gCWAAAD/ii63P4wykmrvTjrzbv/YCiOZGmeaKqubOu+cCzPdG3feK7vfO//wKBwSCwaj8ikcslsOp/QqHRKrVqv2Kx2y+16v+CweEwum8/otHrNbrvf8Lh8Tq/b7/i8fs/v+/+AgYKDhIWGh4iJiouMjY6PkJGSk5SVlpeYmZqbnJ2en6ChoqOkpaanqKmqq6ytrq+wsbKztLW2t7i5uru8vb6/wMGTAcTFxgvGyQHIysXMzcsK0MTC0tPPzdjK2sncx9XT0QLh3s7W0OXU4Nfn2e3b793x38Lk8+bj7Pno9+r1+vb2uRMIj6C8f/wM0gvIUF+whgkhDgz4EKDFiBcHVsTIZXFixoIbPXYsKBFktZMoU6pcybKly5cwY8qcSbOmzZs4c+rcybOnz59AgwodSrSo0aNIkypdyrSp06dQo0qdSrWq1atYs2rdyrWr169gw4odS7as2bNo06pdy7at27dw48qdKyABACH5BAUKAAMALF4ARQAKAAsAAAMKCLrc/jDKSattCQAh+QQFCgADACxeAEUAFwALAAADJRi6LA4wSkaDEzKD2p6O3HJ9YGh5pHli5GaOrQqnL/rJtoazZAIAIfkEBQoAAwAsawBFABcACwAAAyUYuiwOMEpGgxMyg9qejtxyfWBoeaR5YuRmjq0Kpy/6ybaGs2QCACH5BAUKAAMALHgARQAXAAsAAAMlGLosDjBKRoMTMoPano7ccn1gaHmkeWLkZo6tCqcv+sm2hrNkAgAh+QQFCgADACyFAEUAFwALAAADJRi6LA4wSkaDEzKD2p6O3HJ9YGh5pHli5GaOrQqnL/rJtoazZAIAOw=="
	alt="Loading Image" />

<body onload="javascript:doload();">
	</body>
</html>