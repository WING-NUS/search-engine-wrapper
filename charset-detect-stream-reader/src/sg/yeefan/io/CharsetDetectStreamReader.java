/*
 * CharsetDetectStreamReader.java
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

package sg.yeefan.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;

/**
 * The charset detect stream reader is a Java <code>Reader</code> class that
 * takes in a byte stream (<code>InputStream</code>), automatically detects the
 * most likely character encoding of the byte stream, and turns it into a
 * character stream (<code>Reader</code>) using that encoding. If the detected
 * character encoding is UTF-8, UTF-16, or UTF-32, and contains a Unicode
 * byte-order mark (BOM), the BOM will be removed from the byte stream and not
 * appear in the character stream. The <code>CharsetDetectStreamReader</code>
 * class can be used in place of the <code>InputStreamReader</code> class
 * provided in the Java API.
 * <p>
 * This class detects the most likely character encoding of the byte stream by
 * successively calling the following character enoding detectors in the order
 * as showm, until one of them is able to detect a character encoding. Each
 * detector reads in a small amount of data from the byte stream to perform the
 * detection.
 * <ol>
 * <li>The {@linkplain BOMStreamCharsetDetector BOM detector} attempts to detect
 * the BOM that uniquely identifies the UTF-8, UTF-16, or UTF-32 character
 * encodings.</li>
 * <li>The {@linkplain XMLStreamCharsetDetector XML detector} attempts to detect
 * the XML declaration <code>&lt;?xml ... ?&gt;</code>, which may contain a
 * character encoding.</li>
 * <li>The {@linkplain ASCIIStreamCharsetDetector ASCII detector} reads in a
 * small amount of data from the input byte stream, and checks that the bytes
 * read are all 7-bit US-ASCII characters.</li>
 * <li>The {@linkplain ICUStreamCharsetDetector ICU detector} uses the
 * <a href="http://icu-project.org/">International Components for Unicode for
 * Java (ICU4J)</a> package to detect the most likely character encoding.</li>
 * </ol>
 * The above algorithm for character encoding detection is by necessity a
 * heuristic and is not guranteed to always detect the correct encoding.
 * <p>
 * This class is written partially in response to Sun Java developers closing
 * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4508058">bug
 * report 4508058</a> with a &quot;Will Not Fix&quot;.
 *
 * @author Tan Yee Fan
 */
public class CharsetDetectStreamReader extends Reader {
	/** The input byte stream character encoding detectors. */
	private static final StreamCharsetDetector[] DETECTORS = {
	  new BOMStreamCharsetDetector(),
	  new XMLStreamCharsetDetector(),
	  new ASCIIStreamCharsetDetector(),
	  new ICUStreamCharsetDetector()
	};

	/** The character stream. */
	private InputStreamReader reader;

	/** The detected encoding. */
	private String encoding;

	/**
	 * Creates a new character stream from the given file.
	 *
	 * @throws IOException If an I/O error occurs.
	 * @throws UnsupportedEncodingException If the encoding cannot be
	 *         detected.
	 */
	public CharsetDetectStreamReader(String fileName) throws IOException {
		this(new FileInputStream(fileName), null);
	}

	/**
	 * Creates a new character stream from the given file, with the belief
	 * that it is more likely to be of a particular encoding.
	 *
	 * @throws IOException If an I/O error occurs.
	 * @throws UnsupportedEncodingException If the encoding cannot be
	 *         detected.
	 */
	public CharsetDetectStreamReader(String fileName, String defaultEncoding) throws IOException {
		this(new FileInputStream(fileName), defaultEncoding);
	}

	/**
	 * Creates a new character stream from the given byte array.
	 *
	 * @throws IOException If an I/O error occurs.
	 * @throws UnsupportedEncodingException If the encoding cannot be
	 *         detected.
	 */
	public CharsetDetectStreamReader(byte[] bytes) throws IOException {
		this(new ByteArrayInputStream(bytes), null);
	}

