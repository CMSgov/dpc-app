# README

This is the Data Point of Care (DPC) website. It will allow users to sign up for the DPC service, get credentialed and configure their account.

# Running via Docker

The DPC website can be run locally via docker. Follow the below steps to build and run the website into a Docker container.

-   Run `make portal` to build the docker image, and `make start-portal` to start dpc-portal in a docker container.
-   Run `make portal-console` to open an interactive Rails console within the dpc-portal container.
-   Run `make psql` to open a psql shell within the database container.
-   Run `make down-portals` to stop dpc-portal.
-   To run tests against dpc-portal, run `make ci-portal`.

### A note for local development

Locally, you'll need to be running Zscaler in order to access the CPI API Gateway endpoints. If you need run that functionality on your machine, you'll want to add the following directly before line 5 of the Dockerfile:

`COPY Zscaler-Root-CA.pem /usr/local/share/ca-certificates/ZScaler-Root-CA.pem`

## View Components

We utilize the [ViewComponent](https://viewcomponent.org/) library to create custom components. These components are documented and viewable within [Lookbook](http://localhost:3100/portal/lookbook).

Emails are not viewable in lookbook, but rather [here](http://localhost:3100/portal/rails/mailers/).

## Assets Pipeline

Read more about the assets pipeline [here](/docs/portal/assets-pipeline.md).
