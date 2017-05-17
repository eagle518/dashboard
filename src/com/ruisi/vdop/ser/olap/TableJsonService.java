package com.ruisi.vdop.ser.olap;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.ruisi.ext.engine.util.IdCreater;
import com.ruisi.ext.engine.view.context.cross.CrossCols;
import com.ruisi.ext.engine.view.context.cross.CrossField;
import com.ruisi.ext.engine.view.context.cross.CrossReportContext;
import com.ruisi.ext.engine.view.context.cross.CrossReportContextImpl;
import com.ruisi.ext.engine.view.context.cross.CrossRows;
import com.ruisi.ext.engine.view.context.cross.RowDimContext;
import com.ruisi.ext.engine.view.context.cross.RowLinkContext;
import com.ruisi.vdop.ser.bireport.MyCrossFieldLoader;
import com.ruisi.vdop.ser.bireport.TableSqlJsonVO;
import com.ruisi.vdop.ser.bireport.TableSqlJsonVO.DimInfo;
import com.ruisi.vdop.ser.bireport.TableSqlJsonVO.KpiFilter;
import com.ruisi.vdop.ser.bireport.TableSqlJsonVO.KpiInfo;

/**
 * 转化crossTable2json
 * @author hq
 * @date Aug 13, 2010
 */
public class TableJsonService {
	
	public final static String deftMvId = "mv.test.tmp";
	
	private StringBuffer scripts; //用来构造js 脚本的字符串对象
	
	public TableJsonService(){
		scripts = new StringBuffer();
	}
	
	public CrossReportContext json2Table(JSONObject tableJson, TableSqlJsonVO sqlVO) throws ParseException{
		return json2Table(tableJson, sqlVO, null);
	}
	
	public CrossReportContext json2Table(JSONObject tableJson, TableSqlJsonVO sqlVO, String compId) throws ParseException{
		CrossReportContext ctx = new CrossReportContextImpl();
		
		CrossCols cols = new CrossCols();
		cols.setCols(new ArrayList<CrossField>());
		ctx.setCrossCols(cols);
		
		CrossRows rows = new CrossRows();
		rows.setRows(new ArrayList<CrossField>());
		ctx.setCrossRows(rows);
		
		//判断是否有事件
		//JSONObject linkAccept = sqlVO.getLinkAccept();
		//if(linkAccept != null && !linkAccept.isNullObject() && !linkAccept.isEmpty()){
		//	ctx.setLabel(compId);
		//}
		ctx.setLabel(compId);  //给组件设置label
		boolean uselink = false;
		JSONObject link = sqlVO.getLink();
		if(link != null && !link.isNullObject() && !link.isEmpty()){
			RowLinkContext rlink = new RowLinkContext();
			String url = (String)link.get("url");  //url 优先与联动组件
			if(url != null && url.length() >0){
				rlink.setUrl(url);
			}else{
				rlink.setTarget(new String[]{(String)link.get("target")});
				rlink.setType(new String[]{(String)link.get("type")});
			}
			ctx.getCrossRows().setLink(rlink);
			uselink = true;
		}
		
		//表格钻取维度
		List<RowDimContext> drill = this.getDrillDim(tableJson);
		if(drill != null && drill.size() > 0){
			ctx.setDims(drill);
			uselink = true;
		}
		
		JSONArray colsStr = tableJson.getJSONArray("cols");
		loopJsonField(colsStr, cols.getCols(), sqlVO, "col", uselink);
		
		JSONArray rowsStr = tableJson.getJSONArray("rows");
		loopJsonField(rowsStr, rows.getRows(), sqlVO, "row", uselink);
		
		//如果没有维度，添加none维度
		if(cols.getCols().size() == 0){
			CrossField cf = new CrossField();
			cf.setType("none");
			cf.setDesc("合计");
			cols.getCols().add(cf);
		}
		if(rows.getRows().size() == 0){
			CrossField cf = new CrossField();
			cf.setType("none");
			cf.setDesc("合计");
			rows.getRows().add(cf);
		}
		
		return ctx;
	}
	
	public List<RowDimContext> getDrillDim(JSONObject json){
		JSONArray drillDim = (JSONArray)json.get("drillDim");
		if(drillDim == null || drillDim.isEmpty()){
			return null;
		}
		List<RowDimContext> ret = new ArrayList<RowDimContext>();
		for(int i=0; i<drillDim.size(); i++){
			JSONObject obj = drillDim.getJSONObject(i);
			RowDimContext dim = new RowDimContext();
			dim.setCode(obj.getString("code"));
			dim.setName(obj.getString("name"));
			dim.setCodeDesc(dim.getCode()+"_desc");
			dim.setType("frd");
			ret.add(dim);
		}
		return ret;
	}
	
