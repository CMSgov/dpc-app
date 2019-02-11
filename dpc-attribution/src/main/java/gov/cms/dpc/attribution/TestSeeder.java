package gov.cms.dpc.attribution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.MissingResourceException;

public class TestSeeder implements AttributionSeeder {

    private static final String CSV = "./test_associations.csv";
    private final Logger logger = LoggerFactory.getLogger(TestSeeder.class);

    private final AttributionEngine engine;

    @Inject
    TestSeeder(AttributionEngine engine) {
        this.engine = engine;
        this.seedAttribution();
    }

    @Override
    public void seedAttribution() {
        logger.info("Seeding attributions");
        // Get the test seeds
        final InputStream resource = TestSeeder.class.getClassLoader().getResourceAsStream(CSV);
        if (resource == null) {
            throw new MissingResourceException("Can not find seeds", this.getClass().getName(), CSV);
        }

        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    final String[] splits = line.split(",");
                    logger.debug("Associating {} with {}.", splits[1], splits[0]);
                    engine.addAttributionRelationship(splits[1], splits[0]);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        logger.debug("Finished loading seeds");
    }
}
