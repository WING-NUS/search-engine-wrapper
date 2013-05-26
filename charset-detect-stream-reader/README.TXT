CHARSET DETECT STREAM READER
Tan Yee Fan


INTRODUCTION
------------

The charset detect stream reader is a Java Reader class that takes in a byte
stream (InputStream), automatically detects the most likely character encoding
of the byte stream, and turns it into a character stream (Reader) using that
encoding. The CharsetDetectStreamReader class can be used in place of the
InputStreamReader class provided in the Java API.

The default character encoding detector used is a sequential combination of
several other detectors. The default sequence of detectors that are combined is
as follows, in which they are applied successively on the input byte stream in
the given order, until one of them is able to detect a character encoding.
1. The BOM detector attempts to detect the Unicode byte-order mark (BOM) that
   uniquely identifies the UTF-8, UTF-16, or UTF-32 character encodings.
2. The XML detector attempts to detect the XML declaration <?xml ... ?>, which
   may contain a character encoding.
3. The HTML Detector XML detector} attempts to detect the HTML <meta ... > tag,
   which may contain a character encoding.
4. The ASCII detector reads in a small amount of data from the input byte
   stream, and checks that the bytes read are all 7-bit US-ASCII characters.
5. The ICU detector uses the International Components for Unicode for Java
   (ICU4J) package to detect the most likely character encoding.
If the BOM detector is applied and a Unicode byte-order mark (BOM) is found,
then it is removed from the byte stream and not appear in the character stream.
Removing the BOM from the byte stream also overcomes a bug in Java that was
closed with a "Will Not Fix".

Please note that character encoding detection is by necessity a heuristic and is
not guaranteed to always detect the correct encoding.


LICENSE
-------

The entire package, except the bundled third-party components, is distributed
under the Apache License, Version 2.0. See LICENSE.TXT for the text of the
license.

The third-party components included in this distribution are as follows:
1. jsoup 1.7.2 (jsoup.jar)
   URL: http://jsoup.org/
   License: MIT License
2. International Components for Unicode for Java 51.2 (icu4j.jar)
   URL: http://icu-project.org/
   License: ICU License

The source code for this package can be downloaded from:
    http://wing.comp.nus.edu.sg/~tanyeefa/downloads/charsetdetectstreamreader/


