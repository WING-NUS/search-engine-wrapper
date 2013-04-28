/*
 * SearchEngineFatalException.java
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

package sg.yeefan.searchenginewrapper;

import java.io.Serializable;

/**
 * An exception that indicates that a fatal error occurred when querying a
 * search engine, and therefore the query should not be retried. Usually, it
 * wraps around another exception as its cause.
 *
 * @author Tan Yee Fan
 */
public class SearchEngineFatalException extends SearchEngineException implements Serializable {
	/** Serialization version. */
	private static final long serialVersionUID = 7039736823041493168L;

	/**
	 * Constructs a new search engine exception with <code>null</code> as
	 * its detail message. The cause is uninitialized.
	 */
	public SearchEngineFatalException() {
		super();
	}

	/**
	 * Constructs a new search engine exception with the specified detail
	 * message. The cause is uninitialized.
	 */
	public SearchEngineFatalException(String message) {
		super(message);
	}

	/**
	 * Constructs a new search engine exception with the specified detail
	 * message and cause.
	 */
	public SearchEngineFatalException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new search engine exception with the specified cause.
	 * The detail message is the string form of the cause.
	 */
	public SearchEngineFatalException(Throwable cause) {
		super(cause);
	}
}

