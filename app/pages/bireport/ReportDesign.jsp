<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8" import="com.ruisi.vdop.web.bireport.ReportDesignAction"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<%@ taglib prefix="bi" uri="/WEB-INF/common.tld"%>
<%
boolean showtit = true;
String stit = request.getParameter("showtit");
if(stit != null && stit.length() > 0){
	showtit = "true".equals(stit);
}
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
  <title><s:if test="pageName != null && pageName !=''">${pageName} - </s:if>多维分析工具(OLAP)</title>
  
  <script type="text/javascript" src="../ext-res/js/jquery.min.js"></script>
  <link rel="stylesheet" type="text/css" href="../resource/jquery-easyui-1.3.4/themes/gray/easyui.css"/>
  <link rel="stylesheet" type="text/css" href="../resource/jquery-easyui-1.3.4/themes/icon.css"/>
  <script type="text/javascript" src="../resource/jquery-easyui-1.3.4/jquery.easyui.min.js"></script>
  <script language="javascript" src="../resource/js/bireport.js?v2"></script>
  <script language="javascript" src="../resource/js/bitable.js"></script>
  <script language="javascript" src="../resource/js/bichart.js"></script>
  <script language="javascript" src="../resource/js/bidrill.js"></script>
   <link rel="stylesheet" type="text/css" href="../ext-res/css/fonts-min.css" />
	<link rel="stylesheet" type="text/css" href="../ext-res/css/boncbase.css?v3" />
	<link rel="stylesheet" type="text/css" href="../resource/css/bireport.css?v3" />
  
	<script type="text/javascript" src="../ext-res/My97DatePicker/WdatePicker.js"></script>
	<script language="javascript" src="../resource/js/json.js"></script>
    <script type="text/javascript" src="../ext-res/js/echarts.min.js"></script>
</head>

<script language="javascript">

<%
String pageInfo = (String)request.getAttribute("pageInfo");
if(pageInfo == null){
	%>
	var pageInfo = {"selectDs":'${selectDs}', comps:[{"name":"表格组件","id":1, "type":"table"}], params:[]};
	var isnewpage = true;
	<%
}else{
%>
	var pageInfo = <%=pageInfo%>;
	var isnewpage = false;
<%}%>
var showtit = <%=showtit%>;
var curTmpInfo = {"menus":"${menus}"}; //临时对象
curTmpInfo.isupdate = false; //页面是否已经修改
curTmpInfo.chartpos = "left";  //too/left 表示图形配置属性的位
$(function(){
	
	//初始化TAB信息
	$("#l_tab").tabs({fit:true,border:false});
	
	//初始化数据集
	reloadDatasetTree();
	if(pageInfo.selectDs == ''){
		$("#datasettree").tree("options").dnd = false;
	}
	
	//初始化我的报表
	//loadMyReportTree();
	
	//初始化视图
	initviewTree();
	
	//初始化参数
	initparam();
	
	//初始化默认组件
	for(i=0;i<pageInfo.comps.length; i++){
		var t = pageInfo.comps[i];		
		if(t.type =="customComp"){
			addComp(t.id, t.name, str, false, t.type, isnewpage ? null : t.json,false,t.customCompType);
		}else{
			var str = t.type == 'text' ? t.text.replace(/\n/g,"<br>") : null;
			addComp(t.id, t.name, str, false, t.type, isnewpage ? null : t);
		}
	}
	
	

	
	//初始化关闭按钮图标颜色,及删除动作事件
	$(".comp_table .title a.easyui-linkbutton").live("click", function(evt){		
		var id = $(this).attr("pid");
		if($(this).hasClass("btn_delete")){
			delComp(id);
			return ;
		}
		var name = $(this).attr("name");
		var cmpInfo=findCompById(id)
		if(cmpInfo == null){
			return;
		}
		var type=cmpInfo.type;		
		if(type in customComp){
			var cmp=customComp[type];
			if(toolsHandler.toolsHandler){
				toolsHandler.toolsHandler.call(window,id,name,this);
			}			
		}
		
	});
	$(".comp_table .title .ticon a").live("mouseover", function(){
		$(this).css("opacity", 1);
	}).live("mouseout", function(){
		$(this).css("opacity", 0.6);
	});
	$(".dimoptbtn,.one_p,.charticon").live("mouseover", function(){
		$(this).css("opacity", 1);
	}).live("mouseout", function(){
		$(this).css("opacity", 0.6);
	});
	$(".dimDrill,.dimgoup,.chartdrillDim a").live("mouseover", function(){
		$(this).css("opacity", 1);
	}).live("mouseout",function(){
		$(this).css("opacity", 0.5);
	});
	
	<%if(!ReportDesignAction.isShowMenu("view", request)){%>
	$("#viewtreediv").hide();
	$("#l_tab li.tabs-selected").next().hide();
	<%}%>
});

