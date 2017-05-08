package com.ruisi.vdop.ser.portal;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

import com.ruisi.ext.engine.ExtConstants;
import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.ext.engine.init.TemplateManager;
import com.ruisi.ext.engine.util.IdCreater;
import com.ruisi.ext.engine.view.context.Element;
import com.ruisi.ext.engine.view.context.ExtContext;
import com.ruisi.ext.engine.view.context.MVContext;
import com.ruisi.ext.engine.view.context.chart.ChartContext;
import com.ruisi.ext.engine.view.context.cross.BaseKpiField;
import com.ruisi.ext.engine.view.context.cross.CrossKpi;
import com.ruisi.ext.engine.view.context.cross.CrossReportContext;
import com.ruisi.ext.engine.view.context.cross.RowDimContext;
import com.ruisi.ext.engine.view.context.dc.grid.GridDataCenterContext;
import com.ruisi.ext.engine.view.context.form.TextFieldContext;
import com.ruisi.ext.engine.view.context.form.TextFieldContextImpl;
import com.ruisi.ext.engine.view.context.gridreport.GridReportContext;
import com.ruisi.ext.engine.view.context.html.TextContext;
import com.ruisi.ext.engine.view.context.html.TextContextImpl;
import com.ruisi.ext.engine.view.context.html.TextProperty;
import com.ruisi.vdop.cache.ModelCacheManager;
import com.ruisi.vdop.ser.bireport.ChartService;
import com.ruisi.vdop.ser.bireport.TableSqlJsonVO;
import com.ruisi.vdop.ser.bireport.TableSqlJsonVO.DimInfo;
import com.ruisi.vdop.ser.bireport.TableSqlJsonVO.KpiInfo;
import com.ruisi.vdop.ser.olap.TableJsonService;
import com.ruisi.vdop.util.VDOPUtils;

public class PortalCompService {
	private MVContext mv;
	private PortalPageService pageSer;
	
	private TableJsonService jsonService;
	private PortalTableService tableSer;
	private PortalChartService ser;
	private ChartService chartser;
	private PortalGridService gridService;
	private PortalBoxService boxService;
	private StringBuffer scripts = new StringBuffer();  //脚本
	
	public PortalCompService(MVContext mv, PortalPageService pageSer){
		this.mv = mv;
		this.pageSer = pageSer;
		this.tableSer = new PortalTableService();
		this.chartser  = new ChartService();
		gridService = new PortalGridService();
		boxService = new PortalBoxService();
		ser = new PortalChartService();
	}
	
	public void createBox(Element td, JSONObject compJson) throws IOException{
		String dsetId = compJson.getString("dsetId");
		String dsid = compJson.getString("dsid");
		if(!pageSer.getDsids().contains(dsid)){
			pageSer.getDsids().add(dsid);
		}
		DaoHelper dao = VDOPUtils.getDaoHelper(pageSer.getSctx());
		boxService.setDset(ModelCacheManager.getDset(dsetId, dao));
		boxService.setDsource(ModelCacheManager.getDsource(dsid, dao));
		boxService.json2Box((JSONObject)compJson.get("kpiJson"), (JSONArray)compJson.get("params"), td);
	}
	
