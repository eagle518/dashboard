package com.ruisi.vdop.web.model;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.vdop.ser.report.CubeService;
import com.ruisi.vdop.util.VDOPUtils;

public class CubeAction {
	
	private DaoHelper daoHelper;
	
	private String cubeId; //立方体ID
	private String dsetId; //数据集ID
	
	private String targetId;
	
	private String cfg;
	
	public String list() throws IOException{
		List ls = daoHelper.getSqlMapClientTemplate().queryForList("bi.model.listCube", this);
		HttpServletResponse resp = VDOPUtils.getResponse();
		resp.setContentType("text/xml; charset=UTF-8");
		String ctx = JSONArray.fromObject(ls).toString();
		resp.getWriter().println(ctx);
		return null;
	}
	
	public String getCube() throws IOException{
		Map cube = (Map)this.daoHelper.getSqlMapClientTemplate().queryForObject("bi.model.getCube", this);

		//获取立方体所属表的字段列表
		this.dsetId = (String)cube.get("dsetId");
		String m = (String)daoHelper.getSqlMapClientTemplate().queryForObject("bi.model.getDset", this);
		JSONObject dset = JSONObject.fromObject(m);
		cube.put("cols", dset.get("cols"));
		
		//动态字段
		cube.put("dynamic", dset.get("dynamic"));
		
		//获取立方体维度
		List dims = daoHelper.getSqlMapClientTemplate().queryForList("bi.model.getTableDims", this);
		cube.put("dims", dims);
		
		//获取立方体指标
		List kpis = daoHelper.getSqlMapClientTemplate().queryForList("bi.model.getTableKpis", this);
		cube.put("kpis", kpis);
		
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		VDOPUtils.getResponse().getWriter().print(JSONObject.fromObject(cube));
		return null;
	}
	
	public String save(){
		JSONObject cubeJson = JSONObject.fromObject(cfg);
		CubeService cube = new CubeService(cubeJson, daoHelper);
		cube.saveCube();
		return null;
	}
	
	public String del(){
		//删除表
		this.daoHelper.getSqlMapClientTemplate().delete("bi.model.delCube", this);
		this.daoHelper.getSqlMapClientTemplate().delete("bi.model.deleteCubeMeta", this);
		//删除维度
		this.daoHelper.getSqlMapClientTemplate().delete("bi.model.deldimByCubeId", this);
		//删除指标
		this.daoHelper.getSqlMapClientTemplate().delete("bi.model.delkpiByCubeId", this);
		//删除分组
		this.daoHelper.getSqlMapClientTemplate().delete("bi.model.delgroup", this);
		return null;
	}
	
	public String update(){
		JSONObject cubeJson = JSONObject.fromObject(cfg);
		CubeService cube = new CubeService(cubeJson, daoHelper);
		cube.updateCube();
		return null;
	}

	public DaoHelper getDaoHelper() {
		return daoHelper;
	}

	public void setDaoHelper(DaoHelper daoHelper) {
		this.daoHelper = daoHelper;
	}

	public String getCubeId() {
		return cubeId;
	}

	public void setCubeId(String cubeId) {
		this.cubeId = cubeId;
	}

	public String getCfg() {
		return cfg;
	}

	public void setCfg(String cfg) {
		this.cfg = cfg;
	}

	public String getDsetId() {
		return dsetId;
	}

	public void setDsetId(String dsetId) {
		this.dsetId = dsetId;
	}

	public String getTargetId() {
		return targetId;
	}

	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}

}
