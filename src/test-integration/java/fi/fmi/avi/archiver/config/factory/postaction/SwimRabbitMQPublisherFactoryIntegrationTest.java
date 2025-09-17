package fi.fmi.avi.archiver.config.factory.postaction;

import fi.fmi.avi.archiver.AviationMessageArchiver;
import fi.fmi.avi.archiver.config.ConversionConfig;
import fi.fmi.avi.archiver.config.PostActionFactoryConfig;
import fi.fmi.avi.archiver.config.TestConfig;
import fi.fmi.avi.archiver.config.model.PostActionFactory;
import fi.fmi.avi.archiver.message.processor.postaction.PostAction;
import fi.fmi.avi.archiver.message.processor.postaction.SwimRabbitMQPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest({
        "auto.startup=false",
        "testclass.name=fi.fmi.avi.archiver.config.factory.postaction.SwimRabbitMQPublisherFactoryIntegrationTest"
})
@ContextConfiguration(classes = {
        AviationMessageArchiver.class,
        TestConfig.class,
        ConversionConfig.class,
        PostActionFactoryConfig.class
}, loader = AnnotationConfigContextLoader.class,
        initializers = {ConfigDataApplicationContextInitializer.class})
@ActiveProfiles({"integration-test", "SwimRabbitMQPublisherFactoryIntegrationTest"})
class SwimRabbitMQPublisherFactoryIntegrationTest {

    @SpyBean(name = "swimRabbitMQPublisherPostActionFactory")
    private PostActionFactory<SwimRabbitMQPublisher> factory;

    @Autowired
    private List<PostAction> postActions;

    @Test
    void test_config_instantiation() {
        @SuppressWarnings("unchecked") final ArgumentCaptor<Map<String, Object>> configCaptor = ArgumentCaptor.forClass(Map.class);
        verify(factory, times(1)).newInstance(configCaptor.capture());

        final Map<String, Object> config = configCaptor.getValue();
        assertThat(config.get("id")).isEqualTo("test-id");

        assertThat(postActions).hasSize(1);
        final PostAction postAction = postActions.getFirst();
        assertThat(postAction).isInstanceOf(SwimRabbitMQPublisher.class);
        assertThat(postAction.toString()).isEqualTo("SwimRabbitMQPublisher(test-id)");
    }
}