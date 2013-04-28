/*
 * SearchEngineResultsDownloader.java
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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.text.DecimalFormat;
import sg.yeefan.filedownloader.FileDownloader;
import sg.yeefan.filedownloader.FileDownloaderException;
import sg.yeefan.filedownloader.SimpleMediaTypeAcceptor;

/**
 * A simple downloader for the files at the URLs in search engine results. The
 * various <code>download</code> methods generally allow the output filenames to
 * be directly specified via the parameter <code>outputFiles</code>, or
 * implicitly through the parameter <code>outputDir</code>. In the implicit
 * case, all files would be downloaded into the directory
 * <code>outputDir</code>, and the filenames would be of the form
 * <code>label-index</code>, where <code>label</code> is the label of the search
 * engine results, and <code>index</code> is the index of the search engine
 * result formatted as a four-digit integer string.
 * <p>
 * It is possible to specify the accepted media types, such as "application/xml"
 * and "text/*".
 * <p>
 * Unlike the search engine wrapper, this class does not offer automated retries
 * when downloads fail. Also, no parallel downloading is attempted.
 *
 * @author Tan Yee Fan
 * @see SearchEngineResults
 */
public class SearchEngineResultsDownloader {
	/**
	 * Interface for receiving feedback messages during the downloading
	 * process.
	 */
	public interface Messages {
		/**
		 * This message is sent just before the start of a file
		 * download.
		 */
		public void startDownload(String url, String outputFile);

		/**
		 * This message is sent just after the end of a file download.
		 */
		public void endDownload(String url, String outputFile, boolean success);

		/**
		 * This message is sent when an error occurred during a file
		 * download.
		 *
		 * @return <code>true</code> to retry the download,
		 *         <code>false</code> to abort the download.
		 */
		public boolean error(String url, String outputFile, String reason);
	}

	/** Feedback messages object. */
	private Messages messages;

	/** Accepted media types. */
	private String[] mediaTypes;

	/** URL connection timeout in milliseconds. */
	private int urlConnectionTimeout;

	/** URL read timeout in milliseconds. */
	private int urlReadTimeout;

	/**
	 * Constructor.
	 */
	public SearchEngineResultsDownloader() {
		this.messages = null;
		this.mediaTypes = new String[]{"*/*"};
		this.urlConnectionTimeout = 10000;
		this.urlReadTimeout = 10000;
	}

	/**
	 * Returns the feedback messages object.
	 */
	public Messages getMessages() {
		return this.messages;
	}

	/**
	 * Sets the feedback messages object.
	 */
	public void setMessages(Messages messages) {
		this.messages = messages;
	}

	/**
	 * Returns the accepted media types.
	 */
	public String[] getMediaTypes() {
		return Arrays.copyOf(this.mediaTypes, this.mediaTypes.length);
	}

	/**
	 * Sets the accepted media types.
	 */
	public void setMediaTypes(String[] mediaTypes) {
		this.mediaTypes = Arrays.copyOf(mediaTypes, mediaTypes.length);
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
	 * Downloads the files at the URLs in the list of search engine results
	 * in the given XML file.
	 *
	 * @throws IOException If an error occurred when reading the input XML
	 *         file.
	 */
	public void download(String inputFile, String outputDir) throws IOException {
		ResultsXMLReader reader = new ResultsXMLReader(inputFile);
		List<SearchEngineResults> resultsList = reader.readResultsList();
		reader.close();
		download(resultsList, outputDir);
	}

	/**
	 * Downloads the files at the URLs in the given list of search engine
	 * results.
	 */
	public void download(List<SearchEngineResults> resultsList, String outputDir) {
		for (SearchEngineResults results: resultsList)
			download(results, outputDir);
	}

	/**
	 * Downloads the files at the URLs in the given search engine results.
	 */
	public void download(SearchEngineResults results, String outputDir) {
		DecimalFormat df = new DecimalFormat("0000");
		String label = results.getLabel();
		long startIndex = results.getStartIndex();
		int numResults = (int)results.getNumResults();
		String[] outputFiles = new String[numResults];
		for (int i = 0; i < numResults; i++)
			outputFiles[i] = outputDir + File.separatorChar + label + "-" + df.format(startIndex + i);
		download(results, outputFiles);
	}

	/**
	 * Downloads the files at the URLs in the given search engine results.
	 */
	public void download(SearchEngineResults results, String[] outputFiles) {
		SearchEngineResult[] resultArray = results.getResults();
		if (resultArray.length != outputFiles.length)
			throw new IllegalArgumentException("Number of search number results and number of output filenames must be equal.");
		for (int i = 0; i < resultArray.length; i++)
			download(resultArray[i], outputFiles[i]);
	}

	/**
	 * Downloads the file at the URL in the given search engine result.
	 */
	public void download(SearchEngineResult result, String outputFile) {
		String url = result.getURL();
		downloadFile(url, outputFile);
	}

	/**
	 * Downloads the file at the given URL.
	 */
	public void downloadFile(String url, String outputFile) {
		FileDownloader downloader = new FileDownloader();
		downloader.setMediaTypeAcceptor(new SimpleMediaTypeAcceptor(this.mediaTypes));
		downloader.setUrlConnectionTimeout(this.urlConnectionTimeout);
		downloader.setUrlReadTimeout(this.urlReadTimeout);
		if (this.messages != null)
			this.messages.startDownload(url, outputFile);
		boolean success = false;
		boolean retry;
		do {
			retry = false;
			try {
				downloader.download(url, outputFile);
				success = true;
			}
			catch (FileDownloaderException e) {
				if (this.messages != null)
					retry = this.messages.error(url, outputFile, e.getReason().getString());
			}
		}
		while (!success && retry);
		if (this.messages != null)
			this.messages.endDownload(url, outputFile, success);
	}

	/**
	 * Main method.
	 */
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.println("Usage: java sg.yeefan.searchenginewrapper.SearchEngineResultsDownloader input-file output-dir [media-type]");
			System.exit(1);
		}
		String inputFile = args[0];
		String outputDir = args[1];
		String mediaType = "*/*";
		if (args.length >= 3)
			mediaType = args[2];
		Messages messages = new Messages(){
			private String reason = null;

			@Override
			public void startDownload(String url, String outputFile) {
				System.out.println(url + " -> " + outputFile);
			}

			@Override
			public void endDownload(String url, String outputFile, boolean success) {
				String status;
				if (success)
					status = "success";
				else
					status = this.reason;
				System.out.println("... " + status);
			}

			@Override
			public boolean error(String url, String outputFile, String reason) {
				this.reason = reason;
				return false;
			}
		};
		SearchEngineResultsDownloader downloader = new SearchEngineResultsDownloader();
		downloader.setMessages(messages);
		downloader.setMediaTypes(new String[]{mediaType});
		downloader.download(inputFile, outputDir);
	}
}

