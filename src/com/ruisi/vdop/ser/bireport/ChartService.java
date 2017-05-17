package com.ruisi.vdop.ser.bireport;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.ruisi.ext.engine.ExtConstants;
import com.ruisi.ext.engine.init.TemplateManager;
import com.ruisi.ext.engine.util.IdCreater;
import com.ruisi.ext.engine.view.context.ExtContext;
import com.ruisi.ext.engine.view.context.MVContext;
import com.ruisi.ext.engine.view.context.MVContextImpl;
import com.ruisi.ext.engine.view.context.chart.ChartContext;
import com.ruisi.ext.engine.view.context.chart.ChartContextImpl;
import com.ruisi.ext.engine.view.context.chart.ChartKeyContext;
import com.ruisi.ext.engine.view.context.dc.grid.GridDataCenterContext;
import com.ruisi.ext.engine.view.context.dc.grid.GridDataCenterContextImpl;
import com.ruisi.ext.engine.view.context.dc.grid.GridSetConfContext;
import com.ruisi.ext.engine.view.context.dsource.DataSourceContext;
import com.ruisi.ext.engine.view.context.form.InputField;
import com.ruisi.ext.engine.view.context.html.TextContext;
import com.ruisi.ext.engine.view.context.html.TextContextImpl;
import com.ruisi.vdop.ser.bireport.TableSqlJsonVO.DimInfo;
import com.ruisi.vdop.ser.bireport.TableSqlJsonVO.KpiFilter;
import com.ruisi.vdop.ser.bireport.TableSqlJsonVO.KpiInfo;
import com.ruisi.vdop.ser.portal.PortalPageService;
import com.ruisi.vdop.service.frame.DataControlInterface;
import com.ruisi.vdop.util.VDOPUtils;
import com.ruisi.vdop.util.VdopConstant;

public class ChartService {
	
	public final static String deftMvId = "mv.chart.tmp";
	
	private Map<String, InputField> mvParams = new HashMap(); //mv的参数
	
	private Integer xcolId; //x轴维度ID
	
	private DataControlInterface dataControl; //数据权限控制
	
	private JSONObject dset;  //数据集
	private JSONObject dsource;  //数据源
	
