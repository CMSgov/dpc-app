package gov.cms.dpc.attribution.jobs;

import com.google.inject.Injector;
import gov.cms.dpc.attribution.jdbi.RelationshipDAO;
import org.knowm.sundial.Job;
import org.knowm.sundial.SundialJobScheduler;
import org.knowm.sundial.annotations.SimpleTrigger;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@SimpleTrigger(repeatInterval = 30, timeUnit = TimeUnit.SECONDS)
public class ExpireAttributions extends Job {

    private static final Logger logger = LoggerFactory.getLogger(ExpireAttributions.class);

    @Inject
    private RelationshipDAO dao;
    @Inject
    private Duration expirationThreshold;
    private OffsetDateTime expirationTemporal;

    public ExpireAttributions() {
        final Injector attribute = (Injector) SundialJobScheduler.getServletContext().getAttribute("com.google.inject.Injector");
        attribute.injectMembers(this);
        this.expirationTemporal = OffsetDateTime.now().minus(this.expirationThreshold);
    }

    public ExpireAttributions(RelationshipDAO dao, Duration threshold) {
        this.dao = dao;
        this.expirationTemporal = OffsetDateTime.now().minus(threshold);
    }


    @Override
    public void doRun() throws JobInterruptException {
        // Calculate the expiration date (e.g. all relationships created BEFORE this time will be removed
        // Find all the jobs and remove them
        logger.debug("Removing attribution relationships created before {}.", expirationTemporal.format(DateTimeFormatter.ISO_DATE_TIME));

        this.dao
                .getAttributions()
                .stream()
                .filter((attr) -> attr.getCreated().isBefore(this.expirationTemporal))
                .forEach(relationship -> {
                    logger.debug("Removing attribution {}", relationship);
                    this.dao.removeAttributionRelationship(relationship);
                });
    }
}
