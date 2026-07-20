package com.xrookiefight.reactor;

import com.xrookiefight.reactor.api.ReactorApi;
import com.xrookiefight.reactor.api.ReactorProvider;
import com.xrookiefight.reactor.auth.MojangSessionService;
import com.xrookiefight.reactor.config.ReactorConfig;
import com.xrookiefight.reactor.protocol.ReactorProtocolTranslator;
import com.xrookiefight.reactor.proxy.ReactorProxyServer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.powernukkitx.plugin.PluginBase;

/**
 * @author xRookieFight
 * @since 2026-07-19
 */
public class Reactor extends PluginBase implements ReactorApi {

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private static Reactor instance;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private volatile boolean ready;

    @Getter
    private ReactorConfig reactorConfig;

    @Getter
    private ReactorProtocolTranslator protocolTranslator;

    @Getter
    private ReactorProxyServer proxyServer;

    @Getter
    private MojangSessionService sessionService;

    @Override
    public void onLoad() {
        instance = this;
        loadReactorConfig();
    }

    @Override
    public void onEnable() {
        try {
            if (reactorConfig.isOnlineMode()) {
                sessionService = new MojangSessionService();
            }

            protocolTranslator = new ReactorProtocolTranslator(this);
            protocolTranslator.init();

            proxyServer = new ReactorProxyServer(this);
            proxyServer.start();

            ReactorProvider.register(this);
            setReady(true);

            getLogger().info("Enabled Reactor v" + getVersion());
            getLogger().info("Java Edition players can now connect on port " + reactorConfig.getBindPort());
            getLogger().info("Bedrock backend: " + reactorConfig.getBedrockHost() + ":" + reactorConfig.getBedrockPort());
            getLogger().info(reactorConfig.isOnlineMode()
                    ? "Running in ONLINE mode, Mojang authentication enabled"
                    : "Running in OFFLINE mode, no Mojang authentication required");
        } catch (Exception e) {
            getLogger().error("Failed to enable Reactor", e);
            throw new RuntimeException("Failed to enable Reactor", e);
        }
    }

    @Override
    public void onDisable() {
        setReady(false);
        ReactorProvider.unregister();

        if (proxyServer != null) {
            proxyServer.shutdown();
        }
        if (protocolTranslator != null) {
            protocolTranslator.shutdown();
        }
    }

    @Override
    public @NotNull String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public int getConnectedJavaPlayerCount() {
        return proxyServer == null ? 0 : proxyServer.getConnectedClients().size();
    }

    private void loadReactorConfig() {
        saveDefaultConfig();
        reactorConfig = ReactorConfig.from(getConfig());
        getLogger().info("Reactor configuration loaded");
    }
}
