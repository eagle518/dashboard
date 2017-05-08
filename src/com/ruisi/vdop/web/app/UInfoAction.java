package com.ruisi.vdop.web.app;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Map;

import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;

import org.apache.struts2.config.ParentPackage;

import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.vdop.util.JsonDateValueProcessor;
import com.ruisi.vdop.util.VDOPUtils;
import com.ruisi.vdop.util.VdopConstant;

/**
 * 获取用户信息
 * @author hq
 * @date 2016-2-29
 */

@ParentPackage("app-default")
public class UInfoAction {

	private DaoHelper daoHelper;
	private String userId;
	
	public String execute() throws IOException{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		this.userId = VDOPUtils.getAppUserId();
		Map m = (Map)this.daoHelper.getSqlMapClientTemplate().queryForObject("vdop.frame.user.uinfo", this);
		String end = sdf.format(m.get("date_end"));
		m.put("date_end", end);
		String start = sdf.format(m.get("date_start"));
		m.put("date_start", start);
		JsonConfig jsonConfig = new JsonConfig();
		jsonConfig.registerJsonValueProcessor(java.sql.Date.class, new JsonDateValueProcessor());
		String str = JSONObject.fromObject(m, jsonConfig).toString();
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		VDOPUtils.getResponse().getWriter().print(str);
		return null;
	}

	public DaoHelper getDaoHelper() {
		return daoHelper;
	}

	public String getUserId() {
		return userId;
	}

	public void setDaoHelper(DaoHelper daoHelper) {
		this.daoHelper = daoHelper;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	
}
