package fi.fmi.avi.archiver.util.instantiation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PropertyRenamingObjectFactoryTest {
    private final Object configValue = "testConfigValue";

    @Mock
    private ObjectFactory<Object> delegate;
    @Captor
    private ArgumentCaptor<Map<String, Object>> configCaptor;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    private Map<String, Object> createConfig(final String... propertyNames) {
        return Arrays.stream(propertyNames)//
                .collect(Collectors.toMap(Function.identity(), key -> configValue));
    }

    @Test
    void newInstance_delegates_with_transformed_config_property_names() {
        final PropertyRenamingObjectFactory<Object> factory = new PropertyRenamingObjectFactory<>(delegate, name -> name + "RENAMED");

        final Map<String, Object> config = createConfig("prop1", "prop2");

        factory.newInstance(config);

        verify(delegate).newInstance(configCaptor.capture());
        final Map<String, Object> actualConfig = configCaptor.getValue();
        final Map<String, Object> expectedConfig = createConfig("prop1RENAMED", "prop2RENAMED");
        assertThat(actualConfig).containsExactlyInAnyOrderEntriesOf(expectedConfig);
    }

    @Test
    void newInstance_given_keys_transforming_to_duplicate_throws_exception() {
        final PropertyRenamingObjectFactory<Object> factory = new PropertyRenamingObjectFactory<>(delegate, name -> "RENAMED");
        final Map<String, Object> config = createConfig("prop1", "prop2");

        assertThatIllegalArgumentException().isThrownBy(() -> factory.newInstance(config))//
                .withMessageContaining("RENAMED");
    }
}
