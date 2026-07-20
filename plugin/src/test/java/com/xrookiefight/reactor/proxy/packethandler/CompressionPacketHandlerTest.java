package com.xrookiefight.reactor.proxy.packethandler;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.xrookiefight.reactor.TestSupport;
import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import io.netty.channel.embedded.EmbeddedChannel;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginCompressionPacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginGameProfilePacket;
import net.raphimc.netminecraft.packet.impl.play.S2CPlaySetCompressionPacket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
class CompressionPacketHandlerTest {

    private final EmbeddedChannel serverChannel = new EmbeddedChannel();
    private final EmbeddedChannel clientChannel = new EmbeddedChannel();

    @AfterEach
    void tearDown() {
        TestSupport.clearMockReactor();
    }

    private CompressionPacketHandler handler(ProtocolVersion clientVersion) {
        ProxyConnection connection = mock(ProxyConnection.class);
        when(connection.getChannel()).thenReturn(serverChannel);
        when(connection.getC2P()).thenReturn(clientChannel);
        when(connection.getClientVersion()).thenReturn(clientVersion);
        return new CompressionPacketHandler(connection);
    }

    @Test
    void playCompressionChangeIsConsumed() throws Exception {
        TestSupport.installMockReactor(TestSupport.defaultConfig());
        CompressionPacketHandler handler = handler(ProtocolVersion.v1_20_2);

        assertFalse(handler.handleP2S(new S2CPlaySetCompressionPacket(128), new ArrayList<>()));
        assertEquals(128, serverChannel.attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).get());
    }

    @Test
    void loginCompressionIsConsumed() throws Exception {
        TestSupport.installMockReactor(TestSupport.defaultConfig());
        CompressionPacketHandler handler = handler(ProtocolVersion.v1_20_2);

        assertFalse(handler.handleP2S(new S2CLoginCompressionPacket(64), new ArrayList<>()));
        assertEquals(64, serverChannel.attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).get());
    }

    @Test
    void gameProfileEnablesClientCompression() throws Exception {
        TestSupport.installMockReactor(TestSupport.defaultConfig());
        CompressionPacketHandler handler = handler(ProtocolVersion.v1_20_2);

        assertTrue(handler.handleP2S(new S2CLoginGameProfilePacket(UUID.randomUUID(), "player"), new ArrayList<>()));
        clientChannel.runPendingTasks();

        assertEquals(256, clientChannel.attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).get());
        assertTrue(serverChannel.config().isAutoRead());

        S2CLoginCompressionPacket sent = clientChannel.readOutbound();
        assertEquals(256, sent.compressionThreshold);
    }

    @Test
    void gameProfileSkipsWhenCompressionDisabled() throws Exception {
        TestSupport.installMockReactor(TestSupport.config(Map.of("protocol-settings.compression-threshold", -1)));
        CompressionPacketHandler handler = handler(ProtocolVersion.v1_20_2);

        assertTrue(handler.handleP2S(new S2CLoginGameProfilePacket(UUID.randomUUID(), "player"), new ArrayList<>()));
        assertNull(clientChannel.attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).get());
    }

    @Test
    void gameProfileSkipsForLegacyClients() throws Exception {
        TestSupport.installMockReactor(TestSupport.defaultConfig());
        CompressionPacketHandler handler = handler(ProtocolVersion.v1_7_2);

        assertTrue(handler.handleP2S(new S2CLoginGameProfilePacket(UUID.randomUUID(), "player"), new ArrayList<>()));
        assertNull(clientChannel.attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).get());
    }

    @Test
    void gameProfileSkipsWhenAlreadyCompressed() throws Exception {
        TestSupport.installMockReactor(TestSupport.defaultConfig());
        CompressionPacketHandler handler = handler(ProtocolVersion.v1_20_2);
        clientChannel.attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(256);

        assertTrue(handler.handleP2S(new S2CLoginGameProfilePacket(UUID.randomUUID(), "player"), new ArrayList<>()));
        assertTrue(serverChannel.config().isAutoRead());
    }
}
