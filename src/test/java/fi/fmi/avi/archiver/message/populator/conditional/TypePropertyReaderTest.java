package fi.fmi.avi.archiver.message.populator.conditional;

import static org.assertj.core.api.Assertions.assertThat;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.google.common.collect.ImmutableBiMap;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorTests;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorTests.TypeId;
import fi.fmi.avi.model.MessageType;

class TypePropertyReaderTest {
    @Test
    void readValue_given_target_without_type_returns_null() {
        final InputAviationMessage input = InputAviationMessage.builder().buildPartial();
        final ArchiveAviationMessage.Builder target = ArchiveAviationMessage.builder();
        final TypePropertyReader propertyReader = new TypePropertyReader(MessagePopulatorTests.TYPE_IDS);

        final MessageType result = propertyReader.readValue(input, target);

        assertThat(result).isNull();
    }

    @ParameterizedTest
    @EnumSource(MessagePopulatorTests.TypeId.class)
    void readValue_given_target_with_type_returns_type(final MessagePopulatorTests.TypeId typeId) {
        final InputAviationMessage input = InputAviationMessage.builder().buildPartial();
        final ArchiveAviationMessage.Builder target = ArchiveAviationMessage.builder()//
                .setType(typeId.getId());
        final TypePropertyReader propertyReader = new TypePropertyReader(MessagePopulatorTests.TYPE_IDS);

        final MessageType result = propertyReader.readValue(input, target);

        assertThat(result).isEqualTo(typeId.getType());
    }

    @Test
    void validate_given_known_type_returns_true() {
        final TypePropertyReader propertyReader = new TypePropertyReader(ImmutableBiMap.of(TypeId.METAR.getType(), TypeId.METAR.getId()));

        final boolean result = propertyReader.validate(TypeId.METAR.getType());

        assertThat(result).isTrue();
    }

    @Test
    void validate_given_unknown_type_returns_true() {
        final TypePropertyReader propertyReader = new TypePropertyReader(ImmutableBiMap.of(TypeId.METAR.getType(), TypeId.METAR.getId()));

        final boolean result = propertyReader.validate(TypeId.SPECI.getType());

        assertThat(result).isFalse();
    }

    @Test
    void testGetValueGetterForType() {
        final class TestReader extends AbstractConditionPropertyReader<MessageType> {
            @Nullable
            @Override
            public MessageType readValue(final InputAviationMessage input, final ArchiveAviationMessage.Builder target) {
                return null;
            }
        }
        final TypePropertyReader reader = new TypePropertyReader(ImmutableBiMap.of());
        final TestReader controlReader = new TestReader();
        assertThat(reader.getValueGetterForType().getGenericReturnType()).isEqualTo(controlReader.getValueGetterForType().getGenericReturnType());
    }

    @Test
    void testGetPropertyName() {
        final class TypePropertyReader extends ConditionPropertyReaderTests.AbstractTestStringConditionPropertyReader {
            public TypePropertyReader() {
            }
        }
        final fi.fmi.avi.archiver.message.populator.conditional.TypePropertyReader reader //
                = new fi.fmi.avi.archiver.message.populator.conditional.TypePropertyReader(ImmutableBiMap.of());
        final TypePropertyReader controlReader = new TypePropertyReader();
        assertThat(reader.getPropertyName()).isEqualTo(controlReader.getPropertyName());
    }

    @Test
    void testToString() {
        final TypePropertyReader propertyReader = new TypePropertyReader(ImmutableBiMap.of());
        assertThat(propertyReader.toString()).isEqualTo(propertyReader.getPropertyName());
    }
}
