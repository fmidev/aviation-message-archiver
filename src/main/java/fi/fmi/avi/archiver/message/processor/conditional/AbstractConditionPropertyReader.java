package fi.fmi.avi.archiver.message.processor.conditional;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;

import java.lang.reflect.Method;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * An abstract implementation of {@code ConditionPropertyReader}, that applies convention over code principle.
 *
 * <p>
 * Condition property readers returning properties of bulletin heading are recommended to extend {@link AbstractBulletinHeadingConditionPropertyReader} instead.
 * </p>
 *
 * @param <T> property type
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
     * This implementation returns the {@link #readValue(InputAviationMessage, ArchiveAviationMessageOrBuilder)} method of this object.
     * </p>
     *
     * @return {@inheritDoc}
     * @throws IllegalStateException if the method cannot be resolved or method return type is {@code Object}.
     */
    @Override
    public Method getValueGetterForType() {
        final Method method;
        try {
            method = this.getClass().getMethod("readValue", InputAviationMessage.class, ArchiveAviationMessageOrBuilder.class);
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
     * This implementation returns {@link Comparator#naturalOrder()} if property type implements {@link Comparable}.
     * Otherwise, empty is returned. If comparison is not applicable for a {@code Comparable} value type, subclasses
     * should override this method to always return {@code Optional.empty()}.
     * </p>
     *
     * @return natural order comparator or empty
     */
    @SuppressWarnings("unchecked")
    @Override
    public Optional<Comparator<T>> getComparator() {
        return Comparable.class.isAssignableFrom(getValueGetterForType().getReturnType())
                ? Optional.of((Comparator<T>) Comparator.naturalOrder())
                : Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation returns always {@code true}. Subclasses should override this method whenever a constraint is applied on the value.
     * </p>
     *
     * @param value value to validate
     * @return {@inheritDoc}
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
