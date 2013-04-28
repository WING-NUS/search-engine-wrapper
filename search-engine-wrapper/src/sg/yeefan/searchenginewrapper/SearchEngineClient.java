/*
 * SearchEngineClient.java
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
 * Interface for a search engine client.
 * <p>
 * Sample usage:
 * <pre><code>
 * SearchEngineClient client = SearchEngineClientFactory.getClient(name);
 * DefaultSearchEngineQuery defaultQuery = new DefaultSearchEngineQuery();
 * // Set the query parameters...
 * SearchEngineQuery query = defaultQuery;
 * int maxNumQueries = 5;
 * for (int i = 0; i &lt; maxNumQueries; i++) {
 *     results = client.getResults(query);
 *     // Process results...
 *     query = results.getNextQuery();
 *     if (query == null)
 *         break;
 * }
 * </code></pre>
 * Implementation notes: A class implementing the {@code SearchEngineClient}
 * interface should contain a no-argument constructor.
 *
 * @author Tan Yee Fan
 * @see SearchEngineClientFactory
 * @see SearchEngineWrapper
 */
public interface SearchEngineClient {
	/**
	 * Makes a query to the search engine and returns its results.
	 *
	 * @param query The search engine query.
	 * @throws SearchEngineFatalException If a fatal error occurred during
	 *         the querying.
	 * @throws SearchEngineQuotaException If the quota for the
	 *         (rate-limited) allowable number of queries has been exceeded.
	 * @throws SearchEngineException If any other error occurred during
	 *         the querying.
	 */
	public SearchEngineResults getResults(SearchEngineQuery query) throws SearchEngineException;
}

