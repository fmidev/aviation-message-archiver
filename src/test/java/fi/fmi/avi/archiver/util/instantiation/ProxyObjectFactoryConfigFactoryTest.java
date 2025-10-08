package fi.fmi.avi.archiver.util.instantiation;

import com.google.common.testing.EqualsTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;

class ProxyObjectFactoryConfigFactoryTest {
    private static final int INT_VALUE = 2;
    private static final int INT_VALUE_WITHOUT_GET = 3;
    private static final long LONG_VALUE = 11;
    private static final long LONG_VALUE_WITHOUT_GET = 12;
    private static final short SHORT_VALUE = 21;
    private static final short SHORT_VALUE_WITHOUT_GET = 22;
    private static final double DOUBLE_VALUE = 31.2;
    private static final double DOUBLE_VALUE_WITHOUT_GET = 31.3;
    private static final float FLOAT_VALUE = 42.1f;
    private static final float FLOAT_VALUE_WITHOUT_GET = 42.2f;
    private static final boolean BOOLEAN_VALUE = true;
    private static final boolean BOOLEAN_VALUE_WITHOUT_IS = false;
    private static final String STRING_VALUE = "string value";
    private static final String STRING_VALUE_WITHOUT_GET = "string value without get";
    private static final String SUBCONFIG_STRING_VALUE = "subconfig string value";
    private static final String OPTIONAL_STRING_VALUE = "optional string value";
    private static final int OPTIONAL_INT_VALUE = 4;
    private static final long OPTIONAL_LONG_VALUE = 13;
    private static final double OPTIONAL_DOUBLE_VALUE = 32.1;

    private ObjectFactoryConfigFactory factory;

    static Stream<Arguments> invalidConfigTypes() {
        final HashMap<String, Object> cyclicConfig1 = new HashMap<>();
        final HashMap<String, Object> cyclicConfig2 = new HashMap<>();
        cyclicConfig1.put("cyclicConfig2", cyclicConfig2);
        cyclicConfig2.put("cyclicConfig1", cyclicConfig1);

        return Stream.of(
                arguments("Not extending ObjectFactoryConfig",
                        ObjectFactory.class,
                        Map.of(),
                        "must extend interface fi.fmi.avi.archiver.util.instantiation.ObjectFactoryConfig"),
                arguments("Classes are not supported",
                        ClassConfig.class,
                        Map.of(),
                        "must be an interface"),
                arguments("Ambiguous property names when prefix stripped",
                        AmbiguousPropertiesConfig1.class,
                        Map.of(),
                        "Ambiguous config method names"),
                arguments("Ambiguous property names when prefix stripped",
                        AmbiguousPropertiesConfig2.class,
                        Map.of(),
                        "Ambiguous config method names"),
                arguments("May not contain methods with parameters",
                        MethodsWithParametersConfig.class,
                        Map.of(),
                        "must not have parameters"),
                arguments("May not contain method returning itself",
                        SelfReturningConfig.class,
                        Map.of("selfReturningConfig", Map.of()),
                        "Circular property path detected"),
                arguments("May not contain cyclic return types",
                        CyclicConfig1.class,
                        cyclicConfig1,
                        "Circular property path detected"),
                arguments("Has invalid subconfig",
                        ConfigWithInvalidSubConfig.class,
                        Map.of("invalidSubConfig", Map.of()),
                        "must not have parameters"),
                arguments("Has invalid optional subconfig",
                        ConfigWithOptionalInvalidSubConfig.class,
                        Map.of("invalidSubConfig", Map.of()),
                        "must not have parameters")
        );
    }

