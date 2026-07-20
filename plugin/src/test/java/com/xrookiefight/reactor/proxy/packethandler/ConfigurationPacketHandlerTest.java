package com.xrookiefight.reactor.proxy.packethandler;

import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import com.xrookiefight.reactor.proxy.util.ChannelUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.packet.impl.configuration.C2SConfigFinishConfigurationPacket;
import net.raphimc.netminecraft.packet.impl.configuration.S2CConfigFinishConfigurationPacket;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginAcknowledgedPacket;
import net.raphimc.netminecraft.packet.impl.play.C2SPlayConfigurationAcknowledgedPacket;
import net.raphimc.netminecraft.packet.impl.play.S2CPlayStartConfigurationPacket;
import net.raphimc.netminecraft.packet.UnknownPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
class ConfigurationPacketHandlerTest {

    private EmbeddedChannel serverChannel;
    private ProxyConnection connection;
    private ConfigurationPacketHandler handler;

    @BeforeEach
    void setUp() {
        serverChannel = new EmbeddedChannel();
        connection = mock(ProxyConnection.class);
        when(connection.getChannel()).thenReturn(serverChannel);
        when(connection.getClientAddressString()).thenReturn("client");
        handler = new ConfigurationPacketHandler(connection);
    }

    private void runListeners(List<ChannelFutureListener> listeners) throws Exception {
        for (ChannelFutureListener listener : listeners) {
            listener.operationComplete(serverChannel.newSucceededFuture());
        }
    }

    @Test
    void loginAcknowledgedSwitchesToConfiguration() throws Exception {
        ChannelUtil.disableAutoRead(serverChannel);

        List<ChannelFutureListener> listeners = new ArrayList<>();
        assertTrue(handler.handleC2P(new C2SLoginAcknowledgedPacket(), listeners));
        assertEquals(1, listeners.size());

        runListeners(listeners);
        verify(connection).setC2pConnectionState(ConnectionState.CONFIGURATION);
        verify(connection).setP2sConnectionState(ConnectionState.CONFIGURATION);
        assertTrue(serverChannel.config().isAutoRead());
    }

    @Test
    void finishConfigurationSwitchesToPlay() throws Exception {
        ChannelUtil.disableAutoRead(serverChannel);

        List<ChannelFutureListener> listeners = new ArrayList<>();
        assertTrue(handler.handleC2P(new C2SConfigFinishConfigurationPacket(), listeners));
        runListeners(listeners);

        verify(connection).setC2pConnectionState(ConnectionState.PLAY);
        verify(connection).setP2sConnectionState(ConnectionState.PLAY);
        assertTrue(serverChannel.config().isAutoRead());
    }

    @Test
    void configurationAcknowledgedSwitchesBack() throws Exception {
        ChannelUtil.disableAutoRead(serverChannel);

        List<ChannelFutureListener> listeners = new ArrayList<>();
        assertTrue(handler.handleC2P(new C2SPlayConfigurationAcknowledgedPacket(), listeners));
        runListeners(listeners);

        verify(connection).setC2pConnectionState(ConnectionState.CONFIGURATION);
        verify(connection).setP2sConnectionState(ConnectionState.CONFIGURATION);
        assertTrue(serverChannel.config().isAutoRead());
    }

    @Test
    void serverConfigurationPacketsDisableAutoRead() throws Exception {
        assertTrue(handler.handleP2S(new S2CConfigFinishConfigurationPacket(), new ArrayList<>()));
        assertFalse(serverChannel.config().isAutoRead());

        ChannelUtil.restoreAutoRead(serverChannel);

        assertTrue(handler.handleP2S(new S2CPlayStartConfigurationPacket(), new ArrayList<>()));
        assertFalse(serverChannel.config().isAutoRead());
    }

    @Test
    void unrelatedPacketsPassThrough() throws Exception {
        List<ChannelFutureListener> listeners = new ArrayList<>();
        assertTrue(handler.handleC2P(new UnknownPacket(0), listeners));
        assertTrue(handler.handleP2S(new UnknownPacket(0), listeners));
        assertTrue(listeners.isEmpty());
    }

    @Test
    void failedFuturesDoNotSwitchState() throws Exception {
        List<ChannelFutureListener> listeners = new ArrayList<>();
        assertTrue(handler.handleC2P(new C2SLoginAcknowledgedPacket(), listeners));
        for (ChannelFutureListener listener : listeners) {
            listener.operationComplete(serverChannel.newFailedFuture(new RuntimeException("fail")));
        }
        verify(connection).setC2pConnectionState(ConnectionState.CONFIGURATION);
    }
}
