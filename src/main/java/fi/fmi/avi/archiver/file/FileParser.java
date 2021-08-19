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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.w3c.dom.Document;
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
        final FilenamePattern pattern = (FilenamePattern) headers.get(MessageFileMonitorInitializer.MESSAGE_FILE_PATTERN);
        final Instant fileModified = (Instant) headers.get(MessageFileMonitorInitializer.FILE_MODIFIED);
        final String productIdentifier = (String) headers.get(MessageFileMonitorInitializer.PRODUCT_IDENTIFIER);
        final GenericAviationWeatherMessage.Format fileFormat = (GenericAviationWeatherMessage.Format) headers.get(MessageFileMonitorInitializer.FILE_FORMAT);
        return parse(productIdentifier, headers, pattern, content, fileFormat, fileModified);
    }

    public Message<List<InputAviationMessage>> parse(final String productIdentifier, final MessageHeaders headers, final FilenamePattern filenamePattern, final String content,
                                                     final GenericAviationWeatherMessage.Format fileFormat, @Nullable final Instant fileModified) {
        final List<GTSExchangeFileTemplate.ParseResult> parseResults = GTSExchangeFileTemplate.parseAll(content);
        if (parseResults.isEmpty()) {
            throw new IllegalStateException("Nothing to parse in file");
        }

        final FileMetadata fileMetadata = FileMetadata.builder()
                .setFilenamePattern(filenamePattern)//
                .setProductIdentifier(productIdentifier)//
                .setFileModified(fileModified)
                .build();
        final List<InputAviationMessage.Builder> parsedMessages = new ArrayList<>();
        boolean parsedPartially = false;

        final List<GTSExchangeFileTemplate> templates = parseResults.stream()
                .filter(parseResult -> parseResult.getResult().isPresent())
                .map(parseResult -> parseResult.getResult().get())
                .collect(Collectors.toList());
        if (templates.isEmpty()) {
            // If there are no successful parse results, attempt lenient parsing as a single bulletin
            final GTSExchangeFileTemplate lenientTemplate = GTSExchangeFileTemplate.parseHeadingAndTextLenient(content);
            parsedMessages.addAll(convertBulletin(lenientTemplate, fileFormat));
        } else {
            for (GTSExchangeFileTemplate.ParseResult result : parseResults) {
                if (result.getError().isPresent()) {
                    LOGGER.error("File parsing errors: ", result.getError().get()); // TODO product & file details
                    parsedPartially = true;
                } else if (result.getResult().isPresent()) {
                    final GTSExchangeFileTemplate bulletinTemplate = result.getResult().get();
                    parsedMessages.addAll(convertBulletin(bulletinTemplate, fileFormat));
                }
            }
        }

        if (parsedMessages.isEmpty()) {
            throw new IllegalStateException("Unable to parse any input messages from file"); // TODO product & file details
        }

        final List<InputAviationMessage> inputAviationMessages = parsedMessages.stream()
                .map(messageBuilder -> messageBuilder.setFileMetadata(fileMetadata))
                .map(InputAviationMessage_Builder::build)
                .collect(Collectors.toList());
        return MessageBuilder
                .withPayload(inputAviationMessages)
                .copyHeaders(headers)
                .setHeader(MessageFileMonitorInitializer.FILE_PARSED_PARTIALLY, parsedPartially)
                .build();
    }

    private List<InputAviationMessage.Builder> convertBulletin(final GTSExchangeFileTemplate bulletinTemplate,
                                                               final GenericAviationWeatherMessage.Format fileFormat) {
        final InputAviationMessage.Builder inputBuilder = InputAviationMessage.builder();
        try {
            final InputBulletinHeading gtsBulletinHeading = InputBulletinHeading.builder()
                    .setBulletinHeading(BulletinHeadingDecoder.decode(bulletinTemplate.getHeading(), ConversionHints.EMPTY))
                    .setBulletinHeadingString(bulletinTemplate.getHeading())
                    .build();
            inputBuilder.setGtsBulletinHeading(gtsBulletinHeading);
        } catch (final Exception e) {
            if (fileFormat == GenericAviationWeatherMessage.Format.TAC) {
                LOGGER.error("TAC bulletin is missing GTS heading"); // TODO product & file details
            }
        }

        final String convertableContent = (bulletinTemplate.getHeading() + "\n" + bulletinTemplate.getText()).trim();
        if (fileFormat == GenericAviationWeatherMessage.Format.TAC) {
            return convertTac(inputBuilder, convertableContent);
        } else {
            try {
                final Document iwxxmDocument;
                if (inputBuilder.getGtsBulletinHeadingBuilder().getBulletinHeading().isPresent()) {
                    iwxxmDocument = stringToDocument(bulletinTemplate.getText().trim());
                } else {
                    iwxxmDocument = stringToDocument(convertableContent);
                }
                if (usesCollectSchema(iwxxmDocument)) {
                    return convertIwxxmCollectDocument(inputBuilder, iwxxmDocument);
                } else {
                    final InputAviationMessage.Builder builder = convertIwxxmMessage(inputBuilder, iwxxmDocument);
                    return Collections.singletonList(builder);
                }
            } catch (final IOException | SAXException e) {
                LOGGER.error("Unable to parse bulletin text into an IWXXM document"); // TODO product & file details
            }
        }
        return Collections.singletonList(inputBuilder);
    }

    private List<InputAviationMessage.Builder> convertTac(final InputAviationMessage.Builder inputBuilder, final String bulletinContent) {
        final ConversionResult<GenericMeteorologicalBulletin> conversion =
                aviMessageConverter.convertMessage(bulletinContent, TACConverter.TAC_TO_GENERIC_BULLETIN_POJO);
        if (conversion.getConvertedMessage().isPresent()) {
            if (!conversion.getConversionIssues().isEmpty()) {
                LOGGER.warn("Conversion issues when converting TAC into a bulletin: {}", conversion.getConversionIssues()); // TODO product & file details
            }
            final GenericMeteorologicalBulletin bulletin = conversion.getConvertedMessage().get();
            return bulletin.getMessages().stream()//
                    .map(inputBuilder::setMessage)
                    .collect(Collectors.toList());
        } else {
            LOGGER.warn("Unable to parse TAC as a bulletin. Attempting to parse as a single message"); // TODO product & file details
            final ConversionResult<GenericAviationWeatherMessage> messageConversion =
                    aviMessageConverter.convertMessage(bulletinContent, TACConverter.TAC_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO);
            if (messageConversion.getConvertedMessage().isPresent()) {
                if (!messageConversion.getConversionIssues().isEmpty()) {
                    LOGGER.warn("Conversion issues when converting TAC into a single message: {}", conversion.getConversionIssues()); // TODO product & file details
                }
                inputBuilder.setMessage(messageConversion.getConvertedMessage().get());
            } else {
                LOGGER.error("Unable to parse TAC as a message"); // TODO product & file details
            }
            return Collections.singletonList(inputBuilder);
        }
    }

    private List<InputAviationMessage.Builder> convertIwxxmCollectDocument(final InputAviationMessage.Builder inputBuilder, final Document iwxxmDocument) {
        final ConversionResult<GenericMeteorologicalBulletin> conversion = aviMessageConverter.convertMessage(iwxxmDocument,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO);
        if (conversion.getConvertedMessage().isPresent()) {
            if (!conversion.getConversionIssues().isEmpty()) {
                LOGGER.warn("Conversion issues when converting IWXXM document into a message: {}", conversion.getConversionIssues()); // TODO product & file details
            }
            final Optional<String> collectIdentifier = getCollectIdentifier(iwxxmDocument);
            if (!collectIdentifier.isPresent()) {
                LOGGER.warn("IWXXM document uses collect schema but has no identifier"); // TODO product & file details
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
            LOGGER.error("IWXXM document could not be converted into a bulletin. Conversion issues: {}", conversion.getConversionIssues()); // TODO product & file details
            return Collections.singletonList(inputBuilder);
        }
    }

    private InputAviationMessage.Builder convertIwxxmMessage(final InputAviationMessage.Builder inputBuilder, final Document iwxxmDocument) {
        final ConversionResult<GenericAviationWeatherMessage> conversion = aviMessageConverter.convertMessage(iwxxmDocument,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO);
        if (conversion.getConvertedMessage().isPresent()) {
            if (!conversion.getConversionIssues().isEmpty()) {
                LOGGER.warn("Conversion issues when converting IWXXM document into a message: {}", conversion.getConversionIssues()); // TODO product & file details
            }
            inputBuilder.setMessage(conversion.getConvertedMessage().get());
        } else {
            LOGGER.error("IWXXM document could not be converted into a message. Conversion issues: {}", conversion.getConversionIssues()); // TODO product & file details
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

}
