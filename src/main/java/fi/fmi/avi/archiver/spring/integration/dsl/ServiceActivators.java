package fi.fmi.avi.archiver.spring.integration.dsl;

import fi.fmi.avi.archiver.spring.messaging.MessageHeaderReference;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public final class ServiceActivators {
    private ServiceActivators() {
        throw new AssertionError();
    }

    /**
     * Return a {@code GenericHandler} that executes provided {@code Runnable} and passes / returns the payload as is.
     *
     * @param action action to execute
     * @param <P>    payload type
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
     * @param <P>            payload type
     * @param <H>            header type
     * @param headerRef      header reference
     * @param headerConsumer action to execute on header
     * @return service activator
     */
    public static <P, H> GenericHandler<P> peekPayloadAndHeaderIfExists(final MessageHeaderReference<H> headerRef, final BiConsumer<P, H> headerConsumer) {
        requireNonNull(headerRef, "headerRef");
        return peekPayloadAndHeaderIfExists(headerRef.getType(), headerRef.getName(), headerConsumer);
    }

    /**
     * Return a {@code GenericHandler} that looks for specified header, and if it exists, executes provided action on payload and the header.
     * The returned handler passes / returns the payload as is.
     *
     * @param <P>        payload type
     * @param <H>        header type
     * @param headerType header type
     * @param headerKey  header key
     * @param action     action to execute on payload and header
     * @return service activator
     */
    public static <P, H> GenericHandler<P> peekPayloadAndHeaderIfExists(final Class<H> headerType, final String headerKey, final BiConsumer<P, H> action) {
        requireNonNull(headerType, "headerType");
        requireNonNull(headerKey, "headerKey");
        requireNonNull(action, "action");
        return (payload, headers) -> {
            final H header = headers.get(headerKey, headerType);
            if (header != null) {
                action.accept(payload, header);
            }
            return payload;
        };
    }

    /**
     * Return a {@code GenericHandler} that looks for specified header, or {@code fallbackHeader} if it does not exit,
     * and then executes provided action on payload and the header.
     * The returned handler passes / returns the payload as is, and adds the fallback header into the message if it did not exist before.
     * Instead of a fallback header value, the {@code fallbackHeader} supplier may throw an exception, if desired,
     * which will be passed to the Spring Integration flow.
     *
     * @param <P>            payload type
     * @param <H>            header type
     * @param headerRef      header reference
     * @param fallbackHeader fallback header supplier in case header does not exist
     * @param action         action to execute on payload and header
     * @return service activator
     */
    public static <P, H> GenericHandler<P> peekPayloadAndHeader(
            final MessageHeaderReference<H> headerRef, final Supplier<H> fallbackHeader, final BiConsumer<P, H> action) {
        requireNonNull(headerRef, "headerRef");
        return peekPayloadAndHeader(headerRef.getType(), headerRef.getName(), fallbackHeader, action);
    }

    /**
     * Return a {@code GenericHandler} that looks for specified header, or {@code fallbackHeader} if it does not exit,
     * and then executes provided action on payload and the header.
     * The returned handler passes / returns the payload as is, and adds the fallback header into the message if it did not exist before.
     * Instead of a fallback header value, the {@code fallbackHeader} supplier may throw an exception, if desired,
     * which will be passed to the Spring Integration flow.
     *
     * @param <P>            payload type
     * @param <H>            header type
     * @param headerType     header type
     * @param headerKey      header key
     * @param fallbackHeader fallback header supplier in case header does not exist
     * @param action         action to execute on payload and header
     * @return service activator
     */
    public static <P, H> GenericHandler<P> peekPayloadAndHeader(
            final Class<H> headerType, final String headerKey, final Supplier<H> fallbackHeader,
            final BiConsumer<P, H> action) {
        requireNonNull(headerType, "headerType");
        requireNonNull(headerKey, "headerKey");
        requireNonNull(fallbackHeader, "fallbackHeader");
        requireNonNull(action, "action");
        return (payload, headers) -> {
            final H header = Optional.ofNullable(headers.get(headerKey, headerType))
                    .orElseGet(fallbackHeader);
            action.accept(payload, header);
            return MessageBuilder.withPayload(payload)
                    .copyHeaders(headers)
                    .setHeaderIfAbsent(headerKey, header)
                    .build();
        };
    }

    /**
     * Return a {@code GenericHandler} that looks for specified header, and if it exists, executes provided action on payload and the header.
     * The returned handler returns the payload from action when executed, otherwise the input payload.
     *
     * @param <P>       input payload type
     * @param <H>       header type
     * @param headerRef header reference
     * @param action    mapping function to execute on payload and header
     * @return service activator
     */
    public static <P, H> GenericHandler<P> mapPayloadWithHeaderIfExists(final MessageHeaderReference<H> headerRef, final BiFunction<P, H, ?> action) {
        requireNonNull(headerRef, "headerRef");
        return mapPayloadWithHeaderIfExists(headerRef.getType(), headerRef.getName(), action);
    }

    /**
     * Return a {@code GenericHandler} that looks for specified header, and if it exists, executes provided action on payload and the header.
     * The returned handler returns the payload from action when executed, otherwise the input payload.
     *
     * @param <P>        input payload type
     * @param <H>        header type
     * @param headerType header type
     * @param headerKey  header key
     * @param action     mapping function to execute on payload and header
     * @return service activator
     */
    public static <P, H> GenericHandler<P> mapPayloadWithHeaderIfExists(final Class<H> headerType, final String headerKey, final BiFunction<P, H, ?> action) {
        requireNonNull(headerType, "headerType");
        requireNonNull(headerKey, "headerKey");
        requireNonNull(action, "action");
        return (payload, headers) -> {
            final H header = headers.get(headerKey, headerType);
            if (header == null) {
                return payload;
            } else {
                return action.apply(payload, header);
            }
        };
    }

    /**
     * Return a {@code GenericHandler} that looks for specified header, or {@code fallbackHeader} if it does not exit,
     * and then executes provided action on payload and the header.
     * The returned handler returns the payload from action, and adds the fallback header into the message if it did not exist before.
     * Instead of a fallback header value, the {@code fallbackHeader} supplier may throw an exception, if desired,
     * which will be passed to the Spring Integration flow.
     *
     * @param <P>            input payload type
     * @param <H>            header type
     * @param headerRef      header reference
     * @param fallbackHeader fallback header supplier in case header does not exist
     * @param action         mapping function to execute on payload and header
     * @return service activator
     */
    public static <P, H> GenericHandler<P> mapPayloadWithHeader(
            final MessageHeaderReference<H> headerRef, final Supplier<H> fallbackHeader, final BiFunction<P, H, ?> action) {
        requireNonNull(headerRef, "headerRef");
        return mapPayloadWithHeader(headerRef.getType(), headerRef.getName(), fallbackHeader, action);
    }

    /**
     * Return a {@code GenericHandler} that looks for specified header, or {@code fallbackHeader} if it does not exit,
     * and then executes provided action on payload and the header.
     * The returned handler returns the payload from action, and adds the fallback header into the message if it did not exist before.
     * Instead of a fallback header value, the {@code fallbackHeader} supplier may throw an exception, if desired,
     * which will be passed to the Spring Integration flow.
     *
     * @param <P>            input payload type
     * @param <H>            header type
     * @param headerType     header type
     * @param headerKey      header key
     * @param fallbackHeader fallback header supplier in case header does not exist
     * @param action         mapping function to execute on payload and header
     * @return service activator
     */
    public static <P, H> GenericHandler<P> mapPayloadWithHeader(
            final Class<H> headerType, final String headerKey, final Supplier<H> fallbackHeader,
            final BiFunction<P, H, ?> action) {
        requireNonNull(headerType, "headerType");
        requireNonNull(headerKey, "headerKey");
        requireNonNull(fallbackHeader, "fallbackHeader");
        requireNonNull(action, "action");
        return (payload, headers) -> {
            final H header = Optional.ofNullable(headers.get(headerKey, headerType))
                    .orElseGet(fallbackHeader);
            final Object newPayload = action.apply(payload, header);
            return MessageBuilder.withPayload(newPayload)
                    .copyHeaders(headers)
                    .setHeaderIfAbsent(headerKey, header)
                    .build();
        };
    }

    /**
     * Return a {@code GenericHandler} that looks for specified header, and if it exists, executes the provided action on it.
     * The returned handler passes / returns the payload as is.
     *
     * @param <P>            payload type
     * @param <H>            header type
     * @param headerRef      header reference
     * @param headerConsumer action to execute on header
     * @return service activator
     */
    public static <P, H> GenericHandler<P> peekHeaderIfExists(final MessageHeaderReference<H> headerRef, final Consumer<H> headerConsumer) {
        requireNonNull(headerRef, "headerRef");
        return peekHeaderIfExists(headerRef.getType(), headerRef.getName(), headerConsumer);
    }

    /**
     * Return a {@code GenericHandler} that looks for specified header, and if it exists, executes the provided action on it.
     * The returned handler passes / returns the payload as is.
     *
     * @param <P>            payload type
     * @param <H>            header type
     * @param headerType     header type
     * @param headerKey      header key
     * @param headerConsumer action to execute on header
     * @return service activator
     */
    public static <P, H> GenericHandler<P> peekHeaderIfExists(final Class<H> headerType, final String headerKey, final Consumer<H> headerConsumer) {
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
     * Return a {@code GenericHandler} that looks for specified header, or {@code fallbackHeader} if it does not exit,
     * and then executes provided action on the header.
     * The returned handler passes / returns the payload as is and adds the fallback header into the message if it did not exist before.
     * Instead of a fallback header value, the {@code fallbackHeader} supplier may throw an exception, if desired,
     * which will be passed to the Spring Integration flow.
     *
     * @param <P>            payload type
     * @param <H>            header type
     * @param fallbackHeader fallback header supplier in case header does not exist
     * @param headerConsumer action to execute on header
     * @return service activator
     */
    public static <P, H> GenericHandler<P> peekHeader(
            final MessageHeaderReference<H> headerRef, final Supplier<H> fallbackHeader, final Consumer<H> headerConsumer) {
        requireNonNull(headerRef, "headerRef");
        return peekHeader(headerRef.getType(), headerRef.getName(), fallbackHeader, headerConsumer);
    }

    /**
     * Return a {@code GenericHandler} that looks for specified header, or {@code fallbackHeader} if it does not exit,
     * and then executes provided action on the header.
     * The returned handler passes / returns the payload as is and adds the fallback header into the message if it did not exist before.
     * Instead of a fallback header value, the {@code fallbackHeader} supplier may throw an exception, if desired,
     * which will be passed to the Spring Integration flow.
     *
     * @param <P>            payload type
     * @param <H>            header type
     * @param headerType     header type
     * @param headerKey      header key
     * @param fallbackHeader fallback header supplier in case header does not exist
     * @param headerConsumer action to execute on header
     * @return service activator
     */
    public static <P, H> GenericHandler<P> peekHeader(
            final Class<H> headerType, final String headerKey, final Supplier<H> fallbackHeader,
            final Consumer<H> headerConsumer) {
        requireNonNull(headerType, "headerType");
        requireNonNull(headerKey, "headerKey");
        requireNonNull(fallbackHeader, "fallbackHeader");
        requireNonNull(headerConsumer, "headerConsumer");
        return (payload, headers) -> {
            final H header = Optional.ofNullable(headers.get(headerKey, headerType))
                    .orElseGet(fallbackHeader);
            headerConsumer.accept(header);
            return MessageBuilder.withPayload(payload)
                    .copyHeaders(headers)
                    .setHeaderIfAbsent(headerKey, header)
                    .build();
        };
    }
}
