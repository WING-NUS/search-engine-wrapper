/*
 * FileDownloader.java
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.TreeMap;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Convenience class for downloading a file from the web.
 * <p>
 * The following code snippet illustrates the basic usage of this class:
 * <pre>
 *     FileDownloader downloader = new FileDownloader();
 *     long size = downloader.download(url, filename);
 * </pre>
 * It is possible to repeatedly call the {@link #download download} method on
 * the same {@code FileDownloader} instance to download multiple files.
 * <p>
 * HTTP basic authentication is supported. To trigger the authentication, both
 * the username and the password must be set to non-null values.
 * <p>
 * The {@link #download download} method calls the following methods in the
 * order listed: {@link #getURLInputStream getURLInputStream},
 * {@link #getFileOutputStream getFileOutputStream},
 * {@link #copyBytes copyBytes}, {@link #closeInputStream closeInputStream},
 * {@link #closeOutputStream closeOutputStream}. This class exposes these
 * methods to allow greater flexibility to the programmer. For example, the
 * programmer may choose to download a file to a memory stream rather than a
 * file stream.
 *
 * @author Tan Yee Fan
 */
public class FileDownloader {
	/** HTTP request property map. */
	private Map<String, String> requestPropertyMap;

	/** HTTP user agent string. */
	private String userAgent;

	/** HTTP basic authentication username. */
	private String username;

	/** HTTP basic authentication password. */
	private String password;

	/** URL connection timeout in milliseconds. */
	private int urlConnectionTimeout;

	/** URL read timeout in milliseconds. */
	private int urlReadTimeout;

	/** Media type acceptor. */
	private MediaTypeAcceptor mediaTypeAcceptor;

	/** Size of the buffer in bytes. */
	private int bufferSize;

	/** Whether flushing is enabled after each read. */
	private boolean flush;

	/** Download progress handler. */
	private ProgressHandler progressHandler;

	/** Connection handler. */
	private ConnectionHandler connectionHandler;

	/** Connection opened by the last call to
	    {@link #download(String, String)} or
	    {@link #getURLInputStream(String)}. */
	private URLConnection connection;

	/** HTTP response code of the connection opened by the last call to
	    {@link #download(String, String)} or
	    {@link #getURLInputStream(String)}. */
	private int responseCode;

	/** HTTP response message of the connection opened by the last call to
	    {@link #download(String, String)} or
	    {@link #getURLInputStream(String)}. */
	private String responseMessage;

	/** Content length of the connection opened by the last call to
	    {@link #download(String, String)} or
	    {@link #getURLInputStream(String)}. */
	private int contentLength;

	/** Media type of the connection opened by the last call to
	    {@link #download(String, String)} or
	    {@link #getURLInputStream(String)}. */
	private String mediaType;

	/** Character encoding of the connection opened by the last call to
	    {@link #download(String, String)} or
	    {@link #getURLInputStream(String)}. */
	private String charset;

	/**
	 * Constructs a new file downloader.
	 */
	public FileDownloader() {
		this.requestPropertyMap = new TreeMap<String, String>();
		this.userAgent = null;
		this.username = null;
		this.password = null;
		this.urlConnectionTimeout = 10000;
		this.urlReadTimeout = 10000;
		this.mediaTypeAcceptor = null;
		this.bufferSize = 4096;
		this.flush = false;
		this.progressHandler = null;
		this.connection = null;
		this.responseCode = 0;
		this.responseMessage = null;
		this.contentLength = -1;
		this.mediaType = null;
		this.charset = null;
	}

	/**
	 * Returns the value of the given HTTP request property.
	 */
	public String getRequestProperty(String property) {
		return this.requestPropertyMap.get(property);
	}

	/**
	 * Sets the value of the given HTTP request property.
	 */
	public void setRequestProperty(String property, String value) {
		this.requestPropertyMap.put(property, value);
	}

	/**
	 * Returns the HTTP user agent string.
	 */
	public String getUserAgent() {
		return this.userAgent;
	}

