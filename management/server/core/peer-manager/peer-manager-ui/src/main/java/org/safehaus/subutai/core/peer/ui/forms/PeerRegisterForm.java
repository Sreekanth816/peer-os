package org.safehaus.subutai.core.peer.ui.forms;


import java.security.KeyStore;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.safehaus.subutai.common.peer.PeerException;
import org.safehaus.subutai.common.peer.PeerInfo;
import org.safehaus.subutai.common.peer.PeerStatus;
import org.safehaus.subutai.common.security.crypto.keystore.KeyStoreData;
import org.safehaus.subutai.common.security.crypto.keystore.KeyStoreManager;
import org.safehaus.subutai.common.settings.ChannelSettings;
import org.safehaus.subutai.common.settings.SecuritySettings;
import org.safehaus.subutai.common.util.JsonUtil;
import org.safehaus.subutai.common.util.RestUtil;
import org.safehaus.subutai.core.peer.ui.PeerManagerPortalModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.form.Form;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.vaadin.annotations.AutoGenerated;
import com.vaadin.data.Property;
import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;


/**
 * Registration process should be handled in save manner so no middleware attacks occur. In order to get there peers
 * need to exchange with public keys. This will create ssl layer by encrypting all traffic passing through their
 * connection. So first initial handshake will be one direction, to pass keys through encrypted channel and register
 * them in peers' trust stores. These newly saved keys will be used further for safe communication, with bidirectional
 * authentication.
 *
 *
 * TODO here still exists some issues concerned via registration/reject/approve requests. Some of them must pass through
 * secure channel such as unregister process. Which already must be in bidirectional auth completed stage.
 */
public class PeerRegisterForm extends CustomComponent
{

    private static final Logger LOG = LoggerFactory.getLogger( PeerRegisterForm.class.getName() );
    public final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @AutoGenerated
    private AbsoluteLayout mainLayout;
    @AutoGenerated
    private Table peersTable;
    @AutoGenerated
    private Button showPeersButton;
    @AutoGenerated
    private Button registerRequestButton;
    @AutoGenerated
    private TextField ipTextField;

    private PeerManagerPortalModule module;


    /**
     * The constructor should first build the main layout, set the composition root and then do any custom
     * initialization. <p/> The constructor will not be automatically regenerated by the visual editor.
     */
    public PeerRegisterForm( final PeerManagerPortalModule module )
    {
        buildMainLayout();
        setCompositionRoot( mainLayout );

        this.module = module;

        showPeersButton.click();
    }


    @AutoGenerated
    private AbsoluteLayout buildMainLayout()
    {
        // common part: create layout
        mainLayout = new AbsoluteLayout();
        mainLayout.setImmediate( false );
        mainLayout.setWidth( "100%" );
        mainLayout.setHeight( "100%" );

        // top-level component properties
        setWidth( "100.0%" );
        setHeight( "100.0%" );

        // peerRegisterLayout
        final AbsoluteLayout peerRegisterLayout = buildAbsoluteLayout_2();
        mainLayout.addComponent( peerRegisterLayout, "top:20.0px;right:0.0px;bottom:-20.0px;left:0.0px;" );

        return mainLayout;
    }


    @AutoGenerated
    private AbsoluteLayout buildAbsoluteLayout_2()
    {

        // common part: create layout
        AbsoluteLayout absoluteLayout = new AbsoluteLayout();
        absoluteLayout.setImmediate( false );
        absoluteLayout.setWidth( "100.0%" );
        absoluteLayout.setHeight( "100.0%" );

        // peerRegistration
        final Label peerRegistration = new Label();
        peerRegistration.setImmediate( false );
        peerRegistration.setWidth( "-1px" );
        peerRegistration.setHeight( "-1px" );
        peerRegistration.setValue( "Peer registration" );
        absoluteLayout.addComponent( peerRegistration, "top:0.0px;left:20.0px;" );

        // IP
        final Label IP = new Label();
        IP.setImmediate( false );
        IP.setWidth( "-1px" );
        IP.setHeight( "-1px" );
        IP.setValue( "IP" );
        absoluteLayout.addComponent( IP, "top:36.0px;left:20.0px;" );

        // ipTextField
        ipTextField = new TextField();
        ipTextField.setImmediate( false );
        ipTextField.setWidth( "-1px" );
        ipTextField.setHeight( "-1px" );
        ipTextField.setMaxLength( 15 );
        absoluteLayout.addComponent( ipTextField, "top:36.0px;left:150.0px;" );

        // registerRequestButton
        registerRequestButton = createRegisterButton();
        absoluteLayout.addComponent( registerRequestButton, "top:160.0px;left:20.0px;" );
        registerRequestButton = createRegisterButton();

        // showPeersButton
        showPeersButton = createShowPeersButton();
        absoluteLayout.addComponent( showPeersButton, "top:234.0px;left:20.0px;" );

        // peersTable
        peersTable = new Table();
        peersTable.setCaption( "Peers" );
        peersTable.setImmediate( false );
        peersTable.setWidth( "1000px" );
        peersTable.setHeight( "283px" );
        absoluteLayout.addComponent( peersTable, "top:294.0px;left:20.0px;" );

        return absoluteLayout;
    }


