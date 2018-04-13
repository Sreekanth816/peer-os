package io.subutai.hub.share.broker;


import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;


public class BrokerSettingsTest
{
    public BrokerTransport tcpTransport;
    public BrokerTransport wsTransport;

    BrokerSettings brokerSettings;

    private ObjectMapper objectMapper;


    @Before
    public void setup() throws InvalidTransportException
    {
        this.objectMapper = new ObjectMapper();

        this.tcpTransport = new BrokerTransport( URI.create( "tcp://192.168.1.1" ) );
        this.wsTransport = new BrokerTransport( URI.create( "ws://192.168.1.2" ) );

        final Map<BrokerTransport.Type, BrokerTransport> transports = new HashMap<>();

        transports.put( tcpTransport.getType( tcpTransport.getUri() ), tcpTransport );
        transports.put( wsTransport.getType( wsTransport.getUri() ), wsTransport );

        this.brokerSettings = new BrokerSettings( "broker1", transports );
    }


    @Test
    public void testJsonSerializationAndDeserialization() throws IOException
    {
        final String json = objectMapper.writeValueAsString( brokerSettings );

        System.out.println( json );
        final BrokerSettings restoredObject = objectMapper.readValue( json, BrokerSettings.class );

        assertEquals( brokerSettings, restoredObject );
    }
}