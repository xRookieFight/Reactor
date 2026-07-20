package com.xrookiefight.reactor.proxy.packethandler;

import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.UnknownPacket;

import java.util.List;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
public class UnexpectedPacketHandler extends PacketHandler {

    public UnexpectedPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public boolean handleC2P(Packet packet, List<ChannelFutureListener> listeners) {
        final ConnectionState connectionState = this.proxyConnection.getC2pConnectionState();

        if (connectionState == ConnectionState.HANDSHAKING) {
            throw new IllegalStateException("Unexpected packet in " + connectionState + " state: " + packet.getClass().getSimpleName());
        }

        if (packet instanceof UnknownPacket unknownPacket) {
            log.debug("Unknown C2P packet in {} state: id=0x{}", connectionState, Integer.toHexString(unknownPacket.packetId));
        }

        return true;
    }

    @Override
    public boolean handleP2S(Packet packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof UnknownPacket unknownPacket) {
            final ConnectionState connectionState = this.proxyConnection.getP2sConnectionState();
            log.debug("Unknown P2S packet in {} state: id=0x{}", connectionState, Integer.toHexString(unknownPacket.packetId));
        }

        return true;
    }
}
