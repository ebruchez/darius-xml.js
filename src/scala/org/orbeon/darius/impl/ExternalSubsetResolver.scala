package org.orbeon.darius.impl

import java.io.IOException

import org.orbeon.darius.xni.XNIException
import org.orbeon.darius.xni.grammars.XMLDTDDescription
import org.orbeon.darius.xni.parser.XMLEntityResolver
import org.orbeon.darius.xni.parser.XMLInputSource

/**
 * This interface extends `XMLEntityResolver` providing
 * a method to resolve external subsets for documents which do not
 * explicitly provide one. The application can register an object that
 * implements this interface with the parser configuration. If registered,
 * it will be queried to locate an external subset when none is provided,
 * even for documents that do not contain DOCTYPE declarations. If the
 * registered external subset resolver does not provide an external subset
 * for a given document, it should return `null`.
 */
trait ExternalSubsetResolver extends XMLEntityResolver {

  /**
   * Locates an external subset for documents which do not explicitly
   * provide one. If no external subset is provided, this method should
   * return `null`.
   *
   * @param grammarDescription a description of the DTD
   *
   * @throws XNIException Thrown on general error.
   * @throws IOException  Thrown if resolved entity stream cannot be
   *                      opened or some other i/o error occurs.
   */
  def getExternalSubset(grammarDescription: XMLDTDDescription): XMLInputSource
}
