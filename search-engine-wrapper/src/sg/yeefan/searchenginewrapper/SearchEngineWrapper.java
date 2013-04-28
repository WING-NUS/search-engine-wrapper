/*
 * SearchEngineWrapper.java
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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import sg.yeefan.io.CharsetDetectStreamReader;

/**
 * A wrapper for a search engine. This wrapper reads from an input file
 * containing the query strings, and writes to an output file containing the
 * search engine results.
 * <p>
 * The following gives a sample usage of this class:
 * <pre><code>
 * SearchEngineWrapper wrapper = new SearchEngineWrapper();
 * wrapper.setKeys(keys);
 * wrapper.setInputFile(inputFile);
 * wrapper.setOutputFile(outputFile);
 * wrapper.setClient(client);
 * wrapper.setNumResults(numResults);
 * wrapper.run();
 * </code></pre>
 * The input file consists of lines of the form <code>label: query</code>, as
 * illustrated in the following example:
 * <pre><code>
 * 0001: "tan yee fan"
 * 0002: sensei's library
 * 0003: java tutorial
 * </code></pre>
 * The output file is in XML format. The root element is
 * <code>&lt;resultslist&gt;</code>, which contains a
 * <code>&lt;results&gt;</code> element for each query in the input file. An
 * example of the output XML file is shown below:
 * <pre><code>
 * &lt;?xml version="1.0" encoding="utf-8"?&gt;
 * 
 * &lt;resultslist&gt;
 * 
 * &lt;results&gt;
 * &lt;label&gt;0001&lt;/label&gt;
 * &lt;query&gt;"tan yee fan"&lt;/query&gt;
 * &lt;totalresults&gt;468&lt;/totalresults&gt;
 * &lt;startindex&gt;1&lt;/startindex&gt;
 * &lt;starttime&gt;1269907585796&lt;/starttime&gt;
 * &lt;endtime&gt;1269907587203&lt;/endtime&gt;
 * &lt;elapsedtime&gt;1407&lt;/elapsedtime&gt;
 * &lt;fatalerror&gt;0&lt;/fatalerror&gt;
 * &lt;result&gt;
 * &lt;url&gt;http://www.comp.nus.edu.sg/~tanyeefa/&lt;/url&gt;
 * &lt;title&gt;TAN Yee Fan&lt;/title&gt;
 * &lt;snippet&gt;Webpage of Tan Yee Fan.&lt;/snippet&gt;
 * &lt;/result&gt;
 * ... more &lt;result&gt; elements ...
 * &lt;/results&gt;
 * 
 * ... more &lt;results&gt; elements ...
 * 
 * &lt;/resultslist&gt;
 * </code></pre>
 * Most fields in the <code>&lt;results&gt;</code> element should be self
 * explanatory. The times are given in milliseconds; and
 * <code>&lt;starttime&gt;</code> and <code>&lt;endtime&gt;</code> refer to the
 * number of milliseconds past the midnight of 1970 January 1 (UTC) as returned
 * by <code>System.currentTimeMillis()</code>.
 * <p>
 * As search engines place restrictions on the number of results they return per
 * request, the search engine wrapper automatically issues as many requests as
 * needed to obtain the required number of results for a particular query
 * string. An exponential backoff delay mechanism is implemented for temporary
 * search engine failures before reissuing the query. The first failure causes a
 * waiting time of <code>waitIntervalFirst</code> seconds, and every subsequent
 * failure will cause the waiting time to be multiplied by
 * <code>waitIntervalMultiplier</code>, with the maximum waiting time capped at
 * <code>waitIntervalMax</code> seconds. If the search engine requires a
 * registration key, this wrapper can manage a pool of keys to be rotated. In
 * this case, the key will be switched to the next key if the number of
 * consecutive waits for a key reaches <code>maxNumWaits</code>, or immediately
 * if the search engine returns a quota exceeded error. The parameters
 * <code>waitIntervalFirst</code>, <code>waitIntervalMultiplier</code>,
 * <code>waitIntervalMax</code>, and <code>maxNumWaits</code> can be set in this
 * class.
 *
 * @author Tan Yee Fan
 * @see SearchEngineClient
 * @see SearchEngineResults
 */
