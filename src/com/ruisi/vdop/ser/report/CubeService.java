package com.ruisi.vdop.ser.report;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCallback;

import com.ruisi.ext.engine.ExtConstants;
import com.ruisi.ext.engine.dao.DaoHelper;
import com.ruisi.vdop.util.VDOPUtils;

public class CubeService {
	
	private JSONObject cubeJson;
	private DaoHelper daoHelper;

	private Integer cubeId;
	private String targetId;
	private String dbName = VDOPUtils.getConstant(ExtConstants.dbName);
	
	public CubeService(JSONObject cubeJson, DaoHelper daoHelper){
		this.cubeJson = cubeJson;
		this.daoHelper = daoHelper;
	}
	
	/**
	 * 更新cube数据的过程是先删除以前数据，再插入新数据。
	 */
	public void updateCube(){
		daoHelper.getSqlMapClientTemplate().update("bi.model.uptableCube", cubeJson);
		cubeId = cubeJson.getInt("cubeId");
		
		//在编辑立方体时，通过delObj 来描述哪些维度，度量、分组被删除掉了。先第一步删除这些
		JSONArray delObj = cubeJson.getJSONArray("delObj");
		if(delObj != null && !delObj.isEmpty()){
			for(int i=0; i<delObj.size(); i++){
				JSONObject obj = delObj.getJSONObject(i);
				String tp = obj.getString("type");
				Object id = obj.get("id");
				if(id == null || id.toString().length() == 0){
					continue;
				}
				this.targetId = id.toString();
				
				if("dim".equals(tp)){
					this.daoHelper.getSqlMapClientTemplate().delete("bi.model.deldim", this);
				}else if("kpi".equals(tp)){
					this.daoHelper.getSqlMapClientTemplate().delete("bi.model.delkpi", this);
				}else if("group".equals(tp)){
					this.daoHelper.getSqlMapClientTemplate().delete("bi.model.delgroup", this);
				}
			}
		}
		//删除关系表数据，再从建
		this.daoHelper.getSqlMapClientTemplate().delete("bi.model.deleteCubeMeta", this);
		//处理维度
		this.updateDim(cubeId);
		this.insertDimRela(cubeId);
		//处理指标
		this.updateKpi(cubeId);
		this.insertKpiRela(cubeId);
	}
	
	public void saveCube(){
		this.cubeId = (Integer)daoHelper.getSqlMapClientTemplate().queryForObject("bi.model.maxcubeId");
		//插入表数据
		Map m = new HashMap();
		m.put("cubeId", cubeId);
		m.put("cubeName", cubeJson.get("name"));
		m.put("cubeDesc", cubeJson.get("note"));
		m.put("dsetId", cubeJson.get("dsetId"));
		this.daoHelper.getSqlMapClientTemplate().insert("bi.model.insertCube", m);
		this.insertDim(cubeId);
		this.insertDimRela(cubeId);
		this.insertKpi(cubeId);
		this.insertKpiRela(cubeId);
	}
	
	public void updateKpi(int cubeId){
		JSONArray kpis = this.cubeJson.getJSONArray("kpi");
		for(int i=0; i<kpis.size(); i++){
			JSONObject kpi = kpis.getJSONObject(i);
			kpi.put("cubeId", cubeId);
			kpi.put("dbName", dbName);
			if(kpi.getBoolean("calc")){
				kpi.put("calc", 1);
			}else{
				kpi.put("calc", 0);
			}
			
			Object targetId = kpi.get("targetId");
			if(targetId == null){ //新增
				if("oracle".equals(dbName)){
					targetId = daoHelper.getSqlMapClientTemplate().queryForObject("bi.model.maxkpiid", this);
					kpi.put("kpiId", targetId);
				}
				this.daoHelper.getSqlMapClientTemplate().insert("bi.model.savekpi", kpi);
				targetId = targetId == null ? this.daoHelper.queryForInt("select max(rid) from olap_kpi_list") : targetId;
			}else{ //修改
				String isupdate = (String)kpi.get("isupdate");
				if("y".equals(isupdate)){
					this.daoHelper.getSqlMapClientTemplate().update("bi.model.updatekpi", kpi);
				}
			}
			kpi.put("kpiId", targetId);
		}
	}
	
