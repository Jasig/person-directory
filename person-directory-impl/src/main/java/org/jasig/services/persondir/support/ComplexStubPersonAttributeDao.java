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
package org.jasig.services.persondir.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jasig.services.persondir.IPersonAttributeDao;
import org.jasig.services.persondir.IPersonAttributes;
import org.jasig.services.persondir.util.PatternHelper;


/**
 * Looks up the user's attribute Map in the backingMap. If using the {@link org.jasig.services.persondir.IPersonAttributeDao#getUserAttributes(Map)}
 * method the attribute value returned for the key {@link #getUsernameAttributeProvider()} will
 * be used as the key for the backingMap.
 * 
 * <br>
 * <br>
 * Configuration:
 * <table border="1" summary="">
 *     <tr>
 *         <th align="left">Property</th>
 *         <th align="left">Description</th>
 *         <th align="left">Required</th>
 *         <th align="left">Default</th>
 *     </tr>
 *     <tr>
 *         <td align="right" valign="top">backingMap</td>
 *         <td>
 *             Sets the backing map to use to return user attributes from. The backing map
 *             should have keys of type {@link String} which are the uid for the user. The
 *             values should be of type {@link Map} which follow the Map restrictions decribed
 *             by {@link org.jasig.services.persondir.IPersonAttributeDao#getUserAttributes(Map)}.
 *         </td>
 *         <td valign="top">No</td>
 *         <td valign="top">{@link Collections#EMPTY_MAP}</td>
 *     </tr>
 * </table>
 * 
 * @version $Revision$ $Date$
 */
public class ComplexStubPersonAttributeDao extends AbstractQueryPersonAttributeDao<String> {
    private Map<String, Map<String, List<Object>>> backingMap = Collections.emptyMap();
    private Set<String> possibleUserAttributeNames = Collections.emptySet();
    private String queryAttributeName = null;
    
    /**
     * Creates a new, empty, dao.
     */
    public ComplexStubPersonAttributeDao() {
    }
    
    /**
     * Creates a new DAO with the specified backing map.
     * @param backingMap The backingMap to call {@link #setBackingMap(Map)} with.
     */
    public ComplexStubPersonAttributeDao(final Map<String, Map<String, List<Object>>> backingMap) {
        this.setBackingMap(backingMap);
    }
    
    /**
     * Creates a new DAO with the specified backing map and query attribute.
     * @param queryAttributeName The queryAttributeName to call {@link #setQueryAttributeName(String)} with.
     * @param backingMap The backingMap to call {@link #setBackingMap(Map)} with.
     */
    public ComplexStubPersonAttributeDao(final String queryAttributeName, final Map<String, Map<String, List<Object>>> backingMap) {
        this.setQueryAttributeName(queryAttributeName);
        this.setBackingMap(backingMap);
    }
    
    public String getQueryAttributeName() {
        return this.queryAttributeName;
    }
    /**
     * Name of the attribute to look for as key into the backing map. If not set the value returned by
     * {@link #getUsernameAttributeProvider()} will be used.
     *
     * @param queryAttributeName query attribute name
     */
    public void setQueryAttributeName(final String queryAttributeName) {
        this.queryAttributeName = queryAttributeName;
    }

    public Map<String, Map<String, List<Object>>> getBackingMap() {
        return this.backingMap;
    }
    /**
     * The backing Map to use for queries, the outer map is keyed on the query attribute. The inner
     * Map is the set of user attributes to be returned for the query attribute.
     *
     * @param backingMap backing map
     */
    public void setBackingMap(final Map<String, Map<String, List<Object>>> backingMap) {
        if (backingMap == null) {
            this.backingMap = new HashMap<>();
            this.possibleUserAttributeNames = new HashSet<>();
        }
        else {
            this.backingMap = new LinkedHashMap<>(backingMap);
            this.initializePossibleAttributeNames();
        }
    }
    
    /* (non-Javadoc)
     * @see org.jasig.services.persondir.support.AbstractQueryPersonAttributeDao#getPossibleUserAttributeNames()
     */
    @Override
    @JsonIgnore
    public Set<String> getPossibleUserAttributeNames() {
        return this.possibleUserAttributeNames;
    }
    
    /* (non-Javadoc)
     * @see org.jasig.services.persondir.support.AbstractQueryPersonAttributeDao#getAvailableQueryAttributes()
     */
    @JsonIgnore
    @Override
    public Set<String> getAvailableQueryAttributes() {
        final IUsernameAttributeProvider usernameAttributeProvider = this.getUsernameAttributeProvider();
        final String usernameAttribute = usernameAttributeProvider.getUsernameAttribute();

        final Set list = new HashSet();
        list.add(usernameAttribute);
        
        return list;
    }

