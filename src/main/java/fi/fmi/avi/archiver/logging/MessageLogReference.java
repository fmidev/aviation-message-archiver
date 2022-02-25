package fi.fmi.avi.archiver.logging;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.inferred.freebuilder.FreeBuilder;

import com.google.common.base.Preconditions;

@FreeBuilder
public abstract class MessageLogReference extends AbstractMemoizingStructuredLoggable {
    private static final Pattern XML_EXCERPT_PATTERN = Pattern.compile("^\\s*<[^>]+[:\\s]id\\s*=\\s*\"([^\"]+)\"");
    private static final int MESSAGE_INDEX_LENGTH_ESTIMATE = 4;
    private static final int EXCERPT_MAX_LENGTH = 42;

    public static Builder builder() {
        return new Builder();
    }

    public abstract int getMessageIndex();

    public abstract Optional<String> getMessageContent();

    public Optional<String> getMessageExcerpt() {
        return getMessageContent()//
                .map(content -> {
                    final Matcher matcher = XML_EXCERPT_PATTERN.matcher(content);
                    if (matcher.find()) {
                        return LoggableUtils.sanitize(content, EXCERPT_MAX_LENGTH, matcher.start(1), matcher.end(1));
                    } else {
                        return LoggableUtils.sanitize(content, EXCERPT_MAX_LENGTH);
                    }
                });
    }

    @Override
    protected void appendOnceTo(final StringBuilder builder) {
        builder.append(getMessageIndex() + 1);
        getMessageExcerpt().ifPresent(messageExcerpt -> builder.append('(').append(messageExcerpt).append(')'));
    }

    @Override
    public int estimateLogStringLength() {
        return MESSAGE_INDEX_LENGTH_ESTIMATE + EXCERPT_MAX_LENGTH + 2;
    }

    public abstract Builder toBuilder();

    public static class Builder extends MessageLogReference_Builder {
        Builder() {
        }

        @Override
        public Builder setMessageIndex(final int messageIndex) {
            Preconditions.checkArgument(messageIndex >= 0, "messageIndex must be non-negative; was: %s", messageIndex);
            return super.setMessageIndex(messageIndex);
        }
    }
}