	public void createText(Element td, JSONObject compJson){
		StringBuffer css = this.pageSer.getCss();
		TextContext text = new TextContextImpl();
		JSONObject style = (JSONObject)compJson.get("style");
		if(style != null && !style.isNullObject() && !style.isEmpty()){
			TextProperty tp = new TextProperty();
			tp.setAlign((String)style.get("talign"));
			tp.setHeight((String)style.get("tfontheight"));
			tp.setSize((String)style.get("tfontsize"));
			String fontweight = (String)style.get("tfontweight");
			tp.setWeight("true".equals(fontweight)?"bold":"normal");
			tp.setColor((String)style.get("tfontcolor"));
			tp.setId("C"+IdCreater.create());
			text.setTextProperty(tp);
			
			css.append("#"+tp.getId()+"{");
			String italic = (String)style.get("titalic");
			String underscore = (String)style.get("tunderscore");
			String lineheight = (String)style.get("tlineheight");
			String tbgcolor = (String)style.get("tbgcolor");
			if("true".equals(italic)){
				css.append("font-style:italic;");
			}
			if("true".equals(underscore)){
				css.append("text-decoration: underline;");
			}
			if(lineheight != null && lineheight.length() > 0){
				css.append("line-height:"+lineheight+"px;");
			}
			if(tbgcolor != null && tbgcolor.length() > 0){
				css.append("background-color:"+tbgcolor+";");
			}
			css.append("}");
		}
		String desc = compJson.getString("desc");
		text.setText(desc);
		text.setParent(td);
		text.setFormatEnter(true);
		td.getChildren().add(text);
	}
	
	public void createChart(Element tabTd, JSONObject compJson, boolean release) throws Exception{
		String dsetId = compJson.getString("dsetId");
		String dsid = compJson.getString("dsid");
		ser.setDset(ModelCacheManager.getDset(dsetId, VDOPUtils.getDaoHelper()));
		ser.setDsource(ModelCacheManager.getDsource(dsid, VDOPUtils.getDaoHelper()));
		JSONObject chartJson = compJson.getJSONObject("chartJson");
		String compId = (String)compJson.get("id");
		if(chartJson == null || chartJson.isNullObject()){
			return;
		}
		if(compJson.get("kpiJson") == null){
			return;
		}
		JSONArray kpiJson = compJson.getJSONArray("kpiJson");
		if(kpiJson.size() == 0){
			return; //未选指标
		}
		Object firstKpi = (Object)kpiJson.get(0);
		if(firstKpi == null || firstKpi instanceof JSONNull){
			return; //未选指标
		}
		JSONArray params = (JSONArray)compJson.get("params");
		TableSqlJsonVO sqlVO = chartser.json2ChartSql(chartJson, kpiJson);
		
		ChartContext cr = ser.json2Chart(chartJson, sqlVO, compId);
		cr.setId("C" + IdCreater.create());
		DimInfo txcol = null;
		DimInfo tscol = null;
			
		//没选维度
		if(sqlVO.getChartDimCount() == 0){
			//自动生成 xcol, scol
			txcol = new DimInfo();
			txcol.setColDesc("合计");
			
			tscol = new DimInfo();
			tscol.setColDesc(sqlVO.getKpis().get(0).getKpiName());
			
		}else if(sqlVO.getChartDimCount() == 1){ //只选一个维度
			JSONObject obj = chartJson.getJSONObject("xcol");
			//选的x坐标
			if(obj != null && !obj.isNullObject() && !obj.isEmpty()){
				txcol = null;
				tscol = sqlVO.getDims().get(0);
			}
			//选的scol坐标
			obj = chartJson.getJSONObject("scol");
			if(obj != null && !obj.isNullObject() && !obj.isEmpty()){
				txcol = new DimInfo();
				txcol.setColDesc("合计");
				tscol = null;
			}
			
		}else{ //选两个维度
			
		}
		
		String sql = ser.createSql(sqlVO, txcol, tscol, (JSONArray)params, 2);
		GridDataCenterContext dc = ser.createDataCenter(sql, sqlVO);
		cr.setRefDataCenter(dc.getId());
		if(mv.getGridDataCenters() == null){
			mv.setGridDataCenters(new HashMap<String, GridDataCenterContext>());
		}
		mv.getGridDataCenters().put(dc.getId(), dc);
		
		tabTd.getChildren().add(cr);
		cr.setParent(tabTd);
		if(mv.getCharts() == null){
			Map crs = new HashMap();
			mv.setCharts(crs);
		}
		mv.getCharts().put(cr.getId(), cr);
		
		//判断是否有事件，是否需要添加参数
		JSONObject linkAccept = (JSONObject)chartJson.get("linkAccept");
		if(linkAccept != null && !linkAccept.isNullObject() && !linkAccept.isEmpty()){
			//创建参数, 如果参数已经存在就不放置
			if(!this.pageSer.getMvParams().containsKey(linkAccept.get("alias"))){
				TextFieldContext linkText = new TextFieldContextImpl();
				linkText.setType("hidden");
				linkText.setDefaultValue((String)linkAccept.get("dftval"));
				linkText.setId((String)linkAccept.get("alias"));
				linkText.setShow(true);
				mv.getChildren().add(0, linkText);
				linkText.setParent(mv);
				if(!release){
					this.pageSer.getMvParams().put(linkText.getId(), linkText);
					ExtContext.getInstance().putServiceParam(mv.getMvid(), linkText.getId(), linkText);
					mv.setShowForm(true);
				}	
			}
		}
		//设置ds
		dc.getConf().setRefDsource(dsid);
		if(!this.pageSer.getDsids().contains(dsid)){
			this.pageSer.getDsids().add(dsid);
		}
	}
	
