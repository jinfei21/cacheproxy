package com.ctriposs.cacheproxy.common;

import java.io.File;

/**
 * Interface to generate Classes from source code
 *
 * @author:yjfei
 * @date: 2/26/2015.
 */
public interface DynamicCodeCompiler {
    Class compile(String sCode, String sName) throws Exception;

    Class compile(File file) throws Exception;
}
