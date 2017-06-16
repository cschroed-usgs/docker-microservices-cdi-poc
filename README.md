## Artifactory

Included is the Artifactory Docker Compose configuration. Artifactory OSS does not
include a Docker repository capability. If you have a license, you can try using the
pro version.

Add Insecure Registry
---
https://docs.docker.com/registry/insecure/
http://akrambenaissi.com/2015/11/17/addingediting-insecure-registry-to-docker-machine-afterwards/

- Edit ~/.docker/machine/machines/cdi-poc/config.json
- docker-machine provision cdi-poc

Generate Wildcard Certificate
---
openssl genrsa -out data/certs/wildcard.key 2048
openssl req -nodes -newkey rsa:2048 -keyout data/certs/wildcard.key -out data/certs/wildcard.csr -subj "/C=US/ST=Wisconsin/L=Middleon/O=US Geological Survey/OU=WMA/CN=registry"
openssl x509 -req -days 9999 -in data/certs/wildcard.csr -signkey data/certs/wildcard.key  -out data/certs/wildcard.crt

Create Auth password
---
`docker run --entrypoint htpasswd registry:2 -Bbn testuser testpassword > auth/htpasswd`

`docker-machine create -d virtualbox --virtualbox-memory "4096" --virtualbox-hostonly-nictype Am79C973 cdi-poc`

```bash
echo "sudo mkdir -p /var/lib/boot2docker/certs; \
echo "\""$(cat root.crt)"\"" | \
sudo tee -a /var/lib/boot2docker/certs/root.crt" | \
docker-machine ssh cdi-poc && docker-machine restart cdi-poc
```

## Jenkins

Jenkins is the glue that creates the Continuous Delivery pipeline.
The Jenkins Docker container included in this project is set up to pull down
the source code for the microservices, compile them, deploy them to an artifact
repository, create the Docker images that use the microservice artifacts and
deploy those Docker images to a private Docker repository.

The Jenkins server is also then able to use those Docker images to perform multiple
types of testing as well as deploy those images as running servers.

#### Building the Jenkins container

When building the Jenkins container, there are a number of pieces of information
that the container will need in order to properly push and pull to various repositories
both externally (like GitHub) as well as in other Docker containers.

##### The ./jenkins/secrets directory
If one does not already exist, create a directory under jenkins named `secrets`.
Within this directory, you should have three files in there that you will need to
provide:

