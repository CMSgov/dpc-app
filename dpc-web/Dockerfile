FROM adoptopenjdk/openjdk11:latest AS java_builder

# Install build dependencies
RUN apt-get update && apt-get install -y build-essential ruby-full fontconfig ttf-dejavu
RUN gem install jekyll

# Build the IG assets
WORKDIR /dpc-app
COPY / /dpc-app
RUN make ig/publish


FROM ruby:2.6.5-alpine AS ruby_builder

# Install build dependencies
RUN apk add --no-cache postgresql-dev && \
    apk add --no-cache --virtual build-deps alpine-sdk npm tzdata

# Set the working directory
RUN mkdir /dpc-web
WORKDIR /dpc-web

# Copy over the files needed to fetch dependencies
COPY /dpc-web/Gemfile /dpc-web/Gemfile.lock /dpc-web/
COPY /dpc-web/package.json /dpc-web/package-lock.json /dpc-web/

# Install the website dependencies
RUN gem install bundler --no-document && bundle install && npm install

# Run bundler audit
RUN bundle exec bundle audit update && bundle exec bundle audit check --ignore CVE-2015-9284

# Copy the code, test the app, and build the assets pipeline
COPY /dpc-web /dpc-web
COPY --from=java_builder /dpc-app/dpc-web/public/ig /dpc-web/public/ig
RUN RAILS_ENV=production SECRET_KEY_BASE=dummy bundle exec rake assets:precompile

# Clean up from the build
RUN rm -rf /usr/local/bundle/cache/*.gem && \
    find /usr/local/bundle/gems/ -name "*.c" -delete && \
    find /usr/local/bundle/gems/ -name "*.o" -delete


FROM ruby:2.6.5-alpine

# Install app dependencies
RUN apk add --no-cache postgresql-client nodejs tzdata

# Set the working directory
RUN mkdir /dpc-web
WORKDIR /dpc-web

# Copy the compiled Ruby app
COPY --from=ruby_builder /usr/local/bundle/ /usr/local/bundle/
COPY --from=ruby_builder /dpc-web /dpc-web

# Start the rails server
CMD ["./docker/entrypoint.sh"]