	public void crtGrid(Element tabTd, JSONObject compJson, boolean release) throws IOException{
		JSONArray cols = (JSONArray)compJson.get("cols");
		if(cols == null || cols.isEmpty() || cols.size() == 0){
			return;
		}
		DaoHelper dao = VDOPUtils.getDaoHelper(pageSer.getSctx());
		gridService.setGridJson(compJson);
		JSONArray params = (JSONArray)compJson.get("params");
		gridService.setPageParams(params);
		String dsid = compJson.getString("dsid");
		gridService.setDsource(ModelCacheManager.getDsource(dsid,dao));
		if(!pageSer.getDsids().contains(dsid)){
			pageSer.getDsids().add(dsid);
		}
		gridService.setDset(ModelCacheManager.getDset(compJson.getString("dsetId"), dao));
		
		//创建corssReport
		GridReportContext cr = gridService.json2Grid();
		//设置ID
		String id = ExtConstants.reportIdPrefix + IdCreater.create();
		cr.setId(id);
		cr.setRefDsource(dsid);
		
		//创建数据sql
		String sql = gridService.createSql(params);
		String name = TemplateManager.getInstance().createTemplate(sql);
		cr.setTemplateName(name);
		
		tabTd.getChildren().add(cr);
		cr.setParent(tabTd);
		
		if(mv.getGridReports() == null){
			Map<String, GridReportContext> crs = new HashMap<String, GridReportContext>();
			mv.setGridReports(crs);
		}
		Map<String, GridReportContext> crs = mv.getGridReports();
		crs.put(cr.getId(), cr);
		
		mv.setGridReports(crs);
	}
	
