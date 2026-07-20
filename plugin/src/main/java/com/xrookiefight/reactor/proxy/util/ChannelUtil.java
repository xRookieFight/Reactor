package com.xrookiefight.reactor.proxy.util;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.Stack;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
public final class ChannelUtil {

    private static final AttributeKey<Stack<Boolean>> LAST_AUTO_READ = AttributeKey.valueOf("reactor-last-auto-read");

    private ChannelUtil() {
    }

    public static void disableAutoRead(Channel channel) {
        if (channel.attr(LAST_AUTO_READ).get() == null) {
            channel.attr(LAST_AUTO_READ).set(new Stack<>());
        }

        channel.attr(LAST_AUTO_READ).get().push(channel.config().isAutoRead());
        channel.config().setAutoRead(false);
    }

    public static void restoreAutoRead(Channel channel) {
        Stack<Boolean> stack = channel.attr(LAST_AUTO_READ).get();
        if (stack == null || stack.isEmpty()) {
            throw new IllegalStateException("Tried to restore auto read, but it was never disabled");
        }
        if (channel.config().isAutoRead()) {
            throw new IllegalStateException("Race condition detected: Auto read has been enabled somewhere else");
        }
        channel.config().setAutoRead(stack.pop());
    }
}