	//插入指标
	public void insertKpi(int cubeId){
		int kpiId = 0;
		if("oracle".equals(dbName)){
			kpiId = this.daoHelper.queryForInt("select case WHEN max(rid) is null then 1 else  max(rid) + 1 end  from olap_kpi_list");
		}
		JSONArray kpis = this.cubeJson.getJSONArray("kpi");
		for(int i=0; i<kpis.size(); i++){
			JSONObject kpi = kpis.getJSONObject(i);
			kpi.put("cubeId", cubeId);
			kpi.put("dbName", dbName);
			if(kpi.getBoolean("calc")){
				kpi.put("calc", 1);
			}else{
				kpi.put("calc", 0);
			}
			if("oracle".equals(dbName)){  //oracle 自动生成id
				kpi.put("kpiId", kpiId);
				kpiId++;
			}
			this.daoHelper.getSqlMapClientTemplate().insert("bi.model.savekpi", kpi);
			if(!"oracle".equals(dbName)){  //不是oracle 才有数据库生成id,再获取id
				if(i == 0){
					kpiId = this.daoHelper.queryForInt("select max(rid) from olap_kpi_list");
				}else{
					kpiId++;
				}
				kpi.put("kpiId", kpiId);
			}
		}
	}
	
	public void updateDim(int cubeId){
		Map groupkeys = new HashMap(); //group 的 hashmap 对象
		List ls = daoHelper.getSqlMapClientTemplate().queryForList("bi.model.listGroup", this);
		for(int i=0; i<ls.size(); i++){
			String str = (String)ls.get(i);
			groupkeys.put(str, "");
		}
		JSONArray dims = this.cubeJson.getJSONArray("dim");
		for(int i=0; i<dims.size(); i++){
			JSONObject dim = dims.getJSONObject(i);
			dim.put("cubeId", cubeId);
			dim.put("ord", i);
			dim.put("dbName", dbName);
			String type = (String)dim.get("type");
			if(type == null || type.length() == 0){
				dim.put("type", "frd");
			}
			//判断是否有分组，如果有分组插入分组
			String groupId = (String)dim.get("groupId");
			if(groupId != null && groupId.length() > 0 && !groupkeys.containsKey(groupId)){
				daoHelper.getSqlMapClientTemplate().insert("bi.model.addGroup", dim);
				groupkeys.put(groupId, "");
			}
			Object targetId = dim.get("targetId");
			if(targetId == null){  //新增维度
				if("oracle".equals(dbName)){
					targetId = this.daoHelper.getSqlMapClientTemplate().queryForObject("bi.model.maxdimid", this);
					dim.put("dimId", targetId);
				}
				this.daoHelper.getSqlMapClientTemplate().insert("bi.model.insertdim", dim);
				targetId = targetId == null ? this.daoHelper.queryForInt("select max(dim_id) from olap_dim_list") : targetId;
			}else{ //修改维度
				String isupdate = (String)dim.get("isupdate");
				if("y".equals(isupdate)){  //只有修改过的维度才更新
					this.daoHelper.getSqlMapClientTemplate().update("bi.model.updatedim", dim);
				}
			}
			dim.put("dimId", targetId);
		}
	}
	
