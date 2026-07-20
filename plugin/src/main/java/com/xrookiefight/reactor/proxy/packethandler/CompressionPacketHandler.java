package com.xrookiefight.reactor.proxy.packethandler;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.xrookiefight.reactor.Reactor;
import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import com.xrookiefight.reactor.proxy.util.ChannelUtil;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginCompressionPacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginGameProfilePacket;
import net.raphimc.netminecraft.packet.impl.play.S2CPlaySetCompressionPacket;

import java.util.List;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
public class CompressionPacketHandler extends PacketHandler {

    public CompressionPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public boolean handleP2S(Packet packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof S2CPlaySetCompressionPacket setCompressionPacket) {
            log.debug("Server changed compression threshold to: {}", setCompressionPacket.compressionThreshold);
            this.proxyConnection.getChannel().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(setCompressionPacket.compressionThreshold);
            return false;
        } else if (packet instanceof S2CLoginGameProfilePacket) {
            if (this.proxyConnection.getClientVersion().newerThanOrEqualTo(ProtocolVersion.v1_8)) {
                var config = Reactor.getInstance().getReactorConfig();
                int configThreshold = config.getCompressionThreshold();
                Integer currentThreshold = this.proxyConnection.getC2P().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).get();

                if (configThreshold > -1 && (currentThreshold == null || currentThreshold == -1)) {
                    ChannelUtil.disableAutoRead(this.proxyConnection.getChannel());

                    this.proxyConnection.getC2P().writeAndFlush(new S2CLoginCompressionPacket(configThreshold)).addListeners(
                            ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE,
                            (ChannelFutureListener) f -> {
                                if (f.isSuccess()) {
                                    log.debug("Enabled client compression with threshold: {}", configThreshold);
                                    this.proxyConnection.getC2P().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(configThreshold);
                                    ChannelUtil.restoreAutoRead(this.proxyConnection.getChannel());
                                }
                            }
                    );
                }
            }
        } else if (packet instanceof S2CLoginCompressionPacket loginCompressionPacket) {
            log.debug("Server enabled compression with threshold: {}", loginCompressionPacket.compressionThreshold);
            this.proxyConnection.getChannel().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(loginCompressionPacket.compressionThreshold);
            return false;
        }

        return true;
    }
}