	public ChartService(){
		String clazz = ExtContext.getInstance().getConstant("dataControl");
		if(clazz != null && clazz.length() != 0){
			try {
				dataControl = (DataControlInterface)Class.forName(clazz).newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
	}
	
	public static DimInfo getDimFromJson(JSONObject obj){
		DimInfo dim = new DimInfo();
		dim.setType(obj.getString("type"));
		dim.setId(obj.getString("id"));
		dim.setColName((String)obj.get("colname"));
		dim.setTableName((String)obj.get("tableName"));
		dim.setTableColKey((String)obj.get("tableColKey"));
		dim.setTableColName((String)obj.get("tableColName"));
		dim.setVals((String)obj.get("vals"));
		dim.setIssum((String)obj.get("issum"));
		dim.setAlias((String)obj.get("alias"));
		dim.setDimOrd((String)obj.get("dimord"));
		dim.setColDesc((String)obj.get("dimdesc"));
		dim.setValDesc((String)obj.get("valDesc"));
		dim.setValType((String)obj.get("valType"));
		dim.setOrdcol((String)obj.get("ordcol"));
		dim.setPos((String)obj.get("pos"));
		dim.setTname((String)obj.get("tname"));
		dim.setCalc(obj.getInt("calc"));
		
		//日期、月份特殊处理
		if("day".equals(dim.getType()) && obj.get("startdt") != null && obj.get("startdt").toString().length() > 0){
			TableSqlJsonVO.QueryDay d = new TableSqlJsonVO.QueryDay();
			d.setStartDay((String)obj.get("startdt"));
			d.setEndDay((String)obj.get("enddt"));
			dim.setDay(d);
		}
		
		if("month".equals(dim.getType()) && obj.get("startmt") != null && obj.get("startmt").toString().length() > 0){
			TableSqlJsonVO.QueryMonth m = new TableSqlJsonVO.QueryMonth();
			m.setStartMonth((String)obj.get("startmt"));
			m.setEndMonth((String)obj.get("endmt"));
			dim.setMonth(m);
		}
	
		return dim;
	}
	
	public MVContext json2MV(JSONObject chartJson, JSONArray kpiJson, String compId,JSONArray params, boolean xlsdata) throws Exception{
		TableSqlJsonVO sqlVO = json2ChartSql(chartJson, kpiJson);
		
		//创建MV
		MVContext mv = new MVContextImpl();
		mv.setChildren(new ArrayList());
		String formId = ExtConstants.formIdPrefix + IdCreater.create();
		mv.setFormId(formId);
		mv.setMvid(deftMvId);
		
	
		if(!xlsdata){
			//创建图形钻取项
			this.createChartDrill(mv, sqlVO, compId);
		}
		
		//创建chart
		ChartContext cr = this.json2Chart(chartJson, sqlVO, false);
		cr.setXlsData(xlsdata);
		
		DimInfo txcol = null;
		DimInfo tscol = null;
		
			
		//没选维度, 剔除了param类型维度
		if(sqlVO.getChartDimCount() == 0){
			//自动生成 xcol, scol
			txcol = new DimInfo();
			txcol.setColDesc("合计");
			
			tscol = new DimInfo();
			String tp = chartJson.getString("type");
			if(tp.equals("scatter")){
				tscol.setColDesc("合计");
			}else{
				tscol.setColDesc(sqlVO.getKpis().get(0).getKpiName());
			}
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
		
		String sql = this.createSql(sqlVO, txcol, tscol, params, 0);
		GridDataCenterContext dc = this.createDataCenter(sql, sqlVO);
		cr.setRefDataCenter(dc.getId());
		if(mv.getGridDataCenters() == null){
			mv.setGridDataCenters(new HashMap<String, GridDataCenterContext>());
		}
		mv.getGridDataCenters().put(dc.getId(), dc);
		
		mv.getChildren().add(cr);
		cr.setParent(mv);
		
		Map crs = new HashMap();
		crs.put(cr.getId(), cr);
		mv.setCharts(crs);
		
		String dsid = PortalPageService.createDsource(dsource, mv);
		dc.getConf().setRefDsource(dsid);
		
		return mv;
	}
	
	public ChartContext json2Chart(JSONObject chartJson, TableSqlJsonVO sqlVO, boolean is3g){
		ChartContext ctx = new ChartContextImpl();
		ctx.setLabel((String)chartJson.get("label"));
		//设置x
		JSONObject obj = chartJson.getJSONObject("xcol");
		if(obj != null && !obj.isNullObject() && !obj.isEmpty()){
			String tp = obj.getString("type");
			String alias = (obj.getString("alias"));
			String key = (String)obj.get("tableColKey");
			String txt = (String)obj.get("tableColName");
			if("day".equals(tp)){
				ctx.setDateType(tp);
				ctx.setDateTypeFmt((String)obj.get("dateformat"));
			}
			if(key != null && key.length() > 0 && txt != null && txt.length() > 0){  //只有在维度关联了维度表后才进行判断
				ctx.setXcolDesc(key); //用来关联ID,用在钻取中
				ctx.setXcol(txt);
			}else{
				ctx.setXcolDesc(alias);
				ctx.setXcol(alias);
			}
			this.xcolId = obj.getInt("id");
		}else{
			ctx.setXcol("x");
			ctx.setXcolDesc("x");
		}
		
		
		KpiInfo kpiInfo = sqlVO.getKpis().get(0);
		String y = kpiInfo.getAlias();
		ctx.setYcol(y);
		
		//如果是散点图或气泡图，需要 y2col
		if(sqlVO.getKpis().size() > 1){
			ctx.setY2col(sqlVO.getKpis().get(1).getAlias());
		}
		if(sqlVO.getKpis().size() > 2){
			ctx.setY3col(sqlVO.getKpis().get(2).getAlias());
		}
		
		JSONObject scol = chartJson.getJSONObject("scol");
		if(scol != null && !scol.isNullObject() && !scol.isEmpty()){
			String tp2 = scol.getString("type");
			String tableName = (String)scol.get("tableName");
			ctx.setScol(scol.getString("alias"));
			if(tableName != null && tableName.length() > 0){  //只有在维度关联了维度表后才进行判断
				if("frd".equals(tp2) || "year".equals(tp2) || "quarter".equals(tp2)){
					ctx.setScol(ctx.getScol() + "_desc");
				}
			}
		}else{
			ctx.setScol("ser");
		}
		
		ctx.setShape(chartJson.getString("type"));
		if(is3g){
			//手机页面，宽度设置为100%
			ctx.setWidth("100%");
		}else{
			ctx.setWidth("auto");
		}
		ctx.setHeight("240");
		
		//设置ID
		String chartId = ExtConstants.chartIdPrefix + IdCreater.create();
		ctx.setId(chartId);
		
		//设置配置信息
		List<ChartKeyContext> properties = new ArrayList();
		ChartKeyContext val1 = new ChartKeyContext("margin", is3g?"30, 20, 50, 75":"30, 20, 50, 90");  //手机页面减少左边距
		properties.add(val1);
		
		//设置倍率  (在SQL中获取基本单位，运算单位（万、千、百万）等通过倍率获取 )
		if(kpiInfo.getRate() != null){
			ctx.setRate(kpiInfo.getRate());
		}
		if(sqlVO.getKpis().size() > 1){
			ctx.setRate2(sqlVO.getKpis().get(1).getRate());
		}
		if(sqlVO.getKpis().size() > 2){
			ctx.setRate3(sqlVO.getKpis().get(2).getRate());
		}
		
		properties.add(new ChartKeyContext("ydesc",kpiInfo.getKpiName()+ "(" + formatUnits(kpiInfo) +kpiInfo.getUnit()+")"));
		
		//格式化配置信息
		if(kpiInfo.getFmt() != null && kpiInfo.getFmt().length() > 0){
			properties.add(new ChartKeyContext("formatCol","kpi_fmt"));
		}
		
		if(kpiInfo.getUnit() != null && kpiInfo.getUnit().length() > 0){
			properties.add(new ChartKeyContext("unitCol","kpi_unit"));
		}
		//启用钻取
		properties.add(new ChartKeyContext("action","drillChart"));
		
		if("pie".equals(ctx.getShape())){
			properties.add(new ChartKeyContext("showLegend","true"));
			//ctx.setHeight("280"); //重新设置高度,宽度
			if(!is3g){
				ctx.setWidth("600");
			}
		}
		if("gauge".equals(ctx.getShape()) && !is3g){
			ctx.setWidth("210");
		}
		if("radar".equals(ctx.getShape()) && !is3g){
			ctx.setHeight("340"); //重新设置雷达图的高度
		}
		if("map".equals(ctx.getShape()) && !is3g){
			ctx.setWidth("600");
			ctx.setHeight("350");
		}
		if("bubble".equals(ctx.getShape()) || "scatter".equals(ctx.getShape())){
			KpiInfo kpiInfo2 = sqlVO.getKpis().get(1);
			//对于散点图和气泡图，需要设置xdesc
			properties.add(new ChartKeyContext("xdesc", kpiInfo2.getKpiName() + "(" + formatUnits(kpiInfo2) +kpiInfo2.getUnit()+")"));
			properties.add(new ChartKeyContext("formatCol2", kpiInfo2.getFmt()));
			properties.add(new ChartKeyContext("unitCol2", kpiInfo2.getUnit()));
			//设置气泡图
			if("bubble".equals(ctx.getShape())){
				KpiInfo kpiInfo3 = sqlVO.getKpis().get(2);
				properties.add(new ChartKeyContext("formatCol3", kpiInfo3.getFmt()));
				properties.add(new ChartKeyContext("unitCol3", kpiInfo3.getUnit()));
			}
		}
		//对于曲线图、柱状图设置图例位置
		if("line".equals(ctx.getShape()) || "column".equals(ctx.getShape())){
			properties.add(new ChartKeyContext("legendLayout","horizontal"));
		}
		//饼图不显示Legend
		if("pie".equals(ctx.getShape())){
			properties.add(new ChartKeyContext("showLegend","false"));
		}
		
		//如果是地图，需要设置地图的 mapJson
		if("map".equals(ctx.getShape())){
			properties.add(new ChartKeyContext("mapJson","china.json"));
		}
		//手机页面，横轴旋转角度
		if(is3g){
			properties.add(new ChartKeyContext("routeXaxisLable","-45"));
		}
		
		ctx.setProperties(properties);
		
		return ctx;
	}
	
	public TableSqlJsonVO json2ChartSql(JSONObject chartJson, JSONArray kpiJson){
		TableSqlJsonVO vo = new TableSqlJsonVO();
		
		//取时间
		JSONObject baseDate = (JSONObject)chartJson.get("baseDate");
		if(baseDate != null && !baseDate.isNullObject()){
			TableSqlJsonVO.BaseDate bd = new TableSqlJsonVO.BaseDate();
			bd.setStart((String)baseDate.get("start"));
			bd.setEnd((String)baseDate.get("end"));
			vo.setBaseDate(bd);
		}
		//取维度
		JSONObject xcol = chartJson.getJSONObject("xcol");
		if(xcol != null && !xcol.isNullObject() && !xcol.isEmpty()){
			DimInfo dim = getDimFromJson(xcol);
			dim.setDimpos("xcol"); //表示维度来路
			vo.getDims().add(dim);
		}
		JSONObject scol = chartJson.getJSONObject("scol");
		if(scol != null && !scol.isNullObject() && !scol.isEmpty()){
			DimInfo dim = getDimFromJson(scol);
			dim.setDimpos("xcol"); //表示维度来路
			vo.getDims().add(dim);
		}
		
		
		
		JSONArray params = chartJson.get("params") == null ? null : chartJson.getJSONArray("params");
		if(params != null && !params.isEmpty()){
			for(int i=0; i<params.size(); i++){
				JSONObject obj = params.getJSONObject(i);
				DimInfo dim = getDimFromJson(obj);
				dim.setDimpos("param"); //表示维度来路
				vo.getDims().add(dim);
			}
		}
		
		
		//取linkAccept
		vo.setLinkAccept((JSONObject)chartJson.get("linkAccept"));
		
		//取指标
		for(int i=0; i<kpiJson.size(); i++){
			JSONObject kpij = kpiJson.getJSONObject(i);
			if(kpij == null || kpij.isNullObject() || kpij.isEmpty()){
				continue;
			}
			KpiInfo kpi = new KpiInfo();
			kpi.setAggre(kpij.getString("aggre"));
			kpi.setAlias(kpij.getString("alias"));
			kpi.setColName(kpij.getString("col_name"));
			kpi.setFmt(kpij.getString("fmt"));
			kpi.setKpiName(kpij.getString("kpi_name"));
			kpi.setYdispName((String)kpij.get("ydispName"));
			kpi.setTname(kpij.getString("tname"));
			kpi.setDescKey((String)kpij.get("descKey"));
			if(kpij.get("rate") != null && kpij.get("rate").toString().length() > 0 && !kpij.get("rate").toString().equals("null")){
				kpi.setRate(kpij.getInt("rate"));
			}
			kpi.setId(kpij.get("kpi_id") == null ? null :kpij.get("kpi_id").toString());
			kpi.setUnit((String)kpij.get("unit"));
			kpi.setSort((String)kpij.get("sort"));
			kpi.setMin((String)kpij.get("ymin"));
			kpi.setMax((String)kpij.get("ymax"));
			vo.getKpis().add(kpi);
			
			Object ftObj = kpij.get("filter");
			if(ftObj != null){
				JSONObject ft = (JSONObject)ftObj;
				KpiFilter kf = new KpiFilter();
				kf.setFilterType(ft.getString("filterType"));
				kf.setVal1(ft.getDouble("val1"));
				kf.setVal2(ft.getDouble("val2"));
				kpi.setFilter(kf);
			}
		}
		
		return vo;
	}
	
	public static String formatUnits(KpiInfo kpi){
		Integer rate = kpi.getRate();
		if(rate == null){
			return "";
		}
		if(rate.intValue() == 1000){
			return "千";
		}else if(rate.intValue() == 10000){
			return "万";
		}else if(rate.intValue() == 1000000){
			return "百万";
		}else if(rate.intValue() == 100000000){
			return "亿";
		}
		return "";
	}
	
	
	
	/**
	 * 创建图形钻取菜单
	 * @param mv
	 */
	public void createChartDrill(MVContext mv, TableSqlJsonVO sqlVO, String compId){
		StringBuffer txt = new StringBuffer();
		txt.append("<div class=\"chartdrillmenu\">");
		
		int cnt = 0;
		for(DimInfo dim : sqlVO.getDims()){
			if(dim.getDimpos().equals("param")){
				if(cnt == 0){
					txt.append("钻取维：");
				}
				txt.append("<span class=\"chartdrillDim\"><a href=\"javascript:;\" title=\"上卷\" onclick=\"chartGoupDim("+compId+", "+dim.getId()+",'"+dim.getPos()+"',true)\" style=\"opacity:0.5\"></a>"+dim.getColDesc()+"("+dim.getValDesc()+")</span>");
				cnt++;
			}
		}
		if(cnt == 0){
			txt.append("<span class=\"charttip\">(点击图形节点进行钻取分析)</span>");
		}
		txt.append("</div>");
		
		TextContext text = new TextContextImpl();
		text.setText(txt.toString());
		text.setParent(mv);
		mv.getChildren().add(text);
	}
	
	/**
	 * 处理地图的地域维度。
	 * 对于地图展现，默认添加地域维度
	 */
	public void dealMapArea(){
		
	}
	
	/**
	 * 创建图形的dataCenter
	 * @param sql
	 * @param sqlVO
	 * @return
	 * @throws IOException
	 */
	public GridDataCenterContext createDataCenter(String sql, TableSqlJsonVO sqlVO) throws IOException{
		GridDataCenterContext ctx = new GridDataCenterContextImpl();
		GridSetConfContext conf = new GridSetConfContext();
		ctx.setConf(conf);
		ctx.setId("DC-" + IdCreater.create());
		String name = TemplateManager.getInstance().createTemplate(sql);
		ctx.getConf().setTemplateName(name);
		
		return ctx;
	}
	
	/**
	public MVContext json2MVByApi(JSONObject chartJson, JSONArray kpiJson, String compId,JSONArray params, String dsid) throws Exception{
		TableSqlJsonVO sqlVO = json2ChartSql(chartJson, kpiJson);
		
		//创建MV
		MVContext mv = new MVContextImpl();
		mv.setChildren(new ArrayList());
		String formId = ExtConstants.formIdPrefix + IdCreater.create();
		mv.setFormId(formId);
		mv.setMvid(deftMvId);
		
		//获取所查询数据表信息
		Map tinfo = ChartService.selectTable(sqlVO, params);
		
		
		
		//创建图形钻取项
		//this.createChartDrill(mv, sqlVO, compId);
		
		//创建标题
		//createTitle(mv, sqlVO,  compId, tinfo, true);
		
		//创建chart
		ChartContext cr = this.json2Chart(chartJson, sqlVO, false);
		
		DimInfo txcol = null;
		DimInfo tscol = null;
		
			
		//没选维度, 剔除了param类型维度
		if(sqlVO.getChartDimCount() == 0){
			//自动生成 xcol, scol
			txcol = new DimInfo();
			txcol.setColDesc("合计");
			
			tscol = new DimInfo();
			String tp = chartJson.getString("type");
			if(tp.equals("scatter")){
				tscol.setColDesc("合计");
			}else{
				tscol.setColDesc(sqlVO.getKpis().get(0).getKpiName());
			}
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
		
		String sql = this.createSql(sqlVO, txcol, tscol, tinfo, params, false);
		GridDataCenterContext dc = this.createDataCenter(sql, sqlVO);
		cr.setRefDataCenter(dc.getId());
		if(mv.getGridDataCenters() == null){
			mv.setGridDataCenters(new HashMap<String, GridDataCenterContext>());
		}
		mv.getGridDataCenters().put(dc.getId(), dc);
		
		mv.getChildren().add(cr);
		cr.setParent(mv);
		
		Map crs = new HashMap();
		crs.put(cr.getId(), cr);
		mv.setCharts(crs);
		
		//判断是否需要创建数据源
		if(dsid != null && dsid.length() > 0){
			//表格显示需要用到dsource
			cr.setRefDsource(dsid);
			DataSourceContext ds = new DataSourceContext();
			String dsstr = (String)VDOPUtils.getDaoHelper().queryForObject("select content from "+VdopConstant.getSysUser()+".olap_obj_share where id='"+dsid+"' and tp='dsource'", String.class);
			JSONObject dsource = JSONObject.fromObject(dsstr);
			String use = (String)dsource.get("use");
			if(use == null || use.equals("jdbc")){
				ds.putProperty("linktype", dsource.getString("linktype"));
				ds.putProperty("linkname", dsource.getString("linkname"));
				ds.putProperty("linkpwd", dsource.getString("linkpwd"));
				ds.putProperty("linkurl", dsource.getString("linkurl"));
			}else{
				ds.putProperty("jndiname", dsource.getString("jndiname"));
			}
			ds.putProperty("id", dsource.getString("dsid"));
			ds.putProperty("use", use);
			
			if(mv.getDsources() == null){
				mv.setDsources(new HashMap<String, DataSourceContext>());
			}
			mv.getDsources().put(ds.getId(), ds);
		}
		if(this.cacheDims.size() > 0){
			mv.setScripts(TableService.cache2Script(this.cacheDims));
		}
		return mv;
	}
**/
	
	public static int type2value(String tp){
		int curDate = 4;;
		if(tp.equals("year")){
			curDate = 4;
		}else if(tp.equals("quarter")){
			curDate = 3;
		}else if(tp.equals("month")){
			curDate = 2;
		}else if(tp.equals("day")){
			curDate = 1;
		}
		return curDate;
	}
	
	/**
	 * 创建sql语句所用函数，图形用这个函数创建SQL
	 * 其中第二个参数只用在图形中，当用户没选X轴时(xcol)时，用这个做默认xcol
	 * 其中第三个参数只用在图形中，当用户没选图例(scol)时，用这个做默认图例
	 * release 表示当前为发布状态, 0 表示不是发布，1表示发布到多维分析，2表示发布到仪表盘
	 * @param sqlVO
	 * @param ser
	 * @return
	 * @throws ParseException
	 */
	public String createSql(TableSqlJsonVO sqlVO, DimInfo xcol, DimInfo ser, JSONArray params, int release) throws ParseException{
		Map<String, String> tableAlias = PortalPageService.createTableAlias(dset);
		
		StringBuffer sql = new StringBuffer();
		
		sql.append("select ");
		List<DimInfo> dims = sqlVO.getDims();
		for(int i=0; i<dims.size(); i++){
			DimInfo dim = dims.get(i);
			String key = dim.getTableColKey();
			String txt = dim.getTableColName();
			String tname = dim.getTableName();
			if(key != null && txt != null && key.length() >0 && txt.length() >0){
				sql.append(tableAlias.get(tname)+"."+key+", " + tableAlias.get(tname) +"."+ txt + ",");
			}else{
				if(dim.getCalc() == 1){ 
					sql.append(" "+dim.getColName()+" "+dim.getAlias()+", ");
				}else{
					sql.append(" " + tableAlias.get(dim.getTname()) + "."+dim.getColName()+" "+dim.getAlias()+", ");
				}
			}
			
		}
		
		//判断是否需要添加ser项，只用在图形中
		if(ser != null){
			sql.append("'"+ser.getColDesc()+"' ser, ");
		}
		if(xcol != null){
			sql.append("'"+xcol.getColDesc()+"' x, ");
		}
		
		//判断是否添加 格式化 字段
		KpiInfo info = sqlVO.getKpis().get(0);
		if(info.getFmt() != null && info.getFmt().length() > 0){
			sql.append("'"+info.getFmt()+"' kpi_fmt,");
		}
		//判断是否添加单位字段
		if(info.getUnit() != null && info.getUnit().length() > 0){
			//sql.append("'" + ChartService.formatUnits(info)+info.getUnit()+"' kpi_unit,");
			sql.append("'" + info.getUnit()+"' kpi_unit,");
		}
		
		//第二个指标
		if(sqlVO.getKpis().size() > 1){
			KpiInfo sinfo = sqlVO.getKpis().get(1);
			if(sinfo.getFmt() != null && sinfo.getFmt().length() > 0){
				sql.append("'"+sinfo.getFmt()+"' kpi_fmt2,");
			}
			if(sinfo.getUnit() != null && sinfo.getUnit().length() > 0){
				sql.append("'" +sinfo.getUnit()+"' kpi_unit2,");
			}
		}
		//第三个指标
		if(sqlVO.getKpis().size() > 2){
			KpiInfo sinfo = sqlVO.getKpis().get(2);
			if(sinfo.getFmt() != null && sinfo.getFmt().length() > 0){
				sql.append("'"+sinfo.getFmt()+"' kpi_fmt3,");
			}
			if(sinfo.getUnit() != null && sinfo.getUnit().length() > 0){
				sql.append("'" +sinfo.getUnit()+"' kpi_unit3,");
			}
		}
		
		List<KpiInfo> kpis = sqlVO.getKpis();
		if(kpis.size() == 0){
			sql.append(" null kpi_value ");
		}else{
			for(int i=0; i<kpis.size(); i++){
				KpiInfo kpi = kpis.get(i);
				sql.append(kpi.getColName() + " ");
				sql.append(kpi.getAlias());
				
				if(i != kpis.size() - 1){
					sql.append(",");
				}
			}
		}
		
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
		
		//数据权限控制筛选
		if(dataControl != null){
			String ret = dataControl.process(VDOPUtils.getLoginedUser(),  dset.getString("master"));
			if(ret != null){
				sql.append(ret + " ");
			}
		}
		for(int i=0; i<dims.size(); i++){
			DimInfo dim = dims.get(i);
			if(dim.getType().equals("frd")  || "year".equals(dim.getType()) || "quarter".equals(dim.getType())){
				
				//限制维度筛选
				if(dim.getVals() != null && dim.getVals().length() > 0){
					String  vls = dim.getVals();
					if("string".equalsIgnoreCase(dim.getValType())){
						vls = VDOPUtils.dealStringParam(dim.getVals());
					}
					sql.append(" and " + dim.getColName() + " in ("+vls+")");
				}
			}else
			//处理日期限制
			if(dim.getType().equals("day")){
				if(dim.getDay() != null){
					sql.append(" and " + dim.getColName() + " between '"+dim.getDay().getStartDay()+"' and '" + dim.getDay().getEndDay()+"'");
				}else
				if(dim.getVals() != null && dim.getVals().length() > 0){
					String vls = VDOPUtils.dealStringParam(dim.getVals());
					sql.append(" and " + dim.getColName() + " in ("+vls+")");
				}
			}else
			if(dim.getType().equals("month")){
				if(dim.getMonth() != null){
					sql.append(" and " + dim.getColName() + " between '"+dim.getMonth().getStartMonth()+"' and '" + dim.getMonth().getEndMonth()+"'");
				}else
				if(dim.getVals() != null && dim.getVals().length() > 0){
					String vls = VDOPUtils.dealStringParam(dim.getVals());
					sql.append(" and " + dim.getColName() + " in ("+vls+")");
					//isDealDate = true;
				}
			}
		
		}
		
		//限制日期和月份维度， 默认为日期365天，月份12月，用户可以在 数据周期中修改
		/**
		if(sqlVO.getBaseDate() == null){
			if((Integer)tinfo.get("ttype") == 1){
				sql.append(" and " +  tinfo.get("datecol") + "$extUtils.dayAdd($s.defDay, -365)");
			}else{
				sql.append(" and " +  tinfo.get("datecol") +  "$extUtils.monthAdd($s.defMonth, -12)");
			}
		}else{
			sql.append(" and " +  tinfo.get("datecol") + " between "+sqlVO.getBaseDate().getStart()+" and " + sqlVO.getBaseDate().getEnd());
		}
		**/
		
		//限制参数的查询条件
		for(int i=0; params!=null&&i<params.size(); i++){
			JSONObject param = params.getJSONObject(i);
			int cubeId = param.getInt("cubeId");
			String tp = param.getString("type");
			String colname = param.getString("colname");
			String alias = param.getString("alias");
			String dateformat = (String)param.get("dateformat");
			//只有参数和组件都来源于同一个表，才能进行参数拼装
			if((tp.equals("frd") || "year".equals(tp) || "quarter".equals(tp))){
				if(release == 0 && param.get("vals") != null && param.getString("vals").length() > 0){
					//字符串特殊处理
					String  vls = param.getString("vals");
					if("string".equalsIgnoreCase(param.getString("valType"))){
						vls = VDOPUtils.dealStringParam(param.getString("vals"));
					}
					sql.append(" and " + colname + " in ("+vls+")");
				}else if(release == 1 || release == 2){
					sql.append(" #if($"+alias+" != '') and " + colname + " in ($extUtils.printVals($"+alias+", '"+param.getString("valType")+"')) #end");
				}
			}else if((tp.equals("day") || tp.equals("month"))){
				if(release == 0 && param.get("st") != null && param.getString("st").length() > 0 ){
					sql.append(" and " + colname + " between '"+ param.getString("st") + "' and '" +  param.getString("end") + "'");
				}else if(release == 1){
					sql.append(" and " + colname + " between '$s_"+alias+"' and '$e_"+alias+"'");
				}else if(release == 2){
					sql.append(" #if($"+alias+" != '') and " + colname + " = $"+alias + " #end");   //仪表盘发布，日期,月份只有一个参数
				}
			}
		}
		
		//
		JSONObject linkAccept = sqlVO.getLinkAccept();
		if(linkAccept != null && !linkAccept.isNullObject() && !linkAccept.isEmpty()){
			String col = (String)linkAccept.get("col");
			String valtype = (String)linkAccept.get("valType");
			String ncol = "$" + col;
			if("string".equalsIgnoreCase(valtype)){
				ncol = "'" + ncol + "'";
			}
			sql.append(" and  " + col + " = " + ncol);
		}
		
		if(dims.size() > 0){
			sql.append(" group by ");
			for(int i=0; i<dims.size(); i++){
				DimInfo dim = dims.get(i);
				String key = dim.getTableColKey();
				String txt = dim.getTableColName();
				String tname = dim.getTableName();
				if(key != null && txt != null && key.length() >0 && txt.length() >0){
					sql.append(tableAlias.get(tname)+"."+key+", " + tableAlias.get(tname) + "." + txt);
				}else{
					if(dim.getCalc() == 1){
						sql.append(dim.getColName());
					}else{
						sql.append(tableAlias.get(dim.getTname())+"."+dim.getColName());
					}
				}
				if(i != dims.size() - 1){
					sql.append(",");
				}
			}
		}
		//处理指标过滤
		StringBuffer filter = new StringBuffer("");
		for(KpiInfo kpi : sqlVO.getKpis()){
			if(kpi.getFilter() != null){
				filter.append(" and "+kpi.getColName()+" ");
				String tp = kpi.getFilter().getFilterType();
				filter.append(tp);
				filter.append(" ");
				double val1 = kpi.getFilter().getVal1();
				if(kpi.getFmt() != null && kpi.getFmt().endsWith("%")){
					val1 = val1 / 100;
				}
				filter.append(val1 * (kpi.getRate() == null ? 1 : kpi.getRate()));
				if("between".equals(tp)){
					double val2 = kpi.getFilter().getVal2();
					if(kpi.getFmt() != null && kpi.getFmt().endsWith("%")){
						val2 = val2 / 100;
					}
					filter.append(" and " + val2 * (kpi.getRate() == null ? 1 : kpi.getRate()));
				}
			}
		}
		if(filter.length() > 0){
			sql.append(" having 1=1 " + filter);
		}
		if(dims.size() > 0){
			StringBuffer order = new StringBuffer();
			order.append(" order by ");
			KpiInfo kpi = sqlVO.getKpis().get(0);
			if(kpi.getSort() != null && kpi.getSort().length() > 0){
				order.append(kpi.getAlias() + " " + kpi.getSort()) ;
				order.append(",");
			}
			for(int i=0; i<dims.size(); i++){
				DimInfo dim = dims.get(i);
				if(dim.getDimOrd() != null && dim.getDimOrd().length() > 0){
					if(dim.getOrdcol() != null && dim.getOrdcol().length() > 0){  //处理维度排序
						order.append(dim.getOrdcol() + " " + dim.getDimOrd() + ",");
					}else{
						order.append("" + dim.getColName() + " " + dim.getDimOrd() + ",");
					}
				}
			}
			if(order.length() <= 11 ){  //判断是否拼接了 order by 字段
				
			}else{
				//返回前先去除最后的逗号
				 sql.append(order.toString().substring(0, order.length() - 1));
			}
		}
		String ret = sql.toString();
		//替换 ## 为 函数，##在velocity中为注释意思
		ret = ret.replaceAll("##", "\\$extUtils.printJH()").replaceAll("@", "'");
		return ret;
	}

	public Map<String, InputField> getMvParams() {
		return mvParams;
	}

	public void setMvParams(Map<String, InputField> mvParams) {
		this.mvParams = mvParams;
	}

	public Integer getXcolId() {
		return xcolId;
	}

	public void setXcolId(Integer xcolId) {
		this.xcolId = xcolId;
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
