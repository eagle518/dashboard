package com.ruisi.vdop.web.bireport;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.vdop.util.VDOPUtils;

public class DataSetAction {
	
	private DaoHelper daoHelper;
	
	private String key;
	private String selectDsIds;
	
	private Integer page; //当前第几页，从1开始
	private Integer rows; //每页的记录数
	
	public String listSubject() throws IOException{
		
		if(this.key != null && this.key.length() > 0){
			this.key = " and (a.cube_name like '%"+key+"%' or a.cube_desc like '%"+key+"%')";
		}
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		List ls = daoHelper.getSqlMapClientTemplate().queryForList("bi.report.listSubject", this, (this.page - 1) * this.rows,this.rows);
		int cnt = (Integer)daoHelper.getSqlMapClientTemplate().queryForObject("bi.report.subject-count", this);
		Map map = new HashMap();
		map.put("total", cnt);
		map.put("rows", ls);
		String str = JSONObject.fromObject(map).toString();
		VDOPUtils.getResponse().getWriter().print(str);
		return null;
	}
	
	/**
	 * 查看已选数据集的指标解释
	 * @return
	 */
	public String kpidesc(){
		List ls = daoHelper.getSqlMapClientTemplate().queryForList("bi.report.listKpiDesc", this);
		VDOPUtils.getRequest().setAttribute("ls", ls);
		return "kpidesc";
	}

	public DaoHelper getDaoHelper() {
		return daoHelper;
	}

	public void setDaoHelper(DaoHelper daoHelper) {
		this.daoHelper = daoHelper;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Integer getPage() {
		return page;
	}

	public Integer getRows() {
		return rows;
	}

	public void setPage(Integer page) {
		this.page = page;
	}

	public void setRows(Integer rows) {
		this.rows = rows;
	}

	public String getSelectDsIds() {
		return selectDsIds;
	}

	public void setSelectDsIds(String selectDsIds) {
		this.selectDsIds = selectDsIds;
	}
	
	
}
