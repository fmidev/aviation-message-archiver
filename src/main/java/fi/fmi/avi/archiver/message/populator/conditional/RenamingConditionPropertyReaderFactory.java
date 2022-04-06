package fi.fmi.avi.archiver.message.populator.conditional;

import static java.util.Objects.requireNonNull;

import java.util.function.UnaryOperator;

public class RenamingConditionPropertyReaderFactory implements ConditionPropertyReaderFactory {
    private final ConditionPropertyReaderFactory delegate;
    private final UnaryOperator<String> renameOperator;

    public RenamingConditionPropertyReaderFactory(final ConditionPropertyReaderFactory delegate, final UnaryOperator<String> renameOperator) {
        this.delegate = requireNonNull(delegate, "delegate");
        this.renameOperator = requireNonNull(renameOperator, "renameOperator");
    }

    @Override
    public ConditionPropertyReader<?> getInstance(final String propertyName) {
        requireNonNull(propertyName, "propertyName");
        return delegate.getInstance(renameOperator.apply(propertyName));
    }
}
