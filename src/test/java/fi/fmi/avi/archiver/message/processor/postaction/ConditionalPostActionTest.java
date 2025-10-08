package fi.fmi.avi.archiver.message.processor.postaction;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.archiver.message.processor.TestMessageProcessorContext;
import fi.fmi.avi.archiver.message.processor.conditional.ActivationCondition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
class ConditionalPostActionTest {
    @Mock
    private ActivationCondition condition;
    @Mock
    private PostAction delegate;
    private AutoCloseable mocks;

    private MessageProcessorContext context;
    private ArchiveAviationMessage message;
    private ConditionalPostAction action;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        context = TestMessageProcessorContext.create(InputAviationMessage.builder().buildPartial());
        message = ArchiveAviationMessage.builder().buildPartial();
        action = new ConditionalPostAction(condition, delegate);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void run_delegates_when_condition_is_true() {
        when(condition.test(any(), any())).thenReturn(true);

        action.run(context, message);

        verify(delegate).run(context, message);
    }

    @Test
    void run_does_nothing_when_condition_is_false() {
        when(condition.test(any(), any())).thenReturn(false);

        action.run(context, message);

        verify(delegate, never()).run(context, message);
    }
}
