package fi.fmi.avi.archiver.config;

import javax.annotation.PostConstruct;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import fi.fmi.avi.archiver.message.populator.BulletinHeadingSource;
import fi.fmi.avi.archiver.message.populator.conditional.ConditionPropertyReader;
import fi.fmi.avi.archiver.message.populator.conditional.DataDesignatorPropertyReader;

@Configuration
public class MessagePopulatorConditionPropertyReaderConfig {
    private final ConfigurableApplicationContext applicationContext;

    public MessagePopulatorConditionPropertyReaderConfig(final ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    void registerBulletinHeadingConditionPropertyReaders() {
        BulletinHeadingSource.getPermutations()
                .forEach(bulletinHeadingSources -> registerConditionPropertyReader(new DataDesignatorPropertyReader(bulletinHeadingSources)));

    }

    private void registerConditionPropertyReader(final ConditionPropertyReader<?> conditionPropertyReader) {
        final String beanName = conditionPropertyReader.getClass().getSimpleName() + "." + conditionPropertyReader.getPropertyName();
        applicationContext.getBeanFactory().registerSingleton(beanName, conditionPropertyReader);
    }
}
