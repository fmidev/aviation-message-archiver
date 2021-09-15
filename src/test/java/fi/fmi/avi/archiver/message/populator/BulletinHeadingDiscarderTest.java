package fi.fmi.avi.archiver.message.populator;

import com.google.common.collect.ImmutableList;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;
import fi.fmi.avi.archiver.message.ProcessingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.regex.Pattern;

import static fi.fmi.avi.archiver.util.BulletinHeadingSource.COLLECT_IDENTIFIER;
import static fi.fmi.avi.archiver.util.BulletinHeadingSource.GTS_BULLETIN_HEADING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BulletinHeadingDiscarderTest {

    private BulletinHeadingDiscarder bulletinHeadingDiscarder;

    @BeforeEach
    public void setUp() {
        bulletinHeadingDiscarder = new BulletinHeadingDiscarder();
        bulletinHeadingDiscarder.setHeadingPattern(Pattern.compile("XX42"));
        bulletinHeadingDiscarder.setBulletinHeadingSources(ImmutableList.of(GTS_BULLETIN_HEADING, COLLECT_IDENTIFIER));
    }

    @Test
    public void discard_based_on_gts_heading() {
        final InputAviationMessage inputAviationMessage = createTacMessage("TEST_XX42");
        assertThrows(MessageDiscardedException.class, () -> bulletinHeadingDiscarder.populate(inputAviationMessage, ArchiveAviationMessage.builder()));
    }

    @Test
    public void discard_based_on_collect_identifier() {
        final InputAviationMessage inputAviationMessage = createIwxxmMessage("TEST_XX42");
        assertThrows(MessageDiscardedException.class, () -> bulletinHeadingDiscarder.populate(inputAviationMessage, ArchiveAviationMessage.builder()));
    }

    @Test
    public void no_discard_based_on_gts_heading() throws MessageDiscardedException {
        final InputAviationMessage inputAviationMessage = createTacMessage("TEST");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();
        bulletinHeadingDiscarder.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void no_discard_based_on_collect_identifier() throws MessageDiscardedException {
        final InputAviationMessage inputAviationMessage = createIwxxmMessage("TEST");
        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();
        bulletinHeadingDiscarder.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void no_discard_without_collect_identifier_source() throws MessageDiscardedException {
        bulletinHeadingDiscarder.setBulletinHeadingSources(Collections.singletonList(GTS_BULLETIN_HEADING));
        final InputAviationMessage inputAviationMessage = InputAviationMessage.builder()
                .setCollectIdentifier(InputBulletinHeading.builder()
                        .setBulletinHeadingString("TEST_XX42")
                        .buildPartial())
                .setGtsBulletinHeading(InputBulletinHeading.builder()
                        .setBulletinHeadingString("TEST")
                        .buildPartial())
                .buildPartial();

        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();
        bulletinHeadingDiscarder.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    @Test
    public void no_discard_without_gts_heading_source() throws MessageDiscardedException {
        bulletinHeadingDiscarder.setBulletinHeadingSources(Collections.singletonList(COLLECT_IDENTIFIER));
        final InputAviationMessage inputAviationMessage = InputAviationMessage.builder()
                .setCollectIdentifier(InputBulletinHeading.builder()
                        .setBulletinHeadingString("TEST")
                        .buildPartial())
                .setGtsBulletinHeading(InputBulletinHeading.builder()
                        .setBulletinHeadingString("TEST_XX42")
                        .buildPartial())
                .buildPartial();

        final ArchiveAviationMessage.Builder builder = ArchiveAviationMessage.builder();
        bulletinHeadingDiscarder.populate(inputAviationMessage, builder);
        assertThat(builder.getProcessingResult()).isEqualTo(ProcessingResult.OK);
    }

    private static InputAviationMessage createTacMessage(final String gtsHeading) {
        return InputAviationMessage.builder()
                .setGtsBulletinHeading(InputBulletinHeading.builder()
                        .setBulletinHeadingString(gtsHeading)
                        .buildPartial())
                .buildPartial();
    }

    private static InputAviationMessage createIwxxmMessage(final String collectIdentifier) {
        return InputAviationMessage.builder()
                .setCollectIdentifier(InputBulletinHeading.builder()
                        .setBulletinHeadingString(collectIdentifier)
                        .buildPartial())
                .buildPartial();
    }

}
