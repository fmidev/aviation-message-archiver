package fi.fmi.avi.archiver.message.processor.populator;

import fi.fmi.avi.archiver.AviationMessageArchiver;
import fi.fmi.avi.archiver.config.ConversionConfig;
import fi.fmi.avi.archiver.config.MessagePopulatorConfig;
import fi.fmi.avi.archiver.config.TestConfig;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.InputAndArchiveAviationMessage;
import fi.fmi.avi.archiver.message.InputAndArchiveAviationMessageTests;
import fi.fmi.avi.archiver.message.processor.MessageProcessorTestHelper;
import fi.fmi.avi.archiver.message.processor.MessageProcessorTestHelper.FormatId;
import fi.fmi.avi.archiver.message.processor.MessageProcessorTestHelper.RouteId;
import fi.fmi.avi.archiver.message.processor.MessageProcessorTestHelper.StationId;
import fi.fmi.avi.archiver.message.processor.MessageProcessorTestHelper.TypeId;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;
import fi.fmi.avi.util.BulletinHeadingDecoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest({"auto.startup=false", "testclass.name=fi.fmi.avi.archiver.message.processor.populator.ConditionalMessagePopulatorIntegrationTest"})
@ContextConfiguration(classes = {AviationMessageArchiver.class, TestConfig.class, ConversionConfig.class},
        loader = AnnotationConfigContextLoader.class,
        initializers = {ConfigDataApplicationContextInitializer.class})
