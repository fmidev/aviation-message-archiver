package fi.fmi.avi.archiver.util.instantiation;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ObjectFactoryConfigUtilsTest {
    private final Object configValue = "testConfigValue";

    private Map<String, Object> createConfig(final String... propertyNames) {
        return Arrays.stream(propertyNames)//
                .collect(Collectors.toMap(Function.identity(), key -> configValue));
    }

    @Test
    void renameProperties_transforms_config_property_names() {
        final Map<String, Object> config = createConfig("prop1", "prop2");
        final UnaryOperator<String> renameOperator = name -> name + "RENAMED";

        final Map<String, Object> result = ObjectFactoryConfigUtils.renameProperties(config, renameOperator);

        final Map<String, Object> expectedConfig = createConfig("prop1RENAMED", "prop2RENAMED");
        assertThat(result).containsExactlyInAnyOrderEntriesOf(expectedConfig);
    }

    @Test
    void renameProperties_given_keys_transforming_to_duplicate_throws_exception() {
        final Map<String, Object> config = createConfig("prop1", "prop2");
        final UnaryOperator<String> renameOperator = name -> "RENAMED";

        assertThatIllegalArgumentException().isThrownBy(() -> ObjectFactoryConfigUtils.renameProperties(config, renameOperator))//
                .withMessageContaining("RENAMED");
    }
}