    private static void assertValidConfigWithoutOptionals(final ValidConfig config) {
        assertThat(config).isNotNull();
        assertThat(config.getStringValue()).isEqualTo(STRING_VALUE);
        assertThat(config.stringValueWithoutGet()).isEqualTo(STRING_VALUE_WITHOUT_GET);
        assertThat(config.getIntValue()).isEqualTo(INT_VALUE);
        assertThat(config.intValueWithoutGet()).isEqualTo(INT_VALUE_WITHOUT_GET);
        assertThat(config.getLongValue()).isEqualTo(LONG_VALUE);
        assertThat(config.longValueWithoutGet()).isEqualTo(LONG_VALUE_WITHOUT_GET);
        assertThat(config.getShortValue()).isEqualTo(SHORT_VALUE);
        assertThat(config.shortValueWithoutGet()).isEqualTo(SHORT_VALUE_WITHOUT_GET);
        assertThat(config.getDoubleValue()).isEqualTo(DOUBLE_VALUE);
        assertThat(config.doubleValueWithoutGet()).isEqualTo(DOUBLE_VALUE_WITHOUT_GET);
        assertThat(config.getFloatValue()).isEqualTo(FLOAT_VALUE);
        assertThat(config.floatValueWithoutGet()).isEqualTo(FLOAT_VALUE_WITHOUT_GET);
        assertThat(config.isSomething()).isEqualTo(BOOLEAN_VALUE);
        assertThat(config.somethingWithoutIs()).isEqualTo(BOOLEAN_VALUE_WITHOUT_IS);

        assertThat(config.getDefaultString())
                .isEqualTo(ValidConfig.DEFAULT_STRING_PREFIX + STRING_VALUE);
        assertThat(config.concatenateWith("concatenated with"))
                .isEqualTo(ValidConfig.CONCATENATE_PREFIX + "concatenated with");
        assertThat(ValidConfig.staticMethod("param value"))
                .isEqualTo(ValidConfig.STATIC_STRING_PREFIX + "param value");

        final ValidSubConfig subConfig = config.getValidSubConfig();
        assertThat(subConfig).isNotNull();
        assertThat(subConfig.getSubConfigStringValue()).isEqualTo(SUBCONFIG_STRING_VALUE);
    }

    private static Pattern stringPropertyPattern(final String key, final Object value) {
        return Pattern.compile(".*(\\{|, )" + Pattern.quote(key + "=" + value) + "(, |}).*");
    }

    static Stream<Arguments> create_does_not_check_nested_config_when_disabled() {
        return Stream.of(
                args(ConfigWithInvalidSubConfig.class, mock(InvalidSubConfig.class),
                        ConfigWithInvalidSubConfig::getInvalidSubConfig),
                args(ConfigWithOptionalInvalidSubConfig.class, Optional.of(mock(InvalidSubConfig.class)),
                        ConfigWithOptionalInvalidSubConfig::getInvalidSubConfig)
        );
    }

    static <T extends ObjectFactoryConfig, S> Arguments args(
            final Class<T> configType, final S invalidSubConfigInstance, final Function<T, S> getInvalidSubConfig) {
        return arguments(configType, invalidSubConfigInstance, getInvalidSubConfig);
    }

    @BeforeEach
    public void setUp() {
        factory = new ProxyObjectFactoryConfigFactory(TestConfigValueConverter.INSTANCE, true);
    }

