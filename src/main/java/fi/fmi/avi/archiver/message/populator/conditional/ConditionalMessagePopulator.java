package fi.fmi.avi.archiver.message.populator.conditional;

import static java.util.Objects.requireNonNull;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;
import fi.fmi.avi.archiver.message.populator.MessagePopulator;

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
    public void populate(final InputAviationMessage input, final ArchiveAviationMessage.Builder builder) throws MessageDiscardedException {
        requireNonNull(input, "input");
        requireNonNull(builder, "builder");
        if (condition.test(input, builder)) {
            try {
                delegate.populate(input, builder);
            } catch (final MessageDiscardedException e) {
                throw new MessageDiscardedException("Discarded message satisfying <" + condition + ">: " + e.getMessage(), e);
            }
        }
    }
}
