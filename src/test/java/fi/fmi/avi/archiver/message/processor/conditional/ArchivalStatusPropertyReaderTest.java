package fi.fmi.avi.archiver.message.processor.conditional;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchivalStatus;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

class ArchivalStatusPropertyReaderTest {
    @Test
    void readValue_given_message_without_archival_status_returns_initial_status() {
        final InputAviationMessage input = InputAviationMessage.builder().buildPartial();
        final ArchiveAviationMessage.Builder message = ArchiveAviationMessage.builder();
        final ArchivalStatusPropertyReader propertyReader = new ArchivalStatusPropertyReader();

        final ArchivalStatus result = propertyReader.readValue(input, message);

        assertThat(result).isEqualTo(message.getArchivalStatus());
    }

    @ParameterizedTest
    @EnumSource(ArchivalStatus.class)
    void readValue_given_message_with_archival_status_returns_the_status(final ArchivalStatus archivalStatus) {
        final InputAviationMessage input = InputAviationMessage.builder().buildPartial();
        final ArchiveAviationMessage.Builder message = ArchiveAviationMessage.builder()//
                .setArchivalStatus(archivalStatus);
        final ArchivalStatusPropertyReader propertyReader = new ArchivalStatusPropertyReader();

        final ArchivalStatus result = propertyReader.readValue(input, message);

        assertThat(result).isEqualTo(archivalStatus);
    }

    @ParameterizedTest
    @EnumSource(ArchivalStatus.class)
    void validate_given_any_value_returns_true(final ArchivalStatus archivalStatus) {
        final ArchivalStatusPropertyReader propertyReader = new ArchivalStatusPropertyReader();

        final boolean result = propertyReader.validate(archivalStatus);

        assertThat(result).isTrue();
    }

    @Test
    void testGetValueGetterForType() {
        final class TestReader extends AbstractConditionPropertyReader<ArchivalStatus> {
            @Nullable
            @Override
            public ArchivalStatus readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder message) {
                return null;
            }
        }
        final ArchivalStatusPropertyReader reader = new ArchivalStatusPropertyReader();
        final TestReader controlReader = new TestReader();

        assertThat(reader.getValueGetterForType().getGenericReturnType()).isEqualTo(controlReader.getValueGetterForType().getGenericReturnType());
    }

    @Test
    void testGetPropertyName() {
        final class ArchivalStatusPropertyReader extends ConditionPropertyReaderTests.AbstractTestStringConditionPropertyReader {
            public ArchivalStatusPropertyReader() {
            }
        }
        final fi.fmi.avi.archiver.message.processor.conditional.ArchivalStatusPropertyReader reader //
                = new fi.fmi.avi.archiver.message.processor.conditional.ArchivalStatusPropertyReader();
        final ArchivalStatusPropertyReader controlReader = new ArchivalStatusPropertyReader();

        assertThat(reader.getPropertyName()).isEqualTo(controlReader.getPropertyName());
    }

    @Test
    void testToString() {
        final ArchivalStatusPropertyReader propertyReader = new ArchivalStatusPropertyReader();

        assertThat(propertyReader.toString()).isEqualTo(propertyReader.getPropertyName());
    }
}
