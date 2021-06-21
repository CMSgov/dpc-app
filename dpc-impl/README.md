# DPC Implementer Portal README
This is version 2 of the Data at the Point of Care (DPCv2) web app, specifically designed for implementers in the provider-implementer relationship. DPCv2 allows implementer users to invite users to their group, manage multiple provider organizations and their associated public keys and client tokens. There is an associated [admin portal](https://github.com/CMSgov/dpc-app/tree/master/dpc-adminv2) to manage users, implementers, and provider organizations and a [static site](https://github.com/CMSgov/dpc-static-site) for the public facing static pages.

## Before Installation
DPCv2 is Ruby on Rails driven application with a PostgreSQL database. In order to run either this application or the admin portal, the following will need to be installed locally:

- Ruby (v. 2.7.2)
- Rails (v. 6.1.3.2)
- PostgreSQL (v. 11)

## Install DPCv2

### Run DPCv2 Locally
Once you have installed the above requirements, clone the root directory of this repo to your local computer:
```SSH
git clone git@github.com:CMSgov/dpc-app.git
```

For HTTPS:
```HTTP
https://github.com/CMSgov/dpc-app.git
```

Navigate to `dpc-app/dpc-impl` directory.

Install bundler:
```Bash
gem install bundler --no-doucment
```

Install gems:
```
bundle install
```

Install NPM:
```
npm install
```

If no errors were raised, everything required to run the application locally should be installed.

### Set Up Local Environment
In order to run locally, create two files: `.env.development.local` and `.env.test.local`.

For `.env.development.local` provide the following variables for full functionality:
```
DB_USER=postgres
DB_PASS=password
API_METADATA_URL=
STATIC_SITE_URL=
```

If you are running the full DPCv2 including [admin portal](https://github.com/CMSgov/dpc-app/tree/master/dpc-adminv2), [static site](https://github.com/CMSgov/dpc-static-site), and DPCv2 API(https://github.com/CMSgov/dpc-app/), fill in the related urls with the local addresses (i.e. `http://localhost:3500` for `STATIC_SITE_URL` and `http://localhost:3001` for `API_METADATA_URL`).

### Run the Server
To run DPCv2 Implementer portal run the following commands in order:
```Bash
rails db:create db:migrate db:seed
rails server
```

## Run the Tests
To run tests locally, add the following to `.env.test.local`:
```
DB_USER=postgres
DB_PASS=password
API_METADATA_URL=http://dpc.example.com
```

**Do not change the `API_METADATA_URL` from `http://dpc.example.com`.** It will break tests for API calls.

Once local variables are set, navigate to `dpc-app/dpc-impl` and run `rspec spec` to run all tests.

## Run in Docker (with all portals)
1. Navigate to `dpc-app`
2. Run command `make ci-portals` to run all tests (this may take up to 40 minutes)
3. Run command `make start-portals` to launch all portals (web app, admin, impl, and adminv2), redis server, sidekiq server, and database.

### Other helpful make commands
`make stop-portals` : Stops web app, admin, impl, admin v2 portals; redis and sidekiq servers; database\
`make down-portals` : Removes web app, admin, impl, admin v2 portals; redis and sidekiq servers; database; docker network

### Default Ports
**Shared:**
Redis Server: `localhost:6379/1`

Impl App: `localhost:4000`\
Impl Letter Opener: localhost:4000/letter_opener

AdminV2: `localhost:4002`

Port mappings can be changed in: `docker-compose.portals.yml`