public class SearchEngineWrapper {
	/** Default character encoding for files. */
	private static final String DEFAULT_ENCODING = "UTF-8";

	/** The PrintWriter for standard output. */
	private PrintStream stdout;

	/** The PrintWriter for standard error. */
	private PrintStream stderr;

	/** The random number generator. */
	private Random random;

	/** The input file containing the query strings. */
	private String inputFile;

	/** The input file encoding. */
	private String inputEncoding;

	/** The output file containing the search engine results. */
	private String outputFile;

	/** The output file encoding. */
	private String outputEncoding;

	/** The search engine client. */
	private SearchEngineClient client;

	/** The number of search results requested per query. */
	private long numResults;

	/** The maximum number of requests per query, excluding retries. */
	private int maxNumRequests;

	/** The pool of registration keys. */
	private String[] keys;

	/** The quota availibility of the registration keys. */
	private boolean[] keyHasQuotas;

	/** The index of the current key used. */
	private int keyIndex;

	/** The interval of the first wait in seconds. */
	private int waitIntervalFirst;

	/** The multipler of the wait interval. */
	private int waitIntervalMultiplier;
	
	/** The maximum wait interval in seconds. */
	private int waitIntervalMax;

	/** The maximum number of waits before changing the key. */
	private int maxNumWaits;

	/** Whether to combine results for the same query. */
	private boolean combineSameQueryResults;

	/**
	 * Constructor.
	 */
	public SearchEngineWrapper() {
		try {
			this.stdout = new PrintStream(System.out, true, DEFAULT_ENCODING);
			this.stderr = new PrintStream(System.err, true, DEFAULT_ENCODING);
		}
		catch (UnsupportedEncodingException e) {
			// Default encoding must be supported by all Java implementations.
			throw new Error(e);
		}
		this.random = new Random();
		this.inputFile = null;
		this.inputEncoding = null;
		this.outputFile = null;
		this.outputEncoding = null;
		this.client = null;
		this.numResults = 10;
		this.maxNumRequests = 10;
		this.keys = null;
		this.keyHasQuotas = null;
		this.keyIndex = 0;
		this.waitIntervalFirst = 5;
		this.waitIntervalMultiplier = 2;
		this.waitIntervalMax = 1800;
		this.maxNumWaits = 9;
		this.combineSameQueryResults = true;
	}

	/**
	 * Returns the input file containing the query strings.
	 */
	public String getInputFile() {
		return this.inputFile;
	}

	/**
	 * Returns the input file encoding.
	 */
	public String getInputEncoding() {
		return this.inputEncoding;
	}

	/**
	 * Returns the output file containing the search engine results.
	 */
	public String getOutputFile() {
		return this.outputFile;
	}

	/**
	 * Returns the output file encoding.
	 */
	public String getOutputEncoding() {
		return this.outputEncoding;
	}

	/**
	 * Returns the search engine client.
	 */
	public SearchEngineClient getClient() {
		return this.client;
	}

	/**
	 * Returns the number of search results requested per query.
	 */
	public long getNumResults() {
		return this.numResults;
	}

	/**
	 * Returns the maximum number of requests per query, excluding retries.
	 */
	public int getMaxNumRequests() {
		return this.maxNumRequests;
	}

	/**
	 * Returns the pool of registration keys.
	 */
	public String[] getKeys() {
		return Arrays.copyOf(this.keys, this.keys.length);
	}

	/**
	 * Returns the interval of the first wait in seconds.
	 */
	public int getWaitIntervalFirst() {
		return this.waitIntervalFirst;
	}

