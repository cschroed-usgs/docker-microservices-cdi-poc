FROM docker.bintray.io/jfrog/artifactory-oss:5.3.2

MAINTAINER Ivan Suftin <isuftin@usgs.gov>

# Install the SSL certificate
COPY root.crt /usr/lib/ssl/certs/local.crt
RUN	ln -sf /usr/lib/ssl/certs/local.crt /usr/lib/ssl/certs/`openssl x509 -hash -noout -in /usr/lib/ssl/certs/local.crt`.0
RUN	/usr/bin/keytool -keystore $JAVA_HOME/jre/lib/security/cacerts -importcert -alias DOIRootCA.crt -file /usr/lib/ssl/certs/local.crt -storepass changeit -noprompt
RUN echo "$(cat /usr/lib/ssl/certs/local.crt)" | tee -a /etc/ssl/certs/ca-certificates.crt

COPY security.import.xml $ARTIFACTORY_HOME/etc/security.import.xml
COPY artifactory.config.import.xml $ARTIFACTORY_HOME/etc/artifactory.config.import.xml
