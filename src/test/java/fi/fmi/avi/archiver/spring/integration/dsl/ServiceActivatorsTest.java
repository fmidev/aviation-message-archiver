package fi.fmi.avi.archiver.spring.integration.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.support.MutableMessageHeaders;

class ServiceActivatorsTest {
    @Test
    void peekHeader_handler_runs_provided_action_on_existing_header_and_returns_payload() {
        final AtomicReference<String> resultingHeaderString = new AtomicReference<>();
        final Object payload = new Object();
        final MutableMessageHeaders headers = new MutableMessageHeaders(new HashMap<>());
        final String headerKey = "myString";
        final String headerValue = "myStringValue";
        headers.put(headerKey, headerValue);

        final GenericHandler<Object> handler = ServiceActivators.peekHeader(String.class, headerKey, resultingHeaderString::set);
        final Object handlerResult = handler.handle(payload, headers);

        assertThat(resultingHeaderString).hasValue(headerValue);
        assertThat(handlerResult).isSameAs(payload);
    }

    @Test
    void peekHeader_handler_skips_provided_action_when_header_is_null_and_returns_payload() {
        final AtomicInteger actionInvocations = new AtomicInteger(0);
        final Object payload = new Object();
        final String headerKey = "myString";
        final HashMap<String, Object> map = new HashMap<>();
        map.put(headerKey, null);
        final MutableMessageHeaders headers = new MutableMessageHeaders(map);

        final GenericHandler<Object> handler = ServiceActivators.peekHeader(String.class, headerKey, header -> actionInvocations.incrementAndGet());
        final Object handlerResult = handler.handle(payload, headers);

        assertThat(actionInvocations).hasValue(0);
        assertThat(handlerResult).isSameAs(payload);
    }

    @Test
    void peekHeader_handler_skips_provided_action_when_header_is_missing_and_returns_payload() {
        final AtomicInteger actionInvocations = new AtomicInteger(0);
        final Object payload = new Object();
        final MutableMessageHeaders headers = new MutableMessageHeaders(new HashMap<>());
        final String headerKey = "myString";

        final GenericHandler<Object> handler = ServiceActivators.peekHeader(String.class, headerKey, header -> actionInvocations.incrementAndGet());
        final Object handlerResult = handler.handle(payload, headers);

        assertThat(actionInvocations).hasValue(0);
        assertThat(handlerResult).isSameAs(payload);
    }
}
