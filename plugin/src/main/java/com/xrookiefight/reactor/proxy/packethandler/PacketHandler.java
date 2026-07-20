package com.xrookiefight.reactor.proxy.packethandler;

import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import io.netty.channel.ChannelFutureListener;
import net.raphimc.netminecraft.packet.Packet;

import java.util.List;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
public abstract class PacketHandler {

    protected final ProxyConnection proxyConnection;

    public PacketHandler(ProxyConnection proxyConnection) {
        this.proxyConnection = proxyConnection;
    }

    public boolean handleC2P(Packet packet, List<ChannelFutureListener> listeners) throws Exception {
        return true;
    }

    public boolean handleP2S(Packet packet, List<ChannelFutureListener> listeners) throws Exception {
        return true;
    }
}
