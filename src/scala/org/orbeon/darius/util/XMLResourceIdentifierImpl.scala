package org.orbeon.darius.util

import org.orbeon.darius.xni.XMLResourceIdentifier

/**
 * The XMLResourceIdentifierImpl class is an implementation of the
 * XMLResourceIdentifier interface which defines the location identity
 * of a resource.
 */
class XMLResourceIdentifierImpl extends XMLResourceIdentifier {

  /**
   The public identifier.
   */
  protected var fPublicId: String = _

  /**
   The literal system identifier.
   */
  protected var fLiteralSystemId: String = _

  /**
   The base system identifier.
   */
  protected var fBaseSystemId: String = _

  /**
   The expanded system identifier.
   */
  protected var fExpandedSystemId: String = _

  /**
   The namespace of the resource.
   */
  protected var fNamespace: String = _

  /**
   * Constructs a resource identifier.
   *
   * @param publicId The public identifier.
   * @param literalSystemId The literal system identifier.
   * @param baseSystemId The base system identifier.
   * @param expandedSystemId The expanded system identifier.
   */
  def this(publicId: String, 
      literalSystemId: String, 
      baseSystemId: String, 
      expandedSystemId: String) {
    this()
    setValues(publicId, literalSystemId, baseSystemId, expandedSystemId, null)
  }

  /**
   * Constructs a resource identifier.
   *
   * @param publicId The public identifier.
   * @param literalSystemId The literal system identifier.
   * @param baseSystemId The base system identifier.
   * @param expandedSystemId The expanded system identifier.
   * @param namespace The namespace.
   */
  def this(publicId: String, 
      literalSystemId: String, 
      baseSystemId: String, 
      expandedSystemId: String, 
      namespace: String) {
    this()
    setValues(publicId, literalSystemId, baseSystemId, expandedSystemId, namespace)
  }

  /**
   Sets the values of the resource identifier.
   */
  def setValues(publicId: String, 
      literalSystemId: String, 
      baseSystemId: String, 
      expandedSystemId: String): Unit = {
    setValues(publicId, literalSystemId, baseSystemId, expandedSystemId, null)
  }

  /**
   Sets the values of the resource identifier.
   */
  def setValues(publicId: String, 
      literalSystemId: String, 
      baseSystemId: String, 
      expandedSystemId: String, 
      namespace: String): Unit = {
    fPublicId = publicId
    fLiteralSystemId = literalSystemId
    fBaseSystemId = baseSystemId
    fExpandedSystemId = expandedSystemId
    fNamespace = namespace
  }

  /**
   Clears the values.
   */
  def clear(): Unit = {
    fPublicId = null
    fLiteralSystemId = null
    fBaseSystemId = null
    fExpandedSystemId = null
    fNamespace = null
  }

  /**
   Sets the public identifier.
   */
  def setPublicId(publicId: String): Unit = {
    fPublicId = publicId
  }

  /**
   Sets the literal system identifier.
   */
  def setLiteralSystemId(literalSystemId: String): Unit = {
    fLiteralSystemId = literalSystemId
  }

  /**
   Sets the base system identifier.
   */
  def setBaseSystemId(baseSystemId: String): Unit = {
    fBaseSystemId = baseSystemId
  }

  /**
   Sets the expanded system identifier.
   */
  def setExpandedSystemId(expandedSystemId: String): Unit = {
    fExpandedSystemId = expandedSystemId
  }

  /**
   Sets the namespace of the resource.
   */
  def setNamespace(namespace: String): Unit = {
    fNamespace = namespace
  }

  /**
   Returns the public identifier.
   */
  def getPublicId: String = fPublicId

  /**
   Returns the literal system identifier.
   */
  def getLiteralSystemId: String = fLiteralSystemId

  /**
   * Returns the base URI against which the literal SystemId is to be resolved.
   */
  def getBaseSystemId: String = fBaseSystemId

  /**
   Returns the expanded system identifier.
   */
  def getExpandedSystemId: String = fExpandedSystemId

  /**
   Returns the namespace of the resource.
   */
  def getNamespace: String = fNamespace

  /**
   Returns a hash code for this object.
   */
  override def hashCode: Int = {
    var code = 0
    if (fPublicId ne null) {
      code += fPublicId.hashCode
    }
    if (fLiteralSystemId ne null) {
      code += fLiteralSystemId.hashCode
    }
    if (fBaseSystemId ne null) {
      code += fBaseSystemId.hashCode
    }
    if (fExpandedSystemId ne null) {
      code += fExpandedSystemId.hashCode
    }
    if (fNamespace ne null) {
      code += fNamespace.hashCode
    }
    code
  }

  /**
   Returns a string representation of this object.
   */
  override def toString: String = {
    val str = new StringBuffer()
    if (fPublicId ne null) {
      str.append(fPublicId)
    }
    str.append(':')
    if (fLiteralSystemId ne null) {
      str.append(fLiteralSystemId)
    }
    str.append(':')
    if (fBaseSystemId ne null) {
      str.append(fBaseSystemId)
    }
    str.append(':')
    if (fExpandedSystemId ne null) {
      str.append(fExpandedSystemId)
    }
    str.append(':')
    if (fNamespace ne null) {
      str.append(fNamespace)
    }
    str.toString
  }
}
