package fi.fmi.avi.archiver.message.populator.conditional;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.inferred.freebuilder.FreeBuilder;

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

    private static <C> StringBuilder appendCondition(final StringBuilder builder, final String conditionName, final C condition,
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
                && satisfiesPatternConditions(value);
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

    public abstract PresencePolicy getPresence();

    public abstract Set<T> getIsAnyOf();

    public abstract Set<T> getIsNoneOf();

    public abstract Pattern getMatches();

    public abstract Pattern getDoesNotMatch();

    public abstract Builder<T> toBuilder();

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        appendCondition(builder, "presence", getPresence(), presence -> false);
        appendCondition(builder, "isAnyOf", getIsAnyOf(), Set::isEmpty);
        appendCondition(builder, "isNoneOf", getIsNoneOf(), Set::isEmpty);
        appendCondition(builder, "matches", getMatches(), pattern -> pattern.pattern().isEmpty());
        appendCondition(builder, "doesNotMatch", getDoesNotMatch(), pattern -> pattern.pattern().isEmpty());
        if (builder.length() > 0) {
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

        private void validateState() {
            if (getPresence() == PresencePolicy.EMPTY && (//
                    !getIsAnyOf().isEmpty() //
                            || !getIsNoneOf().isEmpty() //
                            || !getMatches().pattern().isEmpty() //
                            || !getDoesNotMatch().pattern().isEmpty())) {
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

        public <T2> Builder<T2> transform(final Function<T, T2> function) {
            requireNonNull(function, "function");
            return GeneralPropertyPredicate.<T2> builder()//
                    .addAllIsAnyOf(getIsAnyOf().stream().map(function))//
                    .addAllIsNoneOf(getIsNoneOf().stream().map(function))//
                    .setMatches(getMatches())//
                    .setDoesNotMatch(getDoesNotMatch())//
                    .setPresence(getPresence());
        }

        public Builder<T> validate(final Predicate<T> validator) {
            requireNonNull(validator, "validator");
            validate("isAnyOf", getIsAnyOf(), validator);
            validate("isNoneOf", getIsNoneOf(), validator);
            return this;
        }

        private void validate(final String name, final Set<T> values, final Predicate<T> validator) {
            final List<T> invalidValues = values.stream()//
                    .filter(value -> !validator.test(value))//
                    .collect(Collectors.toList());
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
                    @Nullable
                    final Property contradictingProperty = property.getContradicting();
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
