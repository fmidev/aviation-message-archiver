package fi.fmi.avi.archiver.message.processor.postaction;

import com.google.common.base.CaseFormat;
import fi.fmi.avi.archiver.logging.model.LoggingContext;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.InputAndArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.ImmutableMessageProcessorContext;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public class TestPostAction implements PostAction {
    private final List<Invocation> invocations = Collections.synchronizedList(new ArrayList<>());
    private final String id;

    public TestPostAction(final TestPostActionRegistry registry, final String id) {
        requireNonNull(registry, "registry");
        this.id = requireNonNull(id, "id");
        registry.register(this);
    }

    @Override
    public final void run(final MessageProcessorContext context, final ArchiveAviationMessage message) {
        requireNonNull(context, "context");
        requireNonNull(message, "message");
        invocations.add(new Invocation(context, message));
        doOnRun(context, message);
    }

    /**
     * This method is invoked upon {@link #run(MessageProcessorContext, ArchiveAviationMessage)} after invocation has been registered.
     * Subclasses may override this method to add custom functionality on run.
     *
     * @param context context (see {@link #run(MessageProcessorContext, ArchiveAviationMessage)})
     * @param message message (see {@link #run(MessageProcessorContext, ArchiveAviationMessage)})
     */
    @SuppressWarnings("unused")
    protected void doOnRun(final MessageProcessorContext context, final ArchiveAviationMessage message) {
        requireNonNull(context, "context");
        requireNonNull(message, "message");
    }

    public final void reset() {
        invocations.clear();
        doOnReset();
    }

    /**
     * This method is invoked upon {@link #reset()} after invocations have been cleared.
     * Subclasses may override this method to add custom functionality on reset.
     */
    protected void doOnReset() {
    }

    public final List<Invocation> getInvocations() {
        return List.copyOf(invocations);
    }

    public final String getId() {
        return id;
    }

    public final <ID extends Enum<ID> & EnumId> ID getEnumId(final Class<ID> enumIdClass) {
        requireNonNull(enumIdClass, "enumIdClass");
        return EnumId.fromId(enumIdClass, id);
    }

    public interface EnumId {
        static <ID extends Enum<ID> & EnumId> ID fromId(final Class<ID> enumIdClass, final String id) {
            requireNonNull(enumIdClass, "enumIdClass");
            requireNonNull(id, "id");
            return Enum.valueOf(enumIdClass, CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_UNDERSCORE, id));
        }

        String name();

        default String getId() {
            return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, name());
        }

    }

    public record Invocation(MessageProcessorContext context, ArchiveAviationMessage message) {
        public Invocation {
            requireNonNull(context, "context");
            requireNonNull(message, "message");
        }

        public static Invocation from(final InputAndArchiveAviationMessage message, final LoggingContext loggingContext) {
            requireNonNull(message, "message");
            requireNonNull(loggingContext, "loggingContext");
            return new Invocation(
                    ImmutableMessageProcessorContext.builder()
                            .setInputMessage(message.inputMessage())
                            .setLoggingContext(loggingContext)
                            .build(),
                    message.archiveMessage());
        }

        public static List<Invocation> list(final LoggingContext loggingContext, final InputAndArchiveAviationMessage... messages) {
            requireNonNull(loggingContext, "loggingContext");
            requireNonNull(messages, "messages");
            return stream(loggingContext, Arrays.stream(messages)).toList();
        }

        public static List<Invocation> list(final LoggingContext loggingContext, final Collection<InputAndArchiveAviationMessage> messages) {
            requireNonNull(messages, "messages");
            requireNonNull(loggingContext, "loggingContext");
            return stream(loggingContext, messages.stream()).toList();
        }

        public static Stream<Invocation> stream(final LoggingContext loggingContext, final Stream<InputAndArchiveAviationMessage> messages) {
            requireNonNull(messages, "messages");
            requireNonNull(loggingContext, "loggingContext");
            return messages.map(message -> from(message, loggingContext));
        }
    }
}
