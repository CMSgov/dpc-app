FROM artifactory.cloud.cms.gov/docker/amazoncorretto:17-alpine-jdk

# Add Maven Daemon
ARG MVND_VERSION=1.0.3
ADD https://github.com/apache/maven-mvnd/releases/download/${MVND_VERSION}/maven-mvnd-${MVND_VERSION}-linux-amd64.zip .

# Install unzip and mvnd
RUN apk update &&  \
    apk add --no-cache unzip && \
    mkdir /opt/mvnd &&  \
    unzip maven-mvnd-1.0.3-linux-amd64 && \
    mv maven-mvnd-1.0.3-linux-amd64/* /opt/mvnd
