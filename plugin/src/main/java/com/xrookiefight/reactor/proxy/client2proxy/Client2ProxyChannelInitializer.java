package com.xrookiefight.reactor.proxy.client2proxy;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.platform.ViaChannelInitializer;
import com.xrookiefight.reactor.network.ReactorViaCodec;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import lombok.extern.slf4j.Slf4j;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.codec.NoReadFlowControlHandler;
import net.raphimc.netminecraft.netty.connection.MinecraftChannelInitializer;
import net.raphimc.netminecraft.packet.registry.DefaultPacketRegistry;

import java.util.function.Supplier;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
public class Client2ProxyChannelInitializer extends MinecraftChannelInitializer {

    public Client2ProxyChannelInitializer(Supplier<ChannelHandler> handlerSupplier) {
        super(handlerSupplier);
    }

    @Override
    protected void initChannel(Channel channel) {
        super.initChannel(channel);

        channel.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(new DefaultPacketRegistry(false, -1));

        final UserConnection user = ViaChannelInitializer.createUserConnection(channel, false);

        channel.pipeline().addBefore(MCPipeline.PACKET_CODEC_HANDLER_NAME, ReactorViaCodec.NAME, new ReactorViaCodec(user));
        channel.pipeline().addAfter(ReactorViaCodec.NAME, "via-" + MCPipeline.FLOW_CONTROL_HANDLER_NAME, new NoReadFlowControlHandler());

        log.debug("Initialized client channel: {}", channel.remoteAddress());
    }
}
