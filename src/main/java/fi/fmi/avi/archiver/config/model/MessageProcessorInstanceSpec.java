package fi.fmi.avi.archiver.config.model;

import fi.fmi.avi.archiver.message.processor.conditional.GeneralPropertyPredicate;

import java.util.Map;

public interface MessageProcessorInstanceSpec {
    default String getProcessorInformalType() {
        return "message processor";
    }

    String getName();

    Map<String, GeneralPropertyPredicate.Builder<?>> getActivateOn();

    Map<String, Object> getConfig();
}
