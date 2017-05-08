<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<%@ taglib prefix="bi" uri="/WEB-INF/common.tld"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	 <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
   <title>手机报表管理</title>
   <script type="text/javascript" src="../ext-res/js/jquery.min.js"></script>
    <script type="text/javascript" src="../ext-res/js/ext-base.js"></script>
	<link rel="stylesheet" type="text/css" href="../ext-res/css/fonts-min.css" />
    <link rel="stylesheet" type="text/css" href="../ext-res/css/boncbase.css?v3" />
	<link rel="stylesheet" type="text/css" href="../resource/jquery-easyui-1.3.4/themes/gray/easyui.css">
	<link rel="stylesheet" type="text/css" href="../resource/jquery-easyui-1.3.4/themes/icon.css">
    <script type="text/javascript" src="../resource/js/cube.js"></script>
	<script type="text/javascript" src="../resource/jquery-easyui-1.3.4/jquery.easyui.min.js"></script>
    <script type="text/javascript" src="../resource/jquery-easyui-1.3.4/locale/easyui-lang-zh_CN.js"></script>
</head>
<style>
<!--
.actColumn {
    line-height: 19px;
	white-space:normal; 
	word-break:break-all;
	margin:3px;
}
.actColumn .column {
    background-color: #FFFF99;
    border: 1px solid #FFD784;
    color: #333333;
    cursor: pointer;
    margin-right: 5px;
}
.cubecfg {
	margin-top:5px;
}
-->
</style>
<script language="javascript">
jQuery(function(){
	var dt = [{id:'zty', text:'手机报表分类', iconCls:'icon-subject', children:${str}}];
	$("#typetree").tree({
		data:dt,
		onClick:function(node){
			var type = node.id;
			if(type == "zty"){
				type = "";
			}
			$('#cubelist').datagrid('load',{
				cataId: type,
				t:Math.random()
			});
		}
	});
	
	$("#cubelist").datagrid({
		singleSelect:true,
		collapsible:false,
		url:'PushManager!list.action',
		pagination:false,
		queryParams:{t:Math.random()},
		method:'get',
		onLoadSuccess:function(data){
			$("#rowcnt").html(data.total);
		},
		onDblClickRow: function(index, data){
			editr();
		},
		toolbar:[{
		    text:'编辑',
		   iconCls:'icon-edit',
		   handler:function(){
		    editr();
		   }
		},{
		    text:'删除',
			   iconCls:'icon-cancel',
			   handler:function(){
		    delr();
			   }
		}],
		onRowContextMenu:function(e,index,row){
			e.preventDefault();
			e.stopPropagation();
			$("#cubelist").datagrid("selectRow", index);
			var offset = {left:e.pageX, top:e.pageY};
			$("#menus").menu("show", {left:offset.left, top:offset.top});
		}
	});
});
function editr(){
	var row = $("#cubelist").datagrid("getChecked");
	if(row == null || row.length == 0){
		$.messager.alert("出错了。","您还未勾选数据。", "error");
		return;
	}
	editReport(row[0].id);
}
function delr(){
	var row = $("#cubelist").datagrid("getChecked");
	if(row == null || row.length == 0){
		$.messager.alert("出错了。","您还未勾选数据。", "error");
		return;
	}
	var data = row[0];
	if(confirm("是否确认删除？")){
		$.ajax({
			url:"PushManager!del.action",
			type:"GET",
			data:{id:data.id},
			dataType:"HTML",
			success:function(){
				$('#cubelist').datagrid('load',{
					t:Math.random()
				});
			}
		});
	}
}
function editReport(id){
	var url = '../portal/PortalIndex!customization.action?is3g=y&pageId='+id+"&menus={back:0,print:0,export:0}";
	var tb = [{
		iconCls:'icon-back',
		text:"返回",
		handler:function(){
			
			var win = document.getElementById("reportInfo").contentWindow;
			if(win.curTmpInfo && win.curTmpInfo.isupdate == true){
				$.messager.confirm("请确认","报表未保存，是否确认关闭？", function(r){
					if(r){
						$("#pdailog").dialog("close");
					}
				});
			}else{
				$("#pdailog").dialog("close");
			}
		}
	}];
	var obj = {
		fit:true,
		border:false,
		closed: false,
		cache: false,
		modal: false,
		noheader:true,
		content:"<iframe id=\"reportInfo\" name=\"reportInfo\" src=\""+url+"\" frameborder=\"0\" width=\"100%\" height=\"100%\"></iframe>",
		toolbar:tb
	};
	$('#pdailog').dialog(obj);
}
function fmtdt(value,row,index){
	var myDate = new Date(value.time);
	return myDate.getFullYear() + "-" + (myDate.getMonth()+1) + "-" + myDate.getDate();
}
</script>
<body>
<div class="pctx2">
<div class="panel-header panel-header-noborder"><div class="panel-title">手机报表管理</div></div>


<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td width="280" valign="top" style="padding:5px;">
<div class="easyui-panel" data-options="width:260,cls:'cubecfg'" title="报表分类">
 <ul id="typetree"></ul>
 </div>
   </td>
   <td valign="top" style="padding-top:10px;">
      <table id="cubelist" title="手机报表列表" style="width:auto;height:auto;" >
      <thead>
      <tr>
      	<th data-options="field:'ck',checkbox:true"></th>
       <th data-options="field:'name',width:195">名称</th>
        <th data-options="field:'typename',width:100">分类</th>
       <th data-options="field:'uname',width:100">创建人</th>
       <th data-options="field:'cdate',width:100,formatter:fmtdt">创建时间</th>
       <th data-options="field:'udate',width:100,formatter:fmtdt">修改时间</th>
       </tr>
       </thead>
       </table>
   </td>
   </tr>
   </table>
<div align="right" style="margin:3px;">共<span style="color:#FF0000; font-size:14px;" id="rowcnt">X</span>条数据。</div>
</div>
<div id="menus" class="easyui-menu">
 	<div iconCls="icon-edit" onclick="editr()" >编辑</div>
    <div iconCls="icon-remove" onclick="delr()" >删除</div>
</div>
<div id="pdailog"></div>
</body>
</html>