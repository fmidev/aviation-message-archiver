package fi.fmi.avi.archiver.config;

import org.inferred.freebuilder.FreeBuilder;
import org.springframework.messaging.MessageHeaders;

import jdk.nashorn.internal.ir.annotations.Immutable;

@Immutable
@FreeBuilder
public abstract class FileContent {

    FileContent() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public abstract String getContent();

    public abstract MessageHeaders getMessageHeader();

    public String getHeader() {

        return ((String) getMessageHeader().getOrDefault("file_name", "")).split("\\.")[0];
    }

    public static class Builder extends FileContent_Builder {
        Builder() {
        }
    }

}