	/**
	 * Returns the multipler of the wait interval.
	 */
	public int getWaitIntervalMultiplier() {
		return this.waitIntervalMultiplier;
	}

	/**
	 * Returns the maximum wait interval in seconds.
	 */
	public int getWaitIntervalMax() {
		return this.waitIntervalMax;
	}

	/**
	 * Returns the maximum number of waits before changing the key.
	 */
	public int getMaxNumWaits() {
		return this.maxNumWaits;
	}

	/**
	 * Returns whether to combine results from multiple calls to the search
	 * engine for the same query.
	 */
	public boolean getCombineSameQueryResults() {
		return this.combineSameQueryResults;
	}

	/**
	 * Sets the input file containing the query strings.
	 */
	public void setInputFile(String inputFile) {
		this.inputFile = inputFile;
	}

	/**
	 * Sets the input file encoding.
	 */
	public void setInputEncoding(String inputEncoding) {
		this.inputEncoding = inputEncoding;
	}

	/**
	 * Sets the output file containing the search engine results.
	 */
	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	/**
	 * Sets the output file encoding.
	 */
	public void setOutputEncoding(String outputEncoding) {
		this.outputEncoding = outputEncoding;
	}

	/**
	 * Sets the search engine client.
	 */
	public void setClient(SearchEngineClient client) {
		this.client = client;
	}

	/**
	 * Sets the number of search results requested per query.
	 */
	public void setNumResults(long numResults) {
		if (numResults <= 0)
			throw new IllegalArgumentException("Number of results must be positive.");
		this.numResults = numResults;
	}

	/**
	 * Sets the maximum number of requests per query, excluding retries.
	 */
	public void setMaxNumRequests(int maxNumRequests) {
		this.maxNumRequests = maxNumRequests;
	}

	/**
	 * Sets the pool of registration keys.
	 */
	public void setKeys(String[] keys) {
		if (keys != null) {
			int numKeys = keys.length;
			if (numKeys == 0)
				throw new IllegalArgumentException("Number of keys must be positive.");
			this.keys = Arrays.copyOf(keys, numKeys);
			this.keyHasQuotas = new boolean[numKeys];
			for (int i = 0; i < numKeys; i++)
				this.keyHasQuotas[i] = true;
			this.keyIndex = this.random.nextInt(numKeys);
		}
		else {
			this.keys = null;
			this.keyHasQuotas = null;
			this.keyIndex = 0;
		}
	}

	/**
	 * Sets the interval of the first wait in seconds.
	 */
	public void setWaitIntervalFirst(int waitIntervalFirst) {
		if (waitIntervalFirst < 0)
			throw new IllegalArgumentException("Interval of first wait must be nonnegative.");
		this.waitIntervalFirst = waitIntervalFirst;
	}

	/**
	 * Sets the multipler of the wait interval.
	 */
	public void setWaitIntervalMultiplier(int waitIntervalMultiplier) {
		if (waitIntervalMultiplier < 1)
			throw new IllegalArgumentException("Wait interval multiplier must be positive.");
		this.waitIntervalMultiplier = waitIntervalMultiplier;
	}

	/**
	 * Sets the maximum wait interval in seconds.
	 */
	public void setWaitIntervalMax(int waitIntervalMax) {
		if (waitIntervalMax < 0)
			throw new IllegalArgumentException("Maximum wait interval must be nonnegative.");
		this.waitIntervalMax = waitIntervalMax;
	}

	/**
	 * Sets the maximum number of waits before changing the key.
	 */
	public void setMaxNumWaits(int maxNumWaits) {
		if (maxNumWaits < 0)
			throw new IllegalArgumentException("Maximum number of waits must be nonnegative.");
		this.maxNumWaits = maxNumWaits;
	}

	/**
	 * Sets whether to combine results from multiple calls to the search
	 * engine for the same query.
	 */
	public void setCombineSameQueryResults(boolean combineSameQueryResults) {
		this.combineSameQueryResults = combineSameQueryResults;
	}

