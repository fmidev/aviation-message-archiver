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
            LOGGER.error("Bulletin file has nothing to parse"); // TODO log details
            throw new IllegalStateException("Bulletin file has nothing to parse");
        }

        final FileMetadata fileMetadata = FileMetadata.builder()
                .setFilenamePattern(filenamePattern)//
                .setProductIdentifier(productIdentifier)//
                .setFileModified(fileModified)
                .build();
        final List<InputAviationMessage> parsedMessages = new ArrayList<>();
        boolean parsedPartially = false;

        final List<GTSExchangeFileTemplate> templates = parseResults.stream()
                .filter(parseResult -> parseResult.getResult().isPresent())
                .map(parseResult -> parseResult.getResult().get())
                .collect(Collectors.toList());
        if (templates.isEmpty()) {
            // If there are no successful parse results, attempt lenient parsing as a single bulletin
            final GTSExchangeFileTemplate lenientTemplate = GTSExchangeFileTemplate.parseHeadingAndTextLenient(content);
            parsedMessages.addAll(convertBulletin(lenientTemplate, fileFormat, fileMetadata));
        } else {
            // Multiple bulletins
            for (GTSExchangeFileTemplate.ParseResult result : parseResults) {
                if (result.getError().isPresent()) {
                    LOGGER.error("Parsing errors", result.getError().get()); // TODO log details
                    parsedPartially = true;
                } else if (result.getResult().isPresent()) {
                    final GTSExchangeFileTemplate bulletinTemplate = result.getResult().get();
                    parsedMessages.addAll(convertBulletin(bulletinTemplate, fileFormat, fileMetadata));
                }
            }
        }

        if (parsedMessages.isEmpty()) {
            LOGGER.error("Unable to parse any input messages from file"); // TODO log details
            throw new IllegalStateException("Unable to parse any input messages from file");
        }

        return MessageBuilder
                .withPayload(Collections.unmodifiableList(parsedMessages))
                .copyHeaders(headers)
                .setHeader(MessageFileMonitorInitializer.FILE_PARSED_PARTIALLY, parsedPartially)
                .build();
    }

    private List<InputAviationMessage> convertBulletin(final GTSExchangeFileTemplate bulletinTemplate,
                                                       final GenericAviationWeatherMessage.Format fileFormat, final FileMetadata fileMetadata) {
        @Nullable InputBulletinHeading gtsBulletinHeading = null;
        try {
            gtsBulletinHeading = InputBulletinHeading.builder()
                    .setBulletinHeading(BulletinHeadingDecoder.decode(bulletinTemplate.getHeading(), ConversionHints.EMPTY))
                    .setBulletinHeadingString(bulletinTemplate.getHeading())
                    .build();
        } catch (final Exception e) {
            if (fileFormat == GenericAviationWeatherMessage.Format.TAC) {
                // TODO log and throw. TAC cannot be converted if there's no GTS bulletin heading
            }
        }

        if (fileFormat == GenericAviationWeatherMessage.Format.TAC) {
            final String fileContent = bulletinTemplate.getHeading() + "\n" + bulletinTemplate.getText();
            return convertTac(fileContent, fileMetadata, gtsBulletinHeading);
        } else {
            try {
                final Document iwxxmDocument = stringToDocument(bulletinTemplate.getText());
                if (usesCollectSchema(iwxxmDocument)) {
                    return convertIwxxmCollectDocument(iwxxmDocument, fileMetadata, gtsBulletinHeading);
                } else {
                    Optional<InputAviationMessage> inputAviationMessage = convertIwxxmMessage(iwxxmDocument, fileMetadata, gtsBulletinHeading);
                    return inputAviationMessage.map(Collections::singletonList).orElse(Collections.emptyList());
                }
            } catch (final IOException | SAXException e) {
                // TODO Log error and set the bulletin as a failure in a header(?) FAILED_BULLETINS(?)
                return Collections.emptyList();
            }
        }
    }

    private List<InputAviationMessage> convertTac(final String fileContent, final FileMetadata fileMetadata, final InputBulletinHeading gtsBulletinHeading) {
        final ConversionResult<GenericMeteorologicalBulletin> bulletinConversion =
                aviMessageConverter.convertMessage(fileContent, TACConverter.TAC_TO_GENERIC_BULLETIN_POJO);
        if (!bulletinConversion.getConversionIssues().isEmpty()) {
            // TODO Log conversion issues
        }
        if (!bulletinConversion.getConvertedMessage().isPresent()) {
            // Unable to get any messages out of the file content
            return Collections.emptyList();
        }

        final GenericMeteorologicalBulletin bulletin = bulletinConversion.getConvertedMessage().get();
        return bulletin.getMessages().stream()//
                .map(message -> { //
                    if (!message.getMessageType().isPresent()) {
                        throw new IllegalStateException("Message type cannot be determined");
                    }

                    return InputAviationMessage.builder()//
                            .setGtsBulletinHeading(gtsBulletinHeading)
                            .setFileMetadata(fileMetadata)
                            .setMessage(message)
                            .build();
                }).collect(Collectors.toList());
    }

    private List<InputAviationMessage> convertIwxxmCollectDocument(final Document iwxxmDocument, final FileMetadata fileMetadata,
                                                                   @Nullable final InputBulletinHeading gtsBulletinHeading) {
        final ConversionResult<GenericMeteorologicalBulletin> bulletinConversion = aviMessageConverter.convertMessage(iwxxmDocument,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO);
        if (!bulletinConversion.getConversionIssues().isEmpty()) {
            // TODO Log conversion issues. At what level?
        }
        if (!bulletinConversion.getConvertedMessage().isPresent()) {
            // Unable to get any messages out of the file content
            return Collections.emptyList();
        }

        final Optional<String> collectIdentifier = getCollectIdentifier(iwxxmDocument);
        if (!collectIdentifier.isPresent()) {
            // TODO log at what level? Collect document without a collection identifier
        }
        final GenericMeteorologicalBulletin bulletin = bulletinConversion.getConvertedMessage().get();
        return bulletin.getMessages().stream()//
                .map(message -> { //
                    if (!message.getMessageType().isPresent()) {
                        throw new IllegalStateException("Message type cannot be determined");
                    }

                    final InputAviationMessage.Builder inputAviationMessageBuilder = InputAviationMessage.builder();
                    if (gtsBulletinHeading != null) {
                        inputAviationMessageBuilder.setGtsBulletinHeading(gtsBulletinHeading);
                    }
                    final InputBulletinHeading collectIdentifierHeading = InputBulletinHeading.builder()
                            .setBulletinHeading(bulletinConversion.getConvertedMessage().get().getHeading())//
                            .setBulletinHeadingString(collectIdentifier)
                            .build();
                    return inputAviationMessageBuilder
                            .setCollectIdentifier(collectIdentifierHeading)
                            .setFileMetadata(fileMetadata)
                            .setMessage(message)
                            .build();
                }).collect(Collectors.toList());
    }


    private Optional<InputAviationMessage> convertIwxxmMessage(final Document iwxxmDocument, final FileMetadata fileMetadata,
                                                               @Nullable final InputBulletinHeading gtsBulletinHeading) {
        final ConversionResult<GenericAviationWeatherMessage> messageConversion = aviMessageConverter.convertMessage(iwxxmDocument,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO);
        if (!messageConversion.getConversionIssues().isEmpty()) {
            // TODO Log conversion issues
        }
        if (!messageConversion.getConvertedMessage().isPresent()) {
            return Optional.empty();
        }

        final GenericAviationWeatherMessage message = messageConversion.getConvertedMessage().get();
        if (!message.getMessageType().isPresent()) {
            // TODO
            throw new IllegalStateException("Message type cannot be determined");
        }

        final InputAviationMessage.Builder inputAviationMessageBuilder = InputAviationMessage.builder();
        if (gtsBulletinHeading != null) {
            inputAviationMessageBuilder.setGtsBulletinHeading(gtsBulletinHeading);
        }
        return Optional.of(inputAviationMessageBuilder
                .setFileMetadata(fileMetadata)
                .setMessage(message)
                .build());
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
        } catch (final Exception e) {
            return Optional.empty();
        }
    }

}
