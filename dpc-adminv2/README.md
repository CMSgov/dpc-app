# DPC AdminV2 Portal README
This is the admin portal for the Data at the Point of Care (DPC) 2.0 [implementer portal](https://github.com/CMSgov/dpc-app/tree/master/dpc-impl). Through this portal, admin users can manage DPC users, implementer organizations, and provider organizations.

All static public pages are hosted on a separate static site. This many result in broken links when run locally.

## Before Installation
Follow instructions to set up the [implementer portal](https://github.com/CMSgov/dpc-app/tree/master/dpc-impl) for DPCv2 through creating and migrating the database. Running the implementer portal is not necessary to run the admin portal, though actions may be limited.

## Installation and Set Up
Once the database is built by installing and setting up the implementer portal, navigate to `dpc-app/dpc-adminv2`.

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

### Github OAuth Set Up
The only way to sign into the admin portal is via Github Omniauth through the `devise` gem.

In Github Account `Settings`, select `Developer settings`, then select `New OAuth App`.

Fill out the form with the correct information. For local use, the `Homepage URL` will be `http://localhost:4002/adminv2` and the `Authorization callback URL` will be `http://localhost:4002/adminv2/auth/github/callback`.

After registering the application, you will need the `Client ID` and `Client secret`. The client secret will not be accessible after the initial page. But you can create a new one if you miss it.

### Set up local environment
Similar to the implementer portal, we need to set up our local environments with two files: `.env.development.local` and `.env.test.local`.

The difference is that we will need to include the `DATABASE_URL` for the existing database created by the implementer portal.

For the admin portal sign in with Github, we will also need to include `GITHUB_ORG_TEAM_ID`, `GITHUB_APP_ID`, and `GITHUB_APP_SECRET`.

In `.env.development.local`:

```
DB_USER=postgres
DB_PASS=password
DATABASE_URL=postgresql://localhost/dpc-impl_development
GITHUB_ORG_TEAM_ID=
GITHUB_APP_ID=
GITHUB_APP_SECRET=
```

### Run the Server
To run DPCv2 Implementer portal run the following commands in order:
```Bash
rails server
```

If you are running the full DPCv2 including [implementer portal](https://github.com/CMSgov/dpc-app/tree/master/dpc-impl), [static site](https://github.com/CMSgov/dpc-static-site), and DPCv2 API(https://github.com/CMSgov/dpc-app/), fill in the related urls with the local addresses (i.e. `http://localhost:3500` for `STATIC_SITE_URL` and `http://localhost:3001` for `API_METADATA_URL`).

## Run Tests
In `.env.test.local`, you do not need the Github credentials.

```
DB_USER=postgres
DB_PASS=passowrd
DATABASE_URL=postgresql://localhost/dpc-impl_test
```

Once local variables are set, navigate to `dpc-app/dpc-impl` and run `rspec spec` to run all tests.

## Run in Docker (with all portals)
1. Navigate to `dpc-app`
2. Run command `make ci-portals` to run all tests (this may take up to 40 minutes)
3. Run command `make start-portals` to launch all portals (web app, admin, impl, and adminv2), redis server, sidekiq server, and database.

## Default Ports
**Shared:**
Redis Server: `localhost:6379/1`

Impl App: `localhost:4000`\
Impl Letter Opener: localhost:4000/letter_opener

AdminV2: `localhost:4002`

Port mappings can be changed in: `docker-compose.portals.yml`