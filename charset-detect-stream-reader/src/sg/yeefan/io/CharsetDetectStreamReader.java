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
 * The charset detect stream reader is a Java {@code Reader} class that takes in
 * a byte stream ({@code InputStream}), automatically detects the most likely
 * character encoding of the byte stream, and turns it into a character stream
 * ({@code Reader}) using that encoding. The {@code CharsetDetectStreamReader}
 * class can be used in place of the {@code InputStreamReader} class provided in
 * the Java API.
 * <p>
 * The default character encoding detector used is a
 * {@linkplain SequentialCombinationCharsetDetector sequential combination of
 * several other detectors}. If the {@linkplain BOMStreamCharsetDetector BOM
 * detector} is applied and a Unicode byte-order mark (BOM) is found, then it is
 * removed from the byte stream and not appear in the character stream. Removing
 * the BOM from the byte stream also overcomes a
 * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4508058">bug in
 * Java</a> that was closed with a &quot;Will Not Fix&quot;.
 * <p>
 * Please note that character encoding detection is by necessity a heuristic and
 * is not guaranteed to always detect the correct encoding.
 *
 * @author Tan Yee Fan
 */
public class CharsetDetectStreamReader extends Reader {
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
		this(new FileInputStream(fileName), null, null);
	}

	/**
	 * Creates a new character stream from the given file. The specified
	 * character encoding detector is used instead of the default detector.
	 *
	 * @throws IOException If an I/O error occurs.
	 * @throws UnsupportedEncodingException If the encoding cannot be
	 *         detected.
	 */
	public CharsetDetectStreamReader(String fileName, StreamCharsetDetector detector) throws IOException {
		this(new FileInputStream(fileName), detector, null);
	}

	/**
	 * Creates a new character stream from the given file. The specified
	 * character encoding is specified as being more likely for the given
	 * file.
	 *
	 * @throws IOException If an I/O error occurs.
	 * @throws UnsupportedEncodingException If the encoding cannot be
	 *         detected.
	 */
	public CharsetDetectStreamReader(String fileName, String defaultEncoding) throws IOException {
		this(new FileInputStream(fileName), null, defaultEncoding);
	}

	/**
	 * Creates a new character stream from the given file. The specified
	 * character encoding detector is used instead of the default detector,
	 * and the specified character encoding is specified as being more
	 * likely for the given file.
	 *
	 * @throws IOException If an I/O error occurs.
	 * @throws UnsupportedEncodingException If the encoding cannot be
	 *         detected.
	 */
	public CharsetDetectStreamReader(String fileName, StreamCharsetDetector detector, String defaultEncoding) throws IOException {
		this(new FileInputStream(fileName), detector, defaultEncoding);
	}

	/**
	 * Creates a new character stream from the given byte array.
	 *
	 * @throws IOException If an I/O error occurs.
	 * @throws UnsupportedEncodingException If the encoding cannot be
	 *         detected.
	 */
	public CharsetDetectStreamReader(byte[] bytes) throws IOException {
		this(new ByteArrayInputStream(bytes), null, null);
	}

	/**
	 * Creates a new character stream from the given byte array. The
	 * specified character encoding detector is used instead of the default
	 * detector.
	 *
	 * @throws IOException If an I/O error occurs.
	 * @throws UnsupportedEncodingException If the encoding cannot be
	 *         detected.
	 */
	public CharsetDetectStreamReader(byte[] bytes, StreamCharsetDetector detector) throws IOException {
		this(new ByteArrayInputStream(bytes), detector, null);
	}

	/**
	 * Creates a new character stream from the given byte array. The
	 * specified character encoding is specified as being more likely for
	 * the given byte array.
	 *
	 * @throws IOException If an I/O error occurs.
	 * @throws UnsupportedEncodingException If the encoding cannot be
	 *         detected.
	 */
	public CharsetDetectStreamReader(byte[] bytes, String defaultEncoding) throws IOException {
		this(new ByteArrayInputStream(bytes), null, defaultEncoding);
	}

	/**
	 * Creates a new character stream from the given byte array. The
	 * specified character encoding detector is used instead of the default
	 * detector, and the specified character encoding is specified as being
	 * more likely for the given byte array.
	 *
	 * @throws IOException If an I/O error occurs.
	 * @throws UnsupportedEncodingException If the encoding cannot be
	 *         detected.
	 */
	public CharsetDetectStreamReader(byte[] bytes, StreamCharsetDetector detector, String defaultEncoding) throws IOException {
		this(new ByteArrayInputStream(bytes), detector, defaultEncoding);
	}

	/**
	 * Creates a new character stream from the given byte stream.
	 *
	 * @throws IOException If an I/O error occurs.
	 * @throws UnsupportedEncodingException If the encoding cannot be
	 *         detected.
	 */
	public CharsetDetectStreamReader(InputStream stream) throws IOException {
		this(stream, null, null);
	}

	/**
	 * Creates a new character stream from the given byte stream. The
	 * specified character encoding detector is used instead of the default
	 * detector.
	 *
	 * @throws IOException If an I/O error occurs.
	 * @throws UnsupportedEncodingException If the encoding cannot be
	 *         detected.
	 */
	public CharsetDetectStreamReader(InputStream stream, StreamCharsetDetector detector) throws IOException {
		this(stream, detector, null);
	}

	/**
	 * Creates a new character stream from the given byte stream. The
	 * specified character encoding is specified as being more likely for
	 * the given byte stream.
	 *
	 * @throws IOException If an I/O error occurs.
	 * @throws UnsupportedEncodingException If the encoding cannot be
	 *         detected.
	 */
	public CharsetDetectStreamReader(InputStream stream, String defaultEncoding) throws IOException {
		this(stream, null, defaultEncoding);
	}

	/**
	 * Creates a new character stream from the given byte stream. The
	 * specified character encoding is specified as being more likely for
	 * the given byte stream.
	 *
	 * @throws IOException If an I/O error occurs.
	 * @throws UnsupportedEncodingException If the encoding cannot be
	 *         detected.
	 */
	public CharsetDetectStreamReader(InputStream stream, StreamCharsetDetector detector, String defaultEncoding) throws IOException {
		super();
		if (!stream.markSupported())
			stream = new BufferedInputStream(stream);
		if (detector == null)
			detector = new SequentialCombinationCharsetDetector();
		this.encoding = detector.detect(stream, defaultEncoding);
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
	 * it. Once the stream has been closed, further {@code read()},
	 * {@code ready()}, {@code mark()}, {@code reset()}, or {@code skip()}
	 * invocations will throw an {@code IOException}. Closing a previously
	 * closed stream has no effect.
	 *
	 * @throws IOException If an I/O error occurs.
	 */
	@Override
	public void close() throws IOException {
		this.reader.close();
	}
}

