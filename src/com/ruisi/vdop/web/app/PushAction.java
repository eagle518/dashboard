package com.ruisi.vdop.web.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;

import com.ruisi.ext.engine.ExtConstants;
import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.vdop.util.JsonDateValueProcessor;
import com.ruisi.vdop.util.VDOPUtils;
import com.ruisi.vdop.util.VdopConstant;

/**
 * 推送信息action, 通过app推送每天、每月的报表数据。
 * @author hq
 * @date 2016-4-22
 */
public class PushAction {

	
	
	public String list() throws IOException{
		List ls = new ArrayList();
		String str = JSONArray.fromObject(ls).toString();
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		VDOPUtils.getResponse().getWriter().print(str);
		return null;
	}
	
	public String listMsg() throws IOException{
		List ls = new ArrayList();
		String str = JSONArray.fromObject(ls).toString();
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		VDOPUtils.getResponse().getWriter().print(str);
		return null; 
	}
	
	public String listMsg2() throws IOException{
		List ls = new ArrayList();
		Map ret = new HashMap();
		ret.put("rows", ls);
		ret.put("hasNext",  false);
		String str = JSONObject.fromObject(ret).toString();
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		VDOPUtils.getResponse().getWriter().print(str);
		return null; 
	}
	
	
	
	
}
