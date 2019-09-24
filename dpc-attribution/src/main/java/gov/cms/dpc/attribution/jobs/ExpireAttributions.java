package gov.cms.dpc.attribution.jobs;

import com.google.inject.Injector;
import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.dao.tables.Attributions;
import gov.cms.dpc.attribution.exceptions.AttributionException;
import io.dropwizard.db.ManagedDataSource;
import org.jooq.DSLContext;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.knowm.sundial.Job;
import org.knowm.sundial.SundialJobScheduler;
import org.knowm.sundial.annotations.CronTrigger;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * This job runs every day at midnight to expire (remove) attribution relationships which are older than a certain threshold.
 * The value is set in the config file ({@link DPCAttributionConfiguration#getExpirationThreshold()}) and defaults to 90 days.
 */
@CronTrigger(cron = "0 0 * * * ?")
public class ExpireAttributions extends Job {

    private static final Logger logger = LoggerFactory.getLogger(ExpireAttributions.class);

    @Inject
    private ManagedDataSource dataSource;
    @Inject
    private Settings settings;

    public ExpireAttributions() {
        // Manually load the Guice injector. Since the job loads at the beginning of the startup process, Guice is not automatically injected.
        final Injector attribute = (Injector) SundialJobScheduler.getServletContext().getAttribute("com.google.inject.Injector");
        attribute.injectMembers(this);
    }

    @Override
    public void doRun() throws JobInterruptException {
        final OffsetDateTime expirationTemporal = OffsetDateTime.now(ZoneOffset.UTC);
        // Find all the jobs and remove them
        logger.debug("Expiring active attribution relationships before {}.", expirationTemporal.format(DateTimeFormatter.ISO_DATE_TIME));

        try (final Connection connection = this.dataSource.getConnection(); final DSLContext context = DSL.using(connection, this.settings)) {
            final int updated = context
                    .update(Attributions.ATTRIBUTIONS)
                    .set(Attributions.ATTRIBUTIONS.INACTIVE, true)
                    .set(Attributions.ATTRIBUTIONS.REMOVED_AT, expirationTemporal)
                    .where(Attributions.ATTRIBUTIONS.EXPIRES_AT.le(expirationTemporal))
                    .execute();
            logger.debug("Expired {} attribution relationships.", updated);
        } catch (SQLException e) {
            throw new AttributionException("Unable to open connection to database.", e);
        }

//        Remove everything that is inactive and has been expired for more than 6 months
        try (final Connection connection = this.dataSource.getConnection(); final DSLContext context = DSL.using(connection, this.settings)) {
            final int removed = context
                    .delete(Attributions.ATTRIBUTIONS)
                    .where(Attributions.ATTRIBUTIONS.REMOVED_AT.le(expirationTemporal.minus(6, ChronoUnit.MONTHS))
                            .and(Attributions.ATTRIBUTIONS.INACTIVE.eq(true)))
                    .execute();
            logger.debug("Removed {} attribution relationships.", removed);
        } catch (SQLException e) {
            throw new AttributionException("Unable to open connection to database.", e);
        }
    }
}
