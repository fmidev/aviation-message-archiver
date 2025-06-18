package fi.fmi.avi.archiver.message.populator.conditional;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;
import fi.fmi.avi.archiver.message.populator.BulletinHeadingSource;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;

public final class ConditionPropertyReaderTests {
    private ConditionPropertyReaderTests() {
        throw new AssertionError();
    }

    public abstract static class AbstractTestStringConditionPropertyReader extends AbstractConditionPropertyReader<String> {
        @Nullable
        @Override
        public String readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder target) {
            return null;
        }
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public abstract static class AbstractTestStringBulletinHeadingConditionPropertyReader extends AbstractBulletinHeadingConditionPropertyReader<String> {
        private final List<BulletinHeadingSource> bulletinHeadingSources;

        protected AbstractTestStringBulletinHeadingConditionPropertyReader() {
            this(BulletinHeadingSource.DEFAULT_SOURCES);
        }

        protected AbstractTestStringBulletinHeadingConditionPropertyReader(final List<BulletinHeadingSource> bulletinHeadingSources) {
            this.bulletinHeadingSources = bulletinHeadingSources;
        }

        @Override
        protected List<BulletinHeadingSource> getBulletinHeadingSources() {
            return bulletinHeadingSources;
        }

        @Nullable
        @Override
        public String readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder target) {
            return null;
        }
    }

    public static final class BulletinHeadingSourcesPermutationsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
            return BulletinHeadingSource.getPermutations().stream()//
                    .map(Arguments::of);
        }
    }
}
