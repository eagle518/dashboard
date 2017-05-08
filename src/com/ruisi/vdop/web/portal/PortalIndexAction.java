package com.ruisi.vdop.web.portal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.ruisi.ext.engine.ExtConstants;
import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.ext.engine.view.context.ExtContext;
import com.ruisi.ext.engine.view.context.MVContext;
import com.ruisi.vdop.ser.olap.CompPreviewService;
import com.ruisi.vdop.ser.portal.PortalPageService;
import com.ruisi.vdop.util.VDOPUtils;

/**
 * 企业门户
 * @author hq
 * @date 2013-11-13
 */
public class PortalIndexAction {
	
	private String pageInfo; //页面JSON
	private String pageId; //页面ID
	private String pageName;
	private String dbName = ExtContext.getInstance().getConstant(ExtConstants.dbName);
	private String cataId; //发布目录ID
	private String fileName; //发布时生成的文件名
	private String userId;
	private String is3g; //是否3G页面
	
	private String menus; //控制菜单是否显示，默认显示，如果open=0,表示菜单不显示
	
	private String selectDsIds; //已选择的数据源
	
	private Map curGroup; //当前分组对象.
	
	/**
	 * 门户定制
	 * @return
	 */
	public String customization(){
		if(menus != null && menus.length() > 0){
			JSONObject obj = JSONObject.fromObject(menus);
			VDOPUtils.getRequest().setAttribute("menuDisp", obj);
		}
		DaoHelper dao = VDOPUtils.getDaoHelper();
		pageInfo = (String)dao.getSqlMapClientTemplate().queryForObject("bi.portal.select", this);
		return "customiz";
	}
	
	public String execute(){
		DaoHelper dao = VDOPUtils.getDaoHelper();
		List ls = dao.getSqlMapClientTemplate().queryForList("bi.portal.listPortal", this);
		VDOPUtils.getRequest().setAttribute("ls", ls);
		return "success";
	}
	
	public String show(){
		//DaoHelper dao = VDOPUtils.getDaoHelper();
		//this.siteId = VDOPUtils.getLoginedUser().getSiteId();
		//Map m = (Map)dao.getSqlMapClientTemplate().queryForObject("bi.portal.getOne", this);
		//VDOPUtils.getRequest().setAttribute("info", m);
		return "show";
	}
	
	public String rename(){
		DaoHelper dao = VDOPUtils.getDaoHelper();
		dao.getSqlMapClientTemplate().update("bi.portal.rename", this);
		return null;
	}
	
	public String view() throws Exception{
		DaoHelper dao = VDOPUtils.getDaoHelper();
		pageInfo = (String)dao.getSqlMapClientTemplate().queryForObject("bi.portal.select", this);
		JSONObject pageJson = JSONObject.fromObject(pageInfo);
		PortalPageService pser = new PortalPageService(pageJson, VDOPUtils.getServletContext());
		ExtContext.getInstance().removeMV(PortalPageService.deftMvId);
		MVContext mv = pser.json2MV(false, false);
		CompPreviewService ser = new CompPreviewService();
		ser.setParams(pser.getMvParams());
		ser.initPreview();
		String ret = ser.buildMV(mv);
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		VDOPUtils.getResponse().getWriter().print(ret);
		return null;
	}
	
	public String save() throws IOException{
		HttpServletResponse resp = VDOPUtils.getResponse();
		resp.setContentType("text/html; charset=UTF-8");
		this.userId = VDOPUtils.getLoginedUser().getUserId();
		DaoHelper dao = VDOPUtils.getDaoHelper();
		
		if(pageId == null || pageId.length() == 0){
			JSONObject obj = JSONObject.fromObject(this.pageInfo);
			this.pageId = VDOPUtils.getUUIDStr();
			obj.put("id", this.pageId);
			this.pageInfo = obj.toString();
			dao.getSqlMapClientTemplate().insert("bi.portal.save", this);
			VDOPUtils.getResponse().getWriter().print(this.pageId);
		}else{
			dao.getSqlMapClientTemplate().update("bi.portal.update", this);
		}
		return null;
	}
	
	public String delete() throws IOException{
		DaoHelper dao = VDOPUtils.getDaoHelper();
		dao.getSqlMapClientTemplate().delete("bi.portal.delete", this);
		VDOPUtils.getResponse().sendRedirect("PortalIndex.action");
		return null;
	}
	
