package com.ruisi.vdop.web.m;

import java.io.IOException;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JsonConfig;

import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.vdop.util.JsonDateValueProcessor;
import com.ruisi.vdop.util.VDOPUtils;

public class PushManagerAction {
	
	private DaoHelper daoHelper;
	private String cataId;
	private String id;
	
	public String execute(){
		List ls = daoHelper.getSqlMapClientTemplate().queryForList("bi.mbi.listcata",this);
		String str = JSONArray.fromObject(ls).toString();
		VDOPUtils.getRequest().setAttribute("str", str);
		return "success";
	}
	
	public String list() throws IOException{
		if(this.cataId == null || this.cataId.length() == 0){
			this.cataId = null;
		}
		List ls = daoHelper.getSqlMapClientTemplate().queryForList("bi.portal.list3g", this);
		JsonConfig jsonConfig = new JsonConfig();
		jsonConfig.registerJsonValueProcessor(java.sql.Date.class, new JsonDateValueProcessor());
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		VDOPUtils.getResponse().getWriter().print(JSONArray.fromObject(ls, jsonConfig));
		return null;
	}
	
	public String del(){
		daoHelper.getSqlMapClientTemplate().delete("bi.portal.del3g", this);
		return null;
	}
	
	public DaoHelper getDaoHelper() {
		return daoHelper;
	}


	public void setDaoHelper(DaoHelper daoHelper) {
		this.daoHelper = daoHelper;
	}
	
	public String getCataId() {
		return cataId;
	}

	public void setCataId(String cataId) {
		this.cataId = cataId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	
}
