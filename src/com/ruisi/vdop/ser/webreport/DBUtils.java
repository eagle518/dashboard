package com.ruisi.vdop.ser.webreport;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.ruisi.vdop.util.VDOPUtils;


public class DBUtils {
	
	private static Logger log = Logger.getLogger(DBUtils.class);
	
	public static boolean testConnection(String url, String name, String psd, String clazz, StringBuffer msg) {
		boolean ret = false;
		Connection conn = null;
		try {
			Class.forName(clazz).newInstance();
			conn= DriverManager.getConnection(url,name, psd);
			if(conn != null){
				ret = true;
			}else{
				ret = false;
			}
		} catch (Exception e) {
			ret = false;
			if(msg != null){
				msg.append(e.getMessage());
			}
			log.error("JDBC测试出错。", e);
			msg.append(e.getMessage());
		}finally{
			closeConnection(conn);
		}
		return ret;
	}
	/**
	public static String createUrl(String linktype, String ip, String port, String dbname){
		String url = "";
		if(linktype.equals("mysql")){
			url = "jdbc:mysql://"+ip+":"+port+"/"+dbname+"?useUnicode=true&characterEncoding=UTF8";
		}else if(linktype.equals("oracle")){
			url = "jdbc:oracle:thin:@"+ip+":"+port+":" + dbname;
		}else if(linktype.equals("sqlserver")){
			url = "jdbc:jtds:sqlserver://"+ip+":"+port+"/" + dbname;
		}
		return url;
	}
	**/
	public static boolean testJndi(String jndiname, StringBuffer msg){
		boolean ret = false;
		Connection con = null;
		try{
		  	Context ctx = new InitialContext();      
		    String strLookup = "java:comp/env/"+jndiname; 
		    DataSource ds =(DataSource) ctx.lookup(strLookup);
		    con = ds.getConnection();
		    if (con != null){
		       ret = true;
		    }else{
		    	ret = false;
		    }
		}catch (Exception e) {
			log.error("JNDI测试出错", e);
			msg.append(e.getMessage());
			ret = false;
		}finally{
			closeConnection(con);
		}
		return ret;
	}
	
	public static void closeConnection(Connection conn){
		if(conn != null){
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static Connection getConnection(JSONObject ds) throws Exception{
		Connection conn  = null;
		DataService ser = new DataService();
		DataService.RSDataSource rsds = ser.json2datasource(ds);
		
		//使用自定义数据源
		conn = getConnection(rsds.getLinkurl(), rsds.getUname(), rsds.getPsd(), rsds.getClazz());
		
		return conn;
	}
	
	public static Connection getConnection(String url, String name, String psd, String clazz) throws Exception{
		try {
			Connection conn = null;
			Class.forName(clazz).newInstance();
			conn= DriverManager.getConnection(url,name, psd);
			return conn;
		} catch (Exception e) {
			throw e;
		}
	}
	
	public static Connection getConnection(String jndiname) throws Exception{
		Connection con = null;
		try {
			Context ctx = new InitialContext();      
		    String strLookup = "java:comp/env/"+jndiname; 
		    DataSource ds =(DataSource) ctx.lookup(strLookup);
		    con = ds.getConnection();
		}catch(Exception ex){
			ex.printStackTrace();
			throw ex;
		}
	    return con;
	}
	
	public static List<DSColumn> queryMeta(String sql, DataService.RSDataSource rsds) throws Exception{
		Connection conn  = null;
		try {
			
			conn = getConnection(rsds.getLinkurl(), rsds.getUname(), rsds.getPsd(), rsds.getClazz());
				
			
			PreparedStatement ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			
			ResultSetMetaData meta = rs.getMetaData();
			List<DSColumn> cols = new ArrayList<DSColumn>();
			for(int i=0; i<meta.getColumnCount(); i++){
				String name = meta.getColumnName(i+1);
				String tp = meta.getColumnTypeName(i+1);
				//tp转换
				tp = com.ruisi.vdop.ser.report.DBUtils.columnType2java(tp);
				DSColumn col = new DSColumn();
				col.setName(name);
				col.setType(tp);
				cols.add(col);
			}
			rs.close();
			ps.close();
			return cols;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("sql 执行报错.");
		}finally{
			closeConnection(conn);
		}
	}
	
	
	public static List queryTopN(String sql, DataService.RSDataSource rsds, int n) throws Exception{
		Connection conn  = null;
		try {
			List ret = new ArrayList();
			if(rsds == null){
				conn = VDOPUtils.getConnection();
			}else{
				conn = getConnection(rsds.getLinkurl(), rsds.getUname(), rsds.getPsd(), rsds.getClazz());
			}
			PreparedStatement ps = conn.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();
			
			ResultSetMetaData meta = rs.getMetaData();
			List<String> cols = new ArrayList<String>();
			for(int i=0; i<meta.getColumnCount(); i++){
				String name = meta.getColumnName(i+1);
				cols.add(name);
			}
			ret.add(cols);
			int idx = 0;
			while(rs.next() && idx <= n){
				Map<String, Object> m = new HashMap<String, Object>();
				for(String s : cols){
					m.put(s, rs.getString(s));
				}
				ret.add(m);
				idx++;
			}
			rs.close();
			ps.close();
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}finally{
			closeConnection(conn);
		}
	}
}
