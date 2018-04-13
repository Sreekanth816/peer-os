package io.subutai.core.environment.metadata.impl;


import java.util.concurrent.TimeUnit;

import javax.jms.ConnectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.subutai.common.command.CommandException;
import io.subutai.common.command.RequestBuilder;
import io.subutai.common.environment.Environment;
import io.subutai.common.peer.ContainerHost;
import io.subutai.common.peer.HostNotFoundException;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.environment.metadata.api.EnvironmentMetadataManager;
import io.subutai.core.hubmanager.api.HubManager;
import io.subutai.core.identity.api.IdentityManager;
import io.subutai.core.identity.api.exception.TokenCreateException;
import io.subutai.core.peer.api.PeerManager;
import io.subutai.hub.share.Utils;
import io.subutai.hub.share.broker.BrokerConnectionFactory;
import io.subutai.hub.share.broker.BrokerSettings;
import io.subutai.hub.share.broker.BrokerTransport;
import io.subutai.hub.share.broker.InvalidTransportException;
import io.subutai.hub.share.broker.TransportNotFoundException;
import io.subutai.hub.share.dto.environment.EnvironmentInfoDto;
import io.subutai.hub.share.event.Event;
import io.subutai.hub.share.json.JsonUtil;


/**
 * Environment metadata manager
 **/
public class EnvironmentMetadataManagerImpl implements EnvironmentMetadataManager
{
    private static final Logger LOG = LoggerFactory.getLogger( EnvironmentMetadataManagerImpl.class );
    private final IdentityManager identityManager;
    private Cache<BrokerTransport.Type, BrokerTransport> brokerTransportRequests;
    private PeerManager peerManager;
    private EnvironmentManager environmentManager;
    private HubManager hubManager;
    private BrokerConnectionFactory brokerConnectionFactory = new BrokerConnectionFactory( 1 );
    private BrokerSettings brokerSettings = new BrokerSettings();


    public EnvironmentMetadataManagerImpl( PeerManager peerManager, EnvironmentManager environmentManager,
                                           IdentityManager identityManager, HubManager hubManager )
    {
        this.peerManager = peerManager;
        this.environmentManager = environmentManager;
        this.identityManager = identityManager;
        this.hubManager = hubManager;
        this.brokerTransportRequests = CacheBuilder.newBuilder().expireAfterWrite( 1, TimeUnit.MINUTES ).build();
    }


    @Override
    public void init()
    {
    }


    @Override
    public void dispose()
    {
    }


    @Override
    public void issueToken( String containerIp ) throws TokenCreateException
    {
        try
        {
            ContainerHost container = peerManager.getLocalPeer().getContainerHostByIp( containerIp );
            String environmentId = container.getEnvironmentId().getId();
            String containerId = container.getContainerId().getId();
            String peerId = container.getPeerId();
            String origin = Utils.buildSubutaiOrigin( environmentId, peerId, containerId );
            final String token = identityManager.issueJWTToken( origin );

            placeTokenIntoContainer( container, token );
        }
        catch ( HostNotFoundException | CommandException e )
        {
            throw new TokenCreateException( e.getMessage() );
        }
    }


    @Override
    public EnvironmentInfoDto getEnvironmentInfoDto( final String environmentId )
    {
        Environment environment = environmentManager.getEnvironment( environmentId );
        final EnvironmentInfoDto result = new EnvironmentInfoDto();
        result.setName( environment.getName() );
        result.setSubnetCidr( environment.getSubnetCidr() );
        return result;
    }


    @Override
    public void pushEvent( final Event event )
    {
        try
        {
            String jsonEvent = JsonUtil.toJson( event );
            LOG.debug( "Event received: {} {}", event, jsonEvent );
            LOG.debug( "OS: {}", event.getCustomMetaByKey( "OS" ) );
            String destination = "events." + event.getOrigin().getId();
            ConnectionFactory cf = getConnectionFactory( BrokerTransport.Type.TCP );
            thread( new EventProducer( cf, destination, jsonEvent ), true );
        }
        catch ( JsonProcessingException | TransportNotFoundException | InvalidTransportException e )
        {
            LOG.error( e.getMessage(), e );
        }
    }


    private ConnectionFactory getConnectionFactory( BrokerTransport.Type type )
            throws TransportNotFoundException, InvalidTransportException
    {
        // trying to get the previous connection factory
        ConnectionFactory cf = brokerConnectionFactory.getConnectionFactory( type );
        if ( cf == null )
        {
            // seems it is first time to obtain connection factory
            BrokerTransport transport = requestBrokerTransport( type );
            if ( transport == null )
            {
                throw new TransportNotFoundException( "Transport not found for type: " + type );
            }
            cf = brokerConnectionFactory.getConnectionFactory( transport );
        }
        return cf;
    }


    private void thread( Runnable runnable, boolean daemon )
    {
        Thread brokerThread = new Thread( runnable );
        brokerThread.setDaemon( daemon );
        brokerThread.start();
    }


    private void placeTokenIntoContainer( ContainerHost containerHost, String token ) throws CommandException
    {
        containerHost.executeAsync( new RequestBuilder(
                String.format( "mkdir -p /etc/subutai/ ; echo '%s' > /etc/subutai/jwttoken", token ) ) );
    }


    private BrokerTransport requestBrokerTransport( final BrokerTransport.Type type )
    {
        BrokerTransport transport = this.brokerTransportRequests.getIfPresent( type );

        if ( transport == null )
        {
            transport = hubManager.getBrokerTransport( type );
        }
        return transport;
    }
}

