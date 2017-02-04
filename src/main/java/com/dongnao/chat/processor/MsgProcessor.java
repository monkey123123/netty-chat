package com.dongnao.chat.processor;

import com.alibaba.fastjson.JSONObject;
import com.dongnao.chat.protocol.IMDecoder;
import com.dongnao.chat.protocol.IMEncoder;
import com.dongnao.chat.protocol.IMMessage;
import com.dongnao.chat.protocol.IMP;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * 专门处理消息相关逻辑（消息分发）
 * @author Tom
 *
 */
public class MsgProcessor {
	
	//记录在线用户
	private static ChannelGroup onlineUsers = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	
	private IMDecoder decoder = new IMDecoder();
	private IMEncoder encoder = new IMEncoder();
	
	//定义一些自定义属性
	private final AttributeKey<String> NICK_NAME = AttributeKey.valueOf("nickName");
	private final AttributeKey<String> IP_ADDR = AttributeKey.valueOf("ipAddr");
	private final AttributeKey<JSONObject> ATTRS = AttributeKey.valueOf("attrs");
	
	
	//都是一些设计思想
	
	/**
	 * 如果有用户退出的话，就从容器中将这个用户去掉，并且将在线人数减1
	 * @param client
	 */
	public void logout(Channel client){
		if(getNickName(client) == null){ return; }
		for (Channel channel : onlineUsers) {
			IMMessage msg = new IMMessage(IMP.SYSTEM.getName(),sysTime(),onlineUsers.size(),getNickName(client) + "退出");
			String content = encoder.encode(msg);
			channel.writeAndFlush(new TextWebSocketFrame(content));
		}
		onlineUsers.remove(client);
	}
	
	
	
	/**
	 * 获取客户端的昵称
	 * @param client
	 * @return
	 */
	public String getNickName(Channel client){
		return client.attr(NICK_NAME).get();
	}
	
	/**
	 * 获取IP地址
	 * @param client
	 * @return
	 */
	public String getIpAddr(Channel client){
		return client.remoteAddress().toString().replaceFirst("/", "");
	}
	
	
	/**
	 * 获得扩展属性
	 * @param client
	 * @return
	 */
	public JSONObject getAttrs(Channel client){
		try{ 
			return client.attr(ATTRS).get();
		}catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * 往扩展属性添加自定义key
	 * @param client
	 * @param key
	 * @param value
	 */
	public void setAttrs(Channel client,String key,Object value){
		JSONObject extendAtrrs = client.attr(ATTRS).get();
		if(null == extendAtrrs){
			extendAtrrs = new JSONObject();
		}
		extendAtrrs.put(key, value);
		client.attr(ATTRS).set(extendAtrrs);
	}
	
	
	//把消息分发到每一个连接到服务器上的客户端
	/**
	 * 将消息分发到所有的在线用户
	 * @param client
	 * @param msg
	 */
	public void sendMsg(Channel client,IMMessage msg){
		//分发消息主要是分发到我们的网页里面去
		if(null == msg){ return;}
		
		//开始分发了吧,声明一个容器，来保存所有的在线用户
		if(IMP.LOGIN.getName().equals(msg.getCmd())){
			//我们自己加的一些东西放入到自定的扩展属性里面去
			client.attr(NICK_NAME).getAndSet(msg.getSender());
			client.attr(IP_ADDR).getAndSet(getIpAddr(client));
			
			onlineUsers.add(client);
			
			//扫描所有的在线用户，通知某某上线了
			for (Channel channel : onlineUsers) {
				if(channel != client){
					msg = new IMMessage(IMP.SYSTEM.getName(),sysTime(),onlineUsers.size(),getNickName(client) + "加入");
				}else{
					msg = new IMMessage(IMP.SYSTEM.getName(),sysTime(),onlineUsers.size(),"已与服务器建立连接");
				}
				String content = encoder.encode(msg);
				channel.writeAndFlush(new TextWebSocketFrame(content));
			}
			
			
		//其他指令的解析
		}else if(IMP.CHAT.getName().equals(msg.getCmd())){
			
			for (Channel channel : onlineUsers) {
				if(channel != client){
					msg.setSender(getNickName(client));
				}else{
					msg.setSender("you");
				}
				msg.setTime(sysTime());
				String content = encoder.encode(msg);
				channel.writeAndFlush(new TextWebSocketFrame(content));
			}
			
		}else if(IMP.FLOWER.getName().equals(msg.getCmd())){
			
			//非正常的情况下，就频繁刷花，导致整个屏幕一直是鲜花特效
			//影响聊天效果
			//这时候，我们就要加上一个限制，规定1分钟之内，每个人只能刷一次鲜花
			JSONObject attrs = getAttrs(client);
			
			//如果为空，就表示这个人从来没有送过鲜花
			//处女送
			if(null != attrs){
				//就开始判断上次送花时间
				long lastFlowerTime = attrs.getLongValue("lastFlowerTime");
				int secends = 60;//60秒之内不能重复送花
				long sub = sysTime() - lastFlowerTime;
				if(sub < 1000 * secends){
					msg.setSender("you");
					msg.setCmd(IMP.SYSTEM.getName());
					msg.setOnline(onlineUsers.size());
					msg.setContent("您送鲜花太频繁,请" + (secends - Math.round(sub/1000) ) + "秒后再试");
					
					String content = encoder.encode(msg);
					client.writeAndFlush(new TextWebSocketFrame(content));
					
					return;
				}
			}
			
			
			//正常的送花流程
			for (Channel channel : onlineUsers) {
				if(channel != client){
					msg.setSender(getNickName(client));
					msg.setContent(getNickName(client) + "送来一波鲜花");
				}else{
					msg.setSender("you");
					msg.setContent("你给大家送了一波鲜花");
					setAttrs(client, "lastFlowerTime", sysTime());
				}
				msg.setTime(sysTime());
				String content = encoder.encode(msg);
				channel.writeAndFlush(new TextWebSocketFrame(content));
			}
			
		}else{
			
		}
		
	}
	/**
	 * 将消息分发到所有的在线用户(重载+1)
	 * @param client
	 * @param msg
	 */
	public void sendMsg(Channel client,String msg){
		sendMsg(client, decoder.decode(msg));
	}
	
	/**
	 * 获取系统时间
	 * @return
	 */
	public long sysTime(){
		return System.currentTimeMillis();
	}
	
	
}