    @ParameterizedTest
    @ValueSource(classes = {ObjectFactoryConfig.class, EmptyConfig.class, ValidConfig.class})
    void isValidConfigType_returns_true_on_valid_config_types(final Class<? extends ObjectFactoryConfig> configType) {
        assertThat(factory.isValidConfigType(configType)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(classes = {ConfigWithInvalidSubConfig.class, ConfigWithOptionalInvalidSubConfig.class})
    void isValidConfigType_does_not_check_nested_config_when_disabled(final Class<? extends ObjectFactoryConfig> configType) {
        factory = new ProxyObjectFactoryConfigFactory(TestConfigValueConverter.INSTANCE, false);

        assertThat(factory.isValidConfigType(configType)).isTrue();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @ParameterizedTest
    @MethodSource("invalidConfigTypes")
    void isValidConfigType_returns_false_on_invalid_config_types(final String description, final Class configType) {
        assertThat(factory.isValidConfigType(configType)).as(description).isFalse();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @ParameterizedTest
    @MethodSource("invalidConfigTypes")
    void create_fails_on_invalid_config_types(final String description, final Class configType, final Map<?, ?> configMap, final String expectedMessageExcerpt) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .as(description)
                .isThrownBy(() -> factory.create(configType, configMap))
                .withMessageContaining(expectedMessageExcerpt);
    }

    @ParameterizedTest
    @MethodSource
    <T extends ObjectFactoryConfig, S> void create_does_not_check_nested_config_when_disabled(
            final Class<T> configType, final S invalidSubConfigInstance, final Function<T, S> getInvalidSubConfig) {
        // given
        final ConfigValueConverter configValueConverter = mock(ConfigValueConverter.class, invocation -> invalidSubConfigInstance);
        factory = new ProxyObjectFactoryConfigFactory(configValueConverter, false);
        final Map<Object, Object> configMap = Map.of("invalidSubConfig", Map.of());

        // when
        final T config = factory.create(configType, configMap);

        // then
        assertThat(getInvalidSubConfig.apply(config)).isSameAs(invalidSubConfigInstance);
    }

    @ParameterizedTest
    @ValueSource(classes = {
            ObjectFactoryConfig.class,
            EmptyConfig.class,
    })
    void empty_config(final Class<? extends ObjectFactoryConfig> configType) {
        final ObjectFactoryConfig config = factory.create(configType, Map.of());
        assertThat(config).isNotNull();
    }

    @Test
    void test_ValidConfig_without_optionals() {
        // given
        final HashMap<Object, Object> configMap = ValidConfig.getConfigMapWithoutOptionals();

        // when
        final ValidConfig config = factory.create(ValidConfig.class, configMap);

        // then
        assertValidConfigWithoutOptionals(config);
        assertThat(config.getOptionalString()).isEmpty();
        assertThat(config.getOptionalInt()).isEmpty();
        assertThat(config.getOptionalLong()).isEmpty();
        assertThat(config.getOptionalDouble()).isEmpty();
    }

    @Test
    void test_ValidConfig_with_optionals() {
        // given
        final HashMap<Object, Object> configMap = ValidConfig.getConfigMapWithOptionals();

        // when
        final ValidConfig config = factory.create(ValidConfig.class, configMap);

        // then
        assertValidConfigWithoutOptionals(config);
        assertThat(config.getOptionalString()).hasValue(OPTIONAL_STRING_VALUE);
        assertThat(config.getOptionalInt()).hasValue(OPTIONAL_INT_VALUE);
        assertThat(config.getOptionalLong()).hasValue(OPTIONAL_LONG_VALUE);
        assertThat(config.getOptionalDouble()).hasValue(OPTIONAL_DOUBLE_VALUE);
    }

    @Test
    void create_fails_with_missing_required_config_values() {
        // given
        final HashMap<Object, Object> configMap = ValidConfig.getConfigMapWithOptionals();
        final Object removed = configMap.remove("stringValue");
        // verify test integrity: a value was actually removed
        assertThat(removed).isNotNull();

        // then
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> factory.create(ValidConfig.class, configMap))
                .withMessageContaining("Missing required config option")
                .withMessageContaining("stringValue");
    }

    @Test
    void create_fails_with_unknown_config_values() {
        // given
        final HashMap<Object, Object> configMap = ValidConfig.getConfigMapWithOptionals();
        configMap.put("unknownConfig", "any value");

        // then
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> factory.create(ValidConfig.class, configMap))
                .withMessageContaining("Unknown config option")
                .withMessageContaining("unknownConfig");
    }

    @Test
    void create_fails_with_unconvertible_config_values() {
        // given
        final HashMap<Object, Object> configMap = ValidConfig.getConfigMapWithOptionals();
        final Object replaced = configMap.replace("intValue", "a string");
        // verify test integrity: a value was actually removed
        assertThat(replaced).isNotNull();

        // then
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> factory.create(ValidConfig.class, configMap))
                .withMessageContaining("Failed to resolve value for config option")
                .withMessageContaining("intValue")
                .withMessageContaining("a string");
    }

    @Test
    void test_equals_on_ValidConfig() {
        // given
        final Map<Object, Object> configMapWithoutOptionals = ValidConfig.getConfigMapWithoutOptionals();
        final Map<Object, Object> configMapWithoutOptionals2 = new TreeMap<>(configMapWithoutOptionals);
        final Map<Object, Object> configMapWithOptionals = ValidConfig.getConfigMapWithOptionals();

        // when
        final ValidConfig config1A = factory.create(ValidConfig.class, configMapWithoutOptionals);
        final ValidConfig config1B = factory.create(ValidConfig.class, configMapWithoutOptionals);
        final ValidConfig config1C = factory.create(ValidConfig.class, configMapWithoutOptionals2);
        final ValidConfig config2 = factory.create(ValidConfig.class, configMapWithOptionals);

        // then
        new EqualsTester()
                .addEqualityGroup(config1A, config1A, config1B, config1C)
                .addEqualityGroup(ValidConfigRecord.copyOf(config1A))
                .addEqualityGroup(config2)
                .addEqualityGroup(mock(ValidConfig.class))
                .addEqualityGroup(new Object())
                .testEquals();
    }

    @Test
    void hashCode_on_ValidConfig_is_stable() {
        // given
        final HashMap<Object, Object> configMap = ValidConfig.getConfigMapWithoutOptionals();

        // when
        final ValidConfig config = factory.create(ValidConfig.class, configMap);

        // then
        assertThat(config.hashCode()).isEqualTo(config.hashCode());
    }

