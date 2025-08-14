package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.ProcessingServiceContext;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.logging.model.FileProcessingStatistics;
import fi.fmi.avi.archiver.logging.model.LoggingContext;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.InputAndArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;
import fi.fmi.avi.archiver.message.processor.ImmutableMessageProcessorContext;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class MessagePopulationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessagePopulationService.class);

    private final List<MessagePopulator> messagePopulators;

    public MessagePopulationService(final List<MessagePopulator> messagePopulators) {
        this.messagePopulators = requireNonNull(messagePopulators, "messagePopulators");
    }

    public List<InputAndArchiveAviationMessage> populateMessages(final List<InputAviationMessage> inputMessages, final ProcessingServiceContext context) {
        requireNonNull(inputMessages, "inputMessages");
        requireNonNull(context, "context");

        final LoggingContext loggingContext = context.getLoggingContext();
        final ArrayList<InputAndArchiveAviationMessage> resultBuilder = new ArrayList<>(inputMessages.size());
        final ImmutableMessageProcessorContext.Builder messageContextBuilder = ImmutableMessageProcessorContext.builder()//
                .setLoggingContext(loggingContext);
        for (final InputAviationMessage inputMessage : inputMessages) {
            loggingContext.enterBulletinMessage(inputMessage.getMessagePositionInFile());
            try {
                final ArchiveAviationMessage archiveAviationMessage = populateMessage(messageContextBuilder.setInputMessage(inputMessage).build());
                resultBuilder.add(new InputAndArchiveAviationMessage(inputMessage, archiveAviationMessage));
            } catch (final MessageDiscardedException e) {
                LOGGER.info("Discarded message <{}>: {}", loggingContext, e.getMessage());
                loggingContext.recordProcessingResult(FileProcessingStatistics.ProcessingResult.DISCARDED);
            } catch (final Exception e) {
                context.signalProcessingErrors();
                LOGGER.error("Failed to populate message <{}>.", loggingContext, e);
                loggingContext.recordProcessingResult(FileProcessingStatistics.ProcessingResult.FAILED);
            }
        }
        loggingContext.leaveBulletin();
        return List.copyOf(resultBuilder);
    }

    private ArchiveAviationMessage populateMessage(final MessageProcessorContext context) throws MessageDiscardedException {
        final ArchiveAviationMessage.Builder messageBuilder = ArchiveAviationMessage.builder();
        for (final MessagePopulator messagePopulator : messagePopulators) {
            messagePopulator.populate(context, messageBuilder);
        }
        return messageBuilder.build();
    }
}
