package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidityPeriodPopulatorTest {

    private ValidityPeriodPopulator validityPeriodPopulator;
    private final InputAviationMessage inputAviationMessage = InputAviationMessage.builder().buildPartial();

    @BeforeEach
    public void setUp() {
        validityPeriodPopulator = new ValidityPeriodPopulator();
        validityPeriodPopulator.setTypeId(14);
        validityPeriodPopulator.setValidToOffset(Duration.ofHours(30));
    }

    @Test
    public void swx_validity_end() {
        final ArchiveAviationMessage.Builder aviationMessage = ArchiveAviationMessage.builder()
                .setMessageTime(Instant.parse("2019-05-10T00:00:00Z"))
                .setType(14);
        validityPeriodPopulator.populate(inputAviationMessage, aviationMessage);
        assertThat(aviationMessage.getValidTo().get()).isEqualTo(Instant.parse("2019-05-11T06:00:00Z"));
    }

    @Test
    public void taf_validity_end() {
        final ArchiveAviationMessage.Builder aviationMessage = ArchiveAviationMessage.builder()
                .setMessageTime(Instant.parse("2019-05-09T23:35:00Z"))
                .setValidFrom(Instant.parse("2019-05-10T00:00:00Z"))
                .setValidTo(Instant.parse("2019-05-11T00:00:00Z"))
                .setType(2);
        validityPeriodPopulator.populate(inputAviationMessage, aviationMessage);
        assertThat(aviationMessage.getValidTo().get()).isEqualTo(Instant.parse("2019-05-11T00:00:00Z"));
    }

    @Test
    public void metar_validity_end() {
        final ArchiveAviationMessage.Builder aviationMessage = ArchiveAviationMessage.builder()
                .setMessageTime(Instant.parse("2019-05-10T00:00:00Z"))
                .setType(1);
        validityPeriodPopulator.populate(inputAviationMessage, aviationMessage);
        assertThat(aviationMessage.getValidTo()).isNotPresent();
    }

}