	public void crtTable(Element tabTd, JSONObject compJson, boolean release) throws Exception {
		//初始化dset, dsource
		DaoHelper dao = VDOPUtils.getDaoHelper(pageSer.getSctx());
		String dsid = compJson.getString("dsid");
		tableSer.setDset(ModelCacheManager.getDset(compJson.getString("dsetId"), dao));
		tableSer.setDsource(ModelCacheManager.getDsource(dsid, dao));
		if(!pageSer.getDsids().contains(dsid)){
			pageSer.getDsids().add(dsid);
		}
		
		jsonService = new TableJsonService();
		JSONObject tableJson = compJson.getJSONObject("tableJson");
		if(tableJson == null || tableJson.isNullObject() ){
			return;
		}
		JSONArray params = (JSONArray)compJson.get("params");
		JSONArray kpiJson = compJson.getJSONArray("kpiJson");
		TableSqlJsonVO sqlVO = TableJsonService.json2TableSql(tableJson, kpiJson);
		
		//处理kpiOther
		CrossKpi mybaseKpi = null;
		JSONArray colsStr = tableJson.getJSONArray("cols");
		if(colsStr.size() == 0 || sqlVO.getKpis().size() == 0 || sqlVO.getKpis().size() > 1){
			JSONObject obj = new JSONObject();
			obj.put("type", "kpiOther");
			obj.put("id", "kpi");
			colsStr.add(obj);
		}else{
			//如果只有一个指标，并且具有列维度，放入baseKpi
			KpiInfo kpi = sqlVO.getKpis().get(0);
			CrossKpi baseKpi = new BaseKpiField();
			baseKpi.setAggregation(kpi.getAggre());
			baseKpi.setAlias(kpi.getAlias());
			baseKpi.setFormatPattern(kpi.getFmt());
			baseKpi.setKpiRate(kpi.getRate() == null ? null : new BigDecimal(kpi.getRate()));
			mybaseKpi = baseKpi;
		}
		
		CrossReportContext cr = jsonService.json2Table(tableJson, sqlVO, compJson.getString("id"));
		String id = ExtConstants.reportIdPrefix + IdCreater.create();
		cr.setId(id);
		String lock = (String)tableJson.get("lockhead");
		if("true".equals(lock)){
			cr.setOut("lockUI");
		}else{
			cr.setOut("HTML");
		}
		String height =  (String)tableJson.get("height");
		if(height != null && height.length() > 0){
			cr.setHeight(height);
		}
		cr.setShowData(true);
		if(mybaseKpi != null){
			cr.setBaseKpi(mybaseKpi);
		}
		
		String sql = tableSer.createSql(sqlVO, params, 2, 0);
		GridDataCenterContext dc = tableSer.createDataCenter(sql, sqlVO);
		cr.setRefDataCetner(dc.getId());
		dc.getConf().setRefDsource(dsid);
		if(mv.getGridDataCenters() == null){
			mv.setGridDataCenters(new HashMap<String, GridDataCenterContext>());
		}
		mv.getGridDataCenters().put(dc.getId(), dc);
		//判断是否有钻取维
		List<RowDimContext> drillDims = cr.getDims();
		for(int i=0; drillDims!=null&&i<drillDims.size(); i++){
			RowDimContext drillDim = drillDims.get(i);
			//生成钻取维的DataCenter
			sql = tableSer.createSql(sqlVO, params, 0, i+1);
			GridDataCenterContext drillDc = tableSer.createDataCenter(sql, sqlVO);
			drillDim.setRefDataCenter(drillDc.getId());
			mv.getGridDataCenters().put(drillDc.getId(), drillDc);
		}
		
		tabTd.getChildren().add(cr);
		cr.setParent(tabTd);
		
		//判断是否有事件，是否需要添加参数
		JSONObject linkAccept = sqlVO.getLinkAccept();
		if(linkAccept != null && !linkAccept.isNullObject() && !linkAccept.isEmpty()){
			//如果参数重复，不放置
			if(!pageSer.getMvParams().containsKey(linkAccept.get("alias"))){
				//创建参数
				TextFieldContext linkText = new TextFieldContextImpl();
				linkText.setType("hidden");
				linkText.setDefaultValue((String)linkAccept.get("dftval"));
				linkText.setId((String)linkAccept.get("alias"));
				linkText.setShow(true);
				mv.getChildren().add(0, linkText);
				linkText.setParent(mv);
				if(!release){
					this.pageSer.getMvParams().put(linkText.getId(), linkText);
					ExtContext.getInstance().putServiceParam(mv.getMvid(), linkText.getId(), linkText);
					mv.setShowForm(true);
				}
			}
			
		}
		if(mv.getCrossReports() == null){
			Map crs = new HashMap();
			mv.setCrossReports(crs);
		}
		mv.getCrossReports().put(cr.getId(), cr);
		scripts.append(jsonService.getScripts());
	}

	public StringBuffer getScripts() {
		return scripts;
	}
	
}
