import jenkins.model.*
import org.jfrog.*
import org.jfrog.hudson.*
import org.jfrog.hudson.util.Credentials;

// https://github.com/hayderimran7/useful-jenkins-groovy-init-scripts/blob/master/sonar-artifactory-settings.groovy

def env = System.getenv()

def inst = Jenkins.getInstance()

def desc = inst.getDescriptor("org.jfrog.hudson.ArtifactoryBuilder")
def artifactoryServerId = env['ARTIFACTORY_ID'] ?: 'artifactory'
def artifactoryAddress = env['ARTIFACTORY_ADDRESS'] ?: 'http://registry:8081/artifactory'
def artifactoryDeployerUsername = env['ARTIFACTORY_DEPLOYER_USERNAME'] ?: 'artifactory'
def artifactoryDeployerPassword = env['ARTIFACTORY_DEPLOYER_PASSWORD'] ?: 'artifactory'
def artifactoryResolverUsername = env['ARTIFACTORY_RESOLVER_USERNAME'] ?: ''
def artifactoryResolverPassword = env['ARTIFACTORY_RESOLVER_PASSWORD'] ?: ''
def deployerCredentials = new Credentials(artifactoryDeployerUsername, artifactoryDeployerPassword)
def resolverCredentials = new Credentials(artifactoryResolverUsername, artifactoryResolverPassword)

def sinst = [new ArtifactoryServer(
  artifactoryServerId,
  artifactoryAddress,
  new CredentialsConfig(deployerCredentials, 'artifactory-deployer-credentials', false),
  new CredentialsConfig(resolverCredentials, 'artifactory-resolver-credentials', false),
  300,
  false,
	3 )
]

desc.setArtifactoryServers(sinst)

desc.save()
