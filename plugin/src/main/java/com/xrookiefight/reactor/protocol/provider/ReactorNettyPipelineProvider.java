package com.xrookiefight.reactor.protocol.provider;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.viabedrock.netty.CompressionCodec;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.PacketCompressionAlgorithm;
import net.raphimc.viabedrock.netty.raknet.AesEncryptionCodec;
import net.raphimc.viabedrock.netty.raknet.MessageCodec;
import net.raphimc.viabedrock.protocol.provider.NettyPipelineProvider;

import javax.crypto.SecretKey;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
public class ReactorNettyPipelineProvider extends NettyPipelineProvider {

    @Override
    public void enableCompression(UserConnection user, PacketCompressionAlgorithm algorithm, int threshold) {
        final ProxyConnection proxyConnection = ProxyConnection.fromUserConnection(user);
        final Channel channel = proxyConnection.getChannel();

        if (channel.pipeline().names().contains(MCPipeline.COMPRESSION_HANDLER_NAME)) {
            throw new IllegalStateException("Compression already enabled");
        }

        channel.pipeline().addBefore(MCPipeline.SIZER_HANDLER_NAME, MCPipeline.COMPRESSION_HANDLER_NAME, new CompressionCodec(algorithm, threshold));
        log.debug("Enabled Bedrock compression");
    }

    @Override
    public void enableEncryption(UserConnection user, SecretKey key) {
        final ProxyConnection proxyConnection = ProxyConnection.fromUserConnection(user);
        final Channel channel = proxyConnection.getChannel();

        if (channel.pipeline().names().contains(MCPipeline.ENCRYPTION_HANDLER_NAME)) {
            throw new IllegalStateException("Encryption already enabled");
        }

        if (channel.pipeline().get(MessageCodec.NAME) != null) {
            try {
                channel.pipeline().addAfter(MessageCodec.NAME, MCPipeline.ENCRYPTION_HANDLER_NAME, new AesEncryptionCodec(key));
                log.debug("Enabled Bedrock encryption");
            } catch (Throwable e) {
                throw new RuntimeException("Failed to enable encryption", e);
            }
        }
    }
}
