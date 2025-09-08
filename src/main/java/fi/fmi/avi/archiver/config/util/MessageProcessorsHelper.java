package fi.fmi.avi.archiver.config.util;

import fi.fmi.avi.archiver.config.model.MessageProcessorInstanceSpec;
import fi.fmi.avi.archiver.message.processor.conditional.*;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
import fi.fmi.avi.archiver.util.instantiation.ObjectFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

@Component
public class MessageProcessorsHelper {

    private final ConfigValueConverter configValueConverter;
    private final ConditionPropertyReaderFactory conditionPropertyReaderFactory;

    public MessageProcessorsHelper(final ConfigValueConverter configValueConverter, final ConditionPropertyReaderFactory conditionPropertyReaderFactory) {
        this.configValueConverter = requireNonNull(configValueConverter, "configValueConverter");
        this.conditionPropertyReaderFactory = requireNonNull(conditionPropertyReaderFactory, "conditionPropertyReaderFactory");
    }

    public <P, C extends P, S extends MessageProcessorInstanceSpec> Stream<P> createMessageProcessors(
            final List<? extends ObjectFactory<? extends P>> factories,
            final List<S> specs,
            final BiFunction<ActivationCondition, P, C> conditionalComponentFactory) {
        requireNonNull(factories, "factories");
        requireNonNull(specs, "specs");
        requireNonNull(conditionalComponentFactory, "conditionalComponentFactory");
        final Map<String, ObjectFactory<? extends P>> factoriesByName = factories.stream()//
                .collect(Collectors.toUnmodifiableMap(ObjectFactory::getName, Function.identity()));
        return specs.stream()
                .map(spec -> createMessageProcessor(spec, factoriesByName, conditionalComponentFactory));
    }

    public <P, C extends P, S extends MessageProcessorInstanceSpec> P createMessageProcessor(
            final S spec,
            final Map<String, ? extends ObjectFactory<? extends P>> factoriesByName,
            final BiFunction<ActivationCondition, P, C> conditionalComponentFactory) {
        requireNonNull(spec, "spec");
        requireNonNull(factoriesByName, "factoriesByName");
        requireNonNull(conditionalComponentFactory, "conditionalComponentFactory");
        final ObjectFactory<? extends P> factory = Optional.ofNullable(factoriesByName.get(spec.getName()))
                .orElseThrow(() -> new IllegalArgumentException(String.format(Locale.US, "Unknown %s: <%s>", spec.getProcessorInformalType(), spec.getName())));
        final P component;
        try {
            component = factory.newInstance(spec.getConfig());
        } catch (final RuntimeException exception) {
            throw new IllegalStateException("Failed to instantiate %s '%s': %s".formatted(spec.getProcessorInformalType(), spec.getName(), exception.getMessage()), exception);
        }
        return applyActivationCondition(component, spec, conditionalComponentFactory);
    }

    @SuppressWarnings("unchecked")
    public <P, C extends P, S extends MessageProcessorInstanceSpec, V> P applyActivationCondition(
            final P component,
            final S spec,
            final BiFunction<ActivationCondition, P, C> conditionalComponentFactory) {
        requireNonNull(component, "component");
        requireNonNull(spec, "spec");
        requireNonNull(conditionalComponentFactory, "conditionalComponentFactory");
        return spec.getActivateOn().entrySet().stream()//
                .map(entry -> {
                    try {
                        final String propertyName = entry.getKey();
                        final ConditionPropertyReader<V> conditionPropertyReader = (ConditionPropertyReader<V>) conditionPropertyReaderFactory.getInstance(propertyName);
                        final GeneralPropertyPredicate<V> propertyPredicate = entry.getValue()
                                .transform(
                                        element -> (V) configValueConverter.toReturnValueType(element, conditionPropertyReader.getValueGetterForType()),
                                        conditionPropertyReader.getComparator().orElse(null))
                                .validate(conditionPropertyReader::validate)//
                                .build();
                        return new PropertyActivationCondition<>(conditionPropertyReader, propertyPredicate);
                    } catch (final RuntimeException e) {
                        throw new IllegalStateException("Unable to initialize %s '%s' activateOn: %s"
                                .formatted(spec.getProcessorInformalType(), spec.getName(), e.getMessage()), e);
                    }
                })//
                .collect(Collectors.collectingAndThen(Collectors.toList(), ActivationCondition::and))//
                .<P>map(activationCondition -> conditionalComponentFactory.apply(activationCondition, component))//
                .orElse(component);
    }
}
