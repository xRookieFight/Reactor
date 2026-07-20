package com.xrookiefight.reactor.protocol;

import com.viaversion.viabackwards.ViaBackwardsPlatformImpl;
import com.viaversion.viarewind.ViaRewindPlatformImpl;
import com.viaversion.viaversion.ViaManagerImpl;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5;
import com.xrookiefight.reactor.Reactor;
import com.xrookiefight.reactor.protocol.platform.ReactorViaInjector;
import com.xrookiefight.reactor.protocol.platform.ReactorViaLegacyPlatform;
import com.xrookiefight.reactor.protocol.platform.ReactorViaPlatform;
import com.xrookiefight.reactor.protocol.platform.ReactorViaPlatformLoader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.raphimc.viabedrock.ViaBedrockPlatformImpl;

import java.util.function.Supplier;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
@RequiredArgsConstructor
public class ReactorProtocolTranslator {

    private final Reactor plugin;
    private ReactorViaPlatform viaPlatform;

    @Getter
    private boolean initialized;

    public void init() {
        if (initialized) {
            log.warn("Protocol translator already initialized");
            return;
        }

        log.info("Initializing ViaVersion protocol stack...");

        try {
            viaPlatform = new ReactorViaPlatform(plugin);

            Supplier<?>[] platformSuppliers = new Supplier[]{
                    ViaBackwardsPlatformImpl::new,
                    ViaRewindPlatformImpl::new,
                    ReactorViaLegacyPlatform::new,
                    ViaBedrockPlatformImpl::new
            };

            ViaManagerImpl.initAndLoad(
                    viaPlatform,
                    new ReactorViaInjector(),
                    null,
                    new ReactorViaPlatformLoader(plugin),
                    () -> {
                        for (Supplier<?> platformSupplier : platformSuppliers) {
                            platformSupplier.get();
                        }
                    }
            );

            Protocol1_20_3To1_20_5.strictErrorHandling = false;

            initialized = true;
            log.info("ViaVersion protocol stack initialized successfully");
            logSupportedVersions();
        } catch (Exception e) {
            log.error("Failed to initialize ViaVersion protocol stack", e);
            throw new RuntimeException("Failed to initialize protocol translator", e);
        }
    }

    private void logSupportedVersions() {
        var javaVersions = ProtocolVersion.getProtocols().stream()
                .filter(ProtocolVersion::isKnown)
                .filter(v -> !v.getName().startsWith("Bedrock"))
                .toList();

        if (!javaVersions.isEmpty()) {
            String minJava = javaVersions.stream()
                    .min((a, b) -> Integer.compare(a.getVersion(), b.getVersion()))
                    .map(ProtocolVersion::getName)
                    .orElse("unknown");
            String maxJava = javaVersions.stream()
                    .max((a, b) -> Integer.compare(a.getVersion(), b.getVersion()))
                    .map(ProtocolVersion::getName)
                    .orElse("unknown");
            log.info("Supported Java protocol versions: {} to {}", minJava, maxJava);
        }

        log.info("Target: Bedrock Edition via ViaBedrock translation");
    }

    public void shutdown() {
        if (!initialized) {
            return;
        }

        log.info("Shutting down protocol translator...");
        initialized = false;
    }
}
