/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2007-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.core.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Caches properties files in order to improve performance.
 *
 * @author <a href="mailto:brozow@opennms.org">Mathew Brozowski</a>
 * @version $Id: $
 */
public class PropertiesCache {
	
	private static final Logger LOG = LoggerFactory.getLogger(PropertiesCache.class);

    public static final String CHECK_LAST_MODIFY_STRING = "org.opennms.utils.propertiesCache.enableCheckFileModified";
    public static final String CACHE_TIMEOUT = "org.opennms.utils.propertiesCache.cacheTimeout";
    public static final int DEFAULT_CACHE_TIMEOUT = 3600;

    protected static class PropertiesHolder {
        private Properties m_properties;
        private final File m_file;
        private final Lock lock = new ReentrantLock();
        private long m_lastModify = 0;
        private boolean m_checkLastModify = Boolean.getBoolean(CHECK_LAST_MODIFY_STRING);

        PropertiesHolder(final File file) {
            this.m_file = file;
            this.m_properties = null;
        }
        
        private Properties read() throws IOException {
            if (!m_file.canRead()) {
                return null;
            }

            InputStream in = null;
            try {
                in = new FileInputStream(this.m_file);
                Properties prop = new Properties();
                prop.load(in);
                if (this.m_checkLastModify) {
                    this.m_lastModify = this.m_file.lastModified();
                }
                return prop;
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // Ignore this exception
                    }
                }
            }
        }
        
        private void write() throws IOException {
            if(!this.m_file.getParentFile().mkdirs()) {
            	if(!this.m_file.getParentFile().exists()) {
            		LOG.warn("Could not make directory: {}", this.m_file.getParentFile().getPath());
            	}
            }
            OutputStream out = null;
            try {
                out = new FileOutputStream(this.m_file);
                this.m_properties.store(out, null);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // Ignore this exception
                    }
                }
            }
        }

        public Properties get() throws IOException {
            this.lock.lock();
            try {
                if (this.m_properties == null) {
                    readWithDefault(new Properties());
                } else {
                    if (this.m_checkLastModify && this.m_file.canRead() && this.m_lastModify != this.m_file.lastModified()) {
                        this.m_properties = read();
                    }
                }
                return this.m_properties;
            } finally {
                this.lock.unlock();
            }
        }

        private void readWithDefault(final Properties deflt) throws IOException {
            // this is
            if (deflt == null && !this.m_file.canRead()) {
                // nothing to load so m_properties remains null no writing necessary
                // just return to avoid getting the write lock
                return;
            }
            
            if (this.m_properties == null) {
                this.m_properties = read();
                if (this.m_properties == null) {
                    this.m_properties = deflt;
                }
            }   
        }
        
        public void put(final Properties properties) throws IOException {
            this.lock.lock();
            try {
                this.m_properties = properties;
                write();
            } finally {
                this.lock.unlock();
            }
        }

        public void update(final Map<String, String> props) throws IOException {
            if (props == null) return;
            this.lock.lock();
            try {
                boolean save = false;
                for(Entry<String, String> e : props.entrySet()) {
                    if (!e.getValue().equals(get().get(e.getKey()))) {
                        get().put(e.getKey(), e.getValue());
                        save = true;
                    }
                }
                if (save) {
                    write();
                }
            } finally {
                this.lock.unlock();
            }
        }
        
        public void setProperty(final String key, final String value) throws IOException {
            this.lock.lock();
            try {
                // first we do get to make sure the properties are loaded
                get();
                if (!value.equals(get().get(key))) {
                    get().put(key, value);
                    write();
                }
            } finally {
                this.lock.unlock();
            }
        }

        public Properties find() throws IOException {
            this.lock.lock();
            try {
                if (this.m_properties == null) {
                    readWithDefault(null);
                }
                return this.m_properties;
            } finally {
                this.lock.unlock();
            }
        }

        public String getProperty(final String key) throws IOException {
            this.lock.lock();
            try {
                return get().getProperty(key);
            } finally {
                this.lock.unlock();
            }
            
        }
    }

    protected final Cache<String, PropertiesHolder> m_cache;

    public PropertiesCache() {
        this(CacheBuilder.newBuilder());
    }


    protected PropertiesCache(final CacheBuilder<Object, Object> cacheBuilder) {
        this.m_cache = cacheBuilder
                .expireAfterAccess(Integer.getInteger(CACHE_TIMEOUT, DEFAULT_CACHE_TIMEOUT), TimeUnit.SECONDS)
                .build();
    }

    private PropertiesHolder getHolder(final File propFile) throws IOException {
        final String key = propFile.getCanonicalPath();

        try {
            return this.m_cache.get(key, new Callable<PropertiesHolder>() {
                @Override
                public PropertiesHolder call() throws Exception {
                    return new PropertiesHolder(propFile);
                }
            });
        } catch (final ExecutionException e) {
            throw new IOException("Error creating PropertyHolder instance", e);
        }
    }
    
    /**
     * <p>clear</p>
     */
    public void clear() {
        synchronized (this.m_cache) {
            this.m_cache.invalidateAll();
        }
    }

    /**
     * Get the current properties object from the cache loading it in memory
     *
     * @param propFile a {@link java.io.File} object.
     * @throws java.io.IOException if any.
     * @return a {@link java.util.Properties} object.
     */
    public Properties getProperties(final File propFile) throws IOException {
        return getHolder(propFile).get();
    }
    
    /**
     * <p>findProperties</p>
     *
     * @param propFile a {@link java.io.File} object.
     * @return a {@link java.util.Properties} object.
     * @throws java.io.IOException if any.
     */
    public Properties findProperties(final File propFile) throws IOException {
        return getHolder(propFile).find();
    }
    /**
     * <p>saveProperties</p>
     *
     * @param propFile a {@link java.io.File} object.
     * @param properties a {@link java.util.Properties} object.
     * @throws java.io.IOException if any.
     */
    public void saveProperties(final File propFile, final Properties properties) throws IOException {
        getHolder(propFile).put(properties);
    }

    public void saveProperties(final File propFile, final Map<String, String> attributeMappings) throws IOException {
        if (attributeMappings == null) return;
        final Properties properties = new Properties();
        properties.putAll(attributeMappings);
        saveProperties(propFile, properties);
    }

    /**
     * <p>updateProperties</p>
     *
     * @param propFile a {@link java.io.File} object.
     * @param props a {@link java.util.Map} object.
     * @throws java.io.IOException if any.
     */
    public void updateProperties(final File propFile, final Map<String, String> props) throws IOException {
        getHolder(propFile).update(props);
    }
    
    /**
     * <p>setProperty</p>
     *
     * @param propFile a {@link java.io.File} object.
     * @param key a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     */
    public void setProperty(final File propFile, final String key, final String value) throws IOException {
        getHolder(propFile).setProperty(key, value);
    }
    
    /**
     * <p>getProperty</p>
     *
     * @param propFile a {@link java.io.File} object.
     * @param key a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     */
    public String getProperty(final File propFile, final String key) throws IOException {
        return getHolder(propFile).getProperty(key);
    }
}
