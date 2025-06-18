package fi.fmi.avi.archiver.message.populator.conditional;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;

import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public class PropertyActivationCondition<T> implements ActivationCondition {
    private final ConditionPropertyReader<T> conditionPropertyReader;
    private final Predicate<T> propertyPredicate;

    public PropertyActivationCondition(final ConditionPropertyReader<T> conditionPropertyReader, final Predicate<T> propertyPredicate) {
        this.conditionPropertyReader = requireNonNull(conditionPropertyReader, "conditionPropertyReader");
        this.propertyPredicate = requireNonNull(propertyPredicate, "propertyPredicate");
    }

    @Override
    public boolean test(final InputAviationMessage inputAviationMessage, final ArchiveAviationMessageOrBuilder aviationMessageOrBuilder) {
        requireNonNull(inputAviationMessage, "inputAviationMessage");
        requireNonNull(aviationMessageOrBuilder, "aviationMessageOrBuilder");
        return propertyPredicate.test(conditionPropertyReader.readValue(inputAviationMessage, aviationMessageOrBuilder));
    }

    @Override
    public String toString() {
        return conditionPropertyReader + ": " + propertyPredicate;
    }
}
