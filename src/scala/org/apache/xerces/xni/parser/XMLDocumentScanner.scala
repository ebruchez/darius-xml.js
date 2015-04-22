package org.apache.xerces.xni.parser

import java.io.IOException

/**
 * This interface defines a generic document scanner. This interface
 * allows a scanner to be used interchangably in existing parser
 * configurations.
 * 
 * If the parser configuration uses a document scanner that implements
 * this interface, components should be able to query the scanner
 * instance from the component manager using the following property
 * identifier:
 * 
 *  http://apache.org/xml/properties/internal/document-scanner
 */
trait XMLDocumentScanner extends XMLDocumentSource {

  /**
   * Sets the input source.
   *
   * @param inputSource The input source.
   *
   * @throws IOException Thrown on i/o error.
   */
  def setInputSource(inputSource: XMLInputSource): Unit

  /**
   * Scans a document.
   *
   * @param complete True if the scanner should scan the document
   *                 completely, pushing all events to the registered
   *                 document handler. A value of false indicates that
   *                 that the scanner should only scan the next portion
   *                 of the document and return. A scanner instance is
   *                 permitted to completely scan a document if it does
   *                 not support this "pull" scanning model.
   *
   * @return True if there is more to scan, false otherwise.
   */
  def scanDocument(complete: Boolean): Boolean
}
