/*
 * BingClient.java
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
 * A search engine client for <a href="http://www.bing.com/developers/">Bing
 * Search</a>.
 * <p>
 * Bing Search requires a registration key to operate.
 *
 * @author Tan Yee Fan
 */
public class BingClient implements KeyedSearchEngineClient {
	/**
	 * Constructor.
	 */
	public BingClient() {
	}

	// Classes for binding JSON data to Java objects.

	private static class Response {
		private D d;

		public Response() {
			this.d = new D();
		}

		public D getD() {
			return this.d;
		}

		public void setD(D d) {
			if (d == null)
				d = new D();
			this.d = d;
		}
	}

	private static class D {
		private Result[] results;

		public D() {
			this.results = new Result[1];
			this.results[0] = new Result();
		}

		public Result[] getResults() {
			return this.results;
		}

		public void setResults(Result[] results) {
			if (results == null || results.length == 0) {
				results = new Result[1];
				results[0] = new Result();
			}
			this.results = results;
		}
	}

	private static class Result {
		private String webTotal;
		private Web[] web;

		public Result() {
			this.webTotal = "0";
			this.web = new Web[0];
		}

		@JsonProperty("WebTotal")
		public String getWebTotal() {
			return this.webTotal;
		}

		@JsonProperty("WebTotal")
		public void setWebTotal(String webTotal) {
			if (webTotal == null || webTotal.length() == 0)
				webTotal = "0";
			this.webTotal = webTotal;
		}

		@JsonProperty("Web")
		public Web[] getWeb() {
			return this.web;
		}

		@JsonProperty("Web")
		public void setWeb(Web[] web) {
			if (web == null)
				web = new Web[0];
			this.web = web;
		}
	}

	private static class Web {
		private String title;
		private String description;
		private String url;

		public Web() {
			this.title = "";
			this.description = "";
			this.url = "";
		}

		@JsonProperty("Title")
		public String getTitle() {
			return title;
		}

		@JsonProperty("Title")
		public void setTitle(String title) {
			if (title == null)
				title = "";
			this.title = title;
		}

		@JsonProperty("Description")
		public String getDescription() {
			return this.description;
		}

		@JsonProperty("Description")
		public void setDescription(String description) {
			if (description == null)
				description = "";
			this.description = description;
		}

		@JsonProperty("Url")
		public String getUrl() {
			return this.url;
		}

		@JsonProperty("Url")
		public void setUrl(String url) {
			if (url == null)
				url = "";
			this.url = url;
		}
	}

	/**
	 * Processes the title of the search result.
	 */
	private String processTitle(String title) {
		title = title.replaceAll("\\.\\.\\.", " ").trim().replaceAll("\\s+", " ");
		return title;
	}

	/**
	 * Processes the snippet of the search result.
	 */
	private String[] processSnippet(String snippet) {
		String[] lines = snippet.split("\\.\\.\\.");
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
	 * Makes a query to Bing Search using a default query.
	 */
	private SearchEngineResults getResults(DefaultSearchEngineQuery query) throws SearchEngineException {
		if (query == null)
			throw new SearchEngineFatalException("Missing query.");
		String key = query.getKey();
		String label = query.getLabel();
		String queryString = query.getQuery();
		long startIndex = query.getStartIndex();
		if (startIndex < 1)
			throw new SearchEngineFatalException("Start index must be at least 1.");
		String encodedQuery = null;
		try {
			encodedQuery = URLEncoder.encode("'" + queryString + "'", "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			throw new SearchEngineFatalException(e);
		}
		long startTime = System.currentTimeMillis();
		FileDownloader downloader = new FileDownloader();
		String jsonString = null;
		try {
			downloader.setUserAgent("Search Engine Wrapper (http://wing.comp.nus.edu.sg/~tanyeefa/downloads/searchenginewrapper/)");
			downloader.setUsername(key);
			downloader.setPassword(key);
			String requestUrl = "https://api.datamarket.azure.com/Bing/SearchWeb/Composite?$format=json&$skip=" + (startIndex - 1) +
			  "&Query=" + encodedQuery;
			byte[] bytes = downloader.download(requestUrl);
			jsonString = new String(bytes, "UTF-8");
		}
		catch (FileDownloaderException e) {
			int code = downloader.getResponseCode();
			if (code == 401)
				throw new SearchEngineFatalException(e);
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
		Result[] _results = response.getD().getResults();
		Result result = ((_results.length > 0) ? _results[0] : new Result());
		long total = 0;
		try {
			total = Long.parseLong(result.getWebTotal());
		}
		catch (NumberFormatException e) {
			throw new SearchEngineException(e);
		}
		SearchEngineResults results = new SearchEngineResults();
		results.setLabel(label);
		results.setQuery(queryString);
		results.setTotalResults(total);
		results.setStartIndex(startIndex);
		Web[] items = result.getWeb();
		SearchEngineResult[] resultArray = new SearchEngineResult[items.length];
		for (int i = 0; i < items.length; i++) {
			String url = items[i].getUrl();
			String title = items[i].getTitle();
			String snippet = items[i].getDescription();
			resultArray[i] = new SearchEngineResult();
			resultArray[i].setURL(url);
			resultArray[i].setTitle(processTitle(title));
			resultArray[i].setSnippet(processSnippet(snippet));
		}
		results.setResults(resultArray);
		results.setStartTime(startTime);
		results.setEndTime(endTime);
		if (items.length >= 10) {
			DefaultSearchEngineQuery nextQuery = new DefaultSearchEngineQuery();
			nextQuery.setKey(key);
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

