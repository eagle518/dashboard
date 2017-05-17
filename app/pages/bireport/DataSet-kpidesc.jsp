<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<table class="grid3" id="T_report54" cellpadding="0" cellspacing="0">
<thead>
<tr class="scrollColThead" style="background-color:#FFF">
	<th  class="null" colspan="1"  rowspan="1" width="40%">度量</th>
	<th  class="null" colspan="1"  rowspan="1" width="60%">解释</th>
</tr>
	<s:iterator var="e" value="#request.ls" status="statu">
<tr>
 <td class='kpiData1 grid3-td' align="left">${e.kpi_name}</td>	
 <td class='kpiData1 grid3-td' align="left" style="color:#666">${e.name_desc}</td>
</tr>
 </s:iterator>

</thead>
</table>