/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.kiskis.mgmt.impl.mahout;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.safehaus.kiskis.mgmt.api.agentmanager.AgentManager;
import org.safehaus.kiskis.mgmt.api.dbmanager.DbManager;
import org.safehaus.kiskis.mgmt.api.mahout.Config;
import org.safehaus.kiskis.mgmt.api.mahout.Mahout;
import org.safehaus.kiskis.mgmt.api.taskrunner.Result;
import org.safehaus.kiskis.mgmt.api.taskrunner.Task;
import org.safehaus.kiskis.mgmt.api.taskrunner.TaskRunner;
import org.safehaus.kiskis.mgmt.api.taskrunner.TaskStatus;
import org.safehaus.kiskis.mgmt.api.tracker.ProductOperation;
import org.safehaus.kiskis.mgmt.api.tracker.Tracker;
import org.safehaus.kiskis.mgmt.shared.protocol.Agent;
import org.safehaus.kiskis.mgmt.shared.protocol.Util;

/**
 *
 * @author dilshat
 */
public class MahoutImpl implements Mahout {

    private TaskRunner taskRunner;
    private AgentManager agentManager;
    private DbManager dbManager;
    private Tracker tracker;
    private ExecutorService executor;

    public void init() {
        executor = Executors.newCachedThreadPool();
    }

    public void destroy() {
        executor.shutdown();
    }

