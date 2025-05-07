FROM ubuntu:18.04
LABEL maintainer="HotWax Commerce <sysadmin@hotwaxsystems.com>"

# Install necessary packages in a single RUN command to reduce image layers
RUN apt-get update && \
    apt-get -y install openjdk-11-jdk ca-certificates-java git vim screen bash && \
    apt-get clean

# Set environment variables
ENV JAVA_HOME /usr/lib/jvm/java-11-openjdk-amd64
ENV PATH $JAVA_HOME/bin:$PATH

# Argument for git credentials
ARG GIT_USERNAME
ARG GIT_PASSWORD

# Configure git credentials
RUN echo -e "machine git.hotwax.co\nlogin $GIT_USERNAME\npassword $GIT_PASSWORD" > /root/.netrc

# Clone the necessary repositories in one RUN command to reduce image layers
WORKDIR /
RUN git clone --depth 1 -b "v3.3.0" https://github.com/hotwax/moqui-framework.git /moqui-framework
WORKDIR /moqui-framework
RUN git clone --depth 1 -b "v3.2.0" https://github.com/hotwax/moqui-runtime.git runtime
WORKDIR /moqui-framework/runtime/component
RUN git clone -b "v2.0.0" https://github.com/hotwax/shopify-app-bridge.git

WORKDIR /moqui-framework

# Remove git credentials file
RUN rm -rf /root/.netrc
RUN echo "org.gradle.daemon=false" | tee -a gradle.properties


# Copy configuration files
RUN cp -rf runtime/component/shopify-app-bridge/docker/MoquiProductionConf.xml runtime/conf/

# Create WAR file and extract it
RUN ./gradlew addRuntime
RUN mkdir -p /moqui-deploy && \
    cd /moqui-deploy && \
    jar -xvf /moqui-framework/moqui-plus-runtime.war

# Set environment variables
ENV DB_LOAD="./gradlew loadProduction -Ptypes=seed,seed-initial,install,ext-seed,ext -Praw"
ENV JAVA_OPTS="-Xms1024m -Xmx4096m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/moqui-framework/runtime/log/"
ENV CONF_FILE="/moqui-framework/runtime/conf/MoquiProductionConf.xml"
ENV SLEEP="sleep 2"

# Define additional environment variables for OFBiz and Moqui configuration
ENV MOQUI_HOST=
ENV MOQUI_DB_HOST=
ENV MOQUI_DB_USER=
ENV MOQUI_DB_PASSWORD=
ENV MOQUI_DB_NAME=

ENV ELASTICSEARCH_HOST=
ENV ELASTICSEARCH_USER=
ENV ELASTICSEARCH_PASSWORD=
ENV TIME_ZONE=America/New_York
ENV SCHEDULED_JOB_CHECK_TIME="60"

WORKDIR /moqui-deploy
# Copy entrypoint script and set executable permissions
COPY docker/entrypoint.sh .
RUN chmod +x entrypoint.sh

# Expose the application port
EXPOSE 8080

# Define the entrypoint
ENTRYPOINT ./entrypoint.sh && bash
