package fi.fmi.avi.archiver.message.processor.conditional;

import com.google.common.collect.ImmutableBiMap;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;
import fi.fmi.avi.archiver.message.processor.populator.MessagePopulatorTests;
import fi.fmi.avi.archiver.message.processor.populator.MessagePopulatorTests.FormatId;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

class FormatPropertyReaderTest {
    @Test
    void readValue_given_target_without_format_returns_null() {
        final InputAviationMessage input = InputAviationMessage.builder().buildPartial();
        final ArchiveAviationMessage.Builder target = ArchiveAviationMessage.builder();
        final FormatPropertyReader propertyReader = new FormatPropertyReader(MessagePopulatorTests.FORMAT_IDS);

        final GenericAviationWeatherMessage.Format result = propertyReader.readValue(input, target);

        assertThat(result).isNull();
    }

    @ParameterizedTest
    @EnumSource(MessagePopulatorTests.FormatId.class)
    void readValue_given_target_with_format_returns_format(final FormatId formatId) {
        final InputAviationMessage input = InputAviationMessage.builder().buildPartial();
        final ArchiveAviationMessage.Builder target = ArchiveAviationMessage.builder()//
                .setFormat(formatId.getId());
        final FormatPropertyReader propertyReader = new FormatPropertyReader(MessagePopulatorTests.FORMAT_IDS);

        final GenericAviationWeatherMessage.Format result = propertyReader.readValue(input, target);

        assertThat(result).isEqualTo(formatId.getFormat());
    }

    @Test
    void validate_given_known_format_returns_true() {
        final FormatPropertyReader propertyReader = new FormatPropertyReader(ImmutableBiMap.of(FormatId.IWXXM.getFormat(), FormatId.IWXXM.getId()));

        final boolean result = propertyReader.validate(FormatId.IWXXM.getFormat());

        assertThat(result).isTrue();
    }

    @Test
    void validate_given_unknown_format_returns_true() {
        final FormatPropertyReader propertyReader = new FormatPropertyReader(ImmutableBiMap.of(FormatId.IWXXM.getFormat(), FormatId.IWXXM.getId()));

        final boolean result = propertyReader.validate(FormatId.TAC.getFormat());

        assertThat(result).isFalse();
    }

    @Test
    void testGetValueGetterForType() {
        final class TestReader extends AbstractConditionPropertyReader<GenericAviationWeatherMessage.Format> {
            @Nullable
            @Override
            public GenericAviationWeatherMessage.Format readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder target) {
                return null;
            }
        }
        final FormatPropertyReader reader = new FormatPropertyReader(ImmutableBiMap.of());
        final TestReader controlReader = new TestReader();
        assertThat(reader.getValueGetterForType().getGenericReturnType()).isEqualTo(controlReader.getValueGetterForType().getGenericReturnType());
    }

    @Test
    void testGetPropertyName() {
        final class FormatPropertyReader extends ConditionPropertyReaderTests.AbstractTestStringConditionPropertyReader {
            public FormatPropertyReader() {
            }
        }
        final fi.fmi.avi.archiver.message.processor.conditional.FormatPropertyReader reader //
                = new fi.fmi.avi.archiver.message.processor.conditional.FormatPropertyReader(ImmutableBiMap.of());
        final FormatPropertyReader controlReader = new FormatPropertyReader();
        assertThat(reader.getPropertyName()).isEqualTo(controlReader.getPropertyName());
    }

    @Test
    void testToString() {
        final FormatPropertyReader propertyReader = new FormatPropertyReader(ImmutableBiMap.of());
        assertThat(propertyReader.toString()).isEqualTo(propertyReader.getPropertyName());
    }
}
