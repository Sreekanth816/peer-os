package io.subutai.hub.share.broker;


import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;

import com.google.common.base.Preconditions;


public class BrokerConnectionFactory
{

    private final int poolSize;
    private Map<BrokerTransport.Type, PooledConnectionFactory> pool = new HashMap<>();


    public BrokerConnectionFactory( int poolSize )
    {
        Preconditions.checkArgument( poolSize > 0 );
        Preconditions.checkArgument( poolSize < 5 );

        this.poolSize = poolSize;
    }


    public ConnectionFactory getConnectionFactory( BrokerTransport.Type type )
    {
        return pool.get( type );
    }


    public ConnectionFactory getConnectionFactory( BrokerTransport transport ) throws InvalidTransportException
    {
        Preconditions.checkNotNull( transport );

        BrokerTransport.Type type = BrokerTransport.getType( transport.getUri() );

        PooledConnectionFactory cf = this.pool.get( type );

        if ( cf != null )
        {
            return cf;
        }
        ActiveMQConnectionFactory amq = new ActiveMQConnectionFactory( transport.getUri() );
        cf = new PooledConnectionFactory();
        cf.setConnectionFactory( amq );
        cf.setMaxConnections( this.poolSize );
        this.pool.put( type, cf );

        return cf;
    }
}
