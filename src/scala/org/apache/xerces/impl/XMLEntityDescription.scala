package org.apache.xerces.impl

import org.apache.xerces.xni.XMLResourceIdentifier

/**
 * This interface describes the properties of entities--their
 * physical location and their name.
 */
trait XMLEntityDescription extends XMLResourceIdentifier {

  /**
   * Sets the name of the entity.
   *
   * @param name the name of the entity
   */
  def setEntityName(name: String): Unit

  /**
   * Returns the name of the entity.
   *
   * @return the name of the entity
   */
  def getEntityName: String
}
