package fi.fmi.avi.archiver.message.populator.conditional;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
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
import fi.fmi.avi.model.bulletin.DataTypeDesignatorT1;
import fi.fmi.avi.model.bulletin.DataTypeDesignatorT2;
import fi.fmi.avi.model.bulletin.immutable.BulletinHeadingImpl;

@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC")
class DataDesignatorPropertyReaderTest {
    private static final String GTS_DATA_DESIGNATOR = "SAXX17";
    private static final String COLLECT_DATA_DESIGNATOR = "LAXX17";
    private static final InputAviationMessage INPUT = InputAviationMessage.builder()//
            .setGtsBulletinHeading(inputBulletinHeading(GTS_DATA_DESIGNATOR))//
            .setCollectIdentifier(inputBulletinHeading(COLLECT_DATA_DESIGNATOR))//
            .buildPartial();
    private static final Map<BulletinHeadingSource, String> EXPECTED_RESULTS = Maps.immutableEnumMap(ImmutableMap.of(//
            BulletinHeadingSource.GTS_BULLETIN_HEADING, GTS_DATA_DESIGNATOR, //
            BulletinHeadingSource.COLLECT_IDENTIFIER, COLLECT_DATA_DESIGNATOR//
    ));

    private static InputBulletinHeading inputBulletinHeading(final String dataDesignator) {
        return InputBulletinHeading.builder()//
                .setBulletinHeading(BulletinHeadingImpl.builder()//
                        .setDataTypeDesignatorT1ForTAC(DataTypeDesignatorT1.fromCode(dataDesignator.charAt(0)))//
                        .setDataTypeDesignatorT2(DataTypeDesignatorT2.fromExtensionCode(dataDesignator.charAt(1)))//
                        .setBulletinNumber(Integer.parseInt(dataDesignator.substring(4)))//
                        .setGeographicalDesignator(dataDesignator.substring(2, 4))//
                        .buildPartial())//
                .build();
    }

    @ParameterizedTest
    @CsvSource({ "AAAA00", "ZZZZ99", "ABCD12", "ZYXW87" })
    void validate_given_valid_designator_returns_true(final String designator) {
        final DataDesignatorPropertyReader propertyReader = new DataDesignatorPropertyReader(MessagePopulatorHelper.DEFAULT_BULLETIN_HEADING_SOURCES);
        assertThat(propertyReader.validate(designator)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({ "AAAAA00", "AAAA000", "AAA00", "AAAA0", //
            "0AAA00", "A0AA00", "AA0A00", "AAA000", "AAAAA0", "AAAA0A", //
            "/AAA00", "A/AA00", "AA/A00", "AAA/00", "AAAA/0", "AAAA0/" })
    void validate_given_invalid_designator_returns_false(final String designator) {
        final DataDesignatorPropertyReader propertyReader = new DataDesignatorPropertyReader(MessagePopulatorHelper.DEFAULT_BULLETIN_HEADING_SOURCES);
        assertThat(propertyReader.validate(designator)).isFalse();
    }

    @Test
    void testGetValueGetterForType() {
        final class TestReader extends AbstractTestStringBulletinHeadingConditionPropertyReader {
        }
        final DataDesignatorPropertyReader reader = new DataDesignatorPropertyReader(MessagePopulatorHelper.DEFAULT_BULLETIN_HEADING_SOURCES);
        final TestReader controlReader = new TestReader();
        assertThat(reader.getValueGetterForType().getGenericReturnType()).isEqualTo(controlReader.getValueGetterForType().getGenericReturnType());
    }

    @ParameterizedTest
    @ArgumentsSource(BulletinHeadingSourcesPermutationsProvider.class)
    void testGetPropertyName(final List<BulletinHeadingSource> bulletinHeadingSources) {
        final class DataDesignatorPropertyReader extends AbstractTestStringBulletinHeadingConditionPropertyReader {
            public DataDesignatorPropertyReader(final List<BulletinHeadingSource> bulletinHeadingSources) {
                super(bulletinHeadingSources);
            }
        }
        final fi.fmi.avi.archiver.message.populator.conditional.DataDesignatorPropertyReader reader //
                = new fi.fmi.avi.archiver.message.populator.conditional.DataDesignatorPropertyReader(bulletinHeadingSources);
        final DataDesignatorPropertyReader controlReader = new DataDesignatorPropertyReader(bulletinHeadingSources);
        assertThat(reader.getPropertyName()).isEqualTo(controlReader.getPropertyName());
    }

    @SuppressWarnings("UnstableApiUsage")
    @Test
    public void testNulls() {
        final Class<?> classUnderTest = DataDesignatorPropertyReaderTest.class;
        final DataDesignatorPropertyReader instance = new DataDesignatorPropertyReader(MessagePopulatorHelper.DEFAULT_BULLETIN_HEADING_SOURCES);
        final NullPointerTester tester = new NullPointerTester();
        final NullPointerTester.Visibility minimalVisibility = NullPointerTester.Visibility.PACKAGE;
        tester.setDefault(ArchiveAviationMessage.Builder.class, ArchiveAviationMessage.builder());
        tester.setDefault(InputAviationMessage.class, InputAviationMessage.builder().buildPartial());

        tester.testStaticMethods(classUnderTest, minimalVisibility);
        tester.testConstructors(classUnderTest, minimalVisibility);
        tester.testInstanceMethods(instance, minimalVisibility);
    }

    private static abstract class AbstractTestStringBulletinHeadingConditionPropertyReader extends AbstractBulletinHeadingConditionPropertyReader<String> {
        private final List<BulletinHeadingSource> bulletinHeadingSources;

        protected AbstractTestStringBulletinHeadingConditionPropertyReader() {
            this(MessagePopulatorHelper.DEFAULT_BULLETIN_HEADING_SOURCES);
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
        public String readValue(final InputAviationMessage input, final ArchiveAviationMessage.Builder target) {
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
            final DataDesignatorPropertyReader reader = new DataDesignatorPropertyReader(bulletinHeadingSources);
            return reader.readValue(input, ArchiveAviationMessage.builder());
        }
    }
}
