package io.subutai.core.peer.impl;


import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;

import io.subutai.common.command.CommandCallback;
import io.subutai.common.command.CommandException;
import io.subutai.common.command.CommandResult;
import io.subutai.common.command.CommandStatus;
import io.subutai.common.command.RequestBuilder;
import io.subutai.common.environment.CreateEnvironmentContainerGroupRequest;
import io.subutai.common.exception.HTTPException;
import io.subutai.common.host.ContainerHostState;
import io.subutai.common.host.HostId;
import io.subutai.common.host.HostInfo;
import io.subutai.common.host.HostInfoModel;
import io.subutai.common.host.HostInterfaces;
import io.subutai.common.metric.HostMetric;
import io.subutai.common.metric.ProcessResourceUsage;
import io.subutai.common.metric.ResourceHostMetrics;
import io.subutai.common.network.Gateway;
import io.subutai.common.network.Vni;
import io.subutai.common.peer.ContainerGateway;
import io.subutai.common.peer.ContainerHost;
import io.subutai.common.peer.ContainerId;
import io.subutai.common.peer.ContainersDestructionResult;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.common.peer.EnvironmentId;
import io.subutai.common.peer.Host;
import io.subutai.common.peer.PeerException;
import io.subutai.common.peer.PeerInfo;
import io.subutai.common.protocol.N2NConfig;
import io.subutai.common.protocol.Template;
import io.subutai.common.quota.CpuQuotaInfo;
import io.subutai.common.quota.DiskPartition;
import io.subutai.common.quota.DiskQuota;
import io.subutai.common.quota.QuotaInfo;
import io.subutai.common.quota.QuotaType;
import io.subutai.common.quota.RamQuota;
import io.subutai.common.security.PublicKeyContainer;
import io.subutai.common.settings.ChannelSettings;
import io.subutai.common.settings.Common;
import io.subutai.common.settings.SecuritySettings;
import io.subutai.common.util.CollectionUtil;
import io.subutai.common.util.JsonUtil;
import io.subutai.common.util.RestUtil;
import io.subutai.core.messenger.api.Message;
import io.subutai.core.messenger.api.MessageException;
import io.subutai.core.messenger.api.Messenger;
import io.subutai.core.peer.api.LocalPeer;
import io.subutai.core.peer.api.Payload;
import io.subutai.core.peer.api.RemotePeer;
import io.subutai.core.peer.impl.command.BlockingCommandCallback;
import io.subutai.core.peer.impl.command.CommandRequest;
import io.subutai.core.peer.impl.command.CommandResponseListener;
import io.subutai.core.peer.impl.command.CommandResultImpl;
import io.subutai.core.peer.impl.container.ContainersDestructionResultImpl;
import io.subutai.core.peer.impl.container.CreateEnvironmentContainerGroupResponse;
import io.subutai.core.peer.impl.container.DestroyEnvironmentContainerGroupRequest;
import io.subutai.core.peer.impl.container.DestroyEnvironmentContainerGroupResponse;
import io.subutai.core.peer.impl.request.MessageRequest;
import io.subutai.core.peer.impl.request.MessageResponse;
import io.subutai.core.peer.impl.request.MessageResponseListener;


/**
 * Remote Peer implementation
 */
@PermitAll
public class RemotePeerImpl implements RemotePeer
{
    private static final Logger LOG = LoggerFactory.getLogger( RemotePeerImpl.class );

    private final LocalPeer localPeer;
    protected final PeerInfo peerInfo;
    protected final Messenger messenger;
    private final CommandResponseListener commandResponseListener;
    private final MessageResponseListener messageResponseListener;
    protected RestUtil restUtil = new RestUtil();
    protected JsonUtil jsonUtil = new JsonUtil();
    private String baseUrl;
    Object provider;


    public RemotePeerImpl( LocalPeer localPeer, final PeerInfo peerInfo, final Messenger messenger,
                           CommandResponseListener commandResponseListener,
                           MessageResponseListener messageResponseListener, Object provider )
    {
        this.localPeer = localPeer;
        this.peerInfo = peerInfo;
        this.messenger = messenger;
        this.commandResponseListener = commandResponseListener;
        this.messageResponseListener = messageResponseListener;
        String url = "";

        String port = String.valueOf( peerInfo.getPort() );

        //switch case for formatting request url
        switch ( port )
        {
            case ChannelSettings.OPEN_PORT:
            case ChannelSettings.SPECIAL_PORT_X1:
                url = String.format( "http://%s:%s/rest/v1/peer", peerInfo.getIp(), peerInfo.getPort() );
                break;
            case ChannelSettings.SECURE_PORT_X1:
            case ChannelSettings.SECURE_PORT_X2:
            case ChannelSettings.SECURE_PORT_X3:
                url = String.format( "https://%s:%s/rest/v1/peer", peerInfo.getIp(), peerInfo.getPort() );
                break;
        }
        this.baseUrl = url;
        this.provider = provider;
    }


