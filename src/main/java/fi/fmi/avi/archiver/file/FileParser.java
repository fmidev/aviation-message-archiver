package fi.fmi.avi.archiver.file;

import fi.fmi.avi.archiver.initializing.MessageFileMonitorInitializer;
import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.IWXXMNamespaceContext;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.util.BulletinHeadingDecoder;
import fi.fmi.avi.util.GTSExchangeFileTemplate;
import org.inferred.freebuilder.FreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.file.FileHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class FileParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileParser.class);

    private final AviMessageConverter aviMessageConverter;
    private final DocumentBuilder documentBuilder;

    public FileParser(final AviMessageConverter aviMessageConverter) {
        this.aviMessageConverter = requireNonNull(aviMessageConverter, "aviMessageConverter");

        try {
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            this.documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (final ParserConfigurationException e) {
            throw new IllegalStateException("Unable to initialize file parser", e);
        }
    }

    @ServiceActivator
    public Message<List<InputAviationMessage>> parse(final String content, final MessageHeaders headers) {
        final String filename = headers.get(FileHeaders.FILENAME, String.class);
        final FilenamePattern filenamePattern = headers.get(MessageFileMonitorInitializer.MESSAGE_FILE_PATTERN, FilenamePattern.class);
        final Instant fileModified = headers.get(MessageFileMonitorInitializer.FILE_MODIFIED, Instant.class);
        final String productIdentifier = headers.get(MessageFileMonitorInitializer.PRODUCT_IDENTIFIER, String.class);
        final GenericAviationWeatherMessage.Format fileFormat = headers.get(MessageFileMonitorInitializer.FILE_FORMAT, GenericAviationWeatherMessage.Format.class);

        final List<GTSExchangeFileTemplate.ParseResult> parseResults = GTSExchangeFileTemplate.parseAll(content);
        if (parseResults.isEmpty()) {
            throw new IllegalStateException("Nothing to parse in file " + filename + "(" + productIdentifier + ")");
        }

        final FileMetadata fileMetadata = FileMetadata.builder()
                .setFilenamePattern(filenamePattern)//
                .setProductIdentifier(productIdentifier)//
                .setFileModified(fileModified)
                .build();

        final List<InputAviationMessage.Builder> parsedMessages = new ArrayList<>();
        boolean parsingErrors = false;

        final List<GTSExchangeFileTemplate> templates = parseResults.stream()
                .filter(parseResult -> parseResult.getResult().isPresent())
                .map(parseResult -> parseResult.getResult().get())
                .collect(Collectors.toList());
        if (templates.isEmpty()) {
            // If there are no successful parse results, attempt lenient parsing as a single bulletin
            final GTSExchangeFileTemplate lenientTemplate = GTSExchangeFileTemplate.parseHeadingAndTextLenient(content);
            parsedMessages.addAll(convertBulletin(lenientTemplate, fileFormat, LogDetails.from(filename, productIdentifier, 1)));
        } else {
            for (int i = 0; i < parseResults.size(); i++) {
                final GTSExchangeFileTemplate.ParseResult result = parseResults.get(i);
                final LogDetails logDetails = LogDetails.from(filename, productIdentifier, i + 1);
                if (result.getError().isPresent()) {
                    logError("Error parsing bulletin at index {} in {} ({}): {}", logDetails, result.getError().get().toString());
                    parsingErrors = true;
                } else if (result.getResult().isPresent()) {
                    final GTSExchangeFileTemplate bulletinTemplate = result.getResult().get();
                    parsedMessages.addAll(convertBulletin(bulletinTemplate, fileFormat, logDetails));
                }
            }
        }

        if (parsedMessages.isEmpty()) {
            throw new IllegalStateException("Unable to parse any input messages from file " + filename + "(" + productIdentifier + ")");
        }

        final List<InputAviationMessage> inputAviationMessages = parsedMessages.stream()
                .map(messageBuilder -> messageBuilder.setFileMetadata(fileMetadata))
                .map(InputAviationMessage_Builder::build)
                .collect(Collectors.toList());
        return MessageBuilder
                .withPayload(inputAviationMessages)
                .copyHeaders(headers)
                .setHeader(MessageFileMonitorInitializer.FILE_PARSING_ERRORS, parsingErrors)
                .build();
    }

    private List<InputAviationMessage.Builder> convertBulletin(final GTSExchangeFileTemplate bulletinTemplate,
                                                               final GenericAviationWeatherMessage.Format fileFormat,
                                                               final LogDetails logDetails) {
        final InputAviationMessage.Builder inputBuilder = InputAviationMessage.builder();
        try {
            final InputBulletinHeading gtsBulletinHeading = InputBulletinHeading.builder()
                    .setBulletinHeading(BulletinHeadingDecoder.decode(bulletinTemplate.getHeading(), ConversionHints.EMPTY))
                    .setBulletinHeadingString(bulletinTemplate.getHeading())
                    .build();
            inputBuilder.setGtsBulletinHeading(gtsBulletinHeading);
        } catch (final Exception e) {
            if (fileFormat == GenericAviationWeatherMessage.Format.TAC) {
                logError("Missing GTS heading in TAC bulletin at index {} in {} ({})", logDetails);
            }
        }

        final String convertableContent = (bulletinTemplate.getHeading() + "\n" + bulletinTemplate.getText()).trim();
        if (fileFormat == GenericAviationWeatherMessage.Format.TAC) {
            return convertTac(inputBuilder, convertableContent, logDetails);
        } else {
            try {
                final Document iwxxmDocument;
                if (inputBuilder.getGtsBulletinHeadingBuilder().getBulletinHeading().isPresent()) {
                    iwxxmDocument = stringToDocument(bulletinTemplate.getText().trim());
                } else {
                    iwxxmDocument = stringToDocument(convertableContent);
                }
                if (usesCollectSchema(iwxxmDocument)) {
                    return convertIwxxmCollectDocument(inputBuilder, iwxxmDocument, logDetails);
                } else {
                    final InputAviationMessage.Builder builder = convertIwxxmMessage(inputBuilder, iwxxmDocument, logDetails);
                    return Collections.singletonList(builder);
                }
            } catch (final IOException | SAXException e) {
                logError("Unable to parse bulletin text into an IWXXM document at index {} in {} ({})", logDetails);
            }
        }
        return Collections.singletonList(inputBuilder);
    }

    private List<InputAviationMessage.Builder> convertTac(final InputAviationMessage.Builder inputBuilder,
                                                          final String bulletinContent, final LogDetails logDetails) {
        final ConversionResult<GenericMeteorologicalBulletin> conversion =
                aviMessageConverter.convertMessage(bulletinContent, TACConverter.TAC_TO_GENERIC_BULLETIN_POJO);
        if (conversion.getConvertedMessage().isPresent()) {
            if (!conversion.getConversionIssues().isEmpty()) {
                logWarning("Conversion issues when converting TAC into a bulletin at index {} in {} ({}): {}",
                        logDetails, conversion.getConversionIssues());
            }
            final GenericMeteorologicalBulletin bulletin = conversion.getConvertedMessage().get();
            return bulletin.getMessages().stream()//
                    .map(inputBuilder::setMessage)
                    .collect(Collectors.toList());
        } else {
            logWarning("Unable to parse TAC as a bulletin at index {} in {} ({}). Parsing as a single message", logDetails);
            final ConversionResult<GenericAviationWeatherMessage> messageConversion =
                    aviMessageConverter.convertMessage(bulletinContent, TACConverter.TAC_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO);
            if (messageConversion.getConvertedMessage().isPresent()) {
                if (!messageConversion.getConversionIssues().isEmpty()) {
                    logWarning("Conversion issues when converting TAC into a single message at index {} in {} ({}): {}",
                            logDetails, conversion.getConversionIssues());
                }
                inputBuilder.setMessage(messageConversion.getConvertedMessage().get());
            } else {
                logError("Unable to parse TAC as a message at index {} in {} ({})", logDetails);
            }
            return Collections.singletonList(inputBuilder);
        }
    }

    private List<InputAviationMessage.Builder> convertIwxxmCollectDocument(final InputAviationMessage.Builder inputBuilder,
                                                                           final Document iwxxmDocument, final LogDetails logDetails) {
        final ConversionResult<GenericMeteorologicalBulletin> conversion = aviMessageConverter.convertMessage(iwxxmDocument,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO);
        if (conversion.getConvertedMessage().isPresent()) {
            if (!conversion.getConversionIssues().isEmpty()) {
                logWarning("Conversion issues when converting IWXXM document into a bulletin at index {} in {} ({}): {}",
                        logDetails, conversion.getConversionIssues());
            }
            final Optional<String> collectIdentifier = getCollectIdentifier(iwxxmDocument);
            if (!collectIdentifier.isPresent()) {
                logWarning("IWXXM document uses collect schema but has no identifier at index {} in {} ({})", logDetails);
            }

            final GenericMeteorologicalBulletin bulletin = conversion.getConvertedMessage().get();
            return bulletin.getMessages().stream()//
                    .map(message -> { //
                        final InputBulletinHeading collectIdentifierHeading = InputBulletinHeading.builder()
                                .setBulletinHeading(conversion.getConvertedMessage().get().getHeading())//
                                .setBulletinHeadingString(collectIdentifier)
                                .build();
                        return inputBuilder
                                .setCollectIdentifier(collectIdentifierHeading)
                                .setMessage(message);
                    }).collect(Collectors.toList());
        } else {
            logError("IWXXM document could not be converted into a bulletin at index {} in {} ({}): {}",
                    logDetails, conversion.getConversionIssues());
            return Collections.singletonList(inputBuilder);
        }
    }

    private InputAviationMessage.Builder convertIwxxmMessage(final InputAviationMessage.Builder inputBuilder,
                                                             final Document iwxxmDocument, final LogDetails logDetails) {
        final ConversionResult<GenericAviationWeatherMessage> conversion = aviMessageConverter.convertMessage(iwxxmDocument,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO);
        if (conversion.getConvertedMessage().isPresent()) {
            if (!conversion.getConversionIssues().isEmpty()) {
                logWarning("Conversion issues when converting IWXXM document into a message at index {} in {} ({}): {}",
                        logDetails, conversion.getConversionIssues());
            }
            inputBuilder.setMessage(conversion.getConvertedMessage().get());
        } else {
            logError("IWXXM document could not be converted into a message at index {} in {} ({}): {}",
                    logDetails, conversion.getConversionIssues());
        }
        return inputBuilder;
    }

    private Document stringToDocument(final String content) throws IOException, SAXException {
        return documentBuilder.parse(new InputSource(new StringReader(content)));
    }

    private static boolean usesCollectSchema(final Document document) {
        return document.getDocumentElement().getTagName().equals("collect:MeteorologicalBulletin");
    }

    private static Optional<String> getCollectIdentifier(final Document collectDocument) {
        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new IWXXMNamespaceContext());
        try {
            XPathExpression expr = xpath.compile("/collect:MeteorologicalBulletin/collect:bulletinIdentifier");
            return Optional.of(expr.evaluate(collectDocument.getDocumentElement()));
        } catch (final XPathExpressionException e) {
            return Optional.empty();
        }
    }

    // TODO Logging
    private static void logError(final String message, final LogDetails logDetails, final Object... additionalArguments) {
        LOGGER.error(message, logDetails.getFilename(), logDetails.getProductIdentifier(), logDetails.getBulletinIndex(), additionalArguments);
    }

    // TODO Logging
    private static void logWarning(final String message, final LogDetails logDetails, final Object... additionalArguments) {
        LOGGER.warn(message, logDetails.getFilename(), logDetails.getProductIdentifier(), logDetails.getBulletinIndex(), additionalArguments);
    }

    // TODO Logging
    @FreeBuilder
    public static abstract class LogDetails {
        LogDetails() {
        }

        public static Builder builder() {
            return new LogDetails.Builder();
        }

        static LogDetails from(final String filename, final String productIdentifier, final int bulletinIndex) {
            return builder()
                    .setFilename(filename)
                    .setProductIdentifier(productIdentifier)
                    .setBulletinIndex(bulletinIndex)
                    .build();
        }

        public abstract String getFilename();

        public abstract String getProductIdentifier();

        public abstract int getBulletinIndex();

        public static class Builder extends FileParser_LogDetails_Builder {
            public Builder() {
            }
        }
    }

}
