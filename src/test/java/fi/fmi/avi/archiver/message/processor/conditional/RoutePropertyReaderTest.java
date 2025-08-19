package fi.fmi.avi.archiver.message.processor.conditional;

import com.google.common.collect.ImmutableBiMap;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;
import fi.fmi.avi.archiver.message.processor.MessageProcessorTestHelper;
import fi.fmi.avi.archiver.message.processor.MessageProcessorTestHelper.RouteId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

class RoutePropertyReaderTest {
    @Test
    void readValue_given_target_without_route_returns_null() {
        final InputAviationMessage input = InputAviationMessage.builder().buildPartial();
        final ArchiveAviationMessage.Builder target = ArchiveAviationMessage.builder();
        final RoutePropertyReader propertyReader = new RoutePropertyReader(MessageProcessorTestHelper.ROUTE_IDS);

        final String result = propertyReader.readValue(input, target);

        assertThat(result).isNull();
    }

    @ParameterizedTest
    @EnumSource(RouteId.class)
    void readValue_given_target_with_route_returns_route(final RouteId routeId) {
        final InputAviationMessage input = InputAviationMessage.builder().buildPartial();
        final ArchiveAviationMessage.Builder target = ArchiveAviationMessage.builder()//
                .setRoute(routeId.getId());
        final RoutePropertyReader propertyReader = new RoutePropertyReader(MessageProcessorTestHelper.ROUTE_IDS);

        final String result = propertyReader.readValue(input, target);

        assertThat(result).isEqualTo(routeId.getName());
    }

    @Test
    void validate_given_known_route_returns_true() {
        final RoutePropertyReader propertyReader = new RoutePropertyReader(ImmutableBiMap.of(RouteId.TEST2.getName(), RouteId.TEST2.getId()));

        final boolean result = propertyReader.validate(RouteId.TEST2.getName());

        assertThat(result).isTrue();
    }

    @Test
    void validate_given_unknown_route_returns_true() {
        final RoutePropertyReader propertyReader = new RoutePropertyReader(ImmutableBiMap.of(RouteId.TEST2.getName(), RouteId.TEST2.getId()));

        final boolean result = propertyReader.validate(RouteId.TEST.getName());

        assertThat(result).isFalse();
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
        final RoutePropertyReader reader = new RoutePropertyReader(ImmutableBiMap.of());
        final TestReader controlReader = new TestReader();
        assertThat(reader.getValueGetterForType().getGenericReturnType()).isEqualTo(controlReader.getValueGetterForType().getGenericReturnType());
    }

    @Test
    void testGetPropertyName() {
        final class RoutePropertyReader extends ConditionPropertyReaderTests.AbstractTestStringConditionPropertyReader {
            public RoutePropertyReader() {
            }
        }
        final fi.fmi.avi.archiver.message.processor.conditional.RoutePropertyReader reader //
                = new fi.fmi.avi.archiver.message.processor.conditional.RoutePropertyReader(ImmutableBiMap.of());
        final RoutePropertyReader controlReader = new RoutePropertyReader();
        assertThat(reader.getPropertyName()).isEqualTo(controlReader.getPropertyName());
    }

    @Test
    void testToString() {
        final RoutePropertyReader propertyReader = new RoutePropertyReader(ImmutableBiMap.of());
        assertThat(propertyReader.toString()).isEqualTo(propertyReader.getPropertyName());
    }
}
