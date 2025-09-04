package fi.fmi.avi.archiver.message.processor.conditional;

import org.inferred.freebuilder.FreeBuilder;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/*
 * When adding properties, check that new property is handled properly in
 * * toString()
 * * Builder() (proper default value in constructor)
 * * Builder.validateState()
 * * Builder.transform()
 * * Builder.validate()
 * * Builder.property()
 */
@FreeBuilder
public abstract class GeneralPropertyPredicate<T> implements Predicate<T> {
    private static final String STRING_SEPARATOR = " & ";

    GeneralPropertyPredicate() {
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    private static <C> StringBuilder appendCondition(
            final StringBuilder builder, final String conditionName, final C condition,
            final Predicate<C> conditionIsEmpty) {
        if (!conditionIsEmpty.test(condition)) {
            builder.append(conditionName)//
                    .append('{')//
                    .append(condition)//
                    .append('}')//
                    .append(STRING_SEPARATOR);
        }
        return builder;
    }

    @Override
    public boolean test(@Nullable final T value) {
        return getPresence().test(value) //
                && (value == null || PresencePolicy.EMPTY.test(value) || satisfiesConditionsOnPresentValue(value));
    }

    private boolean satisfiesConditionsOnPresentValue(final T value) {
        return satisfiesSetConditions(value) //
                && satisfiesPatternConditions(value) //
                && satisfiesComparableConditions(value);
    }

    private boolean satisfiesSetConditions(final T value) {
        return (getIsAnyOf().isEmpty() || getIsAnyOf().contains(value)) //
                && !getIsNoneOf().contains(value);
    }

    private boolean satisfiesPatternConditions(final T value) {
        if (getMatches().pattern().isEmpty() && getDoesNotMatch().pattern().isEmpty()) {
            return true;
        }
        final String valueAsString = value.toString();
        return (getMatches().pattern().isEmpty() || getMatches().matcher(valueAsString).matches()) //
                && (getDoesNotMatch().pattern().isEmpty() || !getDoesNotMatch().matcher(valueAsString).matches());
    }

    private boolean satisfiesComparableConditions(final T value) {
        if (getIsLessThan() == null && getIsLessOrEqualTo() == null
                && getIsGreaterThan() == null && getIsGreaterOrEqualTo() == null) {
            return true;
        }

        final Comparator<? super T> cmp = comparatorOrNatural();
        try {
            if (getIsLessThan() != null && !(cmp.compare(value, getIsLessThan()) < 0)) {
                return false;
            }
            if (getIsLessOrEqualTo() != null && !(cmp.compare(value, getIsLessOrEqualTo()) <= 0)) {
                return false;
            }
            if (getIsGreaterThan() != null && !(cmp.compare(value, getIsGreaterThan()) > 0)) {
                return false;
            }
            return getIsGreaterOrEqualTo() == null || cmp.compare(value, getIsGreaterOrEqualTo()) >= 0;
        } catch (final ClassCastException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Comparator<? super T> comparatorOrNatural() {
        if (getComparator() != null) {
            return getComparator();
        }
        return (left, right) -> {
            if (left instanceof Comparable) {
                return ((Comparable<Object>) left).compareTo(right);
            }
            throw new ClassCastException("Value is not Comparable");
        };
    }

    public abstract PresencePolicy getPresence();

    public abstract Set<T> getIsAnyOf();

    public abstract Set<T> getIsNoneOf();

    public abstract Pattern getMatches();

    public abstract Pattern getDoesNotMatch();

    @Nullable
    public abstract Comparator<? super T> getComparator();

    @Nullable
    public abstract T getIsLessThan();

    @Nullable
    public abstract T getIsLessOrEqualTo();

    @Nullable
    public abstract T getIsGreaterThan();

    @Nullable
    public abstract T getIsGreaterOrEqualTo();

    public abstract Builder<T> toBuilder();

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        appendCondition(builder, "presence", getPresence(), presence -> false);
        appendCondition(builder, "isAnyOf", getIsAnyOf(), Set::isEmpty);
        appendCondition(builder, "isNoneOf", getIsNoneOf(), Set::isEmpty);
        appendCondition(builder, "matches", getMatches(), pattern -> pattern.pattern().isEmpty());
        appendCondition(builder, "doesNotMatch", getDoesNotMatch(), pattern -> pattern.pattern().isEmpty());
        appendCondition(builder, "isLessThan", getIsLessThan(), Objects::isNull);
        appendCondition(builder, "isLessOrEqualTo", getIsLessOrEqualTo(), Objects::isNull);
        appendCondition(builder, "isGreaterThan", getIsGreaterThan(), Objects::isNull);
        appendCondition(builder, "isGreaterOrEqualTo", getIsGreaterOrEqualTo(), Objects::isNull);
        if (!builder.isEmpty()) {
            builder.setLength(builder.length() - STRING_SEPARATOR.length());
        } else {
            return "isAnything";
        }
        return builder.toString();
    }

    public enum PresencePolicy {
        PRESENT {
            @Override
            public boolean test(@Nullable final Object object) {
                return toNullable(object) != null;
            }
        }, //
        EMPTY {
            @Override
            public boolean test(@Nullable final Object object) {
                return toNullable(object) == null;
            }
        }, //
        OPTIONAL {
            @Override
            public boolean test(@Nullable final Object object) {
                return true;
            }
        };

        @Nullable
        static Object toNullable(@Nullable final Object object) {
            if (object instanceof Optional) {
                return ((Optional<?>) object).orElse(null);
            } else {
                return object;
            }
        }

        abstract boolean test(@Nullable final Object object);
    }

    public static class Builder<T> extends GeneralPropertyPredicate_Builder<T> {
        private static final Pattern EMPTY_PATTERN = Pattern.compile("");
        private final EnumSet<Property> setProperties = EnumSet.noneOf(Property.class);

        Builder() {
            setMatches(EMPTY_PATTERN);
            setDoesNotMatch(EMPTY_PATTERN);
            setPresence(PresencePolicy.PRESENT);
        }

        @Override
        public GeneralPropertyPredicate<T> build() {
            validateState();
            return super.build();
        }

        @Override
        public GeneralPropertyPredicate<T> buildPartial() {
            validateState();
            return super.buildPartial();
        }

        private boolean anyComparableBoundsSet() {
            return getIsLessThan() != null
                    || getIsLessOrEqualTo() != null
                    || getIsGreaterThan() != null
                    || getIsGreaterOrEqualTo() != null;
        }

        private void validateState() {
            if (getPresence() == PresencePolicy.EMPTY && (//
                    !getIsAnyOf().isEmpty() //
                            || !getIsNoneOf().isEmpty() //
                            || !getMatches().pattern().isEmpty() //
                            || !getDoesNotMatch().pattern().isEmpty()
                            || anyComparableBoundsSet())) {
                throw new IllegalStateException(//
                        appendCondition(new StringBuilder(), "presence", PresencePolicy.EMPTY, presence -> false)//
                                .append(" is mutually exclusive with any other conditions")//
                                .toString());
            }
            Property.checkContradicting(setProperties);
        }

        public Builder<T> setIs(final T value) {
            requireNonNull(value, "value");
            setProperties.add(Property.is);
            return clearIsAnyOf().addIsAnyOf(value);
        }

        public Builder<T> setIsAnyOf(final Set<T> elements) {
            requireNonNull(elements, "elements");
            setProperties.add(Property.isAnyOf);
            return clearIsAnyOf().addAllIsAnyOf(elements);
        }

        public Builder<T> setIsNot(final T value) {
            requireNonNull(value, "value");
            setProperties.add(Property.isNot);
            return clearIsNoneOf().addIsNoneOf(value);
        }

        public Builder<T> setIsNoneOf(final Set<T> elements) {
            requireNonNull(elements, "elements");
            setProperties.add(Property.isNoneOf);
            return clearIsNoneOf().addAllIsNoneOf(elements);
        }

        @Override
        public Builder<T> setComparator(final Comparator<? super T> comparator) {
            super.setComparator(requireNonNull(comparator, "comparator"));
            return this;
        }

        @Override
        public Builder<T> setIsLessThan(final T boundExclusive) {
            super.setIsLessThan(requireNonNull(boundExclusive, "boundExclusive"));
            return this;
        }

        @Override
        public Builder<T> setIsLessOrEqualTo(final T boundInclusive) {
            super.setIsLessOrEqualTo(requireNonNull(boundInclusive, "boundInclusive"));
            return this;
        }

        @Override
        public Builder<T> setIsGreaterThan(final T boundExclusive) {
            super.setIsGreaterThan(requireNonNull(boundExclusive, "boundExclusive"));
            return this;
        }

        @Override
        public Builder<T> setIsGreaterOrEqualTo(final T boundInclusive) {
            super.setIsGreaterOrEqualTo(requireNonNull(boundInclusive, "boundInclusive"));
            return this;
        }

        public <T2> Builder<T2> transform(final Function<T, T2> function) {
            requireNonNull(function, "function");
            final Builder<T2> builder = GeneralPropertyPredicate.<T2>builder()//
                    .addAllIsAnyOf(getIsAnyOf().stream().map(function))//
                    .addAllIsNoneOf(getIsNoneOf().stream().map(function))//
                    .setMatches(getMatches())//
                    .setDoesNotMatch(getDoesNotMatch())//
                    .setPresence(getPresence());

            if (getIsLessThan() != null) {
                builder.setIsLessThan(function.apply(getIsLessThan()));
            }
            if (getIsLessOrEqualTo() != null) {
                builder.setIsLessOrEqualTo(function.apply(getIsLessOrEqualTo()));
            }
            if (getIsGreaterThan() != null) {
                builder.setIsGreaterThan(function.apply(getIsGreaterThan()));
            }
            if (getIsGreaterOrEqualTo() != null) {
                builder.setIsGreaterOrEqualTo(function.apply(getIsGreaterOrEqualTo()));
            }
            return builder;
        }

        public Builder<T> validate(final Predicate<T> validator) {
            requireNonNull(validator, "validator");
            validate("isAnyOf", getIsAnyOf(), validator);
            validate("isNoneOf", getIsNoneOf(), validator);

            if (getIsLessThan() != null && !validator.test(getIsLessThan())) {
                throw new IllegalStateException("isLessThan: invalid value: " + getIsLessThan());
            }
            if (getIsLessOrEqualTo() != null && !validator.test(getIsLessOrEqualTo())) {
                throw new IllegalStateException("isLessOrEqualTo: invalid value: " + getIsLessOrEqualTo());
            }
            if (getIsGreaterThan() != null && !validator.test(getIsGreaterThan())) {
                throw new IllegalStateException("isGreaterThan: invalid value: " + getIsGreaterThan());
            }
            if (getIsGreaterOrEqualTo() != null && !validator.test(getIsGreaterOrEqualTo())) {
                throw new IllegalStateException("isGreaterOrEqualTo: invalid value: " + getIsGreaterOrEqualTo());
            }
            return this;
        }

        private void validate(final String name, final Set<T> values, final Predicate<T> validator) {
            final List<T> invalidValues = values.stream()//
                    .filter(value -> !validator.test(value))//
                    .toList();
            if (!invalidValues.isEmpty()) {
                throw new IllegalStateException(name + ": invalid values: " + invalidValues);
            }
        }

        private enum Property {
            // Values are in lower camel case format instead of upper underscore to avoid need for transformation in toString().
            is, isAnyOf, isNot, isNoneOf;

            private static final Map<Property, Property> CONTRADICTING_PROPERTIES = createContradictingProperties();

            private static Map<Property, Property> createContradictingProperties() {
                final EnumMap<Property, Property> builder = new EnumMap<>(Property.class);
                builder.put(is, isAnyOf);
                builder.put(isAnyOf, is);
                builder.put(isNot, isNoneOf);
                builder.put(isNoneOf, isNot);
                return Collections.unmodifiableMap(builder);
            }

            public static void checkContradicting(final Set<Property> setProperties) {
                for (final Property property : setProperties) {
                    @Nullable final Property contradictingProperty = property.getContradicting();
                    if (setProperties.contains(contradictingProperty)) {
                        throw new IllegalStateException("'" + property + "' is mutually exclusive with '" + contradictingProperty + "'");
                    }
                }
            }

            @Nullable
            public Property getContradicting() {
                return CONTRADICTING_PROPERTIES.get(this);
            }
        }
    }
}
