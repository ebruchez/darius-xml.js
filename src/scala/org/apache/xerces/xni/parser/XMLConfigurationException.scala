package org.apache.xerces.xni.parser

import org.apache.xerces.xni.XNIException

object XMLConfigurationException {

  /**
   Exception type: identifier not recognized.
   */
  val NOT_RECOGNIZED: Short = 0

  /**
   Exception type: identifier not supported.
   */
  val NOT_SUPPORTED: Short = 1
}

/**
 * An XNI parser configuration exception. This exception class extends
 * `XNIException` in order to differentiate between general
 * parsing errors and configuration errors.
 */
class XMLConfigurationException(fType: Short, fIdentifier: String)
    extends XNIException(fIdentifier) {

  /**
   * Returns the exception type.
   *
   * @see #NOT_RECOGNIZED
   * @see #NOT_SUPPORTED
   */
  def getType: Short = fType

  /**
   Returns the feature or property identifier.
   */
  def getIdentifier: String = fIdentifier
}
