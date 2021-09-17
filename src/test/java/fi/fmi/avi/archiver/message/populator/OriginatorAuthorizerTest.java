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

public class OriginatorAuthorizerTest {

    private OriginatorAuthorizer originatorAuthorizer;

    @BeforeEach
    public void setUp() {
        originatorAuthorizer = new OriginatorAuthorizer(Pattern.compile("^XXXX$"), Pattern.compile("^TE..$"));
    }

    @Test
    public void valid_tac_message() {
        final InputAviationMessage inputAviationMessage = createTacMessage("XXXX");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setStationIcaoCode("TEST");
        originatorAuthorizer.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void valid_iwxxm_message() {
        final InputAviationMessage inputAviationMessage = createIwxxmMessage("XXXX");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setStationIcaoCode("TEST");
        originatorAuthorizer.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void invalid_tac_message() {
        final InputAviationMessage inputAviationMessage = createTacMessage("YYYY");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setStationIcaoCode("TEXX");
        originatorAuthorizer.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.FORBIDDEN_BULLETIN_LOCATION_INDICATOR);
    }

    @Test
    public void invalid_iwxxm_message() {
        final InputAviationMessage inputAviationMessage = createIwxxmMessage("YYYY");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setStationIcaoCode("TEXX");
        originatorAuthorizer.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.FORBIDDEN_BULLETIN_LOCATION_INDICATOR);
    }

    @Test
    public void non_matching_message_aerodrome() {
        final InputAviationMessage inputAviationMessage = createTacMessage("YYYY");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setStationIcaoCode("XXXX");
        originatorAuthorizer.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void valid_without_collect_identifier_source() {
        originatorAuthorizer.setBulletinHeadingSources(Collections.singletonList(GTS_BULLETIN_HEADING));
        final InputAviationMessage inputAviationMessage = createIwxxmMessage("YYYY");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setStationIcaoCode("TEXX");
        originatorAuthorizer.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void valid_without_gts_heading_source() {
        originatorAuthorizer.setBulletinHeadingSources(Collections.singletonList(COLLECT_IDENTIFIER));
        final InputAviationMessage inputAviationMessage = createTacMessage("YYYY");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder().setStationIcaoCode("TEXX");
        originatorAuthorizer.populate(inputAviationMessage, builder);
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
