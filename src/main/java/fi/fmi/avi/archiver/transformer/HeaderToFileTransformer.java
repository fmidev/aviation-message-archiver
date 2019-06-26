package fi.fmi.avi.archiver.transformer;

import static org.springframework.integration.file.FileHeaders.ORIGINAL_FILE;

import java.io.File;

import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.handler.annotation.Header;

public class HeaderToFileTransformer {

    @Transformer
    public File transform(@Header(ORIGINAL_FILE) final File originalFile) {
        return originalFile;
    }

}
