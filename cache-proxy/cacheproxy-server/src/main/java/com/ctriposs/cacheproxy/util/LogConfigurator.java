package com.ctriposs.cacheproxy.util;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class LogConfigurator {

	private static Logger logger = LoggerFactory.getLogger(LogConfigurator.class);
    private String appName;
    private String environment;	
	
    public LogConfigurator(String appName,String environment){
    	this.appName = appName;
    	this.environment = environment;
    }
    
    public void config(){
        logger.info("To reconfigure logback.");
        
        try{
        	
        	LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        	
        	try{
        		JoranConfigurator joranConfigurator = new JoranConfigurator();
                joranConfigurator.setContext(lc);
                lc.reset();
                String configFileName = appName + "-logback-" + environment + ".xml";
                InputStream inputStream = LogConfigurator.class.getClassLoader().getResourceAsStream(configFileName);

                if (inputStream == null) {
                    throw new Exception("Can't find the logback config file [ " + configFileName + " ].");
                }

                joranConfigurator.doConfigure(inputStream);

                logger.info("Reconfigure logback.");
        		
        	}catch(JoranException e){
        		e.printStackTrace();
        	}
        	
            StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
        }catch(Exception e){
        	logger.warn("Failed to reconfigure logback.", e);
        }
    }
	
}
