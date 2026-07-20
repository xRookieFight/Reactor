package com.xrookiefight.reactor.auth;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
class MojangSessionServiceTest {

    private static HttpServer server;
    private static String baseUrl;

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
        server.createContext("/badjson", exchange -> {
            byte[] body = "not json at all".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @Test
    void keyPairIsGenerated() {
        MojangSessionService service = new MojangSessionService();
        assertNotNull(service.getKeyPair());
        assertNotNull(service.getKeyPair().getPublic());
        assertNotNull(service.getKeyPair().getPrivate());
    }

    @Test
    void validResponseReturnsProfile() throws Exception {
        MojangSessionService service = new MojangSessionService(baseUrl + "/valid");
        MojangSessionService.GameProfile profile = service.hasJoined("Notch", "hash").get();

        assertNotNull(profile);
        assertEquals("Notch", profile.name());
        assertEquals(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), profile.uuid());
    }

    @Test
    void noContentReturnsNull() throws Exception {
        MojangSessionService service = new MojangSessionService(baseUrl + "/nocontent");
        assertNull(service.hasJoined("Ghost", "hash").get());
    }

    @Test
    void badJsonReturnsNull() throws Exception {
        MojangSessionService service = new MojangSessionService(baseUrl + "/badjson");
        assertNull(service.hasJoined("Broken", "hash").get());
    }

    @Test
    void specialCharactersAreEncoded() throws Exception {
        MojangSessionService service = new MojangSessionService(baseUrl + "/nocontent");
        assertNull(service.hasJoined("name with space", "hash+/=").get());
    }
}