	/**
	 * Sets the HTTP user agent string.
	 */
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	/**
	 * Returns the HTTP basic authentication username.
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Sets the HTTP basic authentication username.
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Returns the HTTP basic authentication password.
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * Sets the HTTP basic authentication password.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Returns the media type acceptor.
	 */
	public MediaTypeAcceptor getMediaTypeAcceptor() {
		return this.mediaTypeAcceptor;
	}

	/**
	 * Sets the media type acceptor.
	 */
	public void setMediaTypeAcceptor(MediaTypeAcceptor mediaTypeAcceptor) {
		this.mediaTypeAcceptor = mediaTypeAcceptor;
	}

	/**
	 * Returns the size of the buffer in bytes.
	 */
	public int getBufferSize() {
		return this.bufferSize;
	}

	/**
	 * Sets the size of the buffer in bytes.
	 */
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	/**
	 * Returns the URL connection timeout in milliseconds.
	 */
	public int getUrlConnectionTimeout() {
		return this.urlConnectionTimeout;
	}

	/**
	 * Sets the URL connection timeout in milliseconds.
	 */
	public void setUrlConnectionTimeout(int urlConnectionTimeout) {
		this.urlConnectionTimeout = urlConnectionTimeout;
	}

	/**
	 * Returns the URL read timeout in milliseconds.
	 */
	public int getUrlReadTimeout() {
		return this.urlReadTimeout;
	}

	/**
	 * Sets the URL read timeout in milliseconds.
	 */
	public void setUrlReadTimeout(int urlReadTimeout) {
		this.urlReadTimeout = urlReadTimeout;
	}

	/**
	 * Returns whether the output stream is to be flushed each time some
	 * data is copied from the input stream.
	 */
	public boolean getFlush() {
		return this.flush;
	}

	/**
	 * Sets whether the output stream is to be flushed each time some data
	 * is copied from the input stream.
	 */
	public void setFlush(boolean flush) {
		this.flush = flush;
	}

	/**
	 * Returns the download progress handler.
	 */
	public ProgressHandler getProgressHandler() {
		return this.progressHandler;
	}

	/**
	 * Sets the download progress handler.
	 */
	public void setProgressHandler(ProgressHandler progressHandler) {
		this.progressHandler = progressHandler;
	}

	/**
	 * Returns the connection handler. This method is intended for advanced
	 * use only, see the documentation for
	 * {@linkplain ConnectionHandler connection handler} for more
	 * information.
	 * <p>
	 * <strong>Support of this method in the file downloader API is
	 * experimental. It may change in the future.</strong>
	 */
	public ConnectionHandler getConnectionHandler() {
		return this.connectionHandler;
	}

	/**
	 * Sets the connection handler. This method is intended for advanced
	 * use only, see the documentation for
	 * {@linkplain ConnectionHandler connection handler} for more
	 * information.
	 * <p>
	 * <strong>Support of this method in the file downloader API is
	 * experimental. It may change in the future.</strong>
	 */
	public void setConnectionHandler(ConnectionHandler connectionHandler) {
		this.connectionHandler = connectionHandler;
	}

	/**
	 * Returns the HTTP response code of the connection opened by the last
	 * call to {@link #download(String, String)} or
	 * {@link #getURLInputStream(String)}. When no HTTP response code is
	 * available, the value {@code 0} is returned.
	 */
	public int getResponseCode() {
		return this.responseCode;
	}

	/**
	 * Returns the HTTP response message of the connection opened by the
	 * last call to {@link #download(String, String)} or
	 * {@link #getURLInputStream(String)}. When no HTTP response code is
	 * available, the value {@code null} is returned.
	 */
	public String getResponseMessage() {
		return this.responseMessage;
	}

	/**
	 * Returns the content length of the connection opened by the last call
	 * to {@link #download(String, String)} or
	 * {@link #getURLInputStream(String)}. When no content length is
	 * available, the value {@code -1} is returned.
	 */
	public int getContentLength() {
		return this.contentLength;
	}

	/**
	 * Returns the media type of the connection opened by the last call to
	 * {@link #download(String, String)} or
	 * {@link #getURLInputStream(String)}. The returned string can be
	 * {@code null}.
	 */
	public String getMediaType() {
		return this.mediaType;
	}

	/**
	 * Returns the character encoding of the connection opened by the last
	 * call to {@link #download(String, String)} or
	 * {@link #getURLInputStream(String)}. The returned string can be
	 * {@code null}.
	 */
	public String getCharset() {
		return this.charset;
	}

