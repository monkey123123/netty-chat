package com.dongnao.chat.server;

import java.io.IOException;

import com.dongnao.chat.protocol.IMDecoder;
import com.dongnao.chat.protocol.IMEncoder;
import com.dongnao.chat.server.handler.HttpHandler;
import com.dongnao.chat.server.handler.ImpHandler;
import com.dongnao.chat.server.handler.WebSocketHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * 聊天服务端
 * @author Tom
 *
 */
public class ChatServer{
	
	private int port = 8080;//服务器启动的监听端口
	
    public void start() {
    	
    	//主从模式、创建主线程
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        //创建子线程，工作线程
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
        	
        	//创建Netty Socket Server 
            ServerBootstrap b = new ServerBootstrap();
            //默认分配1024个工作线程
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 1024)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                	//获取 工作流，流水线,pipeline
                	ChannelPipeline pipeline = ch.pipeline();
                	
                	
                	//粘包拆包
//                	pipeline.addLast(new LineBasedFrameDecoder(1024));
                	
                	//======== 对自定义IMP协议的支持  ==========
                	pipeline.addLast(new IMDecoder());
                	pipeline.addLast(new IMEncoder());
                	pipeline.addLast(new ImpHandler());
                	
                	
                	//======== 对HTTP协议的支持  ==========
                	//Http请求解码器
                	pipeline.addLast(new HttpServerCodec());
                	//主要就是将一个http请求或者响应变成一个FullHttpRequest对象
                	pipeline.addLast(new HttpObjectAggregator(64 * 1024));
                	//这个是用来处理文件流
                	pipeline.addLast(new ChunkedWriteHandler());
                	//处理HTTP请求的业务逻辑
                	pipeline.addLast(new HttpHandler());
                	
                	
                	//======== 对WebSocket协议的支持  ==========
                	//加上这个Handler就已经能够解析WebSocket请求了
                	//相当于WebSocket解码器
                	//im是为了和http请求区分开来，以im开头的请求都有websocket来解析
                	pipeline.addLast(new WebSocketServerProtocolHandler("/im"));
                	//实现处理WebSocket逻辑的Handler
                	pipeline.addLast(new WebSocketHandler());
                	
                }
            });
            //采用同步的方式监听客户端连接
            //NIO同步非阻塞
            ChannelFuture f = b.bind(this.port).sync();
            System.out.println("服务已启动,监听端口" + this.port);
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
    
    
    public static void main(String[] args) throws IOException{
        new ChatServer().start();
    }
}
