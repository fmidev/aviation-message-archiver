package fi.fmi.avi.archiver.message.processor.populator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;

import java.util.List;

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

    public static final List<BulletinHeadingSource> DEFAULT_SOURCES = List.of(values());

    private static final List<List<BulletinHeadingSource>> PERMUTATIONS = List.of(//
            List.of(BulletinHeadingSource.GTS_BULLETIN_HEADING), //
            List.of(BulletinHeadingSource.COLLECT_IDENTIFIER), //
            List.of(BulletinHeadingSource.GTS_BULLETIN_HEADING, BulletinHeadingSource.COLLECT_IDENTIFIER), //
            List.of(BulletinHeadingSource.COLLECT_IDENTIFIER, BulletinHeadingSource.GTS_BULLETIN_HEADING));

    public static List<List<BulletinHeadingSource>> getPermutations() {
        return PERMUTATIONS;
    }

    public abstract InputBulletinHeading get(final InputAviationMessage inputAviationMessage);

    public abstract void set(InputAviationMessage.Builder inputAviationMessageBuilder, final InputBulletinHeading inputBulletinHeading);
}
