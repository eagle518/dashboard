package com.ruisi.vdop.util;

import java.util.Date;

import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.processors.JsonValueProcessor;

/**
 * json-lib 处理 java.sql.Date 对象有问题，转换成 java.uitl.Date 对象
 * @author hq
 * @date 2016-3-17
 */
public class JsonDateValueProcessor implements JsonValueProcessor  {

	@Override
	public Object processArrayValue(Object arg0, JsonConfig arg1) {
		java.sql.Date dt = (java.sql.Date)arg0;
		return JSONObject.fromObject(new Date(dt.getTime()));
	}

	@Override
	public Object processObjectValue(String arg0, Object arg1, JsonConfig arg2) {
		java.sql.Date dt = (java.sql.Date)arg1;
		return JSONObject.fromObject(new Date(dt.getTime()));
	}
	

}
