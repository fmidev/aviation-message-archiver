package fi.fmi.avi.archiver.file;

import com.google.common.collect.ImmutableList;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class FileParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileParser.class);

    private static final String COLLECT_1_2_NAMESPACE = "http://def.wmo.int/collect/2014";
    private static final String BULLETIN_ELEMENT_NAME = "MeteorologicalBulletin";
    private static final ConversionHints CONVERSION_HINTS = ConversionHints.ALLOW_ERRORS;

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

    public FileParseResult parse(final String content, final FileMetadata fileMetadata) {
        final GenericAviationWeatherMessage.Format fileFormat = fileMetadata.getFileConfig().getFormat();
        final List<GTSExchangeFileTemplate.ParseResult> parseResults = GTSExchangeFileTemplate.parseAll(content);
        if (parseResults.isEmpty()) {
            throw new IllegalStateException("Nothing to parse in file " + toLogEntry(fileMetadata));
        }
        final boolean bulletinParseSuccess = parseResults.stream().anyMatch(result -> result.getResult().isPresent());

        try {
            final FileParseResult.Builder resultBuilder = FileParseResult.builder().setParseErrors(false);
            if (bulletinParseSuccess) {
                for (int i = 0; i < parseResults.size(); i++) {
                    final GTSExchangeFileTemplate.ParseResult result = parseResults.get(i);
                    final LogDetails logDetails = LogDetails.from(fileMetadata, i + 1);
                    if (result.getError().isPresent()) {
                        logError("Error parsing bulletin at index {} in {}: {}", logDetails, result.getError().get().toString());
                        resultBuilder.setParseErrors(true);
                    } else if (result.getResult().isPresent()) {
                        final GTSExchangeFileTemplate template = result.getResult().get();
                        resultBuilder.addAllBuildersOfInputAviationMessages(convertContent(content, template, fileFormat, logDetails));
                    }
                }
            } else {
                // If there are no successful parse results, attempt lenient parsing as a single bulletin
                final GTSExchangeFileTemplate template = GTSExchangeFileTemplate.parseHeadingAndTextLenient(content);
                final LogDetails logDetails = LogDetails.from(fileMetadata, 1);
                resultBuilder.addAllBuildersOfInputAviationMessages(convertContent(content, template, fileFormat, logDetails));
            }
            return resultBuilder//
                    .mutateInputAviationMessages(messages ->
                            messages.forEach(messageBuilder -> messageBuilder.setFileMetadata(fileMetadata)))
                    .build();
        } catch (final RuntimeException e) {
            throw new IllegalStateException("Unable to parse any input messages from file " + toLogEntry(fileMetadata), e);
        }
    }

    private List<InputAviationMessage.Builder> convertContent(final String fileContent, final GTSExchangeFileTemplate template,
                                                              final GenericAviationWeatherMessage.Format fileFormat, final LogDetails logDetails) {
        final InputAviationMessage.Builder inputBuilder = InputAviationMessage.builder();
        final Optional<InputBulletinHeading> gtsHeading = createGtsHeading(template);
        if (gtsHeading.isPresent()) {
            inputBuilder.setGtsBulletinHeading(gtsHeading.get());
            if (fileFormat == GenericAviationWeatherMessage.Format.TAC) {
                return convertBulletin(inputBuilder, template.toHeadingAndTextString().trim(), fileFormat, logDetails);
            } else {
                return convertBulletin(inputBuilder, template.getText().trim(), fileFormat, logDetails);
            }
        } else if (fileFormat == GenericAviationWeatherMessage.Format.TAC) {
            logError("Missing GTS heading in TAC bulletin at index {} in {}", logDetails);
        }
        return convertBulletin(inputBuilder, fileContent.trim(), fileFormat, logDetails);
    }

    private List<InputAviationMessage.Builder> convertBulletin(final InputAviationMessage.Builder inputBuilder,
                                                               final String content, final GenericAviationWeatherMessage.Format fileFormat,
                                                               final LogDetails logDetails) {
        if (fileFormat == GenericAviationWeatherMessage.Format.TAC) {
            return convertTac(inputBuilder, content, logDetails);
        } else {
            try {
                final Document iwxxmDocument = stringToDocument(content);
                if (usesCollectSchema(iwxxmDocument)) {
                    return convertIwxxmCollectDocument(inputBuilder, iwxxmDocument, logDetails);
                } else {
                    final InputAviationMessage.Builder builder = convertIwxxmMessage(inputBuilder, iwxxmDocument, logDetails);
                    return Collections.singletonList(builder);
                }
            } catch (final IOException | SAXException | ParserConfigurationException e) {
                logError("Unable to parse bulletin text into an IWXXM document at index {} in {}", logDetails);
            }
        }
        return Collections.singletonList(inputBuilder);
    }

    private List<InputAviationMessage.Builder> convertTac(final InputAviationMessage.Builder inputBuilder,
                                                          final String bulletinContent, final LogDetails logDetails) {
        final ConversionResult<GenericMeteorologicalBulletin> conversion =
                aviMessageConverter.convertMessage(bulletinContent, TACConverter.TAC_TO_GENERIC_BULLETIN_POJO, CONVERSION_HINTS);
        if (conversion.getConvertedMessage().isPresent()) {
            if (!conversion.getConversionIssues().isEmpty()) {
                logWarning("Conversion issues when converting TAC into a bulletin at index {} in {}: {}",
                        logDetails, conversion.getConversionIssues());
            }
            final GenericMeteorologicalBulletin bulletin = conversion.getConvertedMessage().get();
            return bulletin.getMessages().stream()
                    .map(message -> InputAviationMessage.builder().mergeFrom(inputBuilder).setMessage(message))
                    .collect(ImmutableList.toImmutableList());
        } else {
            final ConversionResult<GenericAviationWeatherMessage> messageConversion =
                    aviMessageConverter.convertMessage(bulletinContent,
                            TACConverter.TAC_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, CONVERSION_HINTS);
            if (messageConversion.getConvertedMessage().isPresent()) {
                if (!messageConversion.getConversionIssues().isEmpty()) {
                    logWarning("Conversion issues when converting TAC into a single message at index {} in {}: {}",
                            logDetails, conversion.getConversionIssues());
                }
                inputBuilder.setMessage(messageConversion.getConvertedMessage().get());
            } else {
                logError("Unable to parse TAC as a message at index {} in {}", logDetails);
            }
            return Collections.singletonList(inputBuilder);
        }
    }

    private List<InputAviationMessage.Builder> convertIwxxmCollectDocument(final InputAviationMessage.Builder inputBuilder,
                                                                           final Document iwxxmDocument, final LogDetails logDetails) {
        final ConversionResult<GenericMeteorologicalBulletin> conversion = aviMessageConverter.convertMessage(iwxxmDocument,
                IWXXMConverter.WMO_COLLECT_DOM_TO_GENERIC_BULLETIN_POJO, CONVERSION_HINTS);
        if (conversion.getConvertedMessage().isPresent()) {
            if (!conversion.getConversionIssues().isEmpty()) {
                logWarning("Conversion issues when converting IWXXM document into a bulletin at index {} in {}: {}",
                        logDetails, conversion.getConversionIssues());
            }
            final Optional<String> collectIdentifier = getCollectIdentifier(iwxxmDocument);
            if (!collectIdentifier.isPresent()) {
                logWarning("IWXXM document uses collect schema but has no identifier at index {} in {}", logDetails);
            }

            final GenericMeteorologicalBulletin bulletin = conversion.getConvertedMessage().get();
            final InputBulletinHeading collectIdentifierHeading = InputBulletinHeading.builder()
                    .setBulletinHeading(bulletin.getHeading())
                    .setBulletinHeadingString(collectIdentifier)
                    .build();
            return bulletin.getMessages().stream()
                    .map(message -> InputAviationMessage.builder().mergeFrom(inputBuilder
                            .setCollectIdentifier(collectIdentifierHeading)
                            .setMessage(message)))
                    .collect(ImmutableList.toImmutableList());
        } else {
            logError("IWXXM document could not be converted into a bulletin at index {} in {}: {}",
                    logDetails, conversion.getConversionIssues());
            return Collections.singletonList(inputBuilder);
        }
    }

    private InputAviationMessage.Builder convertIwxxmMessage(final InputAviationMessage.Builder inputBuilder,
                                                             final Document iwxxmDocument, final LogDetails logDetails) {
        final ConversionResult<GenericAviationWeatherMessage> conversion = aviMessageConverter.convertMessage(iwxxmDocument,
                IWXXMConverter.IWXXM_DOM_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO, CONVERSION_HINTS);
        if (conversion.getConvertedMessage().isPresent()) {
            if (!conversion.getConversionIssues().isEmpty()) {
                logWarning("Conversion issues when converting IWXXM document into a message at index {} in {}: {}",
                        logDetails, conversion.getConversionIssues());
            }
            inputBuilder.setMessage(conversion.getConvertedMessage().get());
        } else {
            logError("IWXXM document could not be converted into a message at index {} in {}: {}",
                    logDetails, conversion.getConversionIssues());
        }
        return inputBuilder;
    }

    private Document stringToDocument(final String content) throws IOException, SAXException, ParserConfigurationException {
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(new InputSource(new StringReader(content)));
    }

    private static boolean usesCollectSchema(final Document document) {
        final Element root = document.getDocumentElement();
        return root.getNamespaceURI().equals(COLLECT_1_2_NAMESPACE) && root.getLocalName().equals(BULLETIN_ELEMENT_NAME);
    }

    private static Optional<String> getCollectIdentifier(final Document collectDocument) {
        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new IWXXMNamespaceContext());
        try {
            XPathExpression expr = xpath.compile("/collect:MeteorologicalBulletin/collect:bulletinIdentifier");
            return Optional.ofNullable(expr.evaluate(collectDocument.getDocumentElement()));
        } catch (final XPathExpressionException e) {
            return Optional.empty();
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

    // TODO Logging
    private static void logError(final String message, final LogDetails logDetails, final Object... additionalArguments) {
        LOGGER.error(message, logDetails.getBulletinIndex(), toLogEntry(logDetails.getFileMetadata()), additionalArguments);
    }

    // TODO Logging
    private static void logWarning(final String message, final LogDetails logDetails, final Object... additionalArguments) {
        LOGGER.warn(message, logDetails.getBulletinIndex(), toLogEntry(logDetails.getFileMetadata()), additionalArguments);
    }

    // TODO Logging
    private static String toLogEntry(final FileMetadata fileMetadata) {
        return fileMetadata.getFilename() + " (" + fileMetadata.getProductIdentifier() + ")";
    }

    // TODO Logging
    @FreeBuilder
    public static abstract class LogDetails {
        LogDetails() {
        }

        public static Builder builder() {
            return new LogDetails.Builder();
        }

        static LogDetails from(final FileMetadata fileMetadata, final int bulletinIndex) {
            return builder()
                    .setFileMetadata(fileMetadata)
                    .setBulletinIndex(bulletinIndex)
                    .build();
        }

        public abstract FileMetadata getFileMetadata();

        public abstract int getBulletinIndex();

        public static class Builder extends FileParser_LogDetails_Builder {
            public Builder() {
            }
        }
    }

    @FreeBuilder
    public static abstract class FileParseResult {
        FileParseResult() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public abstract List<InputAviationMessage> getInputAviationMessages();

        public abstract boolean getParseErrors();

        public static class Builder extends FileParser_FileParseResult_Builder {
            public Builder() {
            }
        }
    }

}
