package org.orbeon.darius.xni.parser

import java.io.IOException

/**
 * This interface defines a generic DTD scanner. This interface
 * allows a scanner to be used interchangably in existing parser
 * configurations.
 * 
 * If the parser configuration uses a DTD scanner that implements
 * this interface, components should be able to query the scanner
 * instance from the component manager using the following property
 * identifier:
 * 
 *  http://apache.org/xml/properties/internal/dtd-scanner
 */
trait XMLDTDScanner extends XMLDTDSource with XMLDTDContentModelSource {

  /**
   * Sets the input source.
   *
   * @param inputSource The input source or null.
   *
   * @throws IOException Thrown on i/o error.
   */
  def setInputSource(inputSource: XMLInputSource): Unit

  /**
   * Scans the internal subset of the document.
   *
   * @param complete True if the scanner should scan the document
   *                 completely, pushing all events to the registered
   *                 document handler. A value of false indicates that
   *                 that the scanner should only scan the next portion
   *                 of the document and return. A scanner instance is
   *                 permitted to completely scan a document if it does
   *                 not support this "pull" scanning model.
   * @param standalone True if the document was specified as standalone.
   *                   This value is important for verifying certain
   *                   well-formedness constraints.
   * @param hasExternalSubset True if the document has an external DTD.
   *                          This allows the scanner to properly notify
   *                          the handler of the end of the DTD in the
   *                          absence of an external subset.
   *
   * @return True if there is more to scan, false otherwise.
   */
  def scanDTDInternalSubset(complete: Boolean, standalone: Boolean, hasExternalSubset: Boolean): Boolean

  /**
   * Scans the external subset of the document.
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
  def scanDTDExternalSubset(complete: Boolean): Boolean
}
