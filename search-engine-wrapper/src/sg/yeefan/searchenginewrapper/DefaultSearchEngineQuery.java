/*
 * DefaultSearchEngineQuery.java
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
 * A default search engine query.
 *
 * @author Tan Yee Fan
 * @see SearchEngineQuery
 * @see SearchEngineClient
 */
public class DefaultSearchEngineQuery extends SearchEngineQuery {
	/** The label of the query. */
	private String label;

	/** The query string. */
	private String query;

	/** The result start index of the query. */
	private long startIndex;

	/**
	 * Constructor.
	 */
	public DefaultSearchEngineQuery() {
		super();
		this.label = null;
		this.query = null;
		this.startIndex = 1;
	}

	/**
	 * Returns the label of the query.
	 */
	public String getLabel() {
		return this.label;
	}

	/**
	 * Returns the query string.
	 */
	public String getQuery() {
		return this.query;
	}

	/**
	 * Returns the result start index of the request.
	 */
	public long getStartIndex() {
		return this.startIndex;
	}

	/**
	 * Sets the label of the query.
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Sets the query string.
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	/**
	 * Sets the result start index of the request. The start index is the
	 * index of the first result and is 1-based. To obtain results starting
	 * from the kth result, set the start index to k.
	 */
	public void setStartIndex(long startIndex) {
		this.startIndex = startIndex;
	}
}

