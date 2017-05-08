package com.ruisi.vdop.web.model;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;

import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.vdop.cache.ModelCacheManager;
import com.ruisi.vdop.ser.report.DBUtils;
import com.ruisi.vdop.ser.webreport.DSColumn;
import com.ruisi.vdop.util.JsonDateValueProcessor;
import com.ruisi.vdop.util.VDOPUtils;

public class DatasetAction {
	
	private DaoHelper daoHelper;
	
	private String dsetId; //数据集ID
	
	private String dsid; //数据源ID
	private String name; //数据集名称
	private String cfg;
	
	private String primary_table; //主表名
	
	private String tname; //表名

	public String list() throws IOException{
		List ls = daoHelper.getSqlMapClientTemplate().queryForList("bi.model.listDset");
		HttpServletResponse resp = VDOPUtils.getResponse();
		resp.setContentType("text/xml; charset=UTF-8");
		JsonConfig jsonConfig = new JsonConfig();
		jsonConfig.registerJsonValueProcessor(java.sql.Date.class, new JsonDateValueProcessor());
		String ctx = JSONArray.fromObject(ls, jsonConfig).toString();
		resp.getWriter().println(ctx);
		return null;
	}
	
	public String listTables() throws Exception{
		Map ds = (Map)daoHelper.getSqlMapClientTemplate().queryForObject("bi.model.getDs", this);
		List tables = DBUtils.queryTables(VDOPUtils.getServletContext(), ds);
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		JsonConfig jsonConfig = new JsonConfig();
		jsonConfig.registerJsonValueProcessor(java.sql.Date.class, new JsonDateValueProcessor());
		String str = JSONArray.fromObject(tables, jsonConfig).toString();
		VDOPUtils.getResponse().getWriter().print(str);
		return null;
	}
	
	public String listTableColumns() throws Exception{
		Map ds = (Map)daoHelper.getSqlMapClientTemplate().queryForObject("bi.model.getDs", this);
		String sql = "select * from " + tname + " where 1=2";
		List ls = DBUtils.queryTableCols(VDOPUtils.getServletContext(), sql, ds);
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		String str = JSONArray.fromObject(ls).toString();
		VDOPUtils.getResponse().getWriter().print(str);
		return null;
	}
	
	public String queryDatasetMeta() throws Exception{
		Map ds = (Map)daoHelper.getSqlMapClientTemplate().queryForObject("bi.model.getDs", this);
		List<DSColumn> ls = DBUtils.queryMetaAndIncome(JSONObject.fromObject(cfg), ds);
		HttpServletResponse resp = VDOPUtils.getResponse();
		resp.setContentType("text/xml; charset=UTF-8");
		String ctx = JSONArray.fromObject(ls).toString();
		resp.getWriter().println(ctx);
		return null;
	}
	
	public String del(){
		daoHelper.getSqlMapClientTemplate().delete("bi.model.delDset", this);
		ModelCacheManager.removeDset(this.dsetId);
		return null;
	}
	
	public String save(){
		daoHelper.getSqlMapClientTemplate().insert("bi.model.saveDset", this);
		ModelCacheManager.addDset(this.dsetId, JSONObject.fromObject(cfg));
		return null;
	
	}
	
	public String update(){
		daoHelper.getSqlMapClientTemplate().update("bi.model.updateDset", this);
		ModelCacheManager.addDset(this.dsetId, JSONObject.fromObject(cfg));
		return null;
	}
	
	public String getJson() throws IOException{
		String m = (String)daoHelper.getSqlMapClientTemplate().queryForObject("bi.model.getDset", this);
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		VDOPUtils.getResponse().getWriter().print(m);
		return null;
	}

	public DaoHelper getDaoHelper() {
		return daoHelper;
	}

	public void setDaoHelper(DaoHelper daoHelper) {
		this.daoHelper = daoHelper;
	}

	public String getDsetId() {
		return dsetId;
	}

	public void setDsetId(String dsetId) {
		this.dsetId = dsetId;
	}

	public String getDsid() {
		return dsid;
	}

	public void setDsid(String dsid) {
		this.dsid = dsid;
	}

	public String getTname() {
		return tname;
	}

	public void setTname(String tname) {
		this.tname = tname;
	}

	public String getPrimary_table() {
		return primary_table;
	}

	public void setPrimary_table(String primaryTable) {
		primary_table = primaryTable;
	}

	public String getName() {
		return name;
	}

	public String getCfg() {
		return cfg;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setCfg(String cfg) {
		this.cfg = cfg;
	}

}
