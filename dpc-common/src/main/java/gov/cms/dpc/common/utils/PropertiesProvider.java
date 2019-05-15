package gov.cms.dpc.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.MissingResourceException;
import java.util.Properties;

public class PropertiesProvider {

    private static final String PROPERTIES_FILE = "app.properties";
    private static final DateTimeFormatter MAVEN_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final Properties properties;

    public PropertiesProvider() {
        this.properties = new Properties();
        try (final InputStream is = PropertiesProvider.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (is == null) {
                throw new MissingResourceException("Cannot find application properties file", PropertiesProvider.class.getName(), PROPERTIES_FILE);
            }
            this.properties.load(is);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Cannot load properties file: %s", PROPERTIES_FILE), e);
        }
    }

    public LocalDateTime getBuildTimestamp() {
        return LocalDateTime.parse(this.properties.getProperty("application.builddate"), MAVEN_FORMATTER);
    }

    public String getBuildVersion() {
        return this.properties.getProperty("application.version");
    }
}
