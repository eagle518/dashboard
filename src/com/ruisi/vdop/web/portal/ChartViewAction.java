package com.ruisi.vdop.web.portal;

import javax.servlet.http.HttpServletResponse;

import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.ext.engine.view.context.ExtContext;
import com.ruisi.ext.engine.view.context.MVContext;
import com.ruisi.vdop.cache.ModelCacheManager;
import com.ruisi.vdop.ser.olap.CompPreviewService;
import com.ruisi.vdop.ser.portal.PortalChartService;
import com.ruisi.vdop.util.VDOPUtils;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class ChartViewAction {
	
	public String kpiJson;
	private String chartJson;
	private String compId; //组件的ID
	private String params; //图形筛选参数
	private String pageParams; //页面参数
	
	private String dsource; //数据源id
	private String dset; //数据集ID
	
	private DaoHelper daoHelper;
	
	private static Object lock = new Object();
	
	public String chartType(){
		return "chartType";
	}
	
	public  String execute() throws Exception{
		synchronized(lock){
			ExtContext.getInstance().removeMV(PortalChartService.deftMvId);
	
			JSONObject chartj = JSONObject.fromObject(chartJson);
			JSONArray kpij = JSONArray.fromObject(kpiJson);
			JSONArray parj = JSONArray.fromObject(params);
			JSONArray ppar = JSONArray.fromObject(pageParams);
			PortalChartService cs = new PortalChartService();
			cs.setDset(ModelCacheManager.getDset(dset, daoHelper));
			cs.setDsource(ModelCacheManager.getDsource(dsource, daoHelper));
			MVContext mv = cs.json2MVByPortal(chartj, kpij, this.compId, parj, ppar);
			
			//放入request方便访问
			VDOPUtils.getRequest().setAttribute("compId", compId);
			VDOPUtils.getRequest().setAttribute("xcolid", cs.getXcolId());
			
			CompPreviewService ser = new CompPreviewService();
			ser.setParams(cs.getMvParams());
			ser.initPreview();
			
			String ret = ser.buildMV(mv);
			
			HttpServletResponse resp = VDOPUtils.getResponse();
			resp.setContentType("text/html; charset=UTF-8");
			resp.getWriter().print(ret);
		}
		
		return null;
	}

	public String getKpiJson() {
		return kpiJson;
	}

	public String getChartJson() {
		return chartJson;
	}

	public String getCompId() {
		return compId;
	}
	
	public void setKpiJson(String kpiJson) {
		this.kpiJson = kpiJson;
	}

	public void setChartJson(String chartJson) {
		this.chartJson = chartJson;
	}

	public void setCompId(String compId) {
		this.compId = compId;
	}

	public String getParams() {
		return params;
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

	public String getDsource() {
		return dsource;
	}

	public String getDset() {
		return dset;
	}

	public void setDsource(String dsource) {
		this.dsource = dsource;
	}

	public void setDset(String dset) {
		this.dset = dset;
	}

	public DaoHelper getDaoHelper() {
		return daoHelper;
	}

	public void setDaoHelper(DaoHelper daoHelper) {
		this.daoHelper = daoHelper;
	}
	
	

}
