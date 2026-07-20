package com.xrookiefight.reactor.proxy.packethandler;

import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import io.netty.channel.ChannelFutureListener;
import net.lenni0451.mcstructs.text.components.StringComponent;
import net.raphimc.netminecraft.packet.UnknownPacket;
import net.raphimc.netminecraft.packet.impl.configuration.S2CConfigDisconnectPacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginDisconnectPacket;
import net.raphimc.netminecraft.packet.impl.play.S2CPlayDisconnectPacket;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
class DisconnectPacketHandlerTest {

    private final DisconnectPacketHandler handler = new DisconnectPacketHandler(mock(ProxyConnection.class));

    @Test
    void loginDisconnectAddsCloseListener() throws Exception {
        List<ChannelFutureListener> listeners = new ArrayList<>();
        assertTrue(handler.handleP2S(new S2CLoginDisconnectPacket(new StringComponent("bye")), listeners));
        assertTrue(listeners.contains(ChannelFutureListener.CLOSE));
    }

    @Test
    void configDisconnectAddsCloseListener() throws Exception {
        List<ChannelFutureListener> listeners = new ArrayList<>();
        assertTrue(handler.handleP2S(new S2CConfigDisconnectPacket(new StringComponent("bye")), listeners));
        assertTrue(listeners.contains(ChannelFutureListener.CLOSE));
    }

    @Test
    void playDisconnectAddsCloseListener() throws Exception {
        List<ChannelFutureListener> listeners = new ArrayList<>();
        assertTrue(handler.handleP2S(new S2CPlayDisconnectPacket(new StringComponent("bye")), listeners));
        assertTrue(listeners.contains(ChannelFutureListener.CLOSE));
    }

    @Test
    void otherPacketsPassThrough() throws Exception {
        List<ChannelFutureListener> listeners = new ArrayList<>();
        assertTrue(handler.handleP2S(new UnknownPacket(0), listeners));
        assertTrue(listeners.isEmpty());
    }
}