    @Test
    void test_toString_on_ValidConfig() {
        // given
        final HashMap<Object, Object> configMap = ValidConfig.getConfigMapWithoutOptionals();

        // when
        final ValidConfig config = factory.create(ValidConfig.class, configMap);

        // then
        assertThat(config.toString()).startsWith("ValidConfig{")
                .matches(stringPropertyPattern("stringValue", STRING_VALUE))
                .matches(stringPropertyPattern("stringValueWithoutGet", STRING_VALUE_WITHOUT_GET))
                .matches(stringPropertyPattern("intValue", INT_VALUE))
                .matches(stringPropertyPattern("intValueWithoutGet", INT_VALUE_WITHOUT_GET))
                .matches(stringPropertyPattern("longValue", LONG_VALUE))
                .matches(stringPropertyPattern("longValueWithoutGet", LONG_VALUE_WITHOUT_GET))
                .matches(stringPropertyPattern("shortValue", SHORT_VALUE))
                .matches(stringPropertyPattern("shortValueWithoutGet", SHORT_VALUE_WITHOUT_GET))
                .matches(stringPropertyPattern("doubleValue", DOUBLE_VALUE))
                .matches(stringPropertyPattern("doubleValueWithoutGet", DOUBLE_VALUE_WITHOUT_GET))
                .matches(stringPropertyPattern("floatValue", FLOAT_VALUE))
                .matches(stringPropertyPattern("floatValueWithoutGet", FLOAT_VALUE_WITHOUT_GET))
                .matches(stringPropertyPattern("something", BOOLEAN_VALUE))
                .matches(stringPropertyPattern("somethingWithoutIs", BOOLEAN_VALUE_WITHOUT_IS))
                .matches(stringPropertyPattern("optionalString", Optional.empty()))
                .matches(stringPropertyPattern("optionalInt", OptionalInt.empty()))
                .matches(stringPropertyPattern("optionalLong", OptionalLong.empty()))
                .matches(stringPropertyPattern("optionalDouble", OptionalDouble.empty()))
                .matches(stringPropertyPattern("validSubConfig", config.getValidSubConfig()))
                .endsWith("}")
                .doesNotMatch("\\{, *")
                .doesNotMatch(", *}")
        ;
    }

    @Test
    void toString_on_ValidConfig_is_stable() {
        // given
        final Map<Object, Object> configMap = ValidConfig.getConfigMapWithoutOptionals();
        final Map<Object, Object> configMap2 = new TreeMap<>(configMap);

        // when
        final ValidConfig config = factory.create(ValidConfig.class, configMap);
        final ValidConfig config2 = factory.create(ValidConfig.class, configMap2);

        // then
        assertThat(config.toString())
                .as("unary config")
                .isEqualTo(config.toString());
        assertThat(config.toString())
                .as("config and config2")
                .isEqualTo(config2.toString());
    }

    @Test
    void test_getClass_on_ValidConfig() {
        // given
        final HashMap<Object, Object> configMap = ValidConfig.getConfigMapWithoutOptionals();

        // when
        final ValidConfig config = factory.create(ValidConfig.class, configMap);

        // then
        assertThat(ValidConfig.class).isAssignableFrom(config.getClass());
    }

    interface EmptyConfig extends ObjectFactoryConfig {
    }

    interface ValidConfig extends ObjectFactoryConfig {
        String DEFAULT_STRING_PREFIX = "default string concatenated with string value ";
        String CONCATENATE_PREFIX = "default string concatenated wit provided ";
        String STATIC_STRING_PREFIX = "static string concatenated with provided ";

        static HashMap<Object, Object> getConfigMapWithoutOptionals() {
            final HashMap<Object, Object> configMap = new HashMap<>();
            configMap.put("stringValue", STRING_VALUE);
            configMap.put("stringValueWithoutGet", STRING_VALUE_WITHOUT_GET);
            configMap.put("intValue", INT_VALUE);
            configMap.put("intValueWithoutGet", INT_VALUE_WITHOUT_GET);
            configMap.put("longValue", LONG_VALUE);
            configMap.put("longValueWithoutGet", LONG_VALUE_WITHOUT_GET);
            configMap.put("shortValue", SHORT_VALUE);
            configMap.put("shortValueWithoutGet", SHORT_VALUE_WITHOUT_GET);
            configMap.put("doubleValue", DOUBLE_VALUE);
            configMap.put("doubleValueWithoutGet", DOUBLE_VALUE_WITHOUT_GET);
            configMap.put("floatValue", FLOAT_VALUE);
            configMap.put("floatValueWithoutGet", FLOAT_VALUE_WITHOUT_GET);
            configMap.put("something", BOOLEAN_VALUE);
            configMap.put("somethingWithoutIs", BOOLEAN_VALUE_WITHOUT_IS);
            configMap.put("validSubConfig", ValidSubConfig.getConfigMap());
            return configMap;
        }

