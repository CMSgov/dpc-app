# Manual API Testing

The recommended method for testing the services is with the [Postman](https://www.getpostman.com) application.
This allows easy visualization of responses, as well as simplifies adding the necessary HTTP headers.

## Postman Setup

Note: Prior to running the tests, ensure that you've updated these Postman Environment variables:

-   organization-id
-   client-token
-   public-key
-   private-key

Once the development environment is up and running, you should now be able to run some calls to the API via the DPC Postman Collections. Below, are some useful endpoints for verifying a functional development environment:

-   Register single patient
-   Register practitioner
-   Get all groups
-   Add patient to group
-   Create export data request

## Specific Instructions

Steps for testing the data export:

1.  Start the services using either the `docker-compose` command or through manually running the JARs. If running the JARs manually, you will need to migrate and seed the database before continuing.
1.  Make an initial GET request to the following endpoint: `http://localhost:3002/v1/Group/3461C774-B48F-11E8-96F8-529269fb1459/$export`.
    This will request a data export for all the patients attribution to provider: `3461C774-B48F-11E8-96F8-529269fb1459`.
    You will need to set the Accept header to `application/fhir+json` (per the FHIR bulk spec).
1.  The response from the Export endpoint should be a **204** with the `Content-Location` header containing a URL which the user can use to to check the status of the job.
1.  Make a GET request using the URL provided by the `/Group` endpoint from the previous step.
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
    ```

1.  Download the exported files by calling the `/Data` endpoint with URLs provided (e.g., `http://localhost:3002/v1/Data/de00da66-86cf-4be1-a2a8-0415b21a6a9b.ndjson`).
1.  Enjoy your glorious ND-JSON-formatted FHIR data.
