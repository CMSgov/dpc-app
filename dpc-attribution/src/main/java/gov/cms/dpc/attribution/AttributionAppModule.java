package gov.cms.dpc.attribution;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import gov.cms.dpc.attribution.jdbi.AttributedPatientsDAO;
import gov.cms.dpc.attribution.models.AttributionRelationship;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.migrations.MigrationsBundle;
import org.hibernate.SessionFactory;

class AttributionAppModule extends DropwizardAwareModule<DPCAttributionConfiguration> {

    private final HibernateBundle<DPCAttributionConfiguration> hibernateBundle;

    AttributionAppModule() {

        hibernateBundle = new HibernateBundle<>(AttributionRelationship.class) {

            @Override
            public PooledDataSourceFactory getDataSourceFactory(DPCAttributionConfiguration configuration) {
                return configuration.getDatabase();
            }
            // Not used
        };
    }

    @Override
    public void configure(Binder binder) {

        binder.bind(AttributedPatientsDAO.class);
//        binder.bind(SeedCommand.class);
    }

    @Provides
    SessionFactory getSessionFactory() {
        return hibernateBundle.getSessionFactory();
    }
}
