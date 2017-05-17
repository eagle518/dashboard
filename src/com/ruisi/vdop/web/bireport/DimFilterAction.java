package com.ruisi.vdop.web.bireport;

import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.ext.runtime.tag.CalendarTag;
import com.ruisi.vdop.cache.ModelCacheManager;
import com.ruisi.vdop.ser.webreport.DataService;

/**
 * 维度筛选
 * @author hq
 * @date 2013-10-9
 */
public class DimFilterAction {
	
	private String dimId;
	private String dimType; //维度类型
	private String vals; //已选维度值
	private DaoHelper daoHelper;
	
	private String dft1 = ""; //默认日期
	private String dft2 = "";
	private String dfm1 = ""; //默认月份
	private String dfm2 = "";
	private String max; //最大日期  查询数据中获取
	private String min; //最小日期  查询数据中获取
	
	private String filtertype; //用来筛选的方式，1为区间，2为值筛选
	private String cubeId; //立方体ID
	
	private String keyword; //搜索关键字
	
	private Map dim;
	private List datas;
	
	private String dateformat;
	private String dsid; //数据源
		
	public static String date2Quarter(String dt){
		String year = dt.substring(0, 4);
		String month = dt.substring(4, 6);
		String q = "";
		int m = Integer.parseInt(month);
		if(m <= 3){
			q = "01";
		}else if(m > 3 && m <= 6){
			q = "02";
		}else if(m > 6 && m <= 9){
			q = "03";
		}else{
			q = "04";
		}
		return year + q;
	}
	
	public static boolean exist(String id, String[] ids){
		boolean exist = false;
		for(String tid : ids){
			if(tid.equals(id)){
				exist = true;
				break;
			}
		}
		return exist;
	}

	public String paramFilter() throws Exception {
		//查询事实表
		Map dimCOl = (Map)this.daoHelper.getSqlMapClientTemplate().queryForObject("bi.olap.queryDimCol", this);
		String col = (String)dimCOl.get("col");
		String key = (String)dimCOl.get("colkey");
		String name = (String)dimCOl.get("colname");
		String dimord = (String)dimCOl.get("dimord");
		String ordcol = (String)dimCOl.get("ordcol");
		String tname = (String)dimCOl.get("tname");
		String coltable = (String)dimCOl.get("coltable");
		dimType = (String)dimCOl.get("dimType");
		String sql = "select distinct " +  (key==null||key.length() == 0 ? col : key) + " \"id\", " + (name==null||name.length() == 0 ?col:name) + " \"name\" from ";
		sql += (coltable == null || coltable.length() == 0 ? tname : coltable);
		if(ordcol != null && ordcol.length() > 0){
			sql += " order by " + ordcol;
		}
		if(ordcol != null && ordcol.length() > 0 && dimord != null && dimord.length() > 0){
			sql += " " + dimord;
		}
		sql = sql.replaceAll("@", "'");
		
		JSONObject ds = ModelCacheManager.getDsource(dsid, daoHelper);
		DataService ser = new DataService();
		DataService.RSDataSource rsds = ser.json2datasource(ds);
		datas = com.ruisi.vdop.ser.webreport.DBUtils.queryTopN(sql, rsds, 100);
		
		//默认日期
		if("day".equals(this.dimType)){
			//格式化日期
			Map first = (Map)datas.get(0);
			Map end = (Map)datas.get(datas.size() - 1);
			this.min = (String)first.get("id");
			this.max = (String)end.get("id");
			if(dft1 == null || dft1.length() == 0 ||dft2 == null || dft2.length() == 0){
				this.dft1 = this.max;
				this.dft2 = this.min;
			}
		}
		
		return "pfilter";
	}
	
	public String execute() throws Exception{
		//查询事实表
		Map dimCOl = (Map)this.daoHelper.getSqlMapClientTemplate().queryForObject("bi.olap.queryDimCol", this);
		String col = (String)dimCOl.get("col");
		String key = (String)dimCOl.get("colkey");
		String name = (String)dimCOl.get("colname");
		String dimord = (String)dimCOl.get("dimord");
		String ordcol = (String)dimCOl.get("ordcol");
		String tname = (String)dimCOl.get("tname");
		String coltable = (String)dimCOl.get("coltable");
		dimType = (String)dimCOl.get("dimType");
		String sql =  "select distinct " +  (key==null||key.length() == 0 ? col : key) + " \"id\", " + (name==null||name.length() == 0 ?col:name) + " \"name\"" +
				(ordcol != null && ordcol.length() > 0 ? ", " + (key==null||key.length() == 0 ? col : key) + " as " + ordcol:"") +   //追加排序字段
				" from " ;
		
		sql += (coltable == null || coltable.length() == 0 ? tname : coltable);
		
		if(ordcol != null && ordcol.length() > 0){
			sql += " order by " + ordcol;
		}
		if(ordcol != null && ordcol.length() > 0 && dimord != null && dimord.length() > 0){
			sql += " " + dimord;
		}
		sql = sql.replaceAll("@", "'");
		
		JSONObject ds = ModelCacheManager.getDsource(dsid, daoHelper);
		DataService ser = new DataService();
		DataService.RSDataSource rsds = ser.json2datasource(ds);
		datas = com.ruisi.vdop.ser.webreport.DBUtils.queryTopN(sql, rsds, 100);
		
		//默认日期
		if("day".equals(this.dimType)){
			//格式化日期
			Map first = (Map)datas.get(0);
			Map end = (Map)datas.get(datas.size() - 1);
			this.min = (String)first.get("id");
			this.max = (String)end.get("id");
			if(dft1 == null || dft1.length() == 0 ||dft2 == null || dft2.length() == 0){
				this.dft1 = this.max;
				this.dft2 = this.min;
			}
		}
		
		return "success";
	}
	
