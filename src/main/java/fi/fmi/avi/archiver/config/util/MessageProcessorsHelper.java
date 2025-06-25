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

    public <T, C extends T, S extends MessageProcessorInstanceSpec> Stream<T> createMessageProcessors(
            final List<? extends ObjectFactory<? extends T>> factories,
            final List<S> specs,
            final BiFunction<ActivationCondition, T, C> conditionalComponentFactory) {
        final Map<String, ObjectFactory<? extends T>> factoriesByName = factories.stream()//
                .collect(Collectors.toMap(ObjectFactory::getName, Function.identity()));
        return specs.stream()
                .map(spec -> createMessageProcessor(spec, factoriesByName, conditionalComponentFactory));
    }

    public <T, C extends T, S extends MessageProcessorInstanceSpec> T createMessageProcessor(
            final S spec,
            final Map<String, ? extends ObjectFactory<? extends T>> factoriesByName,
            final BiFunction<ActivationCondition, T, C> conditionalComponentFactory) {
        final T component = Optional.ofNullable(factoriesByName.get(spec.getName()))
                .orElseThrow(() -> new IllegalArgumentException(String.format(Locale.US, "Unknown %s: <%s>", spec.getProcessorInformalType(), spec.getName())))
                .newInstance(spec.getConfig());
        return applyActivationCondition(component, spec, conditionalComponentFactory);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T, C extends T, S extends MessageProcessorInstanceSpec> T applyActivationCondition(
            final T component,
            final S spec,
            final BiFunction<ActivationCondition, T, C> conditionalComponentFactory) {
        return spec.getActivateOn().entrySet().stream()//
                .map(entry -> {
                    try {
                        final String propertyName = entry.getKey();
                        final ConditionPropertyReader conditionPropertyReader = conditionPropertyReaderFactory.getInstance(propertyName);
                        final GeneralPropertyPredicate<?> propertyPredicate = entry.getValue()
                                .transform(element -> configValueConverter.toReturnValueType(element, conditionPropertyReader.getValueGetterForType()))//
                                .validate(conditionPropertyReader::validate)//
                                .build();
                        return new PropertyActivationCondition(conditionPropertyReader, propertyPredicate);
                    } catch (final RuntimeException e) {
                        throw new IllegalStateException("Unable to initialize '" + spec.getName() + "' activateOn: " + e.getMessage(), e);
                    }
                })//
                .collect(Collectors.collectingAndThen(Collectors.toList(), ActivationCondition::and))//
                .<T>map(activationCondition -> conditionalComponentFactory.apply(activationCondition, component))//
                .orElse(component);
    }
}
