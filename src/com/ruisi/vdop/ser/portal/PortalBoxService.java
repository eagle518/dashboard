package com.ruisi.vdop.ser.portal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.ruisi.ext.engine.ExtConstants;
import com.ruisi.ext.engine.init.TemplateManager;
import com.ruisi.ext.engine.util.IdCreater;
import com.ruisi.ext.engine.view.context.Element;
import com.ruisi.ext.engine.view.context.ExtContext;
import com.ruisi.ext.engine.view.context.MVContext;
import com.ruisi.ext.engine.view.context.MVContextImpl;
import com.ruisi.ext.engine.view.context.form.InputField;
import com.ruisi.ext.engine.view.context.html.DataContext;
import com.ruisi.ext.engine.view.context.html.DataContextImpl;
import com.ruisi.ext.engine.view.context.html.TextContext;
import com.ruisi.ext.engine.view.context.html.TextContextImpl;
import com.ruisi.ext.engine.view.context.html.TextProperty;
import com.ruisi.ext.engine.view.emitter.highcharts.util.ChartUtils;
import com.ruisi.vdop.service.frame.DataControlInterface;
import com.ruisi.vdop.util.VDOPUtils;

public class PortalBoxService {
	
	public final static String deftMvId = "mv.portal.box";
	
	private Map<String, InputField> mvParams = new HashMap(); //mv的参数
		
	private DataControlInterface dataControl; //数据权限控制
	
	private JSONObject dset;  //数据集
	private JSONObject dsource; //数据源
	
	public PortalBoxService(){
		String clazz = ExtContext.getInstance().getConstant("dataControl");
		if(clazz != null && clazz.length() != 0){
			try {
				dataControl = (DataControlInterface)Class.forName(clazz).newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
	}
	
	public MVContext json2MV(JSONObject kpiJson, JSONArray params, JSONArray ppar) throws Exception{
		//创建MV
		MVContext mv = new MVContextImpl();
		mv.setChildren(new ArrayList());
		String formId = ExtConstants.formIdPrefix + IdCreater.create();
		mv.setFormId(formId);
		mv.setMvid(deftMvId);
		
		//处理参数,把参数设为hidden
		PortalTableService.parserHiddenParam(ppar, mv, mvParams);	
		
		this.json2Box(kpiJson, params, mv);
		
		PortalPageService.createDsource(dsource, mv);
		
		
		return mv;
	}
	
	/**
	 * 通过数据生成 box 块
	 * @param mv
	 * @throws IOException 
	 */
	public void json2Box(JSONObject kpiJson, JSONArray params, Element mv) throws IOException{
		if(kpiJson == null || kpiJson.isNullObject() || kpiJson.isEmpty()){
			return;
		}
		//创建box 的 data 标签
		String sql = createSql(kpiJson, params);
		DataContext data = new DataContextImpl();
		data.setKey("k" + kpiJson.get("kpi_id"));
		data.setRefDsource(dsource.getString("id"));
		String name = TemplateManager.getInstance().createTemplate(sql);
		data.setTemplateName(name);
		mv.getChildren().add(data);
		data.setParent(mv);
		
		//创建box 显示 text 标签
		TextContext text = new TextContextImpl();
		String str = "#if($!k"+kpiJson.get("kpi_id")+"."+kpiJson.get("alias")+") $extUtils.numberFmt($!k"+kpiJson.get("kpi_id")+"."+kpiJson.get("alias")+", '"+kpiJson.get("fmt")+"') <font size='4' color='#999999'>" ;
		Object rate = kpiJson.get("rate");
		if(rate != null && rate.toString().length() > 0 && !rate.toString().equalsIgnoreCase("null")){
			str += ChartUtils.writerUnit(new Integer(rate.toString()));
		}
		str += kpiJson.get("unit")+"</font>";
		str += "#else - #end";
		String word = TemplateManager.getInstance().createTemplate(str);
		text.setTemplateName(word);
		text.setFormatHtml(true);
		TextProperty tp = new TextProperty();
		tp.setAlign("center");
		tp.setColor("#000000");
		tp.setWeight("bold");
		tp.setSize("32");
		tp.setStyleClass("boxcls");
		text.setTextProperty(tp);
		mv.getChildren().add(text);
		text.setParent(mv);
	}
	
	public String createSql(JSONObject kpiJson, JSONArray params){
		Map<String, String> tableAlias = PortalPageService.createTableAlias(dset);
		
		StringBuffer sql = new StringBuffer();
		sql.append("select ");
		sql.append(kpiJson.get("col_name"));
		Object rate = kpiJson.get("rate");
		if(rate != null && rate.toString().length() > 0 && !rate.toString().equalsIgnoreCase("null")){
			sql.append("/" + rate);
		}
		sql.append(" as ");
		sql.append(kpiJson.get("alias"));
		
		JSONArray joinTabs = (JSONArray)dset.get("joininfo");
		String master = dset.getString("master");
		sql.append(" from " + master + " a0");
		
		for(int i=0; joinTabs!=null&&i<joinTabs.size(); i++){  //通过主表关联
			JSONObject tab = joinTabs.getJSONObject(i);
			String ref = tab.getString("ref");
			String refKey = tab.getString("refKey");
			String jtype = (String)tab.get("jtype");
			if("left".equals(jtype) || "right".equals(jtype)){
				sql.append(" " + jtype);
			}
			sql.append(" join " + ref+ " " + tableAlias.get(ref));
			sql.append(" on a0."+tab.getString("col")+"="+tableAlias.get(ref)+"."+refKey);
			sql.append(" ");
		}
		sql.append(" where 1=1 ");
		
		if(dataControl != null){
			String ret = dataControl.process(VDOPUtils.getLoginedUser(), (String)dset.get("master"));
			if(ret != null){
				sql.append(ret + " ");
			}
		}
		sql.append(" " + PortalTableService.dealCubeParams(params, tableAlias));
		return sql.toString().replaceAll("@", "'");
	}

	public Map<String, InputField> getMvParams() {
		return mvParams;
	}

	public JSONObject getDset() {
		return dset;
	}

	public JSONObject getDsource() {
		return dsource;
	}

	public void setDset(JSONObject dset) {
		this.dset = dset;
	}

	public void setDsource(JSONObject dsource) {
		this.dsource = dsource;
	}
	
	
}
