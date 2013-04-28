FILE DOWNLOADER
Tan Yee Fan


INTRODUCTION
------------

The file downloader Java package provides an API that makes it easy to write
Java code that downloads files from the Internet. The following code snippet
illustrates the basic usage of this package:

    FileDownloader downloader = new FileDownloader();
    long size = downloader.download(url, filename);

There is no need for the programmer to mess around with HTTP connections, which
is already handled by this package. Please refer to the Javadocs for the API
details.

A simple command-line usage is also supported by this package:

    java -jar file-downloader.jar input-url output-file [media-type]

The main intention of this package is to facilitate API usage in Java programs,
as such it does not aim to replace existing command-line programs for
downloading files. Users looking for more sophisticated command-line usage
should consider more powerful programs such as wget and curl.


LICENSE
-------

The entire package, except the bundled third-party components, is distributed
under the Apache License, Version 2.0. See LICENSE.TXT for the text of the
license.

The third-party components included in this distribution are as follows:
1. Base64 Public Domain Encoder/Decoder 2.3.7
   URL: http://iharder.net/base64/
   License: Public Domain

The source code for this package can be downloaded from:
    http://wing.comp.nus.edu.sg/~tanyeefa/downloads/filedownloader/


