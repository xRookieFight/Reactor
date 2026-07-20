package com.xrookiefight.reactor.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author xRookieFight
 * @since 2026-07-19
 */
public final class ReactorProvider {

    private static volatile ReactorApi instance;

    private ReactorProvider() {
    }

    @NotNull
    public static ReactorApi get() {
        ReactorApi api = instance;
        if (api == null) {
            throw new IllegalStateException("Reactor API hasn't registered yet");
        }
        return api;
    }

    @Nullable
    public static ReactorApi getOrNull() {
        return instance;
    }

    public static void register(@NotNull ReactorApi api) {
        instance = api;
    }

    public static void unregister() {
        instance = null;
    }
}
