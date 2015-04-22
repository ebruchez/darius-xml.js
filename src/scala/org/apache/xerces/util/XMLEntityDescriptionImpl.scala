package org.apache.xerces.util

import org.apache.xerces.impl.XMLEntityDescription

/**
 * This class is an implementation of the XMLEntityDescription
 * interface which describes the properties of an entity.
 */
class XMLEntityDescriptionImpl extends XMLResourceIdentifierImpl with XMLEntityDescription {

  /**
   * Constructs an entity description.
   *
   * @param entityName The name of the entity.
   * @param publicId The public identifier.
   * @param literalSystemId The literal system identifier.
   * @param baseSystemId The base system identifier.
   * @param expandedSystemId The expanded system identifier.
   */
  def this(entityName: String, 
      publicId: String, 
      literalSystemId: String, 
      baseSystemId: String, 
      expandedSystemId: String) {
    this()
    setDescription(entityName, publicId, literalSystemId, baseSystemId, expandedSystemId)
  }

  /**
   * Constructs a resource identifier.
   *
   * @param entityName The name of the entity.
   * @param publicId The public identifier.
   * @param literalSystemId The literal system identifier.
   * @param baseSystemId The base system identifier.
   * @param expandedSystemId The expanded system identifier.
   * @param namespace The namespace.
   */
  def this(entityName: String, 
      publicId: String, 
      literalSystemId: String, 
      baseSystemId: String, 
      expandedSystemId: String, 
      namespace: String) {
    this()
    setDescription(entityName, publicId, literalSystemId, baseSystemId, expandedSystemId, namespace)
  }

  /**
   The name of the entity.
   */
  protected var fEntityName: String = _

  /**
   * Sets the name of the entity.
   *
   * @param name the name of the entity
   */
  def setEntityName(name: String): Unit = {
    fEntityName = name
  }

  /**
   * Returns the name of the entity.
   *
   * @return the name of the entity
   */
  def getEntityName: String = fEntityName

  /**
   * Sets the values of this entity description.
   *
   * @param entityName The name of the entity.
   * @param publicId The public identifier.
   * @param literalSystemId The literal system identifier.
   * @param baseSystemId The base system identifier.
   * @param expandedSystemId The expanded system identifier.
   */
  def setDescription(entityName: String, 
      publicId: String, 
      literalSystemId: String, 
      baseSystemId: String, 
      expandedSystemId: String): Unit = {
    setDescription(entityName, publicId, literalSystemId, baseSystemId, expandedSystemId, null)
  }

  /**
   * Sets the values of this entity description.
   *
   * @param entityName The name of the entity.
   * @param publicId The public identifier.
   * @param literalSystemId The literal system identifier.
   * @param baseSystemId The base system identifier.
   * @param expandedSystemId The expanded system identifier.
   * @param namespace The namespace.
   */
  def setDescription(entityName: String, 
      publicId: String, 
      literalSystemId: String, 
      baseSystemId: String, 
      expandedSystemId: String, 
      namespace: String): Unit = {
    fEntityName = entityName
    setValues(publicId, literalSystemId, baseSystemId, expandedSystemId, namespace)
  }

  /**
   * Clears the values.
   */
  override def clear(): Unit = {
    super.clear()
    fEntityName = null
  }

  /**
   Returns a hash code for this object.
   */
  override def hashCode: Int = {
    var code = super.hashCode
    if (fEntityName ne null) {
      code += fEntityName.hashCode
    }
    code
  }

  /**
   Returns a string representation of this object.
   */
  override def toString: String = {
    val str = new StringBuffer()
    if (fEntityName ne null) {
      str.append(fEntityName)
    }
    str.append(':')
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
