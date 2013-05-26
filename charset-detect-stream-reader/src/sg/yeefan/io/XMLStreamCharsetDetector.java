/*
 * XMLStreamCharsetDetector.java
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An input byte stream character encoding detector that uses the XML
 * declaration. XML documents may begin with a XML declaration of the form
 * {@code <?xml ... ?>}, which may also contain a character encoding. The
 * detection is exact if an XML declaration exists and the encoding given in the
 * XML declaration is correct. As a sanity check, the extracted encoding is
 * verified by checking whether the XML declaration can be extracted using that
 * encoding.
 *
 * @author Tan Yee Fan
 */
public class XMLStreamCharsetDetector extends StreamCharsetDetector {
	/**
	 * Constructs a new instance of the character encoding detector.
	 */
	public XMLStreamCharsetDetector() {
		super();
	}

	/** Number of XML declaration encodings that can be detected. */
	private static final int NUM_XMLS = 5;

	/** The XML declaration encodings that can be detected. */
	private static final byte[][] XMLS = {
	  {(byte)0x3C, (byte)0x3F, (byte)0x78, (byte)0x6D},
	  {(byte)0x00, (byte)0x3C, (byte)0x00, (byte)0x3F},
	  {(byte)0x3C, (byte)0x00, (byte)0x3F, (byte)0x00},
	  {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x3C},
	  {(byte)0x3C, (byte)0x00, (byte)0x00, (byte)0x00}
	};

	/** The encodings for the XML declarations. */
	private static final String[] XML_ENCODINGS = {
	  "UTF-8",
	  "UTF-16BE",
	  "UTF-16LE",
	  "UTF-32BE",
	  "UTF-32LE"
	};

	/** The maximum number of bytes for a XML declaration. */
	private static final int MAX_XML_LENGTH = 512;

	/** The pattern matcher for the encoding information in a XML
	    declaration. */
	private static final Pattern XML_ENCODING_PATTERN = Pattern.compile("encoding\\s*=\\s*\"\\s*([^\\s\"]+)\\s*\"");

	/**
	 * Extracts the XML declaration from the given string. If it cannot be
	 * extracted, then {@code null} is returned.
	 */
	private String extractXMLDeclaration(String string) {
		int pos;
		if (string.startsWith("<?xml") && (pos = string.indexOf("?>")) >= 0)
			return string.substring(0, pos + 2);
		else
			return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String detect(InputStream stream, String defaultEncoding) throws IOException {
		// Read some bytes from the byte stream, and reset it.
		byte[] xmlBytes = new byte[MAX_XML_LENGTH];
		stream.mark(MAX_XML_LENGTH);
		int numBytes = 0;
		while (numBytes < MAX_XML_LENGTH) {
			int read = stream.read(xmlBytes, numBytes, MAX_XML_LENGTH - numBytes);
			if (read < 0)
				break;
			numBytes += read;
		}
		stream.reset();
		// Detect a possible encoding by matching the first few bytes
		// with the known XML declaration encodings.
		String possibleEncoding = null;
		int skip = 0;
		for (int i = 0; i < NUM_XMLS; i++) {
			int xmlLength = XMLS[i].length;
			if (numBytes >= xmlLength) {
				boolean found = true;
				for (int j = 0; j < xmlLength; j++) {
					if (xmlBytes[j] != XMLS[i][j]) {
						found = false;
						break;
					}
				}
				if (found) {
					possibleEncoding = XML_ENCODINGS[i];
					skip = xmlLength;
					break;
				}
			}
		}
		// If a possible XML declaration and encoding is detected,
		// verify that it is indeed an XML declaration. If a XML
		// declaration is found, then attempt to extract the encoding
		// information if it exists in the XML declaration, but this
		// encoding information must be verified to be reasonably
		// correct before it can be accepted. Verification is done by
		// seeing whether the XML declaration can be extracted using
		// this encoding.
		String encoding = null;
		if (possibleEncoding != null) {
			String xmlString = decode(xmlBytes, 0, numBytes, possibleEncoding);
			if (xmlString != null) {
				String xmlDeclaration = extractXMLDeclaration(xmlString);
				if (xmlDeclaration != null) {
					encoding = possibleEncoding;
					Matcher matcher = XML_ENCODING_PATTERN.matcher(xmlDeclaration);
					if (matcher.find()) {
						String candidateEncoding = matcher.group(1);
						if (isSupportedEncoding(candidateEncoding)) {
							String candidateXmlString = decode(xmlBytes, 0, numBytes, candidateEncoding);
							if (candidateXmlString != null) {
								String candidateXmlDeclaration = extractXMLDeclaration(candidateXmlString);
								if (candidateXmlDeclaration != null)
									encoding = candidateEncoding;
							}
						}
					}
				}
			}
		}
		// Return an encoding if found.
		return encoding;
	}
}

