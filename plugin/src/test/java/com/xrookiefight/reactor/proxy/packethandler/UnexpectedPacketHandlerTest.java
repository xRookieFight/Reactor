package com.xrookiefight.reactor.proxy.packethandler;

import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.packet.UnknownPacket;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
class UnexpectedPacketHandlerTest {

    @Test
    void handshakingStateRejectsPackets() {
        ProxyConnection connection = mock(ProxyConnection.class);
        when(connection.getC2pConnectionState()).thenReturn(ConnectionState.HANDSHAKING);
        UnexpectedPacketHandler handler = new UnexpectedPacketHandler(connection);

        assertThrows(IllegalStateException.class, () -> handler.handleC2P(new UnknownPacket(0), new ArrayList<>()));
    }

    @Test
    void playStateForwardsUnknownPackets() throws Exception {
        ProxyConnection connection = mock(ProxyConnection.class);
        when(connection.getC2pConnectionState()).thenReturn(ConnectionState.PLAY);
        UnexpectedPacketHandler handler = new UnexpectedPacketHandler(connection);

        assertTrue(handler.handleC2P(new UnknownPacket(0), new ArrayList<>()));
    }

    @Test
    void serverUnknownPacketsAreForwarded() throws Exception {
        ProxyConnection connection = mock(ProxyConnection.class);
        when(connection.getP2sConnectionState()).thenReturn(ConnectionState.PLAY);
        UnexpectedPacketHandler handler = new UnexpectedPacketHandler(connection);

        assertTrue(handler.handleP2S(new UnknownPacket(0), new ArrayList<>()));
    }

    @Test
    void knownPacketsPassBothDirections() throws Exception {
        ProxyConnection connection = mock(ProxyConnection.class);
        when(connection.getC2pConnectionState()).thenReturn(ConnectionState.LOGIN);
        UnexpectedPacketHandler handler = new UnexpectedPacketHandler(connection);

        assertTrue(handler.handleC2P(new net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket("name"), new ArrayList<>()));
        assertTrue(handler.handleP2S(new net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket("name"), new ArrayList<>()));
    }
}
