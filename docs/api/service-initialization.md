# API Service Initialization

All Dropwizard APIs are initialized through the relevant io.dropwizard.core `Application` and `Configuration` instances, e.g. `DPCAPIService` and `DPCAPIConfiguration`.

## Configuration

Configuration instances are loaded from the `src/main/resources/application.yml` file, which use a `SubstitutingSourceProvider` to utilize environment variables. Environment variables are configured through one of the following means:

-   The docker-compose file (local development)
-   Parameter Store (configured and loaded via the ECS task definition)
-   The `{env}.application.env` file, which stores some non-sensitive variables.

## Database Initialization

We utilize [Hibernate](https://hibernate.org/orm/) as our Object Relational Mapping (ORM) library to map database tables to our Java classes.

Each service manages their own database schema and ensures that migrations are up to date during service initialization. This is done using the Dropwizard `MigrationsBundle`.

To create new migrations, look for the relevant `*.migrations.yml` file and follow the [Dropwizard manual here](https://www.dropwizard.io/en/stable/manual/migrations.html).

## Dependency Injection

The Dropwizard APIs are built around the idea of dependency injection. Dependency injection is the process of passing (injecting) dependencies for a class instance, typically through annotated constructor parameters.

For example, if our Dropwizard service is initialized with a Dropwizard module like this:

```java
public class DPCAPIModule extends DropwizardAwareModule<DPCAPIConfiguration> {
    @Override
    public void configure() {
        Binder binder = binder();
        // Allows the EndpointResource class to receive dependency injections
        binder.bind(EndpointResource.class);
    }

    @Provides
    @Singleton
    @Named("attribution")
    public IGenericClient provideFHIRClient(FhirContext ctx) {
        // Creates and returns a configured FHIR API client for the attribution service
    }
}
```

The Endpoint Resource can utilize the attribution client provider:

```java
public class EndpointResource {
    private final IGenericClient client;

    @Inject
    EndpointResource(@Named("attribution") IGenericClient client) {
        this.client = client;
    }
}
```

The default injector is HK2, but we utilize [dropwizard-guicey](https://github.com/xvik/dropwizard-guicey) for more advanced configuration support.