	/**
	 * Returns the error stream of the connection opened by the last call to
	 * {@link #download(String, String)} or
	 * {@link #getURLInputStream(String)}. The returned stream can be
	 * {@code null}.
	 */
	public InputStream getErrorStream() {
		InputStream es = null;
		if (this.connection instanceof HttpURLConnection) {
			HttpURLConnection http = (HttpURLConnection)this.connection;
			es = http.getErrorStream();
		}
		return es;
	}

	/**
	 * Returns the Base64 encoding of the given string.
	 */
	private String encode(String s) {
		return Base64.encodeBytes(s.getBytes());
	} 

	/**
	 * Obtains an input stream for the given URL.
	 *
	 * @throws FileDownloaderException If an error occurred when
	 *         establishing the connection. 
	 */
	public InputStream getURLInputStream(String url) throws FileDownloaderException {
		this.connection = null;
		this.responseCode = 0;
		this.responseMessage = null;
		this.mediaType = null;
		// Open the connection.
		URL u;
		try {
			u = new URL(url);
		}
		catch (MalformedURLException e) {	
			throw new FileDownloaderException(FileDownloaderException.Reason.INPUT_CONNECTION_URL_INVALID, e);
		}
		URLConnection connection;
		try {
			connection = u.openConnection();
			connection.setConnectTimeout(this.urlConnectionTimeout);
			connection.setReadTimeout(this.urlReadTimeout);
			for (Map.Entry<String, String> entry: this.requestPropertyMap.entrySet()) {
				String property = entry.getKey();
				String value = entry.getValue();
				connection.setRequestProperty(property, value);
			}
			if (this.userAgent != null) {
				connection.setRequestProperty("User-Agent", this.userAgent);
			}
			if (this.username != null && this.password != null) {
				String encoded = encode(this.username + ":" + this.password);
				connection.setRequestProperty("Authorization", "Basic " + encoded);
			}
			// Force a connection by calling
			// connection.getContentLength() here.
			IOException exception = null;
			if (this.connectionHandler != null) {
				boolean proceed = this.connectionHandler.beforeConnect(this, connection);
				if (!proceed)
					throw new FileDownloaderException(FileDownloaderException.Reason.DOWNLOAD_ABORTED);
			}
			try {
				connection.connect();
				connection.getContentLength();
			}
			catch (IOException e) {
				exception = e;
			}
			if (this.connectionHandler != null) {
				boolean proceed = this.connectionHandler.afterConnect(this, connection);
				if (!proceed)
					throw new FileDownloaderException(FileDownloaderException.Reason.DOWNLOAD_ABORTED);
			}
			if (exception != null)
				throw exception;
		}
		catch (SocketTimeoutException e) {
			throw new FileDownloaderException(FileDownloaderException.Reason.INPUT_CONNECTION_OPEN_TIMEOUT, e);
		}
		catch (IOException e) {
			throw new FileDownloaderException(FileDownloaderException.Reason.INPUT_CONNECTION_OPEN_ERROR, e);
		}
		this.connection = connection;
		// Process the HTTP response code.
		if (connection instanceof HttpURLConnection) {
			HttpURLConnection http = (HttpURLConnection)connection;
			try {
				int responseCode = http.getResponseCode();
				String responseMessage = http.getResponseMessage();
				if (responseCode >= 0) {
					this.responseCode = responseCode;
					this.responseMessage = responseMessage;
				}
			}
			catch (SocketTimeoutException e) {
				throw new FileDownloaderException(FileDownloaderException.Reason.INPUT_CONNECTION_OPEN_TIMEOUT, e);
			}
			catch (IOException e) {
				throw new FileDownloaderException(FileDownloaderException.Reason.INPUT_CONNECTION_OPEN_ERROR, e);
			}
		}
		// Process the content length.
		this.contentLength = connection.getContentLength();
		// Process the content type.
		String contentType;
		try {
			contentType = connection.getContentType();
		}
		catch (NullPointerException e) {
			throw new FileDownloaderException(FileDownloaderException.Reason.INPUT_CONNECTION_OPEN_ERROR, e);
		}
		String mediaType = null;
		String charset = null;
		if (contentType != null) {
			String[] tokens1 = contentType.trim().split(";");
			if (tokens1.length > 0) {
				mediaType = tokens1[0].trim();
				for (int i = 1; i < tokens1.length; i++) {
					String[] tokens2 = tokens1[i].trim().split("=");
					if (tokens2.length >= 2) {
						String argument = tokens2[0].trim();
						String value = tokens2[1].trim();
						if (argument.equalsIgnoreCase("charset")) {
							charset = value.replace("\"", "").trim();
							break;
						}
					}
				}
			}
		}
		this.mediaType = mediaType;
		this.charset = charset;
		if (this.mediaTypeAcceptor != null && !this.mediaTypeAcceptor.accept(mediaType))
			throw new FileDownloaderException(FileDownloaderException.Reason.INPUT_CONNECTION_MEDIA_TYPE_INVALID);
		// Obtain the input stream.
		InputStream is = null;
		try {
			is = connection.getInputStream();
		}
		catch (SocketTimeoutException e) {
			throw new FileDownloaderException(FileDownloaderException.Reason.INPUT_CONNECTION_OPEN_TIMEOUT, e);
		}
		catch (IOException e) {
			throw new FileDownloaderException(FileDownloaderException.Reason.INPUT_CONNECTION_OPEN_ERROR, e);
		}
		return is;
	}

