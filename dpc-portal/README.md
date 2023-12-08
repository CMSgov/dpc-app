# README

This is the Data Point of Care (DPC) website. It will allow users to sign up for the DPC service, get credentialed and configure their account.

# Running via Docker

The DPC website can be run locally via docker. Follow the below steps to build and run the website into a Docker container.

-   Run `make start-portals` to start dpc-portal in a docker container.
    -   This will also start dpc-web and dpc-admin, which are only used in the sandbox environment.
-   Run `make down-portals` to stop dpc-portal.
-   To run tests against dpc-portal, run `make ci-portal`.

### A note for local development

Locally, you'll need to be running Zscaler in order to access the CPI API Gateway endpoints. If you need run that functionality on your machine, you'll want to add the following directly before line 5 of the Dockerfile:

`COPY Zscaler-Root-CA.pem /usr/local/share/ca-certificates/ZScaler-Root-CA.pem`

## View Components

We utilize the [ViewComponent](https://viewcomponent.org/) library to create custom components. These components are documented and viewable within [Lookbook](http://localhost:3100/portal/lookbook).
