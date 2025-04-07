<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
	response.setStatus(HttpServletResponse.SC_OK);
%>

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Error Page 401</title>

<script type="text/javascript">
	function doOnLoad(){
		alert("사용자 인증에 실패하였습니다.");
		top.close();
	}
</script>

</head>
<body  onload="doOnLoad()">
</body>
</html>