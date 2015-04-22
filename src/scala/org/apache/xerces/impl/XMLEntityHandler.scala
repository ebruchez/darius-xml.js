package org.apache.xerces.impl

import org.apache.xerces.xni.Augmentations
import org.apache.xerces.xni.XMLResourceIdentifier
import org.apache.xerces.xni.XNIException

/**
 * The entity handler interface defines methods to report information
 * about the start and end of entities.
 *
 * @see org.apache.xerces.impl.XMLEntityScanner
 */
trait XMLEntityHandler {

  /**
   * This method notifies of the start of an entity. The DTD has the
   * pseudo-name of "[dtd]" parameter entity names start with '%'; and
   * general entities are just specified by their name.
   *
   * @param name     The name of the entity.
   * @param identifier The resource identifier.
   * @param encoding The auto-detected IANA encoding name of the entity
   *                 stream. This value will be null in those situations
   *                 where the entity encoding is not auto-detected (e.g.
   *                 internal entities or a document entity that is
   *                 parsed from a java.io.Reader).
   * @param augs     Additional information that may include infoset augmentations
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def startEntity(name: String, 
      identifier: XMLResourceIdentifier, 
      encoding: String, 
      augs: Augmentations): Unit

  /**
   * This method notifies the end of an entity. The DTD has the pseudo-name
   * of "[dtd]" parameter entity names start with '%'; and general entities
   * are just specified by their name.
   *
   * @param name The name of the entity.
   * @param augs Additional information that may include infoset augmentations
   *
   * @throws XNIException Thrown by handler to signal an error.
   */
  def endEntity(name: String, augs: Augmentations): Unit
}
