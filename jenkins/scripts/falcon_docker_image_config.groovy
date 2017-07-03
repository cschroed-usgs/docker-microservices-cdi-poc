import hudson.model.AbstractItem
import javax.xml.transform.stream.StreamSource
import jenkins.model.Jenkins

final jenkins = Jenkins.getInstance()

final itemName = 'Falcon Docker Image'
final configXml = new FileInputStream('/falcon_docker_image.xml')
final item = jenkins.getItemByFullName(itemName, AbstractItem.class)

if (item != null) {
  item.updateByXml(new StreamSource(configXml))
} else {
  jenkins.createProjectFromXML(itemName, configXml)
}
