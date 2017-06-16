import jenkins.model.*

def inst = Jenkins.getInstance()
def env = System.getenv()
def desc = inst.getDescriptor("hudson.plugins.git.GitSCM")

desc.setGlobalConfigName(env['GITHUB_CONFIG_NAME'])
desc.setGlobalConfigEmail(env['GITHUB_CONFIG_EMAIL'])

desc.save()
