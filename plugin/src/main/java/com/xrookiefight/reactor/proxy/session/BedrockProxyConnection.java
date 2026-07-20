package com.xrookiefight.reactor.proxy.session;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.xrookiefight.reactor.Reactor;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.DatagramChannel;
import lombok.extern.slf4j.Slf4j;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.util.EventLoops;
import net.raphimc.netminecraft.util.TransportType;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.RakClientChannel;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;

import java.net.SocketAddress;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
public class BedrockProxyConnection extends ProxyConnection {

    public BedrockProxyConnection(ChannelInitializer<Channel> channelInitializer, Channel c2p) {
        super(channelInitializer, c2p);
    }

    @Override
    public void initialize(TransportType transportType, Bootstrap bootstrap) {
        bootstrap
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Reactor.getInstance().getReactorConfig().getConnectTimeout())
                .attr(ProxyConnection.PROXY_CONNECTION_ATTRIBUTE_KEY, this)
                .handler(this.channelInitializer);

        if (this.getC2pConnectionState() == ConnectionState.STATUS) {
            this.initializeRaw(transportType, bootstrap);
        } else {
            this.initializeRakNet(transportType, bootstrap);
        }

        this.channelFuture = bootstrap.register().syncUninterruptibly();
    }

    @Override
    public ChannelFuture connectToServer(SocketAddress serverAddress, ProtocolVersion targetVersion) {
        return super.connectToServer(serverAddress, targetVersion);
    }

    @SuppressWarnings("unchecked")
    protected void initializeRakNet(TransportType transportType, Bootstrap bootstrap) {
        if (!DatagramChannel.class.isAssignableFrom(transportType.udpClientChannelClass())) {
            throw new IllegalArgumentException("Channel type must be a DatagramChannel");
        }
        if (transportType == TransportType.KQUEUE) {
            transportType = TransportType.NIO;
        }

        final RakChannelFactory<RakClientChannel> channelFactory = RakChannelFactory.client((Class<? extends DatagramChannel>) transportType.udpClientChannelClass());
        final TransportType finalTransportType = transportType;
        bootstrap
                .group(EventLoops.getClientEventLoop(finalTransportType))
                .channelFactory(() -> {
                    final Channel channel = channelFactory.newChannel();
                    if (channel.config().setOption(RakChannelOption.RAK_IP_DONT_FRAGMENT, true)) {
                        channel.config().setOption(RakChannelOption.RAK_MTU_SIZES, new Integer[]{1492, 1200, 576});
                    }
                    return channel;
                })
                .option(RakChannelOption.RAK_PROTOCOL_VERSION, ProtocolConstants.BEDROCK_RAKNET_PROTOCOL_VERSION)
                .option(RakChannelOption.RAK_COMPATIBILITY_MODE, true)
                .option(RakChannelOption.RAK_CLIENT_INTERNAL_ADDRESSES, 20)
                .option(RakChannelOption.RAK_TIME_BETWEEN_SEND_CONNECTION_ATTEMPTS_MS, 500)
                .option(RakChannelOption.RAK_CONNECT_TIMEOUT, (long) Reactor.getInstance().getReactorConfig().getConnectTimeout())
                .option(RakChannelOption.RAK_SESSION_TIMEOUT, 30_000L)
                .option(RakChannelOption.RAK_GUID, ThreadLocalRandom.current().nextLong());
    }

    protected void initializeRaw(TransportType transportType, Bootstrap bootstrap) {
        if (transportType == TransportType.KQUEUE) {
            transportType = TransportType.NIO;
        }

        bootstrap
                .group(EventLoops.getClientEventLoop(transportType))
                .channel(transportType.udpClientChannelClass());
    }
}
