package fi.fmi.avi.archiver.config.model;

import fi.fmi.avi.archiver.message.processor.conditional.GeneralPropertyPredicate;

import java.util.Map;

public interface MessageProcessorInstanceSpec {
    String getName();

    Map<String, GeneralPropertyPredicate.Builder<?>> getActivateOn();

    Map<String, Object> getConfig();
}
