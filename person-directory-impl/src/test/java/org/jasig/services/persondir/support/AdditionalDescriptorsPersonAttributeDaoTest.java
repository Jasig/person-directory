/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jasig.services.persondir.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jasig.services.persondir.IPersonAttributes;

import junit.framework.TestCase;

public class AdditionalDescriptorsPersonAttributeDaoTest extends TestCase {
    
    private static final String USERNAME = "user";
    private static final String USERNAME_ATTRIBUTE = "username";
    private static final IUsernameAttributeProvider UAP = new SimpleUsernameAttributeProvider(USERNAME_ATTRIBUTE);
    private static final ICurrentUserProvider CUP = new ICurrentUserProvider() {
        public String getCurrentUserName() {
            return USERNAME;
        }
    };
    private static final String ATTRIBUTE_NAME = "attribute";
    private static final List<Object> ATTRIBUTE_VALUES = Arrays.asList(new Object[] {"foo", "bar"});
    

    /*
     * Public API.
     */
    
    public void testGetAvailableQueryAttributes() {
        
        final AdditionalDescriptors ad = new AdditionalDescriptors();
        ad.setName(USERNAME);
        
        final AdditionalDescriptorsPersonAttributeDao adpad = new AdditionalDescriptorsPersonAttributeDao();
        adpad.setUsernameAttributeProvider(UAP);
        adpad.setCurrentUserProvider(CUP);
        adpad.setDescriptors(ad);
        
        TestCase.assertEquals(adpad.getAvailableQueryAttributes(), Collections.singleton(USERNAME_ATTRIBUTE));
        
    }
    
    public void testGetPeopleWithMultivaluedAttributes() {
        
        final AdditionalDescriptors ad = new AdditionalDescriptors();
        ad.setName(USERNAME);
        ad.setAttributeValues(ATTRIBUTE_NAME, ATTRIBUTE_VALUES);
        
        final AdditionalDescriptorsPersonAttributeDao adpad = new AdditionalDescriptorsPersonAttributeDao();
        adpad.setUsernameAttributeProvider(UAP);
        adpad.setCurrentUserProvider(CUP);
        adpad.setDescriptors(ad);
        
        final Map<String,List<Object>> query = new HashMap<>();
        query.put(ATTRIBUTE_NAME, ATTRIBUTE_VALUES);

        Set<IPersonAttributes> rslt = adpad.getPeopleWithMultivaluedAttributes(query);
        TestCase.assertNull(rslt);

        query.put(USERNAME_ATTRIBUTE, Collections.singletonList((Object) USERNAME));

        rslt = adpad.getPeopleWithMultivaluedAttributes(query);
        TestCase.assertNotNull(rslt);
        TestCase.assertTrue(rslt.size() == 1);
        TestCase.assertTrue(rslt.contains(ad));

    }

}