	/**
	 * Returns the search engine results for a single query. If the query
	 * fails due to a non-fatal error, the same query will be reissued after
	 * a delay, and repeated until the query is successful. If the query
	 * fails due to a fatal error, the query will not be reissued and the
	 * <code>fatalError</code> field of the results will be set to
	 * <code>true</code>.
	 */
	private SearchEngineResults getResults(String label, String queryString, long startIndex, SearchEngineQuery query) {
		long startTime = System.currentTimeMillis();
		int numWaits = 0;
		int waitInterval = this.waitIntervalFirst;
		while (true) {
			if (this.client instanceof KeyedSearchEngineClient && this.keys != null)
				query.setKey(this.keys[this.keyIndex]);
			SearchEngineResults results = null;
			boolean hasQuotaException = false;
			boolean hasException = false;
			try {
				results = this.client.getResults(query);
			}
			catch (SearchEngineFatalException e) {
				e.printStackTrace(this.stderr);
				// Return failure.
				long endTime = System.currentTimeMillis();
				results = new SearchEngineResults();
				results.setLabel(label);
				results.setQuery(queryString);
				results.setTotalResultsBig(BigInteger.ZERO);
				results.setStartIndex(startIndex);
				results.setResults(new SearchEngineResult[0]);
				results.setStartTime(startTime);
				results.setEndTime(endTime);
				results.setFatalError(true);
				return results;
			}
			catch (SearchEngineQuotaException e) {
				e.printStackTrace(this.stderr);
				hasQuotaException = true;
				hasException = true;
			}
			catch (SearchEngineException e) {
				e.printStackTrace(this.stderr);
				hasException = true;
			}
			if (this.client instanceof KeyedSearchEngineClient)
				this.keyHasQuotas[this.keyIndex] = !hasQuotaException;
			if (!hasException) {
				// Return successful.
				long endTime = System.currentTimeMillis();
				results.setStartTime(startTime);
				results.setEndTime(endTime);
				return results;
			}
			boolean toChangeKey = ((hasQuotaException || numWaits >= this.maxNumWaits) &&
			  this.client instanceof KeyedSearchEngineClient && this.keys != null && this.keys.length > 1);
			boolean toWait = true;
			if (toChangeKey) {
				this.stderr.println("Rotating key...");
				this.keyIndex++;
				if (this.keyIndex == this.keys.length)
					this.keyIndex = 0;
				if (this.keyHasQuotas[this.keyIndex]) {
					toWait = false;
					waitInterval = this.waitIntervalFirst;
					numWaits = 0;
				}
			}
			if (toWait) {
				this.stderr.println("Waiting for " + waitInterval + " seconds...");
				try {
					Thread.sleep(waitInterval * 1000);
				}
				catch (InterruptedException e) {
					// Do nothing.
				}
				waitInterval *= this.waitIntervalMultiplier;
				if (waitInterval > this.waitIntervalMax)
					waitInterval = this.waitIntervalMax;
				numWaits++;
			}
		}
	}

