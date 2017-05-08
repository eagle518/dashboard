package com.ruisi.vdop.ser.portal;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.ruisi.ext.engine.ExtConstants;
import com.ruisi.ext.engine.init.TemplateManager;
import com.ruisi.ext.engine.util.IdCreater;
import com.ruisi.ext.engine.util.PasswordEncrypt;
import com.ruisi.ext.engine.view.context.Element;
import com.ruisi.ext.engine.view.context.ExtContext;
import com.ruisi.ext.engine.view.context.MVContext;
import com.ruisi.ext.engine.view.context.MVContextImpl;
import com.ruisi.ext.engine.view.context.dsource.DataSourceContext;
import com.ruisi.ext.engine.view.context.face.OptionsLoader;
import com.ruisi.ext.engine.view.context.form.ButtonContext;
import com.ruisi.ext.engine.view.context.form.ButtonContextImpl;
import com.ruisi.ext.engine.view.context.form.CheckBoxContextImpl;
import com.ruisi.ext.engine.view.context.form.DateSelectContext;
import com.ruisi.ext.engine.view.context.form.DateSelectContextImpl;
import com.ruisi.ext.engine.view.context.form.InputField;
import com.ruisi.ext.engine.view.context.form.MultiSelectContextImpl;
import com.ruisi.ext.engine.view.context.form.RadioContextImpl;
import com.ruisi.ext.engine.view.context.form.SelectContextImpl;
import com.ruisi.ext.engine.view.context.form.TextFieldContext;
import com.ruisi.ext.engine.view.context.form.TextFieldContextImpl;
import com.ruisi.ext.engine.view.context.html.DivContext;
import com.ruisi.ext.engine.view.context.html.DivContextImpl;
import com.ruisi.ext.engine.view.context.html.IncludeContext;
import com.ruisi.ext.engine.view.context.html.IncludeContextImpl;
import com.ruisi.ext.engine.view.context.html.TextContext;
import com.ruisi.ext.engine.view.context.html.TextContextImpl;
import com.ruisi.ext.engine.view.context.html.TextProperty;
import com.ruisi.ext.engine.view.context.html.table.TableContext;
import com.ruisi.ext.engine.view.context.html.table.TableContextImpl;
import com.ruisi.ext.engine.view.context.html.table.TdContext;
import com.ruisi.ext.engine.view.context.html.table.TdContextImpl;
import com.ruisi.ext.engine.view.context.html.table.TrContext;
import com.ruisi.ext.engine.view.context.html.table.TrContextImpl;
import com.ruisi.ext.engine.view.exception.ExtConfigException;
import com.ruisi.vdop.cache.ModelCacheManager;
import com.ruisi.vdop.ser.webreport.DataService;
import com.ruisi.vdop.util.VDOPUtils;
import com.ruisi.vdop.util.VdopConstant;

public class PortalPageService {
	
	public final static String deftMvId = "mv.portal.tmp";
	
	private Map<String, InputField> mvParams = new HashMap(); //mv的参数
	private StringBuffer css = new StringBuffer(); //在创建页面过程中生成所需要的组件样式文件
	
	private JSONObject pageJson;
	
	private String mvid; //发布时，使用的mvid
	
	private ServletContext sctx;
	
	private List<String> dsids = new ArrayList<String>(); //用到的数据原
		
	public PortalPageService(JSONObject pageJson, ServletContext sctx){
		this.pageJson = pageJson;
		this.sctx = sctx;
	}
	
	public PortalPageService(JSONObject pageJson, ServletContext sctx, String mvid){
		this.pageJson = pageJson;
		this.sctx = sctx;
		this.mvid = mvid;
	}
	
	public MVContext json2MV(boolean release, boolean export) throws Exception{
		//创建MV
		MVContext mv = new MVContextImpl();
		mv.setChildren(new ArrayList());
		String formId = ExtConstants.formIdPrefix + IdCreater.create();
		mv.setFormId(formId);
		mv.setMvid(deftMvId);
		
		IncludeContext inc = new IncludeContextImpl();
		String stylename = (String)pageJson.get("stylename");
		if(stylename != null && stylename.length() > 0 && !"def".equals(stylename)){
			inc.setPage("/resource/css/portal-inc-"+stylename+".css");
		}else{
			inc.setPage("/resource/css/portal-inc.css");
		}
		mv.getChildren().add(inc);
		inc.setParent(mv);
		
		//解析参数
		Object param = pageJson.get("params");
		if(param != null && ((JSONArray)param).size()>0){
			DivContext div = new DivContextImpl();
			div.setStyleClass("mv_param");
			div.setParent(mv);
			div.setChildren(new ArrayList());
			mv.getChildren().add(div);
			JSONArray pp = (JSONArray)param;
			for(int i=0; i<pp.size(); i++){
				this.parserParam(pp.getJSONObject(i), div, mv, release?false:true);
			}
			if(!export){
				//创建提交按钮
				ButtonContext btn = new ButtonContextImpl();
				btn.setDesc("查询");
				btn.setType("button");
				btn.setMvId(new String[]{mv.getMvid()});
				div.getChildren().add(btn);
				btn.setParent(div);
			}
		}
		
		JSONObject body = pageJson.getJSONObject("body");
		parserBody(body, mv, param, release);
		//生成样式
		TextContext csstext = new TextContextImpl();
		csstext.setText("<style>" + this.css.toString() + "</style>");
		mv.getChildren().add(csstext);
		csstext.setParent(mv);
		//生成数据原
		for(String dsid : dsids){
			PortalPageService.createDsource(ModelCacheManager.getDsource(dsid, VDOPUtils.getDaoHelper(sctx)), mv);
		}
		return mv;
	}
	
