package org.orbeon.darius.xni

/**
 *  This represents the basic physical description of the location of any
 * XML resource (a Schema grammar, a DTD, a general entity etc.) 
 */
trait XMLResourceIdentifier {

  /**
   Sets the public identifier.
   */
  def setPublicId(publicId: String): Unit

  /**
   Returns the public identifier.
   */
  def getPublicId: String

  /**
   Sets the expanded system identifier.
   */
  def setExpandedSystemId(systemId: String): Unit

  /**
   Returns the expanded system identifier.
   */
  def getExpandedSystemId: String

  /**
   Sets the literal system identifier.
   */
  def setLiteralSystemId(systemId: String): Unit

  /**
   Returns the literal system identifier.
   */
  def getLiteralSystemId: String

  /**
   Sets the base URI against which the literal SystemId is to be
   resolved.
   */
  def setBaseSystemId(systemId: String): Unit

  /**
    Returns the base URI against which the literal SystemId is to be
   resolved. 
   */
  def getBaseSystemId: String

  /**
   Sets the namespace of the resource.
   */
  def setNamespace(namespace: String): Unit

  /**
   Returns the namespace of the resource.
   */
  def getNamespace: String
}
