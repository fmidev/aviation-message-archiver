package fi.fmi.avi.archiver.message.processor.postaction;

import fi.fmi.avi.archiver.AviationMessageArchiver;
import fi.fmi.avi.archiver.config.ConversionConfig;
import fi.fmi.avi.archiver.config.TestConfig;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;
import fi.fmi.avi.archiver.logging.model.FileProcessingStatisticsImpl;
import fi.fmi.avi.archiver.logging.model.LoggingContext;
import fi.fmi.avi.archiver.logging.model.LoggingContextImpl;
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
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest({"auto.startup=false", "testclass.name=fi.fmi.avi.archiver.message.processor.postaction.ConditionalPostActionIntegrationTest"})
@ContextConfiguration(classes = {AviationMessageArchiver.class, TestConfig.class, ConversionConfig.class},
        loader = AnnotationConfigContextLoader.class,
        initializers = {ConfigDataApplicationContextInitializer.class})
@Sql(scripts = {"classpath:/fi/fmi/avi/avidb/schema/h2/schema-h2.sql", "classpath:/h2-data/avidb_test_content.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/h2-data/avidb_cleanup_test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@ActiveProfiles({"integration-test", "ConditionalPostActionIntegrationTest"})
class ConditionalPostActionIntegrationTest {
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
    private static final List<String> PRODUCT_IDS = List.of(TEST_TAC_PRODUCT_ID, TEST_IWXXM_PRODUCT_ID, TEST2_TAC_PRODUCT_ID, TEST2_IWXXM_PRODUCT_ID);

    @Autowired
    private PostActionService postActionService;
    @Autowired
    private MessageProcessorTestHelper messageProcessorTestHelper;
    @Autowired
    private TestPostActionRegistry testPostActionRegistry;

    private InputAviationMessage inputTemplate;
    private LoggingContext loggingContext;

    private static void failOnMissingAssertion(final TestPostActionId id) {
        fail("Missing assertion for TestPostActionId: " + id);
    }

    @BeforeEach
    void setUp() {
        testPostActionRegistry.resetAll();
        loggingContext = new LoggingContextImpl(new FileProcessingStatisticsImpl());

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

    private ListAssert<TestPostAction.Invocation> assertThatInvocationsOn(final TestPostActionId testPostActionId) {
        return assertThat(testPostActionRegistry.get(testPostActionId).getInvocations())
                .as(testPostActionId.getId());
    }

    private List<TestPostAction.Invocation> toInvocationsOnProductIds(final List<InputAndArchiveAviationMessage> messages, final String... productIds) {
        return TestPostAction.Invocation.stream(loggingContext,
                        messages.stream().filter(InputAndArchiveAviationMessageTests.hasProductIdIn(productIds)))
                .toList();
    }

    private InputAndArchiveAviationMessage inputAndArchiveAviationMessage(final String productId, final Consumer<InputAviationMessage.Builder> inputMessageModifier, final Consumer<ArchiveAviationMessage.Builder> archiveMessageModifier) {
        final InputAviationMessage.Builder inputMessageBuilder = messageProcessorTestHelper.setProductId(productId, inputTemplate.toBuilder());
        inputMessageModifier.accept(inputMessageBuilder);
        final ArchiveAviationMessage.Builder archiveMessageBuilder = messageProcessorTestHelper.setProductId(productId, EXPECTED_TEMPLATE.toBuilder());
        archiveMessageModifier.accept(archiveMessageBuilder);
        return new InputAndArchiveAviationMessage(inputMessageBuilder.build(), archiveMessageBuilder.build()
        );
    }

    @Test
    void testActivationOnProductIdentifierAndMessageType() {
        final List<InputAndArchiveAviationMessage> messages = PRODUCT_IDS.stream()
                .map(productId -> inputAndArchiveAviationMessage(productId,
                        builder -> {
                        },
                        builder -> {
                        }))
                .toList();

        postActionService.runPostActions(messages, loggingContext);

        for (final TestPostActionId id : TestPostActionId.values()) {
            switch (id) {
                case TestPostActionId.IS_TESTTAC_OR_TEST2TAC -> assertThatInvocationsOn(id)
                        .containsExactlyInAnyOrderElementsOf(toInvocationsOnProductIds(messages, TEST_TAC_PRODUCT_ID, TEST2_TAC_PRODUCT_ID));
                case TestPostActionId.IS_TESTIWXXM_AND_METAR -> assertThatInvocationsOn(id)
                        .containsExactlyInAnyOrderElementsOf(toInvocationsOnProductIds(messages, TEST_IWXXM_PRODUCT_ID));
                case TestPostActionId.IS_IWXXM_AND_NOT_METAR_SPECI, TestPostActionId.NO_VALIDFROM ->
                        assertThatInvocationsOn(id).isEmpty();
                case TestPostActionId.POSSIBLE_VALIDTO -> assertThatInvocationsOn(id)
                        .containsExactlyInAnyOrderElementsOf(TestPostAction.Invocation.list(loggingContext, messages));
                default -> failOnMissingAssertion(id);
            }
        }
    }

    @Test
    void testActivateOnPatternAndNegatedCollection() {
        final InputAndArchiveAviationMessage tacMetar = inputAndArchiveAviationMessage(TEST_TAC_PRODUCT_ID,
                builder -> builder.mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)
                        .setMessageType(TypeId.METAR.getType())
                        .build()),
                builder -> builder.setType(TypeId.METAR.getId()));
        final InputAndArchiveAviationMessage iwxxmSpeci = inputAndArchiveAviationMessage(TEST_IWXXM_PRODUCT_ID,
                builder -> builder.mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)
                        .setMessageType(TypeId.SPECI.getType())
                        .build()),
                builder -> builder.setType(TypeId.SPECI.getId()));
        final InputAndArchiveAviationMessage tacTaf = inputAndArchiveAviationMessage(TEST2_TAC_PRODUCT_ID,
                builder -> builder.mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)
                        .setMessageType(TypeId.TAF.getType())
                        .build()),
                builder -> builder.setType(TypeId.TAF.getId()));
        final InputAndArchiveAviationMessage iwxxmTaf = inputAndArchiveAviationMessage(TEST2_IWXXM_PRODUCT_ID,
                builder -> builder.mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)
                        .setMessageType(TypeId.TAF.getType())
                        .build()),
                builder -> builder.setType(TypeId.TAF.getId()));
        final InputAndArchiveAviationMessage iwxxmSwx = inputAndArchiveAviationMessage(TEST_IWXXM_PRODUCT_ID,
                builder -> builder.mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)
                        .setMessageType(TypeId.SWX.getType())
                        .build()),
                builder -> builder.setType(TypeId.SWX.getId()));
        final List<InputAndArchiveAviationMessage> messages = List.of(tacMetar, iwxxmSpeci, tacTaf, iwxxmTaf, iwxxmSwx);

        postActionService.runPostActions(messages, loggingContext);

        for (final TestPostActionId id : TestPostActionId.values()) {
            switch (id) {
                case TestPostActionId.IS_TESTTAC_OR_TEST2TAC -> assertThatInvocationsOn(id)
                        .containsExactlyInAnyOrderElementsOf(toInvocationsOnProductIds(messages, TEST_TAC_PRODUCT_ID, TEST2_TAC_PRODUCT_ID));
                case TestPostActionId.IS_TESTIWXXM_AND_METAR, TestPostActionId.NO_VALIDFROM ->
                        assertThatInvocationsOn(id).isEmpty();
                case TestPostActionId.IS_IWXXM_AND_NOT_METAR_SPECI -> assertThatInvocationsOn(id)
                        .containsExactlyInAnyOrderElementsOf(TestPostAction.Invocation.list(loggingContext, iwxxmTaf, iwxxmSwx));
                case TestPostActionId.POSSIBLE_VALIDTO -> assertThatInvocationsOn(id)
                        .containsExactlyInAnyOrderElementsOf(TestPostAction.Invocation.list(loggingContext, messages));
                default -> failOnMissingAssertion(id);
            }
        }
    }

    @Test
    void testActivateOnEmptyValue() {
        final InputAndArchiveAviationMessage testMessage = inputAndArchiveAviationMessage(TEST2_IWXXM_PRODUCT_ID,
                builder -> builder.mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)
                        .setMessageType(TypeId.METAR.getType())
                        .clearValidityTime()
                        .build()),
                builder -> builder.setType(TypeId.METAR.getId())
                        .clearValidFrom()
                        .clearValidTo()
        );
        final List<InputAndArchiveAviationMessage> messages = List.of(testMessage);

        postActionService.runPostActions(messages, loggingContext);

        for (final TestPostActionId id : TestPostActionId.values()) {
            switch (id) {
                case TestPostActionId.IS_TESTTAC_OR_TEST2TAC,
                     TestPostActionId.IS_IWXXM_AND_NOT_METAR_SPECI,
                     TestPostActionId.IS_TESTIWXXM_AND_METAR -> assertThatInvocationsOn(id).isEmpty();
                case TestPostActionId.NO_VALIDFROM, TestPostActionId.POSSIBLE_VALIDTO -> assertThatInvocationsOn(id)
                        .containsExactlyInAnyOrderElementsOf(TestPostAction.Invocation.list(loggingContext, messages));
                default -> failOnMissingAssertion(id);
            }
        }
    }

    private enum TestPostActionId implements TestPostAction.EnumId {
        IS_TESTTAC_OR_TEST2TAC,
        IS_TESTIWXXM_AND_METAR,
        IS_IWXXM_AND_NOT_METAR_SPECI,
        NO_VALIDFROM,
        POSSIBLE_VALIDTO
    }
}
