package com.xrookiefight.reactor.proxy.packethandler;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.xrookiefight.reactor.Reactor;
import com.xrookiefight.reactor.auth.MojangSessionService;
import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import com.xrookiefight.reactor.proxy.util.ChannelUtil;
import com.xrookiefight.reactor.proxy.util.CloseAndReturn;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.crypto.AESEncryption;
import net.raphimc.netminecraft.netty.crypto.CryptUtil;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginAcknowledgedPacket;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginKeyPacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginCompressionPacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginDisconnectPacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginGameProfilePacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginHelloPacket;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
public class LoginPacketHandler extends PacketHandler {

    private C2SLoginHelloPacket pendingHello;
    private byte[] verifyNonce;

    public LoginPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public boolean handleC2P(Packet packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof C2SLoginHelloPacket helloPacket) {
            this.proxyConnection.setUsername(helloPacket.name);
            this.proxyConnection.setPlayerUuid(helloPacket.uuid);
            log.info("Player {} (UUID: {}) logging in", helloPacket.name, helloPacket.uuid);

            if (Reactor.getInstance().getReactorConfig().isOnlineMode()) {
                this.beginAuthentication(helloPacket);
                return false;
            }
        } else if (packet instanceof C2SLoginKeyPacket keyPacket && this.pendingHello != null) {
            this.finishAuthentication(keyPacket);
            return false;
        } else if (packet instanceof C2SLoginAcknowledgedPacket) {
            log.debug("Client acknowledged login success");
        }
        return true;
    }

    @Override
    public boolean handleP2S(Packet packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof S2CLoginHelloPacket) {
            log.debug("Server encryption request");
        } else if (packet instanceof S2CLoginGameProfilePacket profilePacket) {
            final ConnectionState nextState = this.proxyConnection.getClientVersion().newerThanOrEqualTo(ProtocolVersion.v1_20_2)
                    ? ConnectionState.CONFIGURATION
                    : ConnectionState.PLAY;

            log.info("Login success for player {} (UUID: {}). Next state: {}", profilePacket.name, profilePacket.uuid, nextState);
            this.proxyConnection.setUsername(profilePacket.name);
            this.proxyConnection.setPlayerUuid(profilePacket.uuid);

            ChannelUtil.disableAutoRead(this.proxyConnection.getChannel());
            listeners.add(f -> {
                if (f.isSuccess() && nextState != ConnectionState.CONFIGURATION) {
                    this.proxyConnection.setC2pConnectionState(nextState);
                    this.proxyConnection.setP2sConnectionState(nextState);
                    ChannelUtil.restoreAutoRead(this.proxyConnection.getChannel());
                }
            });
        } else if (packet instanceof S2CLoginDisconnectPacket disconnectPacket) {
            log.info("Login disconnect: {}", disconnectPacket.reason);
        } else if (packet instanceof S2CLoginCompressionPacket compressionPacket) {
            log.debug("Server enabled compression with threshold: {}", compressionPacket.compressionThreshold);
        }
        return true;
    }

    private void beginAuthentication(C2SLoginHelloPacket helloPacket) {
        this.pendingHello = helloPacket;
        this.verifyNonce = new byte[4];
        ThreadLocalRandom.current().nextBytes(this.verifyNonce);

        final MojangSessionService sessionService = Reactor.getInstance().getSessionService();
        this.proxyConnection.getC2P().writeAndFlush(new S2CLoginHelloPacket(
                "",
                sessionService.getKeyPair().getPublic().getEncoded(),
                this.verifyNonce,
                true
        )).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    private void finishAuthentication(C2SLoginKeyPacket keyPacket) {
        final MojangSessionService sessionService = Reactor.getInstance().getSessionService();

        try {
            if (keyPacket.encryptedNonce != null) {
                final byte[] decryptedNonce = CryptUtil.decryptData(sessionService.getKeyPair().getPrivate(), keyPacket.encryptedNonce);
                if (!MessageDigest.isEqual(this.verifyNonce, decryptedNonce)) {
                    this.proxyConnection.kickClient("Invalid encryption nonce!");
                }
            } else {
                if (this.pendingHello.key == null || keyPacket.salt == null
                        || !CryptUtil.verifySignedNonce(this.pendingHello.key, this.verifyNonce, keyPacket.salt, keyPacket.signature)) {
                    this.proxyConnection.kickClient("Invalid nonce signature!");
                }
            }

            final var secretKey = CryptUtil.decryptSecretKey(sessionService.getKeyPair().getPrivate(), keyPacket.encryptedSecretKey);
            this.proxyConnection.getC2P().attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).set(new AESEncryption(secretKey));

            final String serverIdHash = new BigInteger(
                    CryptUtil.computeServerIdHash("", sessionService.getKeyPair().getPublic(), secretKey)
            ).toString(16);

            ChannelUtil.disableAutoRead(this.proxyConnection.getC2P());

            sessionService.hasJoined(this.pendingHello.name, serverIdHash).whenComplete((profile, error) ->
                    this.proxyConnection.getC2P().eventLoop().submit(() -> this.completeLogin(profile, error)));
        } catch (CloseAndReturn e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to process encryption response", e);
            this.proxyConnection.kickClient("Failed to process encryption response: " + e.getMessage());
        }
    }

    private void completeLogin(MojangSessionService.GameProfile profile, Throwable error) {
        try {
            if (error != null || profile == null) {
                if (error != null) {
                    log.error("Session server lookup failed for {}", this.pendingHello.name, error);
                }
                this.proxyConnection.kickClient("Mojang authentication failed! Please restart your game and try again.");
            }

            log.info("Player {} authenticated with Mojang as {} (UUID: {})", this.pendingHello.name, profile.name(), profile.uuid());

            this.pendingHello.name = profile.name();
            this.pendingHello.uuid = profile.uuid();
            this.proxyConnection.setUsername(profile.name());
            this.proxyConnection.setPlayerUuid(profile.uuid());

            final C2SLoginHelloPacket forwardedHello = this.pendingHello;
            this.pendingHello = null;

            this.proxyConnection.getChannel().writeAndFlush(forwardedHello).addListeners(
                    ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE,
                    (ChannelFutureListener) f -> {
                        if (f.isSuccess()) {
                            ChannelUtil.restoreAutoRead(this.proxyConnection.getC2P());
                        }
                    }
            );
        } catch (CloseAndReturn ignored) {
        } catch (Throwable e) {
            log.error("Failed to complete authenticated login", e);
            try {
                this.proxyConnection.kickClient("Login failed: " + e.getMessage());
            } catch (CloseAndReturn ignored) {
            }
        }
    }
}
