/*
 * ResultsXMLReader.java
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import sg.yeefan.io.CharsetDetectStreamReader;

/**
 * Reader for reading search engine results from a XML file. The input XML file
 * should contain zero or more <code>&lt;results&gt;</code> elements anywhere in
 * the XML file, each representing a set of search engine results. It is
 * mandatory to call the {@link #close} method after all the search engine
 * results have been read.
 * <p>
 * This reader must be able to read in XML files that contains zero or more root
 * elements, but are otherwise well-formed. XML files produced by the
 * {@link ResultsXMLReader} class or the the legacy
 * <code>SearchEngineResultsXML</code> class must be readable by this reader.
 * <p>
 * Implementatation notes: This implementation uses Streaming API for XML (StAX)
 * to read in the XML file. The input character stream is manipulated on the fly
 * to insert a root element encapsulating the whole XML file before passing it
 * to the XML parser.
 *
 * @author Tan Yee Fan
 */
public class ResultsXMLReader {
	/** File reader. */
	private Reader reader;

	/** XML file reader. */
	private XMLStreamReader xmlReader;

	/** File encoding. */
	private String encoding;

	/**
	 * Construct a new XML reader for the given filename using an
	 * automatically detected file encoding.
	 *
	 * @throws IOException If an error occurred when reading the XML file.
	 */
	public ResultsXMLReader(String filename) throws IOException {
		CharsetDetectStreamReader reader = new CharsetDetectStreamReader(filename);
		String encoding = reader.getEncoding();
		init(reader, encoding);
	}

	/**
	 * Construct a new XML reader for the given filename using the specified
	 * file encoding.
	 *
	 * @throws IOException If an error occurred when reading the XML file.
	 */
	public ResultsXMLReader(String filename, String encoding) throws IOException {
		Reader reader = new InputStreamReader(new FileInputStream(filename), encoding);
		init(reader, encoding);
	}

	/**
	 * Construct a new XML reader for the given character stream reader.
	 *
	 * @throws IOException If an error occurred when reading the XML file.
	 */
	public ResultsXMLReader(Reader reader) throws IOException {
		init(reader, null);
	}

