package fi.fmi.avi.archiver.file;

import fi.fmi.avi.model.bulletin.BulletinHeading;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

@FreeBuilder
public abstract class FileBulletinHeading {

    FileBulletinHeading() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public abstract Builder toBuilder();

    public abstract Optional<BulletinHeading> getBulletinHeading();

    public abstract Optional<String> getBulletinHeadingString();

    public static class Builder extends FileBulletinHeading_Builder {
        Builder() {
        }
    }

}
