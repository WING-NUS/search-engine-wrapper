/*
 * StreamCharsetDetector.java
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

import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

/**
 * An abstract class for an input byte stream character encoding detector. The
 * detection of a possible character encoding is performed by reading in a small
 * amount of data from the beginning of the byte stream.
 *
 * @author Tan Yee Fan
 */
public abstract class StreamCharsetDetector {
	/**
	 * Constructs a new instance of the character encoding detector.
	 */
	public StreamCharsetDetector() {
	}

	/**
	 * Detects a possible character encoding of the given input byte stream.
	 * The byte stream must support the {@code mark} and {@code reset}
	 * methods.
	 *
	 * @return The detected encoding, or {@code null} if no encoding is
	 *         detected.
	 * @throws IOException If an I/O error occurs.
	 */
	public String detect(InputStream stream) throws IOException {
		return detect(stream, null);
	}

	/**
	 * Detects a possible character encoding of the given input byte stream,
	 * with the belief that it is more likely to be of a particular
	 * encoding. The byte stream must support the {@code mark} and
	 * {@code reset} methods.
	 *
	 * @return The detected encoding, or {@code null} if no encoding is
	 *         detected.
	 * @throws IOException If an I/O error occurs.
	 */
	public abstract String detect(InputStream stream, String defaultEncoding) throws IOException;

	/**
	 * Determines whether the given encoding name is supported in Java.
	 */
	protected boolean isSupportedEncoding(String encoding) {
		try {
			return Charset.isSupported(encoding);
		}
		catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Decodes a byte stream into a character string using the specified
	 * encoding. The decoding is performed as leniently as possible, with
	 * bytes that cannot be decoded replaced by special characters. If the
	 * decoding cannot be performed, {@code null} is returned.
	 */
	protected String decode(byte[] bytes, int offset, int length, String encoding) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, offset, length);
		Charset charset = Charset.forName(encoding);
		CharsetDecoder decoder = charset.newDecoder();
		decoder.onMalformedInput(CodingErrorAction.REPLACE);
		decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
		try {
			CharBuffer charBuffer = decoder.decode(byteBuffer);
			return charBuffer.toString();
		}
		catch (CharacterCodingException e) {
			return null;
		}
	}
}

