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
	
	/**
	 * 通过字段信息，生成创建表的SQL
	 * @param ls
	 * @return
	 */
	
	public static String createTableSql(List<DSColumn> ls, String tname, String dbName){
		StringBuffer sb = new StringBuffer("");
		sb.append("create table " + tname + " (\n");
		for(int i=0; i<ls.size(); i++){
			sb.append("\t");
			DSColumn col = ls.get(i);
			sb.append(col.getName());
			sb.append(" ");
			sb.append(javaType2db(col.getType(), dbName,col.getLength()));
			if(i != ls.size() - 1){
				sb.append(",");
			}
			sb.append("\n");
		}
		sb.append(")");//如果是mysql, 增加编码代码,默认编码是 utf8
		if("mysql".equals(dbName)){
			sb.append(" ENGINE=MyISAM CHARSET=utf8");
		}
		return sb.toString();
	}
	
	/**
	 * 通过json串生成sql
	 * @param ls
	 * @param tname
	 * @param dbName
	 * @param crttbb (是否创建填报表， 如果是创建填报表，需要自动创建id, userId 字段)
	 * @return
	 */
	public static String createTableSql2(JSONArray ls, String tname, String tnote, String dbName, boolean crttbb){
		StringBuffer sb = new StringBuffer("");
		sb.append("create table " + tname + " (\n");
		for(int i=0; i<ls.size(); i++){
			sb.append("\t");
			JSONObject col = ls.getJSONObject(i);
			sb.append(col.getString("name"));
			sb.append(" ");
			int length = 0;
			String l = col.get("length") == null ? null : col.get("length").toString();
			if(l != null && l.length() > 0){
				length = Integer.parseInt(l);
			}
			sb.append(javaType2db(col.getString("type"), dbName, length));
			/**
			String defvalue = (String)col.get("defvalue");
			if(defvalue == null || defvalue.length() == 0){
				sb.append("DEFAULT " + ("String".equals(defvalue)?"'":"") + defvalue + ("String".equals(defvalue)?"'":""));
			}
			**/
			if(i != ls.size() - 1){
				sb.append(",");
			}
			sb.append("\n");
		}
		
		sb.append(")");
		//如果是mysql, 增加编码代码,默认编码是 utf8
		if("mysql".equals(dbName)){
			sb.append(" ENGINE=MyISAM CHARSET=utf8 COMMENT='"+tnote+"'");
		}
		return sb.toString();
	}
	
	/**
	 * 转换JAVA类型到SQL类型
	 * @param type
	 * @param dbName
	 * @return
	 */
	private static String javaType2db(String type, String dbName, Integer length){
		if("mysql".equals(dbName)){
			if("String".equals(type)){
				if(length > 6000){
					return "text";
				}else{
					return "varchar("+length+")";
				}
			}else if("Int".equals(type)){
				return "int("+length+")";
			}else if("Double".equals(type)){
				return "DECIMAL("+length+",2)";  //对于 double 类型，默认保留2位小数
			}else if("Date".equals(type)){
				return "DATE";
			}else if("Datetime".equals(type)){
				return "DATETIME";
			}else{
				throw new RuntimeException("类型 " + type + " 未定义。");
			}
		}else if("sqlser".equals(dbName)){
			if("String".equals(type)){
				if(length > 6000){
					return "nvarchar(MAX)";
				}else{
					return "nvarchar("+length+")";
				}
			}else if("Int".equals(type)){
				return "int";
			}else if("Double".equals(type)){
				return "float";
			}else if("Datetime".equals(type)){
				return "datetime";
			}else if("Date".equals(type)){
				return "date";
			}else{
				throw new RuntimeException("类型 " + type + " 未定义。");
			}
		}else if("oracle".equals(dbName)){
			if("String".equals(type)){
				if(length > 6000){
					return "clob";
				}else{
					return "varchar2("+length+")";
				}
			}else if("Int".equals(type)){
				return "number";
			}else if("Double".equals(type)){
				return "number";
			}else if("Date".equals(type)){
				return "date";
			}else if("Datetime".equals(type)){
				return "date";
			}else{
				throw new RuntimeException("类型 " + type + " 未定义。");
			}
		}
		return null;
	}
	
	/**
	 * 从数据源返回连接，数据源配置来源于 olap_obj_share 表
	 * @param dsourceId
	 * @return
	 * @throws Exception 
	 */
	public static Connection getConnByDatasource(String dsourceId, DaoHelper daoHelper, Map<String, String> extInfo) throws Exception{
		Connection conn = null;
		if(dsourceId != null && dsourceId.length() > 0){
			//采用用户定义的数据源进行连接，而不是采用系统连接
			String sql = "select content from olap_obj_share where id='"+dsourceId+"' and tp='dsource'";
			String str = (String)daoHelper.queryForObject(sql, String.class);
			JSONObject json = JSONObject.fromObject(str);
			DataService ser = new DataService();
			DataService.RSDataSource rsds = ser.json2datasource(json);
			conn = com.ruisi.vdop.ser.webreport.DBUtils.getConnection(rsds.getLinkurl(), rsds.getUname(), PasswordEncrypt.decode(rsds.getPsd()), rsds.getClazz());
			if(extInfo != null){
				extInfo.put("dbName", rsds.getLinktype());
			}
		}else{
			conn = VDOPUtils.getConnection();
		}
		return conn;
	}
	
	public static Connection getConnByDatasource(String dsourceId, DaoHelper daoHelper) throws Exception{
		return getConnByDatasource(dsourceId, daoHelper, null);
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
