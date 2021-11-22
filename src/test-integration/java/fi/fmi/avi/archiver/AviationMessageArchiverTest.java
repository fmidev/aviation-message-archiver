package fi.fmi.avi.archiver;

import com.google.common.collect.Streams;
import com.google.common.io.Resources;
import fi.fmi.avi.archiver.config.ConversionConfig;
import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.database.DatabaseAccessTestUtil;
import fi.fmi.avi.archiver.initializing.AviationProductsHolder;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageIWXXMDetails;
import fi.fmi.avi.archiver.message.ProcessingResult;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.custommonkey.xmlunit.XMLUnit;
import org.inferred.freebuilder.FreeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest({"auto.startup=false", "testclass.name=fi.fmi.avi.archiver.AviationMessageArchiverTest"})
@Sql(scripts = {"classpath:/schema-h2.sql", "classpath:/h2-data/avidb_test_content.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/h2-data/avidb_cleanup_test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@ContextConfiguration(classes = {AviationMessageArchiver.class, TestConfig.class, ConversionConfig.class},//
        loader = AnnotationConfigContextLoader.class,//
        initializers = {ConfigDataApplicationContextInitializer.class})
class AviationMessageArchiverTest {

    private static final String TEMP_FILE_SUFFIX = ".tmp";

    @Autowired
    private AviationProductsHolder aviationProductsHolder;
    @Autowired
    private DatabaseAccess databaseAccess;
    @Autowired
    private Clock clock;

    private DatabaseAccessTestUtil databaseAccessTestUtil;
    private final RecursiveComparisonConfiguration archiveMessageComparisonConfiguration = RecursiveComparisonConfiguration.builder()
            .withEqualsForFields(new MessageContentPredicate(), "message")
            .build();

    @BeforeEach
    public void setUp() {
        databaseAccessTestUtil = new DatabaseAccessTestUtil(databaseAccess, clock.instant());
    }

    static Stream<AviationMessageArchiverTestCase> test_file_flow() {
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
                                .build()
                        )
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
                                .build()
                        )
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Empty file fails")//
                        .setProductName("test_taf")//
                        .setInputFileName("empty.txt")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Non-convertible message fails")//
                        .setProductName("test_taf")//
                        .setInputFileName("inconvertible.txt")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Binary data fails (TAC)")//
                        .setProductName("test_taf")//
                        .setInputFileName("binary.txt")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("TAF bulletin with binary data fails (TAC)")//
                        .setProductName("test_taf_bulletin")//
                        .setInputFileName("taf-tac-bulletin-binary.bul")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Binary data fails (IWXXM)")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("binary.xml")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("TAF with binary data fails (IWXXM)")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("taf-binary-8.xml")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Only GTS heading")//
                        .setProductName("test_taf")//
                        .setInputFileName("gts-heading.txt")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("NIL TAC bulletin")//
                        .setProductName("test_taf_bulletin")//
                        .setInputFileName("nil-bulletin.bul")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                        .expectFail()//
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("Empty collect document")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("empty-collect.xml")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
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
                                .build()
                        )
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
                                        .build(),
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
                                        .build()
                        )
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
                                        .build(),
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
                                        .build(),
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
                                        .build(),
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
                                        .build()
                        )
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
                                        .build(),
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
                                        .build()
                        )
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
                                .build()
                        ).addRejectedMessages(ArchiveAviationMessage.builder()
                                .setMessageTime(Instant.parse("2020-05-16T00:00:00Z"))
                                .setStationIcaoCode("XYZW")
                                .setFormat(1)
                                .setType(3)
                                .setRoute(1)
                                .setMessage("TAF XYZW 160000Z NIL=")
                                .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                                .setHeading("FTYU31 YUDO 160000")
                                .setProcessingResult(ProcessingResult.UNKNOWN_STATION_ICAO_CODE)
                                .build())
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("TAF TAC GTS Bulletin cut off")//
                        .setProductName("test_taf_bulletin")//
                        .setInputFileName("taf-tac-bulletin-cut-off.bul")//
                        .setFileModified(Instant.parse("2020-05-15T00:00:00Z"))
                        .expectFail()
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
                                .setMessage(fileContent("taf-1-message.xml"))
                                .setValidFrom(Instant.parse("2017-07-30T12:00:00Z"))
                                .setValidTo(Instant.parse("2017-07-31T12:00:00Z"))
                                .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                        .setXMLNamespace("http://icao.int/iwxxm/2.1")
                                        .build())
                                .build()
                        )
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
                                .setMessage(fileContent("taf-rejected-6-message.xml"))
                                .setValidFrom(Instant.parse("2017-07-30T12:00:00Z"))
                                .setValidTo(Instant.parse("2017-07-31T12:00:00Z"))
                                .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                                .setProcessingResult(ProcessingResult.UNKNOWN_STATION_ICAO_CODE)
                                .build()
                        )
                        .build(),//
                // TODO Fails due to missing namespace declarations in TAF elements
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
                                        .setMessage(fileContent("taf-bulletin-2-message-1.xml"))
                                        .setValidFrom(Instant.parse("2017-07-30T12:00:00Z"))
                                        .setValidTo(Instant.parse("2017-07-31T12:00:00Z"))
                                        .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/2.1")
                                                .setCollectIdentifier("A_LTFI31EFKL301115_C_EFKL_201902011315--.xml")
                                                .build())
                                        .build(),
                                ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2017-07-27T11:37:00Z"))
                                        .setStationId(4)
                                        .setStationIcaoCode("EETN")
                                        .setFormat(2)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage(fileContent("taf-bulletin-2-message-2.xml"))
                                        .setValidFrom(Instant.parse("2017-07-27T12:00:00Z"))
                                        .setValidTo(Instant.parse("2017-07-28T12:00:00Z"))
                                        .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/2.1")
                                                .setCollectIdentifier("A_LTFI31EFKL301115_C_EFKL_201902011315--.xml")
                                                .build())
                                        .build()
                        )
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
                                        .setMessage(fileContent("taf-bulletin-3-message-1.xml"))
                                        .setValidFrom(Instant.parse("2017-07-30T12:00:00Z"))
                                        .setValidTo(Instant.parse("2017-07-31T12:00:00Z"))
                                        .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/2.1")
                                                .setCollectIdentifier("A_LTFI31EFKL301115_C_EFKL_201902011315--.xml")
                                                .build())
                                        .build(),
                                ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2017-07-27T11:37:00Z"))
                                        .setStationId(4)
                                        .setStationIcaoCode("EETN")
                                        .setFormat(2)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage(fileContent("taf-bulletin-3-message-2.xml"))
                                        .setValidFrom(Instant.parse("2017-07-27T12:00:00Z"))
                                        .setValidTo(Instant.parse("2017-07-28T12:00:00Z"))
                                        .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/2.1")
                                                .setCollectIdentifier("A_LTFI31EFKL301115_C_EFKL_201902011315--.xml")
                                                .build())
                                        .build()
                        )
                        .build(),//
                AviationMessageArchiverTestCase.builder()//
                        .setName("IWXXM TAF Collect bulletin cut off")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("taf-bulletin-7-cut-off.xml")//
                        .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                        .expectFail()
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
                                .setMessage(fileContent("taf-gts-heading-4-message-1.xml"))
                                .setValidFrom(Instant.parse("2017-07-30T12:00:00Z"))
                                .setValidTo(Instant.parse("2017-07-31T12:00:00Z"))
                                .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                                .setHeading("LTFI31 EFKL 301115")
                                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                        .setXMLNamespace("http://icao.int/iwxxm/2.1")
                                        .build())
                                .build()
                        )
                        .build(),//
                // TODO Fails due to missing namespace declarations in TAF elements
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
                                        .setMessage(fileContent("taf-gts-heading-bulletin-5-message-1.xml"))
                                        .setValidFrom(Instant.parse("2017-07-30T12:00:00Z"))
                                        .setValidTo(Instant.parse("2017-07-31T12:00:00Z"))
                                        .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                                        .setHeading("LTFI31 EFKL 301115")
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/2.1")
                                                .setCollectIdentifier("A_LTFI31EFKL301115_C_EFKL_201902011315--.xml")
                                                .build())
                                        .build(),
                                ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2017-07-27T11:37:00Z"))
                                        .setStationId(4)
                                        .setStationIcaoCode("EETN")
                                        .setFormat(2)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage(fileContent("taf-gts-heading-bulletin-5-message-2.xml"))
                                        .setValidFrom(Instant.parse("2017-07-27T12:00:00Z"))
                                        .setValidTo(Instant.parse("2017-07-28T12:00:00Z"))
                                        .setFileModified(Instant.parse("2017-07-30T10:30:00Z"))
                                        .setHeading("LTFI31 EFKL 301115")
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/2.1")
                                                .setCollectIdentifier("A_LTFI31EFKL301115_C_EFKL_201902011315--.xml")
                                                .build())
                                        .build()
                        )
                        .build(),
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
                                        .setMessage(fileContent("taf-gts-bulletin-collect-9-message-1.xml"))
                                        .setFileModified(Instant.parse("2021-10-21T14:00:00Z"))
                                        .setHeading("LCFI32 EFKL 211400")
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/3.0")
                                                .setCollectIdentifier("A_LCFI32EFKL211400_C_EFKL_20211021140000.xml")
                                                .build())
                                        .build(),
                                ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2021-10-21T14:36:00Z"))
                                        .setStationId(6)
                                        .setStationIcaoCode("EFKK")
                                        .setFormat(2)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage(fileContent("taf-gts-bulletin-collect-9-message-2.xml"))
                                        .setValidFrom(Instant.parse("2021-10-21T15:00:00Z"))
                                        .setValidTo(Instant.parse("2021-10-22T00:00:00Z"))
                                        .setFileModified(Instant.parse("2021-10-21T14:00:00Z"))
                                        .setHeading("LCFI32 EFKL 211400")
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/3.0")
                                                .setCollectIdentifier("A_LCFI32EFKL211400_C_EFKL_20211021140000.xml")
                                                .build())
                                        .build(),
                                ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2021-10-21T14:37:00Z"))
                                        .setStationId(7)
                                        .setStationIcaoCode("EFPO")
                                        .setFormat(2)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage(fileContent("taf-gts-bulletin-collect-9-message-3.xml"))
                                        .setValidFrom(Instant.parse("2021-10-21T15:00:00Z"))
                                        .setValidTo(Instant.parse("2021-10-21T18:00:00Z"))
                                        .setFileModified(Instant.parse("2021-10-21T14:00:00Z"))
                                        .setHeading("LCFI32 EFKL 211400")
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/3.0")
                                                .setCollectIdentifier("A_LCFI32EFKL211400_C_EFKL_20211021140000.xml")
                                                .build())
                                        .build()
                        )
                        .build(),
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
                                        .setMessage(fileContent("taf-gts-bulletin-collect-10-message-1.xml"))
                                        .setFileModified(Instant.parse("2021-10-21T14:00:00Z"))
                                        .setHeading("LCFI32 EFKL 211400")
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/3.0")
                                                .setCollectIdentifier("A_LCFI32EFKL211400_C_EFKL_20211021140000.xml")
                                                .build())
                                        .build(),
                                ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2021-10-21T14:36:00Z"))
                                        .setStationId(6)
                                        .setStationIcaoCode("EFKK")
                                        .setFormat(2)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage(fileContent("taf-gts-bulletin-collect-10-message-2.xml"))
                                        .setValidFrom(Instant.parse("2021-10-21T15:00:00Z"))
                                        .setValidTo(Instant.parse("2021-10-22T00:00:00Z"))
                                        .setFileModified(Instant.parse("2021-10-21T14:00:00Z"))
                                        .setHeading("LCFI32 EFKL 211400")
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/3.0")
                                                .setCollectIdentifier("A_LCFI32EFKL211400_C_EFKL_20211021140000.xml")
                                                .build())
                                        .build(),
                                ArchiveAviationMessage.builder()
                                        .setMessageTime(Instant.parse("2021-10-21T14:37:00Z"))
                                        .setStationId(7)
                                        .setStationIcaoCode("EFPO")
                                        .setFormat(2)
                                        .setType(3)
                                        .setRoute(1)
                                        .setMessage(fileContent("taf-gts-bulletin-collect-10-message-3.xml"))
                                        .setValidFrom(Instant.parse("2021-10-21T15:00:00Z"))
                                        .setValidTo(Instant.parse("2021-10-21T18:00:00Z"))
                                        .setFileModified(Instant.parse("2021-10-21T14:00:00Z"))
                                        .setHeading("LCFI32 EFKL 211400")
                                        .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                                .setXMLNamespace("http://icao.int/iwxxm/3.0")
                                                .setCollectIdentifier("A_LCFI32EFKL211400_C_EFKL_20211021140000.xml")
                                                .build())
                                        .build()
                        )
                        .build(),
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
                                .setMessage(fileContent("metar-iwxxm-30-message.xml"))
                                .setFileModified(Instant.parse("2012-08-22T15:30:00Z"))
                                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                        .setXMLNamespace("http://icao.int/iwxxm/3.0")
                                        .build())
                                .build()
                        )
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
                                .setMessage(fileContent("taf-iwxxm-30-message-1.xml"))
                                .setFileModified(Instant.parse("2012-08-15T17:00:00Z"))
                                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                        .setXMLNamespace("http://icao.int/iwxxm/3.0")
                                        .build())
                                .build()
                        )
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
                                .setMessage(fileContent("spacewx-gts-heading-1-message-1.xml"))
                                .setHeading("LNXX01 YUDO 110715")
                                .setFileModified(Instant.parse("2016-11-07T23:30:00Z"))
                                .setValidFrom(Instant.parse("2016-11-08T00:00:00Z"))
                                .setValidTo(Instant.parse("2016-11-09T06:00:00Z"))
                                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                        .setXMLNamespace("http://icao.int/iwxxm/3.0")
                                        .build())
                                .build()
                        )
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
                                .setMessage(fileContent("spacewx-bulletin-2-message-1.xml"))
                                .setHeading("LNXX01 YUDO 110715")
                                .setFileModified(Instant.parse("2016-11-07T23:30:00Z"))
                                .setValidFrom(Instant.parse("2016-11-08T00:00:00Z"))
                                .setValidTo(Instant.parse("2016-11-09T06:00:00Z"))
                                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                        .setXMLNamespace("http://icao.int/iwxxm/3.0")
                                        .build())
                                .build()
                        )
                        .build(),
                AviationMessageArchiverTestCase.builder()//
                        .setName("Invalid Space Weather Advisory")//
                        .setProductName("test_iwxxm")//
                        .setInputFileName("spacewx-translation-failed.xml")//
                        .setFileModified(Instant.parse("2016-11-07T23:30:00Z"))
                        .expectFail()
                        .build(),
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
                                .setMessage(fileContent("sigmet-iwxxm-30-message.xml"))
                                .setValidFrom(Instant.parse("2018-07-10T12:00:00Z"))
                                .setValidTo(Instant.parse("2018-07-10T18:00:00Z"))
                                .setFileModified(Instant.parse("2018-07-10T11:00:00Z"))
                                .setIWXXMDetails(ArchiveAviationMessageIWXXMDetails.builder()
                                        .setXMLNamespace("http://icao.int/iwxxm/3.0")
                                        .build())
                                .build()
                        )
                        .build()////
        );
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource
    void test_file_flow(final AviationMessageArchiverTestCase testCase) throws IOException, InterruptedException, URISyntaxException {
        final AviationProductsHolder.AviationProduct product = testCase.getProduct(aviationProductsHolder);
        final Path testFile = Paths.get(product.getInputDir().getPath() + "/" + testCase.getInputFileName());
        final Path tempFile = Paths.get(testFile + TEMP_FILE_SUFFIX);
        Files.copy(testCase.getInputFile(), tempFile);
        Files.setLastModifiedTime(tempFile, FileTime.from(testCase.getFileModified()));
        Files.move(tempFile, testFile);

        testCase.assertInputAndOutputFilesEquals(product, clock.millis());

        if (!testCase.getArchivedMessages().isEmpty()) {
            assertThat(databaseAccessTestUtil.fetchArchiveMessages())
                    .usingRecursiveFieldByFieldElementComparator(archiveMessageComparisonConfiguration)
                    .containsExactlyInAnyOrderElementsOf(testCase.getArchivedMessages());
        } else {
            databaseAccessTestUtil.assertMessagesEmpty();
        }

        if (!testCase.getRejectedMessages().isEmpty()) {
            assertThat(databaseAccessTestUtil.fetchRejectedMessages(testCase.getFormat()))
                    .usingRecursiveFieldByFieldElementComparator(archiveMessageComparisonConfiguration)
                    .containsExactlyInAnyOrderElementsOf(testCase.getRejectedMessages());
        } else {
            databaseAccessTestUtil.assertRejectedMessagesEmpty();
        }

        assertThat(testFile).doesNotExist();
    }

    @FreeBuilder
    static abstract class AviationMessageArchiverTestCase {
        private static final int WAIT_MILLIS = 100;
        private static final int TIMEOUT_MILLIS = 1000;

        AviationMessageArchiverTestCase() {
        }

        public static AviationMessageArchiverTestCase.Builder builder() {
            return new Builder();
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

        public abstract boolean getExpectFail();

        public int getFormat() {
            return Streams.concat(getArchivedMessages().stream(), getRejectedMessages().stream())
                    .map(ArchiveAviationMessage::getFormat)
                    .findFirst().orElse(1);
        }

        public Path getInputFile() throws URISyntaxException {
            final URL resource = requireNonNull(AviationMessageArchiverTest.class.getResource(getInputFileName()));
            final Path path = Paths.get(resource.toURI());
            assertThat(path).exists();
            return path;
        }

        public abstract String getProductName();

        public AviationProductsHolder.AviationProduct getProduct(final AviationProductsHolder holder) {
            final String productName = getProductName();
            return requireNonNull(holder.getProducts().get(productName), productName);
        }

        public void assertInputAndOutputFilesEquals(final AviationProductsHolder.AviationProduct product, final long timestamp) throws InterruptedException {
            final byte[] expectedContent = fileContentAsByteArray(getInputFileName());
            final File expectedOutputFile = new File((getExpectFail() ? product.getFailDir() :
                    product.getArchiveDir()) + "/" + getInputFileName() + "." + timestamp);
            waitUntilFileExists(expectedOutputFile);

            assertThat(expectedOutputFile).exists();
            assertThat(expectedOutputFile).hasBinaryContent(expectedContent);
        }

        private void waitUntilFileExists(final File expectedOutputFile) throws InterruptedException {
            long totalWaitTime = 0;
            while (!expectedOutputFile.exists() && totalWaitTime < TIMEOUT_MILLIS) {
                Thread.sleep(WAIT_MILLIS);
                totalWaitTime += WAIT_MILLIS;
            }
        }

        public static class Builder extends AviationMessageArchiverTest_AviationMessageArchiverTestCase_Builder {

            public Builder() {
                setExpectFail(false);
            }

            public Builder expectFail() {
                return super.setExpectFail(true);
            }
        }
    }

    private static String fileContent(final String filename) {
        try {
            return Resources.toString(AviationMessageArchiverTest.class.getResource(filename), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] fileContentAsByteArray(final String filename) {
        try {
            return Resources.toByteArray(AviationMessageArchiverTest.class.getResource(filename));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class MessageContentPredicate implements BiPredicate<String, String> {
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

}
