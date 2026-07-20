package com.xrookiefight.reactor.proxy.client2proxy;

import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.xrookiefight.reactor.Reactor;
import com.xrookiefight.reactor.proxy.packethandler.CompressionPacketHandler;
import com.xrookiefight.reactor.proxy.packethandler.ConfigurationPacketHandler;
import com.xrookiefight.reactor.proxy.packethandler.DisconnectPacketHandler;
import com.xrookiefight.reactor.proxy.packethandler.LoginPacketHandler;
import com.xrookiefight.reactor.proxy.packethandler.PacketHandler;
import com.xrookiefight.reactor.proxy.packethandler.StatusPacketHandler;
import com.xrookiefight.reactor.proxy.packethandler.UnexpectedPacketHandler;
import com.xrookiefight.reactor.proxy.proxy2server.Proxy2ServerChannelInitializer;
import com.xrookiefight.reactor.proxy.proxy2server.Proxy2ServerHandler;
import com.xrookiefight.reactor.proxy.session.BedrockProxyConnection;
import com.xrookiefight.reactor.proxy.session.DummyProxyConnection;
import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import com.xrookiefight.reactor.proxy.util.ChannelUtil;
import com.xrookiefight.reactor.proxy.util.CloseAndReturn;
import com.xrookiefight.reactor.proxy.util.ExceptionUtil;
import com.xrookiefight.reactor.proxy.util.ThrowingChannelFutureListener;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.IntendedState;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.impl.handshaking.C2SHandshakingClientIntentionPacket;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.nio.channels.UnresolvedAddressException;
import java.util.List;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
public class Client2ProxyHandler extends SimpleChannelInboundHandler<Packet> {

