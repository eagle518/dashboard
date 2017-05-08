package com.ruisi.vdop.web.portal;

import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import net.sf.json.JSONObject;

import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.ext.engine.view.context.ExtContext;
import com.ruisi.ext.engine.view.context.MVContext;
import com.ruisi.ext.engine.view.emitter.ContextEmitter;
import com.ruisi.ext.engine.view.emitter.excel.ExcelEmitter;
import com.ruisi.ext.engine.view.emitter.pdf.PdfEmitter;
import com.ruisi.ext.engine.view.emitter.text.TextEmitter;
import com.ruisi.ext.engine.view.emitter.word.WordEmitter;
import com.ruisi.vdop.ser.olap.CompPreviewService;
import com.ruisi.vdop.ser.portal.PortalPageService;
import com.ruisi.vdop.util.VDOPUtils;
import com.ruisi.vdop.util.VdopConstant;

public class ExportAction {
	
	private String type; //导出方式 
	private String json; //报表JSON
	private String pageId; //如果没有json,传递pageId
	
	private String picinfo;
	
	public String execute() throws Exception{
		if(json == null){
			DaoHelper dao = VDOPUtils.getDaoHelper();
			json = (String)dao.getSqlMapClientTemplate().queryForObject("bi.portal.select", this);
		}
		JSONObject pageJson = JSONObject.fromObject(json);
		PortalPageService pser = new PortalPageService(pageJson, VDOPUtils.getServletContext());
		ExtContext.getInstance().removeMV(PortalPageService.deftMvId);
		MVContext mv = pser.json2MV(false, true);
		
		CompPreviewService ser = new CompPreviewService();
		ser.setParams(pser.getMvParams());
		ser.initPreview();
		
		String fileName = "file.";
		if("html".equals(this.type)){
			fileName += "html";
		}else
		if("excel".equals(this.type)){
			fileName += "xls";
		}else
		if("csv".equals(this.type)){
			fileName += "csv";
		}else
		if("pdf".equals(this.type)){
			fileName += "pdf";
		}else 
		if("word".equals(this.type)){
			fileName += "docx";
		}
		
		HttpServletResponse resp = VDOPUtils.getResponse();
		resp.setContentType("application/x-msdownload");
		String contentDisposition = "attachment; filename=\""+fileName+"\"";
		resp.setHeader("Content-Disposition", contentDisposition);
		
		if("html".equals(this.type)){
			String ret = ser.buildMV(mv);
			String html = htmlPage(ret, VDOPUtils.getConstant("resPath"), "report");
			InputStream is = IOUtils.toInputStream(html, "utf-8");
			IOUtils.copy(is, resp.getOutputStream());
			is.close();
		}else
		if("excel".equals(this.type)){
			ContextEmitter emitter = new ExcelEmitter();
			ser.buildMV(mv, emitter);
		}else
		if("csv".equals(this.type)){
			ContextEmitter emitter = new TextEmitter();
			String ret = ser.buildMV(mv, emitter);
			InputStream is = IOUtils.toInputStream(ret, "gb2312");
			IOUtils.copy(is, resp.getOutputStream());
			is.close();
		}else 
		if("pdf".equals(this.type)){
			ContextEmitter emitter = new PdfEmitter();
			ser.buildMV(mv, emitter);
		}else
		if("word".equals(type)){
			ContextEmitter emitter = new WordEmitter();
			ser.buildMV(mv, emitter);
		}
		
		return null;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getJson() {
		return json;
	}

	public void setJson(String json) {
		this.json = json;
	}

	public String getPageId() {
		return pageId;
	}

	public void setPageId(String pageId) {
		this.pageId = pageId;
	}

	public String getPicinfo() {
		return picinfo;
	}

	public void setPicinfo(String picinfo) {
		this.picinfo = picinfo;
	}
	
	/**
	 * 生成导出html
	 * @param body
	 * @param host
	 * @param type 表示使用的类型，是 olap表示多维分析的导出， report 表示报表的导出
	 * @return
	 */
	public static String htmlPage(String body, String host, String type){
		StringBuffer sb = new StringBuffer();
		
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
		sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
		sb.append("<head>");
		sb.append("<title>睿思BI</title>");
		sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
		sb.append("<script type=\"text/javascript\" src=\""+host+"/ext-res/js/jquery.min.js\"></script>");
		sb.append("<script type=\"text/javascript\" src=\""+host+"/ext-res/js/ext-base.js\"></script>");
		sb.append("<script type=\"text/javascript\" src=\""+host+"/ext-res/js/echarts.min.js\"></script>");
		sb.append("<script type=\"text/javascript\" src=\""+host+"/ext-res/js/sortabletable.js\"></script>");
		sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\""+host+"/ext-res/css/fonts-min.css\" />");
		sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\""+host+"/ext-res/css/boncbase.css\" />");
		sb.append("</head>");
		sb.append("<body class=\"yui-skin-sam\">");
		if("report".equals(type)){  //报表类型需要限制宽度
			sb.append("<div style=\"width:960px; margin:0 auto;\">");
		}
		sb.append(body);
		if("report".equals(type)){
			sb.append("</div>");
		}
		sb.append("</body>");
		sb.append("</html>");
		
		return sb.toString();
	}
	
}
