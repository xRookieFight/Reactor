package com.xrookiefight.reactor.proxy.util;

import com.xrookiefight.reactor.Reactor;
import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
public final class ExceptionUtil {

    private ExceptionUtil() {
    }

    public static void handleNettyException(ChannelHandlerContext ctx, Throwable cause, ProxyConnection proxyConnection, boolean fromClient) {
        if (!ctx.channel().isOpen()) {
            return;
        }

        if (cause instanceof ClosedChannelException) {
            return;
        }

        if (cause instanceof CloseAndReturn) {
            ctx.channel().close();
            return;
        }

        if (cause instanceof IOException) {
            log.info("Connection lost ({}): {}", proxyConnection != null ? proxyConnection.getClientAddressString() : ctx.channel().remoteAddress(), cause.getMessage());
            ctx.channel().close();
            return;
        }

        var config = Reactor.getInstance().getReactorConfig();
        if (!fromClient || !config.protocolSettings().suppressClientProtocolErrors()) {
            log.error("Caught unhandled netty exception", cause);

            try {
                if (proxyConnection != null && !proxyConnection.isClosed()) {
                    proxyConnection.kickClient("§cAn unhandled error occurred in your connection and it has been closed.\n§aError details:§f" + prettyPrint(cause));
                }
            } catch (Throwable ignored) {
            }
        }

        ctx.channel().close();
    }

    public static String prettyPrint(Throwable t) {
        final StringBuilder msg = new StringBuilder();

        if (t instanceof EncoderException && t.getCause() != null) {
            t = t.getCause();
        }
        if (t instanceof DecoderException && t.getCause() != null) {
            t = t.getCause();
        }

        while (t != null) {
            msg.append("\n");
            msg.append("§c").append(t.getClass().getSimpleName()).append("§7: §f").append(t.getMessage());
            t = t.getCause();
            if (t != null) {
                msg.append(" §9Caused by");
            }
        }

        return msg.toString();
    }
}
