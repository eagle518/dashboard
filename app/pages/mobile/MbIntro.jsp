<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<%@ taglib prefix="bi" uri="/WEB-INF/common.tld"%>
<%
String path = request.getContextPath();
String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
	 <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
   <title>移动BI介绍</title>
<link rel="shortcut icon" type="image/x-icon" href="../resource/img/rs_favicon.ico">
   <script type="text/javascript" src="../ext-res/js/jquery.min.js"></script>
	<link rel="stylesheet" type="text/css" href="../ext-res/css/fonts-min.css" />
	<link rel="stylesheet" type="text/css" href="../ext-res/css/boncbase.css?v2" />  
	<script type="text/javascript" src="../ext-res/My97DatePicker/WdatePicker.js"></script>
	<script language="javascript" src="../resource/js/json.js"></script>
    <script language="javascript" src="../ext-res/js/ext-base.js"></script>
	<link rel="stylesheet" type="text/css" href="../resource/jquery-easyui-1.3.4/themes/gray/easyui.css">
	<link rel="stylesheet" type="text/css" href="../resource/jquery-easyui-1.3.4/themes/icon.css">
	<script type="text/javascript" src="../resource/jquery-easyui-1.3.4/jquery.easyui.min.js"></script>
    <script type="text/javascript" src="../resource/jquery-easyui-1.3.4/locale/easyui-lang-zh_CN.js"></script>
</head>


<style>
<!--
.cubecfg{
	margin:20px;
}
-->
</style>

<body>
<div>
<div class="panel-header panel-header-noborder"><div class="panel-title">移动BI介绍</div></div>

<div style="margin:20px; line-height:25px; font-size:14px;">
 通过移动端访问数据报表。 移动BI的报表通过PC端制作，完成后在移动端访问。 <br/>
<br/>
<font color="#FF0000"> 移动端访问报表方法：</font> <br/>
 1. 点击 移动BI - 创建手机报表 菜单来创建一个新的报表(创建报表前请先建立分类)。<br/>
 2. 下载手机APP, 使用注册的账号登录后点击 手机报表 菜单访问刚才创建的报表。<br/> <br/>
 
  <font color="#FF0000">安卓app下载地址： </font><a target="_blank" href="http://shouji.baidu.com/software/10060077.html">http://shouji.baidu.com/software/10060077.html</a> （百度手机市场）<br/>
   <font color="#FF0000">iOS app下载地址： </font><a target="_blank" href="https://itunes.apple.com/cn/app/rui-si-yun-qi-ye-zai-xian/id1099734064?mt=8">https://itunes.apple.com/cn/app/rui-si-yun-qi-ye-zai-xian/id1099734064?mt=8</a> (app Store下载)<br/>
 
  <table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td width="50%" align="center">
    
     <img src="http://www.ruisitech.com/img/az.png">
 <br/>
 安卓客户端下载
    
    </td>
    <td align="center">
    <img src="http://www.ruisitech.com/img/iosyun.png">
 <br/>
 IOS客户端下载
    </td>
  </tr>
</table>

</div>

 



</body>
</html>