package io.subutai.hub.share.broker;


import java.net.URI;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;


public class BrokerTransport
{
    public enum Type
    {
        TCP( "tcp" ), WS( "ws" ), MQTT( "mqtt" );
        private final String scheme;


        Type( final String scheme )
        {
            this.scheme = scheme;
        }


        public String getScheme()
        {
            return scheme;
        }


        public static Type valueOf( URI uri ) throws InvalidTransportException
        {
            try
            {
                return Type.valueOf( uri.getScheme().toUpperCase() );
            }
            catch ( IllegalArgumentException e )
            {
                throw new InvalidTransportException( uri.toASCIIString() );
            }
        }
    }


    @JsonProperty( value = "uri" )
    private URI uri;


    public BrokerTransport( final URI uri ) throws InvalidTransportException
    {
        Preconditions.checkNotNull( uri );

        getType( uri );

        this.uri = uri;
    }


    private BrokerTransport()
    {
    }


    public URI getUri()
    {
        return uri;
    }


    public static Type getType( URI uri ) throws InvalidTransportException
    {
        return Type.valueOf( uri );
    }


    @Override
    public boolean equals( final Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }
        final BrokerTransport that = ( BrokerTransport ) o;
        return Objects.equals( uri, that.uri );
    }


    @Override
    public int hashCode()
    {

        return Objects.hash( uri );
    }
}
