if($ == undefined){
	$ = jQuery;
}
function initdsourcetable(){
	if($("#dsourcetable").size() > 0){
		$("#dsourcetable").datagrid("load", {t:Math.random()});
		return;
	}
	var ctx = "<table id=\"dsourcetable\" title=\"数据源管理\" ><thead><tr><th data-options=\"field:'ck',checkbox:true\"></th><th data-options=\"field:'dsname',width:120\">名称</th><th data-options=\"field:'usetype',width:120,align:'center'\">类型</th><th data-options=\"field:'linktype',width:150,align:'center'\">数据库</th><th data-options=\"field:'linkurl',width:300\">链接字符串</th><th data-options=\"field:'uname',width:120,align:'center'\">用户名</th></tr></thead></table>";
	$("#optarea").html(ctx);
	$("#dsourcetable").datagrid({
		singleSelect:true,
		collapsible:false,
		pagination:false,
		border:false,
		fit:true,
		url:'DataSource!list.action',
		toolbar:[{
		  text:'新增',
		  iconCls:'icon-add',
		  handler:function(){
			newdsource(false);
		  }
		},{
		  text:'修改',
		  iconCls:'icon-edit',
		  handler:function(){
			var row = $("#dsourcetable").datagrid("getChecked");
			if(row == null || row.length == 0){
				$.messager.alert("出错了。","您还未勾选数据。", "error");
				return;
			}
			newdsource(true, row[0].id);
		  }
		},{
		  text:'删除',
		  iconCls:'icon-cancel',
		  handler:function(){
			var row = $("#dsourcetable").datagrid("getChecked");
			if(row == null || row.length == 0){
				$.messager.alert("出错了。","您还未勾选数据。", "error");
				return;
			}
			delDsource(row[0].id);
		  }
		}]
	});
}
function delDsource(dsid){
	if(confirm("是否确认删除？")){
		$.ajax({
			url:'DataSource!del.action',
			data: {dsid:dsid},
			type:'POST',
			dataType:'html',
			success:function(){
				$("#dsourcetable").datagrid("reload", {t:Math.random});
			},
			error:function(){
				msginfo("系统出错，请查看后台日志。");
			}
		});
	}
}
function newdsource(isupdate, dsid){
	var ds;
	if(isupdate){
		$.ajax({
			type:'GET',
			url:'DataSource!getModel.action',
			dataType:'JSON',
			data:{dsid:dsid},
			async:false,
			success: function(resp){
			   ds = resp;
		    }
		});
	}
	 var ctx = "<div id=\"dsource_tab\" style=\"height:auto; width:auto;\"><div title=\"JDBC\"><form id=\"datasourceform\" name=\"datasourceform\"><input type=\"hidden\" name=\"connstate\" id=\"connstate\"><div class=\"textpanel\"><span class=\"inputtext\">数据源名称：</span><input type=\"text\" id=\"dsname\" name=\"dsname\" style=\"width:400px;\" value=\""+(ds&&ds.usetype=='jdbc'?ds.dsname:"")+"\"><br/><span class=\"inputtext\">数据源类型：</span><select id=\"linktype\" name=\"linktype\" style=\"width:400px;\"><option value=\"mysql\" "+(ds&&ds.use=='jdbc'&&ds.linktype=='mysql'?"selected":"")+">MYSQL</option><option value=\"oracle\" "+(ds&&ds.usetype=='jdbc'&&ds.usetype=='jdbc'&&ds.linktype=='oracle'?"selected":"")+">ORACLE</option><option value=\"sqlserver\" "+(ds&&ds.usetype=='jdbc'&&ds.linktype=='sqlserver'?"selected":"")+">SQL Server</option></select><br/><span class=\"inputtext\">连接字符串：</span><input type=\"text\" id=\"linkurl\" name=\"linkurl\" style=\"width:400px;\" value=\""+(ds&&ds.usetype=='jdbc'?ds.linkurl:"jdbc:mysql://ip/database?useUnicode=true&characterEncoding=UTF8")+"\"><br/><span class=\"inputtext\">连接用户名：</span><input type=\"text\" id=\"linkname\" name=\"linkname\" style=\"width:400px;\" value=\""+(ds&&ds.usetype=='jdbc'?ds.uname:"")+"\"> <br/><span class=\"inputtext\">连接密码：</span><input type=\"password\" name=\"linkpwd\" id=\"linkpwd\" style=\"width:400px;\" value=\""+(ds&&ds.usetype=='jdbc'?ds.psd:"")+"\"></div></form></div><div data-options=\""+(ds&&ds.usetype=='jndi'?"selected:true":"")+"\" title=\"JNDI\"><div class=\"textpanel\"><span class=\"inputtext\">JNDI名称：</span><input type=\"text\" value=\""+(ds&&ds.usetype=='jndi'?ds.dsname:"")+"\" style=\"width:400px;\" name=\"jndiname\" id=\"jndiname\"><br/><span class=\"inputtext\">数据源类型：</span><select id=\"jndilinktype\" name=\"jndilinktype\" style=\"width:400px;\"><option value=\"mysql\" "+(ds&&ds.use=='jndi'&&ds.linktype=='mysql'?"selected":"")+">MYSQL</option><option value=\"oracle\" "+(ds&&ds.usetype=='jndi'&&ds.linktype=='oracle'?"selected":"")+">ORACLE</option><option value=\"sqlserver\" "+(ds&&ds.usetype=='jndi'&&ds.linktype=='sqlserver'?"selected":"")+">SQL Server</option></select></div></div></div>";
	$('#pdailog').dialog({
		title: isupdate ? "编辑数据源" : '创建数据源',
		width: 540,
		height: 300,
		closed: false,
		cache: false,
		modal: true,
		toolbar:null,
		content:ctx,
		buttons:[{
			text:"测试连接",
			handler:function(){
				var tab = $('#dsource_tab').tabs('getSelected');
				var index = $('#dsource_tab').tabs('getTabIndex',tab);
				if(index == 0){
					var param = $("#datasourceform").serialize();
					$.ajax({
					   type: "POST",
					   url: "DataSource!testConn.action",
					   dataType:"json",
					   data: param,
					   success: function(resp){
						   if(resp.ret == true){
							   msginfo("测试成功！", "suc");
							   $("#datasourceform #connstate").val("y");
						   }else{
							   msginfo("测试失败！<br/>"+resp.msg);
						   }
					   },
					   error:function(){
						   msginfo("测试失败！");
					   }
					});
				}else if(index == 1){
					var param = {"jndiname":$("#dsource_tab #jndiname").val()};
					$.ajax({
					   type: "POST",
					   url: "DataSource!testJNDI.action",
					   dataType:"json",
					   data: param,
					   success: function(resp){
						   if(resp.ret){
							   msginfo("测试成功！", "suc");
							   $("#datasourceform #connstate").val("y");
						   }else{
							   msginfo("测试失败！<br/>"+resp.msg);
						   }
					   },
					   error:function(){
						   msginfo("测试失败！");
					   }
					});
				}
			}
		},{
				text:'确定',
				handler:function(){
					var tab = $('#dsource_tab').tabs('getSelected');
					var index = $('#dsource_tab').tabs('getTabIndex',tab);
					if(index == 0){
						if($("#datasourceform #dsname").val() == ''){
							msginfo("请输入数据源名称！");
							$("#datasourceform #dsname").focus();
							return;
						}
						if($("#datasourceform #connstate").val() != "y"){
							msginfo("请先测试连接正常再确定！");
							return;
						}
						if(isupdate == false){
							var ds = {"linktype":$("#linktype").val(), "linkname":$("#linkname").val(), "linkpwd":$("#linkpwd").val(), "linkurl":$("#linkurl").val(),"dsname":$("#datasourceform #dsname").val(),"dsid":newGuid(),"use":"jdbc"};
							$.ajax({
								url:'DataSource!save.action',
								data: $.param(ds),
								type:'POST',
								dataType:'html',
								success:function(){
									$("#dsourcetable").datagrid("reload", {t:Math.random});
								},
								error:function(){
									msginfo("系统出错，请查看后台日志。");
								}
							});
						}else{
							var nds = {"linktype":$("#linktype").val(), "linkname":$("#linkname").val(), "linkpwd":$("#linkpwd").val(), "linkurl":$("#linkurl").val(),"dsname":$("#datasourceform #dsname").val(),"dsid":dsid,"use":"jdbc"};
							$.ajax({
								url:'DataSource!update.action',
								data:nds,
								type:'POST',
								dataType:'html',
								success:function(){
									$("#dsourcetable").datagrid("reload", {t:Math.random});
								},
								error:function(){
									msginfo("系统出错，请查看后台日志。");
								}
							});
						}
					}else if(index == 1){
						if($("#dsource_tab #jndiname").val() == ''){
							msginfo("请输入JNDI名称！");
							$("#dsource_tab #jndiname").focus();
							return;
						}
						if($("#datasourceform #connstate").val() != "y"){
							msginfo("请先测试连接正常再确定！");
							return;
						}
						if(isupdate == false){
							var ds = {"dsname":$("#dsource_tab #jndiname").val(),"linktype":$("#dsource_tab #jndilinktype").val(),"dsid":newGuid(),"use":"jndi"};
							$.ajax({
								url:'DataSource!save.action',
								data: ds,
								type:'POST',
								dataType:'html',
								success:function(){
									$("#dsourcetable").datagrid("reload", {t:Math.random});
								},
								error:function(){
									msginfo("系统出错，请查看后台日志。");
								}
							});
						}else{
							var ds = {"dsname":$("#dsource_tab #jndiname").val(),"linktype":$("#dsource_tab #jndilinktype").val(),"dsid":dsid,"use":"jndi"};
							$.ajax({
								url:'DataSource!update.action',
								data:ds,
								type:'POST',
								dataType:'html',
								success:function(){
									$("#dsourcetable").datagrid("reload", {t:Math.random});
								},
								error:function(){
									msginfo("系统出错，请查看后台日志。");
								}
							});
						}
					}
					$('#pdailog').dialog('close');
				}
			},{
				text:'取消',
				handler:function(){
					$('#pdailog').dialog('close');
				}
			}]
	});
	$("#pdailog #dsource_tab").tabs({
		fit:true,border:false
	});
	$("#pdailog #linktype").change(function(){
		var val = $(this).val();
		if(val == "mysql"){
			$("#pdailog #linkurl").val("jdbc:mysql://ip/database?useUnicode=true&characterEncoding=UTF8");
		}else if(val == "oracle"){
			$("#pdailog #linkurl").val("jdbc:oracle:thin:@ip:1521:sid");
		}else if(val == "sqlserver"){
			$("#pdailog #linkurl").val("jdbc:jtds:sqlserver://ip:1433/database");
		}
	});
}