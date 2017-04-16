package io.subutai.core.network.api;


import java.util.Date;

import io.subutai.common.network.LogLevel;
import io.subutai.common.network.P2pLogs;
import io.subutai.common.network.ProxyLoadBalanceStrategy;
import io.subutai.common.network.SshTunnel;
import io.subutai.common.peer.ContainerHost;
import io.subutai.common.peer.Host;
import io.subutai.common.protocol.CustomProxyConfig;
import io.subutai.common.protocol.LoadBalancing;
import io.subutai.common.protocol.P2PConnections;
import io.subutai.common.protocol.Protocol;
import io.subutai.common.protocol.ReservedPorts;
import io.subutai.common.protocol.Tunnels;


public interface NetworkManager
{

    /**
     * Sets up an P2P connection on specified host with explicit IP.
     */
    void joinP2PSwarm( Host host, String interfaceName, String localIp, String p2pHash, String secretKey,
                       long secretKeyTtlSec ) throws NetworkManagerException;


    /**
     * Resets a secret key for a given P2P network
     *
     * @param host - host
     * @param p2pHash - P2P network hash
     * @param newSecretKey - new secret key to set
     * @param ttlSeconds - time-to-live for the new secret key
     */
    void resetSwarmSecretKey( Host host, String p2pHash, String newSecretKey, long ttlSeconds )
            throws NetworkManagerException;


    /**
     * Returns all p2p connections running on the specified host
     *
     * @param host - host
     */

    P2PConnections getP2PConnections( Host host ) throws NetworkManagerException;

    String getP2pVersion( Host host ) throws NetworkManagerException;

    P2pLogs getP2pLogs( Host host, LogLevel logLevel, Date from, Date till ) throws NetworkManagerException;

    void createTunnel( Host host, String tunnelName, String tunnelIp, int vlan, long vni )
            throws NetworkManagerException;

    void deleteTunnel( Host host, String tunnelName ) throws NetworkManagerException;

    Tunnels getTunnels( Host host ) throws NetworkManagerException;


    /**
     * Returns reverse proxy domain assigned to vlan
     *
     * @param vLanId - vlan id
     *
     * @return - domain or null if not assigned
     */
    String getVlanDomain( int vLanId ) throws NetworkManagerException;


    /**
     * Removes reverse proxy domain assigned to vlan if any
     *
     * @param vLanId - vlan id
     */
    void removeVlanDomain( int vLanId ) throws NetworkManagerException;

    /**
     * Assigns reverse proxy domain to vlan
     *
     * @param vLanId - vlan id
     * @param proxyLoadBalanceStrategy - strategy to load balance requests to the domain
     * @param sslCertPath - path to SSL certificate to enable HTTPS access to domai only, null if not needed
     */
    void setVlanDomain( int vLanId, String domain, ProxyLoadBalanceStrategy proxyLoadBalanceStrategy,
                        String sslCertPath ) throws NetworkManagerException;


    /**
     * Checks if IP is in vlan reverse proxy domain
     *
     * @param hostIp - ip to check
     * @param vLanId - vlan id
     *
     * @return - true if ip is in vlan domain, false otherwise
     */
    boolean isIpInVlanDomain( String hostIp, int vLanId ) throws NetworkManagerException;

    /**
     * Adds ip to vlan reverse proxy domain
     *
     * @param hostIp - ip to add
     * @param vLanId - vlan id
     */
    void addIpToVlanDomain( String hostIp, int vLanId ) throws NetworkManagerException;

    /**
     * Removes ip from reverse proxy domain
     *
     * @param hostIp - ip to remove
     * @param vLanId - vlan id
     */
    void removeIpFromVlanDomain( String hostIp, int vLanId ) throws NetworkManagerException;

    /**
     * Sets up SSH connectivity for container identified by @param containerIp
     *
     * @param containerIp - ip fo container
     * @param sshIdleTimeout - timeout during which the ssh connectivity is active in seconds
     *
     * @return - port to which clients should connect to access the container via ssh
     */
    SshTunnel setupContainerSshTunnel( String containerIp, int sshIdleTimeout ) throws NetworkManagerException;

    void addCustomProxy( CustomProxyConfig proxyConfig, ContainerHost containerHost ) throws NetworkManagerException;

    void removeCustomProxy( String vlan ) throws NetworkManagerException;

    ReservedPorts getReservedPorts( final Host host ) throws NetworkManagerException;

    ReservedPorts getContainerPortMappings( final Host host, final Protocol protocol ) throws NetworkManagerException;

    /**
     * Maps specified container port to random RH port
     *
     * @param host RH host
     * @param protocol protocol
     * @param containerIp ip of container
     * @param containerPort container port
     *
     * @return mapped random RH port
     */
    int mapContainerPort( Host host, Protocol protocol, String containerIp, int containerPort )
            throws NetworkManagerException;

    /**
     * Maps specified container port to specified RH port (RH port acts as a clustered group for multiple containers)
     *
     * @param host RH host
     * @param protocol protocol
     * @param containerIp ip of container
     * @param containerPort container port
     * @param rhPort RH port
     */
    void mapContainerPort( Host host, Protocol protocol, String containerIp, int containerPort, int rhPort )
            throws NetworkManagerException;

    /**
     * Removes specified container port mapping
     *
     * @param host RH host
     * @param protocol protocol
     * @param containerIp ip of container
     * @param containerPort container port
     * @param rhPort RH port
     */
    void removeContainerPortMapping( Host host, Protocol protocol, String containerIp, int containerPort, int rhPort )
            throws NetworkManagerException;

    /**
     * Maps specified container port to specified RH port (RH port acts as a clustered group for multiple containers)
     *
     * @param host RH host
     * @param protocol protocol, can only be http or https
     * @param containerIp ip of container
     * @param containerPort container port
     * @param rhPort RH port
     * @param domain domain
     * @param sslCertPath optional path to SSL cert, pass null if not needed
     * @param loadBalancing optional load balancing method, pass null if not needed
     */
    void mapContainerPortToDomain( Host host, Protocol protocol, String containerIp, int containerPort, int rhPort,
                                   String domain, String sslCertPath, LoadBalancing loadBalancing )
            throws NetworkManagerException;

    /**
     * Removes specified container port domain mapping
     *
     * @param host RH host
     * @param protocol protocol, can only be http or https
     * @param containerIp ip of container
     * @param containerPort container port
     * @param rhPort RH port
     * @param domain domain
     */
    void removeContainerPortDomainMapping( Host host, Protocol protocol, String containerIp, int containerPort,
                                           int rhPort, String domain ) throws NetworkManagerException;
}