	/**
	 * Combines a list of <code>SearchEngineResults</code> objects into one
	 * single <code>SearchEngineResults</code> object. It is assumed that
	 * the input list contains at least one <code>SearchEngineResults</code>
	 * object. The <code>results</code> array of the resultant object will
	 * be a concatenation of the <code>results</code> arrays in the list.
	 * The <code>label</code>, <code>queryString</code>,
	 * <code>startIndex</code>, and <code>startTime</code> fields of the
	 * resultant object will be copied from the corresponding fields of the
	 * first object in the list, and the <code>endTime</code> field of the
	 * resultant object will be copied from the corresponding field of the
	 * last object in the list. The <code>fatalError</code> field of the
	 * resultant object will be <code>true</code> if any object in the list
	 * has a <code>fatalError</code> field set to <code>true</code>. This
	 * implementation copies the <code>totalResults</code> field of the
	 * first object in the list to the resultant object, but it is unclear
	 * whether this is the most appropriate action. The input list is not
	 * checked for consistency or errors.
	 */
	private SearchEngineResults combineResults(List<SearchEngineResults> resultsList) {
		List<SearchEngineResult> resultList = new ArrayList<SearchEngineResult>();
		String label = null;
		String query = null;
		long startIndex = 0;
		long startTime = 0;
		long endTime = 0;
		BigInteger totalResultsBig = BigInteger.ZERO;
		boolean fatalError = false;
		boolean first = true;
		for (SearchEngineResults results: resultsList) {
			SearchEngineResult[] resultArray = results.getResults();
			for (int i = 0; i < resultArray.length; i++)
				resultList.add(resultArray[i]);
			if (first) {
				label = results.getLabel();
				query = results.getQuery();
				startIndex = results.getStartIndex();
				startTime = results.getStartTime();
				totalResultsBig = results.getTotalResultsBig();
				first = false;
			}
			endTime = results.getEndTime();
			fatalError = fatalError || results.hasFatalError();
		}
		SearchEngineResult[] resultArray = new SearchEngineResult[resultList.size()];
		resultList.toArray(resultArray);
		SearchEngineResults results = new SearchEngineResults();
		results.setResults(resultArray);
		results.setLabel(label);
		results.setQuery(query);
		results.setStartIndex(startIndex);
		results.setStartTime(startTime);
		results.setEndTime(endTime);
		results.setTotalResultsBig(totalResultsBig);
		results.setFatalError(fatalError);
		return results;
	}

	/**
	 * Runs the process of obtaining search engine results for the input
	 * query strings.
	 *
	 * @throws IOException If an error occurred when reading the input file
	 *         or writing the output file.
	 */
	public void run() throws IOException {
		BufferedReader reader;
		if (this.inputEncoding != null)
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.inputFile), inputEncoding));
		else
			reader = new BufferedReader(new CharsetDetectStreamReader(this.inputFile));
		String outputEncoding = this.outputEncoding;
		if (outputEncoding == null)
			outputEncoding = DEFAULT_ENCODING;
		ResultsXMLWriter writer = new ResultsXMLWriter(this.outputFile, outputEncoding);
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			String label;
			String queryString;
			int pos = line.indexOf(':');
			if (pos >= 0) {
				label = line.substring(0, pos).trim();
				queryString = line.substring(pos + 1).trim();
			}
			else {
				label = "";
				queryString = line;
			}
			if (queryString.length() == 0)
				this.stderr.println("Empty query in line \"" + line + "\", skipping...");
			else {
				if (label.length() > 0)
					this.stdout.println("Query [" + label + "]: " + queryString);
				else
					this.stdout.println("Query: " + queryString);
				long startIndex = 1;
				int numRequests = 0;
				DefaultSearchEngineQuery defaultQuery = new DefaultSearchEngineQuery();
				defaultQuery.setLabel(label);
				defaultQuery.setQuery(queryString);
				defaultQuery.setStartIndex(startIndex);
				SearchEngineQuery query = defaultQuery;
				List<SearchEngineResults> resultsList = new ArrayList<SearchEngineResults>();
				while (startIndex <= this.numResults && numRequests < this.maxNumRequests && query != null) {
					numRequests++;
					SearchEngineResults results = getResults(label, queryString, startIndex, query);
					resultsList.add(results);
					if (results.hasFatalError()) {
						this.stderr.println("Fatal error in query \"" + queryString + "\", not retrying...");
						break;
					}
					startIndex += results.getNumResults();
					query = results.getNextQuery();
				}
				if (this.combineSameQueryResults) {
					SearchEngineResults results = combineResults(resultsList);
					writer.writeResults(results);
				}
				else {
					for (SearchEngineResults results: resultsList)
						writer.writeResults(results);
				}
			}
		}
		reader.close();
		writer.close();
	}
}

