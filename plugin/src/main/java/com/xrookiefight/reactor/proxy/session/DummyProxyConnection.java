package com.xrookiefight.reactor.proxy.session;

import io.netty.channel.Channel;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
public class DummyProxyConnection extends ProxyConnection {

    public DummyProxyConnection(Channel c2p) {
        super(null, c2p);
    }
}
