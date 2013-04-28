/*
 * AddRootXMLReader.java
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

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * A character stream reader that takes in another character stream reader,
 * assumed to be reader of a XML document, and inserts a root element
 * encapsulating the whole XML document on the fly.
 *
 * @author Tan Yee Fan
 */
class AddRootXMLReader extends Reader {
	/** Buffer size. */
	private static final int BUFFER_SIZE = 8192;

	/** Regular expression for the XML declaration string. */
	private static final String XML_DECLARAION_REGEX = "<\\?xml(\\s+[^<>]*)?\\?>";

	/** Regular expression for the DOCTYPE declaration string. */
	private static final String DOCTYPE_REGEX = "<!DOCTYPE(\\s+[^<>\\[\\]]*(\\[(\\s*<[^<>\\[\\]]+>)*\\s*\\])?)?>";

	/** Regular expression for matching either or both of the XML and
	    DOCTYPE declaration strings at the start of the XML document. */
	private static final String REGEX = "^\\s*((" + XML_DECLARAION_REGEX + ")?\\s*(" + DOCTYPE_REGEX + ")?)";

	/** Regular expression pattern matcher. */
	private static final Pattern PATTERN = Pattern.compile(REGEX, Pattern.DOTALL);

	/** Name of root element. */
	private String rootName;

	/** Pushback reader for allowing characters to be unread from the input
	    stream. */
	private PushbackReader pbr;

	/** End of file flag. */
	private boolean eof;

	/**
	 * Constructor.
	 */
	public AddRootXMLReader(Reader reader, String rootName) throws IOException {
		super();
		this.rootName = rootName;
		String rootStartTag = "<" + this.rootName + ">";
		this.pbr = new PushbackReader(reader, BUFFER_SIZE + rootName.length() + 3);
		char[] buffer = new char[BUFFER_SIZE];
		int numChars = 0;
		while (numChars < BUFFER_SIZE) {
			int read = this.pbr.read(buffer, numChars, BUFFER_SIZE - numChars);
			if (read < 0)
				break;
			numChars += read;
		}
		String bufferString = new String(buffer);
		Matcher matcher = PATTERN.matcher(bufferString);
		if (matcher.find()) {
			int start = matcher.start(1);
			int end = matcher.end(1);
			this.pbr.unread(buffer, end, numChars - end);
			this.pbr.unread(rootStartTag.toCharArray());
			this.pbr.unread(buffer, start, end - start);
		}
		else {
			this.pbr.unread(buffer, 0, numChars);
			this.pbr.unread(rootStartTag.toCharArray());
		}
		this.eof = false;
	}

	/**
	 * Reads characters into a portion of an array.
	 */
	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		String rootEndTag = "</" + this.rootName + ">";
		int read = this.pbr.read(cbuf, off, len);
		if (read < 0 && !this.eof) {
			this.pbr.unread(rootEndTag.toCharArray());
			read = this.pbr.read(cbuf, off, len);
			this.eof = true;
		}
		return read;
	}

	/**
	 * Closes the stream and releases any system resources associated with
	 * it.
	 */
	@Override
	public void close() throws IOException {
		this.pbr.close();
	}
}

