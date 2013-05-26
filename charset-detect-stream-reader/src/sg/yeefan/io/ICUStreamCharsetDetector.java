/*
 * ICUStreamCharsetDetector.java
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
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

/**
 * An input byte stream character encoding detector that uses the
 * <a href="http://icu-project.org/">International Components for Unicode for
 * Java (ICU4J)</a> package. The ICU4J package contains the
 * {@code CharsetDetector} class, which contains heuristics that can
 * detect a most likely character encoding for the byte stream. This detector is
 * not guaranteed to always detect the correct encoding.
 *
 * @author Tan Yee Fan
 */
public class ICUStreamCharsetDetector extends StreamCharsetDetector {
	/**
	 * Constructs a new instance of the character encoding detector.
	 */
	public ICUStreamCharsetDetector() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String detect(InputStream stream, String defaultEncoding) throws IOException {
		CharsetDetector detector = new CharsetDetector();
		detector.setText(stream);
		detector.setDeclaredEncoding(defaultEncoding);
		detector.enableInputFilter(true);
		CharsetMatch[] matches = detector.detectAll();
		String encoding = null;
		for (int i = 0; i < matches.length; i++) {
			// Ensure that the detected encoding is supported in Java.
			String candidateEncoding = matches[i].getName();
			if (isSupportedEncoding(candidateEncoding)) {
				encoding = candidateEncoding;
				break;
			}
		}
		return encoding;
	}
}

