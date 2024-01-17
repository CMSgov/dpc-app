package gov.cms.dpc.testing;

import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.util.Generics;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQL10Dialect;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * This is based around the DAOTestExtension that DropWizard already supports, except instead of using an H2 database
 * it uses PostgreSql in a test container.  It will provide each test with a fresh copy of the DB and take care of setting
 * it up and tearing it down.
 * @see <a href="https://www.dropwizard.io/en/latest/manual/testing.html#testing-database-interactions">DropWizard DB Testing</a>
 * @see <a href="https://testcontainers.com">TestContainers</a>
 */
@ExtendWith(DropwizardExtensionsSupport.class)
@Testcontainers
public abstract class AbstractDAOTest<E> {
    @Container
    private final PostgreSQLContainer postgreSql = new PostgreSQLContainer(DockerImageName.parse("postgres:14.7"));

    protected DAOTestExtension db;

    protected AbstractDAOTest() {
        postgreSql.start();

        db = DAOTestExtension.newBuilder()
            .customizeConfiguration(c -> c.setProperty(AvailableSettings.DIALECT, PostgreSQL10Dialect.class.getName()))
            .setDriver(postgreSql.getDriverClassName())
            .setUrl(postgreSql.getJdbcUrl())
            .setUsername(postgreSql.getUsername())
            .setPassword(postgreSql.getPassword())
            .addEntityClass(Generics.getTypeParameter(this.getClass()))
            .build();
    }
}
