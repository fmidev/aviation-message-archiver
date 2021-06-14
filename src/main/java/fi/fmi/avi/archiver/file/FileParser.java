package fi.fmi.avi.archiver.file;

import fi.fmi.avi.archiver.initializing.MessageFileMonitorInitializer;
import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.MessageFormat;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.util.BulletinHeadingEncoder;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageHeaders;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class FileParser {

    private final AviMessageConverter aviMessageConverter;

    public FileParser(final AviMessageConverter aviMessageConverter) {
        this.aviMessageConverter = requireNonNull(aviMessageConverter, "aviMessageConverter");
    }

    @ServiceActivator
    public List<FileAviationMessage> parse(final String content, final MessageHeaders headers) {
        final FilenamePattern pattern = (FilenamePattern) headers.get(MessageFileMonitorInitializer.MESSAGE_FILE_PATTERN);
        final Instant fileLastModified = (Instant) headers.get(MessageFileMonitorInitializer.FILE_LAST_MODIFIED);
        final String productIdentifier = (String) headers.get(MessageFileMonitorInitializer.PRODUCT_IDENTIFIER);
        return parse(productIdentifier, pattern, content.trim(), fileLastModified);
    }

    public List<FileAviationMessage> parse(final String productIdentifier, final FilenamePattern filenamePattern, final String content,
                                           @Nullable final Instant fileLastModified) {
        final ConversionResult<GenericMeteorologicalBulletin> bulletinConversion = aviMessageConverter.convertMessage(content,
                TACConverter.TAC_TO_GENERIC_BULLETIN_POJO);
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

                    return FileAviationMessage.builder()//
                            .setGtsBulletinHeading(bulletin.getHeading())//
                            .setGtsBulletinHeadingString(BulletinHeadingEncoder.encode(bulletin.getHeading(), MessageFormat.TEXT, null))//
                            .setFilenamePattern(filenamePattern)//
                            .setProductIdentifier(productIdentifier)
                            .setFileModified(fileLastModified)//
                            .setMessage(message)
                            .build();
                }).collect(Collectors.toList());
    }

}
