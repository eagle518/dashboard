package com.ruisi.vdop.web.m;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.ruisi.ext.engine.ExtConstants;
import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.vdop.util.VDOPUtils;

/**
 * 手机报表分类管理
 * @author hq
 * @date 2016-1-27
 */
public class MobReportTypeAction {
	private DaoHelper daoHelper;
	private String dbName = VDOPUtils.getConstant(ExtConstants.dbName);
	
	private Integer id;
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	private String name;
	private String note;
	private String crtuser;
	private Integer ord;
	
	public String execute(){
		List ls = daoHelper.getSqlMapClientTemplate().queryForList("bi.mbi.listcata",this);
		String strs = JSONArray.fromObject(ls).toString();
		VDOPUtils.getRequest().setAttribute("str", strs);
		return "success";
	}
	
	public String tree() throws IOException{
		List ls = daoHelper.getSqlMapClientTemplate().queryForList("bi.mbi.listcata",this);
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		VDOPUtils.getResponse().getWriter().print(JSONArray.fromObject(ls));
		return null;
	}
	
	public String add() throws IOException{
		this.crtuser = VDOPUtils.getLoginedUser().getUserId();
		if("oracle".equals(dbName)){
			this.id = (Integer)daoHelper.getSqlMapClientTemplate().queryForObject("bi.mbi.maxid2", this);
		}
		this.daoHelper.getSqlMapClientTemplate().insert("bi.mbi.addtype", this);
		int maxid = (Integer)daoHelper.getSqlMapClientTemplate().queryForObject("bi.mbi.getmaxid", this);
		VDOPUtils.getResponse().getWriter().print(maxid);
		return null;
	}
	
	public String mod(){
		this.daoHelper.getSqlMapClientTemplate().update("bi.mbi.updatetype", this);
		return null;
	}
	
	public String del() throws IOException{
		//判断是否有报表
		int cnt = (Integer)daoHelper.getSqlMapClientTemplate().queryForObject("bi.mbi.cntreport", this);
		if(cnt == 0){
			this.daoHelper.getSqlMapClientTemplate().delete("bi.mbi.deltype", this);
		}
		VDOPUtils.getResponse().getWriter().print(cnt);
		return null;
	}
	
	public String get() throws IOException{
		Map m = (Map)this.daoHelper.getSqlMapClientTemplate().queryForObject("bi.mbi.gettype", this);
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		VDOPUtils.getResponse().getWriter().print(JSONObject.fromObject(m));
		return null;
	}
	
	public DaoHelper getDaoHelper() {
		return daoHelper;
	}

	public void setDaoHelper(DaoHelper daoHelper) {
		this.daoHelper = daoHelper;
	}

	public String getName() {
		return name;
	}

	public String getNote() {
		return note;
	}

	public String getCrtuser() {
		return crtuser;
	}

	public Integer getOrd() {
		return ord;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public void setCrtuser(String crtuser) {
		this.crtuser = crtuser;
	}

	public void setOrd(Integer ord) {
		this.ord = ord;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}
	
}
