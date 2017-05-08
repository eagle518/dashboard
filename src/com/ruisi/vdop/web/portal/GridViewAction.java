package com.ruisi.vdop.web.portal;

import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.ext.engine.view.context.ExtContext;
import com.ruisi.ext.engine.view.context.MVContext;
import com.ruisi.vdop.cache.ModelCacheManager;
import com.ruisi.vdop.ser.olap.CompPreviewService;
import com.ruisi.vdop.ser.portal.PortalGridService;
import com.ruisi.vdop.util.VDOPUtils;

public class GridViewAction {
	
	private String gridJson;
	private String params;
	
	private DaoHelper daoHelper;
	
	public String execute() throws Exception{
		ExtContext.getInstance().removeMV(PortalGridService.deftMvId);
		PortalGridService ser = new PortalGridService();
		JSONObject oGirdJson = JSONObject.fromObject(gridJson);
		JSONArray oParams = JSONArray.fromObject(params);
		ser.setDsource(ModelCacheManager.getDsource(oGirdJson.getString("dsid"), daoHelper));
		ser.setGridJson(oGirdJson);
		ser.setPageParams(oParams);
		ser.setDset(ModelCacheManager.getDset(oGirdJson.getString("dsetId"), daoHelper));
		MVContext mv = ser.json2MV();
		CompPreviewService vser = new CompPreviewService();
		vser.setParams(ser.getMvParams());
		vser.initPreview();
		String ret = vser.buildMV(mv);
		HttpServletResponse resp = VDOPUtils.getResponse();
		resp.setContentType("text/html; charset=UTF-8");
		resp.getWriter().print(ret);
		
		return null;
	}
	
	
	public String getParams() {
		return params;
	}
	
	public void setParams(String params) {
		this.params = params;
	}

	public String getGridJson() {
		return gridJson;
	}

	public void setGridJson(String gridJson) {
		this.gridJson = gridJson;
	}


	public DaoHelper getDaoHelper() {
		return daoHelper;
	}


	public void setDaoHelper(DaoHelper daoHelper) {
		this.daoHelper = daoHelper;
	}

}
