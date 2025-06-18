package fi.fmi.avi.archiver.util.instantiation;

import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageProcessorContext;
import fi.fmi.avi.archiver.message.populator.MessagePopulator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Executable;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.*;

public class AbstractObjectFactoryTest {
    private TestFactory factory;

    @BeforeEach
    void setUp() {
        factory = new TestFactory();
    }

    @Test
    void default_name_is_class_simple_name() {
        assertThat(factory.getName()).isEqualTo(TestPopulator.class.getSimpleName());
    }

    @Test
    void newInstance_ingores_properties_missing_in_config() {
        final TestPopulator instance = factory.newInstance(Collections.emptyMap());
        assertThat(instance).isNotNull();
        assertThat(instance.getInstantiationConfig()).as("instantiationConfig").isEmpty();
        assertThat(instance.getConfigString()).as("configString").isEmpty();
        assertThat(instance.getConfigInt()).as("configInt").isEqualTo(0);
        assertThat(instance.isConfigBoolean()).as("configBoolean").isFalse();
    }

    @Test
    void createInstance_is_invoked_with_only_instantiation_config() {
        final Map<String, Object> instantiationConfig = new HashMap<>();
        instantiationConfig.put("instantiation1", 1);
        instantiationConfig.put("instantiation2", 2);
        final Map<String, Object> config = new HashMap<>();
        config.put("configString", "configuration string");
        config.putAll(instantiationConfig);

        final TestPopulator instance = factory.newInstance(Collections.unmodifiableMap(config));
        assertThat(instance).isNotNull();
        assertThat(instance.getInstantiationConfig()).isEqualTo(instantiationConfig);
    }

    @Test
    void newInstance_populates_properties_of_non_instantiation_config() {
        final Map<String, Object> instantiationConfig = new HashMap<>();
        instantiationConfig.put("instantiation1", 1);
        final Map<String, Object> config = new HashMap<>();
        final String configString = "configuration string";
        config.put("configString", configString);
        config.put("configInt", "17");
        config.put("configBoolean", "true");
        config.putAll(instantiationConfig);

        final TestPopulator instance = factory.newInstance(Collections.unmodifiableMap(config));
        assertThat(instance).isNotNull();
        assertThat(instance.getConfigString()).as("configString").isEqualTo(configString);
        assertThat(instance.getConfigInt()).as("configInt").isEqualTo(17);
        assertThat(instance.isConfigBoolean()).as("configBoolean").isTrue();
    }

    @Test
    void newInstance_fails_on_unknown_property_config() {
        final Map<String, Object> config = new HashMap<>();
        final String unknownConfigName = "unknownConfig";
        config.put(unknownConfigName, "any value");

        assertThatIllegalArgumentException().isThrownBy(() -> factory.newInstance(Collections.unmodifiableMap(config)))//
                .withMessageContaining(unknownConfigName);
    }

    @Test
    void newInstance_fails_on_setter_failing() {
        final Map<String, Object> config = new HashMap<>();
        final String unknownConfigName = "failingProperty";
        config.put(unknownConfigName, "any value");

        assertThatIllegalStateException().isThrownBy(() -> factory.newInstance(Collections.unmodifiableMap(config)))//
                .withMessageContaining(unknownConfigName);
    }

    public enum TestConverter implements ConfigValueConverter {
        INSTANCE;

        @Nullable
        @Override
        public Object toParameterType(@Nullable final Object propertyConfigValue, final Executable targetExecutable, final int parameterIndex) {
            if (propertyConfigValue == null) {
                return null;
            }
            final Type parameterType = targetExecutable.getParameterTypes()[parameterIndex];
            return convert(propertyConfigValue, parameterType);
        }

        @Nullable
        @Override
        public Object toReturnValueType(@Nullable final Object propertyConfigValue, final Executable targetExecutable) {
            if (propertyConfigValue == null) {
                return null;
            }
            final Type parameterType = targetExecutable.getAnnotatedReturnType().getType();
            return convert(propertyConfigValue, parameterType);
        }

        private Object convert(final @Nonnull Object propertyConfigValue, final Type parameterType) {
            final String propertyConfigValueString = String.valueOf(propertyConfigValue);
            if (String.class.equals(parameterType)) {
                return propertyConfigValueString;
            } else if (int.class.equals(parameterType)) {
                return Integer.parseInt(propertyConfigValueString);
            } else if (boolean.class.equals(parameterType)) {
                return Boolean.parseBoolean(propertyConfigValueString);
            } else {
                throw new IllegalArgumentException("Unable to convert [" + propertyConfigValue + "] to " + parameterType);
            }
        }
    }

    static class TestFactory extends AbstractObjectFactory<TestPopulator> {
        @Override
        protected boolean isInstantiationConfigOption(final String configOptionName) {
            return configOptionName.startsWith("instantiation");
        }

        @Override
        protected ConfigValueConverter getConfigValueConverter() {
            return TestConverter.INSTANCE;
        }

        @Override
        protected TestPopulator createInstance(final Map<String, ?> instantiationConfig) {
            return new TestPopulator(instantiationConfig);
        }

        @Override
        public Class<TestPopulator> getType() {
            return TestPopulator.class;
        }
    }

    static class TestPopulator implements MessagePopulator {
        private final Map<String, ?> instantiationConfig;

        private String configString = "";
        private int configInt = 0;
        private boolean configBoolean = false;

        TestPopulator(final Map<String, ?> instantiationConfig) {
            this.instantiationConfig = requireNonNull(instantiationConfig, "instantiationConfig");
        }

        private static AssertionError unexpectedInvocation() {
            return new AssertionError("Unexpected invocation");
        }

        @Override
        public void populate(final MessageProcessorContext context, final ArchiveAviationMessage.Builder target) {
            throw unexpectedInvocation();
        }

        public Map<String, ?> getInstantiationConfig() {
            return instantiationConfig;
        }

        public String getConfigString() {
            return configString;
        }

        public void setConfigString(final String configString) {
            this.configString = configString;
        }

        public int getConfigInt() {
            return configInt;
        }

        public void setConfigInt(final int configInt) {
            this.configInt = configInt;
        }

        public boolean isConfigBoolean() {
            return configBoolean;
        }

        public void setConfigBoolean(final boolean configBoolean) {
            this.configBoolean = configBoolean;
        }

        // not setXXX
        public void unknownConfig(final String value) {
            throw unexpectedInvocation();
        }

        // u is lower case
        public void setunknownConfig(final String value) {
            throw unexpectedInvocation();
        }

        public void setFailingProperty(final String value) {
            throw new IllegalArgumentException("I will always fail");
        }
    }
}
