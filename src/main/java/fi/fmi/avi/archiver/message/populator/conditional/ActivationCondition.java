package fi.fmi.avi.archiver.message.populator.conditional;

import static fi.fmi.avi.archiver.message.populator.conditional.ActivationConditionInternals.compound;

import java.util.Collection;
import java.util.Optional;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.populator.conditional.ActivationConditionInternals.Composition;

public interface ActivationCondition {
    static Optional<ActivationCondition> and(final Collection<? extends ActivationCondition> activationConditions) {
        return compound(activationConditions, Composition.AND);
    }

    static Optional<ActivationCondition> or(final Collection<? extends ActivationCondition> activationConditions) {
        return compound(activationConditions, Composition.OR);
    }

    boolean test(InputAviationMessage inputAviationMessage, ArchiveAviationMessage.Builder aviationMessageBuilder);

    String toString();
}
