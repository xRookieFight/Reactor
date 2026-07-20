package com.xrookiefight.reactor.proxy.packethandler;

import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import net.raphimc.netminecraft.packet.UnknownPacket;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
class PacketHandlerTest {

    @Test
    void defaultHandlersForwardPackets() throws Exception {
        PacketHandler handler = new PacketHandler(mock(ProxyConnection.class)) {
        };

        assertTrue(handler.handleC2P(new UnknownPacket(0), new ArrayList<>()));
        assertTrue(handler.handleP2S(new UnknownPacket(0), new ArrayList<>()));
    }
}
