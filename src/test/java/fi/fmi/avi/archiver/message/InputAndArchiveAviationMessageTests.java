package fi.fmi.avi.archiver.message;

import fi.fmi.avi.archiver.file.InputAviationMessage;

import java.util.*;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public final class InputAndArchiveAviationMessageTests {
    private InputAndArchiveAviationMessageTests() {
        throw new AssertionError();
    }

    public static List<InputAndArchiveAviationMessage> createList(final Collection<InputAviationMessage> inputMessages, final ArchiveAviationMessage... archiveMessages) {
        requireNonNull(inputMessages, "inputMessages");
        requireNonNull(archiveMessages, "archiveMessages");
        return createList(inputMessages, Arrays.asList(archiveMessages));
    }

    public static List<InputAndArchiveAviationMessage> createList(final Collection<InputAviationMessage> inputMessages, final Collection<ArchiveAviationMessage> archiveMessages) {
        requireNonNull(inputMessages, "inputMessages");
        requireNonNull(archiveMessages, "archiveMessages");
        final Iterator<InputAviationMessage> inputIterator = inputMessages.iterator();
        final Iterator<ArchiveAviationMessage> archiveIterator = archiveMessages.iterator();
        final List<InputAndArchiveAviationMessage> builder = new ArrayList<>(inputMessages.size());
        while (inputIterator.hasNext() && archiveIterator.hasNext()) {
            builder.add(new InputAndArchiveAviationMessage(inputIterator.next(), archiveIterator.next()));
        }
        if (inputIterator.hasNext() || archiveIterator.hasNext()) {
            throw new IllegalArgumentException(String.format(Locale.US, "Sizes differ: inputMessages (%d), archiveMessages (%d) ", inputMessages.size(), archiveMessages.size()));
        }
        return List.copyOf(builder);
    }

    public static Predicate<InputAndArchiveAviationMessage> hasProductIdIn(final String... expectedProductIds) {
        requireNonNull(expectedProductIds, "expectedProductIds");
        return hasProductIdIn(new HashSet<>(Arrays.asList(expectedProductIds)));
    }

    public static Predicate<InputAndArchiveAviationMessage> hasProductIdIn(final Collection<String> expectedProductIds) {
        requireNonNull(expectedProductIds, "expectedProductIds");
        return message -> hasProductIdIn(message, expectedProductIds);
    }

    public static boolean hasProductIdIn(final InputAndArchiveAviationMessage message, final Collection<String> expectedProductIds) {
        requireNonNull(message, "message");
        requireNonNull(expectedProductIds, "expectedProductIds");
        return expectedProductIds.contains(message.inputMessage().getFileMetadata().getFileReference().getProductId());
    }
}
