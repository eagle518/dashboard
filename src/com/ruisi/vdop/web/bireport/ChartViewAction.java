package com.ruisi.vdop.web.bireport;

import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.ext.engine.view.context.ExtContext;
import com.ruisi.ext.engine.view.context.MVContext;
import com.ruisi.ext.engine.view.emitter.ContextEmitter;
import com.ruisi.ext.engine.view.emitter.excel.ExcelEmitter;
import com.ruisi.vdop.cache.ModelCacheManager;
import com.ruisi.vdop.ser.bireport.ChartService;
import com.ruisi.vdop.ser.olap.CompPreviewService;
import com.ruisi.vdop.util.VDOPUtils;

public class ChartViewAction {
	
	public String kpiJson;
	private String chartJson;
	private String compId; //组件的ID
	private String params; //图形参数
	private String dsource; //组件使用的数据源
	private String dset; //数据集
	private DaoHelper daoHelper;
	
	private static Object lock = new Object();
	
	public  String execute() throws Exception{
		synchronized(lock){
			ExtContext.getInstance().removeMV(ChartService.deftMvId);
	
			JSONObject chartj = JSONObject.fromObject(chartJson);
			JSONArray kpij = JSONArray.fromObject(kpiJson);
			JSONArray parj = JSONArray.fromObject(params);
			
			ChartService cs = new ChartService();
			cs.setDset(ModelCacheManager.getDset(dset, daoHelper));
			cs.setDsource(ModelCacheManager.getDsource(dsource, daoHelper));
			MVContext mv = cs.json2MV(chartj, kpij, this.compId, parj, false);
			
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
	
	/**
	 * 导出图形为excel文件，此excel文件可以通过插入图形来生成excel图形
	 * @return
	 * @throws Exception 
	 */
	public String export() throws Exception{
		ExtContext.getInstance().removeMV(ChartService.deftMvId);
		
		JSONObject chartj = JSONObject.fromObject(chartJson);
		JSONArray kpij = JSONArray.fromObject(kpiJson);
		JSONArray parj = JSONArray.fromObject(params);
		
		ChartService cs = new ChartService();
		cs.setDset(ModelCacheManager.getDset(dset, daoHelper));
		cs.setDsource(ModelCacheManager.getDsource(dsource, daoHelper));
		MVContext mv = cs.json2MV(chartj, kpij, this.compId, parj, true);
		
		//放入request方便访问
		VDOPUtils.getRequest().setAttribute("compId", compId);
		VDOPUtils.getRequest().setAttribute("xcolid", cs.getXcolId());
		
		HttpServletResponse resp = VDOPUtils.getResponse();
		resp.setContentType("application/x-msdownload");
		String contentDisposition = "attachment; filename=\"file.xls\"";
		resp.setHeader("Content-Disposition", contentDisposition);
		
		CompPreviewService ser = new CompPreviewService();
		ser.setParams(cs.getMvParams());
		ser.initPreview();
		ContextEmitter emitter = new ExcelEmitter();
		ser.buildMV(mv, emitter);
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

	public String getDsource() {
		return dsource;
	}

	public void setDsource(String dsource) {
		this.dsource = dsource;
	}

	public String getDset() {
		return dset;
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
