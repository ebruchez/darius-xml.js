package org.apache.xerces.util

/**
 * A structure that represents an error code, characterized by
 * a domain and a message key.
 */
class XMLErrorCode(var fDomain: String, var fKey: String) {

  /**
   * Convenience method to set the values of an XMLErrorCode.
   *
   * @param domain The error domain.
   * @param key The key of the error message.
   */
  def setValues(domain: String, key: String): Unit = {
    fDomain = domain
    fKey = key
  }

  /**
   * Indicates whether some other object is equal to this XMLErrorCode.
   *
   * @param obj the object with which to compare.
   */
  override def equals(obj: Any): Boolean = {
    if (! obj.isInstanceOf[XMLErrorCode]) // @ebruchez: is this correct?
      return false
    val err = obj.asInstanceOf[XMLErrorCode]
    fDomain == err.fDomain && fKey == err.fKey
  }

  override def hashCode(): Int = fDomain.hashCode + fKey.hashCode
}
