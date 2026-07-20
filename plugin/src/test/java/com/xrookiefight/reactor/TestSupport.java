package com.xrookiefight.reactor;

import com.xrookiefight.reactor.config.ReactorConfig;
import org.mockito.Mockito;
import org.powernukkitx.utils.Config;

import java.lang.reflect.Field;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
public final class TestSupport {

    private TestSupport() {
    }

    public static ReactorConfig config(Map<String, Object> overrides) {
        Config config = mock(Config.class);
        when(config.getString(anyString(), anyString())).thenAnswer(inv ->
                overrides.getOrDefault(inv.getArgument(0), inv.getArgument(1)));
        when(config.getInt(anyString(), anyInt())).thenAnswer(inv ->
                overrides.getOrDefault(inv.getArgument(0), inv.getArgument(1)));
        when(config.getBoolean(anyString(), anyBoolean())).thenAnswer(inv ->
                overrides.getOrDefault(inv.getArgument(0), inv.getArgument(1)));
        return ReactorConfig.from(config);
    }

    public static ReactorConfig defaultConfig() {
        return config(Map.of());
    }

    public static Reactor installMockReactor(ReactorConfig config) {
        Reactor reactor = Mockito.mock(Reactor.class);
        when(reactor.getReactorConfig()).thenReturn(config);
        setInstance(reactor);
        return reactor;
    }

    public static void clearMockReactor() {
        setInstance(null);
    }

    private static void setInstance(Reactor reactor) {
        try {
            Field field = Reactor.class.getDeclaredField("instance");
            field.setAccessible(true);
            field.set(null, reactor);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
