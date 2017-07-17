import jenkins.security.s2m.*
import jenkins.model.*

// https://wiki.jenkins.io/display/JENKINS/Slave+To+Master+Access+Control#SlaveToMasterAccessControl-Enabletheaccesscontrolmechanism

final jenkins = Jenkins.getInstance()
jenkins.injector.getInstance(AdminWhitelistRule.class).setMasterKillSwitch(false);
jenkins.save();