@Sql(scripts = {"classpath:/fi/fmi/avi/avidb/schema/h2/schema-h2.sql", "classpath:/h2-data/avidb_test_content.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/h2-data/avidb_cleanup_test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@ActiveProfiles({"integration-test", "ConditionalMessagePopulatorIntegrationTest"})
class ConditionalMessagePopulatorIntegrationTest {
    private static final InputBulletinHeading INPUT_BULLETIN_HEADING = InputBulletinHeading.builder()
            .setBulletinHeadingString("FNXX01 YUDO 120345")
            .setBulletinHeading(BulletinHeadingDecoder.decode("FNXX01 YUDO 120345", ConversionHints.EMPTY))
            .build();
    private static final ZonedDateTime MESSAGE_TIME = ZonedDateTime.parse("2001-02-03T04:05:06.019Z");
    private static final ZonedDateTime VALID_FROM = ZonedDateTime.parse("2001-02-03T04:00:00Z");
    private static final ZonedDateTime VALID_TO = ZonedDateTime.parse("2001-02-03T05:00:00Z");
    private static final ArchiveAviationMessage EXPECTED_TEMPLATE = ArchiveAviationMessage.builder()
            .setRoute(RouteId.TEST.getId())
            .setFormat(FormatId.TAC.getId())
            .setType(TypeId.METAR.getId())
            .setMessageTime(MESSAGE_TIME.toInstant())
            .setValidFrom(VALID_FROM.toInstant())
            .setValidTo(VALID_TO.toInstant())
            .setStationIcaoCode(StationId.YUDO.getIcaoCode())
            .setStationId(StationId.YUDO.getId())
            .setMessage("Original message")
            .setHeading(INPUT_BULLETIN_HEADING.getBulletinHeadingString())
            .build();
    private static final String TEST_TAC_PRODUCT_ID = "testTAC";
    private static final String TEST_IWXXM_PRODUCT_ID = "testIWXXM";
    private static final String TEST2_TAC_PRODUCT_ID = "test2TAC";
    private static final String TEST2_IWXXM_PRODUCT_ID = "test2IWXXM";

    @Autowired
    private MessagePopulatorConfig.MessagePopulationIntegrationService messagePopulationIntegrationService;
    @Autowired
    private MessageProcessorTestHelper messageProcessorTestHelper;

    private InputAviationMessage inputTemplate;

    @BeforeEach
    void setUp() {
        final InputAviationMessage.Builder builder = InputAviationMessage.builder()
                .setGtsBulletinHeading(INPUT_BULLETIN_HEADING)
                .setMessage(GenericAviationWeatherMessageImpl.builder()
                        .setIssueTime(PartialOrCompleteTimeInstant.of(MESSAGE_TIME))
                        .setValidityTime(PartialOrCompleteTimePeriod.builder()
                                .setStartTime(PartialOrCompleteTimeInstant.of(VALID_FROM))
                                .setEndTime(PartialOrCompleteTimeInstant.of(VALID_TO))
                                .build())
                        .setOriginalMessage("Original message")
                        .setMessageType(MessageType.METAR)
                        .setMessageFormat(FormatId.TAC.getFormat())
                        .putLocationIndicators(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME, StationId.YUDO.getIcaoCode())
                        .build());
        inputTemplate = messageProcessorTestHelper.setProductId(TEST_TAC_PRODUCT_ID, builder).build();
    }

    private List<InputAndArchiveAviationMessage> populate(final List<InputAviationMessage> inputMessages) {
        return messagePopulationIntegrationService.populateMessages(inputMessages, new MessageHeaders(Collections.emptyMap()))
                .getPayload();
    }

    @Test
    void testActivationOnProductIdentifierAndMessageType() {
        final List<InputAviationMessage> inputMessages = Arrays.asList(
                messageProcessorTestHelper.setProductId(TEST_TAC_PRODUCT_ID, inputTemplate.toBuilder()).build(),
                messageProcessorTestHelper.setProductId(TEST_IWXXM_PRODUCT_ID, inputTemplate.toBuilder()).build(),
                messageProcessorTestHelper.setProductId(TEST2_TAC_PRODUCT_ID, inputTemplate.toBuilder()).build(),
                messageProcessorTestHelper.setProductId(TEST2_IWXXM_PRODUCT_ID, inputTemplate.toBuilder()).build());

        final List<InputAndArchiveAviationMessage> result = populate(inputMessages);

        final List<InputAndArchiveAviationMessage> expected = InputAndArchiveAviationMessageTests.createList(inputMessages,
                messageProcessorTestHelper.setProductId(TEST_TAC_PRODUCT_ID, EXPECTED_TEMPLATE.toBuilder())
                        .setMessage("Original message; is testTAC or test2TAC; possible validTo")
                        .build(),
                messageProcessorTestHelper.setProductId(TEST_IWXXM_PRODUCT_ID, EXPECTED_TEMPLATE.toBuilder())
                        .setMessage("Original message; is testIWXXM and METAR; possible validTo")
                        .build(),
                messageProcessorTestHelper.setProductId(TEST2_TAC_PRODUCT_ID, EXPECTED_TEMPLATE.toBuilder())
                        .setMessage("Original message; is testTAC or test2TAC; possible validTo")
                        .build(),
                messageProcessorTestHelper.setProductId(TEST2_IWXXM_PRODUCT_ID, EXPECTED_TEMPLATE.toBuilder())
                        .setMessage("Original message; possible validTo")
                        .build());
        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void testActivateOnPatternAndNegatedCollection() {
        final List<InputAviationMessage> inputMessages = Arrays.asList(
                messageProcessorTestHelper.setProductId(TEST_TAC_PRODUCT_ID, inputTemplate.toBuilder())
                        .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)
                                .setMessageType(TypeId.METAR.getType())
                                .build())
                        .build(),
                messageProcessorTestHelper.setProductId(TEST_IWXXM_PRODUCT_ID, inputTemplate.toBuilder())
                        .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)
                                .setMessageType(TypeId.SPECI.getType())
                                .build())
                        .build(),
                messageProcessorTestHelper.setProductId(TEST2_TAC_PRODUCT_ID, inputTemplate.toBuilder())
                        .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)
                                .setMessageType(TypeId.TAF.getType())
                                .build())
                        .build(),
                messageProcessorTestHelper.setProductId(TEST2_IWXXM_PRODUCT_ID, inputTemplate.toBuilder())
                        .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)
                                .setMessageType(TypeId.TAF.getType())
                                .build())
                        .build(),
                messageProcessorTestHelper.setProductId(TEST_IWXXM_PRODUCT_ID, inputTemplate.toBuilder())
                        .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)
                                .setMessageType(TypeId.SWX.getType())
                                .build())
                        .build());

        final List<InputAndArchiveAviationMessage> result = populate(inputMessages);

        final List<InputAndArchiveAviationMessage> expected = InputAndArchiveAviationMessageTests.createList(inputMessages,
                messageProcessorTestHelper.setProductId(TEST_TAC_PRODUCT_ID, EXPECTED_TEMPLATE.toBuilder())
                        .setType(TypeId.METAR.getId())
                        .setMessage("Original message; is testTAC or test2TAC; possible validTo")
                        .build(),
                messageProcessorTestHelper.setProductId(TEST_IWXXM_PRODUCT_ID, EXPECTED_TEMPLATE.toBuilder())
                        .setType(TypeId.SPECI.getId())
                        .setMessage("Original message; possible validTo")
                        .build(),
                messageProcessorTestHelper.setProductId(TEST2_TAC_PRODUCT_ID, EXPECTED_TEMPLATE.toBuilder())
                        .setType(TypeId.TAF.getId())
                        .setMessage("Original message; is testTAC or test2TAC; possible validTo")
                        .build(),
                messageProcessorTestHelper.setProductId(TEST2_IWXXM_PRODUCT_ID, EXPECTED_TEMPLATE.toBuilder())
                        .setType(TypeId.TAF.getId())
                        .setMessage("Original message; is IWXXM and not METAR/SPECI; possible validTo")
                        .build(),
                messageProcessorTestHelper.setProductId(TEST_IWXXM_PRODUCT_ID, EXPECTED_TEMPLATE.toBuilder())
                        .setType(TypeId.SWX.getId())
                        .setMessage("Original message; is IWXXM and not METAR/SPECI; possible validTo")
                        .build());
        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void testActivateOnEmptyValue() {
        final List<InputAviationMessage> inputMessages = Collections.singletonList(
                messageProcessorTestHelper.setProductId(TEST2_IWXXM_PRODUCT_ID, inputTemplate.toBuilder())
                        .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)
                                .setMessageType(TypeId.METAR.getType())
                                .clearValidityTime()
                                .build())
                        .build());

        final List<InputAndArchiveAviationMessage> result = populate(inputMessages);

        final List<InputAndArchiveAviationMessage> expected = InputAndArchiveAviationMessageTests.createList(inputMessages,
                messageProcessorTestHelper.setProductId(TEST2_IWXXM_PRODUCT_ID, EXPECTED_TEMPLATE.toBuilder())
                        .setType(TypeId.METAR.getId())
                        .clearValidFrom()
                        .clearValidTo()
                        .setMessage("Original message; no validFrom; possible validTo")
                        .build());
        assertThat(result).containsExactlyElementsOf(expected);
    }
}