    protected String request( RestUtil.RequestType requestType, String path, String alias, Map<String, String> params,
                              Map<String, String> headers ) throws HTTPException
    {
        return restUtil.request( requestType,
                String.format( "%s/%s", baseUrl, path.startsWith( "/" ) ? path.substring( 1 ) : path ), alias, params,
                headers, provider );
    }


    protected String get( String path, String alias, Map<String, String> params, Map<String, String> headers )
            throws HTTPException
    {
        return request( RestUtil.RequestType.GET, path, alias, params, headers );
    }


    protected String post( String path, String alias, Map<String, String> params, Map<String, String> headers )
            throws HTTPException
    {

        return request( RestUtil.RequestType.POST, path, alias, params, headers );
    }


    protected String delete( String path, String alias, Map<String, String> params, Map<String, String> headers )
            throws HTTPException
    {

        return request( RestUtil.RequestType.DELETE, path, alias, params, headers );
    }


    @Override
    public String getId()
    {
        return peerInfo.getId();
    }


    //    @Override
    //    public String getRemoteId() throws PeerException
    //    {
    //        String path = "/id";
    //
    //        try
    //        {
    //            return get( path, SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS, null, null );
    //        }
    //        catch ( Exception e )
    //        {
    //            throw new PeerException( "Error obtaining peer id", e );
    //        }
    //    }


    @Override
    public PeerInfo check() throws PeerException
    {
        PeerInfo response = new PeerWebClient( peerInfo.getIp(), provider ).getInfo();
        if ( !peerInfo.getId().equals( response.getId() ) )
        {
            throw new PeerException( String.format(
                    "Remote peer check failed. Id of the remote peer %s changed. Please verify the remote peer.",
                    peerInfo.getId() ) );
        }

        return response;
    }


    @Override
    public boolean isOnline() throws PeerException
    {
        try
        {
            check();
            return true;
        }
        catch ( PeerException e )
        {
            LOG.error( e.getMessage(), e );
            return false;
        }
    }


    @Override
    public boolean isLocal()
    {
        return false;
    }


    @Override
    public String getName()
    {
        return peerInfo.getName();
    }


    @Override
    public String getOwnerId()
    {
        return peerInfo.getOwnerId();
    }


    @Override
    public PeerInfo getPeerInfo()
    {
        return peerInfo;
    }


