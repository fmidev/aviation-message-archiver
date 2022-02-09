package fi.fmi.avi.archiver.message.populator.conditional;

import static fi.fmi.avi.archiver.message.populator.conditional.ActivationConditionInternals.compound;

import java.util.Collection;
import java.util.Optional;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.populator.conditional.ActivationConditionInternals.Composition;

/**
 * Condition when a {@link fi.fmi.avi.archiver.message.populator.MessagePopulator MessagePopulator} is to be activated.
 *
 * @see ConditionalMessagePopulator
 */
public interface ActivationCondition {
    /**
     * Returns an {@code ActivationCondition} that applies logical <em>and</em> operation on each of provided {@code activationConditions}.
     * This method returns an empty {@code Optional} if provided {@code activationConditions} is empty.
     *
     * @param activationConditions
     *         {@code ActivationCondition}s to apply and operation on
     *
     * @return 'and'ed {@code ActivationCondition} or empty
     */
    static Optional<ActivationCondition> and(final Collection<? extends ActivationCondition> activationConditions) {
        return compound(activationConditions, Composition.AND);
    }

    /**
     * Returns an {@code ActivationCondition} that applies logical <em>or</em> operation on each of provided {@code activationConditions}.
     * This method returns an empty {@code Optional} if provided {@code activationConditions} is empty.
     *
     * @param activationConditions
     *         {@code ActivationCondition}s to apply or operation on
     *
     * @return 'or'ed {@code ActivationCondition} or empty
     */
    static Optional<ActivationCondition> or(final Collection<? extends ActivationCondition> activationConditions) {
        return compound(activationConditions, Composition.OR);
    }

    /**
     * Tests, whether this activation condition is satisfied.
     *
     * @param inputAviationMessage
     *         inputAviationMessage
     * @param aviationMessageBuilder
     *         aviationMessageBuilder
     *
     * @return {@code true} if activation condition is satisfied, {@code false} otherwise
     */
    boolean test(InputAviationMessage inputAviationMessage, ArchiveAviationMessage.Builder aviationMessageBuilder);

    /**
     * Return a string representation of this condition suitable for logging.
     *
     * @return a string representation of this condition suitable for logging
     */
    String toString();
}
