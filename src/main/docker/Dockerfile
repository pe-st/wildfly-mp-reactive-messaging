FROM quay.io/wildfly/wildfly:27.0.1.Final-jdk17

COPY standalone.xml $JBOSS_HOME/standalone/configuration/
COPY maven/opendata-gtfs-kafka.war $JBOSS_HOME/standalone/deployments/
COPY *.properties $JBOSS_HOME/standalone/
