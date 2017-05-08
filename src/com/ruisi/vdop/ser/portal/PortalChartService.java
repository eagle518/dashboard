package com.ruisi.vdop.ser.portal;

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
import com.ruisi.ext.engine.view.context.chart.ChartLinkContext;
import com.ruisi.ext.engine.view.context.dc.grid.GridDataCenterContext;
import com.ruisi.ext.engine.view.context.dc.grid.GridDataCenterContextImpl;
import com.ruisi.ext.engine.view.context.dc.grid.GridSetConfContext;
import com.ruisi.ext.engine.view.context.form.InputField;
import com.ruisi.ext.engine.view.context.form.TextFieldContext;
import com.ruisi.ext.engine.view.context.form.TextFieldContextImpl;
import com.ruisi.vdop.ser.bireport.ChartService;
import com.ruisi.vdop.ser.bireport.TableSqlJsonVO;
import com.ruisi.vdop.ser.bireport.TableSqlJsonVO.DimInfo;
import com.ruisi.vdop.ser.bireport.TableSqlJsonVO.KpiInfo;
import com.ruisi.vdop.service.frame.DataControlInterface;
import com.ruisi.vdop.util.VDOPUtils;

public class PortalChartService {
	
	public final static String deftMvId = "mv.portal.chart";
	
	private ChartService chartser;
	
	private Map<String, InputField> mvParams = new HashMap(); //mv的参数
	
	private Integer xcolId; //x轴维度ID
		
	private DataControlInterface dataControl; //数据权限控制
	
	private JSONObject dset;  //数据集
	private JSONObject dsource;  //数据源
	
