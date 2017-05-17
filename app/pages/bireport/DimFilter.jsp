<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>


	<div class="easyui-tabs" id="dimfiltertab">
     	<s:if test="#request.dimType == 'month'">
		<div title="区间筛选"  data-options="">
			<div style="margin:20px;">
            <p/>
            开始月份：   
            <select name="dfm2" id="dfm2">
            <option value=""></option>
                <s:iterator var="e" value="#request.datas" status="statu">
             <option value="${e.id}" <s:if test="id == #request.dfm2">selected</s:if> >${e.name}</option>
             </s:iterator>
            </select>
            <p/>
            结束月份：
            <select name="dfm1" id="dfm1">
            <option value=""></option>
                <s:iterator var="e" value="#request.datas" status="statu">
             <option value="${e.id}" <s:if test="id == #request.dfm1">selected</s:if>>${e.name}</option>
             </s:iterator>
            </select>
			</div>
		</div>
        </s:if>
        <s:if test="#request.dimType == 'day'">
		<div title="区间筛选" style="">
			<div style="margin:20px;">
            <p/>
            开始时间： <input type="text" size="20" value="${dft2}" id="dft2" name="dft2" readonly="true" onclick="WdatePicker({dateFmt:'yyyy-MM-dd', minDate:'${min}', maxDate:'${max}'})" class="Wdate"> <p/>
            结束时间： <input type="text" size="20" value="${dft1}" id="dft1" name="dft1" readonly="true" onclick="WdatePicker({dateFmt:'yyyy-MM-dd', minDate:'${min}', maxDate:'${max}'})" class="Wdate">
            </div>
		</div>
        </s:if>
		<div title="值筛选" <s:if test="#request.filtertype == 2">data-options="selected:true"</s:if>>
<div class="dfilter">
<s:iterator var="e" value="#request.datas" status="statu">
<%
String id = (String)pageContext.findAttribute("id");
String ids = (String)request.getAttribute("vals");
if(id != null && id.length() > 0){  //忽略 id 为 null 的
%>	
<div class="fltone"><input type="checkbox" id="dimval" name="dimval" value="${id}" <%if(com.ruisi.vdop.web.bireport.DimFilterAction.exist(id, ids.split(","))){%>checked="true"<%}%> > ${name} <s:if test="#request.dimType == 'day'"><!-- 追加日期的节日 -->
<%=com.ruisi.vdop.web.bireport.DimFilterAction.getFestival((String)pageContext.findAttribute("id"), (String)request.getAttribute("dateformat"))%>
</s:if></div>
<%
}
%>
</s:iterator>
</div>
		</div>
	</div>
     <s:if test="#request.dimType == 'frd'">
    <div id="dimsearch_div" >
    <input id="dimsearch" style="width:290px;"></input>
    </div>
     </s:if>

<script>
jQuery(function(){
	$(".dfilter .fltone").mousemove(function(){
		$(this).css("background-color","#FFF4D7");
	}).mouseout(function(e){
        $(this).css("background-color","#FFFFFF");
    }).click(function(e) {
		if(e.target.id == 'dimval'){
			return;
		}
        var obj = $(this).find("#dimval");
		if(obj.attr("checked") == "checked"){
			obj.attr("checked", false) 
		}else{
			obj.attr("checked", true);
		}
    });
	$('#dimsearch').searchbox({
		searcher:function(value,name){
			searchDims(value, '<%=request.getAttribute("vals")%>');
		},
		prompt:'请输入查询关键字.'
	});
	var filtertype = "${param.filtertype}";
	$('#dimfiltertab').tabs({
		border:false,
		plain:false,
		<s:if test="#request.dimType == 'frd'">
		width: 290,
		height:250,
		</s:if>
		<s:elseif test="#request.dimType == 'month' || #request.dimType == 'day'">
		width: 290,
		height:230,
		</s:elseif>
		<s:elseif test="#request.dimType == 'year' || #request.dimType == 'quarter'">
		width: 290,
		height:230,
		</s:elseif>
		tabPosition: 'top',
		tools:[{
			text:"清除",
			handler:function(){
				$("#pdailog input[name='dimval']").each(function(){
					$(this).attr("checked", false);
				});
			}
		},{
			text:"全选",
			handler:function(){
				$("#pdailog input[name='dimval']").each(function(){
					$(this).attr("checked", true);
				});
			}
		}]
	});
});
</script>