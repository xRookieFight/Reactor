package com.xrookiefight.reactor.proxy.packethandler;

import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import com.xrookiefight.reactor.proxy.util.ChannelUtil;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.impl.configuration.C2SConfigFinishConfigurationPacket;
import net.raphimc.netminecraft.packet.impl.configuration.S2CConfigFinishConfigurationPacket;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginAcknowledgedPacket;
import net.raphimc.netminecraft.packet.impl.play.C2SPlayConfigurationAcknowledgedPacket;
import net.raphimc.netminecraft.packet.impl.play.S2CPlayStartConfigurationPacket;

import java.util.List;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
public class ConfigurationPacketHandler extends PacketHandler {

    public ConfigurationPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public boolean handleC2P(Packet packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof C2SLoginAcknowledgedPacket) {
            this.proxyConnection.setC2pConnectionState(ConnectionState.CONFIGURATION);
            listeners.add(f -> {
                if (f.isSuccess()) {
                    this.proxyConnection.setP2sConnectionState(ConnectionState.CONFIGURATION);
                    ChannelUtil.restoreAutoRead(this.proxyConnection.getChannel());
                }
            });
        } else if (packet instanceof C2SConfigFinishConfigurationPacket) {
            this.proxyConnection.setC2pConnectionState(ConnectionState.PLAY);
            listeners.add(f -> {
                if (f.isSuccess()) {
                    log.info("Configuration finished for {}. Switching to PLAY state", this.proxyConnection.getClientAddressString());
                    this.proxyConnection.setP2sConnectionState(ConnectionState.PLAY);
                    ChannelUtil.restoreAutoRead(this.proxyConnection.getChannel());
                }
            });
        } else if (packet instanceof C2SPlayConfigurationAcknowledgedPacket) {
            this.proxyConnection.setC2pConnectionState(ConnectionState.CONFIGURATION);
            listeners.add(f -> {
                if (f.isSuccess()) {
                    log.info("Switching to CONFIGURATION state for {}", this.proxyConnection.getClientAddressString());
                    this.proxyConnection.setP2sConnectionState(ConnectionState.CONFIGURATION);
                    ChannelUtil.restoreAutoRead(this.proxyConnection.getChannel());
                }
            });
        }

        return true;
    }

    @Override
    public boolean handleP2S(Packet packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof S2CConfigFinishConfigurationPacket) {
            ChannelUtil.disableAutoRead(this.proxyConnection.getChannel());
        } else if (packet instanceof S2CPlayStartConfigurationPacket) {
            ChannelUtil.disableAutoRead(this.proxyConnection.getChannel());
        }

        return true;
    }
}
