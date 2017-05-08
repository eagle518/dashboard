package com.ruisi.vdop.web.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONArray;

import org.apache.struts2.config.ParentPackage;

import com.ruisi.vdop.util.VDOPUtils;

@ParentPackage("app-default")
public class MenusAction {
	
	private String token;
	
	public String topMenu2() throws IOException{
		List ls = new ArrayList();
		HttpServletRequest request = VDOPUtils.getRequest();
		String path = request.getContextPath();
		String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
		
		Map m2 = new HashMap();
		m2.put("id", 3);
		m2.put("name", "手机报表");
		m2.put("note", "通过手机端查看报表数据,报表需在PC端先创建。");
		m2.put("pic", "resource/img/3g/a4.png");
		m2.put("url", "");
		ls.add(m2);
		
		Map m6 = new HashMap();
		m6.put("id", 5);
		m6.put("name", "系统帮助");
		m6.put("note", "睿思BI系统介绍，指导您正确使用本系统。");
		m6.put("pic", "resource/img/3g/help.png");
		m6.put("url", basePath + "app/Helper.action?token=" + token);
		ls.add(m6);
		
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		String str = JSONArray.fromObject(ls).toString();
		VDOPUtils.getResponse().getWriter().print(str);
		
		return null;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
	
	
}
