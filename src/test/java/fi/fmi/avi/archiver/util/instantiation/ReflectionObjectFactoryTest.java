package fi.fmi.avi.archiver.util.instantiation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;

import com.google.common.testing.NullPointerTester;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.populator.MessagePopulator;
import fi.fmi.avi.archiver.util.instantiation.ReflectionObjectFactory.Builder;

class ReflectionObjectFactoryTest {
    private static Builder<TestPopulator> builder() {
        return ReflectionObjectFactory.builder(TestPopulator.class, AbstractObjectFactoryTest.TestConverter.INSTANCE);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Test
    public void testNulls() {
        final NullPointerTester tester = new NullPointerTester();
        @SuppressWarnings("rawtypes")
        final Class<ReflectionObjectFactory> classUnderTest = ReflectionObjectFactory.class;
        final NullPointerTester.Visibility minimalVisibility = NullPointerTester.Visibility.PACKAGE;
        tester.testStaticMethods(classUnderTest, minimalVisibility);
        tester.testConstructors(classUnderTest, minimalVisibility);
        final ReflectionObjectFactory<TestPopulator> instance = builder().build();
        tester.testInstanceMethods(instance, minimalVisibility);
    }

    @Test
    void build_fails_on_duplicate_config_arg_names() {
        final String duplicatingConfigOptionName = "duplicating";
        assertThatIllegalArgumentException().isThrownBy(() -> {//
                    builder()//
                            .addConfigArg(duplicatingConfigOptionName, int.class)//
                            .addConfigArg(duplicatingConfigOptionName, int.class);
                })//
                .withMessageContaining("Duplicate")//
                .withMessageContaining(duplicatingConfigOptionName);
    }

    @Test
    void default_name_is_class_simple_name() {
        final ReflectionObjectFactory<TestPopulator> factory = builder().build();
        assertThat(factory.getName()).isEqualTo(AbstractObjectFactoryTest.TestPopulator.class.getSimpleName());
    }

    @Test
    void custom_name() {
        final String customFactoryName = "CustomFactoryName";
        final ReflectionObjectFactory<TestPopulator> factory = builder().setName(customFactoryName).build();
        assertThat(factory.getName()).isEqualTo(customFactoryName);
    }

    @Test
    void clearing_name_restores_default_name() {
        final ReflectionObjectFactory<TestPopulator> factory = builder()//
                .setName("CustomName")//
                .clearName()//
                .build();
        assertThat(factory.getName()).isEqualTo(AbstractObjectFactoryTest.TestPopulator.class.getSimpleName());
    }

    @Test
    void setting_null_as_name_restores_default_name() {
        final ReflectionObjectFactory<TestPopulator> factory = builder()//
                .setName("CustomName")//
                .setNullableName(null)//
                .build();
        assertThat(factory.getName()).isEqualTo(AbstractObjectFactoryTest.TestPopulator.class.getSimpleName());
    }

    @Test
    void newInstance_ingores_properties_missing_in_config() {
        final ReflectionObjectFactory<TestPopulator> factory = builder().build();
        final TestPopulator instance = factory.newInstance(Collections.emptyMap());
        assertThat(instance).isNotNull();
        assertThat(instance.getInstantiation1()).as("instantiation1").isEqualTo(0);
        assertThat(instance.getInstantiation2()).as("instantiation2").isEqualTo(0);
        assertThat(instance.getConfigString()).as("configString").isEmpty();
        assertThat(instance.getConfigInt()).as("configInt").isEqualTo(0);
        assertThat(instance.isConfigBoolean()).as("configBoolean").isFalse();
    }

    @Test
    void createInstance_is_invoked_with_only_instantiation_config() {
        final Map<String, Object> config = new HashMap<>();
        config.put("instantiation1", 1);
        config.put("instantiation2", 2);
        config.put("configString", "configuration string");
        final ReflectionObjectFactory<TestPopulator> factory = builder()//
                .addConfigArg("instantiation1", int.class)//
                .addConfigArg("instantiation2", int.class)//
                .build();

        final TestPopulator instance = factory.newInstance(Collections.unmodifiableMap(config));
        assertThat(instance).isNotNull();
        assertThat(instance.getInstantiation1()).as("instantiation1").isEqualTo(1);
        assertThat(instance.getInstantiation2()).as("instantiation2").isEqualTo(2);
    }

    @Test
    void newInstance_populates_properties_of_non_instantiation_config() {
        final Map<String, Object> config = new HashMap<>();
        config.put("instantiation1", 1);
        final String configString = "configuration string";
        config.put("configString", configString);
        config.put("configInt", "17");
        config.put("configBoolean", "true");
        final ReflectionObjectFactory<TestPopulator> factory = builder()//
                .addConfigArg("instantiation1", int.class)//
                .build();

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
        final ReflectionObjectFactory<TestPopulator> factory = builder().build();

        assertThatIllegalArgumentException().isThrownBy(() -> factory.newInstance(Collections.unmodifiableMap(config)))//
                .withMessageContaining(unknownConfigName);
    }

    @Test
    void newInstance_fails_on_setter_failing() {
        final Map<String, Object> config = new HashMap<>();
        final String unknownConfigName = "failingProperty";
        config.put(unknownConfigName, "any value");
        final ReflectionObjectFactory<TestPopulator> factory = builder().build();

        assertThatIllegalStateException().isThrownBy(() -> factory.newInstance(Collections.unmodifiableMap(config)))//
                .withMessageContaining(unknownConfigName);
    }

    @Test
    void build_fails_when_constructor_not_found() {
        final Builder<TestPopulator> builder = builder()//
                .addDependencyArg("dep1")//
                .addConfigArg("instantiation1", int.class)//
                .addDependencyArg(12.3);
        assertThatIllegalStateException().isThrownBy(builder::build)//
                .withMessageContaining("No suitable public constructor found")//
                .withMessageContaining(TestPopulator.class.getName())//
                .withMessageContaining(String.class.getName() + ", " + int.class.getName() + ", " + Double.class.getName());
    }

    @Test
    void newInstance_provides_instantiation_config_and_dependencies_to_constructor() {
        final String dependency1 = "dep1";
        final Double dependency2 = 12.3;
        final ReflectionObjectFactory<TestPopulator> factory = builder()//
                .addDependencyArg(dependency1)//
                .addConfigArg("instantiation1", int.class)//
                .addDependencyArg(dependency2)//
                .addConfigArg("instantiation2", int.class)//
                .build();
        final Map<String, Object> config = new HashMap<>();
        config.put("instantiation1", 1);
        config.put("instantiation2", 2);

        final TestPopulator instance = factory.newInstance(config);
        assertThat(instance.getInstantiation1()).isEqualTo(1);
        assertThat(instance.getInstantiation2()).isEqualTo(2);
        assertThat(instance.getDependency1()).isSameAs(dependency1);
        assertThat(instance.getDependency2()).isSameAs(dependency2);
    }

    @Test
    void newInstance_fails_on_missing_instantiation_config_option() {
        final String dependency1 = "dep1";
        final Double dependency2 = 12.3;
        final ReflectionObjectFactory<TestPopulator> factory = builder()//
                .addDependencyArg(dependency1)//
                .addConfigArg("instantiation1", int.class)//
                .addDependencyArg(dependency2)//
                .addConfigArg("instantiation2", int.class)//
                .build();
        final Map<String, Object> config = new HashMap<>();
        config.put("instantiation2", 2);

        assertThatIllegalArgumentException().isThrownBy(() -> factory.newInstance(config))//
                .withMessageContaining("Missing required config option")//
                .withMessageContaining("instantiation1")//
                .withMessageContaining(factory.getName())//
                .withMessageContaining(factory.getType().getName());
    }

    @Test
    void newInstance_fails_on_config_value_conversion_failure() {
        final ReflectionObjectFactory<TestPopulator> factory = builder()//
                .addConfigArg("inconvertible", StringBuilder.class)//
                .build();
        final Map<String, Object> config = new HashMap<>();
        config.put("inconvertible", "inconvertible value");

        assertThatIllegalArgumentException().isThrownBy(() -> factory.newInstance(config))//
                .withMessageContaining("Unable to convert")//
                .withMessageContaining("inconvertible")//
                .withMessageContaining(factory.getName())//
                .withMessageContaining(factory.getType().getName());
    }

    @Test
    void newInstance_fails_on_constructor_throwing_exception() {
        final ReflectionObjectFactory<TestPopulator> factory = builder()//
                .addConfigArg("fail", boolean.class)//
                .build();
        final Map<String, Object> config = new HashMap<>();
        config.put("fail", "true");

        assertThatIllegalStateException().isThrownBy(() -> factory.newInstance(config))//
                .withMessageContaining("Error while constructing")//
                .withMessageContaining(factory.getName())//
                .withMessageContaining(factory.getType().getName())//
                .havingCause()//
                .withMessage(TestPopulator.CONSTRUCTOR_FAILURE_MESSAGE);
    }

    static class TestPopulator implements MessagePopulator {
        private static final String CONSTRUCTOR_FAILURE_MESSAGE = "TestPopulator failing on request";
        private final int instantiation1;
        private final int instantiation2;
        @Nullable
        private final String dependency1;
        @Nullable
        private final Number dependency2;

        private String configString = "";
        private int configInt = 0;
        private boolean configBoolean = false;

        public TestPopulator() {
            this(null, 0, null, 0);
        }

        public TestPopulator(final int instantiation1) {
            this(null, instantiation1, null, 0);
        }

        public TestPopulator(final int instantiation1, final int instantiation2) {
            this(null, instantiation1, null, instantiation2);
        }

        public TestPopulator(@Nullable final String dependency1, final int instantiation1, @Nullable final Number dependency2, final int instantiation2) {
            this.instantiation1 = instantiation1;
            this.dependency1 = dependency1;
            this.instantiation2 = instantiation2;
            this.dependency2 = dependency2;
        }

        public TestPopulator(final StringBuilder inconvertible) {
            this();
        }

        public TestPopulator(final boolean fail) {
            this();
            if (fail) {
                throw new RuntimeException(CONSTRUCTOR_FAILURE_MESSAGE);
            }
        }

        private static AssertionError unexpectedInvocation() {
            return new AssertionError("Unexpected invocation");
        }

        @Override
        public void populate(final InputAviationMessage inputAviationMessage, final ArchiveAviationMessage.Builder aviationMessageBuilder) {
            throw unexpectedInvocation();
        }

        public int getInstantiation1() {
            return instantiation1;
        }

        public int getInstantiation2() {
            return instantiation2;
        }

        @Nullable
        public String getDependency1() {
            return dependency1;
        }

        @Nullable
        public Number getDependency2() {
            return dependency2;
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
