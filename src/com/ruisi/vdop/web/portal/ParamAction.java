package com.ruisi.vdop.web.portal;

import java.util.List;
import java.util.Map;

import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.vdop.util.VDOPUtils;
import com.ruisi.vdop.util.VdopConstant;

/**
 * 仪表盘的参数
 * @author hq
 * @date 2015-7-30
 */
public class ParamAction {
	
	private String dimId;
	
	private DaoHelper daoHelper;
		
	private List datas;
	
	private String selType = ""; //选择类型，单选还是多选
	
	private String talign; //根据talign判断参数是横向还是纵向的
	
	private String tid;
		
	public String getDim(){
		Map dim = (Map)daoHelper.getSqlMapClientTemplate().queryForObject("bi.olap.queryDims", this);
		//查询事实表
		Map dimCOl = (Map)this.daoHelper.getSqlMapClientTemplate().queryForObject("bi.olap.queryDimCol", this);
		String col = (String)dimCOl.get("col");
		String key = (String)dimCOl.get("colkey");
		String name = (String)dimCOl.get("colname");
		String sql =  "select distinct " +  (key==null||key.length() == 0 ? col : key) + " \"id\", " + (name==null||name.length() == 0 ?col:name) + " \"name\" from " + dimCOl.get("tname");
		datas = daoHelper.queryForList(sql);
		return "getDim";
	}
	
	/**
	 * 获取日历
	 * @return
	 */
	public String getDay(){
		return null;
	}
	
	public String getMonth(){
		return null;
	}

	public DaoHelper getDaoHelper() {
		return daoHelper;
	}



	public void setDaoHelper(DaoHelper daoHelper) {
		this.daoHelper = daoHelper;
	}



	public String getDimId() {
		return dimId;
	}

	public void setDimId(String dimId) {
		this.dimId = dimId;
	}


	public List getDatas() {
		return datas;
	}

	public void setDatas(List datas) {
		this.datas = datas;
	}

	public String getSelType() {
		return selType;
	}

	public void setSelType(String selType) {
		this.selType = selType;
	}

	public String getTalign() {
		return talign;
	}

	public void setTalign(String talign) {
		this.talign = talign;
	}

	public String getTid() {
		return tid;
	}

	public void setTid(String tid) {
		this.tid = tid;
	}
		
}
