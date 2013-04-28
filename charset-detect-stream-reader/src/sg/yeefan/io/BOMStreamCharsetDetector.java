/*
 * BOMStreamCharsetDetector.java
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
 * An input byte stream character encoding detector that uses the Unicode
 * byte-order mark (BOM). The BOM may appear in the first few bytes of byte
 * streams encoded in UTF-8, UTF-16, or UTF-32, which uniquely identifies these
 * encodings. This detector does not attempt to detect a character encoding if
 * the BOM is not found. As a side effect, this detector removes the BOM from
 * the byte stream if it is detected.
 *
 * @author Tan Yee Fan
 */
public class BOMStreamCharsetDetector extends StreamCharsetDetector {
	/**
	 * Constructs a new instance of the character encoding detector.
	 */
	public BOMStreamCharsetDetector() {
		super();
	}

	/** Number of BOMs that can be detected. */
	private static final int NUM_BOMS = 5;

	/** The BOMs that can be detected. */
	private static final byte[][] BOMS = {
	  {(byte)0xEF, (byte)0xBB, (byte)0xBF},
	  {(byte)0x00, (byte)0x00, (byte)0xFE, (byte)0xFF},
	  {(byte)0xFF, (byte)0xFE, (byte)0x00, (byte)0x00},
	  {(byte)0xFE, (byte)0xFF},
	  {(byte)0xFF, (byte)0xFE}
	};

	/** The encodings for the BOMs. */
	private static final String[] BOM_ENCODINGS = {
	  "UTF-8",
	  "UTF-32BE",
	  "UTF-32LE",
	  "UTF-16BE",
	  "UTF-16LE"
	};

	/** The maximum number of bytes for a BOM. */
	private static final int MAX_BOM_LENGTH = 4;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String detect(InputStream stream, String defaultEncoding) throws IOException {
		// Read the first few bytes from the byte stream, and reset it.
		byte[] bomBytes = new byte[MAX_BOM_LENGTH];
		stream.mark(MAX_BOM_LENGTH);
		int numBytes = 0;
		while (numBytes < MAX_BOM_LENGTH) {
			int read = stream.read(bomBytes, numBytes, MAX_BOM_LENGTH - numBytes);
			if (read < 0)
				break;
			numBytes += read;
		}
		stream.reset();
		// Detect the encoding by matching the first few bytes with the
		// known BOMs.
		String encoding = null;
		int skip = 0;
		for (int i = 0; i < NUM_BOMS; i++) {
			int bomLength = BOMS[i].length;
			if (numBytes >= bomLength) {
				boolean found = true;
				for (int j = 0; j < bomLength; j++) {
					if (bomBytes[j] != BOMS[i][j]) {
						found = false;
						break;
					}
				}
				if (found) {
					encoding = BOM_ENCODINGS[i];
					skip = bomLength;
					break;
				}
			}
		}
		// If an encoding is detected, remove the BOM from the byte
		// stream.
		if (encoding != null) {
			int numSkip = 0;
			while (numSkip < skip) {
				int read = (int)stream.skip(skip - numSkip);
				numSkip += read;
			}
		}
		// Return an encoding if found.
		return encoding;
	}
}

