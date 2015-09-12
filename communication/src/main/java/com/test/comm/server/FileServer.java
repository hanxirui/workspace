package com.test.comm.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.FileInputStream;

import org.springframework.beans.factory.annotation.Value;

/**
 * 文件服务器，接收路径请求，发送文件内容.
 */
public class FileServer implements Runnable{

    @Value("#{configProperties['tcp_port']}")
    private int port;
    
    @Value("${tcp_port}")
    private int port1;


    public void run() {
        System.out.println("The FileServer is running.");
        System.out.println("--------------------"+ port);
        System.out.println("--------------------"+ port1);
        // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .option(ChannelOption.SO_BACKLOG, 100)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(final SocketChannel ch) throws Exception {
                     ch.pipeline().addLast(
                             new StringEncoder(CharsetUtil.UTF_8),
                             new LineBasedFrameDecoder(8192),
                             new StringDecoder(CharsetUtil.UTF_8),
                             new FileHandler());
                 }
             });

            // Start the server.
            ChannelFuture f = b.bind(port).sync();

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(final String[] args) throws Exception {
        new FileServer().run();
    }

    private static final class FileHandler extends SimpleChannelInboundHandler<String> {

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }

        @Override
        protected void channelRead0(final ChannelHandlerContext ctx, final String msg) throws Exception {

            File file = new File(msg);
            if (file.exists()) {
                if (!file.isFile()) {
                    ctx.writeAndFlush("Not a file: " + file + '\n');
                    return;
                }
                ctx.write(file + " " + file.length() + '\n');
                FileInputStream fis = new FileInputStream(file);
                FileRegion region = new DefaultFileRegion(fis.getChannel(), 0, file.length());
                ctx.write(region);
                ctx.writeAndFlush("\n");
                fis.close();
            } else {
                ctx.writeAndFlush("File not found: " + file + '\n');
            }
        
            
        }
    }
}
