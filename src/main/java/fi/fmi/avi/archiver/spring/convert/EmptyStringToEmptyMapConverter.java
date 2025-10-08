package fi.fmi.avi.archiver.spring.convert;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts an empty {@link String} into an empty {@link Map}.
 * If the {@code String} is non-empty, an {@link UnsupportedOperationException} is thrown.
 *
 * <p>
 * This dummy converter exist to enable empty keys in the application configuration.
 * E.g. {@code activate-on} and {@code config} in following example:
 * <pre>
 * production-line:
 *   message-populators:
 *     - name: FileMetadataPopulator
 *       activate-on:
 *       config:
 * </pre>
 * </p>
 */
public class EmptyStringToEmptyMapConverter extends AbstractNonEmptyStringConverter<Map<String, ?>> {
    public EmptyStringToEmptyMapConverter() {
    }

    @Nullable
    @Override
    protected Map<String, String> getEmptyValue() {
        return new LinkedHashMap<>();
    }

    @Override
    protected Map<String, String> convertNonEmpty(final String source) {
        throw new UnsupportedOperationException("Converting a non-empty String to map is not supported");
    }
}
