package org.safehaus.subutai.plugin.hipi.impl.handler;


import org.safehaus.subutai.common.exception.ClusterSetupException;
import org.safehaus.subutai.common.protocol.AbstractOperationHandler;
import org.safehaus.subutai.common.protocol.ClusterSetupStrategy;
import org.safehaus.subutai.common.protocol.EnvironmentBuildTask;
import org.safehaus.subutai.common.tracker.ProductOperation;
import org.safehaus.subutai.core.environment.api.exception.EnvironmentBuildException;
import org.safehaus.subutai.core.environment.api.helper.Environment;
import org.safehaus.subutai.plugin.hadoop.api.HadoopClusterConfig;
import org.safehaus.subutai.plugin.hipi.api.HipiConfig;
import org.safehaus.subutai.plugin.hipi.api.SetupType;
import org.safehaus.subutai.plugin.hipi.impl.HipiImpl;


public class InstallOperationHandler extends AbstractOperationHandler<HipiImpl>
{
    private final HipiConfig config;
    private HadoopClusterConfig hadoopConfig;


    public InstallOperationHandler( HipiImpl manager, HipiConfig config )
    {
        super( manager, config.getClusterName() );
        this.config = config;
        productOperation = manager.getTracker().createProductOperation( HipiConfig.PRODUCT_KEY,
                String.format( "Installing %s", HipiConfig.PRODUCT_KEY ) );
    }


    public void setHadoopConfig( HadoopClusterConfig hadoopConfig )
    {
        this.hadoopConfig = hadoopConfig;
    }


    @Override
    public void run()
    {
        ProductOperation po = productOperation;
        Environment env = null;

        if ( config.getSetupType() == SetupType.WITH_HADOOP )
        {

            if ( hadoopConfig == null )
            {
                po.addLogFailed( "No Hadoop configuration specified" );
                return;
            }

            po.addLog( "Preparing environment..." );
            hadoopConfig.setTemplateName( HipiConfig.TEMPLATE_NAME );
            try
            {
                EnvironmentBuildTask eb = manager.getHadoopManager().getDefaultEnvironmentBlueprint( hadoopConfig );
                env = manager.getEnvironmentManager().buildEnvironment( eb );
            }
            catch ( ClusterSetupException ex )
            {
                po.addLogFailed( "Failed to prepare environment: " + ex.getMessage() );
                return;
            }
            catch ( EnvironmentBuildException ex )
            {
                po.addLogFailed( "Failed to build environment: " + ex.getMessage() );
                return;
            }
            po.addLog( "Environment preparation completed" );
        }

        ClusterSetupStrategy s = manager.getClusterSetupStrategy( env, config, po );

        try
        {
            if ( s == null )
            {
                throw new ClusterSetupException( "No setup strategy" );
            }

            s.setup();
            po.addLogDone( "Done" );
        }
        catch ( ClusterSetupException ex )
        {
            po.addLogFailed( "Failed to setup cluster: " + ex.getMessage() );
        }
    }
}
