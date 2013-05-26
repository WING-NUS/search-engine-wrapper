/*
 * FileDownloaderException.java
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

/**
 * Exception class for error events that can occur when downloading a file.
 * 
 * @author Tan Yee Fan
 */
public class FileDownloaderException extends Exception {
	/** Serialization version. */
	private static final long serialVersionUID = 2586313038842434586L;

	/**
	 * The reason for the file download exception.
	 */
	public enum Reason {
		/** Input connection URL invalid. */
		INPUT_CONNECTION_URL_INVALID("input connection url invalid"),
		/** Input connection open timeout. */
		INPUT_CONNECTION_OPEN_TIMEOUT("input connection open timeout"),
		/** Input connection open error. */
		INPUT_CONNECTION_OPEN_ERROR("input connection open error"),
		/** Input connection media type invalid. */
		INPUT_CONNECTION_MEDIA_TYPE_INVALID("input connection media type invalid"),
		/** Output file open error. */
		OUTPUT_FILE_OPEN_ERROR("output file open error"),
		/** Input stream read timeout. */
		INPUT_STREAM_READ_TIMEOUT("input stream read timeout"),
		/** Input stream read error. */
		INPUT_STREAM_READ_ERROR("input stream read error"),
		/** Input stream close error. */
		INPUT_STREAM_CLOSE_ERROR("input stream close error"),
		/** Output stream write timeout. */
		OUTPUT_STREAM_WRITE_TIMEOUT("output stream write timeout"),
		/** Output stream write error. */
		OUTPUT_STREAM_WRITE_ERROR("output stream write error"),
		/** Output stream close error. */
		OUTPUT_STREAM_CLOSE_ERROR("output stream close error"),
		/** Download aborted by handler. */
		DOWNLOAD_ABORTED("download aborted");

		/** The reason string. */
		private String string;

		/**
		 * Constructs a reason, setting the reason string.
		 */
		Reason(String string) {
			this.string = string;
		}

		/**
		 * Returns the reason string.
		 */
		public String getString() {
			return this.string;
		}

		/**
		 * Returns the reason string.
		 */
		@Override
		public String toString() {
			return this.string;
		}
	}

	/** Reason for exception. */
	private Reason reason;

	/**
	 * Constructs a new exception, setting the reason for the exception.
	 */
	public FileDownloaderException(Reason reason) {
		super(reason.getString());
		this.reason = reason;
	}

	/**
	 * Constructs a new exception, setting the reason and cause for the
	 * exception.
	 */
	public FileDownloaderException(Reason reason, Throwable cause) {
		super(reason.getString() + ": " + cause.toString(), cause);
		this.reason = reason;
	}

	/**
	 * Returns the reason for exception.
	 */
	public Reason getReason() {
		return this.reason;
	}
}

