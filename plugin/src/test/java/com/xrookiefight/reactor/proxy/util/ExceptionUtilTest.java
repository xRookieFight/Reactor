package com.xrookiefight.reactor.proxy.util;

import com.xrookiefight.reactor.TestSupport;
import com.xrookiefight.reactor.proxy.session.ProxyConnection;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
class ExceptionUtilTest {

    @AfterEach
    void tearDown() {
        TestSupport.clearMockReactor();
    }

    private ChannelHandlerContext contextFor(EmbeddedChannel channel) {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        when(ctx.channel()).thenReturn(channel);
        return ctx;
    }

    @Test
    void ignoresClosedChannel() {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.close();
        ExceptionUtil.handleNettyException(contextFor(channel), new RuntimeException("x"), null, true);
        assertFalse(channel.isOpen());
    }

    @Test
    void ignoresClosedChannelException() {
        EmbeddedChannel channel = new EmbeddedChannel();
        ExceptionUtil.handleNettyException(contextFor(channel), new ClosedChannelException(), null, true);
        assertTrue(channel.isOpen());
    }

    @Test
    void closeAndReturnClosesChannel() {
        EmbeddedChannel channel = new EmbeddedChannel();
        ExceptionUtil.handleNettyException(contextFor(channel), CloseAndReturn.INSTANCE, null, true);
        assertFalse(channel.isOpen());
    }

    @Test
    void ioExceptionClosesQuietly() {
        TestSupport.installMockReactor(TestSupport.defaultConfig());
        EmbeddedChannel channel = new EmbeddedChannel();
        ProxyConnection proxyConnection = mock(ProxyConnection.class);
        when(proxyConnection.getClientAddressString()).thenReturn("client");

        ExceptionUtil.handleNettyException(contextFor(channel), new IOException("Connection reset"), proxyConnection, true);
        assertFalse(channel.isOpen());
    }

    @Test
    void ioExceptionWithoutConnectionUsesRemoteAddress() {
        TestSupport.installMockReactor(TestSupport.defaultConfig());
        EmbeddedChannel channel = new EmbeddedChannel();

        ExceptionUtil.handleNettyException(contextFor(channel), new IOException("reset"), null, true);
        assertFalse(channel.isOpen());
    }

    @Test
    void suppressedClientErrorSkipsKick() {
        TestSupport.installMockReactor(TestSupport.config(Map.of("protocol-settings.suppress-client-protocol-errors", true)));
        EmbeddedChannel channel = new EmbeddedChannel();
        ProxyConnection proxyConnection = mock(ProxyConnection.class);

        ExceptionUtil.handleNettyException(contextFor(channel), new RuntimeException("boom"), proxyConnection, true);
        assertFalse(channel.isOpen());
    }

    @Test
    void unhandledErrorKicksClient() {
        TestSupport.installMockReactor(TestSupport.defaultConfig());
        EmbeddedChannel channel = new EmbeddedChannel();
        ProxyConnection proxyConnection = mock(ProxyConnection.class);
        when(proxyConnection.isClosed()).thenReturn(false);
        doThrow(CloseAndReturn.INSTANCE).when(proxyConnection).kickClient(org.mockito.ArgumentMatchers.anyString());

        ExceptionUtil.handleNettyException(contextFor(channel), new RuntimeException("boom"), proxyConnection, false);

        verify(proxyConnection).kickClient(org.mockito.ArgumentMatchers.anyString());
        assertFalse(channel.isOpen());
    }

    @Test
    void unhandledErrorKickWithoutThrowCompletes() {
        TestSupport.installMockReactor(TestSupport.defaultConfig());
        EmbeddedChannel channel = new EmbeddedChannel();
        ProxyConnection proxyConnection = mock(ProxyConnection.class);
        when(proxyConnection.isClosed()).thenReturn(false);

        ExceptionUtil.handleNettyException(contextFor(channel), new RuntimeException("boom"), proxyConnection, false);

        verify(proxyConnection).kickClient(org.mockito.ArgumentMatchers.anyString());
        assertFalse(channel.isOpen());
    }

    @Test
    void closedConnectionSkipsKick() {
        TestSupport.installMockReactor(TestSupport.defaultConfig());
        EmbeddedChannel channel = new EmbeddedChannel();
        ProxyConnection proxyConnection = mock(ProxyConnection.class);
        when(proxyConnection.isClosed()).thenReturn(true);

        ExceptionUtil.handleNettyException(contextFor(channel), new RuntimeException("boom"), proxyConnection, false);
        assertFalse(channel.isOpen());
    }

    @Test
    void unhandledErrorWithNullConnectionStillCloses() {
        TestSupport.installMockReactor(TestSupport.defaultConfig());
        EmbeddedChannel channel = new EmbeddedChannel();

        ExceptionUtil.handleNettyException(contextFor(channel), new RuntimeException("boom"), null, false);
        assertFalse(channel.isOpen());
    }

    @Test
    void prettyPrintUnwrapsCodecExceptions() {
        String encoder = ExceptionUtil.prettyPrint(new EncoderException(new IllegalStateException("enc")));
        assertTrue(encoder.contains("IllegalStateException"));
        assertTrue(encoder.contains("enc"));

        String decoder = ExceptionUtil.prettyPrint(new DecoderException(new IllegalArgumentException("dec")));
        assertTrue(decoder.contains("IllegalArgumentException"));
        assertTrue(decoder.contains("dec"));
    }

    @Test
    void prettyPrintChainsCauses() {
        String chain = ExceptionUtil.prettyPrint(new RuntimeException("outer", new IllegalStateException("inner")));
        assertTrue(chain.contains("outer"));
        assertTrue(chain.contains("inner"));
        assertTrue(chain.contains("Caused by"));
    }

    @Test
    void prettyPrintWithoutCause() {
        String single = ExceptionUtil.prettyPrint(new EncoderException("plain"));
        assertTrue(single.contains("EncoderException"));
    }
}
