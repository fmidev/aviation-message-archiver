package fi.fmi.avi.archiver.config;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import fi.fmi.avi.archiver.AviationMessageArchiver;
import fi.fmi.avi.archiver.TestConfig;
import fi.fmi.avi.archiver.config.model.AviationProduct;
import fi.fmi.avi.archiver.config.model.FileConfig;
import fi.fmi.avi.archiver.config.model.MessagePopulatorFactory;
import fi.fmi.avi.archiver.file.FileReference;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessageOrBuilder;
import fi.fmi.avi.archiver.message.InputAndArchiveAviationMessage;
import fi.fmi.avi.archiver.message.processor.MessageProcessorContext;
import fi.fmi.avi.archiver.message.processor.MessageProcessorHelper;
import fi.fmi.avi.archiver.message.processor.conditional.AbstractConditionPropertyReader;
import fi.fmi.avi.archiver.message.processor.populator.MessagePopulator;
import fi.fmi.avi.archiver.message.processor.populator.MessagePopulatorTests.FormatId;
import fi.fmi.avi.archiver.message.processor.populator.MessagePopulatorTests.RouteId;
import fi.fmi.avi.archiver.message.processor.populator.MessagePopulatorTests.StationId;
import fi.fmi.avi.archiver.message.processor.populator.MessagePopulatorTests.TypeId;
import fi.fmi.avi.archiver.util.instantiation.ConfigValueConverter;
import fi.fmi.avi.archiver.util.instantiation.ReflectionObjectFactory;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest({"auto.startup=false", "testclass.name=fi.fmi.avi.archiver.config.MessagePopulatorConfigTest"})
@ContextConfiguration(classes = {AviationMessageArchiver.class, TestConfig.class, ConversionConfig.class},
        loader = AnnotationConfigContextLoader.class,
        initializers = {ConfigDataApplicationContextInitializer.class})
