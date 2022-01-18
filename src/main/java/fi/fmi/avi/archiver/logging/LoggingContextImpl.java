package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;

import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.archiver.file.FileProcessingIdentifier;
import fi.fmi.avi.archiver.file.FileReference;

// TODO: Cleanup
// TODO: More debug / trace logging?
public class LoggingContextImpl extends AbstractAppendingLoggable implements LoggingContext {
    private static final char SEPARATOR = ':';
    /**
     * Maximum file name length is set according to maximum length of mandatory file name files specified by
     * WMO doc 386 Manual on the Global Telecommunication System, General file naming conventions
     */
    private static final int FILENAME_MAX_LENGTH = 128;

    private final FileProcessingIdentifier fileProcessingIdentifier;
    private final FileProcessingStatistics fileProcessingStatistics;
    private final ArrayList<BulletinLogReference> bulletinLogReferences = new ArrayList<>(0);
    private final ArrayList<ArrayList<MessageLogReference>> bulletinMessageLogReferences = new ArrayList<>(0);

    @Nullable
    private FileReference fileReference;

    private int bulletinIndex = -1;
    private int messageIndex = -1;

    public LoggingContextImpl(final FileProcessingIdentifier fileProcessingIdentifier, final FileProcessingStatistics fileProcessingStatistics) {
        this.fileProcessingIdentifier = requireNonNull(fileProcessingIdentifier, "fileProcessingIdentifier");
        this.fileProcessingStatistics = requireNonNull(fileProcessingStatistics, "fileProcessingStatistics");
    }

    private static <E> void ensureSizeAtLeast(final ArrayList<E> list, final int minSize, final IntFunction<E> defaultElement) {
        list.ensureCapacity(minSize);
        for (int nextIndex = list.size(); nextIndex < minSize; nextIndex++) {
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
        }
        if (bulletinIndex >= 0) {
            if (fileReference == null) {
                builder.append(SEPARATOR);
            }
            builder.append(SEPARATOR)//
                    .append(bulletinLogReferences.get(bulletinIndex));
            if (messageIndex >= 0) {
                builder.append(SEPARATOR)//
                        .append(bulletinMessageLogReferences.get(bulletinIndex).get(messageIndex));
            }
        }
    }

    @Override
    public int estimateLogStringLength() {
        return fileProcessingIdentifier.toString().length() //
                + estimateFileReferenceLength() //
                + estimateBulletinLogReferenceLength() //
                + estimateMessageLogReferenceLength();
    }

    private int estimateFileReferenceLength() {
        return fileReference == null
                ? 0
                : fileReference.getProductIdentifier().length() + Math.min(fileReference.getFilename().length(), FILENAME_MAX_LENGTH) + 2;
    }

    private int estimateBulletinLogReferenceLength() {
        return bulletinIndex < 0 ? 0 : bulletinLogReferences.get(bulletinIndex).estimateLogStringLength() + 1;
    }

    private int estimateMessageLogReferenceLength() {
        return bulletinIndex < 0 || messageIndex < 0 ? 0 : bulletinMessageLogReferences.get(bulletinIndex).get(messageIndex).estimateLogStringLength() + 1;
    }

    @Override
    public void enterFile(@Nullable final FileReference fileReference) {
        leaveBulletin();
        if (fileReference == null || !fileReference.equals(this.fileReference)) {
            clearLogReferenceCachesAndStatistics();
        }
        this.fileReference = fileReference;
    }

    private void clearLogReferenceCachesAndStatistics() {
        bulletinLogReferences.clear();
        bulletinLogReferences.trimToSize();
        bulletinMessageLogReferences.clear();
        bulletinMessageLogReferences.trimToSize();
        fileProcessingStatistics.clear();
    }

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
        final int index = bulletinLogReference.getBulletinIndex();
        ensureBulletinLogReferencesHasIndex(index);
        bulletinLogReferences.set(index, bulletinLogReference);
        bulletinIndex = index;
    }

    @Override
    public void enterBulletin(final int index) {
        leaveMessage();
        if (index < 0) {
            bulletinIndex = -1;
            return;
        }
        ensureBulletinLogReferencesHasIndex(index);
        bulletinIndex = index;
    }

    private void ensureBulletinLogReferencesHasIndex(final int expectedIndex) {
        ensureSizeAtLeast(bulletinLogReferences, expectedIndex + 1, i -> BulletinLogReference.builder().setBulletinIndex(i).build());
    }

    @Override
    public Optional<BulletinLogReference> getBulletinLogReference() {
        return bulletinIndex < 0 ? Optional.empty() : Optional.of(bulletinLogReferences.get(bulletinIndex));
    }

    @Override
    public List<BulletinLogReference> getAllBulletinLogReferences() {
        return Collections.unmodifiableList(bulletinLogReferences);
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
        final int index = messageLogReference.getMessageIndex();
        ensureMessageLogReferencesHasIndex(bulletinIndex, index);
        final List<MessageLogReference> messageLogReferences = bulletinMessageLogReferences.get(bulletinIndex);
        messageLogReferences.set(index, messageLogReference);
        messageIndex = index;
    }

    @Override
    public void enterMessage(final int index) {
        if (index < 0) {
            messageIndex = -1;
            return;
        }
        if (bulletinIndex < 0) {
            enterBulletin(0);
        }
        ensureMessageLogReferencesHasIndex(bulletinIndex, index);
        messageIndex = index;
    }

    private void ensureMessageLogReferencesHasIndex(final int bulletinIndex, final int messageIndex) {
        ensureSizeAtLeast(bulletinMessageLogReferences, bulletinIndex + 1, i -> new ArrayList<>(0));
        ensureSizeAtLeast(bulletinMessageLogReferences.get(bulletinIndex), messageIndex + 1, i -> MessageLogReference.builder().setMessageIndex(i).build());
    }

    @Override
    public Optional<MessageLogReference> getMessageLogReference() {
        return bulletinIndex < 0 || messageIndex < 0 ? Optional.empty() : Optional.of(bulletinMessageLogReferences.get(bulletinIndex).get(messageIndex));
    }

    @Override
    public List<MessageLogReference> getBulletinMessageLogReferences() {
        return bulletinIndex < 0 || bulletinIndex >= bulletinMessageLogReferences.size()
                ? Collections.emptyList()
                : Collections.unmodifiableList(bulletinMessageLogReferences.get(bulletinIndex));
    }

    @Override
    public int getMessageIndex() {
        return messageIndex;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    @Override
    public FileProcessingStatistics getStatistics() {
        return fileProcessingStatistics;
    }

    @Override
    public void initStatistics() {
        fileProcessingStatistics.initBulletins(bulletinLogReferences.size());
        for (int bulletinIndex = 0, size = bulletinMessageLogReferences.size(); bulletinIndex < size; bulletinIndex++) {
            fileProcessingStatistics.initMessages(bulletinIndex, bulletinMessageLogReferences.get(bulletinIndex).size());
        }
    }
}
