package fi.fmi.avi.archiver.amqp;

import com.rabbitmq.client.amqp.Message;
import com.rabbitmq.client.amqp.Publisher;
import fi.fmi.avi.archiver.logging.model.LoggingContext;
import fi.fmi.avi.archiver.message.InputAndArchiveAviationMessage;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class AmqpService {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AmqpService.class);

    private final Publisher amqpPublisher;

    public AmqpService(final Publisher amqpPublisher) {
        this.amqpPublisher = requireNonNull(amqpPublisher, "amqpPublisher");
    }

    public List<InputAndArchiveAviationMessage> publishMessages(final List<InputAndArchiveAviationMessage> messages, final LoggingContext loggingContext) {
        requireNonNull(messages, "messages");
        requireNonNull(loggingContext, "loggingContext");

        RuntimeException amqpException = null;
        for (final InputAndArchiveAviationMessage inputAndArchiveMessage : messages) {
            try {
                loggingContext.enterBulletinMessage(inputAndArchiveMessage.inputMessage().getMessagePositionInFile());
                final Message amqpMessage = amqpPublisher
                        .message(inputAndArchiveMessage.archiveMessage().getMessage().getBytes(StandardCharsets.UTF_8))
                        .messageId(1L);
                amqpPublisher.publish(amqpMessage, context -> {
                    if (context.status() == Publisher.Status.ACCEPTED) {
                        LOGGER.debug("Published message <{}>.", loggingContext);
                    } else {
                        // Log the publishing failure in context?
                    }
                });
            } catch (final RuntimeException e) {
                amqpException = e;
                //loggingContext.recordProcessingResult(FileProcessingStatistics.ProcessingResult.FAILED);
            } finally {
                loggingContext.leaveMessage();
            }
        }
        loggingContext.leaveBulletin();
        if (amqpException != null) {
            throw amqpException;
        }
        return messages;
    }

}
