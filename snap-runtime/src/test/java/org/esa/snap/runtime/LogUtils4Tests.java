package org.esa.snap.runtime;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LogUtils4Tests {

    private static final String PROPERTY_NAME_SNAP_MAIN_LOGGER_NAME = "snap.main.logger.name";

    public static void initLogger() throws Exception {
        String mainLoggerName = System.getProperty(PROPERTY_NAME_SNAP_MAIN_LOGGER_NAME); // mainLoggerName = "org.esa";
        if (mainLoggerName == null) {
            return; // no logger to configure
        }
        boolean init = true;
        Logger mainLogger = Logger.getLogger(mainLoggerName);
        for (Handler handler : mainLogger.getHandlers()) {
            if (handler.getFormatter() instanceof CustomLogFormatter) {
                init = false;
                break;
            }
        }
        if (init) {
            EngineConfig engineConfig = EngineConfig.instance();
            engineConfig.logLevel(Level.INFO);
            engineConfig.loggerName(mainLoggerName);

            // Suppress ugly (and harmless) JAI error messages saying that a JAI is going to continue in pure Java mode.
            System.setProperty("com.sun.media.jai.disableMediaLib", "true");  // disable native libraries for JAI

            StringBuilder properties = new StringBuilder();
            Enumeration<?> propertyNames = System.getProperties().propertyNames();
            while (propertyNames.hasMoreElements()) {
                String systemPropertyName = (String)propertyNames.nextElement();
                if (systemPropertyName.endsWith(".level")) {
                    String systemPropertyValue = System.getProperty(systemPropertyName);
                    try {
                        Level level = Level.parse(systemPropertyValue);
                        if (properties.length() > 0) {
                            properties.append(CustomLogFormatter.LINE_SEPARATOR);
                        }
                        properties.append(systemPropertyName)
                                .append("=")
                                .append(level.getName());
                    } catch (IllegalArgumentException exception) {
                        // ignore exception
                    }
                }
            }
            if (properties.length() > 0) {
                properties.append(CustomLogFormatter.LINE_SEPARATOR)
                        .append(".level = ")
                        .append(Level.INFO.getName());

                ByteArrayInputStream inputStream = new ByteArrayInputStream(properties.toString().getBytes());
                LogManager logManager = LogManager.getLogManager();
                logManager.readConfiguration(inputStream);
            }

            Logger rootLogger = Logger.getLogger("");
            for (Handler handler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(handler);
            }

            for (Handler handler : mainLogger.getHandlers()) {
                mainLogger.removeHandler(handler);
            }
            ConsoleHandler consoleHandler = new ConsoleHandler() {
                @Override
                public synchronized void setLevel(Level newLevel) throws SecurityException {
                    super.setLevel(Level.FINEST);
                }
            };
            consoleHandler.setFormatter(new CustomLogFormatter());
            mainLogger.addHandler(consoleHandler);
        }
    }

    private static class CustomLogFormatter extends Formatter {

        public final static String LINE_SEPARATOR = System.getProperty("line.separator", "\r\n");

        private CustomLogFormatter() {
            super();
        }

        @Override
        public synchronized String format(LogRecord record) {
            String message = formatMessage(record);

            StringBuilder result = new StringBuilder();
            result.append(record.getLevel().getName())
                    .append(" [");
            if (record.getSourceClassName() != null) {
                result.append(record.getSourceClassName());
            } else {
                result.append(record.getLoggerName());
            }
            if (record.getSourceMethodName() != null) {
                result.append(", ")
                        .append(record.getSourceMethodName());
            }
            result.append("]: ");
            result.append(message);
            result.append(LINE_SEPARATOR);
            if (record.getThrown() != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                result.append(sw.toString());
            }
            return result.toString();
        }
    }

}