</script>

<body class="easyui-layout" id="layout_data">

<%if(ReportDesignAction.isShowMenu("menu", request)){%>
	<div region="north" border="false" style="height:33px;background:#ffffff; overflow:hidden;">
		<%
        	if(showtit){
        %>
       
        <%
			}else{
		%>
        
        <%
			}
		%>
        <div class="panel-header" style="padding:3px; background-image:url(../ext-res/image/white-top-bottom.gif)">
        <%if(ReportDesignAction.isShowMenu("open", request)){%>
            <a href="javascript:openreport();" id="mb8" class="easyui-linkbutton" plain="true" iconCls="icon-open">打开</a>
        <%}%>
        <%if(ReportDesignAction.isShowMenu("new", request)){%>
            <a href="javascript:newpage()" id="mb1" class="easyui-linkbutton" plain="true" iconCls="icon-newpage" title="新建查询页面">新建</a>
         <%}%>
         <%if(ReportDesignAction.isShowMenu("save", request)){%>
            <a href="javascript:savepage()" id="mb2" class="easyui-linkbutton" plain="true" iconCls="icon-save" title="保存查询页面">保存</a>
         <%}%>
          <%if(ReportDesignAction.isShowMenu("data", request)){%>
            <a href="javascript:selectdataset()" id="mb3" class="easyui-linkbutton" plain="true" iconCls="icon-dataset" title="选择分析主题">数据</a>
            <%}%>
             <%if(ReportDesignAction.isShowMenu("insert", request)){%>
            <a href="javascript:void(0)" id="mb4" class="easyui-menubutton" plain="true" iconCls="icon-add" menu="#insertcompmenu" title="插入组件">插入</a>
            <%}%>
             
              <%if(ReportDesignAction.isShowMenu("export", request)){%>
            <a href="javascript:exportPage()" id="mb6" class="easyui-linkbutton" plain="true" iconCls="icon-export" title="导出数据">导出</a>
            <%}%>
            <%if(ReportDesignAction.isShowMenu("push", request)){
				/**
				%>
            <a href="javascript:pushData()" id="mb9" class="easyui-linkbutton" plain="true" iconCls="icon-push" title="推送数据">推送</a>
            <%
			**/
			}%>
            <%if(ReportDesignAction.isShowMenu("print", request)){%>
            <a href="javascript:printData()" id="mb10" class="easyui-linkbutton" plain="true" iconCls="icon-print" title="打印数据">打印</a>
            <%}%>
             <%if(ReportDesignAction.isShowMenu("desc", request)){%>
            <a href="javascript:kpidesc()" id="mb8" class="easyui-linkbutton" plain="true" iconCls="icon-kpidesc" title="查看指标解释">解释</a>
            <%}%>
             <%if(ReportDesignAction.isShowMenu("dm", request)){%>
            <a href="javascript:helper()" id="mb13" class="easyui-linkbutton" plain="true" iconCls="icon-help" title="帮助">帮助</a>
            <%}%>
        </div>
    </div>
	 <%}%>
     
     <!-- 配置数据是否隐藏 -->
	<%if(ReportDesignAction.isShowMenu("showData", request)){%>
	<div region="west" split="true" style="width:200px;" title="对象浏览">
    	<div id="l_tab" class="easyui-tabs" style="height:auto; width:auto;">
        	<div title="数据" style="">
        		<div id="datasettreediv">
				</div>    
            </div>
            <div title="视图" style="" id="viewtreediv">
            	<ul id="viewtree" class="easyui-tree">
				</ul>  
            </div>
        </div>
    </div>
    <%}%>
    
	<div region="center" title="操作区" >
    	   <div class="easyui-layout" data-options="fit:true">
                <div data-options="region:'north',split:true,border:true" id="p_param" class="param" style="height:37px">
                         <div class="ptabhelpr">
                            拖拽维度到此处作为页面参数
                         </div>
                </div>
                <div data-options="region:'center',border:false" id="optarea"  style="padding:5px;">
                     
                </div>
              </div>
	</div>

