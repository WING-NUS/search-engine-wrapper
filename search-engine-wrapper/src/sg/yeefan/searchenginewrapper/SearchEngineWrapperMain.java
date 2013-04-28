/*
 * SearchEngineWrapperMain.java
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Main class for the search engine wrapper.
 *
 * @author Tan Yee Fan
 * @see SearchEngineWrapper
 */
public class SearchEngineWrapperMain {
	/**
	 * Runs the search engine wrapper.
	 */
	private static void process(SearchEngineClient client, String inputFile, String outputFile,
	  long numResults, String[] keys) throws IOException {
		SearchEngineWrapper wrapper = new SearchEngineWrapper();
		wrapper.setKeys(keys);
		wrapper.setInputFile(inputFile);
		wrapper.setOutputFile(outputFile);
		wrapper.setClient(client);
		wrapper.setNumResults(numResults);
		wrapper.run();
	}

	/**
	 * Reads the registration keys file.
	 */
	private static String[] readKeysFile(String keysFile) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(keysFile));
		ArrayList<String> list = new ArrayList<String>();
		String line;
		while ((line = reader.readLine()) != null)
			list.add(line.trim());
		reader.close();
		String[] result = new String[list.size()];
		list.toArray(result);
		return result;
	}

	/**
	 * Returns a formatted list of client strings.
	 */
	private static String getFormattedClientStrings() {
		String[] clientStrings = SearchEngineClientFactory.getClientStrings();
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (String clientString: clientStrings) {
			if (first)
				first = false;
			else
				builder.append(", ");
			builder.append('"').append(clientString).append('"');
		}
		return builder.toString();
	}

	/**
	 * Main method.
	 */
	public static void main(String[] args) {
		if (args.length < 4) {
			String formattedClientStrings = getFormattedClientStrings();
			System.err.println("Usage: java sg.yeefan.searchenginewrapper.SearchEngineWrapperMain");
			System.err.println("       client input-file output-file num-results [keys-file]");
			System.err.println("    client - One of " + formattedClientStrings + ".");
			System.exit(1);
		}
		SearchEngineClient client = SearchEngineClientFactory.getClient(args[0]);
		if (client == null) {
			System.err.println("Error: Unrecognized search engine client.");
			System.exit(1);
		}
		String inputFile = args[1];
		String outputFile = args[2];
		long numResults = 0;
		try {
			numResults = Long.parseLong(args[3]);
			if (numResults < 0)
				throw new NumberFormatException();
		}
		catch (NumberFormatException e) {
			System.err.println("Error: Number of results must be a nonnegative integer.");
			System.exit(1);
		}
		String[] keys = null;
		if (client instanceof KeyedSearchEngineClient) {
			if (args.length < 5) {
				System.err.println("Error: Search engine client requires registration keys file.");
				System.exit(1);
			}
			try {
				keys = readKeysFile(args[4]);
			}
			catch (IOException e) {
				System.err.println("Error: Unable to read requires registration keys file.");
				System.exit(1);
			}
		}
		try {
			process(client, inputFile, outputFile, numResults, keys);
		}
		catch (IOException e) {
			System.err.println("Error: An I/O error occurred when running the search engine wrapper.");
			e.printStackTrace();
			System.exit(1);
		}
	}
}

