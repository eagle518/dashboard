package com.ruisi.vdop.service.frame;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.vdop.util.VDOPUtils;
import com.ruisi.vdop.util.VdopConstant;
import com.ruisi.vdop.web.app.LoginAction;

/**
 * 判断用户是否登录的拦截器,用来app登录验证中，验证 token
 * @author hq
 * @date Mar 24, 2010
 */
public class LoginCheckAppInterceptor extends AbstractInterceptor  {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4868634967828833782L;
	
	/**
	 * 创建线程安全的hashMap， 用来管理token，提高查询效率
	 */
	private static Map<String, Object> tokenMap = Collections.synchronizedMap(new HashMap<String, Object>());

	public String intercept(ActionInvocation arg0) throws Exception {
		Object action = arg0.getAction();
		//如果是登录，用户注册action 不拦截
		if(action instanceof LoginAction ){
			return arg0.invoke();
		}
		String token = VDOPUtils.getRequest().getParameter("token");
		if(token == null || token.length() != 32){
			return "noLogin3";
		}
		
		//判断token是否存在, 并且获取 user_id， 
		//如果能从缓存中获取数据， 直接取缓存，不能的话查询数据库
		Map curObj = (Map)tokenMap.get(token);
		if(curObj == null){
			DaoHelper dao = VDOPUtils.getDaoHelper();
			Map m = new HashMap();
			m.put("token", token);
			curObj = (Map)dao.getSqlMapClientTemplate().queryForObject("bi.3g.token", m);
			if(curObj == null){
				return "noLogin3";
			}
			int cnt = Integer.parseInt(curObj.get("cnt").toString());
			if(cnt == 0){
				return "noLogin3";
			}
			tokenMap.put(token, curObj);
		}
		//数据放入request,方便app程序读取
		VDOPUtils.getRequest().setAttribute("app.userid", curObj.get("userId").toString());
		VDOPUtils.getRequest().setAttribute("app.channel_id", curObj.get("channel_id"));
		return arg0.invoke();
	}

}
