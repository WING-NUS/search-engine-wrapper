/*
 * SequentialCombinationCharsetDetector.java
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
import java.util.Arrays;

/**
 * An input byte stream character encoding detector that combines one or more
 * other character encoding detectors in a sequential manner. That is, the
 * combined detectors are applied successively on the input byte stream in the
 * given order, until one of them is able to detect a character encoding.
 * <p>
 * The default sequence of detectors that are combined is as follows:
 * <ol>
 * <li>The {@linkplain BOMStreamCharsetDetector BOM detector} attempts to detect
 * the Unicode byte-order mark (BOM) that uniquely identifies the UTF-8, UTF-16,
 * or UTF-32 character encodings.</li>
 * <li>The {@linkplain XMLStreamCharsetDetector XML detector} attempts to detect
 * the XML declaration {@code <?xml ... ?>}, which may contain a character
 * encoding.</li>
 * <li>The {@linkplain HTMLStreamCharsetDetector HTML detector} attempts to
 * detect the HTML {@code <meta ... >} tag, which may contain a character
 * encoding.</li>
 * <li>The {@linkplain ASCIIStreamCharsetDetector ASCII detector} reads in a
 * small amount of data from the input byte stream, and checks that the bytes
 * read are all 7-bit US-ASCII characters.</li>
 * <li>The {@linkplain ICUStreamCharsetDetector ICU detector} uses the
 * <a href="http://icu-project.org/">International Components for Unicode for
 * Java (ICU4J)</a> package to detect the most likely character encoding.</li>
 * </ol>
 * If the BOM detector finds a BOM, then it will be removed from the byte
 * stream. 
 *
 * @author Tan Yee Fan
 */
public class SequentialCombinationCharsetDetector extends StreamCharsetDetector {
	/** The default input byte stream character encoding detectors. */
	private static final StreamCharsetDetector[] DEFAULT_DETECTORS = {
	  new BOMStreamCharsetDetector(),
	  new XMLStreamCharsetDetector(),
	  new HTMLStreamCharsetDetector(),
	  new ASCIIStreamCharsetDetector(),
	  new ICUStreamCharsetDetector()
	};

	/** The input byte stream character encoding detectors. */
	private StreamCharsetDetector[] detectors;

	/**
	 * Constructs a new instance of the character encoding detector. The
	 * default detectors are combined sequentially.
	 */
	public SequentialCombinationCharsetDetector() {
		this(DEFAULT_DETECTORS);
	}

	/**
	 * Constructs a new instance of the character encoding detector. The
	 * specified detectors are combined sequentially.
	 */
	public SequentialCombinationCharsetDetector(StreamCharsetDetector[] detectors) {
		super();
		this.detectors = Arrays.copyOf(detectors, detectors.length);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String detect(InputStream stream, String defaultEncoding) throws IOException {
		for (StreamCharsetDetector detector: this.detectors) {
			String encoding = detector.detect(stream, defaultEncoding);
			if (encoding != null)
				return encoding;
		}
		return null;
	}
}

