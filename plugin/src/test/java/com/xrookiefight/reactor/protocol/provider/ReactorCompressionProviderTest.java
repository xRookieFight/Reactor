package com.xrookiefight.reactor.protocol.provider;

import com.viaversion.viaversion.api.connection.UserConnection;
import io.netty.channel.embedded.EmbeddedChannel;
import net.raphimc.netminecraft.constants.MCPipeline;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
class ReactorCompressionProviderTest {

    private final ReactorCompressionProvider provider = new ReactorCompressionProvider();

    @Test
    void serverSideConnectionIsRejected() {
        UserConnection user = mock(UserConnection.class);
        when(user.isClientSide()).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> provider.handlePlayCompression(user, 256));
    }

    @Test
    void clientSideConnectionSetsThreshold() {
        EmbeddedChannel channel = new EmbeddedChannel();
        UserConnection user = mock(UserConnection.class);
        when(user.isClientSide()).thenReturn(true);
        when(user.getChannel()).thenReturn(channel);

        provider.handlePlayCompression(user, 512);

        assertEquals(512, channel.attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).get());
    }
}
