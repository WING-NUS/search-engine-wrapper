/*
 * DuckDuckGoClient.java
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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * A search engine client for <a href="https://duckduckgo.com/api">DuckDuckGo
 * Search</a>.
 * <p>
 * Additional comments on using DuckDuckGo Search:
 * <ul>
 * <li>The start index in the query is ignored.</li>
 * </ul>
 * Implementation notes: DuckDuckGo Search can return different kinds of results
 * across multiple fields. This client will extract results, where available,
 * from the following fields in the stated order: {@code Abstract},
 * {@code Definition}, {@code Results}, {@code RelatedTopic}. 
 *
 * @author Tan Yee Fan
 */
public class DuckDuckGoClient implements SearchEngineClient {
	/** Regular expression for parsing link text. */
	private static final Pattern PATTERN = Pattern.compile("<a\\s+href=\".*?\">(.*?)</a>");

	/**
	 * Constructor.
	 */
	public DuckDuckGoClient() {
	}

	// Classes for binding JSON data to Java objects.

	private static class Response {
		private String abstractSource;
		private String abstractText;
		private String abstractUrl;
		private String definitionSource;
		private String definition;
		private String definitionUrl;
		private Result[] results;
		private Result[] relatedTopics;

		public Response() {
			this.abstractSource = "";
			this.abstractText = "";
			this.abstractUrl = "";
			this.definitionSource = "";
			this.definition = "";
			this.definitionUrl = "";
			this.results = new Result[0];
			this.relatedTopics = new Result[0];
		}

		@JsonProperty("AbstractSource")
		public String getAbstractSource() {
			return this.abstractSource;
		}

		@JsonProperty("AbstractSource")
		public void setAbstractSource(String abstractSource) {
			if (abstractSource == null)
				abstractSource = "";
			this.abstractSource = abstractSource;
		}

		@JsonProperty("AbstractText")
		public String getAbstractText() {
			return this.abstractText;
		}

		@JsonProperty("AbstractText")
		public void setAbstractText(String abstractText) {
			if (abstractText == null)
				abstractText = "";
			this.abstractText = abstractText;
		}

		@JsonProperty("AbstractURL")
		public String getAbstractUrl() {
			return this.abstractUrl;
		}

		@JsonProperty("AbstractURL")
		public void setAbstractUrl(String abstractUrl) {
			if (abstractUrl == null)
				abstractUrl = "";
			this.abstractUrl = abstractUrl;
		}

		@JsonProperty("DefinitionSource")
		public String getDefinitionSource() {
			return this.definitionSource;
		}

		@JsonProperty("DefinitionSource")
		public void setDefinitionSource(String definitionSource) {
			if (definitionSource == null)
				definitionSource = "";
			this.definitionSource = definitionSource;
		}

		@JsonProperty("Definition")
		public String getDefinition() {
			return this.definition;
		}

		@JsonProperty("Definition")
		public void setDefinition(String definition) {
			if (definition == null)
				definition = "";
			this.definition = definition;
		}

		@JsonProperty("DefinitionURL")
		public String getDefinitionUrl() {
			return this.definitionUrl;
		}

		@JsonProperty("DefinitionURL")
		public void setDefinitionUrl(String definitionUrl) {
			if (definitionUrl == null)
				definitionUrl = "";
			this.definitionUrl = definitionUrl;
		}

		@JsonProperty("Results")
		public Result[] getResults() {
			return this.results;
		}

		@JsonProperty("Results")
		public void setResults(Result[] results) {
			if (results == null)
				results = new Result[0];
			this.results = results;
		}

		@JsonProperty("RelatedTopics")
		public Result[] getRelatedTopics() {
			return this.relatedTopics;
		}

		@JsonProperty("RelatedTopics")
		public void setRelatedTopics(Result[] relatedTopics) {
			if (relatedTopics == null)
				relatedTopics = new Result[0];
			this.relatedTopics = relatedTopics;
		}
	}

	private static class Result {
		private Result[] topics;
		private String result;
		private String text;
		private String firstUrl;

		public Result() {
			this.topics = new Result[0];
			this.result = "";
			this.text = "";
			this.firstUrl = "";
		}

		@JsonProperty("Topics")
		public Result[] getTopics() {
			return this.topics;
		}

