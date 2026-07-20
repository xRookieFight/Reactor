package com.xrookiefight.reactor.protocol.provider;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.xrookiefight.reactor.Reactor;
import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import net.raphimc.viabedrock.protocol.provider.SkinProvider;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
public class ReactorSkinProvider extends SkinProvider {

    @Override
    public Map<String, Object> getClientPlayerSkin(UserConnection user) {
        Map<String, Object> claims = new LinkedHashMap<>(super.getClientPlayerSkin(user));
        claims.putIfAbsent("ClientIsEditorCapable", claims.getOrDefault("IsEditorMode", Boolean.FALSE));
        claims.putIfAbsent("ClientEditorConnectionIntent", 0);

        if (Reactor.getInstance().getReactorConfig().isOnlineMode()) {
            appendWaterdogIdentity(claims, ProxyConnection.fromUserConnection(user));
        }

        return claims;
    }

    private void appendWaterdogIdentity(Map<String, Object> claims, ProxyConnection proxyConnection) {
        if (proxyConnection == null || proxyConnection.getPlayerUuid() == null) {
            return;
        }

        claims.put("Waterdog_XUID", deriveXuid(proxyConnection.getPlayerUuid()));
        if (proxyConnection.getC2P().remoteAddress() instanceof InetSocketAddress address) {
            claims.put("Waterdog_IP", address.getAddress().getHostAddress());
        }
    }

    private String deriveXuid(UUID uuid) {
        long value = Math.abs(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());
        return String.valueOf(2_000_000_000_000_000L + (value % 1_000_000_000_000_000L));
    }
}