- `root.crt`: This is the SSL intercept certificate used in the organization. This
file is imported into the Docker image and used to update openssl and the Java
keystore so that the Jenkins instance may communicate with HTTPS websites
- `id_rsa and id_rsa.pub`: This pair of files should be your ssh keypair that is used
in GitHub to provide access to the repositories that include your microservice stacks.
See [GitHub documentation](https://help.github.com/articles/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent/) for more information

#### Environments file

Docker uses an environments file to alter the functionality of the
running Jenkins container. The environments file contains the following keys:

- `JAVA_OPTS`: This defines the Java options that Jenkins uses for startup. By default,
the only option used is to skip the wizard setup process when the Jenkins container
starts. The Jenkins container should already be set up and ready for you to build
when it starts. The option used is `-Djenkins.install.runSetupWizard=false`. For
other options that can be used, see [the Jenkins wiki on the subject](https://wiki.jenkins-ci.org/display/JENKINS/Features+controlled+by+system+properties)
- `JENKINS_SSH_PORT`: This Jenkins container is set to start the sshd server. This
setting dictates the port that the sshd server will listen on. By default, this is
set to 0, which is random
- `JENKINS_USER`: This is the username that will be created as the default admin user
when Jenkins starts. By default, this is `jenkins`
- `JENKINS_PASS`: This is the password that will be created for the default admin user
when Jenkins starts. By default, this is `jenkins`
- `JENKINS_HOME`: Dictates where the Jenkins home directory will be. Typically, you
do not want to change this. By default it will be `/var/jenkins_home`.
- `GITHUB_ACCESS_TOKEN`: Jenkins needs to access GitHub for reading and writing.
This access token is entered as a Jenkins credential with the key of `GITHUB_ACCESS_TOKEN`.
There is no default for this. You will need to enter your own. See [GitHub help](https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/)
for more information on this
- `GITHUB_CONFIG_NAME`: This is the Github username that Jenkins will use for interaction.
Not having this causes GitHub commits to fail. Default is a placeholder. Use your own.
- `GITHUB_CONFIG_EMAIL`: This is the Github email that Jenkins will use for interaction.
Not having this causes GitHub commits to fail. Default is a placeholder. Use your own.
- `MAVEN_INSTALLATION_VERSION`: Specifies the version of Maven to install when the
container kicks off. Version `3.5.3` is default
- `ARTIFACTORY_ID`: This is the ID for the Artifactory server in the Jenkins Artifactory
plugin. By default, this is `artifactory`.
- `ARTIFACTORY_ADDRESS`: The address of the Artifactory server. This is by default
`http://artifactory:8081/artifactory`. The domain name is valid when the Artifactory
server is running on Docker compose
- `ARTIFACTORY_DEPLOYER_USERNAME`: The username of the admin user in Artifactory.
By default, this is `admin`. This can be changed. To see how, read the Artifactory
section of this README
- `ARTIFACTORY_DEPLOYER_PASSWORD`: The password of the admin user in Artifactory.
By default, this is `adminpassword`. This can be changed. To see how, read the Artifactory
section of this README
- `ARTIFACTORY_RESOLVER_USERNAME`: The URL resolver can be used to resolve dependencies and/or for deployment of both regular artifacts and Ivy module files. The username is blank here.
- `ARTIFACTORY_RESOLVER_PASSWORD`: The URL resolver can be used to resolve dependencies and/or for deployment of both regular artifacts and Ivy module files. The password is blank here.
See [JFrog documentation](https://www.jfrog.com/confluence/display/RTF/Working+with+Ivy#WorkingwithIvy-TheURLResolver) for more information
- `PRIVATE_REGISTRY_USER`: The username for the private Docker registry. Default is `testuser`
- `PRIVATE_REGISTRY_PASSWORD`: The password for the private Docker registry. Default is `testpassword`

##### Local environments file

Docker compose uses a file named `compose.env` located in the jenkins subdirectory.
The values in the compose.env file are default and some are placeholders. You may
replace the values in this file if you wish. However, to maintain compatibility with
future releases as well as avoiding conflicts and secret leaking if contributing
back to the project, you should copy the contents of `compose.env` into another file.
When developing, I tend to call the new file `compose_local.env`. The configuration
for Docker compose is such that you can use your personal version of the file by
creating an environment variable in your shell named `DOCKER_JENKINS_ENV_LOCAL`.
You can set the value to `_local` and Docker compose will then look for the file
`compose_local.env`. The configuration looks for `./jenkins/compose${DOCKER_JENKINS_ENV_LOCAL}.env`

##### Plugins

The Jenkins Docker container is set up to download all the plugins it needs when
it starts up for the first time. However, over time these plugins may become outdated.
You can update the plugins on a running Jenkins container. To make the change permanent
for yourself, you can run the following from the root directory of this project
once you've updated the plugins:

```
JENKINS_HOST=http://jenkins:jenkins@`docker-machine ip <machine name>`:8080
curl -sSL "$JENKINS_HOST/pluginManager/api/xml?depth=1&xpath=/*/*/shortName|/*/*/version&wrapper=plugins" \
  | perl -pe 's/.*?<shortName>([\w-]+).*?<version>([^<]+)()(<\/\w+>)+/\1 \2\n/g'|sed 's/ /:/' > jenkins/plugins.txt
```

This queries a running Jenkins server for all of the plugins and versions it is
using and puts them into the proper file in the jenkins directory. This file is
used by Jenkins when it is started. This list will become a permanent part of your
container when you rebuild it. When using Docker compose, you can also run Jenkins
via `docker-compose up --build jenkins`. This ensures that the latest versions of
all of your files are running in the Docker container.

#### Jobs

##### Dropwizard

This job builds the Dropwizard example project located [in GitHub](https://github.com/USGS-CIDA/poc-dropwizard-example).
Once the Java artifact has been created, Jenkins deploys the artifact to Artifactory.
If a release is created for this artifact, the release is also then sent to Artifactory.

This job, on success, will trigger the Dropwizard Docker Image job.

##### Dropwizard Docker Image

This job takes the archived artifacts from the Dropwizard job and uses them to build
a Docker image. Once the Docker image is built, the job will then deploy that image
to the private Docker registry running in a seperate container.

##### Gotchas

Sometimes when the Jenkins container fires up for the first time, the Maven
auto-installation doesn't happen. Because of this, your build may fail. If you see
a Maven missing error on a build, go to Manage Jenkins -> Global Tool Configuration
scroll to Maven Installations and