	public static TableSqlJsonVO json2TableSql(JSONObject tableJson, JSONArray kpiJson){
		TableSqlJsonVO vo = new TableSqlJsonVO();
		JSONObject baseDate = (JSONObject)tableJson.get("baseDate");
		if(baseDate != null){
			TableSqlJsonVO.BaseDate bd = new TableSqlJsonVO.BaseDate();
			bd.setStart((String)baseDate.get("start"));
			bd.setEnd((String)baseDate.get("end"));
			vo.setBaseDate(bd);
		}
		
		for(int i=0; kpiJson!=null&&i<kpiJson.size(); i++){
			JSONObject kpij = kpiJson.getJSONObject(i);
			KpiInfo kpi = new KpiInfo();
			kpi.setAggre(kpij.getString("aggre"));
			kpi.setAlias(kpij.getString("alias"));
			kpi.setColName(kpij.getString("col_name"));
			kpi.setFmt(kpij.getString("fmt"));
			kpi.setKpiName(kpij.getString("kpi_name"));
			kpi.setTname((String)kpij.get("tname"));
			kpi.setDescKey((String)kpij.get("descKey"));
			kpi.setOrder((String)kpij.get("order"));
			if(kpij.get("rate") != null && kpij.get("rate").toString().length() > 0 && !kpij.get("rate").toString().equals("null")){
				kpi.setRate(kpij.getInt("rate"));
			}
			kpi.setId(kpij.get("kpi_id") == null ? null : kpij.get("kpi_id").toString());
			kpi.setSort((String)kpij.get("sort"));
			kpi.setUnit((String)kpij.get("unit"));
			kpi.setCompute((String)kpij.get("compute"));
			kpi.setFuncname((String)kpij.get("funcname"));
			kpi.setCode((String)kpij.get("code"));
			JSONObject warn = (JSONObject)kpij.get("warning");
			kpi.setWarn(warn);
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
		
		JSONArray colDims = tableJson.getJSONArray("cols");
		JSONArray rowDims = tableJson.getJSONArray("rows");
		
		//colDims.addAll(rowDims);
		
		for(int i=0; i<rowDims.size(); i++){
			JSONObject obj = rowDims.getJSONObject(i);
			DimInfo dim = new DimInfo();
			dim.setType(obj.getString("type"));
			if("kpiOther".equalsIgnoreCase(dim.getType())){
				continue;
			}
			dim.setId(obj.getString("id"));
			dim.setColName((String)obj.get("colname"));
			dim.setAlias((String)obj.get("alias"));
			dim.setTableName((String)obj.get("tableName"));
			dim.setTableColKey((String)obj.get("tableColKey"));
			dim.setTableColName((String)obj.get("tableColName"));
			dim.setVals(obj.get("vals") == null ? null : obj.get("vals").toString());
			dim.setIssum((String)obj.get("issum"));
			dim.setTname((String)obj.get("tname"));
			dim.setDimOrd((String)obj.get("dimord"));
			dim.setValType((String)(obj.get("valType")));
			dim.setOrdcol((String)(obj.get("ordcol")));
			dim.setCalc(obj.getInt("calc"));
			dim.setDimpos("row");
			dim.setDateFormat((String)obj.get("dateformat"));
			
			//日期、月份特殊处理
			if("day".equals(dim.getType())){
				if(obj.get("startdt") != null && !"".equals(obj.get("startdt"))){
					TableSqlJsonVO.QueryDay d = new TableSqlJsonVO.QueryDay();
					d.setStartDay((String)obj.get("startdt"));
					d.setEndDay((String)obj.get("enddt"));
					dim.setDay(d);
				}
			}
			
			if("month".equals(dim.getType())){
				if(obj.get("startmt") != null && !"".equals(obj.get("startmt"))){
					TableSqlJsonVO.QueryMonth m = new TableSqlJsonVO.QueryMonth();
					m.setStartMonth((String)obj.get("startmt"));
					m.setEndMonth((String)obj.get("endmt"));
					dim.setMonth(m);
				}
			}
			
			vo.getDims().add(dim);
		}
		
		for(int i=0; i<colDims.size(); i++){
			JSONObject obj = colDims.getJSONObject(i);
			DimInfo dim = new DimInfo();
			dim.setType(obj.getString("type"));
			if("kpiOther".equalsIgnoreCase(dim.getType())){
				continue;
			}
			dim.setId(obj.getString("id"));
			dim.setColName((String)obj.get("colname"));
			dim.setAlias((String)obj.get("alias"));
			dim.setTableName((String)obj.get("tableName"));
			dim.setTableColKey((String)obj.get("tableColKey"));
			dim.setTableColName((String)obj.get("tableColName"));
			dim.setVals(obj.get("vals") == null ? null : obj.get("vals").toString());
			dim.setIssum((String)obj.get("issum"));
			dim.setTname((String)obj.get("tname"));
			dim.setDimOrd((String)obj.get("dimord"));
			dim.setValType((String)obj.get("valType"));
			dim.setOrdcol((String)(obj.get("ordcol")));
			dim.setCalc(obj.getInt("calc"));
			dim.setDimpos("col");
			dim.setDateFormat((String)obj.get("dateformat"));
			
			//日期、月份特殊处理
			if("day".equals(dim.getType())){
				if(obj.get("startdt") != null && !"".equals(obj.get("startdt"))){
					TableSqlJsonVO.QueryDay d = new TableSqlJsonVO.QueryDay();
					d.setStartDay((String)obj.get("startdt"));
					d.setEndDay((String)obj.get("enddt"));
					dim.setDay(d);
				}
			}
			
			if("month".equals(dim.getType())){
				if(obj.get("startmt") != null && !"".equals(obj.get("startmt"))){
					TableSqlJsonVO.QueryMonth m = new TableSqlJsonVO.QueryMonth();
					m.setStartMonth((String)obj.get("startmt"));
					m.setEndMonth((String)obj.get("endmt"));
					dim.setMonth(m);
				}
			}
			
			vo.getDims().add(dim);
		}
		vo.setLink((JSONObject)tableJson.get("link"));
		vo.setLinkAccept((JSONObject)tableJson.get("linkAccept"));
		vo.setDrillDim((JSONArray)tableJson.get("drillDim"));
		return vo;
	}

	private void loopJsonField(JSONArray arrays, List<CrossField> ls, TableSqlJsonVO sqlVO, String pos, boolean uselink) throws ParseException{
		List<CrossField> tmp = ls;
		for(int i=0; i<arrays.size(); i++){
			JSONObject obj = arrays.getJSONObject(i);
			String type = obj.getString("type");
			String issum = (String)obj.get("issum");
			//String casparent = obj.get("iscas") == null ? "" : obj.get("iscas").toString();
			
			if(type.equals("kpiOther")){
				
				List<CrossField> newCf = new ArrayList<CrossField>();
				if(tmp.size() == 0){
					List<KpiInfo> kpis = sqlVO.getKpis();
					for(KpiInfo kpi : kpis){
						CrossField cf = new CrossField();
						cf.setType(type);
						cf.setAggregation(kpi.getAggre());
						cf.setAlias(kpi.getAlias());
						cf.setFormatPattern(kpi.getFmt());
						cf.setOrder("y".equals(kpi.getOrder()));
						cf.setSubs(new ArrayList<CrossField>());
						//用 id来表示指标ID，用在OLAP中,对指标进行操作
						cf.setId(kpi.getId().toString());
						if(kpi.getRate() != null){
							cf.setKpiRate(new BigDecimal(kpi.getRate()));
						}
						String ru = this.writerUnit(cf.getKpiRate()) +kpi.getUnit();
						if(ru != null && ru.length() > 0){
							cf.setDesc(kpi.getKpiName() + "("  + ru + ")");  //指标名称+ 单位
						}else{
							cf.setDesc(kpi.getKpiName());  //指标名称
						}
						
						//当回调函数和指标预警同时起作用时， 指标预警起作用
						//处理回调函数
						cf.setJsFunc(kpi.getFuncname());
						String code = kpi.getCode();
						if(code != null && code.length() > 0){
							try {
								code = URLDecoder.decode(code, "UTF-8");
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
							this.scripts.append("function "+cf.getJsFunc()+"(value,col,row,data){"+code+"}");
						}
						
						//处理指标预警
						JSONObject warn = kpi.getWarn();
						if(warn != null && !warn.isEmpty()){
							String name = createWarning(warn, kpi.getFmt(), scripts);
							cf.setJsFunc(name);
						}
						tmp.add(cf);
						newCf.add(cf);
						
						//判断指标是否需要进行计算
						if(kpi.getCompute() != null && kpi.getCompute().length() > 0){
							String[] jss = kpi.getCompute().split(",");  //可能有多个计算，用逗号分隔
							for(String js : jss){
								CrossField compute = this.kpiCompute(js, kpi);
								tmp.add(compute);
								newCf.add(compute);
							}
						}
					}
				}else{
					for(CrossField tp : tmp){
						List<KpiInfo> kpis = sqlVO.getKpis();
						for(KpiInfo kpi : kpis){
							CrossField cf = new CrossField();
							cf.setType(type);
							cf.setAggregation(kpi.getAggre());
							cf.setAlias(kpi.getAlias());
							cf.setFormatPattern(kpi.getFmt());
							cf.setOrder("y".equals(kpi.getOrder()));
							cf.setSubs(new ArrayList<CrossField>());
							//用 size来表示指标ID，用在OLAP中
							cf.setId(kpi.getId().toString());
							if(kpi.getRate() != null){
								cf.setKpiRate(new BigDecimal(kpi.getRate()));
							}
							String ru = this.writerUnit(cf.getKpiRate()) +kpi.getUnit();
							if(ru != null && ru.length() > 0){
								cf.setDesc(kpi.getKpiName() + "("  + ru + ")");  //指标名称+ 单位
							}else{
								cf.setDesc(kpi.getKpiName());  //指标名称
							}
							//当回调函数和指标预警同时起作用时， 指标预警起作用
							//处理回调函数
							cf.setJsFunc(kpi.getFuncname());
							String code = kpi.getCode();
							if(code != null && code.length() > 0){
								try {
									code = URLDecoder.decode(code, "UTF-8");
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
								this.scripts.append("function "+cf.getJsFunc()+"(value,col,row,data){"+code+"}");
							}
							//处理指标预警
							JSONObject warn = kpi.getWarn();
							if(warn != null && !warn.isEmpty()){
								String name = createWarning(warn, kpi.getFmt(), scripts);
								cf.setJsFunc(name);
							}
							tp.getSubs().add(cf);
							newCf.add(cf);
							
							//判断指标是否需要进行计算
							if(kpi.getCompute() != null && kpi.getCompute().length() > 0){
								String[] jss = kpi.getCompute().split(",");  //可能有多个计算，用逗号分隔
								for(String js : jss){
									CrossField compute = this.kpiCompute(js, kpi);
									tp.getSubs().add(compute);
									newCf.add(compute);
								}
							}
						}
					}
				}
				tmp = newCf;
				
				
			}else if("day".equals(type)){
				List<CrossField> newCf = new ArrayList<CrossField>();
				
				if(tmp.size() == 0){
					CrossField cf = new CrossField();
					/**
					cf.setType(type);
					if(sqlVO.getDayColumn() != null && sqlVO.getDayColumn().getStartDay() != null && sqlVO.getDayColumn().getStartDay().length() > 0){
						cf.setStart(sqlVO.getDayColumn().getEndDay());
						cf.setSize(sqlVO.getDayColumn().getBetweenDay() + 1);
					}else{
						int size = 365;
						cf.setStart("${s.defDay}");
						cf.setSize(size);
					}
					**/
					cf.setCasParent(true);
					
					String top = (String)obj.get("top");
					if(top != null && top.length() > 0){
						cf.setTop(new Integer(top));
					}
					String topType = (String)obj.get("topType");
					if(topType != null && topType.length() > 0){
						cf.setTopType(topType);
					}
					cf.setId(obj.get("id").toString());
					cf.setType("frd");
					cf.setDateType("day");
					cf.setDateTypeFmt((String)obj.get("dateformat"));
					cf.setUselink(uselink);
					cf.setValue(obj.get("vals") == null ? null : obj.get("vals").toString());
					cf.setMulti(true);
					cf.setShowWeek(false);
					cf.setDesc(obj.getString("dimdesc"));
					String alias = obj.getString("alias");
					cf.setAlias(alias);
					//cf.setAggreDim("true".equalsIgnoreCase((String)obj.get("issum")));
					cf.setAliasDesc(alias);
					cf.setSubs(new ArrayList<CrossField>());
					tmp.add(cf);
					newCf.add(cf);
					
					//添加合计项
					if("y".equals(issum)){
						CrossField sumcf = new CrossField();
						sumcf.setType("none");
						String aggre = (String)obj.get("aggre");
						if(aggre != null && aggre.length() > 0 && !"auto".equals(aggre)){
							sumcf.setDimAggre(aggre);
						}
						sumcf.setDesc(MyCrossFieldLoader.loadFieldName(sumcf.getDimAggre()));
						sumcf.setSubs(new ArrayList<CrossField>());		
						tmp.add(sumcf);
						newCf.add(sumcf);
					}
					
				}else{
					for(CrossField tp : tmp){
						//如果上级是合计，下级不包含维度了
						if(tp.getType().equals("none")){
							continue;
						}
						CrossField cf = new CrossField();
						/**
						cf.setType(type);
						if(sqlVO.getDayColumn() != null && sqlVO.getDayColumn().getStartDay() != null && sqlVO.getDayColumn().getStartDay().length() > 0){
							cf.setStart(sqlVO.getDayColumn().getEndDay());
							cf.setSize(sqlVO.getDayColumn().getBetweenDay() + 1);
						}else{
							int size = 365;
							cf.setStart("${s.defDay}");
							cf.setSize(size);
						}**/
						cf.setCasParent(true);
						
						String top = (String)obj.get("top");
						if(top != null && top.length() > 0){
							cf.setTop(new Integer(top));
						}
						String topType = (String)obj.get("topType");
						if(topType != null && topType.length() > 0){
							cf.setTopType(topType);
						}
						cf.setId(obj.get("id").toString());
						cf.setType("frd");
						cf.setDateType("day");
						cf.setDateTypeFmt((String)obj.get("dateformat"));
						cf.setUselink(uselink);
						cf.setValue(obj.get("vals") == null ? null : obj.get("vals").toString());
						cf.setMulti(true);
						cf.setShowWeek(false);
						cf.setDesc(obj.getString("dimdesc"));
						String alias = obj.getString("alias");
						cf.setAlias(alias);
						cf.setAliasDesc(alias);
						cf.setSubs(new ArrayList<CrossField>());
						cf.setParent(tp);
						
						tp.getSubs().add(cf);
						newCf.add(cf);
						
						//添加合计项
						if("y".equals(issum)){
							CrossField sumcf = new CrossField();
							sumcf.setType("none");
							String aggre = (String)obj.get("aggre");
							if(aggre != null && aggre.length() > 0 && !"auto".equals(aggre)){
								sumcf.setDimAggre(aggre);
							}
							sumcf.setDesc(MyCrossFieldLoader.loadFieldName(sumcf.getDimAggre()));
							sumcf.setSubs(new ArrayList<CrossField>());		
							tp.getSubs().add(sumcf);
							newCf.add(sumcf);
						}
					}
				}
				tmp = newCf;
				
			}else if("month".equals(type)){
				List<CrossField> newCf = new ArrayList<CrossField>();
				if(tmp.size() == 0){
					CrossField cf = new CrossField();
					/**
					cf.setType(type);
					if(sqlVO.getMonthColumn()!= null && sqlVO.getMonthColumn().getStartMonth() != null && sqlVO.getMonthColumn().getStartMonth().length() > 0){
						cf.setStart(sqlVO.getMonthColumn().getEndMonth());
						cf.setSize(sqlVO.getMonthColumn().getBetweenMonth() + 1);
					}else{
						int size = 12;
						cf.setStart("${s.defMonth}");
						cf.setSize(size);
					}
					**/
					cf.setCasParent(true);
					
					String top = (String)obj.get("top");
					if(top != null && top.length() > 0){
						cf.setTop(new Integer(top));
					}
					String topType = (String)obj.get("topType");
					if(topType != null && topType.length() > 0){
						cf.setTopType(topType);
					}
					cf.setId(obj.get("id").toString());
					cf.setType("frd");
					cf.setDateType("month");
					cf.setDateTypeFmt((String)obj.get("dateformat"));
					cf.setUselink(uselink);
					cf.setValue(obj.get("vals") == null ? null : obj.get("vals").toString());
					cf.setMulti(true);
					cf.setDesc(obj.getString("dimdesc"));
					String alias = obj.getString("alias");
					cf.setAlias(alias);
					cf.setAliasDesc(alias);
					cf.setSubs(new ArrayList<CrossField>());
					tmp.add(cf);
					newCf.add(cf);
					
					//添加合计项
					if("y".equals(issum)){
						CrossField sumcf = new CrossField();
						sumcf.setType("none");
						String aggre = (String)obj.get("aggre");
						if(aggre != null && aggre.length() > 0 && !"auto".equals(aggre)){
							sumcf.setDimAggre(aggre);
						}
						sumcf.setDesc(MyCrossFieldLoader.loadFieldName(sumcf.getDimAggre()));
						sumcf.setSubs(new ArrayList<CrossField>());		
						tmp.add(sumcf);
						newCf.add(sumcf);
					}
					
				}else{
					for(CrossField tp : tmp){
						//如果上级是合计，下级不包含维度了
						if(tp.getType().equals("none")){
							continue;
						}
						CrossField cf = new CrossField();
						/**
						cf.setType(type);
						if(sqlVO.getMonthColumn()!= null && sqlVO.getMonthColumn().getStartMonth() != null && sqlVO.getMonthColumn().getStartMonth().length() > 0){
							cf.setStart(sqlVO.getMonthColumn().getEndMonth());
							cf.setSize(sqlVO.getMonthColumn().getBetweenMonth() + 1);
						}else{
							int size = 12;
							cf.setStart("${s.defMonth}");
							cf.setSize(size);
						}
						**/
						cf.setCasParent(true);
						
						String top = (String)obj.get("top");
						if(top != null && top.length() > 0){
							cf.setTop(new Integer(top));
						}
						String topType = (String)obj.get("topType");
						if(topType != null && topType.length() > 0){
							cf.setTopType(topType);
						}
						cf.setId(obj.get("id").toString());
						cf.setType("frd");
						cf.setDateType("month");
						cf.setDateTypeFmt((String)obj.get("dateformat"));
						cf.setUselink(uselink);
						cf.setValue(obj.get("vals") == null ? null : obj.get("vals").toString());
						cf.setMulti(true);
						cf.setDesc(obj.getString("dimdesc"));
						String alias = obj.getString("alias");
						cf.setAlias(alias);
						cf.setAliasDesc(alias);
						cf.setSubs(new ArrayList<CrossField>());
						cf.setParent(tp);
						
						tp.getSubs().add(cf);
						newCf.add(cf);
						
						//添加合计项
						if("y".equals(issum)){
							CrossField sumcf = new CrossField();
							sumcf.setType("none");
							String aggre = (String)obj.get("aggre");
							if(aggre != null && aggre.length() > 0 && !"auto".equals(aggre)){
								sumcf.setDimAggre(aggre);
							}
							sumcf.setDesc(MyCrossFieldLoader.loadFieldName(sumcf.getDimAggre()));
							sumcf.setSubs(new ArrayList<CrossField>());		
							tp.getSubs().add(sumcf);
							newCf.add(sumcf);
						}
					}
				}
				tmp = newCf;
			}else{
				List<CrossField> newCf = new ArrayList<CrossField>();
				if(tmp.size() == 0){
					CrossField cf = new CrossField();
					cf.setType("frd"); //统一为frd
					cf.setId(obj.get("id").toString());
					cf.setDesc(obj.getString("dimdesc"));
					String alias = obj.getString("alias");
					String tableColKey = (String)obj.get("tableColKey");
					String tableColName = (String)obj.get("tableColName");
					if(tableColKey == null || tableColKey.length() == 0 || tableColName == null || tableColName.length() == 0){
						cf.setAlias(alias);
						cf.setAliasDesc(alias);
					}else{
						cf.setAlias(tableColKey);
						cf.setAliasDesc(tableColName);
					}
					cf.setCasParent(true);
					
					String top = (String)obj.get("top");
					if(top != null && top.length() > 0){
						cf.setTop(new Integer(top));
					}
					String topType = (String)obj.get("topType");
					if(topType != null && topType.length() > 0){
						cf.setTopType(topType);
					}
					cf.setUselink(uselink);
					cf.setValue(obj.get("vals") == null ? null : obj.get("vals").toString());
					cf.setMulti(true);
					cf.setSubs(new ArrayList<CrossField>());			
					tmp.add(cf);
					newCf.add(cf);
					
					//添加合计项
					if("y".equals(issum)){
						CrossField sumcf = new CrossField();
						sumcf.setType("none");
						String aggre = (String)obj.get("aggre");
						if(aggre != null && aggre.length() > 0 && !"auto".equals(aggre)){
							sumcf.setDimAggre(aggre);
						}
						sumcf.setDesc(MyCrossFieldLoader.loadFieldName(sumcf.getDimAggre()));
						sumcf.setSubs(new ArrayList<CrossField>());		
						tmp.add(sumcf);
						newCf.add(sumcf);
						
						//如果是col,需要给合计添加指标
						/**
						if(pos.equals("col")){
							List<KpiInfo> kpis = sqlVO.getKpis();
							for(KpiInfo kpi : kpis){
								CrossField kpicf = new CrossField();
								kpicf.setType(type);
								kpicf.setDesc(kpi.getKpiName());
								kpicf.setAggregation(kpi.getAggre());
								kpicf.setAlias(kpi.getAlias());
								kpicf.setFormatPattern(kpi.getFmt());
								kpicf.setSubs(new ArrayList<CrossField>());
								//用 size来表示指标ID，用在OLAP中
								kpicf.setId(kpi.getId().toString());
								if(kpi.getRate() != null){
									kpicf.setKpiRate(new BigDecimal(kpi.getRate()));
								}
								sumcf.getSubs().add(kpicf);
								kpicf.setParent(sumcf);
							}
						}
						**/
					}
					
				}else{
					for(CrossField tp : tmp){
						//如果上级是合计，下级不包含维度了, 但需要包含指标
						if(tp.getType().equals("none")){
							
							//如果是col,需要给合计添加指标
							if(pos.equals("col")){
								List<KpiInfo> kpis = sqlVO.getKpis();
								for(KpiInfo kpi : kpis){
									CrossField kpicf = new CrossField();
									kpicf.setType("kpiOther");
									kpicf.setDesc(kpi.getKpiName());
									kpicf.setAggregation(kpi.getAggre());
									kpicf.setAlias(kpi.getAlias());
									kpicf.setFormatPattern(kpi.getFmt());
									kpicf.setSubs(new ArrayList<CrossField>());
									//用 size来表示指标ID，用在OLAP中
									kpicf.setId(kpi.getId().toString());
									if(kpi.getRate() != null){
										kpicf.setKpiRate(new BigDecimal(kpi.getRate()));
									}
									tp.getSubs().add(kpicf);
									kpicf.setParent(tp);
								}
							}
							
							continue;
						}
						CrossField cf = new CrossField();
						cf.setType("frd"); //统一为frd
						cf.setId(obj.get("id").toString());
						cf.setDesc(obj.getString("dimdesc"));
						String alias = obj.getString("alias");
						String tableColKey = (String)obj.get("tableColKey");
						String tableColName = (String)obj.get("tableColName");
						if(tableColKey == null || tableColKey.length() == 0 || tableColName == null || tableColName.length() == 0){
							cf.setAlias(alias);
							cf.setAliasDesc(alias);
						}else{
							cf.setAlias(tableColKey);
							cf.setAliasDesc(tableColName);
						}
						cf.setCasParent(true);
						
						String top = (String)obj.get("top");
						if(top != null && top.length() > 0){
							cf.setTop(new Integer(top));
						}
						String topType = (String)obj.get("topType");
						if(topType != null && topType.length() > 0){
							cf.setTopType(topType);
						}
						cf.setUselink(uselink);
						cf.setValue(obj.get("vals") == null ? null : obj.get("vals").toString());
						cf.setMulti(true);
						cf.setSubs(new ArrayList<CrossField>());
						cf.setParent(tp);
						tp.getSubs().add(cf);
						newCf.add(cf);
						
						//添加合计项
						if("y".equals(issum)){
							CrossField sumcf = new CrossField();
							sumcf.setType("none");
							String aggre = (String)obj.get("aggre");
							if(aggre != null && aggre.length() > 0 && !"auto".equals(aggre)){
								sumcf.setDimAggre(aggre);
							}
							sumcf.setDesc(MyCrossFieldLoader.loadFieldName(sumcf.getDimAggre()));
							sumcf.setSubs(new ArrayList<CrossField>());		
							tp.getSubs().add(sumcf);
							newCf.add(sumcf);
						}
					}
				}
				tmp = newCf;
			}
			
		}
	}	
	
	//输出单位比例
	public String writerUnit(BigDecimal bd){
		if(bd == null){
			return "";
		}else{
			int v = bd.intValue();
			if(v == 1){
				return "";
			}else if(v == 100){
				return "百";
			}else if(v == 1000){
				return "千";
			}else if(v == 10000){
				return "万";
			}else if(v == 1000000){
				return "百万";
			}else if(v == 100000000){
				return "亿";
			}else{
				return "*" + v;
			}
		}
	}
	
	private CrossField kpiCompute(String compute, KpiInfo kpi){
		CrossField cf = new CrossField();
		if("zb".equals(compute)){
			cf.setDesc("占比");
			cf.setAggregation("avg");
			cf.setAlias(kpi.getAlias() + "_zb");
			cf.setFormatPattern("0.00%");
		}else if("sxpm".equals(compute) || "jxpm".equals(compute)){
			cf.setDesc(("sxpm".equals(compute) ? "升序":"降序") + "排名");
			cf.setAggregation("avg");
			cf.setAlias(kpi.getAlias() + "_order");
			cf.setFormatPattern("#,###");
			cf.setStyleClass("pms");
			cf.setStyleToLine(true);
		}else if("ydpj".equals(compute)){
			cf.setDesc("移动平均");
			cf.setAggregation(kpi.getAggre());
			cf.setAlias(kpi.getAlias() + "_ydpj");
			cf.setFormatPattern(kpi.getFmt());
			if(kpi.getRate() != null){
				cf.setKpiRate(new BigDecimal(kpi.getRate()));
			}
		}else if("sq".equals(compute)){
			cf.setDesc("上期值");
			cf.setAggregation(kpi.getAggre());
			cf.setAlias(kpi.getAlias()+"_sq");
			cf.setFormatPattern(kpi.getFmt());
			if(kpi.getRate() != null){
				cf.setKpiRate(new BigDecimal(kpi.getRate()));
			}
		}else if("tq".equals(compute)){
			cf.setDesc("同期值");
			cf.setAggregation(kpi.getAggre());
			cf.setAlias(kpi.getAlias()+"_tq");
			cf.setFormatPattern(kpi.getFmt());
			if(kpi.getRate() != null){
				cf.setKpiRate(new BigDecimal(kpi.getRate()));
			}
		}else if("zje".equals(compute)){
			cf.setDesc("增减额");
			cf.setAggregation(kpi.getAggre());
			cf.setAlias(kpi.getAlias() + "_zje");
			cf.setFormatPattern(kpi.getFmt());
			if(kpi.getRate() != null){
				cf.setKpiRate(new BigDecimal(kpi.getRate()));
			}
			cf.setFinanceFmt(true);
		}else if("hb".equals(compute)){
			cf.setDesc("环比");
			cf.setAggregation("avg");
			cf.setAlias(kpi.getAlias() + "_hb");
			cf.setFormatPattern("0.00%");
			cf.setFinanceFmt(true);
		}else if("tb".equals(compute)){
			cf.setDesc("同比");
			cf.setAggregation("avg");
			cf.setAlias(kpi.getAlias()+"_tb");
			cf.setFormatPattern("0.00%");
			cf.setFinanceFmt(true);
		}
		cf.setType("kpiOther");
		cf.setId("ext_" + kpi.getId()+"_"+compute); //表示当前指标是由基本指标衍生而来，比如昨日、累计、同比、环比、排名、占比等内容。
		return cf;
	}
	
	public StringBuffer getScripts() {
		return scripts;
	}
	
	public static String createWarning(JSONObject warn, String kpiFmt, StringBuffer scripts ){
		String funcName = "warn"+IdCreater.create();
		scripts.append("function " +funcName+"(val, a, b, c, d){");
		//先输出值
		scripts.append("out.print(val, '"+kpiFmt+"');");
		scripts.append("if(d != 'html'){"); //只在html模式下起作用
		scripts.append(" return;");
		scripts.append("}");
		scripts.append("if(val "+warn.getString("logic1")+" "+warn.getString("val1")+"){");
		scripts.append("out.print(\"<span class='"+warn.getString("pic1")+"'></span>\")");
		scripts.append("}else if(val "+(warn.getString("logic1").equals(">=")?"<":"<=")+" "+warn.getString("val1")+" && val "+warn.getString("logic2")+" "+warn.getString("val2")+"){");
		scripts.append("out.print(\"<span class='"+warn.getString("pic2")+"'></span>\")");
		scripts.append("}else{");
		scripts.append("out.print(\"<span class='"+warn.getString("pic3")+"'></span>\")");
		scripts.append("}");
		scripts.append("}");
		return funcName; 
	}
}
