package top.youlanqiang.lanproxy.proxy.listener;

import io.netty.channel.ChannelHandlerContext;

public interface ChannelStatusListener {

    void channelInactive(ChannelHandlerContext ctx);

}
