package com.xrookiefight.reactor.api;

import org.jetbrains.annotations.NotNull;

/**
 * @author xRookieFight
 * @since 2026-07-19
 */
public interface ReactorApi {

    @NotNull
    String getVersion();

    boolean isReady();
}
