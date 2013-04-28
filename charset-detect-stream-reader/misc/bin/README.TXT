CHARSET DETECT STREAM READER
Tan Yee Fan


INTRODUCTION
------------

The charset detect stream reader is a Java Reader class that takes in a byte
stream (InputStream), automatically detects the most likely character encoding
of the byte stream, and turns it into a character stream (Reader) using that
encoding. If the detected character encoding is UTF-8, UTF-16, or UTF-32, and
contains a Unicode byte-order mark (BOM), the BOM will be removed from the byte
stream and not appear in the character stream. The CharsetDetectStreamReader
class can be used in place of the InputStreamReader class provided in the Java
API.

This class detects the most likely character encoding of the byte stream by
successively calling the following character enoding detectors in the order as
showm, until one of them is able to detect a character encoding. Each detector
reads in a small amount of data from the byte stream to perform the detection.
1. The BOM detector attempts to detect the BOM that uniquely identifies the
   UTF-8, UTF-16, or UTF-32 character encodings.
2. The XML detector attempts to detect the XML declaration <?xml ... ?>, which
   may contain a character encoding.
3. The ASCII detector reads in a small amount of data from the input byte
   stream, and checks that the bytes read are all 7-bit US-ASCII characters.
4. The ICU detector uses the International Components for Unicode for Java
   (ICU4J) package to detect the most likely character encoding.

The above algorithm for character encoding detection is by necessity a heuristic
and is not guranteed to always detect the correct encoding.

This class is written partially in response to Oracle Java developers closing
bug report 4508058 with a "Will Not Fix".


LICENSE
-------

The entire package, except the bundled third-party components, is distributed
under the Apache License, Version 2.0. See LICENSE.TXT for the text of the
license.

The third-party components included in this distribution are as follows:
1. International Components for Unicode for Java 51.1 (icu4j.jar)
   URL: http://icu-project.org/
   License: ICU License

The source code for this package can be downloaded from:
    http://wing.comp.nus.edu.sg/~tanyeefa/downloads/charsetdetectstreamreader/


