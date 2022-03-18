package fi.fmi.avi.archiver.message.populator.conditional;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;
import fi.fmi.avi.archiver.message.populator.MessagePopulatingContext;
import fi.fmi.avi.archiver.message.populator.MessagePopulator;
import fi.fmi.avi.archiver.message.populator.TestMessagePopulatingContext;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class ConditionalMessagePopulatorTest {
    @Mock
    private ActivationCondition condition;
    @Mock
    private MessagePopulator delegate;
    private AutoCloseable mocks;

    private MessagePopulatingContext context;
    private ArchiveAviationMessage.Builder target;
    private ConditionalMessagePopulator populator;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        context = TestMessagePopulatingContext.create(InputAviationMessage.builder().buildPartial());
        target = ArchiveAviationMessage.builder();
        populator = new ConditionalMessagePopulator(condition, delegate);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void populate_delegates_when_condition_is_true() throws MessageDiscardedException {
        when(condition.test(any(), any())).thenReturn(true);

        populator.populate(context, target);

        verify(delegate).populate(context, target);
    }

    @Test
    void populate_does_nothing_when_condition_is_false() throws MessageDiscardedException {
        when(condition.test(any(), any())).thenReturn(false);

        populator.populate(context, target);

        verify(delegate, never()).populate(context, target);
    }

    @Test
    void populate_wraps_MessageDiscardedException_describing_condition() throws MessageDiscardedException {
        final String initialMessage = "test discard";
        final String conditionDescription = "<my condition description>";
        final MessageDiscardedException initialException = new MessageDiscardedException(initialMessage);
        when(condition.test(any(), any())).thenReturn(true);
        when(condition.toString()).thenReturn(conditionDescription);
        doThrow(initialException).when(delegate).populate(any(), any());

        assertThatExceptionOfType(MessageDiscardedException.class)//
                .isThrownBy(() -> populator.populate(context, target))//
                .withMessageContaining(initialMessage)//
                .withMessageContaining(conditionDescription)//
                .withCause(initialException);
    }
}
