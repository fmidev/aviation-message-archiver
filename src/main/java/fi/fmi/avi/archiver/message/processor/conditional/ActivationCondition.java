package fi.fmi.avi.archiver.message.processor.conditional;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;
import fi.fmi.avi.archiver.message.processor.conditional.ActivationConditionInternals.Composition;
import fi.fmi.avi.archiver.message.processor.populator.ConditionalMessagePopulator;
import fi.fmi.avi.archiver.message.processor.populator.MessagePopulator;

import java.util.Collection;
import java.util.Optional;

import static fi.fmi.avi.archiver.message.processor.conditional.ActivationConditionInternals.compound;

/**
 * Condition when a {@link MessagePopulator} is to be activated.
 *
 * @see ConditionalMessagePopulator
 */
public interface ActivationCondition {
    /**
     * Returns an {@code ActivationCondition} that applies logical <em>and</em> operation on each of provided {@code activationConditions}.
     * This method returns an empty {@code Optional} if provided {@code activationConditions} is empty.
     *
     * @param activationConditions {@code ActivationCondition}s to apply and operation on
     * @return 'and'ed {@code ActivationCondition} or empty
     */
    static Optional<ActivationCondition> and(final Collection<? extends ActivationCondition> activationConditions) {
        return compound(activationConditions, Composition.AND);
    }

    /**
     * Returns an {@code ActivationCondition} that applies logical <em>or</em> operation on each of provided {@code activationConditions}.
     * This method returns an empty {@code Optional} if provided {@code activationConditions} is empty.
     *
     * @param activationConditions {@code ActivationCondition}s to apply or operation on
     * @return 'or'ed {@code ActivationCondition} or empty
     */
    static Optional<ActivationCondition> or(final Collection<? extends ActivationCondition> activationConditions) {
        return compound(activationConditions, Composition.OR);
    }

    /**
     * Tests whether this activation condition is satisfied.
     *
     * @param inputAviationMessage     inputAviationMessage
     * @param aviationMessageOrBuilder aviationMessageOrBuilder
     * @return {@code true} if activation condition is satisfied, {@code false} otherwise
     */
    boolean test(InputAviationMessage inputAviationMessage, ArchiveAviationMessageOrBuilder aviationMessageOrBuilder);

    /**
     * Return a string representation of this condition suitable for logging.
     *
     * @return a string representation of this condition suitable for logging
     */
    String toString();
}