    @Override
    public Template getTemplate( final String templateName ) throws PeerException
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( templateName ), "Invalid template name" );

        String path = "/template/get";

        Map<String, String> params = Maps.newHashMap();

        params.put( "templateName", templateName );


        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //*************************************************************


        try
        {
            String response = get( path, SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS, params, headers );

            return jsonUtil.from( response, Template.class );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Error obtaining template", e );
        }
    }


    //********** ENVIRONMENT SPECIFIC REST *************************************


    @RolesAllowed( "Environment-Management|A|Update" )
    @Override
    public void startContainer( final ContainerId containerId ) throws PeerException
    {
        Preconditions.checkNotNull( containerId, "Container id is null" );
        Preconditions.checkArgument( containerId.getPeerId().getId().equals( peerInfo.getId() ) );

        if ( containerId.getEnvironmentId() == null )
        {
            new PeerWebClient( peerInfo.getIp(), provider ).startContainer( containerId );
        }
        else
        {
            new EnvironmentWebClient( provider ).startContainer( peerInfo.getIp(), containerId );
        }
    }


    @RolesAllowed( "Environment-Management|A|Update" )
    @Override
    public void stopContainer( final ContainerId containerId ) throws PeerException
    {
        Preconditions.checkNotNull( containerId, "Container id is null" );
        Preconditions.checkArgument( containerId.getPeerId().getId().equals( peerInfo.getId() ) );

        if ( containerId.getEnvironmentId() == null )
        {
            new PeerWebClient( peerInfo.getIp(), provider ).stopContainer( containerId );
        }
        else
        {
            new EnvironmentWebClient( provider ).stopContainer( peerInfo.getIp(), containerId );
        }
    }


    @RolesAllowed( "Environment-Management|A|Delete" )
    @Override
    public void destroyContainer( final ContainerId containerId ) throws PeerException
    {

        if ( containerId.getEnvironmentId() == null )
        {
            new PeerWebClient( peerInfo.getIp(), provider ).destroyContainer( containerId );
        }
        else
        {
            new EnvironmentWebClient( provider ).destroyContainer( peerInfo.getIp(), containerId );
        }
    }


    @Override
    public void removeEnvironmentKeyPair( final EnvironmentId environmentId ) throws PeerException
    {
        new PeerWebClient( peerInfo.getIp(), provider ).removeEnvironmentKeyPair( environmentId );
    }


    @RolesAllowed( "Environment-Management|A|Delete" )
    @Override
    public void cleanupEnvironmentNetworkSettings( final EnvironmentId environmentId ) throws PeerException
    {
        Preconditions.checkNotNull( environmentId, "Invalid environment id" );

        new PeerWebClient( peerInfo.getIp(), provider ).cleanupEnvironmentNetworkSettings( environmentId );
    }


    @RolesAllowed( "Environment-Management|A|Delete" )
    @Override
    public boolean isConnected( final HostId hostId )
    {
        Preconditions.checkNotNull( hostId, "Host id is null" );

        if ( hostId instanceof ContainerId )
        {
            return ContainerHostState.RUNNING.equals( getContainerState( ( ContainerId ) hostId ) );
        }
        else
        {
            return false;
        }
    }


    @PermitAll
    @Override
    public ProcessResourceUsage getProcessResourceUsage( final ContainerId containerId, int pid ) throws PeerException
    {
        Preconditions.checkNotNull( containerId, "Container id is null" );
        Preconditions.checkArgument( pid > 0, "Process pid must be greater than 0" );

        if ( containerId.getEnvironmentId() == null )
        {
            return new PeerWebClient( peerInfo.getIp(), provider ).getProcessResourceUsage( containerId, pid );
        }
        else
        {
            return new EnvironmentWebClient( provider ).getProcessResourceUsage( peerInfo.getIp(), containerId, pid );
        }
    }


    @Override
    public ContainerHostState getContainerState( final ContainerId containerId )
    {
        Preconditions.checkNotNull( containerId, "Container id is null" );
        Preconditions.checkArgument( containerId.getPeerId().getId().equals( peerInfo.getId() ) );

        if ( containerId.getEnvironmentId() == null )
        {
            return new PeerWebClient( peerInfo.getIp(), provider ).getState( containerId );
        }
        else
        {
            return new EnvironmentWebClient( provider ).getState( peerInfo.getIp(), containerId );
        }
    }


    @Override
    public int getRamQuota( final ContainerHost containerHost ) throws PeerException
    {
        Preconditions.checkNotNull( containerHost, "Container host is null" );
        Preconditions.checkArgument( containerHost instanceof EnvironmentContainerHost );

        EnvironmentContainerHost host = ( EnvironmentContainerHost ) containerHost;
        String path = "/container/quota/ram";

        Map<String, String> params = Maps.newHashMap();
        params.put( "containerId", host.getId() );

        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //*************************************************************
        try
        {
            String alias = SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS;
            String response = get( path, alias, params, headers );

            return jsonUtil.from( response, Integer.class );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Error obtaining container ram quota", e );
        }
    }


    @Override
    public RamQuota getRamQuotaInfo( final ContainerHost containerHost ) throws PeerException
    {
        Preconditions.checkNotNull( containerHost, "Container host is null" );
        Preconditions.checkArgument( containerHost instanceof EnvironmentContainerHost );

        EnvironmentContainerHost host = ( EnvironmentContainerHost ) containerHost;
        String path = "/container/quota/ram/info";

        Map<String, String> params = Maps.newHashMap();
        params.put( "containerId", host.getId() );

        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //*************************************************************

        try
        {
            String alias = SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS;
            String response = get( path, alias, params, headers );

            return jsonUtil.from( response, RamQuota.class );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Error obtaining container ram quota", e );
        }
    }


    @RolesAllowed( "Environment-Management|A|Update" )
    @Override
    public void setRamQuota( final ContainerHost containerHost, final int ramInMb ) throws PeerException
    {
        Preconditions.checkNotNull( containerHost, "Container host is null" );
        Preconditions.checkArgument( containerHost instanceof EnvironmentContainerHost );

        EnvironmentContainerHost host = ( EnvironmentContainerHost ) containerHost;
        Preconditions.checkArgument( ramInMb > 0, "Ram quota value must be greater than 0" );

        String path = "/container/quota/ram";

        Map<String, String> params = Maps.newHashMap();
        params.put( "containerId", host.getId() );
        params.put( "ram", String.valueOf( ramInMb ) );

        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //*************************************************************
        try
        {
            String alias = SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS;
            post( path, alias, params, headers );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Error setting container ram quota", e );
        }
    }


    @Override
    public int getCpuQuota( final ContainerHost containerHost ) throws PeerException
    {
        Preconditions.checkNotNull( containerHost, "Container host is null" );
        Preconditions.checkArgument( containerHost instanceof EnvironmentContainerHost );

        EnvironmentContainerHost host = ( EnvironmentContainerHost ) containerHost;
        String path = "/container/quota/cpu";

        Map<String, String> params = Maps.newHashMap();
        params.put( "containerId", host.getId() );

        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //*************************************************************
        try
        {
            String alias = SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS;
            String response = get( path, alias, params, headers );

            return jsonUtil.from( response, Integer.class );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Error obtaining container cpu quota", e );
        }
    }


    @Override
    public CpuQuotaInfo getCpuQuotaInfo( final ContainerHost containerHost ) throws PeerException
    {
        Preconditions.checkNotNull( containerHost, "Container host is null" );
        Preconditions.checkArgument( containerHost instanceof EnvironmentContainerHost );

        EnvironmentContainerHost host = ( EnvironmentContainerHost ) containerHost;
        String path = "/container/quota/cpu/info";

        Map<String, String> params = Maps.newHashMap();
        params.put( "containerId", host.getId() );

        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //*************************************************************
        try
        {
            String alias = SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS;
            String response = get( path, alias, params, headers );

            return jsonUtil.from( response, CpuQuotaInfo.class );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Error obtaining container cpu quota", e );
        }
    }


    @RolesAllowed( "Environment-Management|A|Update" )
    @Override
    public void setCpuQuota( final ContainerHost containerHost, final int cpuPercent ) throws PeerException
    {
        Preconditions.checkNotNull( containerHost, "Container host is null" );
        Preconditions.checkArgument( containerHost instanceof EnvironmentContainerHost );

        EnvironmentContainerHost host = ( EnvironmentContainerHost ) containerHost;
        Preconditions.checkArgument( cpuPercent > 0, "Cpu quota value must be greater than 0" );

        String path = "/container/quota/cpu";

        Map<String, String> params = Maps.newHashMap();
        params.put( "containerId", host.getId() );
        params.put( "cpu", String.valueOf( cpuPercent ) );

        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //**************************************************************

        try
        {
            String alias = SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS;
            post( path, alias, params, headers );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Error setting container cpu quota", e );
        }
    }


    @Override
    public Set<Integer> getCpuSet( final ContainerHost containerHost ) throws PeerException
    {
        Preconditions.checkNotNull( containerHost, "Container host is null" );
        Preconditions.checkArgument( containerHost instanceof EnvironmentContainerHost );

        EnvironmentContainerHost host = ( EnvironmentContainerHost ) containerHost;
        String path = "/container/quota/cpuset";

        Map<String, String> params = Maps.newHashMap();
        params.put( "containerId", host.getId() );

        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //*************************************************************

        try
        {
            String alias = SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS;
            String response = get( path, alias, params, headers );

            return jsonUtil.from( response, new TypeToken<Set<Integer>>()
            {}.getType() );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Error obtaining container cpu set", e );
        }
    }


    @RolesAllowed( "Environment-Management|A|Update" )
    @Override
    public void setCpuSet( final ContainerHost containerHost, final Set<Integer> cpuSet ) throws PeerException
    {
        Preconditions.checkNotNull( containerHost, "Container host is null" );
        Preconditions.checkArgument( containerHost instanceof EnvironmentContainerHost );

        EnvironmentContainerHost host = ( EnvironmentContainerHost ) containerHost;
        Preconditions.checkArgument( !CollectionUtil.isCollectionEmpty( cpuSet ), "Empty cpu set" );

        String path = "/container/quota/cpuset";

        Map<String, String> params = Maps.newHashMap();
        params.put( "containerId", host.getId() );
        params.put( "cpuset", jsonUtil.to( cpuSet ) );

        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //*************************************************************

        try
        {
            String alias = SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS;
            post( path, alias, params, headers );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Error setting container cpu set", e );
        }
    }


    @Override
    public DiskQuota getDiskQuota( final ContainerHost containerHost, final DiskPartition diskPartition )
            throws PeerException
    {
        Preconditions.checkNotNull( containerHost, "Container host is null" );
        Preconditions.checkArgument( containerHost instanceof EnvironmentContainerHost );

        EnvironmentContainerHost host = ( EnvironmentContainerHost ) containerHost;
        Preconditions.checkNotNull( diskPartition, "Invalid disk partition" );

        String path = "/container/quota/disk";

        Map<String, String> params = Maps.newHashMap();

        params.put( "containerId", host.getId() );
        params.put( "diskPartition", jsonUtil.to( diskPartition ) );

        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //*************************************************************

        try
        {
            String alias = SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS;
            String response = get( path, alias, params, headers );

            return jsonUtil.from( response, new TypeToken<DiskQuota>()
            {}.getType() );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Error obtaining container disk quota", e );
        }
    }


    @RolesAllowed( "Environment-Management|A|Update" )
    @Override
    public void setDiskQuota( final ContainerHost containerHost, final DiskQuota diskQuota ) throws PeerException
    {
        Preconditions.checkNotNull( containerHost, "Container host is null" );
        Preconditions.checkArgument( containerHost instanceof EnvironmentContainerHost );

        EnvironmentContainerHost host = ( EnvironmentContainerHost ) containerHost;
        Preconditions.checkNotNull( diskQuota, "Invalid disk quota" );

        String path = "/container/quota/disk";

        Map<String, String> params = Maps.newHashMap();
        params.put( "containerId", host.getId() );
        params.put( "diskQuota", jsonUtil.to( diskQuota ) );

        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //*************************************************************

        try
        {
            String alias = SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS;
            post( path, alias, params, headers );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Error setting container disk quota", e );
        }
    }


    @RolesAllowed( "Environment-Management|A|Update" )
    @Override
    public void setRamQuota( final ContainerHost containerHost, final RamQuota ramQuota ) throws PeerException
    {
        Preconditions.checkNotNull( containerHost, "Container host is null" );
        Preconditions.checkArgument( containerHost instanceof EnvironmentContainerHost );

        EnvironmentContainerHost host = ( EnvironmentContainerHost ) containerHost;
        Preconditions.checkNotNull( ramQuota, "Invalid ram quota" );

        String path = "/container/quota/ram2";

        Map<String, String> params = Maps.newHashMap();
        params.put( "containerId", host.getId() );
        params.put( "ramQuota", jsonUtil.to( ramQuota ) );

        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //*************************************************************

        try
        {
            String alias = SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS;
            post( path, alias, params, headers );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Error setting ram quota", e );
        }
    }


    @Override
    public int getAvailableRamQuota( final ContainerHost containerHost ) throws PeerException
    {
        Preconditions.checkNotNull( containerHost, "Container host is null" );
        Preconditions.checkArgument( containerHost instanceof EnvironmentContainerHost );

        EnvironmentContainerHost host = ( EnvironmentContainerHost ) containerHost;
        String path = "/container/quota/ram/available";

        Map<String, String> params = Maps.newHashMap();
        params.put( "containerId", host.getId() );

        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //*************************************************************
        try
        {
            String alias = SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS;
            String response = get( path, alias, params, headers );

            return jsonUtil.from( response, Integer.class );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Error obtaining container available ram quota", e );
        }
    }


    @Override
    public int getAvailableCpuQuota( final ContainerHost containerHost ) throws PeerException
    {
        Preconditions.checkNotNull( containerHost, "Container host is null" );
        Preconditions.checkArgument( containerHost instanceof EnvironmentContainerHost );

        EnvironmentContainerHost host = ( EnvironmentContainerHost ) containerHost;
        String path = "/container/quota/cpu/available";

        Map<String, String> params = Maps.newHashMap();
        params.put( "containerId", host.getId() );

        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //*************************************************************

        try
        {
            String alias = SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS;
            String response = get( path, alias, params, headers );

            return jsonUtil.from( response, Integer.class );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Error obtaining container available cpu quota", e );
        }
    }


    @Override
    public DiskQuota getAvailableDiskQuota( final ContainerHost containerHost, final DiskPartition diskPartition )
            throws PeerException
    {
        Preconditions.checkNotNull( containerHost, "Container host is null" );
        Preconditions.checkArgument( containerHost instanceof EnvironmentContainerHost );

        EnvironmentContainerHost host = ( EnvironmentContainerHost ) containerHost;
        Preconditions.checkNotNull( diskPartition, "Invalid disk partition" );

        String path = "/container/quota/disk/available";

        Map<String, String> params = Maps.newHashMap();
        params.put( "containerId", host.getId() );
        params.put( "diskPartition", jsonUtil.to( diskPartition ) );

        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //*************************************************************

        try
        {
            String alias = SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS;
            String response = get( path, alias, params, headers );

            return jsonUtil.from( response, new TypeToken<DiskQuota>()
            {}.getType() );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Error obtaining container available disk quota", e );
        }
    }


    @Override
    public QuotaInfo getQuotaInfo( final ContainerHost containerHost, final QuotaType quotaType ) throws PeerException
    {
        Preconditions.checkNotNull( containerHost, "Container host is null" );
        Preconditions.checkArgument( containerHost instanceof EnvironmentContainerHost );

        EnvironmentContainerHost host = ( EnvironmentContainerHost ) containerHost;
        Preconditions.checkNotNull( quotaType, "Invalid quota type" );

        String path = "/container/quota/info";

        Map<String, String> params = Maps.newHashMap();
        params.put( "containerId", host.getId() );
        params.put( "quotaType", jsonUtil.to( quotaType ) );

        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //*************************************************************

        try
        {
            String alias = SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS;
            String response = get( path, alias, params, headers );

            return jsonUtil.from( response, new TypeToken<QuotaInfo>()
            {}.getType() );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Error obtaining container quota", e );
        }
    }


    @RolesAllowed( "Environment-Management|A|Update" )
    @Override
    public void setQuota( final ContainerHost containerHost, final QuotaInfo quotaInfo ) throws PeerException
    {
        Preconditions.checkNotNull( containerHost, "Container host is null" );
        Preconditions.checkArgument( containerHost instanceof EnvironmentContainerHost );

        EnvironmentContainerHost host = ( EnvironmentContainerHost ) containerHost;
        Preconditions.checkNotNull( quotaInfo, "Invalid quota info" );

        String path = "/container/quota";

        Map<String, String> params = Maps.newHashMap();
        params.put( "containerId", host.getId() );
        params.put( "quotaInfo", jsonUtil.to( quotaInfo ) );

        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //*************************************************************

        try
        {
            String alias = SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS;
            post( path, alias, params, headers );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Error setting container quota", e );
        }
    }


    @PermitAll
    @Override
    public HostInfo getContainerHostInfoById( final String containerHostId ) throws PeerException
    {
        String path = String.format( "/container/info" );
        try
        {
            //*********construct Secure Header ****************************
            Map<String, String> headers = Maps.newHashMap();
            //*************************************************************

            Map<String, String> params = Maps.newHashMap();
            params.put( "containerId", jsonUtil.to( containerHostId ) );
            String response = get( path, SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS, params, headers );
            return jsonUtil.from( response, HostInfoModel.class );
        }
        catch ( Exception e )
        {
            throw new PeerException( String.format( "Error getting hostInfo from peer %s", getName() ), e );
        }
    }


    //DONE


    @Override
    public CommandResult execute( final RequestBuilder requestBuilder, final Host host ) throws CommandException
    {
        return execute( requestBuilder, host, null );
    }


    @Override
    public CommandResult execute( final RequestBuilder requestBuilder, final Host host, final CommandCallback callback )
            throws CommandException
    {
        Preconditions.checkNotNull( requestBuilder, "Invalid request" );
        Preconditions.checkNotNull( host, "Invalid host" );

        BlockingCommandCallback blockingCommandCallback = getBlockingCommandCallback( callback );

        executeAsync( requestBuilder, host, blockingCommandCallback, blockingCommandCallback.getCompletionSemaphore() );

        CommandResult commandResult = blockingCommandCallback.getCommandResult();

        if ( commandResult == null )
        {
            commandResult = new CommandResultImpl( null, null, null, CommandStatus.TIMEOUT );
        }

        return commandResult;
    }


    protected BlockingCommandCallback getBlockingCommandCallback( CommandCallback callback )
    {
        return new BlockingCommandCallback( callback );
    }


    @Override
    public void executeAsync( final RequestBuilder requestBuilder, final Host host, final CommandCallback callback )
            throws CommandException
    {
        executeAsync( requestBuilder, host, callback, null );
    }


    @Override
    public void executeAsync( final RequestBuilder requestBuilder, final Host host ) throws CommandException
    {
        executeAsync( requestBuilder, host, null );
    }


    protected void executeAsync( final RequestBuilder requestBuilder, final Host host, final CommandCallback callback,
                                 Semaphore semaphore ) throws CommandException
    {
        Preconditions.checkNotNull( requestBuilder, "Invalid request" );
        Preconditions.checkNotNull( host, "Invalid host" );

        if ( !host.isConnected() )
        {
            throw new CommandException( "Host disconnected." );
        }

        if ( !( host instanceof ContainerHost ) )
        {
            throw new CommandException( "Operation not allowed" );
        }

        String environmentId = ( ( EnvironmentContainerHost ) host ).getEnvironmentId();
        CommandRequest request = new CommandRequest( requestBuilder, host.getId(), environmentId );
        //cache callback
        commandResponseListener.addCallback( request.getRequestId(), callback, requestBuilder.getTimeout(), semaphore );

        //send command request to remote peer counterpart
        try
        {
            //*********construct Secure Header ****************************
            Map<String, String> headers = Maps.newHashMap();
            //************************************************************************


            sendRequest( request, RecipientType.COMMAND_REQUEST.name(), Timeouts.COMMAND_REQUEST_MESSAGE_TIMEOUT,
                    headers );
        }
        catch ( PeerException e )
        {
            throw new CommandException( e );
        }
    }


    @Override
    public <T, V> V sendRequest( final T request, final String recipient, final int requestTimeout,
                                 Class<V> responseType, int responseTimeout, final Map<String, String> headers )
            throws PeerException
    {
        Preconditions.checkArgument( responseTimeout > 0, "Invalid response timeout" );
        Preconditions.checkNotNull( responseType, "Invalid response type" );

        //send request
        MessageRequest messageRequest = sendRequestInternal( request, recipient, requestTimeout, headers );

        //wait for response here
        MessageResponse messageResponse =
                messageResponseListener.waitResponse( messageRequest, requestTimeout, responseTimeout );

        LOG.debug( String.format( "%s", messageResponse ) );
        if ( messageResponse != null )
        {
            if ( messageResponse.getException() != null )
            {
                throw new PeerException( messageResponse.getException() );
            }
            else if ( messageResponse.getPayload() != null )
            {
                LOG.debug( String.format( "Trying get response object: %s", responseType ) );
                final V message = messageResponse.getPayload().getMessage( responseType );
                LOG.debug( String.format( "Response object: %s", message ) );
                return message;
            }
        }

        return null;
    }


    @Override
    public <T> void sendRequest( final T request, final String recipient, final int requestTimeout,
                                 final Map<String, String> headers ) throws PeerException
    {

        sendRequestInternal( request, recipient, requestTimeout, headers );
    }


    protected <T> MessageRequest sendRequestInternal( final T request, final String recipient, final int requestTimeout,
                                                      final Map<String, String> headers ) throws PeerException
    {
        Preconditions.checkNotNull( request, "Invalid request" );
        Preconditions.checkArgument( !Strings.isNullOrEmpty( recipient ), "Invalid recipient" );
        Preconditions.checkArgument( requestTimeout > 0, "Invalid request timeout" );

        MessageRequest messageRequest =
                new MessageRequest( new Payload( request, localPeer.getId() ), recipient, headers );
        Message message = messenger.createMessage( messageRequest );

        messageRequest.setMessageId( message.getId() );

        try
        {
            messenger.sendMessage( this, message, RecipientType.PEER_REQUEST_LISTENER.name(), requestTimeout, headers );
        }
        catch ( MessageException e )
        {
            throw new PeerException( e );
        }

        return messageRequest;
    }


    @RolesAllowed( "Environment-Management|A|Write" )
    @Override
    public Set<HostInfoModel> createEnvironmentContainerGroup( final CreateEnvironmentContainerGroupRequest request )
            throws PeerException
    {
        Preconditions.checkNotNull( request, "Invalid request" );


        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //************************************************************************

        CreateEnvironmentContainerGroupResponse response =
                sendRequest( request, RecipientType.CREATE_ENVIRONMENT_CONTAINER_GROUP_REQUEST.name(),
                        Timeouts.CREATE_CONTAINER_REQUEST_TIMEOUT, CreateEnvironmentContainerGroupResponse.class,
                        Timeouts.CREATE_CONTAINER_RESPONSE_TIMEOUT, headers );

        if ( response != null )
        {
            return response.getHosts();
        }
        else
        {
            throw new PeerException( "Command timed out" );
        }
    }


    @RolesAllowed( "Environment-Management|A|Delete" )
    @Override
    public ContainersDestructionResult destroyContainersByEnvironment( final String environmentId ) throws PeerException
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( environmentId ), "Invalid environment id" );


        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //**************************************************************************


        DestroyEnvironmentContainerGroupResponse response =
                sendRequest( new DestroyEnvironmentContainerGroupRequest( environmentId ),
                        RecipientType.DESTROY_ENVIRONMENT_CONTAINER_GROUP_REQUEST.name(),
                        Timeouts.DESTROY_CONTAINER_REQUEST_TIMEOUT, DestroyEnvironmentContainerGroupResponse.class,
                        Timeouts.DESTROY_CONTAINER_RESPONSE_TIMEOUT, headers );

        if ( response != null )
        {
            return new ContainersDestructionResultImpl( getId(), response.getDestroyedContainersIds(),
                    response.getException() );
        }
        else
        {
            throw new PeerException( "Command timed out" );
        }
    }


    //networking
    @RolesAllowed( "Environment-Management|A|Write" )
    @Override
    public int setupTunnels( final Map<String, String> peerIps, final String environmentId ) throws PeerException
    {

        Preconditions.checkNotNull( peerIps, "Invalid peer ips set" );
        Preconditions.checkArgument( !peerIps.isEmpty(), "Invalid peer ips set" );
        Preconditions.checkNotNull( environmentId, "Invalid environment id" );

        String path = "/tunnels";

        try
        {
            //*********construct Secure Header ****************************
            Map<String, String> headers = Maps.newHashMap();
            //
            //            headers.put( Common.HEADER_SPECIAL, "ENC" );
            //            headers.put( Common.HEADER_PEER_ID_SOURCE, localPeer.getId() );
            //            headers.put( Common.HEADER_PEER_ID_TARGET, peerInfo.getId() );
            //*************************************************************
            Map<String, String> params = Maps.newHashMap();
            params.put( "peerIps", jsonUtil.to( peerIps ) );
            params.put( "environmentId", environmentId );

            String response = post( path, SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS, params, headers );

            return Integer.parseInt( response );
        }
        catch ( Exception e )
        {
            throw new PeerException( String.format( "Error setting up tunnels on peer %s", getName() ), e );
        }
    }


    @RolesAllowed( "Environment-Management|A|Write" )
    @Override
    public Vni reserveVni( final Vni vni ) throws PeerException
    {
        Preconditions.checkNotNull( vni, "Invalid vni" );

        return new PeerWebClient( peerInfo.getIp(), provider ).reserveVni( vni );
    }
    //************ END ENVIRONMENT SPECIFIC REST


    @RolesAllowed( "Environment-Management|A|Read" )
    @Override
    public Set<Gateway> getGateways() throws PeerException
    {
        String path = "/gateways";
        try
        {
            return new PeerWebClient( peerInfo.getIp(), provider ).getGateways();
        }
        catch ( Exception e )
        {
            throw new PeerException( String.format( "Error obtaining gateways from peer %s", getName() ), e );
        }
    }


    @Override
    public void setDefaultGateway( final ContainerHost containerHost, final String gatewayIp ) throws PeerException
    {
        Preconditions.checkNotNull( containerHost, "Container host is null" );
        Preconditions.checkArgument( containerHost instanceof EnvironmentContainerHost );

        EnvironmentContainerHost host = ( EnvironmentContainerHost ) containerHost;
        Preconditions.checkArgument( !Strings.isNullOrEmpty( gatewayIp ) && gatewayIp.matches( Common.IP_REGEX ),
                "Invalid gateway IP" );

        String path = "peer/container/gateway";

        Map<String, String> params = Maps.newHashMap();
        params.put( "containerId", host.getId() );
        params.put( "gatewayIp", gatewayIp );

        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //*************************************************************

        try
        {
            String alias = SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS;
            post( path, alias, params, headers );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Error setting container gateway ip", e );
        }
    }

    @Override
    public Set<Vni> getReservedVnis() throws PeerException
    {
        return new PeerWebClient( peerInfo.getIp(), provider ).getReservedVnis();
    }


    @Override
    public PublicKeyContainer createEnvironmentKeyPair( EnvironmentId environmentId ) throws PeerException
    {
        Preconditions.checkNotNull( environmentId, "Invalid environmentId" );

        return new PeerWebClient( peerInfo.getIp(), provider ).createEnvironmentKeyPair( environmentId );
    }


    @Override
    public HostInterfaces getInterfaces()
    {
        return new PeerWebClient( peerInfo.getIp(), provider ).getInterfaces();
    }


    @Override
    public void setupN2NConnection( final N2NConfig config )
    {
        Preconditions.checkNotNull( config, "Invalid n2n config" );

        new PeerWebClient( peerInfo.getIp(), provider ).setupN2NConnection( config );
    }


    @Override
    public void removeN2NConnection( final EnvironmentId environmentId ) throws PeerException
    {
        Preconditions.checkNotNull( environmentId, "Invalid environment ID" );
        new PeerWebClient( peerInfo.getIp(), provider ).removeN2NConnection( environmentId );
    }


    @Override
    public void createGateway( final String environmentGatewayIp, final int vlan ) throws PeerException
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( environmentGatewayIp ) );
        Preconditions.checkArgument( vlan > 0 );

        String path = "/gateways";

        try
        {
            //*********construct Secure Header ****************************
            Map<String, String> headers = Maps.newHashMap();
            //*************************************************************

            Map<String, String> params = Maps.newHashMap();
            params.put( "gatewayIp", environmentGatewayIp );
            params.put( "vlan", String.valueOf( vlan ) );

            post( path, SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS, params, headers );
        }
        catch ( Exception e )
        {
            throw new PeerException( String.format( "Error creating gateway on peer %s", getName() ), e );
        }
    }


    @Override
    public ResourceHostMetrics getResourceHostMetrics()
    {
        return new PeerWebClient( peerInfo.getIp(), provider ).getResourceHostMetrics();
    }


//    @Override
//    public HostMetric getHostMetric( final String hostId )
//    {
//        return new PeerWebClient( peerInfo.getIp(), provider ).getHostMetric( hostId );
//    }


    @Override
    public boolean equals( final Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof RemotePeerImpl ) )
        {
            return false;
        }

        final RemotePeerImpl that = ( RemotePeerImpl ) o;

        return getId().equals( that.getId() );
    }


    @Override
    public int hashCode()
    {
        return getId().hashCode();
    }
}
