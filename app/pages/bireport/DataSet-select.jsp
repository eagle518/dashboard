<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<table class="grid3" id="T_report54" cellpadding="0" cellspacing="0">
<thead>
<tr class="scrollColThead" style="background-color:#FFF">
	<th  class="null" colspan="1"  rowspan="1">选择</th>
	<th  class="null" colspan="1"  rowspan="1">表序号</th>
	<th  class="null" colspan="1"  rowspan="1">表名称</th>
	<th  class="null" colspan="1"  rowspan="1">表说明</th>
</tr>
	<s:iterator var="e" value="#request.ls" status="statu">
<tr>
	<td class='kpiData1 grid3-td'><input type="checkbox" id="selectdataset" name="selectdataset" value="${e.def_tid}" /></td>	
 <td class='kpiData1 grid3-td'>${e.ds_id}</td>	
 <td class='kpiData1 grid3-td' align="left">${e.name}</td>
 <td class='kpiData1 grid3-td' align="left" style="color:#666">${e.note}</td>
</tr>
 </s:iterator>

</thead>
</table>