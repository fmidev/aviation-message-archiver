package fi.fmi.avi.archiver.message.populator.conditional;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

class StationPropertyReaderTest {
    @Test
    void readValue_given_target_without_route_returns_null() {
        final InputAviationMessage input = InputAviationMessage.builder().buildPartial();
        final ArchiveAviationMessage.Builder target = ArchiveAviationMessage.builder();
        final StationPropertyReader propertyReader = new StationPropertyReader();

        final String result = propertyReader.readValue(input, target);

        assertThat(result).isNull();
    }

    @Test
    void readValue_given_target_with_route_returns_route() {
        final String stationIcaoCode = "YUDO";
        final InputAviationMessage input = InputAviationMessage.builder().buildPartial();
        final ArchiveAviationMessage.Builder target = ArchiveAviationMessage.builder()//
                .setStationIcaoCode(stationIcaoCode);
        final StationPropertyReader propertyReader = new StationPropertyReader();

        final String result = propertyReader.readValue(input, target);

        assertThat(result).isEqualTo(stationIcaoCode);
    }

    @ParameterizedTest
    @CsvSource({"YUDO", "AAAA", "ZZZZ"})
    void validate_given_valid_originator_returns_true(final String stationIcaoCode) {
        final StationPropertyReader propertyReader = new StationPropertyReader();
        assertThat(propertyReader.validate(stationIcaoCode)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"UDO", "YUDOO", "/UDO", "Y/DO", "YU/O", "YUD/", "0UDO", "Y0DO", "YU0O", "YUD0"})
    void validate_given_invalid_designator_returns_false(final String stationIcaoCode) {
        final StationPropertyReader propertyReader = new StationPropertyReader();
        assertThat(propertyReader.validate(stationIcaoCode)).isFalse();
    }

    @Test
    void testGetValueGetterForType() {
        final class TestReader extends AbstractConditionPropertyReader<String> {
            @Nullable
            @Override
            public String readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder target) {
                return null;
            }
        }
        final StationPropertyReader reader = new StationPropertyReader();
        final TestReader controlReader = new TestReader();
        assertThat(reader.getValueGetterForType().getGenericReturnType()).isEqualTo(controlReader.getValueGetterForType().getGenericReturnType());
    }

    @Test
    void testGetPropertyName() {
        final class StationPropertyReader extends ConditionPropertyReaderTests.AbstractTestStringConditionPropertyReader {
            public StationPropertyReader() {
            }
        }
        final fi.fmi.avi.archiver.message.populator.conditional.StationPropertyReader reader //
                = new fi.fmi.avi.archiver.message.populator.conditional.StationPropertyReader();
        final StationPropertyReader controlReader = new StationPropertyReader();
        assertThat(reader.getPropertyName()).isEqualTo(controlReader.getPropertyName());
    }

    @Test
    void testToString() {
        final StationPropertyReader propertyReader = new StationPropertyReader();
        assertThat(propertyReader.toString()).isEqualTo(propertyReader.getPropertyName());
    }
}
