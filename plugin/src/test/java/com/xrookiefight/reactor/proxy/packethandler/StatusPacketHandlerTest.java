package com.xrookiefight.reactor.proxy.packethandler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xrookiefight.reactor.TestSupport;
import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import io.netty.channel.ChannelFutureListener;
import net.raphimc.netminecraft.packet.impl.status.C2SStatusPingRequestPacket;
import net.raphimc.netminecraft.packet.impl.status.C2SStatusRequestPacket;
import net.raphimc.netminecraft.packet.impl.status.S2CStatusPongResponsePacket;
import net.raphimc.netminecraft.packet.impl.status.S2CStatusResponsePacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
class StatusPacketHandlerTest {

    private static final String STATUS_JSON = "{\"description\":\"backend\",\"players\":{\"max\":10,\"online\":1}}";

    @AfterEach
    void tearDown() {
        TestSupport.clearMockReactor();
    }

    private StatusPacketHandler handler() {
        ProxyConnection connection = mock(ProxyConnection.class);
        when(connection.getClientAddressString()).thenReturn("client");
        return new StatusPacketHandler(connection);
    }

    @Test
    void clientStatusPacketsPassThrough() throws Exception {
        TestSupport.installMockReactor(TestSupport.defaultConfig());
        StatusPacketHandler handler = handler();

        assertTrue(handler.handleC2P(new C2SStatusRequestPacket(), new ArrayList<>()));
        assertTrue(handler.handleC2P(new C2SStatusPingRequestPacket(123L), new ArrayList<>()));
    }

    @Test
    void pongAddsCloseListener() throws Exception {
        TestSupport.installMockReactor(TestSupport.defaultConfig());
        StatusPacketHandler handler = handler();

        List<ChannelFutureListener> listeners = new ArrayList<>();
        assertTrue(handler.handleP2S(new S2CStatusPongResponsePacket(123L), listeners));
        assertTrue(listeners.contains(ChannelFutureListener.CLOSE));
    }

    @Test
    void statusResponseUntouchedByDefault() throws Exception {
        TestSupport.installMockReactor(TestSupport.defaultConfig());
        StatusPacketHandler handler = handler();

        S2CStatusResponsePacket packet = new S2CStatusResponsePacket(STATUS_JSON);
        assertTrue(handler.handleP2S(packet, new ArrayList<>()));
        assertEquals(STATUS_JSON, packet.statusJson);
    }

    @Test
    void customMotdAndMaxPlayersApplied() throws Exception {
        TestSupport.installMockReactor(TestSupport.config(Map.of(
                "server-status-settings.motd", "&aReactor",
                "server-status-settings.max-players", 42,
                "logging-settings.log-status-requests", true
        )));
        StatusPacketHandler handler = handler();

        S2CStatusResponsePacket packet = new S2CStatusResponsePacket(STATUS_JSON);
        assertTrue(handler.handleP2S(packet, new ArrayList<>()));

        JsonObject json = JsonParser.parseString(packet.statusJson).getAsJsonObject();
        assertEquals("§aReactor", json.get("description").getAsString());
        assertEquals(42, json.getAsJsonObject("players").get("max").getAsInt());
    }

    @Test
    void maxPlayersWithoutPlayersObjectIsIgnored() throws Exception {
        TestSupport.installMockReactor(TestSupport.config(Map.of("server-status-settings.max-players", 42)));
        StatusPacketHandler handler = handler();

        S2CStatusResponsePacket packet = new S2CStatusResponsePacket("{\"description\":\"x\"}");
        assertTrue(handler.handleP2S(packet, new ArrayList<>()));
        assertEquals("{\"description\":\"x\"}", packet.statusJson);
    }

    @Test
    void invalidJsonIsLeftUntouched() throws Exception {
        TestSupport.installMockReactor(TestSupport.config(Map.of("server-status-settings.motd", "&aReactor")));
        StatusPacketHandler handler = handler();

        S2CStatusResponsePacket packet = new S2CStatusResponsePacket("not json");
        assertTrue(handler.handleP2S(packet, new ArrayList<>()));
        assertEquals("not json", packet.statusJson);
    }
}
