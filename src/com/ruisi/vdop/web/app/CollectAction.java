package com.ruisi.vdop.web.app;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;

import org.apache.struts2.config.ParentPackage;

import com.ruisi.ext.engine.ExtConstants;
import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.vdop.util.JsonDateValueProcessor;
import com.ruisi.vdop.util.VDOPUtils;
import com.ruisi.vdop.util.VdopConstant;

/**
 * app 收藏夹
 * @author hq
 * @date 2016-3-31
 */

@ParentPackage("app-default")
public class CollectAction {
	
	private DaoHelper daoHelper;
	private String dbName = VDOPUtils.getConstant(ExtConstants.dbName);
	private String rid;
	private String userId;
	private String token;
	
	public String list() throws IOException{
		this.userId = VDOPUtils.getAppUserId();
		HttpServletRequest request = VDOPUtils.getRequest();
		String path = request.getContextPath();
		String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
		List ls = daoHelper.getSqlMapClientTemplate().queryForList("bi.3g.listCollect", this);
		for(int i=0; i<ls.size(); i++){
			Map m = (Map)ls.get(i);
			String url = basePath + "app/Report!view.action?rid=" + m.get("rid")+"&token="+token;
			m.put("url", url);
			
		}
		JsonConfig jsonConfig = new JsonConfig();
		jsonConfig.registerJsonValueProcessor(java.sql.Date.class, new JsonDateValueProcessor());
		String str = JSONArray.fromObject(ls, jsonConfig).toString();
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		VDOPUtils.getResponse().getWriter().print(str);
		return null;
	}
	
	public String add() throws IOException {
		this.userId = VDOPUtils.getAppUserId();
		Map ret = new HashMap();
		int cnt = (Integer)daoHelper.getSqlMapClientTemplate().queryForObject("bi.3g.collectExist", this);
		if(cnt > 0){
			ret.put("result", false);
		}else{
			ret.put("result", true);
			daoHelper.getSqlMapClientTemplate().insert("bi.3g.addCollect", this);
		}
		String str = JSONObject.fromObject(ret).toString();
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		VDOPUtils.getResponse().getWriter().print(str);
		return null;
	}
	
	public String delete() throws IOException {
		this.userId = VDOPUtils.getAppUserId();
		daoHelper.getSqlMapClientTemplate().delete("bi.3g.delCollect", this);
		String str = this.rid;
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		VDOPUtils.getResponse().getWriter().print(str);
		return null;
	}

	public DaoHelper getDaoHelper() {
		return daoHelper;
	}


	public String getDbName() {
		return dbName;
	}

	public void setDaoHelper(DaoHelper daoHelper) {
		this.daoHelper = daoHelper;
	}


	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public String getUserId() {
		return userId;
	}


	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getRid() {
		return rid;
	}

	public void setRid(String rid) {
		this.rid = rid;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
	
	
}
