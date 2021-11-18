package fi.fmi.avi.archiver.spring.integration.dsl;

import static java.util.Objects.requireNonNull;

import java.util.function.Consumer;

import org.springframework.integration.handler.GenericHandler;

public final class ServiceActivators {
    private ServiceActivators() {
        throw new AssertionError();
    }

    /**
     * Return a {@code GenericHandler} that looks for specified header, and if it exists, executes provided action on it.
     * The returned handler passes / returns the payload as is.
     *
     * @param <P>
     *         payload type
     * @param <H>
     *         header type
     * @param headerType
     *         header type
     * @param headerKey
     *         header key
     * @param headerConsumer
     *         action to execute on header
     *
     * @return service activator
     */
    public static <P, H> GenericHandler<P> peekHeader(final Class<H> headerType, final String headerKey, final Consumer<H> headerConsumer) {
        requireNonNull(headerType, "headerType");
        requireNonNull(headerKey, "headerKey");
        requireNonNull(headerConsumer, "headerConsumer");
        return (payload, headers) -> {
            final H header = headers.get(headerKey, headerType);
            if (header != null) {
                headerConsumer.accept(header);
            }
            return payload;
        };
    }
}
