FROM ruby:2.6.2-alpine

# Set the working directory
RUN mkdir /dpc-web
WORKDIR /dpc-web

# Install build dependencies
RUN apk add --no-cache postgresql-dev && \
    apk add --no-cache --virtual build-deps alpine-sdk npm tzdata

# Copy the code
COPY /dpc-web /dpc-web

# Install the website dependencies
RUN gem install bundler --no-document && bundle install

# Clean up from the build
RUN rm -rf /usr/local/bundle/cache/*.gem && \
    find /usr/local/bundle/gems/ -name "*.c" -delete && \
    find /usr/local/bundle/gems/ -name "*.o" -delete

# Start the Sidekiq process
CMD ["bundle", "exec", "sidekiq"]