		@JsonProperty("Topics")
		public void setTopics(Result[] topics) {
			if (topics == null)
				topics = new Result[0];
			this.topics = topics;
		}

		@JsonProperty("Result")
		public String getResult() {
			return this.result;
		}

		@JsonProperty("Result")
		public void setResult(String result) {
			if (result == null)
				result = "";
			this.result = result;
		}

		@JsonProperty("Text")
		public String getText() {
			return this.text;
		}

		@JsonProperty("Text")
		public void setText(String text) {
			if (text == null)
				text = "";
			this.text = text;
		}

		@JsonProperty("FirstURL")
		public String getFirstUrl() {
			return this.firstUrl;
		}

		@JsonProperty("FirstURL")
		public void setFirstUrl(String firstUrl) {
			if (firstUrl == null)
				firstUrl = "";
			this.firstUrl = firstUrl;
		}
	}

	/**
	 * Processes the title of the search result.
	 */
	private String processTitle(String title) {
		Matcher matcher = PATTERN.matcher(title);
		if (matcher.find())
			title = matcher.group(1);
		title = title.trim().replaceAll("\\s+", " ");
		return title;
	}

	/*
	 * Processes the snippet of the search result.
	 */
	private String[] processSnippet(String snippet) {
		String line = StringEscapeUtils.unescapeHtml4(snippet);
		line = line.replaceAll("'''", "");
		line = line.trim().replaceAll("\\s+", " ");
		return new String[]{line};
	}

	/**
	 * Traverses a Result object and adds any results found.
	 */
	private void traverse(Result object, List<SearchEngineResult> results) {
		for (Result topics: object.getTopics())
			traverse(topics, results);
		String url = object.getFirstUrl();
		if (!url.isEmpty()) {
			String title = object.getResult();
			String text = object.getText();
			SearchEngineResult result = new SearchEngineResult();
			result.setTitle(processTitle(title));
			result.setSnippet(processSnippet(text));
			result.setURL(url);
			results.add(result);
		}
	}

	/**
	 * Traverses a Response object and adds any results found.
	 */
	private void traverse(Response object, List<SearchEngineResult> results) {
		String abstractUrl = object.getAbstractUrl();
		if (!abstractUrl.isEmpty()) {
			String abstractSource = object.getAbstractSource();
			String abstractText = object.getAbstractText();
			SearchEngineResult result = new SearchEngineResult();
			result.setTitle(abstractSource);
			result.setSnippet(processSnippet(abstractText));
			result.setURL(abstractUrl);
			results.add(result);
		}
		String definitionUrl = object.getDefinitionUrl();
		if (!definitionUrl.isEmpty()) {
			String definitionSource = object.getDefinitionSource();
			String definition = object.getDefinition();
			SearchEngineResult result = new SearchEngineResult();
			result.setTitle(definitionSource);
			result.setSnippet(processSnippet(definition));
			result.setURL(definitionUrl);
			results.add(result);
		}
		for (Result result: object.getResults())
			traverse(result, results);
		for (Result result: object.getRelatedTopics())
			traverse(result, results);
	}

	/**
	 * Makes a query to DuckDuckGo Search using a default query.
	 */
	private SearchEngineResults getResults(DefaultSearchEngineQuery query) throws SearchEngineException {
		if (query == null)
			throw new SearchEngineFatalException("Missing query.");
		String label = query.getLabel();
		String queryString = query.getQuery();
		long startIndex = query.getStartIndex();
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
			String requestUrl = "http://api.duckduckgo.com/?q=" + encodedQuery + "&format=json&no_html=1";
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
		List<SearchEngineResult> resultList = new ArrayList<SearchEngineResult>(128);
		traverse(response, resultList);
		SearchEngineResult[] resultArray = new SearchEngineResult[resultList.size()];
		resultList.toArray(resultArray);
		// In DuckDuckGo, there is no "next query".
		SearchEngineResults results = new SearchEngineResults();
		results.setLabel(label);
		results.setQuery(queryString);
		results.setTotalResults(resultArray.length);
		results.setStartIndex(startIndex);
		results.setResults(resultArray);
		results.setStartTime(startTime);
		results.setEndTime(endTime);
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

