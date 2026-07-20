package com.xrookiefight.reactor.proxy.util;

import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
class ThrowingChannelFutureListenerTest {

    @Test
    void delegatesToThrowingMethod() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();
        AtomicBoolean called = new AtomicBoolean();

        ThrowingChannelFutureListener listener = f -> called.set(true);
        listener.operationComplete(channel.newSucceededFuture());

        assertTrue(called.get());
    }

    @Test
    void swallowsCloseAndReturn() {
        EmbeddedChannel channel = new EmbeddedChannel();

        ThrowingChannelFutureListener listener = f -> {
            throw CloseAndReturn.INSTANCE;
        };
        listener.operationComplete(channel.newSucceededFuture());

        assertTrue(channel.isOpen());
    }

    @Test
    void firesExceptionOnFailure() {
        List<Throwable> caught = new ArrayList<>();
        EmbeddedChannel channel = new EmbeddedChannel(new io.netty.channel.ChannelInboundHandlerAdapter() {
            @Override
            public void exceptionCaught(io.netty.channel.ChannelHandlerContext ctx, Throwable cause) {
                caught.add(cause);
            }
        });

        RuntimeException failure = new RuntimeException("boom");
        ThrowingChannelFutureListener listener = f -> {
            throw failure;
        };
        ChannelFuture future = channel.newSucceededFuture();
        listener.operationComplete(future);
        channel.runPendingTasks();

        assertEquals(1, caught.size());
        assertEquals(failure, caught.get(0));
    }
}
