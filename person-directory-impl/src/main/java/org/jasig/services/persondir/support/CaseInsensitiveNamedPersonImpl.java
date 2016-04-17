/**
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.services.persondir.support;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Custom IPersonAttributes that uses a case insensitive Map to hide attribute name case.  The attribute names
 * remain in case-specific form.
 */
public class CaseInsensitiveNamedPersonImpl extends NamedPersonImpl {
    private static final long serialVersionUID = 1L;

    public CaseInsensitiveNamedPersonImpl(final String userName, final Map<String, List<Object>> attributes) {
        super(userName, attributes);
    }

    /* (non-Javadoc)
     * @see org.jasig.services.persondir.support.BasePersonImpl#createImmutableAttributeMap(int)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map<String, List<Object>> createImmutableAttributeMap(final int size) {
        return new TreeMap(String.CASE_INSENSITIVE_ORDER);
    }
}
