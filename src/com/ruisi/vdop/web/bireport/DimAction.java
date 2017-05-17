package com.ruisi.vdop.web.bireport;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;

import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.vdop.util.VDOPUtils;

public class DimAction {
	
	private String cubeId;
	private DaoHelper daoHelper;
	
	public String queryDims() throws IOException{
		List dims = daoHelper.getSqlMapClientTemplate().queryForList("bi.report.listDims", this);
		//设置维度的 lvlend 属性，在层次维度中判断维度是属于层次维度的最后一级
		for(int i=0; i<dims.size(); i++){
			Map m = (Map)dims.get(i);
			String grouptype = (String)m.get("grouptype");
			if(m.get("dim_ord") == null){
				m.put("dim_ord", "");
			}
			if(m.get("ordcol") == null){
				m.put("ordcol", "");
			}
			if(m.get("dateformat") == null){
				m.put("dateformat", "");
			}
			if(m.get("dim_tname") == null){
				m.put("dim_tname", "");
			}
			if(m.get("tableColName") == null){
				m.put("tableColName", "");
			}
		}
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		String str = JSONArray.fromObject(dims).toString();
		VDOPUtils.getResponse().getWriter().print(str);
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
	
	
}
