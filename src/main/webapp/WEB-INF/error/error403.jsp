<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
	response.setStatus(HttpServletResponse.SC_OK);
%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Error Page 403</title>


<script type="text/javascript">
	function doOnLoad(){
		alert("접속권한이 없는 페이지에 접근하였습니다.");
	}
</script>

</head>
<body onload="doOnLoad()">
</body>
</html>