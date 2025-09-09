package fi.fmi.avi.archiver.file;

import fi.fmi.avi.archiver.ProcessingServiceContext;
import fi.fmi.avi.archiver.logging.model.BulletinLogReference;
import fi.fmi.avi.archiver.logging.model.FileProcessingStatistics;
import fi.fmi.avi.archiver.logging.model.LoggingContext;
import fi.fmi.avi.archiver.logging.model.MessageLogReference;
import fi.fmi.avi.archiver.message.MessagePositionInFile;
import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.util.BulletinHeadingDecoder;
import fi.fmi.avi.util.GTSDataExchangeTranscoder;
import fi.fmi.avi.util.GTSDataParseException;
import fi.fmi.avi.util.GTSMeteorologicalMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNull;

public class FileParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileParser.class);

    private static final String COLLECT_1_2_NAMESPACE = "http://def.wmo.int/collect/2014";
    private static final String BULLETIN_ELEMENT_NAME = "MeteorologicalBulletin";
    private static final ConversionHints CONVERSION_HINTS = ConversionHints.ALLOW_ERRORS;
    private static final MessageType UNKNOWN_MESSAGE_TYPE = new MessageType("UNKNOWN");
    private static final String IWXXM_NS_PREFIX = "icao.int/iwxxm/";
    private static final String GML_NS_PREFIX = "opengis.net/gml/";
    private static final String XPATH_OBSERVATION_TIME = String.format("""
            normalize-space((
              /*[
                contains(namespace-uri(),'%1$s')
                and (local-name()='METAR' or local-name()='SPECI')
              ]
              /*[
                contains(namespace-uri(),'%1$s')
                and local-name()='observationTime'
              ]
              /*[
                contains(namespace-uri(),'%2$s')
                and local-name()='TimeInstant'
              ]
              /*[
                contains(namespace-uri(),'%2$s')
                and local-name()='timePosition'
              ]
            )[1])
            """, IWXXM_NS_PREFIX, GML_NS_PREFIX);

    private final AviMessageConverter aviMessageConverter;
    private final DocumentBuilderFactory documentBuilderFactory;
    private final XPathFactory xPathFactory;
    private final XPathExpression observationTimeExpression;

    public FileParser(final AviMessageConverter aviMessageConverter) {
        this.aviMessageConverter = requireNonNull(aviMessageConverter, "aviMessageConverter");
        try {
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            this.documentBuilderFactory = documentBuilderFactory;
            this.xPathFactory = XPathFactory.newInstance();
            this.observationTimeExpression = xPathFactory.newXPath().compile(XPATH_OBSERVATION_TIME);
        } catch (final ParserConfigurationException | XPathExpressionException e) {
            throw new IllegalStateException("Unable to initialize file parser", e);
        }
    }

    private static List<InputAviationMessage> error(final ProcessingServiceContext context) {
        context.signalProcessingErrors();
        return Collections.emptyList();
    }

    private static boolean usesCollectSchema(final Document document) {
        final Element root = document.getDocumentElement();
        return root.getNamespaceURI().equals(COLLECT_1_2_NAMESPACE) && root.getLocalName().equals(BULLETIN_ELEMENT_NAME);
    }

    private static Optional<InputBulletinHeading> parseGtsHeading(final String headingString) {
        try {
            return Optional.of(InputBulletinHeading.builder()
                    .setBulletinHeading(BulletinHeadingDecoder.decode(headingString, ConversionHints.EMPTY))
                    .setBulletinHeadingString(headingString)
                    .build());
        } catch (final RuntimeException e) {
            return Optional.empty();
        }
    }

    private static List<InputAviationMessage> toInputAviationMessages(
            final InputAviationMessage.Builder inputBuilder,
            final List<GenericAviationWeatherMessage> parsedMessages,
            final int bulletinIndex,
            final LoggingContext loggingContext) {
        return toInputAviationMessages(
                inputBuilder, parsedMessages, bulletinIndex, loggingContext, (builder, message) -> {
                });
    }

    private static List<InputAviationMessage> toInputAviationMessages(
            final InputAviationMessage.Builder inputBuilder,
            final List<GenericAviationWeatherMessage> parsedMessages,
            final int bulletinIndex,
            final LoggingContext loggingContext,
            final BiConsumer<InputAviationMessage.Builder, GenericAviationWeatherMessage> customizer) {
        final ArrayList<InputAviationMessage> resultBuilder = new ArrayList<>(parsedMessages.size());
        for (int messageIndex = 0, size = parsedMessages.size(); messageIndex < size; messageIndex++) {
            final GenericAviationWeatherMessage message = parsedMessages.get(messageIndex);
            loggingContext.enterMessage(MessageLogReference.builder()
                    .setIndex(messageIndex)
                    .setContent(message.getOriginalMessage())
                    .build());
            final InputAviationMessage.Builder builder = InputAviationMessage.builder()
                    .mergeFrom(inputBuilder)
                    .setMessagePositionInFile(MessagePositionInFile.getInstance(bulletinIndex, messageIndex))
                    .setMessage(message);
            customizer.accept(builder, message);
            resultBuilder.add(builder.build());
            loggingContext.leaveMessage();
        }
        return Collections.unmodifiableList(resultBuilder);
    }

    private static boolean looksLikeMetarOrSpeci(final String messageXML) {
        return messageXML.contains("METAR") || messageXML.contains("SPECI");
    }

    @Nullable
    private String getCollectIdentifier(final Document collectDocument) {
        final XPath xpath = xPathFactory.newXPath();
        xpath.setNamespaceContext(new IWXXMNamespaceContext());
        try {
            final XPathExpression expr = xpath.compile("/collect:MeteorologicalBulletin/collect:bulletinIdentifier");
            return expr.evaluate(collectDocument.getDocumentElement());
        } catch (final XPathExpressionException e) {
            return null;
        }
    }

    public List<InputAviationMessage> parse(final String fileContent, final FileMetadata fileMetadata, final ProcessingServiceContext context) {
        requireNonNull(fileContent, "fileContent");
        requireNonNull(fileMetadata, "fileMetadata");
        requireNonNull(context, "context");

        final LoggingContext loggingContext = context.getLoggingContext();
        final GenericAviationWeatherMessage.Format fileFormat = fileMetadata.getFileConfig().getFormat();
        final List<GTSDataExchangeTranscoder.ParseResult> parseResults = GTSDataExchangeTranscoder.parseAll(fileContent);
        if (parseResults.isEmpty()) {
            loggingContext.recordProcessingResult(FileProcessingStatistics.ProcessingResult.FAILED);
            throw new IllegalArgumentException("Nothing to parse in <" + loggingContext + ">");
        }
        final boolean bulletinParseSuccess = parseResults.stream().anyMatch(result -> result.getMessage().isPresent());

        try {
            final ArrayList<InputAviationMessage> resultBuilder = new ArrayList<>();
            final InputAviationMessage.Builder inputMessageTemplate = InputAviationMessage.builder()//
                    .setFileMetadata(fileMetadata);
            if (bulletinParseSuccess) {
                for (int bulletinIndex = 0, size = parseResults.size(); bulletinIndex < size; bulletinIndex++) {
                    final GTSDataExchangeTranscoder.ParseResult result = parseResults.get(bulletinIndex);
                    loggingContext.enterBulletin(BulletinLogReference.builder()//
                            .setIndex(bulletinIndex)//
                            .setHeading(result.getMessage().map(GTSMeteorologicalMessage::getHeading))//
                            .setCharIndex(result.getStartIndex())//
                            .build());
                    if (result.getError().isPresent()) {
                        context.signalProcessingErrors();
                        final GTSDataParseException error = result.getError().get();
                        LOGGER.error("Error parsing GTS envelope <{}>: {}", loggingContext, error.getMessage());
                        loggingContext.recordProcessingResult(FileProcessingStatistics.ProcessingResult.FAILED);
                    } else if (result.getMessage().isPresent()) {
                        final GTSMeteorologicalMessage gtsMessage = result.getMessage().get();
                        resultBuilder.addAll(parseContent(fileContent, gtsMessage, fileFormat, inputMessageTemplate, bulletinIndex, context));
                    }
                }
            } else {
                // If there are no successful parse results, attempt lenient parsing as a single bulletin
                if (LOGGER.isDebugEnabled()) {
                    final String errorMessage = parseResults.stream()
                            .map(result -> result.getError().orElse(null))
                            .filter(Objects::nonNull)
                            .findFirst()
                            .map(Throwable::getMessage)
                            .orElse("");
                    LOGGER.debug("No GTS envelope detected in <{}>: {}. Parsing leniently as heading and text.", loggingContext, errorMessage);
                }
                final GTSMeteorologicalMessage gtsMessage = GTSMeteorologicalMessage.parseHeadingAndTextLenient(fileContent);
                loggingContext.enterBulletin(BulletinLogReference.builder()//
                        .setIndex(0)//
                        .setHeading(gtsMessage.getHeading())//
                        .setCharIndex(0)//
                        .build());
                resultBuilder.addAll(parseContent(fileContent, gtsMessage, fileFormat, inputMessageTemplate, 0, context));
            }
            loggingContext.leaveBulletin();
            return List.copyOf(resultBuilder);
        } catch (final RuntimeException e) {
            LOGGER.error("Unable to parse any input messages from <{}>", loggingContext, e);
            loggingContext.recordProcessingResult(FileProcessingStatistics.ProcessingResult.FAILED);
            return error(context);
        }
    }

    private List<InputAviationMessage> parseContent(
            final String fileContent, final GTSMeteorologicalMessage gtsMessage,
            final GenericAviationWeatherMessage.Format fileFormat, final InputAviationMessage.Builder inputMessageTemplate,
            final int bulletinIndex, final ProcessingServiceContext context) {
        try {
            return parseContentUnsafe(fileContent, gtsMessage, fileFormat, inputMessageTemplate, bulletinIndex, context);
        } catch (final RuntimeException e) {
            final LoggingContext loggingContext = context.getLoggingContext();
            LOGGER.error("Error while parsing <{}>: {}.", loggingContext, e.getMessage(), e);
            loggingContext.recordProcessingResult(FileProcessingStatistics.ProcessingResult.FAILED);
            return error(context);
        }
    }

    private List<InputAviationMessage> parseContentUnsafe(
            final String fileContent, final GTSMeteorologicalMessage gtsMessage,
            final GenericAviationWeatherMessage.Format fileFormat, final InputAviationMessage.Builder inputMessageTemplate,
            final int bulletinIndex, final ProcessingServiceContext context) {
        final InputAviationMessage.Builder inputBuilder = InputAviationMessage.builder().mergeFrom(inputMessageTemplate);
        final Optional<InputBulletinHeading> gtsHeading = parseGtsHeading(gtsMessage.getHeading());
        if (gtsHeading.isPresent()) {
            inputBuilder.setGtsBulletinHeading(gtsHeading.get());
            if (fileFormat == GenericAviationWeatherMessage.Format.TAC) {
                return parseBulletin(inputBuilder, gtsMessage.toString(GTSMeteorologicalMessage.MessageFormat.HEADING_AND_TEXT).trim(), fileFormat,
                        bulletinIndex, context);
            } else {
                return parseBulletin(inputBuilder, gtsMessage.getText().trim(), fileFormat, bulletinIndex, context);
            }
        } else {
            final LoggingContext loggingContext = context.getLoggingContext();
            loggingContext.modifyBulletin(reference -> reference.toBuilder().clearHeading().build());
            LOGGER.debug("{} bulletin <{}> does not contain GTS heading.", fileFormat, loggingContext);
        }
        return parseBulletin(inputBuilder, fileContent.trim(), fileFormat, bulletinIndex, context);
    }

    private List<InputAviationMessage> parseBulletin(
            final InputAviationMessage.Builder inputBuilder, final String bulletinContent,
            final GenericAviationWeatherMessage.Format fileFormat, final int bulletinIndex,
            final ProcessingServiceContext context) {
        if (fileFormat == GenericAviationWeatherMessage.Format.TAC) {
            return parseTac(inputBuilder, bulletinContent, bulletinIndex, context);
        } else {
            try {
                final Document iwxxmDocument = toDocument(bulletinContent);
                if (usesCollectSchema(iwxxmDocument)) {
                    return parseIwxxmCollectDocument(inputBuilder, iwxxmDocument, bulletinIndex, context);
                } else {
                    return parseIwxxmMessage(inputBuilder, iwxxmDocument, bulletinIndex, context);
                }
            } catch (final IOException | SAXException | ParserConfigurationException e) {
                final LoggingContext loggingContext = context.getLoggingContext();
                LOGGER.error("Unable to parse bulletin <{}> as IWXXM document: {}", loggingContext, String.valueOf(e));
                loggingContext.recordProcessingResult(FileProcessingStatistics.ProcessingResult.FAILED);
                return error(context);
            }
        }
    }

    private List<InputAviationMessage> parseTac(
            final InputAviationMessage.Builder inputBuilder, final String bulletinContent, final int bulletinIndex,
            final ProcessingServiceContext context) {
        final LoggingContext loggingContext = context.getLoggingContext();
        final ConversionResult<GenericMeteorologicalBulletin> bulletinConversion = aviMessageConverter.convertMessage(bulletinContent,
                TACConverter.TAC_TO_GENERIC_BULLETIN_POJO, CONVERSION_HINTS);
        if (bulletinConversion.getConvertedMessage().isPresent()) {
            final List<GenericAviationWeatherMessage> parsedMessages = bulletinConversion.getConvertedMessage().get().getMessages();
            if (bulletinConversion.getConversionIssues().isEmpty()) {
                LOGGER.debug("Successfully parsed <{}> as TAC bulletin with {} messages.", loggingContext, parsedMessages.size());
            } else {
                LOGGER.warn("Issues while parsing TAC bulletin <{}>: {}", loggingContext, bulletinConversion.getConversionIssues());
            }
            return toInputAviationMessages(inputBuilder, parsedMessages, bulletinIndex, loggingContext);
        } else {
            final ConversionResult<GenericAviationWeatherMessage> messageConversion = aviMessageConverter.convertMessage(bulletinContent,
                    TACConverter.TAC_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, CONVERSION_HINTS);
            if (messageConversion.getConvertedMessage().isPresent()) {
                final GenericAviationWeatherMessage message = messageConversion.getConvertedMessage().get();
                if (messageConversion.getConversionIssues().isEmpty()) {
                    LOGGER.debug("Successfully parsed <{}> as TAC {} message.", loggingContext, message.getMessageType().orElse(UNKNOWN_MESSAGE_TYPE));
                } else {
                    LOGGER.warn("Issues while parsing single TAC message <{}>: {}", loggingContext, messageConversion.getConversionIssues());
                }
                return toInputAviationMessages(inputBuilder, Collections.singletonList(message), bulletinIndex, loggingContext);
            } else {
                LOGGER.error("Unable to parse TAC content <{}>: {}", loggingContext, messageConversion.getConversionIssues());
                loggingContext.recordProcessingResult(FileProcessingStatistics.ProcessingResult.FAILED);
                return error(context);
            }
        }
    }

    /**
     * Get observation time from iwxxm:observationTime element for SPECIs and METARs.
     *
     * @param document       DOM of a single IWXXM message (METAR/SPECI)
     * @param loggingContext logging context
     * @return observation time if found and parsed, empty otherwise
     */
    private Optional<OffsetDateTime> getObservationTimeFromIwxxm(final Document document,
                                                                 final LoggingContext loggingContext) {
        try {
            final String observationTime = observationTimeExpression.evaluate(document);
            if (!observationTime.isBlank()) {
                return Optional.of(OffsetDateTime.parse(observationTime));
            }
        } catch (final XPathExpressionException | DateTimeParseException e) {
            LOGGER.warn("Failed to extract observationTime from <{}>", loggingContext, e);
        }
        return Optional.empty();
    }

    private List<InputAviationMessage> parseIwxxmCollectDocument(
            final InputAviationMessage.Builder inputBuilder,
            final Document iwxxmDocument,
            final int bulletinIndex,
            final ProcessingServiceContext context) {
        final ConversionResult<GenericMeteorologicalBulletin> conversion =
                aviMessageConverter.convertMessage(iwxxmDocument,
                        IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, CONVERSION_HINTS);
        final LoggingContext loggingContext = context.getLoggingContext();
        if (conversion.getConvertedMessage().isPresent()) {
            final GenericMeteorologicalBulletin bulletin = conversion.getConvertedMessage().get();
            final List<GenericAviationWeatherMessage> parsedMessages = bulletin.getMessages();
            if (conversion.getConversionIssues().isEmpty()) {
                LOGGER.debug("Successfully parsed <{}> as IWXXM collect document with {} messages.", loggingContext, parsedMessages.size());
            } else {
                LOGGER.warn("Issues while parsing IWXXM collect document <{}>: {}", loggingContext, conversion.getConversionIssues());
            }
            @Nullable final String collectIdentifier = getCollectIdentifier(iwxxmDocument);
            if (collectIdentifier == null) {
                LOGGER.warn("IWXXM collect document <{}> is missing bulletinIdentifier.", loggingContext);
            } else if (inputBuilder.getGtsBulletinHeadingBuilder().getBulletinHeadingString().isEmpty()) {
                loggingContext.modifyBulletin(reference -> reference.toBuilder().setHeading(collectIdentifier).build());
            }

            inputBuilder.setCollectIdentifier(InputBulletinHeading.builder()
                    .setBulletinHeading(bulletin.getHeading())
                    .setNullableBulletinHeadingString(collectIdentifier)
                    .build());
            return toInputAviationMessages(
                    inputBuilder,
                    parsedMessages,
                    bulletinIndex,
                    loggingContext,
                    (builder, message) -> {
                        if (looksLikeMetarOrSpeci(message.getOriginalMessage())) {
                            try {
                                builder.setIwxxmObservationTime(getObservationTimeFromIwxxm(toDocument(message.getOriginalMessage()), loggingContext));
                            } catch (final Exception e) {
                                LOGGER.warn("Failed to create a DOM for extracting observationTime from <{}>", loggingContext, e);
                            }
                        }
                    }
            );
        } else {
            LOGGER.error("Unable to parse IWXXM collect document <{}>: {}", loggingContext, conversion.getConversionIssues());
            loggingContext.recordProcessingResult(FileProcessingStatistics.ProcessingResult.FAILED);
            return error(context);
        }
    }

    private List<InputAviationMessage> parseIwxxmMessage(
            final InputAviationMessage.Builder inputBuilder,
            final Document iwxxmDocument,
            final int bulletinIndex,
            final ProcessingServiceContext context) {
        final ConversionResult<GenericAviationWeatherMessage> conversion =
                aviMessageConverter.convertMessage(iwxxmDocument,
                        IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, CONVERSION_HINTS);
        final LoggingContext loggingContext = context.getLoggingContext();
        if (conversion.getConvertedMessage().isPresent()) {
            final GenericAviationWeatherMessage message = conversion.getConvertedMessage().get();
            if (!conversion.getConversionIssues().isEmpty()) {
                LOGGER.warn("Issues while parsing IWXXM message document <{}>: {}", loggingContext, conversion.getConversionIssues());
            } else {
                LOGGER.debug("Successfully parsed <{}> as IWXXM {} message.", loggingContext, message.getMessageType().orElse(UNKNOWN_MESSAGE_TYPE));
            }
            return toInputAviationMessages(
                    inputBuilder,
                    Collections.singletonList(message),
                    bulletinIndex,
                    loggingContext,
                    (builder, gm) -> {
                        if (looksLikeMetarOrSpeci(message.getOriginalMessage())) {
                            builder.setIwxxmObservationTime(getObservationTimeFromIwxxm(iwxxmDocument, loggingContext));
                        }
                    }
            );
        } else {
            LOGGER.error("Unable to parse IWXXM message document <{}>: {}", loggingContext, conversion.getConversionIssues());
            loggingContext.recordProcessingResult(FileProcessingStatistics.ProcessingResult.FAILED);
            return error(context);
        }
    }

    private Document toDocument(final String documentContent) throws IOException, SAXException, ParserConfigurationException {
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(new InputSource(new StringReader(documentContent)));
    }
}
