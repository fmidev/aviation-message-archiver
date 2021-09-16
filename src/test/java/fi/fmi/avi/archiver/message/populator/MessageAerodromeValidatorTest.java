package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ProcessingResult;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.bulletin.immutable.BulletinHeadingImpl;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.regex.Pattern;

import static fi.fmi.avi.archiver.message.populator.BulletinHeadingSource.COLLECT_IDENTIFIER;
import static fi.fmi.avi.archiver.message.populator.BulletinHeadingSource.GTS_BULLETIN_HEADING;
import static org.assertj.core.api.Assertions.assertThat;

public class MessageAerodromeValidatorTest {

    private MessageAerodromeValidator messageAerodromeValidator;

    @BeforeEach
    public void setUp() {
        messageAerodromeValidator = new MessageAerodromeValidator(Pattern.compile("^TE..$"), Pattern.compile("^XXXX$"));
    }

    @Test
    public void valid_tac_message() {
        final InputAviationMessage inputAviationMessage = createTacMessage("TEST", "XXXX");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();
        messageAerodromeValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void valid_iwxxm_message() {
        final InputAviationMessage inputAviationMessage = createIwxxmMessage("TEST", "XXXX");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();
        messageAerodromeValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void invalid_tac_message() {
        final InputAviationMessage inputAviationMessage = createTacMessage("TEXX", "YYYY");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();
        messageAerodromeValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.INVALID_MESSAGE_AERODROME);
    }

    @Test
    public void invalid_iwxxm_message() {
        final InputAviationMessage inputAviationMessage = createIwxxmMessage("TEXX", "YYYY");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();
        messageAerodromeValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.INVALID_MESSAGE_AERODROME);
    }

    @Test
    public void non_matching_bulletin_location_indicator() {
        final InputAviationMessage inputAviationMessage = createTacMessage("XXXX", "YYYY");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();
        messageAerodromeValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void valid_without_collect_identifier_source() {
        messageAerodromeValidator.setBulletinHeadingSources(Collections.singletonList(GTS_BULLETIN_HEADING));
        final InputAviationMessage inputAviationMessage = createIwxxmMessage("TEXX", "YYYY");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();
        messageAerodromeValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void valid_without_gts_heading_source() {
        messageAerodromeValidator.setBulletinHeadingSources(Collections.singletonList(COLLECT_IDENTIFIER));
        final InputAviationMessage inputAviationMessage = createTacMessage("TEXX", "YYYY");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();
        messageAerodromeValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    private static InputAviationMessage createTacMessage(final String locationIndicator, final String messageAerodrome) {
        return InputAviationMessage.builder()
                .setGtsBulletinHeading(createInputBulletinHeading(locationIndicator))
                .setMessage(createMessage(messageAerodrome))
                .buildPartial();
    }

    private static InputAviationMessage createIwxxmMessage(final String locationIndicator, final String messageAerodrome) {
        return InputAviationMessage.builder()
                .setCollectIdentifier(createInputBulletinHeading(locationIndicator))
                .setMessage(createMessage(messageAerodrome))
                .buildPartial();
    }

    private static GenericAviationWeatherMessage createMessage(final String messageAerodrome) {
        return GenericAviationWeatherMessageImpl.builder()
                .putLocationIndicators(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, messageAerodrome)
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
