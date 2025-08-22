package fi.fmi.avi.archiver;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.common.io.Resources;
import fi.fmi.avi.archiver.config.ConversionConfig;
import fi.fmi.avi.archiver.config.TestConfig;
import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.database.DatabaseAccessTestUtil;
import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.message.ArchivalStatus;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageIWXXMDetails;
import fi.fmi.avi.archiver.message.ProcessingResult;
import fi.fmi.avi.archiver.message.processor.postaction.TestPostAction;
import fi.fmi.avi.archiver.message.processor.postaction.TestPostActionRegistry;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.custommonkey.xmlunit.XMLUnit;
import org.inferred.freebuilder.FreeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest({"auto.startup=false", "testclass.name=fi.fmi.avi.archiver.AviationMessageArchiverTest"})
@Sql(scripts = {"classpath:/fi/fmi/avi/avidb/schema/h2/schema-h2.sql", "classpath:/h2-data/avidb_test_content.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/h2-data/avidb_cleanup_test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@ContextConfiguration(classes = {AviationMessageArchiver.class, TestConfig.class, ConversionConfig.class},//
        loader = AnnotationConfigContextLoader.class,//
        initializers = {ConfigDataApplicationContextInitializer.class})
@ActiveProfiles("integration-test")
class AviationMessageArchiverTest {

    private static final String TEMP_FILE_SUFFIX = ".tmp";
    private static final Set<String> INCLUDE_INPUT_FILES = ImmutableSet.of();

    private final RecursiveComparisonConfiguration archiveMessageComparisonConfiguration = RecursiveComparisonConfiguration.builder()
            .withEqualsForFields(MessageContentPredicate.INSTANCE, "message")
            .build();
    private final RecursiveComparisonConfiguration postActionInvocationComparisonConfiguration = RecursiveComparisonConfiguration.builder()
            .withEqualsForFields(MessageContentPredicate.INSTANCE, "archiveAviationMessage.message")
            .build();

    @Autowired
    private Map<String, AviationProduct> aviationProducts;
    @Autowired
    private DatabaseAccess databaseAccess;
    @Autowired
    private Clock clock;
    @Autowired
    private TestPostActionRegistry testPostActionRegistry;
    private DatabaseAccessTestUtil databaseAccessTestUtil;

    @SuppressWarnings("ConstantConditions")
    private static String readResourceToString(final String filename) throws IOException {
        return Resources.toString(AviationMessageArchiverTest.class.getResource(filename), StandardCharsets.UTF_8);
    }

    static Stream<AviationMessageArchiverTestCase> test_archival() throws IOException {
        return Stream.of(//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Minimal TAC TAF")//
                        .setProductName("test_taf")//
                        .setInputFileName("simple_taf.txt2")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                .setMessageTime(Instant.parse("2020-05-18T15:00:00Z"))
                                .setStationId(1)
                                .setStationIcaoCode("EFXX")
                                .setFormat(1)
                                .setType(3)
                                .setRoute(1)
                                .setMessage("TAF EFXX 181500Z 1812/1912 00000KT CAVOK=")
                                .setValidFrom(Instant.parse("2020-05-18T12:00:00Z"))
                                .setValidTo(Instant.parse("2020-05-19T12:00:00Z"))
                                .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                                .setHeading("FTXX33 XXXX 181500")
                                .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                .build())
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("TAC TAF with unknown station ICAO code")//
                        .setProductName("test_taf")//
                        .setInputFileName("rejected_taf.txt")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                        .addRejectedMessages(ArchiveAviationMessage.builder()
                                .setMessageTime(Instant.parse("2020-05-18T15:00:00Z"))
                                .setStationIcaoCode("XYZW")
                                .setFormat(1)
                                .setType(3)
                                .setRoute(1)
                                .setMessage("TAF XYZW 181500Z 1812/1912 00000KT CAVOK=")
                                .setValidFrom(Instant.parse("2020-05-18T12:00:00Z"))
                                .setValidTo(Instant.parse("2020-05-19T12:00:00Z"))
                                .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                                .setHeading("FTXX33 XXXX 181500")
                                .setProcessingResult(ProcessingResult.UNKNOWN_STATION_ICAO_CODE)
                                .setArchivalStatus(ArchivalStatus.REJECTED)
                                .build())
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Minimal TAC TAF with another product")//
                        .setProductName("test_taf_2")//
                        .setInputFileName("simple_taf.another")//
                        .setFileModified(Instant.parse("2020-06-15T00:00:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                .setMessageTime(Instant.parse("2020-06-18T15:00:00Z"))
                                .setStationId(1)
                                .setStationIcaoCode("EFXX")
                                .setFormat(1)
                                .setType(3)
                                .setRoute(1)
                                .setMessage("TAF EFXX 181500Z 1812/1912 00000KT CAVOK=")
                                .setValidFrom(Instant.parse("2020-06-18T12:00:00Z"))
                                .setValidTo(Instant.parse("2020-06-19T12:00:00Z"))
                                .setFileModified(Instant.parse("2020-06-15T00:00:00Z"))
                                .setHeading("FTXX33 XXXX 181500")
                                .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                .build())
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Unhandled file name extension")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("taf-unhandled-extension.unknown")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))//
                        .expectUnhandled()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Empty file fails")//
                        .setProductName("test_taf")//
                        .setInputFileName("empty.txt")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))//
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Non-convertible message fails")//
                        .setProductName("test_taf")//
                        .setInputFileName("inconvertible.txt")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))//
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Binary data fails (TAC)")//
                        .setProductName("test_taf")//
                        .setInputFileName("binary.txt")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))//
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("TAF bulletin with binary data fails (TAC)")//
                        .setProductName("test_taf_bulletin")//
                        .setInputFileName("taf-tac-bulletin-binary.bul")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))//
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Binary data fails (IWXXM)")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("binary.xml")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))//
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("TAF with binary data fails (IWXXM)")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("taf-binary-8.xml")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))//
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Only GTS heading")//
                        .setProductName("test_taf")//
                        .setInputFileName("gts-heading.txt")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))//
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("NIL TAC bulletin")//
                        .setProductName("test_taf_bulletin")//
                        .setInputFileName("nil-bulletin.bul")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))//
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Empty collect document")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("empty-collect.xml")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))//
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("TAC TAF without GTS heading")//
                        .setProductName("test_taf")//
                        .setInputFileName("taf-missing-gts-heading.txt")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                .setMessageTime(Instant.parse("2020-05-18T15:00:00Z"))
                                .setStationId(1)
                                .setStationIcaoCode("EFXX")
                                .setFormat(1)
                                .setType(3)
                                .setRoute(1)
                                .setMessage("TAF EFXX 181500Z 1812/1912 00000KT CAVOK=")
                                .setValidFrom(Instant.parse("2020-05-18T12:00:00Z"))
                                .setValidTo(Instant.parse("2020-05-19T12:00:00Z"))
                                .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                                .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                .build())
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("TAC TAF GTS Bulletin")//
                        .setProductName("test_taf_bulletin")//
                        .setInputFileName("taf-tac-bulletin.bul")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2020-05-16T00:00:00Z"))
                                        .setStationId(2)
                                        .setStationIcaoCode("YUDO")
                                        .setFormat(1)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage("TAF YUDO 160000Z NIL=")
                                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                                        .setHeading("FTYU31 YUDO 160000")
                                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                        .build(), //
                                ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2020-05-16T00:00:00Z"))
                                        .setStationId(3)
                                        .setStationIcaoCode("YUDD")
                                        .setFormat(1)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage("TAF YUDD 160000Z NIL=")
                                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                                        .setHeading("FTYU31 YUDO 160000")
                                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                        .build())
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Two TAC TAF GTS Bulletins")//
                        .setProductName("test_taf_bulletin")//
                        .setInputFileName("taf-tac-two-bulletins.bul")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2020-05-16T00:00:00Z"))
                                        .setStationId(2)
                                        .setStationIcaoCode("YUDO")
                                        .setFormat(1)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage("TAF YUDO 160000Z NIL=")
                                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                                        .setHeading("FTYU31 YUDO 160000")
                                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                        .build(), //
                                ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2020-05-16T00:00:00Z"))
                                        .setStationId(3)
                                        .setStationIcaoCode("YUDD")
                                        .setFormat(1)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage("TAF YUDD 160000Z NIL=")
                                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                                        .setHeading("FTYU31 YUDO 160000")
                                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                        .build(), //
                                ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2020-05-18T00:00:00Z"))
                                        .setStationId(2)
                                        .setStationIcaoCode("YUDO")
                                        .setFormat(1)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage("TAF YUDO 180000Z NIL=")
                                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                                        .setHeading("FTYU31 YUDO 180000")
                                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                        .build(), //
                                ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2020-05-18T00:00:00Z"))
                                        .setStationId(3)
                                        .setStationIcaoCode("YUDD")
                                        .setFormat(1)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage("TAF YUDD 180000Z NIL=")
                                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                                        .setHeading("FTYU31 YUDO 180000")
                                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                        .build())
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Partially valid TAC TAF GTS Bulletin")//
                        .setProductName("test_taf_bulletin")//
                        .setInputFileName("taf-tac-bulletin-partially-valid.bul")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2020-05-16T00:00:00Z"))
                                        .setStationId(2)
                                        .setStationIcaoCode("YUDO")
                                        .setFormat(1)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage("TAF YUDO 160000Z NIL=")
                                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                                        .setHeading("FTYU31 YUDO 160000")
                                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                        .build(), //
                                ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2020-05-16T00:00:00Z"))
                                        .setStationId(3)
                                        .setStationIcaoCode("YUDD")
                                        .setFormat(1)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage("TAF YUDD 160000Z NIL=")
                                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                                        .setHeading("FTYU31 YUDO 160000")
                                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                        .build())
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Partially rejected TAC TAF GTS Bulletin")//
                        .setProductName("test_taf_bulletin")//
                        .setInputFileName("taf-tac-bulletin-partially-rejected.bul")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                .setMessageTime(Instant.parse("2020-05-16T00:00:00Z"))
                                .setStationId(2)
                                .setStationIcaoCode("YUDO")
                                .setFormat(1)
                                .setType(3)
                                .setRoute(1)
                                .setMessage("TAF YUDO 160000Z NIL=")
                                .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                                .setHeading("FTYU31 YUDO 160000")
                                .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                .build())
                        .addRejectedMessages(ArchiveAviationMessage.builder()
                                .setMessageTime(Instant.parse("2020-05-16T00:00:00Z"))
                                .setStationIcaoCode("XYZW")
                                .setFormat(1)
                                .setType(3)
                                .setRoute(1)
                                .setMessage("TAF XYZW 160000Z NIL=")
                                .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                                .setHeading("FTYU31 YUDO 160000")
                                .setProcessingResult(ProcessingResult.UNKNOWN_STATION_ICAO_CODE)
                                .setArchivalStatus(ArchivalStatus.REJECTED)
                                .build())
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("TAF TAC GTS Bulletin cut off")//
                        .setProductName("test_taf_bulletin")//
                        .setInputFileName("taf-tac-bulletin-cut-off.bul")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))//
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("IWXXM TAF")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("taf-1.xml")//
                        .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                .setMessageTime(Instant.parse("2017-07-30T11:30:00Z"))
                                .setStationId(4)
                                .setStationIcaoCode("EETN")
                                .setFormat(2)
                                .setType(3)
                                .setRoute(1)
                                .setMessage(readResourceToString("taf-1-message.xml"))
                                .setValidFrom(Instant.parse("2017-07-30T12:00:00Z"))
                                .setValidTo(Instant.parse("2017-07-31T12:00:00Z"))
                                .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()//
                                        .setXMLNamespace("http://icao.int/iwxxm/2.1")//
                                        .build())
                                .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                .build())
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("IWXXM TAF with unknown station ICAO code")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("taf-rejected-6.xml")//
                        .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                        .addRejectedMessages(ArchiveAviationMessage.builder()
                                .setMessageTime(Instant.parse("2017-07-30T11:30:00Z"))
                                .setStationIcaoCode("XYZW")
                                .setFormat(2)
                                .setType(3)
                                .setRoute(1)
                                .setMessage(readResourceToString("taf-rejected-6-message.xml"))
                                .setValidFrom(Instant.parse("2017-07-30T12:00:00Z"))
                                .setValidTo(Instant.parse("2017-07-31T12:00:00Z"))
                                .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                                .setProcessingResult(ProcessingResult.UNKNOWN_STATION_ICAO_CODE)
                                .setArchivalStatus(ArchivalStatus.REJECTED)
                                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()//
                                        .setXMLNamespace("http://icao.int/iwxxm/2.1")//
                                        .build())
                                .build())
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("IWXXM TAF Collect bulletin")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("taf-bulletin-2.xml")//
                        .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2017-07-30T11:30:00Z"))
                                        .setStationId(4)
                                        .setStationIcaoCode("EETN")
                                        .setFormat(2)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage(readResourceToString("taf-bulletin-2-message-1.xml"))
                                        .setValidFrom(Instant.parse("2017-07-30T12:00:00Z"))
                                        .setValidTo(Instant.parse("2017-07-31T12:00:00Z"))
                                        .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/2.1")
                                                .setCollectIdentifier("A_LTFI31EFKL301115_C_EFKL_201902011315--.xml")
                                                .build())
                                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                        .build(), //
                                ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2017-07-27T11:37:00Z"))
                                        .setStationId(4)
                                        .setStationIcaoCode("EETN")
                                        .setFormat(2)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage(readResourceToString("taf-bulletin-2-message-2.xml"))
                                        .setValidFrom(Instant.parse("2017-07-27T12:00:00Z"))
                                        .setValidTo(Instant.parse("2017-07-28T12:00:00Z"))
                                        .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/2.1")
                                                .setCollectIdentifier("A_LTFI31EFKL301115_C_EFKL_201902011315--.xml")
                                                .build())
                                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                        .build())
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("IWXXM TAF Collect bulletin with namespace declarations in TAF elements")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("taf-bulletin-3.xml")//
                        .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2017-07-30T11:30:00Z"))
                                        .setStationId(4)
                                        .setStationIcaoCode("EETN")
                                        .setFormat(2)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage(readResourceToString("taf-bulletin-3-message-1.xml"))
                                        .setValidFrom(Instant.parse("2017-07-30T12:00:00Z"))
                                        .setValidTo(Instant.parse("2017-07-31T12:00:00Z"))
                                        .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/2.1")
                                                .setCollectIdentifier("A_LTFI31EFKL301115_C_EFKL_201902011315--.xml")
                                                .build())
                                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                        .build(), //
                                ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2017-07-27T11:37:00Z"))
                                        .setStationId(4)
                                        .setStationIcaoCode("EETN")
                                        .setFormat(2)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage(readResourceToString("taf-bulletin-3-message-2.xml"))
                                        .setValidFrom(Instant.parse("2017-07-27T12:00:00Z"))
                                        .setValidTo(Instant.parse("2017-07-28T12:00:00Z"))
                                        .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/2.1")
                                                .setCollectIdentifier("A_LTFI31EFKL301115_C_EFKL_201902011315--.xml")
                                                .build())
                                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                        .build())
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("IWXXM TAF Collect bulletin cut off")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("taf-bulletin-7-cut-off.xml")//
                        .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))//
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("IWXXM TAF with GTS heading")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("taf-gts-heading-4.xml")//
                        .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                .setMessageTime(Instant.parse("2017-07-30T11:30:00Z"))
                                .setStationId(4)
                                .setStationIcaoCode("EETN")
                                .setFormat(2)
                                .setType(3)
                                .setRoute(1)
                                .setMessage(readResourceToString("taf-gts-heading-4-message-1.xml"))
                                .setValidFrom(Instant.parse("2017-07-30T12:00:00Z"))
                                .setValidTo(Instant.parse("2017-07-31T12:00:00Z"))
                                .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                                .setHeading("LTFI31 EFKL 301115")
                                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()//
                                        .setXMLNamespace("http://icao.int/iwxxm/2.1")//
                                        .build())
                                .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                .build())
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("IWXXM TAF Collect bulletin with GTS heading")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("taf-gts-heading-bulletin-5.xml")//
                        .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2017-07-30T11:30:00Z"))
                                        .setStationId(4)
                                        .setStationIcaoCode("EETN")
                                        .setFormat(2)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage(readResourceToString("taf-gts-heading-bulletin-5-message-1.xml"))
                                        .setValidFrom(Instant.parse("2017-07-30T12:00:00Z"))
                                        .setValidTo(Instant.parse("2017-07-31T12:00:00Z"))
                                        .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                                        .setHeading("LTFI31 EFKL 301115")
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/2.1")
                                                .setCollectIdentifier("A_LTFI31EFKL301115_C_EFKL_201902011315--.xml")
                                                .build())
                                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                        .build(), //
                                ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2017-07-27T11:37:00Z"))
                                        .setStationId(4)
                                        .setStationIcaoCode("EETN")
                                        .setFormat(2)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage(readResourceToString("taf-gts-heading-bulletin-5-message-2.xml"))
                                        .setValidFrom(Instant.parse("2017-07-27T12:00:00Z"))
                                        .setValidTo(Instant.parse("2017-07-28T12:00:00Z"))
                                        .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                                        .setHeading("LTFI31 EFKL 301115")
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/2.1")
                                                .setCollectIdentifier("A_LTFI31EFKL301115_C_EFKL_201902011315--.xml")
                                                .build())
                                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                        .build())//
                        .build(), //
                AviationMessageArchiverTestCase.builder()//
                        .setName("IWXXM 3.0 TAF Collect bulletin within GTS envelopes")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("taf-gts-bulletin-collect-9.xml")//
                        .setFileModified(Instant.parse("2021-10-21T14:00:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2021-10-21T14:00:00Z"))
                                        .setStationId(5)
                                        .setStationIcaoCode("EFHA")
                                        .setFormat(2)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage(readResourceToString("taf-gts-bulletin-collect-9-message-1.xml"))
                                        .setFileModified(Instant.parse("2021-10-21T14:00:00Z"))
                                        .setHeading("LCFI32 EFKL 211300")
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/3.0")
                                                .setCollectIdentifier("A_LCFI32EFKL211300_C_EFKL_20211021130000.xml")
                                                .build())
                                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                        .build(), //
                                ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2021-10-21T14:36:00Z"))
                                        .setStationId(6)
                                        .setStationIcaoCode("EFKK")
                                        .setFormat(2)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage(readResourceToString("taf-gts-bulletin-collect-9-message-2.xml"))
                                        .setValidFrom(Instant.parse("2021-10-21T15:00:00Z"))
                                        .setValidTo(Instant.parse("2021-10-22T00:00:00Z"))
                                        .setFileModified(Instant.parse("2021-10-21T14:00:00Z"))
                                        .setHeading("LCFI32 EFKL 211400")
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/3.0")
                                                .setCollectIdentifier("A_LCFI32EFKL211400_C_EFKL_20211021140000.xml")
                                                .build())
                                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                        .build(), //
                                ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2021-10-21T14:37:00Z"))
                                        .setStationId(7)
                                        .setStationIcaoCode("EFPO")
                                        .setFormat(2)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage(readResourceToString("taf-gts-bulletin-collect-9-message-3.xml"))
                                        .setValidFrom(Instant.parse("2021-10-21T15:00:00Z"))
                                        .setValidTo(Instant.parse("2021-10-21T18:00:00Z"))
                                        .setFileModified(Instant.parse("2021-10-21T14:00:00Z"))
                                        .setHeading("LCFI32 EFKL 211400")
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/3.0")
                                                .setCollectIdentifier("A_LCFI32EFKL211400_C_EFKL_20211021140000.xml")
                                                .build())
                                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                        .build())
                        .build(), //
                AviationMessageArchiverTestCase.builder()//
                        .setName("IWXXM 3.0 TAF Collect bulletin within GTS envelope")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("taf-gts-bulletin-collect-10.xml")//
                        .setFileModified(Instant.parse("2021-10-21T14:00:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2021-10-21T14:00:00Z"))
                                        .setStationId(5)
                                        .setStationIcaoCode("EFHA")
                                        .setFormat(2)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage(readResourceToString("taf-gts-bulletin-collect-10-message-1.xml"))
                                        .setFileModified(Instant.parse("2021-10-21T14:00:00Z"))
                                        .setHeading("LCFI32 EFKL 211400")
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/3.0")
                                                .setCollectIdentifier("A_LCFI32EFKL211400_C_EFKL_20211021140000.xml")
                                                .build())
                                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                        .build(), //
                                ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2021-10-21T14:36:00Z"))
                                        .setStationId(6)
                                        .setStationIcaoCode("EFKK")
                                        .setFormat(2)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage(readResourceToString("taf-gts-bulletin-collect-10-message-2.xml"))
                                        .setValidFrom(Instant.parse("2021-10-21T15:00:00Z"))
                                        .setValidTo(Instant.parse("2021-10-22T00:00:00Z"))
                                        .setFileModified(Instant.parse("2021-10-21T14:00:00Z"))
                                        .setHeading("LCFI32 EFKL 211400")
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/3.0")
                                                .setCollectIdentifier("A_LCFI32EFKL211400_C_EFKL_20211021140000.xml")
                                                .build())
                                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                        .build(), //
                                ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2021-10-21T14:37:00Z"))
                                        .setStationId(7)
                                        .setStationIcaoCode("EFPO")
                                        .setFormat(2)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage(readResourceToString("taf-gts-bulletin-collect-10-message-3.xml"))
                                        .setValidFrom(Instant.parse("2021-10-21T15:00:00Z"))
                                        .setValidTo(Instant.parse("2021-10-21T18:00:00Z"))
                                        .setFileModified(Instant.parse("2021-10-21T14:00:00Z"))
                                        .setHeading("LCFI32 EFKL 211400")
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/3.0")
                                                .setCollectIdentifier("A_LCFI32EFKL211400_C_EFKL_20211021140000.xml")
                                                .build())
                                        .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                        .build())
                        .build(), //
                AviationMessageArchiverTestCase.builder()//
                        .setName("IWXXM 3.0 METAR")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("metar-iwxxm-30.xml")//
                        .setFileModified(Instant.parse("2012-08-22T15:30:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                .setMessageTime(Instant.parse("2012-08-22T16:30:00Z"))
                                .setStationId(2)
                                .setStationIcaoCode("YUDO")
                                .setFormat(2)
                                .setType(1)
                                .setRoute(1)
                                .setMessage(readResourceToString("metar-iwxxm-30-message.xml"))
                                .setFileModified(Instant.parse("2012-08-22T15:30:00Z"))
                                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()//
                                        .setXMLNamespace("http://icao.int/iwxxm/3.0")//
                                        .build())
                                .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                .build())
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("IWXXM 3.0 TAF")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("taf-iwxxm-30.xml")//
                        .setFileModified(Instant.parse("2012-08-15T17:00:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                .setMessageTime(Instant.parse("2012-08-15T18:00:00Z"))
                                .setStationId(2)
                                .setStationIcaoCode("YUDO")
                                .setFormat(2)
                                .setType(3)
                                .setRoute(1)
                                .setValidFrom(Instant.parse("2012-08-16T00:00:00Z"))
                                .setValidTo(Instant.parse("2012-08-16T18:00:00Z"))
                                .setMessage(readResourceToString("taf-iwxxm-30-message-1.xml"))
                                .setFileModified(Instant.parse("2012-08-15T17:00:00Z"))
                                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()//
                                        .setXMLNamespace("http://icao.int/iwxxm/3.0")//
                                        .build())
                                .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                .build())
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Space Weather Advisory with GTS heading")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("spacewx-gts-heading-1.xml")//
                        .setFileModified(Instant.parse("2016-11-07T23:30:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                .setMessageTime(Instant.parse("2016-11-08T00:00:00Z"))
                                .setStationId(2)
                                .setStationIcaoCode("YUDO")
                                .setFormat(2)
                                .setType(8)
                                .setRoute(1)
                                .setMessage(readResourceToString("spacewx-gts-heading-1-message-1.xml"))
                                .setHeading("LNXX01 YUDO 110715")
                                .setFileModified(Instant.parse("2016-11-07T23:30:00Z"))
                                .setValidFrom(Instant.parse("2016-11-08T00:00:00Z"))
                                .setValidTo(Instant.parse("2016-11-09T00:00:00Z"))
                                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()//
                                        .setXMLNamespace("http://icao.int/iwxxm/3.0")//
                                        .build())//
                                .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                .build())//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Space Weather Advisory with GTS bulletin")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("spacewx-bulletin-2.xml")//
                        .setFileModified(Instant.parse("2016-11-07T23:30:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                .setMessageTime(Instant.parse("2016-11-08T00:00:00Z"))
                                .setStationId(2)
                                .setStationIcaoCode("YUDO")
                                .setFormat(2)
                                .setType(8)
                                .setRoute(1)
                                .setMessage(readResourceToString("spacewx-bulletin-2-message-1.xml"))
                                .setHeading("LNXX01 YUDO 110715")
                                .setFileModified(Instant.parse("2016-11-07T23:30:00Z"))
                                .setValidFrom(Instant.parse("2016-11-08T00:00:00Z"))
                                .setValidTo(Instant.parse("2016-11-09T00:00:00Z"))
                                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()//
                                        .setXMLNamespace("http://icao.int/iwxxm/3.0")//
                                        .build())//
                                .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                .build())//
                        .build(), //
                AviationMessageArchiverTestCase.builder()//
                        .setName("Invalid Space Weather Advisory")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("spacewx-translation-failed.xml")//
                        .setFileModified(Instant.parse("2016-11-07T23:30:00Z"))//
                        .expectFail()//
                        .build(), //
                AviationMessageArchiverTestCase.builder()//
                        .setName("IWXXM 3.0 VA SIGMET")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("sigmet-iwxxm-30.xml")//
                        .setFileModified(Instant.parse("2018-07-10T11:00:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                .setMessageTime(Instant.parse("2018-07-10T12:00:00Z"))
                                .setStationId(3)
                                .setStationIcaoCode("YUDD")
                                .setFormat(2)
                                .setType(4)
                                .setRoute(1)
                                .setMessage(readResourceToString("sigmet-iwxxm-30-message.xml"))
                                .setValidFrom(Instant.parse("2018-07-10T12:00:00Z"))
                                .setValidTo(Instant.parse("2018-07-10T18:00:00Z"))
                                .setFileModified(Instant.parse("2018-07-10T11:00:00Z"))
                                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()//
                                        .setXMLNamespace("http://icao.int/iwxxm/3.0")//
                                        .build())
                                .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                .build())
                        .build(), //
                AviationMessageArchiverTestCase.builder()//
                        .setName("IWXXM 2023-1 SIGMET")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("sigmet-iwxxm-2023-1.xml")//
                        .setFileModified(Instant.parse("2024-02-02T11:00:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                .setMessageTime(Instant.parse("2024-02-01T09:09:29Z"))
                                .setStationId(3)
                                .setStationIcaoCode("YUDD")
                                .setFormat(2)
                                .setType(4)
                                .setRoute(1)
                                .setMessage(readResourceToString("sigmet-iwxxm-2023-1.xml"))
                                .setValidFrom(Instant.parse("2024-02-01T09:14:02Z"))
                                .setValidTo(Instant.parse("2024-02-01T11:14:02Z"))
                                .setFileModified(Instant.parse("2024-02-02T11:00:00Z"))
                                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()//
                                        .setXMLNamespace("http://icao.int/iwxxm/2023-1")//
                                        .build())
                                .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                .build())
                        .build(), //
                AviationMessageArchiverTestCase.builder()//
                        .setName("IWXXM 2023-1 VA SIGMET")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("sigmet-va-iwxxm-2023-1.xml")//
                        .setFileModified(Instant.parse("2024-02-02T11:00:00Z"))
                        .addArchivedMessages(ArchiveAviationMessage.builder()
                                .setMessageTime(Instant.parse("2024-02-02T13:17:21Z"))
                                .setStationId(3)
                                .setStationIcaoCode("YUDD")
                                .setFormat(2)
                                .setType(4)
                                .setRoute(1)
                                .setMessage(readResourceToString("sigmet-va-iwxxm-2023-1.xml"))
                                .setValidFrom(Instant.parse("2024-02-02T13:21:54Z"))
                                .setValidTo(Instant.parse("2024-02-02T15:21:54Z"))
                                .setFileModified(Instant.parse("2024-02-02T11:00:00Z"))
                                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()//
                                        .setXMLNamespace("http://icao.int/iwxxm/2023-1")//
                                        .build())
                                .setArchivalStatus(ArchivalStatus.ARCHIVED)
                                .build())
                        .build(), //
                AviationMessageArchiverTestCase.builder()//
                        .setName("Discarded TAF")
                        .setProductName("test_taf_bulletin")
                        .setInputFileName("taf-tac-bulletin-discarded.bul")
                        .setFileModified(Instant.parse("2016-11-07T23:30:00Z"))
                        .build()//
        );
    }

    @BeforeEach
    public void setUp() {
        databaseAccessTestUtil = new DatabaseAccessTestUtil(databaseAccess, clock.instant());
        testPostActionRegistry.resetAll();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource
    void test_archival(final AviationMessageArchiverTestCase testCase) {
        skipExcluded(testCase);

        assertFileFlow(testCase);

        if (!testCase.getArchivedMessages().isEmpty()) {
            assertThat(databaseAccessTestUtil.fetchArchiveMessages()).usingRecursiveFieldByFieldElementComparator(archiveMessageComparisonConfiguration)
                    .containsExactlyInAnyOrderElementsOf(testCase.getArchivedMessages());
        } else {
            databaseAccessTestUtil.assertMessagesEmpty();
        }

        if (!testCase.getRejectedMessages().isEmpty()) {
            assertThat(databaseAccessTestUtil.fetchRejectedMessages()).usingRecursiveFieldByFieldElementComparator(archiveMessageComparisonConfiguration)
                    .containsExactlyInAnyOrderElementsOf(testCase.getRejectedMessages());
        } else {
            databaseAccessTestUtil.assertRejectedMessagesEmpty();
        }

        assertPostActionInvocations(Stream.of(testCase));
    }

    private void skipExcluded(final AviationMessageArchiverTestCase testCase) {
        assumeTrue(isIncluded(testCase), "test case excluded");
    }

    private boolean isIncluded(final AviationMessageArchiverTestCase testCase) {
        return INCLUDE_INPUT_FILES.isEmpty() || INCLUDE_INPUT_FILES.contains(testCase.getInputFileName());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void test_all_at_once() throws IOException {
        final List<AviationMessageArchiverTestCase> cases = test_archival()//
                .filter(this::isIncluded)//
                .toList();
        cases.parallelStream().forEach(this::copyFileSetLastModified);
        cases.parallelStream().forEach(this::renameTempFile);
        cases.parallelStream().forEach(this::assertFileOperations);
        cases.parallelStream().forEach(testCase -> {
            if (!testCase.getArchivedMessages().isEmpty()) {
                assertThat(databaseAccessTestUtil.fetchArchiveMessages())//
                        .usingRecursiveFieldByFieldElementComparator(archiveMessageComparisonConfiguration)//
                        .containsAll(testCase.getArchivedMessages());
            }
            if (!testCase.getRejectedMessages().isEmpty()) {
                assertThat(databaseAccessTestUtil.fetchRejectedMessages())//
                        .usingRecursiveFieldByFieldElementComparator(archiveMessageComparisonConfiguration)//
                        .containsAll(testCase.getRejectedMessages());
            }
        });
        assertPostActionInvocations(cases.stream());
    }

    private void assertFileFlow(final AviationMessageArchiverTestCase testCase) {
        copyFileSetLastModified(testCase);
        renameTempFile(testCase);
        assertFileOperations(testCase);
    }

    private void copyFileSetLastModified(final AviationMessageArchiverTestCase testCase) {
        final Path tempFile = testCase.getTempFile(testCase.getProduct(aviationProducts));
        try {
            Files.copy(testCase.getInputFile(), tempFile);
            Files.setLastModifiedTime(tempFile, FileTime.from(testCase.getFileModified()));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void renameTempFile(final AviationMessageArchiverTestCase testCase) {
        final AviationProduct product = testCase.getProduct(aviationProducts);
        try {
            Files.move(testCase.getTempFile(product), testCase.getTestFile(product));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertFileOperations(final AviationMessageArchiverTestCase testCase) {
        final AviationProduct product = testCase.getProduct(aviationProducts);
        final Path testFile = testCase.getTestFile(product);

        if (testCase.isUnhandled()) {
            assertThat(testFile).exists();
        } else {
            try {
                testCase.assertInputAndOutputFilesEquals(product);
            } catch (final InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
            assertThat(testFile).doesNotExist();
        }
    }

    private void assertPostActionInvocations(final Stream<AviationMessageArchiverTestCase> testCases) {
        assertThat(TestablePostActionInvocation.fromInvocations(testPostActionRegistry.get("test").getInvocations().stream()))
                .usingRecursiveFieldByFieldElementComparator(postActionInvocationComparisonConfiguration)
                .containsExactlyInAnyOrderElementsOf(TestablePostActionInvocation.fromTestCases(testCases));
    }

    private enum MessageContentPredicate implements BiPredicate<String, String> {
        INSTANCE;

        @Override
        public boolean test(final String left, final String right) {
            if (left.equals(right)) {
                return true;
            } else {
                try {
                    XMLUnit.setIgnoreWhitespace(true);
                    XMLUnit.setIgnoreComments(true);
                    return XMLUnit.compareXML(left, right).identical();
                } catch (final IOException | SAXException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
    }

    @FreeBuilder
    static abstract class AviationMessageArchiverTestCase {
        private static final int WAIT_MILLIS = 100;
        private static final int TIMEOUT_MILLIS = 3000;

        AviationMessageArchiverTestCase() {
        }

        public static AviationMessageArchiverTestCase.Builder builder() {
            return new Builder();
        }

        @SuppressWarnings("ConstantConditions")
        private static byte[] readResourceToByteArray(final String filename) throws IOException {
            return Resources.toByteArray(AviationMessageArchiverTest.class.getResource(filename));
        }

        @Override
        public String toString() {
            return getName();
        }

        public abstract String getName();

        public abstract String getInputFileName();

        public abstract Instant getFileModified();

        public abstract List<ArchiveAviationMessage> getArchivedMessages();

        public abstract List<ArchiveAviationMessage> getRejectedMessages();

        public abstract boolean isExpectFail();

        public abstract boolean isUnhandled();

        public int getFormat() {
            return Streams.concat(getArchivedMessages().stream(), getRejectedMessages().stream())//
                    .map(ArchiveAviationMessage::getFormat)//
                    .findFirst()//
                    .orElse(1);
        }

        public Path getInputFile() throws URISyntaxException {
            final URL resource = requireNonNull(AviationMessageArchiverTest.class.getResource(getInputFileName()));
            final Path path = Paths.get(resource.toURI());
            assertThat(path).exists();
            return path;
        }

        public Path getTestFile(final AviationProduct product) {
            return product.getInputDir().resolve(getInputFileName());
        }

        public Path getTempFile(final AviationProduct product) {
            final Path testFile = getTestFile(product);
            return testFile.resolveSibling(testFile.getFileName() + TEMP_FILE_SUFFIX);
        }

        public abstract String getProductName();

        public AviationProduct getProduct(final Map<String, AviationProduct> aviationProducts) {
            final String productName = getProductName();
            return requireNonNull(aviationProducts.get(productName), productName);
        }

        public void assertInputAndOutputFilesEquals(final AviationProduct product) throws InterruptedException, IOException {
            final byte[] expectedContent = readResourceToByteArray(getInputFileName());
            final Path expectedOutputDir = isExpectFail() ? product.getFailDir() : product.getArchiveDir();
            final Path outputFile = waitUntilFileExists(expectedOutputDir, getInputFileName());

            assertThat(outputFile)//
                    .exists()//
                    .hasBinaryContent(expectedContent);
        }

        @Nullable
        private Path waitUntilFileExists(final Path expectedOutputDir, final String expectedFileBaseName) throws InterruptedException, IOException {
            final String expectedFileBaseNameWithSeparator = expectedFileBaseName + ".";
            long totalWaitTime = 0;
            final BiPredicate<Path, BasicFileAttributes> matcher = (path, basicFileAttributes) -> basicFileAttributes.isRegularFile() //
                    && Optional.ofNullable(path.getFileName())//
                    .map(fileName -> fileName.toString().startsWith(expectedFileBaseNameWithSeparator))//
                    .orElse(false);
            @Nullable
            Path concretePath = null;
            while (totalWaitTime < TIMEOUT_MILLIS && concretePath == null) {
                concretePath = Files.find(expectedOutputDir, 1, matcher).findAny().orElse(null);
                Thread.sleep(WAIT_MILLIS);
                totalWaitTime += WAIT_MILLIS;
            }
            return concretePath;
        }

        public static class Builder extends AviationMessageArchiverTest_AviationMessageArchiverTestCase_Builder {
            Builder() {
                setExpectFail(false);
                setUnhandled(false);
            }

            public Builder expectFail() {
                return super.setExpectFail(true);
            }

            public Builder expectUnhandled() {
                return super.setUnhandled(true);
            }
        }
    }

    private record TestablePostActionInvocation(
            FileReference fileReference,
            Optional<Instant> fileModified,
            ArchiveAviationMessage archiveAviationMessage
    ) {
        private TestablePostActionInvocation {
            requireNonNull(fileReference, "fileReference");
            requireNonNull(fileModified, "fileModified");
            requireNonNull(archiveAviationMessage, "archiveAviationMessage");
        }

        private static List<TestablePostActionInvocation> fromTestCases(final Stream<AviationMessageArchiverTestCase> testCases) {
            return testCases
                    .flatMap(testCase -> Stream.concat(
                                    testCase.getArchivedMessages().stream(),
                                    testCase.getRejectedMessages().stream())
                            .map(archiveAviationMessage -> from(testCase, archiveAviationMessage)))
                    .toList();
        }

        public static List<TestablePostActionInvocation> fromInvocations(final Stream<TestPostAction.Invocation> invocations) {
            return invocations
                    .map(TestablePostActionInvocation::from)
                    .toList();
        }

        private static TestablePostActionInvocation from(final AviationMessageArchiverTestCase testCase, final ArchiveAviationMessage archiveAviationMessage) {
            return new TestablePostActionInvocation(
                    FileReference.create(testCase.getProductName(), testCase.getInputFileName()),
                    Optional.of(testCase.getFileModified()),
                    archiveAviationMessage
            );
        }

        private static TestablePostActionInvocation from(final TestPostAction.Invocation invocation) {
            return new TestablePostActionInvocation(
                    invocation.context().getInputMessage().getFileMetadata().getFileReference(),
                    invocation.context().getInputMessage().getFileMetadata().getFileModified(),
                    invocation.message()
            );
        }
    }
}
