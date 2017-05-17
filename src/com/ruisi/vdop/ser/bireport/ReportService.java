package com.ruisi.vdop.ser.bireport;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.ruisi.ext.engine.ExtConstants;
import com.ruisi.ext.engine.init.TemplateManager;
import com.ruisi.ext.engine.util.IdCreater;
import com.ruisi.ext.engine.view.context.Element;
import com.ruisi.ext.engine.view.context.MVContext;
import com.ruisi.ext.engine.view.context.MVContextImpl;
import com.ruisi.ext.engine.view.context.chart.ChartContext;
import com.ruisi.ext.engine.view.context.cross.CrossReportContext;
import com.ruisi.ext.engine.view.context.dc.grid.GridDataCenterContext;
import com.ruisi.ext.engine.view.context.dsource.DataSourceContext;
import com.ruisi.ext.engine.view.context.form.ButtonContext;
import com.ruisi.ext.engine.view.context.form.ButtonContextImpl;
import com.ruisi.ext.engine.view.context.form.CheckBoxContext;
import com.ruisi.ext.engine.view.context.form.DateSelectContext;
import com.ruisi.ext.engine.view.context.form.DateSelectContextImpl;
import com.ruisi.ext.engine.view.context.form.InputField;
import com.ruisi.ext.engine.view.context.form.MultiSelectContext;
import com.ruisi.ext.engine.view.context.form.MultiSelectContextImpl;
import com.ruisi.ext.engine.view.context.form.RadioContext;
import com.ruisi.ext.engine.view.context.form.SelectContext;
import com.ruisi.ext.engine.view.context.form.SelectContextImpl;
import com.ruisi.ext.engine.view.context.form.TextFieldContext;
import com.ruisi.ext.engine.view.context.form.TextFieldContextImpl;
import com.ruisi.ext.engine.view.context.gridreport.GridReportContext;
import com.ruisi.ext.engine.view.context.html.CustomContext;
import com.ruisi.ext.engine.view.context.html.CustomContextImpl;
import com.ruisi.ext.engine.view.context.html.DataContext;
import com.ruisi.ext.engine.view.context.html.DivContext;
import com.ruisi.ext.engine.view.context.html.DivContextImpl;
import com.ruisi.ext.engine.view.context.html.IncludeContext;
import com.ruisi.ext.engine.view.context.html.TextContext;
import com.ruisi.ext.engine.view.context.html.TextContextImpl;
import com.ruisi.ext.engine.view.context.html.table.TableContext;
import com.ruisi.ext.engine.view.context.html.table.TdContext;
import com.ruisi.ext.engine.view.context.html.table.TrContext;
import com.ruisi.vdop.cache.ModelCacheManager;
import com.ruisi.vdop.ser.bireport.TableSqlJsonVO.DimInfo;
import com.ruisi.vdop.ser.olap.TableJsonService;
import com.ruisi.vdop.ser.portal.PortalPageService;
import com.ruisi.vdop.util.VDOPUtils;
import com.ruisi.vdop.util.VdopConstant;

public class ReportService {
	
	public final static String deftMvId = "mv.export.tmp";
	private TableJsonService jsonService = new TableJsonService();
	private TableService tableSer;
	private ChartService ser;
	private ReportXMLService xmlSer ;
	private String mvid;
	
	public ReportService(){
		tableSer  = new TableService();
		ser = new ChartService();
		xmlSer = new ReportXMLService();
	}
	
	public ReportService(String mvid){
		this.mvid = mvid;
		tableSer =  new TableService();
		ser = new ChartService();
		xmlSer = new ReportXMLService();
	}
	
	public CustomContext createDataming(MVContext mv , JSONObject obj){
		CustomContext ctx = new CustomContextImpl();
		ctx.setJson(obj.toString());
		ctx.setParent(mv);
		mv.getChildren().add(ctx);
		return ctx;
	}
	
	public TextContext createText(MVContext mv, String txt){
		TextContext text = new TextContextImpl();
		text.setText(txt);
		text.setParent(mv);
		mv.getChildren().add(text);
		return text;
	}
	