	//解析布局器
	public void parserBody(JSONObject body, MVContext mv, Object param, boolean release) throws Exception{
		PortalCompService compSer = new PortalCompService(mv, this);
		
		TableContext tab = new TableContextImpl();
		tab.setStyleClass("r_layout");
		tab.setChildren(new ArrayList());
		mv.getChildren().add(tab);
		tab.setParent(mv);
		for(int i=1; true; i++){
			Object tmp = body.get("tr" + i);
			if(tmp == null){
				break;
			}
			JSONArray trs = (JSONArray)tmp;
			TrContext tabTr = new TrContextImpl();
			tabTr.setChildren(new ArrayList());
			tab.getChildren().add(tabTr);
			tabTr.setParent(tab);
			for(int j=0; j<trs.size(); j++){
				JSONObject td = trs.getJSONObject(j);
				TdContext tabTd = new TdContextImpl();
				tabTd.setStyleClass("layouttd");
				tabTd.setChildren(new ArrayList());
				tabTd.setParent(tabTr);
				tabTr.getChildren().add(tabTd);
				tabTd.setColspan(String.valueOf(td.getInt("colspan")));
				tabTd.setRowspan(String.valueOf(td.getInt("rowspan")));
				tabTd.setWidth(td.getInt("width") + "%");
				
				Object cldTmp = td.get("children");
				
				if(cldTmp != null){
					JSONArray children = (JSONArray)cldTmp;
					for(int k=0; k<children.size(); k++){
						JSONObject comp = children.getJSONObject(k);
						String tp = comp.getString("type");
						
						//生成外层div
						DivContext div = new DivContextImpl(); //外层div
						div.setStyleClass("cbox");
						if(tp.equals("table") || tp.equals("grid")){  //表格取消边框
							div.setStyle("border:none;");
						}
						div.setChildren(new ArrayList());
						tabTd.getChildren().add(div);
						div.setParent(tabTd);
						
						//判断是否生成title
						String showtitle = (String)comp.get("showtitle");
						if(showtitle != null && "false".equalsIgnoreCase(showtitle)){   //不生成head
							
						}else{   //生成head
							DivContext head = new DivContextImpl(); //内层head Div
							head.setChildren(new ArrayList());
							if(tp.equals("table") || tp.equals("grid")){  //表格设置head边框
								head.setStyleClass("ctableview");
							}else{
								head.setStyleClass("ctitleview");
							}
							div.getChildren().add(head);
							head.setParent(div);
							
							TextContext text = new TextContextImpl(); //head Div 的文字
							text.setText(comp.getString("name"));
							TextProperty ctp = new TextProperty();
							ctp.setAlign("center");
							ctp.setWeight("bold");
							text.setTextProperty(ctp);
							head.getChildren().add(text);
							text.setParent(head);
						}
						
						DivContext content = new DivContextImpl(); //内层content Div
						content.setStyleClass("cctx");
						//content.setStyle("margin:3px;");
						content.setChildren(new ArrayList());
						div.getChildren().add(content);
						content.setParent(div);
						
						if(tp.equals("text")){
							compSer.createText(content, comp);
						}else if(tp.equals("chart")){
							compSer.createChart(content, comp, release);
						}else if(tp.equals("table")){
							compSer.crtTable(content, comp, release);
						}else if(tp.equals("grid")){
							compSer.crtGrid(content, comp, release);
						}else if(tp.equals("box")){
							compSer.createBox(content, comp);
						}
					}
				}
			}
		}
		//生成scripts
		String s = compSer.getScripts().toString();
		if(s.length() > 0){
			mv.setScripts(s);
		}
	}
	
