package com.ruisi.vdop.ser.bireport;

import java.io.IOException;
import java.math.BigDecimal;
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
import com.ruisi.ext.engine.view.context.cross.BaseKpiField;
import com.ruisi.ext.engine.view.context.cross.CrossKpi;
import com.ruisi.ext.engine.view.context.cross.CrossReportContext;
import com.ruisi.ext.engine.view.context.dc.grid.ComputeMoveAvgContext;
import com.ruisi.ext.engine.view.context.dc.grid.GridAccountContext;
import com.ruisi.ext.engine.view.context.dc.grid.GridDataCenterContext;
import com.ruisi.ext.engine.view.context.dc.grid.GridDataCenterContextImpl;
import com.ruisi.ext.engine.view.context.dc.grid.GridFilterContext;
import com.ruisi.ext.engine.view.context.dc.grid.GridJoinContext;
import com.ruisi.ext.engine.view.context.dc.grid.GridSetConfContext;
import com.ruisi.ext.engine.view.context.dc.grid.GridShiftContext;
import com.ruisi.ext.engine.view.context.dc.grid.GridSortContext;
import com.ruisi.ext.engine.view.context.dsource.DataSourceContext;
import com.ruisi.ext.engine.view.context.form.InputField;
import com.ruisi.ext.engine.view.context.form.TextFieldContext;
import com.ruisi.ext.engine.view.context.form.TextFieldContextImpl;
import com.ruisi.ext.engine.view.context.html.TextContext;
import com.ruisi.ext.engine.view.context.html.TextContextImpl;
import com.ruisi.ext.engine.view.context.html.TextProperty;
import com.ruisi.ispire.dc.grid.GridFilter;
import com.ruisi.ispire.dc.grid.GridProcContext;
import com.ruisi.ispire.dc.grid.GridShift;
import com.ruisi.vdop.ser.bireport.TableSqlJsonVO.DimInfo;
import com.ruisi.vdop.ser.bireport.TableSqlJsonVO.KpiInfo;
import com.ruisi.vdop.ser.bireport.TableSqlJsonVO.QueryDay;
import com.ruisi.vdop.ser.bireport.TableSqlJsonVO.QueryMonth;
import com.ruisi.vdop.ser.olap.TableJsonService;
import com.ruisi.vdop.ser.portal.PortalPageService;
import com.ruisi.vdop.service.frame.DataControlInterface;
import com.ruisi.vdop.util.VDOPUtils;
import com.ruisi.vdop.util.VdopConstant;

public class TableService {
	
	public final static String deftMvId = "mv.test.tmp";
		
	private TableJsonService jsonService = new TableJsonService();
	
	private Map<String, InputField> mvParams = new HashMap(); //mv的参数
	
	/***
	 * 当指标有计算指标时，需要计算上期、同期等值，在显示数据时需要对偏移的数据进行过滤，
	 */
	private List<GridFilterContext> filters = new ArrayList<GridFilterContext>();
	
	private DataControlInterface dataControl;
	
	private JSONObject dset;  //数据集
	private JSONObject dsource;  //数据源
	
