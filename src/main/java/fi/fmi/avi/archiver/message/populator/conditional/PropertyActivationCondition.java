package fi.fmi.avi.archiver.message.populator.conditional;

import static java.util.Objects.requireNonNull;

import java.util.function.Predicate;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

public class PropertyActivationCondition<T> implements ActivationCondition {
    private final ConditionPropertyReader<T> conditionPropertyReader;
    private final Predicate<T> propertyPredicate;

    public PropertyActivationCondition(final ConditionPropertyReader<T> conditionPropertyReader, final Predicate<T> propertyPredicate) {
        this.conditionPropertyReader = requireNonNull(conditionPropertyReader, "conditionPropertyReader");
        this.propertyPredicate = requireNonNull(propertyPredicate, "propertyPredicate");
    }

    @Override
    public boolean test(final InputAviationMessage inputAviationMessage, final ArchiveAviationMessage.Builder aviationMessageBuilder) {
        requireNonNull(inputAviationMessage, "inputAviationMessage");
        requireNonNull(aviationMessageBuilder, "aviationMessageBuilder");
        return propertyPredicate.test(conditionPropertyReader.readValue(inputAviationMessage, aviationMessageBuilder));
    }
}
