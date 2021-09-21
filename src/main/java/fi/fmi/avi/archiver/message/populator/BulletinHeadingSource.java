package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;

public enum BulletinHeadingSource {
    GTS_BULLETIN_HEADING {
        @Override
        public InputBulletinHeading get(final InputAviationMessage inputAviationMessage) {
            return inputAviationMessage.getGtsBulletinHeading();
        }
    }, COLLECT_IDENTIFIER {
        @Override
        public InputBulletinHeading get(final InputAviationMessage inputAviationMessage) {
            return inputAviationMessage.getCollectIdentifier();
        }
    };

    public abstract InputBulletinHeading get(final InputAviationMessage inputAviationMessage);
}
