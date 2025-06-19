package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.archiver.message.processor.conditional.ActivationCondition;

import static java.util.Objects.requireNonNull;

/**
 * A {@code MessagePopulator} that invokes backing populator only when provided {@link ActivationCondition} is satisfied.
 */
public class ConditionalMessagePopulator implements MessagePopulator {
    private final ActivationCondition condition;
    private final MessagePopulator delegate;

    public ConditionalMessagePopulator(final ActivationCondition condition, final MessagePopulator delegate) {
        this.condition = requireNonNull(condition, "condition");
        this.delegate = requireNonNull(delegate, "delegate");
    }

    @Override
    public void populate(final MessageProcessorContext context, final ArchiveAviationMessage.Builder target) throws MessageDiscardedException {
        requireNonNull(context, "context");
        requireNonNull(target, "target");
        if (condition.test(context.getInputMessage(), target)) {
            try {
                delegate.populate(context, target);
            } catch (final MessageDiscardedException e) {
                throw new MessageDiscardedException("Discarded message satisfying <" + condition + ">: " + e.getMessage(), e);
            }
        }
    }
}
