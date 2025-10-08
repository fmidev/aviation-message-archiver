package fi.fmi.avi.archiver.config.model;

import com.google.common.testing.ForwardingWrapperTester;
import fi.fmi.avi.archiver.message.processor.postaction.PostAction;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactory;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ForwardingPostActionFactoryTest {

    public static final Map<String, Object> TEST_ACTION_CONFIG = Map.of("configKey", "configValue");

    private static <T extends PostAction> void verifyForwarding(final ForwardingPostActionFactory<T> result, final ObjectFactory<T> delegate, final AutoCloseable closeableDelegate) throws Exception {
        result.getType();
        result.getName();
        result.newInstance(TEST_ACTION_CONFIG);
        ((AutoCloseable) result).close();

        final InOrder inOrder = inOrder(delegate, closeableDelegate);
        inOrder.verify(delegate).getType();
        inOrder.verify(delegate).getName();
        inOrder.verify(delegate).newInstance(TEST_ACTION_CONFIG);
        inOrder.verify(closeableDelegate).close();
        verifyNoMoreInteractions(delegate, closeableDelegate);
    }

    @SuppressWarnings("rawtypes")
    @Test
    void testForwarding() {
        new ForwardingWrapperTester().testForwarding(PostActionFactory.class, ForwardingPostActionFactory::create);
    }

    @Test
    void create_creates_AutoCloseable_if_provided_delegate_is_AutoCloseable() throws Exception {
        // given
        final AutoCloaseablePostActionFactory delegate = mock(AutoCloaseablePostActionFactory.class);

        // when
        final ForwardingPostActionFactory<TestPostAction> result = ForwardingPostActionFactory.create(delegate);

        // then
        assertThat(result)
                .isInstanceOf(PostActionFactory.class)
                .isInstanceOf(AutoCloseable.class);
        verifyForwarding(result, delegate, delegate);
    }

    @Test
    void createAutoCloseable_creates_AutoCloseable() throws Exception {
        // given
        final AutoCloaseablePostActionFactory delegate = mock(AutoCloaseablePostActionFactory.class);

        // when
        final ForwardingPostActionFactory.AutoCloseableFactory<TestPostAction> result = ForwardingPostActionFactory.createAutoCloseable(delegate);

        // then
        verifyForwarding(result, delegate, delegate);
    }

    @Test
    void createAutoCloseable_creates_delegates_to_provided_AutoCloseable() throws Exception {
        // given
        @SuppressWarnings("unchecked") final PostActionFactory<TestPostAction> delegate = mock(PostActionFactory.class);
        final AutoCloseable closeable = mock(AutoCloseable.class);

        // when
        final ForwardingPostActionFactory.AutoCloseableFactory<TestPostAction> result = ForwardingPostActionFactory.createAutoCloseable(delegate, closeable);

        // then
        verifyForwarding(result, delegate, closeable);
    }

    private interface TestPostAction extends PostAction {
    }

    interface AutoCloaseablePostActionFactory extends PostActionFactory<TestPostAction>, AutoCloseable {
    }
}
