package com.xrookiefight.reactor.proxy.proxy2server;

import com.google.common.collect.Lists;
import com.xrookiefight.reactor.Reactor;
import com.xrookiefight.reactor.proxy.packethandler.PacketHandler;
import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import com.xrookiefight.reactor.proxy.util.ExceptionUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.packet.Packet;

import java.util.List;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
public class Proxy2ServerHandler extends SimpleChannelInboundHandler<Packet> {

    private ProxyConnection proxyConnection;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.proxyConnection = ProxyConnection.fromChannel(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        if (Reactor.getInstance().getReactorConfig().isLogStatusRequests() || this.proxyConnection.getP2sConnectionState() != ConnectionState.STATUS) {
            log.info("Connection to server closed for client: {}", this.proxyConnection.getClientAddressString());
        }

        try {
            this.proxyConnection.getC2P().close();
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        if (this.proxyConnection.isClosed()) return;

        final List<ChannelFutureListener> listeners = Lists.newArrayList(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        for (PacketHandler packetHandler : this.proxyConnection.getPacketHandlers()) {
            if (!packetHandler.handleP2S(packet, listeners)) {
                return;
            }
        }

        this.proxyConnection.getC2P().writeAndFlush(packet).addListeners(listeners.toArray(new ChannelFutureListener[0]));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ExceptionUtil.handleNettyException(ctx, cause, this.proxyConnection, false);
    }
}
