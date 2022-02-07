package fi.fmi.avi.archiver.message.populator.conditional;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.CsvSource;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.testing.NullPointerTester;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.populator.BulletinHeadingSource;
import fi.fmi.avi.archiver.message.populator.BulletinHeadingSourceSensitiveTester;
import fi.fmi.avi.archiver.message.populator.MessagePopulatorHelper;
import fi.fmi.avi.model.bulletin.immutable.BulletinHeadingImpl;

@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC")
class OriginatorPropertyReaderTest {
    private static final String GTS_ORIGINATOR = "YUDO";
    private static final String COLLECT_ORIGINATOR = "YUDD";
    private static final InputAviationMessage INPUT = InputAviationMessage.builder()//
            .setGtsBulletinHeading(inputBulletinHeading(GTS_ORIGINATOR))//
            .setCollectIdentifier(inputBulletinHeading(COLLECT_ORIGINATOR))//
            .buildPartial();
    private static final Map<BulletinHeadingSource, String> EXPECTED_RESULTS = Maps.immutableEnumMap(ImmutableMap.of(//
            BulletinHeadingSource.GTS_BULLETIN_HEADING, GTS_ORIGINATOR, //
            BulletinHeadingSource.COLLECT_IDENTIFIER, COLLECT_ORIGINATOR//
    ));

    private static InputBulletinHeading inputBulletinHeading(final String originator) {
        return InputBulletinHeading.builder()//
                .setBulletinHeading(BulletinHeadingImpl.builder()//
                        .setLocationIndicator(originator)//
                        .buildPartial())//
                .build();
    }

    @ParameterizedTest
    @CsvSource({ "YUDO", "AAAA", "ZZZZ" })
    void validate_given_valid_originator_returns_true(final String originator) {
        final OriginatorPropertyReader propertyReader = new OriginatorPropertyReader(MessagePopulatorHelper.DEFAULT_BULLETIN_HEADING_SOURCES);
        assertThat(propertyReader.validate(originator)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({ "UDO", "YUDOO", "/UDO", "Y/DO", "YU/O", "YUD/", "0UDO", "Y0DO", "YU0O", "YUD0" })
    void validate_given_invalid_designator_returns_false(final String originator) {
        final OriginatorPropertyReader propertyReader = new OriginatorPropertyReader(MessagePopulatorHelper.DEFAULT_BULLETIN_HEADING_SOURCES);
        assertThat(propertyReader.validate(originator)).isFalse();
    }

    @Test
    void testGetValueGetterForType() {
        final class TestReader extends ConditionPropertyReaderTests.AbstractTestStringBulletinHeadingConditionPropertyReader {
        }
        final OriginatorPropertyReader reader = new OriginatorPropertyReader(MessagePopulatorHelper.DEFAULT_BULLETIN_HEADING_SOURCES);
        final TestReader controlReader = new TestReader();
        assertThat(reader.getValueGetterForType().getGenericReturnType()).isEqualTo(controlReader.getValueGetterForType().getGenericReturnType());
    }

    @ParameterizedTest
    @ArgumentsSource(ConditionPropertyReaderTests.BulletinHeadingSourcesPermutationsProvider.class)
    void testGetPropertyName(final List<BulletinHeadingSource> bulletinHeadingSources) {
        final class OriginatorPropertyReader extends ConditionPropertyReaderTests.AbstractTestStringBulletinHeadingConditionPropertyReader {
            public OriginatorPropertyReader(final List<BulletinHeadingSource> bulletinHeadingSources) {
                super(bulletinHeadingSources);
            }
        }
        final fi.fmi.avi.archiver.message.populator.conditional.OriginatorPropertyReader reader //
                = new fi.fmi.avi.archiver.message.populator.conditional.OriginatorPropertyReader(bulletinHeadingSources);
        final OriginatorPropertyReader controlReader = new OriginatorPropertyReader(bulletinHeadingSources);
        assertThat(reader.getPropertyName()).isEqualTo(controlReader.getPropertyName());
    }

    @ParameterizedTest
    @ArgumentsSource(ConditionPropertyReaderTests.BulletinHeadingSourcesPermutationsProvider.class)
    void testToString(final List<BulletinHeadingSource> bulletinHeadingSources) {
        final OriginatorPropertyReader propertyReader = new OriginatorPropertyReader(bulletinHeadingSources);
        assertThat(propertyReader.toString()).isEqualTo(propertyReader.getPropertyName());
    }

    @SuppressWarnings("UnstableApiUsage")
    @Test
    public void testNulls() {
        final Class<?> classUnderTest = OriginatorPropertyReader.class;
        final OriginatorPropertyReader instance = new OriginatorPropertyReader(MessagePopulatorHelper.DEFAULT_BULLETIN_HEADING_SOURCES);
        final NullPointerTester tester = new NullPointerTester();
        final NullPointerTester.Visibility minimalVisibility = NullPointerTester.Visibility.PACKAGE;
        tester.setDefault(ArchiveAviationMessage.Builder.class, ArchiveAviationMessage.builder());
        tester.setDefault(InputAviationMessage.class, InputAviationMessage.builder().buildPartial());

        tester.testStaticMethods(classUnderTest, minimalVisibility);
        tester.testConstructors(classUnderTest, minimalVisibility);
        tester.testInstanceMethods(instance, minimalVisibility);
    }

    @Nested
    class ReadValueTest extends BulletinHeadingSourceSensitiveTester<String> {
        @Override
        protected InputAviationMessage getInput() {
            return INPUT;
        }

        @Override
        protected Map<BulletinHeadingSource, String> getExpectedResults() {
            return EXPECTED_RESULTS;
        }

        @Nullable
        @Override
        protected String invoke(final InputAviationMessage input, final List<BulletinHeadingSource> bulletinHeadingSources) {
            final OriginatorPropertyReader propertyReader = new OriginatorPropertyReader(bulletinHeadingSources);
            return propertyReader.readValue(input, ArchiveAviationMessage.builder());
        }
    }
}
