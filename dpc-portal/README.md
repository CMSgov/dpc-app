# DPC Portal

This is the Data Point of Care (DPC) Portal website. It allows users to manage access and credentials for their provider organizations.

# Local Development

## Running via Docker

The DPC website can be run locally via docker. Follow the below steps to build and run the website into a Docker container.

-   The DPC API is required for much of the Portal's functionality. To run the DPC API, run `make start-api`.
-   Run `make portal` to build the docker image, and `make start-portal` to start dpc-portal in a docker container.
-   Run `make down-portals` to stop dpc-portal.

The following commands can be useful for manual interaction:

-   Run `make portal-sh` to open an interactive shell within the dpc-portal container.
-   Run `make portal-console` to open an interactive Rails console within the dpc-portal container.
-   Run `make psql` to open a psql shell within the database container.

## Running Tests

-   To run all tests against dpc-portal, run `make ci-portal`.
-   To run individual tests, open a portal shell and run rspec directly.

    ```sh
    $ make portal-sh
    > rails db:create db:migrate RAILS_ENV=test # Create the test database
    > bundle exec rspec path/to/test # Run individual test files
    ```

## Manual Workflows

To support manual workflows, the system offers rake commands that perform administrative commands for new accounts. These rake commands can be run within a portal shell (`make portal-sh`).

-   `rails dpc:invite_ao` - Creates a new AO invitation. The name does not currently matter, but the email should match your Login.gov sandbox email.

    ```sh
    $ rails dpc:invite_ao INVITE=Bob,Hoskins,bob@example.com,7838426501

    Invitation created for Bob Hoskins for Organization 7838426501
    http://localhost:3100/portal/organizations/2/invitations/4/accept
    ```

## Local AO Verification Scenarios

One of the following fake SSNs are required on your Login.gov sandbox account in order to successfully pass AO-Organization verification:

-   900111111
-   900666666
-   900777777
-   900888888
-   666222222

If you need to use another SSN, you can add it to the ao_ssns variable in the [fake enrollment roles endpoint](/dpc-portal/spec/support/fake_cpi_gateway.rb).

Most generated NPIs will return a successful AO/organization verification when run against the [fake CPI API Gateway](/dpc-portal/spec/support/fake_cpi_gateway.rb). More specific scenarios are supported below:

| Fake Organization NPI | Scenario                                       |
| --------------------- | ---------------------------------------------- |
| 900666666             | SSN has MED sanctions                          |
| 900777777             | SSN has waived MED sanctions                   |
| 3598564557            | Organization has MED Sanctions                 |
| 3098168743            | Organization has waived MED Sanctions          |
| 3299073577            | Providers and Enrollments endpoints return 404 |
| 3782297014            | No approved enrollments                        |

## Development Dashboards

### Lookbook / View Components

We utilize the [ViewComponent](https://viewcomponent.org/) library to create custom components. These components are documented and viewable within [Lookbook](http://localhost:3100/portal/lookbook).

### Email Mailers

Emails are not viewable in lookbook, but can be found [here](http://localhost:3100/portal/rails/mailers/).

### Sidekiq (Background Jobs)

You can check the status of any jobs by going to the [Sidekiq dashboard](http://localhost:3100/portal/sidekiq).

## Accessing the VAL CPI API Gateway from Local Dev

By default, the local portal is connected to a mock CPI API Gateway server with preset responses. To connect with the VAL CPI API Gateway environment, you'll need to:

-   Configure the Portal to connect to CPI Val
-   Run Zscaler.

### Configuring the Portal

Update the following environment variables in docker-compose.portals.yml. See [Confluence](https://confluence.cms.gov/pages/viewpage.action?spaceKey=DAPC&title=Authorized+Official+Verification) for the required values.

```
- CPI_API_GW_BASE_URL=<URL>
- CMS_IDM_OAUTH_URL=<URL>
```

### Running ZScaler

Spin up Zscaler on your machine. Once it's running, the local portal container will need the CA certificate loaded. You'll want to add the following directly before line 5 of the Dockerfile and rerun the build steps:

`COPY Zscaler-Root-CA.pem /usr/local/share/ca-certificates/ZScaler-Root-CA.pem`

## The Assets Pipeline

Read more about the assets pipeline [here](/docs/portal/assets-pipeline.md).
