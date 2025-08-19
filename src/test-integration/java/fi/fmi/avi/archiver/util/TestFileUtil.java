package fi.fmi.avi.archiver.util;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static java.util.Objects.requireNonNull;

public final class TestFileUtil {
    private static final int WAIT_MILLIS = 100;
    private static final Duration TIMEOUT = Duration.ofMillis(1000);

    private TestFileUtil() {
        throw new AssertionError();
    }

    public static Path getResourcePath(final Class<?> resourceClass, final String fileName) throws URISyntaxException, FileNotFoundException {
        requireNonNull(resourceClass, "resourceClass");
        requireNonNull(fileName, "fileName");
        @Nullable final URL resource = resourceClass.getResource(fileName);
        if (resource == null) {
            throw new FileNotFoundException("Resource " + resourceClass.getPackageName().replace('.', '/') + "/" + fileName + " not found");
        }
        final Path path = Paths.get(resource.toURI());
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Path " + path + " does not exist");
        }
        return path;
    }

    public static void waitUntilFileExists(final Path file) throws InterruptedException {
        waitUntilFileExists(file, TIMEOUT);
    }

    public static void waitUntilFileExists(final Path file, final Duration timeout) throws InterruptedException {
        requireNonNull(file, "file");
        requireNonNull(timeout, "timeout");
        final long timoutMillis = timeout.toMillis();
        long totalWaitTime = 0;
        while (!Files.exists(file) && totalWaitTime < timoutMillis) {
            //noinspection BusyWait
            Thread.sleep(WAIT_MILLIS);
            totalWaitTime += WAIT_MILLIS;
        }
    }
}