        static HashMap<Object, Object> getConfigMapWithOptionals() {
            final HashMap<Object, Object> configMap = getConfigMapWithoutOptionals();
            configMap.put("optionalString", OPTIONAL_STRING_VALUE);
            configMap.put("optionalInt", OPTIONAL_INT_VALUE);
            configMap.put("optionalLong", OPTIONAL_LONG_VALUE);
            configMap.put("optionalDouble", OPTIONAL_DOUBLE_VALUE);
            return configMap;
        }

        static String staticMethod(final String string) {
            requireNonNull(string, "string");
            return STATIC_STRING_PREFIX + string;
        }

        default String getDefaultString() {
            return DEFAULT_STRING_PREFIX + getStringValue();
        }

        default String concatenateWith(final String string) {
            requireNonNull(string, "string");
            return CONCATENATE_PREFIX + string;
        }

        String getStringValue();

        String stringValueWithoutGet();

        int getIntValue();

        int intValueWithoutGet();

        long getLongValue();

        long longValueWithoutGet();

        short getShortValue();

        short shortValueWithoutGet();

        double getDoubleValue();

        double doubleValueWithoutGet();

        float getFloatValue();

        float floatValueWithoutGet();

        boolean isSomething();

        boolean somethingWithoutIs();

        Optional<String> getOptionalString();

        OptionalInt getOptionalInt();

        OptionalLong getOptionalLong();

        OptionalDouble getOptionalDouble();

        ValidSubConfig getValidSubConfig();
    }

    interface ValidSubConfig extends ObjectFactoryConfig {
        static HashMap<Object, Object> getConfigMap() {
            final HashMap<Object, Object> subConfigMap = new HashMap<>();
            subConfigMap.put("subConfigStringValue", SUBCONFIG_STRING_VALUE);
            return subConfigMap;
        }

        String getSubConfigStringValue();
    }

    interface AmbiguousPropertiesConfig1 extends ObjectFactoryConfig {
        boolean getBooleanValue();

        boolean isBooleanValue();
    }

    interface AmbiguousPropertiesConfig2 extends ObjectFactoryConfig {
        boolean booleanValue();

        boolean isBooleanValue();
    }

    interface MethodsWithParametersConfig extends ObjectFactoryConfig {
        String getStringValue();

        String soSomething(String string);
    }

    interface SelfReturningConfig extends ObjectFactoryConfig {
        SelfReturningConfig getSelfReturningConfig();
    }

    interface CyclicConfig1 extends ObjectFactoryConfig {
        CyclicConfig2 getCyclicConfig2();
    }

    interface CyclicConfig2 extends ObjectFactoryConfig {
        CyclicConfig1 getCyclicConfig1();
    }

    interface ConfigWithInvalidSubConfig extends ObjectFactoryConfig {
        InvalidSubConfig getInvalidSubConfig();
    }

    interface ConfigWithOptionalInvalidSubConfig extends ObjectFactoryConfig {
        Optional<InvalidSubConfig> getInvalidSubConfig();
    }

    interface InvalidSubConfig extends ObjectFactoryConfig {
        String doSomething(String string);
    }

    static class ClassConfig implements ObjectFactoryConfig {
    }

    record ValidConfigRecord(
            String getStringValue,
            String stringValueWithoutGet,
            int getIntValue,
            int intValueWithoutGet,
            long getLongValue,
            long longValueWithoutGet,
            short getShortValue,
            short shortValueWithoutGet,
            double getDoubleValue,
            double doubleValueWithoutGet,
            float getFloatValue,
            float floatValueWithoutGet,
            boolean isSomething,
            boolean somethingWithoutIs,
            Optional<String> getOptionalString,
            OptionalInt getOptionalInt,
            OptionalLong getOptionalLong,
            OptionalDouble getOptionalDouble,
            ValidSubConfig getValidSubConfig
    ) implements ValidConfig {
        static ValidConfigRecord copyOf(final ValidConfig config) {
            return new ValidConfigRecord(
                    config.getStringValue(),
                    config.stringValueWithoutGet(),
                    config.getIntValue(),
                    config.intValueWithoutGet(),
                    config.getLongValue(),
                    config.longValueWithoutGet(),
                    config.getShortValue(),
                    config.shortValueWithoutGet(),
                    config.getDoubleValue(),
                    config.doubleValueWithoutGet(),
                    config.getFloatValue(),
                    config.floatValueWithoutGet(),
                    config.isSomething(),
                    config.somethingWithoutIs(),
                    config.getOptionalString(),
                    config.getOptionalInt(),
                    config.getOptionalLong(),
                    config.getOptionalDouble(),
                    config.getValidSubConfig()
            );
        }
    }
}
