/**
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apereo.services.persondir.support;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apereo.services.persondir.IPersonAttributeDao;
import org.springframework.util.CollectionUtils;
import org.springmodules.cache.key.CacheKeyGenerator;
import org.springmodules.cache.key.HashCodeCacheKey;
import org.springmodules.cache.key.HashCodeCalculator;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Generates a cache key using a hash of the {@link Method} being called and for
 * {@link IPersonAttributeDao#getMultivaluedUserAttributes(String, org.apereo.services.persondir.IPersonAttributeDaoFilter)} and
 * {@link IPersonAttributeDao#getUserAttributes(String, org.apereo.services.persondir.IPersonAttributeDaoFilter)} the {@link String} uid or for
 * {@link IPersonAttributeDao#getMultivaluedUserAttributes(Map, org.apereo.services.persondir.IPersonAttributeDaoFilter)} and
 * {@link IPersonAttributeDao#getUserAttributes(Map, org.apereo.services.persondir.IPersonAttributeDaoFilter)} attributes from the seed {@link Map}
 * as specified by the <code>cacheKeyAttributes</code> {@link Set}
 *
 * @author Eric Dalquist

 */
public class AttributeBasedCacheKeyGenerator implements CacheKeyGenerator {
    private static final Map<String, Object> POSSIBLE_USER_ATTRIBUTE_NAMES_SEED_MAP = Collections.singletonMap("getPossibleUserAttributeNames_seedMap", (Object) new Serializable() {
        private static final long serialVersionUID = 1L;
    });
    private static final Map<String, Object> AVAILABLE_QUERY_ATTRIBUTES_SEED_MAP = Collections.singletonMap("getAvailableQueryAttributes_seedMap", (Object) new Serializable() {
        private static final long serialVersionUID = 1L;
    });

    protected final Log logger = LogFactory.getLog(this.getClass());

    /**
     * Methods on {@link IPersonAttributeDao} that are cachable
     */
    public enum CachableMethod {
        @Deprecated
        MULTIVALUED_USER_ATTRIBUTES__MAP("getMultivaluedUserAttributes", Map.class),
        @Deprecated
        MULTIVALUED_USER_ATTRIBUTES__STR("getMultivaluedUserAttributes", String.class),
        @Deprecated
        USER_ATTRIBUTES__MAP("getUserAttributes", Map.class),
        @Deprecated
        USER_ATTRIBUTES__STR("getUserAttributes", String.class),

        PERSON_STR("getPerson", String.class),
        PEOPLE_MAP("getPeople", Map.class),
        PEOPLE_MULTIVALUED_MAP("getPeopleWithMultivaluedAttributes", Map.class),
        POSSIBLE_USER_ATTRIBUTE_NAMES("getPossibleUserAttributeNames"),
        AVAILABLE_QUERY_ATTRIBUTES("getAvailableQueryAttributes");

        private final String name;
        private final Class<?>[] args;

        private CachableMethod(final String name, final Class<?>... args) {
            this.name = name;
            this.args = args;
        }

        /**
         * @return the name
         */
        public String getName() {
            return this.name;
        }

        /**
         * @return the args
         */
        public Class<?>[] getArgs() {
            return this.args;
        }

        @Override
        public String toString() {
            return this.name + "(" + Arrays.asList(this.args) + ")";
        }
    }

    /*
     * The set of attributes to use to generate the cache key.
     */
    private Set<String> cacheKeyAttributes = null;

    private String defaultAttributeName = "username";
    private Set<String> defaultAttributeNameSet = Collections.singleton(this.defaultAttributeName);
    private boolean useAllAttributes = false;
    private boolean ignoreEmptyAttributes = false;

    /**
     * @return the cacheKeyAttributes
     */
    public Set<String> getCacheKeyAttributes() {
        return cacheKeyAttributes;
    }

    /**
     * @param cacheKeyAttributes the cacheKeyAttributes to set
     */
    public void setCacheKeyAttributes(final Set<String> cacheKeyAttributes) {
        this.cacheKeyAttributes = cacheKeyAttributes;
    }

    /**
     * @return the defaultAttributeName
     */
    public String getDefaultAttributeName() {
        return this.defaultAttributeName;
    }

