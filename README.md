## Workflow

Development workflow: Merge new code to Dropwizard project -> Jenkins checks out
new code and builds -> Artifact is shipped to Artifactory -> Jenkins uses successfully
built archived artifacts to create new Docker image -> Docker image is pushed to
private Docker repository at "LATEST" version

Release workflow: New release is created for the Dropwizard project using Jenkins ->
Jenkins checks out code and builds new artifact -> Jenkins ships new artifact to
Artifactory -> Jenkins uses successfully built archived artifacts to create new
Docker image at a specific release tag -> Docker image is pushed to Artifactory

## Docker Software Versions

For testing this configuration, I have the following software installed at these versions:

- Docker Machine: 0.12.10
- Docker Compose: 1.13.0
- Docker Client: 17.05.0-ce

## Docker Compose

For most of the work done in this continuous delivery POC, I use Docker Compose to
orchestrate building, startup and shutdown of the Docker containers. All of the
instructions provided are for Docker compose. The Docker compose configuration is
provided with this project.

## Docker Machine ([tl;dr](#machine-tldr))

Please be aware that it is not strictly necessary to use Docker Machine if using a
Linux OS as a host. However, if you desire to keep the CDI POC encapsulated, be aware
that `boot2docker` running in VirtualBox will mount the `/home` directory of a Linux
host to `/hosthome`. This can be addressed by doing the following soft linking from the host:

```
$docker-machine ssh cdi-poc ln -s -f /hosthome/your_user_on_the_host /home/your_user_on_the_host
```  
A gotcha of doing this is that `/home/your_user_on_the_host` has been found to
be owned by root when the `boot2docker` is restarted. The command should be rerun
if the `boot2docker` VM is restarted. Alternatively, a `/var/lib/boot2docker/bootlocal.sh`
can be created on the VM that is run on boot. This can be done as follows:

```
$printf "#\!/bin/sh\n\nln -s -f \"/hosthome/${USER}\" \"/home/${USER}\"\nchown -R docker:staff \"/home/${USER}\"\n" | docker-machine ssh cdi-poc sudo tee /var/lib/boot2docker/bootlocal.sh
$docker-machine ssh cdi-poc sudo chmod +x /var/lib/boot2docker/bootlocal.sh
```

In addition, if using Docker Machine on a Linux host, if you went through the procedure outlined at https://docs.docker.com/engine/installation/linux/linux-postinstall/, you will need to backout those
changes. Otherwise, Docker commands will only be executed on the host rather than on the VM _despite_
the environment being set to use the VM.

This Continuous Delivery implementation works when using Docker Machine. A large
reason for this is because the Docker engine lives in the Docker Machine VM. This
allows the Jenkins Docker container to take advantage of that Docker engine by using
it to create Docker images and interact with the private Docker registry. While
it's possible to run the [Docker engine within a Docker container](https://github.com/jpetazzo/dind),
it's not ideal and has a lot of caveats that take away from the way this Continuous
delivery pipeline would be set up in the real world. The Jenkins Docker container
interacts with the Docker Machine Docker engine in the same way as the host running
Docker Machine does: via HTTPS.