	public String search() throws Exception{
		//查询事实表
		Map dimCOl = (Map)this.daoHelper.getSqlMapClientTemplate().queryForObject("bi.olap.queryDimCol", this);
		String col = (String)dimCOl.get("col");
		String key = (String)dimCOl.get("colkey");
		String name = (String)dimCOl.get("colname");
		String dimord = (String)dimCOl.get("dimord");
		String ordcol = (String)dimCOl.get("ordcol");
		String tname = (String)dimCOl.get("tname");
		String coltable = (String)dimCOl.get("coltable");
		String sql =  "select distinct " +  (key==null||key.length() == 0 ? col : key) + " \"id\", " + (name==null||name.length() == 0 ?col:name) + " \"name\" from ";
		sql += (coltable == null || coltable.length() == 0 ? tname : coltable);
		sql += " where "+(name==null||name.length() == 0 ?col:name)+" like '%"+keyword+"%'";
		if(ordcol != null && ordcol.length() > 0){
			sql += " order by " + ordcol;
		}
		if(ordcol != null && ordcol.length() > 0 && dimord != null && dimord.length() > 0){
			sql += " " + dimord;
		}
		sql = sql.replaceAll("@", "'");
		
		JSONObject ds = ModelCacheManager.getDsource(dsid, daoHelper);
		DataService ser = new DataService();
		DataService.RSDataSource rsds = ser.json2datasource(ds);
		datas = com.ruisi.vdop.ser.webreport.DBUtils.queryTopN(sql, rsds, 100);
		
		return "search";
	}
	
	public static String getFestival(Object key, String dateformat){
		String ret = CalendarTag.getFestival((String)key, dateformat);
		return ret;
	}
	
	public String getDimId() {
		return dimId;
	}

	public void setDimId(String dimId) {
		this.dimId = dimId;
	}

	public DaoHelper getDaoHelper() {
		return daoHelper;
	}

	public void setDaoHelper(DaoHelper daoHelper) {
		this.daoHelper = daoHelper;
	}

	public Map getDim() {
		return dim;
	}

	public void setDim(Map dim) {
		this.dim = dim;
	}

	public List getDatas() {
		return datas;
	}

	public void setDatas(List datas) {
		this.datas = datas;
	}

	public String getVals() {
		return vals;
	}

	public void setVals(String vals) {
		this.vals = vals;
	}

	public String getDft1() {
		return dft1;
	}

	public String getDft2() {
		return dft2;
	}

	public String getDfm1() {
		return dfm1;
	}

	public String getDfm2() {
		return dfm2;
	}

	public void setDft1(String dft1) {
		this.dft1 = dft1;
	}

	public void setDft2(String dft2) {
		this.dft2 = dft2;
	}

	public void setDfm1(String dfm1) {
		this.dfm1 = dfm1;
	}

	public String getDimType() {
		return dimType;
	}

	public void setDimType(String dimType) {
		this.dimType = dimType;
	}

	public void setDfm2(String dfm2) {
		this.dfm2 = dfm2;
	}

	public String getMax() {
		return max;
	}

	public String getMin() {
		return min;
	}

	public void setMax(String max) {
		this.max = max;
	}

	public String getFiltertype() {
		return filtertype;
	}

	public void setFiltertype(String filtertype) {
		this.filtertype = filtertype;
	}

	public void setMin(String min) {
		this.min = min;
	}


	public String getCubeId() {
		return cubeId;
	}

	public void setCubeId(String cubeId) {
		this.cubeId = cubeId;
	}

	public String getDateformat() {
		if(dateformat != null){
			dateformat = dateformat.replaceAll("mm", "MM");
		}
		return dateformat;
	}

	public void setDateformat(String dateformat) {
		this.dateformat = dateformat;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public String getDsid() {
		return dsid;
	}

	public void setDsid(String dsid) {
		this.dsid = dsid;
	}
	
}