    public void setDbManager(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    public void setTracker(Tracker tracker) {
        this.tracker = tracker;
    }

    public void setTaskRunner(TaskRunner taskRunner) {
        this.taskRunner = taskRunner;
    }

    public void setAgentManager(AgentManager agentManager) {
        this.agentManager = agentManager;
    }

    public UUID installCluster(final Config config) {
        final ProductOperation po
                = tracker.createProductOperation(Config.PRODUCT_KEY,
                        String.format("Installing cluster %s", config.getClusterName()));

        executor.execute(new Runnable() {

            public void run() {
                if (dbManager.getInfo(Config.PRODUCT_KEY, config.getClusterName(), Config.class) != null) {
                    po.addLogFailed(String.format("Cluster with name '%s' already exists\nInstallation aborted", config.getClusterName()));
                    return;
                }

                //check if node agent is connected
                for (Iterator<Agent> it = config.getNodes().iterator(); it.hasNext();) {
                    Agent node = it.next();
                    if (agentManager.getAgentByHostname(node.getHostname()) == null) {
                        po.addLog(String.format("Node %s is not connected. Omitting this node from installation", node.getHostname()));
                        it.remove();
                    }
                }

                if (config.getNodes().isEmpty()) {
                    po.addLogFailed("No nodes eligible for installation. Operation aborted");
                    return;
                }

                po.addLog("Checking prerequisites...");

                //check installed ksks packages
                Task checkInstalled = taskRunner.executeTask(Tasks.getCheckInstalledTask(config.getNodes()));

                if (!checkInstalled.isCompleted()) {
                    po.addLogFailed("Failed to check presence of installed ksks packages\nInstallation aborted");
                    return;
                }

                for (Iterator<Agent> it = config.getNodes().iterator(); it.hasNext();) {
                    Agent node = it.next();

                    Result result = checkInstalled.getResults().get(node.getUuid());

                    if (result.getStdOut().contains("ksks-mahout")) {
                        po.addLog(String.format("Node %s already has Mahout installed. Omitting this node from installation", node.getHostname()));
                        it.remove();
                    } else if (!result.getStdOut().contains("ksks-hadoop")) {
                        po.addLog(String.format("Node %s has no Hadoop installation. Omitting this node from installation", node.getHostname()));
                        it.remove();
                    }
                }

                if (config.getNodes().isEmpty()) {
                    po.addLogFailed("No nodes eligible for installation. Operation aborted");
                    return;
                }
                po.addLog("Updating db...");
                //save to db
                if (dbManager.saveInfo(Config.PRODUCT_KEY, config.getClusterName(), config)) {
                    po.addLog("Cluster info saved to DB\nInstalling Mahout...");
                    //install mahout            

                    Task installTask = taskRunner.executeTask(Tasks.getInstallTask(config.getNodes()));

                    if (installTask.getTaskStatus() == TaskStatus.SUCCESS) {
                        po.addLogDone("Installation succeeded\nDone");
                    } else {
                        String err = "";
                        for (Map.Entry<UUID, Result> res : installTask.getResults().entrySet()) {
                            if (!Util.isStringEmpty(res.getValue().getStdErr())) {
                                err = res.getValue().getStdErr();
                                break;
                            }
                        }
                        po.addLogFailed(String.format("Installation failed, %s", err));
                    }
                } else {
                    po.addLogFailed("Could not save cluster info to DB! Please see logs\nInstallation aborted");
                }
            }
        });

        return po.getId();
    }

    public UUID uninstallCluster(final String clusterName) {
        final ProductOperation po
                = tracker.createProductOperation(Config.PRODUCT_KEY,
                        String.format("Destroying cluster %s", clusterName));

        executor.execute(new Runnable() {

            public void run() {
                Config config = dbManager.getInfo(Config.PRODUCT_KEY, clusterName, Config.class);
                if (config == null) {
                    po.addLogFailed(String.format("Cluster with name %s does not exist\nOperation aborted", clusterName));
                    return;
                }

                po.addLog("Uninstalling Mahout...");

                Task uninstallTask = taskRunner.executeTask(Tasks.getUninstallTask(config.getNodes()));

                if (uninstallTask.isCompleted()) {
                    for (Map.Entry<UUID, Result> res : uninstallTask.getResults().entrySet()) {
                        Result result = res.getValue();
                        Agent agent = agentManager.getAgentByUUID(res.getKey());
                        if (result.getExitCode() != null && result.getExitCode() == 0) {
                            if (result.getStdOut().contains("Package ksks-mahout is not installed, so not removed")) {
                                po.addLog(String.format("Mahout is not installed, so not removed on node %s", result.getStdErr(),
                                        agent == null ? res.getKey() : agent.getHostname()));
                            } else {
                                po.addLog(String.format("Mahout is removed from node %s",
                                        agent == null ? res.getKey() : agent.getHostname()));
                            }
                        } else {
                            po.addLog(String.format("Error %s on node %s", result.getStdErr(),
                                    agent == null ? res.getKey() : agent.getHostname()));
                        }
                    }
                    po.addLog("Updating db...");
                    if (dbManager.deleteInfo(Config.PRODUCT_KEY, config.getClusterName())) {
                        po.addLogDone("Cluster info deleted from DB\nDone");
                    } else {
                        po.addLogFailed("Error while deleting cluster info from DB. Check logs.\nFailed");
                    }
                } else {
                    String err = "";
                    for (Map.Entry<UUID, Result> res : uninstallTask.getResults().entrySet()) {
                        if (!Util.isStringEmpty(res.getValue().getStdErr())) {
                            err = res.getValue().getStdErr();
                            break;
                        }
                    }
                    po.addLogFailed(String.format("Uninstallation failed, %s", err));
                }

            }
        });

        return po.getId();
    }

    public UUID destroyNode(final String clusterName, final String lxcHostname) {
        final ProductOperation po
                = tracker.createProductOperation(Config.PRODUCT_KEY,
                        String.format("Destroying %s in %s", lxcHostname, clusterName));

        executor.execute(new Runnable() {

            public void run() {
                final Config config = dbManager.getInfo(Config.PRODUCT_KEY, clusterName, Config.class);
                if (config == null) {
                    po.addLogFailed(String.format("Cluster with name %s does not exist\nOperation aborted", clusterName));
                    return;
                }

                Agent agent = agentManager.getAgentByHostname(lxcHostname);
                if (agent == null) {
                    po.addLogFailed(String.format("Agent with hostname %s is not connected\nOperation aborted", lxcHostname));
                    return;
                }

                if (config.getNodes().size() == 1) {
                    po.addLogFailed("This is the last node in the cluster. Please, destroy cluster instead\nOperation aborted");
                    return;
                }
                po.addLog("Uninstalling Mahout...");
                Task uninstallTask = taskRunner.executeTask(Tasks.getUninstallTask(Util.wrapAgentToSet(agent)));

                if (uninstallTask.isCompleted()) {
                    Map.Entry<UUID, Result> res = uninstallTask.getResults().entrySet().iterator().next();
                    Result result = res.getValue();
                    if (result.getExitCode() != null && result.getExitCode() == 0) {
                        if (result.getStdOut().contains("Package ksks-mahout is not installed, so not removed")) {
                            po.addLog(String.format("Mahout is not installed, so not removed on node %s", result.getStdErr(),
                                    agent.getHostname()));
                        } else {
                            po.addLog(String.format("Mahout is removed from node %s",
                                    agent.getHostname()));
                        }
                    } else {
                        po.addLog(String.format("Error %s on node %s", result.getStdErr(),
                                agent.getHostname()));
                    }

                    config.getNodes().remove(agent);
                    po.addLog("Updating db...");

                    if (dbManager.saveInfo(Config.PRODUCT_KEY, config.getClusterName(), config)) {
                        po.addLogDone("Cluster info update in DB\nDone");
                    } else {
                        po.addLogFailed("Error while updating cluster info in DB. Check logs.\nFailed");
                    }
                } else {
                    String err = "";
                    for (Map.Entry<UUID, Result> res : uninstallTask.getResults().entrySet()) {
                        if (!Util.isStringEmpty(res.getValue().getStdErr())) {
                            err = res.getValue().getStdErr();
                            break;
                        }
                    }
                    po.addLogFailed(String.format("Uninstallation failed, %s", err));
                }
            }
        });

        return po.getId();
    }

    public UUID addNode(final String clusterName, final String lxcHostname) {
        final ProductOperation po
                = tracker.createProductOperation(Config.PRODUCT_KEY,
                        String.format("Adding node to %s", clusterName));

        executor.execute(new Runnable() {

            public void run() {
                Config config = dbManager.getInfo(Config.PRODUCT_KEY, clusterName, Config.class);
                if (config == null) {
                    po.addLogFailed(String.format("Cluster with name %s does not exist\nOperation aborted", clusterName));
                    return;
                }

                //check if node agent is connected
                Agent agent = agentManager.getAgentByHostname(lxcHostname);
                if (agent == null) {
                    po.addLogFailed(String.format("Node %s is not connected\nOperation aborted", lxcHostname));
                    return;
                }

                //check installed ksks packages
                Task checkInstalled = taskRunner.executeTask(Tasks.getCheckInstalledTask(Util.wrapAgentToSet(agent)));

                if (!checkInstalled.isCompleted()) {
                    po.addLogFailed("Failed to check presence of installed ksks packages\nInstallation aborted");
                    return;
                }

                Result result = checkInstalled.getResults().get(agent.getUuid());

                if (result.getStdOut().contains("ksks-mahout")) {
                    po.addLogFailed(String.format("Node %s already has Mahout installed\nInstallation aborted", lxcHostname));
                    return;
                } else if (!result.getStdOut().contains("ksks-hadoop")) {
                    po.addLogFailed(String.format("Node %s has no Hadoop installation\nInstallation aborted", lxcHostname));
                    return;
                }

                config.getNodes().add(agent);
                po.addLog("Updating db...");
                //save to db
                if (dbManager.saveInfo(Config.PRODUCT_KEY, config.getClusterName(), config)) {
                    po.addLog("Cluster info updated in DB\nInstalling Mahout...");
                    //install mahout            

                    Task installTask = taskRunner.executeTask(Tasks.getInstallTask(config.getNodes()));

                    if (installTask.getTaskStatus() == TaskStatus.SUCCESS) {
                        po.addLogDone("Installation succeeded\nDone");
                    } else {
                        String err = "";
                        for (Map.Entry<UUID, Result> res : installTask.getResults().entrySet()) {
                            if (!Util.isStringEmpty(res.getValue().getStdErr())) {
                                err = res.getValue().getStdErr();
                                break;
                            }
                        }
                        po.addLogFailed(String.format("Installation failed, %s", err));
                    }
                } else {
                    po.addLogFailed("Could not update cluster info in DB! Please see logs\nInstallation aborted");
                }

            }
        });

        return po.getId();
    }

    public List<Config> getClusters() {
        return dbManager.getInfo(Config.PRODUCT_KEY, Config.class);
    }

}
