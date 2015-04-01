package com.ctriposs.cacheproxy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctriposs.cacheproxy.util.LogConfigurator;
import com.ctriposs.cacheproxy.util.X;
import com.ctriposs.tools.infoboard.InfoBoardServer;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.HealthCheckCallback;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.PropertiesInstanceConfig;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryManager;

public abstract class AbstractServer {

	private static Logger logger = LoggerFactory.getLogger(AbstractServer.class);
	
    protected LogConfigurator logConfigurator;
    protected String appName;
    protected InfoBoardServer internalsServer;
	
	
    public AbstractServer() throws Exception{
    	appName = ConfigurationManager.getDeploymentContext().getApplicationId();
    	
        logger.info("Initializing the  {} ...", appName);
        try {
            loadConfiguration();
            configLog();
            registerEureka();

            internalsServer = new InfoBoardServer(ConfigurationManager.getConfigInstance().getInt("server.internals.port", 8077), appName);
            internalsServer.start();

            init();

            logger.info("Has initialized the {}.", appName);
        } catch (Exception e) {
            logger.error("Failed to initialize the " + appName + ".", e);
            throw e;
        }
        
    }
    
    protected abstract void init() throws Exception;
    protected abstract void doStart() throws Exception;
    protected abstract void doClose() throws Exception;
    
    public void start() throws Exception {
        logger.info("Starting the  {} ...", appName);

        try {
            doStart();
            try {
                ApplicationInfoManager.getInstance().setInstanceStatus(InstanceInfo.InstanceStatus.UP);
            } catch (Exception ignore) {}
        } catch (Exception e) {
            logger.error("Failed to start " + appName + ".", e);
            throw e;
        }

        logger.info("Started the {}.", appName);
    }

    public void close() {
        logger.info("Stopping the  {} ...", appName);

        try {
            ApplicationInfoManager.getInstance().setInstanceStatus(InstanceInfo.InstanceStatus.DOWN);
        } catch (Exception ignore) {}
        DiscoveryManager.getInstance().shutdownComponent();

        try {
            doClose();
            internalsServer.shutdown();
        } catch (Exception e) {
            logger.error("Error stopping httpServer ...", e);
        }
    }
    
    private void configLog() {
        logConfigurator = new LogConfigurator(appName, ConfigurationManager.getDeploymentContext().getDeploymentEnvironment());
        logConfigurator.config();
    }
    

    private void loadConfiguration() {
        System.setProperty(DynamicPropertyFactory.ENABLE_JMX, "true");

        // Loading properties via archaius.
        if (null != appName) {
            try {
                logger.info(String.format("Loading application properties with app id: %s and environment: %s", appName,
                        ConfigurationManager.getDeploymentContext().getDeploymentEnvironment()));
                ConfigurationManager.loadCascadedPropertiesFromResources(appName);
            } catch (IOException e) {
                logger.error(String.format(
                        "Failed to load properties for application id: %s and environment: %s. This is ok, if you do not have application level properties.",
                        appName,
                        ConfigurationManager.getDeploymentContext().getDeploymentEnvironment()), e);
            }
        } else {
            logger.warn("Application identifier not defined, skipping application level properties loading. You must set a property 'archaius.deployment.applicationId' to be able to load application level properties.");
        }

    }


    private void registerEureka() {
        DynamicBooleanProperty eurekaEnabled = DynamicPropertyFactory.getInstance().getBooleanProperty("eureka.enabled", false);
        if(!eurekaEnabled.get()) return;

        EurekaInstanceConfig eurekaInstanceConfig = new PropertiesInstanceConfig() {};

        DiscoveryManager.getInstance().initComponent(eurekaInstanceConfig, new DefaultEurekaClientConfig());

        final DynamicStringProperty serverStatus = DynamicPropertyFactory.getInstance().getStringProperty("server."+ X.getIp()+".status", "up");
        DiscoveryManager.getInstance().getDiscoveryClient().registerHealthCheckCallback(new HealthCheckCallback() {
            @Override
            public boolean isHealthy() {
                return serverStatus.get().toLowerCase().equals("up");
            }
        });

        String version = String.valueOf(System.currentTimeMillis());
        String group = ConfigurationManager.getConfigInstance().getString("server.group", "default");
        String dataCenter = ConfigurationManager.getConfigInstance().getString("server.data-center", "default");

        final Map<String, String> metadata = new HashMap<String,String>();
        metadata.put("version", version);
        metadata.put("group", group);
        metadata.put("dataCenter", dataCenter);

        String turbineInstance = getTurbineInstance();
        if (turbineInstance != null) {
            metadata.put("turbine.instance", turbineInstance);
        }

        ApplicationInfoManager.getInstance().registerAppMetadata(metadata);
    }

    public String getTurbineInstance() {
        String instance = null;
        String ip = X.getIp();
        if (ip != null) {
            instance = ip + ":" + ConfigurationManager.getConfigInstance().getString("server.internals.port", "8077");
        }else{
            logger.warn("Can't build turbine instance as can't fetch the ip.");

        }
        return instance;
    }
}
