package com.dongnao.chat.server.handler;

import com.dongnao.chat.processor.MsgProcessor;
import com.dongnao.chat.protocol.IMMessage;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 处理自定义协议的逻辑
 * @author Tom
 *
 */
public class ImpHandler extends SimpleChannelInboundHandler<IMMessage> {

	MsgProcessor processor = new MsgProcessor();
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, IMMessage msg) throws Exception {
		processor.sendMsg(ctx.channel(), msg);
	}

}
