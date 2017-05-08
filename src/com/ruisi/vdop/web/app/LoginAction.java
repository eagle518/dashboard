package com.ruisi.vdop.web.app;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.struts2.config.ParentPackage;

import net.sf.json.JSONObject;

import com.ruisi.ext.engine.ExtConstants;
import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.ext.engine.view.context.ExtContext;
import com.ruisi.vdop.bean.User;
import com.ruisi.vdop.service.frame.LoginLogServ;
import com.ruisi.vdop.service.frame.LoginServ;
import com.ruisi.vdop.util.VDOPUtils;
import com.ruisi.vdop.util.VdopConstant;

@ParentPackage("app-default")
public class LoginAction {
	
	private String userName;
	private String password;
	private String dbName = ExtContext.getInstance().getConstant(ExtConstants.dbName);
	private String userId;
	private String channel_id; //设备号
	
	private String token;
	
	/**
	 * 退出登录
	 * @return
	 */
	public String logout(){
		Map m = new HashMap();
		m.put("user_id", userId);
		m.put("token", token);
		DaoHelper daoHelper=VDOPUtils.getDaoHelper();
		daoHelper.getSqlMapClientTemplate().delete("vdop.frame.user.deletetoken", m);
		
		return null;
	}
	
	public String login() throws IOException{
		Map ret = new HashMap();
		
		boolean isok = true;
		
		List loginUser = LoginServ.getUserInfo(this);
		if (loginUser == null || loginUser.size() == 0 || loginUser.get(0) == null) {
			ret.put("msg" ,"账号不存在，请确认账号是否输入正确！");
			ret.put("result", false);
			isok = false;
		}else{
			User user = (User)loginUser.get(0);
	
			if(user.getState() == 0){
				ret.put("msg" ,"账号已停用，请联系客服人员。");
				ret.put("result", false);
				isok = false;
			}
			
			String temPassword=user.getPassword();
			String s = VDOPUtils.getEncodedStr(password);
	
			if (!temPassword.equals(s)) {
				ret.put("msg" , "口令输入错误！");
				ret.put("result", false);
				isok = false;
			}
			
			if(isok){
				//生成token staffId + date
				String token = VDOPUtils.getMD5((user.getStaffId() + new Date().getTime()).getBytes());
				
				ret.put("result", true);
				ret.put("user", user);
				ret.put("token", token);
				
				Map m = new HashMap();
				m.put("dbName", this.dbName);
				m.put("user_id", user.getUserId());
				m.put("token", token);
				m.put("channel_id", this.channel_id);
				DaoHelper daoHelper=VDOPUtils.getDaoHelper();
				
				
				//把token 写入数据库
				daoHelper.getSqlMapClientTemplate().insert("vdop.frame.user.addtoken", m);
				LoginLogServ.updateLogInfo(user);// 更新登录信息
			}
		}
		VDOPUtils.getResponse().setContentType("text/html; charset=UTF-8");
		String str = JSONObject.fromObject(ret).toString();
		VDOPUtils.getResponse().getWriter().print(str);
		return null;
	}
	
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getChannel_id() {
		return channel_id;
	}

	public void setChannel_id(String channelId) {
		channel_id = channelId;
	}
	
	

}
