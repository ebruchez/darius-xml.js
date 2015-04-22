package org.apache.xerces.xni.parser

import java.io.IOException

import org.apache.xerces.xni.XMLResourceIdentifier
import org.apache.xerces.xni.XNIException

/**
 * This interface is used to resolve external parsed entities. The
 * application can register an object that implements this interface
 * with the parser configuration in order to intercept entities and
 * resolve them explicitly. If the registered entity resolver cannot
 * resolve the entity, it should return `null` so that the
 * parser will try to resolve the entity using a default mechanism.
 *
 * @see XMLParserConfiguration
 */
trait XMLEntityResolver {

  /**
   * Resolves an external parsed entity. If the entity cannot be
   * resolved, this method should return null.
   *
   * @param resourceIdentifier location of the XML resource to resolve
   *
   * @throws XNIException Thrown on general error.
   * @throws IOException  Thrown if resolved entity stream cannot be
   *                      opened or some other i/o error occurs.
   * @see org.apache.xerces.xni.XMLResourceIdentifier
   */
  def resolveEntity(resourceIdentifier: XMLResourceIdentifier): XMLInputSource
}
