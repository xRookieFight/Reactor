package com.xrookiefight.reactor.protocol.provider;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.protocols.v1_8to1_9.provider.CompressionProvider;
import lombok.extern.slf4j.Slf4j;
import net.raphimc.netminecraft.constants.MCPipeline;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
public class ReactorCompressionProvider extends CompressionProvider {

    @Override
    public void handlePlayCompression(UserConnection user, int threshold) {
        if (!user.isClientSide()) {
            throw new IllegalStateException("PLAY state Compression packet is unsupported");
        }

        log.debug("Enabling compression with threshold {} for {}", threshold, user.getChannel().remoteAddress());

        user.getChannel().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(threshold);
    }
}
