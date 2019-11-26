package gov.cms.dpc.attribution.cli;

import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import io.dropwizard.Application;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class ImportCommand extends EnvironmentCommand<DPCAttributionConfiguration> {

    public ImportCommand(Application<DPCAttributionConfiguration> application) {
        super(application, "import", "Import into the website");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);
    }

    @Override
    protected void run(Environment environment, Namespace namespace, DPCAttributionConfiguration configuration) throws Exception {
        final PooledDataSourceFactory attDataSourceFactory = configuration.getDatabase();
        final ManagedDataSource attDataSource = attDataSourceFactory.build(null, "attribution-import");

        final PooledDataSourceFactory webDataSourceFactory = configuration.getDatabase_website();
        final ManagedDataSource webDataSource= webDataSourceFactory.build(null, "website-import");

        try ( final Connection attConnection = attDataSource.getConnection() ) {
            try ( final Connection webConnection = webDataSource.getConnection() ) {
                this.startImportProcess(attConnection, webConnection);
            }
        }
    }

    protected void startImportProcess(Connection attConnection, Connection webConnection) throws Exception {
        Statement statement = attConnection.createStatement();
        ResultSet resultSet = statement.executeQuery(
                "select o.id, o.id_value, o.organization_name, e.id " +
                        "from organizations o " +
                        "left join organization_endpoints e ON e.organization_id = o.id " +
                        "limit 1"// +
                        //"where organization_name not in ('Template Provider Organization');"
        );

        while ( resultSet.next() ) {
            String id = resultSet.getString(1);
            String npi = resultSet.getString(2);
            String name = resultSet.getString(3);
            String endpointId = resultSet.getString(4);

            this.importIntoWebsite(webConnection, id, npi, name, endpointId);
        }
    }

    protected void importIntoWebsite(Connection webConnection, String id, String npi, String name, String endpointId) throws Exception {
        String orgInsert = String.format(
                "INSERT INTO organizations(name, npi, organization_type, num_providers, api_environments, created_at, updated_at) VALUES ('%s', '%s', 0, 0, '{0}', now(), now())",
                name,
                npi
        );
        System.out.println(orgInsert);
        PreparedStatement orgInsertStatement = webConnection.prepareStatement(orgInsert, Statement.RETURN_GENERATED_KEYS);
        orgInsertStatement.executeUpdate();

        String createdOrgId = "";
        ResultSet resultSet = orgInsertStatement.getGeneratedKeys();
        if ( resultSet.next() ) {
            createdOrgId = resultSet.getString(1);
        }

        String regOrgInsert = String.format(
                "INSERT INTO registered_organizations(organization_id, api_id, api_env, api_endpoint_ref, created_at, updated_at) VALUES ('%s', '%s', 0, 'Endpoint/%s', now(), now())",
                createdOrgId,
                id,
                endpointId
        );
        System.out.println(regOrgInsert);
        PreparedStatement regOrgInsertStatement = webConnection.prepareStatement(regOrgInsert);
        regOrgInsertStatement.executeUpdate();

        String fhirInsert = String.format(
                "INSERT INTO fhir_endpoints(name, status, uri, organization_id) VALUES ('DPC Sandbox Test Endpoint', 0, 'https://dpc.cms.gov/test-endpoint', '%s')",
                createdOrgId
        );
        System.out.println(fhirInsert);
        PreparedStatement fhirInsertStatement = webConnection.prepareStatement(fhirInsert);
        fhirInsertStatement.executeUpdate();
    }
}
