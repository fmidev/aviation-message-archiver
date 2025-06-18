package fi.fmi.avi.archiver.message.populator.conditional;

import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;
import fi.fmi.avi.archiver.message.populator.BulletinHeadingSource;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class AbstractBulletinHeadingConditionPropertyReaderTest {
    @Test
    void createPropertyNamePrefixes_adds_all_values() {
        final Map<BulletinHeadingSource, String> result = AbstractBulletinHeadingConditionPropertyReader.createPropertyNamePrefixes();
        assertThat(result.keySet()).containsExactlyInAnyOrder(BulletinHeadingSource.values());
    }

    @Test
    void getPropertyName_throws_exception_when_bulletinHeadingSources_is_empty() {
        final List<BulletinHeadingSource> bulletinHeadingSources = Collections.emptyList();
        final TestPropertyReader reader = new TestPropertyReader(bulletinHeadingSources);
        assertThatIllegalStateException().isThrownBy(reader::getPropertyName);
    }

    @Test
    void getPropertyName_prefixes_with_gts() {
        final List<BulletinHeadingSource> bulletinHeadingSources = Collections.singletonList(BulletinHeadingSource.GTS_BULLETIN_HEADING);
        assertThat(new TestPropertyReader(bulletinHeadingSources).getPropertyName()).isEqualTo("gtsTest");
    }

    @Test
    void getPropertyName_prefixes_with_collect() {
        final List<BulletinHeadingSource> bulletinHeadingSources = Collections.singletonList(BulletinHeadingSource.COLLECT_IDENTIFIER);
        assertThat(new TestPropertyReader(bulletinHeadingSources).getPropertyName()).isEqualTo("collectTest");
    }

    @Test
    void getPropertyName_prefixes_with_gtsOrCollect() {
        final List<BulletinHeadingSource> bulletinHeadingSources = Arrays.asList(BulletinHeadingSource.GTS_BULLETIN_HEADING,
                BulletinHeadingSource.COLLECT_IDENTIFIER);
        assertThat(new TestPropertyReader(bulletinHeadingSources).getPropertyName()).isEqualTo("gtsOrCollectTest");
    }

    @Test
    void getPropertyName_prefixes_with_collectOrGts() {
        final List<BulletinHeadingSource> bulletinHeadingSources = Arrays.asList(BulletinHeadingSource.COLLECT_IDENTIFIER,
                BulletinHeadingSource.GTS_BULLETIN_HEADING);
        assertThat(new TestPropertyReader(bulletinHeadingSources).getPropertyName()).isEqualTo("collectOrGtsTest");
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    private static final class TestPropertyReader extends AbstractBulletinHeadingConditionPropertyReader<String> {
        private final List<BulletinHeadingSource> bulletinHeadingSources;

        private TestPropertyReader(final List<BulletinHeadingSource> bulletinHeadingSources) {
            this.bulletinHeadingSources = bulletinHeadingSources;
        }

        @Override
        protected List<BulletinHeadingSource> getBulletinHeadingSources() {
            return bulletinHeadingSources;
        }

        @Override
        public String readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder target) {
            return "";
        }
    }
}
