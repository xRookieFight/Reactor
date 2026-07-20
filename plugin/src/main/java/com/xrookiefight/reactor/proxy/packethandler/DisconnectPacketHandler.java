package com.xrookiefight.reactor.proxy.packethandler;

import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.impl.configuration.S2CConfigDisconnectPacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginDisconnectPacket;
import net.raphimc.netminecraft.packet.impl.play.S2CPlayDisconnectPacket;

import java.util.List;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
public class DisconnectPacketHandler extends PacketHandler {

    public DisconnectPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public boolean handleP2S(Packet packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof S2CLoginDisconnectPacket disconnectPacket) {
            log.info("Server disconnected client during login: {}", disconnectPacket.reason);
            listeners.add(ChannelFutureListener.CLOSE);
        } else if (packet instanceof S2CConfigDisconnectPacket disconnectPacket) {
            log.info("Server disconnected client during configuration: {}", disconnectPacket.reason);
            listeners.add(ChannelFutureListener.CLOSE);
        } else if (packet instanceof S2CPlayDisconnectPacket disconnectPacket) {
            log.info("Server disconnected client during play: {}", disconnectPacket.reason);
            listeners.add(ChannelFutureListener.CLOSE);
        }
        return true;
    }
}
