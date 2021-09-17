package fi.fmi.avi.archiver.message.populator;

import com.google.common.collect.ImmutableList;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;
import fi.fmi.avi.archiver.message.ProcessingResult;
import fi.fmi.avi.model.bulletin.DataTypeDesignatorT1;
import fi.fmi.avi.model.bulletin.DataTypeDesignatorT2;
import fi.fmi.avi.model.bulletin.immutable.BulletinHeadingImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.regex.Pattern;

import static fi.fmi.avi.archiver.message.populator.BulletinHeadingSource.COLLECT_IDENTIFIER;
import static fi.fmi.avi.archiver.message.populator.BulletinHeadingSource.GTS_BULLETIN_HEADING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DataDesignatorDiscarderTest {

    private DataDesignatorDiscarder dataDesignatorDiscarder;

    @BeforeEach
    public void setUp() {
        dataDesignatorDiscarder = new DataDesignatorDiscarder(Pattern.compile(".*XX42"));
        dataDesignatorDiscarder.setBulletinHeadingSources(ImmutableList.of(GTS_BULLETIN_HEADING, COLLECT_IDENTIFIER));
    }

    @Test
    public void discard_based_on_gts_heading() {
        final InputAviationMessage inputAviationMessage = createTacMessage("XX", 42);
        assertThrows(MessageDiscardedException.class, () -> dataDesignatorDiscarder.populate(inputAviationMessage, ArchiveAviationMessage.builder()));
    }

    @Test
    public void discard_based_on_collect_identifier() {
        final InputAviationMessage inputAviationMessage = createIwxxmMessage("XX", 42);
        assertThrows(MessageDiscardedException.class, () -> dataDesignatorDiscarder.populate(inputAviationMessage, ArchiveAviationMessage.builder()));
    }

    @Test
    public void no_discard_based_on_gts_heading() throws MessageDiscardedException {
        final InputAviationMessage inputAviationMessage = createTacMessage("YY", 1);
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();
        dataDesignatorDiscarder.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void no_discard_based_on_collect_identifier() throws MessageDiscardedException {
        final InputAviationMessage inputAviationMessage = createIwxxmMessage("YY", 1);
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();
        dataDesignatorDiscarder.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void no_discard_without_collect_identifier_source() throws MessageDiscardedException {
        dataDesignatorDiscarder.setBulletinHeadingSources(Collections.singletonList(GTS_BULLETIN_HEADING));
        final InputAviationMessage inputAviationMessage = InputAviationMessage.builder()
                .setCollectIdentifier(createInputBulletinHeading("XX", 42))
                .setGtsBulletinHeading(createInputBulletinHeading("YY", 1))
                .buildPartial();

        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();
        dataDesignatorDiscarder.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void no_discard_without_gts_heading_source() throws MessageDiscardedException {
        dataDesignatorDiscarder.setBulletinHeadingSources(Collections.singletonList(COLLECT_IDENTIFIER));
        final InputAviationMessage inputAviationMessage = InputAviationMessage.builder()
                .setCollectIdentifier(createInputBulletinHeading("YY", 1))
                .setGtsBulletinHeading(createInputBulletinHeading("XX", 42))
                .buildPartial();

        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();
        dataDesignatorDiscarder.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    private static InputAviationMessage createTacMessage(final String locationIndicator, final int bulletinNumber) {
        return InputAviationMessage.builder()
                .setGtsBulletinHeading(createInputBulletinHeading(locationIndicator, bulletinNumber))
                .buildPartial();
    }

    private static InputAviationMessage createIwxxmMessage(final String locationIndicator, final int bulletinNumber) {
        return InputAviationMessage.builder()
                .setCollectIdentifier(createInputBulletinHeading(locationIndicator, bulletinNumber))
                .buildPartial();
    }

    private static InputBulletinHeading createInputBulletinHeading(final String locationIndicator, final int bulletinNumber) {
        return InputBulletinHeading.builder()
                .setBulletinHeading(BulletinHeadingImpl.builder()
                        .setGeographicalDesignator(locationIndicator)
                        .setBulletinNumber(bulletinNumber)
                        .setLocationIndicator("TEST")
                        .setDataTypeDesignatorT1ForTAC(DataTypeDesignatorT1.FORECASTS)
                        .setDataTypeDesignatorT2(DataTypeDesignatorT2.ForecastsDataTypeDesignatorT2.FCT_AERODROME_VT_LONG)
                        .buildPartial())
                .buildPartial();
    }

}
