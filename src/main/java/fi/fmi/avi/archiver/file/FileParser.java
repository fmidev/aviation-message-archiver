package fi.fmi.avi.archiver.file;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.inferred.freebuilder.FreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import fi.fmi.avi.archiver.logging.BulletinLogReference;
import fi.fmi.avi.archiver.logging.FileProcessingStatistics;
import fi.fmi.avi.archiver.logging.LoggingContext;
import fi.fmi.avi.archiver.logging.MessageLogReference;
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
import fi.fmi.avi.util.GTSExchangeFileParseException;
import fi.fmi.avi.util.GTSExchangeFileTemplate;

public class FileParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileParser.class);

    private static final String COLLECT_1_2_NAMESPACE = "http://def.wmo.int/collect/2014";
    private static final String BULLETIN_ELEMENT_NAME = "MeteorologicalBulletin";
    private static final ConversionHints CONVERSION_HINTS = ConversionHints.ALLOW_ERRORS;
    private static final MessageType UNKNOWN_MESSAGE_TYPE = new MessageType("UNKNOWN");

    private final AviMessageConverter aviMessageConverter;
    private final DocumentBuilderFactory documentBuilderFactory;

    public FileParser(final AviMessageConverter aviMessageConverter) {
        this.aviMessageConverter = requireNonNull(aviMessageConverter, "aviMessageConverter");

        try {
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            this.documentBuilderFactory = documentBuilderFactory;
        } catch (final ParserConfigurationException e) {
            throw new IllegalStateException("Unable to initialize file parser", e);
        }
    }

    private static boolean usesCollectSchema(final Document document) {
        final Element root = document.getDocumentElement();
        return root.getNamespaceURI().equals(COLLECT_1_2_NAMESPACE) && root.getLocalName().equals(BULLETIN_ELEMENT_NAME);
    }

    @Nullable
    private static String getCollectIdentifier(final Document collectDocument) {
        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new IWXXMNamespaceContext());
        try {
            final XPathExpression expr = xpath.compile("/collect:MeteorologicalBulletin/collect:bulletinIdentifier");
            return expr.evaluate(collectDocument.getDocumentElement());
        } catch (final XPathExpressionException e) {
            return null;
        }
    }

    private static Optional<InputBulletinHeading> createGtsHeading(final GTSExchangeFileTemplate template) {
        try {
            return Optional.of(InputBulletinHeading.builder()
                    .setBulletinHeading(BulletinHeadingDecoder.decode(template.getHeading(), ConversionHints.EMPTY))
                    .setBulletinHeadingString(template.getHeading())
                    .build());
        } catch (final RuntimeException e) {
            return Optional.empty();
        }
    }

    public FileParseResult parse(final String content, final FileMetadata fileMetadata, final LoggingContext loggingContext) {
        requireNonNull(content, "content");
        requireNonNull(fileMetadata, "fileMetadata");
        requireNonNull(loggingContext, "loggingContext");

        final GenericAviationWeatherMessage.Format fileFormat = fileMetadata.getFileConfig().getFormat();
        final List<GTSExchangeFileTemplate.ParseResult> parseResults = GTSExchangeFileTemplate.parseAll(content);
        if (parseResults.isEmpty()) {
            loggingContext.recordStatus(FileProcessingStatistics.Status.FAILED);
            throw new IllegalArgumentException("Nothing to parse in <" + loggingContext + ">");
        }
        final boolean bulletinParseSuccess = parseResults.stream().anyMatch(result -> result.getResult().isPresent());

        try {
            final FileParseResult.Builder resultBuilder = FileParseResult.builder();
            final InputAviationMessage.Builder inputMessageTemplate = InputAviationMessage.builder()//
                    .setFileMetadata(fileMetadata);
            if (bulletinParseSuccess) {
                for (int bulletinIndex = 0, size = parseResults.size(); bulletinIndex < size; bulletinIndex++) {
                    final GTSExchangeFileTemplate.ParseResult result = parseResults.get(bulletinIndex);
                    loggingContext.enterBulletin(BulletinLogReference.builder()//
                            .setBulletinIndex(bulletinIndex)//
                            .setBulletinHeading(result.getResult().map(GTSExchangeFileTemplate::getHeading))//
                            .setCharIndex(result.getStartIndex())//
                            .build());
                    if (result.getError().isPresent()) {
                        resultBuilder.setParseErrors(true);
                        final GTSExchangeFileParseException error = result.getError().get();
                        LOGGER.error("Error parsing GTS envelope <{}>: {}", loggingContext, error.getMessage());
                        loggingContext.recordStatus(FileProcessingStatistics.Status.FAILED);
                    } else if (result.getResult().isPresent()) {
                        final GTSExchangeFileTemplate template = result.getResult().get();
                        resultBuilder.mergeFrom(parseContent(content, template, fileFormat, inputMessageTemplate, bulletinIndex, loggingContext));
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
                final GTSExchangeFileTemplate template = GTSExchangeFileTemplate.parseHeadingAndTextLenient(content);
                loggingContext.enterBulletin(BulletinLogReference.builder()//
                        .setBulletinIndex(0)//
                        .setBulletinHeading(template.getHeading())//
                        .setCharIndex(0)//
                        .build());
                resultBuilder.mergeFrom(parseContent(content, template, fileFormat, inputMessageTemplate, 0, loggingContext));
            }
            loggingContext.leaveBulletin();
            return resultBuilder.build();
        } catch (final RuntimeException e) {
            LOGGER.error("Unable to parse any input messages from <{}>", loggingContext, e);
            loggingContext.recordStatus(FileProcessingStatistics.Status.FAILED);
            return FileParseResult.error();
        }
    }

    private FileParseResult parseContent(final String fileContent, final GTSExchangeFileTemplate template,
            final GenericAviationWeatherMessage.Format fileFormat, final InputAviationMessage.Builder inputMessageTemplate, final int bulletinIndex,
            final LoggingContext loggingContext) {
        try {
            return parseContentUnsafe(fileContent, template, fileFormat, inputMessageTemplate, bulletinIndex, loggingContext);
        } catch (final RuntimeException e) {
            LOGGER.error("Error while parsing <{}>: {}.", loggingContext, e.getMessage(), e);
            loggingContext.recordStatus(FileProcessingStatistics.Status.FAILED);
            return FileParseResult.error();
        }
    }

    private FileParseResult parseContentUnsafe(final String fileContent, final GTSExchangeFileTemplate template,
            final GenericAviationWeatherMessage.Format fileFormat, final InputAviationMessage.Builder inputMessageTemplate, final int bulletinIndex,
            final LoggingContext loggingContext) {
        final InputAviationMessage.Builder inputBuilder = InputAviationMessage.builder().mergeFrom(inputMessageTemplate);
        final Optional<InputBulletinHeading> gtsHeading = createGtsHeading(template);
        if (gtsHeading.isPresent()) {
            inputBuilder.setGtsBulletinHeading(gtsHeading.get());
            if (fileFormat == GenericAviationWeatherMessage.Format.TAC) {
                return parseBulletin(inputBuilder, template.toHeadingAndTextString().trim(), fileFormat, bulletinIndex, loggingContext);
            } else {
                return parseBulletin(inputBuilder, template.getText().trim(), fileFormat, bulletinIndex, loggingContext);
            }
        } else {
            loggingContext.modifyBulletinLogReference(reference -> reference.toBuilder().clearBulletinHeading().build());
            LOGGER.debug("{} bulletin <{}> does not contain GTS heading.", fileFormat, loggingContext);
        }
        return parseBulletin(inputBuilder, fileContent.trim(), fileFormat, bulletinIndex, loggingContext);
    }

    private FileParseResult parseBulletin(final InputAviationMessage.Builder inputBuilder, final String content,
            final GenericAviationWeatherMessage.Format fileFormat, final int bulletinIndex, final LoggingContext loggingContext) {
        if (fileFormat == GenericAviationWeatherMessage.Format.TAC) {
            return parseTac(inputBuilder, content, bulletinIndex, loggingContext);
        } else {
            try {
                final Document iwxxmDocument = stringToDocument(content);
                if (usesCollectSchema(iwxxmDocument)) {
                    return parseIwxxmCollectDocument(inputBuilder, iwxxmDocument, bulletinIndex, loggingContext);
                } else {
                    return parseIwxxmMessage(inputBuilder, iwxxmDocument, bulletinIndex, loggingContext);
                }
            } catch (final IOException | SAXException | ParserConfigurationException e) {
                LOGGER.error("Unable to parse bulletin <{}> as IWXXM document: {}", loggingContext, String.valueOf(e));
                loggingContext.recordStatus(FileProcessingStatistics.Status.FAILED);
                return FileParseResult.error();
            }
        }
    }

    private FileParseResult parseTac(final InputAviationMessage.Builder inputBuilder, final String bulletinContent, final int bulletinIndex,
            final LoggingContext loggingContext) {
        final ConversionResult<GenericMeteorologicalBulletin> bulletinConversion = aviMessageConverter.convertMessage(bulletinContent,
                TACConverter.TAC_TO_GENERIC_BULLETIN_POJO, CONVERSION_HINTS);
        final FileParseResult.Builder resultBuilder = FileParseResult.builder();
        if (bulletinConversion.getConvertedMessage().isPresent()) {
            final List<GenericAviationWeatherMessage> parsedMessages = bulletinConversion.getConvertedMessage().get().getMessages();
            if (bulletinConversion.getConversionIssues().isEmpty()) {
                LOGGER.debug("Successfully parsed <{}> as TAC bulletin with {} messages.", loggingContext, parsedMessages.size());
            } else {
                LOGGER.warn("Issues while parsing TAC bulletin <{}>: {}", loggingContext, bulletinConversion.getConversionIssues());
            }
            return addMessages(resultBuilder, inputBuilder, parsedMessages, bulletinIndex, loggingContext).build();
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
                return addMessages(resultBuilder, inputBuilder, Collections.singletonList(message), bulletinIndex, loggingContext).build();
            } else {
                LOGGER.error("Unable to parse TAC content <{}>: {}", loggingContext, messageConversion.getConversionIssues());
                loggingContext.recordStatus(FileProcessingStatistics.Status.FAILED);
                return FileParseResult.error();
            }
        }
    }

    private FileParseResult.Builder addMessages(final FileParseResult.Builder resultBuilder, final InputAviationMessage.Builder inputBuilder,
            final List<GenericAviationWeatherMessage> parsedMessages, final int bulletinIndex, final LoggingContext loggingContext) {
        for (int messageIndex = 0, size = parsedMessages.size(); messageIndex < size; messageIndex++) {
            final GenericAviationWeatherMessage message = parsedMessages.get(messageIndex);
            loggingContext.enterMessage(MessageLogReference.builder()//
                    .setMessageIndex(messageIndex)//
                    .setMessageContent(message.getOriginalMessage())//
                    .build());
            resultBuilder.addInputAviationMessages(InputAviationMessage.builder()//
                    .mergeFrom(inputBuilder)//
                    .setMessagePositionInFile(MessagePositionInFile.getInstance(bulletinIndex, messageIndex))//
                    .setMessage(message)//
                    .build());
            loggingContext.leaveMessage();
        }
        return resultBuilder;
    }

    private FileParseResult parseIwxxmCollectDocument(final InputAviationMessage.Builder inputBuilder, final Document iwxxmDocument, final int bulletinIndex,
            final LoggingContext loggingContext) {
        final ConversionResult<GenericMeteorologicalBulletin> conversion = aviMessageConverter.convertMessage(iwxxmDocument,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, CONVERSION_HINTS);
        if (conversion.getConvertedMessage().isPresent()) {
            final GenericMeteorologicalBulletin bulletin = conversion.getConvertedMessage().get();
            final List<GenericAviationWeatherMessage> parsedMessages = bulletin.getMessages();
            if (conversion.getConversionIssues().isEmpty()) {
                LOGGER.debug("Successfully parsed <{}> as IWXXM collect document with {} messages.", loggingContext, parsedMessages.size());
            } else {
                LOGGER.warn("Issues while parsing IWXXM collect document <{}>: {}", loggingContext, conversion.getConversionIssues());
            }
            @Nullable
            final String collectIdentifier = getCollectIdentifier(iwxxmDocument);
            if (collectIdentifier == null) {
                LOGGER.warn("IWXXM collect document <{}> is missing bulletinIdentifier.", loggingContext);
            } else if (!inputBuilder.getGtsBulletinHeadingBuilder().getBulletinHeadingString().isPresent()) {
                loggingContext.modifyBulletinLogReference(reference -> reference.toBuilder().setBulletinHeading(collectIdentifier).build());
            }

            inputBuilder.setCollectIdentifier(InputBulletinHeading.builder()//
                    .setBulletinHeading(bulletin.getHeading())//
                    .setNullableBulletinHeadingString(collectIdentifier)//
                    .build());
            return addMessages(FileParseResult.builder(), inputBuilder, parsedMessages, bulletinIndex, loggingContext).build();
        } else {
            LOGGER.error("Unable to parse IWXXM collect document <{}>: {}", loggingContext, conversion.getConversionIssues());
            loggingContext.recordStatus(FileProcessingStatistics.Status.FAILED);
            return FileParseResult.error();
        }
    }

    private FileParseResult parseIwxxmMessage(final InputAviationMessage.Builder inputBuilder, final Document iwxxmDocument, final int bulletinIndex,
            final LoggingContext loggingContext) {
        final ConversionResult<GenericAviationWeatherMessage> conversion = aviMessageConverter.convertMessage(iwxxmDocument,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, CONVERSION_HINTS);
        final FileParseResult.Builder resultBuilder = FileParseResult.builder();
        if (conversion.getConvertedMessage().isPresent()) {
            final GenericAviationWeatherMessage message = conversion.getConvertedMessage().get();
            if (!conversion.getConversionIssues().isEmpty()) {
                LOGGER.warn("Issues while parsing IWXXM message document <{}>: {}", loggingContext, conversion.getConversionIssues());
            } else {
                LOGGER.debug("Successfully parsed <{}> as IWXXM {} message.", loggingContext, message.getMessageType().orElse(UNKNOWN_MESSAGE_TYPE));
            }
            addMessages(resultBuilder, inputBuilder, Collections.singletonList(message), bulletinIndex, loggingContext);
        } else {
            resultBuilder.setParseErrors(true);
            LOGGER.error("Unable to parse IWXXM message document <{}>: {}", loggingContext, conversion.getConversionIssues());
            loggingContext.recordStatus(FileProcessingStatistics.Status.FAILED);
        }
        return resultBuilder.build();
    }

    private Document stringToDocument(final String content) throws IOException, SAXException, ParserConfigurationException {
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(new InputSource(new StringReader(content)));
    }

    @FreeBuilder
    public static abstract class FileParseResult {
        private static final FileParseResult ERROR = FileParseResult.builder().setParseErrors(true).build();

        FileParseResult() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public static FileParseResult error() {
            return ERROR;
        }

        public abstract List<InputAviationMessage> getInputAviationMessages();

        public abstract boolean getParseErrors();

        public static class Builder extends FileParser_FileParseResult_Builder {
            public Builder() {
                setParseErrors(false);
            }
        }
    }
}
