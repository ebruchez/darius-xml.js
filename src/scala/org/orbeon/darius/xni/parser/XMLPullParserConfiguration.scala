package org.orbeon.darius.xni.parser

import java.io.IOException

import org.orbeon.darius.xni.XNIException

/**
 * Represents a parser configuration that can be used as the
 * configuration for a "pull" parser. A pull parser allows the
 * application to drive the parser instead of having document
 * information events "pushed" to the registered handlers.
 * 
 * A pull parser using this type of configuration first calls
 * the `setInputSource` method. After the input
 * source is set, the pull parser repeatedly calls the
 * `parse(boolean):boolean` method. This method
 * returns a value of true if there is more to parse in the
 * document.
 * 
 * Calling the `parse(XMLInputSource)` is equivalent
 * to setting the input source and calling the
 * `parse(boolean):boolean` method with a "complete"
 * value of `true`.
 */
trait XMLPullParserConfiguration extends XMLParserConfiguration {

  /**
   * Sets the input source for the document to parse.
   *
   * @param inputSource The document's input source.
   *
   * @throws XMLConfigurationException Thrown if there is a
   *                        configuration error when initializing the
   *                        parser.
   * @throws IOException Thrown on I/O error.
   *
   * @see #parse(boolean)
   */
  def setInputSource(inputSource: XMLInputSource): Unit

  /**
   * Parses the document in a pull parsing fashion.
   *
   * @param complete True if the pull parser should parse the
   *                 remaining document completely.
   *
   * @return True if there is more document to parse.
   *
   * @throws XNIException Any XNI exception, possibly wrapping
   *                         another exception.
   * @throws IOException  An IO exception from the parser, possibly
   *                         from a byte stream or character stream
   *                         supplied by the parser.
   *
   * @see #setInputSource
   */
  def parse(complete: Boolean): Boolean

  /**
   * If the application decides to terminate parsing before the xml document
   * is fully parsed, the application should call this method to free any
   * resource allocated during parsing. For example, close all opened streams.
   */
  def cleanup(): Unit
}
