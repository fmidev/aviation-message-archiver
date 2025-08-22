package fi.fmi.avi.archiver.spring.integration.dsl;

import fi.fmi.avi.archiver.spring.messaging.MessageHeaderReference;
import org.junit.jupiter.api.Test;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.Message;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceActivatorsTest {
    @Test
    void peekPayloadAndHeaderIfExists_handler_runs_provided_action_on_payload_and_existing_header_and_returns_payload() {
        final AtomicReference<PayloadAndHeader<Object, String>> seenPayloadAndHeader = new AtomicReference<>();
        final Object inputPayload = "inputPayload";
        final MutableMessageHeaders headers = new MutableMessageHeaders(new HashMap<>());
        final MessageHeaderReference<String> headerRef = MessageHeaderReference.of("myString", String.class);
        final String headerValue = "myStringValue";
        headers.put(headerRef.getName(), headerValue);

        final GenericHandler<Object> handler = ServiceActivators.peekPayloadAndHeaderIfExists(headerRef,
                (payload, header) -> seenPayloadAndHeader.set(new PayloadAndHeader<>(payload, header)));
        final Object handlerResult = handler.handle(inputPayload, headers);

        assertThat(seenPayloadAndHeader)
                .as("seen header")
                .hasValue(new PayloadAndHeader<>(inputPayload, headerValue));
        assertThat(handlerResult)
                .as("handler return value")
                .isSameAs(inputPayload);
    }

    @Test
    void peekPayloadAndHeaderIfExists_handler_skips_provided_action_when_header_is_null_and_returns_payload() {
        final AtomicReference<PayloadAndHeader<Object, String>> seenPayloadAndHeader = new AtomicReference<>();
        final Object inputPayload = "inputPayload";
        final MessageHeaderReference<String> headerRef = MessageHeaderReference.of("myString", String.class);
        final HashMap<String, Object> map = new HashMap<>();
        map.put(headerRef.getName(), null);
        final MutableMessageHeaders headers = new MutableMessageHeaders(map);

        final GenericHandler<Object> handler = ServiceActivators.peekPayloadAndHeaderIfExists(headerRef,
                (payload, header) -> seenPayloadAndHeader.set(new PayloadAndHeader<>(payload, header)));
        final Object handlerResult = handler.handle(inputPayload, headers);

        assertThat(seenPayloadAndHeader).hasValue(null);
        assertThat(handlerResult).isSameAs(inputPayload);
    }

    @Test
    void peekPayloadAndHeaderIfExists_handler_skips_provided_action_when_header_is_missing_and_returns_payload() {
        final AtomicReference<PayloadAndHeader<Object, String>> seenPayloadAndHeader = new AtomicReference<>();
        final Object inputPayload = "inputPayload";
        final MutableMessageHeaders headers = new MutableMessageHeaders(new HashMap<>());
        final MessageHeaderReference<String> headerRef = MessageHeaderReference.of("myString", String.class);

        final GenericHandler<Object> handler = ServiceActivators.peekPayloadAndHeaderIfExists(headerRef,
                (payload, header) -> seenPayloadAndHeader.set(new PayloadAndHeader<>(payload, header)));
        final Object handlerResult = handler.handle(inputPayload, headers);

        assertThat(seenPayloadAndHeader).hasValue(null);
        assertThat(handlerResult).isSameAs(inputPayload);
    }

    @Test
    void peekPayloadAndHeader_handler_runs_provided_action_on_payload_and_existing_header_and_returns_payload() {
        final AtomicReference<PayloadAndHeader<Object, String>> seenPayloadAndHeader = new AtomicReference<>();
        final Object inputPayload = "inputPayload";
        final MutableMessageHeaders headers = new MutableMessageHeaders(new HashMap<>());
        final MessageHeaderReference<String> headerRef = MessageHeaderReference.of("myString", String.class);
        final String headerValue = "myStringValue";
        final String fallbackHeaderValue = "fallbackHeaderValue";
        headers.put(headerRef.getName(), headerValue);

        final GenericHandler<Object> handler = ServiceActivators.peekPayloadAndHeader(headerRef,
                () -> fallbackHeaderValue,
                (payload, header) -> seenPayloadAndHeader.set(new PayloadAndHeader<>(payload, header)));
        final Object handlerResult = handler.handle(inputPayload, headers);

        assertThat(seenPayloadAndHeader)
                .as("seen header")
                .hasValue(new PayloadAndHeader<>(inputPayload, headerValue));
        assertThat(handlerResult)
                .as("handler return value")
                .isInstanceOf(Message.class)
                .extracting(message -> PayloadAndHeader.fromMessage(message, headerRef))
                .isEqualTo(new PayloadAndHeader<>(inputPayload, headerValue));
    }

    @Test
    void peekPayloadAndHeader_handler_uses_fallback_header_value_when_header_is_null_and_returns_payload() {
        final AtomicReference<PayloadAndHeader<Object, String>> seenPayloadAndHeader = new AtomicReference<>();
        final Object inputPayload = "inputPayload";
        final MessageHeaderReference<String> headerRef = MessageHeaderReference.of("myString", String.class);
        final String fallbackHeaderValue = "fallbackHeaderValue";
        final HashMap<String, Object> map = new HashMap<>();
        map.put(headerRef.getName(), null);
        final MutableMessageHeaders headers = new MutableMessageHeaders(map);

        final GenericHandler<Object> handler = ServiceActivators.peekPayloadAndHeader(headerRef,
                () -> fallbackHeaderValue,
                (payload, header) -> seenPayloadAndHeader.set(new PayloadAndHeader<>(payload, header)));
        final Object handlerResult = handler.handle(inputPayload, headers);

        assertThat(seenPayloadAndHeader)
                .as("seen header")
                .hasValue(new PayloadAndHeader<>(inputPayload, fallbackHeaderValue));
        assertThat(handlerResult)
                .as("handler return value")
                .isInstanceOf(Message.class)
                .extracting(message -> PayloadAndHeader.fromMessage(message, headerRef))
                .isEqualTo(new PayloadAndHeader<>(inputPayload, fallbackHeaderValue));
    }

    @Test
    void peekPayloadAndHeader_handler_uses_fallback_header_value_when_header_is_missing_and_returns_payload() {
        final AtomicReference<PayloadAndHeader<Object, String>> seenPayloadAndHeader = new AtomicReference<>();
        final Object inputPayload = "inputPayload";
        final MutableMessageHeaders headers = new MutableMessageHeaders(new HashMap<>());
        final MessageHeaderReference<String> headerRef = MessageHeaderReference.of("myString", String.class);
        final String fallbackHeaderValue = "fallbackHeaderValue";

        final GenericHandler<Object> handler = ServiceActivators.peekPayloadAndHeader(headerRef,
                () -> fallbackHeaderValue,
                (payload, header) -> seenPayloadAndHeader.set(new PayloadAndHeader<>(payload, header)));
        final Object handlerResult = handler.handle(inputPayload, headers);

        assertThat(seenPayloadAndHeader)
                .as("seen header")
                .hasValue(new PayloadAndHeader<>(inputPayload, fallbackHeaderValue));
        assertThat(handlerResult)
                .as("handler return value")
                .isInstanceOf(Message.class)
                .extracting(message -> PayloadAndHeader.fromMessage(message, headerRef))
                .isEqualTo(new PayloadAndHeader<>(inputPayload, fallbackHeaderValue));
    }

    @Test
    void mapPayloadWithHeaderIfExists_handler_runs_provided_action_on_payload_and_existing_header_and_returns_payload() {
        final AtomicReference<PayloadAndHeader<Object, String>> seenPayloadAndHeader = new AtomicReference<>();
        final Object inputPayload = "inputPayload";
        final Object outputPayload = "outputPayload";
        final MutableMessageHeaders headers = new MutableMessageHeaders(new HashMap<>());
        final MessageHeaderReference<String> headerRef = MessageHeaderReference.of("myString", String.class);
        final String headerValue = "myStringValue";
        headers.put(headerRef.getName(), headerValue);

        final GenericHandler<Object> handler = ServiceActivators.mapPayloadWithHeaderIfExists(headerRef,
                (payload, header) -> {
                    seenPayloadAndHeader.set(new PayloadAndHeader<>(payload, header));
                    return outputPayload;
                });
        final Object handlerResult = handler.handle(inputPayload, headers);

        assertThat(seenPayloadAndHeader)
                .as("seen header")
                .hasValue(new PayloadAndHeader<>(inputPayload, headerValue));
        assertThat(handlerResult)
                .as("handler return value")
                .isSameAs(outputPayload);
    }

    @Test
    void mapPayloadWithHeaderIfExists_handler_skips_provided_action_when_header_is_null_and_returns_payload() {
        final AtomicReference<PayloadAndHeader<Object, String>> seenPayloadAndHeader = new AtomicReference<>();
        final Object inputPayload = "inputPayload";
        final Object outputPayload = "outputPayload";
        final MessageHeaderReference<String> headerRef = MessageHeaderReference.of("myString", String.class);
        final HashMap<String, Object> map = new HashMap<>();
        map.put(headerRef.getName(), null);
        final MutableMessageHeaders headers = new MutableMessageHeaders(map);

        final GenericHandler<Object> handler = ServiceActivators.mapPayloadWithHeaderIfExists(headerRef,
                (payload, header) -> {
                    seenPayloadAndHeader.set(new PayloadAndHeader<>(payload, header));
                    return outputPayload;
                });
        final Object handlerResult = handler.handle(inputPayload, headers);

        assertThat(seenPayloadAndHeader).hasValue(null);
        assertThat(handlerResult).isSameAs(inputPayload);
    }

    @Test
    void mapPayloadWithHeaderIfExists_handler_skips_provided_action_when_header_is_missing_and_returns_payload() {
        final AtomicReference<PayloadAndHeader<Object, String>> seenPayloadAndHeader = new AtomicReference<>();
        final Object inputPayload = "inputPayload";
        final Object outputPayload = "outputPayload";
        final MutableMessageHeaders headers = new MutableMessageHeaders(new HashMap<>());
        final MessageHeaderReference<String> headerRef = MessageHeaderReference.of("myString", String.class);

        final GenericHandler<Object> handler = ServiceActivators.mapPayloadWithHeaderIfExists(headerRef,
                (payload, header) -> {
                    seenPayloadAndHeader.set(new PayloadAndHeader<>(payload, header));
                    return outputPayload;
                });
        final Object handlerResult = handler.handle(inputPayload, headers);

        assertThat(seenPayloadAndHeader).hasValue(null);
        assertThat(handlerResult).isSameAs(inputPayload);
    }

    @Test
    void mapPayloadWithHeader_handler_runs_provided_action_on_payload_and_existing_header_and_returns_payload() {
        final AtomicReference<PayloadAndHeader<Object, String>> seenPayloadAndHeader = new AtomicReference<>();
        final Object inputPayload = "inputPayload";
        final Object outputPayload = "outputPayload";
        final MutableMessageHeaders headers = new MutableMessageHeaders(new HashMap<>());
        final MessageHeaderReference<String> headerRef = MessageHeaderReference.of("myString", String.class);
        final String headerValue = "myStringValue";
        final String fallbackHeaderValue = "fallbackHeaderValue";
        headers.put(headerRef.getName(), headerValue);

        final GenericHandler<Object> handler = ServiceActivators.mapPayloadWithHeader(headerRef,
                () -> fallbackHeaderValue,
                (payload, header) -> {
                    seenPayloadAndHeader.set(new PayloadAndHeader<>(payload, header));
                    return outputPayload;
                });
        final Object handlerResult = handler.handle(inputPayload, headers);

        assertThat(seenPayloadAndHeader)
                .as("seen header")
                .hasValue(new PayloadAndHeader<>(inputPayload, headerValue));
        assertThat(handlerResult)
                .as("handler return value")
                .isInstanceOf(Message.class)
                .extracting(message -> PayloadAndHeader.fromMessage(message, headerRef))
                .isEqualTo(new PayloadAndHeader<>(outputPayload, headerValue));
    }

    @Test
    void mapPayloadWithHeader_handler_uses_fallback_header_value_when_header_is_null_and_returns_payload() {
        final AtomicReference<PayloadAndHeader<Object, String>> seenPayloadAndHeader = new AtomicReference<>();
        final Object inputPayload = "inputPayload";
        final Object outputPayload = "outputPayload";
        final MessageHeaderReference<String> headerRef = MessageHeaderReference.of("myString", String.class);
        final String fallbackHeaderValue = "fallbackHeaderValue";
        final HashMap<String, Object> map = new HashMap<>();
        map.put(headerRef.getName(), null);
        final MutableMessageHeaders headers = new MutableMessageHeaders(map);

        final GenericHandler<Object> handler = ServiceActivators.mapPayloadWithHeader(headerRef,
                () -> fallbackHeaderValue,
                (payload, header) -> {
                    seenPayloadAndHeader.set(new PayloadAndHeader<>(payload, header));
                    return outputPayload;
                });
        final Object handlerResult = handler.handle(inputPayload, headers);

        assertThat(seenPayloadAndHeader)
                .as("seen header")
                .hasValue(new PayloadAndHeader<>(inputPayload, fallbackHeaderValue));
        assertThat(handlerResult)
                .as("handler return value")
                .isInstanceOf(Message.class)
                .extracting(message -> PayloadAndHeader.fromMessage(message, headerRef))
                .isEqualTo(new PayloadAndHeader<>(outputPayload, fallbackHeaderValue));
    }

    @Test
    void mapPayloadWithHeader_handler_uses_fallback_header_value_when_header_is_missing_and_returns_payload() {
        final AtomicReference<PayloadAndHeader<Object, String>> seenPayloadAndHeader = new AtomicReference<>();
        final Object inputPayload = "inputPayload";
        final Object outputPayload = "outputPayload";
        final MutableMessageHeaders headers = new MutableMessageHeaders(new HashMap<>());
        final MessageHeaderReference<String> headerRef = MessageHeaderReference.of("myString", String.class);
        final String fallbackHeaderValue = "fallbackHeaderValue";

        final GenericHandler<Object> handler = ServiceActivators.mapPayloadWithHeader(headerRef,
                () -> fallbackHeaderValue,
                (payload, header) -> {
                    seenPayloadAndHeader.set(new PayloadAndHeader<>(payload, header));
                    return outputPayload;
                });
        final Object handlerResult = handler.handle(inputPayload, headers);

        assertThat(seenPayloadAndHeader)
                .as("seen header")
                .hasValue(new PayloadAndHeader<>(inputPayload, fallbackHeaderValue));
        assertThat(handlerResult)
                .as("handler return value")
                .isInstanceOf(Message.class)
                .extracting(message -> PayloadAndHeader.fromMessage(message, headerRef))
                .isEqualTo(new PayloadAndHeader<>(outputPayload, fallbackHeaderValue));
    }

    @Test
    void peekHeaderIfExists_handler_runs_provided_action_on_existing_header_and_returns_payload() {
        final AtomicReference<String> seenHeader = new AtomicReference<>();
        final Object inputPayload = "inputPayload";
        final MutableMessageHeaders headers = new MutableMessageHeaders(new HashMap<>());
        final MessageHeaderReference<String> headerRef = MessageHeaderReference.of("myString", String.class);
        final String headerValue = "myStringValue";
        headers.put(headerRef.getName(), headerValue);

        final GenericHandler<Object> handler = ServiceActivators.peekHeaderIfExists(headerRef, seenHeader::set);
        final Object handlerResult = handler.handle(inputPayload, headers);

        assertThat(seenHeader).hasValue(headerValue);
        assertThat(handlerResult).isSameAs(inputPayload);
    }

    @Test
    void peekHeaderIfExists_handler_skips_provided_action_when_header_is_null_and_returns_payload() {
        final AtomicReference<String> seenHeader = new AtomicReference<>();
        final Object inputPayload = "inputPayload";
        final MessageHeaderReference<String> headerRef = MessageHeaderReference.of("myString", String.class);
        final HashMap<String, Object> map = new HashMap<>();
        map.put(headerRef.getName(), null);
        final MutableMessageHeaders headers = new MutableMessageHeaders(map);

        final GenericHandler<Object> handler = ServiceActivators.peekHeaderIfExists(headerRef, seenHeader::set);
        final Object handlerResult = handler.handle(inputPayload, headers);

        assertThat(seenHeader).hasValue(null);
        assertThat(handlerResult).isSameAs(inputPayload);
    }

    @Test
    void peekHeaderIfExists_handler_skips_provided_action_when_header_is_missing_and_returns_payload() {
        final AtomicReference<String> seenHeader = new AtomicReference<>();
        final Object inputPayload = "inputPayload";
        final MutableMessageHeaders headers = new MutableMessageHeaders(new HashMap<>());
        final MessageHeaderReference<String> headerRef = MessageHeaderReference.of("myString", String.class);

        final GenericHandler<Object> handler = ServiceActivators.peekHeaderIfExists(headerRef, seenHeader::set);
        final Object handlerResult = handler.handle(inputPayload, headers);

        assertThat(seenHeader).hasValue(null);
        assertThat(handlerResult).isSameAs(inputPayload);
    }

    @Test
    void peekHeader_handler_runs_provided_action_on_payload_and_existing_header_and_returns_payload() {
        final AtomicReference<String> seenHeader = new AtomicReference<>();
        final Object inputPayload = "inputPayload";
        final MutableMessageHeaders headers = new MutableMessageHeaders(new HashMap<>());
        final MessageHeaderReference<String> headerRef = MessageHeaderReference.of("myString", String.class);
        final String headerValue = "myStringValue";
        final String fallbackHeaderValue = "fallbackHeaderValue";
        headers.put(headerRef.getName(), headerValue);

        final GenericHandler<Object> handler = ServiceActivators.peekHeader(headerRef,
                () -> fallbackHeaderValue,
                seenHeader::set);
        final Object handlerResult = handler.handle(inputPayload, headers);

        assertThat(seenHeader)
                .as("seen header")
                .hasValue(headerValue);
        assertThat(handlerResult)
                .as("handler return value")
                .isInstanceOf(Message.class)
                .extracting(message -> PayloadAndHeader.fromMessage(message, headerRef))
                .isEqualTo(new PayloadAndHeader<>(inputPayload, headerValue));
    }

    @Test
    void peekHeader_handler_uses_fallback_header_value_when_header_is_null_and_returns_payload() {
        final AtomicReference<String> seenHeader = new AtomicReference<>();
        final Object inputPayload = "inputPayload";
        final MessageHeaderReference<String> headerRef = MessageHeaderReference.of("myString", String.class);
        final String fallbackHeaderValue = "fallbackHeaderValue";
        final HashMap<String, Object> map = new HashMap<>();
        map.put(headerRef.getName(), null);
        final MutableMessageHeaders headers = new MutableMessageHeaders(map);

        final GenericHandler<Object> handler = ServiceActivators.peekHeader(headerRef,
                () -> fallbackHeaderValue,
                seenHeader::set);
        final Object handlerResult = handler.handle(inputPayload, headers);

        assertThat(seenHeader)
                .as("seen header")
                .hasValue(fallbackHeaderValue);
        assertThat(handlerResult)
                .as("handler return value")
                .isInstanceOf(Message.class)
                .extracting(message -> PayloadAndHeader.fromMessage(message, headerRef))
                .isEqualTo(new PayloadAndHeader<>(inputPayload, fallbackHeaderValue));
    }

    @Test
    void peekHeader_handler_uses_fallback_header_value_when_header_is_missing_and_returns_payload() {
        final AtomicReference<String> seenHeader = new AtomicReference<>();
        final Object inputPayload = "inputPayload";
        final MutableMessageHeaders headers = new MutableMessageHeaders(new HashMap<>());
        final MessageHeaderReference<String> headerRef = MessageHeaderReference.of("myString", String.class);
        final String fallbackHeaderValue = "fallbackHeaderValue";

        final GenericHandler<Object> handler = ServiceActivators.peekHeader(headerRef,
                () -> fallbackHeaderValue,
                seenHeader::set);
        final Object handlerResult = handler.handle(inputPayload, headers);

        assertThat(seenHeader)
                .as("seen header")
                .hasValue(fallbackHeaderValue);
        assertThat(handlerResult)
                .as("handler return value")
                .isInstanceOf(Message.class)
                .extracting(message -> PayloadAndHeader.fromMessage(message, headerRef))
                .isEqualTo(new PayloadAndHeader<>(inputPayload, fallbackHeaderValue));
    }

    private record PayloadAndHeader<P, H>(
            P payload,
            H header
    ) {
        public static <P, H> PayloadAndHeader<P, H> from(final Message<P> message, final MessageHeaderReference<H> headerRef) {
            return new PayloadAndHeader<>(message.getPayload(), headerRef.getNullable(message.getHeaders()));
        }

        @SuppressWarnings("unchecked")
        public static <P, H> PayloadAndHeader<P, H> fromMessage(final Object message, final MessageHeaderReference<H> headerRef) {
            if (message instanceof Message) {
                return from(((Message<P>) message), headerRef);
            }
            throw new IllegalArgumentException("messageObject is not a Message, but %s".formatted(message.getClass()));
        }
    }
}
