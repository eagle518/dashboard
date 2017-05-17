package com.ruisi.vdop.ser.report;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCallback;

import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.ext.engine.util.PasswordEncrypt;
import com.ruisi.vdop.ser.webreport.DSColumn;
import com.ruisi.vdop.ser.webreport.DataService;
import com.ruisi.vdop.util.VDOPUtils;
import com.ruisi.vdop.util.VdopConstant;

public class DBUtils {
	

	private static void copyData(ResultSet rs, Map m) throws SQLException{
		String tname = rs.getString(1);
		m.put("id", tname);
		m.put("text", tname);
		m.put("iconCls", "icon-table");
	}
	
	/**
	 * type == 1, 表示 ds 为 ID, 通过ID获取JSON
	 * type == 2, 表示 ds 为JSON,直接使用
	 * @param ctx
	 * @param dsid
	 * @param type
	 * @return
	 * @throws Exception 
	 */
	public static List<Map> queryTables(ServletContext ctx, Map dsource) throws Exception{
		DaoHelper dao = VDOPUtils.getDaoHelper(ctx);
		final List ret = new ArrayList();
	
		DataService ser = new DataService();
		DataService.RSDataSource rsds = ser.json2datasource(dsource);
		Connection conn = null;
		try {
			if(rsds.getUsetype().equals("jndi")){
				conn = com.ruisi.vdop.ser.webreport.DBUtils.getConnection(rsds.getName());
			}else if(rsds.getUsetype().equals("jdbc")){
				conn = com.ruisi.vdop.ser.webreport.DBUtils.getConnection(rsds.getLinkurl(), rsds.getUname(), rsds.getPsd(), rsds.getClazz());
			}
			
			String qsql = null;
			if("mysql".equals(rsds.getLinktype())){
				qsql = DataService.showTables_mysql;
			}else if("oracle".equals(rsds.getLinktype())){
				qsql = DataService.showTables_oracle;
			}else if("sqlserver".equals(rsds.getLinktype())){
				qsql = DataService.showTables_sqlser;
			}
			PreparedStatement ps = conn.prepareStatement(qsql);
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				Map m = new HashMap();
				copyData(rs, m);
				ret.add(m);
			}
			rs.close();
			ps.close();
		}catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("sql 执行报错.");
		}finally{
			com.ruisi.vdop.ser.webreport.DBUtils.closeConnection(conn);
		}
		return ret;
		
	}
	
	public static List<DSColumn> copyValue(ResultSet rs) throws SQLException{
		ResultSetMetaData meta = rs.getMetaData();
		List<DSColumn> cols = new ArrayList<DSColumn>();
		for(int i=0; i<meta.getColumnCount(); i++){
			String name = meta.getColumnName(i+1);
			String tp = meta.getColumnTypeName(i+1);
			//meta.get
			//tp转换
			tp = columnType2java(tp);
			DSColumn col = new DSColumn();
			col.setName(name);
			col.setType(tp);
			col.setIsshow(true);
			col.setIdx(i+1);
			if("Date".equals(tp)){
				//日期不设置长度
			}else{
				col.setLength(meta.getColumnDisplaySize(i + 1));
			}
			cols.add(col);
		}
		return cols;
	}
	
	public static String columnType2java(String tp){
		tp = tp.replaceAll(" UNSIGNED", ""); //mysql 存在 UNSIGNED 类型, 比如： INT UNSIGNED
		String ret = null;
		if("varchar".equalsIgnoreCase(tp) || "varchar2".equalsIgnoreCase(tp) || "nvarchar".equalsIgnoreCase(tp) || "char".equalsIgnoreCase(tp)){
			ret = "String";
		}else if("int".equalsIgnoreCase(tp) || "MEDIUMINT".equalsIgnoreCase(tp) || "BIGINT".equalsIgnoreCase(tp) || "smallint".equalsIgnoreCase(tp) || "TINYINT".equalsIgnoreCase(tp)){
			ret = "Int";
		}else if("number".equalsIgnoreCase(tp) || "DECIMAL".equalsIgnoreCase(tp) || "Float".equalsIgnoreCase(tp) || "Double".equalsIgnoreCase(tp)){
			ret = "Double";
		}else if("DATETIME".equalsIgnoreCase(tp) || "DATE".equalsIgnoreCase(tp) || "Timestamp".equalsIgnoreCase(tp)){
			ret = "Date";
		}
		return ret;
	}

	
	/**
	 * 根据SQL获取 字段信息
	 * @param ctx
	 * @param sql
	 * @param ds
	 * @param type
	 * @return
	 * @throws Exception 
	 */
	public static List queryTableCols(ServletContext ctx, String sql, Map ds) throws Exception{

		//采用用户定义的数据源进行连接，而不是采用系统连接
		JSONObject json = null;
		json = JSONObject.fromObject(ds);
		DataService ser = new DataService();
		DataService.RSDataSource rsds = ser.json2datasource(json);
		Connection conn = null;
		try {
			if(rsds.getUsetype().equals("jndi")){
				conn = com.ruisi.vdop.ser.webreport.DBUtils.getConnection(rsds.getName());
			}else if(rsds.getUsetype().equals("jdbc")){
				conn = com.ruisi.vdop.ser.webreport.DBUtils.getConnection(rsds.getLinkurl(), rsds.getUname(), rsds.getPsd(), rsds.getClazz());
			}
			PreparedStatement ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			List<DSColumn> cols = copyValue(rs);
			rs.close();
			ps.close();
			return cols;
		}catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("sql 执行报错.");
		}finally{
			com.ruisi.vdop.ser.webreport.DBUtils.closeConnection(conn);
		}
		
	}
	
	public static List<DSColumn> queryMetaAndIncome(JSONObject dataset, Map ds) throws Exception{
		DataService ser = new DataService();
		DataService.RSDataSource rsds = ser.json2datasource(ds);
		
		List<String> tables = new ArrayList<String>();
		//需要进行关联的表
		JSONArray joinTabs = (JSONArray)dataset.get("joininfo");
		//生成sql
		StringBuffer sb = new StringBuffer("select a0.* ");
		//添加 列的分隔符，方便识别列是从哪个表来
		if(joinTabs!=null&&joinTabs.size() != 0){ //无关联表，不需要该字段
			sb.append(",'' a$idx ");
		}
		
		List<String> tabs = new ArrayList(); //需要进行关联的表，从joininfo中获取，剔除重复的表
		for(int i=0; joinTabs!=null&&i<joinTabs.size(); i++){
			JSONObject join = joinTabs.getJSONObject(i);
			String ref = join.getString("ref");
			if(!tabs.contains(ref)){
				tabs.add(ref);
			}
		}
		
		for(int i=0; i<tabs.size(); i++){
			sb.append(", a"+(i+1)+".* ");
			if(i != tabs.size() - 1){
				//添加 列的分隔符，方便识别列是从哪个表来
				sb.append(",'' a$idx");
			}
		}
		sb.append("from ");
		String master = dataset.getString("master");
		sb.append( master + " a0 ");
		tables.add(dataset.getString("master"));
		for(int i=0; i<tabs.size(); i++){
			String tab = tabs.get(i);
			sb.append(", " +tab);
			sb.append(" a"+(i+1)+" ");
			tables.add(tab);
		}
		sb.append("where 1=2 ");
		for(int i=0; i<tabs.size(); i++){
			String tab = tabs.get(i);
			List<JSONObject> refs = getJoinInfoByTname(tab, joinTabs);
			for(int k=0; k<refs.size(); k++){
				JSONObject t = refs.get(k);
				sb.append("and a0."+t.getString("col")+"=a"+(i+1)+"."+t.getString("refKey"));
				sb.append(" ");
			}
		}
		
		Connection conn  = null;
		try {
			if(rsds.getUsetype().equals("jndi")){
				conn = com.ruisi.vdop.ser.webreport.DBUtils.getConnection(rsds.getName());
			}else if(rsds.getUsetype().equals("jdbc")){
				conn = com.ruisi.vdop.ser.webreport.DBUtils.getConnection(rsds.getLinkurl(), rsds.getUname(), rsds.getPsd(), rsds.getClazz());
			}
			PreparedStatement ps = conn.prepareStatement(sb.toString());
			ResultSet rs = ps.executeQuery();
			
			ResultSetMetaData meta = rs.getMetaData();
			List<DSColumn> cols = new ArrayList<DSColumn>();
			String tname = tables.get(0);
			int idx = 1;
			for(int i=0; i<meta.getColumnCount(); i++){
				String name = meta.getColumnName(i+1);
				String tp = meta.getColumnTypeName(i+1);
				//遇到a$idx 表示字段做分割, 需要变换字段所属表信息
				if("a$idx".equalsIgnoreCase(name)){
					tname = tables.get(idx);
					idx++;
					continue;
				}
				tp = columnType2java(tp);
				DSColumn col = new DSColumn();
				col.setName(name);
				col.setType(tp);
				col.setTname(tname);
				col.setIsshow(true);
				cols.add(col);
			}
			rs.close();
			ps.close();
			return cols;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("sql 执行报错.");
		}finally{
			com.ruisi.vdop.ser.webreport.DBUtils.closeConnection(conn);
		}
	}
	
	private static List<JSONObject> getJoinInfoByTname(String tname, JSONArray joins){
		List<JSONObject> ret = new ArrayList();
		for(int i=0; joins!=null&&i<joins.size(); i++){
			JSONObject join = joins.getJSONObject(i);
			String ref = join.getString("ref");
			if(ref.equals(tname)){
				ret.add(join);
			}
		}
		return ret;
	}

}