    private Button createShowPeersButton()
    {
        showPeersButton = new Button();
        showPeersButton.setCaption( "Show peers" );
        showPeersButton.setImmediate( false );
        showPeersButton.setWidth( "-1px" );
        showPeersButton.setHeight( "-1px" );

        showPeersButton.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( final Button.ClickEvent clickEvent )
            {
                populateData();
                peersTable.refreshRowCache();
            }
        } );

        return showPeersButton;
    }


    private void populateData()
    {
        List<PeerInfo> peers = module.getPeerManager().peers();
        peersTable.removeAllItems();
        peersTable.addContainerProperty( "ID", UUID.class, null );
        peersTable.addContainerProperty( "Name", String.class, null );
        peersTable.addContainerProperty( "IP", String.class, null );
        peersTable.addContainerProperty( "Status", PeerStatus.class, null );
        peersTable.addContainerProperty( "ActionsAdvanced", PeerManageActionsComponent.class, null );

        for ( final PeerInfo peer : peers )
        {
            if ( peer == null || peer.getStatus() == null )
            {
                continue;
            }
            /**
             * According to peer status perform sufficient action
             */
            PeerManageActionsComponent.PeerManagerActionsListener listener =
                    new PeerManageActionsComponent.PeerManagerActionsListener()

                    {
                        @Override
                        public void OnPositiveButtonTrigger( final PeerInfo peer )
                        {
                            switch ( peer.getStatus() )
                            {
                                case REQUESTED:
                                    PeerInfo selfPeer;

                                    selfPeer = module.getPeerManager().getLocalPeerInfo();

                                    //************ Send Trust SSL Cert **************************************

                                    KeyStore keyStore;
                                    KeyStoreData keyStoreData;
                                    KeyStoreManager keyStoreManager;

                                    keyStoreData = new KeyStoreData();
                                    keyStoreData.setupKeyStorePx2();

                                    keyStoreManager = new KeyStoreManager();
                                    keyStore = keyStoreManager.load( keyStoreData );

                                    String HEXCert =
                                            keyStoreManager.exportCertificateHEXString( keyStore, keyStoreData );


                                    //***********************************************************************

                                    if ( approvePeerRegistration( selfPeer, peer, HEXCert ) )
                                    {
                                        peer.setStatus( PeerStatus.APPROVED );
                                    }

                                    break;
                                case REGISTERED:
                                    //TODO In further devs plan to support several states
                                    break;
                                case BLOCKED:
                                    break;
                            }
                            Property property = peersTable.getItem( peer.getId() ).getItemProperty( "Status" );
                            property.setValue( peer.getStatus() );
                            module.getPeerManager().update( peer );
                        }


                        @Override
                        public void OnNegativeButtonTrigger( final PeerInfo peer )
                        {
                            try
                            {
                                //TODO perform different actions on peer rejected request
                                PeerInfo selfPeer = module.getPeerManager().getLocalPeerInfo();
                                switch ( peer.getStatus() )
                                {
                                    case REJECTED:
                                        removeMeFromRemote( selfPeer, peer );
                                        peersTable.removeItem( peer.getId() );
                                        module.getPeerManager().unregister( peer.getId().toString() );
                                        break;
                                    case BLOCKED:
                                    case BLOCKED_PEER:
                                    case REQUESTED:
                                    case REQUEST_SENT:
                                        if ( rejectPeerRegistration( selfPeer, peer ) )
                                        {
                                            peer.setStatus( PeerStatus.REJECTED );
                                        }
                                        Property property =
                                                peersTable.getItem( peer.getId() ).getItemProperty( "Status" );
                                        property.setValue( peer.getStatus() );
                                        module.getPeerManager().update( peer );
                                        break;
                                    case APPROVED:
                                        module.getPeerManager().unregister( peer.getId().toString() );
                                        peersTable.removeItem( peer.getId() );
                                        unregisterMeFromRemote( selfPeer, peer );
                                        break;
                                }
                            }
                            catch ( PeerException pe )
                            {
                                Notification.show( pe.getMessage(), Notification.Type.ERROR_MESSAGE );
                            }
                        }
                    };
            PeerManageActionsComponent component = new PeerManageActionsComponent( module, peer, listener );
            peersTable
                    .addItem( new Object[] { peer.getId(), peer.getName(), peer.getIp(), peer.getStatus(), component },
                            peer.getId() );
        }
    }


    /**
     * Send peer to register on remote peer. To construct secure connection. For now initializer peer doesn't send its
     * px2 (public key requiring bidirectional authentication).
     *
     * @param peerToRegister - initializer peer info for registration process
     * @param ip - target peer ip address
     *
     * @return - remote peer info to whom registration is requested
     */
    private PeerInfo registerMeToRemote( PeerInfo peerToRegister, String ip )
    {
        String baseUrl = String.format( "https://%s:%s/cxf", ip, ChannelSettings.SECURE_PORT_X1 );
        WebClient client = RestUtil.createTrustedWebClient( baseUrl );//WebClient.create( baseUrl );
        client.type( MediaType.MULTIPART_FORM_DATA ).accept( MediaType.APPLICATION_JSON );
        Form form = new Form();
        form.set( "peer", GSON.toJson( peerToRegister ) );

        Response response = client.path( "peer/register" ).form( form );
        if ( response.getStatus() == Response.Status.OK.getStatusCode() )
        {
            Notification.show( String.format( "Request sent to %s!", ip ) );
            String responseString = response.readEntity( String.class );
            LOG.info( response.toString() );
            PeerInfo remotePeerInfo = JsonUtil.from( responseString, new TypeToken<PeerInfo>()
            {
            }.getType() );
            if ( remotePeerInfo != null )
            {
                remotePeerInfo.setStatus( PeerStatus.REQUEST_SENT );
                return remotePeerInfo;
            }
        }
        else
        {
            LOG.warn( "Response for registering peer: " + response.toString() );
        }
        return null;
    }


    private void unregisterMeFromRemote( PeerInfo peerToUnregister, PeerInfo remotePeerInfo )
    {
        String baseUrl = String.format( "https://%s:%s/cxf", remotePeerInfo.getIp(), ChannelSettings.SECURE_PORT_X2 );
        WebClient client = RestUtil.createTrustedWebClientWithAuth( baseUrl,
                SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS );// WebClient.create( baseUrl );
        Response response =
                client.path( "peer/unregister" ).type( MediaType.APPLICATION_JSON ).accept( MediaType.APPLICATION_JSON )
                      .query( "peerId", GSON.toJson( peerToUnregister.getId().toString() ) ).delete();
        if ( response.getStatus() == Response.Status.OK.getStatusCode() )
        {
            LOG.info( response.toString() );
            Notification.show( String.format( "Request sent to %s!", remotePeerInfo.getName() ) );
            //************ Delete Trust SSL Cert **************************************
            KeyStore keyStore;
            KeyStoreData keyStoreData;
            KeyStoreManager keyStoreManager;

            keyStoreData = new KeyStoreData();
            keyStoreData.setupTrustStorePx2();
            keyStoreData.setAlias( remotePeerInfo.getId().toString() );

            keyStoreManager = new KeyStoreManager();
            keyStore = keyStoreManager.load( keyStoreData );

            keyStoreManager.deleteEntry( keyStore, keyStoreData );
            //***********************************************************************

            module.getSslContextFactory().reloadTrustStore();
            //            new Thread( new RestartCoreServlet() ).start();
        }
        else
        {
            LOG.warn( "Response for registering peer: " + response.toString() );
        }
    }


    private void removeMeFromRemote( PeerInfo peerToUnregister, PeerInfo remotePeerInfo )
    {
        String baseUrl = String.format( "https://%s:%s/cxf", remotePeerInfo.getIp(), ChannelSettings.SECURE_PORT_X1 );
        WebClient client = RestUtil.createTrustedWebClient( baseUrl );// WebClient.create( baseUrl );
        Response response =
                client.path( "peer/remove" ).type( MediaType.APPLICATION_JSON ).accept( MediaType.APPLICATION_JSON )
                      .query( "rejectedPeerId", GSON.toJson( peerToUnregister.getId().toString() ) ).delete();
        if ( response.getStatus() == Response.Status.NO_CONTENT.getStatusCode() )
        {
            LOG.info( response.toString() );
            Notification.show( String.format( "Request sent to %s!", remotePeerInfo.getName() ) );
        }
        else
        {
            LOG.warn( "Response for registering peer: " + response.toString() );
        }
    }


    private boolean approvePeerRegistration( PeerInfo peerToUpdateOnRemote, PeerInfo remotePeer, String cert )
    {
        String baseUrl = String.format( "https://%s:%s/cxf", remotePeer.getIp(), ChannelSettings.SECURE_PORT_X1 );
        WebClient client = RestUtil.createTrustedWebClient( baseUrl );//WebClient.create( baseUrl );
        client.type( MediaType.APPLICATION_FORM_URLENCODED ).accept( MediaType.APPLICATION_JSON );

        Form form = new Form();
        form.set( "approvedPeer", GSON.toJson( peerToUpdateOnRemote ) );
        form.set( "root_cert_px2", cert );

        Response response = client.path( "peer/approve" ).put( form );
        if ( response.getStatus() == Response.Status.OK.getStatusCode() )
        {
            LOG.info( response.readEntity( String.class ) );
            remotePeer.setStatus( PeerStatus.APPROVED );
            String root_cert_px2 = response.readEntity( String.class );
            Notification.show( String.format( "Request sent to %s!", remotePeer.getName() ) );
            //************ Save Trust SSL Cert **************************************
            KeyStore keyStore;
            KeyStoreData keyStoreData;
            KeyStoreManager keyStoreManager;

            keyStoreData = new KeyStoreData();
            keyStoreData.setupTrustStorePx2();
            keyStoreData.setHEXCert( root_cert_px2 );
            keyStoreData.setAlias( remotePeer.getId().toString() );

            keyStoreManager = new KeyStoreManager();
            keyStore = keyStoreManager.load( keyStoreData );

            keyStoreManager.importCertificateHEXString( keyStore, keyStoreData );
            //***********************************************************************

            module.getSslContextFactory().reloadTrustStore();

            //            new Thread( new RestartCoreServlet() ).start();
            return true;
        }
        else
        {
            LOG.warn( "Response for registering peer: " + response.toString() );
            return false;
        }
    }


    /**
     * Peer request rejection intented to be handled before they exchange with keys
     *
     * @param peerToUpdateOnRemote - local peer info to update/send to remote peer
     * @param remotePeer - remote peer whose request was rejected
     *
     * @return - status mentioning does remote has responded with 202 status code.
     */
    private boolean rejectPeerRegistration( PeerInfo peerToUpdateOnRemote, PeerInfo remotePeer )
    {
        String baseUrl = String.format( "https://%s:%s/cxf", remotePeer.getIp(), ChannelSettings.SECURE_PORT_X1 );
        WebClient client = RestUtil.createTrustedWebClient( baseUrl );// WebClient.create( baseUrl );
        client.type( MediaType.APPLICATION_FORM_URLENCODED ).accept( MediaType.APPLICATION_JSON );

        Form form = new Form();
        form.set( "rejectedPeerId", peerToUpdateOnRemote.getId().toString() );

        Response response = client.path( "peer/reject" ).put( form );
        if ( response.getStatus() == Response.Status.NO_CONTENT.getStatusCode() )
        {
            LOG.info( "Successfully reject peer request" );
            Notification.show( String.format( "Request sent to %s!", remotePeer.getName() ) );

            //TODO maybe will implement certificates exchange later on initial request, before peer approves
            // initializer' request
            //************ Delete Trust SSL Cert **************************************
            //            KeyStore keyStore;
            //            KeyStoreData keyStoreData;
            //            KeyStoreManager keyStoreManager;
            //
            //            keyStoreData = new KeyStoreData();
            //            keyStoreData.setupTrustStorePx2();
            //            keyStoreData.setAlias( remotePeer.getId().toString() );
            //
            //            keyStoreManager = new KeyStoreManager();
            //            keyStore = keyStoreManager.load( keyStoreData );
            //
            //            keyStoreManager.deleteEntry( keyStore, keyStoreData );
            //***********************************************************************
            return true;
        }
        else
        {
            LOG.warn( "Response for registering peer: " + response.toString() );
            return false;
        }
    }


    /**
     * Send peer registration request for further handshakes.
     *
     * @return - vaadin button with request initializing click listener
     */
    private Button createRegisterButton()
    {
        registerRequestButton = new Button();
        registerRequestButton.setCaption( "Register" );
        registerRequestButton.setImmediate( true );
        registerRequestButton.setWidth( "-1px" );
        registerRequestButton.setHeight( "-1px" );

        registerRequestButton.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( final Button.ClickEvent clickEvent )
            {
                getUI().access( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        String ip = ipTextField.getValue();
                        LOG.warn( ip );

                        try
                        {
                            PeerInfo selfPeer = module.getPeerManager().getLocalPeerInfo();
                            PeerInfo remotePeer = registerMeToRemote( selfPeer, ip );
                            if ( remotePeer != null )
                            {
                                module.getPeerManager().register( remotePeer );
                            }
                            showPeersButton.click();
                        }
                        catch ( PeerException e )
                        {
                            Notification.show( e.getMessage(), Notification.Type.ERROR_MESSAGE );
                        }
                    }
                } );
            }
        } );

        return registerRequestButton;
    }
}