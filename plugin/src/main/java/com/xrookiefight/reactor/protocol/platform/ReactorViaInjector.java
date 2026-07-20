package com.xrookiefight.reactor.protocol.platform;

import com.viaversion.viaversion.api.platform.ViaInjector;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.xrookiefight.reactor.network.ReactorViaCodec;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
public class ReactorViaInjector implements ViaInjector {

    @Override
    public void inject() {
        log.debug("ViaVersion injector called, Reactor handles pipeline injection directly");
    }

    @Override
    public void uninject() {
        log.debug("ViaVersion uninject called");
    }

    @Override
    public boolean lateProtocolVersionSetting() {
        return true;
    }

    @Override
    public ProtocolVersion getServerProtocolVersion() {
        return ProtocolVersion.unknown;
    }

    @Override
    public String getEncoderName() {
        return ReactorViaCodec.NAME;
    }

    @Override
    public String getDecoderName() {
        return ReactorViaCodec.NAME;
    }

    @Override
    public JsonObject getDump() {
        JsonObject dump = new JsonObject();
        dump.addProperty("injectorType", "Reactor");
        dump.addProperty("lateProtocolVersionSetting", true);
        return dump;
    }
}
