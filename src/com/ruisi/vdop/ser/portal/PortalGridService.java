package com.ruisi.vdop.ser.portal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.ruisi.ext.engine.ExtConstants;
import com.ruisi.ext.engine.init.TemplateManager;
import com.ruisi.ext.engine.util.IdCreater;
import com.ruisi.ext.engine.view.context.ExtContext;
import com.ruisi.ext.engine.view.context.MVContext;
import com.ruisi.ext.engine.view.context.MVContextImpl;
import com.ruisi.ext.engine.view.context.form.InputField;
import com.ruisi.ext.engine.view.context.grid.PageInfo;
import com.ruisi.ext.engine.view.context.gridreport.GridCell;
import com.ruisi.ext.engine.view.context.gridreport.GridReportContext;
import com.ruisi.ext.engine.view.context.gridreport.GridReportContextImpl;
import com.ruisi.vdop.service.frame.DataControlInterface;
import com.ruisi.vdop.util.VDOPUtils;

/**
 * 明细表 service
 * @author hq
 * @date 2016-11-15
 */
public class PortalGridService {
	
	public final static String deftMvId = "mv.portal.gridReport";
		
	private Map<String, InputField> mvParams = new HashMap(); //mv的参数
	
	private DataControlInterface dataControl; //数据权限控制
	
	private JSONObject gridJson;
	private JSONObject dsource;
	private JSONArray pageParams;
	private JSONObject dset;
	
