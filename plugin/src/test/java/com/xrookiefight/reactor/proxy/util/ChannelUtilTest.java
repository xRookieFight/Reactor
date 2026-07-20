package com.xrookiefight.reactor.proxy.util;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
class ChannelUtilTest {

    @Test
    void disableAndRestoreAutoRead() {
        EmbeddedChannel channel = new EmbeddedChannel();
        assertTrue(channel.config().isAutoRead());

        ChannelUtil.disableAutoRead(channel);
        assertFalse(channel.config().isAutoRead());

        ChannelUtil.restoreAutoRead(channel);
        assertTrue(channel.config().isAutoRead());
    }

    @Test
    void nestedDisableRestore() {
        EmbeddedChannel channel = new EmbeddedChannel();

        ChannelUtil.disableAutoRead(channel);
        ChannelUtil.disableAutoRead(channel);
        assertFalse(channel.config().isAutoRead());

        ChannelUtil.restoreAutoRead(channel);
        assertFalse(channel.config().isAutoRead());

        ChannelUtil.restoreAutoRead(channel);
        assertTrue(channel.config().isAutoRead());
    }

    @Test
    void restoreWithoutDisableThrows() {
        EmbeddedChannel channel = new EmbeddedChannel();
        assertThrows(IllegalStateException.class, () -> ChannelUtil.restoreAutoRead(channel));
    }

    @Test
    void restoreDetectsExternalAutoReadChange() {
        EmbeddedChannel channel = new EmbeddedChannel();
        ChannelUtil.disableAutoRead(channel);
        channel.config().setAutoRead(true);
        assertThrows(IllegalStateException.class, () -> ChannelUtil.restoreAutoRead(channel));
    }
}
