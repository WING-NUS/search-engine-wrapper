/*
 * FacebookClient.java
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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import sg.yeefan.filedownloader.FileDownloader;
import sg.yeefan.filedownloader.FileDownloaderException;
import sg.yeefan.searchenginewrapper.KeyedSearchEngineClient;
import sg.yeefan.searchenginewrapper.SearchEngineException;
import sg.yeefan.searchenginewrapper.SearchEngineFatalException;
import sg.yeefan.searchenginewrapper.SearchEngineQuery;
import sg.yeefan.searchenginewrapper.DefaultSearchEngineQuery;
import sg.yeefan.searchenginewrapper.SearchEngineResult;
import sg.yeefan.searchenginewrapper.SearchEngineResults;

/**
 * A search engine client for
 * <a href="https://developers.facebook.com/">Facebook Search</a>.
 * <p>
 * Facebook Search requires two keys to operate:
 * <ul>
 * <li>{@code app_id}: App ID.</li>
 * <li>{@code app_secret}: App secret.</li>
 * </ul>
 * In the query, the registration key should be supplied by concatenating these
 * keys together, using {@code "$"} as the delimiter:
 * <pre><code>
 * app_id + "$" + app_secret
 * </code></pre>
 * <p>
 * Additional comments on using Facebook Search:
 * <ul>
 * <li>The start index in the query is ignored.</li>
 * <li>The title in each search engine result contains the name of the user
 * making the post.</li>
 * <li>The total results field in the returned search engine results is set to
 * {@code Long.MAX_VALUE}.</li>
 * </ul>
 *
 * @author Tan Yee Fan
 */
public class FacebookClient implements KeyedSearchEngineClient {
	/** Cache of registration keys to access tokens. */
	private LRUCache<String, String> accessTokenCache;

	/**
	 * Constructor.
	 */
	public FacebookClient() {
		this.accessTokenCache = new LRUCache<String, String>(20);
	}

	// Classes for binding JSON data to Java objects.

	private static class Response {
		private Post[] data;
		private Paging paging;

		public Response() {
			this.data = new Post[0];
			this.paging = new Paging();
		}

		public Post[] getData() {
			return this.data;
		}

		public void setData(Post[] data) {
			if (data == null)
				data = new Post[0];
			this.data = data;
		}

		public Paging getPaging() {
			return this.paging;
		}

		public void setPaging(Paging paging) {
			if (paging == null)
				paging = new Paging();
			this.paging = paging;
		}
	}

	private static class Post {
		private String id;
		private User from;
		private String message;
		private String name;
		private String caption;
		private String description;

		public Post() {
			this.id = "";
			this.from = new User();
			this.message = "";
			this.name = "";
			this.caption = "";
			this.description = "";
		}

		public String getId() {
			return this.id;
		}

		public void setId(String id) {
			if (id == null)
				id = "";
			this.id = id;
		}

		public User getFrom() {
			return this.from;
		}

		public void setFrom(User from) {
			if (from == null)
				from = new User();
			this.from = from;
		}

		public String getMessage() {
			return this.message;
		}

		public void setMessage(String message) {
			if (message == null)
				message = "";
			this.message = message;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			if (name == null)
				name = "";
			this.name = name;
		}

		public String getCaption() {
			return this.caption;
		}

		public void setCaption(String caption) {
			if (caption == null)
				caption = "";
			this.caption = caption;
		}

		public String getDescription() {
			return this.description;
		}

		public void setDescription(String description) {
			if (description == null)
				description = "";
			this.description = description;
		}
	}

	private static class User {
		private String name;

		public User() {
			this.name = "";
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			if (name == null)
				name = "";
			this.name = name;
		}
	}

	private static class Paging {
		private String next;

		public Paging() {
			this.next = "";
		}

		public String getNext() {
			return this.next;
		}

