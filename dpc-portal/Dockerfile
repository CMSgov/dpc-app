FROM ruby:3.0.6-alpine AS ruby_builder

# Add HHS CA Cert
COPY HHS-FPKI-Intermediate-CA-E1.pem /usr/local/share/ca-certificates/HHS-FPKI-Intermediate-CA-E1.pem
RUN update-ca-certificates

# Install build dependencies
RUN apk update && \
    apk add --no-cache postgresql-dev && \
    apk add --no-cache libsodium-dev && \
    apk add --no-cache shared-mime-info && \
    apk add --no-cache --virtual build-deps alpine-sdk npm tzdata && \
    apk add xz && \
    apk add --no-cache gcompat

# Set the working directory
RUN mkdir -p /dpc-portal/vendor/api_client
WORKDIR /dpc-portal

# Copy over the files needed to fetch dependencies
COPY /engines/api_client/Gemfile /engines/api_client/Gemfile.lock /engines/api_client/api_client.gemspec /dpc-portal/vendor/api_client/
COPY /engines/api_client/lib/api_client/version.rb /dpc-portal/vendor/api_client/lib/api_client/
COPY /dpc-portal/Gemfile /dpc-portal/Gemfile.lock /dpc-portal/
COPY /dpc-portal/package.json /dpc-portal/
COPY /dpc-portal/package-lock.json /dpc-portal/

# Install the website dependencies
RUN gem install bundler --no-document && \
    gem uninstall nokogiri -I && \
    bundle config set force_ruby_platform true && \
    bundle install && \
    npm install

# Run bundler audit
RUN bundle exec bundle audit update && bundle exec bundle audit check --ignore CVE-2015-9284

# Copy the code, test the app, and build the assets pipeline
COPY /engines/api_client /dpc-portal/vendor/api_client
COPY /dpc-portal /dpc-portal

RUN rm -rf /bin/yarn
RUN npm run build:css
RUN RAILS_ENV=production SECRET_KEY_BASE=dummy bundle exec rake assets:precompile --trace

# Clean up from the build
RUN rm -rf /usr/local/bundle/cache/*.gem && \
    find /usr/local/bundle/gems/ -name "*.c" -delete && \
    find /usr/local/bundle/gems/ -name "*.o" -delete

# Declare the entrypoint shell script
ENTRYPOINT ["./docker/entrypoint.sh"]

# Default to running the rails server
CMD ["portal"]
