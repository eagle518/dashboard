package com.ruisi.vdop.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * 在项目启动或结束是做一些初始化或清理工作
 * @author hq
 * @date 2015-4-5
 */
public class MyServletContextListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		ServletContext ctx = sce.getServletContext();
		VDOPUtils.setMyServletContext(ctx);
		
	}

}
