package com.xrookiefight.reactor.protocol.provider;

import com.viaversion.viaversion.api.connection.ProtocolInfo;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionType;
import com.viaversion.viaversion.protocol.SpecialProtocolVersion;
import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
class ReactorVersionProviderTest {

    private final ReactorVersionProvider provider = new ReactorVersionProvider();

    private UserConnection connectionWith(ProtocolVersion version, boolean clientSide) {
        UserConnection user = mock(UserConnection.class);
        ProtocolInfo info = mock(ProtocolInfo.class);
        when(user.getProtocolInfo()).thenReturn(info);
        when(info.protocolVersion()).thenReturn(version);
        when(user.isClientSide()).thenReturn(clientSide);
        return user;
    }

    @Test
    void clientSideUsesProxyConnectionServerVersion() {
        UserConnection user = connectionWith(ProtocolVersion.v1_21, true);
        EmbeddedChannel channel = new EmbeddedChannel();
        ProxyConnection proxyConnection = mock(ProxyConnection.class);
        when(proxyConnection.getServerVersion()).thenReturn(ProtocolVersion.v1_20_5);
        channel.attr(ProxyConnection.PROXY_CONNECTION_ATTRIBUTE_KEY).set(proxyConnection);
        when(user.getChannel()).thenReturn(channel);

        assertEquals(ProtocolVersion.v1_20_5, provider.getClosestServerProtocol(user));
    }

    @Test
    void knownReleaseVersionIsReturnedAsIs() {
        UserConnection user = connectionWith(ProtocolVersion.v1_8, false);
        assertEquals(ProtocolVersion.v1_8, provider.getClosestServerProtocol(user));
    }

    @Test
    void modernReleaseVersionIsReturnedAsIs() {
        UserConnection user = connectionWith(ProtocolVersion.v1_21, false);
        assertEquals(ProtocolVersion.v1_21, provider.getClosestServerProtocol(user));
    }

    @Test
    void unknownVersionTypeFallsBackTo172() {
        UserConnection user = connectionWith(ProtocolVersion.unknown, false);
        assertEquals(ProtocolVersion.v1_7_2, provider.getClosestServerProtocol(user));
    }

    @Test
    void knownClientProtocolDelegatesToBase() {
        UserConnection user = connectionWith(ProtocolVersion.v1_21, false);
        assertDoesNotThrow(() -> provider.getClientProtocol(user));
    }

    @Test
    void unknownClientProtocolResolvesRegisteredSpecialVersion() {
        if (!ProtocolVersion.isRegistered(VersionType.SPECIAL, 987654)) {
            ProtocolVersion.register(new ProtocolVersion(VersionType.SPECIAL, 987654, -1, "TestSpecial", null));
        }
        ProtocolVersion unregistered = ProtocolVersion.getProtocol(987654);
        UserConnection user = connectionWith(unregistered, false);

        ProtocolVersion resolved = provider.getClientProtocol(user);
        assertEquals(987654, resolved.getOriginalVersion());
    }

    @Test
    void unknownReleaseVersionFallsBackToClosestKnown() {
        ProtocolVersion futureRelease = new ProtocolVersion(VersionType.RELEASE, 987655, -1, "FutureRelease", null);
        UserConnection user = connectionWith(futureRelease, false);

        ProtocolVersion closest = provider.getClosestServerProtocol(user);
        assertEquals(VersionType.RELEASE, closest.getVersionType());
        assertTrue(closest.getVersion() > 0);
    }

    @Test
    void specialProtocolVersionUsesDelegate() {
        SpecialProtocolVersion special = new SpecialProtocolVersion(987656, "AprilFools", ProtocolVersion.v1_20_2);
        UserConnection user = connectionWith(special, false);

        assertEquals(ProtocolVersion.v1_20_2, provider.getClosestServerProtocol(user));
    }
}
