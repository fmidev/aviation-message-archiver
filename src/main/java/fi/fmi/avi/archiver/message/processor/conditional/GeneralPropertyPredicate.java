package fi.fmi.avi.archiver.message.processor.conditional;

import com.google.common.annotations.VisibleForTesting;
import org.inferred.freebuilder.FreeBuilder;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/*
 * When adding properties, check that new property is handled properly in
 * * toString()
 * * Builder() (proper default value in constructor)
 * * Builder.validateState()
 * * Builder.transform()
 * * Builder.validate()
 * * Builder.Property
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
            final StringBuilder builder, final String conditionName, @Nullable final C condition,
            final Predicate<C> conditionIsEmpty) {
        if (!conditionIsEmpty.test(condition)) {
            builder.append(conditionName)
                    .append('{')
                    .append(condition)
                    .append('}')
                    .append(STRING_SEPARATOR);
        }
        return builder;
    }

    @Override
    public boolean test(@Nullable final T value) {
        return getPresence().test(value)
                && (value == null || PresencePolicy.EMPTY.test(value) || satisfiesConditionsOnPresentValue(value));
    }

    private boolean satisfiesConditionsOnPresentValue(final T value) {
        return satisfiesSetConditions(value)
                && satisfiesPatternConditions(value)
                && satisfiesComparableConditions(value);
    }

    private boolean satisfiesSetConditions(final T value) {
        return (getIsAnyOf().isEmpty() || getIsAnyOf().contains(value))
                && !getIsNoneOf().contains(value);
    }

    private boolean satisfiesPatternConditions(final T value) {
        if (getMatches().pattern().isEmpty() && getDoesNotMatch().pattern().isEmpty()) {
            return true;
        }
        final String valueAsString = value.toString();
        return (getMatches().pattern().isEmpty() || getMatches().matcher(valueAsString).matches())
                && (getDoesNotMatch().pattern().isEmpty() || !getDoesNotMatch().matcher(valueAsString).matches());
    }

    private boolean satisfiesComparableConditions(final T value) {
        return getComparator()
                .map(comparator -> getIsLessThan()
                        .map(boundary -> comparator.compare(value, boundary) < 0)
                        .orElse(true)
                        && getIsLessOrEqualTo()
                        .map(boundary -> comparator.compare(value, boundary) <= 0)
                        .orElse(true)
                        && getIsGreaterThan()
                        .map(boundary -> comparator.compare(value, boundary) > 0)
                        .orElse(true)
                        && getIsGreaterOrEqualTo()
                        .map(boundary -> comparator.compare(value, boundary) >= 0)
                        .orElse(true)
                )
                .orElse(true);
    }

    public abstract PresencePolicy getPresence();

    public abstract Set<T> getIsAnyOf();

    public abstract Set<T> getIsNoneOf();

    public abstract Pattern getMatches();

    public abstract Pattern getDoesNotMatch();

    public abstract Optional<Comparator<? super T>> getComparator();

    public abstract Optional<T> getIsLessThan();

    public abstract Optional<T> getIsLessOrEqualTo();

    public abstract Optional<T> getIsGreaterThan();

    public abstract Optional<T> getIsGreaterOrEqualTo();

    public abstract Builder<T> toBuilder();

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        appendCondition(builder, "presence", getPresence(), presence -> false);
        appendCondition(builder, "isAnyOf", getIsAnyOf(), Set::isEmpty);
        appendCondition(builder, "isNoneOf", getIsNoneOf(), Set::isEmpty);
        appendCondition(builder, "matches", getMatches(), pattern -> pattern.pattern().isEmpty());
        appendCondition(builder, "doesNotMatch", getDoesNotMatch(), pattern -> pattern.pattern().isEmpty());
        appendCondition(builder, "isLessThan", getIsLessThan().orElse(null), Objects::isNull);
        appendCondition(builder, "isLessOrEqualTo", getIsLessOrEqualTo().orElse(null), Objects::isNull);
        appendCondition(builder, "isGreaterThan", getIsGreaterThan().orElse(null), Objects::isNull);
        appendCondition(builder, "isGreaterOrEqualTo", getIsGreaterOrEqualTo().orElse(null), Objects::isNull);
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
        },
        EMPTY {
            @Override
            public boolean test(@Nullable final Object object) {
                return toNullable(object) == null;
            }
        },
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
        private final EnumSet<Property> explicitlySetProperties = EnumSet.noneOf(Property.class);

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
            final Set<Property> metaProperties = Property.Category.META.getProperties();
            final Set<Property> setProperties = Property.getSet(this)
                    .filter(property -> !metaProperties.contains(property))
                    .collect(Collectors.toUnmodifiableSet());
            if (getPresence() == PresencePolicy.EMPTY && !setProperties.isEmpty()) {
                throw new IllegalStateException(
                        appendCondition(new StringBuilder(), "presence", PresencePolicy.EMPTY, presence -> false)
                                .append(" is mutually exclusive with any other conditions")
                                .toString());
            }
            if (getComparator().isEmpty()) {
                final EnumSet<Property> setComparableProperties = Property.intersection(Property.Category.COMPARABLE.getProperties(), setProperties);
                if (!setComparableProperties.isEmpty()) {
                    throw new IllegalStateException(
                            "Comparable conditions <%s> cannot be applied because of missing Comparator"
                                    .formatted(setComparableProperties));
                }
            }
            Property.checkContradicting(setProperties);
        }

        public Builder<T> setIs(final T value) {
            requireNonNull(value, "value");
            explicitlySetProperties.add(Property.is);
            return clearIsAnyOf().addIsAnyOf(value);
        }

        public Builder<T> setIsAnyOf(final Set<T> elements) {
            requireNonNull(elements, "elements");
            explicitlySetProperties.add(Property.isAnyOf);
            return clearIsAnyOf().addAllIsAnyOf(elements);
        }

        public Builder<T> setIsNot(final T value) {
            requireNonNull(value, "value");
            explicitlySetProperties.add(Property.isNot);
            return clearIsNoneOf().addIsNoneOf(value);
        }

        public Builder<T> setIsNoneOf(final Set<T> elements) {
            requireNonNull(elements, "elements");
            explicitlySetProperties.add(Property.isNoneOf);
            return clearIsNoneOf().addAllIsNoneOf(elements);
        }

        public <T2> Builder<T2> transform(final Function<T, T2> valueFunction, @Nullable final Comparator<? super T2> comparator) {
            requireNonNull(valueFunction, "valueFunction");
            return GeneralPropertyPredicate.<T2>builder()
                    .addAllIsAnyOf(getIsAnyOf().stream().map(valueFunction))
                    .addAllIsNoneOf(getIsNoneOf().stream().map(valueFunction))
                    .setMatches(getMatches())
                    .setDoesNotMatch(getDoesNotMatch())
                    .setPresence(getPresence())
                    .setNullableComparator(comparator)
                    .setIsLessThan(getIsLessThan().map(valueFunction))
                    .setIsLessOrEqualTo(getIsLessOrEqualTo().map(valueFunction))
                    .setIsGreaterThan(getIsGreaterThan().map(valueFunction))
                    .setIsGreaterOrEqualTo(getIsGreaterOrEqualTo().map(valueFunction));
        }

        public Builder<T> validate(final Predicate<T> validator) {
            requireNonNull(validator, "validator");
            validate("isAnyOf", getIsAnyOf(), validator);
            validate("isNoneOf", getIsNoneOf(), validator);
            getIsLessThan().ifPresent(boundary -> validate("isLessThan", boundary, validator));
            getIsLessOrEqualTo().ifPresent(boundary -> validate("isLessOrEqualTo", boundary, validator));
            getIsGreaterThan().ifPresent(boundary -> validate("isGreaterThan", boundary, validator));
            getIsGreaterOrEqualTo().ifPresent(boundary -> validate("isGreaterOrEqualTo", boundary, validator));
            return this;
        }

        private void validate(final String name, final T value, final Predicate<T> validator) {
            if (!validator.test(value)) {
                throw new IllegalStateException(name + ": invalid value: " + value);
            }
        }

        private void validate(final String name, final Set<T> values, final Predicate<T> validator) {
            final List<T> invalidValues = values.stream()
                    .filter(value -> !validator.test(value))
                    .toList();
            if (!invalidValues.isEmpty()) {
                throw new IllegalStateException(name + ": invalid values: " + invalidValues);
            }
        }

        @VisibleForTesting
        enum Property {
            // Values are in lower camel case format instead of upper underscore to avoid need for transformation in toString().
            presence(Category.META, builder -> true),
            is(Category.COLLECTION, null),
            isAnyOf(Category.COLLECTION, null) {
                @Override
                public boolean isSet(final Builder<?> builder) {
                    return super.isSet(builder) || isSetWithAdd(builder, builder.getIsAnyOf(), is);
                }
            },
            isNot(Category.COLLECTION, null),
            isNoneOf(Category.COLLECTION, null) {
                @Override
                public boolean isSet(final Builder<?> builder) {
                    return super.isSet(builder) || isSetWithAdd(builder, builder.getIsNoneOf(), isNot);
                }
            },
            matches(Category.PATTERN, builder -> !builder.getMatches().pattern().isEmpty()),
            doesNotMatch(Category.PATTERN, builder -> !builder.getDoesNotMatch().pattern().isEmpty()),
            isLessThan(Category.COMPARABLE, builder -> builder.getIsLessThan().isPresent()),
            isLessOrEqualTo(Category.COMPARABLE, builder -> builder.getIsLessOrEqualTo().isPresent()),
            isGreaterThan(Category.COMPARABLE, builder -> builder.getIsGreaterThan().isPresent()),
            isGreaterOrEqualTo(Category.COMPARABLE, builder -> builder.getIsGreaterOrEqualTo().isPresent());

            @VisibleForTesting
            static final Map<Property, Set<Property>> CONTRADICTING_PROPERTIES = createContradictingProperties();
            private static final Map<Category, Set<Property>> PROPERTIES_BY_CATEGORY = Arrays.stream(values())
                    .collect(Collectors.groupingBy(Property::getCategory,
                            Collectors.collectingAndThen(Collectors.toCollection(() -> EnumSet.noneOf(Property.class)),
                                    Collections::unmodifiableSet)));

            private final Category category;
            @Nullable
            private final Predicate<Builder<?>> isSet;

            Property(final Category category, @Nullable final Predicate<Builder<?>> isSet) {
                this.category = requireNonNull(category, "category");
                this.isSet = isSet;
            }

            private static Map<Property, Set<Property>> createContradictingProperties() {
                final Property[] setProperties = new Property[]{is, isAnyOf, isNot, isNoneOf};
                final Property[] comparableProperties = new Property[]{isLessThan, isLessOrEqualTo, isGreaterThan, isGreaterOrEqualTo};
                final EnumMap<Property, Set<Property>> builder = new EnumMap<>(Property.class);
                builder.put(presence, Set.of());
                builder.put(is, unmodifiableEnumSet(isAnyOf, comparableProperties));
                builder.put(isAnyOf, unmodifiableEnumSet(is, comparableProperties));
                builder.put(isNot, unmodifiableEnumSet(isNoneOf, comparableProperties));
                builder.put(isNoneOf, unmodifiableEnumSet(isNot, comparableProperties));
                builder.put(matches, Set.of());
                builder.put(doesNotMatch, Set.of());
                builder.put(isLessThan, unmodifiableEnumSet(isLessOrEqualTo, setProperties));
                builder.put(isLessOrEqualTo, unmodifiableEnumSet(isLessThan, setProperties));
                builder.put(isGreaterThan, unmodifiableEnumSet(isGreaterOrEqualTo, setProperties));
                builder.put(isGreaterOrEqualTo, unmodifiableEnumSet(isGreaterThan, setProperties));
                return Collections.unmodifiableMap(builder);
            }

            private static Set<Property> unmodifiableEnumSet(final Property firstProperty, final Property[] remainingProperties) {
                return Collections.unmodifiableSet(EnumSet.of(firstProperty, remainingProperties));
            }

            static EnumSet<Property> intersection(final Set<Property> set1, final Set<Property> set2) {
                final EnumSet<Property> intersection = toEnumSet(set1);
                intersection.retainAll(set2);
                return intersection;
            }

            private static EnumSet<Property> toEnumSet(final Set<Property> properties) {
                return properties.isEmpty() ? EnumSet.noneOf(Property.class) : EnumSet.copyOf(properties);
            }

            public static void checkContradicting(final Set<Property> setProperties) {
                for (final Property property : setProperties) {
                    final EnumSet<Property> contradictingProperties = intersection(setProperties, property.getContradicting());
                    if (!contradictingProperties.isEmpty()) {
                        throw new IllegalStateException("[%s] is mutually exclusive with %s".formatted(property, contradictingProperties));
                    }
                }
            }

            public static Stream<Property> getSet(final Builder<?> builder) {
                return Arrays.stream(values())
                        .filter(property -> property.isSet(builder));
            }

            public Category getCategory() {
                return category;
            }

            public Set<Property> getContradicting() {
                return CONTRADICTING_PROPERTIES.get(this);
            }

            public boolean isSet(final Builder<?> builder) {
                return isSet == null
                        ? builder.explicitlySetProperties.contains(this)
                        : isSet.test(builder);
            }

            boolean isSetWithAdd(final Builder<?> builder, final Set<?> values, final Property contradictoryProperty) {
                return values.size() > 1
                        || !builder.explicitlySetProperties.contains(contradictoryProperty) && !values.isEmpty();
            }

            enum Category {
                META, COLLECTION, PATTERN, COMPARABLE;

                public Set<Property> getProperties() {
                    return Property.PROPERTIES_BY_CATEGORY.getOrDefault(this, Set.of());
                }
            }
        }
    }
}
