package gov.cms.dpc.attribution.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoOpSeeder implements AttributionSeeder {

    private static final Logger logger = LoggerFactory.getLogger(NoOpSeeder.class);

    @Override
    public void seedAttribution() {
        logger.info("Using No-Op seeder, no data is actually loaded.");
    }
}
