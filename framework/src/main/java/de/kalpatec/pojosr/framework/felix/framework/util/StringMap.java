/* 
 * Copyright 2011 Karl Pauls karlpauls@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.kalpatec.pojosr.framework.felix.framework.util;

import java.util.*;

/**
 * Simple utility class that creates a map for string-based keys. This map can
 * be set to use case-sensitive or case-insensitive comparison when searching
 * for the key. Any keys put into this map will be converted to a
 * <tt>String</tt> using the <tt>toString()</tt> method, since it is only
 * intended to compare strings.
 **/
public class StringMap implements Map<String, Object> {
    private TreeMap<String, Object> m_map;

    public StringMap() {
        this(true);
    }

    public StringMap(boolean caseSensitive) {
        m_map = new TreeMap<>(new StringComparator(caseSensitive));
    }

    public StringMap(Map<String, ?> map, boolean caseSensitive) {
        this(caseSensitive);
        putAll(map);
    }

    public void clear() {
        m_map.clear();
    }

    public boolean containsKey(Object arg0) {
        return m_map.containsKey(arg0);
    }

    public boolean containsValue(Object arg0) {
        return m_map.containsValue(arg0);
    }

    public Set<Entry<String, Object>> entrySet() {
        return m_map.entrySet();
    }

    public Object get(Object arg0) {
        return m_map.get(arg0);
    }

    public boolean isCaseSensitive() {
        return ((StringComparator) m_map.comparator()).isCaseSensitive();
    }

    public boolean isEmpty() {
        return m_map.isEmpty();
    }

    public Set<String> keySet() {
        return m_map.keySet();
    }

    public Object put(String key, Object value) {
        return m_map.put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> map) {
        for (Iterator<?> it = map
                .entrySet().iterator(); it.hasNext();) {
            @SuppressWarnings("rawtypes")
            Map.Entry entry = (Map.Entry) it.next();
            put((String) entry.getKey(), entry.getValue());
        }
    }

    public Object remove(Object arg0) {
        return m_map.remove(arg0);
    }

    public void setCaseSensitive(boolean b) {
        if (isCaseSensitive() != b) {
            TreeMap<String, Object> map = new TreeMap<>(new StringComparator(b));
            map.putAll(m_map);
            m_map = map;
        }
    }

    public int size() {
        return m_map.size();
    }

    public String toString() {
        return m_map.toString();
    }

    public Collection<Object> values() {
        return m_map.values();
    }

}