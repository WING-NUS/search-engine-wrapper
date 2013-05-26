/*
 * SimpleMediaTypeAcceptor.java
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

package sg.yeefan.filedownloader;

import java.util.regex.Pattern;

/**
 * A simple media type acceptor, which accepts a given media type string if it
 * matches one or more patterns. Possible patterns include
 * {@code "application/pdf"} and {@code "text/*"}. This acceptor accepts the
 * {@code null} media type string.
 *
 * @author Tan Yee Fan
 */
public class SimpleMediaTypeAcceptor implements MediaTypeAcceptor {
	/** Patterns. */
	private Pattern[] patterns;

	/**
	 * Constructs an acceptor that matches the given pattern.
	 */
	public SimpleMediaTypeAcceptor(String pattern) {
		this(new String[]{pattern});
	}

	/**
	 * Constructs an acceptor that matches any of the given patterns.
	 */
	public SimpleMediaTypeAcceptor(String[] patterns) {
		int numPatterns = patterns.length;
		this.patterns = new Pattern[numPatterns];
		for (int i = 0; i < numPatterns; i++) {
			String pattern = patterns[i];
			StringBuilder builder = new StringBuilder();
			builder.append('^');
			for (int j = 0; j < pattern.length(); j++) {
				char c = pattern.charAt(j);
				switch (c) {
					case '\\':
					case '(':
					case ')':
					case '[':
					case ']':
					case '.':
					case '?':
					case '+':
					case '|':
					case '^':
					case '$':
						builder.append("\\");
						builder.append(c);
						break;
					case '*':
						builder.append("[^/]+");
						break;
					default:
						builder.append(c);
						break;
				}
			}
			builder.append('$');
			String regex = builder.toString();
			this.patterns[i] = Pattern.compile(regex);
		}
	}

	/** 
	 * Determines whether to accept a particular media type. The media type
	 * string is accepted if it matches a pattern or it is
	 * {@code null}.
	 */
	@Override
	public boolean accept(String mediaType) {
		if (mediaType == null)
			return true;
		int numPatterns = this.patterns.length;
		for (int i = 0; i < numPatterns; i++)
			if (this.patterns[i].matcher(mediaType).matches())
				return true;
		return false;
	}
}

