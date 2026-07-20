package com.xrookiefight.reactor.network;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.platform.ViaCodecHandler;
import com.xrookiefight.reactor.Reactor;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
public class ReactorViaCodec extends ViaCodecHandler {

    public static final String NAME = "via-codec";

    public ReactorViaCodec(UserConnection user) {
        super(user);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        var config = Reactor.getInstance().getReactorConfig();
        if (config.protocolSettings().ignoreProtocolTranslationErrors()) {
            try {
                super.channelRead(ctx, msg);
            } catch (Throwable e) {
                log.error("Protocol translation error occurred", e);
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        var config = Reactor.getInstance().getReactorConfig();
        if (config.loggingSettings().protocolDebug()) {
            log.error("Exception in ViaCodec for {}", ctx.channel().remoteAddress(), cause);
        }
        super.exceptionCaught(ctx, cause);
    }
}