		public void setNext(String next) {
			if (next == null)
				next = "";
			this.next = next;
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
	 * Makes one login request.
	 */
	private String login(String appId, String appSecret) throws SearchEngineException {
		FileDownloader downloader = null;
		try {
			String encodedAppId = URLEncoder.encode(appId, "UTF-8");
			String encodedAppSecret = URLEncoder.encode(appSecret, "UTF-8");
			String url = "https://graph.facebook.com/oauth/access_token?client_id=" + encodedAppId + "&client_secret=" + encodedAppSecret + "&grant_type=client_credentials";
			downloader = new FileDownloader();
			downloader.setUserAgent("Search Engine Wrapper (http://wing.comp.nus.edu.sg/~tanyeefa/downloads/searchenginewrapper/)");
			byte[] bytes = downloader.download(url);
			String response = new String(bytes, "UTF-8");
			String[] params = response.trim().split("&");
			for (String param: params) {
				int pos = param.indexOf('=');
				if (pos >= 0) {
					String key = param.substring(0, pos).trim();
					String value = URLDecoder.decode(param.substring(pos + 1).trim(), "UTF-8");
					if (key.equals("access_token") && !value.isEmpty()) {
						String accessToken = value;
						return accessToken;
					}
				}
			}
			throw new SearchEngineException("Error parsing login response.");
		}
		catch (UnsupportedEncodingException e) {
			// Should not happen for encoding and decoding UTF-8,
			// but we throw an exception anyway.
			throw new SearchEngineException("Error parsing login response.");
		}
		catch (FileDownloaderException e) {
			if (downloader != null && downloader.getResponseCode() == 400)
				throw new SearchEngineFatalException(e);
			throw new SearchEngineException(e);
		}
	}

	/**
	 * Makes one query request.
	 */
	private Response query(String url) throws SearchEngineException {
		FileDownloader downloader = null;
		try {
			downloader = new FileDownloader();
			downloader.setUserAgent("Search Engine Wrapper (http://wing.comp.nus.edu.sg/~tanyeefa/downloads/searchenginewrapper/)");
			downloader.setUrlReadTimeout(60000);  // This can take quite a while.
			byte[] bytes = downloader.download(url);
			String jsonString = new String(bytes, "UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			Response response = mapper.readValue(jsonString, Response.class);
			return response;
		}
		catch (UnsupportedEncodingException e) {
			// Should not happen for encoding and decoding UTF-8,
			// but we throw an exception anyway.
			throw new SearchEngineException("Error parsing login response.");
		}
		catch (FileDownloaderException e) {
			if (downloader != null && downloader.getResponseCode() == 400)
				throw new SearchEngineFatalException(e);
			throw new SearchEngineException(e);
		}
		catch (IOException e) {
			throw new SearchEngineException(e);
		}
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
	private String[] processSnippet(List<String> snippet) {
		List<String> list = new ArrayList<String>(snippet.size());
		for (String line: snippet) {
			line = line.trim().replaceAll("\\s+", " ");
			if (line.length() > 0)
				list.add(line);
		}
		String[] result = new String[list.size()];
		list.toArray(result);
		return result;
	}

	/**
	 * Makes a query to Facebook Search by URL and returns its results.
	 *
	 * @param query Search engine query.
	 */
	private SearchEngineResults getResults(NextQuery query) throws SearchEngineException {
		String keyString = query.getKey();
		// Queries to Facebook requires an access token, but the login
		// process for acquiring an access token needs to be performed
		// only once. An access token may expire, in which case another
		// one may be acquired by repeating the login process. Hence:
		// 1. If we already obtained an access token, attempt the query
		//    and return its results if successful.
		// 2. Attempt the login and obtain an access token if
		//    successful. Then, attempt the query and return its results
		//    if successful.
		long startTime = System.currentTimeMillis();
		String accessToken = this.accessTokenCache.get(keyString);
		Response response = null;
		if (accessToken != null) {
			URLBuilder urlBuilder = new URLBuilder(query.getUrl());
			urlBuilder.setParameterValue("access_token", accessToken);
			String url = urlBuilder.toString();
			try {
				response = query(url);
			}
			catch (SearchEngineFatalException e) {
				this.accessTokenCache.remove(keyString);
			}
		}
		if (response == null) {
			String[] keyStrings = keyString.split("\\$", 0);
			if (keyStrings.length != 2)
				throw new SearchEngineFatalException("Key must be of the form: app_id + \"$\" + app_secret");
			String appId = keyStrings[0];
			String appSecret = keyStrings[1];
			try {
				accessToken = login(appId, appSecret);
			}
			catch (SearchEngineFatalException e) {
				throw e;
			}
			URLBuilder urlBuilder = new URLBuilder(query.getUrl());
			urlBuilder.setParameterValue("access_token", accessToken);
			String url = urlBuilder.toString();
			try {
				response = query(url);
			}
			catch (SearchEngineFatalException e) {
				throw e;
			}
			this.accessTokenCache.put(keyString, accessToken);
		}
		long endTime = System.currentTimeMillis();
		SearchEngineResults results = new SearchEngineResults();
		results.setLabel(query.getLabel());
		results.setQuery(query.getQuery());
		results.setTotalResults(Long.MAX_VALUE);
		results.setStartIndex(query.getStartIndex());
		Post[] posts = response.getData();
		SearchEngineResult[] resultArray = new SearchEngineResult[posts.length];
		for (int i = 0; i < posts.length; i++) {
			String url = "https://www.facebook.com/" + posts[i].getId().trim();
			String title = processTitle(posts[i].getFrom().getName());
			// Snippet content can come from four fields: message,
			// name, caption, and description. We concatenate them
			// together.
			String[] snippet = processSnippet(Arrays.asList(posts[i].getMessage(), posts[i].getName(), posts[i].getCaption(), posts[i].getDescription()));
			resultArray[i] = new SearchEngineResult();
			resultArray[i].setURL(url);
			resultArray[i].setTitle(title);
			resultArray[i].setSnippet(snippet);
		}
		results.setResults(resultArray);
		results.setStartTime(startTime);
		results.setEndTime(endTime);
		String nextUrlString = response.getPaging().getNext().trim();
		if (nextUrlString.startsWith("http://") || nextUrlString.startsWith("https://")) {
			NextQuery nextQuery = new NextQuery();
			nextQuery.setKey(query.getKey());
			nextQuery.setLabel(query.getLabel());
			nextQuery.setQuery(query.getQuery());
			nextQuery.setStartIndex(query.getStartIndex() + posts.length);
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
			String urlString = "https://graph.facebook.com/search?q=" + encodedQuery + "&type=post&limit=100";
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

