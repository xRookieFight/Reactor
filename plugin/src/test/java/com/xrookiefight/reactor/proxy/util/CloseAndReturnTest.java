package com.xrookiefight.reactor.proxy.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
class CloseAndReturnTest {

    @Test
    void singletonInstance() {
        assertSame(CloseAndReturn.INSTANCE, CloseAndReturn.INSTANCE);
    }

    @Test
    void hasNoMessageAndNoStackTrace() {
        assertNull(CloseAndReturn.INSTANCE.getMessage());
        assertNull(CloseAndReturn.INSTANCE.getCause());
        assertEquals(0, CloseAndReturn.INSTANCE.getStackTrace().length);
    }
}