    /**
     * @param defaultAttributeName the defaultAttributeName to set
     */
    public void setDefaultAttributeName(final String defaultAttributeName) {
        Validate.notNull(defaultAttributeName);
        this.defaultAttributeName = defaultAttributeName;
        this.defaultAttributeNameSet = Collections.singleton(this.defaultAttributeName);
    }

    public boolean isUseAllAttributes() {
        return useAllAttributes;
    }

    /**
     * If all seed attributes should be used. If true cacheKeyAttributes and defaultAttributeName are ignored. Defaults
     * to false.
     *
     * @param useAllAttributes True to use all attributes
     */
    public void setUseAllAttributes(final boolean useAllAttributes) {
        this.useAllAttributes = useAllAttributes;
    }

    /**
     * Returns boolean indicating whether seed attributes with empty values (null, empty string or empty list values)
     * should be ignored when generating the cache key
     * @return True if seed attributes should ignore empty values
     */
    public boolean isIgnoreEmptyAttributes() {
        return ignoreEmptyAttributes;
    }

    /**
     * If seed attributes with empty values (null, empty string or empty list values) should be ignored when generating
     * the cache key. Defaults to false.
     *
     * @param ignoreEmptyAttributes True to ignore attributes with empty values
     */
    public void setIgnoreEmptyAttributes(final boolean ignoreEmptyAttributes) {
        this.ignoreEmptyAttributes = ignoreEmptyAttributes;
    }


