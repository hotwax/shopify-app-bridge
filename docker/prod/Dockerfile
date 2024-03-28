FROM ubuntu:18.04
MAINTAINER HotWax Commerce (sandeep.kose@hotwaxsystems.com)

#Install Open JDK 11
RUN apt-get update && apt-get -y install openjdk-11-jdk ca-certificates-java git vim screen bash

ENV JAVA_HOME /usr/lib/jvm/java-11-openjdk-amd64
ENV PATH $JAVA_HOME/bin:$PATH

ARG GIT_USERNAME=
ARG GIT_PASSWORD=
ARG Moqui_HOST=
ARG Moqui_DB_HOST=
ARG Moqui_DB_USER=
ARG Moqui_DB_PASSWORD=
ARG Moqui_DB_NAME=
ARG Moqui_configuration_DB_Name=
ARG ELASTICSEARCH_HOST=
ARG Ofbiz_TRANSACTION_DB_HOST=
ARG Ofbiz_TRANSACTION_DB_USER=
ARG Ofbiz_TRANSACTION_DB_PASSWORD=
ARG Ofbiz_TRANSACTION_DB_NAME=
ARG TIME_ZONE=

RUN echo "machine git.hotwax.co" > /root/.netrc
RUN echo "login $GIT_USERNAME" >> /root/.netrc
RUN echo "password $GIT_PASSWORD" >> /root/.netrc

WORKDIR /

RUN git clone -b "v3.1.0" https://github.com/hotwax/moqui-framework.git

WORKDIR /moqui-framework
RUN git clone -b "v3.1.0" https://github.com/hotwax/moqui-runtime.git runtime

WORKDIR /moqui-framework/runtime/component
RUN git clone -b "v1.0.0" https://github.com/hotwax/shopify-app-bridge.git

RUN rm -rf /root/.netrc

WORKDIR /moqui-framework

RUN cp -rf runtime/component/shopify-app-bridge/docker/MoquiProductionConf.xml runtime/conf/
#RUN for i in runtime/component/*/patches/*.patch; do echo =========$i======; patch -p0 < $i; done
#Make it configurable using environment thing for dev/prod environment
#RUN for i in runtime/component/*/patches/staging/*.patch; do echo =========$i======; patch -p0 < $i; done

#Database Settings
RUN sed -i  's/127.0.0.1:9200/'$ELASTICSEARCH_HOST':9200/g' runtime/conf/MoquiProductionConf.xml
RUN sed -i -e 's/name="entity_ds_host" value="127.0.0.1"/name="entity_ds_host" value="'$Moqui_DB_HOST'"/g' runtime/conf/MoquiProductionConf.xml
RUN sed -i -e 's/name="webapp_http_host" value=""/name="webapp_http_host" value="'$Moqui_HOST'"/g' runtime/conf/MoquiProductionConf.xml

RUN sed -i 's/name="entity_ds_user" value="moqui"/name="entity_ds_user" value="'$Moqui_DB_USER'"/g' runtime/conf/MoquiProductionConf.xml
RUN sed -i 's/name="entity_ds_password" value="moqui"/name="entity_ds_password" value="'$Moqui_DB_PASSWORD'"/g' runtime/conf/MoquiProductionConf.xml
RUN sed -i 's/name="entity_ds_database" value="moqui"/name="entity_ds_database" value="'$Moqui_DB_NAME'"/g' runtime/conf/MoquiProductionConf.xml
RUN sed -i 's/name="entity_ds_database_configuration" value="moqui_configuration"/name="entity_ds_database_configuration" value="'$Moqui_configuration_DB_Name'"/g' runtime/conf/MoquiProductionConf.xml

#Timezone Setting
RUN sed -i 's|name="default_time_zone" value=""|name="default_time_zone" value="'$TIME_ZONE'"|g' runtime/conf/MoquiProductionConf.xml
RUN sed -i 's|name="database_time_zone" value=""|name="database_time_zone" value="'$TIME_ZONE'"|g' runtime/conf/MoquiProductionConf.xml

#Create war file.
RUN ./gradlew addRuntime
#Extract war file.
WORKDIR /moqui-deploy
RUN jar -xvf /moqui-framework/moqui-plus-runtime.war

ENV DB_LOAD="./gradlew loadProduction -Ptypes=seed,seed-initial,install,ext -Praw"
ENV JAVA_OPTS="-Xms512m -Xmx4096m"
ENV CONF_FILE="/moqui-framework/runtime/conf/MoquiProductionConf.xml"
ENV SLEEP="sleep 2"

EXPOSE 8080

ENTRYPOINT $SLEEP && screen -dmS Moqui java $JAVA_OPTS -cp . MoquiStart port=8080 conf=$CONF_FILE && tail -F runtime/log/moqui.log && bash
