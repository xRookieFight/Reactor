package com.xrookiefight.reactor.proxy.util;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
public final class CloseAndReturn extends RuntimeException {

    public static final CloseAndReturn INSTANCE = new CloseAndReturn();

    private CloseAndReturn() {
        super(null, null, false, false);
    }
}