	/**
	 * 
	 * @param mv
	 * @param tableJson
	 * @param kpiJson
	 * @param params
	 * @param release  判断当前是否为发布状态, 0 表示不是发布，1表示发布到多维分析，2表示发布到仪表盘
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	public CrossReportContext createTable(MVContext mv, JSONObject json, JSONArray params, int release) throws IOException, ParseException{
		JSONObject tableJson = json.getJSONObject("tableJson");
		JSONArray kpiJson = json.getJSONArray("kpiJson");
		String dsid = json.getString("dsid");
		String dsetId = json.getString("dsetId");
		tableSer.setDset(ModelCacheManager.getDset(dsetId, VDOPUtils.getDaoHelper()));
		tableSer.setDsource(ModelCacheManager.getDset(dsid, VDOPUtils.getDaoHelper()));
		TableSqlJsonVO sqlVO = TableJsonService.json2TableSql(tableJson, kpiJson);
		
		CrossReportContext cr = jsonService.json2Table(tableJson, sqlVO);
		String id = ExtConstants.reportIdPrefix + IdCreater.create();
		cr.setId(id);
		cr.setOut("html");
		cr.setShowData(true);
		
		String sql = tableSer.createSql(sqlVO,  params, release);
		//创建datacenter
		GridDataCenterContext dc = tableSer.createDataCenter(sql, sqlVO);
		cr.setRefDataCetner(dc.getId());
		dc.getConf().setRefDsource(dsid);
		if(mv.getGridDataCenters() == null){
			mv.setGridDataCenters(new HashMap<String, GridDataCenterContext>());
		}
		mv.getGridDataCenters().put(dc.getId(), dc);
		
		mv.getChildren().add(cr);
		cr.setParent(mv);
		
		Map crs = new HashMap();
		crs.put(cr.getId(), cr);
		mv.setCrossReports(crs);
		return cr;
	}
	
	public ChartContext createChart(MVContext mv, JSONObject json,  JSONArray params, int release) throws IOException, ParseException{
		JSONObject chartJson = json.getJSONObject("chartJson");
		JSONArray kpiJson = json.getJSONArray("kpiJson");
		String dsid = json.getString("dsid");
		String dsetId = json.getString("dsetId");
		ser.setDset(ModelCacheManager.getDset(dsetId, VDOPUtils.getDaoHelper()));
		ser.setDsource(ModelCacheManager.getDsource(dsid, VDOPUtils.getDaoHelper()));
		TableSqlJsonVO sqlVO = ser.json2ChartSql(chartJson, kpiJson);
		
		//创建标题
		//ser.createTitle(mv, sqlVO, kpiType, null, tinfo, false);
		
		//创建钻取维度
		StringBuffer sb = new StringBuffer("");
		int cnt = 0;
		for(DimInfo dim : sqlVO.getDims()){
			if(dim.getDimpos().equals("param")){
				if(cnt == 0){
					sb.append("钻取维：");
				}
				sb.append(dim.getColDesc()+"("+dim.getValDesc()+") - ");
				cnt++;
			}
		}
		String drillText = sb.toString();
		if(drillText.length() > 0){
			TextContext txt = new TextContextImpl();
			txt.setText(drillText);
			mv.getChildren().add(txt);
			txt.setParent(mv);
		}
		ChartContext cr = ser.json2Chart(chartJson, sqlVO, false);
		
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
		
		String sql = ser.createSql(sqlVO, txcol, tscol, params, release);
		GridDataCenterContext dc = ser.createDataCenter(sql, sqlVO);
		cr.setRefDataCenter(dc.getId());
		dc.getConf().setRefDsource(dsid);
		if(mv.getGridDataCenters() == null){
			mv.setGridDataCenters(new HashMap<String, GridDataCenterContext>());
		}
		mv.getGridDataCenters().put(dc.getId(), dc);
		
		mv.getChildren().add(cr);
		cr.setParent(mv);
		
		Map crs = new HashMap();
		crs.put(cr.getId(), cr);
		mv.setCharts(crs);
		return cr;
	}
	
	public String getFilePath(ServletContext ctx){
		String path = "";
		path = ctx.getRealPath("/") + VDOPUtils.getConstant(ExtConstants.xmlResource);
		return path;
	}
	
	/**
	 * 报表/仪表盘 mv 生成 xml
	 * @param mv
	 * @return
	 * @throws IOException 
	 */
	public String mv2XML2(MVContext mv) throws IOException{
		StringBuffer sb = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?><ext-config>");
		//生成scripts;
		if(mv.getScripts() != null && mv.getScripts().length() > 0){
			sb.append("<script><![CDATA[ "+mv.getScripts()+" ]]></script>");
		}
		List<Element> children = mv.getChildren();
		for(int i=0; i<children.size(); i++){
			Element comp = children.get(i);
			if(comp instanceof TableContext){
				TableContext table = (TableContext)comp;
				sb.append("<table");
				if(table.getStyleClass() != null && table.getStyleClass().length() > 0){
					sb.append(" class=\""+table.getStyleClass()+"\"");
				}
				sb.append(">");
				List<Element> trs = table.getChildren();
				for(int j=0; j<trs.size(); j++){
					sb.append("<tr>");
					TrContext tr = (TrContext)trs.get(j);
					List<Element> tds = tr.getChildren();
					for(int k=0; k<tds.size(); k++){
						TdContext td = (TdContext)tds.get(k);
						sb.append("<td colspan=\""+td.getColspan()+"\" width=\""+td.getWidth()+"\" rowspan=\""+td.getRowspan()+"\"");
						if(td.getStyleClass() != null && td.getStyleClass().length() > 0){
							sb.append(" styleClass=\""+td.getStyleClass()+"\"");
						}
						sb.append(">");
						
						List<Element> tdcld = td.getChildren();
						for(int l=0; l<tdcld.size(); l++){
							Element tdcldo = tdcld.get(l);
							if(tdcldo instanceof TextContext){
								xmlSer.createText(sb, (TextContext)tdcldo);
							}else if(tdcldo instanceof CrossReportContext){
								xmlSer.createCrossReport(sb, (CrossReportContext)tdcldo);
							}else if(tdcldo instanceof ChartContext){
								xmlSer.createChart(sb, (ChartContext)tdcldo);
							}else if(tdcldo instanceof GridReportContext){
								xmlSer.createGridReport(sb, (GridReportContext)tdcldo);
							}else if(tdcldo instanceof DivContext){
								xmlSer.createDiv(sb, (DivContext)tdcldo);
							}else if(tdcldo instanceof DataContext){
								DataContext data = (DataContext)tdcldo;
								xmlSer.createData(sb, data);
							}else if(tdcldo instanceof InputField){
								this.processParam(sb, tdcldo);
							}
							//对每个组件之间启用换行
							/**
							if(l != tdcld.size() - 1){
								sb.append("<br/>");
							}
							**/
						}
						
						sb.append("</td>");
					}
					sb.append("</tr>");
				}
				sb.append("</table>");
			}else if(comp instanceof DataContext){
				DataContext data = (DataContext)comp;
				xmlSer.createData(sb, data);
			}else if(comp instanceof IncludeContext){
				IncludeContext include = (IncludeContext)comp;
				xmlSer.createInclude(sb, include);
				
			}else if(comp instanceof DivContext){
				DivContext div = (DivContext)comp;
				sb.append("<div styleClass=\""+div.getStyleClass()+"\">");
				if(div.getChildren() != null){
					List ls = div.getChildren();
					for(int j=0; j<ls.size(); j++){
						Element ele = (Element)ls.get(j);
						processParam(sb, ele);
					}
				}
				sb.append("</div>");
			}else if(comp instanceof TextFieldContext){
				TextFieldContext input = (TextFieldContext)comp;
				sb.append("<textField type=\"hidden\" id=\""+input.getId()+"\" desc=\""+input.getDesc()+"\"");
				if(input.getDefaultValue() != null){
					sb.append(" defaultValue=\""+input.getDefaultValue()+"\"");
				}
				if(input.isShow()){
					sb.append(" show=\"true\" ");
				}
				sb.append("/>");
			}else if(comp instanceof TextContext){
				xmlSer.createText(sb, (TextContext)comp);
			}
		}
		//生成dataCenter
		Map<String, GridDataCenterContext> dcs = mv.getGridDataCenters();
		xmlSer.createDataCenter(sb, dcs);
		
		//生成dataSource
		Map<String, DataSourceContext> dsources = mv.getDsources();
		xmlSer.createDataSource(sb, dsources);
		sb.append("</ext-config>");
		return sb.toString();
	}
	