	/**
	 * Creates a new character stream from the given byte array, with the
	 * belief that it is more likely to be of a particular encoding.
	 *
	 * @throws IOException If an I/O error occurs.
	 * @throws UnsupportedEncodingException If the encoding cannot be
	 *         detected.
	 */
	public CharsetDetectStreamReader(byte[] bytes, String defaultEncoding) throws IOException {
		this(new ByteArrayInputStream(bytes), defaultEncoding);
	}

	/**
	 * Creates a new character stream from the given byte stream.
	 *
	 * @throws IOException If an I/O error occurs.
	 * @throws UnsupportedEncodingException If the encoding cannot be
	 *         detected.
	 */
	public CharsetDetectStreamReader(InputStream stream) throws IOException {
		this(stream, null);
	}

	/**
	 * Creates a new character stream from the given byte stream, with the
	 * belief that it is more likely to be of a particular encoding.
	 *
	 * @throws IOException If an I/O error occurs.
	 * @throws UnsupportedEncodingException If the encoding cannot be
	 *         detected.
	 */
	public CharsetDetectStreamReader(InputStream stream, String defaultEncoding) throws IOException {
		super();
		if (!stream.markSupported())
			stream = new BufferedInputStream(stream);
		this.encoding = null;
		for (StreamCharsetDetector detector: DETECTORS) {
			this.encoding = detector.detect(stream, defaultEncoding);
			if (this.encoding != null)
				break;
		}
		if (this.encoding == null)
			throw new UnsupportedEncodingException("The encoding of the input stream cannot be detected.");
		this.reader = new InputStreamReader(stream, this.encoding);
	}

	/**
	 * Returns the detected character encoding for this stream.
	 */
	public String getEncoding() {
		return this.encoding;
	}

	/**
	 * Attempts to read characters into the specified character buffer. The
	 * buffer is used as a repository of characters as-is: the only changes
	 * made are the results of a put operation. No flipping or rewinding of
	 * the buffer is performed.
	 *
	 * @throws IOException If an I/O error occurs.
	 */
	@Override
	public int read(CharBuffer target) throws IOException {
		return this.reader.read(target);
	}

	/**
	 * Reads a single character. This method will block until a character is
	 * available, an I/O error occurs, or the end of the stream is reached.
	 *
	 * @throws IOException If an I/O error occurs.
	 */
	@Override
	public int read() throws IOException {
		return this.reader.read();
	}

	/**
	 * Reads characters into an array. This method will block until some
	 * input is available, an I/O error occurs, or the end of the stream is
	 * reached.
	 *
	 * @throws IOException If an I/O error occurs.
	 */
	@Override
	public int read(char[] cbuf) throws IOException {
		return this.reader.read(cbuf);
	}

	/**
	 * Reads characters into a portion of an array. This method will block
	 * until some input is available, an I/O error occurs, or the end of the
	 * stream is reached. 
	 *
	 * @throws IOException If an I/O error occurs.
     	 */
	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		return this.reader.read(cbuf, off, len);
	}

	/**
	 * Skips characters. This method will block until some characters are
	 * available, an I/O error occurs, or the end of the stream is reached.
	 *
	 * @throws IOException If an I/O error occurs.
	 */
	@Override
	public long skip(long n) throws IOException {
		return this.reader.skip(n);
	}

	/**
	 * Tells whether this stream is ready to be read.
	 *
	 * @throws IOException If an I/O error occurs.
	 */
	@Override
	public boolean ready() throws IOException {
		return this.reader.ready();
	}

	/**
	 * Closes the stream and releases any system resources associated with
	 * it. Once the stream has been closed, further <code>read()</code>,
	 * <code>ready()</code>, <code>mark()</code>, <code>reset()</code>, or
	 * <code>skip()</code> invocations will throw an
	 * <code>IOException</code>. Closing a previously closed stream has no
	 * effect.
	 *
	 * @throws IOException If an I/O error occurs.
	 */
	@Override
	public void close() throws IOException {
		this.reader.close();
	}
}

