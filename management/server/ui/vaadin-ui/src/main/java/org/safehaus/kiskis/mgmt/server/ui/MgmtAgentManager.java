package org.safehaus.kiskis.mgmt.server.ui;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import org.safehaus.kiskis.mgmt.shared.protocol.Agent;
import org.safehaus.kiskis.mgmt.shared.protocol.api.AgentManagerInterface;
import org.safehaus.kiskis.mgmt.shared.protocol.api.ui.AgentListener;
import org.safehaus.kiskis.mgmt.shared.protocol.settings.Common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA. User: daralbaev Date: 11/8/13 Time: 7:24 PM
 */
@SuppressWarnings("serial")

public final class MgmtAgentManager extends VerticalLayout implements
        Property.ValueChangeListener, AgentListener {

    private final AgentManagerInterface agentManagerInterface;
    private List<Agent> registeredAgents;
    private final Tree tree;
    private HierarchicalContainer container;
    private static final Logger LOG = Logger.getLogger(MgmtAgentManager.class.getName());

    public MgmtAgentManager(AgentManagerInterface agentManagerService) {

        this.agentManagerInterface = agentManagerService;
        setSizeFull();
        setMargin(true);
        tree = new Tree("List of nodes", getNodeContainer());
        tree.setMultiSelect(true);
        tree.setImmediate(true);
        tree.addListener(this);
        addComponent(getRefreshButton());
        addComponent(tree);
        agentManagerService.addListener(this);
    }

    /*
     * Shows a notification when a selection is made.
     */
    @Override
    public void valueChange(Property.ValueChangeEvent event) {
        if (event.getProperty().getValue() instanceof Set) {
            Tree t = (Tree) event.getProperty();

            List<Agent> selectedList = new ArrayList<Agent>();

            for (Object o : (Set<Object>) t.getValue()) {
                if (tree.getItem(o).getItemProperty("value").getValue() != null) {
                    selectedList.add((Agent) tree.getItem(o).getItemProperty("value").getValue());
                }
            }

            MgmtApplication.setSelectedAgents(selectedList);
            getWindow().showNotification(
                    "Selected agents",
                    selectedList.toString(),
                    Window.Notification.TYPE_TRAY_NOTIFICATION);
        }
    }

    private Button getRefreshButton() {
        Button button = new Button("Refresh");
        button.setDescription("Gets LXC agents from Cassandra");
        button.addListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {

                tree.setValue(null);
                container.removeAllItems();
                registeredAgents.clear();
                MgmtApplication.getSelectedAgents().clear();
                refreshAgents(agentManagerInterface.getRegisteredAgents());
            }
        });
        return button;
    }

    @Override
    public void onAgent(List<Agent> freshAgents) {
        refreshAgents(freshAgents);
    }

    public HierarchicalContainer getNodeContainer() {
        registeredAgents = new ArrayList<Agent>();
        container = new HierarchicalContainer();
        container.addContainerProperty("value", Agent.class, null);
        container.addContainerProperty("icon", ThemeResource.class,
                new ThemeResource("icons/16/folder.png"));
        refreshAgents(agentManagerInterface.getRegisteredAgents());
        return container;
    }

    private void refreshAgents(List<Agent> allFreshAgents) {
        if (allFreshAgents != null) {
            try {
                // clear all agents
                List<Agent> setToRemove = new ArrayList<Agent>();
                setToRemove.addAll(registeredAgents);
                setToRemove.removeAll(allFreshAgents);
                refreshNodeContainer(setToRemove, true, null);

                //grab parents
                List<Agent> parents = new ArrayList<Agent>();
                for (Agent agent : allFreshAgents) {
                    if (!agent.isIsLXC()) {
                        parents.add(agent);
                    }
                }
                refreshNodeContainer(parents, false, null);

                //grab children
                List<Agent> childAgents = new ArrayList<Agent>();
                if (!parents.isEmpty()) {
                    for (Agent parent : parents) {
                        List<Agent> children = new ArrayList<Agent>();
                        for (Agent possibleChild : allFreshAgents) {
                            if (possibleChild.isIsLXC()) {
                                if (possibleChild.getHostname() != null && possibleChild.getHostname().startsWith(parent.getHostname() + Common.PARENT_CHILD_LXC_SEPARATOR)) {
                                    children.add(possibleChild);
                                }

                                childAgents.add(possibleChild);
                            }
                        }
                        refreshNodeContainer(children, false, parent);
                    }
                } else {
                    for (Agent possibleChild : allFreshAgents) {
                        if (possibleChild.isIsLXC()) {
                            childAgents.add(possibleChild);
                        }
                    }
                }

                //process orphan agents
                List<Agent> orphanAgents = new ArrayList<Agent>();
                for (Agent possibleOrphan : childAgents) {
                    if (possibleOrphan.getParentHostName() == null
                            || Common.UNKNOWN_LXC_PARENT_NAME.equalsIgnoreCase(possibleOrphan.getParentHostName())) {
                        orphanAgents.add(possibleOrphan);
                    } else {
                        boolean parentFound = false;
                        for (Agent parent : parents) {
                            if (possibleOrphan.getParentHostName().equals(parent.getHostname())) {
                                parentFound = true;
                                break;
                            }
                        }
                        if (!parentFound) {
                            orphanAgents.add(possibleOrphan);
                        }
                    }
                }
                container.removeItemRecursively(Common.UNKNOWN_LXC_PARENT_NAME);
                if (!orphanAgents.isEmpty()) {
                    container.addItem(Common.UNKNOWN_LXC_PARENT_NAME);
                    container.setChildrenAllowed(Common.UNKNOWN_LXC_PARENT_NAME, true);
                    for (Agent orphanAgent : orphanAgents) {
                        Item item = container.addItem(orphanAgent.getHostname());
                        if (item != null) {
                            item.getItemProperty("value").setValue(orphanAgent);
                            container.setParent(orphanAgent.getHostname(), Common.UNKNOWN_LXC_PARENT_NAME);
                            container.setChildrenAllowed(orphanAgent.getHostname(), false);
                        }
                    }
                }

                registeredAgents.clear();
                registeredAgents.addAll(allFreshAgents);

                //deselect agents in tree that are not in allFreshAgents
                if (tree != null) {
                    Set<String> selectedHostnames = (Set<String>) tree.getValue();
                    if (selectedHostnames != null && !selectedHostnames.isEmpty()) {
                        Set<String> actualSelectedHostnames = new HashSet<String>();
                        for (String selectedHostname : selectedHostnames) {
                            for (Agent agent : allFreshAgents) {
                                if (agent.getHostname().equalsIgnoreCase(selectedHostname)) {
                                    Object value
                                            = container.getItem(selectedHostname).getItemProperty("value").getValue();
                                    if (value != null && value instanceof Agent
                                            && ((Agent) value).getUuid().compareTo(agent.getUuid()) == 0) {
                                        actualSelectedHostnames.add(selectedHostname);
                                    }
                                    break;
                                }
                            }
                        }
//                        System.out.println("SeTING VALUES " + actualSelectedHostnames);
                        tree.setValue(actualSelectedHostnames);
                    }
                }

                //process bizzare situations
                List<List<Agent>> allSessionsSelectedAgents = MgmtApplication.getAllSessionsSelectedAgents();
                if (!allSessionsSelectedAgents.isEmpty()) {
                    //iterate all sessions' selected agents
                    for (List<Agent> singleSessionsSelectedAgents : allSessionsSelectedAgents) {
                        if (!singleSessionsSelectedAgents.isEmpty()) {
                            //replace agents whose hostnames are the same but uuids changed somehow
                            for (ListIterator<Agent> iter = singleSessionsSelectedAgents.listIterator(); iter.hasNext();) {
                                Agent selectedAgent = iter.next();
                                for (Agent freshAgent : allFreshAgents) {
                                    if (selectedAgent.getHostname().equalsIgnoreCase(freshAgent.getHostname())
                                            && selectedAgent.getUuid().compareTo(freshAgent.getUuid()) != 0) {
                                        iter.set(freshAgent);
                                        break;
                                    }
                                }
                            }
                            //keep only fresh agents
                            singleSessionsSelectedAgents.retainAll(allFreshAgents);
                        }
                    }
                }
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Error in refreshAgents", ex);
            }
        }
    }

    public void refreshNodeContainer(List<Agent> agents, boolean delete, Agent parent) {
        try {
            if (delete) {
                for (Agent agent : agents) {
                    container.removeItemRecursively(agent.getHostname());
                }
            } else {
                for (Agent agent : agents) {
                    if (container.getItem(agent.getHostname()) == null) {
                        if (agent.getUuid() != null && agent.getHostname() != null) {
                            Item item = container.addItem(agent.getHostname());
                            if (item != null) {
                                item.getItemProperty("value").setValue(agent);
                                if (!agent.isIsLXC()) {
                                    container.setChildrenAllowed(agent.getHostname(), true);
                                } else {
                                    item.getItemProperty("icon").setValue(new ThemeResource("icons/16/document.png"));
                                    container.setParent(agent.getHostname(), parent.getHostname());
                                    container.setChildrenAllowed(agent.getHostname(), false);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error in refreshNodeContainer", ex);
        }
    }
}
