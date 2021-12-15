package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import fi.fmi.avi.model.bulletin.immutable.BulletinHeadingImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.regex.Pattern;

import static fi.fmi.avi.archiver.message.populator.BulletinHeadingSource.COLLECT_IDENTIFIER;
import static fi.fmi.avi.archiver.message.populator.BulletinHeadingSource.GTS_BULLETIN_HEADING;
import static org.assertj.core.api.Assertions.assertThat;

public class StationIcaoCodeAuthorizerTest {

    private StationIcaoCodeAuthorizer stationIcaoCodeAuthorizer;

    @BeforeEach
    public void setUp() {
        stationIcaoCodeAuthorizer = new StationIcaoCodeAuthorizer(Pattern.compile("^TE..$"), Pattern.compile("^XXXX$"));
    }

    @Test
    void valid_tac_message() {
        final InputAviationMessage inputAviationMessage = createTacMessage("TEST");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setStationIcaoCode("XXXX");
        stationIcaoCodeAuthorizer.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    void valid_iwxxm_message() {
        final InputAviationMessage inputAviationMessage = createIwxxmMessage("TEST");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setStationIcaoCode("XXXX");
        stationIcaoCodeAuthorizer.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    void invalid_tac_message() {
        final InputAviationMessage inputAviationMessage = createTacMessage("TEXX");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setStationIcaoCode("YYYY");
        stationIcaoCodeAuthorizer.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.FORBIDDEN_MESSAGE_STATION_ICAO_CODE);
    }

    @Test
    void invalid_iwxxm_message() {
        final InputAviationMessage inputAviationMessage = createIwxxmMessage("TEXX");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setStationIcaoCode("YYYY");
        stationIcaoCodeAuthorizer.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.FORBIDDEN_MESSAGE_STATION_ICAO_CODE);
    }

    @Test
    void non_matching_bulletin_location_indicator() {
        final InputAviationMessage inputAviationMessage = createTacMessage("XXXX");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setStationIcaoCode("YYYY");
        stationIcaoCodeAuthorizer.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    void valid_without_collect_identifier_source() {
        stationIcaoCodeAuthorizer.setBulletinHeadingSources(Collections.singletonList(GTS_BULLETIN_HEADING));
        final InputAviationMessage inputAviationMessage = createIwxxmMessage("TEXX");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setStationIcaoCode("YYYY");
        stationIcaoCodeAuthorizer.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    void valid_without_gts_heading_source() {
        stationIcaoCodeAuthorizer.setBulletinHeadingSources(Collections.singletonList(COLLECT_IDENTIFIER));
        final InputAviationMessage inputAviationMessage = createTacMessage("TEXX");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setStationIcaoCode("YYYY");
        stationIcaoCodeAuthorizer.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    private static InputAviationMessage createTacMessage(final String locationIndicator) {
        return InputAviationMessage.builder()
                .setGtsBulletinHeading(createInputBulletinHeading(locationIndicator))
                .buildPartial();
    }

    private static InputAviationMessage createIwxxmMessage(final String locationIndicator) {
        return InputAviationMessage.builder()
                .setCollectIdentifier(createInputBulletinHeading(locationIndicator))
                .buildPartial();
    }

    private static InputBulletinHeading createInputBulletinHeading(final String locationIndicator) {
        return InputBulletinHeading.builder()
                .setBulletinHeading(BulletinHeadingImpl.builder()
                        .setLocationIndicator(locationIndicator)
                        .buildPartial())
                .buildPartial();
    }

}
