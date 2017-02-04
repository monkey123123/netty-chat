package com.dongnao.chat.server.handler;

import com.dongnao.chat.processor.MsgProcessor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class WebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

	private MsgProcessor processor = new MsgProcessor();
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {  //(3)
		processor.sendMsg(ctx.channel(), msg.text());
	}

//	@Override
//	public void channelActive(ChannelHandlerContext ctx) throws Exception {   //(2)
//		System.out.println("Client " + ctx.channel().remoteAddress() + "上线");
//	}
//
//	@Override
//	public void channelInactive(ChannelHandlerContext ctx) throws Exception {  //(4)
//		System.out.println("Client " + ctx.channel().remoteAddress() + "掉线");
//	}

//	@Override
//	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {//异常
//		System.out.println("Client " + ctx.channel().remoteAddress() + "异常");
//		cause.printStackTrace();
//		ctx.close();
//	}

//	@Override
//	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {  //(1)
//		System.out.println("Client " + ctx.channel().remoteAddress() + "加入");
//	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {  //(5)
		System.out.println("Client " + ctx.channel().remoteAddress() + "离开");
		processor.logout(ctx.channel());
	}

}