	public void processParam(StringBuffer sb, Element comp) throws IOException{
		if(comp instanceof TextFieldContext){
			TextFieldContext input = (TextFieldContext)comp;
			sb.append("<textField type=\""+(input.getType() == null ? input.getType() : "")+"\" id=\""+input.getId()+"\" desc=\""+(input.getDesc() == null ? "":input.getDesc())+"\"");
			if(input.getDefaultValue() != null){
				sb.append(" defaultValue=\""+input.getDefaultValue()+"\"");
			}
			if(input.isShow()){
				sb.append(" show=\"true\"");
			}
			sb.append("/>");
		}else if(comp instanceof DateSelectContext){
			DateSelectContext input = (DateSelectContext)comp;
			sb.append("<dateSelect id=\""+input.getId()+"\" desc=\""+(input.getDesc() == null ? "":input.getDesc())+"\" ");
			if(input.getDefaultValue() != null){
				sb.append(" defaultValue=\""+input.getDefaultValue()+"\"");
			}
			if(input.getShowCalendar() != null && input.getShowCalendar()){
				sb.append("	showCalendar=\"true\"");
			}
			if(input.getTarget() != null){
				sb.append("	target=\""+ReportXMLService.array2String(input.getTarget())+"\"");
			}
			sb.append("/>");
		}else if(comp instanceof SelectContext){
			SelectContext input = (SelectContext)comp;
			sb.append("<select id=\""+input.getId()+"\" desc=\""+input.getDesc()+"\" multiple=\""+(input instanceof MultiSelectContext ? "true":"")+"\" ");
			if(input.getDefaultValue() != null){
				sb.append(" defaultValue=\""+input.getDefaultValue()+"\"");
			}
			sb.append(" refDsource=\""+(input.getRefDsource()==null?"":input.getRefDsource())+"\" >");
			//判断是sql还是直接配置的 option
			if(input.getTemplateName() != null && input.getTemplateName().length() > 0){
				String sql = TemplateManager.getInstance().getTemplate(input.getTemplateName());
				sb.append("<![CDATA[");
				sb.append(sql);
				sb.append("]]>");
			}else{
				List ls = input.loadOptions();
				for(int j=0; j<ls.size(); j++){
					Map<String, String> m = (Map<String, String>)ls.get(j);
					sb.append("<option value=\""+m.get("value")+"\">"+m.get("text")+"</option>");
				}
			}
			sb.append("</select>");
		}else if(comp instanceof CheckBoxContext){	
			CheckBoxContext input = (CheckBoxContext)comp;
			sb.append("<checkBox id=\""+input.getId()+"\" desc=\""+(input.getDesc() == null ? "":input.getDesc())+"\" ");
			if(input.getDefaultValue() != null){
				sb.append(" defaultValue=\""+input.getDefaultValue()+"\"");
			}
			if(input.getShowSpan() != null && input.getShowSpan()){
				sb.append("	showSpan=\"true\"");
			}
			if(input.getCheckboxStyle() != null && input.getCheckboxStyle().length() > 0){
				sb.append("	checkboxStyle=\""+input.getCheckboxStyle()+"\"");
			}
			if(input.getTarget() != null){
				sb.append("	target=\""+ReportXMLService.array2String(input.getTarget())+"\"");
			}
			sb.append(" refDsource=\""+(input.getRefDsource()==null?"":input.getRefDsource())+"\" >");
			//判断是sql还是直接配置的 option
			if(input.getTemplateName() != null && input.getTemplateName().length() > 0){
				String sql = TemplateManager.getInstance().getTemplate(input.getTemplateName());
				sb.append("<![CDATA[");
				sb.append(sql);
				sb.append("]]>");
			}else{
				List ls = input.loadOptions();
				for(int j=0; j<ls.size(); j++){
					Map<String, String> m = (Map<String, String>)ls.get(j);
					sb.append("<option value=\""+m.get("value")+"\">"+m.get("text")+"</option>");
				}
			}
			sb.append("</checkBox>");
		}else if(comp instanceof ButtonContext){
			ButtonContext btn = (ButtonContext)comp;
			sb.append("<button type=\""+btn.getType()+"\" desc=\""+btn.getDesc()+"\" mvId=\""+(this.mvid == null ? deftMvId : this.mvid)+"\"/>");
		}else if(comp instanceof RadioContext){
			RadioContext input = (RadioContext)comp;
			sb.append("<radio id=\""+input.getId()+"\" desc=\""+(input.getDesc() == null ? "":input.getDesc())+"\" ");
			if(input.getDefaultValue() != null){
				sb.append(" defaultValue=\""+input.getDefaultValue()+"\"");
			}
			if(input.getShowSpan() != null && input.getShowSpan()){
				sb.append("	showSpan=\"true\"");
			}
			if(input.getRadioStyle() != null && input.getRadioStyle().length() > 0){
				sb.append(" radioStyle=\""+input.getRadioStyle()+"\"");
			}
			if(input.getTarget() != null){
				sb.append("	target=\""+ReportXMLService.array2String(input.getTarget())+"\"");
			}
			sb.append(" refDsource=\""+(input.getRefDsource()==null?"":input.getRefDsource())+"\" >");
			//判断是sql还是直接配置的 option
			if(input.getTemplateName() != null && input.getTemplateName().length() > 0){
				String sql = TemplateManager.getInstance().getTemplate(input.getTemplateName());
				sb.append("<![CDATA[");
				sb.append(sql);
				sb.append("]]>");
			}else{
				List ls = input.loadOptions();
				for(int j=0; j<ls.size(); j++){
					Map<String, String> m = (Map<String, String>)ls.get(j);
					sb.append("<option value=\""+m.get("value")+"\">"+m.get("text")+"</option>");
				}
			}
			sb.append("</radio>");
		}else if(comp instanceof TextContext){
			TextContext input = (TextContext)comp;
			sb.append("<text><![CDATA["+input.getText()+"]]></text>");
		}
	}
	
