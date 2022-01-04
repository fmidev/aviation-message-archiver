package fi.fmi.avi.archiver.logging;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public interface FileProcessingStatistics extends AppendingLoggable {
    void recordMessageStatus(int bulletinIndex, int messageIndex, Status status);

    void recordBulletinStatus(int bulletinIndex, Status status);

    void recordFileStatus(Status status);

    enum Status {
        NOTHING("N"), ARCHIVED("A"), DISCARDED("D"), REJECTED("R"), FAILED("F");

        private static final List<Status> VALUES = Collections.unmodifiableList(Arrays.asList(Status.values()));
        private final String abbreviatedName;

        Status(final String abbreviatedName) {
            this.abbreviatedName = requireNonNull(abbreviatedName, "abbreviatedName");
        }

        public static Comparator<Status> getComparator() {
            return Comparator.naturalOrder();
        }

        @SuppressFBWarnings(value = "MS_EXPOSE_REP", justification = "immutable value")
        public static List<Status> getValues() {
            return VALUES;
        }

        String getAbbreviatedName() {
            return abbreviatedName;
        }
    }
}
