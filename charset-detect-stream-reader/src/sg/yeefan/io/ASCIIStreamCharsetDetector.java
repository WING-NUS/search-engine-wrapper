/*
 * ASCIIStreamCharsetDetector.java
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

/**
 * An input byte stream character encoding detector for the 7-bit US-ASCII
 * encoding. The detection is performed by reading in a small amount of data
 * from the beginning of the byte stream. The detection is exact for the data
 * that is read in, but may be erroneous if the remainder of byte stream
 * contains non-ASCII characters.
 *
 * @author Tan Yee Fan
 */
public class ASCIIStreamCharsetDetector extends StreamCharsetDetector {
	/**
	 * Constructs a new instance of the character encoding detector.
	 */
	public ASCIIStreamCharsetDetector() {
		super();
	}

	/** The maximum number of bytes to read in. */
	private static final int BUFFER_SIZE = 8000;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String detect(InputStream stream, String defaultEncoding) throws IOException {
		// Read some bytes from the byte stream, and reset it.
		byte[] bytes = new byte[BUFFER_SIZE];
		stream.mark(BUFFER_SIZE);
		int numBytes = 0;
		while (numBytes < BUFFER_SIZE) {
			int read = stream.read(bytes, numBytes, BUFFER_SIZE - numBytes);
			if (read < 0)
				break;
			numBytes += read;
		}
		stream.reset();
		// Verify that all bytes are ASCII characters.
		boolean isASCII = true;
		for (int i = 0; i < numBytes; i++) {
			if ((bytes[i] & 0x80) != 0) {
				isASCII = false;
				break;
			}
		}
		// Return an encoding if found.
		String encoding = (isASCII ? "US-ASCII" : null);
		return encoding;
	}
}