	/**
	 * 多维分析mv 生成 xml
	 * @param mv
	 * @return
	 * @throws IOException
	 */
	public String mv2XML(MVContext mv) throws IOException{
		StringBuffer sb = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?><ext-config>");
		//先生成scripts
		if(mv.getScripts() != null){
			sb.append("<script><![CDATA["+mv.getScripts()+"]]></script>");
		}
		List<Element> children = mv.getChildren();
		for(int i=0; i<children.size(); i++){
			Element comp = children.get(i);
			if(comp instanceof TextContext){
				xmlSer.createText(sb, (TextContext)comp);
			}else if(comp instanceof TextFieldContext){
				//处理隐藏参数
				TextFieldContext input = (TextFieldContext)comp;
				sb.append("<textField type=\"hidden\" id=\""+input.getId()+"\"");
				if(input.getDefaultValue() != null){
					sb.append(" defaultValue=\""+input.getDefaultValue()+"\"");
				}
				sb.append("/>");
			}else if(comp instanceof CrossReportContext){
				xmlSer.createCrossReport(sb, (CrossReportContext)comp);
			}else if(comp instanceof ChartContext){
				xmlSer.createChart(sb, (ChartContext)comp);
			}else if(comp instanceof DivContext){
				DivContext div = (DivContext)comp;
				sb.append("<div styleClass=\""+div.getStyleClass()+"\">");
				if(div.getChildren() != null){
					List ls = div.getChildren();
					for(int j=0; j<ls.size(); j++){
						Element ele = (Element)ls.get(j);
						processParam(sb, ele);
					}
				}
				sb.append("</div>");
			}
			//对每个组件之间启用换行
			/**
			if(i != children.size() - 1){
				sb.append("<br/>");
			}
			**/
		}
		
		//生成dataCenter
		Map<String, GridDataCenterContext> dcs = mv.getGridDataCenters();
		xmlSer.createDataCenter(sb, dcs);
		
		//生成dataSource
		Map<String, DataSourceContext> dsources = mv.getDsources();
		xmlSer.createDataSource(sb, dsources);
		sb.append("</ext-config>");
		return sb.toString();
	}
	
