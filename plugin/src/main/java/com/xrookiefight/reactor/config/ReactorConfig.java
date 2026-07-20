package com.xrookiefight.reactor.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.powernukkitx.utils.Config;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ReactorConfig {

    private final NetworkSettings networkSettings;
    private final BedrockBackendSettings bedrockBackendSettings;
    private final ProtocolSettings protocolSettings;
    private final ServerStatusSettings serverStatusSettings;
    private final LoggingSettings loggingSettings;
    private final AuthenticationSettings authenticationSettings;

    public static ReactorConfig from(Config config) {
        return new ReactorConfig(
                new NetworkSettings(
                        config.getString("network-settings.bind-address", "0.0.0.0"),
                        config.getInt("network-settings.bind-port", 25565),
                        config.getInt("network-settings.connect-timeout", 8000)
                ),
                new BedrockBackendSettings(
                        config.getString("bedrock-backend-settings.bedrock-host", "127.0.0.1"),
                        config.getInt("bedrock-backend-settings.bedrock-port", 19132)
                ),
                new ProtocolSettings(
                        config.getInt("protocol-settings.compression-threshold", 256),
                        config.getBoolean("protocol-settings.ignore-protocol-translation-errors", false),
                        config.getBoolean("protocol-settings.suppress-client-protocol-errors", false)
                ),
                new ServerStatusSettings(
                        config.getString("server-status-settings.motd", ""),
                        config.getInt("server-status-settings.max-players", -1)
                ),
                new LoggingSettings(
                        config.getBoolean("logging-settings.log-ips", true),
                        config.getBoolean("logging-settings.log-status-requests", false),
                        config.getBoolean("logging-settings.protocol-debug", false),
                        config.getBoolean("logging-settings.suppress-translation-warnings", true)
                ),
                new AuthenticationSettings(
                        config.getBoolean("authentication-settings.online-mode", false)
                )
        );
    }

    @Getter
    @Accessors(fluent = true)
    @RequiredArgsConstructor
    public static class NetworkSettings {
        private final String bindAddress;
        private final int bindPort;
        private final int connectTimeout;
    }

    @Getter
    @Accessors(fluent = true)
    @RequiredArgsConstructor
    public static class BedrockBackendSettings {
        private final String bedrockHost;
        private final int bedrockPort;
    }

    @Getter
    @Accessors(fluent = true)
    @RequiredArgsConstructor
    public static class ProtocolSettings {
        private final int compressionThreshold;
        private final boolean ignoreProtocolTranslationErrors;
        private final boolean suppressClientProtocolErrors;
    }

    @Getter
    @Accessors(fluent = true)
    @RequiredArgsConstructor
    public static class ServerStatusSettings {
        private final String motd;
        private final int maxPlayers;
    }

    @Getter
    @Accessors(fluent = true)
    @RequiredArgsConstructor
    public static class LoggingSettings {
        private final boolean logIps;
        private final boolean logStatusRequests;
        private final boolean protocolDebug;
        private final boolean suppressTranslationWarnings;
    }

    @Getter
    @Accessors(fluent = true)
    @RequiredArgsConstructor
    public static class AuthenticationSettings {
        private final boolean onlineMode;
    }

    public String getBindAddress() {
        return networkSettings.bindAddress();
    }

    public int getBindPort() {
        return networkSettings.bindPort();
    }

    public int getConnectTimeout() {
        return networkSettings.connectTimeout();
    }

    public String getBedrockHost() {
        return bedrockBackendSettings.bedrockHost();
    }

    public int getBedrockPort() {
        return bedrockBackendSettings.bedrockPort();
    }

    public int getCompressionThreshold() {
        return protocolSettings.compressionThreshold();
    }

    public String getMotd() {
        return serverStatusSettings.motd();
    }

    public int getMaxPlayers() {
        return serverStatusSettings.maxPlayers();
    }

    public boolean isLogIps() {
        return loggingSettings.logIps();
    }

    public boolean isLogStatusRequests() {
        return loggingSettings.logStatusRequests();
    }

    public boolean isProtocolDebug() {
        return loggingSettings.protocolDebug();
    }

    public boolean isOnlineMode() {
        return authenticationSettings.onlineMode();
    }
}
