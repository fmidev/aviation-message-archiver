package fi.fmi.avi.archiver.config;

import fi.fmi.avi.archiver.config.model.PostActionFactory;
import fi.fmi.avi.archiver.message.processor.postaction.PostAction;
import fi.fmi.avi.archiver.message.processor.postaction.ResultLogger;
import fi.fmi.avi.archiver.util.StringCaseFormat;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
import fi.fmi.avi.archiver.util.instantiation.PropertyRenamingObjectFactory;
import fi.fmi.avi.archiver.util.instantiation.ReflectionObjectFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Objects.requireNonNull;

@Configuration
public class PostActionFactoryConfig {
    private final ConfigValueConverter configValueConverter;

    PostActionFactoryConfig(final ConfigValueConverter configValueConverter) {
        this.configValueConverter = requireNonNull(configValueConverter, "configValueConverter");
    }

    <T extends PostAction> ReflectionObjectFactory.Builder<T> builder(final Class<T> type) {
        return ReflectionObjectFactory.builder(type, configValueConverter);
    }

    <T extends PostAction> PostActionFactory<T> build(final ReflectionObjectFactory.Builder<T> builder) {
        return new PostActionFactory<>(new PropertyRenamingObjectFactory<>(builder.build(), StringCaseFormat::dashToCamel));
    }

    @Bean
    PostActionFactory<ResultLogger> resultLoggerPostActionFactory() {
        return build(builder(ResultLogger.class)
                .addConfigArg("message", String.class));
    }
}
