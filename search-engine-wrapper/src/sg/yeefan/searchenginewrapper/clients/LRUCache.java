/*
 * LRUCache.java
 *
 * Copyright (C) Tan Yee Fan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sg.yeefan.searchenginewrapper.clients;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A least-recently-used cache.
 *
 * @author Tan Yee Fan
 */
class LRUCache<K, V> extends LinkedHashMap<K, V> {
	/** Load factor. */
	private static final float LOAD_FACTOR = 0.75f;

	/** Capacity. */
	private int capacity;

	/**
	 * Creates a new least-recently-used cache.
	 *
	 * @param capacity Capacity of the cache.
	 */
	public LRUCache(int capacity) {
		super((int)Math.ceil(capacity / LOAD_FACTOR) + 1, LOAD_FACTOR, true);
		this.capacity = capacity;
	}

	/**
	 * Returns whether to remove the eldest entry.
	 *
	 * @param eldest Eldest entry.
	 */
	@Override
	protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
		return (size() > this.capacity);
	}
}

