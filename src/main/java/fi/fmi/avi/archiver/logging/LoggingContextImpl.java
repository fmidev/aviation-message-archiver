package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;

import javax.annotation.Nullable;

import fi.fmi.avi.archiver.file.FileProcessingIdentifier;
import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.message.MessageReference;

// TODO: SynchronizedLoggingContext & SynchronizedFileProcessingStatistics
// TODO: Tests
// TODO: Document
// TODO: Cleanup
// TODO: More debug / trace logging?
public class LoggingContextImpl extends AbstractAppendingLoggable implements LoggingContext {
    private static final char SEPARATOR = ':';
    private static final int FILENAME_MAX_LENGTH = 256;

    private final FileProcessingIdentifier fileProcessingIdentifier;
    private final FileProcessingStatistics fileProcessingStatistics;
    private final List<BulletinLogReference> bulletinLogReferences = new ArrayList<>();
    private final List<List<MessageLogReference>> bulletinMessageLogReferences = new ArrayList<>();

    @Nullable
    private FileReference fileReference;

    private int bulletinIndex = -1;
    private int messageIndex = -1;

    public LoggingContextImpl(final FileProcessingIdentifier fileProcessingIdentifier, final FileProcessingStatistics fileProcessingStatistics) {
        this.fileProcessingIdentifier = requireNonNull(fileProcessingIdentifier, "fileProcessingIdentifier");
        this.fileProcessingStatistics = requireNonNull(fileProcessingStatistics, "fileProcessingStatistics");
    }

    private static <E> void ensureIndex(final List<E> list, final int expectedIndex, final IntFunction<E> defaultElement) {
        for (int nextIndex = list.size(); expectedIndex >= nextIndex; nextIndex++) {
            list.add(defaultElement.apply(nextIndex));
        }
    }

    @Override
    public void appendTo(final StringBuilder builder) {
        builder.append(fileProcessingIdentifier);
        if (fileReference != null) {
            builder//
                    .append(SEPARATOR)//
                    .append(fileReference.getProductIdentifier())//
                    .append('/')//
                    .append(LoggableUtils.sanitize(fileReference.getFilename(), FILENAME_MAX_LENGTH));
            if (bulletinIndex >= 0) {
                builder.append(SEPARATOR);
                bulletinLogReferences.get(bulletinIndex).appendTo(builder);
                if (messageIndex >= 0) {
                    builder.append(SEPARATOR);
                    bulletinMessageLogReferences.get(bulletinIndex).get(messageIndex).appendTo(builder);
                }
            }
        }
    }

    @Override
    protected int estimateLogStringLength() {
        return fileProcessingIdentifier.toString().length() //
                + (fileReference == null
                ? 0
                : fileReference.getProductIdentifier().length() + Math.min(fileReference.getFilename().length(), FILENAME_MAX_LENGTH) + 2) //
                + getBulletinLogReference().map(BulletinLogReference::estimateLogStringLength).orElse(-1) + 1 //
                + getMessageLogReference().map(MessageLogReference::estimateLogStringLength).orElse(-1) + 1;
    }

    @Override
    public void enterFile(@Nullable final FileReference fileReference) {
        leaveBulletin();
        this.fileReference = fileReference;
        if (fileReference == null) {
            clearLogReferenceCaches();
        }
    }

    private void clearLogReferenceCaches() {
        bulletinLogReferences.clear();
        bulletinMessageLogReferences.clear();
    }

    @Nullable
    @Override
    public Optional<FileReference> getFileReference() {
        return Optional.ofNullable(fileReference);
    }

    @Override
    public void enterBulletin(@Nullable final BulletinLogReference bulletinLogReference) {
        leaveMessage();
        if (bulletinLogReference == null) {
            bulletinIndex = -1;
            return;
        }
        final int newIndex = bulletinLogReference.getBulletinIndex();
        ensureBulletinLogReferencesIndex(newIndex);
        final BulletinLogReference existingReference = bulletinLogReferences.get(newIndex);
        if (!existingReference.equals(bulletinLogReference)) {
            bulletinLogReferences.set(newIndex, bulletinLogReference);
        }
        bulletinIndex = newIndex;
    }

    @Override
    public void enterBulletin(final int index) {
        leaveMessage();
        if (index < 0) {
            bulletinIndex = -1;
            return;
        }
        ensureBulletinLogReferencesIndex(index);
        bulletinIndex = index;
    }

    private void ensureBulletinLogReferencesIndex(final int expectedIndex) {
        ensureIndex(bulletinLogReferences, expectedIndex, i -> BulletinLogReference.builder().setBulletinIndex(i).build());
    }

    @Override
    public Optional<BulletinLogReference> getBulletinLogReference() {
        return bulletinIndex < 0 ? Optional.empty() : Optional.of(bulletinLogReferences.get(bulletinIndex));
    }

    @Override
    public int getBulletinIndex() {
        return bulletinIndex;
    }

    @Override
    public void enterMessage(@Nullable final MessageLogReference messageLogReference) {
        if (messageLogReference == null) {
            messageIndex = -1;
            return;
        }
        if (bulletinIndex < 0) {
            enterBulletin(0);
        }
        final int newIndex = messageLogReference.getMessageIndex();
        ensureMessageLogReferencesIndex(bulletinIndex, newIndex);
        final List<MessageLogReference> messageLogReferences = bulletinMessageLogReferences.get(bulletinIndex);
        final MessageLogReference existingReference = messageLogReferences.get(newIndex);
        if (!existingReference.equals(messageLogReference)) {
            messageLogReferences.set(newIndex, messageLogReference);
        }
        messageIndex = newIndex;
    }

    @Override
    public void enterMessage(final MessageReference messageReference) {
        requireNonNull(messageReference, "messageReference");
        enterBulletin(messageReference.getBulletinIndex());
        final int newIndex = messageReference.getMessageIndex();
        if (newIndex < 0) {
            messageIndex = -1;
            return;
        }
        ensureMessageLogReferencesIndex(messageReference.getBulletinIndex(), newIndex);
        messageIndex = newIndex;
    }

    private void ensureMessageLogReferencesIndex(final int bulletinIndex, final int messageIndex) {
        ensureIndex(bulletinMessageLogReferences, bulletinIndex, i -> new ArrayList<>());
        ensureIndex(bulletinMessageLogReferences.get(bulletinIndex), messageIndex, i -> MessageLogReference.builder().setMessageIndex(i).build());
    }

    @Override
    public Optional<MessageLogReference> getMessageLogReference() {
        return bulletinIndex < 0 || messageIndex < 0 ? Optional.empty() : Optional.of(bulletinMessageLogReferences.get(bulletinIndex).get(messageIndex));
    }

    @Override
    public int getMessageIndex() {
        return messageIndex;
    }

    @Override
    public FileProcessingStatistics getStatistics() {
        return fileProcessingStatistics;
    }
}