	/**
	 * Initialization method common to all constructors.
	 *
	 * @throws IOException If an error occurred when reading the XML file.
	 */
	private void init(Reader reader, String encoding) throws IOException {
		try {
			// To read in a legacy XML file, we need to wrap its
			// contents inside a dummy root element.
			Reader addRootReader = new AddRootXMLReader(reader, "resultslist");
			this.reader = addRootReader;
			XMLInputFactory factory = XMLInputFactory.newInstance();
			this.xmlReader = factory.createXMLStreamReader(this.reader);
			this.encoding = encoding;
		}
		catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Returns the file encoding of this XML reader. If the constructor
	 * {@link #ResultsXMLReader(Reader)} is called, then <code>null</code>
	 * is returned.
	 */
	public String getEncoding() {
		return this.encoding;
	}

	/**
	 * Skips all elements until an element start tag of the given name.
	 *
	 * @throws IOException If an error occurred when reading the XML file.
	 */
	private boolean skipUntilStartElement(String name) throws XMLStreamException {
		boolean found = false;
		while (this.xmlReader.hasNext()) {
			int event = this.xmlReader.next();
			if (event == XMLStreamConstants.START_ELEMENT) {
				String currName = this.xmlReader.getLocalName();
				if (currName.equals(name)) {
					found = true;
					break;
				}
			}
		}
		return found;
	}

	/**
	 * Skips the current subtree.
	 *
	 * @throws IOException If an error occurred when reading the XML file.
	 */
	private void skipSubTree() throws XMLStreamException {
		this.xmlReader.require(XMLStreamConstants.START_ELEMENT, null, null);
		int level = 1;
		while (level > 0) {
			int event = this.xmlReader.next();
			switch (event) {
				case XMLStreamConstants.START_ELEMENT:
					level++;
					break;
				case XMLStreamConstants.END_ELEMENT:
					level--;
					break;
				default:
					break;
			}
		}
	}

	/**
	 * Read a search engine result.
	 *
	 * @throws IOException If an error occurred when reading the XML file.
	 */
	private SearchEngineResult readResult() throws IOException {
		try {
			this.xmlReader.require(XMLStreamConstants.START_ELEMENT, null, "result");
			SearchEngineResult result = new SearchEngineResult();
			while (true) {
				int event = this.xmlReader.next();
				boolean stop = false;
				switch (event) {
					case XMLStreamConstants.START_ELEMENT:
						String name = this.xmlReader.getLocalName();
						if (name.equals("url")) {
							String url = this.xmlReader.getElementText().trim();
							result.setURL(url);
						}
						else if (name.equals("title")) {
							String title = this.xmlReader.getElementText().trim();
							result.setTitle(title);
						}
						else if (name.equals("snippet")) {
							String text = this.xmlReader.getElementText().trim();
							String[] snippet = text.split("\n");
							for (int i = 0; i < snippet.length; i++)
								snippet[i] = snippet[i].trim();
							result.setSnippet(snippet);
						}
						else
							skipSubTree();
						break;
					case XMLStreamConstants.END_ELEMENT:
						stop = true;
						break;
					default:
						break;
				}
				if (stop)
					break;
			}
			return result;
		}
		catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Reads a set of search engine results. If no more result sets are
	 * available, {@code null} is returned.
	 *
	 * @throws IOException If an error occurred when reading the XML file.
	 */
	public SearchEngineResults readResults() throws IOException {
		try {
			boolean found = skipUntilStartElement("results");
			if (!found)
				return null;
			SearchEngineResults results = new SearchEngineResults();
			ArrayList<SearchEngineResult> resultList = new ArrayList<SearchEngineResult>();
			while (true) {
				int event = this.xmlReader.next();
				boolean stop = false;
				switch (event) {
					case XMLStreamConstants.START_ELEMENT:
						String name = this.xmlReader.getLocalName();
						if (name.equals("label")) {
							String label = this.xmlReader.getElementText().trim();
							results.setLabel(label);
						}
						else if (name.equals("query")) {
							String query = this.xmlReader.getElementText().trim();
							results.setQuery(query);
						}
						else if (name.equals("totalresults")) {
							BigInteger totalResultsBig = new BigInteger(this.xmlReader.getElementText().trim());
							results.setTotalResultsBig(totalResultsBig);
						}
						else if (name.equals("startindex")) {
							long startIndex = Long.parseLong(this.xmlReader.getElementText().trim());
							results.setStartIndex(startIndex);
						}
						else if (name.equals("starttime")) {
							long startTime = Long.parseLong(this.xmlReader.getElementText().trim());
							results.setStartTime(startTime);
						}
						else if (name.equals("endtime")) {
							long endTime = Long.parseLong(this.xmlReader.getElementText().trim());
							results.setEndTime(endTime);
						}
						else if (name.equals("fatalerror")) {
							boolean fatalError = (Long.parseLong(this.xmlReader.getElementText().trim()) != 0L);
							results.setFatalError(fatalError);
						}
						else if (name.equals("result")) {
							SearchEngineResult result = readResult();
							resultList.add(result);
						}
						else
							skipSubTree();
						break;
					case XMLStreamConstants.END_ELEMENT:
						stop = true;
						break;
					default:
						break;
				}
				if (stop)
					break;
			}
			SearchEngineResult[] resultArray = new SearchEngineResult[resultList.size()];
			resultList.toArray(resultArray);
			results.setResults(resultArray);
			return results;
		}
		catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Reads a list containing all sets of search engine results.
	 *
	 * @throws IOException If an error occurred when reading the XML file.
	 */
	public List<SearchEngineResults> readResultsList() throws IOException {
		ArrayList<SearchEngineResults> resultsList = new ArrayList<SearchEngineResults>();
		SearchEngineResults results;
		while ((results = readResults()) != null)
			resultsList.add(results);
		resultsList.trimToSize();
		return resultsList;
	}

	/**
	 * Closes this reader as well as any system resources associated with
	 * it.
	 *
	 * @throws IOException If an error occurred when reading the XML file.
	 */
	public void close() throws IOException {
		try {
			this.xmlReader.close();
			this.reader.close();
		}
		catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}
}

