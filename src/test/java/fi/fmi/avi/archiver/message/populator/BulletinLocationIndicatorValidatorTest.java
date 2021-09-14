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

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class BulletinLocationIndicatorValidatorTest {

    private BulletinLocationIndicatorValidator bulletinLocationIndicatorValidator;

    @BeforeEach
    public void setUp() {
        bulletinLocationIndicatorValidator = new BulletinLocationIndicatorValidator();
        bulletinLocationIndicatorValidator.setBulletinLocationIndicatorPattern(Pattern.compile("^XXXX$"));
        bulletinLocationIndicatorValidator.setMessageAerodromePattern(Pattern.compile("^TE..$"));
    }

    @Test
    public void valid() {
        final InputAviationMessage inputAviationMessage = createInputAviationMessage("XXXX", "TEST");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();

        bulletinLocationIndicatorValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void invalid() {
        final InputAviationMessage inputAviationMessage = createInputAviationMessage("YYYY", "TEXX");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();
        bulletinLocationIndicatorValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.INVALID_BULLETIN_LOCATION_INDICATOR);
    }

    @Test
    public void non_matching_message_aerodrome() {
        final InputAviationMessage inputAviationMessage = createInputAviationMessage("YYYY", "XXXX");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();
        bulletinLocationIndicatorValidator.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    private static InputAviationMessage createInputAviationMessage(final String bulletinLocationIndicator, final String messageAerodrome) {
        return InputAviationMessage.builder()
                .setGtsBulletinHeading(InputBulletinHeading.builder()
                        .setBulletinHeading(BulletinHeadingImpl.builder()
                                .setLocationIndicator(bulletinLocationIndicator)
                                .buildPartial())
                        .buildPartial())
                .setMessage(GenericAviationWeatherMessageImpl.builder()
                        .putLocationIndicators(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, messageAerodrome)
                        .buildPartial())
                .buildPartial();
    }

}
