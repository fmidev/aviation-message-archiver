package fi.fmi.avi.archiver.message.populator.conditional;

public interface ConditionPropertyReaderFactory {
    ConditionPropertyReader<?> getInstance(String propertyName);
}
