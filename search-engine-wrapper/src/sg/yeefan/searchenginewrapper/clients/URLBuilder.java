/*
 * URLBuilder.java
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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A URL builder.
 *
 * @author Tan Yee Fan
 */
class URLBuilder {
	/** Path. */
	private String path;

	/** Parameters. */
	private Map<String, String> parameterMap;

	/** Fragment. */
	private String fragment;

	/**
	 * Constructs a new URL builder.
	 *
	 * @param url URL.
	 */
	public URLBuilder(String url) {
		if (url == null)
			throw new NullPointerException();
		int fragmentPos = url.indexOf('#');
		if (fragmentPos >= 0) {
			this.fragment = url.substring(fragmentPos + 1);
			url = url.substring(0, fragmentPos);
		}
		else {
			this.fragment = null;
		}
		this.parameterMap = new LinkedHashMap<String, String>();
		int parameterPos = url.indexOf('?');
		if (parameterPos >= 0) {
			String[] parameters = url.substring(parameterPos + 1).trim().split("&");
			for (String parameter: parameters) {
				int pos = parameter.indexOf('=');
				if (pos >= 0) {
					String key = parameter.substring(0, pos).trim();
					try {
						String value = URLDecoder.decode(parameter.substring(pos + 1).trim(), "UTF-8");
						this.parameterMap.put(key, value);
					}
					catch (UnsupportedEncodingException e) {
						this.parameterMap.put(key, null);
					}
				}
				else {
					String key = parameter;
					this.parameterMap.put(key, null);
				}
			}
			this.path = url.substring(0, parameterPos);
		}
		else {
			this.path = url;
		}
	}

	/**
	 * Returns the path.
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * Return whether the parameter with the given key exists.
	 *
	 * @param key Parameter key.
	 */
	public boolean hasParameter(String key) {
		return this.parameterMap.containsKey(key);
	}

	/**
	 * Returns the parameter value for the given key.
	 *
	 * @param key Parameter key.
	 */
	public String getParameterValue(String key) {
		return this.parameterMap.get(key);
	}

	/**
	 * Sets the parameter value for the given key.
	 *
	 * @param key Parameter key.
	 * @param value Parameter value.
	 */
	public void setParameterValue(String key, String value) {
		this.parameterMap.put(key, value);
	}

	/**
	 * Removes the parameter with the given key.
	 *
	 * @param key 
	 */
	public void removeParameter(String key) {
		this.parameterMap.remove(key);
	}

	/**
	 * Returns the fragment.
	 */
	public String getFragment() {
		return this.fragment;
	}

	/**
	 * Sets the fragment.
	 *
	 * @param fragment Fragment.
	 */
	public void setFragment(String fragment) {
		this.fragment = fragment;
	}

	/**
	 * Returns the URL string.
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.path);
		if (!this.parameterMap.isEmpty()) {
			builder.append('?');
			boolean first = true;
			for (Map.Entry<String, String> entry: this.parameterMap.entrySet()) {
				if (first)
					first = false;
				else
					builder.append('&');
				String key = entry.getKey();
				String value = entry.getValue();
				builder.append(key).append('=');
				if (value != null) {
					try {
						builder.append(URLEncoder.encode(value, "UTF-8"));
					}
					catch (UnsupportedEncodingException e) {
						// Should not occur.
					}
				}
			}
		}
		if (this.fragment != null)
			builder.append('#').append(this.fragment);
		return builder.toString();
	}
}

