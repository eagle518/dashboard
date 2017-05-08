package com.ruisi.vdop.web.portal;

import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.ext.engine.view.context.ExtContext;
import com.ruisi.ext.engine.view.context.MVContext;
import com.ruisi.vdop.cache.ModelCacheManager;
import com.ruisi.vdop.ser.olap.CompPreviewService;
import com.ruisi.vdop.ser.portal.PortalBoxService;
import com.ruisi.vdop.util.VDOPUtils;

/**
 * 数据块视图
 * @author hq
 * @date 2017-3-12
 */
public class BoxViewAction {
	
	private String kpiJson;
	private String params; //组件筛选参数
	private String pageParams; //页面参数
	
	private String dsource; //数据源id
	private String dset; //数据集ID
	
	private DaoHelper daoHelper;
	
	private static Object lock = new Object();
	
	public String execute() throws Exception{
		synchronized(lock){
			ExtContext.getInstance().removeMV(PortalBoxService.deftMvId);
			JSONObject kpij = JSONObject.fromObject(kpiJson);
			JSONArray pj = JSONArray.fromObject(params);
			JSONArray ppj = JSONArray.fromObject(pageParams);
			PortalBoxService bser = new PortalBoxService();
			bser.setDset(ModelCacheManager.getDset(dset, daoHelper));
			bser.setDsource(ModelCacheManager.getDsource(dsource, daoHelper));
			MVContext mv = bser.json2MV(kpij, pj, ppj);
			CompPreviewService ser = new CompPreviewService();
			ser.setParams(bser.getMvParams());
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

	public String getParams() {
		return params;
	}

	public String getPageParams() {
		return pageParams;
	}

	public void setKpiJson(String kpiJson) {
		this.kpiJson = kpiJson;
	}

	public void setParams(String params) {
		this.params = params;
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

	public static Object getLock() {
		return lock;
	}

	public void setDsource(String dsource) {
		this.dsource = dsource;
	}

	public void setDset(String dset) {
		this.dset = dset;
	}

	public static void setLock(Object lock) {
		BoxViewAction.lock = lock;
	}

	public DaoHelper getDaoHelper() {
		return daoHelper;
	}

	public void setDaoHelper(DaoHelper daoHelper) {
		this.daoHelper = daoHelper;
	}
	
	
}