	public TableService(){
		String clazz = ExtContext.getInstance().getConstant("dataControl");
		if(clazz != null && clazz.length() != 0){
			try {
				dataControl = (DataControlInterface)Class.forName(clazz).newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
	}
	
	/**
	 * 生成表格SQL
	 * @param sqlVO
	 * @param tinfo
	 * @param params
	 * @param release  判断当前是否为发布状态, 0 表示不是发布，1表示发布到多维分析，2表示发布到仪表盘
	 * @return
	 * @throws ParseException
	 */
	public String createSql(TableSqlJsonVO sqlVO, JSONArray params, int release) throws ParseException{
		Map<String, String> tableAlias = PortalPageService.createTableAlias(dset);
		//判断是否需要计算上期、同期值
		int jstype = sqlVO.getKpiComputeType();
		StringBuffer sql = new StringBuffer();
		sql.append("select ");
		List<DimInfo> dims = sqlVO.getDims();
		for(int i=0; i<dims.size(); i++){
			DimInfo dim = dims.get(i);
			String key = dim.getTableColKey();
			String txt = dim.getTableColName();
			String tname = dim.getTableName();
			if(key != null && txt != null && key.length() >0 && txt.length() >0){
				sql.append( tableAlias.get(tname) + "."+key+", " + tableAlias.get(tname) + "." + txt + ",");
			}else{
				if(dim.getCalc() == 0){
					sql.append( tableAlias.get(dim.getTname()) + "." + dim.getColName()+" "+dim.getAlias()+", ");
				}else{
					sql.append( dim.getColName()+" "+dim.getAlias()+", ");
				}
			}
		}
		
		List<KpiInfo> kpis = sqlVO.getKpis();
		if(kpis.size() == 0){
			sql.append(" null kpi_value ");
		}else{
			for(int i=0; i<kpis.size(); i++){
				KpiInfo kpi = kpis.get(i);
				//if(kpi.getRate() == null){
					sql.append(kpi.getColName() + " ");
				//}else{
				//	sql.append("(" + kpi.getColName() + ")/"+kpi.getRate()+" ");
				//}
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
		
		//处理数据权限
		if(dataControl != null){
			sql.append(dataControl.process(VDOPUtils.getLoginedUser(), dset.getString("master")));
		}
		
		for(int i=0; i<dims.size(); i++){
			DimInfo dim = dims.get(i);
			if(dim.getType().equals("frd") || "year".equals(dim.getType()) || "quarter".equals(dim.getType())){
				
				//限制维度筛选
				if(dim.getVals() != null && dim.getVals().length() > 0){
					String vls = null;
					if(("year".equals(dim.getType()) || "quarter".equals(dim.getType())) && jstype != 0){  //有计算指标，需要从写时间值
						vls = resetVals(dim.getVals(), dim.getType(), dim.getDateFormat(), jstype);
						GridFilterContext filter = new GridFilterContext();
						filter.setColumn(dim.getAlias());
						filter.setFilterType(GridFilter.in);
						filter.setValue(dim.getVals());
						this.filters.add(filter);
					}else{
						vls = dim.getVals();
					}
					//处理字符串
					if("string".equalsIgnoreCase(dim.getValType())){
						vls = VDOPUtils.dealStringParam(vls);
					}
					sql.append(" and " + (dim.getCalc() == 1 ? dim.getColName() : tableAlias.get(dim.getTname()) + "." + dim.getColName()) + " in ("+vls+")");
				}
			}
			
			//处理日期限制
			if(dim.getType().equals("day")){
				if(dim.getDay() != null){
					String start = dim.getDay().getStartDay();
					String end = dim.getDay().getEndDay();
					if(jstype != 0){ //有计算指标，从写数据区间
						String[] q = resetBetween(start, end, dim.getType(), dim.getDateFormat(), jstype);
						start = q[0];
						end = q[1];
						GridFilterContext filter = new GridFilterContext();
						filter.setColumn(dim.getAlias());
						filter.setFilterType(GridFilter.between);
						filter.setValue(dim.getDay().getStartDay());
						filter.setValue2(dim.getDay().getEndDay());
						filter.setDateFormat(dim.getDateFormat());
						this.filters.add(filter);
					}
					sql.append(" and " + dim.getColName() + " between '"+start+"' and '" + end + "'");
				}else
				if(dim.getVals() != null && dim.getVals().length() > 0){
					String ret = dim.getVals();
					if(jstype != 0){
						ret = resetVals(ret, dim.getType(), dim.getDateFormat(), jstype);
						GridFilterContext filter = new GridFilterContext();
						filter.setColumn(dim.getAlias());
						filter.setFilterType(GridFilter.in);
						filter.setValue(dim.getVals());
						this.filters.add(filter);
					}
					ret = VDOPUtils.dealStringParam(ret);
					sql.append(" and " + dim.getColName() + " in ("+ret+")");
				}
			}
			//处理月份
			if(dim.getType().equals("month")){
				if(dim.getMonth() != null){
					//如果有计算指标，需要重写数据区间
					String start = dim.getMonth().getStartMonth();
					String end = dim.getMonth().getEndMonth();
					if(jstype != 0){
						String[] q = resetBetween(start, end, dim.getType(), dim.getDateFormat(), jstype);
						start = q[0];
						end = q[1];
						GridFilterContext filter = new GridFilterContext();
						filter.setColumn(dim.getAlias());
						filter.setFilterType(GridFilter.between);
						filter.setValue(dim.getMonth().getStartMonth());
						filter.setValue2(dim.getMonth().getEndMonth());
						filter.setDateFormat(dim.getDateFormat());
						this.filters.add(filter);
					}
					sql.append(" and " + dim.getColName() + " between '"+start+"' and '" + end + "'");
				}else
				if(dim.getVals() != null && dim.getVals().length() > 0){
					//如果有计算指标，需要重写数据值列表
					String ret = dim.getVals();
					if(jstype != 0){
						ret = resetVals(ret, dim.getType(), dim.getDateFormat(), jstype);
						GridFilterContext filter = new GridFilterContext();
						filter.setColumn(dim.getAlias());
						filter.setFilterType(GridFilter.in);
						filter.setValue(dim.getVals());
						this.filters.add(filter);
					}
					ret = VDOPUtils.dealStringParam(ret);
					sql.append(" and " + dim.getColName() + " in ("+ret+")");
				}
			}
		}
		
		//限制参数的查询条件
		for(int i=0; params != null && i<params.size(); i++){
			JSONObject param = params.getJSONObject(i);
			String tp = param.getString("type");
			String colname = param.getString("colname");
			String alias = param.getString("alias");
			String valType = param.getString("valType");
			String dateformat = (String)param.get("dateformat");
			//只有参数和组件都来源于同一个表，才能进行参数拼装
			if((tp.equals("frd") || tp.equals("year") || tp.equals("quarter"))){
				if(release == 0 && param.get("vals") != null && ((String)param.get("vals")).length() > 0){
					//字符串特殊处理
					String  vls = param.getString("vals");
					if(jstype != 0 && ("year".equals(tp) || "quarter".equals(tp))){
						vls = resetVals(vls, tp, param.getString("dateformat"), jstype);
					}
					if("string".equalsIgnoreCase(valType)){
						vls = VDOPUtils.dealStringParam(vls);
					}
					sql.append(" and " + colname + " in ("+vls+")");
				}else if(release == 1 || release == 2){
					sql.append(" #if($"+alias+" != '') and " + colname + " in ($extUtils.printVals($myUtils.resetVals($"+alias+",'"+tp+"','"+dateformat+"', "+jstype+"), '"+valType+"')) #end");
				}
				//生成filter
				if(jstype != 0 && ("year".equals(tp) || "quarter".equals(tp))){
					GridFilterContext filter = new GridFilterContext();
					filter.setColumn(param.getString("alias"));
					filter.setFilterType(GridFilter.in);
					if(release == 0 && param.get("vals") != null && ((String)param.get("vals")).length() > 0){
						filter.setValue(param.getString("vals"));
					}else if(release == 1 || release == 2){
						filter.setValue("${"+colname+"}");
					}
					this.filters.add(filter);
				}
				
			}else if((tp.equals("day") || tp.equals("month"))){
				if(release == 0 && param.get("st") != null && param.getString("st").length() > 0 ){
					String ostart = param.getString("st");
					String oend = param.getString("end");
					String start = ostart;
					String end = oend;
					if(jstype != 0){
						String[] q = resetBetween(start, end, tp, param.getString("dateformat"), jstype);
						start = q[0];
						end = q[1];
					}
					sql.append(" and " + colname + " between '"+ start  + "' and '" + end + "'");
				}else if(release == 1){
					sql.append(" #if($s_"+alias+" != '' && $e_"+alias+" != '') and " + colname + " between $myUtils.resetBetween($s_"+alias+", $e_"+alias+", '"+tp+"', '"+dateformat+"', "+jstype+") #end");
				}else if(release == 2){
					sql.append(" #if($"+alias+" != '') and "+colname+" = $"+alias+" #end");
				}
				//生成filter
				if(jstype != 0){
					GridFilterContext filter = new GridFilterContext();
					filter.setColumn(param.getString("alias"));
					filter.setFilterType(GridFilter.between);
					filter.setDateFormat((String)param.get("dateformat"));
					if(release == 0 && param.get("st") != null && param.getString("st").length() > 0 ){
						String ostart = param.getString("st");
						String oend = param.getString("end");
						filter.setValue(ostart);
						filter.setValue2(oend);
					}else if(release == 1){
						filter.setValue("${s_"+alias+"}");
						filter.setValue2("${e_"+alias+"}");
					}else if(release == 2){
						filter.setValue("${"+alias+"}");
					}
					this.filters.add(filter);
				}
			}
		}
		
		//处理事件接受的参数限制条件
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
					sql.append(tableAlias.get(tname) + "."+key+", "+ tableAlias.get(tname) + "." + txt);
				}else{
					if(dim.getCalc() == 1){
						sql.append(dim.getColName());
					}else{
						sql.append(tableAlias.get(dim.getTname()) + "."+dim.getColName());
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
			//先按col排序
			for(int i=0; i<dims.size(); i++){
				DimInfo dim = dims.get(i);
				if(!dim.getDimpos().equals("col")){
					continue;
				}
				if(dim.getDimOrd() != null && dim.getDimOrd().length() > 0){
					if(dim.getOrdcol() != null && dim.getOrdcol().length() > 0){  //处理维度排序
						order.append(dim.getOrdcol() + " " + dim.getDimOrd() + ",");
					}else{
						order.append(dim.getColName() + " " + dim.getDimOrd() + ",");
					}
				}
			}
			//判断是否按指标排序
			for(int i=0; i<kpis.size(); i++){
				KpiInfo kpi = kpis.get(i);
				if(kpi.getSort() != null && kpi.getSort().length() > 0){
					order.append(kpi.getAlias() + " " + kpi.getSort());
					order.append(",");
				}
			}
			
			//再按row排序
			for(int i=0; i<dims.size(); i++){
				DimInfo dim = dims.get(i);
				if(!dim.getDimpos().equals("row")){
					continue;
				}
				if(dim.getDimOrd() != null && dim.getDimOrd().length() > 0){
					if(dim.getOrdcol() != null && dim.getOrdcol().length() > 0){  //处理维度排序
						order.append(dim.getOrdcol() + " " + dim.getDimOrd() + ",");
					}else{
						order.append(dim.getColName() + " " + dim.getDimOrd() + ",");
					}
				}
			}
			
			
			if(order.length() <= 11 ){  //判断是否拼接了 order by 字段
				return sql.toString().replaceAll("@", "'");
			}else{
				//返回前先去除最后的逗号
				return (sql + order.toString().substring(0, order.length() - 1)).replaceAll("@", "'");
			}
		}else{
			return sql.toString().replaceAll("@", "'");
		}
	}
	
	/**
	 * 创建表格datacenter
	 * @param sql
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
		
		//判断指标计算
		for(KpiInfo kpi : sqlVO.getKpis()){
			if(kpi.getCompute() != null && kpi.getCompute().length() > 0){
				if("zb".equals(kpi.getCompute())){
					GridProcContext proc = this.createAccount(sqlVO, kpi);
					ctx.getProcess().add(proc);
				}else if("sxpm".equals(kpi.getCompute()) || "jxpm".equals(kpi.getCompute())){
					GridProcContext proc = this.createSort(sqlVO, kpi);
					ctx.getProcess().add(proc);
				}else if("ydpj".equals(kpi.getCompute())){
					GridProcContext proc = this.createMoveAvg(sqlVO, kpi);
					ctx.getProcess().add(proc);
				}else{
					String[] jss = kpi.getCompute().split(",");
					for(String js : jss){
						GridProcContext proc = this.createShift(sqlVO, kpi, js);
						ctx.getProcess().add(proc);
					}
				}
			}
		}
		
		//判断是否有时间偏移的计算
		for(GridFilterContext filter : this.filters){
			ctx.getProcess().add(filter);
		}
		
		return ctx;
	}
	
	/**
	 * 创建指标排名process
	 * @param sqlVO
	 * @param kpi
	 * @return
	 */
	private GridProcContext createSort(TableSqlJsonVO sqlVO, KpiInfo kpi){
		GridSortContext proc = new GridSortContext();
		proc.setAppendOrder(true);
		proc.setChangeOldOrder(false);
		
		//创建排序的分组维
		StringBuffer sb = new StringBuffer("");
		StringBuffer orderSb = new StringBuffer("");
		for(int i=0; i<sqlVO.getDims().size(); i++){
			DimInfo dim = sqlVO.getDims().get(i);
			//设置 col 维度 为分组维
			if(dim.getDimpos().equals("col")){
				sb.append(dim.getAlias());
				sb.append(",");
				orderSb.append(dim.getDimOrd());
				orderSb.append(",");
			}
		}
		sb.append(kpi.getAlias());
		orderSb.append("sxpm".equals(kpi.getCompute())?"asc":"desc");
		proc.setColumn(sb.toString().split(","));
		proc.setType(orderSb.toString().split(","));
		return proc;
	}
	
	/**
	 * 创建时间偏移process,时间偏移用来计算同比、环比、上期、同期等值
	 * @param sqlVO
	 * @param kpi
	 * @return
	 */
	private GridProcContext createShift(TableSqlJsonVO sqlVO, KpiInfo kpi, String compute){
		//查询最小时间维度
		int minDate = 4;
		DimInfo minDim = null;
		for(DimInfo dim : sqlVO.getDims()){
			String tp = dim.getType();
			if("frd".equalsIgnoreCase(tp)){
				continue;
			}
			int curDate = ChartService.type2value(tp);
			if(curDate <= minDate){
				minDate = curDate;
				minDim = dim;
			}
		}
		GridShiftContext proc = new GridShiftContext();
		proc.setDateType(minDim.getType());
		proc.setDateFormat(minDim.getDateFormat());
		proc.setDateColumn(minDim.getAlias());
		proc.setComputeType(compute);
		proc.setKpiColumn(new String[]{kpi.getAlias()});
		//设置过滤维度
		StringBuffer sb = new StringBuffer("");
		for(DimInfo dim : sqlVO.getDims()){
			String tp = dim.getType();
			if("year".equals(tp) || "quarter".equals(tp) || "month".equals(tp) || "day".equals(tp)){
				continue;
			}
			sb.append(dim.getAlias());
			sb.append(",");
		}
		if(sb.length() > 0){
			String str = sb.substring(0, sb.length() - 1);
			proc.setKeyColumns(str.split(","));
		}
		return proc;
	}
	
	private GridProcContext createMoveAvg(TableSqlJsonVO sqlVO, KpiInfo kpi){
		ComputeMoveAvgContext ctx = new ComputeMoveAvgContext();
		ctx.setAlias(kpi.getAlias()+"_ydpj");
		ctx.setColumn(kpi.getAlias());
		ctx.setStep(3);
		return ctx;
	}
	
	/**
	 * 创建占比计算process
	 */
	private GridProcContext createAccount(TableSqlJsonVO sqlVO, KpiInfo kpi){
		GridAccountContext proc = new GridAccountContext();
		proc.setColumn(kpi.getAlias());
		//创建计算的分组维
		StringBuffer sb = new StringBuffer("");
		for(int i=0; i<sqlVO.getDims().size(); i++){
			DimInfo dim = sqlVO.getDims().get(i);
			//剔除row 最后一个维度
			if(dim.getDimpos().equals("row") && (i == sqlVO.getDims().size() - 1 || sqlVO.getDims().get(i + 1).getDimpos().equals("col")) ){
			}else{
				sb.append(dim.getColName());
				sb.append(",");
			}
		}
		if(sb.length() > 0){
			String str = sb.substring(0, sb.length() - 1);
			proc.setGroupDim(str.split(","));
		}
		return proc;
	}
	
	public MVContext json2MV(JSONObject tableJson, JSONArray kpiJson, String compId, JSONArray params) throws Exception{
		TableSqlJsonVO sqlVO = TableJsonService.json2TableSql(tableJson, kpiJson);
		
		//创建MV
		MVContext mv = new MVContextImpl();
		mv.setChildren(new ArrayList());
		String formId = ExtConstants.formIdPrefix + IdCreater.create();
		mv.setFormId(formId);
		mv.setMvid(deftMvId);
		
		//创建corssReport
		CrossReportContext cr = jsonService.json2Table(tableJson, sqlVO);
		//cr.setDataUrl("comp/table/TableDrill.action?a=1");
		//设置ID
		String id = ExtConstants.reportIdPrefix + IdCreater.create();
		cr.setId(id);
		cr.setOut("olap");
		cr.setShowData(true);
		//cr.setExportName(title);
	
		mv.getChildren().add(cr);
		cr.setParent(mv);
		
		Map<String, CrossReportContext> crs = new HashMap<String, CrossReportContext>();
		crs.put(cr.getId(), cr);
		mv.setCrossReports(crs);
		
		//创建datacenter
		String sql = this.createSql(sqlVO, params, 0);
		GridDataCenterContext dc = this.createDataCenter(sql, sqlVO);
		cr.setRefDataCetner(dc.getId());
		if(mv.getGridDataCenters() == null){
			mv.setGridDataCenters(new HashMap<String, GridDataCenterContext>());
		}
		mv.getGridDataCenters().put(dc.getId(), dc);
		
		//判断是否需要创建数据源
		String dsid = PortalPageService.createDsource(dsource, mv);
		dc.getConf().setRefDsource(dsid);
		
		String scripts = jsonService.getScripts().toString();
		if(scripts != null && scripts.length() > 0){
			mv.setScripts(scripts);
		}
		return mv;
	}
	
	//生成json接口时调用
	/**
	public MVContext json2MV(JSONObject tableJson, JSONArray kpiJson) throws Exception{
		TableSqlJsonVO sqlVO = TableJsonService.json2TableSql(tableJson, kpiJson);
		
		//创建MV
		MVContext mv = new MVContextImpl();
		mv.setChildren(new ArrayList());
		String formId = ExtConstants.formIdPrefix + IdCreater.create();
		mv.setFormId(formId);
		mv.setMvid(deftMvId);
		
		//获取所查询数据表信息
		Map tinfo = ChartService.selectTable(sqlVO, null);
		
		
		//创建corssReport
		CrossReportContext cr = jsonService.json2Table(tableJson, sqlVO);
		//cr.setDataUrl("comp/table/TableDrill.action?a=1");
		//设置ID
		String id = ExtConstants.reportIdPrefix + IdCreater.create();
		cr.setId(id);
		cr.setOut("json");
		cr.setShowData(true);
		//cr.setExportName(title);
	
		mv.getChildren().add(cr);
		cr.setParent(mv);
		
		Map<String, CrossReportContext> crs = new HashMap<String, CrossReportContext>();
		crs.put(cr.getId(), cr);
		mv.setCrossReports(crs);
		
		//创建datacenter
		String sql = this.createSql(sqlVO, tinfo, null, false);
		GridDataCenterContext dc = this.createDataCenter(sql, sqlVO);
		cr.setRefDataCetner(dc.getId());
		if(mv.getGridDataCenters() == null){
			mv.setGridDataCenters(new HashMap<String, GridDataCenterContext>());
		}
		mv.getGridDataCenters().put(dc.getId(), dc);
		if(this.cacheDims.size() > 0){
			mv.setScripts(this.cache2Script(this.cacheDims));
		}
	
		return mv;
	}
	**/
	
	/**
	 * 根据指标计算的值筛选，从新设置时间字段的值列表
	 */
	public static String resetVals(String inputval, String type, String dateFormat, int jstype){
		if(jstype == 0){
			return inputval;
		}
		String[] vals = inputval.split(",");
		List<String> rets = new ArrayList<String>();
		for(String val : vals){
			//先添加他自己
			if(!rets.contains(val)){
				rets.add(val);
			}
			if(jstype == 1 || jstype == 3){ //上期
				String nval = GridShift.getDateShiftValue(val, type, dateFormat, "sq");
				if(!rets.contains(nval)){
					rets.add(nval);
				}
			}
			if(jstype == 2 || jstype == 3){ //同期
				String nval = GridShift.getDateShiftValue(val, type, dateFormat, "tq");
				if(!rets.contains(nval)){
					rets.add(nval);
				}
			}
		}
		return list2String(rets);
	}
	
	/**
	 * 根据指标计算的值筛选，从新设置时间字段的数据区间，主要针对日、月份的数据区间控制
	 */
	public static String[] resetBetween(String start, String end, String type, String dateFormat, int jstype){
		if(jstype == 0){ //无计算
			return new String[]{start, end};
		}
		if("day".equals(type)){
			if(jstype == 1 || jstype == 3){ //上期
				String nval = GridShift.getDateShiftValue(start, type, dateFormat, "sq");
				start = nval;
			}
			if(jstype == 2 || jstype == 3){ //同期
				String nval2 = GridShift.getDateShiftValue(start, type, dateFormat, "tq");
				start = nval2;
			}
			return new String[]{start, end};
		}else if("month".equals(type)){
			if(jstype == 1 || jstype == 3){ //上期
				String nval = GridShift.getDateShiftValue(start, type, dateFormat, "sq");
				start = nval;
			}
			if(jstype == 2 || jstype == 3){ //同期
				String nval = GridShift.getDateShiftValue(start, type, dateFormat, "tq");
				start = nval;
			}
			return new String[]{start, end};
		}else{
			return null;
		}
	}
	
	private static String list2String(List<String> rets){
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<rets.size(); i++){
			String ret = rets.get(i);
			sb.append(ret);
			if(i != rets.size() - 1){
				sb.append(",");
			}
		}
		return sb.toString();
	}

	public Map<String, InputField> getMvParams() {
		return mvParams;
	}

	public void setMvParams(Map<String, InputField> mvParams) {
		this.mvParams = mvParams;
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
