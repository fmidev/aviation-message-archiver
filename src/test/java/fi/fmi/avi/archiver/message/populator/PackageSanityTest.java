package fi.fmi.avi.archiver.message.populator;

import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;

import org.springframework.messaging.MessageHeaders;

import com.google.common.testing.AbstractPackageSanityTests;

import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;

public class PackageSanityTest extends AbstractPackageSanityTests {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setDefault(ArchiveAviationMessage.Builder.class, ArchiveAviationMessage.builder());
        setDefault(Clock.class, Clock.systemUTC());
        setDefault(DatabaseAccess.class, mock(DatabaseAccess.class));
        setDefault(FileMetadata.class, FileMetadata.builder().buildPartial());
        setDefault(InputAviationMessage.class, InputAviationMessage.builder().buildPartial());
        setDefault(MessageHeaders.class, new MessageHeaders(Collections.emptyMap()));
        setDefault(MessagePopulatorHelper.class, new MessagePopulatorHelper(Clock.systemUTC()));
        setDefault(PartialOrCompleteTimeInstant.class, PartialOrCompleteTimeInstant.builder().buildPartial());
        setDefault(PartialOrCompleteTimePeriod.class, PartialOrCompleteTimePeriod.builder().buildPartial());
        setDistinctValues(Instant.class, Instant.now(), Instant.now().plusSeconds(1));
    }

}
