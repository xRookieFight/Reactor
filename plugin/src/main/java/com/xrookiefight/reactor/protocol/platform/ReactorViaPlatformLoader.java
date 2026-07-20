package com.xrookiefight.reactor.protocol.platform;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.platform.ViaPlatformLoader;
import com.viaversion.viaversion.api.protocol.version.VersionProvider;
import com.viaversion.viaversion.protocols.v1_8to1_9.provider.CompressionProvider;
import com.xrookiefight.reactor.Reactor;
import com.xrookiefight.reactor.protocol.provider.ReactorCompressionProvider;
import com.xrookiefight.reactor.protocol.provider.ReactorNettyPipelineProvider;
import com.xrookiefight.reactor.protocol.provider.ReactorSkinProvider;
import com.xrookiefight.reactor.protocol.provider.ReactorVersionProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.raphimc.viabedrock.protocol.provider.NettyPipelineProvider;
import net.raphimc.viabedrock.protocol.provider.SkinProvider;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
@RequiredArgsConstructor
public class ReactorViaPlatformLoader implements ViaPlatformLoader {

    private final Reactor plugin;

    @Override
    public void load() {
        log.info("Loading Reactor ViaVersion providers...");

        Via.getManager().getProviders().use(VersionProvider.class, new ReactorVersionProvider());
        Via.getManager().getProviders().use(CompressionProvider.class, new ReactorCompressionProvider());
        Via.getManager().getProviders().use(NettyPipelineProvider.class, new ReactorNettyPipelineProvider());
        Via.getManager().getProviders().use(SkinProvider.class, new ReactorSkinProvider());

        log.info("Reactor ViaVersion providers loaded");
    }

    @Override
    public void unload() {
        log.info("Unloading Reactor ViaVersion providers...");
    }
}
