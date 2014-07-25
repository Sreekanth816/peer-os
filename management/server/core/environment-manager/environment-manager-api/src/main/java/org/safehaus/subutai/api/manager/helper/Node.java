package org.safehaus.subutai.api.manager.helper;


import org.safehaus.subutai.api.templateregistry.Template;
import org.safehaus.subutai.shared.protocol.Agent;


/**
 * Created by bahadyr on 7/24/14.
 */
public class Node {

    private Agent agent;
    private Template template;
    private String nodeGroupName;


    public Node( final Agent agent, final Template template, final String nodeGroupName ) {
        this.agent = agent;
        this.template = template;
        this.nodeGroupName = nodeGroupName;
    }


    public String getNodeGroupName() {
        return nodeGroupName;
    }


    public Agent getAgent() {
        return agent;
    }


    public Template getTemplate() {
        return template;
    }
}
