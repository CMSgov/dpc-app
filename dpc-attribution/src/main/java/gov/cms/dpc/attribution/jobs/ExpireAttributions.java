package gov.cms.dpc.attribution.jobs;

import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.dao.tables.Attributions;
import gov.cms.dpc.attribution.exceptions.AttributionException;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.jobs.Job;
import io.dropwizard.jobs.annotations.On;

import org.jooq.DSLContext;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
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
@On("0 0 * * * ?")
public class ExpireAttributions extends Job {

    private static final Logger logger = LoggerFactory.getLogger(ExpireAttributions.class);

    @Inject
    private ManagedDataSource dataSource;
    @Inject
    private Settings settings;

    public ExpireAttributions() {}

    @Override
    public void doJob(JobExecutionContext jobContext) throws JobExecutionException {
        final OffsetDateTime expirationTemporal = OffsetDateTime.now(ZoneOffset.UTC);
        // Find all the jobs and remove them
        logger.debug("Expiring active attribution relationships before {}.", expirationTemporal.format(DateTimeFormatter.ISO_DATE_TIME));

        try (final Connection connection = this.dataSource.getConnection(); final DSLContext context = DSL.using(connection, this.settings)) {
            final int updated = context
                    .update(Attributions.ATTRIBUTIONS)
                    .set(Attributions.ATTRIBUTIONS.INACTIVE, true)
                    .where(Attributions.ATTRIBUTIONS.PERIOD_END.le(expirationTemporal))
                    .execute();
            logger.debug("Expired {} attribution relationships.", updated);
        } catch (SQLException e) {
            throw new AttributionException("Unable to open connection to database.", e);
        }

//        Remove everything that is inactive and has been expired for more than 6 months
        try (final Connection connection = this.dataSource.getConnection(); final DSLContext context = DSL.using(connection, this.settings)) {
            final int removed = context
                    .delete(Attributions.ATTRIBUTIONS)
                    .where(Attributions.ATTRIBUTIONS.PERIOD_END.le(expirationTemporal.minus(6, ChronoUnit.MONTHS))
                            .and(Attributions.ATTRIBUTIONS.INACTIVE.eq(true)))
                    .execute();
            logger.debug("Removed {} attribution relationships.", removed);
        } catch (SQLException e) {
            throw new AttributionException("Unable to open connection to database.", e);
        }
    }
}
