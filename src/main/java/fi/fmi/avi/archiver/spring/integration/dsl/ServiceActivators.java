package fi.fmi.avi.archiver.spring.integration.dsl;

import static java.util.Objects.requireNonNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.springframework.integration.handler.GenericHandler;

public final class ServiceActivators {
    private ServiceActivators() {
        throw new AssertionError();
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

    /**
     * Return a {@code GenericHandler} that looks for two specified headers, and if both exist, executes provided action on them.
     * The returned handler passes / returns the payload as is.
     *
     * @param <P>
     *         payload type
     * @param <H1>
     *         header 1 type
     * @param <H2>
     *         header 2 type
     * @param header1Type
     *         header 1 type
     * @param header1Key
     *         header 1 key
     * @param header2Type
     *         header 2 type
     * @param header2Key
     *         header 2 key
     * @param headerConsumer
     *         action to execute on header
     *
     * @return service activator
     */
    public static <P, H1, H2> GenericHandler<P> peekHeaders(final Class<H1> header1Type, final String header1Key, final Class<H2> header2Type,
            final String header2Key, final BiConsumer<H1, H2> headerConsumer) {
        requireNonNull(header1Type, "header1Type");
        requireNonNull(header1Key, "header1Key");
        requireNonNull(header2Type, "header2Type");
        requireNonNull(header2Key, "header2Key");
        requireNonNull(headerConsumer, "headerConsumer");
        return (payload, headers) -> {
            final H1 header1 = headers.get(header1Key, header1Type);
            final H2 header2 = headers.get(header2Key, header2Type);
            if (header1 != null && header2 != null) {
                headerConsumer.accept(header1, header2);
            }
            return payload;
        };
    }
}
