package com.ruisi.vdop.service.frame;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import com.ruisi.vdop.bean.User;
import com.ruisi.vdop.util.VDOPUtils;

public class LoginCheck2Interceptor extends AbstractInterceptor {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String intercept(ActionInvocation arg0) throws Exception {
		User user = VDOPUtils.getLoginedUser();
		if(user == null){
			return "noLogin2";
		}else{
			return arg0.invoke();
		}
	}

}
