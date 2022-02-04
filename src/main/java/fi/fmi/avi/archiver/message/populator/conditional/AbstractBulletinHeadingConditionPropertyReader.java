package fi.fmi.avi.archiver.message.populator.conditional;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;

import fi.fmi.avi.archiver.message.populator.BulletinHeadingSource;

public abstract class AbstractBulletinHeadingConditionPropertyReader<T> extends AbstractConditionPropertyReader<T> {
    private static final Map<BulletinHeadingSource, String> PROPERTY_NAME_PREFIXES = createPropertyNamePrefixes();

    @VisibleForTesting
    static Map<BulletinHeadingSource, String> createPropertyNamePrefixes() {
        final EnumMap<BulletinHeadingSource, String> builder = new EnumMap<>(BulletinHeadingSource.class);
        builder.put(BulletinHeadingSource.GTS_BULLETIN_HEADING, "Gts");
        builder.put(BulletinHeadingSource.COLLECT_IDENTIFIER, "Collect");
        return Collections.unmodifiableMap(builder);
    }

    protected abstract List<BulletinHeadingSource> getBulletinHeadingSources();

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
