package gov.cms.dpc.attribution.tasks;

import com.google.common.collect.ImmutableMultimap;
import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.dao.tables.Attributions;
import gov.cms.dpc.attribution.dao.tables.Patients;
import gov.cms.dpc.attribution.dao.tables.Providers;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.servlets.tasks.Task;
import org.jooq.DSLContext;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.PrintWriter;
import java.sql.Connection;

/**
 * Admin task for truncating tables in the attribution database
 * This is mostly used for testing and the admin port will not be exposed in the higher environments
 */
@Singleton
public class TruncateDatabase extends Task {

    private final DPCAttributionConfiguration configuration;

    @Inject
    public TruncateDatabase(DPCAttributionConfiguration config) {
        super("truncate");
        this.configuration = config;
    }


    @Override
    public void execute(ImmutableMultimap<String, String> immutableMultimap, PrintWriter printWriter) throws Exception {

        // Get the db factory
        final PooledDataSourceFactory dataSourceFactory = configuration.getDatabase();
        final ManagedDataSource dataSource = dataSourceFactory.build(null, "attribution-seeder");

        try (final Connection connection = dataSource.getConnection();
             DSLContext context = DSL.using(connection, new Settings().withRenderNameStyle(RenderNameStyle.AS_IS))) {

            // Truncate everything
            context.truncate(Patients.PATIENTS).cascade().execute();
            context.truncate(Providers.PROVIDERS).cascade().execute();
            context.truncate(Attributions.ATTRIBUTIONS).cascade().execute();
        }
    }
}