package com.ctriposs.cacheproxy;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctriposs.cacheproxy.util.ShutdownHookManager;

public class CacheProxyMain {

    private static final Logger logger = LoggerFactory.getLogger(CacheProxyMain.class);

    private static String APPLICATION_ID = "cacheproxy";
    
	public static void main(String args[]){
		
        //Archaius loading configuration depends on this property.
        System.setProperty("archaius.deployment.applicationId", APPLICATION_ID);

        String environment = System.getProperty("archaius.deployment.environment");
        if(environment==null || environment.equals("")){
            System.setProperty("archaius.deployment.environment", "locale");
        }

        printStartupAndShutDown(args);
        CacheProxyServer server = null;
        try {
        	server = new CacheProxyServer();
        	server.start();

            final CacheProxyServer finalServer = server;
            ShutdownHookManager.get().addShutdownHook(new Runnable() {
                @Override
                public void run() {
                	finalServer.close();
                }
            },Integer.MAX_VALUE);

        } catch (Exception e) {
            if(server!=null) server.close();
            logger.error("Can not to start the cacheproxy then is going to shutdown", e);
        }
	}
	
	private static void printStartupAndShutDown(String[] args) {
        String host= "Unknown";
        try {
            host = InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        final String hostName = host;
        final String className = CacheProxyMain.class.getSimpleName();

        logger.info("STARTUP_MSG:\n" +
                        "*******************************************\n" +
                        "\tStarting : {}\n" +
                        "\tHost : {}\n" +
                        "\tArgs : {}\n" +
                        "*******************************************",
                className,hostName, Arrays.toString(args));

        ShutdownHookManager.get().addShutdownHook(new Runnable() {
            @Override
            public void run() {
                logger.info("SHUTDOWN_MSG:\n" +
                                "*******************************************\n" +
                                "\tShutting down : {}\n" +
                                "\tHost : {}\n" +
                                "*******************************************",
                        className,hostName);

            }
        }, 1);
    }
}
