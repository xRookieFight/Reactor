package com.xrookiefight.reactor.protocol.provider;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.viabedrock.netty.raknet.MessageCodec;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.PacketCompressionAlgorithm;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
class ReactorNettyPipelineProviderTest {

    private final ReactorNettyPipelineProvider provider = new ReactorNettyPipelineProvider();

    private UserConnection userFor(EmbeddedChannel channel) {
        ProxyConnection proxyConnection = mock(ProxyConnection.class);
        when(proxyConnection.getChannel()).thenReturn(channel);
        channel.attr(ProxyConnection.PROXY_CONNECTION_ATTRIBUTE_KEY).set(proxyConnection);

        UserConnection user = mock(UserConnection.class);
        when(user.getChannel()).thenReturn(channel);
        return user;
    }

    @Test
    void compressionIsAddedBeforeSizer() {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(MCPipeline.SIZER_HANDLER_NAME, new ChannelDuplexHandler());
        UserConnection user = userFor(channel);

        provider.enableCompression(user, PacketCompressionAlgorithm.ZLib, 256);
        assertNotNull(channel.pipeline().get(MCPipeline.COMPRESSION_HANDLER_NAME));
    }

    @Test
    void doubleCompressionThrows() {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(MCPipeline.SIZER_HANDLER_NAME, new ChannelDuplexHandler());
        UserConnection user = userFor(channel);

        provider.enableCompression(user, PacketCompressionAlgorithm.ZLib, 256);
        assertThrows(IllegalStateException.class, () -> provider.enableCompression(user, PacketCompressionAlgorithm.ZLib, 256));
    }

    @Test
    void encryptionIsAddedAfterMessageCodec() {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(MessageCodec.NAME, new ChannelDuplexHandler());
        UserConnection user = userFor(channel);

        provider.enableEncryption(user, new SecretKeySpec(new byte[16], "AES"));
        assertNotNull(channel.pipeline().get(MCPipeline.ENCRYPTION_HANDLER_NAME));
    }

    @Test
    void encryptionSkippedWithoutMessageCodec() {
        EmbeddedChannel channel = new EmbeddedChannel();
        UserConnection user = userFor(channel);

        provider.enableEncryption(user, new SecretKeySpec(new byte[16], "AES"));
        assertNull(channel.pipeline().get(MCPipeline.ENCRYPTION_HANDLER_NAME));
    }

    @Test
    void doubleEncryptionThrows() {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(MessageCodec.NAME, new ChannelDuplexHandler());
        UserConnection user = userFor(channel);

        provider.enableEncryption(user, new SecretKeySpec(new byte[16], "AES"));
        assertThrows(IllegalStateException.class, () -> provider.enableEncryption(user, new SecretKeySpec(new byte[16], "AES")));
    }

    @Test
    void invalidKeyWrapsException() {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(MessageCodec.NAME, new ChannelDuplexHandler());
        UserConnection user = userFor(channel);

        assertThrows(RuntimeException.class, () -> provider.enableEncryption(user, new SecretKeySpec(new byte[3], "AES")));
    }
}
