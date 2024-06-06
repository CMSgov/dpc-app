package gov.cms.dpc.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.stream.Stream;

public class PropertiesProvider {

    private static final String[] PROPERTIES_FILES = {"app.properties", "git.properties"};
    private static final DateTimeFormatter MAVEN_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");

    private final Properties properties;

    public PropertiesProvider() {
        this.properties = new Properties();
        Stream.of(PROPERTIES_FILES)
                .forEach(this::loadPropertyFile);
    }

    public OffsetDateTime getBuildTimestamp() {
        System.out.println("builddate: " + this.properties.getProperty("application.builddate"));
        return OffsetDateTime.parse(this.properties.getProperty("application.builddate"), MAVEN_FORMATTER);
    }

    /**
     * Returns the application version (e.g. The maven version 0.4.0-SNAPSHOT)
     *
     * @return - {@link String} application version
     */
    public String getApplicationVersion() {
        return this.properties.getProperty("application.version");
    }

    /**
     * Returns the application build version, which is the first 7 characters of the commit sha, and the build timestamp.
     *
     * @return - {@link String} application build version
     */
    public String getBuildVersion() {
        final String commitAbbrev = this.properties.getProperty("git.commit.id.abbrev");
        return String.format("%s.%s", commitAbbrev, getBuildTimestamp());
    }

    private void loadPropertyFile(String file) {
        try (final InputStream is = PropertiesProvider.class.getClassLoader().getResourceAsStream(file)) {
            if (is == null) {
                throw new MissingResourceException("Cannot find application properties file", PropertiesProvider.class.getName(), file);
            }
            this.properties.load(is);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Cannot load properties file: %s", file), e);
        }
    }
}
