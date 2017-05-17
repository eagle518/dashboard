package com.ruisi.vdop.web.bireport;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;

import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.vdop.util.VDOPUtils;
import com.ruisi.vdop.util.VdopConstant;

public class MyReportAction {
	
	private DaoHelper daoHelper;
	
	private String userId;
	
	private String reportId;
	
	private String reportName; //改名后的名称
	
	private String cataId; //所属目录
	private String keyword; //关键词

	public String tree() throws IOException{
		this.userId = VDOPUtils.getLoginedUser().getUserId();
		HttpServletResponse resp = VDOPUtils.getResponse();
		resp.setContentType("text/xml; charset=UTF-8");
		List ls = daoHelper.getSqlMapClientTemplate().queryForList("bi.report.querymyreport", this);
		String ctx = JSONArray.fromObject(ls).toString();
		resp.getWriter().println(ctx);
		return null;
	}
	
	public String delete(){
		this.userId = VDOPUtils.getLoginedUser().getUserId();
		HttpServletResponse resp = VDOPUtils.getResponse();
		//resp.setContentType("text/xml; charset=UTF-8");
		daoHelper.getSqlMapClientTemplate().delete("bi.report.delReport", this);
		//String ctx = JSONArray.fromObject(ls).toString();
		//resp.getWriter().println(ctx);
		return null;
	}
	
	public String rename(){
		this.userId = VDOPUtils.getLoginedUser().getUserId();
		//HttpServletResponse resp = VDOPUtils.getResponse();
		//resp.setContentType("text/xml; charset=UTF-8");
		daoHelper.getSqlMapClientTemplate().update("bi.report.rename", this);
		//String ctx = JSONArray.fromObject(ls).toString();
		//resp.getWriter().println(ctx);
		return null;
	}
	
	public String list(){
		this.userId = VDOPUtils.getLoginedUser().getUserId();
		List ls = daoHelper.getSqlMapClientTemplate().queryForList("bi.report.listreport", this);
		VDOPUtils.getRequest().setAttribute("ls", ls);
		return "list";
	}

	public DaoHelper getDaoHelper() {
		return daoHelper;
	}

	public void setDaoHelper(DaoHelper daoHelper) {
		this.daoHelper = daoHelper;
	}
	public String getReportId() {
		return reportId;
	}

	public void setReportId(String reportId) {
		this.reportId = reportId;
	}

	public String getReportName() {
		return reportName;
	}

	public void setReportName(String reportName) {
		this.reportName = reportName;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getCataId() {
		return cataId;
	}

	public void setCataId(String cataId) {
		this.cataId = cataId;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
	
}
