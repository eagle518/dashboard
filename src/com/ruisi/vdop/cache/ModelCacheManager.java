package com.ruisi.vdop.cache;

import java.util.HashMap;
import java.util.Map;

import com.ruisi.ext.engine.dao.DaoHelper;

import net.sf.json.JSONObject;

/**
 * 数据建模数据源/数据集缓存对象
 * @author hy
 * @date 2017-5-2
 */
public class ModelCacheManager {
	
	/**
	 * 数据源缓存对象
	 */
	private Map<String, JSONObject> dsources = new HashMap<String, JSONObject>();
	/**
	 * 数据集缓存对象
	 */
	private Map<String, JSONObject> dsets = new HashMap<String, JSONObject>();
	
	private static ModelCacheManager mananger = null;
	
	private static Object lock = new Object();
	
	private ModelCacheManager(){
		
	}
	
	static {
		synchronized(lock) {
			if(mananger == null){
				mananger = new ModelCacheManager();
			}
		}
	}
	
	public synchronized static JSONObject getDsource(String id, DaoHelper daoHelper){
		JSONObject ret = mananger.dsources.get(id);
		if(ret == null){
			Map m = (Map)daoHelper.getSqlMapClientTemplate().queryForObject("bi.model.getDs", id);
			mananger.dsources.put(id, JSONObject.fromObject(m));
			ret = mananger.dsources.get(id);
		}
		return ret;
	}
	
	public synchronized static JSONObject getDset(String id, DaoHelper daoHelper){
		JSONObject ret = mananger.dsets.get(id);
		if(ret == null){
			String cfg = (String)daoHelper.getSqlMapClientTemplate().queryForObject("bi.model.getDset", id);
			mananger.dsets.put(id, JSONObject.fromObject(cfg));
			ret = mananger.dsets.get(id);
		}
		return ret;
	}
	
	public synchronized static void removeDsource(String id){
		mananger.dsources.remove(id);
	}
	
	public synchronized static void removeDset(String id){
		mananger.dsets.remove(id);
	}
	
	public synchronized static void addDsource(String id, JSONObject json){
		mananger.dsources.put(id, json);
	}
	
	public synchronized static void addDset(String id, JSONObject json){
		mananger.dsets.put(id, json);
	}
}
