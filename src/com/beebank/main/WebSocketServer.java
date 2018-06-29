package com.beebank.main;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class WebSocketServer {

	public static void main(String[] arg) {
		WebSocketServer socketServer = new WebSocketServer();
		socketServer.run(8080);
	}
	
	public void run(int port) {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		try { 
		     	ServerBootstrap serverBootStrap = new ServerBootstrap();
			serverBootStrap.group(bossGroup, workerGroup);
			serverBootStrap.channel(NioServerSocketChannel.class);
			serverBootStrap.option(ChannelOption.SO_BACKLOG, 1024);
			serverBootStrap.handler(new LoggingHandler(LogLevel.INFO));
			serverBootStrap.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel socketChannel)
								throws Exception {
							socketChannel.pipeline().addLast("http-codec",
									new HttpServerCodec());
							socketChannel.pipeline().addLast("aggregator",
									new HttpObjectAggregator(65536));
							socketChannel.pipeline().addLast("http-chunked",
									new ChunkedWriteHandler());
							socketChannel.pipeline().addLast("handler",
									new WebSocketServerHandler());
						}
					});

			ChannelFuture future = serverBootStrap.bind(port).sync();
			future.channel().closeFuture().sync();
		} catch (Exception e) {

		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
}
