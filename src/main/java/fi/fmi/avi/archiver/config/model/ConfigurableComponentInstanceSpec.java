package fi.fmi.avi.archiver.config.model;

import fi.fmi.avi.archiver.message.populator.conditional.GeneralPropertyPredicate;

import java.util.Map;

public interface ConfigurableComponentInstanceSpec {
    String getName();

    Map<String, GeneralPropertyPredicate.Builder<?>> getActivateOn();

    Map<String, Object> getConfig();
}