	public MVContext json2MV(JSONObject json) throws Exception{
		return json2MV(json, 0);
	}
	
	/**
	 * 构建 多维分析 的 mv 对象
	 * @param json
	 * @param release ==0 表示为不是发布状态，1 发布到多维分析，2 发布到仪表盘，如果是发布状态，多维分析的参数变成可以更改的。
	 * @return
	 * @throws Exception
	 */
	public MVContext json2MV(JSONObject json, int release) throws Exception{
		//创建MV
		MVContext mv = new MVContextImpl();
		mv.setChildren(new ArrayList());
		String formId = ExtConstants.formIdPrefix + IdCreater.create();
		mv.setFormId(formId);
		if(this.mvid == null || this.mvid.length() == 0){
			mv.setMvid(deftMvId);
		}else{
			mv.setMvid(this.mvid);
		}
		
		JSONArray params = json.getJSONArray("params"); 
		
		//构建参数Text
		if(!params.isEmpty()){
			if(release == 0){
				StringBuffer sb = new StringBuffer("参数： ");
				TextContext parStr = new TextContextImpl();
				for(int i=0; i<params.size(); i++){
					JSONObject param = params.getJSONObject(i);
					String name = param.getString("name");
					String type = param.getString("type");
					//String colname = param.getString("colname");
					if("frd".equals(type) || "year".equals(type) || "quarter".equals(type)){
						sb.append(name + "(" + (param.get("valStrs") == null ? "无" : param.get("valStrs"))+")");
					}else if("month".equals(type) || "day".equals(type)){
						sb.append(name + "(" + (param.get("st") == null ? "无" : param.get("st")) + " 至 " + (param.get("end") == null ? "无" : param.get("end")) + ")");
					}
					sb.append("  ");
					
					
				}
				parStr.setText(sb.toString());
				mv.getChildren().add(parStr);
				parStr.setParent(mv);
			}else{
				//把参数变成动态值
				DivContext div = new DivContextImpl();
				div.setStyleClass("rpeortParam");
				div.setChildren(new ArrayList<Element>());
				mv.getChildren().add(div);
				div.setParent(mv);
				for(int i=0; i<params.size(); i++){
					JSONObject param = params.getJSONObject(i);
					String name = param.getString("name");
					String type = param.getString("type");
					String colname = param.getString("colname");
					String values = (String)param.get("vals");
					
					InputField input = null;
					InputField input2 = null;
					if("frd".equalsIgnoreCase(type) || "year".equalsIgnoreCase(type) || "quarter".equalsIgnoreCase(type)){
						MultiSelectContextImpl target = new MultiSelectContextImpl();
						String sql = this.createDimSql(param);
						String template = TemplateManager.getInstance().createTemplate(sql);
						target.setTemplateName(template);
						input = target;
						input.setDefaultValue(values == null ? "" : values);
						input.setDesc(name);
						input.setId(colname);
					}else if("day".equalsIgnoreCase(type)){
						DateSelectContext target = new DateSelectContextImpl();
						String val = (String)param.get("st");
						target.setDefaultValue(val == null ? "" : val.replaceAll("-", ""));
						target.setDesc("开始" + name);
						target.setId("s_" + colname);
						input = target;
						
						//创建第二个参数
						DateSelectContext target2 = new DateSelectContextImpl();
						String val2 = (String)param.get("end");
						target2.setDefaultValue(val2 == null ? "" : val2.replaceAll("-", ""));
						target2.setDesc("结束" + name);
						target2.setId("e_" + colname);
						input2 = target2;
					}else if("month".equalsIgnoreCase(type)){
						SelectContextImpl target = new SelectContextImpl();
						String sql = this.createMonthSql();
						String template = TemplateManager.getInstance().createTemplate(sql);
						target.setTemplateName(template);
						input = target;
						input.setDefaultValue((String)param.get("st"));
						input.setDesc("开始" + name);
						input.setId("s_" +colname);
						
						//创建第二个参数
						SelectContextImpl target2 = new SelectContextImpl();
						String template2 = TemplateManager.getInstance().createTemplate(sql);
						target2.setTemplateName(template2);
						target2.setDefaultValue((String)param.get("end"));
						target2.setDesc("结束" + name);
						target2.setId("e_" + colname);
						input2 = target2;
					}
					div.getChildren().add(input);
					input.setParent(div);
					if(input2 != null){
						div.getChildren().add(input2);
						input2.setParent(div);
					}
				}
				ButtonContext btn = new ButtonContextImpl();
				btn.setDesc("查询");
				btn.setType("button");
				btn.setMvId(new String[]{mvid == null ? deftMvId : mvid});
				div.getChildren().add(btn);
				btn.setParent(div);
			}
		}
		JSONArray comps = json.getJSONArray("comps");
		List<String> dsids = new ArrayList();
		for(int i=0; i<comps.size(); i++){
			JSONObject obj = comps.getJSONObject(i);
			String dsid = (String)obj.get("dsid");
			if(dsid != null && dsid.length() > 0 && !dsids.contains(dsid)){
				dsids.add(dsid);
			}
			String type = obj.getString("type");
			if("text".equals(type)){
				String txt = obj.getString("text");
				createText(mv, txt);
			}
			if("table".equals(type)){
				createTable(mv, obj, params, release);
			}
			if("chart".equals(type)){
				createChart(mv, obj, params, release);
			}
			if("customComp".equals(type)){
				this.createDataming(mv , obj);
			}
		}
		//生成数据原
		for(String dsid : dsids){
			PortalPageService.createDsource(ModelCacheManager.getDsource(dsid, VDOPUtils.getDaoHelper()), mv);
		}
		
		return mv;
	}
	
