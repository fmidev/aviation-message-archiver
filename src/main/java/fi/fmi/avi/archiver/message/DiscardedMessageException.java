package fi.fmi.avi.archiver.message;

import static java.util.Objects.requireNonNull;

public class DiscardedMessageException extends Exception {

    private static final long serialVersionUID = 4599599829314600414L;

    public DiscardedMessageException() {
        super();
    }

    public DiscardedMessageException(final String message) {
        super(requireNonNull(message, "message"));
    }

    public DiscardedMessageException(final String message, final Throwable cause) {
        super(requireNonNull(message, "message"), requireNonNull(cause, "cause"));
    }

    public DiscardedMessageException(final Throwable cause) {
        super(requireNonNull(cause, "cause"));
    }

}
