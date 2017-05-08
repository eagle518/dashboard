<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<%@ taglib prefix="bi" uri="/WEB-INF/common.tld"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	 <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
   <title>手机报表分类管理</title>
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

<script language="javascript">
$(function(){
	var dt = [{id:'zty', text:'手机报表分类', iconCls:'icon-subject', children:${str}}];
	$("#typetree").tree({
		data:dt,
		onContextMenu: function(e, node){
			e.preventDefault();
			$('#typetree').tree('select', node.target);
			if(node.id == 'zty'){
				$('#typeMenu').menu("disableItem", $("#typeMenu #mod"));
				$('#typeMenu').menu("disableItem", $("#typeMenu #del"));
			}else{
				$('#typeMenu').menu("enableItem", $("#typeMenu #mod"));
				$('#typeMenu').menu("enableItem", $("#typeMenu #del"));
			}
			$('#typeMenu').menu('show', {
				left: e.pageX,
				top: e.pageY
			});
		}
	});
});
function addType(update){
	var node = $("#typetree").tree("getSelected");
	var obj;
	if(update){
		$.ajax({
			   type: "GET",
			   async: false,
			   url: "MobReportType!get.action?t="+Math.random(),
			   dataType:"JSON",
			   data: {"id":node.id},
			   success: function(resp){
				  obj = resp;
			   }
		});
	}
	var ord = $("#typetree").tree("getChildren", $("#typetree div[node-id='zty']")).length + 1 ;
	var ctx = "<div class=\"textpanel\"><span class=\"inputtext\">名称：</span><input type=\"text\" id=\"name\" class=\"inputform\" value=\""+(obj&&obj.name!=null?obj.name:"")+"\"><br/><span class=\"inputtext\">说明：</span><input type=\"text\" id=\"note\" class=\"inputform\" value=\""+((obj&&obj.note!=null?obj.note:""))+"\"><br/><span class=\"inputtext\">排序：</span><input type=\"text\" id=\"order\" class=\"inputform\" value=\""+(obj&&obj.ord!=null?obj.ord:ord)+"\"><br/></div>";
	$('#pdailog').dialog({
		title: update?'修改分类':'新建分类',
		width: 350,
		height: 220,
		closed: false,
		cache: false,
		modal: true,
		toolbar:null,
		content: ctx,
		buttons:[{
				text:'确定',
				iconCls:'icon-ok',
				handler:function(){
					var name = $("#pdailog #name").val();
					var note = $("#pdailog #note").val();
					var order = $("#pdailog #order").val();
					if(name == ''){
						$.messager.alert("出错了","名称必须填写。", "error", function(){
							$("#pdailog #name").focus();
						});
						return;
					}
					if(isNaN(order)){
						$.messager.alert("出错了","排序字段必须是数字类型。", "error", function(){
							$("#pdailog #order").select();
						});
						return;
					}
					if(update==false){
						$.ajax({
						   type: "POST",
						   url: "MobReportType!add.action",
						   dataType:"text",
						   data: {"name":name,"note":note,"ord":order},
						   success: function(resp){
							   $("#typetree").tree("append", {parent:$("#typetree div[node-id='zty']"), data:[{id:resp,text:name,iconCls:'icon-subject3'}]});
						   }
						});
					}else{
						$.ajax({
						   type: "POST",
						   url: "MobReportType!mod.action",
						   dataType:"text",
						   data: {"name":name,"note":note,"ord":order, "id":node.id},
						   success: function(resp){
							   $("#typetree").tree("update", {target:node.target, text:name});
						   },
						   error: function(a, b, c){
							   $.messager.alert("出错了。","修改出错。", "error");
						   }
						});
					}
					$('#pdailog').dialog('close');
				}
			},{
				text:'取消',
				iconCls:"icon-cancel",
				handler:function(){
					$('#pdailog').dialog('close');
				}
			}]
	});
}
function delType(){
		if(confirm('是否确认删除？')){
			var node = $("#typetree").tree("getSelected");
			$.ajax({
			   type: "POST",
			   url: "MobReportType!del.action",
			   dataType:"html",
			   data: {"id":node.id},
			   success: function(resp){
				   if(resp == 0){
				   	$("#typetree").tree("remove", node.target);
				   }else{
					   $.messager.alert("出错了。","目录下包含报表，不能删除。", "error");
				   }
			   },
			   error: function(){
				   $.messager.alert("出错了。","删除出错。", "error");
			   }
			});
		}
	}
</script>
<style>
<!--
.cubecfg{
	margin:20px;
}
.textpanel{
	line-height:25px;
	margin:10px;
}
.inputform {
	width:160px;
}
span.inputtext {
	display:inline-block;
	width:90px;
}
-->
</style>

<body>
<div class="pctx2">
<div class="panel-header panel-header-noborder"><div class="panel-title">手机报表分类管理</div></div>


<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td width="10%" valign="top">
     <div class="easyui-panel" data-options="width:260,cls:'cubecfg'" title="报表分类">
 <ul id="typetree"></ul>
 </div>
    </td>
    <td width="90%" valign="top"><div class="cubecfg"> <font color="#FF0000">说明：</font>此处定义的分类在APP报表列表中显示,在分类上点击鼠标右键来新建或编辑分类。</div></td>
  </tr>
</table>

 
 <div style="margin:10px 10px 10px 20px; ">
 <a href="SiteConfig.action" class="easyui-linkbutton" data-options="iconCls:'icon-back'">返回</a>
</div>
</div>

<div id="pdailog"></div>
<div id="typeMenu" class="easyui-menu">
	<div onclick="addType(false)" id="add">新增...</div>
    <div onclick="addType(true)" id="mod">修改...</div>
    <div onclick="delType()" id="del">删除</div>
</div>


</body>
</html>