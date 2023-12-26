package gov.cms.dpc.api.jdbi;

import gov.cms.dpc.api.entities.IpAddressEntity;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQL10Dialect;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@ExtendWith(DropwizardExtensionsSupport.class)
@Testcontainers
abstract class AbstractDAOTest {
    @Container
    private static final PostgreSQLContainer postgreSql = new PostgreSQLContainer(DockerImageName.parse("postgres:14.7"));
    protected DAOTestExtension db = DAOTestExtension.newBuilder()
            .customizeConfiguration(c -> c.setProperty(AvailableSettings.DIALECT, PostgreSQL10Dialect.class.getName()))
            .setDriver(postgreSql.getDriverClassName())
            .setUrl(postgreSql.getJdbcUrl())
            .setUsername(postgreSql.getUsername())
            .setPassword(postgreSql.getPassword())
            .addEntityClass(IpAddressEntity.class)
            .build();
}
