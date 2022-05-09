package fi.fmi.avi.archiver.message.populator.conditional;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.annotations.VisibleForTesting;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;
import fi.fmi.avi.archiver.message.populator.BulletinHeadingSource;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorHelper;

/**
 * An abstract implementation of {@code ConditionPropertyReader} for properties of bulletin heading, that applies convention over code principle.
 * Main difference to {@link AbstractConditionPropertyReader} is in the {@link #getPropertyName() property name generation}.
 *
 * @param <T>
 *         property type
 */
public abstract class AbstractBulletinHeadingConditionPropertyReader<T> extends AbstractConditionPropertyReader<T> {
    public static final String GTS_BULLETIN_HEADING_PREFIX = "Gts";
    public static final String COLLECT_IDENTIFIER_PREFIX = "Collect";

    private static final Map<BulletinHeadingSource, String> PROPERTY_NAME_PREFIXES = createPropertyNamePrefixes();

    @VisibleForTesting
    static Map<BulletinHeadingSource, String> createPropertyNamePrefixes() {
        final EnumMap<BulletinHeadingSource, String> builder = new EnumMap<>(BulletinHeadingSource.class);
        builder.put(BulletinHeadingSource.GTS_BULLETIN_HEADING, GTS_BULLETIN_HEADING_PREFIX);
        builder.put(BulletinHeadingSource.COLLECT_IDENTIFIER, COLLECT_IDENTIFIER_PREFIX);
        return Collections.unmodifiableMap(builder);
    }

    /**
     * Return bulletin heading sources to read property form in preferred order.
     *
     * @return bulletin heading sources to read property form in preferred order
     */
    protected abstract List<BulletinHeadingSource> getBulletinHeadingSources();

    /**
     * A helper method to invoke {@link MessagePopulatorHelper#getFirstNonNullFromBulletinHeading(Collection, InputAviationMessage, Function)}.
     * It adds bulletin heading sources from {@link #getBulletinHeadingSources()} in the invocation.
     *
     * @param input
     *         input aviation message
     * @param fn
     *         function to get an optional value from the input heading
     *
     * @return non-null value if present
     */
    protected Optional<String> getFirstNonNullFromBulletinHeading(final InputAviationMessage input, final Function<InputBulletinHeading, Optional<String>> fn) {
        return MessagePopulatorHelper.getFirstNonNullFromBulletinHeading(getBulletinHeadingSources(), input, fn);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * In addition the name is prefixed with {@value GTS_BULLETIN_HEADING_PREFIX} and/or {@value COLLECT_IDENTIFIER_PREFIX}, separated by word 'Or' if
     * necessary. The result is formatted as lower camel case.
     * E.g. if implementation class name is {@code MyCustomPropertyReader} and bulletin heading sources is {@code [GTS_BULLETIN_HEADING, COLLECT_IDENTIFIER]},
     * the resulting name would be {@code gtsOrCollectMyCustom}.
     * </p>
     *
     * @return {@inheritDoc}
     */
    @Override
    public String getPropertyName() {
        final List<BulletinHeadingSource> bulletinHeadingSources = getBulletinHeadingSources();
        if (bulletinHeadingSources.isEmpty()) {
            throw new IllegalStateException("Empty bulletinHeadingSources");
        }
        final StringBuilder builder = new StringBuilder();
        for (final BulletinHeadingSource bulletinHeadingSource : bulletinHeadingSources) {
            builder.append(PROPERTY_NAME_PREFIXES.get(bulletinHeadingSource))//
                    .append("Or");
        }
        builder.setLength(builder.length() - 2);
        final int prefixLength = builder.length();
        builder.append(super.getPropertyName());
        builder.setCharAt(0, Character.toLowerCase(builder.charAt(0)));
        builder.setCharAt(prefixLength, Character.toUpperCase(builder.charAt(prefixLength)));
        return builder.toString();
    }
}