When creating a Docker Machine VM, for this reference implementation, I would suggest
creating one with about 4G of RAM and at least 2 CPUs. If using MacOS, I would also
suggest [initiating the machine with the PCnet-FAST III NIC type](https://github.com/docker/machine/issues/1942).

An example of a launch would look like this:

```
$ docker-machine create -d virtualbox --virtualbox-memory "4096" --virtualbox-hostonly-nictype Am79C973 --virtualbox-cpu-count "2" cdi-poc

Running pre-create checks...
(cdi-poc) Default Boot2Docker ISO is out-of-date, downloading the latest release...
(cdi-poc) Latest release for github.com/boot2docker/boot2docker is v17.05.0-ce
(cdi-poc) Downloading /Users/isuftin/.docker/machine/cache/boot2docker.iso from https://github.com/boot2docker/boot2docker/releases/download/v17.05.0-ce/boot2docker.iso...
(cdi-poc) 0%....10%....20%....30%....40%....50%....60%....70%....80%....90%....100%
Creating machine...
(cdi-poc) Copying /Users/isuftin/.docker/machine/cache/boot2docker.iso to /Users/isuftin/.docker/machine/machines/cdi-poc/boot2docker.iso...
(cdi-poc) Creating VirtualBox VM...
(cdi-poc) Creating SSH key...
(cdi-poc) Starting the VM...
(cdi-poc) Check network to re-create if needed...
(cdi-poc) Waiting for an IP...
Waiting for machine to be running, this may take a few minutes...
Detecting operating system of created instance...
Waiting for SSH to be available...
Detecting the provisioner...
Provisioning with boot2docker...
Copying certs to the local machine directory...
Copying certs to the remote machine...
Setting Docker configuration on the remote daemon...
Checking connection to Docker...
Docker is up and running!
To see how to connect your Docker Client to the Docker Engine running on this virtual machine, run: docker-machine env cdi-poc
```

Now, after typing `$ eval $(docker-machine env cdi-poc)` my Docker client on my MacOS
host can freely communicate with Docker Machine.  

#### Version requirement

The current version of Docker Machine that this project has been tested with is `0.12.0`

#### Root SSL certificates
When on the DOI network, or any network with a requirement that all SSL traffic go
through a proxy, you must append your organization's root certificate into the VM
running the Docker engine. Not doing so leaves the Docker engine unable to pull
Docker images from Dockerhub due to the inability to verify TLS communication.
Download your organization's root certificate to your current directory. Once you have it locally, you can copy it directly to your VM and restart it. In this example, the root certificate is named `root.crt` in my current directory and my VM's name is `cdi-poc`:

```
$ echo "sudo mkdir -p /var/lib/boot2docker/certs; \
echo "\""$(cat root.crt)"\"" | \
sudo tee -a /var/lib/boot2docker/certs/root.crt" | \
docker-machine ssh cdi-poc && docker-machine restart cdi-poc

Boot2Docker version 17.05.0-ce, build HEAD : 5ed2840 - Fri May  5 21:04:09 UTC 2017
Docker version 17.05.0-ce, build 89658be
-----BEGIN CERTIFICATE-----
MIIJ+jCCB...kVEl2DE0WcUw=
-----END CERTIFICATE-----
Restarting "cdi-poc"...
(cdi-poc) Check network to re-create if needed...
(cdi-poc) Waiting for an IP...
Waiting for SSH to be available...
Detecting the provisioner...
Restarted machines may have new IP addresses. You may need to re-run the `docker-machine env` command.
```

Now you can try to pull a small image to test:

```
$ docker pull alpine
Using default tag: latest
latest: Pulling from library/alpine
2aecc7e1714b: Pull complete
Digest: sha256:0b94d1d1b5eb130dd0253374552445b39470653fb1a1ec2d81490948876e462c
Status: Downloaded newer image for alpine:latest
```

#### <a name="machine-tldr"></a>TL;DR

Any text prefaced with `$` is a bash command

- Copy your organization's root SSL certificate to your current working directory

- `$ docker-machine create -d virtualbox --virtualbox-memory "4096" --virtualbox-hostonly-nictype Am79C973 --virtualbox-cpu-count "2" cdi-poc`

- `$ echo "sudo mkdir -p /var/lib/boot2docker/certs; \
echo "\""$(cat root.crt)"\"" | \
sudo tee -a /var/lib/boot2docker/certs/root.crt" | \
docker-machine ssh cdi-poc && docker-machine restart cdi-poc`

- `$ eval $(docker-machine env cdi-poc)`

## Artifactory ([tl;dr](#artifactory-tldr))

Included is the Artifactory Docker Compose configuration. This server is used as a reference implementation of a Maven repository. To build the Artifactory OSS version, simply go to the root directory of this project and type `docker-compose build artifactory`. The Artifactory container build configuration is in the `artifactory` subdirectory. Note that if you are on a network that does require an SSL certificate for TLS communications, copy your organization's root SSL certificate to the artifactory subdirectory and name it `root.crt`. The Artifactory container build will automatically pick this up and use it to modify the openssl and Java keystore of the Artifactory container.

Artifactory OSS does not include a Docker repository capability. If you have a license, you can try using the pro version. To do so, edit the Dockerfile in `artifactory/Dockerfile` and replace the first line. Instead of `FROM docker.bintray.io/jfrog/artifactory-oss:5.3.2` it should then read `FROM docker.bintray.io/jfrog/artifactory-pro:5.3.2`

To launch, just type `docker-compose up artifactory`

### Configuration
The Artifactory container is pre-configured with a Maven repository. The Artifactory repository configuration is held in `artifactory/artifactory.config.import.xml` and is used in the build and startup of the Artifactory container. If you update the configuration for your own purposes, you can have Artifactory export this configuration. Copy that export to `artifactory/artifactory.config.import.xml` and rebuild the container.

The Artifactory container is also pre-configured with login credentials. The credentials are held in a file `artifactory/security.import.xml`. The initial username is `admin` and the password is `adminpassword`. If you decide to change this, Artifactory can export the security settings via the administration console. Copy those settings to `artifactory/security.import.xml` and rebuild the container.

### PostgreSQL

The Docker container can be started by typing `docker-compose up artifactory`. The Artifactory container uses PostgreSQL for a back-end server. There is a PostgreSQL container configured and the container starts up automatically when Artifactory starts.

### Persistence

Both the Artifactory container as well as the PostgreSQL container use a Docker data volume when created with Docker Compose. These data volumes are created the first time that the Artifactory and PostgreSQL containers start and persist beyond the termination of the containers. The next time the containers start up again, they are automatically reattached to the new containers. This provides a very simple way to give data persistence to Artifactory.

The data volume for Artifactory is mounted to `/var/opt/jfrog/artifactory` in the container. [More info here](https://www.jfrog.com/confluence/display/RTF/Installing+with+Docker#InstallingwithDocker-UsingHostDirectories).

### Accessing Artifactory UI

Once Artifactory has been launched, you should be able to point a browser at your Docker Machine's IP at port 8081 to access the web UI for Artifactory.

To find out the Docker Machine's IP, you can type `docker-machine ip <machine name>`:

```
docker-machine ip cdi-poc
192.168.99.100
```

You can then point your browser to http://192.168.99.100:8081

#### <a name="artifactory-tldr"></a>TL;DR

- `$ docker-machine up artifactory`
- `$ docker-machine ip cdi-poc`
- Point your browser to the IP that docker-machine provides in the previous command. http://<ip>:8081
- Log in using admin and adminpassword for username and password, respectively

## Docker Private Registry ([tl;dr](#registry-tldr))

Because the Artifactory OSS version does not provide a Docker repository, I've opted to include a container that does serve as a private Docker repository. The repository is created by the official Docker repository container.

### Creating a user/password for the registry

You only need to read this if you want to create the Docker private registry not using the default username and password of `testuser` and `testpassword` respectively.

Before creating the Docker registry container, you will need to create an auth password for Docker clients to use. To do so, the Docker registry image contains a script to create the configuration needed.

Here is an example of the command:

`docker run --entrypoint htpasswd registry:2 -Bbn testuser testpassword > auth/htpasswd`

Once this is done, the registry can be used given the username and password you've used in this command.

The initial htpasswd file is included with this repository. It is the outcome of the above command set.

### Creating server certificates

You only need to read this if you want to create the Docker registry using SSL certificates for TLS communication that are not just wildcard self-signed certificates.

The registry, to be a secured private registry, needs TLS certificates installed. To do so, you can generate them on the host machine and copy them to the mounted volume directory that is mounted to `/certs` in the Docker container. On your host, that directory is `./data/certs`. This directory exists in the root of the project. You would just replace the files that are already sitting there as part of the source of this project. Here is an example of how a wildcard certificate for the Docker registry may be created:

```
$ openssl genrsa -out data/certs/wildcard.key 2048
$ openssl req -nodes -newkey rsa:2048 -keyout data/certs/wildcard.key -out data/certs/wildcard.csr -subj "/C=US/ST=Wisconsin/L=Middleon/O=US Geological Survey/OU=WMA/CN=registry"
$ openssl x509 -req -days 9999 -in data/certs/wildcard.csr -signkey data/certs/wildcard.key  -out data/certs/wildcard.crt
```

Once the certificates are in the `data/certs` directory, the Docker container will pick them up and use them for TLS-enabled communication.

A default certificate using the above command set is included with this repository.

### Persistence

The Docker private registry persists data to a Docker volume. The Docker volume is created the first time you start the registry via Docker Compose. The registry data path is `/var/lib/registry`. The registry holds its login information at `/auth`. The certificates are held in `/certs`

If you wish to recreate the registry without any images, you can stop the container and remove all content in the `./data/registry` directory on your host. The context for this directory is this repository.

### Environments

The default environments file for the private Docker registry is located in `registry/compose.env`

You can change this to suit your needs or you can use your own local version of this file. Simply copy `registry/compose.yml` to `registry/compose_local.yml` and export a bash variable named `DOCKER_REGISTRY_ENV_LOCAL` to the value of `_local`. Then start the registry via Docker Compose:

```
$ export DOCKER_REGISTRY_ENV_LOCAL=_local
$ docker-compose up registry
```
### Caveats

#### Hosts

While the Docker registry container is known to other containers using the network address `registry` when launched via Docker Compose, the Docker client running on the Jenkins container will still access it via localhost. The reason for this is that the Docker client commands the Docker engine running in the VM. This is the same Docker engine that runs the Jenkins and the Registry containers. In the context of the Docker engine, that Docker registry container does run on the host of the VM, hence the Docker engine will connect to it via localhost. This is confusing at first when you see jobs configured in Jenkins to use localhost for the registry. In the real world, a Jenkins instance would end up calling the Docker engine through the TCP stack or HTTPS address of the remote machine.  

#### <a name="registry-tldr"></a>TL;DR

- `$ docker-compose up registry`

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
- `GITHUB_SSH_KEY_PASS`: If your SSH keys that you are importing to Jenkins have a
password associated with them, this is where it should be entered. Default is blank.
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
scroll to Maven Installations and verify there is an installer there. Then hit save.
