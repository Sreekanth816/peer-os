package io.subutai.hub.share.broker;


import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;


@JsonAutoDetect( fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE )
public class BrokerSettings
{
    @JsonProperty( value = "name" )
    private String name;

    @JsonProperty( value = "transports" )
    private Map<BrokerTransport.Type, BrokerTransport> transports;


    protected BrokerSettings( final String name, final Map<BrokerTransport.Type, BrokerTransport> transports )
    {
        this.name = name;
        this.transports = transports;
    }


    public BrokerSettings()
    {
    }


    public String getName()
    {
        return name;
    }


    public void addTransport( final BrokerTransport transport ) throws InvalidTransportException
    {
        Preconditions.checkNotNull( transport );
        this.transports.put( getType( transport ), transport );
    }


    private BrokerTransport.Type getType( final BrokerTransport transport ) throws InvalidTransportException
    {
        return BrokerTransport.Type.valueOf( transport.getUri() );
    }


    protected Collection<BrokerTransport> getTransports()
    {
        return this.transports.values();
    }

    //
    //    public BrokerTransport getRandomTransportsByType( BrokerTransport.Type type ) throws
    // TransportNotFoundException
    //    {
    //
    //        final List<BrokerTransport> transports = getTransportsByType( type );
    //
    //        if ( transports.size() == 0 )
    //        {
    //            throw new TransportNotFoundException();
    //        }
    //        return transports.get( new Random().nextInt( transports.size() ) );
    //    }


    public BrokerTransport getTransportByType( final BrokerTransport.Type type )
    {
        return this.transports.get( type );
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

        final BrokerSettings that = ( BrokerSettings ) o;

        return new EqualsBuilder().append( name, that.name ).append( transports, that.transports ).isEquals();
    }


    @Override
    public int hashCode()
    {
        return new HashCodeBuilder( 17, 37 ).append( name ).append( transports ).toHashCode();
    }
}
