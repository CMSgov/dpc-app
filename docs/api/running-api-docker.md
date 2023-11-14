## Building the DPC API

###### [`^`](#table-of-contents)

### How the API Works

By default, the API components will attempt to connect to the `dpc_attribution`, `dpc_queue`, and `dpc_auth` databases on the localhost as the `postgres` user with a password of `dpc-safe`.

All of these databases should be created automatically from the previous step. When the API applications start, migrations will run and initialize the databases with the correct tables and data. If this behavior is not desired, set an environment variable of `DB_MIGRATION=0`.

The defaults can be overridden in the configuration files.
Common configuration options (such as database connection strings) are stored in a [server.conf](src/main/resources/server.conf) file and included in the various modules via the `include "server.conf"` attribute in module application config files.
See the `dpc-attribution` [application.conf](dpc-attribution/src/main/resources/application.conf) for an example.

Default settings can be overridden, either directly in the module configurations or via an `application.local.conf` file in the project root directory.
For example, modifying the `dpc-attribution` configuration:

```yaml
dpc.attribution {
database = {
driverClass = org.postgresql.Driver
url = "jdbc:postgresql://localhost:5432/dpc-dev"
user = postgres
}
}
```

**Note**: On startup, the services look for a local override file (application.local.conf) in the root of their _current_ working directory. This can create an issue when running tests with IntelliJ. The default sets the working directory to be the module root, which means any local overrides are ignored.
This can be fixed by setting the working directory to the project root, but needs to be done manually.

### There are two ways to build DPC:

##### Option 1: Full integration test

Run `make ci-app`. This will start the dependencies, build all components, run integration tests, and run a full end-to-end test. You will be left with compiled JARs for each component, as well as compiled Docker containers.

##### Option 2: Manually

Run `make docker-base` to build the common, baseline Docker image (i.e., `dpc-base:latest`) used across DPC services.

Next, in order to make the decrypted environment variables accessible to Maven, run `make maven-config`.
This command will convert the contents of `ops/config/decrypted/local.env` to Maven flags which can be viewed in `.mvn/maven.config` if successful.

Then, run `mvn clean install` to build and test the application. Dependencies will need to be up and running for this option to succeed.

Running `mvn clean install` will also construct the Docker images for the individual services. To skip the Docker build, pass `-Djib.skip=True`.

Note that the `dpc-base` image produced by `make docker-base` is not stored in a remote repository. The `mvn clean install` process relies on the base image being available via the local Docker daemon.

## Running the DPC API

###### [`^`](#table-of-contents)

Once the JARs are built, they can be run in two ways, either via [`docker-compose`](https://docs.docker.com/compose/overview/) or by manually running the JARs.

### Running the DPC API via Docker

Click on [Install Docker](https://www.docker.com/products/docker-desktop) to set up Docker.
The application (along with all required dependencies) can be automatically started with the following command: `make start-app`.
The individual services can be started (along with their dependencies) by passing the service name to the `up` command.

```bash
docker-compose up {db,aggregation,attribution,api}
```

By default, the Docker containers start with minimal authentication enabled, meaning that some functionality (such as extracting the organization_id from the access token) will not work as expected and always returns the same value.
This can be overridden during startup by setting the `AUTH_DISABLED=false` environment variable.

When running locally, you'll need to update the docker-compse.yml file by adding:

```yaml
ports:
    - "5432:5432"
```

in the `db` node e.g.

```yaml
db:
    image: postgres:11
    ports:
        - "5432:5432"
```
