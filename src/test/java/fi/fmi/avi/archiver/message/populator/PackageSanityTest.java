package fi.fmi.avi.archiver.message.populator;

import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.springframework.messaging.MessageHeaders;

import com.google.common.collect.ImmutableMap;
import com.google.common.testing.AbstractPackageSanityTests;

import fi.fmi.avi.archiver.database.DatabaseAccess;
import fi.fmi.avi.archiver.file.FileMetadata;
import fi.fmi.avi.archiver.file.InputAviationMessage;
import fi.fmi.avi.archiver.file.InputBulletinHeading;
import fi.fmi.avi.archiver.message.ArchiveAviationMessage;
import fi.fmi.avi.archiver.util.GeneratedClasses;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;

@SuppressWarnings("UnstableApiUsage")
public class PackageSanityTest extends AbstractPackageSanityTests {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        ignoreClasses(GeneratedClasses::isKnownGenerated);

        setDefault(ArchiveAviationMessage.Builder.class, ArchiveAviationMessage.builder());
        setDefault(ArchiveAviationMessage.class, ArchiveAviationMessage.builder().buildPartial());
        setDefault(Clock.class, Clock.systemUTC());
        setDefault(DatabaseAccess.class, mock(DatabaseAccess.class));
        setDefault(FileMetadata.class, FileMetadata.builder().buildPartial());
        setDefault(InputAviationMessage.class, InputAviationMessage.builder().buildPartial());
        setDefault(InputAviationMessage.Builder.class, InputAviationMessage.builder());
        setDefault(InputBulletinHeading.class, InputBulletinHeading.builder().buildPartial());
        setDefault(MessageHeaders.class, new MessageHeaders(Collections.emptyMap()));
        setDefault(MessagePopulationService.PopulationResult.class, MessagePopulationService.PopulationResult.builder().buildPartial());
        setDefault(MessagePopulatorHelper.class, new MessagePopulatorHelper(Clock.systemUTC()));
        setDefault(PartialOrCompleteTimeInstant.class, PartialOrCompleteTimeInstant.builder().buildPartial());
        setDefault(PartialOrCompleteTimePeriod.class, PartialOrCompleteTimePeriod.builder().buildPartial());
        setDistinctValues(Instant.class, Instant.now(), Instant.now().plusSeconds(1));
        setDefault(String.class, "test");
        setDefault(Duration.class, Duration.ofHours(1));
        setDefault(Map.class, ImmutableMap.of());
    }

}