    /* (non-Javadoc)
     * @see org.jasig.services.persondir.support.AbstractQueryPersonAttributeDao#appendAttributeToQuery(java.lang.Object, java.lang.String, java.util.List)
     */
    @Override
    protected String appendAttributeToQuery(final String queryBuilder, final String dataAttribute, final List<Object> queryValues) {
        if (queryBuilder != null) {
            return queryBuilder;
        }
        
        final String keyAttributeName;
        if (this.queryAttributeName != null) {
            keyAttributeName = this.queryAttributeName;
        }
        else {
            final IUsernameAttributeProvider usernameAttributeProvider = this.getUsernameAttributeProvider();
            keyAttributeName = usernameAttributeProvider.getUsernameAttribute();
        }
        
        if (keyAttributeName.equals(dataAttribute)) {
            return String.valueOf(queryValues.get(0));
        }
        
        return null;
    }

    /* (non-Javadoc)
     * @see org.jasig.services.persondir.support.AbstractQueryPersonAttributeDao#getPeopleForQuery(java.lang.Object, java.lang.String)
     */
    @Override
    protected List<IPersonAttributes> getPeopleForQuery(final String seedValue, final String queryUserName) {
        if (seedValue != null && seedValue.contains(IPersonAttributeDao.WILDCARD)) {
            final Pattern seedPattern = PatternHelper.compilePattern(seedValue);
            
            final List<IPersonAttributes> results = new LinkedList<>();
            
            for (final Map.Entry<String, Map<String, List<Object>>> attributesEntry : this.backingMap.entrySet()) {
                final String attributesKey = attributesEntry.getKey();
                final Matcher keyMatcher = seedPattern.matcher(attributesKey);
                if (keyMatcher.matches()) {
                    final Map<String, List<Object>> attributes = attributesEntry.getValue();
                    if (attributes != null) {
                        final IPersonAttributes person = this.createPerson(null, queryUserName, attributes);
                        results.add(person);
                    }
                }
            }
            
            if (results.size() == 0) {
                return null;
            }
            
            return results;
        }
        
        final Map<String, List<Object>> attributes = this.backingMap.get(seedValue);
        
        if (attributes == null) {
            return null;
        }

        final IPersonAttributes person = this.createPerson(seedValue, queryUserName, attributes);
        final List list = new ArrayList();
        list.add(person);

        return list;
    }

    private IPersonAttributes createPerson(final String seedValue, final String queryUserName, final Map<String, List<Object>> attributes) {
        final IPersonAttributes person;
        final String userNameAttribute = this.getConfiguredUserNameAttribute();
        if (this.isUserNameAttributeConfigured() && attributes.containsKey(userNameAttribute)) {
            // Option #1:  An attribute is named explicitly in the config, 
            // and that attribute is present in the results from LDAP;  use it
            person = new AttributeNamedPersonImpl(userNameAttribute, attributes);
        } else if (queryUserName != null) {
            // Option #2:  Use the userName attribute provided in the query 
            // parameters.  (NB:  I'm not entirely sure this choice is 
            // preferable to Option #3.  Keeping it because it most closely 
            // matches the legacy behavior there the new option -- Option #1 
            // -- doesn't apply.  ~drewwills)
            person = new NamedPersonImpl(queryUserName, attributes);
        } else {
            // Option #3:  Create the IPersonAttributes doing a best-guess 
            // at a userName attribute
            if (seedValue != null && userNameAttribute.equals(this.queryAttributeName)) {
                person = new NamedPersonImpl(seedValue, attributes);
            }
            else {
                person = new AttributeNamedPersonImpl(userNameAttribute, attributes);
            }
        }

        return person;
    }

    /**
     * Compute the set of attribute names that map to a value for at least one
     * user in our backing map and store it as the instance variable 
     * possibleUserAttributeNames.
     */
    private void initializePossibleAttributeNames() {
        final Set<String> possibleAttribNames = new LinkedHashSet<>();
        
        for (final Map<String, List<Object>> attributeMapForSomeUser : this.backingMap.values()) {
            final Set<String> keySet = attributeMapForSomeUser.keySet();
            possibleAttribNames.addAll(keySet);
        }
        
        this.possibleUserAttributeNames = Collections.unmodifiableSet(possibleAttribNames);
    }
}

