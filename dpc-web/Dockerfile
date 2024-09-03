FROM ruby:3.3-alpine AS ruby_builder

# Install build dependencies
RUN apk update && \
    apk add --no-cache postgresql-dev && \
    apk add --no-cache shared-mime-info && \
    apk add --no-cache --virtual build-deps alpine-sdk npm tzdata && \
    apk add xz && \
    apk add --no-cache gcompat

# Set the working directory
RUN mkdir -p /dpc-web/vendor/api_client
WORKDIR /dpc-web

# Copy over the files needed to fetch dependencies
COPY /engines/api_client/ /dpc-web/vendor/api_client/
COPY /dpc-web/Gemfile /dpc-web/Gemfile.lock /dpc-web/
COPY /dpc-web/package.json /dpc-web/package-lock.json /dpc-web/

# Install the website dependencies
RUN gem install bundler --no-document && \
    gem uninstall nokogiri -I && \
    bundle config set force_ruby_platform true && \
    bundle install && \
    npm install

# Run bundler audit
RUN bundle exec bundle audit update && bundle exec bundle audit check --ignore CVE-2015-9284

# Copy the code, test the app, and build the assets pipeline
COPY /dpc-web /dpc-web

RUN RAILS_ENV=production SECRET_KEY_BASE=dummy bundle exec rake assets:precompile

# Clean up from the build
RUN rm -rf /usr/local/bundle/cache/*.gem && \
    find /usr/local/bundle/gems/ -name "*.c" -delete && \
    find /usr/local/bundle/gems/ -name "*.o" -delete && \
    find /usr/local/bundle/gems/ -name "Gemfile.lock" -delete


FROM ruby:3.3-alpine

# Add HHS CA Cert
COPY certs/HHS-FPKI-Intermediate-CA-E1.cer /usr/local/share/ca-certificates/HHS-FPKI-Intermediate-CA-E1.cer
COPY certs/fcpcag2.cer /usr/local/share/ca-certificates/fcpcag2.cer

RUN cat /usr/local/share/ca-certificates/fcpcag2.cer >> /etc/ssl/certs/ca-certificates.crt && \
    cat /usr/local/share/ca-certificates/HHS-FPKI-Intermediate-CA-E1.cer >> /etc/ssl/certs/ca-certificates.crt && \
    apk --no-cache add \
        curl
        
# Install app dependencies
RUN apk add --no-cache postgresql-client nodejs tzdata

# Set the working directory
RUN mkdir /dpc-web
WORKDIR /dpc-web

# Copy the compiled Ruby app
COPY --from=ruby_builder /usr/local/bundle/ /usr/local/bundle/
COPY --from=ruby_builder /dpc-web /dpc-web

# Declare the entrypoint shell script
ENTRYPOINT ["./docker/entrypoint.sh"]

# Default to running the rails server
CMD ["web"]
