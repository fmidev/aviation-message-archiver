package fi.fmi.avi.archiver.spring.convert;

import org.springframework.core.convert.converter.Converter;

import fi.fmi.avi.archiver.message.populator.conditional.GeneralPropertyPredicate;

public class StringToGeneralPropertyPredicateBuilderConverter implements Converter<String, GeneralPropertyPredicate.Builder<?>> {
    @Override
    public GeneralPropertyPredicate.Builder<?> convert(final String source) {
        if ("absent".equals(source)) {
            return GeneralPropertyPredicate.builder().setIsAbsent(true);
        }
        throw new IllegalArgumentException("Unable to convert '" + source + "' to GeneralPropertyPredicate.Builder");
    }
}
