# README

This is the Data Point of Care (DPC) website. It will allow users to sign up for the DPC service, get credentialed and configure their account.

# Running via Docker

The DPC website can be run locally via docker. Follow the below steps to build and run the website into a Docker container.

-   Run `make start-portals` to start dpc-portal in a docker container.
    -   This will also start dpc-web and dpc-admin, which are only used in the sandbox environment.
-   Run `make down-portals` to stop dpc-portal.
-   To run tests against dpc-portal, run `make ci-portal`.

## View Components

We utilize the [ViewComponent](https://viewcomponent.org/) library to create custom components. These components are documented and viewable within [Lookbook](http://localhost:3100/portal/lookbook).
