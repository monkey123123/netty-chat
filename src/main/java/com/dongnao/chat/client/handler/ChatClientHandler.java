package com.dongnao.chat.client.handler;

import java.util.Scanner;

import com.dongnao.chat.protocol.IMMessage;
import com.dongnao.chat.protocol.IMP;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ChatClientHandler extends ChannelInboundHandlerAdapter {
	private String nickName;
	private ChannelHandlerContext ctx;
	
	
	public ChatClientHandler(String nickName) {
		this.nickName = nickName;
	}
	
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		this.ctx = ctx;
		//首先要登录吧
		IMMessage message = new IMMessage(IMP.LOGIN.getName(), System.currentTimeMillis(), this.nickName);
		sendMsg(message);
		System.out.println("成功连接至服务器，已执行登录动作");
		session();
	}

	
	/**
	 * 从控制输入消息内容，搞一个独立的线程
	 */
	private void session(){
		new Thread(){
			
			public void run(){
				System.out.println(nickName + ",你好，请在控制台输入消息内容");
				IMMessage msg = null;
				Scanner sc = new Scanner(System.in);
				do {
					String content = sc.nextLine();
					if("exit".equals(content)){
						msg = new IMMessage(IMP.LOGOUT.getName(), System.currentTimeMillis(), nickName);
					}else{
						msg = new IMMessage(IMP.CHAT.getName(), System.currentTimeMillis(), nickName,content);
					}
				} while (sendMsg(msg));
				sc.close();
			}
			
		}.start();
	}
	
	/**
	 * 往服务器端发送消息
	 * @param msg
	 * @return
	 */
	private boolean sendMsg(IMMessage msg){
		ctx.channel().writeAndFlush(msg);
		System.out.println("消息已发送至服务器,请继续输入");
		return msg.getCmd().equals(IMP.LOGOUT) ? false : true;
	}
	
}
