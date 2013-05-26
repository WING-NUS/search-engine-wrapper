/*
 * WikipediaClient.java
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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringEscapeUtils;
import sg.yeefan.filedownloader.FileDownloader;
import sg.yeefan.filedownloader.FileDownloaderException;
import sg.yeefan.searchenginewrapper.SearchEngineException;
import sg.yeefan.searchenginewrapper.SearchEngineFatalException;
import sg.yeefan.searchenginewrapper.SearchEngineQuery;
import sg.yeefan.searchenginewrapper.DefaultSearchEngineQuery;
import sg.yeefan.searchenginewrapper.SearchEngineClient;
import sg.yeefan.searchenginewrapper.SearchEngineResult;
import sg.yeefan.searchenginewrapper.SearchEngineResults;

/**
 * A search engine client for
 * <a href="http://www.mediawiki.org/wiki/API:Search">Wikipedia Search</a> (part
 * of the <a href="http://www.mediawiki.org/wiki/API:Main_page">Wikipedia
 * API</a>).
 *
 * @author Tan Yee Fan
 */
public class WikipediaClient implements SearchEngineClient {
	/** Hostname of the server. */
	private String host;

	/**
	 * Constructor.
	 * <p>
	 * The client will query from English Wikipedia.
	 */
	public WikipediaClient() {
		this("en");
	}

	/**
	 * Constructor.
	 *
	 * @param host Either the language of the Wikipedia server
	 *        (e.g., {@code "en"}), or the hostname of the server hosting a
	 *        MediaWiki installation (e.g., {@code "www.mediawiki.org"}).
	 */
	public WikipediaClient(String host) {
		if (!host.contains(":") && !host.contains("."))
			host = host + ".wikipedia.org";
		this.host = host;
	}

	/**
	 * Constructor.
	 *
	 * @param host Hostname of the server hosting a MediaWiki installation
	 *        (e.g., {@code "www.mediawiki.org"}).
	 * @param port Port of the server.
	 */
	public WikipediaClient(String host, int port) {
		this((port == 80 || port <= 0) ? host : (host + ":" + port));
	}

	// Classes for binding JSON data to Java objects.

	private static class Response {
		private Query query;
		private QueryContinue queryContinue;

		public Response() {
			this.query = new Query();
		}

		public Query getQuery() {
			return this.query;
		}

		public void setQuery(Query query) {
			if (query == null)
				query = new Query();
			this.query = query;
		}

		@JsonProperty("query-continue")
		public QueryContinue getQueryContinue() {
			return this.queryContinue;
		}

		@JsonProperty("query-continue")
		public void setQueryContinue(QueryContinue queryContinue) {
			// Do not change the value to non-null if it is null.
			this.queryContinue = queryContinue;
		}
	}

	private static class QueryContinue {
	}

	private static class Query {
		private SearchInfo searchinfo;
		private Item[] search;

		public Query() {
			this.searchinfo = new SearchInfo();
			this.search = new Item[0];
		}

		public SearchInfo getSearchinfo() {
			return this.searchinfo;
		}

		public void setSearchinfo(SearchInfo searchinfo) {
			if (searchinfo == null)
				searchinfo = new SearchInfo();
			this.searchinfo = searchinfo;
		}

		public Item[] getSearch() {
			return this.search;
		}

		public void setSearch(Item[] search) {
			if (search == null)
				search = new Item[0];
			this.search = search;
		}
	}

	private static class SearchInfo {
		private long totalhits;

		public SearchInfo() {
			this.totalhits = 0L;
		}

		public long getTotalhits() {
			return this.totalhits;
		}

		public void setTotalhits(long totalhits) {
			this.totalhits = totalhits;
		}
	}

	private static class Item {
		private String title;
		private String snippet;

		public Item() {
			this.title = "";
			this.snippet = "";
		}

		public String getTitle() {
			return this.title;
		}

		public void setTitle(String title) {
			if (title == null)
				title = "";
			this.title = title;
		}

		public String getSnippet() {
			return this.snippet;
		}

		public void setSnippet(String snippet) {
			if (snippet == null)
				snippet = "";
			this.snippet = snippet;
		}
	}

	/**
	 * Obtains the Wikipedia article URL from the title.
	 */
	private String getArticleURL(String title) {
		title = title.replaceAll(" ", "_");
		try {
			title = URLEncoder.encode(title, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			// Not supposed to happen.
		}
		String url = "http://" + this.host + "/wiki/" + title;
		return url;
	}

	/**
	 * Processes the snippet of the search result.
	 */
	private String[] processSnippet(String snippet) {
		String[] lines = snippet.split("<b>\\.+</b>", 0);
		List<String> list = new ArrayList<String>(lines.length);
		for (String line: lines) {
			line = line.replaceAll("\\<.*?>", "");
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
		if (startIndex < 1)
			throw new SearchEngineFatalException("Start index must be at least 1.");
		String encodedQuery = null;
		try {
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
			String requestUrl = "http://" + this.host + "/w/api.php?action=query&list=search&srsearch=" + encodedQuery +
			  "&srinfo=totalhits&srprop=snippet&sroffset=" + (startIndex - 1) + "&srlimit=50&format=json";
			byte[] bytes = downloader.download(requestUrl);
			jsonString = new String(bytes, "UTF-8");
		}
		catch (FileDownloaderException e) {
			// It appears that rate-limiting quotas is not
			// implemented.
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
		Query responseQuery = response.getQuery();
		SearchEngineResults results = new SearchEngineResults();
		results.setLabel(label);
		results.setQuery(queryString);
		results.setTotalResults(responseQuery.getSearchinfo().getTotalhits());
		results.setStartIndex(startIndex);
		Item[] items = responseQuery.getSearch();
		SearchEngineResult[] resultArray = new SearchEngineResult[items.length];
		for (int i = 0; i < items.length; i++) {
			String title = items[i].getTitle();
			String snippet = items[i].getSnippet();
			resultArray[i] = new SearchEngineResult();
			resultArray[i].setURL(getArticleURL(title));
			resultArray[i].setTitle(title);
			resultArray[i].setSnippet(processSnippet(snippet));
		}
		results.setResults(resultArray);
		results.setStartTime(startTime);
		results.setEndTime(endTime);
		if (response.getQueryContinue() != null) {
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

