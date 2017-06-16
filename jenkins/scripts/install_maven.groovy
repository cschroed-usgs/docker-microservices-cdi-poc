import jenkins.model.*

println "Adding an auto installer for Maven 3.3.9"

def mavenPluginExtension = Jenkins.instance.getExtensionList(hudson.tasks.Maven.DescriptorImpl.class)[0]
def env = System.getenv()
def asList = (mavenPluginExtension.installations as List)
asList.add(
	new hudson.tasks.Maven.MavenInstallation(
		'M3 ' + env['MAVEN_INSTALLATION_VERSION'],
		null,
		[
			new hudson.tools.InstallSourceProperty([new hudson.tasks.Maven.MavenInstaller(env['MAVEN_INSTALLATION_VERSION'])])
		]
	)
)

mavenPluginExtension.installations = asList

mavenPluginExtension.save()
