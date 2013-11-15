package org.safehaus.kiskis.mgmt.shared.communication;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.safehaus.kiskis.mgmt.shared.protocol.Command;
import org.safehaus.kiskis.mgmt.shared.protocol.CommandJson;
import org.safehaus.kiskis.mgmt.shared.protocol.Response;
import org.safehaus.kiskis.mgmt.shared.protocol.api.BrokerInterface;
import org.safehaus.kiskis.mgmt.shared.protocol.api.CommandTransportInterface;
import javax.jms.*;

public class CommandTransport implements CommandTransportInterface {

    private BrokerService broker;
    private BrokerInterface brokerService;
    private int amqPort;
    private String amqHost;
    private String amqBindAddress;
    private String amqServiceQueue;
    private String amqBrokerCertificateName;
    private String amqBrokerTrustStoreName;
    private String amqBrokerCertificatePwd;
    private String amqBrokerTrustStorePwd;
    Session listenerSession;

    public void setAmqPort(int amqPort) {
        this.amqPort = amqPort;
    }

    public void setAmqHost(String amqHost) {
        this.amqHost = amqHost;
    }

    public void setAmqBindAddress(String amqBindAddress) {
        this.amqBindAddress = amqBindAddress;
    }

    public void setAmqServiceQueue(String amqServiceQueue) {
        this.amqServiceQueue = amqServiceQueue;
    }

    public void setAmqBrokerCertificateName(String amqBrokerCertificateName) {
        this.amqBrokerCertificateName = amqBrokerCertificateName;
    }

    public void setAmqBrokerTrustStoreName(String amqBrokerTrustStoreName) {
        this.amqBrokerTrustStoreName = amqBrokerTrustStoreName;
    }

    public void setAmqBrokerCertificatePwd(String amqBrokerCertificatePwd) {
        this.amqBrokerCertificatePwd = amqBrokerCertificatePwd;
    }

    public void setAmqBrokerTrustStorePwd(String amqBrokerTrustStorePwd) {
        this.amqBrokerTrustStorePwd = amqBrokerTrustStorePwd;
    }

    @Override
    public Response sendCommand(Command command) {
        thread(new CommandProducer(command, amqHost, amqPort), false);
        return null;
    }

    public static void thread(Runnable runnable, boolean daemon) {
        Thread brokerThread = new Thread(runnable);
        brokerThread.setDaemon(daemon);
        brokerThread.start();
    }

    public static class CommandProducer implements Runnable {

        String host;
        int port;
        Command command;

        public CommandProducer(Command command, String host, int port) {
            this.command = command;
            this.port = port;
            this.host = host;
        }

        public void run() {
            try {
                ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("ssl://" + host + ":" + port);
                Connection connection = connectionFactory.createConnection();
                connection.start();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createQueue(command.getCommand().getUuid());
                javax.jms.MessageProducer producer = session.createProducer(destination);
                producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
                String json = CommandJson.getJson(command);
                TextMessage message = session.createTextMessage(json);
                producer.send(message);
                session.close();
                connection.close();
            } catch (JMSException e) {
                System.out.println("Caught: " + e);
            }
        }
    }

    public void setBrokerService(BrokerInterface brokerService) {
        this.brokerService = brokerService;
        if (brokerService != null) {
            System.out.println("......." + this.getClass().getName() + " BrokerInterface initialized");
        } else {
            System.out.println("......." + this.getClass().getName() + " BrokerInterface not initialized");
        }

    }

    public void init() {

        try {
            listenerSession.close();
        } catch (Exception e) {
        }
        try {

            broker.stop();
            broker.waitUntilStopped();
        } catch (Exception e) {
        }

        try {
            System.setProperty("javax.net.ssl.keyStore", System.getProperty("karaf.base") + "/" + this.amqBrokerCertificateName);
            System.setProperty("javax.net.ssl.keyStorePassword", this.amqBrokerCertificatePwd);
            System.setProperty("javax.net.ssl.trustStore", System.getProperty("karaf.base") + "/" + this.amqBrokerTrustStoreName);
            System.setProperty("javax.net.ssl.trustStorePassword", this.amqBrokerTrustStorePwd);

            broker = new BrokerService();
            broker.setPersistent(true);
            broker.setUseJmx(false);
            broker.addConnector("ssl://" + this.amqBindAddress + ":" + this.amqPort);
            broker.start();
            broker.waitUntilStarted();
            setupListener();
            System.out.println("ActiveMQ started...");
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }

    }

    public void destroy() {
        try {
            broker.stop();
            System.out.println("ActiveMQ stopped...");
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    private void setupListener() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("ssl://" + this.amqHost + ":" + this.amqPort);
        Connection connection;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            listenerSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination adminQueue = listenerSession.createQueue(this.amqServiceQueue);

            MessageConsumer consumer = listenerSession.createConsumer(adminQueue);
            consumer.setMessageListener(new CommunicationMessageListener(listenerSession, brokerService));
        } catch (JMSException ex) {
            System.out.println(ex.toString());
        }
    }
}
