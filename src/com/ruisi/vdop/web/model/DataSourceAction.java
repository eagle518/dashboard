package com.ruisi.vdop.web.model;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.vdop.cache.ModelCacheManager;
import com.ruisi.vdop.ser.webreport.DBUtils;
import com.ruisi.vdop.ser.webreport.DataService;
import com.ruisi.vdop.util.VDOPUtils;

public class DataSourceAction {
	
	private String linktype;
	private String linkname;
	private String linkpwd;
	private String linkurl;
	private String dsname;
	private String jndiname;
	private String use; //使用jdbc/jndi
	private String dsid;
	
	private DaoHelper daoHelper;
	
	public String save(){
		daoHelper.getSqlMapClientTemplate().insert("bi.model.saveDsource", this);
		ModelCacheManager.removeDsource(dsid);
		return null;
	}
	
	public String update(){
		daoHelper.getSqlMapClientTemplate().update("bi.model.updateDsource", this);
		ModelCacheManager.removeDsource(dsid);
		return null;
	}
	
	public String list() throws IOException{
		List ls = daoHelper.getSqlMapClientTemplate().queryForList("bi.model.list");
		HttpServletResponse resp = VDOPUtils.getResponse();
		resp.setContentType("text/xml; charset=UTF-8");
		String ctx = JSONArray.fromObject(ls).toString();
		resp.getWriter().println(ctx);
		return null;
	}
	
	public String getModel() throws IOException{
		Map m = (Map)daoHelper.getSqlMapClientTemplate().queryForObject("bi.model.getDs", this);
		HttpServletResponse resp = VDOPUtils.getResponse();
		resp.setContentType("text/xml; charset=UTF-8");
		String ctx = JSONObject.fromObject(m).toString();
		resp.getWriter().println(ctx);
		return null;
	}
	
	public String del(){
		daoHelper.getSqlMapClientTemplate().delete("bi.model.delDsource", this);
		ModelCacheManager.removeDsource(dsid);
		return null;
	}
	
	public String testConn() throws IOException{
		String clazz = "";
		if(linktype.equals("mysql")){
			clazz = DataService.mysql;
		}else if(linktype.equals("oracle")){
			clazz = DataService.oracle;
		}else if(linktype.equals("sqlserver")){
			clazz = DataService.sqlserver;
		}
		StringBuffer sb = new StringBuffer();
		boolean ret = DBUtils.testConnection(linkurl, linkname, linkpwd, clazz, sb);
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		VDOPUtils.getResponse().getWriter().print("{\"ret\":"+ret+", \"msg\":\""+sb.toString()+"\"}");
		return null;
	}
	
	public String testJNDI() throws IOException{
		StringBuffer sb = new StringBuffer();
		boolean ret = DBUtils.testJndi(this.jndiname, sb);
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		VDOPUtils.getResponse().getWriter().print("{\"ret\":"+ret+", \"msg\":\""+sb.toString()+"\"}");
		return null;
	}

	public String getLinktype() {
		return linktype;
	}

	public String getLinkname() {
		return linkname;
	}

	public String getLinkpwd() {
		return linkpwd;
	}

	public String getLinkurl() {
		return linkurl;
	}

	public String getDsname() {
		return dsname;
	}

	public String getJndiname() {
		return jndiname;
	}

	public String getUse() {
		return use;
	}

	public void setLinktype(String linktype) {
		this.linktype = linktype;
	}

	public void setLinkname(String linkname) {
		this.linkname = linkname;
	}

	public void setLinkpwd(String linkpwd) {
		this.linkpwd = linkpwd;
	}

	public void setLinkurl(String linkurl) {
		this.linkurl = linkurl;
	}

	public void setDsname(String dsname) {
		this.dsname = dsname;
	}

	public void setJndiname(String jndiname) {
		this.jndiname = jndiname;
	}

	public void setUse(String use) {
		this.use = use;
	}

	public String getDsid() {
		return dsid;
	}

	public void setDsid(String dsid) {
		this.dsid = dsid;
	}

	public DaoHelper getDaoHelper() {
		return daoHelper;
	}

	public void setDaoHelper(DaoHelper daoHelper) {
		this.daoHelper = daoHelper;
	}

	
}
