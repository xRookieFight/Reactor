package com.xrookiefight.reactor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.powernukkitx.plugin.PluginBase;
import com.xrookiefight.reactor.api.ReactorApi;
import com.xrookiefight.reactor.api.ReactorProvider;
import org.jetbrains.annotations.NotNull;

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

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        ReactorProvider.register(this);
        setReady(true);
        getLogger().info("Enabled Reactor v " + getVersion());
    }

    @Override
    public void onDisable() {
        setReady(false);
        ReactorProvider.unregister();
    }

    @Override
    public @NotNull String getVersion() {
        return getDescription().getVersion();
    }
}