	public PortalGridService(){
		String clazz = ExtContext.getInstance().getConstant("dataControl");
		if(clazz != null && clazz.length() != 0){
			try {
				dataControl = (DataControlInterface)Class.forName(clazz).newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
	}

	public MVContext json2MV() throws Exception{
		
		//创建MV
		MVContext mv = new MVContextImpl();
		mv.setChildren(new ArrayList());
		String formId = ExtConstants.formIdPrefix + IdCreater.create();
		mv.setFormId(formId);
		mv.setMvid(deftMvId);
		
		//处理参数,把参数设为hidden
		PortalTableService.parserHiddenParam(pageParams, mv, this.mvParams);
		
		//创建corssReport
		GridReportContext cr = json2Grid();
		//设置ID
		String id = ExtConstants.reportIdPrefix + IdCreater.create();
		cr.setId(id);
		
		//创建数据sql
		JSONArray params = (JSONArray)gridJson.get("params");
		String sql = this.createSql(params);
		String name = TemplateManager.getInstance().createTemplate(sql);
		cr.setTemplateName(name);
		
		
		mv.getChildren().add(cr);
		cr.setParent(mv);
		
		Map<String, GridReportContext> crs = new HashMap<String, GridReportContext>();
		crs.put(cr.getId(), cr);
		mv.setGridReports(crs);
		
		//设置数据集
		String dsid = PortalPageService.createDsource(dsource, mv);
		cr.setRefDsource(dsid);
		
		return mv;
	}
	
	public GridReportContext json2Grid(){
		GridReportContext grid = new GridReportContextImpl();
		String lockhead = (String)gridJson.get("lockhead");
		String height = (String)gridJson.get("height");
		if("true".equals(lockhead)){
			grid.setOut("lockUI");
		}else{
			grid.setOut("html");
		}
		if(height != null && height.length() > 0){
			grid.setHeight(height);
		}
		JSONArray cols = gridJson.getJSONArray("cols");
		//生成head
		GridCell[][] headers = new GridCell[1][cols.size()];
		for(int i=0; i<cols.size(); i++){
			JSONObject col = cols.getJSONObject(i);
			GridCell cell = new GridCell();
			cell.setColSpan(1);
			cell.setRowSpan(1);
			String name = col.getString("name");
			String id = col.getString("id");
			String dispName = (String)col.get("dispName");
			cell.setDesc(dispName == null || dispName.length() == 0 ? name : dispName);
			cell.setAlias(id);
			headers[0][i] = cell;
		}
		grid.setHeaders(headers);
		
		//生成Detail
		GridCell[][] detail = new GridCell[1][cols.size()];
		for(int i=0; i<cols.size(); i++){
			JSONObject col = cols.getJSONObject(i);
			GridCell cell = new GridCell();
			String id = col.getString("id");
			String type = (String)col.get("type");
			cell.setAlias(id);
			String fmt = (String)col.get("fmt");
			String align = (String)col.get("align");
			if(fmt != null && fmt.length() > 0){
				cell.setFormatPattern(fmt);
			}
			if(align != null && align.length() > 0){
				cell.setAlign(align);
			}
			detail[0][i] = cell;
		}
		grid.setDetails(detail);
		
		//设置分页
		String pageSize = (String)gridJson.get("pageSize");
		if(pageSize == null || pageSize.length() == 0){
			pageSize = "10";
		}
		PageInfo page = new PageInfo();
		page.setPagesize(new Integer(pageSize));
		//是否禁用分页
		String isnotfy = (String)gridJson.get("isnotfy");
		if("true".equals(isnotfy)){
			
		}else{
			grid.setPageInfo(page);
		}
		return grid;
	}
	
	public String createSql(JSONArray compParams){
		Map<String, String> tableAlias = PortalPageService.createTableAlias(dset);
		StringBuffer sb = new StringBuffer("select ");
		JSONArray cols = gridJson.getJSONArray("cols");
		for(int i=0; i<cols.size(); i++){
			JSONObject col = cols.getJSONObject(i);
			String tname = col.getString("tname");
			String name = col.getString("name");
			String expression = (String)col.get("expression");  //表达式字段
			if(expression != null && expression.length() > 0){
				sb.append(" "+ expression + " as " + name);
			}else{
				sb.append(" "+tableAlias.get(tname)+"."+name);
			}
			if(i != cols.size() - 1){
				sb.append(",");
			}
		}
		JSONArray joinTabs = (JSONArray)dset.get("joininfo");
		String master = dset.getString("master");
		sb.append(" from " + master + " a0");
		
		for(int i=0; joinTabs!=null&&i<joinTabs.size(); i++){  //通过主表关联
			JSONObject tab = joinTabs.getJSONObject(i);
			String ref = tab.getString("ref");
			String refKey = tab.getString("refKey");
			String jtype = (String)tab.get("jtype");
			if("left".equals(jtype) || "right".equals(jtype)){
				sb.append(" " + jtype);
			}
			sb.append(" join " + ref+ " " + tableAlias.get(ref));
			sb.append(" on a0."+tab.getString("col")+"="+tableAlias.get(ref)+"."+refKey);
			sb.append(" ");
			
		}
		sb.append(" where 1=1 ");
		//数据权限
		if(dataControl != null){
			String ret = dataControl.process(VDOPUtils.getLoginedUser(), master);
			if(ret != null){
				sb.append(ret + " ");
			}
		}
		
		//添加参数筛选
		for(int i=0; compParams!=null&&i<compParams.size(); i++){
			JSONObject param = compParams.getJSONObject(i);
			String col = param.getString("col");
			String tname = param.getString("tname");
			String expression = (String)param.get("expression");  //如果有表达式，用表达式替换 字段
			if(expression != null && expression.length() > 0) {
				col = expression;
			}else{
				col = tableAlias.get(tname)+"." + col;
			}
			String type = param.getString("type");
			String val = (String)param.get("val");
			String val2 = (String)param.get("val2");
			String valuetype = param.getString("valuetype");
			String usetype = param.getString("usetype");
			String linkparam = (String)param.get("linkparam");
			String linkparam2 = (String)param.get("linkparam2");
			
			
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
					if("in".equals(type)){  //in需要把数据用逗号分隔的重新生成
						String[] vls = val.split(",");
						val = "";
						for(int j=0; j<vls.length; j++){
							val = val + "'" + vls[j] + "'";
							if(j != vls.length - 1){
								val = val + ",";
							}
						}
					}else{
						val = "'" + val + "'";
					}
				}
				if(val2 != null){
					val2 = "'" + val2 + "'";
				}
			}
			if(type.equals("between")){
				if(usetype.equals("gdz")){
					sb.append(" and " +  col + " " + type + " " + val + " and " + val2);
				}else{
					sb.append("#if([x]"+linkparam+" != '' && [x]"+linkparam2+" != '') ");
					sb.append(" and "  + col + " " + type + " " + ("string".equals(valuetype)?"'":"") + "[x]"+linkparam +("string".equals(valuetype)?"'":"") + " and " + ("string".equals(valuetype)?"'":"")+ "[x]"+linkparam2 + ("string".equals(valuetype)?"'":"") + " #end");
				}
			}else if(type.equals("in")){
				if(usetype.equals("gdz")){
					sb.append(" and " + col + " in (" + val + ")");
				}else{
					sb.append("#if([x]"+linkparam+" != '') ");
					sb.append(" and " + col + " in (" + "$extUtils.printVals([x]"+linkparam + ", '"+valuetype+"'))");
					sb.append("  #end");
				}
			}else{
				if(usetype.equals("gdz")){
					sb.append(" and " + col + " " + type + " " + val);
				}else{
					sb.append("#if([x]"+linkparam+" != '') ");
					sb.append(" and " + col + " "+type+" " + ("string".equals(valuetype) ? "'"+("like".equals(type)?"%":"")+""+"[x]"+linkparam+""+("like".equals(type)?"%":"")+"'":"[x]"+linkparam) + "");
					sb.append("  #end");
				}
			}
		}
		//排序字段
		for(int i=0; i<cols.size(); i++){
			JSONObject col = cols.getJSONObject(i);
			String id = col.getString("id");
			String sort = (String)col.get("sort");
			String tname = col.getString("tname");
			String expression = (String)col.get("expression");
			if(sort != null && sort.length() > 0){
				sb.append(" order by "+(expression != null && expression.length() > 0 ? "" :tableAlias.get(tname)+".") + id + " ");
				sb.append(sort);
				break;
			}
		}
		return sb.toString().replaceAll("@", "'").replaceAll("\\[x\\]", "\\$");
	}

	public Map<String, InputField> getMvParams() {
		return mvParams;
	}

	public void setGridJson(JSONObject gridJson) {
		this.gridJson = gridJson;
	}

	public void setDsource(JSONObject dsource) {
		this.dsource = dsource;
	}

	public void setPageParams(JSONArray pageParams) {
		this.pageParams = pageParams;
	}

	public void setDset(JSONObject dset) {
		this.dset = dset;
	}
	
}
