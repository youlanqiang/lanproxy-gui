package top.youlanqiang.lanproxy.proxy.handlers;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import top.youlanqiang.lanproxy.proxy.ClientChannelMannager;
import top.youlanqiang.lanproxy.proxy.listener.ChannelStatusListener;
import top.youlanqiang.lanproxy.proxy.listener.ProxyChannelBorrowListener;
import top.youlanqiang.lanproxy.proxy.protocol.Constants;
import top.youlanqiang.lanproxy.proxy.protocol.ProxyMessage;


/**
 *
 * @author fengfei
 *
 */
public class ClientChannelHandler extends SimpleChannelInboundHandler<ProxyMessage> {


    private String serverHost;

    private Integer serverPort;

    private String clientKey;

    private Bootstrap bootstrap;

    private Bootstrap proxyBootstrap;

    private ChannelStatusListener channelStatusListener;

    public ClientChannelHandler(String serverHost, Integer serverPort, String clientKey, Bootstrap bootstrap, Bootstrap proxyBootstrap, ChannelStatusListener channelStatusListener) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.clientKey = clientKey;
        this.bootstrap = bootstrap;
        this.proxyBootstrap = proxyBootstrap;
        this.channelStatusListener = channelStatusListener;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage proxyMessage) throws Exception {

        switch (proxyMessage.getType()) {
            case ProxyMessage.TYPE_CONNECT:
                handleConnectMessage(ctx, proxyMessage);
                break;
            case ProxyMessage.TYPE_DISCONNECT:
                handleDisconnectMessage(ctx, proxyMessage);
                break;
            case ProxyMessage.P_TYPE_TRANSFER:
                handleTransferMessage(ctx, proxyMessage);
                break;
            default:
                break;
        }
    }

    private void handleTransferMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        Channel realServerChannel = ctx.channel().attr(Constants.NEXT_CHANNEL).get();
        if (realServerChannel != null) {
            ByteBuf buf = ctx.alloc().buffer(proxyMessage.getData().length);
            buf.writeBytes(proxyMessage.getData());
            realServerChannel.writeAndFlush(buf);
        }
    }

    private void handleDisconnectMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        Channel realServerChannel = ctx.channel().attr(Constants.NEXT_CHANNEL).get();

        if (realServerChannel != null) {
            ctx.channel().attr(Constants.NEXT_CHANNEL).remove();
            ClientChannelMannager.returnProxyChanel(ctx.channel());
            realServerChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void handleConnectMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        final Channel cmdChannel = ctx.channel();
        final String userId = proxyMessage.getUri();
        String[] serverInfo = new String(proxyMessage.getData()).split(":");
        String ip = serverInfo[0];
        int port = Integer.parseInt(serverInfo[1]);
        bootstrap.connect(ip, port).addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {

                // 连接后端服务器成功
                if (future.isSuccess()) {
                    final Channel realServerChannel = future.channel();


                    realServerChannel.config().setOption(ChannelOption.AUTO_READ, false);

                    // 获取连接
                    ClientChannelMannager.borrowProxyChanel(serverHost,serverPort,proxyBootstrap, new ProxyChannelBorrowListener() {

                        @Override
                        public void success(Channel channel) {
                            // 连接绑定
                            channel.attr(Constants.NEXT_CHANNEL).set(realServerChannel);
                            realServerChannel.attr(Constants.NEXT_CHANNEL).set(channel);

                            // 远程绑定
                            ProxyMessage proxyMessage = new ProxyMessage();
                            proxyMessage.setType(ProxyMessage.TYPE_CONNECT);
                            proxyMessage.setUri(userId + "@" + clientKey);
                            channel.writeAndFlush(proxyMessage);

                            realServerChannel.config().setOption(ChannelOption.AUTO_READ, true);
                            ClientChannelMannager.addRealServerChannel(userId, realServerChannel);
                            ClientChannelMannager.setRealServerChannelUserId(realServerChannel, userId);
                        }

                        @Override
                        public void error(Throwable cause) {
                            ProxyMessage proxyMessage = new ProxyMessage();
                            proxyMessage.setType(ProxyMessage.TYPE_DISCONNECT);
                            proxyMessage.setUri(userId);
                            cmdChannel.writeAndFlush(proxyMessage);
                        }
                    });

                } else {
                    ProxyMessage proxyMessage = new ProxyMessage();
                    proxyMessage.setType(ProxyMessage.TYPE_DISCONNECT);
                    proxyMessage.setUri(userId);
                    cmdChannel.writeAndFlush(proxyMessage);
                }
            }
        });
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel realServerChannel = ctx.channel().attr(Constants.NEXT_CHANNEL).get();
        if (realServerChannel != null) {
            realServerChannel.config().setOption(ChannelOption.AUTO_READ, ctx.channel().isWritable());
        }

        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        // 控制连接
        if (ClientChannelMannager.getCmdChannel() == ctx.channel()) {
            ClientChannelMannager.setCmdChannel(null);
            ClientChannelMannager.clearRealServerChannels();
            channelStatusListener.channelInactive(ctx);
        } else {
            // 数据传输连接
            Channel realServerChannel = ctx.channel().attr(Constants.NEXT_CHANNEL).get();
            if (realServerChannel != null && realServerChannel.isActive()) {
                realServerChannel.close();
            }
        }

        ClientChannelMannager.removeProxyChanel(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

}