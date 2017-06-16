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

secretText = new StringCredentialsImpl(
	CredentialsScope.GLOBAL,
	"GITHUB_ACCESS_TOKEN",
	"The access token to use with GitHub",
	Secret.fromString(env['GITHUB_ACCESS_TOKEN'])
)

store.addCredentials(domain, secretText)
