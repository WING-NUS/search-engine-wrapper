/*
 * SearchEngineQuery.java
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

package sg.yeefan.searchenginewrapper;

/**
 * A search engine query. It is either user specified, or returned as part of
 * search engine results for obtaining the next set of results. An instance of
 * this class is to be supplied to a search engine client.
 *
 * @author Tan Yee Fan
 * @see DefaultSearchEngineQuery
 * @see SearchEngineClient
 * @see SearchEngineResults
 */
public abstract class SearchEngineQuery {
	/** The registration key for using the search engine client, if
	    required. */
	private String key;

	/**
	 * Constructor.
	 */
	protected SearchEngineQuery() {
		this.key = null;
	}

	/**
	 * Returns the registration key for using the search engine client, if
	 * required.
	 */
	public String getKey() {
		return this.key;
	}

	/**
	 * Sets the registration key for using the search engine client, if
	 * required.
	 */
	public void setKey(String key) {
		this.key = key;
	}
}

