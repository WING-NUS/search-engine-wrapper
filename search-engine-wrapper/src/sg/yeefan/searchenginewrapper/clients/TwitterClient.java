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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthException;
import org.apache.commons.lang3.StringEscapeUtils;
import sg.yeefan.filedownloader.ConnectionHandler;
import sg.yeefan.filedownloader.FileDownloader;
import sg.yeefan.filedownloader.FileDownloaderException;
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
		private Status retweetedStatus;

		public Status() {
			this.user = new User();
			this.text = "";
			this.id = 0L;
			this.retweetedStatus = null;
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

		@JsonProperty("retweeted_status")
		public Status getRetweetedStatus() {
			return this.retweetedStatus;
		}

		@JsonProperty("retweeted_status")
		public void setRetweetedStatus(Status retweetedStatus) {
			// Do not change the value to non-null if it is null.
			this.retweetedStatus = retweetedStatus;
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
	 * File download connection handler for OAuth 1.0.
	 */
	private static class OAuthConnectionHandler implements ConnectionHandler {
		private OAuthConsumer consumer;
		private OAuthException exception;

		public OAuthConnectionHandler(OAuthConsumer consumer) {
			this.consumer = consumer;
		}
	
		public OAuthException getException() {
			return this.exception;
		}
	
		@Override
		public boolean beforeConnect(FileDownloader downloader, URLConnection connection) {
			boolean success = false;
			try {
				consumer.sign(connection);
				success = true;
			}
			catch (OAuthException e) {
				this.exception = e;
			}
			return success;
		}

		@Override
		public boolean afterConnect(FileDownloader downloader, URLConnection connection) {
			return true;
		}
	}

	/**
	 * Extracts the text from the status object.
	 */
	private String getStatusText(Status status) {
		// We do special processing of text if the status is a retweet,
		// because the text may be truncated. Truncated text may be
		// determined by checking whether the text ends with an ellipsis
		// (0x2026), optionally followed by an URL. In this case, we try
		// to restore the untruncated text by reconstructing from the
		// original tweet. But it is possible that the retweeted text
		// also contains addtional user-added text, in which case we use
		// the truncated text instead.
		String text = status.getText();
		Status rtStatus = status.getRetweetedStatus();
		if (rtStatus != null && text.startsWith("RT ")) {
			int ellipsisPos = text.lastIndexOf('\u2026');
			if (ellipsisPos >= 0) {
				String textFront = text.substring(0, ellipsisPos);
				String textRemainder = text.substring(ellipsisPos + 1);
				String pattern = " http://";
				boolean patternMatch;
				if (textRemainder.length() > pattern.length())
					patternMatch = textRemainder.startsWith(pattern);
				else
					patternMatch = pattern.startsWith(textRemainder);
				if (patternMatch) {
					String rtUser = rtStatus.getUser().getScreenName();
					String rtText = rtStatus.getText();
					String reconstructedText = "RT @" + rtUser + ": " + rtText;
					if (reconstructedText.startsWith(textFront))
						text = reconstructedText;
				}
			}
		}
		return text;
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
				list.add(line);
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
		OAuthConsumer consumer = new DefaultOAuthConsumer(consumerKey, consumerSecret);
		consumer.setTokenWithSecret(tokenKey, tokenSecret);
		OAuthConnectionHandler connectionHandler = new OAuthConnectionHandler(consumer);
		FileDownloader downloader = new FileDownloader();
		String jsonString = null;
		try {
			downloader.setUserAgent("Search Engine Wrapper (http://wing.comp.nus.edu.sg/~tanyeefa/downloads/searchenginewrapper/)");
			downloader.setConnectionHandler(connectionHandler);
			String requestUrl = query.getUrl();
			byte[] bytes = downloader.download(requestUrl);
			jsonString = new String(bytes, "UTF-8");
		}
		catch (FileDownloaderException e) {
			if (e.getReason() == FileDownloaderException.Reason.DOWNLOAD_ABORTED) {
				OAuthException exception = connectionHandler.getException();
				if (exception != null)
					throw new SearchEngineFatalException(exception);
				throw new SearchEngineException(e);
			}
			else {
				int code = downloader.getResponseCode();
				if (code == 429)
					throw new SearchEngineQuotaException(e);
				if (code % 100 == 4)
					throw new SearchEngineFatalException(e);
				throw new SearchEngineException(e);
			}
		}
		catch (UnsupportedEncodingException e) {
			throw new SearchEngineException(e);
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
			String url = "https://twitter.com/" + items[i].getUser().getScreenName().trim() + "/status/" + items[i].getId();
			String title = processTitle(user.getName().trim() + " (" + user.getScreenName().trim() + ")");
			String snippet = getStatusText(items[i]);  // Use this instead of items[i].getText().
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

