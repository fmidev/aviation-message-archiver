package fi.fmi.avi.archiver.util.instantiation;

import static java.util.Objects.requireNonNull;

public class ConfigValueConversionException extends RuntimeException {
    public ConfigValueConversionException() {
    }

    public ConfigValueConversionException(final String message) {
        super(requireNonNull(message, "message"));
    }

    public ConfigValueConversionException(final String message, final Throwable cause) {
        super(requireNonNull(message, "message"), requireNonNull(cause, "cause"));
    }

    public ConfigValueConversionException(final Throwable cause) {
        super(requireNonNull(cause, "cause"));
    }
}
