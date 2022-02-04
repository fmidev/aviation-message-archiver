package fi.fmi.avi.archiver.message.populator.conditional;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

public abstract class AbstractConditionPropertyReader<T> implements ConditionPropertyReader<T> {
    private static final List<String> SUFFIXES = initSuffixes();

    protected AbstractConditionPropertyReader() {
    }

    private static List<String> initSuffixes() {
        final ArrayList<String> suffixes = new ArrayList<>();
        final String simpleName = AbstractConditionPropertyReader.class.getSimpleName();
        for (int i = 0, length = simpleName.length(); i < length; i++) {
            if (Character.isUpperCase(simpleName.charAt(i))) {
                suffixes.add(simpleName.substring(i));
            }
        }
        suffixes.trimToSize();
        return Collections.unmodifiableList(suffixes);
    }

    @Override
    public Method getValueGetterForType() {
        final Method method;
        try {
            method = this.getClass().getMethod("readValue", InputAviationMessage.class, ArchiveAviationMessage.Builder.class);
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException("Unable to resolve method 'readValue' in " + getClass(), e);
        }
        if (method.getReturnType().isAssignableFrom(Object.class)) {
            throw new IllegalStateException(
                    "Unable to resolve method returning property type. Explicitly implement " + getClass().getName() + "getValueGetterForType()");
        }
        return method;
    }

    @Override
    public String getPropertyName() {
        final String simpleName = getClass().getSimpleName();
        if (simpleName.isEmpty()) {
            throw new IllegalStateException("Unable to resolve property name: " + getClass() + " has no simpleName");
        }
        final StringBuilder builder = new StringBuilder(simpleName);
        builder.setCharAt(0, Character.toLowerCase(simpleName.charAt(0)));
        for (final String suffix : SUFFIXES) {
            if (simpleName.endsWith(suffix)) {
                builder.setLength(builder.length() - suffix.length());
                break;
            }
        }
        return builder.toString();
    }

    @Override
    public boolean validate(final T value) {
        return true;
    }

    @Override
    public String toString() {
        return getPropertyName();
    }
}
