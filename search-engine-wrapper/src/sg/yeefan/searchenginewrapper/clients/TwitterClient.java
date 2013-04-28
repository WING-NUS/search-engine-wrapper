/*
 * TwitterClient.java
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import org.apache.commons.lang3.StringEscapeUtils;
import sg.yeefan.searchenginewrapper.KeyedSearchEngineClient;
import sg.yeefan.searchenginewrapper.SearchEngineException;
import sg.yeefan.searchenginewrapper.SearchEngineFatalException;
import sg.yeefan.searchenginewrapper.SearchEngineQuery;
import sg.yeefan.searchenginewrapper.DefaultSearchEngineQuery;
import sg.yeefan.searchenginewrapper.SearchEngineQuotaException;
import sg.yeefan.searchenginewrapper.SearchEngineResult;
import sg.yeefan.searchenginewrapper.SearchEngineResults;

/**
 * A search engine client for <a href="https://dev.twitter.com/">Twitter
 * Search</a>.
 * <p>
 * Twitter Search uses <a href="http://oauth.net/">OAuth</a> for authentication,
 * and as such requires four keys to operate:
 * <ul>
 * <li>{@code consumer_key}: OAuth consumer key.</li>
 * <li>{@code consumer_secret}: OAuth consumer secret.</li>
 * <li>{@code token_key}: OAuth access token key.</li>
 * <li>{@code token_secret}: OAuth access token secret.</li>
 * </ul>
 * In the query, the registration key should be supplied by concatenating these
 * keys together, using {@code "$"} as the delimiter:
 * <pre><code>
 * consumer_key + "$" + consumer_secret + "$" + token_key + "$" + token_secret
 * </code></pre>
 * <p>
 * Additional comments on using Twitter Search:
 * <ul>
 * <li>The start index in the query is ignored.</li>
 * <li>The title in each search engine result contains the user name and screen
 * name, in the form {@code screen_name (user_name)}.</li>
 * <li>The total results field in the returned search engine results is set to
 * {@code Long.MAX_VALUE}.</li>
 * </ul>
 *
 * @author Tan Yee Fan
 */
public class TwitterClient implements KeyedSearchEngineClient {
	/**
	 * Constructor.
	 */
	public TwitterClient() {
	}

	// Classes for binding JSON data to Java objects.

	private static class Response {
		private Status[] statuses;
		private SearchMetadata searchMetadata;

		public Response() {
			this.statuses = new Status[0];
			this.searchMetadata = new SearchMetadata();
		}

		public Status[] getStatuses() {
			return this.statuses;
		}

		public void setStatuses(Status[] statuses) {
			if (statuses == null)
				statuses = new Status[0];
			this.statuses = statuses;
		}

		@JsonProperty("search_metadata")
		public SearchMetadata getSearchMetadata() {
			return this.searchMetadata;
		}

		@JsonProperty("search_metadata")
		public void setSearchMetadata(SearchMetadata searchMetadata) {
			if (searchMetadata == null)
				searchMetadata = new SearchMetadata();
			this.searchMetadata = searchMetadata;
		}
	}

	private static class SearchMetadata {
		private long count;
		private String nextResults;

		public SearchMetadata() {
			this.count = 0L;
			this.nextResults = null;
		}

		public long getCount() {
			return this.count;
		}

		public void setCount(long count) {
			this.count = count;
		}

		@JsonProperty("next_results")
		public String getNextResults() {
			return this.nextResults;
		}

		@JsonProperty("next_results")
		public void setNextResults(String nextResults) {
			this.nextResults = nextResults;
		}
	}

	private static class Status {
		private User user;
		private String text;
		private long id;

		public Status() {
			this.user = new User();
			this.text = "";
			this.id = 0L;
		}

		public User getUser() {
			return user;
		}

		public void setUser(User user) {
			if (user == null)
				user = new User();
			this.user = user;
		}

		public String getText() {
			return this.text;
		}

		public void setText(String text) {
			if (text == null)
				text = "";
			this.text = text;
		}

		public long getId() {
			return this.id;
		}

		public void setId(long id) {
			this.id = id;
		}
	}

	private static class User {
		private String name;
		private String screenName;

		public User() {
			this.name = "";
			this.screenName = "";
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			if (name == null)
				name = "";
			this.name = name;
		}

		@JsonProperty("screen_name")
		public String getScreenName() {
			return this.screenName;
		}

		@JsonProperty("screen_name")
		public void setScreenName(String screenName) {
			if (screenName == null)
				screenName = "";
			this.screenName = screenName;
		}
	}

	/**
	 * Next search engine query.
	 */
	private static class NextQuery extends DefaultSearchEngineQuery {
		private String url;

		public NextQuery() {
			this.url = null;
		}

		public String getUrl() {
			return this.url;
		}

		public void setUrl(String url) {
			this.url = url;
		}
	}

	/**
	 * Downloads the document from the given URL connection.
	 *
	 * @param connection URL connection.
	 */
	private byte[] download(URLConnection connection) throws IOException {
		connection.setConnectTimeout(10000);
		connection.setReadTimeout(10000);
		int length = connection.getContentLength();
		if (length < 1024)
			length = 1024;
		InputStream inputStream = connection.getInputStream();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(length);
		byte[] buffer = new byte[1024];
		while (true) {
			int numBytes = inputStream.read(buffer);
			if (numBytes < 0)
				break;
			if (numBytes == 0)
				continue;
			outputStream.write(buffer, 0, numBytes);
		}
		return outputStream.toByteArray();
	}

	/**
	 * Processes the title of the search result.
	 */
	private String processTitle(String title) {
		title = title.replaceAll("\\s+", " ");
		return title;
	}

