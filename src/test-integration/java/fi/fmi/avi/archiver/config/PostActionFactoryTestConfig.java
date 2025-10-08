package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.config.model.PostActionFactory;
import fi.fmi.avi.archiver.message.processor.postaction.NoOp;
import fi.fmi.avi.archiver.message.processor.postaction.TestPostAction;
import fi.fmi.avi.archiver.message.processor.postaction.TestPostActionRegistry;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("integration-test")
public class PostActionFactoryTestConfig extends AbstractPostActionFactoryConfig {
    PostActionFactoryTestConfig(final ConfigValueConverter configValueConverter) {
        super(configValueConverter);
    }

    @Bean
    TestPostActionRegistry testPostActionRegistry() {
        return new TestPostActionRegistry();
    }

    @Bean
    PostActionFactory<TestPostAction> testPostActionFactory(final TestPostActionRegistry testPostActionRegistry) {
        return build(builder(TestPostAction.class)
                .addDependencyArg(testPostActionRegistry)
                .addConfigArg("id", String.class));
    }

    @Bean
    PostActionFactory<NoOp> noOpPostActionFactory() {
        return build(builder(NoOp.class));
    }
}
