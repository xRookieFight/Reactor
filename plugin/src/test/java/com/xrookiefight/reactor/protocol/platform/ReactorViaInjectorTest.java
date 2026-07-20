package com.xrookiefight.reactor.protocol.platform;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.xrookiefight.reactor.network.ReactorViaCodec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
class ReactorViaInjectorTest {

    private final ReactorViaInjector injector = new ReactorViaInjector();

    @Test
    void injectAndUninjectAreNoOps() {
        assertDoesNotThrow(injector::inject);
        assertDoesNotThrow(injector::uninject);
    }

    @Test
    void usesLateProtocolVersionSetting() {
        assertTrue(injector.lateProtocolVersionSetting());
    }

    @Test
    void serverProtocolVersionIsUnknown() {
        assertEquals(ProtocolVersion.unknown, injector.getServerProtocolVersion());
    }

    @Test
    void codecNamesMatch() {
        assertEquals(ReactorViaCodec.NAME, injector.getEncoderName());
        assertEquals(ReactorViaCodec.NAME, injector.getDecoderName());
    }

    @Test
    void dumpContainsInjectorType() {
        JsonObject dump = injector.getDump();
        assertEquals("Reactor", dump.get("injectorType").getAsString());
        assertTrue(dump.get("lateProtocolVersionSetting").getAsBoolean());
    }
}
