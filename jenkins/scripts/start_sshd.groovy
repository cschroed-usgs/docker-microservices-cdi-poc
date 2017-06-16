import jenkins.model.*

def env = System.getenv()
def instance = Jenkins.getInstance()
def sshd = instance.getDescriptor("org.jenkinsci.main.modules.sshd.SSHD")
def currentPort = sshd.getActualPort()
int expectedPort = env.JENKINS_SSH_PORT.toInteger()

if (currentPort != expectedPort) {
  sshd.setPort(expectedPort)
}
