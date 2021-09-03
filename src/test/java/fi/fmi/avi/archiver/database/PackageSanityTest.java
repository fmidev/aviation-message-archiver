package fi.fmi.avi.archiver.database;

import com.google.common.testing.AbstractPackageSanityTests;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import java.time.Clock;

import static org.mockito.Mockito.mock;

public class PackageSanityTest extends AbstractPackageSanityTests {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setDefault(Clock.class, Clock.systemUTC());
        setDefault(NamedParameterJdbcTemplate.class, new NamedParameterJdbcTemplate(new DelegatingDataSource()));
        setDefault(DatabaseAccess.class, mock(DatabaseAccess.class));
    }

}
