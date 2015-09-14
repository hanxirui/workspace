package com.test.comm.client;

import io.netty.bootstrap.Bootstrap;
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

import com.test.comm.handler.ClientHandler;
import com.test.comm.server.FileServer;

/**
客户上传数据时的上传通讯头
数据元 数据类型    说明
Flag    N1  上传/下载标志
IsEnc   N1  是否加密标志
IsCompress  N1  是否压缩标志
TradeCode   N2  行业代码
POSID   N8  POS机号
CorpID  N11 公司代码
SendSeq N6  发送批次号
FileSize    N8  表示文件大小
CorpName    ANS16   （营运单位简称）
LocalDateTime   YYYYMMDDhhmmss  传输时间
Reserved    ANS8    保留域
FileAbstract    H48 文件摘要（加密）
说明：
➢   上传/下载标志，‘0’-上传，‘1’-下载。
➢   是否加密标志   ‘0’- 明文     ‘1’ – 密文
➢   是否压缩标志    ‘0’- 没压缩  ‘1’ – 压缩
➢   文件摘要，采用SHA-1算法。
客户下载数据时的请求控制头
数据元 数据类型    说明
Flag    N1  上传/下载标志
IsEnc   N1  是否加密标志
IsCompress  N1  是否压缩标志
TradeCode   N2  行业代码
POSID   N8  POS机号
CorpId  N11 营运单位代码
CorpName    ANS16   （营运单位简称）
LocalDateTime   YYYYMMDDhhmmss  传输时间
ROMName ANS32   ROM程序名称
Reserved    ANS8    保留域
客户下载数据时，服务器端下传给客户下载通讯头（每一文件对应一个控制头）
数据元 数据类型    说明
FileName    ANS32   参数名称
FileVersion N10 参数版本
FileSize    N8  表示参数大小
FileAbstract    H48 文件摘要（加密）SHA-1

中间报文
数据元 数据类型    说明
FileBlock   ANS 数据块
BlockCheck  H8  数据块校验（CRC32） 
➢   将文件按通讯头指定的报文大小组成多个报文连续发送，最后一个文件块取其实际大小。
断点通知报文
数据元 数据类型    说明
FileSize    N8  已接收的文件大小（或记录数，全9表示已收到过）
   文件数通知报文
文件数通知报文
数据元 数据类型    说明
Files   N4  需下载的文件个数
DateTime    YYYYMMDDHHmmss  主机当前时间

   尾报文
尾报文
数据元 数据类型    说明
结束标志    ANS9    ***EOF***
本批次笔数   N8  
本批次金额   N12 分为单位
说明：内容暂定为 “***EOF***”
   应答报文
应答报文
数据元 数据类型    说明
RespCode    N2  应答码

上面描述的报文在发送/接收时，都有一个4字节大小的长度信息头。这4个字节表示该报的实际大小（不包括4字节本身）。
4字节报头   报文

 * <br>
 */
public class FileClient implements Runnable{

    @Value("#{configProperties['tcp_port']}")
    private int port;
    
    @Value("${tcp_port}")
    private int port1;


    public void run() {
        System.out.println("The FileServer is running.");
        System.out.println("--------------------"+ port);
        System.out.println("--------------------"+ port1);
        // Configure the server.
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
             .channel(NioServerSocketChannel.class)
             .option(ChannelOption.SO_BACKLOG, 100)
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(final SocketChannel ch) throws Exception {
                     ch.pipeline().addLast(
                             new LoggingHandler(LogLevel.INFO),
                             new StringEncoder(CharsetUtil.UTF_8),
                             new LineBasedFrameDecoder(8192),
                             new StringDecoder(CharsetUtil.UTF_8),
                             new ClientHandler());
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
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(final String[] args) throws Exception {
        new FileServer().run();
    }


    private static final class FileDownloadHandler extends SimpleChannelInboundHandler<String> {

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


