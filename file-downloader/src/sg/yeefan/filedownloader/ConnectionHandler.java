/*
 * ConnectionHandler.java
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

import java.net.URLConnection;

/**
 * Interface for handling events immediately before and after establishing the
 * connection to the URL.
 * <p>
 * This class is intended for advanced use only, where direct manipulation of
 * the underlying {@code URLConnection} object is necessary. Implementations
 * must not create connections with this object, otherwise the behaviour of the
 * {@linkplain FileDownloader file downloader} is undefined.
 * <p>
 * <strong>Support of this class in the file downloader API is experimental. It
 * may change in the future.</strong>
 *
 * @author Tan Yee Fan
 */
public interface ConnectionHandler {
	/**
	 * This method is called immediately before the connection is
	 * established. Implementations should return {@code true} to continue
	 * the download, or {@code false} to abort the download.
	 */
	public boolean beforeConnect(FileDownloader downloader, URLConnection connection);

	/**
	 * This method is called immediately after the connection is
	 * established. Implementations should return {@code true} to continue
	 * the download, or {@code false} to abort the download.
	 */
	public boolean afterConnect(FileDownloader downloader, URLConnection connection);
}

