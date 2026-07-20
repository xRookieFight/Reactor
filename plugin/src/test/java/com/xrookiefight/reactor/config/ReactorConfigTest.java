package com.xrookiefight.reactor.config;

import com.xrookiefight.reactor.TestSupport;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
class ReactorConfigTest {

    @Test
    void defaultsAreApplied() {
        ReactorConfig config = TestSupport.defaultConfig();

        assertEquals("0.0.0.0", config.getBindAddress());
        assertEquals(25565, config.getBindPort());
        assertEquals(8000, config.getConnectTimeout());
        assertEquals("127.0.0.1", config.getBedrockHost());
        assertEquals(19132, config.getBedrockPort());
        assertEquals(256, config.getCompressionThreshold());
        assertEquals("", config.getMotd());
        assertEquals(-1, config.getMaxPlayers());
        assertTrue(config.isLogIps());
        assertFalse(config.isLogStatusRequests());
        assertFalse(config.isProtocolDebug());
        assertFalse(config.isOnlineMode());
        assertFalse(config.protocolSettings().ignoreProtocolTranslationErrors());
        assertFalse(config.protocolSettings().suppressClientProtocolErrors());
        assertTrue(config.loggingSettings().suppressTranslationWarnings());
    }

    @Test
    void overridesAreApplied() {
        ReactorConfig config = TestSupport.config(Map.ofEntries(
                Map.entry("network-settings.bind-address", "127.0.0.1"),
                Map.entry("network-settings.bind-port", 25577),
                Map.entry("network-settings.connect-timeout", 5000),
                Map.entry("bedrock-backend-settings.bedrock-host", "10.0.0.5"),
                Map.entry("bedrock-backend-settings.bedrock-port", 19133),
                Map.entry("protocol-settings.compression-threshold", -1),
                Map.entry("protocol-settings.ignore-protocol-translation-errors", true),
                Map.entry("protocol-settings.suppress-client-protocol-errors", true),
                Map.entry("server-status-settings.motd", "&aHello"),
                Map.entry("server-status-settings.max-players", 100),
                Map.entry("logging-settings.log-ips", false),
                Map.entry("logging-settings.log-status-requests", true),
                Map.entry("logging-settings.protocol-debug", true),
                Map.entry("logging-settings.suppress-translation-warnings", false),
                Map.entry("authentication-settings.online-mode", true)
        ));

        assertEquals("127.0.0.1", config.getBindAddress());
        assertEquals(25577, config.getBindPort());
        assertEquals(5000, config.getConnectTimeout());
        assertEquals("10.0.0.5", config.getBedrockHost());
        assertEquals(19133, config.getBedrockPort());
        assertEquals(-1, config.getCompressionThreshold());
        assertEquals("&aHello", config.getMotd());
        assertEquals(100, config.getMaxPlayers());
        assertFalse(config.isLogIps());
        assertTrue(config.isLogStatusRequests());
        assertTrue(config.isProtocolDebug());
        assertTrue(config.isOnlineMode());
        assertTrue(config.protocolSettings().ignoreProtocolTranslationErrors());
        assertTrue(config.protocolSettings().suppressClientProtocolErrors());
        assertFalse(config.loggingSettings().suppressTranslationWarnings());
    }

    @Test
    void nestedSectionsExposeValues() {
        ReactorConfig config = TestSupport.defaultConfig();

        assertEquals("0.0.0.0", config.networkSettings().bindAddress());
        assertEquals(25565, config.networkSettings().bindPort());
        assertEquals(8000, config.networkSettings().connectTimeout());
        assertEquals("127.0.0.1", config.bedrockBackendSettings().bedrockHost());
        assertEquals(19132, config.bedrockBackendSettings().bedrockPort());
        assertEquals(256, config.protocolSettings().compressionThreshold());
        assertEquals("", config.serverStatusSettings().motd());
        assertEquals(-1, config.serverStatusSettings().maxPlayers());
        assertTrue(config.loggingSettings().logIps());
        assertFalse(config.loggingSettings().logStatusRequests());
        assertFalse(config.loggingSettings().protocolDebug());
        assertFalse(config.authenticationSettings().onlineMode());
    }
}
