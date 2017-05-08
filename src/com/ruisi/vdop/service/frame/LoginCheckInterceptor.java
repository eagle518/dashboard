package com.ruisi.vdop.service.frame;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import com.ruisi.vdop.bean.User;
import com.ruisi.vdop.util.VDOPUtils;

/**
 * 判断用户是否登录的拦截器
 * @author hq
 * @date Mar 24, 2010
 */
public class LoginCheckInterceptor extends AbstractInterceptor  {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1960664016160102195L;


	public String intercept(ActionInvocation arg0) throws Exception {
		User user = VDOPUtils.getLoginedUser();
		if(user == null){
			return "noLogin";
		}else{
			return arg0.invoke();
		}
	}

}
