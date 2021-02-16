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

# Testing
To test the rails app, run `rspec spec` in the terminal.