	public String createDimSql(JSONObject dim){
		String tname = (String)dim.get("tableName");
		if(tname == null || tname.length() == 0){  //维度未关联码表,直接从数据中查询。
			String sql = "select distinct "+(String)dim.get("colname")+" \"value\", "+(String)dim.get("colname")+" \"text\" from " + dim.get("tname");
			sql += " order by "+dim.get("colname")+" "  + dim.get("dimord");
			return sql;
		}else{
			String sql = "select "+(String)dim.get("tableColKey")+" \"value\", "+(String)dim.get("tableColName")+" \"text\" from " + tname;
			sql += " order by "+dim.get("tableColKey")+" "  + dim.get("dimord");
			return sql;
		}
	}
	
	public String createMonthSql(){
		String sql = "select mid \"value\", mname \"text\" from code_month order by mid desc";
		return sql;
	}
	
	/**
	 * 生成导出html
	 * @param body
	 * @param host
	 * @param type 表示使用的类型，是 olap表示多维分析的导出， report 表示报表的导出
	 * @return
	 */
	public static String htmlPage(String body, String host, String type){
		StringBuffer sb = new StringBuffer();
		
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
		sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
		sb.append("<head>");
		sb.append("<title>睿思BI</title>");
		sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
		sb.append("<script type=\"text/javascript\" src=\""+host+"/ext-res/js/jquery.min.js\"></script>");
		sb.append("<script type=\"text/javascript\" src=\""+host+"/ext-res/js/ext-base.js\"></script>");
		sb.append("<script type=\"text/javascript\" src=\""+host+"/ext-res/js/echarts.min.js\"></script>");
		sb.append("<script type=\"text/javascript\" src=\""+host+"/ext-res/js/sortabletable.js\"></script>");
		sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\""+host+"/ext-res/css/fonts-min.css\" />");
		sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\""+host+"/ext-res/css/boncbase.css\" />");
		sb.append("</head>");
		sb.append("<body class=\"yui-skin-sam\">");
		if("report".equals(type)){  //报表类型需要限制宽度
			sb.append("<div style=\"width:960px; margin:0 auto;\">");
		}
		sb.append(body);
		if("report".equals(type)){
			sb.append("</div>");
		}
		sb.append("</body>");
		sb.append("</html>");
		
		return sb.toString();
	}

	public ReportXMLService getXmlSer() {
		return xmlSer;
	}
}
