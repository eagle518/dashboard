package com.ruisi.vdop.ser.webreport;

import java.util.List;
import java.util.Map;

import com.ruisi.vdop.util.VDOPUtils;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class DataService {
	public static final String mysql = "com.mysql.jdbc.Driver";
	public static final String oracle = "oracle.jdbc.driver.OracleDriver";
	public static final String sqlserver = "net.sourceforge.jtds.jdbc.Driver";
	
	public static final String showTables_mysql = "show tables";
	public static final String showTables_oracle = "select table_name from tabs";
	public static final String showTables_sqlser = "select name from sysobjects where xtype='U' order by name";
	
	public static final String[] dataTypes = new String[]{"String", "Int", "Double"}; 
	
	/**
	 * 在处理 $xx 这种特殊字符的时候，先用 [x] 代替，再替换回来 
	 * @param obj
	 * @return
	 */
	public String createDatasetSql(JSONObject obj){
		String sql = obj.getString("querysql");
		Object objs = obj.get("param");
		if(objs != null){
			JSONArray params = (JSONArray)objs;
			for(int i=0; i<params.size(); i++){
				StringBuffer sb = new StringBuffer("");
				JSONObject param = params.getJSONObject(i);
				String filterid = (String)param.get("filterid");
				if(filterid == null || filterid.length() == 0){
					continue;
				}
				String col = param.getString("col");
				String type = param.getString("type");
				String val = (String)param.get("val");
				String val2 = (String)param.get("val2");
				String valuetype = param.getString("valuetype");
				String usetype = param.getString("usetype");
				String linkparam = (String)param.get("linkparam");
				String linkparam2 = (String)param.get("linkparam2");
				String tablealias = (String)param.get("tablealias");
				
				if(type.equals("like")){
					if(val != null){
						val = "%"+val+"%";
					}
					if(val2 != null){
						val2 = "%"+val2+"%";
					}
				}
				if("string".equals(valuetype)){
					if(val != null){
						val = "'" + val + "'";
					}
					if(val2 != null){
						val2 = "'" + val2 + "'";
					}
				}
				if(type.equals("between")){
					if(usetype.equals("gdz")){
						sb.append(" and " + (tablealias != null && tablealias.length() > 0 ? tablealias+".":"") + col + " " + type + " " + val + " and " + val2);
					}else{
						sb.append("#if([x]"+linkparam+" != '' && [x]"+linkparam2+" != '') ");
						sb.append(" and " + (tablealias != null && tablealias.length() > 0 ? tablealias+".":"")  + col + " " + type + " " + ("string".equals(valuetype)?"'":"") + "[x]"+linkparam +("string".equals(valuetype)?"'":"") + " and " + ("string".equals(valuetype)?"'":"")+ "[x]"+linkparam2 + ("string".equals(valuetype)?"'":"") + " #end");
					}
				}else if(type.equals("in")){
					if(usetype.equals("gdz")){
						sb.append(" and " + (tablealias != null && tablealias.length() > 0 ? tablealias+".":"") + col + " in (" + val + ")");
					}else{
						sb.append("#if([x]"+linkparam+" != '') ");
						sb.append(" and " + (tablealias != null && tablealias.length() > 0 ? tablealias+".":"") + col + " in (" + "[x]"+linkparam + ")");
						sb.append("  #end");
					}
				}else{
					if(usetype.equals("gdz")){
						sb.append(" and " + (tablealias != null && tablealias.length() > 0 ? tablealias+".":"") + col + " " + type + " " + val);
					}else{
						sb.append("#if([x]"+linkparam+" != '') ");
						sb.append(" and " + (tablealias != null && tablealias.length() > 0 ? tablealias+".":"") + col + " "+type+" " + ("string".equals(valuetype) ? "'" + ("like".equals(type)?"%":"") +"[x]"+linkparam+""+("like".equals(type)?"%":"")+"'":"[x]"+linkparam) + "");
						sb.append("  #end");
					}
				}
				sql = sql.replaceAll("\\["+filterid+"\\]", sb.toString()).replaceAll("\\[x\\]", "\\$");
			}
			return sql;
		}
		return sql;
	}
	
	public RSDataSource json2datasource(Map obj){
		RSDataSource ds = new RSDataSource();
		String linktype = (String)obj.get("linktype");
		String dsname = (String)obj.get("dsname");
		String usetype = (String)obj.get("usetype");
		if("jdbc".equals(usetype)){
			String linkurl = (String)obj.get("linkurl");
			String uname = (String)obj.get("uname");
			String psd = (String)obj.get("psd");
			ds.setLinkurl(linkurl);
			ds.setUname(uname);
			ds.setPsd(psd);
		}
		String clazz = null;
		ds.setLinktype(linktype);
		ds.setName(dsname);
		ds.setUsetype(usetype);
	
		if("jdbc".equals(usetype)){
			if(linktype.equals("mysql")){
				clazz = DataService.mysql;
			}else if(linktype.equals("oracle")){
				clazz = DataService.oracle;
			}else if(linktype.equals("sqlserver")){
				clazz = DataService.sqlserver;
			}
			ds.setClazz(clazz);
		}
		return ds;
	}
	
	public List queryDimValues(String text, String val, String tname){
		String sql = "select "+text+" \"text\", "+val+" \"val\" from "+tname + " order by " + val;
		return VDOPUtils.getDaoHelper().queryForList(sql);
	}
	
	public static class RSDataSource {
		private String linktype; //连接方式,mysql/sqlser/oracle
		private String linkurl;
		private String uname; 
		private String psd;
		private String clazz;		
		private String name;
		private String usetype;  //使用jdbc 还是 jndi
		public String getLinktype() {
			return linktype;
		}
		public String getLinkurl() {
			return linkurl;
		}
		public String getUname() {
			return uname;
		}
		public String getPsd() {
			return psd;
		}
		public String getClazz() {
			return clazz;
		}
		public String getName() {
			return name;
		}
		public String getUsetype() {
			return usetype;
		}
		public void setLinktype(String linktype) {
			this.linktype = linktype;
		}
		public void setLinkurl(String linkurl) {
			this.linkurl = linkurl;
		}
		public void setUname(String uname) {
			this.uname = uname;
		}
		public void setPsd(String psd) {
			this.psd = psd;
		}
		public void setClazz(String clazz) {
			this.clazz = clazz;
		}
		public void setName(String name) {
			this.name = name;
		}
		public void setUsetype(String usetype) {
			this.usetype = usetype;
		}

	}
}
