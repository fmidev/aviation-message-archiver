package fi.fmi.avi.archiver.logging.model;

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

public class LoggingContextImpl extends AbstractLoggingContext implements LoggingContext {

    private final FileProcessingIdentifier processingId;
    private final FileProcessingStatistics fileProcessingStatistics;
    private final ArrayList<BulletinLogReference> bulletins = new ArrayList<>(0);
    private final ArrayList<ArrayList<MessageLogReference>> bulletinMessages = new ArrayList<>(0);

    @Nullable
    private FileReference file;
    private int bulletinIndex = -1;
    private int messageIndex = -1;

    public LoggingContextImpl(final FileProcessingIdentifier processingId, final FileProcessingStatistics fileProcessingStatistics) {
        this.processingId = requireNonNull(processingId, "processingId");
        this.fileProcessingStatistics = requireNonNull(fileProcessingStatistics, "fileProcessingStatistics");
    }

    private static <E> void ensureSizeAtLeast(final ArrayList<E> list, final int minSize, final IntFunction<E> defaultElement) {
        list.ensureCapacity(minSize);
        for (int nextIndex = list.size(); nextIndex < minSize; nextIndex++) {
            list.add(defaultElement.apply(nextIndex));
        }
    }

    @Override
    public FileProcessingIdentifier getProcessingId() {
        return processingId;
    }

    @Override
    public void enterFile(@Nullable final FileReference file) {
        leaveBulletin();
        if (file == null || !file.equals(this.file)) {
            clearLogReferenceCachesAndStatistics();
        }
        this.file = file;
    }

    private void clearLogReferenceCachesAndStatistics() {
        bulletins.clear();
        bulletins.trimToSize();
        bulletinMessages.clear();
        bulletinMessages.trimToSize();
        fileProcessingStatistics.clear();
    }

    @Override
    public Optional<FileReference> getFile() {
        return Optional.ofNullable(file);
    }

    @Override
    public void enterBulletin(@Nullable final BulletinLogReference bulletin) {
        leaveMessage();
        if (bulletin == null) {
            bulletinIndex = -1;
            return;
        }
        final int index = bulletin.getIndex();
        ensureBulletinLogReferencesHasIndex(index);
        bulletins.set(index, bulletin);
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
        ensureSizeAtLeast(bulletins, expectedIndex + 1, i -> BulletinLogReference.builder().setIndex(i).build());
    }

    @Override
    public Optional<BulletinLogReference> getBulletin() {
        return bulletinIndex < 0 ? Optional.empty() : Optional.of(bulletins.get(bulletinIndex));
    }

    @Override
    public List<BulletinLogReference> getAllBulletins() {
        return Collections.unmodifiableList(bulletins);
    }

    @Override
    public int getBulletinIndex() {
        return bulletinIndex;
    }

    @Override
    public void enterMessage(@Nullable final MessageLogReference message) {
        if (message == null) {
            messageIndex = -1;
            return;
        }
        if (bulletinIndex < 0) {
            enterBulletin(0);
        }
        final int index = message.getIndex();
        ensureMessageLogReferencesHasIndex(bulletinIndex, index);
        final List<MessageLogReference> messages = bulletinMessages.get(bulletinIndex);
        messages.set(index, message);
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
        ensureSizeAtLeast(bulletinMessages, bulletinIndex + 1, i -> new ArrayList<>(0));
        ensureSizeAtLeast(bulletinMessages.get(bulletinIndex), messageIndex + 1, i -> MessageLogReference.builder().setIndex(i).build());
    }

    @Override
    public Optional<MessageLogReference> getMessage() {
        return bulletinIndex < 0 || messageIndex < 0 ? Optional.empty() : Optional.of(bulletinMessages.get(bulletinIndex).get(messageIndex));
    }

    @Override
    public List<MessageLogReference> getBulletinMessages() {
        return bulletinIndex < 0 || bulletinIndex >= bulletinMessages.size()
                ? Collections.emptyList()
                : Collections.unmodifiableList(bulletinMessages.get(bulletinIndex));
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
        fileProcessingStatistics.initBulletins(bulletins.size());
        for (int bulletinIndex = 0, size = bulletinMessages.size(); bulletinIndex < size; bulletinIndex++) {
            fileProcessingStatistics.initMessages(bulletinIndex, bulletinMessages.get(bulletinIndex).size());
        }
    }
}
