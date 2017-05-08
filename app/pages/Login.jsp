<%@page contentType="text/html; charset=UTF-8"%>
<%@page contentType="text/html; charset=UTF-8"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>睿思BI-开源商业智能|数据可视化系统 - 用户登录</title>
<link rel="shortcut icon" type="image/x-icon" href="resource/img/rs_favicon.ico">

<link rel="stylesheet" href="resource/css/ht.css" type="text/css">
<link rel="stylesheet" type="text/css" href="resource/jquery-easyui-1.3.4/themes/gray/easyui.css">
<link rel="stylesheet" type="text/css" href="resource/jquery-easyui-1.3.4/themes/icon.css">
<script type="text/javascript" src="ext-res/js/jquery.js"></script>
<script type="text/javascript" src="ext-res/js/ext-base.js"></script>
<script type="text/javascript" src="resource/jquery-easyui-1.3.4/jquery.easyui.min.js"></script>
<style type="text/css">
<!--
body  {
	margin-left: 0px;
	margin-top: 0px;
	margin-right: 0px;
	margin-bottom: 0px;
	font-family: "songti";
	font-size: 12px;
	background-image: url(resource/img/bg.png);
	background-color:#D6DEE0;
	background-repeat: no-repeat;
	background-position: top center;
}
-->
</style>
<script language="javascript">
if(window.top != window.self){
	window.top.location.href = 'Login.action'
}

function chkpw(ff){
	if(ff.userName.value == ''){
		alert('请输入账号！');
		return false;
	}
	if(ff.password.value == ''){
		alert('请输入口令！');
		return false;
	}
}
</script>
</head>

<body>

<div class="denglu " id="denglu">
		<form name="form1" method="post" action="Login!login.action" onsubmit="return chkpw(document.form1)">
        <div style="margin-bottom:30px;"><img src="resource/img/rsyun.png"></div>
   <table width="508" border="0" align="center" cellpadding="0" cellspacing="0" style="background-image:url(resource/img/tcs.png); background-repeat:no-repeat;">
    <tr>
      <td height="99" colspan="3" align="center" valign="middle"><div style="padding-top:20px;" align="center">
      <img src="resource/img/logtitle.png">
      </div></td>
    </tr>

    <tr>
      <td width="284" rowspan="3" align="center">
      <img src="resource/img/xsqq.png">
      </td>
      <td width="49" height="43" align="left">帐号：</td>
      <td width="175" align="left"><input name="userName" type="text" size="24" style="height:20px; font-size:12px; width: 140px;" value="${userName}" placeholder="您的账号"/></td>
      
    </tr>

    <tr>
      <td height="31" align="left">口令：</td>
      <td align="left"><input name="password" type="password" size="24"  style="height:20px; font-size:12px; width: 140px;" placeholder="您的密码"/></td>
    </tr>
    <tr>
      <td height="32" align="left">&nbsp;</td>
      <td align="left" height="32">
      <input name="button3" src="resource/img/login.png" type="submit"  value=" 登 录 " />
      </td>
    </tr>

   
    <tr>

      <td height="62" align="left" colspan="">
      </td>
      <td></td>
      <td>
      
      </td>
    </tr>
  </table>
  </form>
  
  管理员账号： admin/123456
  
  <div align="center" style="color:#666; font-size:14px; margin:20px;">
   © <a href="http://www.ruisitech.com" target="_blank" style="color:#666;">北京睿思科技有限公司</a> 2016 版权所有</div>
</div>

	<%
      	String einfo = (String)request.getAttribute("errorInfo");
      	if(einfo != null && einfo.length() > 0){
      	%>
      	<script>
      		jQuery(function(){
      			alert("操作失败：${errorInfo}");
      		});
      		</script>
      <%}%>
<div id="pdailog"></div>
</body>
</html>
