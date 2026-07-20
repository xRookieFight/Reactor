package com.xrookiefight.reactor.protocol.provider;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionType;
import com.viaversion.viaversion.protocol.SpecialProtocolVersion;
import com.viaversion.viaversion.protocol.version.BaseVersionProvider;
import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import lombok.extern.slf4j.Slf4j;
import net.raphimc.netminecraft.constants.MCVersion;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
public class ReactorVersionProvider extends BaseVersionProvider {

    @Override
    public ProtocolVersion getClientProtocol(UserConnection connection) {
        final ProtocolVersion clientProtocol = connection.getProtocolInfo().protocolVersion();
        if (!clientProtocol.isKnown() && ProtocolVersion.isRegistered(VersionType.SPECIAL, clientProtocol.getOriginalVersion())) {
            return ProtocolVersion.getProtocol(VersionType.SPECIAL, clientProtocol.getOriginalVersion());
        }
        return super.getClientProtocol(connection);
    }

    @Override
    public ProtocolVersion getClosestServerProtocol(UserConnection connection) {
        final ProtocolVersion clientProtocol = connection.getProtocolInfo().protocolVersion();

        if (connection.isClientSide()) {
            return ProxyConnection.fromUserConnection(connection).getServerVersion();
        }

        if (clientProtocol.getVersionType() == VersionType.RELEASE) {
            if (MCVersion.ALL_VERSIONS.containsKey(clientProtocol.getVersion())) {
                return clientProtocol;
            } else {
                return MCVersion.ALL_VERSIONS.keySet().stream()
                        .min((o1, o2) -> {
                            final int diff1 = Math.abs(o1 - clientProtocol.getVersion());
                            final int diff2 = Math.abs(o2 - clientProtocol.getVersion());
                            return Integer.compare(diff1, diff2);
                        })
                        .map(ProtocolVersion::getProtocol)
                        .orElse(ProtocolVersion.unknown);
            }
        } else if (clientProtocol instanceof SpecialProtocolVersion specialProtocolVersion) {
            return specialProtocolVersion.getDelegate();
        }

        return ProtocolVersion.v1_7_2;
    }
}
