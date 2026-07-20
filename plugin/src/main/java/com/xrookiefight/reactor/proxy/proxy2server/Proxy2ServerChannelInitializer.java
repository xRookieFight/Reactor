package com.xrookiefight.reactor.proxy.proxy2server;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.platform.ViaChannelInitializer;
import com.xrookiefight.reactor.network.ReactorViaCodec;
import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import lombok.extern.slf4j.Slf4j;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.codec.NoReadFlowControlHandler;
import net.raphimc.netminecraft.netty.connection.MinecraftChannelInitializer;
import net.raphimc.netminecraft.packet.registry.DefaultPacketRegistry;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.viabedrock.netty.BatchLengthCodec;
import net.raphimc.viabedrock.netty.DisconnectHandler;
import net.raphimc.viabedrock.netty.PacketCodec;
import net.raphimc.viabedrock.netty.raknet.MessageCodec;
import net.raphimc.viabedrock.netty.util.DatagramCodec;
import net.raphimc.viabedrock.protocol.RakNetStatusProtocol;

import java.util.function.Supplier;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
public class Proxy2ServerChannelInitializer extends MinecraftChannelInitializer {

    public Proxy2ServerChannelInitializer(Supplier<ChannelHandler> handlerSupplier) {
        super(handlerSupplier);
    }

    @Override
    protected void initChannel(Channel channel) {
        final ProxyConnection proxyConnection = ProxyConnection.fromChannel(channel);

        super.initChannel(channel);

        channel.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(new DefaultPacketRegistry(true, proxyConnection.getClientVersion().getVersion()));

        final UserConnection user = ViaChannelInitializer.createUserConnection(channel, true);
        proxyConnection.setUserConnection(user);

        channel.pipeline().addBefore(MCPipeline.PACKET_CODEC_HANDLER_NAME, ReactorViaCodec.NAME, new ReactorViaCodec(user));
        channel.pipeline().addAfter(ReactorViaCodec.NAME, "via-" + MCPipeline.FLOW_CONTROL_HANDLER_NAME, new NoReadFlowControlHandler());

        if (proxyConnection.getServerVersion().equals(BedrockProtocolVersion.bedrockLatest)) {
            channel.pipeline().remove(MCPipeline.COMPRESSION_HANDLER_NAME);
            channel.pipeline().remove(MCPipeline.ENCRYPTION_HANDLER_NAME);

            if (proxyConnection.getC2pConnectionState() != ConnectionState.STATUS) {
                channel.pipeline().addBefore(MCPipeline.SIZER_HANDLER_NAME, DisconnectHandler.NAME, new DisconnectHandler());
                channel.pipeline().addBefore(MCPipeline.SIZER_HANDLER_NAME, MessageCodec.NAME, new MessageCodec());
                channel.pipeline().replace(MCPipeline.SIZER_HANDLER_NAME, MCPipeline.SIZER_HANDLER_NAME, new BatchLengthCodec());
                channel.pipeline().addBefore(ReactorViaCodec.NAME, PacketCodec.NAME, new PacketCodec());
            } else {
                channel.pipeline().replace(MCPipeline.SIZER_HANDLER_NAME, DatagramCodec.NAME, new DatagramCodec());
                user.getProtocolInfo().getPipeline().add(RakNetStatusProtocol.INSTANCE);
            }
        }

        log.debug("Initialized server channel for Bedrock connection");
    }
}
