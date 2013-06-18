/*
 * SearchEngineClientFactory.java
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

import java.util.LinkedHashMap;
import java.util.Map;
import sg.yeefan.searchenginewrapper.clients.BingClient;
import sg.yeefan.searchenginewrapper.clients.DuckDuckGoClient;
import sg.yeefan.searchenginewrapper.clients.FacebookClient;
import sg.yeefan.searchenginewrapper.clients.GoogleCustomClient;
import sg.yeefan.searchenginewrapper.clients.TwitterClient;
import sg.yeefan.searchenginewrapper.clients.WikipediaClient;

/**
 * Factory class for search engine clients.
 *
 * @author Tan Yee Fan
 * @see SearchEngineClient
 */
public class SearchEngineClientFactory {
	/**
	 * Private constructor.
	 */
	private SearchEngineClientFactory() {
	}

	/** Map of client strings to clients. */
	private static final Map<String, Class<? extends SearchEngineClient>> CLIENT_MAP;

	static {
		CLIENT_MAP = new LinkedHashMap<String, Class<? extends SearchEngineClient>>();
		CLIENT_MAP.put("google-custom", GoogleCustomClient.class);
		CLIENT_MAP.put("bing", BingClient.class);
		CLIENT_MAP.put("duckduckgo", DuckDuckGoClient.class);
		CLIENT_MAP.put("wikipedia", WikipediaClient.class);
		CLIENT_MAP.put("twitter", TwitterClient.class);
		CLIENT_MAP.put("facebook", FacebookClient.class);
	}

	/**
	 * Returns the search engine client for a given client string. If the
	 * given client string is not recognized, {@code null} is returned.
	 */
	public static SearchEngineClient getClient(String clientString) {
		Class<? extends SearchEngineClient> clazz = CLIENT_MAP.get(clientString);
		if (clazz == null)
			return null;
		try {
			return clazz.newInstance();
		}
		catch (InstantiationException e) {
			return null;
		}
		catch (IllegalAccessException e) {
			return null;
		}
	}

	/**
	 * Returns the available client strings.
	 */
	public static String[] getClientStrings() {
		String[] clientStrings = new String[CLIENT_MAP.size()];
		CLIENT_MAP.keySet().toArray(clientStrings);
		return clientStrings;
	}
}

