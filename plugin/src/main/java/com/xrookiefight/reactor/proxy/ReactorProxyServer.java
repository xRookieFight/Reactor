package com.xrookiefight.reactor.proxy;

import com.xrookiefight.reactor.Reactor;
import com.xrookiefight.reactor.proxy.client2proxy.Client2ProxyChannelInitializer;
import com.xrookiefight.reactor.proxy.client2proxy.Client2ProxyHandler;
import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.NetServer;

import java.net.InetSocketAddress;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
@RequiredArgsConstructor
public class ReactorProxyServer {

    private final Reactor plugin;

    @Getter
    private final ChannelGroup connectedClients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private NetServer netServer;

    public void start() {
        final String host = plugin.getReactorConfig().getBindAddress();
        final int port = plugin.getReactorConfig().getBindPort();
        final InetSocketAddress address = new InetSocketAddress(host, port);

        MCPipeline.useOptimizedPipeline();

        netServer = new NetServer(new Client2ProxyChannelInitializer(Client2ProxyHandler::new));
        netServer.bind(address, false);

        log.info("Reactor proxy server started on {}:{}", host, port);
    }

    public void shutdown() {
        log.info("Shutting down Reactor proxy server...");

        if (netServer != null) {
            for (Channel channel : connectedClients) {
                try {
                    ProxyConnection proxyConnection = ProxyConnection.fromChannel(channel);
                    if (proxyConnection != null) {
                        proxyConnection.kickClient("Reactor is shutting down");
                    }
                } catch (Throwable ignored) {
                }
            }

            if (netServer.getChannel() != null) {
                netServer.getChannel().close();
            }
        }

        log.info("Reactor proxy server stopped");
    }
}
