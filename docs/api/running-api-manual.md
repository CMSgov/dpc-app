# Manual JAR execution

Individual services can be manually executed using the `server` command for each service.

When manually running the individual services, you'll need to ensure that there are no listening port collisions. By default, each service starts with the same application (8080) and admin (9900) ports. We provide a sample `application.local.conf` file which contains all the necessary configuration options. This file can be copied and used directly: `cp application.local.conf.sample application.local.conf`.

**Important Note**: The API service requires authentication before performing actions. This will cause most integration tests to fail, as they expect the endpoints to be open. Authentication can be disabled in one of two ways:

-   Set the `ENV` environment variable to `local` (which is the default when running under Docker).
-   Set `dpc.api.authenticationDisabled=true` in the config file (the default from the sample config file).

Next, start each service in a new terminal window, from within the the `dpc-app` root directory.

```bash
java -jar dpc-attribution/target/dpc-attribution.jar server
java -jar dpc-aggregation/target/dpc-aggregation.jar server
java -jar dpc-api/target/dpc-api.jar server
```

By default, the services will attempt to load the `local.application.conf` file from the current execution directory.
This can be overridden in two ways:

-   Passing `ENV={dev,test,prod}` will load a `{dev,test,prod}.application.conf` file from the service resources directory.
-   Manually specifying a configuration file after the server command `server src/main/resources/application.conf` will directly load that configuration set.

**Note**: Manually specifying a config file will disable the normal configuration merging process.
This means that only the config variables directly specified in the file will be loaded, no other `application.conf` or `reference.conf` files will be processed.

-   You can check that the application is running by requesting the FHIR `CapabilitiesStatement` for the `dpc-api` service, which will return a JSON-formatted FHIR resource.
    ```bash
    curl -H "Accept: application/fhir+json" http://localhost:3002/v1/metadata
    ```

## Starting the Database

###### [`^`](#table-of-contents)

DPC requires an external Postgres database to be running. While a separate Postgres server can be used, the `docker-compose` file includes everything needed, and can be started like so:

```bash
docker-compose up start_core_dependencies
```

**Warning**: If you do have an existing Postgres database running on port 5342, docker-compose **will not** alert you to the port conflict. Ensure any local Postgres databases are stopped before starting docker-compose.