	/**
	 * Obtains an output stream for the given local file.
	 *
	 * @throws FileDownloaderException If an error occurred when
	 *         opening the file.
	 */
	public OutputStream getFileOutputStream(String filename) throws FileDownloaderException {
		OutputStream os;
		try {
			os = new FileOutputStream(filename);
		}
		catch (IOException e) {
			throw new FileDownloaderException(FileDownloaderException.Reason.OUTPUT_FILE_OPEN_ERROR, e);
		}
		return os;
	}

	/**
	 * Closes the given input stream. If the input stream is
	 * {@code null}, no action is taken.
	 *
	 * @throws FileDownloaderException If an error occurred when closing
	 *         the stream.
	 */
	public void closeInputStream(InputStream is) throws FileDownloaderException {
		if (is != null) {
			try {
				is.close();
			}
			catch (IOException e) {
				throw new FileDownloaderException(FileDownloaderException.Reason.INPUT_STREAM_CLOSE_ERROR, e);
			}
		}
	}

	/**
	 * Closes the given output stream. If the output stream is
	 * {@code null}, no action is taken.
	 *
	 * @throws FileDownloaderException If an error occurred when closing
	 *         the stream.
	 */
	public void closeOutputStream(OutputStream os) throws FileDownloaderException {
		if (os != null) {
			try {
				os.close();
			}
			catch (IOException e) {
				throw new FileDownloaderException(FileDownloaderException.Reason.OUTPUT_STREAM_CLOSE_ERROR, e);
			}
		}
	}

