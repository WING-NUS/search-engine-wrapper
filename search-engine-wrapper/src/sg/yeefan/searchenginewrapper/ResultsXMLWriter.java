/*
 * ResultsXMLWriter.java
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Writer for writing search engine results to a XML file. The output XML file
 * contains a root element <code>&lt;resultslist&gt;</code>, whose children are
 * zero or more <code>&lt;results&gt;</code> elements, each representing a set
 * of search engine results. It is mandatory to call the {@link #close} method
 * after all the search engine results have been written.
 * <p>
 * This writer must produce XML files that are well-formed, such that a
 * non-validating XML parser is able to parse the XML files. The XML files
 * produced by this writer must be readable by both the {@link ResultsXMLReader}
 * class as well as the legacy <code>SearchEngineResultsXML</code> class.
 * <p>
 * Implementatation notes: This implementation uses Streaming API for XML (StAX)
 * to output the XML file. The line breaks between XML elements are important
 * for backwards compatability.
 *
 * @author Tan Yee Fan
 */
public class ResultsXMLWriter {
	/** Default character encoding for XML files. */
	private static final String DEFAULT_ENCODING = "UTF-8";

	/** File writer. */
	private Writer writer;

	/** XML file writer. */
	private XMLStreamWriter xmlWriter;

	/** File encoding. */
	private String encoding;

	/**
	 * Construct a new XML writer for the given filename using the UTF-8
	 * file encoding.
	 *
	 * @throws IOException If an error occurred when writing the XML file.
	 */
	public ResultsXMLWriter(String filename) throws IOException {
		Writer writer = new OutputStreamWriter(new FileOutputStream(filename), DEFAULT_ENCODING);
		init(writer, DEFAULT_ENCODING);
	}

	/**
	 * Construct a new XML writer for the given filename using the specified
	 * file encoding.
	 *
	 * @throws IOException If an error occurred when writing the XML file.
	 */
	public ResultsXMLWriter(String filename, String encoding) throws IOException {
		Writer writer = new OutputStreamWriter(new FileOutputStream(filename), encoding);
		init(writer, encoding);
	}

	/**
	 * Construct a new XML writer for the given character stream writer.
	 *
	 * @throws IOException If an error occurred when writing the XML file.
	 */
	public ResultsXMLWriter(Writer writer) throws IOException {
		init(writer, null);
	}

	/**
	 * Initialization method common to all constructors.
	 *
	 * @throws IOException If an error occurred when writing the XML file.
	 */
	private void init(Writer writer, String encoding) throws IOException {
		this.encoding = encoding;
		writeStartDocument(writer);
		writeLineBreak();
		writeStartElement("resultslist");
		writeLineBreak();
	}

	/**
	 * Returns the file encoding of this XML writer. If the constructor
	 * {@link #ResultsXMLWriter(Writer)} is called, then <code>null</code>
	 * is returned.
	 */
	public String getEncoding() {
		return this.encoding;
	}

	/**
	 * Writes a line break.
	 *
	 * @throws IOException If an error occurred when writing the XML file.
	 */
	private void writeLineBreak() throws IOException {
		String lineSep = System.getProperty("line.separator");
		try {
			this.xmlWriter.writeCharacters(lineSep);
		}
		catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Writes an element start tag.
	 *
	 * @throws IOException If an error occurred when writing the XML file.
	 */
	private void writeStartElement(String name) throws IOException {
		try {
			this.xmlWriter.writeStartElement(name);
			writeLineBreak();
		}
		catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Writes an element end tag.
	 *
	 * @throws IOException If an error occurred when writing the XML file.
	 */
	private void writeEndElement() throws IOException {
		try {
			this.xmlWriter.writeEndElement();
			writeLineBreak();
		}
		catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Writes an element with its value.
	 *
	 * @throws IOException If an error occurred when writing the XML file.
	 */
	private void writeElement(String name, String value) throws IOException {
		try {
			this.xmlWriter.writeStartElement(name);
			this.xmlWriter.writeCharacters(value);
			this.xmlWriter.writeEndElement();
			writeLineBreak();
		}
		catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Opens the XML file and writes the start of the XML document.
	 *
	 * @throws IOException If an error occurred when writing the XML file.
	 */
	private void writeStartDocument(Writer writer) throws IOException {
		try {
			this.writer = writer;
			XMLOutputFactory factory = XMLOutputFactory.newInstance();
			this.xmlWriter = factory.createXMLStreamWriter(this.writer);
			if (this.encoding != null)
				this.xmlWriter.writeStartDocument(this.encoding.toLowerCase(), "1.0");
			else
				this.xmlWriter.writeStartDocument("1.0");
			writeLineBreak();
		}
		catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Writes the end of the XML document and closes the XML file.
	 *
	 * @throws IOException If an error occurred when writing the XML file.
	 */
	private void writeEndDocument() throws IOException {
		try {
			this.xmlWriter.writeEndDocument();
			this.xmlWriter.flush();
			this.xmlWriter.close();
			this.writer.close();
		}
		catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Writes a search engine result.
	 *
	 * @throws IOException If an error occurred when writing the XML file.
	 */
	private void writeResult(SearchEngineResult result) throws IOException {
		String lineSep = System.getProperty("line.separator");
		writeStartElement("result");
		writeElement("url", result.getURL());
		writeElement("title", result.getTitle());
		String[] snippet = result.getSnippet();
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (String line: snippet) {
			if (first)
				first = false;
			else
				builder.append(lineSep);
			builder.append(line.trim());
		}
		writeElement("snippet", builder.toString());
		writeEndElement();  // result
	}

	/**
	 * Writes a set of search engine results.
	 *
	 * @throws IOException If an error occurred when writing the XML file.
	 */
	public void writeResults(SearchEngineResults results) throws IOException {
		writeStartElement("results");
		writeElement("label", results.getLabel());
		writeElement("query", results.getQuery());
		writeElement("totalresults", results.getTotalResultsBig().toString());
		writeElement("startindex", Long.toString(results.getStartIndex()));
		writeElement("starttime", Long.toString(results.getStartTime()));
		writeElement("endtime", Long.toString(results.getEndTime()));
		writeElement("elapsedtime", Long.toString(results.getElapsedTime()));
		writeElement("fatalerror", (results.hasFatalError() ? "1" : "0"));
		SearchEngineResult[] resultArray = results.getResults();
		for (SearchEngineResult result: resultArray)
			writeResult(result);
		writeEndElement();  // results
		writeLineBreak();
	}

	/**
	 * Writes a list containing sets of search engine results.
	 *
	 * @throws IOException If an error occurred when writing the XML file.
	 */
	public void writeResultsList(List<SearchEngineResults> resultsList) throws IOException {
		for (SearchEngineResults results: resultsList)
			writeResults(results);
	}

	/**
	 * Closes this writer as well as any system resources associated with
	 * it.
	 *
	 * @throws IOException If an error occurred when writing the XML file.
	 */
	public void close() throws IOException {
		writeEndElement();  // resultslist
		writeLineBreak();
		writeEndDocument();
	}
}

