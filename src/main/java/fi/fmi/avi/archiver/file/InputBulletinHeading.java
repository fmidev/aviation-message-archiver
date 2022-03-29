package fi.fmi.avi.archiver.file;

import java.util.Optional;

import org.inferred.freebuilder.FreeBuilder;

import fi.fmi.avi.model.bulletin.BulletinHeading;

/**
 * Model representing an optional parsed bulletin heading.
 */
@FreeBuilder
public abstract class InputBulletinHeading {

    InputBulletinHeading() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public abstract Builder toBuilder();

    /**
     * Return bulletin heading as parsed object, if available.
     *
     * @return bulletin heading as parsed object, or empty if not available
     */
    public abstract Optional<BulletinHeading> getBulletinHeading();

    /**
     * Return original bulletin heading string, if available.
     *
     * @return original bulletin heading string, or empty if not available
     */
    public abstract Optional<String> getBulletinHeadingString();

    public static class Builder extends InputBulletinHeading_Builder {
        Builder() {
        }
    }

}
