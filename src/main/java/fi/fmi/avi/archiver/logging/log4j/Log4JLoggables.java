package fi.fmi.avi.archiver.logging.log4j;

import org.slf4j.MDC;

import fi.fmi.avi.archiver.logging.StructuredLoggable;

public final class Log4JLoggables {
    private Log4JLoggables() {
        throw new AssertionError();
    }

    public static void putMDC(final StructuredLoggable loggable) {
        MDC.put(loggable.getStructureName(), loggable.toString());
    }

    public static void removeMDC(final StructuredLoggable loggable) {
        MDC.remove(loggable.getStructureName());
    }
}
