package com.xrookiefight.reactor.proxy.session;

import com.google.common.net.HostAndPort;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import com.xrookiefight.reactor.Reactor;
import com.xrookiefight.reactor.proxy.packethandler.PacketHandler;
import com.xrookiefight.reactor.proxy.util.CloseAndReturn;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.util.AttributeKey;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.mcstructs.text.components.StringComponent;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.NetClient;
import net.raphimc.netminecraft.packet.impl.configuration.S2CConfigDisconnectPacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginDisconnectPacket;
import net.raphimc.netminecraft.packet.impl.play.S2CPlayDisconnectPacket;
import net.raphimc.netminecraft.packet.impl.status.S2CStatusResponsePacket;
import net.raphimc.netminecraft.packet.registry.DefaultPacketRegistry;
import net.raphimc.netminecraft.util.TransportType;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
@Getter
public class ProxyConnection extends NetClient {

    public static final AttributeKey<ProxyConnection> PROXY_CONNECTION_ATTRIBUTE_KEY = AttributeKey.valueOf("proxy_connection");

    private final Channel c2p;
    private final List<PacketHandler> packetHandlers = new ArrayList<>();

    @Setter
    private SocketAddress serverAddress;
    @Setter
    private ProtocolVersion serverVersion;
    private ProtocolVersion clientVersion;
    @Setter
    private HostAndPort clientHandshakeAddress;
    @Setter
    private String username;
    @Setter
    private UUID playerUuid;
    @Setter
    private UserConnection userConnection;
    private ConnectionState c2pConnectionState = ConnectionState.HANDSHAKING;
    private ConnectionState p2sConnectionState = ConnectionState.HANDSHAKING;

    public ProxyConnection(ChannelInitializer<Channel> channelInitializer, Channel c2p) {
        super(channelInitializer);
        this.c2p = c2p;
    }

    public static ProxyConnection fromChannel(Channel channel) {
        return channel.attr(PROXY_CONNECTION_ATTRIBUTE_KEY).get();
    }

    public static ProxyConnection fromUserConnection(UserConnection userConnection) {
        return fromChannel(userConnection.getChannel());
    }

    @Override
    @Deprecated
    public ChannelFuture connect(SocketAddress address) {
        throw new UnsupportedOperationException("Use connectToServer instead");
    }

    @Override
    public void initialize(TransportType transportType, Bootstrap bootstrap) {
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Reactor.getInstance().getReactorConfig().getConnectTimeout());
        bootstrap.attr(PROXY_CONNECTION_ATTRIBUTE_KEY, this);
        super.initialize(transportType, bootstrap);
    }

    public ChannelFuture connectToServer(SocketAddress serverAddress, ProtocolVersion targetVersion) {
        this.serverAddress = serverAddress;
        this.serverVersion = targetVersion;
        return super.connect(serverAddress);
    }

    public Channel getC2P() {
        return c2p;
    }

    public void setClientVersion(ProtocolVersion clientVersion) {
        this.clientVersion = clientVersion;
        this.c2p.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(new DefaultPacketRegistry(false, clientVersion.getVersion()));
    }

    public void setC2pConnectionState(ConnectionState connectionState) {
        this.c2pConnectionState = connectionState;
        this.c2p.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).get().setConnectionState(connectionState);
    }

    public void setP2sConnectionState(ConnectionState connectionState) {
        this.p2sConnectionState = connectionState;
        if (this.getChannel() != null) {
            this.getChannel().attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).get().setConnectionState(connectionState);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends PacketHandler> T getPacketHandler(Class<T> packetHandlerType) {
        for (PacketHandler packetHandler : this.packetHandlers) {
            if (packetHandlerType.isInstance(packetHandler)) {
                return (T) packetHandler;
            }
        }
        return null;
    }

    public void kickClient(String message) throws CloseAndReturn {
        log.warn("Kicking client {}: {}", getClientAddressString(), message);

        final ChannelFuture future;
        if (this.c2pConnectionState == ConnectionState.STATUS) {
            future = this.c2p.writeAndFlush(new S2CStatusResponsePacket("{\"players\":{\"max\":0,\"online\":0},\"description\":" + new JsonPrimitive(message) + ",\"version\":{\"protocol\":-1,\"name\":\"Reactor\"}}"));
        } else if (this.c2pConnectionState == ConnectionState.LOGIN) {
            future = this.c2p.writeAndFlush(new S2CLoginDisconnectPacket(new StringComponent(message)));
        } else if (this.c2pConnectionState == ConnectionState.CONFIGURATION) {
            future = this.c2p.writeAndFlush(new S2CConfigDisconnectPacket(new StringComponent(message)));
        } else if (this.c2pConnectionState == ConnectionState.PLAY) {
            future = this.c2p.writeAndFlush(new S2CPlayDisconnectPacket(new StringComponent(message)));
        } else {
            future = this.c2p.newSucceededFuture();
        }

        future.addListener(ChannelFutureListener.CLOSE);
        throw CloseAndReturn.INSTANCE;
    }

    public boolean isClosed() {
        return !this.c2p.isOpen() || (this.getChannel() != null && !this.getChannel().isOpen());
    }

    public String getClientAddressString() {
        if (c2p.remoteAddress() == null) {
            return "unknown";
        }

        if (!Reactor.getInstance().getReactorConfig().isLogIps()) {
            return "<ip-hidden>";
        }

        return c2p.remoteAddress().toString();
    }
}
