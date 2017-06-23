import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.jenkins.plugins.sshcredentials.impl.*
import org.jenkinsci.plugins.plaincredentials.*
import org.jenkinsci.plugins.plaincredentials.impl.*
import hudson.util.Secret
import hudson.plugins.sshslaves.*
import org.apache.commons.fileupload.*
import org.apache.commons.fileupload.disk.*
import java.nio.file.Files

domain = Domain.global()
store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
def env = System.getenv()

gitHubSshCredentials = new BasicSSHUserPrivateKey(
  CredentialsScope.GLOBAL,
  "GITHUB_SSH_CREDENTIALS",
  env['GITHUB_CONFIG_NAME'],
  new BasicSSHUserPrivateKey.FileOnMasterPrivateKeySource("/usr/share/jenkins/ref/.ssh/id_rsa"),
  env['GITHUB_SSH_KEY_PASS'],
  "SSH Credentials For Github User"
)



store.addCredentials(domain, gitHubSshCredentials)
