package io.subutai.core.broker.api;


/**
 * Allows to subscribe to messages from topics and send messages to them
 */
public interface Broker
{

    /**
     * Sends text message to a given topic
     *
     * @param topic - name of target topic
     * @param message -text message to send
     *
     * @throws BrokerException - thrown in case something went wrong
     */
    public void sendTextMessage( String topic, String message ) throws BrokerException;

    /**
     * Sends byte message to a given topic
     *
     * @param topic - name of target topic
     * @param message - byte message to send
     *
     * @throws BrokerException - thrown in case something went wrong
     */
    public void sendByteMessage( String topic, byte[] message ) throws BrokerException;
}