    /* (non-Javadoc)
     * @see org.springmodules.cache.key.CacheKeyGenerator#generateKey(org.aopalliance.intercept.MethodInvocation)
     */
    @Override
    public Serializable generateKey(final MethodInvocation methodInvocation) {
        //Determine the tareted CachableMethod
        final CachableMethod cachableMethod = this.resolveCacheableMethod(methodInvocation);

        //Use the resolved cachableMethod to determine the seed Map and then get the hash of the key elements
        final Object[] methodArguments = methodInvocation.getArguments();
        final Map<String, Object> seed = this.getSeed(methodArguments, cachableMethod);
        final Integer keyHashCode = this.getKeyHash(seed);

        //If no code generated return null
        if (keyHashCode == null) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("No cache key generated for MethodInvocation='" + methodInvocation + "'");
            }
            return null;
        }

        //Calculate the hashCode and checkSum
        final HashCodeCalculator hashCodeCalculator = new HashCodeCalculator();
        hashCodeCalculator.append(keyHashCode);

        //Assemble the serializable key object
        final long checkSum = hashCodeCalculator.getCheckSum();
        final int hashCode = hashCodeCalculator.getHashCode();
        final HashCodeCacheKey hashCodeCacheKey = new HashCodeCacheKey(checkSum, hashCode);

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Generated cache key '" + hashCodeCacheKey + "' for MethodInvocation='" + methodInvocation + "'");
        }
        return hashCodeCacheKey;
    }

    /**
     * Get the see {@link Map} that was passed to the {@link CachableMethod}. For {@link CachableMethod}s that
     * take {@link String} arguments this method is responsible for converting it into a {@link Map} using the
     * <code>defaultAttributeName</code>.
     *
     * @param methodArguments The method arguments
     * @param cachableMethod The targeted cachable method
     * @return The seed Map for the method call
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> getSeed(final Object[] methodArguments, final CachableMethod cachableMethod) {
        final Map<String, Object> seed;
        switch (cachableMethod) {
            //Both methods that take a Map argument can just have the first argument returned
            case PEOPLE_MAP:
            case PEOPLE_MULTIVALUED_MAP:
            case MULTIVALUED_USER_ATTRIBUTES__MAP:
            case USER_ATTRIBUTES__MAP: {
                seed = (Map<String, Object>) methodArguments[0];
            }
            break;

            //The multivalued attributes with a string needs to be converted to Map<String, List<Object>>
            case MULTIVALUED_USER_ATTRIBUTES__STR: {
                final String uid = (String) methodArguments[0];
                seed = Collections.singletonMap(this.defaultAttributeName, (Object) Collections.singletonList(uid));
            }
            break;

            //The single valued attributes with a string needs to be converted to Map<String, Object>
            case PERSON_STR:
            case USER_ATTRIBUTES__STR: {
                final String uid = (String) methodArguments[0];
                seed = Collections.singletonMap(this.defaultAttributeName, (Object) uid);
            }
            break;

            //The getPossibleUserAttributeNames has a special Map seed that we return to represent calls to it 
            case POSSIBLE_USER_ATTRIBUTE_NAMES: {
                seed = POSSIBLE_USER_ATTRIBUTE_NAMES_SEED_MAP;
            }
            break;

            //The getAvailableQueryAttributes has a special Map seed that we return to represent calls to it 
            case AVAILABLE_QUERY_ATTRIBUTES: {
                seed = AVAILABLE_QUERY_ATTRIBUTES_SEED_MAP;
            }
            break;

            default: {
                throw new IllegalArgumentException("Unsupported CachableMethod resolved: '" + cachableMethod + "'");
            }
        }
        return seed;
    }

    /**
     * Gets the hash of the key elements from the seed {@link Map}. The key elements are specified by
     * the <code>cacheKeyAttributes</code> {@link Set} or if it is <code>null</code> the
     * <code>defaultAttributeName</code> is used as the key attribute.
     *
     * @param seed Seed
     * @return Hash of key elements from the seed
     */
    protected Integer getKeyHash(final Map<String, Object> seed) {
        //Determine the attributes to build the cache key with
        final Set<String> cacheAttributes;
        if (this.useAllAttributes) {
            cacheAttributes = seed.keySet();
        } else if (this.cacheKeyAttributes != null) {
            cacheAttributes = this.cacheKeyAttributes;
        } else {
            cacheAttributes = this.defaultAttributeNameSet;
        }

        //Build the cache key based on the attribute Set
        final HashMap<String, Object> cacheKey = new HashMap<>(cacheAttributes.size());
        for (final String attr : cacheAttributes) {
            if (seed.containsKey(attr)) {
                final Object value = seed.get(attr);

                if (!this.ignoreEmptyAttributes) {
                    cacheKey.put(attr, value);
                } else if (value instanceof Collection) {
                    if (!CollectionUtils.isEmpty((Collection<?>) value)) {
                        cacheKey.put(attr, value);
                    }
                } else if (value instanceof String) {
                    if (StringUtils.isNotEmpty((String) value)) {
                        cacheKey.put(attr, value);
                    }
                } else if (value != null) {
                    cacheKey.put(attr, value);
                }
            }
        }

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Generated cache Map " + cacheKey + " from seed Map " + seed);
        }

        //If no entries don't return a key
        if (cacheKey.isEmpty()) {
            return null;
        }

        //Return the key map's hash code
        return cacheKey.hashCode();
    }

    /**
     * Iterates over the {@link CachableMethod} instances to determine which instance the
     * passed {@link MethodInvocation} applies to.
     *
     * @param methodInvocation method invocation
     * @return Cachable method
     */
    protected CachableMethod resolveCacheableMethod(final MethodInvocation methodInvocation) {
        final Method targetMethod = methodInvocation.getMethod();
        final Class<?> targetClass = targetMethod.getDeclaringClass();

        for (final CachableMethod cachableMethod : CachableMethod.values()) {
            Method cacheableMethod = null;
            try {
                cacheableMethod = targetClass.getMethod(cachableMethod.getName(), cachableMethod.getArgs());
            } catch (final SecurityException e) {
                this.logger.warn("Security exception while attempting to if the target class '" + targetClass + "' implements the cachable method '" + cachableMethod + "'", e);
            } catch (final NoSuchMethodException e) {
                final String message = "Taret class '" + targetClass + "' does not implement possible cachable method '" + cachableMethod + "'. Is the advice applied to the correct bean and methods?";

                if (this.logger.isDebugEnabled()) {
                    this.logger.debug(message, e);
                } else {
                    this.logger.warn(message);
                }
            }

            if (targetMethod.equals(cacheableMethod)) {
                return cachableMethod;
            }
        }

        throw new IllegalArgumentException("Do not know how to generate a cache for for '" + targetMethod + "' on class '" + targetClass + "'. Is the advice applied to the correct bean and methods?");
    }
}
