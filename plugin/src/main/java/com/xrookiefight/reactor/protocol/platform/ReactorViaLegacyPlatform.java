package com.xrookiefight.reactor.protocol.platform;

import com.viaversion.viaversion.api.Via;
import net.raphimc.vialegacy.ViaLegacyPlatformImpl;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
public class ReactorViaLegacyPlatform extends ViaLegacyPlatformImpl {

    @Override
    public String getCpeAppName() {
        return Via.getPlatform().getPlatformName() + " " + Via.getPlatform().getPlatformVersion();
    }
}
