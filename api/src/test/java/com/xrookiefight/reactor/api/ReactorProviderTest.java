package com.xrookiefight.reactor.api;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
class ReactorProviderTest {

    private static final ReactorApi DUMMY_API = new ReactorApi() {
        @Override
        public @NotNull String getVersion() {
            return "1.0.0";
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public int getConnectedJavaPlayerCount() {
            return 3;
        }
    };

    @AfterEach
    void tearDown() {
        ReactorProvider.unregister();
    }

    @Test
    void getThrowsWhenNotRegistered() {
        assertThrows(IllegalStateException.class, ReactorProvider::get);
    }

    @Test
    void getOrNullReturnsNullWhenNotRegistered() {
        assertNull(ReactorProvider.getOrNull());
    }

    @Test
    void getReturnsRegisteredApi() {
        ReactorProvider.register(DUMMY_API);
        assertSame(DUMMY_API, ReactorProvider.get());
        assertSame(DUMMY_API, ReactorProvider.getOrNull());
    }

    @Test
    void unregisterClearsApi() {
        ReactorProvider.register(DUMMY_API);
        ReactorProvider.unregister();
        assertNull(ReactorProvider.getOrNull());
    }

    @Test
    void apiContractIsExposed() {
        ReactorProvider.register(DUMMY_API);
        ReactorApi api = ReactorProvider.get();
        assertEquals("1.0.0", api.getVersion());
        assertEquals(true, api.isReady());
        assertEquals(3, api.getConnectedJavaPlayerCount());
    }
}
