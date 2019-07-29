FROM adoptopenjdk/openjdk11:latest AS java_builder

# Install build dependencies
RUN apt-get update && apt-get install -y build-essential ruby-full fontconfig ttf-dejavu
RUN gem install jekyll

# Build the IG assets
WORKDIR /dpc-app
COPY / /dpc-app
RUN make ig/publish


FROM ruby:2.6.2-alpine AS ruby_builder

# Install build dependencies
RUN apk add --no-cache postgresql-dev && \
    apk add --no-cache --virtual build-deps alpine-sdk npm tzdata

# Set the working directory
RUN mkdir /dpc-web
WORKDIR /dpc-web

# Copy over the files needed to fetch dependencies
COPY /dpc-web/Gemfile /dpc-web/Gemfile.lock /dpc-web/
COPY /dpc-web/package.json /dpc-web/package-lock.json /dpc-web/
COPY --from=java_builder /dpc-app/dpc-web/public/ig /dpc-web/public/ig

# Install the website dependencies
RUN gem install bundler --no-document && bundle install && npm install

# Copy and build the assets pipeline
COPY /dpc-web /dpc-web
RUN RAILS_ENV=production SECRET_KEY_BASE=dummy bundle exec rake assets:precompile

# Clean up from the build
RUN rm -rf /usr/local/bundle/cache/*.gem && \
    find /usr/local/bundle/gems/ -name "*.c" -delete && \
    find /usr/local/bundle/gems/ -name "*.o" -delete && \
    rm -rf node_modules tmp/cache app/assets vendor/assets lib/assets spec


FROM ruby:2.6.2-alpine

# Install app dependencies
RUN apk add --no-cache postgresql-client tzdata

# Set the working directory
RUN mkdir /dpc-web
WORKDIR /dpc-web

# Copy the compiled Ruby app
COPY --from=ruby_builder /usr/local/bundle/ /usr/local/bundle/
COPY --from=ruby_builder /dpc-web /dpc-web

# Disable ExecJS (Install Node and remove this if we find a need for it)
ENV EXECJS_RUNTIME Disabled

# Start the rails server
CMD ["./docker/entrypoint.sh"]