	public PortalChartService(){
		chartser = new ChartService();
		chartser.setDset(dset);
		chartser.setDsource(dsource);
		String clazz = ExtContext.getInstance().getConstant("dataControl");
		if(clazz != null && clazz.length() != 0){
			try {
				dataControl = (DataControlInterface)Class.forName(clazz).newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
	}
	
	public MVContext json2MVByPortal(JSONObject chartJson, JSONArray kpiJson, String compId,JSONArray params, JSONArray ppar) throws Exception{
		TableSqlJsonVO sqlVO = chartser.json2ChartSql(chartJson, kpiJson);
		
		//创建MV
		MVContext mv = new MVContextImpl();
		mv.setChildren(new ArrayList());
		String formId = ExtConstants.formIdPrefix + IdCreater.create();
		mv.setFormId(formId);
		mv.setMvid(deftMvId);
		
		//处理参数,把参数设为hidden
		PortalTableService.parserHiddenParam(ppar, mv, mvParams);	
		
		//创建chart
		ChartContext cr = this.json2Chart(chartJson, sqlVO, compId);
		
		//重新设置chartId
		cr.setId("C"+compId);
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
		
		String sql = createSql(sqlVO, txcol, tscol, params, 0);
		GridDataCenterContext dc = chartser.createDataCenter(sql, sqlVO);
		cr.setRefDataCenter(dc.getId());
		if(mv.getGridDataCenters() == null){
			mv.setGridDataCenters(new HashMap<String, GridDataCenterContext>());
		}
		mv.getGridDataCenters().put(dc.getId(), dc);
		
		mv.getChildren().add(cr);
		cr.setParent(mv);
		//判断是否有事件，是否需要添加参数
		JSONObject linkAccept = (JSONObject)chartJson.get("linkAccept");
		if(linkAccept != null && !linkAccept.isNullObject() && !linkAccept.isEmpty()){
			//创建参数
			TextFieldContext linkText = new TextFieldContextImpl();
			linkText.setType("hidden");
			linkText.setDefaultValue((String)linkAccept.get("dftval"));
			linkText.setId((String)linkAccept.get("alias"));
			mv.getChildren().add(0, linkText);
			linkText.setParent(mv);
			this.mvParams.put(linkText.getId(), linkText);
			ExtContext.getInstance().putServiceParam(mv.getMvid(), linkText.getId(), linkText);
			mv.setShowForm(true);
		}
		
		Map crs = new HashMap();
		crs.put(cr.getId(), cr);
		mv.setCharts(crs);
		
		//设置数据集
		String dsid = PortalPageService.createDsource(dsource, mv);
		dc.getConf().setRefDsource(dsid);
		
		return mv;
	}
	
	public ChartContext json2Chart(JSONObject chartJson, TableSqlJsonVO sqlVO, String compId){
		ChartContext ctx = new ChartContextImpl();
		//设置x
		JSONObject obj = chartJson.getJSONObject("xcol");
		if(obj != null && !obj.isNullObject() && !obj.isEmpty()){
			String alias = (obj.getString("alias"));
			String key = (String)obj.get("tableColKey");
			String txt = (String)obj.get("tableColName");
			if(key != null && key.length() > 0 && txt != null && txt.length() > 0){  //只有在维度关联了维度表后才进行判断
				ctx.setXcolDesc(key); //用来关联ID,用在钻取中
				ctx.setXcol(txt);
			}else{
				ctx.setXcol(alias);
				ctx.setXcolDesc(alias);
			}
		}else{
			ctx.setXcol("x");
			ctx.setXcolDesc("x");
		}
		
		
		KpiInfo kpiInfo = sqlVO.getKpis().get(0);
		String y = kpiInfo.getAlias();
		ctx.setYcol(y);
		
		//如果是散点图或气泡图，需要 y2col
		String chartType = chartJson.getString("type");
		if(chartType.equals("scatter")){
			ctx.setY2col(sqlVO.getKpis().get(1).getAlias());
		}
		if(chartType.equals("bubble")){
			ctx.setY2col(sqlVO.getKpis().get(1).getAlias());
			ctx.setY3col(sqlVO.getKpis().get(2).getAlias());
		}
		
		//设置倍率
		if(kpiInfo.getRate() != null){
			ctx.setRate(kpiInfo.getRate());
		}
		if(sqlVO.getKpis().size() > 1){
			ctx.setRate2(sqlVO.getKpis().get(1).getRate());
		}
		if(sqlVO.getKpis().size() > 2){
			ctx.setRate3(sqlVO.getKpis().get(2).getRate());
		}
		
		
		JSONObject scol = chartJson.getJSONObject("scol");
		if(scol != null && !scol.isNullObject() && !scol.isEmpty()){
			ctx.setScol(scol.getString("alias"));
		}else{
			ctx.setScol("ser");
		}
		
		ctx.setShape(chartJson.getString("type"));
		if("pie".equals(ctx.getShape()) || "gauge".equals(ctx.getShape())){
			ctx.setWidth(chartJson.get("width") == null ? "360" : (String)chartJson.get("width"));
			ctx.setHeight(chartJson.get("height") == null ? "250" : (String)chartJson.get("height"));
		}else{
			ctx.setWidth(chartJson.get("width") == null ? "600" : (String)chartJson.get("width"));
			ctx.setHeight(chartJson.get("height") == null ? "250" : (String)chartJson.get("height"));
		}
		//默认图形为居中
		ctx.setAlign("center");
		
		
		//设置配置信息
		List<ChartKeyContext> properties = new ArrayList<ChartKeyContext>();
		String unitStr = formatUnits(kpiInfo) + (kpiInfo.getUnit() == null ? "" : kpiInfo.getUnit());
		
		properties.add(new ChartKeyContext("ydesc",kpiInfo.getYdispName()+(unitStr.length() == 0 ? "" : "("+unitStr+")")));
		if("bubble".equals(ctx.getShape()) || "scatter".equals(ctx.getShape())){
			KpiInfo kpiInfo2 = sqlVO.getKpis().get(1);
			//对于散点图和气泡图，需要设置xdesc
			String unit2Str =formatUnits(kpiInfo2) + (kpiInfo2.getUnit() == null ? "" : kpiInfo2.getUnit());
			properties.add(new ChartKeyContext("xdesc", kpiInfo2.getKpiName() + (unit2Str.length() == 0 ? "": "("+unit2Str+")")));
			properties.add(new ChartKeyContext("formatCol2", kpiInfo2.getFmt()));
		}else
		if(!chartJson.getJSONObject("xcol").isNullObject() && !chartJson.getJSONObject("xcol").isEmpty()){
			properties.add(new ChartKeyContext("xdesc", chartJson.getJSONObject("xcol").getString("xdispName")));
		}
		//title
		/**
		String tit = (String)chartJson.get("title");
		if(tit != null && tit.length() > 0){
			properties.add(new ChartKeyContext("title", tit));
		}
		**/
		
		
		//格式化配置信息
		if(kpiInfo.getFmt() != null && kpiInfo.getFmt().length() > 0){
			properties.add(new ChartKeyContext("formatCol", "kpi_fmt"));
		}
		
		if(kpiInfo.getUnit() != null && kpiInfo.getUnit().length() > 0){
			properties.add(new ChartKeyContext("unitCol", "kpi_unit"));
		}
		if(kpiInfo.getMin() != null && kpiInfo.getMin().length() > 0){
			properties.add(new ChartKeyContext("ymin", kpiInfo.getMin()));
		}
		if(kpiInfo.getMax() != null && kpiInfo.getMax().length() > 0){
			properties.add(new ChartKeyContext("ymax", kpiInfo.getMax()));
		}
		//lengend
		if(chartJson.get("showLegend") != null && chartJson.get("showLegend").equals("true")){
			ChartKeyContext val1 = new ChartKeyContext("showLegend", "false");
			properties.add(val1);
		}else{
			ChartKeyContext val1 = new ChartKeyContext("showLegend", "true");
			properties.add(val1);
		}
		//legendLayout
		String legendLayout = (String)chartJson.get("legendLayout");
		if(legendLayout != null){
			ChartKeyContext val1 = new ChartKeyContext("legendLayout", legendLayout);
			properties.add(val1);
		}
		//legendLayout
		String legendpos = (String)chartJson.get("legendpos");
		if(legendpos != null){
			ChartKeyContext val1 = new ChartKeyContext("legendPosition", legendpos);
			properties.add(val1);
		}
		
		if(obj != null && !obj.isNullObject() && !obj.isEmpty()){
			//取得top
			String top = (String)obj.get("top");
			if(top != null){
				ChartKeyContext val1 = new ChartKeyContext("xcnt", top);
				properties.add(val1);
			}
			if(obj.get("tickInterval") != null){
				ChartKeyContext val1 = new ChartKeyContext("tickInterval", (String)obj.get("tickInterval"));
				properties.add(val1);
			}
			if(obj.get("routeXaxisLable") != null){
				ChartKeyContext val1 = new ChartKeyContext("routeXaxisLable", (String)obj.get("routeXaxisLable"));
				properties.add(val1);
			}
		}
		
		//设置饼图是否显示标签
		String dataLabel = (String)chartJson.get("dataLabel");
		ChartKeyContext val3 = new ChartKeyContext("showLabel", dataLabel == null ? "" : dataLabel);
		properties.add(val3);
		
		//设置仪表盘数量
		ChartKeyContext val1 = new ChartKeyContext("gaugeCnt", "1");
		properties.add(val1);
		
		//marginLeft,marginRight
		String marginLeft = (String)chartJson.get("marginLeft");
		if(marginLeft != null && marginLeft.length() > 0){
			ChartKeyContext tmp = new ChartKeyContext("marginLeft", marginLeft);
			properties.add(tmp);
		}
		String marginRight = (String)chartJson.get("marginRight");
		if(marginRight != null && marginRight.length() > 0){
			ChartKeyContext tmp = new ChartKeyContext("marginRight", marginRight);
			properties.add(tmp);
		}
		
		String markerEnabled = (String)chartJson.get("markerEnabled");
		if(markerEnabled != null && "true".equals(markerEnabled)){
			ChartKeyContext md = new ChartKeyContext("markerEnabled", "false");
			properties.add(md);
		}
		//如果是地图，需要设置地图的 mapJson
		if("map".equals(ctx.getShape())){
			properties.add(new ChartKeyContext("mapJson",(String)chartJson.get("maparea")));
		}
		
		ctx.setProperties(properties);
		
		//判断是否有事件
		JSONObject link = (JSONObject)chartJson.get("link");
		if(link != null && !link.isNullObject() && !link.isEmpty()){
			ctx.setLink(createChartLink(link));
		}
		/**
		JSONObject linkAccept = (JSONObject)chartJson.get("linkAccept");
		if(linkAccept != null && !linkAccept.isNullObject() && !linkAccept.isEmpty()){
			
		}
		**/
		ctx.setLabel(compId);  //都加上label
		
		//判断曲线图、柱状图是否双坐标轴
		String typeIndex = (String)chartJson.get("typeIndex");
		if((chartType.equals("column")||chartType.equals("line")) && "2".equals(typeIndex) && sqlVO.getKpis().size() > 1 && sqlVO.getKpis().get(1) != null){
			List<KpiInfo> kpis = sqlVO.getKpis();
			ctx.setY2col(kpis.get(1).getAlias());
			String y2unit = formatUnits(kpis.get(1)) + (kpis.get(1).getUnit() == null ? "" : kpis.get(1).getUnit()) ;
			ChartKeyContext y2desc = new ChartKeyContext("y2desc", kpis.get(1).getYdispName() + (y2unit.length() ==0 ? "" : "("+y2unit+")"));
			properties.add(y2desc);
			ChartKeyContext y2fmtcol = new ChartKeyContext("formatCol2", kpis.get(1).getFmt());
			properties.add(y2fmtcol);
		}
		//判断柱状图是否显示为堆积图
		if("column".equals(chartType) && "3".equals(typeIndex)){
			ChartKeyContext stack = new ChartKeyContext("stack", "true");
			properties.add(stack);
		}
		
		return ctx;
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
	
	public ChartLinkContext createChartLink(JSONObject link){
		String type = (String)link.get("type");
		String target = (String)link.get("target");
		String url = (String)link.get("url");
		
		ChartLinkContext clink = new ChartLinkContext();
		if(url != null && url.length() > 0){
			clink.setLinkUrl(url);
		}else{
			clink.setTarget(target.split(","));
			clink.setType(type.split(","));
		}
		return clink;
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
			String tname = dim.getTname();
			String tableName = dim.getTableName();
			String key = dim.getTableColKey();
			String txt = dim.getTableColName();
			if(key != null && txt != null && key.length() >0 && txt.length() >0){
				sql.append(tableAlias.get(tableName)+"."+key+", " + tableAlias.get(tableName) + "." + txt + ",");
			}else{
				if(dim.getCalc() == 0){
					sql.append(" "+tableAlias.get(tname)+"."+dim.getColName()+" "+dim.getAlias()+", ");
				}else{
					sql.append(" " + dim.getColName() + " "+dim.getAlias()+", ");
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
		//数据权限
		if(dataControl != null){
			String ret = dataControl.process(VDOPUtils.getLoginedUser(), dset.getString("master"));
			if(ret != null){
				sql.append(ret + " ");
			}
		}
		
		//限制参数的查询条件
		sql.append(PortalTableService.dealCubeParams(params, tableAlias));
		
		//
		JSONObject linkAccept = sqlVO.getLinkAccept();
		if(linkAccept != null && !linkAccept.isNullObject() && !linkAccept.isEmpty()){
			String col = (String)linkAccept.get("col");
			String alias = (String)linkAccept.get("alias");
			String valtype = (String)linkAccept.get("valType");
			String ncol = "$" + alias;
			if("string".equalsIgnoreCase(valtype)){
				ncol = "'" + ncol + "'";
			}
			sql.append("#if($"+alias+" != '') and  " + col + " = " + ncol + " #end");
		}
		
		if(dims.size() > 0){
			sql.append(" group by ");
			for(int i=0; i<dims.size(); i++){
				DimInfo dim = dims.get(i);
				String tname = dim.getTname();
				String tableName = dim.getTableName();
				String key = dim.getTableColKey();
				String txt = dim.getTableColName();
				if(key != null && txt != null && key.length() >0 && txt.length() >0){
					sql.append(tableAlias.get(tableName)  + "." + key+", " + tableAlias.get(tableName) + "." + txt);
				}else{
					if(dim.getCalc() == 1){
						sql.append(dim.getColName());
					}else{
						sql.append(tableAlias.get(tname)+"."+dim.getColName());
					}
				}
				
				if(i != dims.size() - 1){
					sql.append(",");
				}
			}
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
						order.append(tableAlias.get(dim.getTname()) + "." + dim.getOrdcol() + " " + dim.getDimOrd() + ",");
					}else{
						order.append(tableAlias.get(dim.getTname()) + "." + dim.getColName() + " " + dim.getDimOrd() + ",");
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

	public Integer getXcolId() {
		return xcolId;
	}

	public void setMvParams(Map<String, InputField> mvParams) {
		this.mvParams = mvParams;
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
