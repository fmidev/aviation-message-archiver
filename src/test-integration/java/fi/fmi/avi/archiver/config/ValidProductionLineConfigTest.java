package fi.fmi.avi.archiver.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidProductionLineConfigTest extends AbstractConfigValidityTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "testProduct",
            "testEqualArchiveAndFailDirsInMultipleProducts",
            "testMessagePopulatorWithEmptyConfigSection",
            "testMessagePopulatorWithEmptyActivateOnSection",
            "testPostActionWithEmptyConfigSection",
            "testPostActionWithEmptyActivateOnSection",
    })
    void testConfigIsValidForProfile(final String profileName) {
        assertThatNoExceptionIsThrownByProfile(profileName);
    }

    @Test
    void testMissingPostActionExecutionChain() {
        assertThatNoExceptionIsThrownByProfile("testMissingPostActionExecutionChain");
        assertThat(bean("postActionSpecs", List.class)).isEmpty();
    }
}