	/**
	 * 生成动态参数
	 * @param params
	 * @param mv
	 * @param isput 是否把参数放入MV对象，在发布的时候不用
	 * @throws ExtConfigException
	 * @throws IOException
	 */
	public void parserParam(JSONObject param, DivContext div, MVContext mv, boolean isput) throws ExtConfigException, IOException {
	
			String type = param.getString("type");
			String id = param.getString("paramid");
			String desc = param.getString("name");
			String def = (String)param.get("defvalue");
			String vtp = (String)param.get("valtype");
			String dtformat = (String)param.get("dtformat");
			String hiddenprm = (String)param.get("hiddenprm");
			
			InputField input = null;
			if("y".equals(hiddenprm)){
				TextFieldContext txt = new TextFieldContextImpl();
				txt.setType("hidden");
				txt.setShow(true);
				input = txt;
			}else{
				if("radio".equals(type)){
					SelectContextImpl target = new SelectContextImpl();
					if("static".equals(vtp)){
						this.paramOptions(param, target);
					}else if("dynamic".equals(vtp)){
						String sql = this.createDimSql(param);
						String template = TemplateManager.getInstance().createTemplate(sql);
						target.setTemplateName(template);
					}
					target.setAddEmptyValue(true);
					input = target;
				}else if("checkbox".equals(type)){
					CheckBoxContextImpl target = new CheckBoxContextImpl(); 
					if("static".equals(vtp)){
						this.paramOptions(param, target);
					}else if("dynamic".equals(vtp)){
						String sql = this.createDimSql(param);
						String template = TemplateManager.getInstance().createTemplate(sql);
						target.setTemplateName(template);
					}
					input = target;
				}else if("dateselect".equals(type) || "monthselect".equals(type) || "yearselect".equals(type)){  //日历框
					DateSelectContextImpl target = new DateSelectContextImpl();
					//target.setShowCalendar(true);
					String max =  (String)param.get("maxval");
					if(max != null && max.length() > 0){
						target.setMaxDate(max);
					}
					String min = (String)param.get("minval");
					if(min != null && min.length() > 0){
						target.setMinDate(min);
					}
					if(dtformat != null && dtformat.length() > 0){
						target.setDateFormat(dtformat);
					}
					if("monthselect".equals(type)){
						target.setDateType("month");
					}else if("yearselect".equals(type)){
						target.setDateType("year");
					}
					input = target;
				}else if("text".equals(type)){
					TextFieldContext target = new TextFieldContextImpl();
					input = target;
				}
			}
			input.setId(id);
			input.setDesc(desc);
			String size = (String)param.get("size");
			if(size != null && size.length() > 0){
				if("radio".equals(type)){
					//select 框就是 radio,他的size表示像素，转换成实际size
					input.setSize(String.valueOf(Integer.parseInt(size) * 8));
				}else{
					input.setSize(size);
				}
			}
			if(def != null && def.length() > 0){
				if(("dateselect".equals(type) || "monthselect".equals(type) || "yearselect".equals(type) )&& "now".equals(def)){
					def = new SimpleDateFormat(dtformat).format(new Date());
				}
				input.setDefaultValue(def);
			}
			div.getChildren().add(input);
			input.setParent(div);
			
			//把参数放入对象
			if(isput){
				this.mvParams.put(input.getId(), input);
				ExtContext.getInstance().putServiceParam(mv.getMvid(), input.getId(), input);
			}
						
			//处理样式
			JSONObject style = (JSONObject)param.get("style");
			if(style != null && !style.isNullObject() && !style.isEmpty()){
				StringBuffer sb = new StringBuffer();
				String talign = (String)style.get("talign"); //排列方式
				if(talign != null && "horizontal".equals(talign) && ("radio".equals(type) || "checkbox".equals(type))){
					((RadioContextImpl)input).setRadioStyle("display:inline;");
				}
				String theight = (String)style.get("theight");
				String fontweight = (String)style.get("tfontweight");
				String tfontcolor = (String)style.get("tfontcolor");
				String tfontsize = (String)style.get("tfontsize");
				if(tfontsize != null && tfontsize.length() > 0){
					sb.append("font-size:"+tfontsize+"px;");
				}
				if(theight != null && theight.length() > 0){
					sb.append("height:"+theight+"px;");
				}
				if(fontweight != null && "true".equals(fontweight)){
					sb.append("font-weight:bold;");
				}
				if(tfontcolor != null && tfontcolor.length() > 0){
					sb.append("color:" + tfontcolor+";");
				}
				
				String italic = (String)style.get("titalic");
				String underscore = (String)style.get("tunderscore");
				String lineheight = (String)style.get("tlineheight");
				String tbgcolor = (String)style.get("tbgcolor");
				if("true".equals(italic)){
					sb.append("font-style:italic;");
				}
				if("true".equals(underscore)){
					sb.append("text-decoration: underline;");
				}
				if(lineheight != null && lineheight.length() > 0){
					sb.append("line-height:"+lineheight+"px;");
				}
				if(tbgcolor != null && tbgcolor.length() > 0){
					sb.append("background-color:"+tbgcolor+";");
				}
				div.setStyle(sb.toString());
			}
		
	}
	
