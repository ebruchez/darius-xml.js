## What is Darius XML?

An XML parser based on [Apache Xerces][xerces] running in the Web browser via [Scala.js][scalajs].

See the [demo][demo] for a quick feel.

## Status

Darius XML is able to parse a number of XML files already, but hasn't been tested to any serious extent.

There is not yet a usable API, whether for JavaScript or for Scala ([issue #2][issue2]).

## Why is it needed?

If you need to process XML documents, you need an XML parser. Web browsers all embed one, and there is even a [standard
API for this][domparser], but:
 
- there is no support for any XML parsing or XML DOM within Web workers
- standard error handling is awkward and varies between browsers
- each browser has its own implementation, so parsing behavior might vary
  
There are a numbers of XML parsers for JavaScript, but as of April 2015 I have found none that can pretends to a minimum
of compliance with the XML specification.

## How it is done

Since it doesn't seem wise to start writing an XML parser from scratch in 2015, I thought that starting with the
time-tested Apache Xerces (which is also Java's built-in XML parser), which has more than 15 years of development behind
it, might be a good idea.

But Xerces is written in Java. So in order to run it in the browser, I converted a subset of the code to Scala (first
automatically, then fixing issues manually) in order to compile it with Scala.js.

## Goals and non-goals

The following are the immediate goals:

- provide XML parsing (including parsing the internal DTD subset if present)
- work in evergreen browsers
- have Scala and JavaScript APIs 

The following are explicit non-goals at the moment:

- be a validating parser (whether with DTD or XML Schema)
- support SAX, DOM, JAXP APIs, XML Schema, XInclude, XPath, and XML 1.1
- remain API-compatible with Xerces
- support obsolete or failed features (XML without namespaces, XML 1.1)
- look like idiomatic Scala

## Benefits and drawbacks

Benefits:

- The parser reuses code which has been tested for more than 15 years.
- There is no need to write a parser from scratch. (You might think it is easy, being "just pointy brackets", but
  doing it properly, following the [XML 1.0][xml10] and [Namespaces in XML 1.0][xmlns10] specs, is in fact pretty hard.)

Drawbacks:

- As excellent as Scala.js is, the resulting library is probably larger than a library written from scratch in
  JavaScript or languages closer to it. In addition to the fact that Scala.js works with higher-level constructs than
  plain JavaScript, there can also be leftover bloat from the original Java library, and (at first at least) the Scala
  version depends on Scala collections (such as `HashMap`) which add some weight.
- The Scala code is the result of a translation from Java which means that errors might have been introduced. The best
  way to address this is to run a solid test suite on the Scala.js version.

## Numbers

The current demo app, including the XML parser:
 
- weighs 144 KB of compressed JavaScript
    - including relevant Scala library code
    - excluding jQuery, which is used for the demo app
- has 82 Scala files (including a few files for the demo)
- has 11,867 lines of non-blank, non-comments, non-test Scala code
    - `impl`: 7,627 lines 
    - `util`: 2,630 lines
    - `xni`: 767 lines
    - `parsers`: 664 lines
    - `demo`: 139 lines
    - `api`: 40 lines
    
For comparison:

- a compressed Java JAR containing only the Xerces parser is 206 KB
- a compressed Java JAR of the full Xerces parser is 1.4 MB

## API

TBD

## Building

```
sbt fastOptJs
sbt fullOptJs
```

## Open source license

Xerces is provided under the Apache 2 license. This means that the Scala files directly translated from Xerces are also
released under that same Apache 2 license.

Files specific to Darius XML are also under the Apache 2 license. 

[xerces]: https://xerces.apache.org/xerces2-j/

[scalajs]: http://www.scala-js.org/

[demo]: http://ebruchez.github.io/darius-xml.js/

[issue2]: https://github.com/ebruchez/darius-xml.js/issues/2

[domparser]: https://developer.mozilla.org/en-US/docs/Web/API/DOMParser

[xml10]: http://www.w3.org/TR/REC-xml/

[xmlns10]: http://www.w3.org/TR/REC-xml-names/
