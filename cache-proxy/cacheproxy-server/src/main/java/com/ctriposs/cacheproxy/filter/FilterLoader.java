package com.ctriposs.cacheproxy.filter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctriposs.cacheproxy.common.DynamicCodeCompiler;
import com.ctriposs.cacheproxy.common.FilterFactory;

/**
 * This class is one of the core classes in GateKeeper. It compiles, loads from a File, and checks if source code changed.
 * It also holds GateFilters by filterType.
 *
 * @author:yjfei
 * @date: 2/27/2015.
 */
public class FilterLoader {
    final static FilterLoader INSTANCE = new FilterLoader();

    private static final Logger LOG = LoggerFactory.getLogger(FilterLoader.class);

    static DynamicCodeCompiler COMPILER;
    static FilterFactory FILTER_FACTORY = new DefaultFilterFactory();

    /**
     * @return Singleton FilterLoader
     */
    public static FilterLoader getInstance() {
        return INSTANCE;
    }

    private final ConcurrentHashMap<String, Long> filterClassLastModified = new ConcurrentHashMap<String, Long>();
    private final ConcurrentHashMap<String, String> filterClassCode = new ConcurrentHashMap<String, String>();
    private final ConcurrentHashMap<String, String> filterCheck = new ConcurrentHashMap<String, String>();
    private final ConcurrentHashMap<String, List<ProxyFilter>> hashFiltersByType = new ConcurrentHashMap<String, List<ProxyFilter>>();

    private FilterRegistry filterRegistry = FilterRegistry.instance();

    private FilterLoader(){}

    /**
     * Sets a Dynamic Code Compiler
     *
     * @param compiler
     */
    public void setCompiler(DynamicCodeCompiler compiler) {
        COMPILER = compiler;
    }

    // overidden by tests
    public void setFilterRegistry(FilterRegistry r) {
        this.filterRegistry = r;
    }

    /**
     * Sets a FilterFactory
     *
     * @param factory
     */
    public void setFilterFactory(FilterFactory factory) {
        FILTER_FACTORY = factory;
    }


    /**
     * Given source and name will compile and store the filter if it detects that the filter code has changed or
     * the filter doesn't exist. Otherwise it will return an instance of the requested GateFilter
     *
     * @param sCode source code
     * @param sName name of the filter
     * @return the GateFilter
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public ProxyFilter getFilter(String sCode, String sName) throws Exception {

        if (filterCheck.get(sName) == null) {
            filterCheck.putIfAbsent(sName, sName);
            if (!sCode.equals(filterClassCode.get(sName))) {
                LOG.info("reloading code " + sName);
                filterRegistry.remove(sName);
            }
        }
        ProxyFilter filter = filterRegistry.get(sName);
        if (filter == null) {
            Class clazz = COMPILER.compile(sCode, sName);
            if (!Modifier.isAbstract(clazz.getModifiers())) {
                filter = (ProxyFilter) FILTER_FACTORY.newInstance(clazz);
            }
        }
        return filter;

    }

    /**
     * @return the total number of Gate filters
     */
    public int filterInstanceMapSize() {
        return filterRegistry.size();
    }


    /**
     * From a file this will read the GateFilter source code, compile it, and add it to the list of current filters
     * a true response means that it was successful.
     *
     * @param file
     * @return true if the filter in file successfully read, compiled, verified and added to Gate
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IOException
     */
    public boolean putFilter(File file) throws Exception {
        String sName = file.getAbsolutePath() + file.getName();
        if (filterClassLastModified.get(sName) != null && (file.lastModified() != filterClassLastModified.get(sName))) {
            LOG.debug("reloading filter " + sName);
            filterRegistry.remove(sName);
        }
        ProxyFilter filter = filterRegistry.get(sName);
        if (filter == null) {
            Class clazz = COMPILER.compile(file);
            if (!Modifier.isAbstract(clazz.getModifiers())) {
                filter = (ProxyFilter) FILTER_FACTORY.newInstance(clazz);
                filterRegistry.put(file.getAbsolutePath() + file.getName(), filter);
                filterClassLastModified.put(sName, file.lastModified());
                List<ProxyFilter> list = hashFiltersByType.get(filter.filterType());
                if (list != null) {
                    hashFiltersByType.remove(filter.filterType()); //rebuild this list
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a list of filters by the filterType specified
     *
     * @param filterType
     * @return a List<GateFilter>
     */
    public List<ProxyFilter> getFiltersByType(String filterType) {

        List<ProxyFilter> list = hashFiltersByType.get(filterType);
        if (list != null) return list;

        list = new ArrayList<ProxyFilter>();

        Collection<ProxyFilter> filters = filterRegistry.getAllFilters();
        for (Iterator<ProxyFilter> iterator = filters.iterator(); iterator.hasNext(); ) {
            ProxyFilter filter = iterator.next();
            if (filter.filterType().equals(filterType)) {
                list.add(filter);
            }
        }
        Collections.sort(list); // sort by priority

        hashFiltersByType.putIfAbsent(filterType, list);
        return list;
    }
}
