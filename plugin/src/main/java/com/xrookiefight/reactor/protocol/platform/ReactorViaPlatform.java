package com.xrookiefight.reactor.protocol.platform;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.platform.UserConnectionViaVersionPlatform;
import com.xrookiefight.reactor.Reactor;
import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import com.xrookiefight.reactor.proxy.util.CloseAndReturn;
import com.xrookiefight.reactor.util.JLoggerToSLF4J;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

import java.util.logging.Logger;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
public class ReactorViaPlatform extends UserConnectionViaVersionPlatform {

    @Getter
    private final Reactor plugin;

    public ReactorViaPlatform(Reactor plugin) {
        super(plugin.getDataFolder());
        this.plugin = plugin;
    }

    @Override
    public Logger createLogger(String name) {
        boolean quiet = plugin.getReactorConfig().loggingSettings().suppressTranslationWarnings();
        return new JLoggerToSLF4J(LoggerFactory.getLogger(name), quiet);
    }

    @Override
    public String getPlatformName() {
        return "Reactor";
    }

    @Override
    public String getPlatformVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean kickPlayer(UserConnection connection, String message) {
        Channel channel = connection.getChannel();
        if (channel == null) {
            log.warn("Cannot kick player: channel is null");
            return false;
        }

        log.info("Kicking player at {}: {}", channel.remoteAddress(), message);

        ProxyConnection proxyConnection = ProxyConnection.fromChannel(channel);
        if (proxyConnection != null && !proxyConnection.isClosed()) {
            try {
                proxyConnection.kickClient(message);
            } catch (CloseAndReturn ignored) {
            }
            return true;
        }

        channel.close();
        return true;
    }

    @Override
    public JsonObject getDump() {
        JsonObject dump = new JsonObject();
        dump.addProperty("platform", getPlatformName());
        dump.addProperty("platformVersion", getPlatformVersion());
        dump.addProperty("pluginVersion", getPluginVersion());
        return dump;
    }

    @Override
    public boolean hasPlugin(String name) {
        var target = plugin.getServer().getPluginManager().getPlugin(name);
        return target != null && target.isEnabled();
    }
}
