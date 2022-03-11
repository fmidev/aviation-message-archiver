package fi.fmi.avi.archiver.spring.integration.dsl;

import static java.util.Objects.requireNonNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.springframework.integration.handler.GenericHandler;

import fi.fmi.avi.archiver.spring.messaging.MessageHeaderReference;

public final class ServiceActivators {
    private ServiceActivators() {
        throw new AssertionError();
    }

    /**
     * Return a {@code GenericHandler} that executes provided {@code Runnable} and passes / returns the payload as is.
     *
     * @param action
     *         action to execute
     * @param <P>
     *         payload type
     *
     * @return handler to execute provided action
     */
    public static <P> GenericHandler<P> execute(final Runnable action) {
        requireNonNull(action, "action");
        return ((payload, headers) -> {
            action.run();
            return payload;
        });
    }

    /**
     * Return a {@code GenericHandler} that looks for specified header, and if it exists, executes provided action on payload and the header.
     * The returned handler passes / returns the payload as is.
     *
     * @param <P>
     *         payload type
     * @param <H>
     *         header type
     * @param headerRef
     *         header reference
     * @param headerConsumer
     *         action to execute on header
     *
     * @return service activator
     */
    public static <P, H> GenericHandler<P> peekPayloadAndHeader(final MessageHeaderReference<H> headerRef, final BiConsumer<P, H> headerConsumer) {
        requireNonNull(headerRef, "headerRef");
        return peekPayloadAndHeader(headerRef.getType(), headerRef.getName(), headerConsumer);
    }

    /**
     * Return a {@code GenericHandler} that looks for specified header, and if it exists, executes provided action on payload and the header.
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
    public static <P, H> GenericHandler<P> peekPayloadAndHeader(final Class<H> headerType, final String headerKey, final BiConsumer<P, H> headerConsumer) {
        requireNonNull(headerType, "headerType");
        requireNonNull(headerKey, "headerKey");
        requireNonNull(headerConsumer, "headerConsumer");
        return (payload, headers) -> {
            final H header = headers.get(headerKey, headerType);
            if (header != null) {
                headerConsumer.accept(payload, header);
            }
            return payload;
        };
    }

    /**
     * Return a {@code GenericHandler} that looks for specified header, and if it exists, executes the provided action on it.
     * The returned handler passes / returns the payload as is.
     *
     * @param <P>
     *         payload type
     * @param <H>
     *         header type
     * @param headerRef
     *         header reference
     * @param headerConsumer
     *         action to execute on header
     *
     * @return service activator
     */
    public static <P, H> GenericHandler<P> peekHeader(final MessageHeaderReference<H> headerRef, final Consumer<H> headerConsumer) {
        requireNonNull(headerRef, "headerRef");
        return peekHeader(headerRef.getType(), headerRef.getName(), headerConsumer);
    }

    /**
     * Return a {@code GenericHandler} that looks for specified header, and if it exists, executes the provided action on it.
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
