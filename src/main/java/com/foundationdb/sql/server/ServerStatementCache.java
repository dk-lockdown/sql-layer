/**
 * Copyright (C) 2009-2013 Akiban Technologies, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.foundationdb.sql.server;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cache of parsed statements.
 */
public class ServerStatementCache<T extends ServerStatement>
{
    private final CacheCounters counters;
    private final Cache<T> cache;

    static class Cache<T> extends LinkedHashMap<String,T> {
        private int capacity;

        public Cache(int capacity) {
            super(capacity, 0.75f, true);
            this.capacity = capacity;
        }
        
        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return (size() > capacity); 
        }  
    }

    public ServerStatementCache(CacheCounters counters, int size) {
        this.counters = counters;
        this.cache = new Cache<>(size);
    }

    public int getCapacity() {
        return cache.getCapacity();
    }

    public synchronized void setCapacity(int capacity) {
        cache.setCapacity(capacity);
        cache.clear();
    }

    public synchronized T get(String sql) {
        T entry = cache.get(sql);
        if (entry != null)
            counters.incrementHits();
        else
            counters.incrementMisses();
        return entry;
    }

    public synchronized void put(String sql, T stmt) {
        // TODO: Count number of times this is non-null, meaning that
        // two threads computed the same statement?
        cache.put(sql, stmt);
    }

    public synchronized void invalidate() {
        cache.clear();
    }

    public synchronized void reset() {
        cache.clear();
    }
}
