<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>

<s:if test="#request.dim.type == 'day'">
	<div style="margin:20px;">
	<p/>
	开始时间： <input type="text" size="20" value="${dft2}" id="dft2" name="dft2" readonly="true" onclick="WdatePicker({dateFmt:'yyyy-MM-dd', minDate:'2012-01-01', maxDate:'${maxdt}'})" class="Wdate"> <p/>
	结束时间： <input type="text" size="20" value="${dft1}" id="dft1" name="dft1" readonly="true" onclick="WdatePicker({dateFmt:'yyyy-MM-dd', minDate:'2012-01-01', maxDate:'${maxdt}'})" class="Wdate">
	</div>
</s:if>

<s:if test="#request.dim.type == 'month'">
	<div style="margin:20px;">
	<p/>
		开始月份：   
		<select name="dfm2" id="dfm2">
			<s:iterator var="e" value="#request.months" status="statu">
		 <option value="${e.mid}" <s:if test="mid == #request.dfm2">selected</s:if> >${e.mname}</option>
		 </s:iterator>
		</select>
		<p/>
		结束月份：
		<select name="dfm1" id="dfm1">
			<s:iterator var="e" value="#request.months" status="statu">
		 <option value="${e.mid}" <s:if test="mid == #request.dfm1">selected</s:if>>${e.mname}</option>
		 </s:iterator>
		</select>
	</div>
</s:if>