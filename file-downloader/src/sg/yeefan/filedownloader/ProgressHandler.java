/*
 * ProgressHandler.java
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
 * Interface for receiving progress information for a download.
 *
 * @author Tan Yee Fan
 */
public interface ProgressHandler {
	/**
	 * This method is called when a download is started. Implementations
	 * should return {@code true} to continue the download, or {@code false}
	 * to abort the download.
	 */
	public boolean start(FileDownloader downloader);

	/**
	 * This method is called to inform the handler the number of bytes that
	 * have been copied so far. Implementations should return {@code true}
	 * to continue the download, or {@code false} to abort the download.
	 */
	public boolean progress(FileDownloader downloader, long numBytesCopied);

	/**
	 * This method is called when a download is stopped, informing the
	 * handler whether the download has completed successfully or an error
	 * has occurred.
	 */
	public void stop(FileDownloader downloader, boolean complete);
}

