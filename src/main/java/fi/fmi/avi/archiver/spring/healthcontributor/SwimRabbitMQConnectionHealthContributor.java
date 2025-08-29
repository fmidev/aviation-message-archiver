package fi.fmi.avi.archiver.spring.healthcontributor;

public class SwimRabbitMQConnectionHealthContributor extends RegistryCompositeHealthContributor {

    public void registerIndicators(final String id,
                                   final RabbitMQConnectionHealthIndicator connectionIndicator,
                                   final RabbitMQPublisherHealthIndicator publisherIndicator) {
        registerContributor(id + ".connection", connectionIndicator);
        registerContributor(id + ".publisher", publisherIndicator);
    }

}
