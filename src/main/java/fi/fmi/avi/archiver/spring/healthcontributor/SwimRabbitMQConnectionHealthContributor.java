package fi.fmi.avi.archiver.spring.healthcontributor;

import static java.util.Objects.requireNonNull;

public class SwimRabbitMQConnectionHealthContributor extends RegistryCompositeHealthContributor {

    public void registerIndicators(final String id,
                                   final RabbitMQConnectionHealthIndicator connectionIndicator,
                                   final RabbitMQPublisherHealthIndicator publisherIndicator) {
        requireNonNull(id, "id");
        requireNonNull(connectionIndicator, "connectionIndicator");
        requireNonNull(publisherIndicator, "publisherIndicator");
        registerContributor(id + ".connection", connectionIndicator);
        registerContributor(id + ".publisher", publisherIndicator);
    }

}