	/**
	 * Copies bytes from an input stream to an output stream until the end
	 * of input stream is encountered. This method does not close either the
	 * input stream or the output stream.
	 *
	 * @return The number of bytes copied.
	 * @throws FileDownloaderException If an error occurred during the
	 *         copying.
	 */
	public long copyBytes(InputStream is, OutputStream os) throws FileDownloaderException {
		FileDownloaderException exception = null;
		long numBytesCopied = 0;
		try {
			if (this.progressHandler != null) {
				boolean proceed = this.progressHandler.start(this);
				if (!proceed)
					throw new FileDownloaderException(FileDownloaderException.Reason.DOWNLOAD_ABORTED);
			}
			if (!(is instanceof BufferedInputStream))
				is = new BufferedInputStream(is, this.bufferSize);
			if (!(os instanceof BufferedOutputStream))
				os = new BufferedOutputStream(os, this.bufferSize);
			byte[] buf = new byte[this.bufferSize];
			if (this.progressHandler != null) {
				boolean proceed = this.progressHandler.progress(this, numBytesCopied);
				if (!proceed)
					throw new FileDownloaderException(FileDownloaderException.Reason.DOWNLOAD_ABORTED);
			}
			while (true) {
				int numBytes;
				try {
					numBytes = is.read(buf);
				}
				catch (SocketTimeoutException e) {
					throw new FileDownloaderException(FileDownloaderException.Reason.INPUT_STREAM_READ_TIMEOUT, e);
				}
				catch (IOException e) {
					throw new FileDownloaderException(FileDownloaderException.Reason.INPUT_STREAM_READ_ERROR, e);
				}
				if (numBytes < 0)
					break;
				if (numBytes == 0)
					continue;
				try {
					os.write(buf, 0, numBytes);
					if (this.flush)
						os.flush();
				}
				catch (SocketTimeoutException e) {
					throw new FileDownloaderException(FileDownloaderException.Reason.OUTPUT_STREAM_WRITE_TIMEOUT, e);
				}
				catch (IOException e) {
					throw new FileDownloaderException(FileDownloaderException.Reason.OUTPUT_STREAM_WRITE_ERROR, e);
				}
				numBytesCopied += numBytes;
				if (this.progressHandler != null) {
					boolean proceed = this.progressHandler.progress(this, numBytesCopied);
					if (!proceed)
						throw new FileDownloaderException(FileDownloaderException.Reason.DOWNLOAD_ABORTED);
				}
			}
			if (!this.flush) {
				try {
					os.flush();
				}
				catch (IOException e) {
					throw new FileDownloaderException(FileDownloaderException.Reason.OUTPUT_STREAM_WRITE_ERROR, e);
				}
			}
		}
		catch (FileDownloaderException e) {
			exception = e;
		}
		if (this.progressHandler != null) {
			boolean complete = (exception == null);
			this.progressHandler.stop(this, complete);
		}
		if (exception != null)
			throw exception;
		return numBytesCopied;
	}

	/**
	 * Downloads the contents at a given URL to a local file.
	 *
	 * @return The number of bytes downloaded.
	 * @throws FileDownloaderException If an error occurred during the
	 *         downloading. 
	 */
	public long download(String url, String filename) throws FileDownloaderException {
		InputStream is = null;
		OutputStream os = null;
		long size;
		try {
			is = getURLInputStream(url);
			os = getFileOutputStream(filename);
			size = copyBytes(is, os);
			closeInputStream(is);
			is = null;
			closeOutputStream(os);
			os = null;
		}
		catch (FileDownloaderException e) {
			try {
				closeInputStream(is);
				closeOutputStream(os);
			}
			catch (FileDownloaderException fde) {
				// Do nothing.
			}
			throw e;
		}
		return size;
	}

	/**
	 * Downloads the contents at a given URL to a byte array in memory.
	 *
	 * @throws FileDownloaderException If an error occurred during the
	 *         downloading. 
	 */
	public byte[] download(String url) throws FileDownloaderException {
		InputStream is = null;
		ByteArrayOutputStream os = new ByteArrayOutputStream(4096);
		try {
			is = getURLInputStream(url);
			copyBytes(is, os);
			closeInputStream(is);
			is = null;
		}
		catch (FileDownloaderException e) {
			try {
				closeInputStream(is);
			}
			catch (FileDownloaderException fde) {
				// Do nothing.
			}
			throw e;
		}
		return os.toByteArray();
	}

	/**
	 * Main method. Expects two arguments, input URL and output filename.
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("Usage: java sg.yeefan.filedownloader.FileDownloader input-url output-file [media-type]");
			System.exit(1);
		}
		String url = args[0];
		String filename = args[1];
		MediaTypeAcceptor mediaTypeAcceptor = null;
		if (args.length >= 3) {
			String mediaType = args[2];
			mediaTypeAcceptor = new SimpleMediaTypeAcceptor(mediaType);
		}
		FileDownloader downloader = new FileDownloader();
		downloader.setMediaTypeAcceptor(mediaTypeAcceptor);
		String s1 = url + " -> " + filename;
		String s3;
		try {
			long size = downloader.download(url, filename);
			s3 = size + " bytes";
		}
		catch (FileDownloaderException e) {
			s3 = e.getReason().getString();
		}
		String s2 = downloader.getResponseCode() + " " + downloader.getMediaType();
		System.out.println(s1 + " [" + s2 + "] " + s3);
	}
}

