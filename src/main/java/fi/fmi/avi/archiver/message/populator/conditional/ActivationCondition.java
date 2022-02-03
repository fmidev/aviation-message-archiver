package fi.fmi.avi.archiver.message.populator.conditional;

import java.util.Collection;
import java.util.Optional;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

@FunctionalInterface
public interface ActivationCondition {
    static Optional<ActivationCondition> and(final Collection<? extends ActivationCondition> activationConditions) {
        final int size = activationConditions.size();
        if (size == 0) {
            return Optional.empty();
        } else if (size == 1) {
            return Optional.of(activationConditions.iterator().next());
        } else {
            return Optional.of((input, builder) -> {
                for (final ActivationCondition activationCondition : activationConditions) {
                    if (!activationCondition.test(input, builder)) {
                        return false;
                    }
                }
                return true;
            });
        }
    }

    static Optional<ActivationCondition> or(final Collection<? extends ActivationCondition> activationConditions) {
        final int size = activationConditions.size();
        if (size == 0) {
            return Optional.empty();
        } else if (size == 1) {
            return Optional.of(activationConditions.iterator().next());
        } else {
            return Optional.of((input, builder) -> {
                for (final ActivationCondition activationCondition : activationConditions) {
                    if (activationCondition.test(input, builder)) {
                        return true;
                    }
                }
                return false;
            });
        }
    }

    boolean test(InputAviationMessage inputAviationMessage, ArchiveAviationMessage.Builder aviationMessageBuilder);
}
