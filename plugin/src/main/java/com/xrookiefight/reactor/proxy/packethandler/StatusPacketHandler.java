package com.xrookiefight.reactor.proxy.packethandler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xrookiefight.reactor.Reactor;
import com.xrookiefight.reactor.config.ReactorConfig;
import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.impl.status.C2SStatusPingRequestPacket;
import net.raphimc.netminecraft.packet.impl.status.C2SStatusRequestPacket;
import net.raphimc.netminecraft.packet.impl.status.S2CStatusPongResponsePacket;
import net.raphimc.netminecraft.packet.impl.status.S2CStatusResponsePacket;

import java.util.List;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
public class StatusPacketHandler extends PacketHandler {

    public StatusPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public boolean handleC2P(Packet packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof C2SStatusRequestPacket) {
            log.debug("Client requested server status");
        } else if (packet instanceof C2SStatusPingRequestPacket pingPacket) {
            log.debug("Client ping request: {}", pingPacket.startTime);
        }
        return true;
    }

    @Override
    public boolean handleP2S(Packet packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof S2CStatusPongResponsePacket) {
            listeners.add(ChannelFutureListener.CLOSE);
        } else if (packet instanceof S2CStatusResponsePacket statusResponsePacket) {
            ReactorConfig config = Reactor.getInstance().getReactorConfig();

            if (config.isLogStatusRequests()) {
                log.info("Sending status response to {}", this.proxyConnection.getClientAddressString());
            }

            String customMotd = config.getMotd();
            int maxPlayers = config.getMaxPlayers();

            if (!customMotd.isEmpty() || maxPlayers >= 0) {
                try {
                    JsonObject statusJson = JsonParser.parseString(statusResponsePacket.statusJson).getAsJsonObject();

                    if (!customMotd.isEmpty()) {
                        statusJson.addProperty("description", customMotd.replace('&', '§'));
                    }

                    if (maxPlayers >= 0) {
                        JsonObject players = statusJson.getAsJsonObject("players");
                        if (players != null) {
                            players.addProperty("max", maxPlayers);
                        }
                    }

                    statusResponsePacket.statusJson = statusJson.toString();
                } catch (Exception e) {
                    log.warn("Failed to modify status response JSON", e);
                }
            }
        }

        return true;
    }
}
