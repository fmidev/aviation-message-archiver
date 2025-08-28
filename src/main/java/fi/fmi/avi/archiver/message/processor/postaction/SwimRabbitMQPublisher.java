package fi.fmi.avi.archiver.message.processor.postaction;

import com.rabbitmq.client.amqp.Message;
import com.rabbitmq.client.amqp.Publisher;
import fi.fmi.avi.archiver.logging.model.ReadableLoggingContext;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.archiver.spring.healthcontributor.RabbitMQPublisherHealthIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import static java.util.Objects.requireNonNull;

public class SwimRabbitMQPublisher implements PostAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwimRabbitMQPublisher.class);

    private final Publisher amqpPublisher;
    private final RabbitMQPublisherHealthIndicator healthIndicator;

    public SwimRabbitMQPublisher(final Publisher amqpPublisher, final RabbitMQPublisherHealthIndicator healthIndicator) {
        this.amqpPublisher = requireNonNull(amqpPublisher, "amqpPublisher");
        this.healthIndicator = requireNonNull(healthIndicator, "healthIndicator");
    }

    @Override
    public void run(final MessageProcessorContext context, final ArchiveAviationMessage message) {
        final ReadableLoggingContext loggingContext = context.getLoggingContext();
        final Message amqpMessage = amqpPublisher
                .message(message.getMessage().getBytes(StandardCharsets.UTF_8))
                .messageId(1L);
        amqpPublisher.publish(amqpMessage, publisherContext -> {
            healthIndicator.update(publisherContext);
            if (publisherContext.status() == Publisher.Status.ACCEPTED) {
                LOGGER.debug("Published message <{}>.", loggingContext);
            } else if (publisherContext.failureCause() != null) {
                final Throwable failureCause = publisherContext.failureCause();
                LOGGER.error("Failed to publish message <{}> with status: <{}>: {}", loggingContext, publisherContext.status(),
                        failureCause.getMessage(), failureCause);
            } else {
                LOGGER.error("Failed to publish message <{}> with status: <{}>.", loggingContext, publisherContext.status());
            }
        });
    }
}