@Sql(scripts = {"classpath:/fi/fmi/avi/avidb/schema/h2/schema-h2.sql", "classpath:/h2-data/avidb_test_content.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:/h2-data/avidb_cleanup_test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@ActiveProfiles({"integration-test", "ConditionalMessagePopulatorTest"})
class ConditionalMessagePopulatorConfigTest {
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
    private static final Map<FormatId, String> FILENAMES = Maps.immutableEnumMap(ImmutableMap.of(
            FormatId.TAC, "messagefile.txt",
            FormatId.IWXXM, "messagefile.xml"));

    @Autowired
    private MessagePopulatorConfig.MessagePopulationIntegrationService messagePopulationIntegrationService;
    @Autowired
    private Map<String, AviationProduct> aviationProducts;

    private InputAviationMessage inputTemplate;

    private static List<InputAndArchiveAviationMessage> toInputAndArchiveAviationMessages(final List<InputAviationMessage> inputMessages, final ArchiveAviationMessage... archiveMessages) {
        return toInputAndArchiveAviationMessages(inputMessages, Arrays.asList(archiveMessages));
    }

    private static List<InputAndArchiveAviationMessage> toInputAndArchiveAviationMessages(final List<InputAviationMessage> inputMessages, final List<ArchiveAviationMessage> archiveMessages) {
        final Iterator<InputAviationMessage> inputIterator = inputMessages.iterator();
        final Iterator<ArchiveAviationMessage> archiveIterator = archiveMessages.iterator();
        final List<InputAndArchiveAviationMessage> result = new ArrayList<>(inputMessages.size());
        while (inputIterator.hasNext() && archiveIterator.hasNext()) {
            final InputAviationMessage inputMessage = inputIterator.next();
            result.add(new InputAndArchiveAviationMessage(inputMessage, archiveIterator.next()));
        }
        if (inputIterator.hasNext() || archiveIterator.hasNext()) {
            throw new IllegalArgumentException(String.format(Locale.US, "Sizes differ: inputMessages (%d), archiveMessages (%d) ", inputMessages.size(), archiveMessages.size()));
        }
        return result;
    }

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
        inputTemplate = setProductId(TEST_TAC_PRODUCT_ID, builder).build();
    }

    private FileConfig fileConfig(final String productId) {
        return aviationProducts.get(productId).getFileConfigs().getFirst();
    }

    private InputAviationMessage.Builder setProductId(final String productId, final InputAviationMessage.Builder builder) {
        final FileConfig fileConfig = fileConfig(productId);
        final FormatId formatId = FormatId.valueOf(fileConfig.getFormat());
        return builder
                .mutateFileMetadata(metadataBuilder -> metadataBuilder
                        .setFileConfig(fileConfig)
                        .setFileReference(FileReference.builder()
                                .setProductId(productId)
                                .setFilename(FILENAMES.get(formatId))
                                .build()))
                .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)
                        .setMessageFormat(formatId.getFormat())
                        .build());
    }

    private ArchiveAviationMessage.Builder setProductId(final String productId, final ArchiveAviationMessage.Builder builder) {
        final FileConfig fileConfig = fileConfig(productId);
        return builder
                .setRoute(aviationProducts.get(productId).getRouteId())
                .setFormat(FormatId.valueOf(fileConfig.getFormat()).getId());
    }

    private List<InputAndArchiveAviationMessage> populate(final List<InputAviationMessage> inputMessages) {
        return messagePopulationIntegrationService.populateMessages(inputMessages, new MessageHeaders(Collections.emptyMap()))
                .getPayload();
    }

    @Test
    void testActivationOnProductIdentifierAndMessageType() {
        final List<InputAviationMessage> inputMessages = Arrays.asList(
                setProductId(TEST_TAC_PRODUCT_ID, inputTemplate.toBuilder()).build(),
                setProductId(TEST_IWXXM_PRODUCT_ID, inputTemplate.toBuilder()).build(),
                setProductId(TEST2_TAC_PRODUCT_ID, inputTemplate.toBuilder()).build(),
                setProductId(TEST2_IWXXM_PRODUCT_ID, inputTemplate.toBuilder()).build());

        final List<InputAndArchiveAviationMessage> result = populate(inputMessages);

        final List<InputAndArchiveAviationMessage> expected = toInputAndArchiveAviationMessages(inputMessages,
                setProductId(TEST_TAC_PRODUCT_ID, EXPECTED_TEMPLATE.toBuilder())
                        .setMessage("Original message; is testTAC or test2TAC; possible validTo")
                        .build(),
                setProductId(TEST_IWXXM_PRODUCT_ID, EXPECTED_TEMPLATE.toBuilder())
                        .setMessage("Original message; is testIWXXM and METAR; possible validTo")
                        .build(),
                setProductId(TEST2_TAC_PRODUCT_ID, EXPECTED_TEMPLATE.toBuilder())
                        .setMessage("Original message; is testTAC or test2TAC; possible validTo")
                        .build(),
                setProductId(TEST2_IWXXM_PRODUCT_ID, EXPECTED_TEMPLATE.toBuilder())
                        .setMessage("Original message; possible validTo")
                        .build());
        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void testActivateOnPatternAndNegatedCollection() {
        final List<InputAviationMessage> inputMessages = Arrays.asList(
                setProductId(TEST_TAC_PRODUCT_ID, inputTemplate.toBuilder())
                        .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)
                                .setMessageType(TypeId.METAR.getType())
                                .build())
                        .build(),
                setProductId(TEST_IWXXM_PRODUCT_ID, inputTemplate.toBuilder())
                        .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)
                                .setMessageType(TypeId.SPECI.getType())
                                .build())
                        .build(),
                setProductId(TEST2_TAC_PRODUCT_ID, inputTemplate.toBuilder())
                        .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)
                                .setMessageType(TypeId.TAF.getType())
                                .build())
                        .build(),
                setProductId(TEST2_IWXXM_PRODUCT_ID, inputTemplate.toBuilder())
                        .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)
                                .setMessageType(TypeId.TAF.getType())
                                .build())
                        .build(),
                setProductId(TEST_IWXXM_PRODUCT_ID, inputTemplate.toBuilder())
                        .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)
                                .setMessageType(TypeId.SWX.getType())
                                .build())
                        .build());

        final List<InputAndArchiveAviationMessage> result = populate(inputMessages);

        final List<InputAndArchiveAviationMessage> expected = toInputAndArchiveAviationMessages(inputMessages,
                setProductId(TEST_TAC_PRODUCT_ID, EXPECTED_TEMPLATE.toBuilder())
                        .setType(TypeId.METAR.getId())
                        .setMessage("Original message; is testTAC or test2TAC; possible validTo")
                        .build(),
                setProductId(TEST_IWXXM_PRODUCT_ID, EXPECTED_TEMPLATE.toBuilder())
                        .setType(TypeId.SPECI.getId())
                        .setMessage("Original message; possible validTo")
                        .build(),
                setProductId(TEST2_TAC_PRODUCT_ID, EXPECTED_TEMPLATE.toBuilder())
                        .setType(TypeId.TAF.getId())
                        .setMessage("Original message; is testTAC or test2TAC; possible validTo")
                        .build(),
                setProductId(TEST2_IWXXM_PRODUCT_ID, EXPECTED_TEMPLATE.toBuilder())
                        .setType(TypeId.TAF.getId())
                        .setMessage("Original message; is IWXXM and not METAR/SPECI; possible validTo")
                        .build(),
                setProductId(TEST_IWXXM_PRODUCT_ID, EXPECTED_TEMPLATE.toBuilder())
                        .setType(TypeId.SWX.getId())
                        .setMessage("Original message; is IWXXM and not METAR/SPECI; possible validTo")
                        .build());
        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void testActivateOnEmptyValue() {
        final List<InputAviationMessage> inputMessages = Collections.singletonList(
                setProductId(TEST2_IWXXM_PRODUCT_ID, inputTemplate.toBuilder())
                        .mapMessage(message -> GenericAviationWeatherMessageImpl.Builder.from(message)
                                .setMessageType(TypeId.METAR.getType())
                                .clearValidityTime()
                                .build())
                        .build());

        final List<InputAndArchiveAviationMessage> result = populate(inputMessages);

        final List<InputAndArchiveAviationMessage> expected = toInputAndArchiveAviationMessages(inputMessages,
                setProductId(TEST2_IWXXM_PRODUCT_ID, EXPECTED_TEMPLATE.toBuilder())
                        .setType(TypeId.METAR.getId())
                        .clearValidFrom()
                        .clearValidTo()
                        .setMessage("Original message; no validFrom; possible validTo")
                        .build());
        assertThat(result).containsExactlyElementsOf(expected);
    }

    public static class MessageAppendingPopulator implements MessagePopulator {
        private final String content;

        public MessageAppendingPopulator(final String content) {
            this.content = requireNonNull(content, "content");
        }

        @Override
        public void populate(final MessageProcessorContext context, final ArchiveAviationMessage.Builder target) {
            target.mapMessage(message -> message + "; " + content);
        }
    }

    public static class TestProductIdentifierPropertyReader extends AbstractConditionPropertyReader<String> {
        @Override
        public String readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder target) {
            return input.getFileMetadata().getFileReference().getProductId();
        }
    }

    public static class TestTargetMessageTypePropertyReader extends AbstractConditionPropertyReader<MessageType> {
        private final BiMap<MessageType, Integer> messageTypeIds;

        public TestTargetMessageTypePropertyReader(final BiMap<MessageType, Integer> messageTypeIds) {
            this.messageTypeIds = requireNonNull(messageTypeIds, "messageTypeIds");
        }

        @Nullable
        @Override
        public MessageType readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder target) {
            final OptionalInt messageTypeId = MessageProcessorHelper.tryGetInt(target, ArchiveAviationMessageOrBuilder::getType);
            return messageTypeId.isPresent() ? messageTypeIds.inverse().get(messageTypeId.getAsInt()) : null;
        }

        @Override
        public boolean validate(final MessageType value) {
            return messageTypeIds.containsKey(value);
        }
    }

    public static class TestInputValidFromReader extends AbstractConditionPropertyReader<ZonedDateTime> {
        @Nullable
        @Override
        public ZonedDateTime readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder target) {
            return input.getMessage().getValidityTime()
                    .flatMap(PartialOrCompleteTimePeriod::getStartTime)
                    .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                    .orElse(null);
        }
    }

    public static class TestInputValidToReader extends AbstractConditionPropertyReader<ZonedDateTime> {
        @Nullable
        @Override
        public ZonedDateTime readValue(final InputAviationMessage input, final ArchiveAviationMessageOrBuilder target) {
            return input.getMessage().getValidityTime()
                    .flatMap(PartialOrCompleteTimePeriod::getEndTime)
                    .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)
                    .orElse(null);
        }
    }

    @Configuration
    @Profile({"integration-test", "ConditionalMessagePopulatorTest"})
    static class MessagePopulatorTestConfig {
        @Bean
        MessagePopulatorFactory<MessageAppendingPopulator> messageAppendingPopulator(final ConfigValueConverter messagePopulatorConfigValueConverter) {
            return new MessagePopulatorFactory<>(ReflectionObjectFactory.builder(MessageAppendingPopulator.class, messagePopulatorConfigValueConverter)
                    .addConfigArg("content", String.class)
                    .build());
        }

        @Bean
        TestProductIdentifierPropertyReader testProductIdentifierPropertyReader() {
            return new TestProductIdentifierPropertyReader();
        }

        @Bean
        TestTargetMessageTypePropertyReader testTargetMessageTypePropertyReader(final BiMap<MessageType, Integer> messageTypeIds) {
            return new TestTargetMessageTypePropertyReader(messageTypeIds);
        }

        @Bean
        TestInputValidFromReader testInputValidFromReader() {
            return new TestInputValidFromReader();
        }

        @Bean
        TestInputValidToReader testInputValidToReader() {
            return new TestInputValidToReader();
        }
    }
}
