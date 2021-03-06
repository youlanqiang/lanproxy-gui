package top.youlanqiang.lanproxy.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import top.youlanqiang.lanproxy.proxy.common.container.Container;
import top.youlanqiang.lanproxy.proxy.common.container.ContainerHelper;
import top.youlanqiang.lanproxy.proxy.handlers.ClientChannelHandler;
import top.youlanqiang.lanproxy.proxy.handlers.RealServerChannelHandler;
import top.youlanqiang.lanproxy.proxy.listener.ChannelStatusListener;
import top.youlanqiang.lanproxy.proxy.protocol.IdleCheckHandler;
import top.youlanqiang.lanproxy.proxy.protocol.ProxyMessage;
import top.youlanqiang.lanproxy.proxy.protocol.ProxyMessageDecoder;
import top.youlanqiang.lanproxy.proxy.protocol.ProxyMessageEncoder;


import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.util.Arrays;

public class ProxyClientContainer implements Container, ChannelStatusListener {


    private String serverHost;


    public Integer serverPort;

    public String clientKey;

    private static final int MAX_FRAME_LENGTH = 1024 * 1024;

    private static final int LENGTH_FIELD_OFFSET = 0;

    private static final int LENGTH_FIELD_LENGTH = 4;

    private static final int INITIAL_BYTES_TO_STRIP = 0;

    private static final int LENGTH_ADJUSTMENT = 0;

    private NioEventLoopGroup workerGroup;

    private Bootstrap bootstrap;

    private Bootstrap realServerBootstrap;


    private SSLContext sslContext;

    private long sleepTimeMill = 1000;

    public ProxyClientContainer(String serverHost, Integer serverPort, String clientKey) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.clientKey = clientKey;
        workerGroup = new NioEventLoopGroup();
        realServerBootstrap = new Bootstrap();
        realServerBootstrap.group(workerGroup);
        realServerBootstrap.channel(NioSocketChannel.class);
        realServerBootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new RealServerChannelHandler());
            }
        });

        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch) throws Exception {

                ch.pipeline().addLast(new ProxyMessageDecoder(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP));
                ch.pipeline().addLast(new ProxyMessageEncoder());
                ch.pipeline().addLast(new IdleCheckHandler(IdleCheckHandler.READ_IDLE_TIME, IdleCheckHandler.WRITE_IDLE_TIME - 10, 0));
                ch.pipeline().addLast(new ClientChannelHandler(serverHost,serverPort,clientKey,realServerBootstrap, bootstrap, ProxyClientContainer.this));
            }
        });
    }

    @Override
    public void start() {
        connectProxyServer();
    }

    private ChannelHandler createSslHandler(SSLContext sslContext) {
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(true);
        return new SslHandler(sslEngine);
    }

    private void connectProxyServer() {

        bootstrap.connect(serverHost, serverPort).addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {

                    // 连接成功，向服务器发送客户端认证信息（clientKey）
                    ClientChannelMannager.setCmdChannel(future.channel());
                    ProxyMessage proxyMessage = new ProxyMessage();
                    proxyMessage.setType(ProxyMessage.C_TYPE_AUTH);
                    proxyMessage.setUri(clientKey);
                    future.channel().writeAndFlush(proxyMessage);
                    sleepTimeMill = 1000;
                } else {
                    // 连接失败，发起重连
                    reconnectWait();
                    connectProxyServer();
                }
            }
        });
    }

    @Override
    public void stop() {
        workerGroup.shutdownGracefully();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        reconnectWait();
        connectProxyServer();
    }

    private void reconnectWait() {
        try {
            if (sleepTimeMill > 60000) {
                sleepTimeMill = 1000;
            }

            synchronized (this) {
                sleepTimeMill = sleepTimeMill * 2;
                wait(sleepTimeMill);
            }
        } catch (InterruptedException e) {
        }
    }

    public static void start(String serverHost, Integer serverPort, String clientKey) {
        ContainerHelper.start(Arrays.asList(new Container[] { new ProxyClientContainer(
                serverHost,serverPort,clientKey
        ) }));
    }


}
