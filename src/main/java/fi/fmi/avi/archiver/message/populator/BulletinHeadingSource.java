package fi.fmi.avi.archiver.message.populator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;

@SuppressFBWarnings(value = "MS_EXPOSE_REP", justification = "Is immutable")
public enum BulletinHeadingSource {
    GTS_BULLETIN_HEADING {
        @Override
        public InputBulletinHeading get(final InputAviationMessage inputAviationMessage) {
            return inputAviationMessage.getGtsBulletinHeading();
        }

        @Override
        public void set(final InputAviationMessage.Builder inputAviationMessageBuilder, final InputBulletinHeading inputBulletinHeading) {
            inputAviationMessageBuilder.setGtsBulletinHeading(inputBulletinHeading);
        }
    }, //
    COLLECT_IDENTIFIER {
        @Override
        public InputBulletinHeading get(final InputAviationMessage inputAviationMessage) {
            return inputAviationMessage.getCollectIdentifier();
        }

        @Override
        public void set(final InputAviationMessage.Builder inputAviationMessageBuilder, final InputBulletinHeading inputBulletinHeading) {
            inputAviationMessageBuilder.setCollectIdentifier(inputBulletinHeading);
        }
    };

    public static final List<BulletinHeadingSource> DEFAULT_SOURCES = Collections.unmodifiableList(Arrays.asList(values()));

    private static final List<List<BulletinHeadingSource>> PERMUTATIONS = ImmutableList.of(//
            ImmutableList.of(BulletinHeadingSource.GTS_BULLETIN_HEADING), //
            ImmutableList.of(BulletinHeadingSource.COLLECT_IDENTIFIER), //
            ImmutableList.of(BulletinHeadingSource.GTS_BULLETIN_HEADING, BulletinHeadingSource.COLLECT_IDENTIFIER), //
            ImmutableList.of(BulletinHeadingSource.COLLECT_IDENTIFIER, BulletinHeadingSource.GTS_BULLETIN_HEADING));

    public static List<List<BulletinHeadingSource>> getPermutations() {
        return PERMUTATIONS;
    }

    public abstract InputBulletinHeading get(final InputAviationMessage inputAviationMessage);

    public abstract void set(InputAviationMessage.Builder inputAviationMessageBuilder, final InputBulletinHeading inputBulletinHeading);
}
