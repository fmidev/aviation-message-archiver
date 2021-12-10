package fi.fmi.avi.archiver.message.populator;

import fi.fmi.avi.archiver.config.IntegrationFlowConfig;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.MessageDiscardedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class MessagePopulatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessagePopulatorService.class);

    private final List<MessagePopulator> messagePopulators;

    public MessagePopulatorService(final List<MessagePopulator> messagePopulators) {
        this.messagePopulators = requireNonNull(messagePopulators, "messagePopulators");
    }

    public Message<List<ArchiveAviationMessage>> populateMessages(final List<InputAviationMessage> inputMessages,
                                                                  final MessageHeaders headers) {
        requireNonNull(inputMessages, "inputMessages");
        requireNonNull(headers, "headers");

        final List<ArchiveAviationMessage> populatedMessages = new ArrayList<>();
        final List<InputAviationMessage> failures = new ArrayList<>();
        final List<InputAviationMessage> discards = new ArrayList<>();
        for (final InputAviationMessage inputMessage : inputMessages) {
            try {
                final ArchiveAviationMessage archiveAviationMessage = populateMessage(inputMessage);
                populatedMessages.add(archiveAviationMessage);
            } catch (final MessageDiscardedException e) {
                LOGGER.info("Message was discarded", e); // TODO Logging
                discards.add(inputMessage);
            } catch (final Exception e) {
                LOGGER.error("Populating an archive message failed: {}", inputMessage, e); // TODO Logging
                failures.add(inputMessage);
            }
        }
        return MessageBuilder
                .withPayload(Collections.unmodifiableList(populatedMessages))
                .copyHeaders(headers)
                .setHeader(IntegrationFlowConfig.FAILED_MESSAGES, Collections.unmodifiableList(failures))
                .setHeader(IntegrationFlowConfig.DISCARDED_MESSAGES, Collections.unmodifiableList(discards))
                .build();
    }

    private ArchiveAviationMessage populateMessage(final InputAviationMessage inputAviationMessage) throws MessageDiscardedException {
        final ArchiveAviationMessage.Builder messageBuilder = ArchiveAviationMessage.builder();
        for (final MessagePopulator messagePopulator : messagePopulators) {
            messagePopulator.populate(inputAviationMessage, messageBuilder);
        }
        return messageBuilder.build();
    }

}
