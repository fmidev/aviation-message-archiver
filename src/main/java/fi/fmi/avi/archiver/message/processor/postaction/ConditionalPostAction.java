package fi.fmi.avi.archiver.message.processor.postaction;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.archiver.message.processor.conditional.ActivationCondition;

import static java.util.Objects.requireNonNull;

/**
 * A {@code PostAction} that invokes backing action only when provided {@link ActivationCondition} is satisfied.
 */
public class ConditionalPostAction implements PostAction {
    private final ActivationCondition condition;
    private final PostAction delegate;

    public ConditionalPostAction(final ActivationCondition condition, final PostAction delegate) {
        this.condition = requireNonNull(condition, "condition");
        this.delegate = requireNonNull(delegate, "delegate");
    }

    @Override
    public void run(final MessageProcessorContext context, final ArchiveAviationMessage message) {
        requireNonNull(context, "context");
        requireNonNull(message, "message");
        if (condition.test(context.getInputMessage(), message)) {
            delegate.run(context, message);
        }
    }
}
