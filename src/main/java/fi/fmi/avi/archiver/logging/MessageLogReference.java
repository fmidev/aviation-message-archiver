package fi.fmi.avi.archiver.logging;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.inferred.freebuilder.FreeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;

@FreeBuilder
public abstract class MessageLogReference extends AbstractMemoizingStructuredLoggable {
    private static final Pattern XML_EXCERPT_PATTERN = Pattern.compile("^\\s*<[^>]+[:\\s]id\\s*=\\s*\"([^\"]+)\"");
    private static final int MESSAGE_INDEX_LENGTH_ESTIMATE = 4;
    private static final int EXCERPT_MAX_LENGTH = 42;

    public static Builder builder() {
        return new Builder();
    }

    public abstract int getIndex();

    @JsonIgnore
    public abstract Optional<String> getContent();

    public Optional<String> getExcerpt() {
        return getContent()//
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
        builder.append(getIndex() + 1);
        getExcerpt().ifPresent(excerpt -> builder.append('(').append(excerpt).append(')'));
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
        public Builder setIndex(final int index) {
            Preconditions.checkArgument(index >= 0, "index must be non-negative; was: %s", index);
            return super.setIndex(index);
        }
    }
}
