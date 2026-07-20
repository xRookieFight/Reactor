package com.xrookiefight.reactor.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
class JLoggerToSLF4JTest {

    private Logger slf4j;

    @BeforeEach
    void setUp() {
        slf4j = mock(Logger.class);
        when(slf4j.getName()).thenReturn("test");
    }

    @Test
    void severeMapsToError() {
        Throwable t = new RuntimeException("x");
        JLoggerToSLF4J logger = new JLoggerToSLF4J(slf4j);
        logger.severe("plain");
        logger.log(Level.SEVERE, "thrown", t);

        verify(slf4j).error("plain");
        verify(slf4j).error("thrown", t);
    }

    @Test
    void warningMapsToWarn() {
        Throwable t = new RuntimeException("x");
        JLoggerToSLF4J logger = new JLoggerToSLF4J(slf4j);
        logger.warning("plain");
        logger.log(Level.WARNING, "thrown", t);

        verify(slf4j).warn("plain");
        verify(slf4j).warn("thrown", t);
    }

    @Test
    void quietWarningsMapToDebug() {
        Throwable t = new RuntimeException("x");
        JLoggerToSLF4J logger = new JLoggerToSLF4J(slf4j, true);
        logger.warning("plain");
        logger.log(Level.WARNING, "thrown", t);

        verify(slf4j).debug("plain");
        verify(slf4j).debug("thrown", t);
    }

    @Test
    void infoMapsToInfo() {
        Throwable t = new RuntimeException("x");
        JLoggerToSLF4J logger = new JLoggerToSLF4J(slf4j);
        logger.info("plain");
        logger.log(Level.INFO, "thrown", t);

        verify(slf4j).info("plain");
        verify(slf4j).info("thrown", t);
    }

    @Test
    void fineMapsToDebug() {
        Throwable t = new RuntimeException("x");
        JLoggerToSLF4J logger = new JLoggerToSLF4J(slf4j);
        logger.fine("plain");
        logger.log(Level.FINE, "thrown", t);

        verify(slf4j).debug("plain");
        verify(slf4j).debug("thrown", t);
    }

    @Test
    void finestMapsToTrace() {
        Throwable t = new RuntimeException("x");
        JLoggerToSLF4J logger = new JLoggerToSLF4J(slf4j);
        logger.finest("plain");
        logger.log(Level.FINEST, "thrown", t);

        verify(slf4j).trace("plain");
        verify(slf4j).trace("thrown", t);
    }

    @Test
    void nullRecordAndMessageAreIgnored() {
        JLoggerToSLF4J logger = new JLoggerToSLF4J(slf4j);
        logger.getHandlers()[0].publish(null);
        logger.getHandlers()[0].publish(new LogRecord(Level.INFO, null));
        logger.getHandlers()[0].flush();
        logger.getHandlers()[0].close();

        verify(slf4j).getName();
        verifyNoMoreInteractions(slf4j);
    }

    @Test
    void offLevelRecordsAreIgnoredByLevelFilter() {
        JLoggerToSLF4J logger = new JLoggerToSLF4J(slf4j);
        logger.setLevel(Level.OFF);
        logger.info("hidden");

        verify(slf4j).getName();
        verifyNoMoreInteractions(slf4j);
    }
}
