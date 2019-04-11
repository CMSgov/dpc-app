Data @ The Point of Care
-

[![Build Status](https://travis-ci.org/CMSgov/dpc-app.svg?branch=master)](https://travis-ci.org/CMSgov/dpc-app)
[![Maintainability](https://api.codeclimate.com/v1/badges/46309e9b1877a7b18324/maintainability)](https://codeclimate.com/github/CMSgov/dpc-app/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/46309e9b1877a7b18324/test_coverage)](https://codeclimate.com/github/CMSgov/dpc-app/test_coverage)

Building DPC
---

1. Run `mvn clean install` to build your application.
This will also construct the *Docker* images for the API and Aggregation services.
To skip the Docker build pass `-Djib.skip=True`

Required services
---

DPC requires two external services to be running. *Postgres* and *Redis*

The `docker-compose` file includes the necessary applications and configurations, and can be started like so: 

```bash
docker-compose up db redis
```

By default, the application attempts to connect to the `dpc_atrribution` database on the localhost as the `postgres` user.
This database needs to be manually created, but table setup and data migration will be handled by the DPC services.

For Redis, we assume the server is running on the localhost, with the default port. 

The defaults can be overridden in the configuration files.
For example, modifying the `dpc-attribution` configuration:

```yaml
dpc.attribution {
  database = {
    driverClass = org.postgresql.Driver
    url = "jdbc:postgresql://localhost:5432/dpc-dev"
    user = postgres
  }
  queue {
      singleServerConfig {
        address = "redis://localhost:6379"
      }
    }
}
```

Running DPC
--- 

Once the JARs are built, they can be run in two ways.

1. Executed using *Docker Compose* `docker-compose up`
1. Manually by running each of the JARs
    1. `java -jar target/dpc-attribution-0.1.0.jar server`
    1. `java -jar target/dpc-web-0.1.0.jar server`
    
    By default, the services will attempt to load a `local.application.conf` file from the current execution directory. 
    This can be overriden in two ways.
    1. Passing `ENV={dev,test,prod}` will load a `{dev,test,prod}.application.conf` file from the service resources directory.
    1. Manually specifying a configuration file after the server command `server src/main/resources/application.conf` will directly load that configuration set.
    
    ***Note**: Manually specifying a config file will disable the normal configuration merging process. 
    This means that only the config variables directly specified in the file will be loaded, no other `application.conf` or `reference.conf` files will be processed.* 

1. To check that your application is running enter url `http://localhost:8080/v1/Group/1`, it should return a *404* error.

Seeding the database
---

In order to successfully test the application, there needs to be initial data loaded into the attribution database.
We provide a small CSV [file](src/main/resources/test_associations.csv) which associates some fake providers with valid patients from the BlueButton sandbox.
The database can be automatically migrated and seeded by running the following commands, before starting the Attribution service. 

```bash
java -jar target/dpc-attribution-0.1.0.jar db migrate
java -jar target/dpc-attribution-0.1.0.jar seed

``` 

Testing the Application
---

The recommended method for testing the Services is with the [Postman](https://www.getpostman.com) application.
This allows easy visualization of responses, as well as simplifies adding the necessary HTTP Headers. 

***Note:** the CURL command strips off the trailing `$export` from any request URIs, which makes it impossible to correctly test the initial data export request.*

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
 The server should return a *204* response until the job has completed.
 Once the job is complete, the endpoint should return data in the following format (the actual values will be different):
 
     ```javascript
    {
        "transactionTime": 1550868647.776162,
        "request": "http://localhost:3002/v1/Job/de00da66-86cf-4be1-a2a8-0415b21a6a9b",
        "requiresAccessToken": false,
        "output": [
            "http://localhost:3002/v1/Data/de00da66-86cf-4be1-a2a8-0415b21a6a9b"
        ],
        "error": []
    }
    ```
    The output array contains a list of URLs where the exported files can be downloaded from.
1. Download the exported files by calling the `/Data` endpoint with URLs provided. e.g. `http://localhost:3002/v1/Data/de00da66-86cf-4be1-a2a8-0415b21a6a9b`.
1. Enjoy your glorious ND-JSON formatted FHIR data.

Health Check
---

To see your applications health enter url `http://localhost:9900/healthcheck`
