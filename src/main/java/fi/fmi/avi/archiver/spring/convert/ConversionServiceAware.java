package fi.fmi.avi.archiver.spring.convert;

import org.springframework.core.convert.ConversionService;

public interface ConversionServiceAware {
    void setConversionService(ConversionService conversionService);
}
