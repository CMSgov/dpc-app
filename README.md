# User Guide

This document serves as a guide for running the Data at the Point of Care (DPC) API on your local environment.

[![Build Status](https://travis-ci.org/CMSgov/dpc-app.svg?branch=master)](https://travis-ci.org/CMSgov/dpc-app)
[![Maintainability](https://api.codeclimate.com/v1/badges/46309e9b1877a7b18324/maintainability)](https://codeclimate.com/github/CMSgov/dpc-app/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/46309e9b1877a7b18324/test_coverage)](https://codeclimate.com/github/CMSgov/dpc-app/test_coverage)

<!-- TOC -->

## Table of Contents

-   [What Is DPC?](#what-is-dpc)
-   [Components](#components)
    -   [Main Services](#main-services)
    -   [Shared Modules](#shared-modules)
-   [Local Development Setup](#tech-environment)
    -   [Required Dependencies](#required-dependencies)
    -   [Recommended tools](#recommended-tools)
    -   [Installing and Using Pre-Commit](#installing-and-using-pre-commit)
    -   [Quickstart](#quickstart)
-   [Managing Encrypted Files](#managing-encrypted-files)
    -   [Re-encrypting files](#re-encrypting-files)
-   [Starting the Database](#starting-the-database)
-   [Building the DPC API](#building-dpc)
    -   [How the API Works](#how-the-api-works)
    -   [Option 1: Full integration test](#option-1-full-integration-test)
    -   [Option 2: Manually](#option-2-manually)
-   [Running the DPC API](#running-dpc)
    -   [Running the DPC API via Docker](#running-dpc-via-docker)
    -   [Generating a golden macaroon](#generating-a-golden-macaroon)
    -   [Manual JAR execution](#manual-jar-execution)
-   [Seeding the Database](#seeding-the-database)
-   [Testing the Application](#testing-the-application)
    -   [Demo client](#demo-client)
    -   [Manual testing](#manual-testing)
    -   [Smoke tests](#smoke-tests)
-   [Generating the Source Code Documentation via JavaDoc](#generating-the-source-code-documentation-via-javadoc)
-   [Building the Additional Services](#building-the-additional-services)
    -   [Postman collection](#postman-collection)
-   [Code Coverage](#code-coverage)
-   [Local Debugging](#local-debugging)
-   [Debugging Integration Tests](#debugging-integration-tests)
-   [Other Notes](#other-notes)
    -   [BFD transaction time details](#bfd-transaction-time-details)
-   [Troubleshooting](#troubleshooting)
<!-- TOC -->

## What Is DPC?

DPC is a pilot application programming interface (API) whose goal is to enable healthcare
providers to deliver high quality care directly to Medicare beneficiaries. See
[DPC One-Pager](https://dpc.cms.gov/assets/downloads/dpc-one-pager.pdf) and the [DPC Website](https://dpc.cms.gov/) to learn more about the API.

## Components

#### Main Services

The DPC application is split into multiple services.

| Service                             | Type                    | Description                                                                                | Stack                      |
| ----------------------------------- | ----------------------- | ------------------------------------------------------------------------------------------ | -------------------------- |
| [dpc-web](/dpc-web)                 | Public Portal           | Portal for managing organizations (Sandbox only, and soon to be deprecated)                | Ruby on Rails              |
| [dpc-admin](/dpc-admin)             | Internal Portal         | Administrative Portal for managing organizations (Sandbox only, and soon to be deprecated) | Ruby on Rails              |
| [dpc-portal](/dpc-portal)           | Public Portal           | Portal for managing organizations                                                          | Ruby on Rails              |
| [dpc-api](/dpc-api)                 | Public API              | Asynchronous FHIR API for managing organizations and requesting or retrieving data         | Java (Dropwizard)          |
| [dpc-attribution](/dpc-attribution) | Internal API            | Provides and updates data about attribution                                                | Java (Dropwizard)          |
| [dpc-consent](/dpc-consent)         | Internal API            | Provides and updates information about data-sharing consent for individuals                | Java (Dropwizard)          |
| [dpc-queue](/dpc-queue)             | Internal API            | Provides and updates data about export jobs and batches                                    | Java (Dropwizard)          |
| [dpc-aggregation](/dpc-aggregation) | Internal Worker Service | Polls for job batches and exports data for singular batches                                | Java (Dropwizard + RxJava) |

#### Shared Modules

In addition to services, several modules are shared across components.

| Module Name                       | Description                                    | Stack |
| --------------------------------- | ---------------------------------------------- | ----- |
| [dpc-bluebutton](/dpc-bluebutton) | Bluebutton API Client                          | Java  |
| [dpc-macaroons](/dpc-macaroons)   | Implementation of macaroons for authentication | Java  |
| [dpc-common](/dpc-common)         | Shared utilities for components                | Java  |
| [dpc-testing](/dpc-testing)       | Shared utilities for testing                   | Java  |
| [dpc-smoketest](/dpc-smoketest)   | Smoke test suite                               | Java  |
| [engines](/engines)               | Shared engines                                 | Ruby  |

## Local Development Setup

###### [`^`](#table-of-contents)

### Required Dependencies

When running the applications locally, you'll want to run everything through Docker. This simplifies the process of spinning up multiple services, connecting them together, and upgrading tooling versions over time.

In that scenario, you only need the following dependencies:

-   Install [Ansible Vault](https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html#)
-   Install [Docker Desktop](https://docs.docker.com/install/) (make sure to allocate more than the default 2Gb of memory)
-   Install [Pre-commit](https://pre-commit.com/) with [Gitleaks](https://github.com/gitleaks/gitleaks)

If you want to build applications locally, you'll need the following tools:

-   Ruby and `bundler`
-   Java 11 and Maven (`mvn`)

> **Note:** DPC only supports Java 11 due to our use of new languages features, which prevents using older JDK versions.
>
> In addition, some of the upstream dependencies have not been updated to support Java 12 and newer, but we plan on adding support at a later date.

In addition, it's helpful to have the following installed for more specific scenarios:

-   Running [smoke tests](#smoke-tests): Python 3 (includes `pip`)
-   Running [postman tests](#postman-collection): Node.js (includes `npm`)

### Recommended tools

For development, we recommend the following tooling:

-   Code Editor: JetBrains [Intelli-J Idea IDE](https://jetbrains.com/idea) or [Visual Studio Code](https://code.visualstudio.com/)
-   Database browser: [PgAdmin](https://pgadmin.org) or [Postico](https://postico.com) _(MacOS)_
-   API browser and testing tool: [Postman](https://www.postman.com/downloads/)

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

## Quickstart

In order to encrypt and decrypt configuration variables, you must create a `.vault_password` file in the root directory. Contact another team member to gain access to the vault password.

### Run the API Java Services

```
make start-app
```

If you need to force-rebuild the container, you can run:

```
make  ........
```

### Run the Portals

#### Generating a golden macaroon

You will need to configure the portals with a golden macaroon for communication with the DPC API. Run the command below to generate one:
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

#### Running the Portal applications

```
make start-portals
```

to rebuild the applications:

```
make portal
make start-portals
```

### Running Tests:

```
make unit-tests # Java unit tests
make ci-app # Java unit and integration tests
make ci-portals-v1 # All portal tests
make ci-api-client # Tests for shared DPC API client in Ruby
```

## Building the Portal Services

###### [`^`](#table-of-contents)

-   Documentation on building the DPC Portal is covered in the specific [README](dpc-portal/README.md).
-   Documentation on building the DPC Website is covered in the specific [README](dpc-web/README.md).

## Troubleshooting

###### [`^`](#table-of-contents)

Please see the [troublshooting document ](docs/Troubleshooting.md) for more help.
