/*
 * GoogleCustomClient.java
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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringEscapeUtils;
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
 * A search engine client for <a href="http://www.google.com/cse/">Google Custom
 * Search</a>.
 * <p>
 * Google Custom Search requires two keys to operate:
 * <ul>
 * <li>{@code api_key}: Google API key.</li>
 * <li>{@code cx}: Custom search engine identifier.</li>
 * </ul>
 * In the query, the registration key should be supplied by concatenating these
 * keys together, using {@code "$"} as the delimiter:
 * <pre><code>
 * api_key + "$" + cx
 * </code></pre>
 *
 * @author Tan Yee Fan
 */
public class GoogleCustomClient implements KeyedSearchEngineClient {
	/**
	 * Constructor.
	 */
	public GoogleCustomClient() {
	}

	// Classes for binding JSON data to Java objects.

	private static class Response {
		private SearchInformation searchInformation;
		private Item[] items;

		public Response() {
			this.searchInformation = new SearchInformation();
			this.items = new Item[0];
		}

		public SearchInformation getSearchInformation() {
			return this.searchInformation;
		}

		public void setSearchInformation(SearchInformation searchInformation) {
			if (searchInformation == null)
				searchInformation = new SearchInformation();
			this.searchInformation = searchInformation;
		}

		public Item[] getItems() {
			return this.items;
		}

		public void setItems(Item[] items) {
			if (items == null)
				items = new Item[0];
			this.items = items;
		}
	}

	private static class Item {
		String link;
		String title;
		String htmlSnippet;

		public Item() {
			this.link = "";
			this.title = "";
			this.htmlSnippet = "";
		}

		public String getLink() {
			return this.link;
		}

		public void setLink(String link) {
			if (link == null)
				link = "";
			this.link = link;
		}

		public String getTitle() {
			return this.title;
		}

		public void setTitle(String title) {
			if (title == null)
				title = "";
			this.title = title;
		}

		public String getHtmlSnippet() {
			return this.htmlSnippet;
		}

		public void setHtmlSnippet(String htmlSnippet) {
			if (htmlSnippet == "")
				htmlSnippet = "";
			this.htmlSnippet = htmlSnippet;
		}
	}

	private static class SearchInformation {
		private long totalResults;

		public SearchInformation() {
			this.totalResults = 0L;
		}

		public long getTotalResults() {
			return this.totalResults;
		}

		public void setTotalResults(long totalResults) {
			this.totalResults = totalResults;
		}
	}

	/**
	 * Processes the snippet of the search result.
	 */
	private String[] processSnippet(String snippet) {
		String[] lines = snippet.split("<b>\\.+</b>", 0);
		List<String> list = new ArrayList<String>(lines.length);
		for (String line: lines) {
			line = line.replaceAll("<b>", "").replaceAll("</b>", "").replaceAll("<br>", "");
			line = StringEscapeUtils.unescapeHtml4(line);
			line = line.trim().replaceAll("\\s+", " ");
			if (line.length() > 0)
				list.add(line);
		}
		String[] result = new String[list.size()];
		list.toArray(result);
		return result;
	}

	/**
	 * Makes a query to Google Custom Search using a default query.
	 */
	private SearchEngineResults getResults(DefaultSearchEngineQuery query) throws SearchEngineException {
		if (query == null)
			throw new SearchEngineFatalException("Missing query.");
		String keyString = query.getKey();
		String label = query.getLabel();
		String queryString = query.getQuery();
		long startIndex = query.getStartIndex();
		String[] keyStrings = keyString.split("\\$", 0);
		if (keyStrings.length != 2)
			throw new SearchEngineFatalException("Key must be of the form: api_key + \"$\" + cx");
		String apiKey = keyStrings[0];
		String cx = keyStrings[1];
		if (startIndex < 1)
			throw new SearchEngineFatalException("Start index must be at least 1.");
		String encodedApiKey = null;
		String encodedCx = null;
		String encodedQuery = null;
		try {
			encodedApiKey = URLEncoder.encode(apiKey, "UTF-8");
			encodedCx = URLEncoder.encode(cx, "UTF-8");
			encodedQuery = URLEncoder.encode(queryString, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			throw new SearchEngineFatalException(e);
		}
		long startTime = System.currentTimeMillis();
		FileDownloader downloader = new FileDownloader();
		String jsonString = null;
		try {
			downloader.setUserAgent("Search Engine Wrapper (http://wing.comp.nus.edu.sg/~tanyeefa/downloads/searchenginewrapper/)");
			String requestUrl = "https://www.googleapis.com/customsearch/v1?key=" + encodedApiKey +
			  "&cx=" + encodedCx +
			  "&q=" + encodedQuery +
			  "&start=" + startIndex +
			  "&num=10";
			byte[] bytes = downloader.download(requestUrl);
			jsonString = new String(bytes, "UTF-8");
		}
		catch (FileDownloaderException e) {
			// TODO: Handle response code and error stream to check
			// whether quota is exceeded.
			throw new SearchEngineException(e);
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
		results.setLabel(label);
		results.setQuery(queryString);
		results.setTotalResults(response.getSearchInformation().getTotalResults());
		results.setStartIndex(startIndex);
		Item[] items = response.getItems();
		SearchEngineResult[] resultArray = new SearchEngineResult[items.length];
		for (int i = 0; i < items.length; i++) {
			String url = items[i].getLink();
			String title = items[i].getTitle();
			String snippet = items[i].getHtmlSnippet();
			resultArray[i] = new SearchEngineResult();
			resultArray[i].setURL(url);
			resultArray[i].setTitle(title);
			resultArray[i].setSnippet(processSnippet(snippet));
		}
		results.setResults(resultArray);
		results.setStartTime(startTime);
		results.setEndTime(endTime);
		if (items.length >= 10) {
			DefaultSearchEngineQuery nextQuery = new DefaultSearchEngineQuery();
			nextQuery.setKey(keyString);
			nextQuery.setLabel(label);
			nextQuery.setQuery(queryString);
			nextQuery.setStartIndex(startIndex + items.length);
			results.setNextQuery(nextQuery);
		}
		return results;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchEngineResults getResults(SearchEngineQuery query) throws SearchEngineException {
		if (query instanceof DefaultSearchEngineQuery) {
			DefaultSearchEngineQuery defaultQuery = (DefaultSearchEngineQuery)query;
			return getResults(defaultQuery);
		}
		else
			throw new SearchEngineFatalException("Invalid query.");
	}
}