	/*
	 * Processes the snippet of the search result.
	 */
	private String[] processSnippet(String snippet) {
		String[] lines = StringEscapeUtils.unescapeHtml4(snippet).split("\\n+");
		List<String> list = new ArrayList<String>(lines.length);
		for (String line: lines) {
			line = line.trim().replaceAll("\\s+", " ");
			if (line.length() > 0)
				list.add(snippet);
		}
		String[] result = new String[list.size()];
		list.toArray(result);
		return result;
	}

	/**
	 * Makes a query to Twitter Search by URL and returns its results.
	 *
	 * @param query Search engine query.
	 */
	private SearchEngineResults getResults(NextQuery query) throws SearchEngineException {
		String keyString = query.getKey();
		String[] keyStrings = keyString.split("\\$", 0);
		if (keyStrings.length != 4)
			throw new SearchEngineFatalException("Key must be of the form: consumer_key + \"$\" + consumer_secret + \"$\" + token_key + \"$\" + token_secret");
		String consumerKey = keyStrings[0];
		String consumerSecret = keyStrings[1];
		String tokenKey = keyStrings[2];
		String tokenSecret = keyStrings[3];
		long startTime = System.currentTimeMillis();
		URLConnection connection = null;
		InputStream inputStream = null;
		String jsonString = null;
		try {
			OAuthConsumer consumer = new DefaultOAuthConsumer(consumerKey, consumerSecret);
			consumer.setTokenWithSecret(tokenKey, tokenSecret);
			URL url = new URL(query.getUrl());
			connection = url.openConnection();
			connection.setRequestProperty("User-Agent", "Search Engine Wrapper (http://wing.comp.nus.edu.sg/~tanyeefa/downloads/searchenginewrapper/)");
			consumer.sign(connection);
			byte[] bytes = download(connection);
			jsonString = new String(bytes, "UTF-8");
		}
		catch (OAuthMessageSignerException e) {
			throw new SearchEngineFatalException(e);
		}
		catch (OAuthExpectationFailedException e) {
			throw new SearchEngineFatalException(e);
		}
		catch (OAuthCommunicationException e) {
			throw new SearchEngineFatalException(e);
		}
		catch (IOException e) {
			if (connection != null) {
				if (connection instanceof HttpURLConnection) {
					HttpURLConnection http = (HttpURLConnection)connection;
					try {
						int code = http.getResponseCode();
						if (code == 429)
							throw new SearchEngineQuotaException(e);
						if (code % 100 == 4)
							throw new SearchEngineFatalException(e);
						throw new SearchEngineException(e);
					}
					catch (IOException ioe) {
						throw new SearchEngineException(e);
					}
				}
				else {
					throw new SearchEngineException(e);
				}
			}
			else {
				throw new SearchEngineException(e);
			}
		}
		finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				}
				catch (IOException e) {
				}
			}
		}
		long endTime = System.currentTimeMillis();
		Response response = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			response = mapper.readValue(jsonString, Response.class);
		}
		catch (IOException e) {
			throw new SearchEngineException(e);
		}
		SearchEngineResults results = new SearchEngineResults();
		results.setLabel(query.getLabel());
		results.setQuery(query.getQuery());
		results.setTotalResults(Long.MAX_VALUE);
		results.setStartIndex(query.getStartIndex());
		Status[] items = response.getStatuses();
		SearchEngineResult[] resultArray = new SearchEngineResult[items.length];
		for (int i = 0; i < items.length; i++) {
			User user = items[i].getUser();
			String url = "https://twitter.com/" + items[i].getUser().getName().trim() + "/status/" + items[i].getId();
			String title = processTitle(user.getScreenName().trim() + " (" + user.getName().trim() + ")");
			String snippet = items[i].getText();
			resultArray[i] = new SearchEngineResult();
			resultArray[i].setURL(url);
			resultArray[i].setTitle(title);
			resultArray[i].setSnippet(processSnippet(snippet));
		}
		results.setResults(resultArray);
		results.setStartTime(startTime);
		results.setEndTime(endTime);
		String nextUrlString = response.getSearchMetadata().getNextResults();
		if (nextUrlString != null) {
			nextUrlString = "https://api.twitter.com/1.1/search/tweets.json" + nextUrlString;
			NextQuery nextQuery = new NextQuery();
			nextQuery.setKey(query.getKey());
			nextQuery.setLabel(query.getLabel());
			nextQuery.setQuery(query.getQuery());
			nextQuery.setStartIndex(query.getStartIndex() + items.length);
			nextQuery.setUrl(nextUrlString);
			results.setNextQuery(nextQuery);
		}
		return results;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchEngineResults getResults(SearchEngineQuery query) throws SearchEngineException {
		if (query instanceof NextQuery) {
			NextQuery nextQuery = (NextQuery)query;
			return getResults(nextQuery);
		}
		else if (query instanceof DefaultSearchEngineQuery) {
			DefaultSearchEngineQuery defaultQuery = (DefaultSearchEngineQuery)query;
			String encodedQuery = null;
			try {
				encodedQuery = URLEncoder.encode(defaultQuery.getQuery(), "UTF-8");
			}
			catch (UnsupportedEncodingException e) {
				throw new SearchEngineFatalException(e);
			}
			String urlString = "https://api.twitter.com/1.1/search/tweets.json?count=100&q=" + encodedQuery;
			NextQuery nextQuery = new NextQuery();
			nextQuery.setKey(defaultQuery.getKey());
			nextQuery.setLabel(defaultQuery.getLabel());
			nextQuery.setQuery(defaultQuery.getQuery());
			nextQuery.setUrl(urlString);
			return getResults(nextQuery);
		}
		else
			throw new SearchEngineFatalException("Invalid query.");
	}
}

