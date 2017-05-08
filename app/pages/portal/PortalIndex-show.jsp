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
jQuery(function(){
	$("#optarea").load("PortalIndex!view.action?pageId=${pageId}&T="+Math.random());
});
function deleteybp(){
	var url = "PortalIndex!delete.action?pageId=${pageId}";
	if(confirm("是否确认删除？")){
		location.href = url;
	}
}
function printpage() {
	var url2 = "about:blank";
	var name = "printwindow";
	window.open(url2, name);
	var ctx = "<form name='prtff' method='post' target='printwindow' action=\"PortalIndex!print.action\" id='expff'><input type='hidden' name='pageId' id='pageId' value='${pageId}'></form>";
	$(ctx).appendTo("body").submit().remove();
}
function exportpage(){
	var expType = "html";
	var ctx = "<form name='expff' method='post' action=\"Export.action\" id='expff'><input type='hidden' name='type' id='type'><input type='hidden' name='pageId' id='pageId' value='${pageId}'><input type='hidden' name='picinfo' id='picinfo'><div class='exportpanel'><span class='exptp select' tp='html'><img src='../resource/img/export-html.gif'><br>HTML</span>"+
			"<span class='exptp' tp='csv'><img src='../resource/img/export-csv.gif'><br>CSV</span>" +
			"<span class='exptp' tp='excel'><img src='../resource/img/export-excel.gif'><br>EXCEL</span>" + 
			"<span class='exptp' tp='word'><img src='../resource/img/export-word.gif'><br>WORD</span>" + 
			"<span class='exptp' tp='pdf'><img src='../resource/img/export-pdf.gif'><br>PDF</span></div></form>";
	$('#pdailog').dialog({
		title: '导出数据',
		width: 376,
		height: 200,
		closed: false,
		cache: false,
		modal: true,
		toolbar:null,
		content: ctx,
		buttons:[{
					text:'确定',
					iconCls:'icon-ok',
					handler:function(){
						var tp = expType;
						$("#expff #type").val(tp);
						//把图形转换成图片
						var strs = "";
						if(tp == "pdf" || tp == "excel" || tp == "word"){
							$("div.chartUStyle").each(function(index, element) {
                                var id = $(this).attr("id");
								id = id.substring(1, id.length);
								var chart = echarts.getInstanceByDom(document.getElementById(id));
								var str = chart.getDataURL({type:'png', pixelRatio:1, backgroundColor: '#fff'});
								str = str.split(",")[1]; //去除base64标记
								str = $(this).attr("label") + "," + str; //加上label标记
								strs = strs  +  str;
								if(index != $("div.chartUStyle").size() - 1){
									strs = strs + "@";
								}
								
                            });
						}
						$("#expff #picinfo").val(strs);
						$("#expff").submit();
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
	//注册事件
	$(".exportpanel span.exptp").click(function(e) {
		$(".exportpanel span.exptp").removeClass("select");
        $(this).addClass("select");
		expType = $(this).attr("tp");
    });
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
<body >
<div style="width:980px; overflow:auto; margin:auto;">
 <div style=""><a href="PortalIndex!customization.action?pageId=${pageId}" class="easyui-linkbutton" data-options="iconCls:'icon-cut',plain:true">定制报表</a>  <a href="#" onclick="exportpage()" class="easyui-linkbutton" data-options="iconCls:'icon-export',plain:true">导出</a>
  <a href="#" onclick="printpage()" class="easyui-linkbutton" data-options="iconCls:'icon-print',plain:true">打印</a>
  <a href="#" onclick="deleteybp()" class="easyui-linkbutton" data-options="iconCls:'icon-remove',plain:true">删除</a>
   <a href="PortalIndex.action" class="easyui-linkbutton" data-options="iconCls:'icon-back',plain:true">返回</a>
 </div>

<div id="optarea" style=""><img src="../ext-res/image/large-loading.gif">Loading...</div>
</div>

<div id="pdailog"></div>

</body>
</html>