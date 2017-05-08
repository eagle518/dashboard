package com.ruisi.vdop.web.portal;

import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.ext.engine.view.context.ExtContext;
import com.ruisi.ext.engine.view.context.MVContext;
import com.ruisi.vdop.cache.ModelCacheManager;
import com.ruisi.vdop.ser.olap.CompPreviewService;
import com.ruisi.vdop.ser.portal.PortalTableService;
import com.ruisi.vdop.util.VDOPUtils;

public class TableViewAction {
	private String tableJson;
	private String kpiJson;
	private String params; //组建筛选参数
	private String pageParams; //页面参数
	
	private String dset;
	private String dsource;
	private DaoHelper daoHelper;
	
	private static Object lock = new Object();
	
	public String execute() throws Exception{
		synchronized(lock){
			ExtContext.getInstance().removeMV(PortalTableService.deftMvId);
			
			JSONObject tablej = JSONObject.fromObject(tableJson);
			JSONArray kpij = JSONArray.fromObject(kpiJson);
			JSONArray parj = JSONArray.fromObject(params);
			JSONArray ppar = JSONArray.fromObject(pageParams);
			
			//放入request,方便访问
			VDOPUtils.getRequest().setAttribute("tablej", tablej);
			VDOPUtils.getRequest().setAttribute("kpij", kpij);
			//VDOPUtils.getRequest().setAttribute("compId", compId);
			PortalTableService tser = new PortalTableService();
			tser.setDset(ModelCacheManager.getDset(dset, daoHelper));
			tser.setDsource(ModelCacheManager.getDsource(dsource, daoHelper));
			MVContext mv = tser.json2MVByPortal(tablej, kpij, parj, ppar);
			
			CompPreviewService ser = new CompPreviewService();
			ser.setParams(tser.getMvParams());
			ser.initPreview();
			
			String ret = ser.buildMV(mv);
			
			HttpServletResponse resp = VDOPUtils.getResponse();
			resp.setContentType("text/html; charset=UTF-8");
			resp.getWriter().print(ret);
		}
		return null;
	}

	public String getTableJson() {
		return tableJson;
	}

	public String getKpiJson() {
		return kpiJson;
	}

	public String getParams() {
		return params;
	}

	public void setTableJson(String tableJson) {
		this.tableJson = tableJson;
	}

	public void setKpiJson(String kpiJson) {
		this.kpiJson = kpiJson;
	}

	public void setParams(String params) {
		this.params = params;
	}

	public String getPageParams() {
		return pageParams;
	}

	public void setPageParams(String pageParams) {
		this.pageParams = pageParams;
	}

	public String getDset() {
		return dset;
	}

	public String getDsource() {
		return dsource;
	}

	public void setDset(String dset) {
		this.dset = dset;
	}

	public void setDsource(String dsource) {
		this.dsource = dsource;
	}

	public DaoHelper getDaoHelper() {
		return daoHelper;
	}

	public void setDaoHelper(DaoHelper daoHelper) {
		this.daoHelper = daoHelper;
	}
	
	
}
