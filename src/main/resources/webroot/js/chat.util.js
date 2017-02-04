//扩展一个date对象的format方法
Date.prototype.format = function(format){ 
	var o = { 
	"M+" : this.getMonth()+1, //月 
	"d+" : this.getDate(), //日
	"h+" : this.getHours(), //时
	"m+" : this.getMinutes(), //分
	"s+" : this.getSeconds(), //秒
	"q+" : Math.floor((this.getMonth()+3)/3), //刻
	"S" : this.getMilliseconds() //毫秒
	} 
	
	if(/(y+)/.test(format)) { 
		format = format.replace(RegExp.$1, (this.getFullYear()+"").substr(4 - RegExp.$1.length)); 
	} 
	
	for(var k in o) { 
		if(new RegExp("("+ k +")").test(format)) { 
			format = format.replace(RegExp.$1, RegExp.$1.length==1 ? o[k] : ("00"+ o[k]).substr((""+ o[k]).length)); 
		} 
	} 
	return format; 
};


$(document).ready(function(){
	//老师写代码的套路，一般是先写思路，搭框架，然后，再填空
	
	var host = location.href.replace(/http:\/\//i,"");
	//写一个客户端对象
	window.CHAT = {
		serverAddr:"ws://" + host + "im",
		//保存客户端Socket对象
		socket : null,
		//保存用户的昵称
		nickname:null,
		//登录方法
		login:function(){
			$("#error-msg").empty();
			var _reg = /^\S{1,10}/;
			var nickname = $("#nickname").val();
			if(!_reg.test($.trim(nickname))){
				$("#error-msg").html("昵称长度必须在10个字以内");
				return false;
			}
			$("#nickname").val("");
			$("#loginbox").hide();
			$("#chatbox").show();
			this.init(nickname);
		},
		//登出
		logout:function(){
			location.reload();
		},
		//将屏幕滚动到最下方
		scrollToBottom:function(){
			window.scrollTo(0,$("#onlinemsg")[0].scrollHeight);
		},
		//添加系统提示
		//动态创建一个HTML元素
		addSystemTip:function(c){
			var html = "";
	        html += '<div class="msg-system">';
	        html += c;
	        html += '</div>';
	        var section = document.createElement('section');
	        section.className = 'system J-mjrlinkWrap J-cutMsg';
	        section.innerHTML = html;
	        
	        $("#onlinemsg").append(section);
		},
		//发送聊天消息
		sendText:function(){
			var input = $("#send-message");
			if($.trim(input.html()) == ""){ return; }
			
			if(CHAT.socket.readyState == WebSocket.OPEN){
				var msg = "[CHAT][" + new Date().getTime() +"][" + CHAT.nickname + "] - " + input.html().replace(/\n/ig,"<br/>");
				CHAT.socket.send(msg);
				input.empty();
				input.focus();
			}else{
				CHAT.addSystemTip("你已处于离线状态，无法发送消息");
			}
		},
		//选择表情
		openFace:function(){
			var box = $("#face-box");
			//避免重复打开表情选择框
			if(box.hasClass("open")){
				box.hide();
				box.removeClass("open");
				return;
			}
			box.addClass("open");
			box.show();
			
			if(box.html() != ""){ return; }
			
			var faceIcon = "";
			for(var i = 1; i <= 130; i ++){
				var path = '/images/face/' + i + ".gif";
				faceIcon += '<span class="face-item" onclick="CHAT.selectFace(\'' + path + '\')">'
				faceIcon += '<img src="' + path + '"/>';
				faceIcon += '</span>';
			}
			box.html(faceIcon);
		},
		//选择一张图片
		selectFace:function(path){
			var faceBox = $("#face-box");
			faceBox.hide();
			faceBox.removeClass("open");
			var img = '<img src="' + path + '"/>';
			$("#send-message").append(img);
			$("#send-message").focus();
		},
		//送鲜花
		sendFlower:function(){
			if(CHAT.socket.readyState == WebSocket.OPEN){
				var msg = "[FLOWER][" + new Date().getTime() +"][" + CHAT.nickname +"]";
				CHAT.socket.send(msg);
			}else{
				CHAT.addSystemTip("你已处于离线状态，无法发送鲜花");
			}
		},
		//初始化
		init :function(nickname){
			
			//这是初始化赋值
			CHAT.nickname = nickname;
			$("#shownikcname").html(nickname);
			
			var message = $("#send-message");
			//自动获得焦点
			message.focus();
			//按Ctrl + 回车自动发送消息
			message.keydown(function(e){
				if((e.ctrlKey && e.which == 13) || e.which == 10){
					CHAT.sendText();
				}
			});
			
			//将消息添加到聊天面板
			//专门处理服务端发来的消息
			var appendToPanel = function(msg){
				//用正则来解析自定义协议
				var regx = /^\[(.*)\](\s\-\s(.*))?/g;
				//分组，  标签,消息体
				var group = '',header = "",content = "",cmd = "",time = 0, sender = "";
				while(group = regx.exec(msg)){
					header = group[1];
					content = group[3];
				}
				//alert(header + "," + content);
				var headers = header.split("][");
				cmd = headers[0];
				time = headers[1];
				sender = headers[2];
				
				if(cmd == "SYSTEM"){
					var online = headers[2];
					$("#onlinecount").html("" + online);
					
					CHAT.addSystemTip(content);
				}else if(cmd == "CHAT"){
					//解析消息的发送时间
					var date = new Date(parseInt(time));
					//添加一个系统时间标签
					CHAT.addSystemTip('<span class="time-label">' + date.format("hh:mm:ss") + '</span>');
					
					//把聊天内容 加到聊天面板中去
					var isme = (sender == "you") ? true : false;
			        var contentDiv = '<div>' + content + '</div>';
			        var usernameDiv = '<span>' + sender + '</span>';

			        var section = document.createElement('section');
			        //用来区分聊天消息是自己发的，还是别人发的
			        //显示出来的样式不一样而已
			        if (isme) {
			            section.className = 'user';
			            section.innerHTML = contentDiv + usernameDiv;
			        } else {
			            section.className = 'service';
			            section.innerHTML = usernameDiv + contentDiv;
			        }
			        
			        //最后把创建好的元素添加到聊天面板
			        $("#onlinemsg").append(section);
				}else if(cmd == "FLOWER"){
					//先出现一个提示
					CHAT.addSystemTip(content);
					
					//触发一个送鲜花的特效
					$(document).snowfall('clear');
					$(document).snowfall({
						image:"/images/face/50.gif",
						flakeCount:60,
						minSize:20,
						maxSize:40
					});
					window.flowerTimer = window.setTimeout(function(){
						$(document).snowfall('clear');
						window.clearTimeout(flowerTimer);
					},5000);
				}
				
				//将屏幕滚动到最后
				CHAT.scrollToBottom();
			};
			
			
			//判断浏览器是否支持WebSocket协议
			if(!window.WebSocket){
				window.WebSocket = window.MozWebSocket;
			}
			
			if(window.WebSocket){
				CHAT.socket = new WebSocket(CHAT.serverAddr);
				CHAT.socket.onmessage = function(e){
					appendToPanel(e.data);
					//console.log("获取服务器发来的消息" + e.data);
				};
				CHAT.socket.onopen = function(e){
					CHAT.socket.send("[LOGIN][" + new Date().getTime() + "][" + CHAT.nickname +"]");
				}
				CHAT.socket.onclose = function(e){
					console.log("服务器关闭");
				}
			}else{
				alert("您的浏览器不支持WebSocket！");
			}
			
			
		}
			
	};
	
});