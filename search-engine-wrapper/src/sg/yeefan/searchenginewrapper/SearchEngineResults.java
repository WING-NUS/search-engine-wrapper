/*
 * SearchEngineResults.java
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

import java.math.BigInteger;
import java.util.Arrays;

/**
 * The search engine results for a query. It encapsulates the following
 * information:
 * <ul>
 * <li>The label of the query as provided to the search engine client.</li>
 * <li>The query string as provided to the search engine client.</li>
 * <li>The (estimated) total number of results matching the query. Note that
 * this is the hit count returned by the search engine and not the number of
 * individual results contained in this results object.</li>
 * <li>The index of the first individual result, or start index.</li>
 * <li>The individual results for the query. Each individual result consists of
 * a URL, a title, and a snippet.</li>
 * <li>The start time of the execution of this query.</li>
 * <li>The end time of the execution of this query.</li>
 * <li>Whether a fatal error occurred during the query.</li>
 * </ul>
 * To elaborate on the total number of results and start index, consider a user
 * performing a manual query on a search engine through a standard web browser.
 * Typically the results page will contain a line like "Results 1 - 10 of about
 * 600". Here, the index of the first result is 1 and the total number of
 * results, or hit count, is estimated to be about 600.
 *
 * @author Tan Yee Fan
 * @see SearchEngineClient
 * @see SearchEngineResult
 * @see SearchEngineWrapper
 */
public class SearchEngineResults {
	/** The label of the query. */	
	private String label;

	/** The query string. */
	private String query;

	/** The total number of results matching the query (hit count). */
	private BigInteger totalResultsBig;

	/** The index of the first result. */
	private long startIndex;

	/** The search engine results. */
	private SearchEngineResult[] results;

	/** The start time of the query. */
	private long startTime;

	/** The end time of the query. */
	private long endTime;

	/** Whether a fatal error occurred. */
	private boolean fatalError;

	/** The query for getting additional results, if it exists. */
	private SearchEngineQuery nextQuery;

	/**
	 * Constructor.
	 */
	public SearchEngineResults() {
		this.label = null;
		this.query = null;
		this.totalResultsBig = BigInteger.ZERO;
		this.startIndex = 0;
		this.results = new SearchEngineResult[0];
		this.startTime = 0;
		this.endTime = 0;
		this.fatalError = false;
		this.nextQuery = null;
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
	 * Returns the total number of results (hit count) matching the query.
	 */
	public BigInteger getTotalResultsBig() {
		return this.totalResultsBig;
	}

	/**
	 * Returns the total number of results (hit count) matching the query.
	 * If this value is greater than <code>Long.MAX_VALUE</code>, then
	 * <code>Long.MAX_VALUE</code> is returned.
	 */
	public long getTotalResults() {
		BigInteger longMinValueBig = BigInteger.valueOf(Long.MIN_VALUE);
		BigInteger longMaxValueBig = BigInteger.valueOf(Long.MAX_VALUE);
		BigInteger clippedValueBig = this.totalResultsBig.max(longMinValueBig).min(longMaxValueBig);
		return clippedValueBig.longValue();
	}

	/**
	 * Returns the index of the first result.
	 */
	public long getStartIndex() {
		return this.startIndex;
	}

	/**
	 * Returns the number of results.
	 */
	public long getNumResults() {
		return this.results.length;
	}

	/**
	 * Returns the search engine results.
	 */
	public SearchEngineResult[] getResults() {
		return Arrays.copyOf(this.results, this.results.length);
	}

	/**
	 * Returns the start time of the query.
	 */
	public long getStartTime() {
		return this.startTime;
	}

	/**
	 * Returns the end time of the query.
	 */
	public long getEndTime() {
		return this.endTime;
	}

	/**
	 * Returns the elapsed time of the query.
	 */
	public long getElapsedTime() {
		return (this.endTime - this.startTime);
	}

	/**
	 * Returns whether a fatal error occurred.
	 */
	public boolean hasFatalError() {
		return this.fatalError;
	}

	/**
	 * Returns the query for getting additional results, if it exists.
	 */
	public SearchEngineQuery getNextQuery() {
		return this.nextQuery;
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
	 * Sets the total number of results matching the query (hit count).
	 */
	public void setTotalResultsBig(BigInteger totalResultsBig) {
		this.totalResultsBig = totalResultsBig;
	}

	/**
	 * Sets the total number of results matching the query (hit count).
	 */
	public void setTotalResults(long totalResults) {
		this.totalResultsBig = BigInteger.valueOf(totalResults);
	}

	/**
	 * Sets the start index of the query.
	 */
	public void setStartIndex(long startIndex) {
		this.startIndex = startIndex;
	}

	/**
	 * Sets the search engine results.
	 */
	public void setResults(SearchEngineResult[] results) {
		this.results = Arrays.copyOf(results, results.length);
	}

	/**
	 * Sets the start time of the query.
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	/**
	 * Sets the end time of the query.
	 */
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	/**
	 * Sets whether a fatal error occurred.
	 */
	public void setFatalError(boolean fatalError) {
		this.fatalError = fatalError;
	}

	/**
	 * Sets the query for getting additional results, if it exists. If no
	 * more results are available, then {@code null} is returned.
	 */
	public void setNextQuery(SearchEngineQuery nextQuery) {
		this.nextQuery = nextQuery;
	}

	/**
	 * Returns a hash code value for the object.
	 */
	@Override
	public int hashCode() {
		return ((this.label != null) ? this.label.hashCode() : 0);
	}

	/**
	 * Returns whether the given search engine result is equal to this one.
	 * Equality occurs when the URLs, titles, and snippets are all the same.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof SearchEngineResults))
			return false;
		SearchEngineResults results = (SearchEngineResults)obj;
		return ((this.label == results.label) || (this.label != null && this.label.equals(results.label))) &&
		  ((this.query == results.query) || (this.query != null && this.query.equals(results.query))) &&
		  ((this.totalResultsBig == results.totalResultsBig) || (this.totalResultsBig != null && this.totalResultsBig.equals(results.totalResultsBig))) &&
		  (this.startIndex == results.startIndex) &&
		  (this.startTime == results.startTime) &&
		  (this.endTime == results.endTime) &&
		  Arrays.equals(this.results, results.results);
	}
}