	public void insertDim(int cubeId){
		Map groupkeys = new HashMap(); //group 的 hashmap 对象
		JSONArray dims = this.cubeJson.getJSONArray("dim");
		int dimId = 0;
		for(int i=0; i<dims.size(); i++){
			JSONObject dim = dims.getJSONObject(i);
			dim.put("cubeId", cubeId);
			dim.put("ord", i);
			dim.put("dbName", dbName);
			String type = (String)dim.get("type");
			if(type == null || type.length() == 0){
				dim.put("type", "frd");
			}
			//判断是否有分组，如果有分组插入分组
			String groupId = (String)dim.get("groupId");
			if(groupId != null && groupId.length() > 0 && !groupkeys.containsKey(groupId)){
				daoHelper.getSqlMapClientTemplate().insert("bi.model.addGroup", dim);
				groupkeys.put(groupId, "");
			}
			if("oracle".equals(dbName)){  //oracle 插入数据库，需要 dim_id
				if(dimId == 0){
					dimId = this.daoHelper.queryForInt("select case WHEN max(dim_id) is null then 1 else  max(dim_id) + 1 end  from olap_dim_list");
				}else{
					dimId++;
				}
				dim.put("dimId", dimId);
				this.daoHelper.getSqlMapClientTemplate().insert("bi.model.insertdim", dim);
			}else{  //其他数据库采用自增
				this.daoHelper.getSqlMapClientTemplate().insert("bi.model.insertdim", dim);
				if(i == 0){
					dimId = this.daoHelper.queryForInt("select max(dim_id) from olap_dim_list");
				}else{
					dimId++;
				}
			}
			dim.put("dimId", dimId);
		}
	}
	
	
	private void insertKpiRela(final int cubeId){
		//先删除指标数据
		String delsql = "delete from olap_cube_col_meta where cube_id = " + cubeId + " and col_type = 2  ";
		this.daoHelper.execute(delsql);
		
		//获取id
		String maxsql = "select case WHEN max(rid) is null then 1 else  max(rid) + 1 end  from olap_cube_col_meta";
		final int maxid = "oracle".equals(dbName) ? this.daoHelper.queryForInt(maxsql) : 0;
		
		//添加指标
		JSONArray array = this.cubeJson.getJSONArray("kpi");
		for(int i=0; i<array.size();i++){
			final JSONObject kpi = array.getJSONObject(i);
			final int idx = i;
			String sql = "insert into olap_cube_col_meta(cube_id, col_type, tname, col_id, col_name, alias, ord, calc"+("oracle".equals(dbName)?",rid":"")+") values(?,?,?,?,?,?,?,?"+("oracle".equals(dbName)?",?":"")+")";
			this.daoHelper.execute(sql, new PreparedStatementCallback(){

				@Override
				public Object doInPreparedStatement(PreparedStatement ps)
						throws SQLException, DataAccessException {
					ps.setInt(1, cubeId);
					ps.setInt(2, 2);
					ps.setString(3, kpi.getString("tname"));
					ps.setInt(4, kpi.getInt("kpiId"));
					//如果指标不是计算指标，直接拼接，计算指标直接取公式
					int calcKpi = kpi.getInt("calcKpi");  //新增度量那创建的计算指标
					int calc = kpi.getInt("calc");  //数据集创建的动态字段
					if(calcKpi == 0){
						ps.setString(5, kpi.getString("aggre") + "(" + kpi.getString("col") + ")");
					}else{
						ps.setString(5, kpi.getString("col"));
					}
					ps.setString(6, kpi.getString("alias"));
					ps.setInt(7, idx);
					ps.setInt(8, kpi.getInt("calc"));
					if("oracle".equals(dbName)){
						ps.setInt(9, maxid + idx);
					}
					ps.executeUpdate();
					return null;
				}
				
			});
		}
	}
	
	private void insertDimRela(final int cubeId){
		//先删除维度数据
		String delsql = "delete from olap_cube_col_meta where cube_id = " + cubeId + " and col_type = 1";
		this.daoHelper.execute(delsql);
		
		String maxsql = "select case WHEN max(rid) is null then 1 else  max(rid) + 1 end  from olap_cube_col_meta";
		
		final int maxid = "oracle".equals(dbName) ? this.daoHelper.queryForInt(maxsql) : 0;
		
		//添加维
		JSONArray array = this.cubeJson.getJSONArray("dim");
		for(int i=0; i<array.size();i++){
			final JSONObject dim = array.getJSONObject(i);
			final int idx = i;
			String sql = "insert into olap_cube_col_meta(cube_id, col_type, col_id, tname, col_name, alias, calc, ord"+(dbName.equals("oracle")?",rid":"")+") values(?,?,?,?,?,?,?,?"+(dbName.equals("oracle")?",?":"")+")";
			this.daoHelper.execute(sql, new PreparedStatementCallback(){

				@Override
				public Object doInPreparedStatement(PreparedStatement ps)
						throws SQLException, DataAccessException {
					ps.setInt(1, cubeId);
					ps.setInt(2, 1);
					ps.setInt(3, dim.getInt("dimId"));
					ps.setString(4, dim.getString("tname"));
					ps.setString(5, dim.getString("col"));
					ps.setString(6, dim.getString("alias"));
					ps.setInt(7, dim.getBoolean("calc") == true ? 1: 0);
					ps.setInt(8, idx);
					if(dbName.equals("oracle")){
						ps.setInt(9, maxid + idx);
					}
					ps.executeUpdate();
					return null;
				}
				
			});
		}
	}
	
	

	public Integer getCubeId() {
		return cubeId;
	}

	public void setCubeId(Integer cubeId) {
		this.cubeId = cubeId;
	}

	public String getTargetId() {
		return targetId;
	}

	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}

}
