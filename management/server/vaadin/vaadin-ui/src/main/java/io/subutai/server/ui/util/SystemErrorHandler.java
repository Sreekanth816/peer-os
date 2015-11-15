package io.subutai.server.ui.util;


import java.security.AccessControlException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.ErrorEvent;
import com.vaadin.server.ErrorHandler;
import com.vaadin.shared.ui.label.ContentMode;

import io.subutai.server.ui.MainUI;
import io.subutai.server.ui.views.ErrorView;


/**
 *
 */
public class SystemErrorHandler implements ErrorHandler
{
    private static final Logger LOG = LoggerFactory.getLogger( SystemErrorHandler.class.getName() );
    private MainUI mainUI;

    public SystemErrorHandler(MainUI mainUI)
    {
        this.mainUI = mainUI;
    }

    @Override
    public void error( final ErrorEvent event )
    {
        String message= "",errorMessage="";

        try
        {
            for (Throwable t = event.getThrowable();t != null; t = t.getCause())
            {
                if ( t.getCause() == null )
                {
                    if(t.getClass()  ==  AccessControlException.class)
                    {
                        message = "Access Denied! You don't have permission to access resource";
                        errorMessage = t.toString();
                    }
                    else
                    {
                        message = "System error Please check log files.";
                        errorMessage = t.toString();
                    }
                }
            }

            LOG.error("***** Error:"+errorMessage);
            ErrorView errorView = new ErrorView(mainUI,null);
            errorView.setErrorMessage( message );

            mainUI.getUI().addWindow( errorView);

        }
        catch(Exception ex)
        {
            //ignore
        }
    }
}
