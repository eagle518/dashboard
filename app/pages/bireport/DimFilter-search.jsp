<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
     
     
<div class="dfilter">
<s:iterator var="e" value="#request.datas" status="statu">
<%
String id = (String)pageContext.findAttribute("id");
String ids = (String)request.getAttribute("vals");
if(id != null && id.length() > 0){  //忽略 id 为 null 的
%>	
<div class="fltone"><input type="checkbox" id="dimval" name="dimval" desc="${name}" value="${id}"  <%if(com.ruisi.vdop.web.bireport.DimFilterAction.exist(id, ids.split(","))){%>checked="true"<%}%> > ${name}</div>
<%
}
%>
</s:iterator>
</div>
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
});
</script>
