package com.ruisi.vdop.web.bireport;

import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.ext.engine.view.context.ExtContext;
import com.ruisi.ext.engine.view.context.MVContext;
import com.ruisi.vdop.cache.ModelCacheManager;
import com.ruisi.vdop.ser.bireport.TableService;
import com.ruisi.vdop.ser.olap.CompPreviewService;
import com.ruisi.vdop.util.VDOPUtils;

/**
 * 编辑时预览action
 * @author hq
 * @date 2013-11-21
 */
public class TableViewAction {
	
	private String tableJson;
	private String kpiJson;
	private String compId; //当前预览组件的ID
	private String params; //外部参数
	private String dsource; //组件使用的数据源
	private String dset; //数据集
	private DaoHelper daoHelper;

	public String execute() throws Exception{
		ExtContext.getInstance().removeMV(TableService.deftMvId);
		
		JSONObject tablej = JSONObject.fromObject(tableJson);
		JSONArray kpij = JSONArray.fromObject(kpiJson);
		JSONArray parj = JSONArray.fromObject(params);
		
		//放入request,方便访问
		VDOPUtils.getRequest().setAttribute("tablej", tablej);
		VDOPUtils.getRequest().setAttribute("kpij", kpij);
		VDOPUtils.getRequest().setAttribute("compId", compId);
		TableService tser = new TableService();
		tser.setDset(ModelCacheManager.getDset(dset, daoHelper));
		tser.setDsource(ModelCacheManager.getDsource(dsource, daoHelper));
		MVContext mv = tser.json2MV(tablej, kpij,  this.compId, parj);
		
		CompPreviewService ser = new CompPreviewService();
		ser.setParams(tser.getMvParams());
		ser.initPreview();
		
		String ret = ser.buildMV(mv);
		
		HttpServletResponse resp = VDOPUtils.getResponse();
		resp.setContentType("text/html; charset=UTF-8");
		resp.getWriter().print(ret);
		
		return null;
	}
	
	public String getTableJson() {
		return tableJson;
	}

	public void setTableJson(String tableJson) {
		this.tableJson = tableJson;
	}

	public String getKpiJson() {
		return kpiJson;
	}

	public void setKpiJson(String kpiJson) {
		this.kpiJson = kpiJson;
	}

	public String getCompId() {
		return compId;
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

	public DaoHelper getDaoHelper() {
		return daoHelper;
	}

	public void setDset(String dset) {
		this.dset = dset;
	}

	public void setDaoHelper(DaoHelper daoHelper) {
		this.daoHelper = daoHelper;
	}

}
