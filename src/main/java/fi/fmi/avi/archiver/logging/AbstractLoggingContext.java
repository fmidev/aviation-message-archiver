package fi.fmi.avi.archiver.logging;

import javax.annotation.Nullable;

import fi.fmi.avi.archiver.file.FileReference;

public abstract class AbstractLoggingContext extends AbstractAppendingLoggable implements ReadableLoggingContext {
    private static final char SEPARATOR = ':';
    /**
     * Maximum file name length is set according to maximum length of mandatory file name fields specified by
     * WMO doc 386 Manual on the Global Telecommunication System, General file naming conventions
     */
    private static final int FILENAME_MAX_LENGTH = 128;

    @Override
    public void appendTo(final StringBuilder builder) {
        builder.append(getFileProcessingIdentifier());
        @Nullable
        final FileReference fileReference = getFileReference().orElse(null);
        if (fileReference != null) {
            builder//
                    .append(SEPARATOR)//
                    .append(fileReference.getProductIdentifier())//
                    .append('/')//
                    .append(LoggableUtils.sanitize(fileReference.getFilename(), FILENAME_MAX_LENGTH));
        }
        @Nullable
        final BulletinLogReference bulletinLogReference = getBulletinLogReference().orElse(null);
        if (bulletinLogReference != null) {
            if (fileReference == null) {
                builder.append(SEPARATOR);
            }
            builder.append(SEPARATOR)//
                    .append(bulletinLogReference);
            @Nullable
            final MessageLogReference messageLogReference = getMessageLogReference().orElse(null);
            if (messageLogReference != null) {
                builder.append(SEPARATOR)//
                        .append(messageLogReference);
            }
        }
    }

    @Override
    public int estimateLogStringLength() {
        return getFileProcessingIdentifier().toString().length() //
                + estimateFileReferenceLength() //
                + estimateBulletinLogReferenceLength() //
                + estimateMessageLogReferenceLength();
    }

    private int estimateFileReferenceLength() {
        @Nullable
        final FileReference fileReference = getFileReference().orElse(null);
        return fileReference == null
                ? 0
                : fileReference.getProductIdentifier().length() + Math.min(fileReference.getFilename().length(), FILENAME_MAX_LENGTH) + 2;
    }

    private int estimateBulletinLogReferenceLength() {
        @Nullable
        final BulletinLogReference bulletinLogReference = getBulletinLogReference().orElse(null);
        return bulletinLogReference == null ? 0 : bulletinLogReference.estimateLogStringLength() + 1;
    }

    private int estimateMessageLogReferenceLength() {
        @Nullable
        final MessageLogReference messageLogReference = getMessageLogReference().orElse(null);
        return messageLogReference == null ? 0 : messageLogReference.estimateLogStringLength() + 1;
    }
}
