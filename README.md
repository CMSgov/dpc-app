# DPC: *Data @ The Point of Care*
This document serves as a guide for running the DPC API on your local environment.


[![Build Status](https://travis-ci.org/CMSgov/dpc-app.svg?branch=master)](https://travis-ci.org/CMSgov/dpc-app)
[![Maintainability](https://api.codeclimate.com/v1/badges/46309e9b1877a7b18324/maintainability)](https://codeclimate.com/github/CMSgov/dpc-app/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/46309e9b1877a7b18324/test_coverage)](https://codeclimate.com/github/CMSgov/dpc-app/test_coverage)



<!-- TOC -->
## Table of Contents
* [DPC: *Data @ The Point of Care*](#dpc--data--the-point-of-care)
  * [Documentation](#additional-documentation)
  * [What is DPC?](#what-is-dpc)
  * [Required tools and languages](#required-tools-and-languages)
  * [Recommended tools](#recommended-tools)
  * [Troubleshooting](#troubleshooting-)
  * [Decrypting encrypted files](#decrypting-encrypted-files)
  * [Building DPC](#building-dpc)
    * [There are two ways to build DPC:](#there-are-two-ways-to-build-dpc-)
      * [Option 1: Full Integration Test](#option-1--full-integration-test)
      * [Option 2: Manually](#option-2--manually)
  * [Running DPC](#running-dpc)
  * [Running via Docker](#running-via-docker)
  * [Running DPC v2 via Docker](#running-dpc-v2-via-docker)
  * [Manual JAR execution](#manual-jar-execution)
  * [Seeding the database](#seeding-the-database)
  * [Testing the Application](#testing-the-application)
    * [Demo client](#demo-client)
    * [Manual testing](#manual-testing)
    * [Smoke tests](#smoke-tests)
  * [Generating the source code documentation via JavaDoc](#generating-the-source-code-documentation-via-javadoc)
  * [Building the Additional Services](#building-the-additional-services)
    * [Postman Collection](#postman-collection)
  * [Secrets management](#secrets-management)
    * [Sensitive Docker configuration files](#sensitive-docker-configuration-files-)
    * [Managing encrypted files](#managing-encrypted-files-)
    * [BFD Transaction Time details](#bfd-transaction-time-details-)
<!-- TOC -->

## Additional Documentation
[DPC One-Pager](https://dpc.cms.gov/assets/downloads/dpc-one-pager.pdf)

What Is DPC?
---
Data at the Point of Care **(DPC)** is a pilot application
programming interface **(API)** whose goal is to enable healthcare
providers to deliver high quality care directly to Medicare
beneficiaries. Visit our [website](https://dpc.cms.gov/) for background information.

## Tech Environment
###### [`^`](#table-of-contents)
### Required tools and languages

- Python 3 and `pip`
- Java 11 and `mvn`
- Go
- Ruby and `bundler`
- `npm`
- [Ansible Vault](https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html#)
- Docker (make sure to allocate more than the default 2Gb of memory)
- [AWS CLI](https://aws.amazon.com/cli/)


### Recommended tools
###### [`^`](#table-of-contents)
- [PgAdmin](https://pgadmin.org) or [Postico](https://postico.com) *(MacOS)*
- JetBrains [Intelli-J Idea IDE](https://jetbrains.com/idea)
- [Docker Desktop](https://docs.docker.com/desktop/mac/install/)
- [Postman](https://www.postman.com/downloads/)



Decrypting Encrypted Files
---
###### [`^`](#table-of-contents)
See [Secrets Management](#secrets-management) for details on how to encrypt and decrypt required secrets.

Before building the app or running any tests, the decrypted secrets must be available as environment variables.

In order to encrypt and decrypt configuration variables, you must create a `.vault_password` file in this repository root directory.
Contact another team member to gain access to the vault password.

Included in the cloned project you will find a couple of encrypted files located at  `dpc-app/ops/config/encrypted` that will need to be decrypted before proceeding.

Run `make secure-envs` to decrypt the encrypted files.
If decrypted successfully, you will see the decrypted data in new files under `/ops/config/decrypted` with the same names as the corresponding encrypted files.

This command also creates a git pre-commit hook in order to avoid accidentally committing a decrypted file.
Required services < "required services" looks like this in an incomplete sentence?> 
---

DPC requires an external Postgres database to be running. While a separate Postgres server can be used, the `docker-compose` file includes everything needed, and can be started like so: 

```bash
docker-compose up start_core_dependencies
```

> Warning: If you do have an existing Postgres database running on port 5342, docker-compose WILL NOT alert you to the port conflict. Ensure any local Postgres databases are stopped before starting docker-compose.

By default, the application attempts to connect to the `dpc_attribution`, `dpc_queue`, and `dpc_auth` databases on the localhost as the `postgres` user with a password of `dpc-safe`.
When using docker-compose, all the required databases will be created automatically. Upon container startup, the databases will be initialized automatically with all the correct data. If this behavior is not desired, set an environment variable of `DB_MIGRATION=0`.

The defaults can be overridden in the configuration files.
Common configuration options (such as database connection strings) are stored in a [server.conf](src/main/resources/server.conf) file and included in the various modules via the `include "server.conf"` attribute in module application config files.
See the `dpc-attribution` [application.conf](dpc-attribution/src/main/resources/application.conf) for an example.

Default settings can be overridden either directly in the module configurations, or via an `application.local.conf` file in the project root directory. 
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

> Note: On startup, the services look for a local override file (application.local.conf) in the root of their *current* working directory.
This can create an issue when running tests with IntelliJ. The default sets the working directory to be the module root, which means any local overrides are ignored.
This can be fixed by setting the working directory to the project root, but needs to be done manually.

Building DPC
---
###### [`^`](#table-of-contents)
> Note: Before building DPC, you must first ensure that [the required secrets are decrypted](#decrypting-encrypted-files). 

### There are two ways to build DPC:

> Note: DPC only supports Java 11 due to our use of new languages features, which prevents using older JDK versions.
In addition, some of upstream dependencies have not been updated to support Java 12 and newer, but we plan on adding support at a later date. 

#### Option 1: Full Integration Test

Run `make ci-app`. This will start the dependencies, build all components, run integration tests, and run a full end to end test. You will be left with compiled JARs for each component, as well as compiled Docker containers.

#### Option 2: Manually

Run `make docker-base` to build the common, baseline Docker image (i.e., `dpc-base:latest`) used across DPC services.

Next, in order to make the decrypted environment variables accessible to Maven, run `make maven-config`.
This command will convert the contents of `ops/config/decrypted/local.env` to Maven flags which can be viewed in `.mvn/maven.config` if successful.

Then, run `mvn clean install` to build and test the application. Dependencies will need to be up and running for this option to succeed.

Running `mvn clean install` will also construct the Docker images for the individual services. To skip the Docker build, pass `-Djib.skip=True`.

Note that the `dpc-base` image produced by `make docker-base` is not stored in a remote repository. The `mvn clean install` process relies on the base image being available via the local Docker daemon.

Running DPC
--- 

Once the JARs are built, they can be run in two ways, either via [`docker-compose`](https://docs.docker.com/compose/overview/) or by manually running the JARs.

## Running via Docker 
###### [`^`](#table-of-contents)

Click on [Install Docker](https://www.docker.com/products/docker-desktop) to setup Docker.
The application (along with all required dependencies) can be automatically started with the following command: `make start-app`. 
The individual services can be started (along with their dependencies) by passing the service name to the `up` command.

```bash
docker-compose up {db,aggregation,attribution,api}
``` 

By default, the Docker containers start with minimal authentication enabled, meaning that some functionality (such as extracting the organization_id from the access token) will not work as expected and always returns the same value.
This can be overridden during startup by setting the `AUTH_DISABLED=false` environment variable. 

When running locally, you'll need to update the docker-compse.yml file by adding
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
### Generating a Golden Macaroon
We will need a macaroon for the Docker configuration. Run the command below to generate one:
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

## Running DPC V2 via Docker
###### [`^`](#table-of-contents)
In order to start up all required services for V2 of DPC locally, use the command `make start-v2`.

To seed the database, use `make seed-db`. This will populate data in V1 version of the `dpc_attribution_db`.

Conversely, to shut down DPC V2 locally, use `make down-v2`. This coommand will shut down all running containers and remove the Docker network.

## Manual JAR Execution
###### [`^`](#table-of-contents)
Alternatively, the individual services can be manually executing the `server` command for the various services.

> Note: When manually running the individual services, you'll need to ensure that there are no listening port collisions.
By default, each service starts with the same application (8080) and admin (9900) ports. We provide a sample `application.local.conf` file which contains all the necessary configuration options.
This file can be copied and used directly: `cp application.local.conf.sample application.local.conf`.

> Note: The API service requires authentication before performing actions. This will cause most integration tests to fail, as they expect the endpoints to be open.
Authentication can be disabled in one of two ways: 
Set the `ENV` environment variable to `local` (which is the default when running under Docker).
Or, set `dpc.api.authenticationDisabled=true` in the config file (the default from the sample config file).   

Next, start each service in a new terminal window, from within the the `dpc-app` root directory. 

```bash
java -jar dpc-attribution/target/dpc-attribution.jar server
java -jar dpc-aggregation/target/dpc-aggregation.jar server
java -jar dpc-api/target/dpc-api.jar server
```

By default, the services will attempt to load the `local.application.conf` file from the current execution directory. 
This can be overridden in two ways:
1. Passing `ENV={dev,test,prod}` will load a `{dev,test,prod}.application.conf` file from the service resources directory.
1. Manually specifying a configuration file after the server command `server src/main/resources/application.conf` will directly load that configuration set.

> Note: Manually specifying a config file will disable the normal configuration merging process. 
This means that only the config variables directly specified in the file will be loaded, no other `application.conf` or `reference.conf` files will be processed. 

1. You can check that the application is running by requesting the FHIR `CapabilitiesStatement` for the `dpc-api` service, which will return a JSON-formatted FHIR resource.
    ```bash
    curl -H "Accept: application/fhir+json" http://localhost:3002/v1/metadata
    ```

Seeding the Database
---
###### [`^`](#table-of-contents)
> Note: This step is not required when directly running the `demo` for the `dpc-api` service, which partially seeds the database on first execution.

By default, DPC initially starts with an empty attribution database, which means that no patients have been attributed to any providers and thus nothing can be exported from BlueButton2.0.

In order to successfully test and demonstrate the application, there needs to be initial data loaded into the attribution database.
We provide a small CSV [file](src/main/resources/test_associations.csv) which associates some fake providers with valid patients from the BlueButton2.0 Sandbox.

The database can be automatically migrated and seeded by running `make seed-db` or by using the following commands:

> **Note:** For instances where one cannot set up the DPC due to authorization issues, follow the steps in the [manual table setup document](DbTables.md) to populate the necessary tables manually. 

```bash
java -jar dpc-attribution/target/dpc-attribution.jar db migrate
java -jar dpc-attribution/target/dpc-attribution.jar seed
``` 

Testing the Application
---

### Demo client
###### [`^`](#table-of-contents)
The `dpc-api` component contains a `demo` command, which illustrates the basic workflow for submitting an export request and modifying an attribution roster.
It can be executed with the following command:

`java -jar dpc-api/target/dpc-api.jar demo`

> Note: The demo client expects the entire system (all databases and services) to be running from a new state (no data in the database).
This is the default when starting the services from the *docker-compose* file.
When running the JARs manually, the user will need to ensure that the `dpc_attribution` database is truncated after each run. 

The demo performs the following actions:

1. Makes an export request for a given provider.
This request fails because the provider is not registered with the application and has no attributed patients
1. Generates an attribution roster for the provider using the [test_association.csv](src/main/resources/test_associations.csv) file.
1. Resubmits the original export request.
1. Polls the *Job* endpoint using the URL returned from the export request and waits for a completed status.
1. Outputs the download URLs for all files generated by the export request.

### Manual testing
###### [`^`](#table-of-contents)

The recommended method for testing the Services is with the [Postman](https://www.getpostman.com) application.
This allows easy visualization of responses, as well as simplifies adding the necessary HTTP Headers.

Steps for testing the data export:

1. Start the services using either the `docker-compose` command or through manually running the JARs.

    If running the JARs manually, you will need to migrate and seed the database before continuing. 
1. Make an initial *GET* request to the following endpoint: `http://localhost:3002/v1/Group/3461C774-B48F-11E8-96F8-529269fb1459/$export`.
This will request a data export for all the patients attribution to provider: `3461C774-B48F-11E8-96F8-529269fb1459`.
You will need to set the *ACCEPT* header to `application/fhir+json` (per the FHIR bulk spec).
1. The response from the export endpoint should be a *204* with the `Content-Location` header containing a URL which the user can use to to check the status of the job.
1. Make a *GET* request using the URL provided by the `/Group` endpoint from the previous step.
 Which has this format: `http://localhost:3002/v1/Jobs/{unique UUID of export job}`.
 You will need to ensure that the *ACCEPT* header is set to `application/fhir+json` (per the FHIR bulk spec).
 You will need to ensure that the *PREFER* header is set to `respond-async`.
 The server should return a *204* response until the job has completed.
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
1. Download the exported files by calling the `/Data` endpoint with URLs provided. e.g. `http://localhost:3002/v1/Data/de00da66-86cf-4be1-a2a8-0415b21a6a9b.ndjson`.
1. Enjoy your glorious ND-JSON formatted FHIR data.


### Smoke tests
###### [`^`](#table-of-contents)

Smoke tests are provided by [Taurus](https://github.com/Blazemeter/taurus) and [JMeter](https://jmeter.apache.org).
The tests can be run by the environment specific Makefile commands. e.g. `make smoke/local` will run the smoke tests against the locally running Docker instances.

In order to run the tests, you'll need to ensure that `virtualenv` is installed.

```bash
pip3 install virtualenv
```
Generating the source code documentation via JavaDoc
---
The entire project's code base documentation can be generated in HTML format by using the Java's
JavaDoc tool. The [Intelli-J Idea](https://jetbrains.com/idea) IDE makes this easy to do. Navigate to the **Tools>Generate JavaDoc** menu item, specify the scope of the documentation and the output location and you'll be able to view an interactive document outlining the code members.


Building the Additional Services
---
###### [`^`](#table-of-contents)
Documentation on building the DPC Website is covered in the specific [README](dpc-web/README.md).


### Postman Collection
###### [`^`](#table-of-contents)
> Note: Prior to running the tests, ensure that you've updated these Postman Environment variables: 
>- organization-id
>- client-token
>- public-key
>- private-key

Once the development environment is up and running, you should now be able to run some calls to the API via the DPC Postman Collections. Below, are some useful endpoints for verifying a functional development environment:
- Register Single Patient
- Register Practitioner
- Get all groups
- Add Patient to Group
- Create Export Data Request


In order to encrypt and decrypt configuration variables, you must create a `.vault_password` file in this repository root directory and in the `/dpc-go/dpc-attribution` directory. Contact another team member to gain access to the vault password.

**IMPORTANT:** Files containing sensitive information are enumerated in the `.secrets` file in this directory. If you want to protect the contents of a file using the `ops/scripts/secrets` helper script, it must match a pattern listed in `.secrets`.

To avoid committing and pushing unencrypted secret files, use the included `ops/scripts/pre-commit` Git pre-commit hook from this directory:


Secrets management
---
###### [`^`](#table-of-contents)
> Note: You can use `make secure-envs` to decrypt files and create the pre-commit hook at the same time.

### Sensitive Docker configuration files  [`^`](#table-of-contents)

The files committed in the `ops/config/encrypted` directory hold secret information, and are encrypted with [Ansible Vault](https://docs.ansible.com/ansible/2.4/vault.html).

In order to encrypt and decrypt configuration variables, you must create a `.vault_password` file in this repository root directory and in the `/dpc-go/dpc-attribution` directory. Contact another team member to gain access to the vault password.

**IMPORTANT:** Files containing sensitive information are enumerated in the `.secrets` file in this directory. If you want to protect the contents of a file using the `ops/scripts/secrets` helper script, it must match a pattern listed in `.secrets`.

To avoid committing and pushing unencrypted secret files, use the included `ops/scripts/pre-commit` Git pre-commit hook from this directory:

```
cp ops/scripts/pre-commit .git/hooks
```

### Managing encrypted files   [`^`](#table-of-contents)
* Temporarily decrypt files by running the following command from this directory:
```
./ops/scripts/secrets --decrypt
```

* While files are decrypted, copy the files from `ops/config/encrypted` to the sibling directory `ops/config/decrypted`.

* Encrypt changed files with:
```
./ops/scripts/secrets --encrypt <filename>
```

### BFD Transaction Time details   [`^`](#table-of-contents)
When requesting data from BFD, you must ensure that the `_since` time in the request is after the current BFD transaction time.

The BFD transaction time comes from the Meta object in the bundled response (Bundle.Meta.LastUpdated).

According to fhir, BFD must return a bundle (which may be empty but still contain the required metadata) even if the patient ID doesn't match.

Therefore, using a fake patient ID which is guaranteed not to match is an easy way to get back a lean response with the valid BFD Transaction Time:

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

## Troubleshooting  [`^`](#table-of-contents)

Please see the [troublshooting document ](Troubleshooting.md) for more help
