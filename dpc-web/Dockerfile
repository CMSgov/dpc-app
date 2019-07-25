FROM ruby:2.6.2-alpine

# Install build dependencies
RUN apk add --no-cache postgresql-dev alpine-sdk npm tzdata

# Set the working directory
RUN mkdir /dpc-web
WORKDIR /dpc-web

# Copy over the files needed to fetch dependencies
COPY ../Gemfile /dpc-web/Gemfile
COPY ../Gemfile.lock /dpc-web/Gemfile.lock
COPY package.json /dpc-web/package.json
COPY package-lock.json /dpc-web/package-lock.json

# Install the website dependencies
RUN gem install bundler --no-document && bundle install && npm install

# Copy the website
COPY . /dpc-web

# Start the rails server
CMD ["rails", "server", "-b", "0.0.0.0"]