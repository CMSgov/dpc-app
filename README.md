# User Guide
This document serves as a guide for running the Data at the Point of Care (DPC) API on your local environment. 


[![Build Status](https://travis-ci.org/CMSgov/dpc-app.svg?branch=master)](https://travis-ci.org/CMSgov/dpc-app)
[![Maintainability](https://api.codeclimate.com/v1/badges/46309e9b1877a7b18324/maintainability)](https://codeclimate.com/github/CMSgov/dpc-app/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/46309e9b1877a7b18324/test_coverage)](https://codeclimate.com/github/CMSgov/dpc-app/test_coverage)



<!-- TOC -->
## Table of Contents
* [What Is DPC?](#what-is-dpc)
* [Components](#components)
  * [Main Services](#main-services)
  * [Shared Modules](#shared-modules)
* [Local Development Setup](#tech-environment)
   * [Required Dependencies](#required-dependencies)
   * [Recommended tools](#recommended-tools)
   * [Installing and Using Pre-Commit](#installing-and-using-pre-commit)
   * [Quickstart](#quickstart)
 * [Managing Encrypted Files](#managing-encrypted-files)
   * [Re-encrypting files](#re-encrypting-files)
 * [Starting the Database](#starting-the-database)
 * [Building the DPC API](#building-dpc)
     * [How the API Works](#how-the-api-works)
     * [Option 1: Full integration test](#option-1-full-integration-test)
     * [Option 2: Manually](#option-2-manually)
  * [Running the DPC API](#running-dpc)
     * [Running the DPC API via Docker](#running-dpc-via-docker)
     * [Generating a golden macaroon](#generating-a-golden-macaroon)
     * [Manual JAR execution](#manual-jar-execution)
  * [Seeding the Database](#seeding-the-database)
  * [Testing the Application](#testing-the-application)
    * [Demo client](#demo-client)
    * [Manual testing](#manual-testing)
    * [Smoke tests](#smoke-tests)
  * [Generating the Source Code Documentation via JavaDoc](#generating-the-source-code-documentation-via-javadoc)
  * [Building the Additional Services](#building-the-additional-services)
    * [Postman collection](#postman-collection)
  * [Code Coverage](#code-coverage)
  * [Local Debugging](#local-debugging)
  * [Debugging Integration Tests](#debugging-integration-tests)
  * [Other Notes](#other-notes)
    * [BFD transaction time details](#bfd-transaction-time-details)
  * [Troubleshooting](#troubleshooting) 
<!-- TOC -->


## What Is DPC?

DPC is a pilot application programming interface (API) whose goal is to enable healthcare
providers to deliver high quality care directly to Medicare beneficiaries. See 
[DPC One-Pager](https://dpc.cms.gov/assets/downloads/dpc-one-pager.pdf) and the [DPC Website](https://dpc.cms.gov/) to learn more about the API.

## Components

#### Main Services

The DPC application is split into multiple services.

| Service|Type|Description|Stack|
|-------------------------------------|---|----------------------------------------------------------------------------------------|---|
| [dpc-web](/dpc-web)                 |Public Portal| Portal for managing organizations (Sandbox only, and soon to be deprecated)            |Ruby on Rails|
| [dpc-admin](/dpc-admin)             |Internal Portal| Administrative Portal for managing organizations (Sandbox only, and soon to be deprecated) |Ruby on Rails|
| [dpc-portal](/dpc-portal)           |Public Portal| Portal for managing organizations                                                      |Ruby on Rails|
| [dpc-api](/dpc-api)                 |Public API| Asynchronous FHIR API for managing organizations and requesting or retrieving data     |Java (Dropwizard)|
| [dpc-attribution](/dpc-attribution) |Internal API| Provides and updates data about attribution                                            |Java (Dropwizard)|
| [dpc-consent](/dpc-consent)         |Internal API| Provides and updates information about data-sharing consent for individuals            |Java (Dropwizard)|
| [dpc-queue](/dpc-queue)             |Internal API| Provides and updates data about export jobs and batches                                |Java (Dropwizard)|
| [dpc-aggregation](/dpc-aggregation) |Internal Worker Service| Polls for job batches and exports data for singular batches                            |Java (Dropwizard + RxJava)|

#### Shared Modules

In addition to services, several modules are shared across components.

|Module Name|Description|Stack|
|---|---|---|
|[dpc-bluebutton](/dpc-bluebutton)|Bluebutton API Client|Java|
|[dpc-macaroons](/dpc-macaroons)|Implementation of macaroons for authentication|Java|
|[dpc-common](/dpc-common)|Shared utilities for components|Java|
|[dpc-testing](/dpc-testing)|Shared utilities for testing|Java|
|[dpc-smoketest](/dpc-smoketest)|Smoke test suite|Java|
|[engines](/engines)|Shared engines|Ruby|

## Local Development Setup
###### [`^`](#table-of-contents)

### Required Dependencies

When running the applications locally, you'll want to run everything through Docker. This simplifies the process of spinning up multiple services, connecting them together, and upgrading tooling versions over time.

In that scenario, you only need the following dependencies:

- Install [Ansible Vault](https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html#)
- Install [Docker Desktop](https://docs.docker.com/install/) (make sure to allocate more than the default 2Gb of memory)
- Install [Pre-commit](https://pre-commit.com/) with [Gitleaks](https://github.com/gitleaks/gitleaks)

If you want to build applications locally, you'll need the following tools:

- Ruby and `bundler`
- Java 11 and Maven (`mvn`)

> **Note:** DPC only supports Java 11 due to our use of new languages features, which prevents using older JDK versions. 
>
> In addition, some of the upstream dependencies have not been updated to support Java 12 and newer, but we plan on adding support at a later date. 

In addition, it's helpful to have the following installed for more specific scenarios:

- Running [smoke tests](#smoke-tests): Python 3 (includes `pip`)
- Running [postman tests](#postman-collection): Node.js (includes `npm`)

### Recommended tools

For development, we recommend the following tooling:

- Code Editor: JetBrains [Intelli-J Idea IDE](https://jetbrains.com/idea) or [Visual Studio Code](https://code.visualstudio.com/)
- Database browser: [PgAdmin](https://pgadmin.org) or [Postico](https://postico.com) *(MacOS)*
- API browser and testing tool: [Postman](https://www.postman.com/downloads/)

### Installing and Using Pre-commit

Anyone committing to this repo must use the pre-commit hook to lower the likelihood that secrets will be exposed.

#### Step 1: Install pre-commit

You can install pre-commit using the MacOS package manager Homebrew:

```sh
brew install pre-commit
```

Other installation options can be found in the [pre-commit documentation](https://pre-commit.com/#install).

#### Step 2: Install the hooks

Run the following command to install the gitleaks hook:

```sh
pre-commit install
```

This will download and install the pre-commit hooks specified in `.pre-commit-config.yaml`.

### Quickstart

The fastest way to get started with building and running the applications is to follow [the Quickstart guide](/QuickStart.md). However, you can see below for more granular details.

## Managing Encrypted Files
###### [`^`](#table-of-contents)

The files committed in the `ops/config/encrypted` directory hold secret information, and are encrypted with [Ansible Vault](https://docs.ansible.com/ansible/2.4/vault.html).

Before building the app or running any tests, the decrypted secrets must be available as environment variables.

In order to encrypt and decrypt configuration variables, you must create a `.vault_password` file in the root directory. Contact another team member to gain access to the vault password.

Run the following to decrypt the encrypted files:

```sh
make secure-envs
```

If decrypted successfully, you will see the decrypted data in new files under `/ops/config/decrypted` with the same names as the corresponding encrypted files.

### Re-encrypting files

To re-encrypt files after updating them, you can run the following command:

```
./ops/scripts/secrets --encrypt <filename>
```

Note that this will always generate a unique hash, even if you didn't change the file.
## Starting the Database
###### [`^`](#table-of-contents)


DPC requires an external Postgres database to be running. While a separate Postgres server can be used, the `docker-compose` file includes everything needed, and can be started like so: 

```bash
docker-compose up start_core_dependencies
```

**Warning**: If you do have an existing Postgres database running on port 5342, docker-compose **will not** alert you to the port conflict. Ensure any local Postgres databases are stopped before starting docker-compose.

## Building the DPC API
###### [`^`](#table-of-contents)

### How the API Works

By default, the API components will attempt to connect to the `dpc_attribution`, `dpc_queue`, and `dpc_auth` databases on the localhost as the `postgres` user with a password of `dpc-safe`.

All of these databases should be created automatically from the previous step. When the API applications start, migrations will run and initialize the databases with the correct tables and data. If this behavior is not desired, set an environment variable of `DB_MIGRATION=0`.

Default settings can be overridden, either directly in the module configurations or via `local.application.env` file in the project resources directory. 
For example, modifying the `dpc-attribution` configuration:

```yaml
database:
  driverClass: org.postgresql.Driver
  url: "jdbc:postgresql://localhost:5432/dpc-dev"
  user: postgres
```

**Note**: On startup, the services look for a local override file (local.application.env) in the root of their *current* working directory. This can create an issue when running tests with IntelliJ. The default sets the working directory to be the module root, which means any local overrides are ignored.
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
### Generating a golden macaroon

You will need a macaroon for the Docker configuration. Run the command below to generate one:
`curl -X POST http://localhost:9903/tasks/generate-token`


Also, the docker-compose.portal.yml file requires adding the **`API_METADATA URL`** variable and the **`GOLDEN_MACAROON`** variable.
```yaml
dpc-web: 
  ... 
environments: 
  ... 
  - GOLDEN_MACAROON: ...  
  - API_METADATA_URL=http://host.docker.internal:3002/v1
  .. 
dpc_admin: 
  ...
  - API_METADATA_URL=${API_METADATA URL}
  - GOLDEN_MACAROON: ...
```

### Manual JAR execution

Alternatively, the individual services can be manually executing the `server` command for the various services.

When manually running the individual services, you'll need to ensure that there are no listening port collisions. By default, each service starts with the same application (8080) and admin (9900) ports. We provide a sample `application.local.conf` file which contains all the necessary configuration options. This file can be copied and used directly: `cp application.local.conf.sample application.local.conf`.

**Important Note**: The API service requires authentication before performing actions. This will cause most integration tests to fail, as they expect the endpoints to be open. Authentication can be disabled in one of two ways: 
* Set the `ENV` environment variable to `local` (which is the default when running under Docker).
* Set `authenticationDisabled=true` in the config file (the default from the sample config file).   

Next, start each service in a new terminal window, from within the `dpc-app` root directory. 

```bash
java -jar dpc-attribution/target/dpc-attribution.jar server
java -jar dpc-aggregation/target/dpc-aggregation.jar server
java -jar dpc-api/target/dpc-api.jar server
```

By default, the services will attempt to load the `local.application.env` file from the current execution directory. 
This can be overridden by passing `ENV={dev,test,prod}`, which will load `{dev,test,prod}.application.env` file from the service resources directory.

**Note**: Manually specifying a config file will disable the normal configuration merging process. 
This means that only the config variables directly specified in the file will be loaded, no other `application.env` files will be processed. 

* You can check that the application is running by requesting the FHIR `CapabilitiesStatement` for the `dpc-api` service, which will return a JSON-formatted FHIR resource.
    ```bash
    curl -H "Accept: application/fhir+json" http://localhost:3002/v1/metadata
    ```

## Seeding the Database
###### [`^`](#table-of-contents)

**Note**: This step is not required when directly running the `demo` for the `dpc-api` service, which partially seeds the database on first execution.

By default, DPC initially starts with an empty attribution database, which means that no patients have been attributed to any providers and thus nothing can be exported from BlueButton2.0.

In order to successfully test and demonstrate the application, there needs to be initial data loaded into the attribution database.
We provide a small CSV [file](src/main/resources/test_associations.csv) which associates some fake providers with valid patients from the BlueButton2.0 Sandbox.

The database can be automatically migrated and seeded by running `make seed-db` or by using the following commands:

**Note:** For instances where one cannot set up the DPC due to authorization issues, follow the steps in the [manual table setup document](DbTables.md) to populate the necessary tables manually. 

```bash
java -jar dpc-attribution/target/dpc-attribution.jar db migrate
java -jar dpc-attribution/target/dpc-attribution.jar seed
``` 

## Testing the Application
###### [`^`](#table-of-contents)

### Demo client

The `dpc-api` component contains a `demo` command, which illustrates the basic workflow for submitting an export request and modifying an attribution roster.
It can be executed with the following command:

`java -jar dpc-api/target/dpc-api.jar demo`

**Note**: The demo client expects the entire system (all databases and services) to be running from a new state (no data in the database).
This is the default when starting the services from the docker-compose file.
When running the JARs manually, the user will need to ensure that the `dpc_attribution` database is truncated after each run. 

The demo performs the following actions:

1. Makes an export request for a given provider. This request fails because the provider is not registered with the application and has no attributed patients.
1. Generates an attribution roster for the provider using the [test_association.csv](src/main/resources/test_associations.csv) file.
1. Resubmits the original Export request.
1. Polls the Job endpoint using the URL returned from the Export request and waits for a completed status.
1. Outputs the download URLs for all files generated by the Export request.

### Manual testing

The recommended method for testing the services is with the [Postman](https://www.getpostman.com) application.
This allows easy visualization of responses, as well as simplifies adding the necessary HTTP headers.

Steps for testing the data export:

1. Start the services using either the `docker-compose` command or through manually running the JARs. If running the JARs manually, you will need to migrate and seed the database before continuing. 
1. Make an initial GET request to the following endpoint: `http://localhost:3002/v1/Group/3461C774-B48F-11E8-96F8-529269fb1459/$export`.
This will request a data export for all the patients attribution to provider: `3461C774-B48F-11E8-96F8-529269fb1459`.
You will need to set the Accept header to `application/fhir+json` (per the FHIR bulk spec).
1. The response from the Export endpoint should be a **204** with the `Content-Location` header containing a URL which the user can use to to check the status of the job. 
1. Make a GET request using the URL provided by the `/Group` endpoint from the previous step.
 Which has this format: `http://localhost:3002/v1/Jobs/{unique UUID of export job}`.
 You will need to ensure that the Accept header is set to `application/fhir+json` (per the FHIR bulk spec).
 You will need to ensure that the Prefer header is set to `respond-async`.
 The server should return a **204** response until the job has completed.
 Once the job is complete, the endpoint should return data in the following format (the actual values will be different):
 
     ```javascript
    {
        "transactionTime": 1550868647.776162,
        "request": "http://localhost:3002/v1/Job/de00da66-86cf-4be1-a2a8-0415b21a6a9b",
        "requiresAccessToken": false,
        "output": [
            "http://localhost:3002/v1/Data/de00da66-86cf-4be1-a2a8-0415b21a6a9b.ndjson"
        ],
        "error": []
    }
    ```
    The output array contains a list of URLs where the exported files can be downloaded from.
1. Download the exported files by calling the `/Data` endpoint with URLs provided (e.g., `http://localhost:3002/v1/Data/de00da66-86cf-4be1-a2a8-0415b21a6a9b.ndjson`).
1. Enjoy your glorious ND-JSON-formatted FHIR data.


### Smoke tests

Smoke tests are provided by [Taurus](https://github.com/Blazemeter/taurus) and [JMeter](https://jmeter.apache.org).
The tests can be run by the environment-specific Makefile commands (e.g., `make smoke/local` will run the smoke tests against the locally running Docker instances).

In order to run the tests, you'll need to ensure that `virtualenv` is installed.

```bash
pip3 install virtualenv
```
## Generating the Source Code Documentation via JavaDoc 
###### [`^`](#table-of-contents)

The entire project's code base documentation can be generated in HTML format by using the Java's
JavaDoc tool. The [Intelli-J Idea](https://jetbrains.com/idea) integrated development environment makes this easy to do. Navigate to the **Tools>Generate JavaDoc** menu item, specify the scope of the documentation and the output location, and you'll be able to view an interactive document outlining the code members.


## Building the Additional Services
###### [`^`](#table-of-contents)

- Documentation on building the DPC Portal is covered in the specific [README](dpc-portal/README.md).
- Documentation on building the DPC Website is covered in the specific [README](dpc-web/README.md).


### Postman collection

Note: Prior to running the tests, ensure that you've updated these Postman Environment variables: 
- organization-id
- client-token
- public-key
- private-key

Once the development environment is up and running, you should now be able to run some calls to the API via the DPC Postman Collections. Below, are some useful endpoints for verifying a functional development environment:
- Register single patient
- Register practitioner
- Get all groups
- Add patient to group
- Create export data request


## Code Coverage
###### [`^`](#table-of-contents)

- Run `make unit-tests` to use Jacoco to generate local code coverage reports.  Within each module, the human-readable report can be found at `module/target/site/jacoco/index.html`.  The machine-readable version that gets loaded to SonarQube is `jacoco.xml` in the same directory.
- Stand up a local version of SonarQube inside a Docker container as described [here](https://docs.sonarsource.com/sonarqube/latest/try-out-sonarqube/).  Essentially, just run the following command `docker run -d --name sonarqube -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true -p 9000:9000 sonarqube:latest`.
  - Login to SonarQube at http://localhost:9000 with login:pass of admin:admin.
  - Setup a new project as described in the link above.
- Run the following command to load your coverage data into SonarQube, inserting your project key, name and token...
    ```
    mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.7.0.1746:sonar \
      -Dsonar.projectKey={YOUR PROJECT KEY} \
      -Dsonar.projectName='{YOUR PROJECT NAME}' \
      -Dsonar.host.url=http://localhost:9000 \
      -Dsonar.token={YOUR PROJECT TOKEN}
    ```
- Your code coverage results should now be in your local version of SonarQube.

## Local Debugging
###### [`^`](#table-of-contents)

If you're running locally through Docker and you want to use your debugger there are two steps.
- Open up port 5005 on whichever service you want to debug
  - Add the following to docker-compose.yml under api, aggregation, attribution or consent.
    ```    
    ports:
        - "5005:5005"
    ```
- Instead of using `make start-dpc` or `make start-app` to start the application, use `make start-dpc-debug` or `make start-app-debug`.
  - They'll both do a clean compile of the app with debug information and start each service with the debug agent.
- Now you can attach your debugger to the running app on port 5005.
  - If you're using IntelliJ, there are instructions [here](https://www.jetbrains.com/help/idea/attaching-to-local-process.html#attach-to-remote).


## Debugging Integration Tests
###### [`^`](#table-of-contents)
If you want to run and debug integration tests through IntelliJ there are a few steps you have to do first.  The same concepts should apply to VS Code, but you'll have to figure out the details yourself.
- When running a test in the IDE, IntelliJ creates a temporary debug configuration.  We need to make sure our secure env variables get included.
  - Go to Run -> Edit Configurations
  - Click Edit Configuration Templates and select JUnit
  - At the bottom, add a new .env file and point it to `ops/config/decrypted/local.env`
- We need to start our dependent services, so run `make start-it-debug`
  - This will recompile dpc with debug extensions included and start containers for dpc-attribution, dpc-aggregation, dpc-consent and a db.
- Now you should be able to run any of the integration tests under dpc-api by clicking on the little green arrow next to their implementation.
  - Need to debug a test?  Right click on the triangle and select debug.
- If you have to debug one of the dependant services, for instance because an IT is calling dpc-attribution and getting a 500, and you can't figure out why, follow the instructions under [Local Debugging](#local-debugging) to open up the dependant service's debugger port in docker-compose, then rerun `make start-it-debug`.
  - Now you can attach your debugger to that service and still run integration tests as described above.
  - You'll have one debugger tab open on an IT in dpc-api and another on the dependant service, allowing you to set break points in either and examine the test end to end.

#### Running Integration Tests Against the BFD Sandbox
Want to run your integration tests against the real BFD sandbox instead of using the MockBlueButtonClient?  In docker-compose.yml, under the aggregation service, set the USE_BFD_MOCK env variable to true and then rerun `make start-it-debug.`

Note: Many of our integration tests are written for specific test data that only exists in our MockBlueButtonClient.  If you switch to the real BFD sandbox these tests will fail, but if you want a true end to end test this is the way to go.  A list of synthetic patients in the sandbox can be found [here](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Synthetic-Data-Guide).

## Other Notes
###### [`^`](#table-of-contents)

### BFD transaction time details   

When requesting data from BFD, you must ensure that the `_since` time in the request is after the current BFD transaction time.

The BFD transaction time comes from the Meta object in the bundled response (Bundle.Meta.LastUpdated).

According to FHIR, BFD must return a bundle (which may be empty but still contain the required metadata) even if the patient ID doesn't match.

Therefore, using a fake patient ID which is guaranteed not to match is an easy way to get back a lean response with the valid BFD transaction time:

```json
{
  "resourceType": "Bundle",
  "id": "dc6f27bd-0448-43aa-a067-c120f85199a6",
  "meta": {
    "lastUpdated": "2021-06-08T01:55:08.681+00:00"
  },
  "type": "searchset",
  "total": 0,
  "link": [
    {
      "relation": "self",
      "url": "https://exampleURL/Patient/?_id=blah&_lastUpdated=le2021-06-22T09%3A31%3A28.837731-05%3A00"
    }
  ]
}
```

## Troubleshooting  
###### [`^`](#table-of-contents)

Please see the [troublshooting document ](Troubleshooting.md) for more help.
