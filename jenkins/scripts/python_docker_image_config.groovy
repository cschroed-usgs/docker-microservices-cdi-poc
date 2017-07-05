import hudson.model.AbstractItem
import javax.xml.transform.stream.StreamSource
import jenkins.model.Jenkins

final jenkins = Jenkins.getInstance()

final itemName = 'python_docker_image'
final configXml = new FileInputStream('/python_docker_image.xml')
final item = jenkins.getItemByFullName(itemName, AbstractItem.class)

if (item != null) {
  item.updateByXml(new StreamSource(configXml))
} else {
  jenkins.createProjectFromXML(itemName, configXml)
}