<div id="insertcompmenu" style="width:150px;">
		<div onclick="insertTable()" >插入表格</div>
		<div onclick="insertChart()" >插入图形...</div>
        <div onclick="insertText('insert')">插入文本...</div>
</div>

<div id="pdailog"></div>
<div class="indicator">==></div>
<div id="kpioptmenu" class="easyui-menu">
	<div>
    	<span>计算</span>
   	   <div style="width:120px;">
    	<div onclick="kpicompute('sq')">上期值</div>
        <div onclick="kpicompute('tq')">同期值</div>
        <div onclick="kpicompute('zje')">增减额</div>
        <div onclick="kpicompute('hb')">环比(%)</div>
        <div onclick="kpicompute('tb')">同比(%)</div>
        <div class="menu-sep"></div>
        <div onclick="kpicompute('sxpm')">升序排名</div>
        <div onclick="kpicompute('jxpm')">降序排名</div>
        <div onclick="kpicompute('zb')">占比(%)</div>
        <div onclick="kpicompute('ydpj')">移动平均</div>
       </div>
    </div>
	<div onclick="kpiproperty()">属性...</div>
    <div onclick="crtChartfromTab()">图形...</div>
    <div onclick="kpiFilter('table')">筛选...</div>
    <div onclick="kpiwarning()">预警...</div>
    <div>
    <span>排序</span>
    <div style="width:120px;">
    	<div id="k_kpi_ord1" onclick="kpisort('asc')">升序</div>
        <div id="k_kpi_ord2"  onclick="kpisort('desc')">降序</div>
        <div id="k_kpi_ord3" iconCls="icon-ok" onclick="kpisort('')">默认</div>
    </div>
    </div>
    <div iconCls="icon-remove" onclick="delJsonKpiOrDim('kpi')">删除</div>
</div>
<div id="dimoptmenu" class="easyui-menu">
	<div onclick="dimsort('asc')">升序</div>
    <div onclick="dimsort('desc')">降序</div>
    <div>
    <span>移动</span>
    <div style="width:120px;">
    	<div iconCls="icon-back" onclick="dimmove('left')">左移</div>
        <div iconCls="icon-right" onclick="dimmove('right')">右移</div>
        <div id="m_moveto" onclick="dimexchange()">移至</div>
    </div>
    </div>
    <div iconCls="icon-reload" onclick="changecolrow(true)">行列互换</div>
    <div iconCls="icon-filter" onclick="filterDims()">筛选...</div>
    <div iconCls="icon-sum" onclick="aggreDim()" id="m_aggre">聚合</div>
    <div onclick="getDimTop()" id="m_aggre">取Top...</div>
    <div onclick="delJsonKpiOrDim('dim')" iconCls="icon-remove">删除</div>
</div>
<div id="chartoptmenu" class="easyui-menu">
	<div onclick="chartsort('asc')">升序</div>
    <div onclick="chartsort('desc')">降序</div>
    <div iconCls="icon-filter" onclick="chartfilterDims()" >筛选...</div>
    <div onclick="setChartKpi()" id="m_set">属性...</div>
    <div onclick="delChartKpiOrDim()" iconCls="icon-remove">清除</div>
</div>
<div class='chartloading' id="Cloading"><div class="ldclose" onclick="hideLoading()"></div><div class="ltxt">Loading...</div></div>
</body>
</html>