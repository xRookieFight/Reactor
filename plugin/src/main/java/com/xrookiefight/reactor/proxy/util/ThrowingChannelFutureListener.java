package com.xrookiefight.reactor.proxy.util;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@FunctionalInterface
public interface ThrowingChannelFutureListener extends ChannelFutureListener {

    @Override
    default void operationComplete(ChannelFuture future) {
        try {
            operationCompleteThrows(future);
        } catch (CloseAndReturn ignored) {
        } catch (Throwable e) {
            future.channel().pipeline().fireExceptionCaught(e);
        }
    }

    void operationCompleteThrows(ChannelFuture future) throws Exception;
}
