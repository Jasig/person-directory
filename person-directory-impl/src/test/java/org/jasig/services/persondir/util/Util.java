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
package org.jasig.services.persondir.util;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eric Dalquist
 * @version $Revision$
 */
public class Util {
    /**
     * Utility for making a mutable list of objects
     */
    public static List<Object> list(final Object... objects) {
        if (objects == null) {
            final List<Object> list = new ArrayList<>(1);
            list.add(null);
            return list;
        }

        final List<Object> list = new ArrayList<>(objects.length);

        for (final Object obj : objects) {
            list.add(obj);
        }

        return list;
    }

    /**
     * Utility for making a mutable list of Ts
     */
    public static <T> List<T> genList(final T... objects) {
        if (objects == null) {
            final List<T> list = new ArrayList<>(1);
            list.add(null);
            return list;
        }

        final List<T> list = new ArrayList<>(objects.length);

        for (final T obj : objects) {
            list.add(obj);
        }

        return list;
    }
}
