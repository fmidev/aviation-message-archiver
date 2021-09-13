package fi.fmi.avi.archiver.message;

import static java.util.Objects.requireNonNull;

public class MessageDiscardedException extends Exception {

    private static final long serialVersionUID = 4599599829314600414L;

    public MessageDiscardedException() {
        super();
    }

    public MessageDiscardedException(final String message) {
        super(requireNonNull(message, "message"));
    }

    public MessageDiscardedException(final String message, final Throwable cause) {
        super(requireNonNull(message, "message"), requireNonNull(cause, "cause"));
    }

    public MessageDiscardedException(final Throwable cause) {
        super(requireNonNull(cause, "cause"));
    }

}