    private ProxyConnection proxyConnection;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.proxyConnection = new DummyProxyConnection(ctx.channel());
        Reactor.getInstance().getProxyServer().getConnectedClients().add(ctx.channel());
        log.info("Client connected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if (this.proxyConnection instanceof DummyProxyConnection) return;

        try {
            final var serverChannel = this.proxyConnection.getChannel();
            if (serverChannel != null && serverChannel.isActive()) {
                serverChannel.flush();
                serverChannel.close();
            }
        } catch (Throwable ignored) {
        }

        log.info("Client disconnected: {}", ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        if (this.proxyConnection.isClosed()) return;

        if (this.proxyConnection.getC2pConnectionState() == ConnectionState.HANDSHAKING) {
            if (packet instanceof C2SHandshakingClientIntentionPacket handshakePacket) {
                this.handleHandshake(handshakePacket);
            } else {
                throw new IllegalStateException("Unexpected packet in HANDSHAKING state");
            }
            return;
        }

        final List<ChannelFutureListener> listeners = Lists.newArrayList(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        for (PacketHandler packetHandler : this.proxyConnection.getPacketHandlers()) {
            if (!packetHandler.handleC2P(packet, listeners)) {
                return;
            }
        }

        this.proxyConnection.getChannel().writeAndFlush(packet).addListeners(listeners.toArray(new ChannelFutureListener[0]));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ExceptionUtil.handleNettyException(ctx, cause, this.proxyConnection, true);
    }

    private void handleHandshake(C2SHandshakingClientIntentionPacket packet) {
        final ProtocolVersion clientVersion = ProtocolVersion.getProtocol(packet.protocolVersion);

        if (packet.intendedState == null) {
            throw CloseAndReturn.INSTANCE;
        }

        this.proxyConnection.setClientVersion(clientVersion);
        this.proxyConnection.setC2pConnectionState(packet.intendedState.getConnectionState());

        if (!ProtocolVersion.isRegistered(clientVersion.getVersionType(), clientVersion.getOriginalVersion())) {
            this.proxyConnection.kickClient("Your client version is not supported!");
        }

        final Reactor plugin = Reactor.getInstance();
        final InetSocketAddress serverAddress = new InetSocketAddress(
                plugin.getReactorConfig().getBedrockHost(),
                plugin.getReactorConfig().getBedrockPort()
        );
        final ProtocolVersion serverVersion = BedrockProtocolVersion.bedrockLatest;

        final String[] handshakeParts = new String[]{packet.address};

        HostAndPort clientHandshakeAddress;
        try {
            clientHandshakeAddress = HostAndPort.fromParts(handshakeParts[0], packet.port);
        } catch (Throwable e) {
            clientHandshakeAddress = null;
        }

        ChannelUtil.disableAutoRead(this.proxyConnection.getC2P());

        this.connect(serverAddress, serverVersion, clientVersion, packet.intendedState, clientHandshakeAddress, handshakeParts);
    }

    private void connect(
            InetSocketAddress serverAddress,
            ProtocolVersion serverVersion,
            ProtocolVersion clientVersion,
            IntendedState intendedState,
            HostAndPort clientHandshakeAddress,
            String[] handshakeParts
    ) {
        final BedrockProxyConnection bedrockProxyConnection = new BedrockProxyConnection(
                new Proxy2ServerChannelInitializer(Proxy2ServerHandler::new),
                this.proxyConnection.getC2P()
        );

        this.proxyConnection = bedrockProxyConnection;
        this.proxyConnection.getC2P().attr(ProxyConnection.PROXY_CONNECTION_ATTRIBUTE_KEY).set(this.proxyConnection);
        this.proxyConnection.setClientVersion(clientVersion);
        this.proxyConnection.setClientHandshakeAddress(clientHandshakeAddress);
        this.proxyConnection.setC2pConnectionState(intendedState.getConnectionState());

        this.proxyConnection.getPacketHandlers().add(new StatusPacketHandler(this.proxyConnection));
        this.proxyConnection.getPacketHandlers().add(new CompressionPacketHandler(this.proxyConnection));
        this.proxyConnection.getPacketHandlers().add(new LoginPacketHandler(this.proxyConnection));
        this.proxyConnection.getPacketHandlers().add(new DisconnectPacketHandler(this.proxyConnection));
        if (clientVersion.newerThanOrEqualTo(ProtocolVersion.v1_20_2) || serverVersion.newerThanOrEqualTo(ProtocolVersion.v1_20_2)) {
            this.proxyConnection.getPacketHandlers().add(new ConfigurationPacketHandler(this.proxyConnection));
        }
        this.proxyConnection.getPacketHandlers().add(new UnexpectedPacketHandler(this.proxyConnection));

        log.info("[{} <-> {}] Connecting to {}", clientVersion.getName(), serverVersion.getName(), serverAddress);

        final int handshakePort = clientHandshakeAddress != null ? clientHandshakeAddress.getPort() : 25565;

        this.proxyConnection.connectToServer(serverAddress, serverVersion).addListeners((ThrowingChannelFutureListener) f -> {
            if (f.isSuccess()) {
                f.channel().eventLoop().submit(() -> {
                    final C2SHandshakingClientIntentionPacket newHandshakePacket = new C2SHandshakingClientIntentionPacket(
                            clientVersion.getOriginalVersion(),
                            String.join("\0", handshakeParts),
                            handshakePort,
                            intendedState
                    );
                    this.proxyConnection.getChannel().writeAndFlush(newHandshakePacket).addListeners(
                            ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE,
                            (ChannelFutureListener) f2 -> {
                                if (f2.isSuccess()) {
                                    this.proxyConnection.setP2sConnectionState(intendedState.getConnectionState());
                                    ChannelUtil.restoreAutoRead(this.proxyConnection.getC2P());
                                }
                            }
                    );
                });
            }
        }, (ThrowingChannelFutureListener) f -> {
            if (!f.isSuccess()) {
                if (f.cause() instanceof ConnectException || f.cause() instanceof UnresolvedAddressException || f.cause() instanceof PortUnreachableException) {
                    this.proxyConnection.kickClient("Could not connect to the Bedrock server!");
                } else {
                    log.error("Error while connecting to the backend server", f.cause());
                    this.proxyConnection.kickClient("An error occurred while connecting: " + f.cause().getMessage());
                }
            }
        });
    }
}
