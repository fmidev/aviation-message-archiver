package fi.fmi.avi.archiver.message.postaction;

import fi.fmi.avi.archiver.logging.model.LoggingContext;
import fi.fmi.avi.archiver.message.ImmutableMessageProcessorContext;
import fi.fmi.avi.archiver.message.InputAndArchiveAviationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class PostActionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostActionService.class);

    private final List<PostAction> postActions;

    public PostActionService(final List<PostAction> postActions) {
        this.postActions = requireNonNull(postActions, "postActions");
    }

    public void runPostActions(final List<InputAndArchiveAviationMessage> messages, final LoggingContext loggingContext) {
        requireNonNull(messages, "messages");
        requireNonNull(loggingContext, "loggingContext");
        final ImmutableMessageProcessorContext.Builder contextBuilder = ImmutableMessageProcessorContext.builder()
                .setLoggingContext(loggingContext);
        for (final PostAction postAction : postActions) {
            for (final InputAndArchiveAviationMessage inputAndArchiveMessage : messages) {
                loggingContext.enterBulletinMessage(inputAndArchiveMessage.inputMessage().getMessagePositionInFile());
                try {
                    contextBuilder.setInputMessage(inputAndArchiveMessage.inputMessage());
                    postAction.run(contextBuilder.build(), inputAndArchiveMessage.archiveMessage());
                } catch (final RuntimeException e) {
                    LOGGER.error("Post-action failed on message <{}>.", loggingContext, e);
                }
            }
            loggingContext.leaveBulletin();
        }
    }
}
