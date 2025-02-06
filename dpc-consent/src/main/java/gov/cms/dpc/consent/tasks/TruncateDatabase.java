package gov.cms.dpc.consent.tasks;

import gov.cms.dpc.common.utils.DBUtils;
import gov.cms.dpc.consent.DPCConsentConfiguration;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.servlets.tasks.Task;
import org.jooq.DSLContext;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * Admin task for truncating tables in the consent database
 * This is only used for testing and the admin port will not be exposed in the higher environments
 */
@Singleton
public class TruncateDatabase extends Task {

	private final DPCConsentConfiguration configuration;

	@Inject
	public TruncateDatabase(DPCConsentConfiguration config) {
		super("truncate");
		this.configuration = config;
	}


	@Override
	public void execute(Map<String, List<String>> map, PrintWriter printWriter) throws Exception {

		// Get the db factory
		final PooledDataSourceFactory dataSourceFactory = configuration.getConsentDatabase();
		final ManagedDataSource dataSource = dataSourceFactory.build(null, "truncate-db");

		try (final Connection connection = dataSource.getConnection();
			 DSLContext context = DSL.using(connection, new Settings().withRenderQuotedNames(RenderQuotedNames.EXPLICIT_DEFAULT_UNQUOTED))) {

			DBUtils.truncateAllTables(context, "public");
		}
	}

}
