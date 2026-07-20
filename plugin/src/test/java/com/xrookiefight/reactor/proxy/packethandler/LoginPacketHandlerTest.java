package com.xrookiefight.reactor.proxy.packethandler;

import com.sun.net.httpserver.HttpServer;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.xrookiefight.reactor.Reactor;
import com.xrookiefight.reactor.TestSupport;
import com.xrookiefight.reactor.auth.MojangSessionService;
import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import com.xrookiefight.reactor.proxy.util.CloseAndReturn;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.lenni0451.mcstructs.text.components.StringComponent;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.netty.crypto.CryptUtil;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginAcknowledgedPacket;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginKeyPacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginCompressionPacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginDisconnectPacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginGameProfilePacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginHelloPacket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
class LoginPacketHandlerTest {

    private static HttpServer server;
    private static String baseUrl;

    private EmbeddedChannel clientChannel;
    private EmbeddedChannel serverChannel;
    private ProxyConnection connection;
    private LoginPacketHandler handler;

    @BeforeAll
    static void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/valid", exchange -> {
            byte[] body = "{\"id\":\"069a79f444e94726a5befca90e38aaf5\",\"name\":\"Notch\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/nocontent", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @BeforeEach
    void setUp() {
        clientChannel = new EmbeddedChannel();
        serverChannel = new EmbeddedChannel();
        connection = mock(ProxyConnection.class);
        when(connection.getC2P()).thenReturn(clientChannel);
        when(connection.getChannel()).thenReturn(serverChannel);
        when(connection.getClientVersion()).thenReturn(ProtocolVersion.v1_21);
        doThrow(CloseAndReturn.INSTANCE).when(connection).kickClient(anyString());
        handler = new LoginPacketHandler(connection);
    }

    @AfterEach
    void tearDown() {
        TestSupport.clearMockReactor();
    }

    private void installReactor(String hasJoinedPath) {
        Reactor reactor = TestSupport.installMockReactor(TestSupport.config(Map.of("authentication-settings.online-mode", hasJoinedPath != null)));
        if (hasJoinedPath != null) {
            when(reactor.getSessionService()).thenReturn(new MojangSessionService(baseUrl + hasJoinedPath));
        }
    }

