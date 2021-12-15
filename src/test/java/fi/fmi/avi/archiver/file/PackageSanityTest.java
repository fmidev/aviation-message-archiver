package fi.fmi.avi.archiver.file;

import com.google.common.testing.AbstractPackageSanityTests;
import fi.fmi.avi.archiver.config.model.FileConfig;
import fi.fmi.avi.model.GenericAviationWeatherMessage;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.regex.Pattern;

public class PackageSanityTest extends AbstractPackageSanityTests {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final FileConfig fileConfig = FileConfig.builder()
                .setPattern(Pattern.compile("test"))
                .setNameTimeZone(ZoneOffset.UTC)
                .setFormat(GenericAviationWeatherMessage.Format.TAC)
                .setFormatId(0)
                .build();
        final FileMetadata fileMetadata = FileMetadata.builder().setFileConfig(fileConfig).buildPartial();
        setDefault(FileConfig.class, fileConfig);
        setDefault(FileMetadata.class, fileMetadata);
        setDefault(FileParser.FileParseResult.class, FileParser.FileParseResult.builder().buildPartial());
        setDefault(FileParser.LogDetails.class, FileParser.LogDetails.builder().setFileMetadata(fileMetadata).buildPartial());
        setDefault(InputAviationMessage.class, InputAviationMessage.builder().setFileMetadata(fileMetadata).buildPartial());
        setDefault(InputBulletinHeading.class, InputBulletinHeading.builder().buildPartial());
        setDefault(ZoneId.class, ZoneOffset.UTC);
    }

}
