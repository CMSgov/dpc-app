FROM ruby:3.3-alpine AS ruby_builder

# Install build dependencies
RUN apk update && \
    apk add --no-cache postgresql-dev && \
    apk add --no-cache libsodium-dev && \
    apk add --no-cache libffi-dev && \
    apk add --no-cache yaml-dev && \
    apk add --no-cache shared-mime-info && \
    apk add --no-cache --virtual build-deps alpine-sdk npm tzdata && \
    apk add xz && \
    apk add --no-cache gcompat && \
    apk add --no-cache curl

# Set the working directory
RUN mkdir /api-client
WORKDIR /api-client

# Copy over the files needed to fetch dependencies
COPY Gemfile Gemfile.lock api_client.gemspec /api-client/
COPY lib /api-client/lib
RUN gem install bundler --no-document && \
    bundle install
COPY app /api-client/app
COPY spec /api-client/spec
COPY coverage /api-client/coverage
COPY .rubocop.yml /api-client/.rubocop.yml
