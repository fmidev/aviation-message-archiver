package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.config.model.PostActionFactory;
import fi.fmi.avi.archiver.config.model.PostActionInstanceSpec;
import fi.fmi.avi.archiver.config.util.ConfigurableComponentsUtil;
import fi.fmi.avi.archiver.message.populator.conditional.ConditionalPostAction;
import fi.fmi.avi.archiver.message.postaction.PostAction;
import fi.fmi.avi.archiver.message.postaction.PostActionService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

@ConfigurationProperties(prefix = "production-line.post-actions")
public class PostActionConfig {
    @Bean(name = "postActions")
    List<PostAction> postActions(final List<PostActionFactory<? extends PostAction>> postActionFactories,
                                 final List<PostActionInstanceSpec> postActionSpecs,
                                 final ConfigurableComponentsUtil configurableComponentsUtil) {
        return configurableComponentsUtil.createConditionalComponents(
                        postActionFactories, postActionSpecs,
                        ConditionalPostAction::new)
                .toList();
    }

    @Bean
    PostActionService postActionService(final List<PostAction> postActions) {
        return new PostActionService(postActions);
    }
}
