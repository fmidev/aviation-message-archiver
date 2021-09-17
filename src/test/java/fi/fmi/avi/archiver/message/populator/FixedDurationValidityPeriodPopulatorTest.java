package fi.fmi.avi.archiver.message.populator;

import com.google.common.testing.NullPointerTester;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.model.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class FixedDurationValidityPeriodPopulatorTest {

    private FixedDurationValidityPeriodPopulator fixedDurationValidityPeriodPopulator;
    private final InputAviationMessage inputAviationMessage = InputAviationMessage.builder().buildPartial();

    @BeforeEach
    public void setUp() {
        fixedDurationValidityPeriodPopulator = new FixedDurationValidityPeriodPopulator(MessagePopulatorTests.TYPE_IDS,
                MessageType.SPACE_WEATHER_ADVISORY, Duration.ofHours(30));
    }

    @Test
    public void swx_validity_end() {
        final ArchiveAviationMessage.Builder aviationMessage = ArchiveAviationMessage.builder()
                .setMessageTime(Instant.parse("2019-05-10T00:00:00Z"))
                .setType(MessagePopulatorTests.TYPE_IDS.get(MessageType.SPACE_WEATHER_ADVISORY));
        fixedDurationValidityPeriodPopulator.populate(inputAviationMessage, aviationMessage);
        assertThat(aviationMessage.getValidTo()).contains(Instant.parse("2019-05-11T06:00:00Z"));
    }

    @Test
    public void taf_validity_end() {
        final ArchiveAviationMessage.Builder aviationMessage = ArchiveAviationMessage.builder()
                .setMessageTime(Instant.parse("2019-05-09T23:35:00Z"))
                .setValidFrom(Instant.parse("2019-05-10T00:00:00Z"))
                .setValidTo(Instant.parse("2019-05-11T00:00:00Z"))
                .setType(MessagePopulatorTests.TYPE_IDS.get(MessageType.TAF));
        fixedDurationValidityPeriodPopulator.populate(inputAviationMessage, aviationMessage);
        assertThat(aviationMessage.getValidTo()).contains(Instant.parse("2019-05-11T00:00:00Z"));
    }

    @Test
    public void metar_validity_end() {
        final ArchiveAviationMessage.Builder aviationMessage = ArchiveAviationMessage.builder()
                .setMessageTime(Instant.parse("2019-05-10T00:00:00Z"))
                .setType(MessagePopulatorTests.TYPE_IDS.get(MessageType.METAR));
        fixedDurationValidityPeriodPopulator.populate(inputAviationMessage, aviationMessage);
        assertThat(aviationMessage.getValidTo()).isNotPresent();
    }

    @Test
    public void testNulls() {
        final Class<?> classUnderTest = FixedDurationValidityPeriodPopulator.class;
        final NullPointerTester tester = new NullPointerTester();
        tester.setDefault(Map.class, MessagePopulatorTests.TYPE_IDS);
        tester.testAllPublicStaticMethods(classUnderTest);
        tester.testAllPublicConstructors(classUnderTest);
        tester.testAllPublicInstanceMethods(fixedDurationValidityPeriodPopulator);
    }

}
