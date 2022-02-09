package fi.fmi.avi.archiver.message.populator.conditional;

/**
 * A factory to provide a {@link ConditionPropertyReader} instance on property name.
 */
public interface ConditionPropertyReaderFactory {
    ConditionPropertyReader<?> getInstance(String propertyName);
}
