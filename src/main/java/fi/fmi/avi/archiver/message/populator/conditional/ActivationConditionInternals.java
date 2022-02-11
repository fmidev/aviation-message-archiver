package fi.fmi.avi.archiver.message.populator.conditional;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

final class ActivationConditionInternals {
    private ActivationConditionInternals() {
        throw new AssertionError();
    }

    static Optional<ActivationCondition> compound(final Collection<? extends ActivationCondition> activationConditions, final Composition composition) {
        requireNonNull(activationConditions, "activationConditions");
        requireNonNull(composition, "composition");
        final int size = activationConditions.size();
        if (size == 0) {
            return Optional.empty();
        } else if (size == 1) {
            return Optional.of(activationConditions.iterator().next());
        } else {
            return Optional.of(new CompoundActivationCondition(activationConditions, composition));
        }
    }

    enum Composition {
        AND(false, " & "), OR(true, " | ");

        private final boolean oneSufficientToSatisfy;
        private final String stringSeparator;

        Composition(final boolean oneSufficientToSatisfy, final String stringSeparator) {
            this.oneSufficientToSatisfy = oneSufficientToSatisfy;
            this.stringSeparator = requireNonNull(stringSeparator, "stringSeparator");
        }

        public boolean isOneSufficientToSatisfy() {
            return oneSufficientToSatisfy;
        }

        public String getStringSeparator() {
            return stringSeparator;
        }
    }

    private static class CompoundActivationCondition implements ActivationCondition {
        private final Composition composition;
        private final Collection<? extends ActivationCondition> activationConditions;

        public CompoundActivationCondition(final Collection<? extends ActivationCondition> activationConditions, final Composition composition) {
            this.composition = requireNonNull(composition, "composition");
            this.activationConditions = requireNonNull(activationConditions, "activationConditions");
        }

        @Override
        public boolean test(final InputAviationMessage input, final ArchiveAviationMessage.Builder builder) {
            for (final ActivationCondition activationCondition : activationConditions) {
                if (activationCondition.test(input, builder) == composition.isOneSufficientToSatisfy()) {
                    return composition.isOneSufficientToSatisfy();
                }
            }
            return !composition.isOneSufficientToSatisfy();
        }

        @Override
        public String toString() {
            return activationConditions.stream()//
                    .map(Objects::toString)//
                    .collect(Collectors.joining(composition.getStringSeparator(), "[", "]"));
        }
    }
}
