/*
 * HTMLStreamCharsetDetector.java
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * An input byte stream character encoding detector that uses the HTML meta
 * tags, such as {@code <meta charset="utf-8" />} and {@code <meta
 * http-equiv="Content-Type" content="text/html; charset=utf-8" />}. The
 * detection is exact if such a character encoding is declared and it is
 * correct. As a sanity check, the extracted encoding is verified by checking
 * whether same encoding can be extracted when the input stream is decoded using
 * the extracted encoding. For loose parsing of HTML,
 * <a href="http://jsoup.org/">jsoup</a> is used.
 *
 * @author Tan Yee Fan
 */
public class HTMLStreamCharsetDetector extends StreamCharsetDetector {
	/**
	 * Constructs a new instance of the character encoding detector.
	 */
	public HTMLStreamCharsetDetector() {
		super();
	}

	/** The maximum number of bytes to read in. */
	private static final int BUFFER_SIZE = 8000;

	/**
	 * Attempts to extract the character encoding from the given bytes,
	 * using the given default encoding as an initial guess. The default
	 * encoding can be {@code null}. If the encoding can be found in the
	 * HTML meta tags, then it is returned, otherwise {@code null} is
	 * returned.
	 */
	private String extractEncoding(byte[] bytes, String defaultEncoding) {
		try {
			Document document = Jsoup.parse(new ByteArrayInputStream(bytes), defaultEncoding, "");
			Element element = document.select("html > head > meta").select("[charset], [http-equiv=content-type]").first();
			if (element != null) {
				// Parse things like <meta charset="utf-8" />
				String encoding = element.attr("charset").trim();
				if (!encoding.isEmpty() && isSupportedEncoding(encoding))
					return encoding;
				// Parse things like <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
				String httpEquiv = element.attr("http-equiv").trim();
				String content = element.attr("content").trim();
				if (httpEquiv.equalsIgnoreCase("content-type") && !content.isEmpty()) {
					int pos1 = content.indexOf(';');
					if (pos1 >= 0) {
						String[] params = content.substring(pos1 + 1).split(",");
						for (String param: params) {
							int pos2 = param.indexOf('=');
							if (pos2 >= 0) {
								String key = param.substring(0, pos2).trim();
								String value = param.substring(pos2 + 1).trim();
								if (key.equalsIgnoreCase("charset") && isSupportedEncoding(value))
									return value;
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
		}
		return null;
	}

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
		// First we attempt to extract a character encoding for the
		// given bytes. If it can be extracted, then verify that the
		// the given bytes actually are in the extracted encoding, by
		// making another extraction attempt.
		String encoding = extractEncoding(bytes, defaultEncoding);
		if (encoding == null && defaultEncoding != null)
			encoding = extractEncoding(bytes, null);
		if (encoding != null) {
			String verifyEncoding = extractEncoding(bytes, encoding);
			if (!encoding.equals(verifyEncoding))
				encoding = null;
		}
		return encoding;
	}
}

