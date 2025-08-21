package fi.fmi.avi.archiver.util.instantiation;

import fi.fmi.avi.archiver.message.processor.MessageProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractTypedConfigObjectFactoryTest {

    @Mock
    private ObjectFactoryConfigFactory configFactory;

    private TestFactory testFactory;
    private AutoCloseable openMocks;

    @BeforeEach
    void setUp() {
        openMocks = MockitoAnnotations.openMocks(this);

        testFactory = new TestFactory(configFactory);
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }

    @Test
    void test_newInstance() {
        // given
        final String configValue = "a test config value";
        final Map<String, Object> configMap = Map.of("configValue", configValue);
        final ConfigImpl config = new ConfigImpl(configValue);
        when(configFactory.create(Config.class, configMap)).thenReturn(config);

        // when
        final TestMessageProcessor result = testFactory.newInstance(configMap);

        // then
        verify(configFactory).create(Config.class, configMap);
        assertThat(result.config()).isEqualTo(config);
    }

    interface Config extends ObjectFactoryConfig {
        String getConfigValue();
    }

    record ConfigImpl(String getConfigValue) implements Config {
    }

    static class TestFactory extends AbstractTypedConfigObjectFactory<TestMessageProcessor, Config> {

        TestFactory(final ObjectFactoryConfigFactory configFactory) {
            super(configFactory);
        }

        @Override
        public Class<Config> getConfigType() {
            return Config.class;
        }

        @Override
        public TestMessageProcessor newInstance(final AbstractTypedConfigObjectFactoryTest.Config config) {
            return new TestMessageProcessor(config);
        }

        @Override
        public Class<TestMessageProcessor> getType() {
            return TestMessageProcessor.class;
        }
    }

    record TestMessageProcessor(Config config) implements MessageProcessor {
        TestMessageProcessor(final Config config) {
            this.config = requireNonNull(config, "config");
        }
    }
}
