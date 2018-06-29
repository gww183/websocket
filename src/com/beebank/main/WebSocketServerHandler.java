package com.beebank.main;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;
import java.util.Date;

import javax.swing.text.AbstractDocument.Content;

public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {
	
	private WebSocketServerHandshaker handshaker;
	
	@Override
	protected void channelRead0(ChannelHandlerContext context, Object msg)
			throws Exception {
		// 传统http接入
		if (msg instanceof FullHttpRequest) {
			handlerHttpRequest(context, (FullHttpRequest) msg);
		} else if (msg instanceof WebSocketFrame) {
			handleWebSocketFrame(context, (WebSocketFrame) msg);
		}

	}

	private void handleWebSocketFrame(ChannelHandlerContext context,
			WebSocketFrame msg) {
		// 判断是否是关闭链路指令
		if(msg instanceof CloseWebSocketFrame) {
			System.out.println("close");
			handshaker.close(context.channel(), (CloseWebSocketFrame) msg.retain());
			return;
		}
		
		// 判断是否发送ping
		if(msg instanceof PingWebSocketFrame) {
			System.out.println("ping");
			context.channel().write(new PongWebSocketFrame(msg.content().retain()));
			return ;
		}
		
		// 仅仅支持文本信息
		if(!(msg instanceof TextWebSocketFrame)) {
			throw new UnsupportedOperationException(String.format("%s from type not support", msg.getClass().getName()));
		}
		
		String request = ((TextWebSocketFrame) msg).text();
		
		context.channel().write(new TextWebSocketFrame(request + ", 欢迎使用 netty websocket 服务器 ，现在时刻 ：" + new Date().toString()));
	}

	private void handlerHttpRequest(ChannelHandlerContext context, FullHttpRequest msg) {
		if (!msg.getDecoderResult().isSuccess() || (!"websocket".equals(msg.headers().get("Upgrade")))) {
			sendHttpResponse(context, msg, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
			return;
		}

		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory("ws://localhost:8080/websocket", null, false);
		handshaker = wsFactory.newHandshaker(msg);
		if (handshaker == null) {
			WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(context.channel());
		} else {
			handshaker.handshake(context.channel(), msg);
		}
	}

	@SuppressWarnings("static-access")
	private void sendHttpResponse(ChannelHandlerContext context,
			FullHttpRequest req, DefaultFullHttpResponse defaultFullHttpResponse) {
		if (defaultFullHttpResponse.getStatus().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(defaultFullHttpResponse.getStatus().toString(), CharsetUtil.UTF_8);
			defaultFullHttpResponse.content().writeBytes(buf);
			buf.release();
			defaultFullHttpResponse.headers().setContentLength(defaultFullHttpResponse,
					defaultFullHttpResponse.content().readableBytes());
		}
		
		ChannelFuture future = context.channel().write(defaultFullHttpResponse);
		if(!HttpHeaders.isKeepAlive(req) || defaultFullHttpResponse.getStatus().code() != 200) {
			future.addListener(ChannelFutureListener.CLOSE);
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
		ctx.close();
	}

}
