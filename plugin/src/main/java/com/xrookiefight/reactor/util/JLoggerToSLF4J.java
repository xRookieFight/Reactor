package com.xrookiefight.reactor.util;

import org.slf4j.Logger;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
public class JLoggerToSLF4J extends java.util.logging.Logger {

    private final Logger slf4jLogger;
    private final boolean quietWarnings;

    public JLoggerToSLF4J(Logger slf4jLogger) {
        this(slf4jLogger, false);
    }

    public JLoggerToSLF4J(Logger slf4jLogger, boolean quietWarnings) {
        super(slf4jLogger.getName(), null);
        this.slf4jLogger = slf4jLogger;
        this.quietWarnings = quietWarnings;
        setLevel(Level.ALL);
        setUseParentHandlers(false);
        addHandler(new SLF4JHandler());
    }

    private class SLF4JHandler extends Handler {

        @Override
        public void publish(LogRecord record) {
            if (record == null) return;

            String message = record.getMessage();
            if (message == null) return;

            Level level = record.getLevel();
            Throwable thrown = record.getThrown();

            if (level.intValue() >= Level.SEVERE.intValue()) {
                log(message, thrown, slf4jLogger::error, slf4jLogger::error);
            } else if (level.intValue() >= Level.WARNING.intValue()) {
                if (quietWarnings) {
                    log(message, thrown, slf4jLogger::debug, slf4jLogger::debug);
                } else {
                    log(message, thrown, slf4jLogger::warn, slf4jLogger::warn);
                }
            } else if (level.intValue() >= Level.INFO.intValue()) {
                log(message, thrown, slf4jLogger::info, slf4jLogger::info);
            } else if (level.intValue() >= Level.FINE.intValue()) {
                log(message, thrown, slf4jLogger::debug, slf4jLogger::debug);
            } else {
                log(message, thrown, slf4jLogger::trace, slf4jLogger::trace);
            }
        }

        private void log(String message, Throwable thrown, java.util.function.Consumer<String> plain, java.util.function.BiConsumer<String, Throwable> withThrowable) {
            if (thrown != null) {
                withThrowable.accept(message, thrown);
            } else {
                plain.accept(message);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }
}
