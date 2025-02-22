#!/bin/bash
#Database Settings
sed -i  's/127.0.0.1:9200/'$ELASTICSEARCH_HOST':9200/g' $CONF_FILE
sed -i -e 's/name="webapp_http_host" value=""/name="webapp_http_host" value="'$MOQUI_HOST'"/g' $CONF_FILE
sed -i 's/name="elasticsearch_index_prefix" value="localhost_"/name="elasticsearch_index_prefix" value="'${OFBIZ_INSTANCE_NAME}'_"/g' $CONF_FILE
sed -i 's/name="elasticsearch_user" value=""/name="elasticsearch_user" value="'${ELASTICSEARCH_USER}'_"/g' $CONF_FILE
sed -i 's/name="elasticsearch_password" value=""/name="elasticsearch_password" value="'${ELASTICSEARCH_PASSWORD}'_"/g' $CONF_FILE

sed -i -e 's/name="entity_ds_host" value="127.0.0.1"/name="entity_ds_host" value="'$MOQUI_DB_HOST'"/g' $CONF_FILE
sed -i 's/name="entity_ds_user" value="moqui"/name="entity_ds_user" value="'$MOQUI_DB_USER'"/g' $CONF_FILE
sed -i 's/name="entity_ds_password" value="moqui"/name="entity_ds_password" value="'$MOQUI_DB_PASSWORD'"/g' $CONF_FILE
sed -i 's/name="entity_ds_database" value="moqui"/name="entity_ds_database" value="'$MOQUI_DB_NAME'"/g' $CONF_FILEE

#Timezone Setting
sed -i 's|name="default_time_zone" value=""|name="default_time_zone" value="'$TIME_ZONE'"|g' $CONF_FILE
sed -i 's|name="database_time_zone" value=""|name="database_time_zone" value="'$TIME_ZONE'"|g' $CONF_FILE

#Control schedule job run setting, default is 60 set to 0 to not run scheduled jobs.
sed -i 's|name="scheduled_job_check_time" value="60"|name="scheduled_job_check_time" value="'$SCHEDULED_JOB_CHECK_TIME'"|g' $CONF_FILE

$SLEEP
screen -dmS Moqui java $JAVA_OPTS -cp . MoquiStart port=8080 conf=$CONF_FILE
tail -F runtime/log/moqui.log