	public String print() throws Exception{
		if(pageInfo == null){
			DaoHelper dao = VDOPUtils.getDaoHelper();
			pageInfo = (String)dao.getSqlMapClientTemplate().queryForObject("bi.portal.select", this);
		}
		JSONObject pageJson = JSONObject.fromObject(pageInfo);
		PortalPageService pser = new PortalPageService(pageJson, VDOPUtils.getServletContext());
		ExtContext.getInstance().removeMV(PortalPageService.deftMvId);
		MVContext mv = pser.json2MV(false, true);
		CompPreviewService ser = new CompPreviewService();
		ser.setParams(pser.getMvParams());
		ser.initPreview();
		String ret = ser.buildMV(mv);
		VDOPUtils.getRequest().setAttribute("str", ret);
		return "print";
	}
	
	public String del() throws IOException{
		DaoHelper dao = VDOPUtils.getDaoHelper();
		dao.getSqlMapClientTemplate().delete("bi.portal.delete", this);
		return null;
	}
	
	public String cubeTree() throws IOException{
		DaoHelper daoHelper = VDOPUtils.getDaoHelper();
		HttpServletResponse resp = VDOPUtils.getResponse();
		resp.setContentType("text/xml; charset=UTF-8");
		String ctx = null;
		if(selectDsIds == null || selectDsIds.length() == 0){
			Map root = new HashMap();
		
			root.put("id", "root");
			root.put("text", "您还未选择数据模型！");
			root.put("state", "open");
			root.put("iconCls", "icon-no");
			root.put("children", new ArrayList());
			ctx = JSONArray.fromObject(root).toString();
		
		}else{
			List ls = daoHelper.getSqlMapClientTemplate().queryForList("bi.portal.listDs", this);
			for(int i=0; i<ls.size(); i++){
				Map m = (Map)ls.get(i);
				m.put("iconCls", "icon-cube");
				String cubeId = m.get("cubeId").toString();
				//给数据集节点添加维度、指标节点
				m.put("children", new ArrayList());
				Map wdnode = new HashMap();
				wdnode.put("id", "wd");
				wdnode.put("text", "维度");
				wdnode.put("state", "open");
				wdnode.put("iconCls", "icon-dim2");
				wdnode.put("children", new ArrayList());
				((List)m.get("children")).add(wdnode);
				Map zbnode = new HashMap();
				zbnode.put("id", "zb");
				zbnode.put("text", "度量");
				zbnode.put("state", "open");
				zbnode.put("iconCls", "icon-kpigroup");
				zbnode.put("children", new ArrayList());
				((List)m.get("children")).add(zbnode);
				
				
				Map p = new HashMap();
				p.put("cubeId", cubeId);
				List children = daoHelper.getSqlMapClientTemplate().queryForList("bi.portal.listDsMeta", p);
				
				//设置attributes;
				for(int j=0; j<children.size(); j++){
					Map child = (Map)children.get(j);
					int col_type = new Integer(child.get("col_type").toString());
					String grouptype = (String)child.get("grouptype");
					if(grouptype == null || grouptype.length() == 0){
						grouptype = null;
					}
					String groupname = (String)child.get("groupname");
					if(grouptype != null && grouptype.length() > 0){
						if(this.curGroup == null || !this.curGroup.get("id").equals(grouptype)){
							//添加分组节点
							Map<String, Object> fz = new HashMap<String, Object>();
							fz.put("id", grouptype);
							fz.put("text", groupname);
							fz.put("state", "open");
							fz.put("iconCls", "icon-dim");
							fz.put("children", new ArrayList());
							//给分组添加attributes (把分组的第一个节点信息传递给他,拖拽分组时就当拖拽第一个节点)
							Map<String, Object> attr = new HashMap<String, Object>();
							fz.put("attributes", attr);
							attr.put("col_type", col_type);
							attr.put("col_id", child.get("col_id"));
							attr.put("col_name", child.get("col_name"));
							attr.put("cubeId", child.get("cubeId"));
							attr.put("dsetId", child.get("dsetId"));
							attr.put("dsid", child.get("dsid"));
							attr.put("alias", child.get("alias"));
							attr.put("dim_type", child.get("dim_type"));
							attr.put("tableName", child.get("tableName") == null ? "" : child.get("tableName"));
							attr.put("tableColKey", child.get("tableColKey") == null ? "" : child.get("tableColKey"));
							attr.put("tableColName", child.get("tableColName") == null ? "" : child.get("tableColName"));
							attr.put("ordcol", child.get("ordcol") == null ? "" : child.get("ordcol"));
							attr.put("dateformat", child.get("dateformat") == null ? "" : child.get("dateformat"));
							attr.put("tname", child.get("tname"));
							attr.put("calc", child.get("calc"));
							if(this.curGroup == null){
								attr.put("iscas", "");
							}else{
								attr.put("iscas", "y");
							}
							attr.put("dimord", child.get("dimord") == null ? "" : child.get("dimord"));
							attr.put("grouptype", grouptype);
							attr.put("valType", child.get("valType"));
							((List)wdnode.get("children")).add(fz);
							this.curGroup = fz;
						}
					}else{
						this.curGroup = null;
					}
					Map<String, Object> attr = new HashMap<String, Object>();
					child.put("attributes", attr);
					//添加立方体所使用的数据源到Tree
					attr.put("col_type", col_type);
					attr.put("col_id", child.get("col_id"));
					attr.put("col_name", child.get("col_name"));
					attr.put("cubeId", child.get("cubeId"));
					attr.put("dsetId", child.get("dsetId"));
					attr.put("dsid", child.get("dsid"));
					attr.put("alias", child.get("alias"));
					attr.put("fmt", child.get("fmt") == null ? "" : child.get("fmt"));
					attr.put("aggre", child.get("aggre"));
					attr.put("dim_type", child.get("dim_type"));
					attr.put("tableName", child.get("tableName") == null ? "" : child.get("tableName"));
					attr.put("tableColKey", child.get("tableColKey") == null ? "" : child.get("tableColKey"));
					attr.put("tableColName", child.get("tableColName") == null ? "" : child.get("tableColName"));
					attr.put("dateformat", child.get("dateformat") == null ? "" : child.get("dateformat"));
					attr.put("tname", child.get("tname"));
					attr.put("calc", child.get("calc"));
					if(this.curGroup == null){
						attr.put("iscas", "");
					}else{
						attr.put("iscas", "y");
					}
					attr.put("dimord", child.get("dimord") == null ? "" : child.get("dimord"));
					attr.put("rate", child.get("rate"));
					attr.put("unit", child.get("unit") == null ? "" : child.get("unit"));
					attr.put("grouptype", grouptype);
					attr.put("calc_kpi", child.get("calc_kpi"));
					attr.put("valType", child.get("valType"));
					attr.put("ordcol", child.get("ordcol") == null ? "" : child.get("ordcol"));
					//设置节点图标
					if(col_type == 1){
						if(grouptype == null || grouptype.length() == 0){
							child.put("iconCls", "icon-dim");
						}else{
							child.put("iconCls", "icon-dimlevel");
						}
					}else{
						child.put("iconCls", "icon-kpi");
					}
					if(col_type == 1){
						if(this.curGroup == null){
							((List)wdnode.get("children")).add(child);
						}else{
							((List)this.curGroup.get("children")).add(child);
						}
					}else{
						((List)zbnode.get("children")).add(child);
					}
				}
			}
			ctx = JSONArray.fromObject(ls).toString();
		}
		resp.getWriter().println(ctx);
		return null;
	}

	public String getPageInfo() {
		return pageInfo;
	}

	public void setPageInfo(String pageInfo) {
		this.pageInfo = pageInfo;
	}

	public String getUserId() {
		return userId;
	}

	public String getDbName() {
		return dbName;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public String getPageId() {
		return pageId;
	}

	public void setPageId(String pageId) {
		this.pageId = pageId;
	}

	public String getPageName() {
		return pageName;
	}

	public void setPageName(String pageName) {
		this.pageName = pageName;
	}

	public String getCataId() {
		return cataId;
	}

	public void setCataId(String cataId) {
		this.cataId = cataId;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getIs3g() {
		return is3g;
	}

	public void setIs3g(String is3g) {
		this.is3g = is3g;
	}

	public String getMenus() {
		return menus;
	}

	public void setMenus(String menus) {
		this.menus = menus;
	}

	public String getSelectDsIds() {
		return selectDsIds;
	}

	public void setSelectDsIds(String selectDsIds) {
		this.selectDsIds = selectDsIds;
	}
	
}