	private void paramOptions(JSONObject param, OptionsLoader option){
		List ls = option.loadOptions();
		if(ls == null){
			ls = new ArrayList();
			option.setOptions(ls);
		}
		Object vals = param.get("values");
		if(vals != null){
			JSONArray values = (JSONArray)vals;
			for(int i=0; i<values.size(); i++){
				JSONObject opt = values.getJSONObject(i);
				Map<String, String> nOption = new HashMap<String, String>();
				nOption.put("text", opt.getString("text"));
				nOption.put("value", opt.getString("value"));
				ls.add(nOption);
			}
		}
	}
	
	public String createDimSql(JSONObject dim){
		JSONObject opt = dim.getJSONObject("option");
		Map p = new HashMap();
		p.put("dimId", opt.get("dimId"));
		p.put("tid", opt.get("tableId"));
		//查询事实表
		Map dimCOl = (Map)VDOPUtils.getDaoHelper().getSqlMapClientTemplate().queryForObject("bi.olap.queryDimCol", p);
		String col = (String)dimCOl.get("col");
		String key = (String)dimCOl.get("colkey");
		String name = (String)dimCOl.get("colname");
		String dimord = (String)dimCOl.get("dimord");
		String ordcol = (String)dimCOl.get("ordcol");
		String sql =  "select distinct " +  (key==null||key.length() == 0 ? col : key) + " \"value\", " + (name==null||name.length() == 0 ?col:name) + " \"text\" from " + dimCOl.get("tname");
		if(ordcol != null && ordcol.length() > 0){
			sql += " order by " + ordcol;
		}
		if(ordcol != null && ordcol.length() > 0 && dimord != null && dimord.length() > 0){
			sql += " " + dimord;
		}
		 //直接从数据中查询。
		return sql;
	}
	
	public String createMonthSql(){
		String sql = "select mid \"value\", mname \"text\" from code_month order by mid desc";
		return sql;
	}
	
	public static String createDsource(JSONObject dsourceJson, MVContext mv){
		DataSourceContext dsource = new DataSourceContext();
		dsource.putProperty("id", dsourceJson.getString("id"));
		Object use = dsourceJson.get("usetype");
		dsource.putProperty("usetype", use == null ? null : use.toString());
		if(use == null || "jdbc".equalsIgnoreCase(use.toString())){
			String linktype = dsourceJson.getString("linktype");
			dsource.putProperty("linktype", linktype);
			dsource.putProperty("linkname", dsourceJson.getString("uname"));
			dsource.putProperty("linkpwd", PasswordEncrypt.encode(dsourceJson.getString("psd")));
			dsource.putProperty("linkurl", dsourceJson.getString("linkurl"));
		}else{
			dsource.putProperty("jndiname", dsourceJson.getString("dsname"));
		}
		//放入MV
		if(mv.getDsources() == null){
			mv.setDsources(new HashMap<String, DataSourceContext>());
		}
		mv.getDsources().put(dsource.getId(), dsource);
		return dsource.getId();
	}
	
	/**
	 * 根据表名生成别名
	 * @param dset
	 */
	public static Map<String, String> createTableAlias(JSONObject dset){
		Map<String, String> tableAlias = new HashMap<String, String>();
		tableAlias.put(dset.getString("master"), "a0");
		JSONArray joinTabs = (JSONArray)dset.get("joininfo");
		for(int i=0; joinTabs != null && i<joinTabs.size(); i++){
			JSONObject tab = joinTabs.getJSONObject(i);
			tableAlias.put(tab.getString("ref"), "a" + (i+1));
		}
		return tableAlias;
	}
	
	public Map<String, InputField> getMvParams() {
		return mvParams;
	}

	public StringBuffer getCss() {
		return css;
	}

	public void setMvParams(Map<String, InputField> mvParams) {
		this.mvParams = mvParams;
	}

	public void setCss(StringBuffer css) {
		this.css = css;
	}

	public JSONObject getPageJson() {
		return pageJson;
	}

	public void setPageJson(JSONObject pageJson) {
		this.pageJson = pageJson;
	}

	public ServletContext getSctx() {
		return sctx;
	}

	public List<String> getDsids() {
		return dsids;
	}
	
	
}
