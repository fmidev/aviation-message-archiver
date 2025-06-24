package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.config.model.PostActionFactory;
import fi.fmi.avi.archiver.message.processor.postaction.PostAction;
import fi.fmi.avi.archiver.message.processor.postaction.TestPostAction;
import fi.fmi.avi.archiver.message.processor.postaction.TestPostActionRegistry;
import fi.fmi.avi.archiver.util.instantiation.ReflectionObjectFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static java.util.Objects.requireNonNull;

@Configuration
@Profile("integration-test")
public class PostActionFactoryTestConfig {
    private final PostActionFactoryConfig postActionFactoryConfig;

    PostActionFactoryTestConfig(final PostActionFactoryConfig postActionFactoryConfig) {
        this.postActionFactoryConfig = requireNonNull(postActionFactoryConfig, "postActionFactoryConfig");
    }

    private <T extends PostAction> ReflectionObjectFactory.Builder<T> builder(final Class<T> type) {
        return postActionFactoryConfig.builder(type);
    }

    private <T extends PostAction> PostActionFactory<T> build(final ReflectionObjectFactory.Builder<T> builder) {
        return postActionFactoryConfig.build(builder);
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
}
