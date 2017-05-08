package com.ruisi.vdop.web.app;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.struts2.config.ParentPackage;

import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.ext.engine.view.context.ExtContext;
import com.ruisi.ext.engine.view.context.MVContext;
import com.ruisi.vdop.bean.User;
import com.ruisi.vdop.ser.olap.CompPreviewService;
import com.ruisi.vdop.ser.portal.PortalPageService;
import com.ruisi.vdop.util.VDOPUtils;
import com.ruisi.vdop.util.VdopConstant;
import com.ruisi.vdop.web.portal.PortalIndexAction;

@ParentPackage("app-default")
public class ReportAction {
	
	private DaoHelper daoHelper;
	private String cataId;
	private String rid;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	private String token;
	private String pageId;
	private String userId;
	
	public String listCata() throws IOException{
		List ls = daoHelper.getSqlMapClientTemplate().queryForList("bi.portal.queryCata", this);
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		String str = JSONArray.fromObject(ls).toString();
		VDOPUtils.getResponse().getWriter().print(str);
		return null;
	}
	
	public String listReport() throws IOException{
		this.userId = VDOPUtils.getAppUserId();
		HttpServletRequest request = VDOPUtils.getRequest();
		String path = request.getContextPath();
		String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
		List ls = daoHelper.getSqlMapClientTemplate().queryForList("bi.portal.appreport", this);
		for(int i=0; i<ls.size(); i++){
			Map m = (Map)ls.get(i);
			Object o = m.get("dt");
			m.put("dt", sdf.format(o));
			String url = basePath + "app/Report!view.action?rid=" + m.get("rid")+"&token="+token;
			m.put("url", url);
			
		}
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		String str = JSONArray.fromObject(ls).toString();
		VDOPUtils.getResponse().getWriter().print(str);
		return null;
	}
	
	public String view() throws Exception{
		this.pageId = rid;
		DaoHelper dao = VDOPUtils.getDaoHelper();
		
		 //获取报表信息
		String pageInfo = (String)dao.getSqlMapClientTemplate().queryForObject("bi.portal.select", this);
		JSONObject pageJson = JSONObject.fromObject(pageInfo);
		PortalPageService pser = new PortalPageService(pageJson, VDOPUtils.getServletContext());
		ExtContext.getInstance().removeMV(PortalPageService.deftMvId);
		MVContext mv = pser.json2MV(false, false);
		CompPreviewService ser = new CompPreviewService();
		ser.setParams(pser.getMvParams());
		ser.initPreview();
		String ret = ser.buildMV(mv);
		VDOPUtils.getRequest().setAttribute("str", ret);
		return "view";
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

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getRid() {
		return rid;
	}

	public String getPageId() {
		return pageId;
	}

	public void setRid(String rid) {
		this.rid = rid;
	}

	public void setPageId(String pageId) {
		this.pageId = pageId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	
}
