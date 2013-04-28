/*
 * SearchEngineResult.java
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

import java.util.Arrays;

/**
 * A search engine result. It consists of a URL, a title, and a snippet.
 *
 * @author Tan Yee Fan
 * @see SearchEngineResults
 */
public class SearchEngineResult {
	/** The URL of the result. */
	protected String url;

	/** The title of the result. */
	protected String title;

	/** The snippet of the result. */
	protected String[] snippet;

	/**
	 * Constructor.
	 */
	public SearchEngineResult() {
		this.url = null;
		this.title = null;
		this.snippet = new String[0];
	}

	/**
	 * Returns the URL of the result.
	 */
	public String getURL() {
		return this.url;
	}

	/**
	 * Returns the title of the result.
	 */
	public String getTitle() {
		return this.title;
	}

	/**
	 * Returns the snippet of the result.
	 */
	public String[] getSnippet() {
		return Arrays.copyOf(this.snippet, this.snippet.length);
	}

	/**
	 * Sets the URL of the result.
	 */
	public void setURL(String url) {
		this.url = url;
	}

	/**
	 * Sets the title of the result.
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Sets the snippet of the result.
	 */
	public void setSnippet(String[] snippet) {
		this.snippet = Arrays.copyOf(snippet, snippet.length);
	}

	/**
	 * Returns a hash code value for the object.
	 */
	@Override
	public int hashCode() {
		return ((this.url != null) ? this.url.hashCode() : 0);
	}

	/**
	 * Returns whether the given search engine result is equal to this one.
	 * Equality occurs when the URLs, titles, and snippets are all the same.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof SearchEngineResult))
			return false;
		SearchEngineResult result = (SearchEngineResult)obj;
		return ((this.url == result.url) || (this.url != null && this.url.equals(result.url))) &&
		  ((this.title == result.title) || (this.title != null && this.title.equals(result.title))) &&
		  Arrays.equals(this.snippet, result.snippet);
	}
}

