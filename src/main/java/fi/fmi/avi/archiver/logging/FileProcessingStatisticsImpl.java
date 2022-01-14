package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;

public final class FileProcessingStatisticsImpl extends AbstractAppendingLoggable implements FileProcessingStatistics {
    private static final int FILE_STATISTICS_LENGTH_ESTIMATE = 4;
    private static final int STATISTICS_CATEGORIES_COUNT = 2; // message, bulletin
    private static final int STRING_LENGTH_ESTIMATE = (Statistics.STRING_LENGTH_ESTIMATE + 4) * STATISTICS_CATEGORIES_COUNT + FILE_STATISTICS_LENGTH_ESTIMATE;

    private final ArrayList<ArrayList<Status>> bulletinMessageStatuses = new ArrayList<>(0);
    private final ArrayList<Status> bulletinStatuses = new ArrayList<>(0);

    private Status fileStatus = INITIAL_STATUS;

    private static void ensureStatusesSizeAtLeast(final ArrayList<Status> statuses, final int minSize) {
        ensureSizeAtLeast(statuses, minSize, i -> INITIAL_STATUS);
    }

    private static <E> void ensureSizeAtLeast(final ArrayList<E> list, final int minSize, final IntFunction<E> defaultElement) {
        list.ensureCapacity(minSize);
        for (int nextIndex = list.size(); nextIndex < minSize; nextIndex++) {
            list.add(defaultElement.apply(nextIndex));
        }
    }

    private static <E> Optional<E> getElement(final List<E> list, final int index) {
        return list.size() > index ? Optional.of(list.get(index)) : Optional.empty();
    }

    private static Status max(final Status status1, final Status status2) {
        return Status.getComparator().compare(status1, status2) >= 0 ? status1 : status2;
    }

    @Override
    public void clear() {
        bulletinMessageStatuses.clear();
        bulletinMessageStatuses.trimToSize();
        bulletinStatuses.clear();
        bulletinStatuses.trimToSize();
        fileStatus = INITIAL_STATUS;
    }

    @Override
    public void recordMessageStatus(final int bulletinIndex, final int messageIndex, final Status status) {
        requireNonNull(status, "status");
        recordStatus(getBulletinMessageStatuses(bulletinIndex), messageIndex, status);
    }

    private ArrayList<Status> getBulletinMessageStatuses(final int bulletinIndex) {
        ensureSizeAtLeast(bulletinMessageStatuses, bulletinIndex + 1, i -> new ArrayList<>(0));
        return bulletinMessageStatuses.get(bulletinIndex);
    }

    private void recordStatus(final ArrayList<Status> statuses, final int index, final Status status) {
        ensureStatusesSizeAtLeast(statuses, index + 1);
        final Status oldStatus = statuses.get(index);
        statuses.set(index, max(oldStatus, status));
    }

    @Override
    public void recordBulletinStatus(final int bulletinIndex, final Status status) {
        requireNonNull(status, "status");
        recordStatus(bulletinStatuses, bulletinIndex, status);
    }

    private Status getFileStatus() {
        return Stream.of(//
                        bulletinMessageStatuses.stream()//
                                .flatMap(Collection::stream), //
                        bulletinStatuses.stream(), //
                        Stream.of(this.fileStatus))//
                .flatMap(Function.identity())//
                .max(Status.getComparator())//
                .orElse(INITIAL_STATUS);
    }

    @Override
    public void recordFileStatus(final Status status) {
        requireNonNull(status, "status");
        fileStatus = max(fileStatus, status);
    }

    private Statistics getBulletinStatistics() {
        final Statistics bulletinStatistics = new Statistics();
        for (int i = 0, size = Math.max(bulletinMessageStatuses.size(), bulletinStatuses.size()); i < size; i++) {
            bulletinStatistics.add(getBulletinStatus(i));
        }
        return bulletinStatistics;
    }

    private Status getBulletinStatus(final int index) {
        final Status bulletinMessageStatus = getElement(bulletinMessageStatuses, index)//
                .flatMap(statuses -> statuses.stream()//
                        .max(Status.getComparator()))//
                .orElse(INITIAL_STATUS);
        final Status bulletinStatus = getElement(bulletinStatuses, index)//
                .orElse(INITIAL_STATUS);
        return max(bulletinMessageStatus, bulletinStatus);
    }

    private Statistics getMessageStatistics() {
        final Statistics messageStatistics = new Statistics();
        bulletinMessageStatuses.forEach(messageStatuses -> messageStatuses.forEach(messageStatistics::add));
        return messageStatistics;
    }

    @Override
    public void appendTo(final StringBuilder builder) {
        builder.append("M{");
        getMessageStatistics().appendTo(builder);
        builder.append("} B{");
        getBulletinStatistics().appendTo(builder);
        builder.append("} F{")//
                .append(getFileStatus().getAbbreviatedName())//
                .append("}");
    }

    @Override
    public int estimateLogStringLength() {
        return STRING_LENGTH_ESTIMATE;
    }

    private static final class Statistics extends AbstractAppendingLoggable {
        private static final int STRING_LENGTH_PER_STATUS_ESTIMATE = 3 /* control chars */ + 4 /* number */;
        static final int STRING_LENGTH_ESTIMATE = (Status.getValues().size() + 1) * STRING_LENGTH_PER_STATUS_ESTIMATE;

        private final int[] statistics = new int[Status.getValues().size()];

        private Statistics() {
        }

        public void add(final Status status) {
            statistics[status.ordinal()]++;
        }

        @Override
        public void appendTo(final StringBuilder builder) {
            int total = 0;
            for (final Status status : Status.getValues()) {
                final int amount = statistics[status.ordinal()];
                total += amount;
                if (amount > 0) {
                    builder.append(status.getAbbreviatedName())//
                            .append(':')//
                            .append(amount)//
                            .append(',');
                }
            }
            builder//
                    .append("T:")//
                    .append(total);
        }

        @Override
        public int estimateLogStringLength() {
            return STRING_LENGTH_ESTIMATE;
        }
    }
}
