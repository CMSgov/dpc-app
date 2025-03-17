package gov.cms.dpc.common.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Properties;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertiesProviderUnitTest {

    static PropertiesProvider provider;
    static Properties properties = new Properties();

    static final List<String> PROPERTIES_FILES = List.of("app.properties", "git.properties");

    @BeforeAll
    static void setUp() {
        provider = new PropertiesProvider();
        PROPERTIES_FILES.forEach(file -> {
            try (InputStream stream = PropertiesProvider.class.getClassLoader().getResourceAsStream(file)) {
                if (stream == null) {
                    throw new MissingResourceException("Cannot find application properties file", PropertiesProviderUnitTest.class.getName(), file);
                }
                properties.load(stream);
            } catch (IOException e) {
                throw new IllegalStateException(String.format("Cannot load properties file: %s", file), e);
            }
        });
    }

    @Test
    void getBuildTimestamp() {
        OffsetDateTime expectedTimestamp = OffsetDateTime.parse(properties.getProperty("application.builddate"), ISO_OFFSET_DATE_TIME);
        assertEquals(expectedTimestamp, provider.getBuildTimestamp());
    }

    @Test
    void getApplicationVersion() {
        String expectedApplicationVersion = properties.getProperty("application.version");
        assertEquals(expectedApplicationVersion, provider.getApplicationVersion());
    }

    @Test
    void getBuildVersion() {
        OffsetDateTime expectedTimestamp = OffsetDateTime.parse(properties.getProperty("application.builddate"), ISO_OFFSET_DATE_TIME);
        String expectedBuildVersion = String.format("%s.%s", properties.getProperty("git.commit.id.abbrev"), expectedTimestamp);
        assertEquals(expectedBuildVersion, provider.getBuildVersion());
    }

}
