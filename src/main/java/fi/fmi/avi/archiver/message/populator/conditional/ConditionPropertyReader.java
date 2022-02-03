package fi.fmi.avi.archiver.message.populator.conditional;

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

public interface ConditionPropertyReader<T> {
    Method getValueGetterForType();

    String getPropertyName();

    @Nullable
    T readValue(InputAviationMessage input, ArchiveAviationMessage.Builder target);

    boolean validate(final T value);
}
