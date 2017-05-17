package com.ruisi.vdop.web.bireport;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;

import com.ruisi.ext.engine.ExtConstants;
import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.ext.engine.view.context.ExtContext;
import com.ruisi.ext.engine.view.context.MVContext;
import com.ruisi.vdop.ser.bireport.ReportService;
import com.ruisi.vdop.ser.olap.CompPreviewService;
import com.ruisi.vdop.util.VDOPUtils;

/**
 * 多维分析 设计页面
 * @author hq
 * @date 2013-11-19
 */
public class ReportDesignAction {
	
	private String pageInfo;
	
	private String userid;
	
	private String pageId; //页面ID
	
	private String pageName; //页面名称
	private String pageNote; //页面备注
	private String pushType; //推送类型
	private String purl;// 新生成的URL
	private String dayParam; //动态链接的日期参数
	private int rid; //唯一标识
	
	private String fileName; //发布时生成的文件名
	private String cataId; //发布时选择的目录
	private String rtype; //发布时选择的类型、olap（动态）报表还是固定报表
	
	private DaoHelper daoHelper;
	
	private String dbName = VDOPUtils.getConstant(ExtConstants.dbName);	
	private String selectDs; //默认选中的数据集
	
	private String menus; //控制菜单是否显示，默认显示，如果open=0,表示菜单不显示
	private String title; //页面标题，控制标题栏的显示内容
	
	public String save() throws IOException{
		HttpServletResponse resp = VDOPUtils.getResponse();
		resp.setContentType("text/html; charset=UTF-8");
		this.userid = VDOPUtils.getLoginedUser().getUserId();
		//判断名字是否重复
		int cnt = (Integer)this.daoHelper.getSqlMapClientTemplate().queryForObject("bi.report.olapExist", this);
		if(cnt > 0){
			resp.getWriter().print("no");
			return null;
		}
		
		if(pageId == null || pageId.length() == 0){
			this.pageId = daoHelper.getSqlMapClientTemplate().queryForObject("bi.report.querypid", this).toString();
			JSONObject page = JSONObject.fromObject(this.pageInfo);
			page.put("id", Integer.parseInt(this.pageId));
			this.pageInfo = page.toString();
			daoHelper.getSqlMapClientTemplate().insert("bi.report.insertReport", this);
		}else{
			daoHelper.getSqlMapClientTemplate().insert("bi.report.updateUserSave", this);
		}
		
		resp.getWriter().print(pageId);
		return null;
	}
		
	public String execute() throws SQLException, IOException{
		this.userid = VDOPUtils.getLoginedUser().getUserId();
		if(pageId != null && pageId.length() > 0){
			Map m = (Map)this.daoHelper.getSqlMapClientTemplate().queryForObject("bi.report.querypageinfo", this);
			if(m == null){
				return "success";
			}
			Object pctx = m.get("pageinfo");
			if(pctx instanceof String){
				this.pageInfo = (String)m.get("pageinfo");
			}else if(pctx instanceof oracle.sql.CLOB){
				oracle.sql.CLOB clob = (oracle.sql.CLOB)pctx;
				Reader is = clob.getCharacterStream();
				this.pageInfo = IOUtils.toString(is);
				is.close();
			}else if(pctx instanceof net.sourceforge.jtds.jdbc.ClobImpl){
				net.sourceforge.jtds.jdbc.ClobImpl clob = (net.sourceforge.jtds.jdbc.ClobImpl)pctx;
				Reader is = clob.getCharacterStream();
				this.pageInfo = IOUtils.toString(is);
				is.close();
			}
			this.pageName = (String)m.get("pagename");
		}else if(selectDs == null){
			String sql = "select min(cube_Id) cubeId from olap_cube_meta ";
			this.selectDs = (String)daoHelper.queryForObject(sql, String.class);
		}
		
		if(menus != null && menus.length() > 0){
			JSONObject obj = JSONObject.fromObject(menus);
			VDOPUtils.getRequest().setAttribute("menuDisp", obj);
		}
		
		return "success";
	}
	
	public static boolean isShowMenu(String name, HttpServletRequest req){
		JSONObject obj = (JSONObject)req.getAttribute("menuDisp");
		if(obj == null){
			return true;
		}
		Object r = obj.get(name);
		if(r == null){
			return true;
		}
		if(r instanceof Integer && (Integer)r == 0){
			return false;
		}else{
			return true;
		}
	}
	
	public String print() throws Exception{
		ExtContext.getInstance().removeMV(ReportService.deftMvId);
		JSONObject rjson = JSONObject.fromObject(this.pageInfo);
		ReportService tser = new ReportService();
		MVContext mv = tser.json2MV(rjson);
		
		CompPreviewService ser = new CompPreviewService();
		ser.setParams(null);
		ser.initPreview();
		
		String ret = ser.buildMV(mv);
		VDOPUtils.getRequest().setAttribute("data", ret);
		
		return "print";
	}

	public String getPageInfo() {
		return pageInfo;
	}

	public void setPageInfo(String pageInfo) {
		this.pageInfo = pageInfo;
	}

	public String getPageId() {
		return pageId;
	}


	public void setPageId(String pageId) {
		this.pageId = pageId;
	}

	public DaoHelper getDaoHelper() {
		return daoHelper;
	}

	public void setDaoHelper(DaoHelper daoHelper) {
		this.daoHelper = daoHelper;
	}

	public String getPageName() {
		return pageName;
	}

	public void setPageName(String pageName) {
		this.pageName = pageName;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public String getPageNote() {
		return pageNote;
	}

	public void setPageNote(String pageNote) {
		this.pageNote = pageNote;
	}

	public String getPushType() {
		return pushType;
	}

	public void setPushType(String pushType) {
		this.pushType = pushType;
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public int getRid() {
		return rid;
	}

	public void setRid(int rid) {
		this.rid = rid;
	}

	public String getRtype() {
		return rtype;
	}

	public void setRtype(String rtype) {
		this.rtype = rtype;
	}

	public String getPurl() {
		return purl;
	}

	public void setPurl(String purl) {
		this.purl = purl;
	}

	public String getDayParam() {
		return dayParam;
	}

	public void setDayParam(String dayParam) {
		this.dayParam = dayParam;
	}

	public String getFileName() {
		return fileName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getCataId() {
		return cataId;
	}

	public String getMenus() {
		return menus;
	}

	public void setMenus(String menus) {
		this.menus = menus;
	}

	public String getSelectDs() {
		return selectDs;
	}

	public void setSelectDs(String selectDs) {
		this.selectDs = selectDs;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public void setCataId(String cataId) {
		this.cataId = cataId;
	}
}
