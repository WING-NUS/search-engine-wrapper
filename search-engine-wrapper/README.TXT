SEARCH ENGINE WRAPPER
Tan Yee Fan


INTRODUCTION
------------

This package provides a Java wrapper framework for unifying programmatic access
to search engines. A convenience class is also included for downloading the
files at the URLs in the search engine results. This package contains an API as
well as a command-line application.

The search engine wrapper has the following key features. Firstly, as search
engines place restrictions on the number of results they return per request, the
search engine wrapper automatically issues as many requests as needed to obtain
the required number of results for a particular query string. Secondly, when
transient errors such as network errors and server reboots occur, the search
engine wrapper will retry search engine queries as many times as needed to
obtain the results. The waiting time between successive retries is determined by
an exponential backoff mechanism. Thirdly, for search engines that requires a
key to function, the search engine wrapper can manage a pool of keys to be
rotated.

The following search engines APIs are supported:
1. Google Custom Search (http://www.google.com/cse/)
2. Bing Search (http://www.bing.com/developers/)
3. DuckDuckGo Search (https://duckduckgo.com/api)
4. Wikipedia Search (http://www.mediawiki.org/wiki/API:Search)
5. Twitter Search (https://dev.twitter.com/)
6. Facebook Search (https://developers.facebook.com/)


IMPORTANT NOTICES
-----------------

See NOTICE.TXT.


SEARCH ENGINE WRAPPER COMMAND LINE USAGE
----------------------------------------

For querying a search engine, the command-line syntax is as follows:
    java -jar search-engine-wrapper.jar <client>
      <input-file> <output-file> <num-results> [<key-file>]

The <client> parameter is one of "google-custom", "bing", "duckduckgo",
"wikipedia", "twitter", and "facebook".

The <input-file> is a text file that contains lines of the form:
    <label>: <query>

The <output-file> will be a XML file containing the results for querying the
search engine.

Some search engines require the use of a registration key. For such search
engines, the <key-file> is required, which is a text file containing one key
per line.

For more information, please refer to the SearchEngineWrapper class in the
Javadocs. Documentation regarding the format of the registration key for a
particular search engine client is also found in the Javadocs for the Java
class that implements the client.


SEARCH ENGINE RESULTS DOWNLOADER COMMAND LINE USAGE
---------------------------------------------------

For downloading the files at the URLs in the search engine results, the
command-line syntax is as follows:
    java -classpath search-engine-wrapper.jar
      sg.yeefan.searchenginewrapper.SearchEngineResultsDownloader
      <input-file> <output-dir> [<media-type>]

The <input-file> is the XML file containing the search engine results.

The <output-dir> is the directory where the files should be downloaded to.

The optional <media-type> parameter restricts the kind of files that will be
downloaded. It can be strings such as "application/xml" and "text/*". When no
<media-type> parameter is specified, files of all media types are downloaded.

For more information, please refer to the SearchEngineResultsDownloader class in
the Javadocs.


API USAGE
---------

For programmatic usage, see the Javadocs under the doc directory for the API
documentation. As a summary, the following Java packages are available:
1. sg.yeefan.searchenginewrapper
   This core package contains the search engine wrapper. The central class to
   look for is SearchEngineWrapper, please consult the documentation for this
   class for detailed information on using this search engine wrapper. The
   search engine wrapper automatically handles the required number of requests
   per query string, as well as the query retries and key rotations.
2. sg.yeefan.searchenginewrapper.clients
   This package contains the clients for search engines. These classes are
   intended to be used in conjunction with the search engine wrapper, but can
   also be used standalone. When used standalone, the features of the search
   engine wrapper are not available.
Note that this API exposes more functionality than the corresponding command
line programs.

Users should note the potential issues when using the search engine wrapper,
given in NOTICE.TXT.


LICENSE
-------

The entire package, except the bundled third-party components, is distributed
under the Apache License, Version 2.0. See LICENSE.TXT for the text of the
license.

Usage of the search engines are governed by the respective license agreements of
these search engines.

The third-party components included in this distribution are as follows:
1. File Downloader 20130527 (file-downloader.jar)
   URL: http://wing.comp.nus.edu.sg/~tanyeefa/downloads/filedownloader/
   License: Apache License (Version 2.0)
2. Charset Detect Stream Reader 20130527 (charset-detect-stream-reader.jar)
   URL: http://wing.comp.nus.edu.sg/~tanyeefa/downloads/charsetdetectstreamreader/
   License: Apache License (Version 2.0)
3. Jackson JSON Processor 2.2.0 (jackson-core.jar, jackson-annotations.jar, jackson-databind.jar)
   URL: http://wiki.fasterxml.com/JacksonHome
   License: Apache License (Version 2.0)
4. jsoup 1.7.2 (jsoup.jar)
   URL: http://jsoup.org/
   License: MIT License
5. International Components for Unicode for Java 51.2 (icu4j.jar)
   URL: http://icu-project.org/
   License: ICU License
6. OAuth Signpost 1.2.1.2 (signpost-core.jar)
   URL: http://code.google.com/p/oauth-signpost/
   License: Apache License (Version 2.0)
7. Apache Commons Codec 1.8 (commons-codec.jar)
   URL: http://commons.apache.org/proper/commons-codec/
   License: Apache License (Version 2.0)
8. Apache Commons Lang 3.1 (commons-lang3.jar)
   URL: http://commons.apache.org/proper/commons-lang/
   License: Apache License (Version 2.0)
9. Base64 Public Domain Encoder/Decoder 2.3.7
   URL: http://iharder.net/base64/
   License: Public Domain

The source code for this package can be downloaded from:
    http://wing.comp.nus.edu.sg/~tanyeefa/downloads/searchenginewrapper/


