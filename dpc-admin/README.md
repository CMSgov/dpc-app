# README

## Required installations
- Ruby (v. 2.6.3)
- Rails (v. 6.0.3.5)
- PostgreSQL

## Install and Configuration of Admin Application
Clone the project using SSH or HTTPS.

```SSH
git clone git@github.com:CMSgov/dpc-app.git
```

For HTTPS:

```HTTP
https://github.com/CMSgov/dpc-app.git
```

**Note:** This clones the whole DPC App including the Java API and the Ruby on Rails Web App.

## Admin App Set Up

1. Change into the directory `dpc-app/dpc-admin`
2. `gem install bundler --no-document`
3. Run `bundle install`
4. Run `npm install`

### Add a .env file
Create a `.env` file in the root director.

Note: If you need to change the database configuration, it can be overridden using the `DB_USER`, `DB_PASS`, and `DATABASE_URL` environment variables. Example:

```
DB_USER=postgres
DB_PASS=password
DATABASE_URL=postgresql://localhost/dpc-website_development
```

You also need to set the Github ENV variables to enable Github OAuth login for internal users:

```
GITHUB_APP_ID=xxx
GITHUB_APP_SECRET=yyy
GITHUB_ORG_TEAM_ID=123
```

If you want to switch to the sandbox environment, add the `ENV` variable:

```
ENV=prod-sbx
```

### The Database
The database is shared with the Ruby on Rails Web app (found in `dpc-app/dpc-web`). If you already set up the database for the web app, you will only need to run `rake db:migrate`. If you have not set up the database run: `rails db:create db:migrate db:seed`.

## Running Admin App
Once the database and credentials are set up, run `rails s` or `rails server`. The app should be live at `http://localhost:3000/`.

## Running Admin App with Web App
To run the Admin App and Web App simultaneously, follow the above instructions to spin up the Admin app. Then in another terminal window, redirect to `dpc-app/dpc-web` and follow the instructions for that application.

The Web app will automatically run on `http://localhost:3500/`.

# Testing
To test the rails app, run `rspec spec` in the terminal.

# Running as docker container
Navigate to /dpc-app

DPC Admin: localhost:3000\
Admin Sidekiq Console: localhost:3000/sidekiq\
Admin Letter Opener: localhost:3000/letter_opener

DPC Web: localhost:9000\
Admin Sidekiq Console: localhost:3900/sidekiq\
Admin Letter Opener: localhost:3900/letter_opener

Port mappings can be changed in:\
`docker-compose.portals.yml`

Helpful make commands:\
`make start-portals` : Starts Web,Admin,Impl,Redis,Sidekiqs,and db\
`make stop-portals` : Stops Web,Admin,Impl,Redis,Sidekiqs,and db\
`make down-portals` : Removes Web,Admin,Impl,Redis,Sidekiqs,db and docker network.
