package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.config.model.PostActionFactory;
import fi.fmi.avi.archiver.config.model.PostActionInstanceSpec;
import fi.fmi.avi.archiver.config.util.MessageProcessorsHelper;
import fi.fmi.avi.archiver.message.processor.postaction.ConditionalPostAction;
import fi.fmi.avi.archiver.message.processor.postaction.PostAction;
import fi.fmi.avi.archiver.message.processor.postaction.PostActionService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

@ConfigurationProperties(prefix = "production-line.post-actions")
public class PostActionConfig {
    @Bean(name = "postActions")
    List<PostAction> postActions(final List<PostActionFactory<? extends PostAction>> postActionFactories,
                                 final List<PostActionInstanceSpec> postActionSpecs,
                                 final MessageProcessorsHelper messageProcessorsHelper) {
        return messageProcessorsHelper.createMessageProcessors(
                        postActionFactories, postActionSpecs,
                        ConditionalPostAction::new)
                .toList();
    }

    @Bean
    PostActionService postActionService(final List<PostAction> postActions) {
        return new PostActionService(postActions);
    }
}
