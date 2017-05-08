<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<%@ taglib prefix="bi" uri="/WEB-INF/common.tld"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	 <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
   <title>睿思云 - 数据报表</title>
<link rel="shortcut icon" type="image/x-icon" href="../resource/img/rs_favicon.ico">
   <script type="text/javascript" src="../ext-res/js/jquery.min.js"></script>
   <script type="text/javascript" src="../ext-res/js/ext-base.js"></script>
	<link rel="stylesheet" type="text/css" href="../ext-res/css/fonts-min.css" />
	<link rel="stylesheet" type="text/css" href="../ext-res/css/boncbase.css" />
	<link rel="stylesheet" type="text/css" href="../resource/css/portal.css?v2" />
	<link rel="stylesheet" type="text/css" href="../resource/jquery-easyui-1.3.4/themes/gray/easyui.css">
	<link rel="stylesheet" type="text/css" href="../resource/jquery-easyui-1.3.4/themes/icon.css">
	<script type="text/javascript" src="../resource/jquery-easyui-1.3.4/jquery.easyui.min.js"></script>
    <script type="text/javascript" src="../ext-res/js/echarts.min.js"></script>
    <script language="javascript" src="../ext-res/js/sortabletable.js"></script>
    <script type="text/javascript" src="../ext-res/My97DatePicker/WdatePicker.js"></script>
</head>
<script language="javascript">
$(function(){
	$("div.myrep").hover(function(e){
		$(this).css("background-color", "#FFFF99");
	}, function(e){
		$(this).css("background-color", "inherit");
	}).bind("contextmenu", function(e){
		window.curId = $(this).attr("cid");
		var offset = {left:e.pageX, top:e.pageY};
		$("#reportmenu").menu("show", {left:offset.left, top:offset.top});
		return false;
	});
});
function renamereport(){
	var id = window.curId;
	$.messager.prompt('报表改名', '请输入新的报表名称？', function(msg){
		if(msg){
			$.ajax({
				  type: "POST",
				   url: "PortalIndex!rename.action",
				   dataType:"HTML",
				   data: {pageId:id, pageName:msg},
				   success: function(resp){
					   $("div.myrep[cid=\""+id+"\"] a.link").text(msg);
				   },
				   error:function(){
					  
				   }
			});
		}
	});
}
function delreport(){
	var id = window.curId;
	if(confirm('是否确认删除？')){
		$.ajax({
			  type: "POST",
			   url: "PortalIndex!del.action",
			   dataType:"HTML",
			   data: {pageId:id},
			   success: function(resp){
				  $("div.myrep[cid=\""+id+"\"]").remove();
			   },
			   error:function(){
				  
			   }
		});
	}
}
function newreport(){
	location.href = 'PortalIndex!customization.action';
}
function editreport(){
	var id = window.curId;
	location.href = 'PortalIndex!customization.action?pageId=' + id;
}
</script>
<style>
<!--
body {
	background-color:#f1f5f6;
	background-image: url(../resource/img/ditu.jpg);
	background-repeat:repeat-y;
	background-position:center top;
}
-->
</style>
<body>

<div style="width:960px; margin:0 auto;">
 <div style="margin:3px 3px 3px 3px;"><a href="PortalIndex!customization.action" class="easyui-linkbutton" data-options="iconCls:'icon-add',plain:true">新建报表</a>
 </div>
 <div style="margin:3px; color:#666;"><font color="#990000">说明：</font>在报表上点击右键，可对选定的报表进行编辑。</div>



<div id="optarea" style="width:1020px; overflow:auto; margin:auto;" align="center">
<br/>
 <s:iterator var="e" value="#request.ls" status="statu">
 <div style="width:135px; height:90px; float:left; font-size:14px; padding-top:3px;" class="myrep" cid="${e.id}">
 <a href="PortalIndex!show.action?pageId=${e.id}"><img src="../resource/img/ybp.png" border="0"></a><br/>
 <a href="PortalIndex!show.action?pageId=${e.id}" class="link">${e.name}</a>
 </div>
 </s:iterator>
</div>

</div>

<div id="reportmenu" class="easyui-menu">
 	<div iconCls="icon-add" onclick="newreport()" >新建</div>
    <div iconCls="icon-edit" onclick="renamereport()" >改名</div>
    <div iconCls="icon-cut" onclick="editreport()" >定制</div>
    <div onclick="delreport()" iconCls="icon-remove">删除</div>
</div>

</body>
</html>