package fi.fmi.avi.archiver.logging.model;

import javax.annotation.Nullable;

import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.logging.AbstractAppendingLoggable;
import fi.fmi.avi.archiver.logging.LoggableUtils;

public abstract class AbstractLoggingContext extends AbstractAppendingLoggable implements ReadableLoggingContext {
    private static final char SEPARATOR = ':';
    /**
     * Maximum file name length is set according to maximum length of mandatory file name fields specified by
     * WMO doc 386 Manual on the Global Telecommunication System, General file naming conventions
     */
    private static final int FILENAME_MAX_LENGTH = 128;

    @Override
    public void appendTo(final StringBuilder builder) {
        builder.append(getProcessingId());
        @Nullable
        final FileReference file = getFile().orElse(null);
        if (file != null) {
            builder//
                    .append(SEPARATOR)//
                    .append(file.getProductId())//
                    .append('/')//
                    .append(LoggableUtils.sanitize(file.getFilename(), FILENAME_MAX_LENGTH));
        }
        @Nullable
        final BulletinLogReference bulletin = getBulletin().orElse(null);
        if (bulletin != null) {
            if (file == null) {
                builder.append(SEPARATOR);
            }
            builder.append(SEPARATOR)//
                    .append(bulletin);
            @Nullable
            final MessageLogReference message = getMessage().orElse(null);
            if (message != null) {
                builder.append(SEPARATOR)//
                        .append(message);
            }
        }
    }

    @Override
    public int estimateLogStringLength() {
        return getProcessingId().toString().length() //
                + estimateFileReferenceLength() //
                + estimateBulletinLogReferenceLength() //
                + estimateMessageLogReferenceLength();
    }

    private int estimateFileReferenceLength() {
        @Nullable
        final FileReference file = getFile().orElse(null);
        return file == null ? 0 : file.getProductId().length() + Math.min(file.getFilename().length(), FILENAME_MAX_LENGTH) + 2;
    }

    private int estimateBulletinLogReferenceLength() {
        @Nullable
        final BulletinLogReference bulletin = getBulletin().orElse(null);
        return bulletin == null ? 0 : bulletin.estimateLogStringLength() + 1;
    }

    private int estimateMessageLogReferenceLength() {
        @Nullable
        final MessageLogReference message = getMessage().orElse(null);
        return message == null ? 0 : message.estimateLogStringLength() + 1;
    }
}