    private void awaitBackendPacket() {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            clientChannel.runPendingTasks();
            serverChannel.runPendingTasks();
            if (serverChannel.outboundMessages().peek() != null || !clientChannel.isOpen()) {
                return;
            }
            Thread.onSpinWait();
        }
    }

    @Test
    void offlineHelloIsForwarded() throws Exception {
        installReactor(null);
        UUID uuid = UUID.randomUUID();

        assertTrue(handler.handleC2P(new C2SLoginHelloPacket("Player", null, null, null, uuid), new ArrayList<>()));
        verify(connection).setUsername("Player");
        verify(connection).setPlayerUuid(uuid);
    }

    @Test
    void loginAcknowledgedPassesThrough() throws Exception {
        installReactor(null);
        assertTrue(handler.handleC2P(new C2SLoginAcknowledgedPacket(), new ArrayList<>()));
    }

    @Test
    void keyPacketWithoutPendingHelloPassesThrough() throws Exception {
        installReactor(null);
        assertTrue(handler.handleC2P(new C2SLoginKeyPacket(new byte[0], new byte[0]), new ArrayList<>()));
    }

    @Test
    void onlineModeSendsEncryptionRequest() throws Exception {
        installReactor("/valid");

        assertFalse(handler.handleC2P(new C2SLoginHelloPacket("Notch"), new ArrayList<>()));

        S2CLoginHelloPacket hello = clientChannel.readOutbound();
        assertNotNull(hello);
        assertEquals("", hello.serverId);
        assertNotNull(hello.publicKey);
        assertEquals(4, hello.nonce.length);
        assertTrue(hello.authenticate);
    }

    @Test
    void successfulAuthenticationForwardsVerifiedHello() throws Exception {
        installReactor("/valid");
        MojangSessionService service = Reactor.getInstance().getSessionService();

        assertFalse(handler.handleC2P(new C2SLoginHelloPacket("Notch"), new ArrayList<>()));
        S2CLoginHelloPacket hello = clientChannel.readOutbound();

        var secretKey = CryptUtil.generateSecretKey();
        C2SLoginKeyPacket keyPacket = new C2SLoginKeyPacket(
                CryptUtil.encryptData(service.getKeyPair().getPublic(), secretKey.getEncoded()),
                CryptUtil.encryptData(service.getKeyPair().getPublic(), hello.nonce)
        );

        assertFalse(handler.handleC2P(keyPacket, new ArrayList<>()));
        awaitBackendPacket();

        C2SLoginHelloPacket forwarded = serverChannel.readOutbound();
        assertNotNull(forwarded);
        assertEquals("Notch", forwarded.name);
        assertEquals(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), forwarded.uuid);
        assertTrue(clientChannel.config().isAutoRead());
    }

    @Test
    void signedNonceAuthenticationSucceeds() throws Exception {
        installReactor("/valid");
        MojangSessionService service = Reactor.getInstance().getSessionService();
        var clientKeys = CryptUtil.generateKeyPair();

        assertFalse(handler.handleC2P(new C2SLoginHelloPacket("Notch", null, clientKeys.getPublic(), new byte[0], UUID.randomUUID()), new ArrayList<>()));
        S2CLoginHelloPacket hello = clientChannel.readOutbound();

        long salt = 42L;
        var secretKey = CryptUtil.generateSecretKey();
        C2SLoginKeyPacket keyPacket = new C2SLoginKeyPacket(
                CryptUtil.encryptData(service.getKeyPair().getPublic(), secretKey.getEncoded()),
                salt,
                CryptUtil.signNonce(clientKeys.getPrivate(), hello.nonce, salt)
        );

        assertFalse(handler.handleC2P(keyPacket, new ArrayList<>()));
        awaitBackendPacket();
        assertNotNull(serverChannel.readOutbound());
    }

    @Test
    void invalidNonceKicksClient() throws Exception {
        installReactor("/valid");
        MojangSessionService service = Reactor.getInstance().getSessionService();

        assertFalse(handler.handleC2P(new C2SLoginHelloPacket("Notch"), new ArrayList<>()));
        clientChannel.readOutbound();

        var secretKey = CryptUtil.generateSecretKey();
        C2SLoginKeyPacket keyPacket = new C2SLoginKeyPacket(
                CryptUtil.encryptData(service.getKeyPair().getPublic(), secretKey.getEncoded()),
                CryptUtil.encryptData(service.getKeyPair().getPublic(), new byte[]{9, 9, 9, 9})
        );

        assertThrows(CloseAndReturn.class, () -> handler.handleC2P(keyPacket, new ArrayList<>()));
        verify(connection).kickClient(anyString());
    }

    @Test
    void missingSignatureKicksClient() throws Exception {
        installReactor("/valid");
        MojangSessionService service = Reactor.getInstance().getSessionService();

        assertFalse(handler.handleC2P(new C2SLoginHelloPacket("Notch"), new ArrayList<>()));
        clientChannel.readOutbound();

        var secretKey = CryptUtil.generateSecretKey();
        C2SLoginKeyPacket keyPacket = new C2SLoginKeyPacket(
                CryptUtil.encryptData(service.getKeyPair().getPublic(), secretKey.getEncoded()),
                7L,
                new byte[]{1, 2, 3}
        );

        assertThrows(CloseAndReturn.class, () -> handler.handleC2P(keyPacket, new ArrayList<>()));
        verify(connection).kickClient(anyString());
    }

    @Test
    void corruptKeyPacketKicksClient() throws Exception {
        installReactor("/valid");
        MojangSessionService service = Reactor.getInstance().getSessionService();

        assertFalse(handler.handleC2P(new C2SLoginHelloPacket("Notch"), new ArrayList<>()));
        S2CLoginHelloPacket hello = clientChannel.readOutbound();

        C2SLoginKeyPacket keyPacket = new C2SLoginKeyPacket(
                new byte[]{1, 2, 3},
                CryptUtil.encryptData(service.getKeyPair().getPublic(), hello.nonce)
        );

        assertThrows(CloseAndReturn.class, () -> handler.handleC2P(keyPacket, new ArrayList<>()));
    }

    @Test
    void failedSessionLookupKicksClient() throws Exception {
        installReactor("/nocontent");
        MojangSessionService service = Reactor.getInstance().getSessionService();

        assertFalse(handler.handleC2P(new C2SLoginHelloPacket("Ghost"), new ArrayList<>()));
        S2CLoginHelloPacket hello = clientChannel.readOutbound();

        var secretKey = CryptUtil.generateSecretKey();
        C2SLoginKeyPacket keyPacket = new C2SLoginKeyPacket(
                CryptUtil.encryptData(service.getKeyPair().getPublic(), secretKey.getEncoded()),
                CryptUtil.encryptData(service.getKeyPair().getPublic(), hello.nonce)
        );

        assertFalse(handler.handleC2P(keyPacket, new ArrayList<>()));
        awaitBackendPacket();

        verify(connection, atLeastOnce()).kickClient(anyString());
    }

    private LoginPacketHandler softHandler(ProxyConnection softConnection) {
        when(softConnection.getC2P()).thenReturn(clientChannel);
        when(softConnection.getChannel()).thenReturn(serverChannel);
        when(softConnection.getClientVersion()).thenReturn(ProtocolVersion.v1_21);
        return new LoginPacketHandler(softConnection);
    }

    @Test
    void invalidNonceKickIsRecordedWithoutThrow() throws Exception {
        installReactor("/valid");
        MojangSessionService service = Reactor.getInstance().getSessionService();
        ProxyConnection soft = mock(ProxyConnection.class);
        LoginPacketHandler softHandler = softHandler(soft);

        assertFalse(softHandler.handleC2P(new C2SLoginHelloPacket("Notch"), new ArrayList<>()));
        clientChannel.readOutbound();

        var secretKey = CryptUtil.generateSecretKey();
        C2SLoginKeyPacket keyPacket = new C2SLoginKeyPacket(
                CryptUtil.encryptData(service.getKeyPair().getPublic(), secretKey.getEncoded()),
                CryptUtil.encryptData(service.getKeyPair().getPublic(), new byte[]{9, 9, 9, 9})
        );

        assertFalse(softHandler.handleC2P(keyPacket, new ArrayList<>()));
        awaitBackendPacket();
        verify(soft).kickClient(anyString());
    }

    @Test
    void missingSignatureKickIsRecordedWithoutThrow() throws Exception {
        installReactor("/valid");
        MojangSessionService service = Reactor.getInstance().getSessionService();
        ProxyConnection soft = mock(ProxyConnection.class);
        LoginPacketHandler softHandler = softHandler(soft);

        assertFalse(softHandler.handleC2P(new C2SLoginHelloPacket("Notch"), new ArrayList<>()));
        clientChannel.readOutbound();

        var secretKey = CryptUtil.generateSecretKey();
        C2SLoginKeyPacket keyPacket = new C2SLoginKeyPacket(
                CryptUtil.encryptData(service.getKeyPair().getPublic(), secretKey.getEncoded()),
                7L,
                new byte[]{1, 2, 3}
        );

        assertFalse(softHandler.handleC2P(keyPacket, new ArrayList<>()));
        awaitBackendPacket();
        verify(soft).kickClient(anyString());
    }

    @Test
    void corruptKeyKickIsRecordedWithoutThrow() throws Exception {
        installReactor("/valid");
        MojangSessionService service = Reactor.getInstance().getSessionService();
        ProxyConnection soft = mock(ProxyConnection.class);
        LoginPacketHandler softHandler = softHandler(soft);

        assertFalse(softHandler.handleC2P(new C2SLoginHelloPacket("Notch"), new ArrayList<>()));
        S2CLoginHelloPacket hello = clientChannel.readOutbound();

        C2SLoginKeyPacket keyPacket = new C2SLoginKeyPacket(
                new byte[]{1, 2, 3},
                CryptUtil.encryptData(service.getKeyPair().getPublic(), hello.nonce)
        );

        assertFalse(softHandler.handleC2P(keyPacket, new ArrayList<>()));
        verify(soft).kickClient(anyString());
    }

    @Test
    void sessionServerErrorKicksClientSoftly() throws Exception {
        Reactor reactor = TestSupport.installMockReactor(TestSupport.config(Map.of("authentication-settings.online-mode", true)));
        when(reactor.getSessionService()).thenReturn(new MojangSessionService("http://127.0.0.1:1/unreachable"));
        MojangSessionService service = reactor.getSessionService();

        ProxyConnection soft = mock(ProxyConnection.class);
        LoginPacketHandler softHandler = softHandler(soft);

        assertFalse(softHandler.handleC2P(new C2SLoginHelloPacket("Notch"), new ArrayList<>()));
        S2CLoginHelloPacket hello = clientChannel.readOutbound();

        var secretKey = CryptUtil.generateSecretKey();
        C2SLoginKeyPacket keyPacket = new C2SLoginKeyPacket(
                CryptUtil.encryptData(service.getKeyPair().getPublic(), secretKey.getEncoded()),
                CryptUtil.encryptData(service.getKeyPair().getPublic(), hello.nonce)
        );

        assertFalse(softHandler.handleC2P(keyPacket, new ArrayList<>()));

        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            clientChannel.runPendingTasks();
            try {
                verify(soft, atLeastOnce()).kickClient(anyString());
                return;
            } catch (AssertionError ignored) {
                Thread.onSpinWait();
            }
        }
        verify(soft, atLeastOnce()).kickClient(anyString());
    }

    @Test
    void sessionServerErrorWithThrowingKickIsSwallowed() throws Exception {
        Reactor reactor = TestSupport.installMockReactor(TestSupport.config(Map.of("authentication-settings.online-mode", true)));
        when(reactor.getSessionService()).thenReturn(new MojangSessionService("http://127.0.0.1:1/unreachable"));
        MojangSessionService service = reactor.getSessionService();

        ProxyConnection soft = mock(ProxyConnection.class);
        doThrow(CloseAndReturn.INSTANCE).when(soft).kickClient(anyString());
        LoginPacketHandler softHandler = softHandler(soft);

        assertFalse(softHandler.handleC2P(new C2SLoginHelloPacket("Notch"), new ArrayList<>()));
        S2CLoginHelloPacket hello = clientChannel.readOutbound();

        var secretKey = CryptUtil.generateSecretKey();
        C2SLoginKeyPacket keyPacket = new C2SLoginKeyPacket(
                CryptUtil.encryptData(service.getKeyPair().getPublic(), secretKey.getEncoded()),
                CryptUtil.encryptData(service.getKeyPair().getPublic(), hello.nonce)
        );

        assertFalse(softHandler.handleC2P(keyPacket, new ArrayList<>()));

        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            clientChannel.runPendingTasks();
            try {
                verify(soft, atLeastOnce()).kickClient(anyString());
                return;
            } catch (AssertionError ignored) {
                Thread.onSpinWait();
            }
        }
        verify(soft, atLeastOnce()).kickClient(anyString());
    }

    @Test
    void secondKickFailureIsSwallowed() throws Exception {
        Reactor reactor = TestSupport.installMockReactor(TestSupport.config(Map.of("authentication-settings.online-mode", true)));
        when(reactor.getSessionService()).thenReturn(new MojangSessionService(baseUrl + "/nocontent"));
        MojangSessionService service = reactor.getSessionService();

        ProxyConnection soft = mock(ProxyConnection.class);
        org.mockito.Mockito.doNothing().doThrow(CloseAndReturn.INSTANCE).when(soft).kickClient(anyString());
        LoginPacketHandler softHandler = softHandler(soft);

        assertFalse(softHandler.handleC2P(new C2SLoginHelloPacket("Ghost"), new ArrayList<>()));
        S2CLoginHelloPacket hello = clientChannel.readOutbound();

        var secretKey = CryptUtil.generateSecretKey();
        C2SLoginKeyPacket keyPacket = new C2SLoginKeyPacket(
                CryptUtil.encryptData(service.getKeyPair().getPublic(), secretKey.getEncoded()),
                CryptUtil.encryptData(service.getKeyPair().getPublic(), hello.nonce)
        );

        assertFalse(softHandler.handleC2P(keyPacket, new ArrayList<>()));

        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            clientChannel.runPendingTasks();
            try {
                verify(soft, org.mockito.Mockito.times(2)).kickClient(anyString());
                return;
            } catch (AssertionError ignored) {
                Thread.onSpinWait();
            }
        }
        verify(soft, org.mockito.Mockito.times(2)).kickClient(anyString());
    }

    @Test
    void gameProfileTransitionsLegacyClientToPlay() throws Exception {
        installReactor(null);
        when(connection.getClientVersion()).thenReturn(ProtocolVersion.v1_19_4);

        List<ChannelFutureListener> listeners = new ArrayList<>();
        UUID uuid = UUID.randomUUID();
        assertTrue(handler.handleP2S(new S2CLoginGameProfilePacket(uuid, "Player"), listeners));
        assertFalse(serverChannel.config().isAutoRead());

        for (ChannelFutureListener listener : listeners) {
            listener.operationComplete(serverChannel.newSucceededFuture());
        }
        verify(connection).setC2pConnectionState(ConnectionState.PLAY);
        verify(connection).setP2sConnectionState(ConnectionState.PLAY);
        assertTrue(serverChannel.config().isAutoRead());
    }

    @Test
    void gameProfileKeepsModernClientInConfiguration() throws Exception {
        installReactor(null);
        when(connection.getClientVersion()).thenReturn(ProtocolVersion.v1_21);

        List<ChannelFutureListener> listeners = new ArrayList<>();
        assertTrue(handler.handleP2S(new S2CLoginGameProfilePacket(UUID.randomUUID(), "Player"), listeners));

        for (ChannelFutureListener listener : listeners) {
            listener.operationComplete(serverChannel.newSucceededFuture());
        }
        assertFalse(serverChannel.config().isAutoRead());
    }

    @Test
    void informationalServerPacketsPassThrough() throws Exception {
        installReactor(null);

        assertTrue(handler.handleP2S(new S2CLoginHelloPacket("", new byte[0], new byte[0], true), new ArrayList<>()));
        assertTrue(handler.handleP2S(new S2CLoginDisconnectPacket(new StringComponent("bye")), new ArrayList<>()));
        assertTrue(handler.handleP2S(new S2CLoginCompressionPacket(256), new ArrayList<>()));
    }
}
