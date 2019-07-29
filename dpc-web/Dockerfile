FROM adoptopenjdk/openjdk11:latest AS JAVA_BUILDER

# Install build dependencies
RUN apt-get update && apt-get install -y build-essential ruby-full fontconfig ttf-dejavu
RUN gem install jekyll

# Build the IG assets
WORKDIR /dpc-app
COPY / /dpc-app
RUN make ig/publish


FROM ruby:2.6.2-alpine

# Install build dependencies
RUN apk add --no-cache postgresql-dev && \
    apk add --no-cache --virtual build-deps alpine-sdk npm tzdata

# Set the working directory
RUN mkdir /dpc-web
WORKDIR /dpc-web

# Copy over the files needed to fetch dependencies
COPY /dpc-web/Gemfile /dpc-web/Gemfile
COPY /dpc-web/Gemfile.lock /dpc-web/Gemfile.lock
COPY /dpc-web/package.json /dpc-web/package.json
COPY /dpc-web/package-lock.json /dpc-web/package-lock.json

# Install the website dependencies
RUN gem install bundler --no-document && bundle install && npm install

COPY /dpc-web /dpc-web
COPY --from=JAVA_BUILDER /dpc-app/dpc-web/public/ig /dpc-web/public/ig

# Start the rails server
CMD ["./docker/entrypoint.sh"]