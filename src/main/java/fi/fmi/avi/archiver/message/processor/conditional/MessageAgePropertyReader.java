package fi.fmi.avi.archiver.message.processor.conditional;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;

import java.time.Clock;
import java.time.Duration;

import static java.util.Objects.requireNonNull;

public class MessageAgePropertyReader extends AbstractConditionPropertyReader<Duration> {

    private final Clock clock;

    public MessageAgePropertyReader(final Clock clock) {
        this.clock = requireNonNull(clock, "clock");
    }

    @Override
    public Duration readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder message) {
        requireNonNull(input, "input");
        requireNonNull(message, "message");
        return Duration.between(message.getMessageTime(), clock.instant());
    }
}
