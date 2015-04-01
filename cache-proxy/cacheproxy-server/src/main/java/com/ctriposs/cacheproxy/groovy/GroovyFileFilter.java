package com.ctriposs.cacheproxy.groovy;

import java.io.File;
import java.io.FilenameFilter;

/**
 * @author:yjfei
 * @date: 2/26/2015.
 */
public class GroovyFileFilter implements FilenameFilter {
    public boolean accept(File dir, String name) {
        return name.endsWith(".groovy");
    }
}
