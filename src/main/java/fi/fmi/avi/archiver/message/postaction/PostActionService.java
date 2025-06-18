package fi.fmi.avi.archiver.message.postaction;

import fi.fmi.avi.archiver.logging.model.LoggingContext;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
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

    public void runPostActions(final List<ArchiveAviationMessage> messages, final LoggingContext loggingContext) {
        requireNonNull(messages, "messages");
        requireNonNull(loggingContext, "loggingContext");
        for (final PostAction postAction : postActions) {
            for (final ArchiveAviationMessage message : messages) {
                loggingContext.enterBulletinMessage(message.getMessagePositionInFile());
                try {
                    postAction.run(loggingContext, message);
                } catch (final RuntimeException e) {
                    LOGGER.error("Post-action failed on message <{}>.", loggingContext, e);
                }
            }
            loggingContext.leaveBulletin();
        }
    }
}
