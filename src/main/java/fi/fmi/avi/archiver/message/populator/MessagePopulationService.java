package fi.fmi.avi.archiver.message.populator;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.inferred.freebuilder.FreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.logging.FileProcessingStatistics;
import fi.fmi.avi.archiver.logging.LoggingContext;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;

public class MessagePopulationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessagePopulationService.class);

    private final List<MessagePopulator> messagePopulators;

    public MessagePopulationService(final List<MessagePopulator> messagePopulators) {
        this.messagePopulators = requireNonNull(messagePopulators, "messagePopulators");
    }

    public List<PopulationResult> populateMessages(final List<InputAviationMessage> inputMessages, final LoggingContext loggingContext) {
        requireNonNull(inputMessages, "inputMessages");
        requireNonNull(loggingContext, "loggingContext");

        final List<PopulationResult> populationResults = new ArrayList<>();
        for (final InputAviationMessage inputMessage : inputMessages) {
            loggingContext.enterMessage(inputMessage.getMessageReference());
            final PopulationResult.Builder builder = PopulationResult.builder()//
                    .setInputMessage(inputMessage);
            try {
                final ArchiveAviationMessage archiveAviationMessage = populateMessage(inputMessage);
                builder.setArchiveMessage(archiveAviationMessage)//
                        .setStatus(PopulationResult.Status.STORE);
            } catch (final MessageDiscardedException e) {
                builder.setStatus(PopulationResult.Status.DISCARD);
                LOGGER.info("Discarded message <{}>", loggingContext, e);
                loggingContext.recordStatus(FileProcessingStatistics.Status.DISCARDED);
            } catch (final Exception e) {
                builder.setStatus(PopulationResult.Status.FAIL);
                LOGGER.error("Failed to populate message <{}>.", loggingContext, e);
                loggingContext.recordStatus(FileProcessingStatistics.Status.FAILED);
            }
            populationResults.add(builder.build());
        }
        loggingContext.leaveBulletin();
        return populationResults;
    }

    private ArchiveAviationMessage populateMessage(final InputAviationMessage inputAviationMessage) throws MessageDiscardedException {
        final ArchiveAviationMessage.Builder messageBuilder = ArchiveAviationMessage.builder()//
                .setMessageReference(inputAviationMessage.getMessageReference());
        for (final MessagePopulator messagePopulator : messagePopulators) {
            messagePopulator.populate(inputAviationMessage, messageBuilder);
        }
        return messageBuilder.build();
    }

    @FreeBuilder
    public static abstract class PopulationResult {
        public static Builder builder() {
            return new Builder();
        }

        public abstract InputAviationMessage getInputMessage();

        public abstract Optional<ArchiveAviationMessage> getArchiveMessage();

        public abstract Status getStatus();

        public abstract Builder toBuilder();

        public enum Status {
            STORE, DISCARD, FAIL
        }

        public static class Builder extends MessagePopulationService_PopulationResult_Builder {
            Builder() {
            }

            @Override
            public PopulationResult build() {
                return super.build();
            }
        }
    }
}
