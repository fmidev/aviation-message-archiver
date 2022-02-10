package fi.fmi.avi.archiver.message.populator.conditional;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;

/**
 * An abstract implementation of {@code ConditionPropertyReader}, that applies convention over code principle.
 *
 * <p>
 * Condition property readers returning properties of bulletin heading are recommended to extend {@link AbstractBulletinHeadingConditionPropertyReader} instead.
 * </p>
 *
 * @param <T>
 *         property type
 */
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

    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation returns the {@link #readValue(InputAviationMessage, ArchiveAviationMessage.Builder)} method of this object.
     * </p>
     *
     * @return {@inheritDoc}
     *
     * @throws IllegalStateException
     *         if the method cannot be resolved or method return type is {@code Object}.
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation returns property name based on the implementation class name, starting with a lower case letter and removing suffix containing all
     * or any last words of {@code ConditionPropertyReader}.
     * </p>
     *
     * @return {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation returns always {@code true}. Subclasses should override this method whenever a constraint is applied on the value.
     * </p>
     *
     * @param value
     *         value to validate
     *
     * @return @inheritDoc
     */
    @Override
    public boolean validate(final T value) {
        requireNonNull(value, "value");
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation returns {@link #getPropertyName() property name}.
     * </p>
     *
     * @return {@inheritDoc}
     */
    @Override
    public String toString() {
        return getPropertyName();
    }
}
