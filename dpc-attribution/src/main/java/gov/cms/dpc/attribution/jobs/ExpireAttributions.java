package gov.cms.dpc.attribution.jobs;

import com.google.inject.Injector;
import gov.cms.dpc.attribution.dao.tables.Attributions;
import org.jooq.DSLContext;
import org.knowm.sundial.Job;
import org.knowm.sundial.SundialJobScheduler;
import org.knowm.sundial.annotations.CronTrigger;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This job runs every day at midnight to expire (remove) attribution relationships which are older than a certain threshold.
 * The value is set in the config file and defaults to 90 days.
 */
@CronTrigger(cron = "0 0 * * * ?")
public class ExpireAttributions extends Job {

    private static final Logger logger = LoggerFactory.getLogger(ExpireAttributions.class);

    @Inject
    private DSLContext context;
    @Inject
    private Duration expirationThreshold;
    private OffsetDateTime expirationTemporal;

    public ExpireAttributions() {
        final Injector attribute = (Injector) SundialJobScheduler.getServletContext().getAttribute("com.google.inject.Injector");
        attribute.injectMembers(this);
        this.expirationTemporal = OffsetDateTime.now().minus(this.expirationThreshold);
    }

    @Override
    public void doRun() throws JobInterruptException {
        // Calculate the expiration date (e.g. all relationships created BEFORE this time will be removed
        // Find all the jobs and remove them
        logger.debug("Removing attribution relationships created before {}.", expirationTemporal.format(DateTimeFormatter.ISO_DATE_TIME));

        try {

            final int removed = context
                    .delete(Attributions.ATTRIBUTIONS)
                    .where(Attributions.ATTRIBUTIONS.CREATED_AT.le(Timestamp.from(this.expirationTemporal.toInstant())))
                    .execute();
            logger.debug("Expired {} attribution relationships.", removed);
        } finally {
            context.close();
        }


    }
}
