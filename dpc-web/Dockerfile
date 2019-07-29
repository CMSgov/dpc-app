FROM ruby:2.6.2-alpine

# Install build dependencies
RUN apk add --no-cache postgresql-dev alpine-sdk npm tzdata fontconfig ttf-dejavu
RUN apk add --no-cache --repository=http://dl-cdn.alpinelinux.org/alpine/edge/community openjdk11

# Configure Alpine Java dependencies
RUN ln -s /usr/lib/libfontconfig.so.1 /usr/lib/libfontconfig.so && \
    ln -s /lib/libuuid.so.1 /usr/lib/libuuid.so.1 && \
    ln -s /lib/libc.musl-x86_64.so.1 /usr/lib/libc.musl-x86_64.so.1
ENV LD_LIBRARY_PATH /usr/lib

# Set the working directory
RUN mkdir /dpc-app
WORKDIR /dpc-app/dpc-web

# Copy over the files needed to fetch dependencies
COPY /dpc-web/Gemfile /dpc-app/dpc-web/Gemfile
COPY /dpc-web/Gemfile.lock /dpc-app/dpc-web/Gemfile.lock
COPY /dpc-web/package.json /dpc-app/dpc-web/package.json
COPY /dpc-web/package-lock.json /dpc-app/dpc-web/package-lock.json

# Install the website dependencies
RUN gem install bundler --no-document && bundle install && npm install
RUN gem install jekyll

# Copy the website and dependencies needed to build supplimental website components
RUN mkdir -p /dpc-app/dpc-common/src/main/resources/validations && mkdir -p /dpc-app/dpc-api/src/main/resources
COPY /dpc-web /dpc-app/dpc-web
COPY /Makefile /dpc-app/Makefile
COPY /ig /dpc-app/ig
COPY /dpc-common/src/main/resources/validations /dpc-app/dpc-common/src/main/resources/validations
COPY /dpc-api/src/main/resources /dpc-app/dpc-api/src/main/resources

# Build the IG component
WORKDIR /dpc-app
RUN make ig/publish

# Clean up after building
RUN mv /dpc-app/dpc-web /dpc-web && cd / && rm -rf dpc-app
WORKDIR /dpc-web

# Start the rails server
CMD ["./docker/entrypoint.sh"]