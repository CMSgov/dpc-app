# README
This is the *draft* version of the Data Point of Care (DPC) website. The website has functioning public static content as well as a sign-up section allowing providers to volunteer for beta-testing the DPC application. The sign-up section, and whatever functionality and specialized contentthat we provide signed up beta participants will require authentication to view. In addition, there is an administrator dashboard to manage the registration and communications with the participants.

## Installation of Ruby
This is a Ruby on Rails driven website, so you'll need Ruby and a few "gems" to get up and running. Installing Ruby and Rails, as well as PostGresql - the database on a Windows environment is difficult, and beyond the scope of this document - sorry, but you'll need a third party package to install Ruby, such as [Ruby Installer](https://rubyinstaller.org/). **Disclaimer**: the previous sentence was not an endorsement.

The situation for OSX is much, much simpler using [Homebrew](https://brew.sh). You can install ruby directly from Ruby's site (https://www.ruby-lang.org/en/). Using homebrew, you can also install ruby directly.

### Optional Installation of Homebrew to install Ruby or Rbenv
We can use homebrew to install ruby directly, or to install both a ruby version manager (rbenv) and a gemset manager(rbenv gemset). To install homebrew, either navigate to the homebrew link and follow the directions, or in the terminal window issue the following

```bash
/usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
```

Once homebrew is installed, you can either install the latest version of ruby directly (`brew install ruby`) or you can optionally install **rbenv** in the terminal window:

```bash
brew install rbenv
```

> Follow the message brew install issues once rbenv is installed. This is important - you'll need to edit the ~/.bash_profile.

You can now install the latest version of Ruby on your machine, or the version this app uses: 2.6.2 as follows:

```bash
rbenv install 2.6.2
rbenv rehash
```

Following the installation of rbenv, install **rbenv-gemset**. This is an optional step, but its good not to overcomplicate your gemsets.

```bash
brew install rbenv-gemset
```

## Installing Postgresql
Lastly, install postgresql, if necessary, using homebrew:

```bash
brew install postgresql
```

Follow the prompts following the installation to start running postgres as a service.

## Installation and Configuration of the Application
Assuming you have access to the github repository housing this application (since you are reading this), clone the project using SSH or HTTPS. Change into the directory where you want the website to reside and using the command line. For SSH:

```SSH
git clone git@github.com:CMSgov/dpc-app.git
```

For HTTPS:

```HTTP
https://github.com/CMSgov/dpc-app.git
```

You'll need to

1. Change into the installation directory `dpc-app/dpc-web`
2. If you are using rbenv-gemset issue the following

```Bash
rbenv gemset create {latest-ruby-version: eg 2.6.2} dpc-website
```

3. `gem install bundler --no-document`
4. Run `bundle install`
5. Run `npm install`

### Credentials

The database is password encrypted. Credentials are passed via environment variables. See the "Running the Server" section for information on which environment variables to set.

### Running the server
```Bash
rails db:create db:migrate db:seed
rails server
```

Note: If you need to change the database configuration, it can be overridden using the `DB_USER`, `DB_PASS`, and `DATABASE_URL` environment variables. Example:

```
export DB_USER=postgres
export DB_PASS=password
export DATABASE_URL=postgresql://localhost/dpc-website_development
```

You also need to set the Okta ENV variables to enable Okta OAuth login for internal users:

```
export OKTA_CLIENT_ID=aaa
export OKTA_CLIENT_SECRET=bbb
export OKTA_ORG=
export OKTA_DOMAIN=
export OKTA_URL=
export OKTA_ISSUER=
export OKTA_AUTH_SERVER_ID="default"
export OKTA_REDIRECT_URI="http://localhost:3000/internal/auth/oktaoauth/callback"
```

#### Background job processing
In order to process background jobs such as sending email, you need to make sure [DelayedJob](https://github.com/collectiveidea/delayed_job) is running:

```
bin/delayed_job start
```

This command starts DelayedJob in the background. To stop DelayedJob:

```
bin/delayed_job stop
```

# Running via Docker

The DPC website can also be run via docker. Follow the below steps to build and run the website into a Docker container.

## Build the Docker Container

To build the container, simply run the following command:

```Bash
docker-compose build
```

## Start the Docker Container

The following command will start both the database server and rails server:

```Bash
docker-compose up
```

## Stop / Destroy the Docker Container

When you're done, shut down the server with the following command (this also destroys the database):

```Bash
docker-compose down
```

# Production

When running in production, the Docker Compose database should not be used. RDS should be used instead. Additionally, the following environment variable should be used to set production mode: `RAILS_ENV=production`.