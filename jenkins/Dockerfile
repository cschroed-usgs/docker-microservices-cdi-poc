FROM jenkins:2.60.1

MAINTAINER Ivan Suftin <isuftin@usgs.gov>

USER root

# Install the SSL certificate
COPY root.crt /usr/lib/ssl/certs/local.crt
RUN	ln -sf /usr/lib/ssl/certs/local.crt /usr/lib/ssl/certs/`openssl x509 -hash -noout -in /usr/lib/ssl/certs/local.crt`.0
RUN	/usr/bin/keytool -keystore $JAVA_HOME/jre/lib/security/cacerts -importcert -alias DOIRootCA.crt -file /usr/lib/ssl/certs/local.crt -storepass changeit -noprompt
RUN echo "$(cat /usr/lib/ssl/certs/local.crt)" | tee -a /etc/ssl/certs/ca-certificates.crt

# Install the Docker engine and commandline client
RUN apt-get update && apt-get install -y apt-transport-https ca-certificates curl gnupg2 software-properties-common sudo
RUN curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add -
RUN add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable"
RUN apt-get update && apt-get -y install docker-ce
RUN apt-get install -y python3-pip python3-dev build-essential virtualenv
RUN /usr/bin/pip3 install --upgrade pip
RUN usermod -aG docker jenkins

# This gets us around having to manually add github to the known hosts file
RUN mkdir /usr/share/jenkins/ref/.ssh
RUN ssh-keyscan -t rsa,dsa -H github.com >> /usr/share/jenkins/ref/.ssh/known_hosts

USER jenkins

# Install plugins
COPY plugins.txt /plugins.txt

# This works until https://github.com/jenkinsci/docker/pull/365 is merged
RUN /usr/local/bin/install-plugins.sh $(cat /plugins.txt | tr '\n' ' ')

ENV JAVA_OPTS ${JENKINS_JAVA_OPTS:-"-Djenkins.install.runSetupWizard=false"}
ENV JENKINS_HOME /var/jenkins_home

# Copy RSA keys
COPY secrets/id_rsa /usr/share/jenkins/ref/.ssh/id_rsa
COPY secrets/id_rsa.pub /usr/share/jenkins/ref/.ssh/id_rsa.pub

# Copy .pypirc file
COPY secrets/.pypirc /usr/share/jenkins/ref/.pypirc

# Copy pip.conf file
COPY pip.conf /usr/share/jenkins/ref/.pip/pip.conf

USER root
RUN chown -R jenkins:jenkins /usr/share/jenkins/ref/.ssh && chmod 700 /usr/share/jenkins/ref/.ssh && chmod 600 /usr/share/jenkins/ref/.ssh/*
USER jenkins

# Copy in the jobs
COPY jobs/dropwizard.xml /dropwizard.xml
COPY jobs/dropwizard_docker_image.xml /dropwizard_docker_image.xml
COPY jobs/build_python_wheel.xml /build_python_wheel.xml
COPY jobs/python_docker_image.xml /python_docker_image.xml

# Copy in the scripts
COPY scripts/start_sshd.groovy /usr/share/jenkins/ref/init.groovy.d/1_start_sshd.groovy
COPY scripts/default_user.groovy /usr/share/jenkins/ref/init.groovy.d/2_default_user.groovy
COPY scripts/add_github_token.groovy /usr/share/jenkins/ref/init.groovy.d/3_add_github_token.groovy
COPY scripts/add_artifactory_server.groovy /usr/share/jenkins/ref/init.groovy.d/4_add_artifactory_server.groovy
COPY scripts/install_maven.groovy /usr/share/jenkins/ref/init.groovy.d/5_install_maven.groovy
COPY scripts/add_registry_credentials.groovy /usr/share/jenkins/ref/init.groovy.d/6_add_registry_credentials.groovy
COPY scripts/set_git_global_config.groovy /usr/share/jenkins/ref/init.groovy.d/7_set_git_global_config.groovy
COPY scripts/add_github_ssh_credentials.groovy /usr/share/jenkins/ref/init.groovy.d/8_add_github_ssh_credentials.groovy

COPY scripts/dropwizard_config.groovy /usr/share/jenkins/ref/init.groovy.d/50_dropwizard_config.groovy
COPY scripts/dropwizard_docker_image_config.groovy /usr/share/jenkins/ref/init.groovy.d/51_dropwizard_docker_image_config.groovy
COPY scripts/build_python_wheel_config.groovy /usr/share/jenkins/ref/init.groovy.d/60_build_python_wheel_config.groovy
COPY scripts/python_docker_image_config.groovy /usr/share/jenkins/ref/init.groovy.d/61_python_docker_image_config.groovy
